package com.findu.help.v2.infrastructure.adapter.out.storage;

import com.findu.help.v2.application.port.out.storage.FileUpload;
import com.findu.help.v2.domain.valueobject.S3ObjectReference;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.infrastructure.config.properties.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageAdapterTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private S3FileStorageAdapter s3FileStorageAdapter;

    @BeforeEach
    void setUp() {
        lenient().when(s3Properties.getBucket()).thenReturn("test-bucket");
        lenient().when(s3Properties.getRegion()).thenReturn("us-east-1");
    }

    @Test
    @DisplayName("Debe subir un archivo a S3 y retornar su S3ObjectReference")
    void upload_Success() {
        byte[] bytes = "Contenido de imagen de prueba".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);

        FileUpload upload = FileUpload.builder()
                .ticketCode(new TicketCode("HT-2026-0001"))
                .fileName("captura.png")
                .mimeType("image/png")
                .content(is)
                .fileSizeBytes(bytes.length)
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        S3ObjectReference ref = s3FileStorageAdapter.upload(upload);

        assertThat(ref).isNotNull();
        assertThat(ref.getBucket()).isEqualTo("test-bucket");
        assertThat(ref.getKey()).contains("ticket/ayuda/HT-2026-0001/");
        assertThat(ref.getS3Url()).contains("https://test-bucket.s3.us-east-1.amazonaws.com/");

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
