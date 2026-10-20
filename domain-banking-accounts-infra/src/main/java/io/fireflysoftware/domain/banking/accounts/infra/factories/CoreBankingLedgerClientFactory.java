package io.fireflysoftware.domain.banking.accounts.infra.factories;

import io.fireflysoftware.core.banking.ledger.sdk.api.AccountLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLegsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineDepositApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineFeeApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineInterestApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionLineTransferApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionStatusHistoryApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.ledger.sdk.invoker.ApiClient;
import io.fireflysoftware.domain.banking.accounts.infra.properties.CoreBankingLedgerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Factory that exposes {@link io.fireflysoftware.core.banking.ledger.sdk.api} clients as Spring beans.
 * Wires the core-banking-ledger SDK against the base path defined in application.yaml.
 * Every monetary mutation orchestrated by domain-banking-accounts passes through these APIs,
 * so that the ledger remains the authoritative source of truth and account_balance becomes a projection.
 */
@Component
public class CoreBankingLedgerClientFactory {

    private final ApiClient apiClient;

    public CoreBankingLedgerClientFactory(CoreBankingLedgerProperties properties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(properties.getBasePath());
    }

    @Bean
    public TransactionsApi transactionsApi() {
        return new TransactionsApi(apiClient);
    }

    @Bean
    public TransactionLegsApi transactionLegsApi() {
        return new TransactionLegsApi(apiClient);
    }

    @Bean
    public AccountLegsApi accountLegsApi() {
        return new AccountLegsApi(apiClient);
    }

    @Bean
    public TransactionStatusHistoryApi transactionStatusHistoryApi() {
        return new TransactionStatusHistoryApi(apiClient);
    }

    @Bean
    public TransactionLineDepositApi transactionLineDepositApi() {
        return new TransactionLineDepositApi(apiClient);
    }

    @Bean
    public TransactionLineFeeApi transactionLineFeeApi() {
        return new TransactionLineFeeApi(apiClient);
    }

    @Bean
    public TransactionLineInterestApi transactionLineInterestApi() {
        return new TransactionLineInterestApi(apiClient);
    }

    @Bean
    public TransactionLineTransferApi transactionLineTransferApi() {
        return new TransactionLineTransferApi(apiClient);
    }
}
