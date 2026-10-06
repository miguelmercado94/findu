package com.findu.help.v2.domain.event;

import com.findu.help.v2.domain.enums.SenderType;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TicketMessageAddedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final String messageId;
    private final Long senderId;
    private final SenderType senderType;
    private final String senderName;
    private final String content;
    private final Boolean isInternalNote;
    private final Instant occurredOn;

    public TicketMessageAddedEvent(String ticketId, String ticketCode, String messageId, Long senderId, SenderType senderType, String senderName, String content, Boolean isInternalNote) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.senderName = senderName;
        this.content = content;
        this.isInternalNote = isInternalNote;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
