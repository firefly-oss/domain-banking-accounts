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


package io.fireflysoftware.domain.banking.accounts.core.holder.workflows;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.AddPartyToContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RemovePartyFromContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.GetAccountContractCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.holder.utils.AddAccountHolderConstants.*;

@Saga(name = SAGA_ADD_ACCOUNT_HOLDER_NAME)
@Service
public class AddAccountHolderSaga {

    private final CommandBus commandBus;

    public AddAccountHolderSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_GET_CONTRACT)
    @StepEvent(type = EVENT_CONTRACT_RETRIEVED)
    public Mono<UUID> getAccountContract(GetAccountContractCommand cmd, ExecutionContext ctx) {
        // ExecutionContext.variables is a ConcurrentHashMap — guard against null value NPE.
        if (cmd.getAccountId() != null) ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        return commandBus.<UUID>send(cmd)
                .doOnNext(contractId -> ctx.putVariable(CTX_CONTRACT_ID, contractId));
    }

    @SagaStep(id = STEP_ADD_PARTY, compensate = COMPENSATE_REMOVE_PARTY, dependsOn = STEP_GET_CONTRACT)
    @StepEvent(type = EVENT_PARTY_ADDED)
    public Mono<UUID> addPartyToContract(AddPartyToContractCommand cmd, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        ctx.putVariable(CTX_PARTY_ID, cmd.getPartyId());
        return commandBus.<UUID>send(cmd.withContractId(contractId));
    }

    public Mono<Void> removePartyFromContract(UUID partyLinkId, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        UUID partyId = (UUID) ctx.getVariable(CTX_PARTY_ID);
        return commandBus.<Void>send(RemovePartyFromContractCommand.builder()
                .contractId(contractId)
                .partyId(partyId)
                .build());
    }

    @SagaStep(id = STEP_RECORD_ACTIVITY, dependsOn = STEP_ADD_PARTY)
    @StepEvent(type = EVENT_ACTIVITY_RECORDED)
    public Mono<String> recordActivity(ExecutionContext ctx) {
        // No compensation - notification is informational.
        // Must emit exactly one element to satisfy @SagaStep contract (Mono.empty() = FAILURE).
        return Mono.just("skipped");
    }
}
