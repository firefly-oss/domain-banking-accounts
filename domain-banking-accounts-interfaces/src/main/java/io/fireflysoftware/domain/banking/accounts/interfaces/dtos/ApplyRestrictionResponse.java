package io.fireflysoftware.domain.banking.accounts.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Response after applying an account restriction")
public class ApplyRestrictionResponse {

    @Schema(description = "The unique identifier of the restriction")
    private UUID restrictionId;

    @Schema(description = "The execution ID of the saga")
    private String executionId;

    @Schema(description = "The status of the saga execution")
    private String status;
}
