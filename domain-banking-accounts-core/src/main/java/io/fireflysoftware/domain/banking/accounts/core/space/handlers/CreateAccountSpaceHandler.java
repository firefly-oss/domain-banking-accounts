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
import io.fireflysoftware.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateAccountSpaceHandler extends CommandHandler<CreateAccountSpaceCommand, UUID> {

    private final AccountSpacesApi accountSpacesApi;

    public CreateAccountSpaceHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateAccountSpaceCommand cmd) {
        if (cmd.getAccountId() == null) {
            return Mono.error(new IllegalArgumentException("Account id is required"));
        }
        if (cmd.getName() == null || cmd.getName().isBlank()) {
            return Mono.error(new IllegalArgumentException("Space name is required"));
        }
        if (cmd.getSpaceType() == null || cmd.getSpaceType().isBlank()) {
            return Mono.error(new IllegalArgumentException("Space type is required"));
        }
        AccountSpaceDTO space = new AccountSpaceDTO();
        space.setAccountId(cmd.getAccountId());
        space.setSpaceName(cmd.getName());
        space.setSpaceType(AccountSpaceDTO.SpaceTypeEnum.fromValue(cmd.getSpaceType()));
        space.setBalance(BigDecimal.ZERO);
        space.setTargetAmount(cmd.getTargetAmount());
        if (cmd.getTargetDate() != null) {
            space.setTargetDate(cmd.getTargetDate().atStartOfDay());
        }
        return accountSpacesApi.createAccountSpace(space, UUID.randomUUID().toString())
                .mapNotNull(created -> Objects.requireNonNull(created).getAccountSpaceId());
    }
}
