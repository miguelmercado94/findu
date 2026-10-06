package com.findu.help.v2.infrastructure.adapter.input.web.mapper;

import com.findu.help.v2.application.port.in.command.AddTicketMessageCommand;
import com.findu.help.v2.application.port.in.command.ChangeTicketStatusCommand;
import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.domain.model.ticket.*;
import com.findu.help.v2.domain.valueobject.*;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.AddTicketMessageRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.ChangeTicketStatusRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.CreateTicketRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.LocationRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.response.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface HelpTicketWebMapper {

    default CreateTicketCommand toCommand(CreateTicketRequest request) {
        if (request == null) return null;

        Location location = request.getLocation() != null
                ? new Location(request.getLocation().getCountry(), request.getLocation().getDepartment(), request.getLocation().getCity())
                : null;

        return CreateTicketCommand.builder()
                .userId(request.getUserId())
                .userRole(request.getUserRole())
                .solicitudId(request.getSolicitudId())
                .categoryId(request.getCategoryId())
                .subcategoryId(request.getSubcategoryId())
                .subject(request.getSubject())
                .description(request.getDescription())
                .location(location)
                .initialAttachments(Collections.emptyList())
                .build();
    }

    default ChangeTicketStatusCommand toCommand(String ticketReference, ChangeTicketStatusRequest request) {
        if (request == null) return null;

        AssignedAgent agent = null;
        if (request.getAgentId() != null) {
            agent = new AssignedAgent(request.getAgentId(), request.getAgentName(), request.getAgentEmail());
        }

        return ChangeTicketStatusCommand.builder()
                .ticketReference(ticketReference)
                .newStatus(request.getNewStatus())
                .assignedAgent(agent)
                .reason(request.getReason())
                .build();
    }

    default AddTicketMessageCommand toCommand(String ticketId, AddTicketMessageRequest request, List<MessageAttachment> attachments) {
        if (request == null) return null;

        return AddTicketMessageCommand.builder()
                .ticketId(ticketId)
                .senderId(request.getSenderId())
                .senderType(request.getSenderType())
                .senderName(request.getSenderName())
                .content(request.getContent())
                .isInternalNote(request.getIsInternalNote() != null ? request.getIsInternalNote() : false)
                .attachments(attachments != null ? attachments : Collections.emptyList())
                .build();
    }

    @Mapping(target = "ticketCode", source = "ticketCode.value")
    HelpTicketResponse toResponse(HelpTicket ticket);

    TicketRequesterResponse toResponse(TicketRequester requester);

    AssignedAgentResponse toResponse(AssignedAgent agent);

    LocationResponse toResponse(Location location);

    @Mapping(target = "s3Url", source = "s3Reference.s3Url")
    @Mapping(target = "key", source = "s3Reference.key")
    MessageAttachmentResponse toResponse(MessageAttachment attachment);

    TicketMessageResponse toResponse(TicketMessage message);

    TicketStatusHistoryResponse toResponse(TicketStatusHistory history);

    TicketResolutionResponse toResponse(TicketResolution resolution);

    List<HelpTicketResponse> toResponseList(List<HelpTicket> tickets);

    List<TicketMessageResponse> toMessageResponseList(List<TicketMessage> messages);
}
