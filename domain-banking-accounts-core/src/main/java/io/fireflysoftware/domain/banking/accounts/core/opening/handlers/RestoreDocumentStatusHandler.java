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
import io.fireflysoftware.domain.banking.accounts.core.opening.KycRows;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.RestoreDocumentStatusCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RestoreDocumentStatusHandler extends CommandHandler<RestoreDocumentStatusCommand, Void> {

    private final KycVerificationApi kyc;

    public RestoreDocumentStatusHandler(KycVerificationApi kyc) {
        this.kyc = kyc;
    }

    @Override
    protected Mono<Void> doHandle(RestoreDocumentStatusCommand cmd) {
        return kyc.getKycVerification(cmd.partyId(), cmd.verificationId(), KycRows.key())
                .flatMap(current -> {
                    current.setVerificationStatus(cmd.status());
                    return kyc.updateKycVerification(cmd.partyId(), cmd.verificationId(), current, KycRows.key());
                })
                .then();
    }
}
