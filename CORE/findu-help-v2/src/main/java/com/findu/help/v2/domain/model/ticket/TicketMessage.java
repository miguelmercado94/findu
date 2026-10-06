package com.findu.help.v2.domain.model.ticket;

import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.valueobject.MessageAttachment;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class TicketMessage {
    private final String messageId;
    private final Long senderId;
    private final SenderType senderType;
    private final String senderName;
    private final String content;
    private final Boolean isInternalNote;
    private final List<MessageAttachment> attachments;
    private final Instant createdAt;

    public TicketMessage(String messageId, Long senderId, SenderType senderType, String senderName, String content, Boolean isInternalNote, List<MessageAttachment> attachments, Instant createdAt) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("El messageId es obligatorio");
        }
        if (senderId == null) {
            throw new IllegalArgumentException("El senderId es obligatorio");
        }
        if (senderType == null) {
            throw new IllegalArgumentException("El senderType es obligatorio");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío");
        }
        this.messageId = messageId.trim();
        this.senderId = senderId;
        this.senderType = senderType;
        this.senderName = senderName != null ? senderName.trim() : "";
        this.content = content.trim();
        this.isInternalNote = isInternalNote != null ? isInternalNote : false;
        this.attachments = attachments != null ? List.copyOf(attachments) : Collections.emptyList();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
