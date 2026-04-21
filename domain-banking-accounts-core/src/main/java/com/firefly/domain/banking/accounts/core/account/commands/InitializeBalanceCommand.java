package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class InitializeBalanceCommand implements Command<UUID> {
    private UUID accountId;
    private String balanceType;
    private BigDecimal initialAmount;
    private String currency;

    public InitializeBalanceCommand withAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
}
