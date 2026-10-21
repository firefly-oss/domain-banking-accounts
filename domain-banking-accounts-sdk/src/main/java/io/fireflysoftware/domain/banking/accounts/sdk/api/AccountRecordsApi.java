/*
 * Copyright 2026 Firefly Software Foundation
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

package io.fireflysoftware.domain.banking.accounts.sdk.api;

import io.fireflysoftware.domain.banking.accounts.sdk.invoker.ApiClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain client for statement close, withdrawal limit, space sweep, and servicing mandate.
 * A journey uses this class. It does not call the accounts core.
 */
public class AccountRecordsApi {

    private final WebClient webClient;
    private final String basePath;

    public AccountRecordsApi(ApiClient apiClient) {
        this.webClient = apiClient.getWebClient();
        this.basePath = apiClient.getBasePath();
    }

    public Mono<UUID> closeStatement(UUID accountId, String currency, BigDecimal openingBalance,
                                      List<BigDecimal> lineAmounts, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currency", currency);
        body.put("openingBalance", openingBalance);
        body.put("lineAmounts", lineAmounts);
        return post("/api/v1/account-records/accounts/" + accountId + "/statements", body, key);
    }

    public Mono<UUID> recordWithdrawalLimit(UUID accountId, String currency, BigDecimal withdrawalLimit,
                                             BigDecimal overdraftLimit, BigDecimal balance,
                                             BigDecimal withdrawalAmount, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currency", currency);
        body.put("withdrawalLimit", withdrawalLimit);
        body.put("overdraftLimit", overdraftLimit);
        body.put("balance", balance);
        body.put("withdrawalAmount", withdrawalAmount);
        return post("/api/v1/account-records/accounts/" + accountId + "/limits", body, key);
    }

    public Mono<UUID> recordSpaceSweep(UUID accountId, UUID sourceSpaceId, UUID targetSpaceId,
                                        BigDecimal amount, String currency, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceSpaceId", sourceSpaceId);
        body.put("targetSpaceId", targetSpaceId);
        body.put("amount", amount);
        body.put("currency", currency);
        return post("/api/v1/account-records/accounts/" + accountId + "/sweeps", body, key);
    }

    public Mono<UUID> createServicingMandate(UUID accountId, UUID partyId, String role, boolean active, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partyId", partyId);
        body.put("role", role);
        body.put("active", active);
        return post("/api/v1/account-records/accounts/" + accountId + "/servicing-mandates", body, key);
    }

    private Mono<UUID> post(String path, Map<String, Object> body, String key) {
        return webClient.post()
                .uri(basePath + path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", key)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(RecordedId.class)
                .map(RecordedId::id);
    }

    public record RecordedId(UUID id) {
    }
}
