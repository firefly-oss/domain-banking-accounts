/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountBalancesApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountBalanceDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponse;
import io.fireflysoftware.core.banking.ledger.sdk.api.AccountLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionLegDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.queries.GetAccountBalancesQuery;
import io.fireflysoftware.domain.banking.accounts.infra.properties.BalanceQueryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Returns the current balance of an account. The implementation follows
 * {@link BalanceQueryProperties.Source}:
 *
 * <ul>
 *   <li>{@code LEDGER} (default) composes a single {@link AccountBalanceDTO} by paginating
 *       every leg via {@code AccountLegsApi} and summing {@code credits − debits}. This makes
 *       {@code core-banking-ledger} the single source of truth and treats {@code account_balance}
 *       as a projection only.</li>
 *   <li>{@code SNAPSHOT} preserves the pre-ledger behaviour — delegate to
 *       {@code AccountBalancesApi.getAllBalances} — kept as a rollback switch while projection
 *       consumers catch up.</li>
 * </ul>
 */
@Slf4j
@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountBalancesHandler extends QueryHandler<GetAccountBalancesQuery, PaginationResponse> {

    private static final int LEG_PAGE_SIZE = 100;
    private static final String LEG_CREDIT = "CREDIT";
    private static final String DEFAULT_SORT_BY = "dateCreated";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 100;

    private final AccountBalancesApi accountBalancesApi;
    private final AccountLegsApi accountLegsApi;
    private final ObjectMapper objectMapper;
    private final BalanceQueryProperties balanceQueryProperties;

    @Override
    protected Mono<PaginationResponse> doHandle(GetAccountBalancesQuery query) {
        BalanceQueryProperties.Source source = balanceQueryProperties.getSource() == null
                ? BalanceQueryProperties.Source.LEDGER
                : balanceQueryProperties.getSource();
        return switch (source) {
            case LEDGER -> composeFromLedger(query);
            case SNAPSHOT -> readSnapshot(query);
        };
    }

    private Mono<PaginationResponse> composeFromLedger(GetAccountBalancesQuery query) {
        return sumLegsFromPage(query.getAccountId(), 0, BigDecimal.ZERO)
                .map(residual -> wrap(query.getAccountId(), residual))
                .doOnSuccess(response -> log.info(
                        "Composed account balance from ledger accountId={} residual={}",
                        query.getAccountId(),
                        extractAmount(response)));
    }

    private Mono<PaginationResponse> readSnapshot(GetAccountBalancesQuery query) {
        return accountBalancesApi.getAllBalances(
                query.getAccountId(),
                query.getPage() != null ? query.getPage() : DEFAULT_PAGE,
                query.getSize() != null ? query.getSize() : DEFAULT_SIZE,
                query.getSortBy() != null ? query.getSortBy() : DEFAULT_SORT_BY,
                query.getSortDirection() != null ? query.getSortDirection() : DEFAULT_SORT_DIRECTION,
                UUID.randomUUID().toString());
    }

    private Mono<BigDecimal> sumLegsFromPage(UUID accountId, int page, BigDecimal accumulator) {
        return accountLegsApi
                .listAccountLegs(accountId, page, LEG_PAGE_SIZE, null, null, UUID.randomUUID().toString())
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

    private boolean hasMorePages(io.fireflysoftware.core.banking.ledger.sdk.model.PaginationResponse response, int currentPage) {
        if (response == null || response.getTotalPages() == null) {
            return false;
        }
        return currentPage + 1 < response.getTotalPages();
    }

    private PaginationResponse wrap(UUID accountId, BigDecimal residual) {
        AccountBalanceDTO composed = new AccountBalanceDTO()
                .accountId(accountId)
                .balanceType(AccountBalanceDTO.BalanceTypeEnum.CURRENT)
                .balanceAmount(residual)
                .asOfDatetime(LocalDateTime.now());
        PaginationResponse response = new PaginationResponse();
        response.setContent(List.of(composed));
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setCurrentPage(0);
        return response;
    }

    private BigDecimal extractAmount(PaginationResponse response) {
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object first = response.getContent().get(0);
        if (first instanceof AccountBalanceDTO dto) {
            return dto.getBalanceAmount();
        }
        return BigDecimal.ZERO;
    }
}
