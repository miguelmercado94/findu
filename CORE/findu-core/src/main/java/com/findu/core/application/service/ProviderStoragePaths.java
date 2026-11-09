package com.findu.core.application.service;

import com.findu.core.domain.exception.InvalidProviderDocumentException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Unico lugar que construye claves S3 del proveedor:
 * proveedores/{TIPODOC}{NUMERO}/(perfil|biometrico|especializaciones)/...
 */
public final class ProviderStoragePaths {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ProviderStoragePaths() {
    }

    /** "CC","1.143.378.452" -> "CC1143378452". Lanza InvalidProviderDocumentException (422) si queda vacio. */
    public static String sanitize(String tipoDoc, String numero) {
        String tipo = clean(tipoDoc);
        String num = clean(numero);
        if (tipo.isEmpty() || num.isEmpty()) {
            throw new InvalidProviderDocumentException(
                    "El tipo y numero de identificacion del proveedor son requeridos para almacenar archivos");
        }
        if (tipo.equals("PA") || tipo.equals("PAS") || tipo.equals("PASAPORTE")) {
            tipo = "PASAPORTE";
        }
        return tipo + num;
    }

    public static String prefix(String tipoDoc, String numero) {
        return "proveedores/" + sanitize(tipoDoc, numero) + "/";
    }

    public static String perfilKey(String tipoDoc, String numero, String extension) {
        return prefix(tipoDoc, numero) + "perfil/" + uniqueName(extension);
    }

    public static String rostroPrefix(String tipoDoc, String numero) {
        return prefix(tipoDoc, numero) + "biometrico/rostro/";
    }

    public static String rostroKey(String tipoDoc, String numero) {
        return rostroPrefix(tipoDoc, numero) + "rostro.png";
    }

    public static String documentoFrenteKey(String tipoDoc, String numero) {
        return prefix(tipoDoc, numero) + "biometrico/documento/frente.png";
    }

    public static String documentoReversoKey(String tipoDoc, String numero) {
        return prefix(tipoDoc, numero) + "biometrico/documento/reverso.png";
    }

    public static String portafolioKey(String tipoDoc, String numero, Object servicioId, String extension) {
        return prefix(tipoDoc, numero) + "especializaciones/" + servicioSegment(servicioId)
                + "/portafolio/" + uniqueName(extension);
    }

    public static String credencialKey(String tipoDoc, String numero, Object servicioId, String extension) {
        return prefix(tipoDoc, numero) + "especializaciones/" + servicioSegment(servicioId)
                + "/credenciales/" + uniqueName(extension);
    }

    private static String servicioSegment(Object servicioId) {
        String s = servicioId == null ? "" : servicioId.toString().replaceAll("[^A-Za-z0-9_-]", "");
        if (s.isEmpty()) {
            throw new InvalidProviderDocumentException("El servicio de la especializacion es requerido");
        }
        return s;
    }

    private static String uniqueName(String extension) {
        String ext = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (ext.isEmpty()) {
            ext = "png";
        }
        return TS.format(LocalDateTime.now()) + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(upper, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.replaceAll("[^A-Z0-9]", "");
    }
}