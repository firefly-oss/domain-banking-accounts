package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OpenAccountCommand implements Command<UUID> {

    private UUID partyId;
    private UUID productId;
    private UUID productCatalogId;
    private UUID roleInContractId;
    private String accountType;
    private String currency;
    private UUID branchId;
    private List<AccountParameter> defaultParameters;

    @Data
    @Builder
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

    public InitializeBalanceCommand toBalanceCommand() {
        return InitializeBalanceCommand.builder()
                .balanceType("CURRENT")
                .initialAmount(BigDecimal.ZERO)
                .currency(currency)
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
