package com.firefly.domain.banking.accounts.core.holder.handlers;

import com.firefly.core.banking.accounts.sdk.api.AccountsApi;
import com.firefly.core.contract.sdk.api.ContractPartiesApi;
import com.firefly.core.contract.sdk.model.FilterRequestContractPartyDTO;
import com.firefly.core.contract.sdk.model.PaginationResponse;
import com.firefly.domain.banking.accounts.core.holder.queries.GetAccountHoldersQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountHoldersHandler extends QueryHandler<GetAccountHoldersQuery, PaginationResponse> {

    private final AccountsApi accountsApi;
    private final ContractPartiesApi contractPartiesApi;

    @Override
    protected Mono<PaginationResponse> doHandle(GetAccountHoldersQuery query) {
        return accountsApi.getAccount(query.getAccountId(), UUID.randomUUID().toString())
                .flatMap(account -> contractPartiesApi.filterContractParties(
                        account.getContractId(),
                        new FilterRequestContractPartyDTO(),
                        UUID.randomUUID().toString()
                ));
    }
}
