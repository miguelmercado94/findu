package com.findu.help.v2.domain.valueobject;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class MessageAttachment {
    private final S3ObjectReference s3Reference;
    private final String fileName;
    private final String mimeType;
    private final Long fileSizeBytes;

    public MessageAttachment(S3ObjectReference s3Reference, String fileName, String mimeType, Long fileSizeBytes) {
        if (s3Reference == null) {
            throw new IllegalArgumentException("La referencia S3 no puede ser nula");
        }
        this.s3Reference = s3Reference;
        this.fileName = fileName != null ? fileName.trim() : "adjunto.jpg";
        this.mimeType = mimeType != null ? mimeType.trim() : "image/jpeg";
        this.fileSizeBytes = fileSizeBytes != null ? fileSizeBytes : 0L;
    }
}
