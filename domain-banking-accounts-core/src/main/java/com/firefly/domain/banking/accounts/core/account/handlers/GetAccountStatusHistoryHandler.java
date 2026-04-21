package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountStatusHistoryApi;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountStatusHistoryQuery;
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
