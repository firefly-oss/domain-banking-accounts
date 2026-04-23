package com.firefly.domain.banking.accounts.core.holder.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountsApi;
import com.firefly.domain.banking.accounts.core.holder.commands.GetAccountContractCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class GetAccountContractHandler extends CommandHandler<GetAccountContractCommand, UUID> {

    private final AccountsApi accountsApi;

    public GetAccountContractHandler(AccountsApi accountsApi) {
        this.accountsApi = accountsApi;
    }

    @Override
    protected Mono<UUID> doHandle(GetAccountContractCommand cmd) {
        return accountsApi.getAccount(cmd.getAccountId(), UUID.randomUUID().toString())
                .mapNotNull(account -> Objects.requireNonNull(account).getContractId());
    }
}
