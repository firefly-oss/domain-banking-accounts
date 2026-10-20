package io.fireflysoftware.domain.banking.accounts.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Response after configuring an account space")
public class ConfigureSpaceResponse {

    @Schema(description = "The unique identifier of the created space")
    private UUID spaceId;

    @Schema(description = "The execution ID of the saga")
    private String executionId;

    @Schema(description = "The status of the saga execution")
    private String status;
}
