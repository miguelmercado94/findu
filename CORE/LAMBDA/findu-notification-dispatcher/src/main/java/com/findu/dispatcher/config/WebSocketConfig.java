package com.findu.dispatcher.config;

import com.findu.dispatcher.infrastructure.websocket.EventWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Value("${findu.websocket.endpoint:/ws/events}")
    private String websocketEndpoint;

    @Bean
    public HandlerMapping webSocketHandlerMapping(EventWebSocketHandler eventWebSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put(websocketEndpoint, eventWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
