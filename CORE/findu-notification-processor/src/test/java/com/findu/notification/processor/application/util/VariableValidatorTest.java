package com.findu.notification.processor.application.util;

import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.exception.InvalidTemplateException;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableValidatorTest {

    private final VariableValidator validator = new VariableValidator();

    private NotificationTemplate template(List<String> variables) {
        return NotificationTemplate.builder()
                .id("VERIFICACION_OTP#SMS#es")
                .codigo("VERIFICACION_OTP")
                .tipo(TemplateType.SMS)
                .variables(variables)
                .build();
    }

    @Test
    @DisplayName("No lanza cuando todas las variables declaradas están presentes")
    void validate_allPresent() {
        NotificationTemplate tpl = template(List.of("otp_code", "expiration_minutes"));
        assertThatCode(() -> validator.validate(tpl,
                Map.of("otp_code", "123456", "expiration_minutes", "5")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Lanza InvalidTemplateException cuando falta una variable requerida")
    void validate_missingVariable() {
        NotificationTemplate tpl = template(List.of("otp_code", "expiration_minutes"));
        assertThatThrownBy(() -> validator.validate(tpl, Map.of("otp_code", "123456")))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("expiration_minutes");
    }

    @Test
    @DisplayName("Una plantilla sin variables declaradas no requiere validación")
    void validate_noVariablesDeclared() {
        NotificationTemplate tpl = template(List.of());
        assertThatCode(() -> validator.validate(tpl, null)).doesNotThrowAnyException();
    }
}
