package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequesterDocument {
    private Long userId;
    private UserRole userRole;
    private String name;
    private String email;
}
