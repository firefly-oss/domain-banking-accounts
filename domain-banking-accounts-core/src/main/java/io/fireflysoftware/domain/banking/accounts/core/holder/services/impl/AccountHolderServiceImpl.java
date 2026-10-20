package io.fireflysoftware.domain.banking.accounts.core.holder.services.impl;

import io.fireflysoftware.domain.banking.accounts.core.account.commands.AddPartyToContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.AddAccountHolderCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.commands.GetAccountContractCommand;
import io.fireflysoftware.domain.banking.accounts.core.holder.services.AccountHolderService;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.banking.accounts.core.holder.utils.AddAccountHolderConstants.*;

@Service
public class AccountHolderServiceImpl implements AccountHolderService {

    private final SagaEngine engine;

    public AccountHolderServiceImpl(SagaEngine engine) {
        this.engine = engine;
    }

    @Override
    public Mono<SagaResult> addHolder(AddAccountHolderCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_GET_CONTRACT, GetAccountContractCommand.builder()
                        .accountId(command.getAccountId())
                        .build())
                .forStepId(STEP_ADD_PARTY, AddPartyToContractCommand.builder()
                        .partyId(command.getPartyId())
                        .roleInContractId(command.getRoleInContractId())
                        .build())
                .build();

        return engine.execute(SAGA_ADD_ACCOUNT_HOLDER_NAME, inputs);
    }
}
