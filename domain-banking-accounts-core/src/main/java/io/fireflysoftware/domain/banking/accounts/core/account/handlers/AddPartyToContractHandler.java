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
