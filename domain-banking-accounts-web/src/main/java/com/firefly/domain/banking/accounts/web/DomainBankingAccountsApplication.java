package com.firefly.domain.banking.accounts.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(
    scanBasePackages = {
        "com.firefly.domain.banking.accounts",
        "org.fireflyframework.web"
    }
)
@EnableWebFlux
@ConfigurationPropertiesScan(basePackages = "com.firefly.domain.banking.accounts")
@OpenAPIDefinition(
    info = @Info(
        title = "Domain Banking Accounts API",
        version = "1.0.0",
        description = "Domain layer service for banking account orchestration - manages account lifecycle, holders, spaces, and restrictions",
        contact = @Contact(
            name = "Firefly Software Solutions Inc",
            email = "dev@getfirefly.io"
        )
    ),
    servers = {
        @Server(url = "http://domain.getfirefly.io/domain-banking-accounts", description = "Development Environment"),
        @Server(url = "/", description = "Local Development Environment")
    }
)
public class DomainBankingAccountsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DomainBankingAccountsApplication.class, args);
    }
}
