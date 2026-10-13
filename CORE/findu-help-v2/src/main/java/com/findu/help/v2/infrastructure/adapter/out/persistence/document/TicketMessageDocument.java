package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

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
public class TicketMessageDocument {
    private String id;
    private Long senderId;
    private SenderType senderType;
    private String senderName;
    private String content;
    private Boolean isInternalNote;
    private List<MessageAttachmentDocument> attachments;
    private Instant createdAt;
}
