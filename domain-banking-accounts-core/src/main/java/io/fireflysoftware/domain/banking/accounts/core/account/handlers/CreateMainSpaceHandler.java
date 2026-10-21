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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpacesApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountSpaceDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateMainSpaceCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateMainSpaceHandler extends CommandHandler<CreateMainSpaceCommand, UUID> {

    private final AccountSpacesApi accountSpacesApi;

    public CreateMainSpaceHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateMainSpaceCommand cmd) {
        AccountSpaceDTO spaceDTO = new AccountSpaceDTO();
        spaceDTO.setAccountId(cmd.getAccountId());
        spaceDTO.setSpaceName(cmd.getSpaceName());
        spaceDTO.setSpaceType(AccountSpaceDTO.SpaceTypeEnum.fromValue(cmd.getSpaceType()));

        return accountSpacesApi.createAccountSpace(spaceDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getAccountSpaceId());
    }
}
