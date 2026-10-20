package io.fireflysoftware.domain.banking.accounts.core.space.queries;

import io.fireflysoftware.core.banking.accounts.sdk.model.PaginationResponseAccountSpaceDTO;
import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
public class GetAccountSpacesQuery implements Query<PaginationResponseAccountSpaceDTO> {
    private UUID accountId;
}
