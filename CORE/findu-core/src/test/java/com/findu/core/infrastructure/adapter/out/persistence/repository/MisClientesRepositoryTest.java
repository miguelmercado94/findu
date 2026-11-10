package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.domain.model.ClienteVinculoResumen;
import com.findu.core.domain.model.ServicioHistorialItem;
import com.findu.core.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ClienteProveedorFavoritoEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.OfertaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilClienteEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ProveedorClienteClasificacionEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ServicioEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.SolicitudServicioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Valida el JPQL real contra H2: vinculo, baja, contador, orden, busqueda, aislamiento entre proveedores. */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.cloud.config.enabled=false",
        "spring.config.import="
})
class MisClientesRepositoryTest {

    private static final long PROV = 10L;
    private static final long OTRO_PROV = 11L;

    @Autowired
    private TestEntityManager em;
    @Autowired
    private MisClientesRepository repository;

    private Map<String, Long> ids;
    private Map<String, UUID> refs;
    private long servicioId;

    @BeforeEach
    void seed() {
        CategoriaEntity cat = em.persist(CategoriaEntity.builder().nombre("Hogar").build());
        servicioId = em.persist(ServicioEntity.builder().nombre("Plomeria").categoriaId(cat.getId()).build()).getId();

        long zoe = cliente("Zoe");      // en servicio (ACEPTADA)
        long mario = cliente("Mario");  // favorito interno
        long ana = cliente("Ana");      // normal
        long beto = cliente("Beto");    // vinculo dado de baja
        long cami = cliente("Cami");    // FINALIZADA + CANCELADA + EN_CURSO
        long dora = cliente("Dora");    // sin vinculo con PROV
        long eva = cliente("Eva");      // vinculo solo con OTRO_PROV
        ids = Map.of("zoe", zoe, "mario", mario, "ana", ana, "beto", beto, "cami", cami, "dora", dora, "eva", eva);

        refs = Map.of(
                "zoe", vinculo(zoe, PROV, true), "mario", vinculo(mario, PROV, true), "ana", vinculo(ana, PROV, true),
                "beto", vinculo(beto, PROV, false), "cami", vinculo(cami, PROV, true), "eva", vinculo(eva, OTRO_PROV, true));

        em.persist(ProveedorClienteClasificacionEntity.builder().perfilProveedorId(PROV).perfilClienteId(mario)
                .favoritoInterno(true).updatedAt(LocalDateTime.now()).build());

        solicitud(zoe, "ACEPTADA", PROV, "ACEPTADA");
        solicitud(cami, "FINALIZADA", PROV, "ACEPTADA");
        solicitud(cami, "CANCELADA", PROV, "ACEPTADA");
        solicitud(cami, "EN_CURSO", PROV, "ACEPTADA");
        solicitud(cami, "FINALIZADA", OTRO_PROV, "ACEPTADA");   // de otro proveedor: no cuenta ni aparece
        solicitud(cami, "FINALIZADA", PROV, "RECHAZADA");       // oferta no aceptada: no cuenta
        solicitud(dora, "FINALIZADA", PROV, "ACEPTADA");        // sin vinculo: invisible
        solicitud(eva, "FINALIZADA", OTRO_PROV, "ACEPTADA");
        em.flush();
        em.clear();
    }

    private long cliente(String nombre) {
        long n = System.nanoTime();
        return em.persist(PerfilClienteEntity.builder().authUserId(n).username("u" + n).email(n + "@x.co")
                .nombreCompleto(nombre).numeroIdentificacion("N" + n).tipoIdentificacion("CC")
                .urlImagenPerfil("Zoe".equals(nombre) ? "data:image/png;base64,AAAA" : null).build()).getId();
    }

    private UUID vinculo(long clienteId, long provId, boolean activo) {
        UUID ref = UUID.randomUUID();
        em.persist(ClienteProveedorFavoritoEntity.builder().perfilClienteId(clienteId).perfilProveedorId(provId)
                .clienteRef(ref).activo(activo).favoritoDesde(LocalDateTime.now()).build());
        return ref;
    }

