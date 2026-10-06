package com.findu.help.v2.infrastructure.adapter.input.web.mapper;

import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.CreateTicketRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.LocationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class HelpTicketWebMapperTest {

    private final HelpTicketWebMapper mapper = Mappers.getMapper(HelpTicketWebMapper.class);

    @Test
    @DisplayName("Debe mapear CreateTicketRequest a CreateTicketCommand correctamente")
    void toCommand_CreateTicketRequest_Success() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setUserId(100L);
        request.setUserRole(UserRole.CLIENTE);
        request.setSolicitudId(500L);
        request.setCategoryId("cat-1");
        request.setSubcategoryId("sub-1");
        request.setSubject("Asunto de prueba");
        request.setDescription("Descripción detallada");
        request.setLocation(new LocationRequest("Colombia", "Antioquia", "Medellin"));

        CreateTicketCommand command = mapper.toCommand(request);

        assertThat(command).isNotNull();
        assertThat(command.getUserId()).isEqualTo(100L);
        assertThat(command.getUserRole()).isEqualTo(UserRole.CLIENTE);
        assertThat(command.getSolicitudId()).isEqualTo(500L);
        assertThat(command.getSubject()).isEqualTo("Asunto de prueba");
        assertThat(command.getLocation()).isNotNull();
        assertThat(command.getLocation().country()).isEqualTo("Colombia");
    }
}
