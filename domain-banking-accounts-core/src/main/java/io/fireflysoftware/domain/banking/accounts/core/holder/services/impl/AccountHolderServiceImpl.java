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


package io.fireflysoftware.domain.banking.accounts.core.holder.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.AddPartyToContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.GetAccountContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.services.AccountHolderService;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.banking.accounts.core.holder.utils.AddAccountHolderConstants.*;

@Service
public class AccountHolderServiceImpl implements AccountHolderService {

    private final SagaEngine engine;

    public AccountHolderServiceImpl(SagaEngine engine) {
        this.engine = engine;
    }

    @Override
    public Mono<SagaResult> addHolder(AddAccountHolderCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_GET_CONTRACT, GetAccountContractCommand.builder()
                        .accountId(command.getAccountId())
                        .build())
                .forStepId(STEP_ADD_PARTY, AddPartyToContractCommand.builder()
                        .partyId(command.getPartyId())
                        .roleInContractId(command.getRoleInContractId())
                        .build())
                .build();

        return engine.execute(SAGA_ADD_ACCOUNT_HOLDER_NAME, inputs);
    }
}
