package io.fireflysoftware.domain.banking.accounts.core.restriction.queries;

import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
public class GetAccountRestrictionsQuery implements Query<AccountRestrictionDTO> {
    private UUID accountId;
    private boolean activeOnly;
}
