package com.firefly.domain.banking.accounts.core.restriction.workflows;

import com.firefly.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import com.firefly.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.*;

@Saga(name = SAGA_APPLY_RESTRICTION_NAME)
@Service
public class ApplyAccountRestrictionSaga {

    private static final Set<String> STATUS_CHANGING_RESTRICTIONS = Set.of(
            "ACCOUNT_FREEZE", "LEGAL_ORDER", "FRAUD_INVESTIGATION"
    );

    private final CommandBus commandBus;

    public ApplyAccountRestrictionSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_CREATE_RESTRICTION, compensate = COMPENSATE_REMOVE_RESTRICTION)
    @StepEvent(type = EVENT_RESTRICTION_CREATED)
    public Mono<UUID> createRestriction(ApplyRestrictionCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        // TODO: Call AccountRestrictionsApi.createRestriction once SDK is rebuilt
        return Mono.just(UUID.randomUUID())
                .doOnNext(restrictionId -> ctx.putVariable(CTX_RESTRICTION_ID, restrictionId));
    }

    public Mono<Void> removeRestriction(UUID restrictionId, ExecutionContext ctx) {
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        return commandBus.<Void>send(RemoveRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionId(restrictionId)
                .build());
    }

    @SagaStep(id = STEP_UPDATE_STATUS, compensate = COMPENSATE_REVERT_STATUS, dependsOn = STEP_CREATE_RESTRICTION)
    @StepEvent(type = EVENT_STATUS_UPDATED)
    public Mono<Void> updateAccountStatus(ApplyRestrictionCommand cmd, ExecutionContext ctx) {
        if (!STATUS_CHANGING_RESTRICTIONS.contains(cmd.getRestrictionType())) {
            return Mono.empty();
        }
        // TODO: Store previous status and update to SUSPENDED
        ctx.putVariable(CTX_PREVIOUS_STATUS, "OPEN");
        return Mono.empty();
    }

    public Mono<Void> revertAccountStatus(ExecutionContext ctx) {
        String previousStatus = (String) ctx.getVariable(CTX_PREVIOUS_STATUS);
        if (previousStatus == null) {
            return Mono.empty();
        }
        // TODO: Revert to previous status
        return Mono.empty();
    }

    @SagaStep(id = STEP_NOTIFY, dependsOn = STEP_CREATE_RESTRICTION)
    @StepEvent(type = EVENT_HOLDER_NOTIFIED)
    public Mono<Void> notifyHolder(ApplyRestrictionCommand cmd, ExecutionContext ctx) {
        // TODO: Call AccountNotificationsApi.createNotification
        return Mono.empty();
    }
}
