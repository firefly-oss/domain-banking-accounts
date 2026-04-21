package com.firefly.domain.banking.accounts.core.restriction.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplyRestrictionCommand implements Command<UUID> {
    private UUID accountId;
    private String restrictionType;
    private String reason;
    private BigDecimal restrictedAmount;
    private String referenceNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDateTime;
}
