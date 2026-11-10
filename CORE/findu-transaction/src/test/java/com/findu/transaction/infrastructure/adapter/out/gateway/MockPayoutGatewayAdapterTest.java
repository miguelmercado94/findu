package com.findu.transaction.infrastructure.adapter.out.gateway;

import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort.PayoutTransferRequest;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort.PayoutTransferResult;
import com.findu.transaction.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPayoutGatewayAdapterTest {

    private final MockPayoutGatewayAdapter adapter = new MockPayoutGatewayAdapter();

    @Test
    @DisplayName("Siempre exitosa, con referencia MOCK-TRANSFER-<uuid> distinta en cada llamada")
    void alwaysSucceedsWithUniqueReference() {
        PayoutTransferRequest req = new PayoutTransferRequest("PO-1", 1L, Money.of(185000), "SET-1-20261108");
        PayoutTransferResult a = adapter.transfer(req);
        PayoutTransferResult b = adapter.transfer(req);

        assertThat(a.successful()).isTrue();
        assertThat(a.reference()).startsWith("MOCK-TRANSFER-");
        UUID.fromString(a.reference().substring("MOCK-TRANSFER-".length()));
        assertThat(a.reference()).isNotEqualTo(b.reference());
    }
}