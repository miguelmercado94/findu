# Diagrama de Casos de Uso — Análisis de Ofertas de Profesionales

Diagrama alineado al estilo clásico de casos de uso. En este flujo bidireccional, el actor **Proveedor** interactúa desde la parte superior (descubriendo oportunidades y ofertando) y el actor **Cliente** desde la parte inferior (evaluando y aceptando). La frontera del sistema en tono **ámbar** representa el núcleo de operaciones en `findu-core`.

Puedes visualizarlo en el IDE (extensión Mermaid) o en Mermaid Live Editor.

```mermaid
flowchart TB
    classDef actor fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#111
    classDef caso fill:#e1bee7,stroke:#6a1b9a,stroke-width:1.5px,color:#111

    subgraph filaProveedor[" "]
        direction LR
        ActorProveedor((Proveedor)):::actor
        subgraph MS_CORE_P["Microservicio: findu-core"]
            direction LR
            UC1([Solicitudes Compatibles GET]):::caso
            UC2([Enviar Oferta POST]):::caso
        end
    end

    subgraph filaCliente[" "]
        direction LR
        ActorCliente((Cliente)):::actor
        subgraph MS_CORE_C["Microservicio: findu-core"]
            direction LR
            UC3([Revisar Ofertas GET]):::caso
            UC4([Consultar Perfil Proveedor GET]):::caso
            UC5([Aceptar Oferta PATCH]):::caso
        end
    end

    ActorProveedor --> UC1
    ActorProveedor --> UC2
    ActorCliente --> UC3
    ActorCliente --> UC4
    ActorCliente --> UC5

    style MS_CORE_P fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style MS_CORE_C fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style filaProveedor fill:transparent,stroke:transparent,color:transparent
    style filaCliente fill:transparent,stroke:transparent,color:transparent
```

Documentación ampliada del flujo y API: [findu-core-architecture.md](./findu-core-architecture.md).