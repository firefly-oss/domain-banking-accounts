package com.firefly.domain.banking.accounts.core.account.services;

import com.firefly.core.banking.accounts.sdk.model.AccountDTO;
import com.firefly.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountQueryService {

    Mono<AccountDTO> getAccount(UUID accountId);

    Mono<PaginationResponse> getAccountBalances(UUID accountId);

    Mono<PaginationResponse> getAccountParameters(UUID accountId);

    Mono<PaginationResponse> getAccountStatusHistory(UUID accountId);

    Mono<PaginationResponseAccountSpaceDTO> getAccountSpaces(UUID accountId);

    Mono<AccountRestrictionDTO> getAccountRestrictions(UUID accountId, boolean activeOnly);

    Mono<com.firefly.core.contract.sdk.model.PaginationResponse> getAccountHolders(UUID accountId);
}
