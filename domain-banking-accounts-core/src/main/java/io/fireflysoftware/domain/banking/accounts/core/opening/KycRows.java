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


package io.fireflysoftware.domain.banking.accounts.core.opening;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fireflysoftware.core.kycb.sdk.model.KycVerificationDTO;
import io.fireflysoftware.core.kycb.sdk.model.PaginationRequest;
import io.fireflysoftware.core.kycb.sdk.model.PaginationResponse;
import io.fireflysoftware.core.kycb.sdk.model.VerificationDocumentDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class KycRows {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    private KycRows() {
    }

    public static PaginationRequest firstPage() {
        return new PaginationRequest().pageNumber(0).pageSize(50);
    }

    public static String key() {
        return UUID.randomUUID().toString();
    }

    public static List<KycVerificationDTO> verifications(PaginationResponse page) {
        return convert(page, KycVerificationDTO.class);
    }

    public static List<VerificationDocumentDTO> documents(PaginationResponse page) {
        return convert(page, VerificationDocumentDTO.class);
    }

    private static <T> List<T> convert(PaginationResponse page, Class<T> type) {
        if (page == null || page.getContent() == null) {
            return List.of();
        }
        List<T> rows = new ArrayList<>();
        for (Object item : page.getContent()) {
            if (item == null) {
                continue;
            }
            rows.add(type.isInstance(item) ? type.cast(item) : JSON.convertValue(item, type));
        }
        return rows;
    }
}
