package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpTicketDocument;
import com.findu.help.v2.infrastructure.adapter.out.persistence.mapper.HelpTicketMongoMapper;
import com.findu.help.v2.infrastructure.adapter.out.persistence.repository.SpringDataHelpTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoHelpTicketRepositoryTest {

    @Mock
    private SpringDataHelpTicketRepository springDataHelpTicketRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private HelpTicketMongoMapper mapper;

    @InjectMocks
    private MongoHelpTicketRepository mongoHelpTicketRepository;

    private HelpTicket sampleTicket;
    private HelpTicketDocument sampleDoc;

    @BeforeEach
    void setUp() {
        sampleTicket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );

        sampleDoc = new HelpTicketDocument();
        sampleDoc.setId(sampleTicket.getId());
        sampleDoc.setTicketCode(sampleTicket.getTicketCode().getValue());
    }

    @Test
    @DisplayName("Debe guardar un ticket invocando a SpringDataHelpTicketRepository")
    void save_Success() {
        when(mapper.toDocument(sampleTicket)).thenReturn(sampleDoc);
        when(springDataHelpTicketRepository.save(sampleDoc)).thenReturn(sampleDoc);
        when(mapper.toDomain(sampleDoc)).thenReturn(sampleTicket);

        HelpTicket saved = mongoHelpTicketRepository.save(sampleTicket);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(sampleTicket.getId());
        verify(springDataHelpTicketRepository, times(1)).save(sampleDoc);
    }

    @Test
    @DisplayName("Debe buscar ticket por ticketCode")
    void findByTicketCode_Success() {
        TicketCode code = sampleTicket.getTicketCode();
        when(springDataHelpTicketRepository.findByTicketCode(code.getValue())).thenReturn(Optional.of(sampleDoc));
        when(mapper.toDomain(sampleDoc)).thenReturn(sampleTicket);

        Optional<HelpTicket> found = mongoHelpTicketRepository.findByTicketCode(code);

        assertThat(found).isPresent();
        assertThat(found.get().getTicketCode().getValue()).isEqualTo(code.getValue());
    }
}
