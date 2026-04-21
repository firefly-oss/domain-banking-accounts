package com.firefly.domain.banking.accounts.core.restriction.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveRestrictionCommand implements Command<Void> {
    private UUID accountId;
    private UUID restrictionId;
    private String reason;
}
