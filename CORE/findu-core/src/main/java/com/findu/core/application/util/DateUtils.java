package com.findu.core.application.util;

import java.time.LocalDate;
import java.time.Period;

/**
 * Utilidades de fecha para reglas de negocio.
 */
public final class DateUtils {

    private DateUtils() {}

    private static final int EDAD_MINIMA = 18;

    /**
     * Calcula la edad en años a partir de la fecha de nacimiento.
     *
     * @param fechaNacimiento fecha de nacimiento del usuario
     * @return edad en años
     * @throws IllegalArgumentException si la fecha es null o futura
     */
    public static int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es requerida.");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /**
     * Valida que el usuario sea mayor de edad (≥18 años).
     *
     * @param fechaNacimiento fecha de nacimiento del usuario
     * @throws IllegalArgumentException si es menor de 18 años
     */
    public static void validarMayorDeEdad(LocalDate fechaNacimiento) {
        int edad = calcularEdad(fechaNacimiento);
        if (edad < EDAD_MINIMA) {
            throw new IllegalArgumentException(
                    String.format("Debes tener al menos %d años para registrarte. Edad calculada: %d.", EDAD_MINIMA, edad));
        }
    }

    /**
     * Verifica si es mayor de edad sin lanzar excepción.
     *
     * @param fechaNacimiento fecha de nacimiento
     * @return true si tiene 18 o más años
     */
    public static boolean esMayorDeEdad(LocalDate fechaNacimiento) {
        return fechaNacimiento != null && calcularEdad(fechaNacimiento) >= EDAD_MINIMA;
    }
}
