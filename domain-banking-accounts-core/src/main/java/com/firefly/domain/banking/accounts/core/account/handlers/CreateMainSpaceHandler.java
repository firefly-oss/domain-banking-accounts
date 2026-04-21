package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountSpacesApi;
import com.firefly.core.banking.accounts.sdk.model.AccountSpaceDTO;
import com.firefly.domain.banking.accounts.core.account.commands.CreateMainSpaceCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateMainSpaceHandler extends CommandHandler<CreateMainSpaceCommand, UUID> {

    private final AccountSpacesApi accountSpacesApi;

    public CreateMainSpaceHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateMainSpaceCommand cmd) {
        AccountSpaceDTO spaceDTO = new AccountSpaceDTO();
        spaceDTO.setAccountId(cmd.getAccountId());
        spaceDTO.setSpaceName(cmd.getSpaceName());
        spaceDTO.setSpaceType(AccountSpaceDTO.SpaceTypeEnum.fromValue(cmd.getSpaceType()));

        return accountSpacesApi.createAccountSpace(spaceDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountSpaceId());
    }
}
