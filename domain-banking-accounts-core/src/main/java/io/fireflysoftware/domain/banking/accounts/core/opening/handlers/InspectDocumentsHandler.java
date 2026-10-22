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


package io.fireflysoftware.domain.banking.accounts.core.opening.handlers;

import io.fireflysoftware.core.kycb.sdk.api.KycVerificationApi;
import io.fireflysoftware.core.kycb.sdk.api.KycVerificationDocumentsApi;
import io.fireflysoftware.core.kycb.sdk.model.FilterRequestKycVerificationDTO;
import io.fireflysoftware.core.kycb.sdk.model.FilterRequestVerificationDocumentDTO;
import io.fireflysoftware.core.kycb.sdk.model.KycVerificationDTO;
import io.fireflysoftware.core.kycb.sdk.model.VerificationDocumentDTO;
import io.fireflysoftware.domain.banking.accounts.core.opening.KycRows;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.InspectDocumentsCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@CommandHandlerComponent
public class InspectDocumentsHandler extends CommandHandler<InspectDocumentsCommand, InspectDocumentsCommand.Findings> {

    private final KycVerificationApi kyc;
    private final KycVerificationDocumentsApi documents;

    public InspectDocumentsHandler(KycVerificationApi kyc, KycVerificationDocumentsApi documents) {
        this.kyc = kyc;
        this.documents = documents;
    }

    @Override
    protected Mono<InspectDocumentsCommand.Findings> doHandle(InspectDocumentsCommand cmd) {
        FilterRequestKycVerificationDTO filter = new FilterRequestKycVerificationDTO().pagination(KycRows.firstPage());
        return kyc.listKycVerifications(cmd.partyId(), filter, KycRows.key())
                .map(KycRows::verifications)
                .flatMap(rows -> {
                    KycVerificationDTO chosen = rows.stream()
                            .filter(row -> row.getKycVerificationId() != null)
                            .max(Comparator.comparing(KycVerificationDTO::getVerificationDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                            .orElse(null);
                    if (chosen == null) {
                        return Mono.error(new IllegalStateException("Eligibility was not recorded for this party"));
                    }
                    FilterRequestVerificationDocumentDTO documentsFilter = new FilterRequestVerificationDocumentDTO()
                            .pagination(KycRows.firstPage());
                    return documents.listVerificationDocuments(cmd.partyId(), chosen.getKycVerificationId(), documentsFilter, KycRows.key())
                            .map(page -> findings(cmd, chosen, KycRows.documents(page)));
                });
    }

    private static InspectDocumentsCommand.Findings findings(InspectDocumentsCommand cmd, KycVerificationDTO chosen,
                                                             List<VerificationDocumentDTO> documents) {
        boolean verified = documents.stream().anyMatch(row -> Boolean.TRUE.equals(row.getIsVerified()));
        String next;
        if (verified) {
            next = "VERIFIED";
        } else if (!documents.isEmpty()) {
            next = "PENDING";
        } else {
            next = "UNVERIFIED";
        }
        String previous = chosen.getVerificationStatus() == null ? "" : chosen.getVerificationStatus();
        return new InspectDocumentsCommand.Findings(cmd.partyId(), chosen.getKycVerificationId(), previous, next);
    }
}
