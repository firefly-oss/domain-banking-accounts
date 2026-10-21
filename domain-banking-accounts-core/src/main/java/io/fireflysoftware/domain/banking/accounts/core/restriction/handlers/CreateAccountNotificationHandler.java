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
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountNotificationDTO;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.CreateAccountNotificationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateAccountNotificationHandler extends CommandHandler<CreateAccountNotificationCommand, UUID> {

    private final AccountNotificationsApi accountNotificationsApi;

    public CreateAccountNotificationHandler(AccountNotificationsApi accountNotificationsApi) {
        this.accountNotificationsApi = accountNotificationsApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateAccountNotificationCommand cmd) {
        if (cmd.accountId() == null) {
            return Mono.error(new IllegalArgumentException("Account id is required"));
        }
        String restrictionType = cmd.restrictionType() == null ? "restriction" : cmd.restrictionType();
        AccountNotificationDTO notification = new AccountNotificationDTO();
        notification.setAccountId(cmd.accountId());
        notification.setNotificationType(AccountNotificationDTO.NotificationTypeEnum.RESTRICTION_APPLIED);
        notification.setTitle("Account restriction applied");
        notification.setMessage("A " + restrictionType + " restriction was applied to the account.");
        notification.setCreationDateTime(LocalDateTime.now());
        notification.setPriority(2);
        notification.setIsRead(false);
        return accountNotificationsApi.createAccountNotification(notification, UUID.randomUUID().toString())
                .mapNotNull(created -> Objects.requireNonNull(created).getAccountNotificationId());
    }
}
