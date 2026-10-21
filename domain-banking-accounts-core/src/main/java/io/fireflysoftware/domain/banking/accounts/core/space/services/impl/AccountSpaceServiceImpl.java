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


package io.fireflysoftware.domain.banking.accounts.core.space.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
import io.fireflysoftware.domain.banking.accounts.core.space.services.AccountSpaceService;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.*;

@Service
public class AccountSpaceServiceImpl implements AccountSpaceService {

    private final SagaEngine engine;

    public AccountSpaceServiceImpl(SagaEngine engine) {
        this.engine = engine;
    }

    @Override
    public Mono<SagaResult> configureSpace(CreateAccountSpaceCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_CREATE_SPACE, command)
                .forStepId(STEP_CONFIG_AUTO_TRANSFER, command)
                .build();

        return engine.execute(SAGA_CONFIGURE_SPACES_NAME, inputs);
    }
}
