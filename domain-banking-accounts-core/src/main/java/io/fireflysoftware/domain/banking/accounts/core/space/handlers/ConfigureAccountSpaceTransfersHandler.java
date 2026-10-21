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
import io.fireflysoftware.domain.banking.accounts.core.space.commands.ConfigureAccountSpaceTransfersCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class ConfigureAccountSpaceTransfersHandler extends CommandHandler<ConfigureAccountSpaceTransfersCommand, UUID> {

    private final AccountSpacesApi accountSpacesApi;

    public ConfigureAccountSpaceTransfersHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<UUID> doHandle(ConfigureAccountSpaceTransfersCommand cmd) {
        if (cmd.accountSpaceId() == null) {
            return Mono.error(new IllegalArgumentException("Account space id is required"));
        }
        String key = UUID.randomUUID().toString();
        return accountSpacesApi.getAccountSpace(cmd.accountSpaceId(), key)
                .flatMap(space -> {
                    space.setEnableAutomaticTransfers(cmd.enabled());
                    if (cmd.frequency() == null || cmd.frequency().isBlank()) {
                        space.setTransferFrequency(null);
                    } else {
                        space.setTransferFrequency(AccountSpaceDTO.TransferFrequencyEnum.fromValue(cmd.frequency()));
                    }
                    space.setTransferAmount(cmd.amount());
                    space.setSourceSpaceId(cmd.sourceSpaceId());
                    return accountSpacesApi.updateAccountSpace(cmd.accountSpaceId(), space, UUID.randomUUID().toString());
                })
                .mapNotNull(updated -> Objects.requireNonNull(updated).getAccountSpaceId());
    }
}
