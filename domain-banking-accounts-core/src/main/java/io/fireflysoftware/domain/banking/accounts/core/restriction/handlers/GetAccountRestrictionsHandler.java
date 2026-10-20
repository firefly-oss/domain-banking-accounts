package io.fireflysoftware.domain.banking.accounts.core.restriction.handlers;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountRestrictionsApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountRestrictionDTO;
import io.fireflysoftware.domain.banking.accounts.core.restriction.queries.GetAccountRestrictionsQuery;
import lombok.RequiredArgsConstructor;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@QueryHandlerComponent
@RequiredArgsConstructor
public class GetAccountRestrictionsHandler extends QueryHandler<GetAccountRestrictionsQuery, AccountRestrictionDTO> {

    private final AccountRestrictionsApi accountRestrictionsApi;

    @Override
    protected Mono<AccountRestrictionDTO> doHandle(GetAccountRestrictionsQuery query) {
        if (query.isActiveOnly()) {
            return accountRestrictionsApi.getActiveAccountRestrictionsByAccountId(
                    query.getAccountId(),
                    UUID.randomUUID().toString()
            );
        } else {
            return accountRestrictionsApi.getAccountRestrictionsByAccountId(
                    query.getAccountId(),
                    UUID.randomUUID().toString()
            );
        }
    }
}
