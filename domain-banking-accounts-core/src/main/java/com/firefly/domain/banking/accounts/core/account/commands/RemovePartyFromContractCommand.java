package com.firefly.domain.banking.accounts.core.account.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemovePartyFromContractCommand implements Command<Void> {
    private UUID contractId;
    private UUID partyId;
}
