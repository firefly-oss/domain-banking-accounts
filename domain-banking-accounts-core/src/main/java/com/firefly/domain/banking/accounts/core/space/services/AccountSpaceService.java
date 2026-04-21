package com.firefly.domain.banking.accounts.core.space.services;

import com.firefly.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

public interface AccountSpaceService {
    Mono<SagaResult> configureSpace(CreateAccountSpaceCommand command);
}
