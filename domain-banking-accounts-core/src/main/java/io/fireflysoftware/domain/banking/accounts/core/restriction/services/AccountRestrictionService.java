package io.fireflysoftware.domain.banking.accounts.core.restriction.services;

import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

public interface AccountRestrictionService {
    Mono<SagaResult> applyRestriction(ApplyRestrictionCommand command);
    Mono<Void> removeRestriction(RemoveRestrictionCommand command);
}
