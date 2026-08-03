package com.findu.core.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "municipio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MunicipioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_dane", nullable = false, unique = true)
    private String codigoDane;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String departamento;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;
}
