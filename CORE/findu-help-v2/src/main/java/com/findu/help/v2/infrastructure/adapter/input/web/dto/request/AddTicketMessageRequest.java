package com.findu.help.v2.infrastructure.adapter.input.web.dto.request;

import com.findu.help.v2.domain.enums.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTicketMessageRequest {

    @NotNull(message = "El senderId es obligatorio")
    private Long senderId;

    @NotNull(message = "El senderType es obligatorio")
    private SenderType senderType;

    private String senderName;

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    private String content;

    private Boolean isInternalNote;
}
