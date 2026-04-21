package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountBalancesApi;
import com.firefly.core.banking.accounts.sdk.model.AccountBalanceDTO;
import com.firefly.domain.banking.accounts.core.account.commands.InitializeBalanceCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class InitializeBalanceHandler extends CommandHandler<InitializeBalanceCommand, UUID> {

    private final AccountBalancesApi accountBalancesApi;

    public InitializeBalanceHandler(AccountBalancesApi accountBalancesApi) {
        this.accountBalancesApi = accountBalancesApi;
    }

    @Override
    protected Mono<UUID> doHandle(InitializeBalanceCommand cmd) {
        AccountBalanceDTO balanceDTO = new AccountBalanceDTO();
        balanceDTO.setBalanceType(AccountBalanceDTO.BalanceTypeEnum.fromValue(cmd.getBalanceType()));
        balanceDTO.setBalanceAmount(cmd.getInitialAmount());
        balanceDTO.setAssetSymbol(cmd.getCurrency());

        return accountBalancesApi.createBalance(cmd.getAccountId(), balanceDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountBalanceId());
    }
}
