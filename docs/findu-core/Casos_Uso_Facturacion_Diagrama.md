# Diagrama de Casos de Uso — Facturación y Cobros Adicionales

Diagrama alineado al estilo clásico de casos de uso. Muestra la interacción de la triada: el **Admin** (monitoreo/auditoría superior), el **Proveedor** (gestión de cobros extra) y el **Cliente** (visibilidad y aprobación).

Puedes visualizarlo en el IDE (extensión Mermaid) o en Mermaid Live Editor.

```mermaid
flowchart TB
    classDef actor fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#111
    classDef caso fill:#e1bee7,stroke:#6a1b9a,stroke-width:1.5px,color:#111

    subgraph filaAdmin[" "]
        direction LR
        ActorAdmin((Admin)):::actor
        subgraph MS_ADMIN["Microservicio: findu-admin"]
            direction LR
            UC5([Aprobar/Auditar Extras PATCH]):::caso
        end
    end

    subgraph filaProveedor[" "]
        direction LR
        ActorProveedor((Proveedor)):::actor
        subgraph MS_CORE_P["Microservicio: findu-core"]
            direction LR
            UC1([Agregar Detalle Extra POST]):::caso
            UC2([Modificar/Eliminar Extra]):::caso
        end
    end

    subgraph filaCliente[" "]
        direction LR
        ActorCliente((Cliente)):::actor
        subgraph MS_CORE_C["Microservicio: findu-core"]
            direction LR
            UC3([Ver Factura GET]):::caso
            UC4([Aprobar/Rechazar Extra PATCH]):::caso
        end
    end

    ActorAdmin --> UC5
    ActorProveedor --> UC1
    ActorProveedor --> UC2
    ActorCliente --> UC3
    ActorCliente --> UC4

    style MS_ADMIN fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style MS_CORE_P fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style MS_CORE_C fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style filaAdmin fill:transparent,stroke:transparent,color:transparent
    style filaProveedor fill:transparent,stroke:transparent,color:transparent
    style filaCliente fill:transparent,stroke:transparent,color:transparent
```