# Diagrama de Casos de Uso — Gestión de Perfil

Diagrama alineado al estilo clásico de casos de uso: **admin arriba**, **cliente abajo**, actor a la **izquierda**, frontera del sistema (microservicio) en **ámbar** y casos de uso en **púrpura**. Mermaid no soporta `useCaseDiagram` como PlantUML; esta versión usa `flowchart` con estilos.

Puedes visualizarlo en el IDE (extensión Mermaid) o en [Mermaid Live Editor](https://mermaid.live/).

```mermaid
flowchart TB
    classDef actor fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#111
    classDef caso fill:#e1bee7,stroke:#6a1b9a,stroke-width:1.5px,color:#111

    subgraph filaAdmin[" "]
        direction LR
        ActorAdmin((Admin)):::actor
        subgraph MS_ADMIN["Microservicio: findu-admin"]
            direction LR
            UC6([Modificar Identidad/Cumpleaños]):::caso
            UC7([Eliminación Definitiva DELETE]):::caso
        end
    end

    subgraph filaCliente[" "]
        direction LR
        ActorCliente((Cliente)):::actor
        subgraph MS_CORE["Microservicio: findu-core"]
            direction LR
            UC1([Crear Perfil POST]):::caso
            UC2([Consultar Info Perfil GET]):::caso
            UC3([Actualizar Datos Básicos PUT]):::caso
            UC4([Actualizar Datos Sensibles OTP]):::caso
            UC5([Habilitar/Deshabilitar Perfil PATCH]):::caso
        end
    end

    ActorAdmin --> UC6
    ActorAdmin --> UC7
    ActorCliente --> UC1
    ActorCliente --> UC2
    ActorCliente --> UC3
    ActorCliente --> UC4
    ActorCliente --> UC5

    style MS_ADMIN fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style MS_CORE fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style filaAdmin fill:transparent,stroke:transparent,color:transparent
    style filaCliente fill:transparent,stroke:transparent,color:transparent
```

Documentación ampliada del flujo y API: [findu-core-architecture.md](./findu-core-architecture.md).
