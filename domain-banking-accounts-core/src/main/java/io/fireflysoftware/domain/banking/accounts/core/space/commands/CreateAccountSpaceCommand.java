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


package io.fireflysoftware.domain.banking.accounts.core.space.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateAccountSpaceCommand implements Command<UUID> {
    private UUID accountId;
    private String spaceType;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private AutoTransferRules autoTransferRules;

    /** Currency of the parent account — used on the ledger legs for the space init transfer. */
    private String currency;

    /** Optional seed amount to move into the new space from {@link #sourceSpaceId}. */
    private BigDecimal initialAmount;

    /** Space to debit when seeding the new space — required when {@link #initialAmount} is non-zero. */
    private UUID sourceSpaceId;

    @Data
    @Builder
    public static class AutoTransferRules {
        private UUID sourceSpaceId;
        private String frequency;
        private BigDecimal amount;
    }
}
