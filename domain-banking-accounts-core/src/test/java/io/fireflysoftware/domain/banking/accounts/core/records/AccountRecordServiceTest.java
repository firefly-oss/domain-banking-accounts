/*
 * Copyright 2026 Firefly Software Foundation
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

package io.fireflysoftware.domain.banking.accounts.core.records;

import io.fireflysoftware.core.banking.accounts.sdk.api.DepositProductApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.ServicingMandateApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountLimitDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountStatementDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountSweepDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.ServicingMandateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecordServiceTest {

    @Mock
    private DepositProductApi products;
    @Mock
    private ServicingMandateApi mandates;

    @Test
    void statementLimitSweepAndMandateGoThroughTheAccountsCoreSdk() {
        AccountRecordService service = new AccountRecordService(products, mandates);
        UUID accountId = UUID.randomUUID();
        UUID statementId = UUID.randomUUID();
        UUID limitId = UUID.randomUUID();
        UUID sweepId = UUID.randomUUID();
        UUID mandateId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        AccountStatementDTO statement = new AccountStatementDTO().accountStatementId(statementId).currency("USD");
        AccountLimitDTO limit = new AccountLimitDTO().accountLimitId(limitId).withdrawalLimit(new BigDecimal("50.00"));
        AccountSweepDTO sweep = new AccountSweepDTO().accountSweepId(sweepId).amount(new BigDecimal("15.00"));
        ServicingMandateDTO mandate = new ServicingMandateDTO().servicingMandateId(mandateId).partyId(partyId);

        when(products.recordAccountStatement(eq(accountId), any(), any())).thenReturn(Mono.just(statement));
        when(products.getAccountStatement(eq(accountId), eq(statementId), any())).thenReturn(Mono.just(statement));
        when(products.recordAccountLimit(eq(accountId), any(), any())).thenReturn(Mono.just(limit));
        when(products.getAccountLimit(eq(accountId), eq(limitId), any())).thenReturn(Mono.just(limit));
        when(products.recordAccountSweep(eq(accountId), any(), any())).thenReturn(Mono.just(sweep));
        when(products.getAccountSweep(eq(accountId), eq(sweepId), any())).thenReturn(Mono.just(sweep));
        when(mandates.createServicingMandate(eq(accountId), any(), any())).thenReturn(Mono.just(mandate));
        when(mandates.getServicingMandate(eq(accountId), eq(mandateId), any())).thenReturn(Mono.just(mandate));

        UUID closed = service.closeStatement(accountId, new AccountRecordRequests.StatementClose(
                "USD", new BigDecimal("100.00"), List.of(new BigDecimal("25.00")))).block();
        AccountStatementDTO readStatement = service.statement(accountId, closed).block();
        UUID limited = service.recordWithdrawalLimit(accountId, new AccountRecordRequests.WithdrawalLimit(
                "USD", new BigDecimal("50.00"), new BigDecimal("10.00"),
                new BigDecimal("40.00"), new BigDecimal("45.00"))).block();
        AccountLimitDTO readLimit = service.withdrawalLimit(accountId, limited).block();
        UUID swept = service.recordSpaceSweep(accountId, new AccountRecordRequests.SpaceSweep(
                source, target, new BigDecimal("15.00"), "USD")).block();
        AccountSweepDTO readSweep = service.spaceSweep(accountId, swept).block();
        UUID granted = service.createServicingMandate(accountId, new AccountRecordRequests.ServicingMandate(
                partyId, "OPERATOR", true)).block();
        ServicingMandateDTO readMandate = service.servicingMandate(accountId, granted).block();

        assertThat(readStatement.getAccountStatementId()).isEqualTo(statementId);
        assertThat(readLimit.getWithdrawalLimit()).isEqualByComparingTo("50.00");
        assertThat(readSweep.getAmount()).isEqualByComparingTo("15.00");
        assertThat(readMandate.getPartyId()).isEqualTo(partyId);
        verify(products).recordAccountStatement(eq(accountId), any(), any());
        verify(products).getAccountStatement(eq(accountId), eq(statementId), any());
        verify(products).recordAccountLimit(eq(accountId), any(), any());
        verify(products).recordAccountSweep(eq(accountId), any(), any());
        verify(mandates).createServicingMandate(eq(accountId), any(), any());
        verify(mandates).getServicingMandate(eq(accountId), eq(mandateId), any());
    }
}
