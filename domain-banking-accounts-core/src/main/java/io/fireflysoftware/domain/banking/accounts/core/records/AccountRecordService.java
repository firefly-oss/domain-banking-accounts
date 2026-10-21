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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Statement close, withdrawal limit, space sweep, and servicing mandate.
 * The accounts core already stores these rows. This service calls that core SDK.
 */
@Service
public class AccountRecordService {

    private final DepositProductApi products;
    private final ServicingMandateApi mandates;

    public AccountRecordService(DepositProductApi products, ServicingMandateApi mandates) {
        this.products = products;
        this.mandates = mandates;
    }

    public Mono<UUID> closeStatement(UUID accountId, AccountRecordRequests.StatementClose request) {
        AccountStatementDTO body = new AccountStatementDTO()
                .currency(request.currency())
                .openingBalance(request.openingBalance())
                .lineAmounts(request.lineAmounts() == null ? List.of() : request.lineAmounts());
        return products.recordAccountStatement(accountId, body, key())
                .map(AccountStatementDTO::getAccountStatementId);
    }

    public Mono<AccountStatementDTO> statement(UUID accountId, UUID statementId) {
        return products.getAccountStatement(accountId, statementId, key());
    }

    public Mono<UUID> recordWithdrawalLimit(UUID accountId, AccountRecordRequests.WithdrawalLimit request) {
        AccountLimitDTO body = new AccountLimitDTO()
                .currency(request.currency())
                .withdrawalLimit(request.withdrawalLimit())
                .overdraftLimit(request.overdraftLimit())
                .balance(request.balance())
                .withdrawalAmount(request.withdrawalAmount());
        return products.recordAccountLimit(accountId, body, key())
                .map(AccountLimitDTO::getAccountLimitId);
    }

    public Mono<AccountLimitDTO> withdrawalLimit(UUID accountId, UUID limitId) {
        return products.getAccountLimit(accountId, limitId, key());
    }

    public Mono<UUID> recordSpaceSweep(UUID accountId, AccountRecordRequests.SpaceSweep request) {
        AccountSweepDTO body = new AccountSweepDTO()
                .sourceSpaceId(request.sourceSpaceId())
                .targetSpaceId(request.targetSpaceId())
                .amount(request.amount())
                .currency(request.currency());
        return products.recordAccountSweep(accountId, body, key())
                .map(AccountSweepDTO::getAccountSweepId);
    }

    public Mono<AccountSweepDTO> spaceSweep(UUID accountId, UUID sweepId) {
        return products.getAccountSweep(accountId, sweepId, key());
    }

    public Mono<UUID> createServicingMandate(UUID accountId, AccountRecordRequests.ServicingMandate request) {
        ServicingMandateDTO body = new ServicingMandateDTO()
                .partyId(request.partyId())
                .role(role(request.role()))
                .active(request.active() == null || request.active());
        return mandates.createServicingMandate(accountId, body, key())
                .map(ServicingMandateDTO::getServicingMandateId);
    }

    public Mono<ServicingMandateDTO> servicingMandate(UUID accountId, UUID mandateId) {
        return mandates.getServicingMandate(accountId, mandateId, key());
    }

    private static ServicingMandateDTO.RoleEnum role(String role) {
        if (role == null || role.isBlank()) {
            return ServicingMandateDTO.RoleEnum.OPERATOR;
        }
        return ServicingMandateDTO.RoleEnum.fromValue(role);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
