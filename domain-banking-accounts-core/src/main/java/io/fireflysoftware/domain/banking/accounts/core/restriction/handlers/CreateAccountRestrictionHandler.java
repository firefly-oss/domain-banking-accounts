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
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateAccountRestrictionHandler extends CommandHandler<ApplyRestrictionCommand, UUID> {

    static final String APPLIED_BY = "domain-banking-accounts";

    private final AccountRestrictionsApi accountRestrictionsApi;

    public CreateAccountRestrictionHandler(AccountRestrictionsApi accountRestrictionsApi) {
        this.accountRestrictionsApi = accountRestrictionsApi;
    }

    @Override
    protected Mono<UUID> doHandle(ApplyRestrictionCommand cmd) {
        if (cmd.getAccountId() == null) {
            return Mono.error(new IllegalArgumentException("Account id is required"));
        }
        if (cmd.getRestrictionType() == null || cmd.getRestrictionType().isBlank()) {
            return Mono.error(new IllegalArgumentException("Restriction type is required"));
        }
        AccountRestrictionDTO restriction = new AccountRestrictionDTO();
        restriction.setAccountId(cmd.getAccountId());
        restriction.setRestrictionType(AccountRestrictionDTO.RestrictionTypeEnum.fromValue(cmd.getRestrictionType()));
        restriction.setReason(cmd.getReason() == null || cmd.getReason().isBlank()
                ? "Restriction applied by the domain service"
                : cmd.getReason());
        restriction.setAppliedBy(APPLIED_BY);
        restriction.setStartDateTime(cmd.getStartDate() == null ? LocalDateTime.now() : cmd.getStartDate());
        restriction.setEndDateTime(cmd.getEndDateTime());
        restriction.setRestrictedAmount(cmd.getRestrictedAmount());
        restriction.setReferenceNumber(cmd.getReferenceNumber());
        restriction.setIsActive(true);
        return accountRestrictionsApi.createAccountRestriction(restriction, UUID.randomUUID().toString())
                .mapNotNull(created -> Objects.requireNonNull(created).getAccountRestrictionId());
    }
}
