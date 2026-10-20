package io.fireflysoftware.domain.banking.accounts.core.restriction.workflows;

import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
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
import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.COMPENSATE_REACTIVATE_RESTRICTION;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_HOLD_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ORIGINAL_HOLD_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_RESTRICTION_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.EVENT_HOLD_REVERSED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.EVENT_RESTRICTION_INACTIVATED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.RESULT_SKIPPED;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.SAGA_REMOVE_RESTRICTION_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.STEP_MARK_RESTRICTION_INACTIVE;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.STEP_REVERSE_LEDGER_HOLD;

/**
 * Saga orchestrator for removing an account restriction. Counterpart of
 * {@link ApplyAccountRestrictionSaga}.
 *
 * <p>Two steps:
 * <ol>
 *   <li>{@code markRestrictionInactive} — flips the {@code is_active} flag on the
 *       {@code account_restriction} row (metadata).</li>
 *   <li>{@code reverseLedgerHold} — if the original apply saga placed a monetary hold, posts a
 *       REVERSAL transaction pointing at the original hold tx id. Skipped for non-monetary
 *       restrictions (when the command carries no {@code originalHoldTxId}).</li>
 * </ol>
 *
 * <p>The caller passes the original hold transaction id explicitly via the command. Callers that
 * do not know it may look it up via {@code TransactionsApi.findByExternalReference} using
 * {@code originalSagaId + ":" + STEP_POST_LEDGER_HOLD}.
 */
@Slf4j
@Saga(name = SAGA_REMOVE_RESTRICTION_NAME)
@Service
public class RemoveAccountRestrictionSaga {

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String TX_TYPE_FEE = "FEE";
    private static final String STATUS_POSTED = "POSTED";
    private static final String RELATION_REVERSAL = "REVERSAL";
    private static final String DESC_HOLD_REVERSED = "Reversal of account restriction hold on removal";

    private final CommandBus commandBus;
    private final LedgerGlProperties ledgerGlProperties;

    public RemoveAccountRestrictionSaga(CommandBus commandBus, LedgerGlProperties ledgerGlProperties) {
        this.commandBus = commandBus;
        this.ledgerGlProperties = ledgerGlProperties;
    }

    @SagaStep(id = STEP_MARK_RESTRICTION_INACTIVE, compensate = COMPENSATE_REACTIVATE_RESTRICTION)
    @StepEvent(type = EVENT_RESTRICTION_INACTIVATED)
    public Mono<Object> markRestrictionInactive(RemoveRestrictionCommand cmd, ExecutionContext ctx) {
        // NOTE: ExecutionContext.variables is a ConcurrentHashMap — it rejects null values
        // with an unhelpful NPE (error=null in orchestration logs). Guard every put.
        if (cmd.getAccountId() != null) ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        if (cmd.getRestrictionId() != null) ctx.putVariable(CTX_RESTRICTION_ID, cmd.getRestrictionId());
        if (cmd.getOriginalHoldTxId() != null) ctx.putVariable(CTX_ORIGINAL_HOLD_TX_ID, cmd.getOriginalHoldTxId());
        if (cmd.getHoldAmount() != null) ctx.putVariable(CTX_HOLD_AMOUNT, cmd.getHoldAmount());
        if (cmd.getCurrency() != null) ctx.putVariable(CTX_ACCOUNT_CURRENCY, cmd.getCurrency());
        // TODO: Call AccountRestrictionsApi.updateRestriction with is_active=false once the SDK endpoint is wired.
        return Mono.just(RESULT_SKIPPED);
    }

    public Mono<Void> reactivateRestriction(ExecutionContext ctx) {
        // TODO: Call AccountRestrictionsApi.updateRestriction with is_active=true once the SDK endpoint is wired.
        return Mono.empty();
    }

    @SagaStep(id = STEP_REVERSE_LEDGER_HOLD, dependsOn = STEP_MARK_RESTRICTION_INACTIVE)
    @StepEvent(type = EVENT_HOLD_REVERSED)
    public Mono<Object> reverseLedgerHold(@CorrelationId String sagaId, ExecutionContext ctx) {
        UUID originalTxId = (UUID) ctx.getVariable(CTX_ORIGINAL_HOLD_TX_ID);
        if (originalTxId == null) {
            log.info("Skipping ledger hold reversal — non-monetary restriction sagaId={}", sagaId);
            return Mono.just(RESULT_SKIPPED);
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        BigDecimal holdAmount = ctx.getVariableAs(CTX_HOLD_AMOUNT, BigDecimal.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        UUID holdsClearing = ledgerGlProperties.getHoldsClearingAccountId();

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + STEP_REVERSE_LEDGER_HOLD)
                .transactionType(TX_TYPE_FEE)
                .totalAmount(holdAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_HOLD_REVERSED)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(holdsClearing).legType(LEG_DEBIT).amount(holdAmount).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_CREDIT).amount(holdAmount).currency(currency).build()))
                .relatedTransactionId(originalTxId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(reversal)
                .map(PostLedgerTransactionResult::getTransactionId)
                .map(id -> (Object) id);
    }
}
