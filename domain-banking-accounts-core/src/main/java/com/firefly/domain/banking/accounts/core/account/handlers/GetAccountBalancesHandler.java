package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountBalancesApi;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountBalancesQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountBalancesHandler extends QueryHandler<GetAccountBalancesQuery, PaginationResponse> {

    private final AccountBalancesApi accountBalancesApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetAccountBalancesQuery query) {
        return accountBalancesApi.getAllBalances(
                query.getAccountId(),
                query.getPage() != null ? query.getPage() : 0,
                query.getSize() != null ? query.getSize() : 100,
                query.getSortBy() != null ? query.getSortBy() : "dateCreated",
                query.getSortDirection() != null ? query.getSortDirection() : "DESC",
                UUID.randomUUID().toString()
        );
    }
}
