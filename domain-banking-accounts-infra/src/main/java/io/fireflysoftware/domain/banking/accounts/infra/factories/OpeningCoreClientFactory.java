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

import io.fireflysoftware.core.customer.sdk.api.PartiesApi;
import io.fireflysoftware.core.kycb.sdk.api.KycVerificationApi;
import io.fireflysoftware.core.kycb.sdk.api.KycVerificationDocumentsApi;
import io.fireflysoftware.core.product.sdk.api.ProductApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpeningCoreClientFactory {

    private final io.fireflysoftware.core.customer.sdk.invoker.ApiClient customers;
    private final io.fireflysoftware.core.product.sdk.invoker.ApiClient products;
    private final io.fireflysoftware.core.kycb.sdk.invoker.ApiClient kyc;

    public OpeningCoreClientFactory(
            @Value("${api-configuration.core-common.customer.base-path:http://localhost:8084}") String customerBasePath,
            @Value("${api-configuration.core-common.product.base-path:http://localhost:8086}") String productBasePath,
            @Value("${api-configuration.core-common.kyc.base-path:http://localhost:8087}") String kycBasePath) {
        this.customers = new io.fireflysoftware.core.customer.sdk.invoker.ApiClient();
        this.customers.setBasePath(trim(customerBasePath));
        this.products = new io.fireflysoftware.core.product.sdk.invoker.ApiClient();
        this.products.setBasePath(trim(productBasePath));
        this.kyc = new io.fireflysoftware.core.kycb.sdk.invoker.ApiClient();
        this.kyc.setBasePath(trim(kycBasePath));
    }

    @Bean
    public PartiesApi partiesApi() {
        return new PartiesApi(customers);
    }

    @Bean
    public ProductApi productApi() {
        return new ProductApi(products);
    }

    @Bean
    public KycVerificationApi kycVerificationApi() {
        return new KycVerificationApi(kyc);
    }

    @Bean
    public KycVerificationDocumentsApi kycVerificationDocumentsApi() {
        return new KycVerificationDocumentsApi(kyc);
    }

    private static String trim(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Core base path is not set");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
