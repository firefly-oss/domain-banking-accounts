package com.firefly.domain.banking.accounts.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Identifiers of the general-ledger (GL) "internal" accounts used by domain-banking-accounts
 * sagas to post double-entry ledger transactions. These are not customer accounts — they are
 * suspense / clearing / income accounts owned by the bank in core-banking-ledger. Populated
 * from {@code firefly.ledger.gl.*} in application.yaml (or config-server per environment).
 */
@Configuration
@ConfigurationProperties(prefix = "firefly.ledger.gl")
@Data
public class LedgerGlProperties {

    /** GL account credited when an account is opened — offset of the opening deposit. */
    private UUID openingSuspenseAccountId;

    /** GL account credited when a monetary hold is placed on an account. */
    private UUID holdsClearingAccountId;

    /** GL account credited when fees are charged to a customer account. */
    private UUID feeIncomeAccountId;
}
