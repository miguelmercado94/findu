package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.PerfilProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerfilProveedorRepository extends JpaRepository<PerfilProveedorEntity, Long> {

    Optional<PerfilProveedorEntity> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    @Query("""
        SELECT p FROM PerfilProveedorEntity p\s
        WHERE p.disponible = true\s
          AND p.estado = 'ACTIVO'\s
          AND p.id IN (SELECT e.perfilProveedorId FROM PerfilEspecialistaEntity e WHERE e.servicioId = :servicioId AND e.active = true)
          AND (
            :municipioId IS NULL OR
            p.id IN (SELECT c.perfilProveedorId FROM ProveedorCoberturaEntity c WHERE c.municipioId = :municipioId)
            OR NOT EXISTS (SELECT 1 FROM ProveedorCoberturaEntity c2 WHERE c2.perfilProveedorId = p.id)
          )
    """)
    List<PerfilProveedorEntity> findEligibleProviders(@Param("servicioId") Long servicioId, @Param("municipioId") Long municipioId);

    List<PerfilProveedorEntity> findByDisponibleTrueAndEstado(String estado);
}
