package io.fireflysoftware.domain.banking.accounts.web.openapi;

import org.fireflyframework.web.openapi.EnableOpenApiGen;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

@EnableOpenApiGen
@ComponentScan(basePackages = "io.fireflysoftware.domain.banking.accounts.web.controllers")
public class OpenApiGenApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenApiGenApplication.class, args);
    }
}
