# Diagrama de Casos de Uso — Calificación y Retroalimentación

Diagrama alineado al estilo clásico de casos de uso. Presenta a los tres actores del flujo: **Admin** (moderación en findu-admin), **Proveedor** y **Cliente** (valoración mutua en findu-core). La frontera de cada módulo está marcada en tono **ámbar**.

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
            UC5([Auditar/Moderar Reseñas PATCH]):::caso
        end
    end

    subgraph filaProveedor[" "]
        direction LR
        ActorProveedor((Proveedor)):::actor
        subgraph MS_CORE_P["Microservicio: findu-core"]
            direction LR
            UC1([Calificar Cliente POST]):::caso
            UC2([Ver Reputación GET]):::caso
        end
    end

    subgraph filaCliente[" "]
        direction LR
        ActorCliente((Cliente)):::actor
        subgraph MS_CORE_C["Microservicio: findu-core"]
            direction LR
            UC3([Calificar Proveedor POST]):::caso
            UC4([Ver Reputación GET]):::caso
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

Documentación ampliada del flujo y API: [findu-core-architecture.md](./findu-core-architecture.md).