package com.findu.help.v2.infrastructure.adapter.out.storage;

import com.findu.help.v2.application.port.out.storage.FileStoragePort;
import com.findu.help.v2.application.port.out.storage.FileUpload;
import com.findu.help.v2.domain.exception.FileStorageException;
import com.findu.help.v2.domain.valueobject.S3ObjectReference;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.infrastructure.config.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public S3ObjectReference upload(FileUpload file) {
        if (file == null) {
            throw new IllegalArgumentException("El archivo a subir no puede ser nulo");
        }

        String fileId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String cleanFileName = file.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_");
        String ticketCodeValue = file.getTicketCode() != null ? file.getTicketCode().getValue() : "HT-UNKNOWN";
        String objectKey = String.format("ticket/ayuda/%s/%s_%s_%s", ticketCodeValue, timestamp, fileId, cleanFileName);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .contentType(file.getMimeType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getContent(), file.getFileSizeBytes())
            );

            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Properties.getBucket(), s3Properties.getRegion(), objectKey);
            return new S3ObjectReference(s3Properties.getBucket(), objectKey, s3Url);
        } catch (S3Exception e) {
            throw new FileStorageException("Error de AWS S3 al subir el archivo: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new FileStorageException("Error inesperado al subir el archivo a S3: " + e.getMessage(), e);
        }
    }

    @Override
    public S3ObjectReference storeImage(TicketCode ticketCode, String fileName, String mimeType, InputStream content, long fileSizeBytes) {
        FileUpload upload = FileUpload.builder()
                .ticketCode(ticketCode)
                .fileName(fileName)
                .mimeType(mimeType)
                .content(content)
                .fileSizeBytes(fileSizeBytes)
                .build();
        return upload(upload);
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("El objectKey es obligatorio para eliminar un archivo de S3");
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey.trim())
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("Error de AWS S3 al eliminar el archivo: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new FileStorageException("Error inesperado al eliminar el archivo de S3: " + e.getMessage(), e);
        }
    }
}
