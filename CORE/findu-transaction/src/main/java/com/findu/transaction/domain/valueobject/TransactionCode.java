package com.findu.transaction.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
@EqualsAndHashCode
@ToString
public class TransactionCode {

    private final String value;

    public TransactionCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de transacción no puede estar vacío");
        }
        this.value = value.trim();
    }

    public static TransactionCode generate(String prefix) {
        String cleanPrefix = (prefix != null && !prefix.isBlank()) ? prefix : "TX";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String randomDigits = String.format("%06d", (int) (Math.random() * 999999));
        return new TransactionCode(cleanPrefix + "-" + timestamp + "-" + randomDigits);
    }
}
