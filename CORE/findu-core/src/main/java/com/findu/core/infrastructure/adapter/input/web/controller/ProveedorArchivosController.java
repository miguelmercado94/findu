package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionArchivosProveedorUseCase;
import com.findu.core.application.port.in.GestionArchivosProveedorUseCase.FotoPerfil;
import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ArchivoProveedorException.Codigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/perfil-proveedor/{id}/foto-perfil")
@RequiredArgsConstructor
@Tag(name = "Perfil Proveedor", description = "Foto de perfil del proveedor")
public class ProveedorArchivosController {

    private final GestionArchivosProveedorUseCase archivosUseCase;

    public record FotoPerfilJsonRequest(String imagenBase64) {
    }

    public record FotoPerfilResponse(String urlImagenPerfil) {
    }

    @GetMapping
    @Operation(summary = "Obtener foto de perfil", description = "Devuelve los bytes de la foto; si falta y la biometria esta completa, se repara desde biometrico/rostro")
    public ResponseEntity<byte[]> obtener(@PathVariable Long id) {
        FotoPerfil foto = archivosUseCase.obtenerFotoPerfil(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.contentType()))
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
                .body(foto.bytes());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cambiar foto de perfil (JSON base64)")
    public ResponseEntity<FotoPerfilResponse> cambiarJson(@PathVariable Long id,
                                                          @RequestBody FotoPerfilJsonRequest request) {
        byte[] bytes = decodeBase64(request != null ? request.imagenBase64() : null);
        return ResponseEntity.ok(new FotoPerfilResponse(archivosUseCase.cambiarFotoPerfil(id, bytes)));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cambiar foto de perfil (multipart, parte 'file')")
    public ResponseEntity<FotoPerfilResponse> cambiarMultipart(@PathVariable Long id,
                                                               @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(new FotoPerfilResponse(archivosUseCase.cambiarFotoPerfil(id, file.getBytes())));
    }

    /** Acepta base64 puro o data-URI. */
    static byte[] decodeBase64(String value) {
        if (value == null || value.isBlank()) {
            throw new ArchivoProveedorException(Codigo.IMAGEN_INVALIDA, "La imagen es requerida");
        }
        String payload = value.trim();
        int comma = payload.indexOf(',');
        if (payload.regionMatches(true, 0, "data:", 0, 5) && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        try {
            return Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new ArchivoProveedorException(Codigo.IMAGEN_INVALIDA, "La imagen no es un base64 valido");
        }
    }
}
