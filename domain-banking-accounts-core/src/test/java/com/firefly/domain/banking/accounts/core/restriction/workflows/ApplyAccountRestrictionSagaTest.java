package com.firefly.domain.banking.accounts.core.restriction.workflows;

import com.firefly.core.banking.ledger.sdk.model.TransactionLineFeeDTO;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import com.firefly.domain.banking.accounts.infra.properties.LedgerGlProperties;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_ACCOUNT_CURRENCY;
import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_ACCOUNT_ID;
import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_HOLD_TX_ID;
import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_RESTRICTED_AMOUNT;
import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.CTX_RESTRICTION_TYPE;
import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.RESULT_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyAccountRestrictionSaga — postLedgerHold / reverseLedgerHold Tests")
class ApplyAccountRestrictionSagaTest {

    @Mock
    private CommandBus commandBus;
    @Mock
    private ExecutionContext ctx;

    private LedgerGlProperties ledgerGlProperties;
    private ApplyAccountRestrictionSaga saga;

    private static final UUID GL_HOLDS_CLEARING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String SAGA_ID = "saga-restrict-abc-1";

    @BeforeEach
    void setUp() {
        ledgerGlProperties = new LedgerGlProperties();
        ledgerGlProperties.setHoldsClearingAccountId(GL_HOLDS_CLEARING);
        saga = new ApplyAccountRestrictionSaga(commandBus, ledgerGlProperties);
    }

    @Test
    @DisplayName("monetary restriction with positive amount → dispatches PENDING FEE tx with DEBIT account / CREDIT holds_clearing")
    void postLedgerHold_happyPath() {
        UUID accountId = UUID.randomUUID();
        UUID newTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        when(ctx.getVariableAs(CTX_RESTRICTION_TYPE, String.class)).thenReturn("WITHDRAWAL_HOLD");
        when(ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(newTxId).build()));

        StepVerifier.create(saga.postLedgerHold(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(newTxId))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":postLedgerHold");
        assertThat(dispatched.getTransactionType()).isEqualTo("FEE");
        assertThat(dispatched.getInitialStatus()).isEqualTo("PENDING");
        assertThat(dispatched.getTotalAmount()).isEqualByComparingTo(amount);
        assertThat(dispatched.getCurrency()).isEqualTo("EUR");

        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(GL_HOLDS_CLEARING);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");

        assertThat(dispatched.getLineType()).isEqualTo("FEE");
        assertThat(dispatched.getLineDto()).isInstanceOf(TransactionLineFeeDTO.class);
        TransactionLineFeeDTO feeLine = (TransactionLineFeeDTO) dispatched.getLineDto();
        assertThat(feeLine.getFeeType()).isEqualTo("HOLD");
        assertThat(feeLine.getFeeFixedAmount()).isEqualByComparingTo(amount);

        verify(ctx).putVariable(CTX_HOLD_TX_ID, newTxId);
    }

    @Test
    @DisplayName("non-monetary restriction type → returns \"skipped\" and does NOT dispatch")
    void postLedgerHold_nonMonetarySkipped() {
        when(ctx.getVariableAs(CTX_RESTRICTION_TYPE, String.class)).thenReturn("DORMANCY");
        when(ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class)).thenReturn(new BigDecimal("100.00"));

        StepVerifier.create(saga.postLedgerHold(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("monetary type with zero amount → returns \"skipped\" and does NOT dispatch")
    void postLedgerHold_zeroAmountSkipped() {
        when(ctx.getVariableAs(CTX_RESTRICTION_TYPE, String.class)).thenReturn("WITHDRAWAL_HOLD");
        when(ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class)).thenReturn(BigDecimal.ZERO);

        StepVerifier.create(saga.postLedgerHold(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("reverseLedgerHold with existing tx id → dispatches REVERSAL with swapped legs")
    void reverseLedgerHold_dispatchesReversal() {
        UUID originalTxId = UUID.randomUUID();
        UUID reversalTxId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        when(ctx.getVariable(CTX_HOLD_TX_ID)).thenReturn(originalTxId);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_RESTRICTED_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(reversalTxId).build()));

        StepVerifier.create(saga.reverseLedgerHold(null, SAGA_ID, ctx)).verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getRelatedTransactionId()).isEqualTo(originalTxId);
        assertThat(dispatched.getRelationType()).isEqualTo("REVERSAL");
        assertThat(dispatched.getInitialStatus()).isEqualTo("POSTED");

        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(GL_HOLDS_CLEARING);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("reverseLedgerHold without hold tx id (step skipped) → no-op, never dispatches")
    void reverseLedgerHold_noTxIdNoOp() {
        when(ctx.getVariable(CTX_HOLD_TX_ID)).thenReturn(null);

        StepVerifier.create(saga.reverseLedgerHold(null, SAGA_ID, ctx)).verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }
}
