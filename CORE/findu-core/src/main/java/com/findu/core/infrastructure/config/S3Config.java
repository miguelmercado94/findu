package com.findu.core.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Bean
    public S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.getRegion()));

        if (notBlank(properties.getAccessKey()) && notBlank(properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        boolean hasEndpoint = notBlank(properties.getEndpoint());
        if (hasEndpoint) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        // Con endpoint personalizado (LocalStack) siempre path-style.
        builder.forcePathStyle(hasEndpoint || properties.isPathStyle());
        return builder.build();
    }

    /** Crea el bucket si falta (solo cuando findu.aws.s3.create-bucket-if-missing=true). Idempotente. */
    @Bean
    public ApplicationRunner s3BucketInitializer(S3Client s3Client, S3Properties properties) {
        return args -> {
            if (!properties.isCreateBucketIfMissing()) {
                return;
            }
            String bucket = properties.getBucket();
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("Bucket S3 '{}' ya existe", bucket);
            } catch (NoSuchBucketException e) {
                createBucket(s3Client, bucket);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    createBucket(s3Client, bucket);
                } else {
                    log.warn("No se pudo verificar el bucket S3 '{}': status={}", bucket, e.statusCode());
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo verificar el bucket S3 '{}': {}", bucket, e.getClass().getSimpleName());
            }
        };
    }

    private void createBucket(S3Client s3Client, String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket S3 '{}' creado", bucket);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear el bucket S3 '{}': {}", bucket, e.getClass().getSimpleName());
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}