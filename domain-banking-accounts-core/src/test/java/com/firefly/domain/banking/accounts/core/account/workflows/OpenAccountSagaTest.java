package com.firefly.domain.banking.accounts.core.account.workflows;

import com.firefly.domain.banking.accounts.core.account.commands.PostOpeningLedgerInput;
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

import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_ACCOUNT_CURRENCY;
import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_ACCOUNT_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_INITIAL_AMOUNT;
import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_OPENING_TX_ID;
import static com.firefly.domain.banking.accounts.core.account.utils.OpenAccountConstants.RESULT_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountSaga — postOpeningLedger / reverseOpeningLedger Tests")
class OpenAccountSagaTest {

    @Mock
    private CommandBus commandBus;
    @Mock
    private ExecutionContext ctx;

    private LedgerGlProperties ledgerGlProperties;
    private OpenAccountSaga saga;

    private static final UUID GL_OPENING_SUSPENSE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SAGA_ID = "saga-abc-123";

    @BeforeEach
    void setUp() {
        ledgerGlProperties = new LedgerGlProperties();
        ledgerGlProperties.setOpeningSuspenseAccountId(GL_OPENING_SUSPENSE);
        saga = new OpenAccountSaga(commandBus, ledgerGlProperties);
    }

    @Test
    @DisplayName("initialAmount > 0 → dispatches PostLedgerTransactionCommand with balanced DEBIT/CREDIT legs")
    void postOpeningLedger_happyPath() {
        UUID accountId = UUID.randomUUID();
        UUID newTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(newTxId).build()));

        PostOpeningLedgerInput input = PostOpeningLedgerInput.builder().initialAmount(amount).build();

        StepVerifier.create(saga.postOpeningLedger(input, SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(newTxId))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":postOpeningLedger");
        assertThat(dispatched.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(dispatched.getTotalAmount()).isEqualByComparingTo(amount);
        assertThat(dispatched.getCurrency()).isEqualTo("EUR");
        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(GL_OPENING_SUSPENSE);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
        assertThat(dispatched.getInitialStatus()).isEqualTo("POSTED");

        verify(ctx).putVariable(CTX_INITIAL_AMOUNT, amount);
        verify(ctx).putVariable(CTX_OPENING_TX_ID, newTxId);
    }

    @Test
    @DisplayName("initialAmount == 0 → returns \"skipped\" sentinel and does NOT call commandBus")
    void postOpeningLedger_zeroAmountSkipped() {
        PostOpeningLedgerInput input = PostOpeningLedgerInput.builder().initialAmount(BigDecimal.ZERO).build();

        StepVerifier.create(saga.postOpeningLedger(input, SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("initialAmount == null → returns \"skipped\" sentinel and does NOT call commandBus")
    void postOpeningLedger_nullAmountSkipped() {
        PostOpeningLedgerInput input = PostOpeningLedgerInput.builder().initialAmount(null).build();

        StepVerifier.create(saga.postOpeningLedger(input, SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("reverseOpeningLedger with existing tx id → dispatches REVERSAL with relatedTransactionId")
    void reverseOpeningLedger_dispatchesReversal() {
        UUID originalTxId = UUID.randomUUID();
        UUID reversalTxId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        when(ctx.getVariable(CTX_OPENING_TX_ID)).thenReturn(originalTxId);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(reversalTxId).build()));

        StepVerifier.create(saga.reverseOpeningLedger(null, SAGA_ID, ctx)).verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getRelatedTransactionId()).isEqualTo(originalTxId);
        assertThat(dispatched.getRelationType()).isEqualTo("REVERSAL");
        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(GL_OPENING_SUSPENSE);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("reverseOpeningLedger without opening tx id (step skipped) → no-op, never dispatches")
    void reverseOpeningLedger_noTxIdNoOp() {
        when(ctx.getVariable(CTX_OPENING_TX_ID)).thenReturn(null);

        StepVerifier.create(saga.reverseOpeningLedger(null, SAGA_ID, ctx)).verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }
}
