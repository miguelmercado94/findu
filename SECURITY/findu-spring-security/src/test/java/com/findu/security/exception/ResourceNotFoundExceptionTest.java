package com.findu.security.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void message() {
        assertThat(new ResourceNotFoundException("x").getMessage()).isEqualTo("x");
    }
}
