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

import io.fireflysoftware.core.customer.sdk.api.PartiesApi;
import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.banking.accounts.core.opening.KycRows;
import io.fireflysoftware.domain.banking.accounts.core.opening.commands.ResolveOpeningEligibilityCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class ResolveOpeningEligibilityHandler extends CommandHandler<ResolveOpeningEligibilityCommand, Boolean> {

    private final PartiesApi parties;
    private final ProductApi products;

    public ResolveOpeningEligibilityHandler(PartiesApi parties, ProductApi products) {
        this.parties = parties;
        this.products = products;
    }

    @Override
    protected Mono<Boolean> doHandle(ResolveOpeningEligibilityCommand cmd) {
        return parties.getPartyById(cmd.partyId(), KycRows.key())
                .switchIfEmpty(Mono.error(new IllegalStateException("Party was not found")))
                .zipWith(products.getProductById(cmd.productId(), KycRows.key())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Product was not found"))))
                .map(pair -> pair.getT2().getProductStatus() == ProductDTO.ProductStatusEnum.ACTIVE);
    }
}
