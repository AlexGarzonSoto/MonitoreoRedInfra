# Diagramas de Arquitectura — NetWatch

Los diagramas usan **Mermaid**, renderizado nativo en GitHub.  
Las fuentes UML originales en PlantUML están en los archivos `.puml` de esta carpeta.  
El modelo de amenazas está en [`threat-model.json`](threat-model.json) — se abre en <https://www.threatdragon.com/>.

---

## 1. Diagrama de Componentes

```mermaid
flowchart LR
    U([👤 Usuario])

    subgraph fe["Frontend :3000"]
        FE["Vue.js 3 + Vite\nNginx :80"]
    end

    subgraph api["API Layer"]
        GW["API Gateway\nSpring Boot :8080\nJWT · RBAC · RateLimit"]
    end

    subgraph broker["Message Broker"]
        MQ{{"RabbitMQ 3.12\nExchange: netwatch.direct"}}
    end

    subgraph workers["Workers"]
        WC["Worker Captura :8082\nPcap4J"]
        WA["Worker Análisis :8081\nThreatDetectionEngine"]
        WAL["Worker Alertas :8083\nSMTP / Webhook"]
        WO["Worker OSINT :8084\nGeoIP + Valkey"]
        WS["Worker Scanner :8085\nNmap + NVD"]
    end

    subgraph persistence["Persistencia"]
        DB[("PostgreSQL 15\nTimescaleDB :5432")]
        VK[("Valkey 7.2\n:6379")]
    end

    subgraph obs["Observabilidad"]
        PROM["Prometheus :9090"]
        GRAF["Grafana :3001"]
        LOKI["Loki :3100"]
    end

    GEO["ip-api.com"]
    SMTP["SMTP / Webhook"]

    U -->|"HTTPS"| FE
    FE -->|"/api/** proxy"| GW
    GW --> DB
    GW <-->|"threats.detected"| MQ
    WC -->|"packets.raw"| MQ
    MQ -->|"packets.raw"| WA
    WA -->|"threats.detected\nalerts.notify\nosint.enrich\nscan.requests"| MQ
    MQ -->|"alerts.notify"| WAL
    WAL --> SMTP
    MQ -->|"osint.enrich"| WO
    WO --> VK
    WO --> GEO
    WO -->|"threats.detected enriched"| MQ
    MQ -->|"scan.requests"| WS
    PROM -->|"scrape"| GW & WA & WAL & WO & WC & WS
    GRAF --> PROM & LOKI
```

---

## 2. Diagrama de Despliegue

```mermaid
flowchart TB
    subgraph host["🖥️ Host Linux (MX Linux / Ubuntu)"]
        subgraph net["Red Docker: netwatch-net (bridge)"]
            subgraph data["Datos"]
                PG["postgres\nTimescaleDB 15\n:5432"]
                RMQ["rabbitmq\n3.12-management\n:5672 / :15672"]
                VK["valkey\n7.2-alpine\n:6379"]
            end
            subgraph app["Aplicación"]
                GW["api-gateway\nSpring Boot\n:8080"]
                WA["worker-analysis\nSpring Boot\n:8081"]
                WAL["worker-alerts\nSpring Boot\n:8083"]
                WO["worker-osint\nSpring Boot\n:8084"]
                WS["worker-scanner\nSpring Boot\n:8085"]
                FE["frontend\nVue.js + Nginx\n:3000 → :80"]
            end
            subgraph monitoring["Observabilidad"]
                PROM["prometheus\n:9090"]
                GRAF["grafana\n:3001"]
                LOKI["loki\n:3100"]
                PT["promtail\n/var/log (ro)"]
            end
        end
        WC["worker-capture\nnetwork_mode: host\ncap: NET_ADMIN, NET_RAW\n:8082"]
    end

    subgraph vols["💾 Volúmenes"]
        V1[postgres_data]
        V2[rabbitmq_data]
        V3[valkey_data]
        V4[grafana_data]
        V5[loki_data]
    end

    PG --- V1
    RMQ --- V2
    VK --- V3
    GRAF --- V4
    LOKI --- V5

    FE -->|"proxy /api"| GW
    GW --> PG & RMQ
    WC --> RMQ
    WA & WAL & WO & WS --> RMQ
    PROM -->|"scrape /actuator/prometheus"| GW & WA & WAL & WO & WS
    GRAF --> PROM & LOKI
    PT --> LOKI
```

