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


package io.fireflysoftware.domain.banking.accounts.core.sweep;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpaceTransactionsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpacesApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountSpaceDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.SpaceTransactionDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceSweepTest {

    @Mock private AccountSpacesApi spaces;
    @Mock private AccountSpaceTransactionsApi transactions;

    @Test
    void sweepMovesBothSpacesAndPostsABalancedLedgerPair() {
        UUID accountId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AccountSpaceDTO source = new AccountSpaceDTO().accountId(accountId)
                .balance(new BigDecimal("80.00")).isFrozen(false);
        AccountSpaceDTO target = new AccountSpaceDTO().accountId(accountId)
                .balance(new BigDecimal("20.00")).isFrozen(false);
        when(spaces.getAccountSpace(eq(sourceId), any())).thenReturn(Mono.just(source));
        when(spaces.getAccountSpace(eq(targetId), any())).thenReturn(Mono.just(target));
        when(transactions.recordTransaction(eq(sourceId), eq(new BigDecimal("25.00").negate()), any(), any(), any()))
                .thenReturn(Mono.just(new SpaceTransactionDTO()));
        when(transactions.recordTransaction(eq(targetId), eq(new BigDecimal("25.00")), any(), any(), any()))
                .thenReturn(Mono.just(new SpaceTransactionDTO()));
        SandboxLedgerAdapter ledger = new SandboxLedgerAdapter();

        UUID posting = new SweepSpacesService(spaces, transactions, ledger)
                .sweep(accountId, sourceId, targetId, new BigDecimal("25.00"), "USD").block();

        assertThat(posting).isNotNull();
        verify(transactions).recordTransaction(eq(sourceId), eq(new BigDecimal("-25.00")), any(), any(), any());
        verify(transactions).recordTransaction(eq(targetId), eq(new BigDecimal("25.00")), any(), any(), any());
    }

    @Test
    void sweepRejectsSpacesOfDifferentAccounts() {
        UUID left = UUID.randomUUID();
        UUID right = UUID.randomUUID();
        assertThatThrownBy(() -> SpaceSweep.plan(left, left, right, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10"), new BigDecimal("1"), false, false, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one account");
    }
}
