package com.firefly.domain.banking.accounts.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Runtime toggle between the two balance-query implementations:
 *
 * <ul>
 *   <li>{@code LEDGER} (default) composes the {@code AccountBalanceDTO} from posted ledger
 *       legs, treating {@code account_balance} as a projection only.</li>
 *   <li>{@code SNAPSHOT} reads the {@code account_balance} projection as-is — useful as a
 *       rollback switch while the projection consumer catches up.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "firefly.balance")
@Data
public class BalanceQueryProperties {

    /** {@code LEDGER} (default) or {@code SNAPSHOT}. */
    private Source source = Source.LEDGER;

    /** Balance source — LEDGER recomputes from ledger legs; SNAPSHOT uses the projection. */
    public enum Source {
        LEDGER,
        SNAPSHOT
    }
}
