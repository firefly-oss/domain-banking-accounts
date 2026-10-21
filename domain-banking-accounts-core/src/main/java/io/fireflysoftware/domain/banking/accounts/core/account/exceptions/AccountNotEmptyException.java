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


package io.fireflysoftware.domain.banking.accounts.core.account.exceptions;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thrown by {@code CloseAccountSaga} when the account still has a non-zero residual balance in
 * the ledger and the caller did not request {@code forceClose}. Carries the offending amount
 * so callers can decide whether to re-issue the command with {@code forceClose=true} plus a
 * {@code destinationAccountId} for the residual transfer.
 */
@Getter
public class AccountNotEmptyException extends RuntimeException {

    private final UUID accountId;
    private final BigDecimal residual;

    public AccountNotEmptyException(UUID accountId, BigDecimal residual) {
        super("Account " + accountId + " cannot be closed — residual ledger balance " + residual + " is non-zero");
        this.accountId = accountId;
        this.residual = residual;
    }
}
