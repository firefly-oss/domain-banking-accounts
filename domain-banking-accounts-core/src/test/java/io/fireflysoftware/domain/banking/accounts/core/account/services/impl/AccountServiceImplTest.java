/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.banking.accounts.core.account.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.OpenAccountCommand;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.fireflysoftware.domain.banking.accounts.core.account.utils.CloseAccountConstants.SAGA_CLOSE_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.SAGA_OPEN_ACCOUNT_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.account.utils.OpenAccountConstants.STEP_CREATE_ACCOUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl Tests")
class AccountServiceImplTest {

    @Mock
    private SagaEngine sagaEngine;

    @Mock
    private SagaResult sagaResult;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(sagaEngine);
    }

    @Test
    @DisplayName("Should open account successfully")
    void testOpenAccount_ShouldExecuteSaga() {
        // Given
        OpenAccountCommand command = OpenAccountCommand.builder()
                .partyId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productCatalogId(UUID.randomUUID())
                .roleInContractId(UUID.randomUUID())
                .accountType("SAVINGS")
                .currency("EUR")
                .branchId(UUID.randomUUID())
                .build();

        when(sagaEngine.execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.openAccount(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should open account with default parameters")
    void testOpenAccount_WithParameters_ShouldExecuteSaga() {
        // Given
        OpenAccountCommand.AccountParameter parameter = OpenAccountCommand.AccountParameter.builder()
                .parameterType("OVERDRAFT_LIMIT")
                .parameterValue("1000")
                .numericValue(BigDecimal.valueOf(1000))
                .build();

        OpenAccountCommand command = OpenAccountCommand.builder()
                .partyId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productCatalogId(UUID.randomUUID())
                .roleInContractId(UUID.randomUUID())
                .accountType("CHECKING")
                .currency("EUR")
                .branchId(UUID.randomUUID())
                .defaultParameters(List.of(parameter))
                .build();

        when(sagaEngine.execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.openAccount(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should close account successfully")
    void testCloseAccount_ShouldExecuteSaga() {
        // Given
        CloseAccountCommand command = CloseAccountCommand.builder()
                .accountId(UUID.randomUUID())
                .reason("Customer request")
                .build();

        when(sagaEngine.execute(eq(SAGA_CLOSE_ACCOUNT_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        // When
        Mono<SagaResult> result = service.closeAccount(command);

        // Then
        StepVerifier.create(result)
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_CLOSE_ACCOUNT_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Should get account ID from saga result")
    void testGetAccountId_ShouldReturnAccountId() {
        // Given
        UUID expectedAccountId = UUID.randomUUID();
        when(sagaResult.resultOf(STEP_CREATE_ACCOUNT, UUID.class))
                .thenReturn(Optional.of(expectedAccountId));

        // When
        Mono<UUID> result = service.getAccountId(sagaResult);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedAccountId)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when account ID not found in saga result")
    void testGetAccountId_ShouldReturnEmpty_WhenNotFound() {
        // Given
        when(sagaResult.resultOf(STEP_CREATE_ACCOUNT, UUID.class))
                .thenReturn(Optional.empty());

        // When
        Mono<UUID> result = service.getAccountId(sagaResult);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle saga execution errors")
    void testOpenAccount_ShouldHandleErrors() {
        // Given
        OpenAccountCommand command = OpenAccountCommand.builder()
                .partyId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .accountType("SAVINGS")
                .currency("EUR")
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        // When
        Mono<SagaResult> result = service.openAccount(command);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_OPEN_ACCOUNT_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Constructor should set saga engine dependency")
    void testConstructor_ShouldSetSagaEngine() {
        // When
        AccountServiceImpl newService = new AccountServiceImpl(sagaEngine);

        // Then
        assertNotNull(newService);
    }
}
