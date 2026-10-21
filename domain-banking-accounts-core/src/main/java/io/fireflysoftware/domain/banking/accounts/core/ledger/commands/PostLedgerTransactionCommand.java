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


package io.fireflysoftware.domain.banking.accounts.core.ledger.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reusable command that every domain saga uses to post a double-entry ledger transaction through
 * core-banking-ledger. Carries the transaction header, balanced legs, and an optional typed line
 * (deposit / fee / interest / transfer). Idempotency is driven by {@code externalReference};
 * the handler additionally passes a fresh {@code xIdempotencyKey} on every underlying SDK call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLedgerTransactionCommand implements Command<PostLedgerTransactionResult> {

    /** Used for idempotency — callers typically pass {@code sagaId + ":" + stepId}. */
    private String externalReference;

    /**
     * Maps to {@code TransactionDTO.TransactionTypeEnum} — the enum values actually accepted by the ledger
     * (DEPOSIT, WITHDRAWAL, TRANSFER, FEE, INTEREST, CARD, ACH, SEPA_TRANSFER, WIRE_TRANSFER, STANDING_ORDER,
     * DIRECT_DEBIT, and crypto variants). "Hold" and "reversal" are NOT distinct transaction types: holds are
     * expressed by keeping the transaction in PENDING status (via {@link #initialStatus}), and reversals are
     * expressed by pointing {@link #relatedTransactionId} at the original transaction with
     * {@link #relationType} = "REVERSAL".
     */
    private String transactionType;

    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime valueDate;
    private LocalDateTime bookingDate;
    private String description;

    /** Balanced list of legs — Σ DEBIT must equal Σ CREDIT. */
    private List<LedgerLegSpec> legs;

    /** Optional: one of DEPOSIT | FEE | INTEREST | TRANSFER. Null means no typed line. */
    private String lineType;

    /** The typed line DTO matching {@link #lineType}. The handler downcasts by {@link #lineType}. */
    private Object lineDto;

    /** POSTED (default) transitions the transaction to POSTED after leg creation; PENDING keeps it pending. */
    private String initialStatus;

    /** Optional — original transaction id when this is a reversal, adjustment, chargeback or correction. */
    private UUID relatedTransactionId;

    /** Optional — REVERSAL | ADJUSTMENT | CHARGEBACK | CORRECTION. */
    private String relationType;
}
