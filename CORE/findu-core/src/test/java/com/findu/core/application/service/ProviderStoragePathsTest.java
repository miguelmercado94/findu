package com.findu.core.application.service;

import com.findu.core.domain.exception.InvalidProviderDocumentException;
import com.findu.core.infrastructure.adapter.out.storage.InMemoryObjectStorageAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderStoragePathsTest {

    @Test
    void cedulaConPuntos() {
        assertEquals("CC1143378452", ProviderStoragePaths.sanitize("CC", "1.143.378.452"));
    }

    @Test
    void cedulaExtranjera() {
        assertEquals("CE0001234", ProviderStoragePaths.sanitize("ce", "0001234"));
    }

    @Test
    void nitConGuion() {
        assertEquals("NIT9017654321", ProviderStoragePaths.sanitize("NIT", "901.765.432-1"));
    }

    @Test
    void pasaporteCapitalizadoYVariantes() {
        assertEquals("PASAPORTEAB123456", ProviderStoragePaths.sanitize("Pasaporte", "ab 123-456"));
        assertEquals("PASAPORTEAB1", ProviderStoragePaths.sanitize("PA", "AB1"));
        assertEquals("PASAPORTEAB1", ProviderStoragePaths.sanitize("PAS", "AB1"));
    }

    @Test
    void tildesYEspacios() {
        assertEquals("CC12AB", ProviderStoragePaths.sanitize("  cc ", " 12 áb "));
    }

    @Test
    void vacioLanza() {
        assertThrows(InvalidProviderDocumentException.class, () -> ProviderStoragePaths.sanitize("CC", " .- "));
        assertThrows(InvalidProviderDocumentException.class, () -> ProviderStoragePaths.sanitize(null, "123"));
        assertThrows(InvalidProviderDocumentException.class, () -> ProviderStoragePaths.sanitize("CC", null));
    }

    @Test
    void clavesFijasYUnicas() {
        assertEquals("proveedores/CC123/biometrico/rostro/", ProviderStoragePaths.rostroPrefix("CC", "123"));
        assertEquals("proveedores/CC123/biometrico/rostro/rostro.png", ProviderStoragePaths.rostroKey("CC", "123"));
        assertEquals("proveedores/CC123/biometrico/documento/frente.png",
                ProviderStoragePaths.documentoFrenteKey("CC", "123"));
        assertEquals("proveedores/CC123/biometrico/documento/reverso.png",
                ProviderStoragePaths.documentoReversoKey("CC", "123"));
        assertTrue(ProviderStoragePaths.perfilKey("CC", "123", "png")
                .matches("proveedores/CC123/perfil/\\d{14}_[0-9a-f]{8}\\.png"));
        assertTrue(ProviderStoragePaths.portafolioKey("CC", "123", 7L, "jpg")
                .matches("proveedores/CC123/especializaciones/7/portafolio/\\d{14}_[0-9a-f]{8}\\.jpg"));
        assertTrue(ProviderStoragePaths.credencialKey("CC", "123", 7L, "pdf")
                .matches("proveedores/CC123/especializaciones/7/credenciales/\\d{14}_[0-9a-f]{8}\\.pdf"));
    }

    @Test
    void adaptadorEnMemoriaPutGetListCopyDelete() {
        InMemoryObjectStorageAdapter s = new InMemoryObjectStorageAdapter();
        String k = ProviderStoragePaths.rostroKey("CC", "1");
        s.put(k, new byte[]{1, 2}, "image/png");
        assertTrue(s.exists(k));
        assertArrayEquals(new byte[]{1, 2}, s.get(k));
        assertEquals(1, s.listKeys(ProviderStoragePaths.rostroPrefix("CC", "1")).size());
        s.copy(k, "proveedores/CC1/perfil/x.png");
        assertTrue(s.exists("proveedores/CC1/perfil/x.png"));
        s.delete(k);
        assertFalse(s.exists(k));
    }
}