package com.findu.core.infrastructure.adapter.externalapi;

import com.findu.core.application.port.output.externalapi.StorageServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter stub para StorageServicePort.
 * Se usará mientras el micro findu-s3-servicios no esté desarrollado.
 *
 * A futuro, reemplazar por un adapter real que use WebClient para comunicarse
 * con findu-s3-servicios via Eureka (lb://FINDU-S3-SERVICIOS).
 */
@Component
public class StorageServiceStubAdapter implements StorageServicePort {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceStubAdapter.class);

    @Override
    public String uploadFile(String bucket, String fileName, String fileBase64) {
        log.warn("[STUB] uploadFile llamado — findu-s3-servicios no disponible. bucket={}, fileName={}", bucket, fileName);
        // Retorna una URL placeholder para no bloquear el flujo
        return null;
    }

    @Override
    public String getFileBase64(String fileUrl) {
        log.warn("[STUB] getFileBase64 llamado — findu-s3-servicios no disponible. url={}", fileUrl);
        return null;
    }
}
