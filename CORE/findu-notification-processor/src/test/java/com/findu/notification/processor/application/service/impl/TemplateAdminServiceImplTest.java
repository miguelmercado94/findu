package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.dto.request.ActivateVersionRequest;
import com.findu.notification.processor.application.dto.request.AlertMetaRequest;
import com.findu.notification.processor.application.dto.request.CreateTemplateRequest;
import com.findu.notification.processor.application.dto.request.NotificationRoutingRequest;
import com.findu.notification.processor.application.dto.request.UpdateTemplateRequest;
import com.findu.notification.processor.application.dto.response.TemplateResponse;
import com.findu.notification.processor.application.dto.response.TemplateVersionResponse;
import com.findu.notification.processor.application.port.out.persistence.TemplateRepositoryPort;
import com.findu.notification.processor.application.util.TemplateDtoMapper;
import com.findu.notification.processor.domain.enums.AlertCategory;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.exception.InvalidTemplateException;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateAdminServiceImplTest {

    private static final String CACHE_NAME = "notification-templates";

    private TemplateRepositoryPort repository;
    private CacheManager cacheManager;
    private TemplateAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(TemplateRepositoryPort.class);
        cacheManager = new ConcurrentMapCacheManager(CACHE_NAME);
        service = new TemplateAdminServiceImpl(repository, new TemplateDtoMapper(), cacheManager);
    }

    private CreateTemplateRequest smsCreateRequest() {
        return CreateTemplateRequest.builder()
                .codigo("VERIFICACION_OTP")
                .tipo(TemplateType.SMS)
                .language("es")
                .bodyTemplate("Tu codigo es {{codigo}}")
                .variables(List.of("codigo"))
                .build();
    }

    @Test
    @DisplayName("create: escribe versión 1, invalida caché y devuelve la versión activa (version=1)")
    void create_returnsVersionOne() {
        NotificationTemplate active = NotificationTemplate.builder()
                .id("VERIFICACION_OTP#SMS#es")
                .codigo("VERIFICACION_OTP").tipo(TemplateType.SMS).language("es")
                .version(1).active(true).bodyTemplate("Tu codigo es {{codigo}}")
                .build();
        when(repository.findActive("VERIFICACION_OTP", TemplateType.SMS, "es"))
                .thenReturn(Optional.of(active));

        TemplateResponse response = service.create(smsCreateRequest());

        verify(repository, times(1)).createNewVersion(any(NotificationTemplate.class));
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getTipo()).isEqualTo(TemplateType.SMS);
    }

    @Test
    @DisplayName("edit: llama createNewVersion (nueva versión, nunca sobreescribe) y refleja version=2")
    void edit_createsNewVersion() {
        NotificationTemplate v2 = NotificationTemplate.builder()
                .id("VERIFICACION_OTP#SMS#es")
                .codigo("VERIFICACION_OTP").tipo(TemplateType.SMS).language("es")
                .version(2).active(true).bodyTemplate("Nuevo cuerpo {{codigo}}")
                .build();
        // 1ra lectura (conservar auditoría) y 2da lectura (recargar activa).
        when(repository.findActive("VERIFICACION_OTP", TemplateType.SMS, "es"))
                .thenReturn(Optional.of(v2));

        UpdateTemplateRequest update = UpdateTemplateRequest.builder()
                .bodyTemplate("Nuevo cuerpo {{codigo}}")
                .variables(List.of("codigo"))
                .build();

        TemplateResponse response = service.edit("VERIFICACION_OTP", TemplateType.SMS, "es", update);

        verify(repository, times(1)).createNewVersion(any(NotificationTemplate.class));
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("activateVersion: repunta a una versión previa (rollback) y recarga la activa")
    void activateVersion_rollback() {
        NotificationTemplate rolledBack = NotificationTemplate.builder()
                .id("VERIFICACION_OTP#SMS#es")
                .codigo("VERIFICACION_OTP").tipo(TemplateType.SMS).language("es")
                .version(1).active(true).build();
        when(repository.findActive("VERIFICACION_OTP", TemplateType.SMS, "es"))
                .thenReturn(Optional.of(rolledBack));

        ActivateVersionRequest request = ActivateVersionRequest.builder()
                .codigo("VERIFICACION_OTP").tipo(TemplateType.SMS).language("es").version(1)
                .build();

        TemplateResponse response = service.activateVersion(request);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).activateVersion(idCaptor.capture(), eq(1));
        assertThat(idCaptor.getValue()).isEqualTo("VERIFICACION_OTP#SMS#es");
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByType: delega en findByTipo y mapea a TemplateResponse")
    void listByType_mapsResults() {
        NotificationTemplate t = NotificationTemplate.builder()
                .codigo("PAGO").tipo(TemplateType.EMAIL).language("es").version(3).active(true)
                .subject("Pago recibido").build();
        when(repository.findByTipo(TemplateType.EMAIL)).thenReturn(List.of(t));

        List<TemplateResponse> result = service.listByType(TemplateType.EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Pago recibido");
    }

    @Test
    @DisplayName("history: construye el id CODE#TIPO#LANG y mapea a TemplateVersionResponse")
    void history_mapsVersions() {
        NotificationTemplate v1 = NotificationTemplate.builder().version(1).active(false).bodyTemplate("v1").build();
        NotificationTemplate v2 = NotificationTemplate.builder().version(2).active(true).bodyTemplate("v2").build();
        when(repository.findHistory("VERIFICACION_OTP#SMS#es")).thenReturn(List.of(v1, v2));

        List<TemplateVersionResponse> history = service.history("VERIFICACION_OTP", TemplateType.SMS, "es");

        assertThat(history).hasSize(2);
        assertThat(history).extracting(TemplateVersionResponse::getVersion).containsExactly(1, 2);
    }

    @Test
    @DisplayName("deactivate: delega en el puerto con el id correcto e invalida la caché")
    void deactivate_evictsCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        cache.put("VERIFICACION_OTP#SMS#es", "cached");

        service.deactivate("VERIFICACION_OTP", TemplateType.SMS, "es");

        verify(repository).deactivate("VERIFICACION_OTP#SMS#es");
        assertThat(cache.get("VERIFICACION_OTP#SMS#es")).isNull();
    }

    @Test
    @DisplayName("create ALERTA sin categoria -> InvalidTemplateException")
    void create_alertaWithoutCategoria_fails() {
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .codigo("ACTIVACION_PENDIENTE").tipo(TemplateType.ALERTA).language("es")
                .bodyTemplate("Falta completar {{paso}}")
                .alertMeta(AlertMetaRequest.builder().pantallaDestino("PERFIL").build())
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    @DisplayName("create NOTIFICACION con routing resuelve correctamente")
    void create_notificacionWithRouting() {
        NotificationTemplate active = NotificationTemplate.builder()
                .codigo("NUEVO_MENSAJE").tipo(TemplateType.NOTIFICACION).language("es")
                .version(1).active(true).build();
        when(repository.findActive("NUEVO_MENSAJE", TemplateType.NOTIFICACION, "es"))
                .thenReturn(Optional.of(active));

        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .codigo("NUEVO_MENSAJE").tipo(TemplateType.NOTIFICACION).language("es")
                .bodyTemplate("Tienes un mensaje")
                .routing(NotificationRoutingRequest.builder()
                        .condicion("isUserActivo").canalSiActivo("WEBSOCKET").canalSiInactivo("PUSH").build())
                .build();

        TemplateResponse response = service.create(request);

        verify(repository, times(1)).createNewVersion(any(NotificationTemplate.class));
        assertThat(response.getTipo()).isEqualTo(TemplateType.NOTIFICACION);
    }
}
