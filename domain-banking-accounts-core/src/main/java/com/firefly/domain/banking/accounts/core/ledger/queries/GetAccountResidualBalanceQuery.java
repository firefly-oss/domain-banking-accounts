package com.firefly.domain.banking.accounts.core.ledger.queries;

import com.firefly.domain.banking.accounts.core.ledger.queries.result.AccountResidualBalanceResult;
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
