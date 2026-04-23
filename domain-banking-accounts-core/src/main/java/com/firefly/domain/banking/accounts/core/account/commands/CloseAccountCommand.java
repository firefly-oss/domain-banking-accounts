package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseAccountCommand implements Command<Void> {
    private UUID accountId;
    private String reason;

    /** Currency of the account — required for ledger settlement legs. */
    private String currency;

    /** If true, allow closure with a residual by transferring it to {@link #destinationAccountId}. */
    private Boolean forceClose;

    /** Destination for any residual balance when {@link #forceClose} is true. */
    private UUID destinationAccountId;

    /** Total fees owed at closure (monthly fee + unpaid interest). Ledger leg is skipped when null or zero. */
    private BigDecimal feesOwed;
}
