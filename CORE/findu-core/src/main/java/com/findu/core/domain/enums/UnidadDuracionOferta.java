package com.findu.core.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Unidad de medida para la aproximación de duración de una oferta.
 * Por defecto: HORAS (1 hora).
 */
public enum UnidadDuracionOferta {
    MINUTOS,
    HORAS,
    DIAS;

    @JsonCreator
    public static UnidadDuracionOferta fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return HORAS;
        }
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "MINUTO", "MINUTOS", "MIN", "MINS", "MINUTE", "MINUTES" -> MINUTOS;
            case "HORA", "HORAS", "HR", "HRS", "H", "HOUR", "HOURS" -> HORAS;
            case "DIA", "DIAS", "D", "DAY", "DAYS" -> DIAS;
            default -> throw new IllegalArgumentException(
                    "Unidad de duración no válida: '" + raw + "'. Valores permitidos: MINUTOS, HORAS, DIAS.");
        };
    }
}

