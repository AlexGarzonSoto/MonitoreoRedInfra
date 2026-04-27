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

# 🐳 2. `docker.md` (Infraestructura real)

👉 Aquí muestras cómo corre TODO en Docker

### 📌 Qué debes mostrar:
- Contenedores  
- Red Docker  
- Servicios  

---

### ✅ Pega esto:

```md
# 🐳 Arquitectura Docker

Este diagrama representa la estructura de contenedores utilizada para desplegar NetWatch.

```mermaid
flowchart LR
    subgraph Docker
        F[Frontend Container]
        A[API Container]
        W[Worker Container]
        DB[(PostgreSQL Container)]
    end

    F --> A
    A --> W
    W --> DB


---

# 🔐 3. `autenticacion.md` (Seguridad)

👉 Aquí explicas login y JWT

### 📌 Qué debes mostrar:
- Usuario  
- Frontend  
- API  
- Base de datos  
- Token  

---

### ✅ Pega esto:

```md
# 🔐 Flujo de Autenticación

Este diagrama describe el proceso de autenticación de usuarios mediante credenciales y generación de token JWT.

```mermaid
sequenceDiagram
    participant U as Usuario
    participant F as Frontend
    participant A as API
    participant DB as Base de Datos

    U->>F: Ingresa credenciales
    F->>A: POST /login
    A->>DB: Validar usuario
    DB-->>A: Usuario válido
    A-->>F: JWT Token
    F-->>U: Acceso concedido


---

# 👥 4. `casos-uso.md` (Usuarios)

👉 Aquí muestras QUÉ hace cada actor

### 📌 Qué debes mostrar:
- Admin  
- Usuario  
- Acciones  

---

### ✅ Pega esto:

```md
# 👥 Diagrama de Casos de Uso

Este diagrama representa los actores del sistema y sus principales interacciones.

```mermaid
flowchart LR
    Admin -->|Gestiona| Dashboard
    Admin -->|Revisa| Alertas
    Usuario -->|Consulta| Dashboard
    Usuario -->|Visualiza| Eventos


---

# 🔥 5. `owasp.md` (DevSecOps)

👉 Aquí muestras seguridad en pipeline

### 📌 Qué debes mostrar:
- Code  
- SAST  
- Secrets  
- Build  
- Scan  
- Test  
- Deploy  

---

### ✅ Pega esto:

```md
# 🔥 Flujo DevSecOps (OWASP)

Este diagrama representa el pipeline de seguridad implementado en el proyecto.

```mermaid
flowchart LR
    Code --> SAST[Semgrep / Bandit]
    SAST --> Secrets[Gitleaks]
    Secrets --> Build[Docker Build]
    Build --> Scan[Trivy]
    Scan --> Test[DAST - OWASP ZAP]
    Test --> Deploy


---

# 🧠 ORDEN LÓGICO (IMPORTANTÍSIMO)

Este es el orden mental:

1. 🧩 Componentes → qué existe  
2. 🐳 Docker → cómo corre  
3. 🔐 Autenticación → cómo entra el usuario  
4. 👥 Casos de uso → qué puede hacer  
5. 🔥 OWASP → cómo se asegura  

---

# 💡 TIP PRO (esto te sube la nota)

En TODOS los archivos:

✔️ título  
✔️ pequeña explicación  
✔️ diagrama  

👉 eso es exactamente lo que pide el trabajo

---

# 🚀 SIGUIENTE

Si quieres subir nivel:

👉 los adaptamos a tu proyecto real (con RabbitMQ, logs, etc.)  
👉 o los conectamos con el pipeline CI/CD  

Solo dime:

👉 **“ajústalos a mi proyecto”** 💯
