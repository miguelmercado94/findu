package com.findu.help.v2.domain.valueobject;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class AssignedAgent {
    private final Long agentId;
    private final String name;
    private final String email;

    public AssignedAgent(Long agentId, String name, String email) {
        if (agentId == null) {
            throw new IllegalArgumentException("El agentId no puede ser nulo");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del agente es obligatorio");
        }
        this.agentId = agentId;
        this.name = name.trim();
        this.email = email != null ? email.trim() : "";
    }
}
