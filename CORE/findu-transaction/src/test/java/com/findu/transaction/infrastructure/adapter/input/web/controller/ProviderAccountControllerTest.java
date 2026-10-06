package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.GetFinancialQueryUseCase;
import com.findu.transaction.application.port.in.GetProviderBalanceUseCase;
import com.findu.transaction.application.port.in.GetProviderLedgerUseCase;
import com.findu.transaction.application.port.in.query.GetProviderBalanceQuery;
import com.findu.transaction.application.port.in.query.GetProviderLedgerQuery;
import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ProviderAccountResponse;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ProviderLedgerEntryResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderAccountControllerTest {

    @Mock private GetFinancialQueryUseCase getFinancialQueryUseCase;
    @Mock private GetProviderBalanceUseCase getProviderBalanceUseCase;
    @Mock private GetProviderLedgerUseCase getProviderLedgerUseCase;
    @Mock private TransactionWebMapper webMapper;

    @InjectMocks
    private ProviderAccountController controller;

    @Test
    @DisplayName("Debe obtener la cuenta del proveedor y retornar 200 OK")
    void getProviderAccount_Success() {
        Long providerId = 200L;
        ProviderAccount account = ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL);

        ProviderAccountResponse responseDto = new ProviderAccountResponse();
        responseDto.setProviderId(providerId);
        responseDto.setPayableBalance(new BigDecimal("0.00"));

        when(getProviderBalanceUseCase.execute(any(GetProviderBalanceQuery.class))).thenReturn(Optional.of(account));
        when(webMapper.toResponse(account)).thenReturn(responseDto);

        ResponseEntity<ProviderAccountResponse> response = controller.getProviderAccount(providerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProviderId()).isEqualTo(providerId);
    }

    @Test
    @DisplayName("Debe obtener el ledger del proveedor y retornar 200 OK")
    void getProviderLedger_Success() {
        Long providerId = 200L;
        ProviderLedgerEntry entry = new ProviderLedgerEntry(
                providerId, LedgerEntryType.SERVICE_EARNING, new Money(new BigDecimal("80000.00")), LedgerDirection.CREDIT, "tx-1", "Comisión servicio"
        );

        ProviderLedgerEntryResponse responseDto = new ProviderLedgerEntryResponse();
        responseDto.setProviderId(providerId);
        responseDto.setAmount(new BigDecimal("80000.00"));

        when(getProviderLedgerUseCase.execute(any(GetProviderLedgerQuery.class))).thenReturn(List.of(entry));
        when(webMapper.toResponse(entry)).thenReturn(responseDto);

        ResponseEntity<List<ProviderLedgerEntryResponse>> response = controller.getProviderLedger(providerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getAmount()).isEqualTo(new BigDecimal("80000.00"));
    }
}
