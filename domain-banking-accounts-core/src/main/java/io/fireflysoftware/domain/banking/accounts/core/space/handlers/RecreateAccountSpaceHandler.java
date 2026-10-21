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


package io.fireflysoftware.domain.banking.accounts.core.space.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpacesApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountSpaceDTO;
import io.fireflysoftware.domain.banking.accounts.core.space.commands.RecreateAccountSpaceCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RecreateAccountSpaceHandler extends CommandHandler<RecreateAccountSpaceCommand, UUID> {

    private final AccountSpacesApi accountSpacesApi;

    public RecreateAccountSpaceHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<UUID> doHandle(RecreateAccountSpaceCommand cmd) {
        AccountSpaceDTO source = cmd.space();
        if (source == null || source.getAccountId() == null) {
            return Mono.error(new IllegalArgumentException("Account space backup is required"));
        }
        AccountSpaceDTO copy = new AccountSpaceDTO();
        copy.setAccountId(source.getAccountId());
        copy.setSpaceName(source.getSpaceName());
        copy.setSpaceType(source.getSpaceType());
        copy.setBalance(source.getBalance() == null ? BigDecimal.ZERO : source.getBalance());
        copy.setTargetAmount(source.getTargetAmount());
        copy.setTargetDate(source.getTargetDate());
        copy.setDescription(source.getDescription());
        copy.setIconId(source.getIconId());
        copy.setColorCode(source.getColorCode());
        copy.setIsVisible(source.getIsVisible());
        copy.setEnableAutomaticTransfers(source.getEnableAutomaticTransfers());
        copy.setTransferFrequency(source.getTransferFrequency());
        copy.setTransferAmount(source.getTransferAmount());
        copy.setSourceSpaceId(source.getSourceSpaceId());
        return accountSpacesApi.createAccountSpace(copy, UUID.randomUUID().toString())
                .mapNotNull(created -> Objects.requireNonNull(created).getAccountSpaceId());
    }
}
