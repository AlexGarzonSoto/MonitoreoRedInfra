# Manual del Desarrollador — NetWatch

## Tabla de contenidos

1. [Requisitos de entorno](#1-requisitos-de-entorno)
2. [Clonar y configurar el proyecto](#2-clonar-y-configurar-el-proyecto)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Compilar y ejecutar localmente](#4-compilar-y-ejecutar-localmente)
5. [Modo simulación vs captura real](#5-modo-simulación-vs-captura-real)
6. [Ejecutar pruebas unitarias](#6-ejecutar-pruebas-unitarias)
7. [Variables de entorno — referencia completa](#7-variables-de-entorno--referencia-completa)
8. [Desarrollar un nuevo microservicio](#8-desarrollar-un-nuevo-microservicio)
9. [Convenciones de código](#9-convenciones-de-código)
10. [Contribuir al proyecto](#10-contribuir-al-proyecto)
11. [Troubleshooting frecuente](#11-troubleshooting-frecuente)

---

## 1. Requisitos de entorno

### Herramientas obligatorias para desarrollo

| Herramienta | Versión | Instalación en Debian/Ubuntu |
|-------------|---------|------------------------------|
| Java OpenJDK | 21 LTS | `sudo apt install openjdk-21-jdk` |
| Maven | 3.9+ | `sudo apt install maven` |
| Docker Engine | 26.x | [docs.docker.com/engine/install](https://docs.docker.com/engine/install/) |
| Docker Compose v2 | 2.x | Incluido con Docker Engine |
| Git | 2.x | `sudo apt install git` |
| openssl | cualquier | `sudo apt install openssl` |

### Herramientas opcionales (recomendadas)

| Herramienta | Propósito |
|-------------|-----------|
| IntelliJ IDEA Community / VSCode | IDE con soporte Java y Vue.js |
| Postman o Bruno | Probar y documentar endpoints REST |
| DBeaver | Gestión visual de PostgreSQL + TimescaleDB |
| Bruno (API client) | Alternativa open source a Postman |
| Node.js 20 + npm | Desarrollar el frontend fuera de Docker |

### Verificar instalación

```bash
java -version
# openjdk version "21.x.x" ...

mvn -version
# Apache Maven 3.9.x

docker version
# Client: Docker Engine - Community 26.x

docker compose version
# Docker Compose version v2.x

git --version
# git version 2.x
```

---

## 2. Clonar y configurar el proyecto

### Clonar el repositorio

```bash
git clone https://github.com/AlexGarzonSoto/MonitoreoRedInfra.git
cd MonitoreoRedInfra
```

### Crear el archivo de variables de entorno

```bash
cp .env.example .env
```

### Generar claves seguras

Las claves JWT deben tener **mínimo 64 caracteres** (512 bits para HMAC-SHA256). Generarlas con:

```bash
# Generar JWT_SECRET
echo "JWT_SECRET=$(openssl rand -hex 64)"

# Generar JWT_REFRESH_SECRET (diferente al anterior)
echo "JWT_REFRESH_SECRET=$(openssl rand -hex 64)"

# Generar contraseñas para los servicios
echo "POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-32)"
echo "RABBITMQ_PASSWORD=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-32)"
echo "REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-32)"
echo "GRAFANA_PASSWORD=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-32)"
```

Copiar cada valor generado al archivo `.env`.

### Archivo `.env` completo

```bash
# ── JWT (OBLIGATORIO — generar con openssl rand -hex 64) ──────────────────────
JWT_SECRET=<64+ chars>
JWT_REFRESH_SECRET=<64+ chars, diferente al anterior>

# ── PostgreSQL ──────────────────────────────────────────────────────────────
POSTGRES_DB=netwatch
POSTGRES_USER=netwatch
POSTGRES_PASSWORD=<password seguro>

# ── RabbitMQ ────────────────────────────────────────────────────────────────
RABBITMQ_USER=netwatch
RABBITMQ_PASSWORD=<password seguro>

# ── Valkey (caché — reemplaza Redis) ────────────────────────────────────────
REDIS_PASSWORD=<password seguro>

# ── Grafana ─────────────────────────────────────────────────────────────────
GRAFANA_PASSWORD=<password seguro>

# ── Email para alertas (opcional — dejar vacío para deshabilitar) ───────────
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=
SMTP_PASSWORD=
ALERT_EMAIL_ENABLED=false
ALERT_EMAIL_TO=

# ── Webhook para alertas (opcional — dejar vacío para deshabilitar) ─────────
ALERT_WEBHOOK_ENABLED=false
ALERT_WEBHOOK_URL=

# ── Captura de red ──────────────────────────────────────────────────────────
CAPTURE_INTERFACE=eth0       # cambiar a tu interfaz: wlan0, ens3, enp3s0, etc.
CAPTURE_ENABLED=true

# ── Scanner (Nmap + NVD) ────────────────────────────────────────────────────
SCANNER_DRY_RUN=false        # true = simula escaneo sin ejecutar Nmap

# ── Docker Hub (solo para deploy) ────────────────────────────────────────────
DOCKERHUB_USERNAME=tu-usuario-dockerhub
```

---

## 3. Estructura del proyecto

```
proyectoFinal/
├── pom.xml                          # POM padre multi-módulo Maven
├── .env.example                     # Plantilla (sin valores reales — seguro commitear)
├── .env                             # Variables reales (en .gitignore — NUNCA commitear)
├── docker-compose.yml               # Stack de desarrollo (14 servicios)
├── docker-compose.prod.yml          # Override de producción (TLS + límites de recursos)
├── Caddyfile                        # Configuración del reverse proxy con TLS automático
├── README.md
│
├── netwatch-api-gateway/            # Puerto 8080 — API REST + autenticación JWT
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/netwatch/gateway/
│       │   ├── config/
│       │   │   ├── RabbitMQConfig.java       # Exchange, colas, bindings, converters
│       │   │   └── SecurityConfig.java       # JWT filter, RBAC, CSRF off, stateless
│       │   ├── controller/
│       │   │   ├── AuthController.java       # POST /auth/login|refresh|logout
│       │   │   ├── EventController.java      # GET/PATCH /events/**
│       │   │   └── AlertController.java      # GET/PATCH /alerts/**
│       │   ├── consumer/
│       │   │   └── ThreatEventConsumer.java  # Consume threats.detected desde RabbitMQ
│       │   ├── dto/
│       │   │   ├── LoginRequest.java         # record con validaciones @Email @NotBlank
│       │   │   ├── LoginResponse.java        # record con accessToken, refreshToken, role
│       │   │   └── EventDTO.java             # record con método from(NetworkEvent)
│       │   ├── model/
│       │   │   ├── User.java                 # @Entity — roles ADMIN/ANALYST/VIEWER
│       │   │   ├── NetworkEvent.java         # @Entity @Table(network_events) + índices
│       │   │   └── Alert.java                # @Entity — estados OPEN/ACKNOWLEDGED/...
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── NetworkEventRepository.java  # con queries de agregación
│       │   │   └── AlertRepository.java
│       │   ├── security/
│       │   │   ├── JwtTokenProvider.java     # HMAC-SHA256, access+refresh tokens
│       │   │   ├── JwtAuthFilter.java        # OncePerRequestFilter
│       │   │   └── UserDetailsServiceImpl.java
│       │   └── service/
│       │       ├── AuthService.java          # Login, refresh con blacklist en memoria
│       │       ├── EventService.java         # CRUD eventos + resumen
│       │       └── AlertService.java         # Ciclo de vida de alertas
│       ├── main/resources/application.properties
│       └── test/java/com/netwatch/gateway/
│
├── netwatch-worker-analysis/        # Puerto 8081 — Motor de detección de amenazas
│   └── src/main/java/com/netwatch/analysis/
│       ├── engine/ThreatDetectionEngine.java  # PORT_SCAN, BRUTE_FORCE, SYN_FLOOD, DNS
│       └── consumer/PacketConsumer.java       # Consume packets.raw, publica threats
│
├── netwatch-worker-capture/         # Puerto 8082 — Captura de paquetes de red
│   └── src/main/java/com/netwatch/capture/
│       ├── controller/CaptureController.java  # GET /capture/interfaces, /status
│       ├── producer/PacketProducer.java        # Publica en packets.raw
│       └── service/NetworkCaptureService.java  # Pcap4J real o simulación
│
├── netwatch-worker-alerts/          # Puerto 8083 — Notificaciones email/webhook
│   └── src/main/java/com/netwatch/alerts/
│       ├── consumer/AlertConsumer.java         # Consume alerts.notify
│       └── service/NotificationService.java    # JavaMailSender + RestTemplate webhook
│
├── netwatch-worker-osint/           # Puerto 8084 — Enriquecimiento GeoIP
│   └── src/main/java/com/netwatch/osint/
│       ├── consumer/OsintConsumer.java         # Consume osint.enrich, reencola enriquecido
│       └── service/GeoIpService.java           # ip-api.com + caché en Valkey 1h TTL
│
├── netwatch-worker-scanner/         # Puerto 8085 — Escáner Nmap + CVEs NVD
│   └── src/main/java/com/netwatch/scanner/
│       ├── consumer/ScanConsumer.java          # Consume solicitudes de escaneo
│       └── service/ScannerService.java         # Nmap + consulta NVD API
│
├── frontend/                        # Puerto 3000 (→ Nginx :80)
│   ├── Dockerfile                   # Multi-stage: node:20 build → nginx:alpine
│   ├── nginx.conf                   # SPA routing + proxy /api a api-gateway:8080
│   └── src/
│       ├── stores/                  # Pinia: auth.js, events.js
│       ├── services/api.js          # Axios + interceptores JWT refresh automático
│       └── views/                   # Dashboard, Events, Alerts, Scanner, Settings
│
├── infrastructure/
│   ├── sql/init.sql                 # Schema TimescaleDB + hipertabla + índices
│   ├── terraform/                   # IaC con OpenTofu
│   ├── ansible/                     # Playbooks de configuración de servidores
│   └── k8s/                         # Manifiestos Kubernetes (K3s)
│
├── monitoring/
│   ├── prometheus/
│   │   ├── prometheus.yml           # Scrape configs para los 6 microservicios
│   │   └── alert-rules.yml          # 8 reglas de alerta (disponibilidad, JVM, HTTP, RabbitMQ)
│   ├── grafana/dashboards/          # Dashboards JSON para Grafana
│   ├── loki/                        # loki-config.yml + promtail-config.yml
│   └── falco/falco-rules.yml        # Reglas de seguridad en runtime
│
└── .github/workflows/
    ├── ci.yml                       # Pipeline CI: 7 etapas de seguridad
    └── deploy.yml                   # Deploy a Docker Hub (activado por tags semver)
```

---

## 4. Compilar y ejecutar localmente

### Opción A: Docker Compose completo (recomendado para desarrollo)

```bash
# Levantar todos los servicios
docker compose up -d

# Ver logs de un servicio
docker compose logs -f netwatch-api-gateway

# Ver el estado de salud de todos
docker compose ps

# Detener sin borrar datos
docker compose down

# Detener y borrar volúmenes (reinicio limpio)
docker compose down -v
```

### Opción B: Solo las dependencias — ejecutar el gateway desde el IDE

Ideal para desarrollo: levantar solo PostgreSQL, RabbitMQ y Valkey con Docker, y ejecutar el servicio desde IntelliJ/VSCode con recarga en caliente.

```bash
# Levantar solo dependencias de infraestructura
docker compose up -d postgres rabbitmq valkey

# Esperar a que estén listos
docker compose ps
```

Configurar en el IDE estas variables de entorno para ejecutar `netwatch-api-gateway`:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=netwatch
DB_USER=netwatch
DB_PASS=<tu POSTGRES_PASSWORD del .env>
RABBITMQ_HOST=localhost
RABBITMQ_USER=<tu RABBITMQ_USER del .env>
RABBITMQ_PASS=<tu RABBITMQ_PASSWORD del .env>
JWT_SECRET=<tu JWT_SECRET del .env>
JWT_REFRESH_SECRET=<tu JWT_REFRESH_SECRET del .env>
```

### Opción C: Compilar con Maven

```bash
# Compilar todos los módulos (sin tests)
mvn clean package -DskipTests -q

# Compilar solo el gateway (incluye módulos padre)
mvn clean package -DskipTests -pl netwatch-api-gateway -am -q

# Compilar el worker de análisis
mvn clean package -DskipTests -pl netwatch-worker-analysis -am -q
```

### Reconstruir una imagen específica

Después de cambiar código en un servicio:

```bash
# Reconstruir y reiniciar solo el gateway
docker compose build --no-cache api-gateway
docker compose up -d --no-deps api-gateway

# Ver logs del servicio reconstruido
docker compose logs -f api-gateway
```

---

## 5. Modo simulación vs captura real

El `worker-capture` tiene un **sistema de fallback automático** con el siguiente orden de prioridad:

```
1. Leer interfaces del HOST via /host/sys/class/net (montaje de volumen)
2. Intentar captura real con Pcap4J en la interfaz configurada
3. Si Pcap4J falla (sin permisos, interfaz no existe) → modo simulación
```

### ¿Cómo saber en qué modo está el worker?

```bash
# Ver el modo actual en los logs
docker compose logs worker-capture | grep -iE "simul|captura|pcap"

# Modo simulación — salida esperada:
# INFO  Pcap4J no disponible, usando simulación: ...
# INFO  Simulando 5 paquetes de red...

# Modo real — salida esperada:
# INFO  Captura iniciada en interfaz wlan0
# INFO  Paquete capturado: 192.168.1.x → ...
```

También se puede consultar via API:

```bash
# Ver interfaces disponibles detectadas desde el host
curl http://localhost:8082/capture/interfaces

# Ver el estado actual (simulación vs real, interfaz activa)
curl http://localhost:8082/capture/status
```

### Pasar de simulación a captura real

**Paso 1:** Identificar las interfaces de red disponibles en el host:

```bash
# En el servidor host
ip link show
# o
ip -brief link show
# Ejemplos: eth0, ens3, ens18, wlan0, enp3s0

# También via API del worker (lee /host/sys/class/net dentro del contenedor)
curl http://localhost:8082/capture/interfaces
```

**Paso 2:** Actualizar el `.env`:

```bash
# Cambiar la interfaz (ejemplo para WiFi)
CAPTURE_INTERFACE=wlan0
CAPTURE_ENABLED=true
```

**Paso 3:** Reiniciar el worker de captura:

```bash
docker compose up -d --no-deps --force-recreate worker-capture
```

**Paso 4:** Verificar que captura tráfico real:

```bash
docker compose logs -f worker-capture
# Debe aparecer: "Captura iniciada en interfaz wlan0"
# Y paquetes reales como: "Paquete capturado: 192.168.x.x → ..."
```

### Pasar de captura real a simulación

Si se quiere volver al modo de simulación (sin cambiar la interfaz):

```bash
# Opción 1: desconectar la interfaz física del contenedor
CAPTURE_INTERFACE=nonexistent0   # interfaz que no existe → fallback a simulación

# Opción 2: deshabilitar Pcap4J añadiendo una condición en el código
# (para desarrollo — ver NetworkCaptureService.java)

docker compose up -d --no-deps worker-capture
```

### Capturar en múltiples interfaces (avanzado)

Para monitorear varias interfaces simultáneamente, desplegar múltiples instancias del worker con distintos valores de `CAPTURE_INTERFACE`:

```yaml
# docker-compose.override.yml (no commitear)
services:
  worker-capture-wlan:
    extends:
      service: worker-capture
    container_name: netwatch-worker-capture-wlan
    environment:
      CAPTURE_INTERFACE: wlan0
    ports:
      - "8086:8082"
```

---

## 6. Ejecutar pruebas unitarias

### Ejecutar todos los tests con cobertura

```bash
mvn verify -pl netwatch-api-gateway,netwatch-worker-analysis -am
```

Este comando:
1. Compila los módulos y sus dependencias
2. Ejecuta todos los tests con JUnit 5
3. Genera reporte de cobertura con JaCoCo
4. **Falla si la cobertura de líneas cae por debajo del 70%**

### Ejecutar tests de un módulo específico

```bash
# Solo el gateway
mvn test -pl netwatch-api-gateway

# Solo el worker de análisis
mvn test -pl netwatch-worker-analysis

# Solo una clase de test específica
mvn test -pl netwatch-api-gateway -Dtest=AuthServiceTest
```

### Ver el reporte de cobertura JaCoCo

```bash
# Generar el reporte HTML
mvn verify -pl netwatch-api-gateway

# Abrir en el navegador
xdg-open netwatch-api-gateway/target/site/jacoco/index.html
```

El reporte muestra:
- **Verde:** líneas cubiertas por tests
- **Rojo:** líneas sin cubrir (candidatas para nuevos tests)
- **Amarillo:** ramas parcialmente cubiertas

### Tests existentes

| Módulo | Clase de test | Casos cubiertos |
|--------|--------------|----------------|
| api-gateway | `AuthServiceTest` | Login exitoso, password incorrecto, email no existe, cuenta inactiva, refresh válido, refresh inválido, logout sin excepción (9 casos) |
| api-gateway | `JwtTokenProviderTest` | Generación, validación, expiración, claims de access y refresh tokens (12 casos) |
| api-gateway | `EventServiceTest` | Listado paginado, búsqueda por ID, resolución, resumen estadístico (7 casos) |
| api-gateway | `AlertServiceTest` | Acknowledge, resolve, false-positive, listado por estado (9 casos) |
| worker-analysis | `ThreatDetectionEngineTest` | PORT_SCAN, BRUTE_FORCE en SSH/RDP, SYN_FLOOD, DNS_TUNNELING, tráfico normal, reset de contadores (9 casos) |

### Requisito de cobertura (JaCoCo)

Configurado en el POM padre. Si la cobertura cae por debajo del umbral:

```
[ERROR] Coverage check failed for project netwatch-api-gateway:
  Lines covered ratio is 0.68, expected minimum is 0.70
```

Para diagnosticar qué falta cubrir: abrir el reporte HTML y buscar los archivos en rojo.

---

## 7. Variables de entorno — referencia completa

### Mapa de variables por servicio

| Variable | Servicio(s) | Descripción | Valor por defecto |
|----------|------------|-------------|-------------------|
| `JWT_SECRET` | api-gateway | Clave HMAC para access tokens | — (obligatorio) |
| `JWT_REFRESH_SECRET` | api-gateway | Clave HMAC para refresh tokens | — (obligatorio) |
| `DB_HOST` | api-gateway, analysis | Hostname de PostgreSQL | `postgres` (en Docker) |
| `DB_PORT` | api-gateway, analysis | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | api-gateway, analysis | Nombre de la base de datos | `netwatch` |
| `DB_USER` | api-gateway, analysis | Usuario de PostgreSQL | `netwatch` |
| `DB_PASS` | api-gateway, analysis | Contraseña de PostgreSQL | — (obligatorio) |
| `RABBITMQ_HOST` | todos | Hostname de RabbitMQ | `rabbitmq` (en Docker) |
| `RABBITMQ_USER` | todos | Usuario de RabbitMQ | `netwatch` |
| `RABBITMQ_PASS` | todos | Contraseña de RabbitMQ | — (obligatorio) |
| `VALKEY_HOST` | worker-osint | Hostname de Valkey | `valkey` (en Docker) |
| `VALKEY_PORT` | worker-osint | Puerto de Valkey | `6379` |
| `VALKEY_PASSWORD` | worker-osint | Contraseña de Valkey | `${REDIS_PASSWORD}` |
| `CAPTURE_INTERFACE` | worker-capture | Interfaz de red a capturar | `eth0` |
| `CAPTURE_ENABLED` | worker-capture | Activa la captura (false = detenida) | `true` |
| `SCANNER_DRY_RUN` | worker-scanner | `true` = simula sin ejecutar Nmap | `false` |
| `SMTP_HOST` | worker-alerts | Servidor SMTP para email | `smtp.gmail.com` |
| `SMTP_PORT` | worker-alerts | Puerto SMTP | `587` |
| `SMTP_USER` | worker-alerts | Usuario SMTP | vacío |
| `SMTP_PASSWORD` | worker-alerts | Contraseña SMTP | vacío |
| `ALERT_EMAIL_ENABLED` | worker-alerts | Activa notificaciones por email | `false` |
| `ALERT_EMAIL_TO` | worker-alerts | Destinatario de alertas | vacío |
| `ALERT_WEBHOOK_ENABLED` | worker-alerts | Activa notificaciones por webhook | `false` |
| `ALERT_WEBHOOK_URL` | worker-alerts | URL del webhook (Slack, Teams, etc.) | vacío |
| `GRAFANA_PASSWORD` | grafana | Contraseña del admin de Grafana | — (obligatorio) |
| `DOCKERHUB_USERNAME` | docker-compose | Namespace de Docker Hub | `netwatch` |
| `DOMAIN` | docker-compose.prod.yml | Dominio para TLS con Caddy | `localhost` |

### Flujo de variables en Docker Compose

```
archivo .env
    ↓
docker-compose.yml (${VAR} expansion)
    ↓
variables de entorno del contenedor Docker
    ↓
application.properties de Spring Boot (${ENV_VAR:default})
```

Ejemplo concreto para `DB_PASS`:

```bash
# .env
POSTGRES_PASSWORD=miPasswordSeguro

# docker-compose.yml
environment:
  DB_PASS: ${POSTGRES_PASSWORD}

# application.properties
spring.datasource.password=${DB_PASS:netwatch123}
```

---

## 8. Desarrollar un nuevo microservicio

### Patrón estándar de un worker

Todos los workers siguen el mismo patrón Maven + Spring Boot:

**1. Crear la estructura del módulo:**

```bash
mkdir -p netwatch-worker-nuevo/src/main/java/com/netwatch/nuevo
mkdir -p netwatch-worker-nuevo/src/main/resources
```

**2. Crear `netwatch-worker-nuevo/pom.xml`:**

```xml
<parent>
  <groupId>com.netwatch</groupId>
  <artifactId>netwatch-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>netwatch-worker-nuevo</artifactId>
```

**3. Agregar el módulo al POM padre (`pom.xml` raíz):**

```xml
<modules>
  <!-- ... módulos existentes ... -->
  <module>netwatch-worker-nuevo</module>
</modules>
```

**4. Crear el `Dockerfile`** siguiendo el patrón multi-stage de los servicios existentes.

**5. Agregar el servicio en `docker-compose.yml`:**

```yaml
worker-nuevo:
  <<: *default-logging
  build:
    context: .
    dockerfile: netwatch-worker-nuevo/Dockerfile
  networks: [netwatch-net]
  ports:
    - "8086:8086"
  environment:
    RABBITMQ_HOST: rabbitmq
    RABBITMQ_USER: ${RABBITMQ_USER:-netwatch}
    RABBITMQ_PASS: ${RABBITMQ_PASSWORD}
  depends_on:
    rabbitmq:
      condition: service_started   # Spring AMQP reintenta la conexión automáticamente
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8086/actuator/health || exit 1"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s
  restart: unless-stopped
```

**6. Agregar al pipeline CI** en `.github/workflows/ci.yml`:

```yaml
matrix:
  service:
    - netwatch-api-gateway
    # ... servicios existentes ...
    - netwatch-worker-nuevo   # agregar aquí
```

### Patrón de RabbitMQ

```java
// RabbitMQConfig.java — mismo en todos los servicios
public static final String EXCHANGE = "netwatch.direct";
public static final String Q_NUEVO  = "netwatch.nuevo.queue";
public static final String RK_NUEVO = "nuevo.routing.key";

// Consumer
@RabbitListener(queues = Q_NUEVO, containerFactory = "rabbitListenerContainerFactory")
public void procesar(MiModelo payload) {
    // procesar y opcionalmente reenviar
    rabbitTemplate.convertAndSend(EXCHANGE, RK_OTRO, resultado);
}
```

---

## 9. Convenciones de código

### Java

- **Estilo:** Google Java Style Guide
- **Lombok:** usar `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`
- **Transacciones:** `@Transactional(readOnly = true)` por defecto; `@Transactional` solo para escrituras
- **Self-call y AOP:** nunca llamar un método `@Transactional` mediante `this.metodo()` dentro del mismo bean — Spring AOP opera a través de un proxy y las llamadas internas lo evitan, perdiendo la transacción. Invocar siempre el repositorio directamente o separar en un bean auxiliar.
- **Inyección de dependencias:** siempre por constructor (`@RequiredArgsConstructor`), nunca `@Autowired` en campos

### Naming

| Elemento | Convención | Ejemplo |
|----------|-----------|---------|
| Clases | PascalCase | `ThreatDetectionEngine` |
| Métodos | camelCase | `detectPortScan()` |
| Constantes | UPPER_SNAKE_CASE | `SYN_FLOOD_THRESHOLD` |
| Variables de entorno | UPPER_SNAKE_CASE | `JWT_SECRET` |
| Propiedades Spring | kebab-case con punto | `netwatch.jwt.secret` |
| Rutas REST | kebab-case | `/api/v1/network-events` |

### Commits — Conventional Commits

```
tipo(alcance): descripción breve en español

feat(gateway):   nueva funcionalidad
fix(capture):    corrección de error
test(analysis):  agregar o modificar tests
docs:            documentación
ci:              cambios en el pipeline
refactor:        refactorización sin cambio de comportamiento
perf:            mejora de rendimiento
chore:           tareas de mantenimiento
```

---

## 10. Contribuir al proyecto

### Flujo de trabajo

```bash
# 1. Crear una rama para el cambio
git checkout -b feat/mi-nueva-funcionalidad

# 2. Hacer los cambios

# 3. Ejecutar los tests localmente antes de commitear
mvn verify -pl netwatch-api-gateway,netwatch-worker-analysis -am

# 4. Commitear con mensaje descriptivo
git commit -m "feat(gateway): agregar endpoint de búsqueda de eventos por rango de IP"

# 5. Subir la rama
git push origin feat/mi-nueva-funcionalidad

# 6. Crear Pull Request en GitHub hacia main
```

### Checklist antes de crear un Pull Request

- [ ] `mvn verify` pasa sin errores (tests + cobertura ≥ 70%)
- [ ] El Dockerfile compila correctamente (`docker compose build api-gateway`)
- [ ] No hay secretos en el código (`gitleaks detect --source .`)
- [ ] Se actualizó `.env.example` si se añadió una nueva variable
- [ ] Se actualizó la documentación relevante si aplica
- [ ] El pipeline CI pasa en GitHub Actions

---

## 11. Troubleshooting frecuente

### Error: `dependency failed to start: container netwatch-rabbitmq is unhealthy`

Este error ocurría con `condition: service_healthy` para RabbitMQ cuando Docker Compose evaluaba el estado en el instante exacto en que el broker estaba transitando de `starting` a `healthy`. La arquitectura actual lo resuelve de dos formas:

1. Los servicios usan `condition: service_started` para RabbitMQ — arrancan en paralelo sin esperar el healthcheck
2. Spring AMQP reintenta la conexión automáticamente cada 5 segundos (`recovery-interval=5000`) hasta establecerla

Si el error persiste de forma inesperada, verificar que las propiedades de resiliencia AMQP están presentes en el `application.properties` del servicio afectado:

```properties
spring.rabbitmq.connection-timeout=30000
spring.rabbitmq.listener.simple.recovery-interval=5000
spring.rabbitmq.listener.simple.missing-queues-fatal=false
```

### El API Gateway no arranca — `Connection refused` a PostgreSQL

```bash
# Verificar que PostgreSQL está sano
docker compose ps postgres
# Debe mostrar: (healthy)

# Ver logs de PostgreSQL
docker compose logs postgres | tail -20

# Forzar reinicio del gateway esperando a que postgres esté listo
docker compose up -d --no-deps api-gateway
```

### Error: `JWT secret must be at least 32 characters`

Significa que `JWT_SECRET` en el `.env` tiene menos de 64 caracteres (o es el valor de ejemplo). Solución:

```bash
# Verificar el valor actual
grep JWT_SECRET .env

# Generar un valor correcto
openssl rand -hex 64
# Copiar el resultado al .env
```

### El dashboard muestra 0 eventos en modo simulación

El worker de captura puede estar detenido o en error. Verificar:

```bash
docker compose ps worker-capture
docker compose logs worker-capture | tail -30

# Si está reiniciando constantemente, ver el error
docker compose logs worker-capture | grep -iE "error|exception"
```

### Error en tests: `contextLoads` falla

Los tests de contexto Spring requieren que `JWT_SECRET` tenga 64+ caracteres en las propiedades de test:

```bash
# Verificar que el test usa claves suficientemente largas
grep -r "JWT_SECRET\|jwt.secret" netwatch-api-gateway/src/test/
```

### Error: `Coverage check failed` — cobertura por debajo del 70%

```bash
# Generar reporte para identificar qué falta cubrir
mvn verify -pl netwatch-api-gateway

# Abrir reporte HTML
xdg-open netwatch-api-gateway/target/site/jacoco/index.html
# Las líneas rojas son las que necesitan más tests
```

### `worker-capture` no ve la interfaz `wlan0`

Verificar que el volumen `/sys/class/net` del host está montado correctamente:

```bash
# Verificar el montaje dentro del contenedor
docker compose exec worker-capture ls /host/sys/class/net/
# Debe mostrar las interfaces del host: eth0, lo, wlan0, etc.

# Si no muestra wlan0, verificar en el host
ip link show wlan0

# Si la interfaz existe en el host pero no en el contenedor, verificar docker-compose.yml:
# volumes:
#   - /sys/class/net:/host/sys/class/net:ro
```

### `docker compose build` falla con `No such file or directory: pom.xml`

El contexto de build debe ser siempre el directorio raíz del proyecto, no la carpeta del servicio:

```bash
# CORRECTO — contexto raíz, Dockerfile específico
docker build -f netwatch-api-gateway/Dockerfile -t netwatch-api-gateway:test .

# INCORRECTO — dentro de la carpeta del servicio no encuentra el pom.xml padre
cd netwatch-api-gateway && docker build -t netwatch-api-gateway:test .
```

### API Gateway devuelve 503 al consultar `/api/v1/capture/**`

El `CaptureProxyController` actúa como proxy hacia `worker-capture` (puerto 8082). Si ese servicio no está levantado, el gateway responde **503 Service Unavailable** — comportamiento esperado y correcto:

```bash
# Verificar que worker-capture esté corriendo
docker compose ps worker-capture

# Levantarlo si está detenido
docker compose up -d worker-capture

# El 503 desaparece en cuanto el worker responde en :8082/actuator/health
```

---

### Prometheus no carga las reglas de alerta

```bash
# Verificar que el volumen está montado
docker compose exec prometheus ls /etc/prometheus/
# Debe mostrar: prometheus.yml y alert-rules.yml

# Verificar la sintaxis de las reglas
docker compose exec prometheus promtool check rules /etc/prometheus/alert-rules.yml

# Contar las reglas cargadas
curl http://localhost:9090/api/v1/rules | python3 -m json.tool | grep '"name"'
```

### Valkey/Redis — error de autenticación en worker-osint

```bash
# Verificar la contraseña correcta
grep REDIS_PASSWORD .env

# Test manual de conexión
docker compose exec valkey valkey-cli -a "$REDIS_PASSWORD" ping
# Respuesta esperada: PONG
```
