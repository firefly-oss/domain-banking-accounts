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


package io.fireflysoftware.domain.banking.accounts.core.space.workflows;

import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineTransferDTO;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
import io.fireflysoftware.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
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

import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.COMPENSATE_DELETE_SPACE;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.COMPENSATE_REMOVE_AUTO_TRANSFER;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.COMPENSATE_REVERSE_SPACE_INIT;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_ACCOUNT_CURRENCY;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_ACCOUNT_ID;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_INITIAL_AMOUNT;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SOURCE_SPACE_ID;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SPACE_ID;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.CTX_SPACE_INIT_TX_ID;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.EVENT_AUTO_TRANSFER_CONFIGURED;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.EVENT_SPACE_CREATED;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.EVENT_SPACE_INIT_POSTED;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.RESULT_SKIPPED;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.SAGA_CONFIGURE_SPACES_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.STEP_CONFIG_AUTO_TRANSFER;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.STEP_CREATE_SPACE;
import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.STEP_INIT_SPACE_BALANCE;

/**
 * Saga orchestrator for creating a new account space (sub-bucket) and seeding its balance from
 * the source space via a ledger-first intra-account transfer.
 *
 * <p>The saga no longer couples to {@code AccountBalancesApi} — the {@code account_balance}
 * projection row (if still required by downstream UI) is rebuilt by a projection consumer
 * off the ledger event stream, not by this saga. Every monetary effect is expressed as a
 * balanced double-entry ledger transaction so the ledger remains the single source of truth.
 *
 * <ol>
 *   <li>{@code createSpace} — allocates the {@code account_space} row (metadata only).</li>
 *   <li>{@code initializeSpaceBalance} — posts a {@code TRANSFER} ledger tx with two legs
 *       on the SAME {@code accountId}: DEBIT on the source space, CREDIT on the new space.
 *       Skipped when {@code initialAmount} is null or zero.</li>
 *   <li>{@code configureAutoTransfer} — stores optional recurring-transfer configuration.
 *       Execution of the recurring transfers is out of scope of this saga.</li>
 * </ol>
 */
@Slf4j
@Saga(name = SAGA_CONFIGURE_SPACES_NAME)
@Service
public class ConfigureAccountSpacesSaga {

    private static final String LEG_DEBIT = "DEBIT";
    private static final String LEG_CREDIT = "CREDIT";
    private static final String TX_TYPE_TRANSFER = "TRANSFER";
    private static final String LINE_TYPE_TRANSFER = "TRANSFER";
    private static final String STATUS_POSTED = "POSTED";
    private static final String RELATION_REVERSAL = "REVERSAL";
    private static final String TRANSFER_PURPOSE_SPACE_INIT = "SPACE_INIT";
    private static final String DESC_SPACE_INIT = "Account space initialization transfer";
    private static final String DESC_SPACE_INIT_REVERSAL = "Reversal of account space initialization transfer";

    private final CommandBus commandBus;

    public ConfigureAccountSpacesSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_CREATE_SPACE, compensate = COMPENSATE_DELETE_SPACE)
    @StepEvent(type = EVENT_SPACE_CREATED)
    public Mono<UUID> createSpace(CreateAccountSpaceCommand cmd, ExecutionContext ctx) {
        // NOTE: ExecutionContext.variables is a ConcurrentHashMap — it rejects null values
        // with an unhelpful NPE (error=null in orchestration logs). Guard every put.
        if (cmd.getAccountId() != null) ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        if (cmd.getSourceSpaceId() != null) ctx.putVariable(CTX_SOURCE_SPACE_ID, cmd.getSourceSpaceId());
        if (cmd.getCurrency() != null) ctx.putVariable(CTX_ACCOUNT_CURRENCY, cmd.getCurrency());
        if (cmd.getInitialAmount() != null) ctx.putVariable(CTX_INITIAL_AMOUNT, cmd.getInitialAmount());
        // TODO: Call AccountSpacesApi.createSpace once the core-banking-accounts SDK endpoint is wired.
        return Mono.just(UUID.randomUUID())
                .doOnNext(spaceId -> ctx.putVariable(CTX_SPACE_ID, spaceId));
    }

    public Mono<Void> deleteSpace(UUID spaceId, ExecutionContext ctx) {
        // TODO: Call AccountSpacesApi.deleteSpace once the core-banking-accounts SDK endpoint is wired.
        return Mono.empty();
    }

    @SagaStep(id = STEP_INIT_SPACE_BALANCE, compensate = COMPENSATE_REVERSE_SPACE_INIT, dependsOn = STEP_CREATE_SPACE)
    @StepEvent(type = EVENT_SPACE_INIT_POSTED)
    public Mono<Object> initializeSpaceBalance(@CorrelationId String sagaId, ExecutionContext ctx) {
        BigDecimal initialAmount = ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class);
        if (initialAmount == null || initialAmount.signum() == 0) {
            log.info("Skipping space init ledger post — no initial amount sagaId={}", sagaId);
            return Mono.just(RESULT_SKIPPED);
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        UUID sourceSpaceId = ctx.getVariableAs(CTX_SOURCE_SPACE_ID, UUID.class);
        UUID newSpaceId = ctx.getVariableAs(CTX_SPACE_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);

        TransactionLineTransferDTO transferLine = new TransactionLineTransferDTO();
        transferLine.setTransferPurpose(TRANSFER_PURPOSE_SPACE_INIT);
        transferLine.setTransferReference(sagaId);
        transferLine.setTransferSourceAccountId(accountId);
        transferLine.setTransferDestinationAccountId(accountId);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand ledgerCmd = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + STEP_INIT_SPACE_BALANCE)
                .transactionType(TX_TYPE_TRANSFER)
                .totalAmount(initialAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_SPACE_INIT)
                .legs(List.of(
                        LedgerLegSpec.builder()
                                .accountId(accountId).accountSpaceId(sourceSpaceId)
                                .legType(LEG_DEBIT).amount(initialAmount).currency(currency).build(),
                        LedgerLegSpec.builder()
                                .accountId(accountId).accountSpaceId(newSpaceId)
                                .legType(LEG_CREDIT).amount(initialAmount).currency(currency).build()))
                .lineType(LINE_TYPE_TRANSFER)
                .lineDto(transferLine)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(ledgerCmd)
                .doOnNext(result -> ctx.putVariable(CTX_SPACE_INIT_TX_ID, result.getTransactionId()))
                .map(PostLedgerTransactionResult::getTransactionId)
                .map(id -> (Object) id);
    }

    public Mono<Void> reverseSpaceInit(Object ignored, @CorrelationId String sagaId, ExecutionContext ctx) {
        UUID txId = (UUID) ctx.getVariable(CTX_SPACE_INIT_TX_ID);
        if (txId == null) {
            return Mono.empty();
        }
        UUID accountId = ctx.getVariableAs(CTX_ACCOUNT_ID, UUID.class);
        UUID sourceSpaceId = ctx.getVariableAs(CTX_SOURCE_SPACE_ID, UUID.class);
        UUID newSpaceId = ctx.getVariableAs(CTX_SPACE_ID, UUID.class);
        String currency = ctx.getVariableAs(CTX_ACCOUNT_CURRENCY, String.class);
        BigDecimal initialAmount = ctx.getVariableAs(CTX_INITIAL_AMOUNT, BigDecimal.class);

        LocalDateTime now = LocalDateTime.now();
        PostLedgerTransactionCommand reversal = PostLedgerTransactionCommand.builder()
                .externalReference(sagaId + ":" + COMPENSATE_REVERSE_SPACE_INIT)
                .transactionType(TX_TYPE_TRANSFER)
                .totalAmount(initialAmount)
                .currency(currency)
                .valueDate(now)
                .bookingDate(now)
                .description(DESC_SPACE_INIT_REVERSAL)
                .legs(List.of(
                        LedgerLegSpec.builder()
                                .accountId(accountId).accountSpaceId(newSpaceId)
                                .legType(LEG_DEBIT).amount(initialAmount).currency(currency).build(),
                        LedgerLegSpec.builder()
                                .accountId(accountId).accountSpaceId(sourceSpaceId)
                                .legType(LEG_CREDIT).amount(initialAmount).currency(currency).build()))
                .relatedTransactionId(txId)
                .relationType(RELATION_REVERSAL)
                .initialStatus(STATUS_POSTED)
                .build();

        return commandBus.<PostLedgerTransactionResult>send(reversal)
                .doOnSuccess(r -> log.info("Reversed space-init ledger tx sagaId={} originalTxId={} reversalTxId={}",
                        sagaId, txId, r == null ? null : r.getTransactionId()))
                .then();
    }

    @SagaStep(id = STEP_CONFIG_AUTO_TRANSFER, compensate = COMPENSATE_REMOVE_AUTO_TRANSFER, dependsOn = STEP_CREATE_SPACE)
    @StepEvent(type = EVENT_AUTO_TRANSFER_CONFIGURED)
    public Mono<Object> configureAutoTransfer(CreateAccountSpaceCommand cmd, ExecutionContext ctx) {
        if (cmd == null || cmd.getAutoTransferRules() == null) {
            return Mono.just(RESULT_SKIPPED);
        }
        // TODO: Call AccountSpacesApi.updateSpace with auto-transfer configuration once the SDK endpoint is wired.
        return Mono.just(RESULT_SKIPPED);
    }

    public Mono<Void> removeAutoTransferConfig(ExecutionContext ctx) {
        return Mono.empty();
    }
}
