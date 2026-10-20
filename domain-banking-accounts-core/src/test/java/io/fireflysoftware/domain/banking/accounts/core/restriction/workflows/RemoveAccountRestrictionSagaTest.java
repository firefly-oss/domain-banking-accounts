package io.fireflysoftware.domain.banking.accounts.core.restriction.workflows;

import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.infra.properties.LedgerGlProperties;
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

import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_HOLD_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_ORIGINAL_HOLD_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.CTX_RESTRICTION_ID;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.RESULT_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveAccountRestrictionSaga Tests")
class RemoveAccountRestrictionSagaTest {

    @Mock
    private CommandBus commandBus;
    @Mock
    private ExecutionContext ctx;

    private LedgerGlProperties ledgerGlProperties;
    private RemoveAccountRestrictionSaga saga;

    private static final UUID GL_HOLDS_CLEARING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String SAGA_ID = "saga-remove-restrict-1";

    @BeforeEach
    void setUp() {
        ledgerGlProperties = new LedgerGlProperties();
        ledgerGlProperties.setHoldsClearingAccountId(GL_HOLDS_CLEARING);
        saga = new RemoveAccountRestrictionSaga(commandBus, ledgerGlProperties);
    }

    @Test
    @DisplayName("markRestrictionInactive stashes command fields into ctx and returns \"skipped\" sentinel")
    void markRestrictionInactive_stashesContext() {
        UUID accountId = UUID.randomUUID();
        UUID restrictionId = UUID.randomUUID();
        UUID originalHoldTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        RemoveRestrictionCommand cmd = RemoveRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionId(restrictionId)
                .originalHoldTxId(originalHoldTxId)
                .holdAmount(amount)
                .currency("EUR")
                .build();

        StepVerifier.create(saga.markRestrictionInactive(cmd, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(ctx).putVariable(CTX_ACCOUNT_ID, accountId);
        verify(ctx).putVariable(CTX_RESTRICTION_ID, restrictionId);
        verify(ctx).putVariable(CTX_ORIGINAL_HOLD_TX_ID, originalHoldTxId);
        verify(ctx).putVariable(CTX_HOLD_AMOUNT, amount);
        verify(ctx).putVariable(CTX_ACCOUNT_CURRENCY, "EUR");
    }

    @Test
    @DisplayName("reverseLedgerHold with originalHoldTxId → dispatches REVERSAL with swapped legs")
    void reverseLedgerHold_dispatchesReversal() {
        UUID originalTxId = UUID.randomUUID();
        UUID reversalTxId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        when(ctx.getVariable(CTX_ORIGINAL_HOLD_TX_ID)).thenReturn(originalTxId);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_HOLD_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(reversalTxId).build()));

        StepVerifier.create(saga.reverseLedgerHold(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(reversalTxId))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":reverseLedgerHold");
        assertThat(dispatched.getRelatedTransactionId()).isEqualTo(originalTxId);
        assertThat(dispatched.getRelationType()).isEqualTo("REVERSAL");
        assertThat(dispatched.getTransactionType()).isEqualTo("FEE");
        assertThat(dispatched.getInitialStatus()).isEqualTo("POSTED");

        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(GL_HOLDS_CLEARING);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("reverseLedgerHold without originalHoldTxId → returns \"skipped\" and does NOT dispatch")
    void reverseLedgerHold_nonMonetarySkipped() {
        when(ctx.getVariable(CTX_ORIGINAL_HOLD_TX_ID)).thenReturn(null);

        StepVerifier.create(saga.reverseLedgerHold(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }
}
