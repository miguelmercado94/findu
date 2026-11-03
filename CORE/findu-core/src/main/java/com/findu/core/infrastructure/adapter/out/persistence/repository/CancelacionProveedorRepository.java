package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.CancelacionProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CancelacionProveedorRepository extends JpaRepository<CancelacionProveedorEntity, Long> {
    List<CancelacionProveedorEntity> findByPerfilProveedorIdOrderByCreatedAtDesc(Long perfilProveedorId);
    List<CancelacionProveedorEntity> findBySolicitudServicioIdOrderByCreatedAtDesc(Long solicitudServicioId);
    Optional<CancelacionProveedorEntity> findByTicketCodigo(String ticketCodigo);
    List<CancelacionProveedorEntity> findByAmonestacionGeneradaFalse();
}

