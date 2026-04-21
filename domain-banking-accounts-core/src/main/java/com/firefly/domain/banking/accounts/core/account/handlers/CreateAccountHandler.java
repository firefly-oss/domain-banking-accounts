package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountsApi;
import com.firefly.core.banking.accounts.sdk.model.AccountDTO;
import com.firefly.domain.banking.accounts.core.account.commands.CreateAccountCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, UUID> {

    private final AccountsApi accountsApi;

    public CreateAccountHandler(AccountsApi accountsApi) {
        this.accountsApi = accountsApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateAccountCommand cmd) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setContractId(cmd.getContractId());
        accountDTO.setAccountType(AccountDTO.AccountTypeEnum.fromValue(cmd.getAccountType()));
        accountDTO.setCurrency(cmd.getCurrency());
        accountDTO.setBranchId(cmd.getBranchId());

        return accountsApi.createAccount(accountDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountId());
    }
}
