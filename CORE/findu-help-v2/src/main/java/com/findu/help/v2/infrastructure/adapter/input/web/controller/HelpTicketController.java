package com.findu.help.v2.infrastructure.adapter.input.web.controller;

import com.findu.help.v2.application.port.in.*;
import com.findu.help.v2.application.port.in.command.AddTicketMessageCommand;
import com.findu.help.v2.application.port.in.command.ChangeTicketStatusCommand;
import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.application.port.out.storage.FileStoragePort;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.model.ticket.TicketMessage;
import com.findu.help.v2.domain.valueobject.MessageAttachment;
import com.findu.help.v2.domain.valueobject.S3ObjectReference;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.AddTicketMessageRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.ChangeTicketStatusRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.CreateTicketRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.response.HelpTicketResponse;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.response.TicketMessageResponse;
import com.findu.help.v2.infrastructure.adapter.input.web.mapper.HelpTicketWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/help/tickets")
@RequiredArgsConstructor
public class HelpTicketController {

    private final CreateTicketUseCase createTicketUseCase;
    private final ChangeTicketStatusUseCase changeTicketStatusUseCase;
    private final GetTicketsUseCase getTicketsUseCase;
    private final GetTicketChatHistoryUseCase getTicketChatHistoryUseCase;
    private final AddTicketMessageUseCase addTicketMessageUseCase;
    private final GetTicketByCodeUseCase getTicketByCodeUseCase;

    private final FileStoragePort fileStoragePort;
    private final HelpTicketWebMapper webMapper;

    @PostMapping
    public ResponseEntity<HelpTicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        CreateTicketCommand command = webMapper.toCommand(request);
        HelpTicket created = createTicketUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<HelpTicketResponse>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) UserRole userRole,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long assignedAgentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        GetTicketsUseCase.FilterQuery query = GetTicketsUseCase.FilterQuery.builder()
                .status(status)
                .priority(priority)
                .userRole(userRole)
                .userId(userId)
                .assignedAgentId(assignedAgentId)
                .page(page)
                .size(size)
                .build();

        List<HelpTicket> tickets = getTicketsUseCase.execute(query);
        return ResponseEntity.ok(webMapper.toResponseList(tickets));
    }

    @GetMapping("/{ticketCode}")
    public ResponseEntity<HelpTicketResponse> getTicketByCode(@PathVariable String ticketCode) {
        return getTicketByCodeUseCase.execute(ticketCode)
                .map(ticket -> ResponseEntity.ok(webMapper.toResponse(ticket)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{ticketCode}/messages")
    public ResponseEntity<List<TicketMessageResponse>> getTicketChatHistory(@PathVariable String ticketCode) {
        List<TicketMessage> messages = getTicketChatHistoryUseCase.execute(ticketCode);
        return ResponseEntity.ok(webMapper.toMessageResponseList(messages));
    }

    @PutMapping("/{ticketCode}/status")
    public ResponseEntity<HelpTicketResponse> changeTicketStatus(
            @PathVariable String ticketCode,
            @Valid @RequestBody ChangeTicketStatusRequest request) {

        ChangeTicketStatusCommand command = webMapper.toCommand(ticketCode, request);
        HelpTicket updated = changeTicketStatusUseCase.execute(command);
        return ResponseEntity.ok(webMapper.toResponse(updated));
    }

    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketMessageResponse> addMessageJson(
            @PathVariable String ticketId,
            @Valid @RequestBody AddTicketMessageRequest request) {

        AddTicketMessageCommand command = webMapper.toCommand(ticketId, request, List.of());
        TicketMessage added = addTicketMessageUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(added));
    }

    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketMessageResponse> addMessageMultipart(
            @PathVariable String ticketId,
            @Valid @RequestPart("message") AddTicketMessageRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        List<MessageAttachment> attachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String contentType = file.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    throw new IllegalArgumentException("El archivo '" + file.getOriginalFilename() + "' debe ser una imagen válida.");
                }
                S3ObjectReference s3Ref = fileStoragePort.storeImage(
                        new TicketCode(ticketId),
                        file.getOriginalFilename(),
                        contentType,
                        file.getInputStream(),
                        file.getSize()
                );
                attachments.add(new MessageAttachment(s3Ref, file.getOriginalFilename(), contentType, file.getSize()));
            }
        }

        AddTicketMessageCommand command = webMapper.toCommand(ticketId, request, attachments);
        TicketMessage added = addTicketMessageUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(added));
    }
}
