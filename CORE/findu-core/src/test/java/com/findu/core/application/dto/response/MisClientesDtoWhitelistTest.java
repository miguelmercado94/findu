package com.findu.core.application.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.core.domain.enums.ChatEstado;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato de claves JSON de "Mis clientes": solo nombre y foto del cliente; nada de contacto ni tickets. */
class MisClientesDtoWhitelistTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private static Set<String> claves(Object dto) {
        JsonNode node = MAPPER.valueToTree(dto);
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    @Test
    void resumenExponeExactamenteEstasClaves() {
        var dto = new ClienteResumenResponse(UUID.randomUUID(), "Ana Perez", true, false, 2, true, true,
                ChatEstado.NO_DISPONIBLE_AUN);
        assertEquals(Set.of("clienteRef", "nombreCompleto", "tieneFoto", "favoritoInterno", "serviciosRealizados",
                "enServicio", "activo", "chatEstado"), claves(dto));
    }

    @Test
    void detalleExponeExactamenteLasMismasClaves() {
        var dto = new ClienteDetalleResponse(UUID.randomUUID(), "Ana Perez", false, true, 0, false, false,
                ChatEstado.BLOQUEADO);
        assertEquals(claves(new ClienteResumenResponse(null, null, false, false, 0, false, false, null)), claves(dto));
        assertEquals("BLOQUEADO", MAPPER.valueToTree(dto).get("chatEstado").asText());
    }

    @Test
    void historialExponeExactamenteEstasClaves() {
        var dto = new ServicioHistorialItemResponse("Plomeria", "Hogar", LocalDateTime.now(), "FINALIZADA");
        assertEquals(Set.of("servicio", "categoria", "fecha", "estado"), claves(dto));
    }

    @Test
    void paginaExponeExactamenteEstasClaves() {
        var dto = new PaginaResponse<>(List.of(), 0, 20, 0L, 0);
        assertEquals(Set.of("content", "page", "size", "totalElements", "totalPages"), claves(dto));
    }

    @Test
    void ningunaClaveSugiereDatosSensibles() {
        Set<String> todas = new HashSet<>();
        todas.addAll(claves(new ClienteResumenResponse(null, null, false, false, 0, false, false, null)));
        todas.addAll(claves(new ServicioHistorialItemResponse(null, null, null, null)));
        for (String k : todas) {
            String l = k.toLowerCase();
            assertTrue(!l.contains("ticket") && !l.contains("codigo") && !l.contains("telefono")
                    && !l.contains("celular") && !l.contains("email") && !l.contains("direccion")
                    && !l.contains("proveedor") && !l.equals("id"), "clave no permitida: " + k);
        }
    }
}