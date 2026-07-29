package com.findu.core.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorCobertura {
    private Long id;
    private Long perfilProveedorId;
    private Long municipioId;
}
