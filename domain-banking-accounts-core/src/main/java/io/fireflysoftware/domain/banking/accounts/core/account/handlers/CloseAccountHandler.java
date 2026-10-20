package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@CommandHandlerComponent
public class CloseAccountHandler extends CommandHandler<CloseAccountCommand, Void> {

    private final AccountsApi accountsApi;

    public CloseAccountHandler(AccountsApi accountsApi) {
        this.accountsApi = accountsApi;
    }

    @Override
    protected Mono<Void> doHandle(CloseAccountCommand cmd) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setAccountStatus(AccountDTO.AccountStatusEnum.CLOSED);
        accountDTO.setCloseDate(LocalDate.now());
        accountDTO.setDescription(cmd.getReason());

        return accountsApi.updateAccount(cmd.getAccountId(), accountDTO, UUID.randomUUID().toString())
                .then();
    }
}
