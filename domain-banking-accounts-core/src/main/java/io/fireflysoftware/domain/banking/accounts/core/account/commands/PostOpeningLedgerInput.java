package io.fireflysoftware.domain.banking.accounts.core.account.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Input payload for the {@code STEP_POST_OPENING_LEDGER} saga step. Carries the initial deposit
 * amount; the step builds the {@link io.fireflysoftware.domain.banking.accounts.core.ledger.commands.PostLedgerTransactionCommand}
 * by combining this amount with accountId and accountCurrency stored in the execution context
 * by {@code STEP_CREATE_ACCOUNT}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostOpeningLedgerInput {
    private BigDecimal initialAmount;
}
