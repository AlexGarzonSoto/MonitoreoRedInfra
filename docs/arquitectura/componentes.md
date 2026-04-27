# 📐 Diagrama de Componentes (Microservicios)

Este diagrama muestra la arquitectura general del sistema NetWatch basada en microservicios.

```mermaid
flowchart LR
    U[Usuario] --> F[Frontend]
    F --> A[API Gateway]
    A --> W1[Worker Monitoreo]
    A --> W2[Worker Alertas]
    W1 --> DB[(Base de Datos)]
    W2 --> DB

---
