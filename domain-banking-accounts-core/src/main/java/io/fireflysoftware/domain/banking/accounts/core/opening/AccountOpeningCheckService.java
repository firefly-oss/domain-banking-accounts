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


package io.fireflysoftware.domain.banking.accounts.core.opening;

import io.fireflysoftware.domain.banking.accounts.core.opening.commands.InspectDocumentsCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.PersistOpeningEligibilityCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.ResolveOpeningEligibilityCommand;
import io.fireflysoftware.domain.banking.accounts.core.opening.workflows.AccountOpeningCheckSaga;
import io.fireflysoftware.domain.banking.accounts.core.opening.workflows.AccountOpeningDocumentSaga;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class AccountOpeningCheckService {

    private final SagaEngine engine;

    public AccountOpeningCheckService(SagaEngine engine) {
        this.engine = engine;
    }

    public Mono<EligibilityDecision> recordEligibility(UUID partyId, UUID productId) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(AccountOpeningCheckSaga.STEP_RESOLVE, new ResolveOpeningEligibilityCommand(partyId, productId))
                .forStepId(AccountOpeningCheckSaga.STEP_PERSIST, new PersistOpeningEligibilityCommand(partyId, productId, false))
                .build();
        return engine.execute(AccountOpeningCheckSaga.NAME, inputs)
                .flatMap(result -> decision(result, AccountOpeningCheckSaga.STEP_PERSIST, EligibilityDecision.class,
                        "Eligibility was not recorded"));
    }

    public Mono<DocumentVerificationDecision> recordDocumentVerification(UUID partyId) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(AccountOpeningDocumentSaga.STEP_INSPECT, new InspectDocumentsCommand(partyId))
                .forStepId(AccountOpeningDocumentSaga.STEP_RECORD, new InspectDocumentsCommand(partyId))
                .build();
        return engine.execute(AccountOpeningDocumentSaga.NAME, inputs)
                .flatMap(result -> decision(result, AccountOpeningDocumentSaga.STEP_RECORD, DocumentVerificationDecision.class,
                        "Document verification was not recorded"));
    }

    private static <T> Mono<T> decision(SagaResult result, String step, Class<T> type, String failure) {
        if (result == null || !result.isSuccess()) {
            return Mono.error(new IllegalStateException(failure));
        }
        return Mono.justOrEmpty(result.resultOf(step, type).orElse(null))
                .switchIfEmpty(Mono.error(new IllegalStateException(failure)));
    }
}
