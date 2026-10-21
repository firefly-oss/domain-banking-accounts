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


package io.fireflysoftware.domain.banking.accounts.core.account.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.OpenAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.services.AccountService;
import org.fireflyframework.orchestration.saga.engine.ExpandEach;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.SAGA_CLOSE_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.*;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.SAGA_OPEN_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.*;

@Service
public class AccountServiceImpl implements AccountService {

    private final SagaEngine engine;

    public AccountServiceImpl(SagaEngine engine) {
        this.engine = engine;
    }

    @Override
    public Mono<SagaResult> openAccount(OpenAccountCommand command) {
        StepInputs.Builder builder = StepInputs.builder()
                .forStepId(STEP_CREATE_CONTRACT, command.toContractCommand())
                .forStepId(STEP_ADD_PARTY, command.toPartyCommand())
                .forStepId(STEP_CREATE_ACCOUNT, command.toAccountCommand())
                .forStepId(STEP_POST_OPENING_LEDGER, command.toOpeningLedgerInput())
                .forStepId(STEP_CREATE_SPACE, command.toSpaceCommand())
                .forStepId(STEP_RECORD_STATUS, command.toStatusCommand());

        if (command.getDefaultParameters() != null && !command.getDefaultParameters().isEmpty()) {
            builder.forStepId(STEP_SET_PARAMS, ExpandEach.of(command.toParameterCommands()));
        }

        return engine.execute(SAGA_OPEN_ACCOUNT_NAME, builder.build());
    }

    @Override
    public Mono<SagaResult> closeAccount(CloseAccountCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_VALIDATE_BALANCE, command)
                .forStepId(STEP_SETTLE, command)
                .forStepId(STEP_REMOVE_SPACES, command)
                .forStepId(STEP_UPDATE_STATUS, command)
                .forStepId(STEP_TERMINATE_CONTRACT, command)
                .build();

        return engine.execute(SAGA_CLOSE_ACCOUNT_NAME, inputs);
    }

    @Override
    public Mono<UUID> getAccountId(SagaResult result) {
        return Mono.justOrEmpty(result.resultOf(STEP_CREATE_ACCOUNT, UUID.class).orElse(null));
    }
}
