package com.findu.notification.processor.application.util;

import com.findu.notification.processor.domain.exception.InvalidTemplateException;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Valida que todas las variables declaradas por una plantilla ({@code variables})
 * estén presentes en los parámetros antes de renderizar (diseño §10, application/util).
 * Si falta alguna variable requerida lanza {@link InvalidTemplateException} en vez de
 * dejar un placeholder sin reemplazar silenciosamente.
 */
@Component
public class VariableValidator {

    /**
     * @throws InvalidTemplateException si la plantilla declara variables que no están
     *                                  presentes (o están vacías) en {@code params}.
     */
    public void validate(NotificationTemplate template, Map<String, String> params) {
        if (template == null || template.getVariables() == null || template.getVariables().isEmpty()) {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String declared : template.getVariables()) {
            String value = params != null ? params.get(declared) : null;
            if (value == null || value.isBlank()) {
                missing.add(declared);
            }
        }

        if (!missing.isEmpty()) {
            throw new InvalidTemplateException(
                    "Faltan variables requeridas para la plantilla " + template.getId() + ": " + missing);
        }
    }
}
