package com.firefly.domain.banking.accounts.infra.factories;

import com.firefly.core.contract.sdk.api.ContractPartiesApi;
import com.firefly.core.contract.sdk.api.ContractsApi;
import com.firefly.core.contract.sdk.invoker.ApiClient;
import com.firefly.domain.banking.accounts.infra.properties.CoreCommonContractsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CoreCommonContractsClientFactory {

    private final ApiClient apiClient;

    public CoreCommonContractsClientFactory(CoreCommonContractsProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    @Bean
    public ContractsApi contractsApi() {
        return new ContractsApi(apiClient);
    }

    @Bean
    public ContractPartiesApi contractPartiesApi() {
        return new ContractPartiesApi(apiClient);
    }
}
