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


package io.fireflysoftware.domain.banking.accounts.web.controllers;

import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponse;
import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import io.fireflysoftware.domain.banking.accounts.core.account.services.AccountQueryService;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.services.AccountRestrictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Backoffice", description = "Administrative operations for account management")
public class AccountBackofficeController {

    private final AccountQueryService accountQueryService;
    private final AccountRestrictionService accountRestrictionService;

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details", description = "Retrieves full account information for backoffice review")
    public Mono<ResponseEntity<AccountDTO>> getAccount(@PathVariable UUID accountId) {
        return accountQueryService.getAccount(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balances", description = "Retrieves current account balances")
    public Mono<ResponseEntity<PaginationResponse>> getAccountBalances(@PathVariable UUID accountId) {
        return accountQueryService.getAccountBalances(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/holders")
    @Operation(summary = "Get account holders", description = "Retrieves all holders associated with the account")
    public Mono<ResponseEntity<io.fireflysoftware.core.contract.sdk.model.PaginationResponse>> getAccountHolders(
            @PathVariable UUID accountId) {
        return accountQueryService.getAccountHolders(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/spaces")
    @Operation(summary = "Get account spaces", description = "Retrieves all spaces (sub-accounts) for the account")
    public Mono<ResponseEntity<PaginationResponseAccountSpaceDTO>> getAccountSpaces(@PathVariable UUID accountId) {
        return accountQueryService.getAccountSpaces(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/restrictions")
    @Operation(summary = "Get active restrictions", description = "Retrieves active restrictions on the account")
    public Mono<ResponseEntity<AccountRestrictionDTO>> getAccountRestrictions(@PathVariable UUID accountId) {
        return accountQueryService.getAccountRestrictions(accountId, true)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/restrictions/history")
    @Operation(summary = "Get restrictions history", description = "Retrieves all restrictions including historical")
    public Mono<ResponseEntity<AccountRestrictionDTO>> getAccountRestrictionsHistory(@PathVariable UUID accountId) {
        return accountQueryService.getAccountRestrictions(accountId, false)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/parameters")
    @Operation(summary = "Get account parameters", description = "Retrieves account configuration parameters")
    public Mono<ResponseEntity<PaginationResponse>> getAccountParameters(@PathVariable UUID accountId) {
        return accountQueryService.getAccountParameters(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/status-history")
    @Operation(summary = "Get status history", description = "Retrieves account status change history")
    public Mono<ResponseEntity<PaginationResponse>> getAccountStatusHistory(@PathVariable UUID accountId) {
        return accountQueryService.getAccountStatusHistory(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{accountId}/restrictions/{restrictionId}")
    @Operation(summary = "Remove restriction", description = "Removes a restriction from the account")
    public Mono<ResponseEntity<Void>> removeRestriction(
            @PathVariable UUID accountId,
            @PathVariable UUID restrictionId) {
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionId(restrictionId)
                .reason("Administrative removal")
                .build();
        return accountRestrictionService.removeRestriction(command)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
