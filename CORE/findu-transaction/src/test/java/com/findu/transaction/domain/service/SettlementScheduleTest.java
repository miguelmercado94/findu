package com.findu.transaction.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementScheduleTest {

    @Test
    @DisplayName("Martes 10-nov: corte domingo 8-nov y proximo martes 17-nov")
    void tuesday() {
        LocalDate tuesday = LocalDate.of(2026, 11, 10);
        assertThat(SettlementSchedule.cutoffDate(tuesday)).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(SettlementSchedule.nextTuesdayAfter(tuesday)).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    @DisplayName("Lunes 26-oct: corte 25-oct y proximo martes 27-oct")
    void monday() {
        LocalDate monday = LocalDate.of(2026, 10, 26);
        assertThat(SettlementSchedule.cutoffDate(monday)).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(SettlementSchedule.nextTuesdayAfter(monday)).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    @DisplayName("Domingo: el corte es el domingo ESTRICTAMENTE anterior")
    void sunday() {
        assertThat(SettlementSchedule.cutoffDate(LocalDate.of(2026, 11, 8))).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("Fin de ventana exclusivo = inicio del lunes en Bogota; codigo determinista")
    void periodEndAndCode() {
        LocalDate cutoff = LocalDate.of(2026, 11, 8);
        assertThat(SettlementSchedule.periodEndExclusive(cutoff, SettlementSchedule.BOGOTA))
                .isEqualTo(Instant.parse("2026-11-09T05:00:00Z"));
        assertThat(SettlementSchedule.settlementCode(1L, cutoff)).isEqualTo("SET-1-20261108");
    }
}