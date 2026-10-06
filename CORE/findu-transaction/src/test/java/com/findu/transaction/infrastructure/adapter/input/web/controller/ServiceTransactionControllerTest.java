package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreateServiceTransactionUseCase;
import com.findu.transaction.application.port.in.GetCustomerTransactionsUseCase;
import com.findu.transaction.application.port.in.GetFinancialQueryUseCase;
import com.findu.transaction.application.port.in.RegisterPaymentUseCase;
import com.findu.transaction.application.port.in.command.CreateServiceTransactionCommand;
import com.findu.transaction.application.port.in.command.RegisterPaymentCommand;
import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.PaymentStatus;
import com.findu.transaction.domain.enums.TransactionStatus;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreatePaymentRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.request.CreateTransactionRequest;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ServiceTransactionResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceTransactionControllerTest {

    @Mock private CreateServiceTransactionUseCase createServiceTransactionUseCase;
    @Mock private GetFinancialQueryUseCase getFinancialQueryUseCase;
    @Mock private GetCustomerTransactionsUseCase getCustomerTransactionsUseCase;
    @Mock private RegisterPaymentUseCase registerPaymentUseCase;
    @Mock private TransactionWebMapper webMapper;

    @InjectMocks
    private ServiceTransactionController controller;

    @Test
    @DisplayName("Debe crear una transacción y retornar 201 CREATED")
    void createTransaction_Success() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setServiceRequestId(101L);
        request.setProviderId(201L);
        request.setCustomerId(301L);
        request.setServiceAmount(new BigDecimal("100000.00"));
        request.setPaymentMethod(PaymentMethod.CARD);

        ServiceTransaction domain = ServiceTransaction.create(
                101L, 201L, 301L, new Money(new BigDecimal("100000.00")), null, null, null, PaymentMethod.CARD
        );

        ServiceTransactionResponse responseDto = new ServiceTransactionResponse();
        responseDto.setId(domain.getId());
        responseDto.setTransactionCode(domain.getTransactionCode().getValue());
        responseDto.setTransactionStatus(TransactionStatus.REGISTERED);

        when(webMapper.toCommand(any(CreateTransactionRequest.class))).thenReturn(CreateServiceTransactionCommand.builder().build());
        when(createServiceTransactionUseCase.execute(any())).thenReturn(domain);
        when(webMapper.toResponse(domain)).thenReturn(responseDto);

        ResponseEntity<ServiceTransactionResponse> response = controller.createTransaction(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(domain.getId());
    }

    @Test
    @DisplayName("Debe consultar transacción por ID y retornar 200 OK")
    void getTransactionById_Success() {
        String id = "tx-123";
        ServiceTransaction domain = ServiceTransaction.create(
                101L, 201L, 301L, new Money(new BigDecimal("100000.00")), null, null, null, PaymentMethod.CARD
        );

        ServiceTransactionResponse responseDto = new ServiceTransactionResponse();
        responseDto.setId(id);

        when(getFinancialQueryUseCase.getTransactionById(id)).thenReturn(Optional.of(domain));
        when(webMapper.toResponse(domain)).thenReturn(responseDto);

        ResponseEntity<ServiceTransactionResponse> response = controller.getTransactionById(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Debe retornar 404 NOT FOUND al buscar transacción inexistente")
    void getTransactionById_NotFound() {
        when(getFinancialQueryUseCase.getTransactionById("invalid-id")).thenReturn(Optional.empty());

        ResponseEntity<ServiceTransactionResponse> response = controller.getTransactionById("invalid-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Debe registrar pago de transacción y retornar 201 CREATED")
    void registerPayment_Success() {
        String txId = "tx-555";
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId(301L);
        request.setAmount(new BigDecimal("100000.00"));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setReference("PAY-REF-001");

        ServiceTransaction domain = ServiceTransaction.create(
                101L, 201L, 301L, new Money(new BigDecimal("100000.00")), null, null, null, PaymentMethod.CARD
        );

        ServiceTransactionResponse responseDto = new ServiceTransactionResponse();
        responseDto.setId(txId);
        responseDto.setPaymentStatus(PaymentStatus.COMPLETED);

        when(webMapper.toCommand(eq(txId), any(CreatePaymentRequest.class))).thenReturn(RegisterPaymentCommand.builder().build());
        when(registerPaymentUseCase.execute(any())).thenReturn(domain);
        when(webMapper.toResponse(domain)).thenReturn(responseDto);

        ResponseEntity<ServiceTransactionResponse> response = controller.registerPayment(txId, "IDEM-KEY-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
