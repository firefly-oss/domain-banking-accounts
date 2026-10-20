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
