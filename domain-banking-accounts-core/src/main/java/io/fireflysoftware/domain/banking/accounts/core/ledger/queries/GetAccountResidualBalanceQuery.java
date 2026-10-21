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


package io.fireflysoftware.domain.banking.accounts.core.ledger.queries;

import io.fireflysoftware.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

/**
 * Query that asks core-banking-ledger for the signed residual balance of an account, computed
 * as {@code Σ CREDIT.amount − Σ DEBIT.amount} across every leg (every space, every status).
 * Used by {@code CloseAccountSaga} as the source of truth when deciding whether the account
 * can be closed or needs settlement. Ignores the {@code account_balance} projection entirely.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAccountResidualBalanceQuery implements Query<AccountResidualBalanceResult> {
    private UUID accountId;
}
