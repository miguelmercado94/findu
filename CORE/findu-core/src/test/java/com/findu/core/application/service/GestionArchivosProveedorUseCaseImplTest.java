package com.findu.core.application.service;

import com.findu.core.application.port.in.GestionArchivosProveedorUseCase;
import com.findu.core.application.port.in.GestionArchivosProveedorUseCase.FotoPerfil;
import com.findu.core.application.port.out.biometric.FaceComparisonPort;
import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ArchivoProveedorException.Codigo;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.infrastructure.adapter.out.storage.InMemoryObjectStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestionArchivosProveedorUseCaseImplTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 2, 3, 4, 5, 6};

    private InMemoryObjectStorageAdapter storage;
    private PerfilProveedorService perfilService;
    private FaceComparisonPort face;
    private FotoUrlResolver resolver;
    private GestionArchivosProveedorUseCaseImpl useCase;
    private PerfilProveedor perfil;

    private final String rostroKey = ProviderStoragePaths.rostroKey("CC", "1234");

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorageAdapter();
        perfilService = mock(PerfilProveedorService.class);
        face = mock(FaceComparisonPort.class);
        when(face.compare(any(), any())).thenReturn(new FaceComparisonPort.FaceComparisonResult(true, null));
        resolver = mock(FotoUrlResolver.class);
        when(resolver.resolve(any())).thenReturn("/url/resuelta");
        perfil = PerfilProveedor.builder().id(10L).tipoIdentificacion("CC").numeroIdentificacion("1234")
                .estadoVerificacion("VERIFICADO").build();
        when(perfilService.findById(10L)).thenReturn(Optional.of(perfil));
        when(perfilService.save(any(PerfilProveedor.class))).thenAnswer(i -> i.getArgument(0));
        useCase = new GestionArchivosProveedorUseCaseImpl(perfilService, storage, face, resolver, 1024);
    }

    private void conBiometria() {
        perfil.setBiometriaVerificadaAt(LocalDateTime.now());
        storage.put(rostroKey, PNG, "image/png");
    }

    // ---- GET ----

    @Test
    void getDevuelveBytesDeLaClaveGuardada() {
        String key = ProviderStoragePaths.perfilKey("CC", "1234", "jpg");
        storage.put(key, JPG, "image/jpeg");
        perfil.setUrlImagenPerfil(key);
        FotoPerfil foto = useCase.obtenerFotoPerfil(10L);
        assertThat(foto.bytes()).isEqualTo(JPG);
        assertThat(foto.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void getAutorrepararCopiaRostroAPerfil() {
        conBiometria();
        FotoPerfil foto = useCase.obtenerFotoPerfil(10L);
        assertThat(foto.bytes()).isEqualTo(PNG);
        assertThat(perfil.getUrlImagenPerfil()).startsWith("proveedores/CC1234/perfil/").endsWith(".png");
        assertThat(storage.exists(perfil.getUrlImagenPerfil())).isTrue();
        assertThat(storage.exists(rostroKey)).isTrue();
    }

    @Test
    void getSinFotoNiAutorreparacionEs404() {
        // biometria completa pero sin rostro.png
        perfil.setBiometriaVerificadaAt(LocalDateTime.now());
        assertThatThrownBy(() -> useCase.obtenerFotoPerfil(10L)).isInstanceOf(ResourceNotFoundException.class);
        // rostro.png presente pero sin biometria completa
        perfil.setBiometriaVerificadaAt(null);
        storage.put(rostroKey, PNG, "image/png");
        assertThatThrownBy(() -> useCase.obtenerFotoPerfil(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(perfil.getUrlImagenPerfil()).isNull();
    }

    // ---- PUT ----

    @Test
    void putOkGuardaEnPerfilYNoTocaBiometrico() {
        conBiometria();
        storage.put(ProviderStoragePaths.documentoFrenteKey("CC", "1234"), PNG, "image/png");
        List<String> antes = storage.listKeys("proveedores/CC1234/biometrico/");

        String url = useCase.cambiarFotoPerfil(10L, JPG);

        assertThat(url).isEqualTo("/url/resuelta");
        assertThat(perfil.getUrlImagenPerfil()).startsWith("proveedores/CC1234/perfil/").endsWith(".jpg");
        assertThat(storage.get(perfil.getUrlImagenPerfil())).isEqualTo(JPG);
        assertThat(storage.contentTypeOf(perfil.getUrlImagenPerfil())).isEqualTo("image/jpeg");
        assertThat(storage.listKeys("proveedores/CC1234/biometrico/")).isEqualTo(antes);
        assertThat(storage.get(rostroKey)).isEqualTo(PNG);
    }

    @Test
    void putSinBiometriaEs409() {
        storage.put(rostroKey, PNG, "image/png");
        assertThatThrownBy(() -> useCase.cambiarFotoPerfil(10L, JPG))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.BIOMETRIA_REQUERIDA));
        assertThat(perfil.getUrlImagenPerfil()).isNull();
    }

    @Test
    void putSinRostroPngEs422() {
        perfil.setBiometriaVerificadaAt(LocalDateTime.now());
        assertThatThrownBy(() -> useCase.cambiarFotoPerfil(10L, JPG))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.ROSTRO_BIOMETRICO_NO_ENCONTRADO));
    }

    @Test
    void putFormatoNoSoportadoEs415() {
        conBiometria();
        assertThatThrownBy(() -> useCase.cambiarFotoPerfil(10L, GIF))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.FORMATO_NO_SOPORTADO));
    }

    @Test
    void putDemasiadoGrandeEs413() {
        conBiometria();
        byte[] grande = new byte[2048];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertThatThrownBy(() -> useCase.cambiarFotoPerfil(10L, grande))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.ARCHIVO_DEMASIADO_GRANDE));
    }

    @Test
    void putSiLaComparacionFalla422() {
        conBiometria();
        when(face.compare(any(), any())).thenReturn(new FaceComparisonPort.FaceComparisonResult(false, 0.1));
        assertThatThrownBy(() -> useCase.cambiarFotoPerfil(10L, JPG))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.FOTO_NO_COINCIDE));
    }

    // ---- registrarBiometria ----

    private static String imagenBase64(String formato) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, formato, out);
        return java.util.Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private GestionArchivosProveedorUseCase.BiometriaCommand comando() throws Exception {
        return new GestionArchivosProveedorUseCase.BiometriaCommand(
                imagenBase64("jpg"), imagenBase64("png"), "data:image/png;base64," + imagenBase64("png"), 0.93);
    }

    @Test
    void biometriaEscribeTresPngMasPerfilYMarcaEspejo() throws Exception {
        var res = useCase.registrarBiometria(10L, comando());

        assertThat(res.fotoPerfilActualizada()).isTrue();
        assertThat(storage.exists(rostroKey)).isTrue();
        assertThat(storage.exists(ProviderStoragePaths.documentoFrenteKey("CC", "1234"))).isTrue();
        assertThat(storage.exists(ProviderStoragePaths.documentoReversoKey("CC", "1234"))).isTrue();
        // el jpg de entrada se normalizo a PNG (magic bytes)
        assertThat(GestionArchivosProveedorUseCaseImpl.detectFormat(storage.get(rostroKey))).isEqualTo("png");
        assertThat(perfil.getUrlImagenPerfil()).startsWith("proveedores/CC1234/perfil/").endsWith(".png");
        assertThat(storage.get(perfil.getUrlImagenPerfil())).isEqualTo(storage.get(rostroKey));
        assertThat(perfil.getBiometriaVerificadaAt()).isNotNull();
        assertThat(perfil.getEstadoVerificacion()).isEqualTo("VERIFICADO");
    }

    @Test
    void biometriaEsIdempotente() throws Exception {
        var cmd = new GestionArchivosProveedorUseCase.BiometriaCommand(imagenBase64("png"), imagenBase64("png"), imagenBase64("png"), null);
        useCase.registrarBiometria(10L, cmd);
        LocalDateTime primera = perfil.getBiometriaVerificadaAt();
        String fotoPrimera = perfil.getUrlImagenPerfil();
        int objetos = storage.listKeys("proveedores/").size();

        useCase.registrarBiometria(10L, cmd);

        assertThat(storage.listKeys("proveedores/")).hasSize(objetos);
        assertThat(perfil.getUrlImagenPerfil()).isEqualTo(fotoPrimera);
        // la nueva validacion renueva el espejo, nunca lo retrocede
        assertThat(perfil.getBiometriaVerificadaAt()).isAfterOrEqualTo(primera);
    }

    @Test
    void repetirBiometriaConEspejoPrevioReescribeTodoYRenuevaEspejo() throws Exception {
        // proveedor con espejo previo pero sin evidencia en S3 (caso ya verificado antes del despliegue)
        perfil.setBiometriaVerificadaAt(LocalDateTime.now().minusDays(30));
        LocalDateTime previo = perfil.getBiometriaVerificadaAt();
        assertThat(storage.exists(rostroKey)).isFalse();

        useCase.registrarBiometria(10L, comando());

        assertThat(storage.exists(rostroKey)).isTrue();
        assertThat(storage.exists(ProviderStoragePaths.documentoFrenteKey("CC", "1234"))).isTrue();
        assertThat(storage.exists(ProviderStoragePaths.documentoReversoKey("CC", "1234"))).isTrue();
        assertThat(perfil.getUrlImagenPerfil()).startsWith("proveedores/CC1234/perfil/");
        assertThat(storage.get(perfil.getUrlImagenPerfil())).isEqualTo(storage.get(rostroKey));
        assertThat(perfil.getBiometriaVerificadaAt()).isAfter(previo);
    }

    @Test
    void biometriaNuevaValidacionRefrescaFotoDePerfil() throws Exception {
        useCase.registrarBiometria(10L, new GestionArchivosProveedorUseCase.BiometriaCommand(imagenBase64("png"), null, null, null));
        String primera = perfil.getUrlImagenPerfil();
        // el usuario cambia la foto de perfil por una foto distinta
        storage.put("proveedores/CC1234/perfil/otra.jpg", JPG, "image/jpeg");
        perfil.setUrlImagenPerfil("proveedores/CC1234/perfil/otra.jpg");

        useCase.registrarBiometria(10L, new GestionArchivosProveedorUseCase.BiometriaCommand(imagenBase64("png"), null, null, null));

        assertThat(perfil.getUrlImagenPerfil()).isNotEqualTo("proveedores/CC1234/perfil/otra.jpg").isNotEqualTo(primera);
        assertThat(storage.get(perfil.getUrlImagenPerfil())).isEqualTo(storage.get(rostroKey));
    }

    @Test
    void biometriaFalloDeCopyNoInvalidaLaBiometria() throws Exception {
        InMemoryObjectStorageAdapter fallaCopy = new InMemoryObjectStorageAdapter() {
            @Override
            public synchronized void copy(String sourceKey, String destinationKey) {
                throw new com.findu.core.domain.exception.StorageException("copy fallo", null);
            }
        };
        useCase = new GestionArchivosProveedorUseCaseImpl(perfilService, fallaCopy, face, resolver, 1024);

        var res = useCase.registrarBiometria(10L, comando());

        assertThat(res.fotoPerfilActualizada()).isFalse();
        assertThat(fallaCopy.exists(rostroKey)).isTrue();
        assertThat(perfil.getBiometriaVerificadaAt()).isNotNull();
        assertThat(perfil.getEstadoVerificacion()).isEqualTo("VERIFICADO");
        assertThat(perfil.getUrlImagenPerfil()).isNull();
    }

    @Test
    void biometriaBase64InvalidoEs422SinEscrituras() throws Exception {
        var cmd = new GestionArchivosProveedorUseCase.BiometriaCommand(imagenBase64("png"), "###no-es-base64###", null, null);
        assertThatThrownBy(() -> useCase.registrarBiometria(10L, cmd))
                .isInstanceOfSatisfying(ArchivoProveedorException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(Codigo.IMAGEN_INVALIDA));
        assertThat(storage.listKeys("")).isEmpty();
        assertThat(perfil.getBiometriaVerificadaAt()).isNull();

        var noImagen = new GestionArchivosProveedorUseCase.BiometriaCommand(
                java.util.Base64.getEncoder().encodeToString("hola".getBytes()), null, null, null);
        assertThatThrownBy(() -> useCase.registrarBiometria(10L, noImagen)).isInstanceOf(ArchivoProveedorException.class);
    }

    @Test
    void biometriaHabilitaElPutDeFotoDePerfil() throws Exception {
        useCase.registrarBiometria(10L, comando());
        useCase.cambiarFotoPerfil(10L, JPG);
        assertThat(perfil.getUrlImagenPerfil()).endsWith(".jpg");
    }
}