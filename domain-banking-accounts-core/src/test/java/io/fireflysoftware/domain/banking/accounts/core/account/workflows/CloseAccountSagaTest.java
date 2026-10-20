package io.fireflysoftware.domain.banking.accounts.core.account.workflows;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.exceptions.AccountNotEmptyException;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.domain.banking.accounts.core.ledger.queries.GetAccountResidualBalanceQuery;
import io.fireflysoftware.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
import io.fireflysoftware.domain.banking.accounts.infra.properties.LedgerGlProperties;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
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
import java.util.List;
import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_DESTINATION_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_FEES_OWED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_RESIDUAL_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_FEE_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.CTX_SETTLE_RESIDUAL_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_BALANCE_VALIDATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SETTLED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.RESULT_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseAccountSaga — validateZeroBalance / settleOutstanding / reverseSettlement Tests")
class CloseAccountSagaTest {

    @Mock
    private CommandBus commandBus;
    @Mock
    private QueryBus queryBus;
    @Mock
    private ExecutionContext ctx;

    private LedgerGlProperties ledgerGlProperties;
    private CloseAccountSaga saga;

    private static final UUID GL_FEE_INCOME = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final String SAGA_ID = "saga-close-abc-1";

    @BeforeEach
    void setUp() {
        ledgerGlProperties = new LedgerGlProperties();
        ledgerGlProperties.setFeeIncomeAccountId(GL_FEE_INCOME);
        saga = new CloseAccountSaga(commandBus, queryBus, ledgerGlProperties);
    }

    @Test
    @DisplayName("validateZeroBalance with zero residual → validated result, no error")
    void validateZeroBalance_zeroResidualPasses() {
        UUID accountId = UUID.randomUUID();
        CloseAccountCommand cmd = CloseAccountCommand.builder()
                .accountId(accountId).reason("closed").currency("EUR").build();

        when(queryBus.<AccountResidualBalanceResult>query(any(GetAccountResidualBalanceQuery.class)))
                .thenReturn(Mono.just(AccountResidualBalanceResult.builder().residual(BigDecimal.ZERO).build()));

        StepVerifier.create(saga.validateZeroBalance(cmd, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_BALANCE_VALIDATED))
                .verifyComplete();

        verify(ctx).putVariable(CTX_RESIDUAL_AMOUNT, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("validateZeroBalance with non-zero residual and forceClose=false → AccountNotEmptyException")
    void validateZeroBalance_nonZeroWithoutForceFails() {
        UUID accountId = UUID.randomUUID();
        BigDecimal residual = new BigDecimal("10.00");
        CloseAccountCommand cmd = CloseAccountCommand.builder()
                .accountId(accountId).reason("closed").currency("EUR").forceClose(false).build();

        when(queryBus.<AccountResidualBalanceResult>query(any(GetAccountResidualBalanceQuery.class)))
                .thenReturn(Mono.just(AccountResidualBalanceResult.builder().residual(residual).build()));

        StepVerifier.create(saga.validateZeroBalance(cmd, ctx))
                .expectErrorMatches(err -> err instanceof AccountNotEmptyException
                        && ((AccountNotEmptyException) err).getResidual().compareTo(residual) == 0)
                .verify();
    }

    @Test
    @DisplayName("validateZeroBalance with non-zero residual and forceClose=true → validated result, proceeds")
    void validateZeroBalance_nonZeroWithForcePasses() {
        UUID accountId = UUID.randomUUID();
        BigDecimal residual = new BigDecimal("50.00");
        CloseAccountCommand cmd = CloseAccountCommand.builder()
                .accountId(accountId).reason("force").currency("EUR").forceClose(true).build();

        when(queryBus.<AccountResidualBalanceResult>query(any(GetAccountResidualBalanceQuery.class)))
                .thenReturn(Mono.just(AccountResidualBalanceResult.builder().residual(residual).build()));

        StepVerifier.create(saga.validateZeroBalance(cmd, ctx))
                .assertNext(result -> assertThat(result).isEqualTo(RESULT_BALANCE_VALIDATED))
                .verifyComplete();

        verify(ctx).putVariable(CTX_RESIDUAL_AMOUNT, residual);
    }

    @Test
    @DisplayName("settleOutstanding with no fees and no residual → skipped")
    void settleOutstanding_noWorkSkipped() {
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(UUID.randomUUID());
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_RESIDUAL_AMOUNT, BigDecimal.class)).thenReturn(BigDecimal.ZERO);
        when(ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class)).thenReturn(null);
        when(ctx.getVariableAs(CTX_DESTINATION_ACCOUNT_ID, UUID.class)).thenReturn(null);

