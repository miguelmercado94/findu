package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.application.port.in.GetTicketsUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpTicketDocument;
import com.findu.help.v2.infrastructure.adapter.out.persistence.mapper.HelpTicketMongoMapper;
import com.findu.help.v2.infrastructure.adapter.out.persistence.repository.SpringDataHelpTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MongoHelpTicketRepository implements HelpTicketRepository {

    private final SpringDataHelpTicketRepository springDataHelpTicketRepository;
    private final MongoTemplate mongoTemplate;
    private final HelpTicketMongoMapper mapper;

    @Override
    public HelpTicket save(HelpTicket ticket) {
        HelpTicketDocument doc = mapper.toDocument(ticket);
        HelpTicketDocument saved = springDataHelpTicketRepository.save(doc);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<HelpTicket> findById(String id) {
        return springDataHelpTicketRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<HelpTicket> findByTicketCode(TicketCode ticketCode) {
        if (ticketCode == null || ticketCode.getValue() == null) {
            return Optional.empty();
        }
        return springDataHelpTicketRepository.findByTicketCode(ticketCode.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<HelpTicket> findByExternalRef(String externalRef) {
        if (externalRef == null || externalRef.isBlank()) {
            return Optional.empty();
        }
        return springDataHelpTicketRepository.findByExternalRef(externalRef)
                .map(mapper::toDomain);
    }

    @Override
    public List<HelpTicket> findByFilter(GetTicketsUseCase.FilterQuery query) {
        Query mongoQuery = new Query();

        if (query != null) {
            if (query.getStatus() != null) {
                mongoQuery.addCriteria(Criteria.where("status").is(query.getStatus()));
            }
            if (query.getPriority() != null) {
                mongoQuery.addCriteria(Criteria.where("priority").is(query.getPriority()));
            }
            if (query.getUserRole() != null) {
                mongoQuery.addCriteria(Criteria.where("requester.userRole").is(query.getUserRole()));
            }
            if (query.getUserId() != null) {
                mongoQuery.addCriteria(Criteria.where("requester.userId").is(query.getUserId()));
            }
            if (query.getAssignedAgentId() != null) {
                mongoQuery.addCriteria(Criteria.where("assignedAgent.agentId").is(query.getAssignedAgentId()));
            }

            if (query.getPage() != null && query.getSize() != null && query.getSize() > 0) {
                int page = Math.max(0, query.getPage());
                mongoQuery.with(PageRequest.of(page, query.getSize()));
            }
        }

        List<HelpTicketDocument> docs = mongoTemplate.find(mongoQuery, HelpTicketDocument.class);
        return docs.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
