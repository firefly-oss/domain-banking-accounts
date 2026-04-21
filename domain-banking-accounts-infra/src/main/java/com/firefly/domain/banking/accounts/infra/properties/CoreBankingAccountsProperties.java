package com.firefly.domain.banking.accounts.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for core-banking-accounts API.
 * Maps the properties defined in application.yaml under api-configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "api-configuration.core-banking.accounts")
@Data
public class CoreBankingAccountsProperties {
    private String basePath;
}
