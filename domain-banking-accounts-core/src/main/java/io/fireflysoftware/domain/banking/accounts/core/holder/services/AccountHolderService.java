package io.fireflysoftware.domain.banking.accounts.core.holder.services;

import io.fireflysoftware.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

public interface AccountHolderService {
    Mono<SagaResult> addHolder(AddAccountHolderCommand command);
}
