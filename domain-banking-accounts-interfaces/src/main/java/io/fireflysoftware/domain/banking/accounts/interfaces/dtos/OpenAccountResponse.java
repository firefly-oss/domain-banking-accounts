package io.fireflysoftware.domain.banking.accounts.interfaces.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Response after opening an account")
public class OpenAccountResponse {

    @Schema(description = "The unique identifier of the created account")
    private UUID accountId;

    @Schema(description = "The unique identifier of the associated contract")
    private UUID contractId;

    @Schema(description = "The execution ID of the saga that opened the account")
    private String executionId;

    @Schema(description = "The status of the saga execution")
    private String status;
}
