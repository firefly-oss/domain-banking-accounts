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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Moves money between two spaces of one account. The account total does not change.
 * The ledger pair is still balanced: one debit and one credit for the same amount.
 */
public final class SpaceSweep {

    private SpaceSweep() {
    }

    public record Legs(BigDecimal debit, BigDecimal credit, String currency) {
    }

    public static Legs plan(UUID accountId, UUID sourceAccountId, UUID targetAccountId, UUID sourceSpaceId,
                            UUID targetSpaceId, BigDecimal sourceBalance, BigDecimal amount, boolean sourceFrozen,
                            boolean targetFrozen, String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter code");
        }
        if (sourceSpaceId == null || targetSpaceId == null || sourceSpaceId.equals(targetSpaceId)) {
            throw new IllegalArgumentException("A sweep needs two different spaces");
        }
        if (accountId == null || !accountId.equals(sourceAccountId) || !accountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("A space sweep stays inside one account");
        }
        if (sourceFrozen || targetFrozen) {
            throw new IllegalArgumentException("A frozen space cannot be swept");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Sweep amount must be positive");
        }
        if (sourceBalance == null || sourceBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("The source space does not have the sweep amount");
        }
        return new Legs(amount, amount, currency);
    }
}
