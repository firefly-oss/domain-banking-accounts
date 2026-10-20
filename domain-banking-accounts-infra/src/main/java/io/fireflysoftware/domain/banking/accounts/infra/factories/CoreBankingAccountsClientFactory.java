package io.fireflysoftware.domain.banking.accounts.infra.factories;

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountBalancesApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountNotificationsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountParametersApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountProvidersApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountRestrictionsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpaceProvidersApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpaceTransactionsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountSpacesApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountStatusHistoryApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AssetPricesApi;
import io.fireflysoftware.core.banking.accounts.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.banking.accounts.infra.properties.CoreBankingAccountsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CoreBankingAccountsClientFactory {

    private final ApiClient apiClient;

    public CoreBankingAccountsClientFactory(CoreBankingAccountsProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    @Bean
    public AccountsApi accountsApi() {
        return new AccountsApi(apiClient);
    }

    @Bean
    public AccountBalancesApi accountBalancesApi() {
        return new AccountBalancesApi(apiClient);
    }

    @Bean
    public AccountSpacesApi accountSpacesApi() {
        return new AccountSpacesApi(apiClient);
    }

    @Bean
    public AccountParametersApi accountParametersApi() {
        return new AccountParametersApi(apiClient);
    }

    @Bean
    public AccountStatusHistoryApi accountStatusHistoryApi() {
        return new AccountStatusHistoryApi(apiClient);
    }

    @Bean
    public AccountRestrictionsApi accountRestrictionsApi() {
        return new AccountRestrictionsApi(apiClient);
    }

    @Bean
    public AccountNotificationsApi accountNotificationsApi() {
        return new AccountNotificationsApi(apiClient);
    }

    @Bean
    public AccountProvidersApi accountProvidersApi() {
        return new AccountProvidersApi(apiClient);
    }

    @Bean
    public AccountSpaceProvidersApi accountSpaceProvidersApi() {
        return new AccountSpaceProvidersApi(apiClient);
    }

    @Bean
    public AccountSpaceTransactionsApi accountSpaceTransactionsApi() {
        return new AccountSpaceTransactionsApi(apiClient);
    }

    @Bean
    public AssetPricesApi assetPricesApi() {
        return new AssetPricesApi(apiClient);
    }
}
