package com.firefly.domain.banking.accounts.core.restriction.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveRestrictionCommand implements Command<Void> {
    private UUID accountId;
    private UUID restrictionId;
    private String reason;

    /** Optional — id of the original HOLD ledger transaction to reverse; null for non-monetary restrictions. */
    private UUID originalHoldTxId;

    /** Required when {@link #originalHoldTxId} is set — amount to reverse. */
    private BigDecimal holdAmount;

    /** Required when {@link #originalHoldTxId} is set — currency of the reversal legs. */
    private String currency;
}
