package com.firefly.domain.banking.accounts.core.holder.services;

import com.firefly.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

public interface AccountHolderService {
    Mono<SagaResult> addHolder(AddAccountHolderCommand command);
}
