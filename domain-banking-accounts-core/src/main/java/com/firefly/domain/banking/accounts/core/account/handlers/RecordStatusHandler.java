package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountStatusHistoryApi;
import com.firefly.core.banking.accounts.sdk.model.AccountStatusHistoryDTO;
import com.firefly.domain.banking.accounts.core.account.commands.RecordStatusCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RecordStatusHandler extends CommandHandler<RecordStatusCommand, UUID> {

    private final AccountStatusHistoryApi accountStatusHistoryApi;

    public RecordStatusHandler(AccountStatusHistoryApi accountStatusHistoryApi) {
        this.accountStatusHistoryApi = accountStatusHistoryApi;
    }

    @Override
    protected Mono<UUID> doHandle(RecordStatusCommand cmd) {
        AccountStatusHistoryDTO statusDTO = new AccountStatusHistoryDTO();
        statusDTO.setStatusCode(AccountStatusHistoryDTO.StatusCodeEnum.fromValue(cmd.getStatus()));
        statusDTO.setReason(cmd.getReason());
        statusDTO.setStatusStartDatetime(LocalDateTime.now());

        return accountStatusHistoryApi.createStatusHistory(cmd.getAccountId(), statusDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountStatusHistoryId());
    }
}
