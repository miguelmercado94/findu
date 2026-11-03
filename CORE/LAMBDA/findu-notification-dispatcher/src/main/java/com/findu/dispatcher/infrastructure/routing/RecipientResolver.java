package com.findu.dispatcher.infrastructure.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Red de seguridad que normaliza cualquier recipient entrante hacia el identificador canónico
 * único de proveedor ({@code PROVEEDOR_<proveedorId>}) en un único borde de entrada del dispatcher
 * (handshake y enrutamiento). Tolera apps/productores viejos durante la ventana de transición
 * sin exigir un despliegue atómico de los tres repos.
 *
 * <p>El mapa {@code aliasToCanonical} se alimenta del propio tráfico: core emite en el payload los
 * campos {@code canonicalRecipient}/{@code aliasRecipient} y el dispatcher aprende la equivalencia.
 */
@Slf4j
@Component
public class RecipientResolver {

    private static final String PREFIJO_PROVEEDOR = "PROVEEDOR_";
    private static final String PREFIJO_CLIENTE = "CLIENTE_";
    private static final String PREFIJO_AGENTE = "AGENTE_";
    private static final String PREFIJO_SOPORTE = "SOPORTE_";

    private final ConcurrentHashMap<String, String> aliasToCanonical = new ConcurrentHashMap<>();

    /**
     * Normaliza un recipient hacia su forma canónica de proveedor. Reglas en orden (ver design.md):
     * <ol>
     *   <li>null/blank → devolver tal cual (el llamador ya lo descarta).</li>
     *   <li>trim().</li>
     *   <li>CLIENTE_/AGENTE_/SOPORTE_ → tal cual (no es proveedor).</li>
     *   <li>alias conocido → el canónico mapeado.</li>
     *   <li>PROVEEDOR_ → tal cual (ya es forma proveedor).</li>
     *   <li>numérico puro → prefijar PROVEEDOR_.</li>
     *   <li>resto → tal cual (recipient de cliente por username, email, etc.).</li>
     * </ol>
     */
    public String toCanonical(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return recipient;
        }
        String valor = recipient.trim();

        if (valor.startsWith(PREFIJO_CLIENTE) || valor.startsWith(PREFIJO_AGENTE) || valor.startsWith(PREFIJO_SOPORTE)) {
            return valor;
        }

        String canonico = aliasToCanonical.get(valor);
        if (canonico != null) {
            return canonico;
        }

        if (valor.startsWith(PREFIJO_PROVEEDOR)) {
            return valor;
        }

        if (esNumericoPuro(valor)) {
            return PREFIJO_PROVEEDOR + valor;
        }

        return valor;
    }

    /**
     * Registra la equivalencia {@code alias -> canonical} de forma idempotente. No hace nada si
     * {@code alias == canonical}, si alguno es blank, o si alguno no empieza por {@code PROVEEDOR_}.
     */
    public void learnAlias(String alias, String canonical) {
        if (alias == null || canonical == null || alias.isBlank() || canonical.isBlank()) {
            return;
        }
        String a = alias.trim();
        String c = canonical.trim();
        if (a.equals(c)) {
            return;
        }
        if (!a.startsWith(PREFIJO_PROVEEDOR) || !c.startsWith(PREFIJO_PROVEEDOR)) {
            return;
        }
        String previo = aliasToCanonical.put(a, c);
        if (previo == null || !previo.equals(c)) {
            log.info("Alias de proveedor aprendido: {} -> {}", a, c);
        }
    }

    private boolean esNumericoPuro(String valor) {
        for (int i = 0; i < valor.length(); i++) {
            if (!Character.isDigit(valor.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
