package com.findu.dispatcher.application.usecase;

import com.findu.dispatcher.domain.model.*;
import com.findu.dispatcher.domain.port.input.GestionCanalUseCase;
import com.findu.dispatcher.domain.port.output.ChannelRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestionCanalUseCaseImpl implements GestionCanalUseCase {

    private final ChannelRepositoryPort channelRepository;

    @Override
    public Mono<CanalComunicacion> abrirCanal(String canalId, TipoCanal tipo, List<Interoperante> interoperantes) {
        log.info("Abriendo canal de comunicación: canalId={}, tipo={}", canalId, tipo);
        CanalComunicacion nuevoCanal = new CanalComunicacion(canalId, tipo, interoperantes);
        nuevoCanal.setEstado(EstadoCanal.ACTIVO);
        return channelRepository.guardar(nuevoCanal);
    }

    @Override
    public Mono<CanalComunicacion> agregarEvento(String canalId, EventoCanal evento) {
        return channelRepository.buscarPorId(canalId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El canal no existe: " + canalId)))
                .flatMap(canal -> {
                    if (canal.getEstado() == EstadoCanal.CERRADO) {
                        log.warn("Intento de agregar evento a un canal CERRADO: canalId={}", canalId);
                        return Mono.error(new IllegalStateException("No se pueden agregar eventos a un canal CERRADO"));
                    }
                    canal.agregarEvento(evento);
                    return channelRepository.guardar(canal);
                });
    }

    @Override
    public Mono<CanalComunicacion> cambiarEstadoCanal(String canalId, EstadoCanal estado) {
        log.info("Cambiando estado del canal: canalId={}, nuevoEstado={}", canalId, estado);
        return channelRepository.cambiarEstado(canalId, estado);
    }

    @Override
    public Mono<CanalComunicacion> obtenerCanal(String canalId) {
        return channelRepository.buscarPorId(canalId);
    }

    @Override
    public Mono<List<CanalComunicacion>> reactivarCanalesUsuario(String usuarioId, String rol) {
        log.info("Reactivando canales inactivos para usuario: username={}, rol={}", usuarioId, rol);
        return channelRepository.buscarPorUsuarioId(usuarioId)
                .filter(canal -> canal.getEstado() == EstadoCanal.INACTIVO)
                .flatMap(canal -> {
                    canal.setEstado(EstadoCanal.ACTIVO);
                    log.info("Canal {} reactivado a estado ACTIVO para usuario {}", canal.getId(), usuarioId);
                    return channelRepository.guardar(canal);
                })
                .collectList();
    }
}
