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


package io.fireflysoftware.domain.banking.accounts.core.restriction.workflows;

import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineFeeDTO;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.infra.properties.LedgerGlProperties;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.command.CommandBus;
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
import java.util.Set;
import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.COMPENSATE_REMOVE_RESTRICTION;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.COMPENSATE_REVERSE_HOLD;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.COMPENSATE_REVERT_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_HOLD_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_PREVIOUS_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_RESTRICTED_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_RESTRICTION_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_RESTRICTION_TYPE;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.EVENT_HOLDER_NOTIFIED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.EVENT_HOLD_POSTED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.EVENT_RESTRICTION_CREATED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.EVENT_STATUS_UPDATED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.MONETARY_RESTRICTIONS;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.RESULT_SKIPPED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.SAGA_APPLY_RESTRICTION_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_CREATE_RESTRICTION;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_NOTIFY;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_POST_LEDGER_HOLD;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_UPDATE_STATUS;

/**
 * Saga orchestrator for applying an account restriction.
 *
 * <p>The {@code account_restriction} row remains the authoritative metadata of the hold
 * (WHO applied, WHY, audit trail, reference number). The restricted AMOUNT, however, is
 * expressed in the ledger as a PENDING double-entry transaction — DEBIT on the customer
 * account and CREDIT on the {@code holds_clearing} GL account — so that available balance
 * is reduced without touching the posted balance.
 *
 * <ol>
 *   <li>{@code createRestriction} — writes the {@code account_restriction} metadata row.</li>
 *   <li>{@code postLedgerHold} — posts the PENDING ledger transaction for monetary restrictions.
 *       Non-monetary or zero-amount restrictions skip this step.</li>
 *   <li>{@code updateAccountStatus} — for status-changing restrictions (freeze / legal / fraud),
 *       captures previous status so compensation can revert.</li>
 *   <li>{@code notifyHolder} — sends the holder notification.</li>
 * </ol>
 */
@Slf4j
@Saga(name = SAGA_APPLY_RESTRICTION_NAME)
@Service
public class ApplyAccountRestrictionSaga {

    private static final Set<String> STATUS_CHANGING_RESTRICTIONS = Set.of(
            "ACCOUNT_FREEZE", "LEGAL_ORDER", "FRAUD_INVESTIGATION"
    );

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String TX_TYPE_FEE = "FEE";
    private static final String LINE_TYPE_FEE = "FEE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_POSTED = "POSTED";
    private static final String RELATION_REVERSAL = "REVERSAL";
    private static final String FEE_TYPE_HOLD = "HOLD";
    private static final String FEE_CALC_METHOD_FIXED = "FIXED";
    private static final String DESC_HOLD_POSTED = "Account restriction hold posted";
    private static final String DESC_HOLD_REVERSED = "Reversal of account restriction hold";

    private final CommandBus commandBus;
    private final LedgerGlProperties ledgerGlProperties;

    public ApplyAccountRestrictionSaga(CommandBus commandBus, LedgerGlProperties ledgerGlProperties) {
        this.commandBus = commandBus;
        this.ledgerGlProperties = ledgerGlProperties;
    }

    @SagaStep(id = STEP_CREATE_RESTRICTION, compensate = COMPENSATE_REMOVE_RESTRICTION)
    @StepEvent(type = EVENT_RESTRICTION_CREATED)
    public Mono<UUID> createRestriction(ApplyRestrictionCommand cmd, ExecutionContext ctx) {
        // NOTE: ExecutionContext.variables is a ConcurrentHashMap — it rejects null values
        // with an unhelpful NPE (error=null in orchestration logs). Guard every put.
        if (cmd.getAccountId() != null) ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        if (cmd.getRestrictionType() != null) ctx.putVariable(CTX_RESTRICTION_TYPE, cmd.getRestrictionType());
        if (cmd.getRestrictedAmount() != null) ctx.putVariable(CTX_RESTRICTED_AMOUNT, cmd.getRestrictedAmount());
        if (cmd.getCurrency() != null) ctx.putVariable(CTX_ACCOUNT_CURRENCY, cmd.getCurrency());
        // TODO: Call AccountRestrictionsApi.createRestriction once the core-banking-accounts SDK endpoint is wired.
        return Mono.just(UUID.randomUUID())
                .doOnNext(restrictionId -> ctx.putVariable(CTX_RESTRICTION_ID, restrictionId));
    }

    public Mono<Void> removeRestriction(UUID restrictionId, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<Void>send(RemoveRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionId(restrictionId)
                .build());
    }

