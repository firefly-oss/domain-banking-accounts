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
import io.fireflysoftware.core.banking.accounts.sdk.model.FilterRequestAccountSpaceDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import io.fireflysoftware.domain.banking.accounts.core.space.queries.GetAccountSpacesQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountSpacesHandler extends QueryHandler<GetAccountSpacesQuery, PaginationResponseAccountSpaceDTO> {

    private final AccountSpacesApi accountSpacesApi;

    @Override
    protected Mono<PaginationResponseAccountSpaceDTO> doHandle(GetAccountSpacesQuery query) {
        AccountSpaceDTO filterCriteria = new AccountSpaceDTO();
        filterCriteria.setAccountId(query.getAccountId());

        FilterRequestAccountSpaceDTO filter = new FilterRequestAccountSpaceDTO();
        filter.setFilters(filterCriteria);

        return accountSpacesApi.filterAccountSpaces(filter, UUID.randomUUID().toString());
    }
}
