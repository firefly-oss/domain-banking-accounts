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
import io.fireflysoftware.domain.banking.accounts.core.space.commands.RemoveAccountSpaceCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveAccountSpaceHandler extends CommandHandler<RemoveAccountSpaceCommand, Void> {

    private final AccountSpacesApi accountSpacesApi;

    public RemoveAccountSpaceHandler(AccountSpacesApi accountSpacesApi) {
        this.accountSpacesApi = accountSpacesApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveAccountSpaceCommand cmd) {
        if (cmd.accountSpaceId() == null) {
            return Mono.empty();
        }
        return accountSpacesApi.deleteAccountSpace(cmd.accountSpaceId(), UUID.randomUUID().toString());
    }
}
