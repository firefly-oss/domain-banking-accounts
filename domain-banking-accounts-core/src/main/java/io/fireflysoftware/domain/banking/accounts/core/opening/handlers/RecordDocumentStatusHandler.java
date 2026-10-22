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
import io.fireflysoftware.domain.banking.accounts.core.opening.DocumentVerificationDecision;
import io.fireflysoftware.domain.banking.accounts.core.opening.KycRows;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.RecordDocumentStatusCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@CommandHandlerComponent
public class RecordDocumentStatusHandler extends CommandHandler<RecordDocumentStatusCommand, DocumentVerificationDecision> {

    private final KycVerificationApi kyc;

    public RecordDocumentStatusHandler(KycVerificationApi kyc) {
        this.kyc = kyc;
    }

    @Override
    protected Mono<DocumentVerificationDecision> doHandle(RecordDocumentStatusCommand cmd) {
        return kyc.getKycVerification(cmd.partyId(), cmd.verificationId(), KycRows.key())
                .switchIfEmpty(Mono.error(new IllegalStateException("KYC verification was not found")))
                .flatMap(current -> {
                    current.setVerificationStatus(cmd.status());
                    current.setVerificationDate(LocalDateTime.now().withNano(0));
                    return kyc.updateKycVerification(cmd.partyId(), cmd.verificationId(), current, KycRows.key());
                })
                .flatMap(stored -> {
                    if (stored.getKycVerificationId() == null || stored.getVerificationStatus() == null
                            || stored.getVerificationStatus().isBlank()) {
                        return Mono.error(new IllegalStateException("Document verification was not recorded"));
                    }
                    return Mono.just(new DocumentVerificationDecision(stored.getKycVerificationId(), stored.getVerificationStatus()));
                });
    }
}
