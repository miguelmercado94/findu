package com.findu.core.domain.enums;

public enum NivelAmonestacion {
    LEVE(0, "Advertencia formal y registro en historial"),
    MODERADA(3, "Restricción para recibir nuevas ofertas durante 3 días"),
    GRAVE(7, "Restricción para recibir nuevas ofertas durante 7 días");

    private final int diasRestriccion;
    private final String consecuencia;

    NivelAmonestacion(int diasRestriccion, String consecuencia) {
        this.diasRestriccion = diasRestriccion;
        this.consecuencia = consecuencia;
    }

    public int getDiasRestriccion() {
        return diasRestriccion;
    }

    public String getConsecuencia() {
        return consecuencia;
    }

    public static NivelAmonestacion porConteoInjustificadas(int conteo) {
        if (conteo <= 0) {
            return LEVE;
        }
        if (conteo <= 3) {
            return LEVE;
        }
        if (conteo <= 6) {
            return MODERADA;
        }
        return GRAVE;
    }
}

