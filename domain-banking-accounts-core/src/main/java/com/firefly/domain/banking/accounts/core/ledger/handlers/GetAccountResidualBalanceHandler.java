package com.firefly.domain.banking.accounts.core.ledger.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.banking.ledger.sdk.api.AccountLegsApi;
import com.firefly.core.banking.ledger.sdk.model.PaginationResponse;
import com.firefly.core.banking.ledger.sdk.model.TransactionLegDTO;
import com.firefly.domain.banking.accounts.core.ledger.queries.GetAccountResidualBalanceQuery;
import com.firefly.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Query handler that computes the signed residual balance of an account by paginating every
 * leg through {@code AccountLegsApi} and summing {@code credits − debits}. This is the
 * authoritative balance signal used by {@code CloseAccountSaga}; {@code account_balance}
 * snapshots are projections and must never be consulted for closure decisions.
 */
@Slf4j
@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountResidualBalanceHandler
        extends QueryHandler<GetAccountResidualBalanceQuery, AccountResidualBalanceResult> {

    private static final int PAGE_SIZE = 100;
    private static final String LEG_CREDIT = "CREDIT";

    private final AccountLegsApi accountLegsApi;
    private final ObjectMapper objectMapper;

    @Override
    protected Mono<AccountResidualBalanceResult> doHandle(GetAccountResidualBalanceQuery query) {
        return sumLegsFromPage(query.getAccountId(), 0, BigDecimal.ZERO)
                .map(residual -> AccountResidualBalanceResult.builder().residual(residual).build())
                .doOnSuccess(result -> log.info(
                        "Computed residual balance accountId={} residual={}",
                        query.getAccountId(), result.getResidual()));
    }

    private Mono<BigDecimal> sumLegsFromPage(UUID accountId, int page, BigDecimal accumulator) {
        return accountLegsApi
                .listAccountLegs(accountId, page, PAGE_SIZE, null, null, UUID.randomUUID().toString())
                .flatMap(response -> {
                    BigDecimal running = accumulator.add(sumPage(response.getContent()));
                    if (hasMorePages(response, page)) {
                        return sumLegsFromPage(accountId, page + 1, running);
                    }
                    return Mono.just(running);
                });
    }

    private BigDecimal sumPage(List<Object> rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return rawContent.stream()
                .filter(Objects::nonNull)
                .map(raw -> objectMapper.convertValue(raw, TransactionLegDTO.class))
                .map(this::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal signedAmount(TransactionLegDTO leg) {
        if (leg.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return LEG_CREDIT.equalsIgnoreCase(leg.getLegType()) ? leg.getAmount() : leg.getAmount().negate();
    }

    private boolean hasMorePages(PaginationResponse response, int currentPage) {
        if (response == null || response.getTotalPages() == null) {
            return false;
        }
        return currentPage + 1 < response.getTotalPages();
    }
}
