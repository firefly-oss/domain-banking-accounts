package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountParametersApi;
import com.firefly.core.banking.accounts.sdk.model.AccountParameterDTO;
import com.firefly.domain.banking.accounts.core.account.commands.SetParameterCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class SetParameterHandler extends CommandHandler<SetParameterCommand, UUID> {

    private final AccountParametersApi accountParametersApi;

    public SetParameterHandler(AccountParametersApi accountParametersApi) {
        this.accountParametersApi = accountParametersApi;
    }

    @Override
    protected Mono<UUID> doHandle(SetParameterCommand cmd) {
        AccountParameterDTO paramDTO = new AccountParameterDTO();
        paramDTO.setParamType(AccountParameterDTO.ParamTypeEnum.fromValue(cmd.getParameterType()));
        paramDTO.setParamValue(cmd.getNumericValue());
        paramDTO.setEffectiveDate(LocalDateTime.now());

        return accountParametersApi.createParameter(cmd.getAccountId(), paramDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountParameterId());
    }
}