---

## 3. Diagrama de Secuencia — Autenticación JWT

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Frontend (Vue.js)
    participant RL as RateLimitFilter
    participant JF as JwtAuthFilter
    participant AC as AuthController
    participant AS as AuthService
    participant JP as JwtTokenProvider
    participant DB as PostgreSQL

    rect rgb(230,245,255)
        note over U,DB: Inicio de sesión
        U->>FE: email + contraseña
        FE->>RL: POST /api/v1/auth/login
        RL->>RL: Verifica bucket por IP (5 req/min)
        alt Rate limit superado
            RL-->>FE: 429 Too Many Requests
            FE-->>U: "Demasiados intentos"
        else Dentro del límite
            RL->>JF: ruta pública, continúa
            JF->>AC: Sin Bearer → no aplica filtro
            AC->>AS: login(email, password)
            AS->>DB: findByEmailAndActiveTrue(email)
            DB-->>AS: Optional<User>
            alt Credenciales inválidas
                AS-->>FE: 401 Unauthorized
                FE-->>U: "Credenciales incorrectas"
            else Válidas
                AS->>JP: generateAccessToken(userId, role)
                JP-->>AS: JWT HMAC-SHA256 · TTL 30 min
                AS->>JP: generateRefreshToken(userId)
                JP-->>AS: JWT HMAC-SHA256 · TTL 7 días
                AS-->>AC: LoginResponse
                AC-->>FE: 200 {accessToken, refreshToken, role}
                FE->>FE: localStorage.setItem(tokens)
                FE-->>U: redirect → /dashboard
            end
        end
    end

    rect rgb(230,255,230)
        note over U,DB: Petición autenticada
        U->>FE: Navega a /eventos
        FE->>JF: GET /api/v1/events · Authorization: Bearer token
        JF->>JP: validateAccessToken(token)
        JP-->>JF: Claims {userId, role}
        JF->>JF: SecurityContext.setAuthentication(ROLE_ADMIN)
        JF->>AC: request con contexto de seguridad
        AC-->>FE: 200 Page<EventDTO>
        FE-->>U: Tabla de eventos
    end

    rect rgb(255,245,230)
        note over U,DB: Renovación automática (interceptor Axios)
        FE->>JF: GET /api/v1/events · token expirado
        JF->>JP: validateAccessToken → JwtExpiredException
        JF-->>FE: 401 Unauthorized
        FE->>AC: POST /api/v1/auth/refresh · X-Refresh-Token
        AC->>AS: refresh(refreshToken)
        AS->>JP: validateRefreshToken → userId
        AS->>DB: findById(userId)
        AS->>JP: generateAccessToken + generateRefreshToken
        AC-->>FE: 200 {newAccessToken, newRefreshToken}
        FE->>FE: localStorage actualiza tokens
        FE->>AC: Reintenta petición original
        AC-->>FE: 200 datos solicitados
    end

    rect rgb(255,230,230)
        note over U,DB: Cierre de sesión
        U->>FE: Presiona "Salir"
        FE->>AC: POST /api/v1/auth/logout · Bearer token
        AC->>AS: logout(token)
        AS->>AS: Log INFO (blacklist Redis en prod)
        AC-->>FE: 204 No Content
        FE->>FE: localStorage.clear()
        FE-->>U: redirect → /login
    end
