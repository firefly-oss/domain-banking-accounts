package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.contract.sdk.api.ContractsApi;
import com.firefly.core.contract.sdk.model.ContractDTO;
import com.firefly.domain.banking.accounts.core.account.commands.CreateContractCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class CreateContractHandler extends CommandHandler<CreateContractCommand, UUID> {

    private final ContractsApi contractsApi;

    public CreateContractHandler(ContractsApi contractsApi) {
        this.contractsApi = contractsApi;
    }

    @Override
    protected Mono<UUID> doHandle(CreateContractCommand cmd) {
        ContractDTO contractDTO = new ContractDTO();
        contractDTO.setProductId(cmd.getProductId());
        contractDTO.setProductCatalogId(cmd.getProductCatalogId());
        contractDTO.setContractStatus(ContractDTO.ContractStatusEnum.DRAFT);
        contractDTO.setStartDate(LocalDateTime.now());

        return contractsApi.createContract(contractDTO, UUID.randomUUID().toString())
                .mapNotNull(result -> Objects.requireNonNull(result).getContractId());
    }
}
