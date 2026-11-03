package com.findu.dispatcher.infrastructure.routing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre CA5 (normalización de un recipient no canónico resoluble vía alias) y CA6 (los recipients
 * de otros roles no se ven afectados por la normalización de proveedor).
 */
class RecipientResolverTest {

    private final RecipientResolver resolver = new RecipientResolver();

    @Test
    void toCanonical_devuelve_null_y_blank_tal_cual() {
        assertThat(resolver.toCanonical(null)).isNull();
        assertThat(resolver.toCanonical("")).isEqualTo("");
        assertThat(resolver.toCanonical("   ")).isEqualTo("   ");
    }

    @Test
    void toCanonical_numerico_puro_se_prefija_como_proveedor() {
        assertThat(resolver.toCanonical("42")).isEqualTo("PROVEEDOR_42");
        assertThat(resolver.toCanonical("  7  ")).isEqualTo("PROVEEDOR_7");
    }

    @Test
    void toCanonical_proveedor_ya_prefijado_se_mantiene() {
        assertThat(resolver.toCanonical("PROVEEDOR_42")).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void toCanonical_otros_roles_no_se_tocan() {
        assertThat(resolver.toCanonical("CLIENTE_42")).isEqualTo("CLIENTE_42");
        assertThat(resolver.toCanonical("AGENTE_9")).isEqualTo("AGENTE_9");
        assertThat(resolver.toCanonical("SOPORTE_GENERAL")).isEqualTo("SOPORTE_GENERAL");
    }

    @Test
    void toCanonical_recipient_no_numerico_sin_prefijo_se_mantiene() {
        assertThat(resolver.toCanonical("usuario@correo.com")).isEqualTo("usuario@correo.com");
        assertThat(resolver.toCanonical("juanperez")).isEqualTo("juanperez");
    }

    @Test
    void toCanonical_resuelve_alias_conocido_tras_learnAlias() {
        resolver.learnAlias("PROVEEDOR_1001", "PROVEEDOR_42");
        assertThat(resolver.toCanonical("PROVEEDOR_1001")).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void learnAlias_es_noop_cuando_alias_igual_a_canonical() {
        resolver.learnAlias("PROVEEDOR_42", "PROVEEDOR_42");
        assertThat(resolver.toCanonical("PROVEEDOR_42")).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void learnAlias_es_noop_con_blanks() {
        resolver.learnAlias("", "PROVEEDOR_42");
        resolver.learnAlias("PROVEEDOR_1001", "   ");
        resolver.learnAlias(null, "PROVEEDOR_42");
        assertThat(resolver.toCanonical("PROVEEDOR_1001")).isEqualTo("PROVEEDOR_1001");
    }

    @Test
    void learnAlias_es_noop_sin_prefijo_proveedor() {
        resolver.learnAlias("CLIENTE_1001", "PROVEEDOR_42");
        resolver.learnAlias("PROVEEDOR_1001", "CLIENTE_42");
        assertThat(resolver.toCanonical("CLIENTE_1001")).isEqualTo("CLIENTE_1001");
        assertThat(resolver.toCanonical("PROVEEDOR_1001")).isEqualTo("PROVEEDOR_1001");
    }
}
