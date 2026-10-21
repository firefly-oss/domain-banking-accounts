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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountStatusHistoryApi;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.RemoveAccountStatusHistoryCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveAccountStatusHistoryHandler extends CommandHandler<RemoveAccountStatusHistoryCommand, Void> {

    private final AccountStatusHistoryApi accountStatusHistoryApi;

    public RemoveAccountStatusHistoryHandler(AccountStatusHistoryApi accountStatusHistoryApi) {
        this.accountStatusHistoryApi = accountStatusHistoryApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveAccountStatusHistoryCommand cmd) {
        if (cmd.accountId() == null || cmd.statusHistoryId() == null) {
            return Mono.empty();
        }
        return accountStatusHistoryApi.deleteStatusHistory(cmd.accountId(), cmd.statusHistoryId(), UUID.randomUUID().toString());
    }
}
