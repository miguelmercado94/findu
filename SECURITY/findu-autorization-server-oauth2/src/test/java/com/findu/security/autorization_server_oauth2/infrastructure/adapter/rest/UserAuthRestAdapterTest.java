package com.findu.security.autorization_server_oauth2.infrastructure.adapter.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAuthRestAdapterTest {

    private UserAuthRestAdapter adapter;

    @BeforeEach
    void setUp() {
        // We pass a dummy url since we won't execute real HTTP requests in this unit test
        adapter = new UserAuthRestAdapter("http://localhost:8080");
    }

    @Test
    void determineRole_adminClient_returnsAdministratorRole() {
        String role = ReflectionTestUtils.invokeMethod(adapter, "determineRole", "findu-admin");
        assertEquals("ROLE_ADMINISTRATOR", role);
    }

    @Test
    void determineRole_proveedorClient_returnsProveedorRole() {
        String role = ReflectionTestUtils.invokeMethod(adapter, "determineRole", "findu-proveedor");
        assertEquals("ROLE_OUR_PROVEEDOR", role);
    }

    @Test
    void determineRole_otherClient_returnsDefaultClienteRole() {
        String role = ReflectionTestUtils.invokeMethod(adapter, "determineRole", "findu-cliente");
        assertEquals("ROLE_OUR_CLIENTE", role);
    }

    @Test
    void determineRole_nullClient_returnsDefaultClienteRole() {
        String role = ReflectionTestUtils.invokeMethod(adapter, "determineRole", (String) null);
        assertEquals("ROLE_OUR_CLIENTE", role);
    }
}
