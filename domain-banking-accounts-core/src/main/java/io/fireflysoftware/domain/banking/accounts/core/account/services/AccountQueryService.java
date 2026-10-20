package io.fireflysoftware.domain.banking.accounts.core.account.services;

import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponse;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountQueryService {

    Mono<AccountDTO> getAccount(UUID accountId);

    Mono<PaginationResponse> getAccountBalances(UUID accountId);

    Mono<PaginationResponse> getAccountParameters(UUID accountId);

    Mono<PaginationResponse> getAccountStatusHistory(UUID accountId);

    Mono<PaginationResponseAccountSpaceDTO> getAccountSpaces(UUID accountId);

    Mono<AccountRestrictionDTO> getAccountRestrictions(UUID accountId, boolean activeOnly);

    Mono<io.fireflysoftware.core.contract.sdk.model.PaginationResponse> getAccountHolders(UUID accountId);
}
