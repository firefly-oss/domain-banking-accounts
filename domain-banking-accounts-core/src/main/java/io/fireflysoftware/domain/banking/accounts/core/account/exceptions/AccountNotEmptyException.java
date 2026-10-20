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
