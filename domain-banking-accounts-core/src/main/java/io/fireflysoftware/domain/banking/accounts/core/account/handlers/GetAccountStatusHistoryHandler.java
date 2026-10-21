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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountStatusHistoryApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponse;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountStatusHistoryQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountStatusHistoryHandler extends QueryHandler<GetAccountStatusHistoryQuery, PaginationResponse> {

    private final AccountStatusHistoryApi accountStatusHistoryApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetAccountStatusHistoryQuery query) {
        return accountStatusHistoryApi.getAllStatusHistory(
                query.getAccountId(),
                query.getPage() != null ? query.getPage() : 0,
                query.getSize() != null ? query.getSize() : 100,
                query.getSortBy() != null ? query.getSortBy() : "statusStartDatetime",
                query.getSortDirection() != null ? query.getSortDirection() : "DESC",
                UUID.randomUUID().toString()
        );
    }
}