```

---

## 4. Diagrama de Casos de Uso (RBAC)

```mermaid
flowchart TD
    VIEWER(["👁️ VIEWER"])
    ANALYST(["🔍 ANALYST\n(hereda VIEWER)"])
    ADMIN(["🔑 ADMIN\n(hereda ANALYST)"])

    VIEWER -->|hereda| ANALYST
    ANALYST -->|hereda| ADMIN

    subgraph auth["Autenticación (todos los roles)"]
        UC01["UC-01 Iniciar sesión"]
        UC02["UC-02 Cerrar sesión"]
        UC03["UC-03 Renovar token JWT"]
    end

    subgraph dash["Dashboard (VIEWER+)"]
        UC04["UC-04 Ver resumen de amenazas"]
        UC05["UC-05 Ver gráficas por tipo"]
    end

    subgraph events["Gestión de Eventos"]
        UC06["UC-06 Listar eventos (paginado)"]
        UC07["UC-07 Buscar por IP / tipo / severidad"]
        UC08["UC-08 Ver detalle con GeoIP y ASN"]
        UC09["UC-09 Marcar evento como resuelto 🔍"]
    end

    subgraph alerts["Gestión de Alertas"]
        UC10["UC-10 Ver alertas activas"]
        UC11["UC-11 Reconocer alerta → ACKNOWLEDGED 🔍"]
        UC12["UC-12 Resolver alerta → RESOLVED 🔍"]
        UC13["UC-13 Marcar falso positivo 🔍"]
    end

    subgraph capture["Captura de Red"]
        UC14["UC-14 Ver estado de captura"]
        UC15["UC-15 Cambiar interfaz 🔑"]
        UC16["UC-16 Iniciar / Detener captura 🔑"]
    end

    subgraph scan["Escaneo de Vulnerabilidades"]
        UC17["UC-17 Solicitar escaneo de IP 🔑"]
        UC18["UC-18 Ver resultados de escaneos"]
    end

    subgraph reports["Reportes"]
        UC19["UC-19 Exportar eventos JSON/CSV"]
        UC20["UC-20 Exportar alertas JSON/CSV"]
    end

    subgraph admin_pkg["Administración 🔑"]
        UC21["UC-21 Gestionar usuarios"]
        UC22["UC-22 Asignar roles"]
    end

    VIEWER --> auth & dash & UC06 & UC07 & UC08 & UC10 & UC14 & UC18 & UC19 & UC20
    ANALYST --> UC09 & UC11 & UC12 & UC13
    ADMIN --> UC15 & UC16 & UC17 & admin_pkg
```

> **Leyenda:** Sin marca = VIEWER · 🔍 = ANALYST · 🔑 = ADMIN exclusivo

---

## 5. Modelo de Amenazas STRIDE

Archivo: [`threat-model.json`](threat-model.json)  
Herramienta: **OWASP Threat Dragon** (sin registro en <https://www.threatdragon.com/>)  
→ *Open Existing Threat Model* → cargar el archivo JSON

| ID | STRIDE | Componente | Severidad | Contramedida |
|----|--------|-----------|-----------|--------------|
| T-01 | Spoofing | API Gateway / JWT | Alta | HMAC-SHA256 · TTL 30 min · refresh rotation |
| T-02 | Elevation of Privilege | API Gateway / RBAC | Alta | `@PreAuthorize` · roles en BD |
| T-03 | Repudiation | PostgreSQL | Media | Audit log inmutable con `created_at` |
| T-04 | Information Disclosure | API Gateway (respuestas) | Media | EventDTO enmascara campos por rol |
| T-05 | Tampering | API Gateway (entrada) | Alta | `@Valid` Bean Validation · HTTPS |
| T-06 | Tampering | RabbitMQ (mensajes) | Alta | Autenticación AMQP · TLS en prod |
| T-07 | Denial of Service | Worker Captura / API | Alta | RateLimitFilter Bucket4j · prefetch 20 |
| T-08 | Elevation of Privilege | Worker Captura (NET_RAW) | Crítica | Aislado en contenedor · Falco alert |
| T-09 | Information Disclosure | Worker OSINT → ip-api.com | Baja | Datos públicos · caché local TTL 1h |
| T-10 | Information Disclosure | Worker Alertas → SMTP | Baja | Credenciales en variables de entorno |
