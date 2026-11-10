package com.findu.transaction.application.port.out.gateway;

import com.findu.transaction.domain.valueobject.Money;

/**
 * Puerto de salida para transferir el neto de una liquidacion al proveedor.
 *
 * <p>Hoy solo existe {@code MockPayoutGatewayAdapter} (simulacion). Un adaptador real (Bre-B / banco)
 * se enchufa implementando este puerto. Nota: los adaptadores reales son asincronos (la transferencia
 * queda PROCESSING y se confirma por webhook); eso requerira un outbox/estado intermedio en la
 * liquidacion. La ejecucion actual es sincrona y transaccional: si {@link #transfer} falla se lanza
 * excepcion y toda la liquidacion hace rollback.
 *
 * <p>Es independiente de {@link BreBPaymentGatewayPort}, que cubre el cobro de deuda del proveedor.
 */
public interface PayoutGatewayPort {

    PayoutTransferResult transfer(PayoutTransferRequest request);

    record PayoutTransferRequest(String referenceId, Long providerId, Money amount, String settlementCode) {}

    record PayoutTransferResult(boolean successful, String reference, String message) {}
}
