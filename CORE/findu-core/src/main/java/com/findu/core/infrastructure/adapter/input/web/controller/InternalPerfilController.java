package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.application.dto.request.internal.InternalCoberturaRequest;
import com.findu.core.application.dto.request.internal.InternalCrearPerfilRequest;
import com.findu.core.application.dto.request.internal.InternalEspecialidadItem;
import com.findu.core.application.dto.response.internal.InternalPerfilResponse;
import com.findu.core.infrastructure.adapter.out.persistence.entity.DireccionEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilClienteEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilEspecialistaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ProveedorCoberturaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.DireccionRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.EspecialistaCredencialRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilEspecialistaRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PortafolioItemRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.ProveedorCoberturaRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Hidden
@RestController
@RequestMapping("/internal/perfiles")
@RequiredArgsConstructor
@Slf4j
public class InternalPerfilController {

    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    private final PerfilProveedorRepository proveedorRepository;
    private final PerfilClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final ProveedorCoberturaRepository coberturaRepository;
    private final PerfilEspecialistaRepository especialistaRepository;
    private final PortafolioItemRepository portafolioRepository;
    private final EspecialistaCredencialRepository credencialRepository;

    private final Map<String, Long> idempotencyCache = new ConcurrentHashMap<>();

    @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}")
    private String expectedInternalKey;

    private void validateInternalAccess(String serviceKey) {
        if (serviceKey == null || !serviceKey.equals(expectedInternalKey)) {
            log.warn("Access to /internal/perfiles rejected due to invalid service key");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso interno no autorizado");
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<InternalPerfilResponse> crearPerfil(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody InternalCrearPerfilRequest request) {
        validateInternalAccess(serviceKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Long cachedId = idempotencyCache.get(idempotencyKey);
            if (cachedId != null) {
                log.info("Returning cached profile id={} for idempotencyKey={}", cachedId, idempotencyKey);
                return ResponseEntity.status(HttpStatus.CREATED).body(new InternalPerfilResponse(cachedId));
            }
        }

        String flowType = (request.flowType() != null && !request.flowType().isBlank())
                ? request.flowType().trim().toUpperCase() : "PROVEEDOR";

        InternalPerfilResponse response;
        if ("CLIENTE".equals(flowType)) {
            response = crearPerfilCliente(request);
        } else {
            response = crearPerfilProveedor(request);
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank() && response != null) {
            idempotencyCache.put(idempotencyKey, response.perfilId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private InternalPerfilResponse crearPerfilCliente(InternalCrearPerfilRequest request) {
        // Idempotencia: por authUserId si reintenta la misma cuenta
        Optional<PerfilClienteEntity> existing = clienteRepository.findByAuthUserId(request.authUserId());
        if (existing.isPresent()) {
            log.info("Client profile already exists with id={}, returning existing id", existing.get().getId());
            return new InternalPerfilResponse(existing.get().getId());
        }

        String tipoDoc = request.persona() != null && request.persona().tipoDocumento() != null && !request.persona().tipoDocumento().isBlank()
                ? request.persona().tipoDocumento().trim() : "CC";
        String numDoc = request.persona() != null && request.persona().numeroDocumento() != null
                ? request.persona().numeroDocumento().trim() : "";

        if (!numDoc.isBlank()) {
            if (clienteRepository.existsByTipoIdentificacionAndNumeroIdentificacion(tipoDoc, numDoc)
                    || proveedorRepository.existsByTipoIdentificacionAndNumeroIdentificacion(tipoDoc, numDoc)) {
                log.warn("Document collision in client profile creation: tipo={}, num={}", tipoDoc, numDoc);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión");
            }
        }

        LocalDate fechaNac = parseDate(request.persona() != null ? request.persona().fechaNacimiento() : null);

        PerfilClienteEntity cliente = PerfilClienteEntity.builder()
                .authUserId(request.authUserId())
                .username(request.username())
                .email(request.email())
                .nombreCompleto(request.persona() != null ? request.persona().nombreCompleto() : request.username())
                .numeroIdentificacion(request.persona() != null ? request.persona().numeroDocumento() : "")
                .tipoIdentificacion(request.persona() != null ? request.persona().tipoDocumento() : "CC")
                .fechaNacimiento(fechaNac)
                .sexo(request.persona() != null ? request.persona().sexo() : null)
                .celular(request.phone())
                .codPhoneInternational(request.codPhoneInternational() != null ? request.codPhoneInternational() : "+57")
                .estado("COMPLETO")
                .build();

        cliente = clienteRepository.save(cliente);

        if (request.direccion() != null && request.direccion().municipioId() != null) {
            DireccionEntity dir = DireccionEntity.builder()
                    .perfilClienteId(cliente.getId())
                    .direccionTexto(request.direccion().direccionTexto() != null ? request.direccion().direccionTexto() : "")
                    .municipioId(request.direccion().municipioId())
                    .latitud(request.direccion().lat() != null ? BigDecimal.valueOf(request.direccion().lat()) : null)
                    .longitud(request.direccion().lng() != null ? BigDecimal.valueOf(request.direccion().lng()) : null)
                    .esPrincipal(true)
                    .active(true)
                    .build();
            direccionRepository.save(dir);
        }

        return new InternalPerfilResponse(cliente.getId());
    }

    private InternalPerfilResponse crearPerfilProveedor(InternalCrearPerfilRequest request) {
        // Idempotencia: por authUserId si reintenta la misma cuenta
        Optional<PerfilProveedorEntity> existing = proveedorRepository.findByAuthUserId(request.authUserId());
        if (existing.isPresent()) {
            log.info("Provider profile already exists with id={}, returning existing id", existing.get().getId());
            return new InternalPerfilResponse(existing.get().getId());
        }

        String tipoDoc = request.persona() != null && request.persona().tipoDocumento() != null && !request.persona().tipoDocumento().isBlank()
                ? request.persona().tipoDocumento().trim() : "CC";
        String numDoc = request.persona() != null && request.persona().numeroDocumento() != null
                ? request.persona().numeroDocumento().trim() : "";

        if (!numDoc.isBlank()) {
            if (proveedorRepository.existsByTipoIdentificacionAndNumeroIdentificacion(tipoDoc, numDoc)
                    || clienteRepository.existsByTipoIdentificacionAndNumeroIdentificacion(tipoDoc, numDoc)) {
                log.warn("Document collision in provider profile creation: tipo={}, num={}", tipoDoc, numDoc);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con estos datos, inicia sesión");
            }
        }

        LocalDate fechaNac = parseDate(request.persona() != null ? request.persona().fechaNacimiento() : null);

        PerfilProveedorEntity proveedor = PerfilProveedorEntity.builder()
                .authUserId(request.authUserId())
                .nombreCompleto(request.persona() != null ? request.persona().nombreCompleto() : request.username())
                .numeroIdentificacion(request.persona() != null ? request.persona().numeroDocumento() : "")
                .tipoIdentificacion(request.persona() != null ? request.persona().tipoDocumento() : "CC")
                .fechaNacimiento(fechaNac)
                .sexo(request.persona() != null ? request.persona().sexo() : null)
                .celular(request.phone())
                .codPhoneInternational(request.codPhoneInternational() != null ? request.codPhoneInternational() : "+57")
                .estado("ACTIVO")
                .estadoVerificacion("PENDIENTE")
                .disponible(true)
                .calificacionPromedio(new BigDecimal("5.00"))
                .build();

        proveedor = proveedorRepository.save(proveedor);

        if (request.direccion() != null && request.direccion().municipioId() != null) {
            DireccionEntity dir = DireccionEntity.builder()
                    .perfilProveedorId(proveedor.getId())
                    .direccionTexto(request.direccion().direccionTexto() != null ? request.direccion().direccionTexto() : "")
                    .municipioId(request.direccion().municipioId())
                    .latitud(request.direccion().lat() != null ? BigDecimal.valueOf(request.direccion().lat()) : null)
                    .longitud(request.direccion().lng() != null ? BigDecimal.valueOf(request.direccion().lng()) : null)
                    .esPrincipal(true)
                    .active(true)
                    .build();
            direccionRepository.save(dir);
        }

        return new InternalPerfilResponse(proveedor.getId());
    }

    @DeleteMapping("/{perfilId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public ResponseEntity<Void> deletePerfil(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @PathVariable Long perfilId) {
        validateInternalAccess(serviceKey);

        log.info("Compensating perfil creation: cascade deleting perfilId={}", perfilId);

        // Intento borrar como proveedor (cascada cobertura + especialidades)
        if (proveedorRepository.existsById(perfilId)) {
            direccionRepository.deleteAllByPerfilProveedorId(perfilId);
            coberturaRepository.deleteAllByPerfilProveedorId(perfilId);

            List<PerfilEspecialistaEntity> especialidades = especialistaRepository.findAllByPerfilProveedorId(perfilId);
            for (PerfilEspecialistaEntity esp : especialidades) {
                portafolioRepository.deleteAllByPerfilEspecialistaId(esp.getId());
                credencialRepository.deleteAllByPerfilEspecialistaId(esp.getId());
            }
            especialistaRepository.deleteAllByPerfilProveedorId(perfilId);
            proveedorRepository.deleteById(perfilId);
            log.info("Provider profile and child entities deleted for id={}", perfilId);
        } else if (clienteRepository.existsById(perfilId)) {
            direccionRepository.deleteAllByPerfilClienteId(perfilId);
            clienteRepository.deleteById(perfilId);
            log.info("Client profile and child entities deleted for id={}", perfilId);
        }

        idempotencyCache.values().removeIf(id -> id.equals(perfilId));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cobertura")
    @Transactional
    public ResponseEntity<Void> addCobertura(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @PathVariable Long id,
            @RequestBody InternalCoberturaRequest request) {
        validateInternalAccess(serviceKey);

        if (request.municipioIds() != null) {
            List<ProveedorCoberturaEntity> existing = coberturaRepository.findAllByPerfilProveedorId(id);
            List<Long> existingMunIds = existing.stream().map(ProveedorCoberturaEntity::getMunicipioId).toList();

            for (Long munId : request.municipioIds()) {
                if (!existingMunIds.contains(munId)) {
                    coberturaRepository.save(ProveedorCoberturaEntity.builder()
                            .perfilProveedorId(id)
                            .municipioId(munId)
                            .build());
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/especialidades")
    @Transactional
    public ResponseEntity<Void> addEspecialidades(
            @RequestHeader(name = HEADER_INTERNAL_KEY, required = false) String serviceKey,
            @PathVariable Long id,
            @RequestBody List<InternalEspecialidadItem> items) {
        validateInternalAccess(serviceKey);

        if (items != null) {
            for (InternalEspecialidadItem item : items) {
                if (!especialistaRepository.existsByPerfilProveedorIdAndServicioId(id, item.servicioId())) {
                    especialistaRepository.save(PerfilEspecialistaEntity.builder()
                            .perfilProveedorId(id)
                            .servicioId(item.servicioId())
                            .experienciaAnios(item.experienciaAnios() != null ? item.experienciaAnios() : 0)
                            .calificacionPromedio(new BigDecimal("5.00"))
                            .active(true)
                            .build());
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            log.warn("Could not parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
