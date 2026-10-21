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


package io.fireflysoftware.domain.banking.accounts.core.account.workflows;

import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountSpaceDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineFeeDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineTransferDTO;
import io.fireflysoftware.core.contract.sdk.model.ContractDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.ApplyContractStatusCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.GetContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RecordStatusCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RemoveAccountStatusHistoryCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.UpdateAccountStatusCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.exceptions.AccountNotEmptyException;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountQuery;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.GetAccountContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.domain.banking.accounts.core.ledger.queries.GetAccountResidualBalanceQuery;
import io.fireflysoftware.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
import io.fireflysoftware.domain.banking.accounts.core.space.commands.RecreateAccountSpaceCommand;
import io.fireflysoftware.domain.banking.accounts.core.space.commands.RemoveAccountSpaceCommand;
import io.fireflysoftware.domain.banking.accounts.core.space.queries.GetAccountSpacesQuery;
import io.fireflysoftware.domain.banking.accounts.infra.properties.LedgerGlProperties;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.argument.CorrelationId;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REACTIVATE_CONTRACT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_RECREATE_SPACES;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REVERSE_SETTLEMENT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.COMPENSATE_REVERT_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_CONTRACT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_DESTINATION_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_FEES_OWED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_FORCE_CLOSE;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_PREVIOUS_CONTRACT_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_RESIDUAL_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_FEE_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_RESIDUAL_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SPACES_BACKUP;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_STATUS_HISTORY_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_BALANCE_VALIDATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_CONTRACT_TERMINATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_SETTLED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_SPACES_REMOVED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_BALANCE_VALIDATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SETTLED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SKIPPED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.SAGA_CLOSE_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.SETTLE_FEES_TX_KEY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.SETTLE_RESIDUAL_TX_KEY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_REMOVE_SPACES;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_SETTLE;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_TERMINATE_CONTRACT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_UPDATE_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.STEP_VALIDATE_BALANCE;

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

    private static final String CLOSED = "CLOSED";

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
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        return queryBus.<PaginationResponseAccountSpaceDTO>query(GetAccountSpacesQuery.builder().accountId(accountId).build())
                .flatMap(page -> {
                    List<AccountSpaceDTO> spaces = new ArrayList<>(
                            page == null || page.getContent() == null ? List.of() : page.getContent());
                    spaces.sort(Comparator.comparing(space -> space.getSpaceType() == AccountSpaceDTO.SpaceTypeEnum.MAIN));
                    ctx.putVariable(CTX_SPACES_BACKUP, spaces);
                    List<UUID> deleted = new ArrayList<>();
                    return Flux.fromIterable(spaces)
                            .concatMap(space -> commandBus.<Void>send(new RemoveAccountSpaceCommand(space.getAccountSpaceId()))
                                    .doOnSuccess(ignored -> deleted.add(space.getAccountSpaceId())))
                            .then(Mono.just((Object) spaces.size()))
                            .onErrorResume(error -> restoreSpaces(spaces.stream()
                                            .filter(space -> deleted.contains(space.getAccountSpaceId()))
                                            .toList())
                                    .then(Mono.error(error)));
                });
    }

    public Mono<Void> recreateSpaces(ExecutionContext ctx) {
        Object backup = ctx.getVariable(CTX_SPACES_BACKUP);
        if (!(backup instanceof List<?> spaces) || spaces.isEmpty()) {
            return Mono.empty();
        }
        List<AccountSpaceDTO> typed = spaces.stream()
                .filter(AccountSpaceDTO.class::isInstance)
                .map(AccountSpaceDTO.class::cast)
                .toList();
        return restoreSpaces(typed);
    }

    private Mono<Void> restoreSpaces(List<AccountSpaceDTO> spaces) {
        if (spaces == null || spaces.isEmpty()) {
            return Mono.empty();
        }
        List<AccountSpaceDTO> ordered = new ArrayList<>(spaces);
        ordered.sort(Comparator.comparing(space -> space.getSpaceType() != AccountSpaceDTO.SpaceTypeEnum.MAIN));
        return Flux.fromIterable(ordered)
                .concatMap(space -> commandBus.<UUID>send(new RecreateAccountSpaceCommand(space)))
                .then();
    }

    @SagaStep(id = STEP_UPDATE_STATUS, compensate = COMPENSATE_REVERT_STATUS, dependsOn = STEP_REMOVE_SPACES)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<Object> updateAccountStatus(ExecutionContext ctx) {
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        return queryBus.<AccountDTO>query(GetAccountQuery.builder().accountId(accountId).build())
                .flatMap(account -> {
                    String previous = account == null || account.getAccountStatus() == null
                            ? "OPEN"
                            : account.getAccountStatus().getValue();
                    ctx.putVariable(CTX_PREVIOUS_STATUS, previous);
                    if (account != null && account.getContractId() != null) {
                        ctx.putVariable(CTX_CONTRACT_ID, account.getContractId());
                    }
                    return commandBus.<AccountDTO>send(new UpdateAccountStatusCommand(accountId, CLOSED, "Account closed"))
                            .then(commandBus.<UUID>send(RecordStatusCommand.builder()
                                    .accountId(accountId)
                                    .status(CLOSED)
                                    .reason("Account closed")
                                    .build()))
                            .doOnNext(historyId -> ctx.putVariable(CTX_STATUS_HISTORY_ID, historyId))
                            .map(historyId -> (Object) historyId);
                });
    }

    public Mono<Void> revertAccountStatus(ExecutionContext ctx) {
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_STATUS);
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        if (previousStatus == null || accountId == null) {
            return Mono.empty();
        }
        UUID historyId = (UUID) ctx.getVariable(CTX_STATUS_HISTORY_ID);
        Mono<Void> removeHistory = historyId == null
                ? Mono.empty()
                : commandBus.send(new RemoveAccountStatusHistoryCommand(accountId, historyId));
        return removeHistory.then(commandBus.send(
                new UpdateAccountStatusCommand(accountId, previousStatus, "Account closure rolled back")).then());
    }

    @SagaStep(id = STEP_TERMINATE_CONTRACT, compensate = COMPENSATE_REACTIVATE_CONTRACT, dependsOn = STEP_UPDATE_STATUS)
    @StepEvent(type = EVENT_CONTRACT_TERMINATED)
    public Mono<ContractDTO> terminateContract(ExecutionContext ctx) {
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        UUID knownContractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        Mono<UUID> contractId = knownContractId != null
                ? Mono.just(knownContractId)
                : commandBus.<UUID>send(GetAccountContractCommand.builder().accountId(accountId).build());
        return contractId
                .switchIfEmpty(Mono.error(new IllegalStateException("Account has no contract to terminate")))
                .flatMap(id -> {
                    ctx.putVariable(CTX_CONTRACT_ID, id);
                    return commandBus.<ContractDTO>send(new GetContractCommand(id));
                })
                .flatMap(contract -> {
                    if (contract.getContractStatus() == null) {
                        return Mono.error(new IllegalStateException("Contract has no current status"));
                    }
                    ctx.putVariable(CTX_PREVIOUS_CONTRACT_STATUS, contract.getContractStatus().getValue());
                    contract.setContractStatus(ContractDTO.ContractStatusEnum.TERMINATED);
                    contract.setEndDate(LocalDateTime.now());
                    return commandBus.<ContractDTO>send(new ApplyContractStatusCommand(contract.getContractId(), contract));
                });
    }

    public Mono<Void> reactivateContract(ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_CONTRACT_STATUS);
        if (contractId == null || previousStatus == null) {
            return Mono.empty();
        }
        return commandBus.<ContractDTO>send(new GetContractCommand(contractId))
                .flatMap(contract -> {
                    contract.setContractStatus(ContractDTO.ContractStatusEnum.fromValue(previousStatus));
                    contract.setEndDate(null);
                    return commandBus.send(new ApplyContractStatusCommand(contractId, contract)).then();
                });
    }
}
