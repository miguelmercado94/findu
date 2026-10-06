package com.findu.help.v2.application.port.in.command;

import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.valueobject.MessageAttachment;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AddTicketMessageCommand {
    private final String ticketId;
    private final Long senderId;
    private final SenderType senderType;
    private final String senderName;
    private final String content;
    private final Boolean isInternalNote;
    private final List<MessageAttachment> attachments;
}
