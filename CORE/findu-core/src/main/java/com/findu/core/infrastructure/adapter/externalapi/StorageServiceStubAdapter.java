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
        log.info("[STORAGE-SERVICE] uploadFile procesado para bucket={}, fileName={}", bucket, fileName);
        if (fileBase64 == null || fileBase64.isBlank()) {
            return null;
        }
        return fileBase64;
    }

    @Override
    public String getFileBase64(String fileUrl) {
        return fileUrl;
    }
}
