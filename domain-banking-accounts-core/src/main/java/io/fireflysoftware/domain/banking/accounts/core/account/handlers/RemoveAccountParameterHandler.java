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
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RemoveAccountParameterCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveAccountParameterHandler extends CommandHandler<RemoveAccountParameterCommand, Void> {

    private final AccountParametersApi accountParametersApi;

    public RemoveAccountParameterHandler(AccountParametersApi accountParametersApi) {
        this.accountParametersApi = accountParametersApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveAccountParameterCommand cmd) {
        if (cmd.accountId() == null || cmd.parameterId() == null) {
            return Mono.empty();
        }
        return accountParametersApi.deleteParameter(cmd.accountId(), cmd.parameterId(), UUID.randomUUID().toString());
    }
}
