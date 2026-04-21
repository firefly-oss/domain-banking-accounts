package com.firefly.domain.banking.accounts.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for core-common-contracts API.
 * Maps the properties defined in application.yaml under api-configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "api-configuration.core-common.contracts")
@Data
public class CoreCommonContractsProperties {
    private String basePath;
}
