package com.findu.dispatcher.infrastructure.adapter.redis;

import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
public class RedisReactivePubSubAdapter implements RedisPubSubPort {

    private static final String PRESENCE_PREFIX = "user:presence:";
    private static final String CLUSTER_EVENT_CHANNEL_PREFIX = "user:events:";
    private static final String CLUSTER_EVENT_CHANNEL_PATTERN = "user:events:*";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ReactiveRedisMessageListenerContainer listenerContainer;
    private final SessionRegistryPort sessionRegistry;

    public RedisReactivePubSubAdapter(ReactiveStringRedisTemplate redisTemplate,
                                       ReactiveRedisMessageListenerContainer listenerContainer,
                                       SessionRegistryPort sessionRegistry) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.sessionRegistry = sessionRegistry;
    }

    @PostConstruct
    public void initClusterEventListener() {
        listenerContainer.receive(PatternTopic.of(CLUSTER_EVENT_CHANNEL_PATTERN))
                .flatMap(msg -> {
                    String channel = msg.getChannel();
                    String eventJson = msg.getMessage();
                    String userId = channel.substring(CLUSTER_EVENT_CHANNEL_PREFIX.length());

                    log.debug("Evento recibido de Redis Pub/Sub para userId={}: {}", userId, eventJson);
                    return sessionRegistry.sendToUserLocally(userId, eventJson);
                })
                .doOnError(err -> log.error("Error en listener de Redis Pub/Sub: {}", err.getMessage()))
                .subscribe();
    }

    @Override
    public Mono<Void> registerPresence(String userId, String nodeId) {
        String key = PRESENCE_PREFIX + userId;
        return redisTemplate.opsForSet().add(key, nodeId)
                .then(redisTemplate.expire(key, Duration.ofHours(24)))
                .doOnSuccess(v -> log.debug("Presencia registrada en Redis cluster para userId: {}", userId))
                .then();
    }

    @Override
    public Mono<Void> removePresence(String userId, String nodeId) {
        String key = PRESENCE_PREFIX + userId;
        return redisTemplate.opsForSet().remove(key, nodeId)
                .then();
    }

    @Override
    public Mono<Boolean> isUserOnlineInCluster(String userId) {
        String key = PRESENCE_PREFIX + userId;
        return redisTemplate.opsForSet().size(key)
                .map(size -> size != null && size > 0);
    }

    @Override
    public Mono<Void> publishEventToCluster(String userId, String eventJson) {
        String channel = CLUSTER_EVENT_CHANNEL_PREFIX + userId;
        return redisTemplate.convertAndSend(channel, eventJson)
                .doOnSuccess(recipients -> log.info("Evento publicado a Redis Pub/Sub para userId={}. Subscriptores alcanzados: {}", userId, recipients))
                .then();
    }
}
