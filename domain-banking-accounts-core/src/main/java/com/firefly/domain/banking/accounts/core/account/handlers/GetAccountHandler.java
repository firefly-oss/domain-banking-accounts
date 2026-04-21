package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountsApi;
import com.firefly.core.banking.accounts.sdk.model.AccountDTO;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountHandler extends QueryHandler<GetAccountQuery, AccountDTO> {

    private final AccountsApi accountsApi;

    @Override
    protected Mono<AccountDTO> doHandle(GetAccountQuery query) {
        return accountsApi.getAccount(query.getAccountId(), UUID.randomUUID().toString());
    }
}
