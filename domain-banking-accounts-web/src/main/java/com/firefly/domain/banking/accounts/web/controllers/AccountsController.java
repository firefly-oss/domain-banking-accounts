package com.firefly.domain.banking.accounts.web.controllers;

import com.firefly.domain.banking.accounts.core.account.commands.CloseAccountCommand;
import com.firefly.domain.banking.accounts.core.account.commands.OpenAccountCommand;
import com.firefly.domain.banking.accounts.core.account.services.AccountService;
import com.firefly.domain.banking.accounts.interfaces.dtos.OpenAccountResponse;
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

    @PostMapping
    @Operation(summary = "Open a new account", description = "Opens a new bank account with contract, party, balance, and space setup")
    public Mono<ResponseEntity<OpenAccountResponse>> openAccount(@Valid @RequestBody OpenAccountCommand command) {
        return accountService.openAccount(command)
                .map(result -> {
                    UUID accountId = result.resultOf("create-account", UUID.class).orElse(null);
                    UUID contractId = result.resultOf("create-contract", UUID.class).orElse(null);
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
}
