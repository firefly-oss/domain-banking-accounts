package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
public class CreateContractCommand implements Command<UUID> {
    private UUID productId;
    private UUID productCatalogId;
}
