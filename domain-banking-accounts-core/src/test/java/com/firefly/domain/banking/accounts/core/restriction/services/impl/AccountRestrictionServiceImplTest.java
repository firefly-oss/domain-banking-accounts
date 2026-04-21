package com.firefly.domain.banking.accounts.core.restriction.services.impl;

import com.firefly.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import com.firefly.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.firefly.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.SAGA_APPLY_RESTRICTION_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRestrictionServiceImpl Tests")
class AccountRestrictionServiceImplTest {

    @Mock
    private SagaEngine sagaEngine;

    @Mock
    private CommandBus commandBus;

    @Mock
    private SagaResult sagaResult;

    private AccountRestrictionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountRestrictionServiceImpl(sagaEngine, commandBus);
    }

    @Test
    @DisplayName("Should apply restriction successfully")
    void testApplyRestriction_ShouldExecuteSaga() {
        // Given
        ApplyRestrictionCommand command = ApplyRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionType("DEBIT_BLOCK")
                .reason("Fraud investigation")
                .startDate(LocalDateTime.now())
                .build();

        when(sagaEngine.execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.applyRestriction(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should remove restriction successfully")
    void testRemoveRestriction_ShouldSendCommand() {
        // Given
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionId(UUID.randomUUID())
                .reason("Investigation completed")
                .build();

        when(commandBus.send(command)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = service.removeRestriction(command);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(commandBus).send(command);
    }

    @Test
    @DisplayName("Should handle saga execution errors")
    void testApplyRestriction_ShouldHandleErrors() {
        // Given
        ApplyRestrictionCommand command = ApplyRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionType("DEBIT_BLOCK")
                .reason("Fraud investigation")
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        // When
        Mono<SagaResult> result = service.applyRestriction(command);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should handle command bus errors")
    void testRemoveRestriction_ShouldHandleErrors() {
        // Given
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionId(UUID.randomUUID())
                .reason("Investigation completed")
                .build();

        RuntimeException error = new RuntimeException("Command execution failed");
        when(commandBus.send(command)).thenReturn(Mono.error(error));

        // When
        Mono<Void> result = service.removeRestriction(command);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(commandBus).send(command);
    }

    @Test
    @DisplayName("Constructor should set dependencies")
    void testConstructor_ShouldSetDependencies() {
        // When
        AccountRestrictionServiceImpl newService = new AccountRestrictionServiceImpl(sagaEngine, commandBus);

        // Then
        assertNotNull(newService);
    }
}
