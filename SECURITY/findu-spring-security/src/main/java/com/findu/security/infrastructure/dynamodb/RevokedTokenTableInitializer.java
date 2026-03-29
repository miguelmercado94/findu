package com.findu.security.infrastructure.dynamodb;

import com.findu.security.config.properties.DynamoDbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

import java.util.concurrent.ExecutionException;

/**
 * Crea la tabla de sesiones de token (PK access_token_hash, GSI refresh_token_hash) y TTL.
 */
@Component
@ConditionalOnProperty(name = "findu.aws.dynamodb.enabled", havingValue = "true")
public class RevokedTokenTableInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenTableInitializer.class);

    private final DynamoDbAsyncClient client;
    private final DynamoDbProperties properties;

    public RevokedTokenTableInitializer(DynamoDbAsyncClient client, DynamoDbProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.createTableIfNotExists()) {
            return;
        }
        String table = properties.revokedTokensTable() != null && !properties.revokedTokensTable().isBlank()
                ? properties.revokedTokensTable()
                : "findu_revoked_tokens";

        if (tableExists(table)) {
            log.info("DynamoDB tabla '{}' ya existe", table);
            enableTtlIfPossible(table);
            return;
        }

        log.info("Creando tabla DynamoDB '{}' (sesiones token + GSI refresh)", table);
        CreateTableRequest create = CreateTableRequest.builder()
                .tableName(table)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH)
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName(RevokedTokenTableAttributes.REFRESH_TOKEN_HASH)
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(KeySchemaElement.builder()
                        .attributeName(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH)
                        .keyType(KeyType.HASH)
                        .build())
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName(RevokedTokenTableAttributes.GSI_REFRESH_TOKEN_HASH)
                        .keySchema(KeySchemaElement.builder()
                                .attributeName(RevokedTokenTableAttributes.REFRESH_TOKEN_HASH)
                                .keyType(KeyType.HASH)
                                .build())
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build())
                .build();

        client.createTable(create).get();
        waitActive(table);
        enableTtlIfPossible(table);
        log.info("Tabla DynamoDB '{}' lista", table);
    }

    private boolean tableExists(String table) throws ExecutionException, InterruptedException {
        try {
            client.describeTable(DescribeTableRequest.builder().tableName(table).build()).get();
            return true;
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof ResourceNotFoundException) {
                return false;
            }
            if (c != null && "ResourceNotFoundException".equals(c.getClass().getSimpleName())) {
                return false;
            }
            throw e;
        }
    }

    private void waitActive(String table) throws ExecutionException, InterruptedException {
        for (int i = 0; i < 60; i++) {
            var desc = client.describeTable(DescribeTableRequest.builder().tableName(table).build()).get();
            if (desc.table() != null && desc.table().tableStatus() == TableStatus.ACTIVE) {
                return;
            }
            Thread.sleep(500);
        }
        log.warn("Timeout esperando tabla DynamoDB ACTIVE: {}", table);
    }

    private void enableTtlIfPossible(String table) {
        try {
            client.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                    .tableName(table)
                    .timeToLiveSpecification(TimeToLiveSpecification.builder()
                            .enabled(true)
                            .attributeName(RevokedTokenTableAttributes.TTL)
                            .build())
                    .build()).get();
            log.debug("TTL habilitado en {}.{}", table, RevokedTokenTableAttributes.TTL);
        } catch (Exception e) {
            log.warn("No se pudo habilitar TTL en {}: {}", table, e.getMessage());
        }
    }
}
