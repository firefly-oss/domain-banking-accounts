/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountParametersApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountParameterDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.SetParameterCommand;
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
