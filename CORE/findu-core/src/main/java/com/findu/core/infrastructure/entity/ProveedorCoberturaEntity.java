package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedor_cobertura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorCoberturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perfil_proveedor_id", nullable = false)
    private Long perfilProveedorId;

    @Column(name = "municipio_id", nullable = false)
    private Long municipioId;
}
