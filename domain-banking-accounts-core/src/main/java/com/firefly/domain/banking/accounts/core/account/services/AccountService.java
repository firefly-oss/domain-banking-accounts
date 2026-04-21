package com.firefly.domain.banking.accounts.core.account.services;

import com.firefly.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import com.firefly.domain.banking.accounts.core.account.commands.OpenAccountCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountService {

    Mono<SagaResult> openAccount(OpenAccountCommand command);

    Mono<SagaResult> closeAccount(CloseAccountCommand command);

    Mono<UUID> getAccountId(SagaResult result);
}
