package com.findu.notification.processor.application.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    @DisplayName("Reemplaza los placeholders {{var}} por los valores de params")
    void render_replacesPlaceholders() {
        String body = renderer.render("Hola {{nombre}}, tu código es {{otp}}",
                Map.of("nombre", "Ana", "otp", "123456"));
        assertThat(body).isEqualTo("Hola Ana, tu código es 123456");
    }

    @Test
    @DisplayName("Un valor nulo se reemplaza por cadena vacía")
    void render_nullParamsReturnsBodyUnchanged() {
        String body = renderer.render("Hola {{nombre}}", null);
        assertThat(body).isEqualTo("Hola {{nombre}}");
    }

    @Test
    @DisplayName("Body nulo rinde cadena vacía")
    void render_nullBody() {
        assertThat(renderer.render(null, Map.of("x", "y"))).isEmpty();
    }
}
