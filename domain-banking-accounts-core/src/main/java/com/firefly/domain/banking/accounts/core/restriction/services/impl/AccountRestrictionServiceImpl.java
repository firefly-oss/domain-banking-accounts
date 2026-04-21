package com.firefly.domain.banking.accounts.core.restriction.services.impl;

import com.firefly.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import com.firefly.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import com.firefly.domain.banking.accounts.core.restriction.services.AccountRestrictionService;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.*;

@Service
public class AccountRestrictionServiceImpl implements AccountRestrictionService {

    private final SagaEngine engine;
    private final CommandBus commandBus;

    public AccountRestrictionServiceImpl(SagaEngine engine, CommandBus commandBus) {
        this.engine = engine;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<SagaResult> applyRestriction(ApplyRestrictionCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_CREATE_RESTRICTION, command)
                .forStepId(STEP_UPDATE_STATUS, command)
                .forStepId(STEP_NOTIFY, command)
                .build();

        return engine.execute(SAGA_APPLY_RESTRICTION_NAME, inputs);
    }

    @Override
    public Mono<Void> removeRestriction(RemoveRestrictionCommand command) {
        return commandBus.send(command);
    }
}
