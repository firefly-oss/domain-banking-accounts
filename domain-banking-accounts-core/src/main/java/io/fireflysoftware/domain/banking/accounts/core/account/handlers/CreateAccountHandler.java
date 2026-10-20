package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateAccountCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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
        // core-banking-accounts has NOT NULL constraints on account_number, open_date and
        // account_status. Populate them here until the core exposes dedicated allocators /
        // defaults; without this every createAccount fails with 500 at the DB layer.
        accountDTO.setAccountNumber("ACC-" + UUID.randomUUID());
        accountDTO.setOpenDate(LocalDate.now());
        accountDTO.setAccountStatus(AccountDTO.AccountStatusEnum.OPEN);

        return accountsApi.createAccount(accountDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountId());
    }
}
