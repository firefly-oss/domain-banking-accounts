package com.firefly.domain.banking.accounts.core.account.workflows;

import com.firefly.core.banking.ledger.sdk.model.TransactionLineFeeDTO;
import com.firefly.core.banking.ledger.sdk.model.TransactionLineTransferDTO;
import com.firefly.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import com.firefly.domain.banking.accounts.core.account.commands.TerminateContractCommand;
import com.firefly.domain.banking.accounts.core.account.exceptions.AccountNotEmptyException;
import com.firefly.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import com.firefly.domain.banking.accounts.core.ledger.queries.GetAccountResidualBalanceQuery;
import com.firefly.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
import com.firefly.domain.banking.accounts.infra.properties.LedgerGlProperties;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.argument.CorrelationId;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REACTIVATE_CONTRACT;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_RECREATE_SPACES;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REVERSE_SETTLEMENT;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REVERT_STATUS;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_CURRENCY;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_CONTRACT_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_DESTINATION_ACCOUNT_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_FEES_OWED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_FORCE_CLOSE;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_PREVIOUS_STATUS;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_RESIDUAL_AMOUNT;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_FEE_TX_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_RESIDUAL_TX_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SPACES_BACKUP;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_BALANCE_VALIDATED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_CONTRACT_TERMINATED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_SETTLED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_SPACES_REMOVED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_STATUS_UPDATED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_BALANCE_VALIDATED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SETTLED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SKIPPED;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.SAGA_CLOSE_ACCOUNT_NAME;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.SETTLE_FEES_TX_KEY;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.SETTLE_RESIDUAL_TX_KEY;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_REMOVE_SPACES;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_SETTLE;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_TERMINATE_CONTRACT;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_UPDATE_STATUS;
import static com.firefly.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_VALIDATE_BALANCE;

/**
 * Saga orchestrator for closing a bank account.
 *
 * <p>Balance validation is ledger-driven: we ask core-banking-ledger for the signed residual
 * ({@code Σ CREDIT − Σ DEBIT}) across every leg on the account, ignoring the
 * {@code account_balance} projection. Closure decisions happen on that number:
 * <ul>
 *   <li>If residual is zero → proceed to settlement (which may be a no-op).</li>
 *   <li>If residual is non-zero AND {@code forceClose=false} → fail with
 *       {@link AccountNotEmptyException} so the caller can re-issue with
 *       {@code destinationAccountId}.</li>
 *   <li>If residual is non-zero AND {@code forceClose=true} → proceed to settlement, which
 *       posts a fee leg (if {@code feesOwed > 0}) and a residual transfer leg (if a
 *       destination is supplied and the after-fees residual is positive).</li>
 * </ul>
 */
@Slf4j
@Saga(name = SAGA_CLOSE_ACCOUNT_NAME)
@Service
public class CloseAccountSaga {

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String TX_TYPE_FEE = "FEE";
    private static final String TX_TYPE_TRANSFER = "TRANSFER";
    private static final String LINE_TYPE_FEE = "FEE";
    private static final String LINE_TYPE_TRANSFER = "TRANSFER";
    private static final String STATUS_POSTED = "POSTED";
    private static final String RELATION_REVERSAL = "REVERSAL";
    private static final String FEE_TYPE_CLOSURE = "ACCOUNT_CLOSURE_FEE";
    private static final String FEE_CALC_METHOD_FIXED = "FIXED";
    private static final String DESC_SETTLE_FEES = "Account closure fees settlement";
    private static final String DESC_SETTLE_RESIDUAL = "Account closure residual transfer";
    private static final String DESC_REVERSE_FEES = "Reversal of closure fees settlement";
    private static final String DESC_REVERSE_RESIDUAL = "Reversal of closure residual transfer";

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final LedgerGlProperties ledgerGlProperties;

