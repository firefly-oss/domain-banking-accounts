package com.firefly.domain.banking.accounts.core.space.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountSpacesApi;
import com.firefly.core.banking.accounts.sdk.model.AccountSpaceDTO;
import com.firefly.core.banking.accounts.sdk.model.FilterRequestAccountSpaceDTO;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import com.firefly.domain.banking.accounts.core.space.queries.GetAccountSpacesQuery;
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
