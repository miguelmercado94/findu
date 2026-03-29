package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.RevokedTokenRepositoryPort;
import com.findu.security.config.properties.DynamoDbProperties;
import com.findu.security.infrastructure.dynamodb.RevokedTokenTableAttributes;
import com.findu.security.util.TokenHashUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Sesiones de token en DynamoDB (AWS o LocalStack): ítem con jwt, jwt_refresh, available; GSI por refresh.
 */
@Component
@ConditionalOnProperty(name = "findu.aws.dynamodb.enabled", havingValue = "true")
public class RevokedTokenDynamoAdapter implements RevokedTokenRepositoryPort {

    private final DynamoDbAsyncClient client;
    private final String tableName;

    public RevokedTokenDynamoAdapter(DynamoDbAsyncClient client, DynamoDbProperties properties) {
        this.client = client;
        this.tableName = properties.revokedTokensTable() != null && !properties.revokedTokensTable().isBlank()
                ? properties.revokedTokensTable()
                : "findu_revoked_tokens";
    }

    @Override
    public Mono<Void> saveTokenPair(String accessJwt, String refreshJwt, long ttlEpochSeconds) {
        if (accessJwt == null || accessJwt.isBlank() || refreshJwt == null || refreshJwt.isBlank()) {
            return Mono.empty();
        }
        String accessH = TokenHashUtils.sha256Hex(accessJwt);
        String refreshH = TokenHashUtils.sha256Hex(refreshJwt);
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH, s(accessH));
        item.put(RevokedTokenTableAttributes.REFRESH_TOKEN_HASH, s(refreshH));
        item.put(RevokedTokenTableAttributes.JWT, s(accessJwt));
        item.put(RevokedTokenTableAttributes.JWT_REFRESH, s(refreshJwt));
        item.put(RevokedTokenTableAttributes.AVAILABLE, AttributeValue.builder().bool(true).build());
        item.put(RevokedTokenTableAttributes.TTL, n(ttlEpochSeconds));
        PutItemRequest req = PutItemRequest.builder().tableName(tableName).item(item).build();
        return Mono.fromFuture(client.putItem(req)).then();
    }

    @Override
    public Mono<Void> markSessionUnavailable(String accessJwt, String refreshJwtOptional, long ttlEpochSeconds) {
        if (accessJwt == null || accessJwt.isBlank()) {
            return Mono.empty();
        }
        String accessH = TokenHashUtils.sha256Hex(accessJwt);
        return getItemByPk(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH, accessH)
                .switchIfEmpty(getItemByPk(RevokedTokenTableAttributes.TOKEN_HASH_LEGACY, accessH))
                .flatMap(item -> {
                    String pkAttr = resolvePkAttr(item);
                    String pkVal = resolvePkValue(item, pkAttr);
                    if (pkVal == null || pkVal.isBlank()) {
                        return putRevokedStub(accessJwt, refreshJwtOptional, accessH, ttlEpochSeconds);
                    }
                    return updateAvailableFalse(pkAttr, pkVal);
                })
                .switchIfEmpty(Mono.defer(() -> putRevokedStub(accessJwt, refreshJwtOptional, accessH, ttlEpochSeconds)));
    }

    @Override
    public Mono<Boolean> isAccessBlocked(String accessJwt) {
        if (accessJwt == null || accessJwt.isBlank()) {
            return Mono.just(false);
        }
        String h = TokenHashUtils.sha256Hex(accessJwt);
        return getItemByPk(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH, h)
                .switchIfEmpty(getItemByPk(RevokedTokenTableAttributes.TOKEN_HASH_LEGACY, h))
                .map(this::blockedFromItem)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> isRefreshBlocked(String refreshJwt) {
        if (refreshJwt == null || refreshJwt.isBlank()) {
            return Mono.just(false);
        }
        String refreshH = TokenHashUtils.sha256Hex(refreshJwt);
        return queryByRefreshHash(refreshH)
                .map(this::blockedFromItem)
                .switchIfEmpty(
                        getItemByPk(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH, refreshH)
                                .switchIfEmpty(getItemByPk(RevokedTokenTableAttributes.TOKEN_HASH_LEGACY, refreshH))
                                .map(this::blockedFromItem)
                )
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> rotateSession(String oldRefreshJwt, String newAccessJwt, String newRefreshJwt, long ttlEpochSeconds) {
        if (newAccessJwt == null || newAccessJwt.isBlank() || newRefreshJwt == null || newRefreshJwt.isBlank()) {
            return Mono.empty();
        }
        if (oldRefreshJwt == null || oldRefreshJwt.isBlank()) {
            return saveTokenPair(newAccessJwt, newRefreshJwt, ttlEpochSeconds);
        }
        String oldRefreshH = TokenHashUtils.sha256Hex(oldRefreshJwt);
        return queryByRefreshHash(oldRefreshH)
                .flatMap(item -> {
                    String pkAttr = resolvePkAttr(item);
                    String pkVal = resolvePkValue(item, pkAttr);
                    if (pkVal == null || pkVal.isBlank()) {
                        return Mono.empty();
                    }
                    DeleteItemRequest del = DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(Map.of(pkAttr, s(pkVal)))
                            .build();
                    return Mono.fromFuture(client.deleteItem(del)).then();
                })
                .then(saveTokenPair(newAccessJwt, newRefreshJwt, ttlEpochSeconds));
    }

    private static String resolvePkAttr(Map<String, AttributeValue> item) {
        if (item.containsKey(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH)) {
            return RevokedTokenTableAttributes.ACCESS_TOKEN_HASH;
        }
        if (item.containsKey(RevokedTokenTableAttributes.TOKEN_HASH_LEGACY)) {
            return RevokedTokenTableAttributes.TOKEN_HASH_LEGACY;
        }
        return RevokedTokenTableAttributes.ACCESS_TOKEN_HASH;
    }

    private static String resolvePkValue(Map<String, AttributeValue> item, String pkAttr) {
        AttributeValue av = item.get(pkAttr);
        return av != null ? av.s() : null;
    }

    private Mono<Void> updateAvailableFalse(String pkAttr, String pkValue) {
        UpdateItemRequest req = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(pkAttr, s(pkValue)))
                .updateExpression("SET #a = :f")
                .expressionAttributeNames(Map.of("#a", RevokedTokenTableAttributes.AVAILABLE))
                .expressionAttributeValues(Map.of(":f", AttributeValue.builder().bool(false).build()))
                .build();
        return Mono.fromFuture(client.updateItem(req)).then();
    }

    private Mono<Void> putRevokedStub(String accessJwt, String refreshJwtOptional, String accessH, long ttlEpochSeconds) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH, s(accessH));
        item.put(RevokedTokenTableAttributes.JWT, s(accessJwt));
        item.put(RevokedTokenTableAttributes.AVAILABLE, AttributeValue.builder().bool(false).build());
        item.put(RevokedTokenTableAttributes.TTL, n(ttlEpochSeconds));
        if (refreshJwtOptional != null && !refreshJwtOptional.isBlank()) {
            String refreshH = TokenHashUtils.sha256Hex(refreshJwtOptional);
            item.put(RevokedTokenTableAttributes.REFRESH_TOKEN_HASH, s(refreshH));
            item.put(RevokedTokenTableAttributes.JWT_REFRESH, s(refreshJwtOptional));
        }
        PutItemRequest req = PutItemRequest.builder().tableName(tableName).item(item).build();
        return Mono.fromFuture(client.putItem(req)).then();
    }

    private Mono<Map<String, AttributeValue>> getItemByPk(String pkName, String pkValue) {
        if (pkValue == null || pkValue.isBlank()) {
            return Mono.empty();
        }
        GetItemRequest req = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(pkName, s(pkValue)))
                .consistentRead(true)
                .build();
        return Mono.fromFuture(client.getItem(req))
                .map(r -> r.item())
                .filter(m -> m != null && !m.isEmpty());
    }

    private Mono<Map<String, AttributeValue>> queryByRefreshHash(String refreshHash) {
        QueryRequest req = QueryRequest.builder()
                .tableName(tableName)
                .indexName(RevokedTokenTableAttributes.GSI_REFRESH_TOKEN_HASH)
                .keyConditionExpression("refresh_token_hash = :r")
                .expressionAttributeValues(Map.of(":r", s(refreshHash)))
                .limit(1)
                .build();
        return Mono.fromFuture(client.query(req))
                .flatMap(r -> {
                    if (r.items() == null || r.items().isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.just(r.items().get(0));
                });
    }

    private boolean blockedFromItem(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (item.containsKey(RevokedTokenTableAttributes.TOKEN_TYPE_LEGACY)
                && !item.containsKey(RevokedTokenTableAttributes.AVAILABLE)) {
            return true;
        }
        if (item.containsKey(RevokedTokenTableAttributes.AVAILABLE)) {
            AttributeValue av = item.get(RevokedTokenTableAttributes.AVAILABLE);
            return av != null && Boolean.FALSE.equals(av.bool());
        }
        return false;
    }

    private static AttributeValue s(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private static AttributeValue n(long v) {
        return AttributeValue.builder().n(String.valueOf(v)).build();
    }
}
