package com.findu.help.v2.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class S3ObjectReference {
    private final String bucket;
    private final String key;
    private final String s3Url;

    public S3ObjectReference(String bucket, String key, String s3Url) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave S3 (key) no puede estar vacía");
        }
        this.bucket = bucket != null ? bucket.trim() : "";
        this.key = key.trim();
        this.s3Url = s3Url != null ? s3Url.trim() : "";
    }

    public static S3ObjectReference ofTicketImage(String ticketCode, String fileName, String s3Url) {
        String key = "ticket/soporte/" + ticketCode + "/" + fileName;
        return new S3ObjectReference("findu-bucket", key, s3Url);
    }
}
