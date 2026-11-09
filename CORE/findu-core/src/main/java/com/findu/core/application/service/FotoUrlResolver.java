package com.findu.core.application.service;

import com.findu.core.application.port.out.storage.ObjectStoragePort;
import com.findu.core.domain.model.PerfilProveedor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Unico punto que convierte el valor guardado en perfil_proveedor.url_imagen_perfil en la URL que ve el cliente.
 * <ul>
 *   <li>clave {@code proveedores/...} -> URL del endpoint foto-perfil con {@code ?v=} (cache-busting)</li>
 *   <li>http(s) -> pass-through</li>
 *   <li>data-URI / base64 -> migra a {@code perfil/} solo si {@code findu.storage.migrate-legacy=true}; si no, ausente</li>
 *   <li>ruta local, soporte:// u otros -> ausente (null) y se cuenta en log</li>
 * </ul>
 */
@Slf4j
@Component
public class FotoUrlResolver {

    private static final String KEY_PREFIX = "proveedores/";
    private static final Pattern RAW_BASE64 = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]{200,}$");

    private final ObjectStoragePort storage;
    private final PerfilProveedorService perfilService;
    private final PlatformTransactionManager txManager;
    private final boolean migrateLegacy;
    private final String basePath;
    private final AtomicLong ignoredCount = new AtomicLong();

    public FotoUrlResolver(ObjectStoragePort storage,
                           PerfilProveedorService perfilService,
                           PlatformTransactionManager txManager,
                           @Value("${findu.storage.migrate-legacy:false}") boolean migrateLegacy,
                           @Value("${findu.storage.foto-perfil-base-path:/findu-core/api/v1/perfil-proveedor}") String basePath) {
        this.storage = storage;
        this.perfilService = perfilService;
        this.txManager = txManager;
        this.migrateLegacy = migrateLegacy;
        this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
    }

    public static boolean isStorageKey(String value) {
        return value != null && value.startsWith(KEY_PREFIX);
    }

    /** @return URL consumible por el cliente, o null si el perfil no tiene foto utilizable */
    public String resolve(PerfilProveedor perfil) {
        if (perfil == null) {
            return null;
        }
        String stored = perfil.getUrlImagenPerfil();
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String value = stored.trim();
        if (isStorageKey(value)) {
            return endpointUrl(perfil.getId(), value);
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return value;
        }
        if (lower.startsWith("data:") || RAW_BASE64.matcher(value).matches()) {
            return resolveLegacy(perfil, value);
        }
        long total = ignoredCount.incrementAndGet();
        log.warn("Foto de perfil con formato no soportado ignorada perfilId={} (total={})", perfil.getId(), total);
        return null;
    }

    private String resolveLegacy(PerfilProveedor perfil, String value) {
        if (!migrateLegacy) {
            long total = ignoredCount.incrementAndGet();
            log.warn("Foto de perfil legacy (base64/data-URI) ignorada perfilId={} (total={})", perfil.getId(), total);
            return null;
        }
        try {
            String payload = value;
            String contentType = "image/png";
            if (value.toLowerCase().startsWith("data:")) {
                int comma = value.indexOf(',');
                if (comma < 0) {
                    return null;
                }
                String header = value.substring(5, comma);
                int semi = header.indexOf(';');
                contentType = semi >= 0 ? header.substring(0, semi) : header;
                payload = value.substring(comma + 1);
            }
            byte[] bytes = Base64.getMimeDecoder().decode(payload);
            String ext = contentType.contains("jpeg") || contentType.contains("jpg") ? "jpg"
                    : contentType.contains("webp") ? "webp" : "png";
            String key = ProviderStoragePaths.perfilKey(perfil.getTipoIdentificacion(), perfil.getNumeroIdentificacion(), ext);
            storage.put(key, bytes, contentType);
            perfil.setUrlImagenPerfil(key);
            new TransactionTemplate(txManager, requiresNew()).executeWithoutResult(s -> perfilService.save(perfil));
            log.info("Foto de perfil legacy migrada a almacenamiento perfilId={}", perfil.getId());
            return endpointUrl(perfil.getId(), key);
        } catch (RuntimeException e) {
            log.warn("No se pudo migrar la foto legacy perfilId={}: {}", perfil.getId(), e.toString());
            return null;
        }
    }

    private static TransactionDefinition requiresNew() {
        org.springframework.transaction.support.DefaultTransactionDefinition def =
                new org.springframework.transaction.support.DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return def;
    }

    private String endpointUrl(Long perfilId, String key) {
        String name = key.substring(key.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        String version = dot > 0 ? name.substring(0, dot) : name;
        return basePath + "/" + perfilId + "/foto-perfil?v=" + version;
    }
}
