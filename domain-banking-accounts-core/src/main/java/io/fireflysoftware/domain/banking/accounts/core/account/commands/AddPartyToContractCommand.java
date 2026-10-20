package io.fireflysoftware.domain.banking.accounts.core.account.commands;

import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
public class AddPartyToContractCommand implements Command<UUID> {
    private UUID contractId;
    private UUID partyId;
    private UUID roleInContractId;

    public AddPartyToContractCommand withContractId(UUID contractId) {
        this.contractId = contractId;
        return this;
    }
}
