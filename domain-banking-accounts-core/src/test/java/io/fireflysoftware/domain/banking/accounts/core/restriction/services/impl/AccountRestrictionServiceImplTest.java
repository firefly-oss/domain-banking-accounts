package io.fireflysoftware.domain.banking.accounts.core.restriction.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
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

import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.ApplyRestrictionConstants.SAGA_APPLY_RESTRICTION_NAME;
import static io.fireflysoftware.domain.banking.accounts.core.restriction.utils.RemoveRestrictionConstants.SAGA_REMOVE_RESTRICTION_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRestrictionServiceImpl Tests")
class AccountRestrictionServiceImplTest {

    @Mock
    private SagaEngine sagaEngine;

    @Mock
    private SagaResult sagaResult;

    private AccountRestrictionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountRestrictionServiceImpl(sagaEngine);
    }

    @Test
    @DisplayName("applyRestriction dispatches ApplyAccountRestrictionSaga")
    void testApplyRestriction_ShouldExecuteSaga() {
        ApplyRestrictionCommand command = ApplyRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionType("WITHDRAWAL_HOLD")
                .reason("Fraud investigation")
                .startDate(LocalDateTime.now())
                .build();

        when(sagaEngine.execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        StepVerifier.create(service.applyRestriction(command))
                .expectNext(sagaResult)
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("removeRestriction dispatches RemoveAccountRestrictionSaga")
    void testRemoveRestriction_ShouldExecuteSaga() {
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionId(UUID.randomUUID())
                .reason("Investigation completed")
                .build();

        when(sagaEngine.execute(eq(SAGA_REMOVE_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.just(sagaResult));

        StepVerifier.create(service.removeRestriction(command))
                .verifyComplete();

        verify(sagaEngine).execute(eq(SAGA_REMOVE_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("applyRestriction propagates saga errors")
    void testApplyRestriction_ShouldHandleErrors() {
        ApplyRestrictionCommand command = ApplyRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionType("WITHDRAWAL_HOLD")
                .reason("Fraud investigation")
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        StepVerifier.create(service.applyRestriction(command))
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_APPLY_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("removeRestriction propagates saga errors")
    void testRemoveRestriction_ShouldHandleErrors() {
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(UUID.randomUUID())
                .restrictionId(UUID.randomUUID())
                .reason("Investigation completed")
                .build();

        RuntimeException error = new RuntimeException("Saga execution failed");
        when(sagaEngine.execute(eq(SAGA_REMOVE_RESTRICTION_NAME), any(StepInputs.class)))
                .thenReturn(Mono.error(error));

        StepVerifier.create(service.removeRestriction(command))
                .expectError(RuntimeException.class)
                .verify();

        verify(sagaEngine).execute(eq(SAGA_REMOVE_RESTRICTION_NAME), any(StepInputs.class));
    }

    @Test
    @DisplayName("Constructor should set dependencies")
    void testConstructor_ShouldSetDependencies() {
        AccountRestrictionServiceImpl newService = new AccountRestrictionServiceImpl(sagaEngine);
        assertNotNull(newService);
    }
}
