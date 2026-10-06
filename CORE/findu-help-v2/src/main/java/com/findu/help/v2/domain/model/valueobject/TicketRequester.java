package com.findu.help.v2.domain.model.valueobject;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class TicketRequester {
    private final Long userId;
    private final UserRole userRole;
    private final String name;
    private final String email;

    public TicketRequester(Long userId, UserRole userRole, String name, String email) {
        if (userId == null) {
            throw new IllegalArgumentException("El userId del solicitante no puede ser nulo");
        }
        if (userRole == null) {
            throw new IllegalArgumentException("El rol del solicitante no puede ser nulo");
        }
        this.userId = userId;
        this.userRole = userRole;
        this.name = name != null ? name.trim() : "";
        this.email = email != null ? email.trim() : "";
    }
}
