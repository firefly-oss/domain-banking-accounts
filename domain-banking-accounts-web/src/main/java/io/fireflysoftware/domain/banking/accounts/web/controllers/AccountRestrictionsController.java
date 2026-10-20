package io.fireflysoftware.domain.banking.accounts.web.controllers;

import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.ApplyRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.commands.RemoveRestrictionCommand;
import io.fireflysoftware.domain.banking.accounts.core.restriction.services.AccountRestrictionService;
import io.fireflysoftware.domain.banking.accounts.interfaces.dtos.ApplyRestrictionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/restrictions")
@RequiredArgsConstructor
@Tag(name = "Account Restrictions", description = "Manage account restrictions like blocks and holds")
public class AccountRestrictionsController {

    private final AccountRestrictionService accountRestrictionService;

    @PostMapping
    @Operation(summary = "Apply a restriction", description = "Applies a restriction (block, hold, freeze) to an account")
    public Mono<ResponseEntity<ApplyRestrictionResponse>> applyRestriction(
            @PathVariable UUID accountId,
            @Valid @RequestBody ApplyRestrictionCommand command) {
        ApplyRestrictionCommand enrichedCommand = ApplyRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionType(command.getRestrictionType())
                .reason(command.getReason())
                .build();
        return accountRestrictionService.applyRestriction(enrichedCommand)
                .map(result -> {
                    UUID restrictionId = result.resultOf("apply-restriction", UUID.class).orElse(null);
                    return ResponseEntity.ok(ApplyRestrictionResponse.builder()
                            .restrictionId(restrictionId)
                            .executionId(result.correlationId())
                            .status(result.isSuccess() ? "COMPLETED" : "FAILED")
                            .build());
                });
    }

    @DeleteMapping("/{restrictionId}")
    @Operation(summary = "Remove a restriction", description = "Removes an existing restriction from an account")
    public Mono<ResponseEntity<Void>> removeRestriction(
            @PathVariable UUID accountId,
            @PathVariable UUID restrictionId) {
        RemoveRestrictionCommand command = RemoveRestrictionCommand.builder()
                .accountId(accountId)
                .restrictionId(restrictionId)
                .build();
        return accountRestrictionService.removeRestriction(command)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
