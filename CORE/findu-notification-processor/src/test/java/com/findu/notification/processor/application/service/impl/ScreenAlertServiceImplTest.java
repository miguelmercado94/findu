package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.dto.response.ScreenAlertResponse;
import com.findu.notification.processor.application.port.in.ResolveTemplateUseCase;
import com.findu.notification.processor.domain.enums.AlertCategory;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.AlertMeta;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScreenAlertServiceImplTest {

    private ResolveTemplateUseCase resolveTemplateUseCase;
    private ScreenAlertServiceImpl service;

    @BeforeEach
    void setUp() {
        resolveTemplateUseCase = mock(ResolveTemplateUseCase.class);
        service = new ScreenAlertServiceImpl(resolveTemplateUseCase);
    }

    private NotificationTemplate alert(String codigo, Integer prioridad, String titulo,
                                       String ctaLabel, String ctaAction, String condicion) {
        return NotificationTemplate.builder()
                .id(codigo + "#ALERTA#es")
                .sk("v#ACTIVE")
                .codigo(codigo)
                .tipo(TemplateType.ALERTA)
                .language("es")
                .bodyTemplate("cuerpo de " + codigo)
                .active(true)
                .alertMeta(AlertMeta.builder()
                        .categoria(AlertCategory.INFO)
                        .pantallaDestino("perfil_proveedor")
                        .titulo(titulo)
                        .ctaLabel(ctaLabel)
                        .ctaAction(ctaAction)
                        .prioridad(prioridad)
                        .condicionAparicion(condicion)
                        .tiempoDesaparicion(0)
                        .build())
                .build();
    }

    @Test
    @DisplayName("perfil_proveedor resuelve las dos alertas por código y las ordena por prioridad")
    void resolvesPerfilProveedorAlertsSortedByPriority() {
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_CORREO_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.of(alert("PERFIL_CORREO_PENDIENTE", 1, "Confirma tu correo",
                        null, null, "emailOk == false")));
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_BIOMETRIA_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.of(alert("PERFIL_BIOMETRIA_PENDIENTE", 2, "Completa tu validación biométrica",
                        "Validar ahora", "findu://perfil/verificacion-biometrica", "biometriaOk == false")));

        List<ScreenAlertResponse> result = service.resolveForScreen("perfil_proveedor", "es");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCodigo()).isEqualTo("PERFIL_CORREO_PENDIENTE");
        assertThat(result.get(0).getCategoria()).isEqualTo(AlertCategory.INFO);
        assertThat(result.get(0).getTitulo()).isEqualTo("Confirma tu correo");
        assertThat(result.get(0).getBody()).isEqualTo("cuerpo de PERFIL_CORREO_PENDIENTE");
        assertThat(result.get(0).getCondicionAparicion()).isEqualTo("emailOk == false");
        assertThat(result.get(0).getTiempoDesaparicion()).isZero();
        assertThat(result.get(1).getCodigo()).isEqualTo("PERFIL_BIOMETRIA_PENDIENTE");
        assertThat(result.get(1).getCtaLabel()).isEqualTo("Validar ahora");
        assertThat(result.get(1).getCtaAction()).isEqualTo("findu://perfil/verificacion-biometrica");
    }

    @Test
    @DisplayName("solo devuelve las alertas con versión activa presente")
    void skipsMissingActiveVersions() {
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_CORREO_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.of(alert("PERFIL_CORREO_PENDIENTE", 1, "Confirma tu correo",
                        null, null, "emailOk == false")));
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_BIOMETRIA_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.empty());

        List<ScreenAlertResponse> result = service.resolveForScreen("perfil_proveedor", "es");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCodigo()).isEqualTo("PERFIL_CORREO_PENDIENTE");
    }

    @Test
    @DisplayName("pantalla desconocida devuelve lista vacía sin consultar plantillas")
    void unknownScreenReturnsEmpty() {
        List<ScreenAlertResponse> result = service.resolveForScreen("pantalla_inexistente", "es");

        assertThat(result).isEmpty();
        verifyNoInteractions(resolveTemplateUseCase);
    }

    @Test
    @DisplayName("language nulo/blanco cae a es")
    void blankLanguageFallsBackToEs() {
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_CORREO_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.empty());
        when(resolveTemplateUseCase.resolveActive(eq("PERFIL_BIOMETRIA_PENDIENTE"), eq(TemplateType.ALERTA), eq("es")))
                .thenReturn(Optional.empty());

        List<ScreenAlertResponse> result = service.resolveForScreen("perfil_proveedor", "  ");

        assertThat(result).isEmpty();
        verify(resolveTemplateUseCase).resolveActive("PERFIL_CORREO_PENDIENTE", TemplateType.ALERTA, "es");
        verify(resolveTemplateUseCase).resolveActive("PERFIL_BIOMETRIA_PENDIENTE", TemplateType.ALERTA, "es");
    }
}
