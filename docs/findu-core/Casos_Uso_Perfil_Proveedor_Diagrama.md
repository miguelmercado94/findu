# Diagrama de Casos de Uso — Gestión de Perfil Proveedor

Diagrama alineado al estilo clásico de casos de uso: **admin arriba**, **proveedor abajo**, actor a la **izquierda**, frontera del sistema (microservicio) en **ámbar** y casos de uso en **púrpura**. Mermaid no soporta `useCaseDiagram` como PlantUML; esta versión usa `flowchart` con estilos.

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
            UC8([Auditar Perfil/Docs PATCH]):::caso
        end
    end

    subgraph filaProveedor[" "]
        direction LR
        ActorProveedor((Proveedor)):::actor
        subgraph MS_CORE["Microservicio: findu-core"]
            direction LR
            UC1([Crear/Actualizar Perfil]):::caso
            UC2([Gestionar Dir. Única]):::caso
            UC3([Gest. Especialidades]):::caso
            UC4([Gest. Portafolio]):::caso
            UC5([Habilitar/Deshabilitar]):::caso
        end
    end

    ActorAdmin --> UC8
    ActorProveedor --> UC1
    ActorProveedor --> UC2
    ActorProveedor --> UC3
    ActorProveedor --> UC4
    ActorProveedor --> UC5

    style MS_ADMIN fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style MS_CORE fill:#fff9c4,stroke:#f9a825,stroke-width:2px
    style filaAdmin fill:transparent,stroke:transparent,color:transparent
    style filaProveedor fill:transparent,stroke:transparent,color:transparent
```

Documentación ampliada del flujo y API: [findu-core-architecture.md](./findu-core-architecture.md).