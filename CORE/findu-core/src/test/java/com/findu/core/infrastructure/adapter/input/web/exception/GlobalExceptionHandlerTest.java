package com.findu.core.infrastructure.adapter.input.web.exception;

import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ArchivoProveedorException.Codigo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/x");

    private void assertArchivo(Codigo codigo, HttpStatus esperado) {
        ResponseEntity<ErrorResponse> r = handler.handleArchivoProveedor(new ArchivoProveedorException(codigo, "m"), request);
        assertThat(r.getStatusCode()).isEqualTo(esperado);
        assertThat(r.getBody().error()).isEqualTo(codigo.name());
    }

    @Test
    void archivoProveedorSeMapeaASuCodigoHttp() {
        assertArchivo(Codigo.BIOMETRIA_REQUERIDA, HttpStatus.CONFLICT);
        assertArchivo(Codigo.ROSTRO_BIOMETRICO_NO_ENCONTRADO, HttpStatus.UNPROCESSABLE_ENTITY);
        assertArchivo(Codigo.IMAGEN_INVALIDA, HttpStatus.UNPROCESSABLE_ENTITY);
        assertArchivo(Codigo.ARCHIVO_DEMASIADO_GRANDE, HttpStatus.PAYLOAD_TOO_LARGE);
        assertArchivo(Codigo.FORMATO_NO_SOPORTADO, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void responseStatusExceptionConservaSu401Y403() {
        ResponseEntity<ErrorResponse> r401 = handler.handleResponseStatus(
                new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida"), request);
        assertThat(r401.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r401.getBody().message()).isEqualTo("Autenticacion requerida");
        assertThat(handler.handleResponseStatus(
                new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void multipartExcedidoEs413() {
        assertThat(handler.handleMaxUpload(new MaxUploadSizeExceededException(1L), request).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void illegalStateExceptionDeLaGuardaDeNombreEs409() {
        ResponseEntity<ErrorResponse> r = handler.handleConflict(
                new IllegalStateException("Los datos verificados no se pueden modificar; abre un ticket de soporte"), request);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().message()).isEqualTo("Los datos verificados no se pueden modificar; abre un ticket de soporte");
    }
}
