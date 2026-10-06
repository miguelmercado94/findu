package com.findu.help.v2.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class TicketCode {

    private final String value;

    public TicketCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de ticket no puede estar vacío");
        }
        this.value = value.trim();
    }

    public static TicketCode generate(String prefix) {
        String cleanPrefix = (prefix != null && !prefix.isBlank()) ? prefix : "HT";
        String code = cleanPrefix + "-" + String.format("%06d", (int) (Math.random() * 999999));
        return new TicketCode(code);
    }
}
