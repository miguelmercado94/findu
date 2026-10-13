package com.findu.help.v2.infrastructure.adapter.out.persistence.repository;

import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpTicketDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataHelpTicketRepository extends MongoRepository<HelpTicketDocument, String> {
    Optional<HelpTicketDocument> findByTicketCode(String ticketCode);

    Optional<HelpTicketDocument> findByExternalRef(String externalRef);
}
