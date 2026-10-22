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

import io.fireflysoftware.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.commands.OpenAccountCommand;
import io.fireflysoftware.domain.banking.accounts.core.account.services.AccountService;
import io.fireflysoftware.domain.banking.accounts.core.opening.AccountOpeningCheckService;
import io.fireflysoftware.domain.banking.accounts.core.opening.DocumentVerificationDecision;
import io.fireflysoftware.domain.banking.accounts.core.opening.EligibilityDecision;
import io.fireflysoftware.domain.banking.accounts.interfaces.dtos.OpenAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account lifecycle management - open, close, and query accounts")
public class AccountsController {

    private final AccountService accountService;
    private final AccountOpeningCheckService accountOpeningCheckService;

    @PostMapping
    @Operation(summary = "Open a new account", description = "Opens a new bank account with contract, party, balance, and space setup")
    public Mono<ResponseEntity<OpenAccountResponse>> openAccount(@Valid @RequestBody OpenAccountCommand command) {
        return accountService.openAccount(command)
                .map(result -> {
                    UUID accountId = result.resultOf("createAccount", UUID.class).orElse(null);
                    UUID contractId = result.resultOf("createContract", UUID.class).orElse(null);
                    return ResponseEntity.ok(OpenAccountResponse.builder()
                            .accountId(accountId)
                            .contractId(contractId)
                            .executionId(result.correlationId())
                            .status(result.isSuccess() ? "COMPLETED" : "FAILED")
                            .build());
                });
    }

    @PostMapping("/{accountId}/close")
    @Operation(summary = "Close an account", description = "Initiates the account closure process")
    public Mono<ResponseEntity<Void>> closeAccount(
            @PathVariable UUID accountId,
            @RequestParam(required = false, defaultValue = "Customer requested closure") String reason) {
        CloseAccountCommand command = CloseAccountCommand.builder()
                .accountId(accountId)
                .reason(reason)
                .build();
        return accountService.closeAccount(command)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/opening/eligibility")
    @Operation(summary = "Record account-opening eligibility",
            description = "Checks the party and product through their cores and stores the KYC verification that records the result.")
    public Mono<ResponseEntity<EligibilityDecision>> recordEligibility(@Valid @RequestBody EligibilityRequest request) {
        return accountOpeningCheckService.recordEligibility(request.partyId(), request.productId())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/opening/documents")
    @Operation(summary = "Record document verification",
            description = "Reads the party's KYC documents and stores the verification status on the KYC record.")
    public Mono<ResponseEntity<DocumentVerificationDecision>> recordDocumentVerification(
            @Valid @RequestBody DocumentVerificationRequest request) {
        return accountOpeningCheckService.recordDocumentVerification(request.partyId())
                .map(ResponseEntity::ok);
    }

    public record EligibilityRequest(UUID partyId, UUID productId) {
    }

    public record DocumentVerificationRequest(UUID partyId) {
    }
}
