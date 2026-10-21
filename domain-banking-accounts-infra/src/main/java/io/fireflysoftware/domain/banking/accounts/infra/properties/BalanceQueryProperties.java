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


package io.fireflysoftware.domain.banking.accounts.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Runtime toggle between the two balance-query implementations:
 *
 * <ul>
 *   <li>{@code LEDGER} (default) composes the {@code AccountBalanceDTO} from posted ledger
 *       legs, treating {@code account_balance} as a projection only.</li>
 *   <li>{@code SNAPSHOT} reads the {@code account_balance} projection as-is — useful as a
 *       rollback switch while the projection consumer catches up.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "firefly.balance")
@Data
public class BalanceQueryProperties {

    /** {@code LEDGER} (default) or {@code SNAPSHOT}. */
    private Source source = Source.LEDGER;

    /** Balance source — LEDGER recomputes from ledger legs; SNAPSHOT uses the projection. */
    public enum Source {
        LEDGER,
        SNAPSHOT
    }
}
