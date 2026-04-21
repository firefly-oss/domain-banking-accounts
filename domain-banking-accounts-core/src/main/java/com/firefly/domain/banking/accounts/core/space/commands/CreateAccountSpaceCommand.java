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

    @Data
    @Builder
    public static class AutoTransferRules {
        private UUID sourceSpaceId;
        private String frequency;
        private BigDecimal amount;
    }
}
