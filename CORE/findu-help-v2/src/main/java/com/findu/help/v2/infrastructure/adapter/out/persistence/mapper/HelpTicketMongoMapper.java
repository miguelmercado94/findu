package com.findu.help.v2.infrastructure.adapter.out.persistence.mapper;

import com.findu.help.v2.domain.model.ticket.*;
import com.findu.help.v2.domain.valueobject.*;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface HelpTicketMongoMapper {

    @Mapping(target = "ticketCode", source = "ticketCode.value")
    HelpTicketDocument toDocument(HelpTicket domain);

    @Mapping(target = "id", source = "messageId")
    TicketMessageDocument toMessageDocument(TicketMessage domain);

    MessageAttachmentDocument toAttachmentDocument(MessageAttachment domain);

    S3ObjectReferenceDocument toS3ReferenceDocument(S3ObjectReference domain);

    TicketResolutionDocument toResolutionDocument(TicketResolution domain);

    TicketStatusHistoryDocument toStatusHistoryDocument(TicketStatusHistory domain);

    TicketRequesterDocument toRequesterDocument(TicketRequester domain);

    AssignedAgentDocument toAgentDocument(AssignedAgent domain);

    LocationDocument toLocationDocument(Location domain);

    default HelpTicket toDomain(HelpTicketDocument doc) {
        if (doc == null) {
            return null;
        }

        TicketCode ticketCode = doc.getTicketCode() != null ? new TicketCode(doc.getTicketCode()) : null;
        TicketRequester requester = toRequesterDomain(doc.getRequester());
        AssignedAgent assignedAgent = toAgentDomain(doc.getAssignedAgent());
        Location location = toLocationDomain(doc.getLocation());
        List<TicketMessage> messages = toMessageDomainList(doc.getMessages());
        TicketResolution resolution = toResolutionDomain(doc.getResolution());
        List<TicketStatusHistory> statusHistory = toStatusHistoryDomainList(doc.getStatusHistory());

        return HelpTicket.reconstitute(
                doc.getId(),
                ticketCode,
                requester,
                doc.getExternalRef(),
                doc.getSolicitudId(),
                doc.getSubcategoryId(),
                doc.getCategoryName(),
                doc.getSubcategoryName(),
                doc.getStatus(),
                doc.getPriority(),
                doc.getPriorityScore(),
                assignedAgent,
                doc.getSubject(),
                doc.getDescription(),
                location,
                messages,
                resolution,
                statusHistory,
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                doc.getClosedAt()
        );
    }

    default TicketRequester toRequesterDomain(TicketRequesterDocument doc) {
        if (doc == null) return null;
        return new TicketRequester(doc.getUserId(), doc.getUserRole(), doc.getName(), doc.getEmail());
    }

    default AssignedAgent toAgentDomain(AssignedAgentDocument doc) {
        if (doc == null) return null;
        return new AssignedAgent(doc.getAgentId(), doc.getName(), doc.getEmail());
    }

    default Location toLocationDomain(LocationDocument doc) {
        if (doc == null) return null;
        return new Location(doc.getCountry(), doc.getDepartment(), doc.getCity());
    }

    default S3ObjectReference toS3ReferenceDomain(S3ObjectReferenceDocument doc) {
        if (doc == null) return null;
        return new S3ObjectReference(doc.getBucket(), doc.getKey(), doc.getS3Url());
    }

    default MessageAttachment toAttachmentDomain(MessageAttachmentDocument doc) {
        if (doc == null) return null;
        return new MessageAttachment(
                toS3ReferenceDomain(doc.getS3Reference()),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getFileSizeBytes()
        );
    }

    default List<MessageAttachment> toAttachmentDomainList(List<MessageAttachmentDocument> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toAttachmentDomain).collect(Collectors.toList());
    }

    default TicketMessage toMessageDomain(TicketMessageDocument doc) {
        if (doc == null) return null;
        return new TicketMessage(
                doc.getId(),
                doc.getSenderId(),
                doc.getSenderType(),
                doc.getSenderName(),
                doc.getContent(),
                doc.getIsInternalNote(),
                toAttachmentDomainList(doc.getAttachments()),
                doc.getCreatedAt()
        );
    }

    default List<TicketMessage> toMessageDomainList(List<TicketMessageDocument> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toMessageDomain).collect(Collectors.toList());
    }

    default TicketResolution toResolutionDomain(TicketResolutionDocument doc) {
        if (doc == null) return null;
        return new TicketResolution(
                doc.getActionType(),
                doc.getCompensationAmount(),
                doc.getAgentNotes(),
                doc.getResolvedAt()
        );
    }

    default TicketStatusHistory toStatusHistoryDomain(TicketStatusHistoryDocument doc) {
        if (doc == null) return null;
        return new TicketStatusHistory(
                doc.getStatus(),
                doc.getChangedBy(),
                doc.getReason(),
                doc.getChangedAt()
        );
    }

    default List<TicketStatusHistory> toStatusHistoryDomainList(List<TicketStatusHistoryDocument> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toStatusHistoryDomain).collect(Collectors.toList());
    }
}
