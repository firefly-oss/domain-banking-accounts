package io.fireflysoftware.domain.banking.accounts.core.restriction.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.services.AccountRestrictionService;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.SAGA_APPLY_RESTRICTION_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_CREATE_RESTRICTION;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.STEP_NOTIFY;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.SAGA_REMOVE_RESTRICTION_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.STEP_MARK_RESTRICTION_INACTIVE;

@Service
public class AccountRestrictionServiceImpl implements AccountRestrictionService {

    private final SagaEngine engine;

    public AccountRestrictionServiceImpl(SagaEngine engine) {
        this.engine = engine;
    }

    @Override
    public Mono<SagaResult> applyRestriction(ApplyRestrictionCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_CREATE_RESTRICTION, command)
                .forStepId(STEP_NOTIFY, command)
                .build();

        return engine.execute(SAGA_APPLY_RESTRICTION_NAME, inputs);
    }

    @Override
    public Mono<Void> removeRestriction(RemoveRestrictionCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_MARK_RESTRICTION_INACTIVE, command)
                .build();

        return engine.execute(SAGA_REMOVE_RESTRICTION_NAME, inputs).then();
    }
}
