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


package io.fireflysoftware.domain.banking.accounts.core.account.handlers;

import io.fireflysoftware.core.contract.sdk.api.ContractsApi;
import io.fireflysoftware.core.contract.sdk.model.ContractDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.CreateContractCommand;
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
