package io.fireflysoftware.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SetParameterCommand implements Command<UUID> {
    private UUID accountId;
    private String parameterType;
    private String parameterValue;
    private BigDecimal numericValue;

    public SetParameterCommand withAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
}
