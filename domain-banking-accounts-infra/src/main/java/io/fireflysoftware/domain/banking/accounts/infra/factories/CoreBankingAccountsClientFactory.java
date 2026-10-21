/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
