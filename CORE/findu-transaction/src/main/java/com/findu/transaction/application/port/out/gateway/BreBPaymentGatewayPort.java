package com.findu.transaction.application.port.out.gateway;

import com.findu.transaction.domain.valueobject.Money;

public interface BreBPaymentGatewayPort {

    /**
     * Genera la llave personalizada para el proveedor según el estándar Bre-B.
     * Formato: nameuser-{serial} (ej. miguelm-784529).
     */
    String generatePersonalizedKey(Long providerId, String username);

    /**
     * Genera la carga útil (payload / EMVCo / URL / base64) para el código QR dinámico de Bre-B.
     */
    String generateDynamicQrPayload(String breBKey, Money amount, String orderCode, String description);

    /**
     * Valida la firma criptográfica o token secreto del webhook recibido del switch Bre-B / banco.
     */
    boolean validateWebhookSignature(String signature, String payload);
}
