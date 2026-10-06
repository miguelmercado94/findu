package com.findu.help.v2.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "findu.help.aws.s3")
public class S3Properties {

    private String bucket = "findu-help-bucket";
    private String region = "us-east-1";
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
}
