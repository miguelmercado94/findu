package com.findu.notification.processor.application.util;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renderiza un cuerpo de plantilla reemplazando los placeholders {@code {{var}}}
 * por los valores de {@code params}. Lógica extraída de {@code TemplateServiceImpl}
 * (diseño §10, paquete application/util).
 *
 * <p>Si {@code contentType=HTML} el cuerpo ya contiene el HTML completo; solo se
 * sustituyen las variables. Un valor nulo se reemplaza por cadena vacía.</p>
 */
@Component
public class TemplateRenderer {

    public String render(String bodyTemplate, Map<String, String> params) {
        if (bodyTemplate == null) {
            return "";
        }
        String body = bodyTemplate;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                body = body.replace("{{" + entry.getKey() + "}}", value);
            }
        }
        return body;
    }
}
