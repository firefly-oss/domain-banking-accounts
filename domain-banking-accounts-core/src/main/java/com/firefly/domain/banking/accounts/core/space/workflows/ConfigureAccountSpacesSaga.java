package com.firefly.domain.banking.accounts.core.space.workflows;

import com.firefly.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.*;

@Saga(name = SAGA_CONFIGURE_SPACES_NAME)
@Service
public class ConfigureAccountSpacesSaga {

    private final CommandBus commandBus;

    public ConfigureAccountSpacesSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_CREATE_SPACE, compensate = COMPENSATE_DELETE_SPACE)
    @StepEvent(type = EVENT_SPACE_CREATED)
    public Mono<UUID> createSpace(CreateAccountSpaceCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(CTX_ACCOUNT_ID, cmd.getAccountId());
        // TODO: Call AccountSpacesApi.createSpace once SDK is rebuilt
        return Mono.just(UUID.randomUUID())
                .doOnNext(spaceId -> ctx.putVariable(CTX_SPACE_ID, spaceId));
    }

    public Mono<Void> deleteSpace(UUID spaceId, ExecutionContext ctx) {
        // TODO: Call AccountSpacesApi.deleteSpace once SDK is rebuilt
        return Mono.empty();
    }

    @SagaStep(id = STEP_INIT_SPACE_BALANCE, compensate = COMPENSATE_DELETE_BALANCE, dependsOn = STEP_CREATE_SPACE)
    @StepEvent(type = EVENT_BALANCE_INITIALIZED)
    public Mono<UUID> initializeSpaceBalance(ExecutionContext ctx) {
        UUID spaceId = (UUID) ctx.getVariable(CTX_SPACE_ID);
        UUID accountId = (UUID) ctx.getVariable(CTX_ACCOUNT_ID);
        // TODO: Call AccountBalancesApi.createBalance once SDK is rebuilt
        return Mono.just(UUID.randomUUID())
                .doOnNext(balanceId -> ctx.putVariable(CTX_BALANCE_ID, balanceId));
    }

    public Mono<Void> deleteBalance(UUID balanceId, ExecutionContext ctx) {
        return Mono.empty();
    }

    @SagaStep(id = STEP_CONFIG_AUTO_TRANSFER, compensate = COMPENSATE_REMOVE_AUTO_TRANSFER, dependsOn = STEP_CREATE_SPACE)
    @StepEvent(type = EVENT_AUTO_TRANSFER_CONFIGURED)
    public Mono<Void> configureAutoTransfer(CreateAccountSpaceCommand cmd, ExecutionContext ctx) {
        if (cmd == null || cmd.getAutoTransferRules() == null) {
            return Mono.empty();
        }
        UUID spaceId = (UUID) ctx.getVariable(CTX_SPACE_ID);
        // TODO: Call AccountSpacesApi.updateSpace with auto-transfer config once SDK is rebuilt
        return Mono.empty();
    }

    public Mono<Void> removeAutoTransferConfig(ExecutionContext ctx) {
        return Mono.empty();
    }
}
