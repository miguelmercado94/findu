package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import com.findu.help.v2.domain.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageResponse {
    private String messageId;
    private Long senderId;
    private SenderType senderType;
    private String senderName;
    private String content;
    private Boolean isInternalNote;
    private List<MessageAttachmentResponse> attachments;
    private Instant createdAt;
}
