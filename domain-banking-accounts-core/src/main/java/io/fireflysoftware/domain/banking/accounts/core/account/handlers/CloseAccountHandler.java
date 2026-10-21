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
