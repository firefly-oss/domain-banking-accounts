package io.fireflysoftware.domain.banking.accounts.core.holder.queries;

import io.fireflysoftware.core.contract.sdk.model.PaginationResponse;
import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
public class GetAccountHoldersQuery implements Query<PaginationResponse> {
    private UUID accountId;
}
