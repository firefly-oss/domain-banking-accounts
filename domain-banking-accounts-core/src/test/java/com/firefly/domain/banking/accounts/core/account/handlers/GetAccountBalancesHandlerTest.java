package com.firefly.domain.banking.accounts.core.account.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.banking.accounts.sdk.api.AccountBalancesApi;
import com.firefly.core.banking.accounts.sdk.model.AccountBalanceDTO;
import com.firefly.core.banking.accounts.sdk.model.PaginationResponse;
import com.firefly.core.banking.ledger.sdk.api.AccountLegsApi;
import com.firefly.core.banking.ledger.sdk.model.TransactionLegDTO;
import com.firefly.domain.banking.accounts.core.account.queries.GetAccountBalancesQuery;
import com.firefly.domain.banking.accounts.infra.properties.BalanceQueryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountBalancesHandler — LEDGER vs SNAPSHOT modes")
class GetAccountBalancesHandlerTest {

    @Mock private AccountBalancesApi accountBalancesApi;
    @Mock private AccountLegsApi accountLegsApi;

    private ObjectMapper objectMapper;
    private BalanceQueryProperties properties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        properties = new BalanceQueryProperties();
    }

    private GetAccountBalancesHandler handler() {
        return new GetAccountBalancesHandler(accountBalancesApi, accountLegsApi, objectMapper, properties);
    }

    @Test
    @DisplayName("LEDGER mode composes a single AccountBalanceDTO by summing credits − debits")
    void ledgerMode_composesFromLegs() {
        UUID accountId = UUID.randomUUID();
        properties.setSource(BalanceQueryProperties.Source.LEDGER);

        TransactionLegDTO debit = new TransactionLegDTO()
                .legType("DEBIT").amount(new BigDecimal("40.00"));
        TransactionLegDTO credit = new TransactionLegDTO()
                .legType("CREDIT").amount(new BigDecimal("100.00"));
        com.firefly.core.banking.ledger.sdk.model.PaginationResponse legsPage =
                new com.firefly.core.banking.ledger.sdk.model.PaginationResponse();
        legsPage.setContent(List.of(debit, credit));
        legsPage.setTotalPages(1);
        when(accountLegsApi.listAccountLegs(eq(accountId), any(), any(), any(), any(), anyString()))
                .thenReturn(Mono.just(legsPage));

        StepVerifier.create(handler().doHandle(GetAccountBalancesQuery.builder().accountId(accountId).build()))
                .assertNext(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    AccountBalanceDTO dto = (AccountBalanceDTO) response.getContent().get(0);
                    assertThat(dto.getAccountId()).isEqualTo(accountId);
                    assertThat(dto.getBalanceAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
                    assertThat(dto.getBalanceType()).isEqualTo(AccountBalanceDTO.BalanceTypeEnum.CURRENT);
                })
                .verifyComplete();

        verify(accountBalancesApi, never()).getAllBalances(any(), any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("SNAPSHOT mode delegates to AccountBalancesApi and skips the ledger")
    void snapshotMode_readsProjection() {
        UUID accountId = UUID.randomUUID();
        properties.setSource(BalanceQueryProperties.Source.SNAPSHOT);

        PaginationResponse snapshotPage = new PaginationResponse();
        AccountBalanceDTO snapshot = new AccountBalanceDTO()
                .accountId(accountId)
                .balanceAmount(new BigDecimal("999.99"));
        snapshotPage.setContent(List.of(snapshot));
        when(accountBalancesApi.getAllBalances(eq(accountId), any(), any(), any(), any(), anyString()))
                .thenReturn(Mono.just(snapshotPage));

        StepVerifier.create(handler().doHandle(GetAccountBalancesQuery.builder().accountId(accountId).build()))
                .assertNext(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    AccountBalanceDTO dto = (AccountBalanceDTO) response.getContent().get(0);
                    assertThat(dto.getBalanceAmount()).isEqualByComparingTo(new BigDecimal("999.99"));
                })
                .verifyComplete();

        verify(accountLegsApi, never()).listAccountLegs(any(), any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("LEDGER mode paginates across multiple pages until exhausted")
    void ledgerMode_paginatesLegs() {
        UUID accountId = UUID.randomUUID();
        properties.setSource(BalanceQueryProperties.Source.LEDGER);

        com.firefly.core.banking.ledger.sdk.model.PaginationResponse page0 =
                pageOf(List.of(leg("CREDIT", "100.00")), 2);
        com.firefly.core.banking.ledger.sdk.model.PaginationResponse page1 =
                pageOf(List.of(leg("DEBIT", "30.00")), 2);

        when(accountLegsApi.listAccountLegs(eq(accountId), eq(0), any(), any(), any(), anyString()))
                .thenReturn(Mono.just(page0));
        when(accountLegsApi.listAccountLegs(eq(accountId), eq(1), any(), any(), any(), anyString()))
                .thenReturn(Mono.just(page1));

        StepVerifier.create(handler().doHandle(GetAccountBalancesQuery.builder().accountId(accountId).build()))
                .assertNext(response -> {
                    AccountBalanceDTO dto = (AccountBalanceDTO) response.getContent().get(0);
                    assertThat(dto.getBalanceAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Default (null) source falls back to LEDGER mode")
    void nullSourceFallsBackToLedger() {
        UUID accountId = UUID.randomUUID();
        properties.setSource(null);

        com.firefly.core.banking.ledger.sdk.model.PaginationResponse legsPage =
                pageOf(List.of(leg("CREDIT", "25.00")), 1);
        when(accountLegsApi.listAccountLegs(eq(accountId), any(), any(), any(), any(), anyString()))
                .thenReturn(Mono.just(legsPage));

        StepVerifier.create(handler().doHandle(GetAccountBalancesQuery.builder().accountId(accountId).build()))
                .expectNextCount(1)
                .verifyComplete();

        verify(accountBalancesApi, never()).getAllBalances(any(), any(), any(), any(), any(), anyString());
    }

    private static com.firefly.core.banking.ledger.sdk.model.PaginationResponse pageOf(List<TransactionLegDTO> legs, int totalPages) {
        com.firefly.core.banking.ledger.sdk.model.PaginationResponse page =
                new com.firefly.core.banking.ledger.sdk.model.PaginationResponse();
        page.setContent(legs.stream().map(Object.class::cast).toList());
        setField(page, "totalPages", totalPages);
        return page;
    }

    private static TransactionLegDTO leg(String legType, String amount) {
        return new TransactionLegDTO().legType(legType).amount(new BigDecimal(amount));
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not reflect-set " + name, e);
        }
    }
}
