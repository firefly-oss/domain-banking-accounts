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
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAccountCommand implements Command<UUID> {

    private UUID partyId;
    private UUID productId;
    private UUID productCatalogId;
    private UUID roleInContractId;
    private String accountType;
    private String currency;
    private UUID branchId;
    private BigDecimal initialAmount;
    private List<AccountParameter> defaultParameters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountParameter {
        private String parameterType;
        private String parameterValue;
        private BigDecimal numericValue;
    }

    public CreateContractCommand toContractCommand() {
        return CreateContractCommand.builder()
                .productId(productId)
                .productCatalogId(productCatalogId)
                .build();
    }

    public AddPartyToContractCommand toPartyCommand() {
        return AddPartyToContractCommand.builder()
                .partyId(partyId)
                .roleInContractId(roleInContractId)
                .build();
    }

    public CreateAccountCommand toAccountCommand() {
        return CreateAccountCommand.builder()
                .accountType(accountType)
                .currency(currency)
                .branchId(branchId)
                .build();
    }

    public PostOpeningLedgerInput toOpeningLedgerInput() {
        return PostOpeningLedgerInput.builder()
                .initialAmount(initialAmount)
                .build();
    }

    public CreateMainSpaceCommand toSpaceCommand() {
        return CreateMainSpaceCommand.builder()
                .spaceType("MAIN")
                .spaceName("Main")
                .build();
    }

    public RecordStatusCommand toStatusCommand() {
        return RecordStatusCommand.builder()
                .status("OPEN")
                .reason("Account opened")
                .build();
    }

    public List<SetParameterCommand> toParameterCommands() {
        if (defaultParameters == null || defaultParameters.isEmpty()) {
            return List.of();
        }
        return defaultParameters.stream()
                .map(p -> SetParameterCommand.builder()
                        .parameterType(p.getParameterType())
                        .parameterValue(p.getParameterValue())
                        .numericValue(p.getNumericValue())
                        .build())
                .toList();
    }
}
