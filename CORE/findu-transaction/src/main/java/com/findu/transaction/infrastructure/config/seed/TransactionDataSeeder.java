package com.findu.transaction.infrastructure.config.seed;

import com.findu.transaction.application.port.in.CreateServiceTransactionUseCase;
import com.findu.transaction.application.port.in.command.CreateServiceTransactionCommand;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.domain.enums.PaymentMethod;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionDataSeeder implements CommandLineRunner {

    private final ServiceTransactionRepository serviceTransactionRepository;
    private final CreateServiceTransactionUseCase createTransactionUseCase;

    @Override
    public void run(String... args) {
        if (serviceTransactionRepository.findAll().isEmpty()) {
            log.info("TransactionDataSeeder: Inicializando datos semilla para desarrollo en findu-transaction...");

            // 1. Servicio con pago digital (Tarjeta/PSE) - $110.000 COP, comision FindU 10% ($11.000 COP)
            createTransactionUseCase.execute(CreateServiceTransactionCommand.builder()
                    .serviceRequestId(101L)
                    .providerId(1L)
                    .customerId(201L)
                    .serviceAmount(Money.of(new BigDecimal("110000.00")))
                    .commissionRate(new BigDecimal("0.10"))
                    .tipAmount(Money.ZERO)
                    .extraAmount(Money.ZERO)
                    .paymentMethod(PaymentMethod.CARD)
                    .build());

            // 2. Servicio con pago digital (PSE) - $97.000 COP, comision FindU 10% ($9.700 COP)
            createTransactionUseCase.execute(CreateServiceTransactionCommand.builder()
                    .serviceRequestId(102L)
                    .providerId(1L)
                    .customerId(202L)
                    .serviceAmount(Money.of(new BigDecimal("97000.00")))
                    .commissionRate(new BigDecimal("0.10"))
                    .tipAmount(Money.ZERO)
                    .extraAmount(Money.ZERO)
                    .paymentMethod(PaymentMethod.PSE)
                    .build());

            // 3. Servicio con pago en efectivo (CASH) - $8.000 COP, comision FindU 10% ($800 COP)
            createTransactionUseCase.execute(CreateServiceTransactionCommand.builder()
                    .serviceRequestId(103L)
                    .providerId(1L)
                    .customerId(203L)
                    .serviceAmount(Money.of(new BigDecimal("8000.00")))
                    .commissionRate(new BigDecimal("0.10"))
                    .tipAmount(Money.ZERO)
                    .extraAmount(Money.ZERO)
                    .paymentMethod(PaymentMethod.CASH)
                    .build());

            // 4. Servicio con pago en efectivo (CASH) - $5.000 COP, comision FindU 10% ($500 COP)
            createTransactionUseCase.execute(CreateServiceTransactionCommand.builder()
                    .serviceRequestId(104L)
                    .providerId(1L)
                    .customerId(204L)
                    .serviceAmount(Money.of(new BigDecimal("5000.00")))
                    .commissionRate(new BigDecimal("0.10"))
                    .tipAmount(Money.ZERO)
                    .extraAmount(Money.ZERO)
                    .paymentMethod(PaymentMethod.CASH)
                    .build());

            log.info("TransactionDataSeeder: 4 transacciones de prueba creadas exitosamente para providerId=1 (Total: $220.000 COP, Comisión: -$22.000 COP, Efectivo: -$13.000 COP, Balance neto: $185.000 COP).");
        }
    }
}
