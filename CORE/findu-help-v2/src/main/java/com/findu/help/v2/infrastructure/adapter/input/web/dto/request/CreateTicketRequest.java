package com.findu.help.v2.infrastructure.adapter.input.web.dto.request;

import com.findu.help.v2.domain.enums.UserRole;
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
public class CreateTicketRequest {

    @NotNull(message = "El userId es obligatorio")
    private Long userId;

    @NotNull(message = "El userRole es obligatorio")
    private UserRole userRole;

    private Long solicitudId;
    private String categoryId;
    private String subcategoryId;

    @NotBlank(message = "El asunto es obligatorio")
    private String subject;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    private LocationRequest location;
}
