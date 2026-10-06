package com.findu.help.v2.application.port.in.command;

import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.MessageAttachment;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateTicketCommand {
    private final Long userId;
    private final UserRole userRole;
    private final Long solicitudId;
    private final String categoryId;
    private final String subcategoryId;
    private final String subject;
    private final String description;
    private final Location location;
    private final List<MessageAttachment> initialAttachments;
}
