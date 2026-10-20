package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import io.fireflysoftware.core.contract.sdk.api.ContractPartiesApi;
import io.fireflysoftware.core.contract.sdk.model.ContractPartyDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.AddPartyToContractCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class AddPartyToContractHandler extends CommandHandler<AddPartyToContractCommand, UUID> {

    private final ContractPartiesApi contractPartiesApi;

    public AddPartyToContractHandler(ContractPartiesApi contractPartiesApi) {
        this.contractPartiesApi = contractPartiesApi;
    }

    @Override
    protected Mono<UUID> doHandle(AddPartyToContractCommand cmd) {
        ContractPartyDTO partyDTO = new ContractPartyDTO();
        partyDTO.setContractId(cmd.getContractId());
        partyDTO.setPartyId(cmd.getPartyId());
        partyDTO.setRoleInContractId(cmd.getRoleInContractId());
        partyDTO.setDateJoined(LocalDateTime.now());
        partyDTO.setIsActive(true);

        return contractPartiesApi.createContractParty(cmd.getContractId(), partyDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getContractPartyId());
    }
}
