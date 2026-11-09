package com.findu.notification.processor.application.service;

import com.findu.notification.processor.application.port.out.externalapi.SecurityUserLookupPort;
import com.findu.notification.processor.application.port.out.persistence.DeviceTokenRepositoryPort;
import com.findu.notification.processor.domain.model.DeviceToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepositoryPort deviceTokenRepository;

    @Mock
    private SecurityUserLookupPort securityUserLookupPort;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Test
    @DisplayName("registerToken resuelve canonical_recipient=PROVEEDOR_<proveedorId> cuando security lo resuelve")
    void registerToken_resolvesCanonicalRecipient() {
        when(securityUserLookupPort.findProveedorIdByUsername("juan")).thenReturn(Optional.of(77L));

        deviceTokenService.registerToken("juan", "tok-123", "ANDROID");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).upsert(captor.capture());
        DeviceToken saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("juan");
        assertThat(saved.getDeviceToken()).isEqualTo("tok-123");
        assertThat(saved.getPlatform()).isEqualTo("ANDROID");
        assertThat(saved.getCanonicalRecipient()).isEqualTo("PROVEEDOR_77");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("registerToken deja canonical_recipient=null cuando security no resuelve el proveedor")
    void registerToken_nullCanonicalWhenLookupEmpty() {
        when(securityUserLookupPort.findProveedorIdByUsername("juan")).thenReturn(Optional.empty());

        deviceTokenService.registerToken("juan", "tok-123", "ANDROID");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).upsert(captor.capture());
        assertThat(captor.getValue().getCanonicalRecipient()).isNull();
    }

    @Test
    @DisplayName("unregister delega el borrado en el repositorio")
    void unregister_delegatesToRepository() {
        deviceTokenService.unregister("juan", "tok-123");

        verify(deviceTokenRepository).delete("juan", "tok-123");
    }
}
