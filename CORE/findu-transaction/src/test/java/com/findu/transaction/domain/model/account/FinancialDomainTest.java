package com.findu.transaction.domain.model.account;

import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.enums.ProviderAccountStatus;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialDomainTest {

    @Test
    @DisplayName("Debe procesar el escenario financiero obligatorio de 3 servicios y compensación de deudas")
    void testMandatoryFinancialScenarioAndCompensation() {
        Long providerId = 99L;
        Long customerId = 1L;
        BigDecimal commissionRate = new BigDecimal("0.10"); // 10%

        ProviderAccount account = ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL);

        // Servicio 1: $50.000 CASH (Comisión $5.000)
        ServiceTransaction tx1 = ServiceTransaction.create(
                101L, providerId, customerId, Money.of(50000), commissionRate, Money.ZERO, Money.ZERO, PaymentMethod.CASH
        );

        // Servicio 2: $80.000 TRANSFER (Comisión $8.000)
        ServiceTransaction tx2 = ServiceTransaction.create(
                102L, providerId, customerId, Money.of(80000), commissionRate, Money.ZERO, Money.ZERO, PaymentMethod.TRANSFER
        );

        // Servicio 3: $70.000 CASH (Comisión $7.000)
        ServiceTransaction tx3 = ServiceTransaction.create(
                103L, providerId, customerId, Money.of(70000), commissionRate, Money.ZERO, Money.ZERO, PaymentMethod.CASH
        );

        // Registrar servicios en la cuenta del proveedor
        account.recordServiceTransaction(tx1);
        account.recordServiceTransaction(tx2);
        account.recordServiceTransaction(tx3);

        // Verificaciones intermedias
        // FindU debe al proveedor: $72.000 (por el servicio digital de $80.000 menos $8.000 comisión)
        assertThat(account.getPayableBalance()).isEqualTo(Money.of(72000));

        // El proveedor debe a FindU: $12.000 ($5.000 de servicio 1 + $7.000 de servicio 3)
        assertThat(account.getReceivableBalance()).isEqualTo(Money.of(12000));

        // El estado debe ser NORMAL (Deuda 12k < Límite 100k)
        assertThat(account.getStatus()).isEqualTo(ProviderAccountStatus.NORMAL);

        // Aplicar Compensación Automática de Deuda
        Money compensationApplied = account.applyCompensation();

        // Verificaciones post-compensación
        assertThat(compensationApplied).isEqualTo(Money.of(12000));
        assertThat(account.getPayableBalance()).isEqualTo(Money.of(60000)); // Payout neto disponible
        assertThat(account.getReceivableBalance()).isEqualTo(Money.ZERO);    // Deuda saldada por compensación
    }

    @Test
    @DisplayName("Debe restringir la cuenta del proveedor cuando la deuda excede el límite configurado")
    void testProviderRestrictionOnExcessiveDebt() {
        Long providerId = 88L;
        // Persona Natural límite $100.000 COP
        ProviderAccount account = ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL);

        // Servicio de $1.200.000 CASH -> Comisión $120.000 (Excede el límite de $100.000)
        ServiceTransaction tx = ServiceTransaction.create(
                201L, providerId, 2L, Money.of(1200000), new BigDecimal("0.10"), Money.ZERO, Money.ZERO, PaymentMethod.CASH
        );

        account.recordServiceTransaction(tx);

        assertThat(account.getReceivableBalance()).isEqualTo(Money.of(120000));
        assertThat(account.getStatus()).isEqualTo(ProviderAccountStatus.RESTRICTED);
    }
}
