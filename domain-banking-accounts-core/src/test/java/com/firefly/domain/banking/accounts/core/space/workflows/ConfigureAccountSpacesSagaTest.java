package com.firefly.domain.banking.accounts.core.space.workflows;

import com.firefly.core.banking.ledger.sdk.model.TransactionLineTransferDTO;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import com.firefly.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
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

import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_ACCOUNT_CURRENCY;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_ACCOUNT_ID;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_INITIAL_AMOUNT;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SOURCE_SPACE_ID;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SPACE_ID;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SPACE_INIT_TX_ID;
import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.RESULT_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigureAccountSpacesSaga — initializeSpaceBalance / reverseSpaceInit Tests")
class ConfigureAccountSpacesSagaTest {

    @Mock
    private CommandBus commandBus;
    @Mock
    private ExecutionContext ctx;

    private ConfigureAccountSpacesSaga saga;

    private static final String SAGA_ID = "saga-spaces-abc-123";

    @BeforeEach
    void setUp() {
        saga = new ConfigureAccountSpacesSaga(commandBus);
    }

    @Test
    @DisplayName("initialAmount > 0 → dispatches TRANSFER with DEBIT source-space / CREDIT new-space on same accountId")
    void initializeSpaceBalance_happyPath() {
        UUID accountId = UUID.randomUUID();
        UUID sourceSpaceId = UUID.randomUUID();
        UUID newSpaceId = UUID.randomUUID();
        UUID newTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250.00");

        when(ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_SOURCE_SPACE_ID, UUID.class)).thenReturn(sourceSpaceId);
        when(ctx.getVariableAs(CTX_SPACE_ID, UUID.class)).thenReturn(newSpaceId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(newTxId).build()));

        StepVerifier.create(saga.initializeSpaceBalance(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(newTxId))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":initializeSpaceBalance");
        assertThat(dispatched.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(dispatched.getTotalAmount()).isEqualByComparingTo(amount);
        assertThat(dispatched.getCurrency()).isEqualTo("EUR");
        assertThat(dispatched.getInitialStatus()).isEqualTo("POSTED");

        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(0).getAccountSpaceId()).isEqualTo(sourceSpaceId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(1).getAccountSpaceId()).isEqualTo(newSpaceId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");

        assertThat(dispatched.getLineType()).isEqualTo("TRANSFER");
        assertThat(dispatched.getLineDto()).isInstanceOf(TransactionLineTransferDTO.class);
        TransactionLineTransferDTO transferLine = (TransactionLineTransferDTO) dispatched.getLineDto();
        assertThat(transferLine.getTransferPurpose()).isEqualTo("SPACE_INIT");
        assertThat(transferLine.getTransferReference()).isEqualTo(SAGA_ID);

        verify(ctx).putVariable(CTX_SPACE_INIT_TX_ID, newTxId);
    }

    @Test
    @DisplayName("initialAmount == 0 → returns \"skipped\" sentinel and does NOT dispatch")
    void initializeSpaceBalance_zeroAmountSkipped() {
        when(ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class)).thenReturn(BigDecimal.ZERO);

        StepVerifier.create(saga.initializeSpaceBalance(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("initialAmount == null → returns \"skipped\" sentinel and does NOT dispatch")
    void initializeSpaceBalance_nullAmountSkipped() {
        when(ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class)).thenReturn(null);

        StepVerifier.create(saga.initializeSpaceBalance(SAGA_ID, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("reverseSpaceInit with existing tx id → dispatches REVERSAL with relatedTransactionId and swapped legs")
    void reverseSpaceInit_dispatchesReversal() {
        UUID originalTxId = UUID.randomUUID();
        UUID reversalTxId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sourceSpaceId = UUID.randomUUID();
        UUID newSpaceId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250.00");

        when(ctx.getVariable(CTX_SPACE_INIT_TX_ID)).thenReturn(originalTxId);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_SOURCE_SPACE_ID, UUID.class)).thenReturn(sourceSpaceId);
        when(ctx.getVariableAs(CTX_SPACE_ID, UUID.class)).thenReturn(newSpaceId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class)).thenReturn(amount);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(reversalTxId).build()));

        StepVerifier.create(saga.reverseSpaceInit(null, SAGA_ID, ctx)).verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getRelatedTransactionId()).isEqualTo(originalTxId);
        assertThat(dispatched.getRelationType()).isEqualTo("REVERSAL");
        assertThat(dispatched.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(dispatched.getInitialStatus()).isEqualTo("POSTED");

        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountSpaceId()).isEqualTo(newSpaceId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountSpaceId()).isEqualTo(sourceSpaceId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("reverseSpaceInit without init tx id (step skipped) → no-op, never dispatches")
    void reverseSpaceInit_noTxIdNoOp() {
        when(ctx.getVariable(CTX_SPACE_INIT_TX_ID)).thenReturn(null);

        StepVerifier.create(saga.reverseSpaceInit(null, SAGA_ID, ctx)).verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }
}
