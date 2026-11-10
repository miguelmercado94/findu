package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.domain.model.ClienteVinculoResumen;
import com.findu.core.domain.model.ServicioHistorialItem;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ClienteProveedorFavoritoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de solo lectura de "Mis clientes". Toda consulta parte de cliente_proveedor_favorito filtrando por el
 * proveedor, de modo que sin vinculo no hay filas. Una sola query por pagina (sin N+1).
 */
public interface MisClientesRepository extends JpaRepository<ClienteProveedorFavoritoEntity, Long> {

    /** Estados de solicitud que cuentan como "en servicio" (ver domain/model/state). Constante unica. */
    String ESTADOS_EN_SERVICIO = "'ACEPTADA','EN_CURSO'";

    /** Solicitudes del cliente que este proveedor tiene aceptadas. */
    String SOLICITUDES_DEL_PROVEEDOR = "s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o "
            + "WHERE o.perfilProveedorId = v.perfilProveedorId AND o.estadoOferta = 'ACEPTADA')";

    String SELECT_RESUMEN = "SELECT new com.findu.core.domain.model.ClienteVinculoResumen("
            + "c.id, v.clienteRef, c.nombreCompleto, "
            + "CASE WHEN c.urlImagenPerfil IS NOT NULL AND c.urlImagenPerfil <> '' THEN true ELSE false END, "
            + "COALESCE(k.favoritoInterno, false), "
            + "(SELECT COUNT(s) FROM SolicitudServicioEntity s WHERE s.perfilClienteId = c.id "
            + "AND s.estadoSolicitud = 'FINALIZADA' AND " + SOLICITUDES_DEL_PROVEEDOR + "), "
            + "CASE WHEN EXISTS (SELECT 1 FROM SolicitudServicioEntity s WHERE s.perfilClienteId = c.id "
            + "AND s.estadoSolicitud IN (" + ESTADOS_EN_SERVICIO + ") AND " + SOLICITUDES_DEL_PROVEEDOR + ") "
            + "THEN true ELSE false END, "
            + "v.activo) ";

    String FROM_RESUMEN = "FROM ClienteProveedorFavoritoEntity v "
            + "JOIN PerfilClienteEntity c ON c.id = v.perfilClienteId "
            + "LEFT JOIN ProveedorClienteClasificacionEntity k "
            + "ON k.perfilClienteId = v.perfilClienteId AND k.perfilProveedorId = v.perfilProveedorId ";

    @Query(value = SELECT_RESUMEN + FROM_RESUMEN
            + "WHERE v.perfilProveedorId = :proveedorId AND LOWER(c.nombreCompleto) LIKE :patron ESCAPE '\\' "
            + "ORDER BY CASE "
            + "WHEN EXISTS (SELECT 1 FROM SolicitudServicioEntity s WHERE s.perfilClienteId = c.id "
            + "AND s.estadoSolicitud IN (" + ESTADOS_EN_SERVICIO + ") AND " + SOLICITUDES_DEL_PROVEEDOR + ") THEN 0 "
            + "WHEN COALESCE(k.favoritoInterno, false) = true THEN 1 ELSE 2 END, "
            + "LOWER(c.nombreCompleto), v.id",
            countQuery = "SELECT COUNT(v) FROM ClienteProveedorFavoritoEntity v "
                    + "JOIN PerfilClienteEntity c ON c.id = v.perfilClienteId "
                    + "WHERE v.perfilProveedorId = :proveedorId AND LOWER(c.nombreCompleto) LIKE :patron ESCAPE '\\'")
    Page<ClienteVinculoResumen> listar(@Param("proveedorId") Long proveedorId, @Param("patron") String patron,
                                       Pageable pageable);

    @Query(SELECT_RESUMEN + FROM_RESUMEN
            + "WHERE v.perfilProveedorId = :proveedorId AND v.clienteRef = :clienteRef")
    Optional<ClienteVinculoResumen> detalle(@Param("proveedorId") Long proveedorId,
                                            @Param("clienteRef") UUID clienteRef);

    @Query(value = "SELECT new com.findu.core.domain.model.ServicioHistorialItem("
            + "sv.nombre, cat.nombre, s.fechaProgramada, s.estadoSolicitud) "
            + "FROM SolicitudServicioEntity s "
            + "LEFT JOIN ServicioEntity sv ON sv.id = s.servicioId "
            + "LEFT JOIN CategoriaEntity cat ON cat.id = sv.categoriaId "
            + "WHERE s.perfilClienteId = :clienteId AND s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o "
            + "WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA') "
            + "ORDER BY s.fechaProgramada DESC, s.id DESC",
            countQuery = "SELECT COUNT(s) FROM SolicitudServicioEntity s "
                    + "WHERE s.perfilClienteId = :clienteId AND s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o "
                    + "WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA')")
    Page<ServicioHistorialItem> historial(@Param("proveedorId") Long proveedorId, @Param("clienteId") Long clienteId,
                                          Pageable pageable);

    @Query("SELECT c.urlImagenPerfil FROM PerfilClienteEntity c WHERE c.id = :clienteId")
    Optional<String> fotoCliente(@Param("clienteId") Long clienteId);
}