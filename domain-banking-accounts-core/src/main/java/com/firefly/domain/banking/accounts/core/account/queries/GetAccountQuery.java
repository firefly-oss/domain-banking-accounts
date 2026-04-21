package com.firefly.domain.banking.accounts.core.account.queries;

import com.firefly.core.banking.accounts.sdk.model.AccountDTO;
import lombok.Builder;
import lombok.Data;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
public class GetAccountQuery implements Query<AccountDTO> {
    private UUID accountId;
}
