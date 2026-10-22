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


package io.fireflysoftware.domain.banking.accounts.core.opening.workflows;

import io.fireflysoftware.domain.banking.accounts.core.opening.EligibilityDecision;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.PersistOpeningEligibilityCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.RemoveOpeningEligibilityCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.ResolveOpeningEligibilityCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Saga(name = AccountOpeningCheckSaga.NAME)
@Service
public class AccountOpeningCheckSaga {

    public static final String NAME = "account-opening-eligibility";
    public static final String STEP_RESOLVE = "resolve-eligibility";
    public static final String STEP_PERSIST = "persist-eligibility";

    private static final String VAR_ELIGIBLE = "eligible";

    private final CommandBus commandBus;

    public AccountOpeningCheckSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_RESOLVE)
    public Mono<Boolean> resolveEligibility(ResolveOpeningEligibilityCommand cmd, ExecutionContext ctx) {
        return commandBus.<Boolean>send(cmd)
                .doOnNext(eligible -> ctx.putVariable(VAR_ELIGIBLE, eligible));
    }

    @SagaStep(id = STEP_PERSIST, dependsOn = STEP_RESOLVE, compensate = "removeRecordedEligibility")
    public Mono<EligibilityDecision> persistEligibility(PersistOpeningEligibilityCommand cmd, ExecutionContext ctx) {
        boolean eligible = Boolean.TRUE.equals(ctx.getVariable(VAR_ELIGIBLE));
        return commandBus.<EligibilityDecision>send(new PersistOpeningEligibilityCommand(cmd.partyId(), cmd.productId(), eligible));
    }

    public Mono<Void> removeRecordedEligibility(EligibilityDecision decision) {
        if (decision == null || decision.verificationId() == null || decision.partyId() == null) {
            return Mono.empty();
        }
        return commandBus.<Void>send(new RemoveOpeningEligibilityCommand(decision.partyId(), decision.verificationId()));
    }
}
