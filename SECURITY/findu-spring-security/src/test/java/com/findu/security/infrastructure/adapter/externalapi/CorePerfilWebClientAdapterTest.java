package com.findu.security.infrastructure.adapter.externalapi;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorePerfilWebClientAdapterTest {

    private static final String KEY = "test-key";
    private HttpServer server;
    private final Map<String, String> keysByRequest = new ConcurrentHashMap<>();
    private CorePerfilWebClientAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            keysByRequest.put(ex.getRequestMethod() + " " + ex.getRequestURI().getPath(),
                    String.valueOf(ex.getRequestHeaders().getFirst("X-Internal-Service-Key")));
            byte[] body = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        adapter = new CorePerfilWebClientAdapter(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "/perfil", "/internal/perfiles", KEY);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchByAuthUserId_sendsInternalKey() {
        adapter.fetchByAuthUserId(2L).block();
        assertEquals(KEY, keysByRequest.get("GET /perfil/auth/2"));
    }

    @Test
    void pushProfilePhoto_sendsInternalKey() {
        adapter.pushProfilePhoto(1L, "Nombre", "http://x/y.jpg").block();
        assertEquals(KEY, keysByRequest.get("PUT /perfil/1"));
    }
}
