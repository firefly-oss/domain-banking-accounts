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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.UpdateAccountStatusCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@CommandHandlerComponent
public class UpdateAccountStatusHandler extends CommandHandler<UpdateAccountStatusCommand, AccountDTO> {

    private static final String CLOSED = "CLOSED";

    private final AccountsApi accountsApi;

    public UpdateAccountStatusHandler(AccountsApi accountsApi) {
        this.accountsApi = accountsApi;
    }

    @Override
    protected Mono<AccountDTO> doHandle(UpdateAccountStatusCommand cmd) {
        if (cmd.accountId() == null || cmd.status() == null || cmd.status().isBlank()) {
            return Mono.error(new IllegalArgumentException("Account id and status are required"));
        }
        return accountsApi.getAccount(cmd.accountId(), UUID.randomUUID().toString())
                .flatMap(account -> {
                    account.setAccountStatus(AccountDTO.AccountStatusEnum.fromValue(cmd.status()));
                    account.setCloseDate(CLOSED.equals(cmd.status()) ? LocalDate.now() : null);
                    if (cmd.reason() != null && !cmd.reason().isBlank()) {
                        account.setDescription(cmd.reason());
                    }
                    return accountsApi.updateAccount(cmd.accountId(), account, UUID.randomUUID().toString());
                });
    }
}
