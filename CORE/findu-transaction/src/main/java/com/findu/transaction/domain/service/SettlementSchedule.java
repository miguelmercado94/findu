package com.findu.transaction.domain.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Regla de corte y calendario de liquidacion: "corte el domingo; liquidacion el martes".
 *
 * <ul>
 *   <li>La liquidacion del martes T incluye lo ganado hasta el domingo anterior (inclusive, fin de dia).</li>
 *   <li>{@link #cutoffDate(LocalDate)}: domingo ESTRICTAMENTE anterior a la fecha de ejecucion
 *       (si se ejecuta un domingo, el corte es el domingo previo).</li>
 *   <li>{@link #nextTuesdayAfter(LocalDate)}: primer martes estrictamente posterior a la fecha dada
 *       (si hoy es martes, es el martes de la semana siguiente).</li>
 * </ul>
 * Lo ganado despues del corte no se pierde: se arrastra a la liquidacion del martes siguiente.
 */
public final class SettlementSchedule {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private SettlementSchedule() {}

    public static LocalDate cutoffDate(LocalDate executionDate) {
        return executionDate.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    }

    /** Limite superior EXCLUSIVO de la ventana: inicio del dia siguiente al corte. */
    public static Instant periodEndExclusive(LocalDate cutoffDate, ZoneId zone) {
        return cutoffDate.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public static LocalDate nextTuesdayAfter(LocalDate today) {
        return today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
    }

    public static String settlementCode(Long providerId, LocalDate cutoffDate) {
        return "SET-" + providerId + "-" + cutoffDate.toString().replace("-", "");
    }
}
