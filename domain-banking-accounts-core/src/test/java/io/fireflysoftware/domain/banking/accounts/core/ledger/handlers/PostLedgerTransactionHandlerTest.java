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


package io.fireflysoftware.domain.banking.accounts.core.ledger.handlers;

import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineDepositApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineFeeApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineInterestApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineTransferApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionStatusHistoryApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLegDTO;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLineDepositDTO;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.LedgerLegSpec;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand;
import io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionResult;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostLedgerTransactionHandler Tests")
class PostLedgerTransactionHandlerTest {

    @Mock
    private TransactionsApi transactionsApi;
    @Mock
    private TransactionLegsApi transactionLegsApi;
    @Mock
    private TransactionStatusHistoryApi transactionStatusHistoryApi;
    @Mock
    private TransactionLineDepositApi transactionLineDepositApi;
    @Mock
    private TransactionLineFeeApi transactionLineFeeApi;
    @Mock
    private TransactionLineInterestApi transactionLineInterestApi;
    @Mock
    private TransactionLineTransferApi transactionLineTransferApi;

    private PostLedgerTransactionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PostLedgerTransactionHandler(
                transactionsApi,
                transactionLegsApi,
                transactionStatusHistoryApi,
                transactionLineDepositApi,
                transactionLineFeeApi,
                transactionLineInterestApi,
                transactionLineTransferApi);
    }

    @Test
    @DisplayName("Happy path: deposit with 2 balanced legs + deposit line → POSTED")
    void happyPath_depositWithTwoLegs() {
        UUID accountId = UUID.randomUUID();
        UUID glId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        TransactionLineDepositDTO depositLine = new TransactionLineDepositDTO();
        PostLedgerTransactionCommand cmd = PostLedgerTransactionCommand.builder()
                .externalReference("saga-1:STEP_POST_OPENING_LEDGER")
                .transactionType("DEPOSIT")
                .totalAmount(amount)
                .currency("EUR")
                .valueDate(LocalDateTime.now())
                .bookingDate(LocalDateTime.now())
                .description("Account opening deposit")
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType("DEBIT").amount(amount).currency("EUR").build(),
                        LedgerLegSpec.builder().accountId(glId).legType("CREDIT").amount(amount).currency("EUR").build()))
                .lineType("DEPOSIT")
                .lineDto(depositLine)
                .initialStatus("POSTED")
                .build();

        when(transactionsApi.createTransaction(any(TransactionDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionDTO(null, null, txId, null)));
        when(transactionLegsApi.createTransactionLeg(eq(txId), any(TransactionLegDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionLegDTO()));
        when(transactionLineDepositApi.createDepositLine(eq(txId), any(TransactionLineDepositDTO.class), anyString()))
                .thenReturn(Mono.just(depositLine));
        when(transactionsApi.updateTransactionStatus(eq(txId), eq("POSTED"), anyString(), anyString()))
                .thenReturn(Mono.just(new TransactionDTO(null, null, txId, null)));

        StepVerifier.create(handler.handle(cmd))
                .assertNext(result -> assertThat(result.getTransactionId()).isEqualTo(txId))
                .verifyComplete();

        verify(transactionsApi).createTransaction(any(TransactionDTO.class), anyString());
        verify(transactionLegsApi, times(2)).createTransactionLeg(eq(txId), any(TransactionLegDTO.class), anyString());
        verify(transactionLineDepositApi).createDepositLine(eq(txId), any(TransactionLineDepositDTO.class), anyString());
        verify(transactionsApi).updateTransactionStatus(eq(txId), eq("POSTED"), anyString(), anyString());
    }

    @Test
    @DisplayName("Unbalanced legs: Σ DEBIT ≠ Σ CREDIT → IllegalArgumentException, no SDK call")
    void unbalancedLegs_failsBeforeCreation() {
        PostLedgerTransactionCommand cmd = PostLedgerTransactionCommand.builder()
                .externalReference("saga-2:STEP_UNBALANCED")
                .transactionType("DEPOSIT")
                .totalAmount(new BigDecimal("50.00"))
                .currency("EUR")
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(UUID.randomUUID()).legType("DEBIT")
                                .amount(new BigDecimal("50.00")).currency("EUR").build(),
                        LedgerLegSpec.builder().accountId(UUID.randomUUID()).legType("CREDIT")
                                .amount(new BigDecimal("40.00")).currency("EUR").build()))
                .initialStatus("POSTED")
                .build();

        StepVerifier.create(handler.handle(cmd))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(IllegalArgumentException.class);
                    assertThat(err.getMessage()).contains("unbalanced legs");
                })
                .verify();

        verifyNoInteractions(transactionsApi, transactionLegsApi,
                transactionLineDepositApi, transactionLineFeeApi,
                transactionLineInterestApi, transactionLineTransferApi);
    }

    @Test
    @DisplayName("Duplicate externalReference: ledger error propagates")
    void duplicateExternalReference_errorPropagates() {
        UUID accountId = UUID.randomUUID();
        UUID glId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        PostLedgerTransactionCommand cmd = PostLedgerTransactionCommand.builder()
                .externalReference("saga-3:STEP_DUP")
                .transactionType("DEPOSIT")
                .totalAmount(amount)
                .currency("EUR")
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType("DEBIT").amount(amount).currency("EUR").build(),
                        LedgerLegSpec.builder().accountId(glId).legType("CREDIT").amount(amount).currency("EUR").build()))
                .initialStatus("POSTED")
                .build();

        when(transactionsApi.createTransaction(any(TransactionDTO.class), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("duplicate externalReference")));

        StepVerifier.create(handler.handle(cmd))
                .expectErrorMatches(err -> err instanceof IllegalStateException
                        && err.getMessage().contains("duplicate"))
                .verify();

        verify(transactionLegsApi, never()).createTransactionLeg(any(), any(), anyString());
    }

    @Test
    @DisplayName("Reversal: carries relatedTransactionId and relationType into ledger DTO")
    void reversal_propagatesRelationFields() {
        UUID originalTxId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID glId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        PostLedgerTransactionCommand cmd = PostLedgerTransactionCommand.builder()
                .externalReference("saga-4:STEP_REVERSE")
                .transactionType("DEPOSIT")
                .totalAmount(amount)
                .currency("EUR")
                .description("Reversal of opening deposit")
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(glId).legType("DEBIT").amount(amount).currency("EUR").build(),
                        LedgerLegSpec.builder().accountId(accountId).legType("CREDIT").amount(amount).currency("EUR").build()))
                .relatedTransactionId(originalTxId)
                .relationType("REVERSAL")
                .initialStatus("POSTED")
                .build();

        when(transactionsApi.createTransaction(any(TransactionDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionDTO(null, null, txId, null)));
        when(transactionLegsApi.createTransactionLeg(eq(txId), any(TransactionLegDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionLegDTO()));
        when(transactionsApi.updateTransactionStatus(eq(txId), eq("POSTED"), anyString(), anyString()))
                .thenReturn(Mono.just(new TransactionDTO(null, null, txId, null)));

        ArgumentCaptor<TransactionDTO> dtoCaptor = ArgumentCaptor.forClass(TransactionDTO.class);

        StepVerifier.create(handler.handle(cmd))
                .assertNext(result -> assertThat(result.getTransactionId()).isEqualTo(txId))
                .verifyComplete();

        verify(transactionsApi).createTransaction(dtoCaptor.capture(), anyString());
        TransactionDTO sent = dtoCaptor.getValue();
        assertThat(sent.getRelatedTransactionId()).isEqualTo(originalTxId);
        assertThat(sent.getRelationType()).isEqualTo("REVERSAL");
    }

    @Test
    @DisplayName("PENDING initial status skips the POSTED status transition (hold case)")
    void pendingInitialStatus_skipsStatusTransition() {
        UUID accountId = UUID.randomUUID();
        UUID glId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250.00");

        PostLedgerTransactionCommand cmd = PostLedgerTransactionCommand.builder()
                .externalReference("saga-5:STEP_HOLD")
                .transactionType("TRANSFER")
                .totalAmount(amount)
                .currency("EUR")
                .legs(List.of(
                        LedgerLegSpec.builder().accountId(accountId).legType("DEBIT").amount(amount).currency("EUR").build(),
                        LedgerLegSpec.builder().accountId(glId).legType("CREDIT").amount(amount).currency("EUR").build()))
                .initialStatus("PENDING")
                .build();

        when(transactionsApi.createTransaction(any(TransactionDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionDTO(null, null, txId, null)));
        when(transactionLegsApi.createTransactionLeg(eq(txId), any(TransactionLegDTO.class), anyString()))
                .thenReturn(Mono.just(new TransactionLegDTO()));

        StepVerifier.create(handler.handle(cmd))
                .assertNext(result -> assertThat(result.getTransactionId()).isEqualTo(txId))
                .verifyComplete();

        verify(transactionsApi, never()).updateTransactionStatus(any(), anyString(), anyString(), anyString());
    }
}
