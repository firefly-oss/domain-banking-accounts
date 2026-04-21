package com.firefly.domain.banking.accounts.core.account.workflows;

import com.firefly.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import com.firefly.domain.banking.accounts.core.account.commands.TerminateContractCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.*;

/**
 * Saga orchestrator for closing a bank account.
 *
 * Steps:
 * 1. Validate that all balances are zero
 * 2. Settle any outstanding fees/interest
 * 3. Remove all spaces
 * 4. Update account status to CLOSED
 * 5. Terminate the associated contract
 */
@Saga(name = SAGA_CLOSE_ACCOUNT_NAME)
@Service
public class CloseAccountSaga {

    private final CommandBus commandBus;

    public CloseAccountSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_VALIDATE_BALANCE)
    @StepEvent(type = EVENT_BALANCE_VALIDATED)
    public Mono<Boolean> validateZeroBalance(CloseAccountCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        // TODO: Call AccountBalancesApi.getBalances and verify all are zero
        // If not zero, return Mono.error(new AccountNotEmptyException(...))
        return Mono.just(true);
    }

    @SagaStep(id = STEP_SETTLE, compensate = COMPENSATE_REVERSE_SETTLEMENT, dependsOn = STEP_VALIDATE_BALANCE)
    @StepEvent(type = EVENT_SETTLED)
    public Mono<Void> settleOutstanding(CloseAccountCommand cmd, ExecutionContext ctx) {
        // TODO: Check for pending fees, accrued interest and create settlement
        return Mono.empty();
    }

    public Mono<Void> reverseSettlement(ExecutionContext ctx) {
        // TODO: Reverse settlement transaction if needed
        return Mono.empty();
    }

    @SagaStep(id = STEP_REMOVE_SPACES, compensate = COMPENSATE_RECREATE_SPACES, dependsOn = STEP_SETTLE)
    @StepEvent(type = EVENT_SPACES_REMOVED)
    @SuppressWarnings("unchecked")
    public Mono<Void> removeSpaces(CloseAccountCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        // TODO: Get all spaces, store backup, delete non-MAIN first, then MAIN
        ctx.putVariable(CTX_SPACES_BACKUP, List.of());
        return Mono.empty();
    }

    @SuppressWarnings("unchecked")
    public Mono<Void> recreateSpaces(ExecutionContext ctx) {
        List<?> spacesBackup = (List<?>) ctx.getVariable(CTX_SPACES_BACKUP);
        if (spacesBackup == null || spacesBackup.isEmpty()) {
            return Mono.empty();
        }
        // TODO: Recreate spaces from backup
        return Mono.empty();
    }

    @SagaStep(id = STEP_UPDATE_STATUS, compensate = COMPENSATE_REVERT_STATUS, dependsOn = STEP_REMOVE_SPACES)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<Void> updateAccountStatus(CloseAccountCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        ctx.putVariable(CTX_PREVIOUS_STATUS, "OPEN");
        // TODO: Update account status to CLOSED and record in history
        return Mono.empty();
    }

    public Mono<Void> revertAccountStatus(ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_STATUS);
        // TODO: Revert to previous status
        return Mono.empty();
    }

    @SagaStep(id = STEP_TERMINATE_CONTRACT, compensate = COMPENSATE_REACTIVATE_CONTRACT, dependsOn = STEP_UPDATE_STATUS)
    @StepEvent(type = EVENT_CONTRACT_TERMINATED)
    public Mono<Void> terminateContract(CloseAccountCommand cmd, ExecutionContext ctx) {
        // TODO: Get contractId from account, then terminate
        UUID contractId = UUID.randomUUID(); // Placeholder
        ctx.putVariable(CTX_CONTRACT_ID, contractId);
        return commandBus.<Void>send(TerminateContractCommand.builder()
                .contractId(contractId)
                .build());
    }

    public Mono<Void> reactivateContract(ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        // TODO: Reactivate contract
        return Mono.empty();
    }
}
