package io.fireflysoftware.domain.banking.accounts.core.holder.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
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

import static io.fireflysoftware.domain.banking.accounts.core.holder.utils.AddAccountHolderConstants.SAGA_ADD_ACCOUNT_HOLDER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountHolderServiceImpl Tests")
class AccountHolderServiceImplTest {

    @Mock
    private SagaEngine sagaEngine;

    @Mock
    private SagaResult sagaResult;

    private AccountHolderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountHolderServiceImpl(sagaEngine);
    }

    @Test
    @DisplayName("Should add holder successfully")
    void testAddHolder_ShouldExecuteSaga() {
        // Given
        AddAccountHolderCommand command = AddAccountHolderCommand.builder()
                .accountId(UUID.randomUUID())
                .partyId(UUID.randomUUID())
                .roleInContractId(UUID.randomUUID())
                .build();

        when(sagaEngine.execute(eq(SAGA_ADD_ACCOUNT_HOLDER_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.addHolder(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_ADD_ACCOUNT_HOLDER_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should handle saga execution errors")
    void testAddHolder_ShouldHandleErrors() {
        // Given
        AddAccountHolderCommand command = AddAccountHolderCommand.builder()
                .accountId(UUID.randomUUID())
                .partyId(UUID.randomUUID())
                .roleInContractId(UUID.randomUUID())
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_ADD_ACCOUNT_HOLDER_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        // When
        Mono<SagaResult> result = service.addHolder(command);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_ADD_ACCOUNT_HOLDER_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Constructor should set saga engine dependency")
    void testConstructor_ShouldSetSagaEngine() {
        // When
        AccountHolderServiceImpl newService = new AccountHolderServiceImpl(sagaEngine);

        // Then
        assertNotNull(newService);
    }
}