    @SagaStep(id = STEP_POST_LEDGER_HOLD, compensate = COMPENSATE_REVERSE_HOLD, dependsOn = STEP_CREATE_RESTRICTION)
    @StepEvent(type = EVENT_HOLD_POSTED)
    public Mono<Object> postLedgerHold(@CorrelationId String sagaId, ExecutionContext ctx) {
        String restrictionType = ctx.getVariableAs(CTX_RESTRICTION_TYPE, String.class);
        BigDecimal restrictedAmount = ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class);
        if (restrictionType == null || !MONETARY_RESTRICTIONS.contains(restrictionType)
                || restrictedAmount == null || restrictedAmount.signum() == 0) {
            log.info("Skipping ledger hold — non-monetary or zero-amount restriction sagaId={} type={}",
                    sagaId, restrictionType);
            return Mono.just(RESULT_SKIPPED);
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        UUID holdsClearing = ledgerGlProperties.getHoldsClearingAccountId();

        TransactionLineFeeDTO feeLine = new TransactionLineFeeDTO();
        feeLine.setFeeType(FEE_TYPE_HOLD);
        feeLine.setFeeDescription(DESC_HOLD_POSTED);
        feeLine.setFeeReference(sagaId);
        feeLine.setFeeCalculationMethod(FEE_CALC_METHOD_FIXED);
        feeLine.setFeeFixedAmount(restrictedAmount);
        feeLine.setFeeCurrency(currency);
        feeLine.setFeeWaived(false);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand ledgerCmd = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + STEP_POST_LEDGER_HOLD)
                .transactionType(TX_TYPE_FEE)
                .totalAmount(restrictedAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_HOLD_POSTED)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_DEBIT).amount(restrictedAmount).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(holdsClearing).legType(LEG_CREDIT).amount(restrictedAmount).currency(currency).build()))
                .lineType(LINE_TYPE_FEE)
                .lineDto(feeLine)
                .initialStatus(STATUS_PENDING)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(ledgerCmd)
                .doOnNext(result -> ctx.putVariable(CTX_HOLD_TX_ID, result.getTransactionId()))
                .map(PostLedgerTransactionResult::getTransactionId)
                .map(id -> (Object) id);
    }

    public Mono<Void> reverseLedgerHold(Object ignored, @CorrelationId String sagaId, ExecutionContext ctx) {
        UUID txId = (UUID) ctx.getVariable(CTX_HOLD_TX_ID);
        if (txId == null) {
            return Mono.empty();
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal restrictedAmount = ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class);
        UUID holdsClearing = ledgerGlProperties.getHoldsClearingAccountId();

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + COMPENSATE_REVERSE_HOLD)
                .transactionType(TX_TYPE_FEE)
                .totalAmount(restrictedAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_HOLD_REVERSED)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(holdsClearing).legType(LEG_DEBIT).amount(restrictedAmount).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_CREDIT).amount(restrictedAmount).currency(currency).build()))
                .relatedTransactionId(txId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(reversal)
                .doOnSuccess(r -> log.info("Reversed hold ledger tx sagaId={} originalTxId={} reversalTxId={}",
                        sagaId, txId, r == null ? null : r.getTransactionId()))
                .then();
    }

    @SagaStep(id = STEP_UPDATE_STATUS, compensate = COMPENSATE_REVERT_STATUS, dependsOn = STEP_CREATE_RESTRICTION)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<Object> updateAccountStatus(ExecutionContext ctx) {
        String restrictionType = ctx.getVariableAs(CTX_RESTRICTION_TYPE, String.class);
        if (restrictionType == null || !STATUS_CHANGING_RESTRICTIONS.contains(restrictionType)) {
            return Mono.just(RESULT_SKIPPED);
        }
        // TODO: Store previous status and update to SUSPENDED via AccountStatusHistoryApi once wired.
        ctx.putVariable(CTX_PREVIOUS_STATUS, "OPEN");
        return Mono.just(RESULT_SKIPPED);
    }

    public Mono<Void> revertAccountStatus(ExecutionContext ctx) {
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_STATUS);
        if (previousStatus == null) {
            return Mono.empty();
        }
        // TODO: Revert to previous status via AccountStatusHistoryApi once wired.
        return Mono.empty();
    }

    @SagaStep(id = STEP_NOTIFY, dependsOn = STEP_CREATE_RESTRICTION)
    @StepEvent(type = EVENT_HOLDER_NOTIFIED)
    public Mono<Object> notifyHolder(ExecutionContext ctx) {
        // TODO: Call AccountNotificationsApi.createNotification once wired.
        return Mono.just(RESULT_SKIPPED);
    }
}
