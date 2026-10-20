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
