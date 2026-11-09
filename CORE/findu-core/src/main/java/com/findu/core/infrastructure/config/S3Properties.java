package com.findu.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "findu.aws.s3")
public class S3Properties {
    private String bucket = "findu-media-assets-local";
    private String region = "us-east-1";
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private boolean pathStyle = false;
    /** Solo local/dev: crea el bucket al arrancar si no existe. */
    private boolean createBucketIfMissing = false;
}