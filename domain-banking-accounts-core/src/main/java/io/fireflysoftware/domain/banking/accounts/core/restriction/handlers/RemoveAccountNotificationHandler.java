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


package io.fireflysoftware.domain.banking.accounts.core.restriction.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountNotificationsApi;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveAccountNotificationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveAccountNotificationHandler extends CommandHandler<RemoveAccountNotificationCommand, Void> {

    private final AccountNotificationsApi accountNotificationsApi;

    public RemoveAccountNotificationHandler(AccountNotificationsApi accountNotificationsApi) {
        this.accountNotificationsApi = accountNotificationsApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveAccountNotificationCommand cmd) {
        if (cmd.accountNotificationId() == null) {
            return Mono.empty();
        }
        return accountNotificationsApi.deleteAccountNotification(cmd.accountNotificationId(), UUID.randomUUID().toString());
    }
}
