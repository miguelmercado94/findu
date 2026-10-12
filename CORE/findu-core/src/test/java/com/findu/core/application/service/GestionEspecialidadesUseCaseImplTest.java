package com.findu.core.application.service;

import com.findu.core.application.dto.request.CrearEspecialidadRequest;
import com.findu.core.domain.model.PerfilEspecialista;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionEspecialidadesUseCaseImplTest {

    @Mock private PerfilEspecialistaService especialistaService;
    @Mock private PortafolioService portafolioService;

    @InjectMocks private GestionEspecialidadesUseCaseImpl useCase;

    @Test
    void especialidadNaceConCalificacion3_5() {
        CrearEspecialidadRequest request = new CrearEspecialidadRequest(10L, 100L, "desc", 2);
        when(especialistaService.existsByProveedorIdAndServicioId(10L, 100L)).thenReturn(false);
        when(especialistaService.save(any(PerfilEspecialista.class)))
                .thenAnswer(inv -> {
                    PerfilEspecialista arg = inv.getArgument(0);
                    arg.setId(1L);
                    return arg;
                });

        useCase.agregarEspecialidad(request);

        ArgumentCaptor<PerfilEspecialista> captor = ArgumentCaptor.forClass(PerfilEspecialista.class);
        org.mockito.Mockito.verify(especialistaService).save(captor.capture());
        assertThat(captor.getValue().getCalificacionPromedio()).isEqualByComparingTo(new BigDecimal("3.5"));
    }
}