    public CloseAccountSaga(CommandBus commandBus, QueryBus queryBus, LedgerGlProperties ledgerGlProperties) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.ledgerGlProperties = ledgerGlProperties;
    }

    @SagaStep(id = STEP_VALIDATE_BALANCE)
    @StepEvent(type = EVENT_BALANCE_VALIDATED)
    public Mono<Object> validateZeroBalance(CloseAccountCommand cmd, ExecutionContext ctx) {
        // NOTE: ExecutionContext.variables is a ConcurrentHashMap — it rejects null values
        // with an unhelpful NPE (error=null in orchestration logs). Guard every put.
        if (cmd.getAccountId() != null) ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        if (cmd.getCurrency() != null) ctx.putVariable(CTX_ACCOUNT_CURRENCY, cmd.getCurrency());
        ctx.putVariable(CTX_FORCE_CLOSE, Boolean.TRUE.equals(cmd.getForceClose()));
        if (cmd.getDestinationAccountId() != null) ctx.putVariable(CTX_DESTINATION_ACCOUNT_ID, cmd.getDestinationAccountId());
        if (cmd.getFeesOwed() != null) ctx.putVariable(CTX_FEES_OWED, cmd.getFeesOwed());

        return queryBus.<AccountResidualBalanceResult>query(GetAccountResidualBalanceQuery.builder()
                        .accountId(cmd.getAccountId())
                        .build())
                .flatMap(result -> {
                    BigDecimal residual = result == null || result.getResidual() == null
                            ? BigDecimal.ZERO : result.getResidual();
                    ctx.putVariable(CTX_RESIDUAL_AMOUNT, residual);
                    if (residual.signum() == 0 || Boolean.TRUE.equals(cmd.getForceClose())) {
                        return Mono.<Object>just(RESULT_BALANCE_VALIDATED);
                    }
                    return Mono.error(new AccountNotEmptyException(cmd.getAccountId(), residual));
                });
    }

    @SagaStep(id = STEP_SETTLE, compensate = COMPENSATE_REVERSE_SETTLEMENT, dependsOn = STEP_VALIDATE_BALANCE)
    @StepEvent(type = EVENT_SETTLED)
    public Mono<Object> settleOutstanding(@CorrelationId String sagaId, ExecutionContext ctx) {
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal residual = ctx.getVariableAs(CTX_RESIDUAL_AMOUNT, BigDecimal.class);
        BigDecimal feesOwed = ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class);
        UUID destinationAccountId = ctx.getVariableAs(CTX_DESTINATION_ACCOUNT_ID, UUID.class);

        boolean hasFees = feesOwed != null && feesOwed.signum() > 0;
        BigDecimal residualSafe = residual == null ? BigDecimal.ZERO : residual;
        BigDecimal afterFees = hasFees ? residualSafe.subtract(feesOwed) : residualSafe;
        boolean hasResidualToMove = afterFees.signum() > 0 && destinationAccountId != null;

        if (!hasFees && !hasResidualToMove) {
            log.info("Skipping settlement — no fees and no residual to move sagaId={}", sagaId);
            return Mono.just(RESULT_SKIPPED);
        }

        Mono<Void> feeTx = hasFees
                ? postFeeSettlement(sagaId, accountId, feesOwed, currency, ctx).then()
                : Mono.empty();
        Mono<Void> residualTx = hasResidualToMove
                ? postResidualTransfer(sagaId, accountId, destinationAccountId, afterFees, currency, ctx).then()
                : Mono.empty();

        return feeTx.then(residualTx).thenReturn(RESULT_SETTLED);
    }

    private Mono<PostLedgerTransactionResult> postFeeSettlement(String sagaId, UUID accountId,
                                                                BigDecimal feesOwed, String currency,
                                                                ExecutionContext ctx) {
        UUID feeIncomeGl = ledgerGlProperties.getFeeIncomeAccountId();

        TransactionLineFeeDTO feeLine = new TransactionLineFeeDTO();
        feeLine.setFeeType(FEE_TYPE_CLOSURE);
        feeLine.setFeeDescription(DESC_SETTLE_FEES);
        feeLine.setFeeReference(sagaId);
        feeLine.setFeeCalculationMethod(FEE_CALC_METHOD_FIXED);
        feeLine.setFeeFixedAmount(feesOwed);
        feeLine.setFeeCurrency(currency);
        feeLine.setFeeWaived(false);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand feeCmd = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + SETTLE_FEES_TX_KEY)
                .transactionType(TX_TYPE_FEE)
                .totalAmount(feesOwed)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_SETTLE_FEES)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_DEBIT).amount(feesOwed).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(feeIncomeGl).legType(LEG_CREDIT).amount(feesOwed).currency(currency).build()))
                .lineType(LINE_TYPE_FEE)
                .lineDto(feeLine)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(feeCmd)
                .doOnNext(result -> ctx.putVariable(CTX_SETTLE_FEE_TX_ID, result.getTransactionId()));
    }

    private Mono<PostLedgerTransactionResult> postResidualTransfer(String sagaId, UUID accountId,
                                                                   UUID destinationAccountId,
                                                                   BigDecimal residual, String currency,
                                                                   ExecutionContext ctx) {
        TransactionLineTransferDTO transferLine = new TransactionLineTransferDTO();
        transferLine.setTransferSourceAccountId(accountId);
        transferLine.setTransferDestinationAccountId(destinationAccountId);
        transferLine.setTransferPurpose("ACCOUNT_CLOSURE_RESIDUAL");
        transferLine.setTransferReference(sagaId);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand transferCmd = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + SETTLE_RESIDUAL_TX_KEY)
                .transactionType(TX_TYPE_TRANSFER)
                .totalAmount(residual)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_SETTLE_RESIDUAL)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_DEBIT).amount(residual).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(destinationAccountId).legType(LEG_CREDIT).amount(residual).currency(currency).build()))
                .lineType(LINE_TYPE_TRANSFER)
                .lineDto(transferLine)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(transferCmd)
                .doOnNext(result -> ctx.putVariable(CTX_SETTLE_RESIDUAL_TX_ID, result.getTransactionId()));
    }

    public Mono<Void> reverseSettlement(Object ignored, @CorrelationId String sagaId, ExecutionContext ctx) {
        UUID feeTxId = (UUID) ctx.getVariable(CTX_SETTLE_FEE_TX_ID);
        UUID residualTxId = (UUID) ctx.getVariable(CTX_SETTLE_RESIDUAL_TX_ID);

        Mono<Void> reverseFee = feeTxId == null
                ? Mono.empty()
                : reverseFeeTx(sagaId, ctx, feeTxId).then();
        Mono<Void> reverseResidual = residualTxId == null
                ? Mono.empty()
                : reverseResidualTx(sagaId, ctx, residualTxId).then();

        return reverseResidual.then(reverseFee);
    }

    private Mono<PostLedgerTransactionResult> reverseFeeTx(String sagaId, ExecutionContext ctx, UUID feeTxId) {
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal feesOwed = ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class);
        UUID feeIncomeGl = ledgerGlProperties.getFeeIncomeAccountId();

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":REVERSE_" + SETTLE_FEES_TX_KEY)
                .transactionType(TX_TYPE_FEE)
                .totalAmount(feesOwed)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_REVERSE_FEES)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(feeIncomeGl).legType(LEG_DEBIT).amount(feesOwed).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_CREDIT).amount(feesOwed).currency(currency).build()))
                .relatedTransactionId(feeTxId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();
        return commandBus.send(reversal);
    }

    private Mono<PostLedgerTransactionResult> reverseResidualTx(String sagaId, ExecutionContext ctx, UUID residualTxId) {
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal residual = ctx.getVariableAs(CTX_RESIDUAL_AMOUNT, BigDecimal.class);
        BigDecimal feesOwed = ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class);
        UUID destinationAccountId = ctx.getVariableAs(CTX_DESTINATION_ACCOUNT_ID, UUID.class);
        BigDecimal afterFees = (feesOwed == null ? residual : residual.subtract(feesOwed));

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":REVERSE_" + SETTLE_RESIDUAL_TX_KEY)
                .transactionType(TX_TYPE_TRANSFER)
                .totalAmount(afterFees)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_REVERSE_RESIDUAL)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(destinationAccountId).legType(LEG_DEBIT).amount(afterFees).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_CREDIT).amount(afterFees).currency(currency).build()))
                .relatedTransactionId(residualTxId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();
        return commandBus.send(reversal);
    }

    @SagaStep(id = STEP_REMOVE_SPACES, compensate = COMPENSATE_RECREATE_SPACES, dependsOn = STEP_SETTLE)
    @StepEvent(type = EVENT_SPACES_REMOVED)
    public Mono<Object> removeSpaces(ExecutionContext ctx) {
        ctx.putVariable(CTX_SPACES_BACKUP, List.of());
        // TODO: Call AccountSpacesApi to delete non-MAIN spaces, then MAIN, once the SDK endpoint is wired.
        return Mono.just(RESULT_SKIPPED);
    }

    public Mono<Void> recreateSpaces(ExecutionContext ctx) {
        List<?> spacesBackup = (List<?>) ctx.getVariable(CTX_SPACES_BACKUP);
        if (spacesBackup == null || spacesBackup.isEmpty()) {
            return Mono.empty();
        }
        // TODO: Recreate spaces from backup once the SDK endpoint is wired.
        return Mono.empty();
    }

    @SagaStep(id = STEP_UPDATE_STATUS, compensate = COMPENSATE_REVERT_STATUS, dependsOn = STEP_REMOVE_SPACES)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<Object> updateAccountStatus(ExecutionContext ctx) {
        ctx.putVariable(CTX_PREVIOUS_STATUS, "OPEN");
        // TODO: Update account status to CLOSED and record in history via SDK once wired.
        return Mono.just(RESULT_SKIPPED);
    }

    public Mono<Void> revertAccountStatus(ExecutionContext ctx) {
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_STATUS);
        if (previousStatus == null) {
            return Mono.empty();
        }
        // TODO: Revert to previous status via SDK once wired.
        return Mono.empty();
    }

    @SagaStep(id = STEP_TERMINATE_CONTRACT, compensate = COMPENSATE_REACTIVATE_CONTRACT, dependsOn = STEP_UPDATE_STATUS)
    @StepEvent(type = EVENT_CONTRACT_TERMINATED)
    public Mono<Void> terminateContract(ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        if (contractId == null) {
            // TODO: Look up contractId from account via SDK once wired.
            contractId = UUID.randomUUID();
            ctx.putVariable(CTX_CONTRACT_ID, contractId);
        }
        return commandBus.<Void>send(TerminateContractCommand.builder()
                .contractId(contractId)
                .build());
    }

    public Mono<Void> reactivateContract(ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        if (contractId == null) {
            return Mono.empty();
        }
        // TODO: Reactivate contract via SDK once wired.
        return Mono.empty();
    }
}
