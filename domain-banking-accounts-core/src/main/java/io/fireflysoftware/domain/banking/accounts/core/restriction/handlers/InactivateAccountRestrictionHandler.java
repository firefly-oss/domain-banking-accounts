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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountRestrictionsApi;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class InactivateAccountRestrictionHandler extends CommandHandler<RemoveRestrictionCommand, Void> {

    private final AccountRestrictionsApi accountRestrictionsApi;

    public InactivateAccountRestrictionHandler(AccountRestrictionsApi accountRestrictionsApi) {
        this.accountRestrictionsApi = accountRestrictionsApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveRestrictionCommand cmd) {
        if (cmd.getRestrictionId() == null) {
            return Mono.error(new IllegalArgumentException("Restriction id is required"));
        }
        String removedBy = cmd.getReason() == null || cmd.getReason().isBlank()
                ? CreateAccountRestrictionHandler.APPLIED_BY
                : cmd.getReason();
        return accountRestrictionsApi.removeRestriction(cmd.getRestrictionId(), removedBy, UUID.randomUUID().toString())
                .then();
    }
}
