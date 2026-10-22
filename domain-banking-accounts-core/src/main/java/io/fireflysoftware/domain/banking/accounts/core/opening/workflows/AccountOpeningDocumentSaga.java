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


package io.fireflysoftware.domain.banking.accounts.core.opening.workflows;

import io.fireflysoftware.domain.banking.accounts.core.opening.DocumentVerificationDecision;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.InspectDocumentsCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.RecordDocumentStatusCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.RestoreDocumentStatusCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Saga(name = AccountOpeningDocumentSaga.NAME)
@Service
public class AccountOpeningDocumentSaga {

    public static final String NAME = "account-opening-documents";
    public static final String STEP_INSPECT = "inspect-documents";
    public static final String STEP_RECORD = "record-documents";

    private static final String VAR_PARTY_ID = "partyId";
    private static final String VAR_VERIFICATION_ID = "verificationId";
    private static final String VAR_PREVIOUS_STATUS = "previousStatus";
    private static final String VAR_NEXT_STATUS = "nextStatus";

    private final CommandBus commandBus;

    public AccountOpeningDocumentSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_INSPECT)
    public Mono<InspectDocumentsCommand.Findings> inspectDocuments(InspectDocumentsCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(VAR_PARTY_ID, cmd.partyId());
        return commandBus.<InspectDocumentsCommand.Findings>send(cmd)
                .doOnNext(findings -> {
                    ctx.putVariable(VAR_VERIFICATION_ID, findings.verificationId());
                    ctx.putVariable(VAR_PREVIOUS_STATUS, findings.previousStatus());
                    ctx.putVariable(VAR_NEXT_STATUS, findings.nextStatus());
                });
    }

    @SagaStep(id = STEP_RECORD, dependsOn = STEP_INSPECT, compensate = "restoreDocumentStatus")
    public Mono<DocumentVerificationDecision> recordDocuments(InspectDocumentsCommand cmd, ExecutionContext ctx) {
        UUID verificationId = ctx.getVariableAs(VAR_VERIFICATION_ID, UUID.class);
        String next = ctx.getVariableAs(VAR_NEXT_STATUS, String.class);
        return commandBus.<DocumentVerificationDecision>send(new RecordDocumentStatusCommand(cmd.partyId(), verificationId, next));
    }

    public Mono<Void> restoreDocumentStatus(DocumentVerificationDecision decision, ExecutionContext ctx) {
        UUID partyId = ctx.getVariableAs(VAR_PARTY_ID, UUID.class);
        UUID verificationId = ctx.getVariableAs(VAR_VERIFICATION_ID, UUID.class);
        String previous = ctx.getVariableAs(VAR_PREVIOUS_STATUS, String.class);
        if (partyId == null || verificationId == null) {
            return Mono.empty();
        }
        return commandBus.<Void>send(new RestoreDocumentStatusCommand(partyId, verificationId, previous == null ? "" : previous));
    }
}
