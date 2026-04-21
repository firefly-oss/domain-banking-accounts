package com.firefly.domain.banking.accounts.core.account.queries;

import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
public class GetAccountStatusHistoryQuery implements Query<PaginationResponse> {
    private UUID accountId;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
