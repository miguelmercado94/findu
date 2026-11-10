package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.port.in.GestionArchivosProveedorUseCase;
import com.findu.core.application.port.in.GestionArchivosProveedorUseCase.FotoPerfil;
import com.findu.core.application.util.Base64Images;
import com.findu.core.infrastructure.security.ProviderOwnershipGuard;
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
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/perfil-proveedor/{id}/foto-perfil")
@RequiredArgsConstructor
@Tag(name = "Perfil Proveedor", description = "Foto de perfil del proveedor")
public class ProveedorArchivosController {

    private static final String SCOPE = "archivos";

    private final GestionArchivosProveedorUseCase archivosUseCase;
    private final ProviderOwnershipGuard ownershipGuard;

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
        ownershipGuard.requirePerfil(SCOPE, id);
        byte[] bytes = Base64Images.decode(request != null ? request.imagenBase64() : null);
        return ResponseEntity.ok(new FotoPerfilResponse(archivosUseCase.cambiarFotoPerfil(id, bytes)));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cambiar foto de perfil (multipart, parte 'file')")
    public ResponseEntity<FotoPerfilResponse> cambiarMultipart(@PathVariable Long id,
                                                               @RequestPart("file") MultipartFile file) throws IOException {
        ownershipGuard.requirePerfil(SCOPE, id);
        return ResponseEntity.ok(new FotoPerfilResponse(archivosUseCase.cambiarFotoPerfil(id, file.getBytes())));
    }
}
