package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentResponse {
    private String fileName;
    private String mimeType;
    private long fileSizeBytes;
    private String s3Url;
    private String key;
}
