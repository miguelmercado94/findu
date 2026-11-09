package com.findu.core.application.service;

import com.findu.core.application.port.in.GestionArchivosProveedorUseCase;
import com.findu.core.application.port.out.biometric.FaceComparisonPort;
import com.findu.core.application.port.out.storage.ObjectStoragePort;
import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ArchivoProveedorException.Codigo;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.PerfilProveedor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico servicio de archivos del proveedor.
 * TODO D9: anteponer ProviderOwnershipGuard antes de cada operacion (un proveedor solo accede a sus archivos).
 */
@Slf4j
@Service
@Transactional
public class GestionArchivosProveedorUseCaseImpl implements GestionArchivosProveedorUseCase {

    private final PerfilProveedorService perfilService;
    private final ObjectStoragePort storage;
    private final FaceComparisonPort faceComparison;
    private final FotoUrlResolver fotoUrlResolver;
    private final long maxFotoBytes;

    public GestionArchivosProveedorUseCaseImpl(PerfilProveedorService perfilService,
                                               ObjectStoragePort storage,
                                               FaceComparisonPort faceComparison,
                                               FotoUrlResolver fotoUrlResolver,
                                               @Value("${findu.storage.foto-perfil-max-bytes:5242880}") long maxFotoBytes) {
        this.perfilService = perfilService;
        this.storage = storage;
        this.faceComparison = faceComparison;
        this.fotoUrlResolver = fotoUrlResolver;
        this.maxFotoBytes = maxFotoBytes;
    }

    /** Unico criterio de "biometria completa": el espejo biometria_verificada_at. */
    public static boolean biometriaCompleta(PerfilProveedor perfil) {
        return perfil.getBiometriaVerificadaAt() != null;
    }

    @Override
    public FotoPerfil obtenerFotoPerfil(Long perfilId) {
        PerfilProveedor perfil = findPerfil(perfilId);
        String stored = perfil.getUrlImagenPerfil();

        if (stored == null || stored.isBlank()) {
            stored = repararFotoDesdeBiometria(perfil);
        }
        if (stored == null || !FotoUrlResolver.isStorageKey(stored.trim()) || !storage.exists(stored.trim())) {
            throw new ResourceNotFoundException("El proveedor no tiene foto de perfil");
        }
        String key = stored.trim();
        return new FotoPerfil(storage.get(key), contentTypeOf(key), key);
    }

    /** url_imagen_perfil null + biometria completa + existe rostro.png -> copia a perfil/ y actualiza la columna. */
    private String repararFotoDesdeBiometria(PerfilProveedor perfil) {
        if (!biometriaCompleta(perfil)) {
            return null;
        }
        String rostroKey = ProviderStoragePaths.rostroKey(perfil.getTipoIdentificacion(), perfil.getNumeroIdentificacion());
        if (!storage.exists(rostroKey)) {
            return null;
        }
        String perfilKey = ProviderStoragePaths.perfilKey(perfil.getTipoIdentificacion(), perfil.getNumeroIdentificacion(), "png");
        storage.copy(rostroKey, perfilKey);
        perfil.setUrlImagenPerfil(perfilKey);
        perfilService.save(perfil);
        log.info("Foto de perfil autoreparada desde biometria perfilId={}", perfil.getId());
        return perfilKey;
    }

    @Override
    public String cambiarFotoPerfil(Long perfilId, byte[] imagen) {
        PerfilProveedor perfil = findPerfil(perfilId);
        String tipo = perfil.getTipoIdentificacion();
        String numero = perfil.getNumeroIdentificacion();

        if (!biometriaCompleta(perfil)) {
            throw new ArchivoProveedorException(Codigo.BIOMETRIA_REQUERIDA,
                    "Debes completar la verificacion biometrica antes de cambiar la foto de perfil");
        }
        // D11: sin .png bajo biometrico/rostro/ el proveedor debe repetir la biometria
        String rostroPrefix = ProviderStoragePaths.rostroPrefix(tipo, numero);
        String rostroKey = storage.listKeys(rostroPrefix).stream()
                .filter(k -> k.toLowerCase().endsWith(".png"))
                .findFirst()
                .orElseThrow(() -> new ArchivoProveedorException(Codigo.ROSTRO_BIOMETRICO_NO_ENCONTRADO,
                        "No hay una foto biometrica de rostro registrada; repite la verificacion biometrica"));

        byte[] reference = storage.get(rostroKey);
        if (imagen == null || imagen.length == 0) {
            throw new ArchivoProveedorException(Codigo.IMAGEN_INVALIDA, "La imagen es requerida");
        }
        if (!faceComparison.compare(reference, imagen).matched()) {
            throw new ArchivoProveedorException(Codigo.FOTO_NO_COINCIDE,
                    "La foto no coincide con la informacion biometrica");
        }
        String format = detectFormat(imagen);
        if (format == null) {
            throw new ArchivoProveedorException(Codigo.FORMATO_NO_SOPORTADO,
                    "Formato de imagen no soportado; usa PNG, JPEG o WEBP");
        }
        if (imagen.length > maxFotoBytes) {
            throw new ArchivoProveedorException(Codigo.ARCHIVO_DEMASIADO_GRANDE,
                    "La imagen supera el tamano maximo permitido de " + (maxFotoBytes / (1024 * 1024)) + " MB");
        }

        String newKey = ProviderStoragePaths.perfilKey(tipo, numero, format);
        storage.put(newKey, imagen, contentTypeOf(newKey));
        perfil.setUrlImagenPerfil(newKey);
        perfilService.save(perfil);
        log.info("Foto de perfil actualizada perfilId={}", perfilId);
        return fotoUrlResolver.resolve(perfil);
    }

    private PerfilProveedor findPerfil(Long id) {
        return perfilService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id));
    }

    /** Detecta png/jpg/webp por magic bytes. @return extension o null */
    static String detectFormat(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    static String contentTypeOf(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (k.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }
}
