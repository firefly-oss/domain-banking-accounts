/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.banking.accounts.core.opening.handlers;

import io.fireflysoftware.core.kycb.sdk.api.KycVerificationApi;
import io.fireflysoftware.core.kycb.sdk.model.KycVerificationDTO;
import io.fireflysoftware.domain.banking.accounts.core.opening.EligibilityDecision;
import io.fireflysoftware.domain.banking.accounts.core.opening.KycRows;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.PersistOpeningEligibilityCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@CommandHandlerComponent
public class PersistOpeningEligibilityHandler extends CommandHandler<PersistOpeningEligibilityCommand, EligibilityDecision> {

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final KycVerificationApi kyc;

    public PersistOpeningEligibilityHandler(KycVerificationApi kyc) {
        this.kyc = kyc;
    }

    @Override
    protected Mono<EligibilityDecision> doHandle(PersistOpeningEligibilityCommand cmd) {
        String status = cmd.eligible() ? APPROVED : REJECTED;
        KycVerificationDTO body = new KycVerificationDTO()
                .partyId(cmd.partyId())
                .verificationStatus(status)
                .verificationDate(LocalDateTime.now().withNano(0))
                .verificationMethod("ACCOUNT_OPENING")
                .verificationAgent("domain-banking-accounts")
                .riskScore(cmd.eligible() ? 10 : 80)
                .riskLevel(cmd.eligible() ? "LOW" : "HIGH")
                .enhancedDueDiligence(false);
        return kyc.createKycVerification(cmd.partyId(), body, KycRows.key())
                .flatMap(stored -> {
                    if (stored.getKycVerificationId() == null || stored.getVerificationStatus() == null
                            || stored.getVerificationStatus().isBlank()) {
                        return Mono.error(new IllegalStateException("Eligibility was not recorded"));
                    }
                    return Mono.just(new EligibilityDecision(
                            stored.getKycVerificationId(),
                            APPROVED.equals(stored.getVerificationStatus()),
                            stored.getVerificationStatus(),
                            cmd.partyId()));
                });
    }
}
