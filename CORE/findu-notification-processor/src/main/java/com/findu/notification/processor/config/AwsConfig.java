package com.findu.notification.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

/**
 * Configuración de clientes AWS (DynamoDB, SES, SNS).
 * En dev usa LocalStack (endpoint override). En pdn usa credenciales IAM.
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

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder().region(Region.of(region));
        if (dynamoEndpoint != null && !dynamoEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(dynamoEndpoint));
        }
        return builder.build();
    }

    @Bean
    public SesClient sesClient() {
        var builder = SesClient.builder().region(Region.of(region));
        if (sesEndpoint != null && !sesEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(sesEndpoint));
        }
        return builder.build();
    }

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder().region(Region.of(region));
        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }
        return builder.build();
    }
}
