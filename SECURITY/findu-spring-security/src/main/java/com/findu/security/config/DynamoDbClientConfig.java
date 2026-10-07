package com.findu.security.config;

import com.findu.security.config.properties.DynamoDbProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "findu.aws.dynamodb.enabled", havingValue = "true")
public class DynamoDbClientConfig {

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(DynamoDbProperties properties) {
        var builder = DynamoDbAsyncClient.builder()
                .region(Region.of(properties.region() != null && !properties.region().isBlank()
                        ? properties.region()
                        : "us-east-1"));

        boolean hasEndpoint = properties.endpoint() != null && !properties.endpoint().isBlank();

        if (hasEndpoint) {
            // Dev / LocalStack: endpoint override + credenciales ficticias.
            builder.endpointOverride(URI.create(properties.endpoint().trim()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        } else {
            // AWS real: usar la cadena de credenciales por defecto, que lee
            // AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY (y rol IAM, perfiles, etc.).
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
