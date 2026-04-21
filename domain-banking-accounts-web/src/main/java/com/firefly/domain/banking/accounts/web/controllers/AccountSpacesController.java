package com.firefly.domain.banking.accounts.web.controllers;

import com.firefly.domain.banking.accounts.core.space.commands.CreateAccountSpaceCommand;
import com.firefly.domain.banking.accounts.core.space.services.AccountSpaceService;
import com.firefly.domain.banking.accounts.interfaces.dtos.ConfigureSpaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/spaces")
@RequiredArgsConstructor
@Tag(name = "Account Spaces", description = "Manage account spaces (buckets) for organizing funds")
public class AccountSpacesController {

    private final AccountSpaceService accountSpaceService;

    @PostMapping
    @Operation(summary = "Create an account space", description = "Creates a new space (bucket) within an account for organizing funds")
    public Mono<ResponseEntity<ConfigureSpaceResponse>> createSpace(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreateAccountSpaceCommand command) {
        CreateAccountSpaceCommand enrichedCommand = CreateAccountSpaceCommand.builder()
                .accountId(accountId)
                .name(command.getName())
                .spaceType(command.getSpaceType())
                .build();
        return accountSpaceService.configureSpace(enrichedCommand)
                .map(result -> {
                    UUID spaceId = result.resultOf("create-space", UUID.class).orElse(null);
                    return ResponseEntity.ok(ConfigureSpaceResponse.builder()
                            .spaceId(spaceId)
                            .executionId(result.correlationId())
                            .status(result.isSuccess() ? "COMPLETED" : "FAILED")
                            .build());
                });
    }
}
