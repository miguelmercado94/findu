package com.findu.notification.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

/**
 * Configuración de clientes AWS (DynamoDB, SES, SNS).
 *
 * - Con endpoint override (LocalStack / dev): credenciales ficticias "test"/"test".
 * - Sin endpoint (AWS real): DefaultCredentialsProvider, que lee
 *   AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY del entorno (o rol IAM, perfiles, etc.).
 */
@Configuration
public class AwsConfig {

    @Value("${findu.notification.aws.region:us-east-1}")
    private String region;

    @Value("${findu.notification.aws.dynamodb.endpoint:}")
    private String dynamoEndpoint;

    @Value("${findu.notification.aws.ses.endpoint:}")
    private String sesEndpoint;

    @Value("${findu.notification.aws.sns.endpoint:}")
    private String snsEndpoint;

    /** Credenciales ficticias para LocalStack; reales (default chain) para AWS. */
    private AwsCredentialsProvider credentials(String endpoint) {
        boolean hasEndpoint = endpoint != null && !endpoint.isBlank();
        return hasEndpoint
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                : DefaultCredentialsProvider.create();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(dynamoEndpoint));
        if (dynamoEndpoint != null && !dynamoEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(dynamoEndpoint));
        }
        return builder.build();
    }

    @Bean
    public SesClient sesClient() {
        var builder = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(sesEndpoint));
        if (sesEndpoint != null && !sesEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(sesEndpoint));
        }
        return builder.build();
    }

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(snsEndpoint));
        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }
        return builder.build();
    }
}
