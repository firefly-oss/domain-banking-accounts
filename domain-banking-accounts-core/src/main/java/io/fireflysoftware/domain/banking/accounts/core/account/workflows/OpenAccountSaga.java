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

import io.fireflysoftware.domain.banking.accounts.core.account.commands.AddPartyToContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateMainSpaceCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.PostOpeningLedgerInput;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RecordStatusCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RemovePartyFromContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.SetParameterCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.TerminateContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineDepositDTO;
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

import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_CLOSE_ACCOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_DELETE_PARAMS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_DELETE_SPACE;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_REMOVE_PARTY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_REVERSE_OPENING_LEDGER;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.COMPENSATE_TERMINATE_CONTRACT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_ACCOUNT_TYPE;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_CONTRACT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_INITIAL_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_MAIN_SPACE_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_OPENING_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_PARTY_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.CTX_STATUS_ENTRY_ID;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_ACCOUNT_CREATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_CONTRACT_CREATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_OPENING_LEDGER_POSTED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_PARAMS_SET;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_PARTY_ADDED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_SPACE_CREATED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.EVENT_STATUS_RECORDED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.RESULT_SKIPPED;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.SAGA_OPEN_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_ADD_PARTY;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_CREATE_ACCOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_CREATE_CONTRACT;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_CREATE_SPACE;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_POST_OPENING_LEDGER;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_RECORD_STATUS;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_SET_PARAMS;

/**
 * Saga orchestrator for opening a new bank account.
 *
 * This saga coordinates the multi-step process of creating an account:
 * <ol>
 *   <li>Create a contract for the account relationship</li>
 *   <li>Add the primary holder as a party to the contract</li>
 *   <li>Create the account record linked to the contract (also populates the execution context
 *       with {@code accountId}, {@code accountCurrency} and {@code accountType} for downstream
 *       steps)</li>
 *   <li>Post the opening deposit to the core-banking-ledger as a double-entry transaction —
 *       this is the authoritative record of the money; {@code account_balance} is only a
 *       projection. If no initial amount is provided, this step is a no-op.</li>
 *   <li>Create the main space (bucket) for the account</li>
 *   <li>Record the initial status in history</li>
 *   <li>Set default parameters (fees, limits, rates)</li>
 * </ol>
 *
 * Each step that mutates remote state has a compensation method. The compensation for the
 * opening-ledger step posts a REVERSAL transaction rather than deleting rows.
 */
@Slf4j
@Saga(name = SAGA_OPEN_ACCOUNT_NAME)
@Service
public class OpenAccountSaga {

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String TX_TYPE_DEPOSIT = "DEPOSIT";
    private static final String LINE_TYPE_DEPOSIT = "DEPOSIT";
    private static final String STATUS_POSTED = "POSTED";
    private static final String RELATION_REVERSAL = "REVERSAL";
    private static final String DEPOSIT_METHOD_ACCOUNT_OPENING = "ACCOUNT_OPENING";
    private static final String DESC_OPENING_DEPOSIT = "Account opening deposit";
    private static final String DESC_OPENING_REVERSAL = "Reversal of account opening deposit";

    private final CommandBus commandBus;
    private final LedgerGlProperties ledgerGlProperties;

    public OpenAccountSaga(CommandBus commandBus, LedgerGlProperties ledgerGlProperties) {
        this.commandBus = commandBus;
        this.ledgerGlProperties = ledgerGlProperties;
    }

    @SagaStep(id = STEP_CREATE_CONTRACT, compensate = COMPENSATE_TERMINATE_CONTRACT)
    @StepEvent(type = EVENT_CONTRACT_CREATED)
    public Mono<UUID> createContract(CreateContractCommand cmd, ExecutionContext ctx) {
        return commandBus.<UUID>send(cmd)
                .doOnNext(contractId -> ctx.putVariable(CTX_CONTRACT_ID, contractId));
    }

    public Mono<Void> terminateContract(UUID contractId) {
        return commandBus.<Void>send(TerminateContractCommand.builder()
                .contractId(contractId)
                .build());
    }

    @SagaStep(id = STEP_ADD_PARTY, compensate = COMPENSATE_REMOVE_PARTY, dependsOn = STEP_CREATE_CONTRACT)
    @StepEvent(type = EVENT_PARTY_ADDED)
    public Mono<UUID> addPartyToContract(AddPartyToContractCommand cmd, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        // ExecutionContext.variables is a ConcurrentHashMap — guard against null value NPE.
        if (cmd.getPartyId() != null) ctx.putVariable(CTX_PARTY_ID, cmd.getPartyId());
        return commandBus.<UUID>send(cmd.withContractId(contractId));
    }

    public Mono<Void> removePartyFromContract(UUID partyLinkId, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        UUID partyId = (UUID) ctx.getVariable(CTX_PARTY_ID);
        return commandBus.<Void>send(RemovePartyFromContractCommand.builder()
                .contractId(contractId)
                .partyId(partyId)
                .build());
    }

