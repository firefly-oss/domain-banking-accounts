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
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ReactivateAccountRestrictionCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class ReactivateAccountRestrictionHandler extends CommandHandler<ReactivateAccountRestrictionCommand, Void> {

    private final AccountRestrictionsApi accountRestrictionsApi;

    public ReactivateAccountRestrictionHandler(AccountRestrictionsApi accountRestrictionsApi) {
        this.accountRestrictionsApi = accountRestrictionsApi;
    }

    @Override
    protected Mono<Void> doHandle(ReactivateAccountRestrictionCommand cmd) {
        if (cmd.accountRestrictionId() == null) {
            return Mono.empty();
        }
        String key = UUID.randomUUID().toString();
        return accountRestrictionsApi.getAccountRestriction(cmd.accountRestrictionId(), key)
                .flatMap(restriction -> {
                    restriction.setIsActive(true);
                    restriction.setRemovedBy(null);
                    return accountRestrictionsApi.updateAccountRestriction(cmd.accountRestrictionId(), restriction, UUID.randomUUID().toString());
                })
                .then();
    }
}