    private void solicitud(long clienteId, String estado, long provId, String estadoOferta) {
        long sid = em.persist(SolicitudServicioEntity.builder().perfilClienteId(clienteId).servicioId(servicioId)
                .direccionId(1L).estadoSolicitud(estado).fechaProgramada(LocalDateTime.now().plusMinutes(sid())).build()).getId();
        em.persist(OfertaEntity.builder().solicitudServicioId(sid).perfilProveedorId(provId)
                .valorPropuesto(BigDecimal.TEN).estadoOferta(estadoOferta).build());
    }

    private static long contador = 0;

    private static long sid() {
        return contador++;
    }

    @Test
    void listaSoloVinculadosIncluyendoBajaYOrdenaEnServicioFavoritoAlfabetico() {
        List<ClienteVinculoResumen> lista = repository.listar(PROV, "%%", PageRequest.of(0, 50)).getContent();
        assertEquals(List.of("Cami", "Zoe", "Mario", "Ana", "Beto"),
                lista.stream().map(ClienteVinculoResumen::nombreCompleto).toList());
    }

    @Test
    void sinVinculoNoApareceYDetalleEsVacio() {
        assertFalse(repository.listar(PROV, "%%", PageRequest.of(0, 50)).stream()
                .anyMatch(c -> c.nombreCompleto().equals("Dora") || c.nombreCompleto().equals("Eva")));
        assertTrue(repository.detalle(PROV, refs.get("eva")).isEmpty());          // ref de otro proveedor no resuelve
        assertTrue(repository.detalle(PROV, UUID.randomUUID()).isEmpty());
        assertTrue(repository.detalle(OTRO_PROV, refs.get("zoe")).isEmpty());
    }

    @Test
    void tras_baja_sigue_en_lista_con_activo_false() {
        ClienteVinculoResumen beto = repository.detalle(PROV, refs.get("beto")).orElseThrow();
        assertFalse(beto.activo());
        assertEquals(0L, beto.serviciosRealizados());
    }

    @Test
    void contadorCuentaSoloFinalizadasDeEsteProveedorConOfertaAceptada() {
        ClienteVinculoResumen cami = repository.detalle(PROV, refs.get("cami")).orElseThrow();
        assertEquals(1L, cami.serviciosRealizados());
        assertTrue(cami.enServicio());
        ClienteVinculoResumen zoe = repository.detalle(PROV, refs.get("zoe")).orElseThrow();
        assertEquals(0L, zoe.serviciosRealizados());
        assertTrue(zoe.enServicio());
        assertEquals(0L, repository.detalle(PROV, refs.get("ana")).orElseThrow().serviciosRealizados());
    }

    @Test
    void marcasYFotoSeCalculan() {
        assertTrue(repository.detalle(PROV, refs.get("mario")).orElseThrow().favoritoInterno());
        assertFalse(repository.detalle(PROV, refs.get("ana")).orElseThrow().favoritoInterno());
        assertTrue(repository.detalle(PROV, refs.get("zoe")).orElseThrow().tieneFoto());
        assertFalse(repository.detalle(PROV, refs.get("ana")).orElseThrow().tieneFoto());
    }

    @Test
    void busquedaPorNombreYPaginacion() {
        assertEquals(List.of("Ana"), repository.listar(PROV, "%an%", PageRequest.of(0, 50)).stream()
                .map(ClienteVinculoResumen::nombreCompleto).toList());
        var pagina = repository.listar(PROV, "%%", PageRequest.of(1, 2));
        assertEquals(5, pagina.getTotalElements());
        assertEquals(2, pagina.getContent().size());
        assertEquals(3, pagina.getTotalPages());
    }

    @Test
    void historialSoloTrae_solicitudesAceptadasPorEseProveedor() {
        var h = repository.historial(PROV, ids.get("cami"), PageRequest.of(0, 20));
        assertEquals(3, h.getTotalElements());       // FINALIZADA, CANCELADA, EN_CURSO (no la de oferta rechazada ni la de otro proveedor)
        List<String> estados = h.getContent().stream().map(ServicioHistorialItem::estado).sorted().toList();
        assertEquals(List.of("CANCELADA", "EN_CURSO", "FINALIZADA"), estados);
        assertEquals("Plomeria", h.getContent().get(0).servicio());
        assertEquals("Hogar", h.getContent().get(0).categoria());
    }

    @Test
    void fotoDelCliente() {
        assertEquals("data:image/png;base64,AAAA", repository.fotoCliente(ids.get("zoe")).orElseThrow());
    }
}