    @SagaStep(id = STEP_CREATE_ACCOUNT, compensate = COMPENSATE_CLOSE_ACCOUNT, dependsOn = STEP_ADD_PARTY)
    @StepEvent(type = EVENT_ACCOUNT_CREATED)
    public Mono<UUID> createAccount(CreateAccountCommand cmd, ExecutionContext ctx) {
        UUID contractId = (UUID) ctx.getVariable(CTX_CONTRACT_ID);
        // ExecutionContext.variables is a ConcurrentHashMap — guard against null value NPE.
        if (cmd.getCurrency() != null) ctx.putVariable(CTX_ACCOUNT_CURRENCY, cmd.getCurrency());
        if (cmd.getAccountType() != null) ctx.putVariable(CTX_ACCOUNT_TYPE, cmd.getAccountType());
        return commandBus.<UUID>send(cmd.withContractId(contractId))
                .doOnNext(accountId -> ctx.putVariable(CTX_ACCOUNT_ID, accountId));
    }

    public Mono<Void> closeAccount(UUID accountId) {
        return commandBus.<Void>send(CloseAccountCommand.builder()
                .accountId(accountId)
                .reason("Compensation: account opening failed")
                .build());
    }

    @SagaStep(id = STEP_POST_OPENING_LEDGER, compensate = COMPENSATE_REVERSE_OPENING_LEDGER, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_OPENING_LEDGER_POSTED)
    public Mono<Object> postOpeningLedger(PostOpeningLedgerInput input, @CorrelationId String sagaId, ExecutionContext ctx) {
        BigDecimal initialAmount = input == null ? null : input.getInitialAmount();
        if (initialAmount == null || initialAmount.signum() == 0) {
            log.info("Skipping opening-ledger post — no initial amount sagaId={}", sagaId);
            return Mono.just(RESULT_SKIPPED);
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        UUID glAccountId = ledgerGlProperties.getOpeningSuspenseAccountId();
        ctx.putVariable(CTX_INITIAL_AMOUNT, initialAmount);

        TransactionLineDepositDTO depositLine = new TransactionLineDepositDTO();
        depositLine.setDepositMethod(DEPOSIT_METHOD_ACCOUNT_OPENING);
        depositLine.setDepositReference(sagaId);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand ledgerCmd = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + STEP_POST_OPENING_LEDGER)
                .transactionType(TX_TYPE_DEPOSIT)
                .totalAmount(initialAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_OPENING_DEPOSIT)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_DEBIT).amount(initialAmount).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(glAccountId).legType(LEG_CREDIT).amount(initialAmount).currency(currency).build()))
                .lineType(LINE_TYPE_DEPOSIT)
                .lineDto(depositLine)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(ledgerCmd)
                .doOnNext(result -> ctx.putVariable(CTX_OPENING_TX_ID, result.getTransactionId()))
                .map(PostLedgerTransactionResult::getTransactionId)
                .map(id -> (Object) id);
    }

    public Mono<Void> reverseOpeningLedger(Object ignored, @CorrelationId String sagaId, ExecutionContext ctx) {
        UUID txId = (UUID) ctx.getVariable(CTX_OPENING_TX_ID);
        if (txId == null) {
            return Mono.empty();
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal initialAmount = ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class);
        UUID glAccountId = ledgerGlProperties.getOpeningSuspenseAccountId();

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + COMPENSATE_REVERSE_OPENING_LEDGER)
                .transactionType(TX_TYPE_DEPOSIT)
                .totalAmount(initialAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_OPENING_REVERSAL)
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(glAccountId).legType(LEG_DEBIT).amount(initialAmount).currency(currency).build(),
                        LedgerLegSpec.builder().accountId(accountId).legType(LEG_CREDIT).amount(initialAmount).currency(currency).build()))
                .relatedTransactionId(txId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(reversal)
                .doOnSuccess(r -> log.info("Reversed opening ledger tx sagaId={} originalTxId={} reversalTxId={}",
                        sagaId, txId, r == null ? null : r.getTransactionId()))
                .then();
    }

    @SagaStep(id = STEP_CREATE_SPACE, compensate = COMPENSATE_DELETE_SPACE, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_SPACE_CREATED)
    public Mono<UUID> createMainSpace(CreateMainSpaceCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .doOnNext(spaceId -> ctx.putVariable(CTX_MAIN_SPACE_ID, spaceId));
    }

    public Mono<Void> deleteSpace(UUID spaceId, ExecutionContext ctx) {
        return Mono.empty();
    }

    @SagaStep(id = STEP_RECORD_STATUS, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_STATUS_RECORDED)
    public Mono<UUID> recordStatusHistory(RecordStatusCommand cmd, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .doOnNext(statusEntryId -> ctx.putVariable(CTX_STATUS_ENTRY_ID, statusEntryId));
    }

    @SagaStep(id = STEP_SET_PARAMS, compensate = COMPENSATE_DELETE_PARAMS, dependsOn = STEP_CREATE_ACCOUNT)
    @StepEvent(type = EVENT_PARAMS_SET)
    public Mono<Object> setDefaultParameters(SetParameterCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            // @SagaStep must emit exactly one element — Mono.empty() would be treated as FAILURE.
            return Mono.just(RESULT_SKIPPED);
        }
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<UUID>send(cmd.withAccountId(accountId))
                .map(id -> (Object) id);
    }

    public Mono<Void> deleteParameters(Object parameterId, ExecutionContext ctx) {
        return Mono.empty();
    }
}
