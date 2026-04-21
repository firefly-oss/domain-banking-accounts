package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
public class CreateMainSpaceCommand implements Command<UUID> {
    private UUID accountId;
    private String spaceType;
    private String spaceName;

    public CreateMainSpaceCommand withAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
}
