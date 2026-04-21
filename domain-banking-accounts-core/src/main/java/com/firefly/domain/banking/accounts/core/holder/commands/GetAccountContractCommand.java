package com.firefly.domain.banking.accounts.core.holder.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
public class GetAccountContractCommand implements Command<UUID> {
    private UUID accountId;
}
