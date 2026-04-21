package com.firefly.domain.banking.accounts.core.account.workflows;

import com.firefly.domain.banking.accounts.core.account.commands.*;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.*;

/**
 * Saga orchestrator for opening a new bank account.
 *
 * This saga coordinates the multi-step process of creating an account:
 * 1. Create a contract for the account relationship
 * 2. Add the primary holder as a party to the contract
 * 3. Create the account record linked to the contract
 * 4. Initialize the account balance
 * 5. Create the main space (bucket) for the account
 * 6. Record the initial status in history
 * 7. Set default parameters (fees, limits, rates)
 *
 * Each step has a compensation method for rollback on failure.
 */
@Saga(name = SAGA_OPEN_ACCOUNT_NAME)
@Service
public class OpenAccountSaga {

    private final CommandBus commandBus;

    public OpenAccountSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_CREATE_CONTRACT, compensate = COMPENSATE_TERMINATE_CONTRACT)
    @StepEvent(type = EVENT_CONTRACT_CREATED)
    public Mono<UUID> createContract(CreateContractCommand cmd, ExecutionContext ctx) {
        return commandBus.<UUID>send(cmd)
                .doOnNext(contractId -> ctx.putVariable(CTX_CONTRACT_ID, contractId));
    }

    public Mono<Void> terminateContract(UUID contractId) {
        return commandBus.<Void>send(TerminateContractCommand.builder()
                .contractId(contractId)
                .build());
    }

    @SagaStep(id = STEP_ADD_PARTY, compensate = COMPENSATE_REMOVE_PARTY, dependsOn = STEP_CREATE_CONTRACT)
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

    @SagaStep(id = STEP_CREATE_ACCOUNT, compensate = COMPENSATE_CLOSE_ACCOUNT, dependsOn = STEP_ADD_PARTY)
    @StepEvent(type = EVENT_ACCOUNT_CREATED)
    public Mono<UUID> createAccount(CreateAccountCommand cmd, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        return commandBus.<UUID>send(cmd.withContractId(contractId))
                .doOnNext(accountId -> ctx.putVariable(CTX_ACCOUNT_ID, accountId));
    }

    public Mono<Void> closeAccount(UUID accountId) {
        return commandBus.<Void>send(CloseAccountCommand.builder()
                .accountId(accountId)
                .reason("Compensation: account opening failed")
                .build());
    }

    @SagaStep(id = STEP_INIT_BALANCE, compensate = COMPENSATE_DELETE_BALANCE, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_BALANCE_INITIALIZED)
    public Mono<UUID> initializeBalance(InitializeBalanceCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .doOnNext(balanceId -> ctx.putVariable(CTX_BALANCE_ID, balanceId));
    }

    public Mono<Void> deleteBalance(UUID balanceId, ExecutionContext ctx) {
        // Balance deletion is typically not needed as account closure handles it
        return Mono.empty();
    }

    @SagaStep(id = STEP_CREATE_SPACE, compensate = COMPENSATE_DELETE_SPACE, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_SPACE_CREATED)
    public Mono<UUID> createMainSpace(CreateMainSpaceCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .doOnNext(spaceId -> ctx.putVariable(CTX_MAIN_SPACE_ID, spaceId));
    }

    public Mono<Void> deleteSpace(UUID spaceId, ExecutionContext ctx) {
        // Space deletion is typically not needed as account closure handles it
        return Mono.empty();
    }

    @SagaStep(id = STEP_RECORD_STATUS, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_STATUS_RECORDED)
    public Mono<UUID> recordStatusHistory(RecordStatusCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .doOnNext(statusEntryId -> ctx.putVariable(CTX_STATUS_ENTRY_ID, statusEntryId));
    }
    // No compensation for status history - it's an immutable audit trail

    @SagaStep(id = STEP_SET_PARAMS, compensate = COMPENSATE_DELETE_PARAMS, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_PARAMS_SET)
    public Mono<UUID> setDefaultParameters(SetParameterCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.empty();
        }
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId));
    }

    public Mono<Void> deleteParameters(UUID parameterId, ExecutionContext ctx) {
        // Parameter deletion is typically not needed as account closure handles it
        return Mono.empty();
    }
}
