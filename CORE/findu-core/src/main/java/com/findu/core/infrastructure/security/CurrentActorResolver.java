package com.findu.core.infrastructure.security;

import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Identifica al actor autenticado de los endpoints de "Mis clientes" y favoritos. A diferencia de
 * {@link ProviderOwnershipGuard}, la autenticacion es SIEMPRE obligatoria (tambien con authz.mode=OFF, donde el
 * {@link JwtContextFilter} no corre), por eso valida el Bearer directamente con {@link JwtVerifier}.
 * Ignora X-Internal-Service-Key. 401: sin token o token invalido. 403: token valido pero sin perfil adecuado.
 */
@Component
public class CurrentActorResolver {

    private final JwtVerifier verifier;
    private final PerfilProveedorRepository proveedorRepository;
    private final PerfilClienteRepository clienteRepository;

    public CurrentActorResolver(JwtVerifier verifier, PerfilProveedorRepository proveedorRepository,
                                PerfilClienteRepository clienteRepository) {
        this.verifier = verifier;
        this.proveedorRepository = proveedorRepository;
        this.clienteRepository = clienteRepository;
    }

    public AuthenticatedPrincipal requirePrincipal(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank() || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
        }
        Optional<AuthenticatedPrincipal> principal = verifier.verify(auth.substring(7));
        return principal.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida"));
    }

    /** perfil_proveedor.id del JWT (claim userId == perfil_proveedor.auth_user_id). */
    public Long requirePerfilProveedorId(HttpServletRequest request) {
        AuthenticatedPrincipal p = requirePrincipal(request);
        return proveedorRepository.findByAuthUserId(p.userId())
                .map(perfil -> perfil.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado"));
    }

    /** Exige que perfil_cliente {@code perfilClienteId} pertenezca al usuario del JWT. */
    public Long requirePerfilClienteDueno(HttpServletRequest request, Long perfilClienteId) {
        AuthenticatedPrincipal p = requirePrincipal(request);
        boolean dueno = perfilClienteId != null && clienteRepository.findById(perfilClienteId)
                .map(c -> p.userId().equals(c.getAuthUserId()))
                .orElse(false);
        if (!dueno) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }
        return perfilClienteId;
    }
}