package io.fireflysoftware.domain.banking.accounts.web.controllers;

import io.fireflysoftware.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.services.AccountHolderService;
import io.fireflysoftware.domain.banking.accounts.interfaces.dtos.AddHolderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/holders")
@RequiredArgsConstructor
@Tag(name = "Account Holders", description = "Manage account holders and their roles")
public class AccountHoldersController {

    private final AccountHolderService accountHolderService;

    @PostMapping
    @Operation(summary = "Add an account holder", description = "Adds a party as a holder to an existing account contract")
    public Mono<ResponseEntity<AddHolderResponse>> addHolder(
            @PathVariable UUID accountId,
            @Valid @RequestBody AddAccountHolderCommand command) {
        AddAccountHolderCommand enrichedCommand = AddAccountHolderCommand.builder()
                .accountId(accountId)
                .partyId(command.getPartyId())
                .roleInContractId(command.getRoleInContractId())
                .build();
        return accountHolderService.addHolder(enrichedCommand)
                .map(result -> {
                    UUID contractPartyId = result.resultOf("add-party", UUID.class).orElse(null);
                    return ResponseEntity.ok(AddHolderResponse.builder()
                            .contractPartyId(contractPartyId)
                            .executionId(result.correlationId())
                            .status(result.isSuccess() ? "COMPLETED" : "FAILED")
                            .build());
                });
    }
}
