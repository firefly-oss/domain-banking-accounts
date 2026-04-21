package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
public class CreateAccountCommand implements Command<UUID> {
    private UUID contractId;
    private String accountType;
    private String currency;
    private UUID branchId;

    public CreateAccountCommand withContractId(UUID contractId) {
        this.contractId = contractId;
        return this;
    }
}
