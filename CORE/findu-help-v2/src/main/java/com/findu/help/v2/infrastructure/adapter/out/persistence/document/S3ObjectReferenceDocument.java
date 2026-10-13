package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectReferenceDocument {
    private String bucket;
    private String key;
    private String s3Url;
}
