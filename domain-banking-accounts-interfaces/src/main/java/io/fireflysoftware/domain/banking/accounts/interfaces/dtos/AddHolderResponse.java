package io.fireflysoftware.domain.banking.accounts.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Response after adding an account holder")
public class AddHolderResponse {

    @Schema(description = "The unique identifier of the contract-party link")
    private UUID contractPartyId;

    @Schema(description = "The execution ID of the saga")
    private String executionId;

    @Schema(description = "The status of the saga execution")
    private String status;
}
