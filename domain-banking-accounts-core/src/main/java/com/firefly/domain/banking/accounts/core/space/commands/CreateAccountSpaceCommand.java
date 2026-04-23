package com.firefly.domain.banking.accounts.core.space.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateAccountSpaceCommand implements Command<UUID> {
    private UUID accountId;
    private String spaceType;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private AutoTransferRules autoTransferRules;

    /** Currency of the parent account — used on the ledger legs for the space init transfer. */
    private String currency;

    /** Optional seed amount to move into the new space from {@link #sourceSpaceId}. */
    private BigDecimal initialAmount;

    /** Space to debit when seeding the new space — required when {@link #initialAmount} is non-zero. */
    private UUID sourceSpaceId;

    @Data
    @Builder
    public static class AutoTransferRules {
        private UUID sourceSpaceId;
        private String frequency;
        private BigDecimal amount;
    }
}
