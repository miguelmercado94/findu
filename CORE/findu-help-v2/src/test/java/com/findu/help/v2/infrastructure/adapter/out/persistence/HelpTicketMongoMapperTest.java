package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpTicketDocument;
import com.findu.help.v2.infrastructure.adapter.out.persistence.mapper.HelpTicketMongoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class HelpTicketMongoMapperTest {

    private final HelpTicketMongoMapper mapper = Mappers.getMapper(HelpTicketMongoMapper.class);

    @Test
    @DisplayName("Debe mapear de Entidad de Dominio a Documento Mongo y viceversa de forma transparente")
    void domainToDocumentAndBack_Success() {
        HelpTicket domain = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan Perez", "juan@example.com"),
                500L, "sub-123", "Soporte", "Falla App",
                PriorityLevel.ALTA, "Error al pagar", "Detalle de error",
                new Location("Colombia", "Antioquia", "Medellin"),
                Collections.emptyList(), null
        );

        HelpTicketDocument document = mapper.toDocument(domain);

        assertThat(document).isNotNull();
        assertThat(document.getId()).isEqualTo(domain.getId());
        assertThat(document.getTicketCode()).isEqualTo(domain.getTicketCode().getValue());
        assertThat(document.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(document.getRequester().getUserId()).isEqualTo(100L);

        HelpTicket reconstituted = mapper.toDomain(document);

        assertThat(reconstituted).isNotNull();
        assertThat(reconstituted.getId()).isEqualTo(domain.getId());
        assertThat(reconstituted.getTicketCode().getValue()).isEqualTo(domain.getTicketCode().getValue());
        assertThat(reconstituted.getRequester().getUserId()).isEqualTo(domain.getRequester().getUserId());
    }
}
