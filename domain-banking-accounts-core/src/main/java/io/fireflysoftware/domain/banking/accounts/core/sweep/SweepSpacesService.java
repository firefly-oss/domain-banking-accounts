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


package io.fireflysoftware.domain.banking.accounts.core.sweep;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpaceTransactionsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpacesApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sweeps one space into another through the accounts core SDK, then posts a balanced ledger pair.
 */
@Service
public class SweepSpacesService {

    private final AccountSpacesApi spaces;
    private final AccountSpaceTransactionsApi transactions;
    private final LedgerPort ledger;

    public SweepSpacesService(AccountSpacesApi spaces, AccountSpaceTransactionsApi transactions, LedgerPort ledger) {
        this.spaces = spaces;
        this.transactions = transactions;
        this.ledger = ledger;
    }

    public Mono<UUID> sweep(UUID accountId, UUID sourceSpaceId, UUID targetSpaceId, BigDecimal amount, String currency) {
        String key = UUID.randomUUID().toString();
        return spaces.getAccountSpace(sourceSpaceId, key).zipWith(spaces.getAccountSpace(targetSpaceId, key))
                .flatMap(pair -> {
                    var source = pair.getT1();
                    var target = pair.getT2();
                    SpaceSweep.Legs legs = SpaceSweep.plan(accountId, source.getAccountId(), target.getAccountId(),
                            sourceSpaceId, targetSpaceId, source.getBalance(), amount,
                            Boolean.TRUE.equals(source.getIsFrozen()), Boolean.TRUE.equals(target.getIsFrozen()), currency);
                    String reference = accountId + ":" + sourceSpaceId + ":" + targetSpaceId;
                    return transactions.recordTransaction(sourceSpaceId, amount.negate(), "Space sweep out", reference, key)
                            .then(transactions.recordTransaction(targetSpaceId, amount, "Space sweep in", reference, key))
                            .then(ledger.postBalanced(legs.debit(), legs.credit(), legs.currency(), reference));
                });
    }
}
