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


package io.fireflysoftware.domain.banking.accounts.core.account.commands;

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
