package com.findu.core.application.service;

import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.infrastructure.adapter.out.storage.InMemoryObjectStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FotoUrlResolverTest {

    private static final String BASE = "/findu-core/api/v1/perfil-proveedor";

    private InMemoryObjectStorageAdapter storage;
    private PerfilProveedorService perfilService;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorageAdapter();
        perfilService = mock(PerfilProveedorService.class);
    }

    private FotoUrlResolver resolver(boolean migrate) {
        return new FotoUrlResolver(storage, perfilService, mock(PlatformTransactionManager.class), migrate, BASE);
    }

    private PerfilProveedor perfil(String foto) {
        return PerfilProveedor.builder().id(7L).tipoIdentificacion("CC").numeroIdentificacion("1234")
                .urlImagenPerfil(foto).build();
    }

    @Test
    void claveSeConvierteEnUrlDelEndpointConVersion() {
        assertThat(resolver(false).resolve(perfil("proveedores/CC1234/perfil/20260101000000_abcd1234.png")))
                .isEqualTo(BASE + "/7/foto-perfil?v=20260101000000_abcd1234");
    }

    @Test
    void httpSePasaTalCual() {
        assertThat(resolver(false).resolve(perfil("https://cdn.example.com/a.png"))).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void nuloOVacioEsNulo() {
        assertThat(resolver(false).resolve(perfil(null))).isNull();
        assertThat(resolver(false).resolve(perfil("  "))).isNull();
    }

    @Test
    void dataUriSinFlagEsAusente() {
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        assertThat(resolver(false).resolve(perfil(dataUri))).isNull();
        assertThat(storage.listKeys("")).isEmpty();
    }

    @Test
    void dataUriConFlagMigraAPerfil() {
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        PerfilProveedor p = perfil(dataUri);
        String url = resolver(true).resolve(p);
        assertThat(p.getUrlImagenPerfil()).startsWith("proveedores/CC1234/perfil/").endsWith(".png");
        assertThat(storage.get(p.getUrlImagenPerfil())).containsExactly(1, 2, 3);
        assertThat(url).startsWith(BASE + "/7/foto-perfil?v=");
        verify(perfilService).save(p);
    }

    @Test
    void base64CrudoConFlagTambienMigra() {
        String raw = Base64.getEncoder().encodeToString(new byte[300]);
        PerfilProveedor p = perfil(raw);
        assertThat(resolver(true).resolve(p)).isNotNull();
        assertThat(storage.listKeys("proveedores/CC1234/perfil/")).hasSize(1);
    }

    @Test
    void rutaLocalYSoporteSonAusentes() {
        assertThat(resolver(true).resolve(perfil("/data/user/0/cache/foto.jpg"))).isNull();
        assertThat(resolver(true).resolve(perfil("soporte://tickets/1/foto.png"))).isNull();
        assertThat(resolver(true).resolve(perfil("C:\\fotos\\a.png"))).isNull();
    }
}
