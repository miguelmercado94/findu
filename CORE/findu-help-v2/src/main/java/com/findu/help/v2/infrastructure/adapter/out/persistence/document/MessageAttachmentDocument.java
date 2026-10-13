package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentDocument {
    private S3ObjectReferenceDocument s3Reference;
    private String fileName;
    private String mimeType;
    private long fileSizeBytes;
}