        StepVerifier.create(saga.settleOutstanding(SAGA_ID, ctx))
                .assertNext(r -> assertThat(r).isEqualTo(RESULT_SKIPPED))
                .verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("settleOutstanding with fees only → posts ACCOUNT_CLOSURE_FEE tx (DEBIT account / CREDIT fee_income)")
    void settleOutstanding_feesOnly() {
        UUID accountId = UUID.randomUUID();
        UUID feeTxId = UUID.randomUUID();
        BigDecimal fees = new BigDecimal("15.00");

        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_RESIDUAL_AMOUNT, BigDecimal.class)).thenReturn(BigDecimal.ZERO);
        when(ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class)).thenReturn(fees);
        when(ctx.getVariableAs(CTX_DESTINATION_ACCOUNT_ID, UUID.class)).thenReturn(null);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(feeTxId).build()));

        StepVerifier.create(saga.settleOutstanding(SAGA_ID, ctx))
                .assertNext(r -> assertThat(r).isEqualTo(RESULT_SETTLED))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":STEP_SETTLE_FEES");
        assertThat(dispatched.getTransactionType()).isEqualTo("FEE");
        assertThat(dispatched.getTotalAmount()).isEqualByComparingTo(fees);
        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(GL_FEE_INCOME);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");

        verify(ctx).putVariable(CTX_SETTLE_FEE_TX_ID, feeTxId);
    }

    @Test
    @DisplayName("settleOutstanding with residual and destination (forceClose path) → posts TRANSFER tx")
    void settleOutstanding_residualOnly() {
        UUID accountId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        UUID residualTxId = UUID.randomUUID();
        BigDecimal residual = new BigDecimal("100.00");

        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_RESIDUAL_AMOUNT, BigDecimal.class)).thenReturn(residual);
        when(ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class)).thenReturn(null);
        when(ctx.getVariableAs(CTX_DESTINATION_ACCOUNT_ID, UUID.class)).thenReturn(destinationId);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(residualTxId).build()));

        StepVerifier.create(saga.settleOutstanding(SAGA_ID, ctx))
                .assertNext(r -> assertThat(r).isEqualTo(RESULT_SETTLED))
                .verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        PostLedgerTransactionCommand dispatched = cmdCaptor.getValue();

        assertThat(dispatched.getExternalReference()).isEqualTo(SAGA_ID + ":STEP_SETTLE_RESIDUAL");
        assertThat(dispatched.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(dispatched.getTotalAmount()).isEqualByComparingTo(residual);
        assertThat(dispatched.getLegs()).hasSize(2);
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(destinationId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");

        verify(ctx).putVariable(CTX_SETTLE_RESIDUAL_TX_ID, residualTxId);
    }

    @Test
    @DisplayName("reverseSettlement with no tx ids → no-op, never dispatches")
    void reverseSettlement_noTxIdsNoOp() {
        when(ctx.getVariable(CTX_SETTLE_FEE_TX_ID)).thenReturn(null);
        when(ctx.getVariable(CTX_SETTLE_RESIDUAL_TX_ID)).thenReturn(null);

        StepVerifier.create(saga.reverseSettlement(null, SAGA_ID, ctx)).verifyComplete();

        verify(commandBus, never()).send(any(PostLedgerTransactionCommand.class));
    }

    @Test
    @DisplayName("reverseSettlement with fee tx id → dispatches REVERSAL")
    void reverseSettlement_reversesFee() {
        UUID accountId = UUID.randomUUID();
        UUID feeTxId = UUID.randomUUID();
        UUID reversalTxId = UUID.randomUUID();
        BigDecimal fees = new BigDecimal("15.00");

        when(ctx.getVariable(CTX_SETTLE_FEE_TX_ID)).thenReturn(feeTxId);
        when(ctx.getVariable(CTX_SETTLE_RESIDUAL_TX_ID)).thenReturn(null);
        when(ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class)).thenReturn(accountId);
        when(ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class)).thenReturn("EUR");
        when(ctx.getVariableAs(CTX_FEES_OWED, BigDecimal.class)).thenReturn(fees);
        when(commandBus.<PostLedgerTransactionResult>send(any(PostLedgerTransactionCommand.class)))
                .thenReturn(Mono.just(PostLedgerTransactionResult.builder().transactionId(reversalTxId).build()));

        StepVerifier.create(saga.reverseSettlement(null, SAGA_ID, ctx)).verifyComplete();

        ArgumentCaptor<PostLedgerTransactionCommand> cmdCaptor = ArgumentCaptor.forClass(PostLedgerTransactionCommand.class);
        verify(commandBus).send(cmdCaptor.capture());
        List<PostLedgerTransactionCommand> reversals = cmdCaptor.getAllValues();
        assertThat(reversals).hasSize(1);
        PostLedgerTransactionCommand dispatched = reversals.get(0);
        assertThat(dispatched.getRelatedTransactionId()).isEqualTo(feeTxId);
        assertThat(dispatched.getRelationType()).isEqualTo("REVERSAL");
        assertThat(dispatched.getLegs().get(0).getAccountId()).isEqualTo(GL_FEE_INCOME);
        assertThat(dispatched.getLegs().get(0).getLegType()).isEqualTo("DEBIT");
        assertThat(dispatched.getLegs().get(1).getAccountId()).isEqualTo(accountId);
        assertThat(dispatched.getLegs().get(1).getLegType()).isEqualTo("CREDIT");
    }
}
