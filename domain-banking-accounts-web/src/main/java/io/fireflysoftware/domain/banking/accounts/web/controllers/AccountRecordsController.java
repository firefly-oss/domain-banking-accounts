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

package io.fireflysoftware.domain.banking.accounts.web.controllers;

import io.fireflysoftware.domain.banking.accounts.core.records.AccountRecordRequests;
import io.fireflysoftware.domain.banking.accounts.core.records.AccountRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account-records")
public class AccountRecordsController {

    private final AccountRecordService records;

    public AccountRecordsController(AccountRecordService records) {
        this.records = records;
    }

    @PostMapping("/accounts/{accountId}/statements")
    public Mono<AccountRecordRequests.RecordedId> closeStatement(@PathVariable UUID accountId,
                                                                  @RequestBody AccountRecordRequests.StatementClose body) {
        return records.closeStatement(accountId, body).map(AccountRecordRequests.RecordedId::new);
    }

    @GetMapping("/accounts/{accountId}/statements/{statementId}")
    public Mono<AccountRecordRequests.RecordedId> statement(@PathVariable UUID accountId, @PathVariable UUID statementId) {
        return records.statement(accountId, statementId)
                .map(row -> new AccountRecordRequests.RecordedId(row.getAccountStatementId()));
    }

    @PostMapping("/accounts/{accountId}/limits")
    public Mono<AccountRecordRequests.RecordedId> withdrawalLimit(@PathVariable UUID accountId,
                                                                   @RequestBody AccountRecordRequests.WithdrawalLimit body) {
        return records.recordWithdrawalLimit(accountId, body).map(AccountRecordRequests.RecordedId::new);
    }

    @PostMapping("/accounts/{accountId}/sweeps")
    public Mono<AccountRecordRequests.RecordedId> spaceSweep(@PathVariable UUID accountId,
                                                              @RequestBody AccountRecordRequests.SpaceSweep body) {
        return records.recordSpaceSweep(accountId, body).map(AccountRecordRequests.RecordedId::new);
    }

    @PostMapping("/accounts/{accountId}/servicing-mandates")
    public Mono<AccountRecordRequests.RecordedId> servicingMandate(@PathVariable UUID accountId,
                                                                    @RequestBody AccountRecordRequests.ServicingMandate body) {
        return records.createServicingMandate(accountId, body).map(AccountRecordRequests.RecordedId::new);
    }
}
