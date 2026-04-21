package com.firefly.domain.banking.accounts.core.account.services.impl;

import com.firefly.core.banking.accounts.sdk.model.AccountDTO;
import com.firefly.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountBalancesQuery;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountParametersQuery;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountQuery;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountStatusHistoryQuery;
import com.firefly.domain.banking.accounts.core.account.services.AccountQueryService;
import com.firefly.domain.banking.accounts.core.holder.queries.GetAccountHoldersQuery;
import com.firefly.domain.banking.accounts.core.restriction.queries.GetAccountRestrictionsQuery;
import com.firefly.domain.banking.accounts.core.space.queries.GetAccountSpacesQuery;
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
    public Mono<com.firefly.core.contract.sdk.model.PaginationResponse> getAccountHolders(UUID accountId) {
        return queryBus.query(GetAccountHoldersQuery.builder()
                .accountId(accountId)
                .build());
    }
}
