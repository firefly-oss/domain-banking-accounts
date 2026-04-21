package com.firefly.domain.banking.accounts.core.account.handlers;

import com.firefly.core.contract.sdk.api.ContractsApi;
import com.firefly.domain.banking.accounts.core.account.commands.TerminateContractCommand;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class TerminateContractHandler extends CommandHandler<TerminateContractCommand, Void> {

    private final ContractsApi contractsApi;

    public TerminateContractHandler(ContractsApi contractsApi) {
        this.contractsApi = contractsApi;
    }

    @Override
    protected Mono<Void> doHandle(TerminateContractCommand cmd) {
        return contractsApi.deleteContract(cmd.getContractId(), UUID.randomUUID().toString());
    }
}
