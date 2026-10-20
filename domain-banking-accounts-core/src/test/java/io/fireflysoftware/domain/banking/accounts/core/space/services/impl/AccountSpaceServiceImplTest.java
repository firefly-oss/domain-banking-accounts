package io.fireflysoftware.domain.banking.accounts.core.space.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
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

import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.space.utils.ConfigureSpacesConstants.SAGA_CONFIGURE_SPACES_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountSpaceServiceImpl Tests")
class AccountSpaceServiceImplTest {

    @Mock
    private SagaEngine sagaEngine;

    @Mock
    private SagaResult sagaResult;

    private AccountSpaceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountSpaceServiceImpl(sagaEngine);
    }

    @Test
    @DisplayName("Should configure space successfully")
    void testConfigureSpace_ShouldExecuteSaga() {
        // Given
        CreateAccountSpaceCommand command = CreateAccountSpaceCommand.builder()
                .accountId(UUID.randomUUID())
                .spaceType("SAVINGS_GOAL")
                .name("Vacation Fund")
                .build();

        when(sagaEngine.execute(eq(SAGA_CONFIGURE_SPACES_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.configureSpace(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_CONFIGURE_SPACES_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should handle saga execution errors")
    void testConfigureSpace_ShouldHandleErrors() {
        // Given
        CreateAccountSpaceCommand command = CreateAccountSpaceCommand.builder()
                .accountId(UUID.randomUUID())
                .spaceType("SAVINGS_GOAL")
                .name("Vacation Fund")
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_CONFIGURE_SPACES_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        // When
        Mono<SagaResult> result = service.configureSpace(command);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_CONFIGURE_SPACES_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Constructor should set saga engine dependency")
    void testConstructor_ShouldSetSagaEngine() {
        // When
        AccountSpaceServiceImpl newService = new AccountSpaceServiceImpl(sagaEngine);

        // Then
        assertNotNull(newService);
    }
}
