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


package io.fireflysoftware.domain.banking.accounts.core.account.services.impl;

import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponse;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountBalancesQuery;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountParametersQuery;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountQuery;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountStatusHistoryQuery;
import io.fireflysoftware.domain.banking.accounts.core.account.services.AccountQueryService;
import io.fireflysoftware.domain.banking.accounts.core.holder.queries.GetAccountHoldersQuery;
import io.fireflysoftware.domain.banking.accounts.core.restriction.queries.GetAccountRestrictionsQuery;
import io.fireflysoftware.domain.banking.accounts.core.space.queries.GetAccountSpacesQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.query.QueryBus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountQueryServiceImpl implements AccountQueryService {

    private final QueryBus queryBus;

    @Override
    public Mono<AccountDTO> getAccount(UUID accountId) {
        return queryBus.query(GetAccountQuery.builder()
                .accountId(accountId)
                .build());
    }

    @Override
    public Mono<PaginationResponse> getAccountBalances(UUID accountId) {
        return queryBus.query(GetAccountBalancesQuery.builder()
                .accountId(accountId)
                .build());
    }

    @Override
    public Mono<PaginationResponse> getAccountParameters(UUID accountId) {
        return queryBus.query(GetAccountParametersQuery.builder()
                .accountId(accountId)
                .build());
    }

    @Override
    public Mono<PaginationResponse> getAccountStatusHistory(UUID accountId) {
        return queryBus.query(GetAccountStatusHistoryQuery.builder()
                .accountId(accountId)
                .build());
    }

    @Override
    public Mono<PaginationResponseAccountSpaceDTO> getAccountSpaces(UUID accountId) {
        return queryBus.query(GetAccountSpacesQuery.builder()
                .accountId(accountId)
                .build());
    }

    @Override
    public Mono<AccountRestrictionDTO> getAccountRestrictions(UUID accountId, boolean activeOnly) {
        return queryBus.query(GetAccountRestrictionsQuery.builder()
                .accountId(accountId)
                .activeOnly(activeOnly)
                .build());
    }

    @Override
    public Mono<io.fireflysoftware.core.contract.sdk.model.PaginationResponse> getAccountHolders(UUID accountId) {
        return queryBus.query(GetAccountHoldersQuery.builder()
                .accountId(accountId)
                .build());
    }
}
