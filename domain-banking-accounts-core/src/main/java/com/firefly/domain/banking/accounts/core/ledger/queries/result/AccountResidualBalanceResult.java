package com.firefly.domain.banking.accounts.core.ledger.queries.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of {@code GetAccountResidualBalanceQuery}: the signed residual balance of an account,
 * recomputed on demand by aggregating all posted legs in the ledger. Used by the settlement
 * step of {@code CloseAccountSaga} to determine whether an account still carries a residual
 * that must be moved before closure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResidualBalanceResult {

    /**
     * Signed residual balance (credits minus debits) across all legs on the account.
     * Positive means the bank owes money to the customer, negative means the customer
     * owes the bank, zero means the account is settled.
     */
    private BigDecimal residual;
}
