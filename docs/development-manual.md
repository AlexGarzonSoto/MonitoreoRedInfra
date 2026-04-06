# Manual del Desarrollador — NetWatch

## Tabla de contenidos

1. [Requisitos de entorno](#1-requisitos-de-entorno)
2. [Clonar y configurar](#2-clonar-y-configurar)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Compilar y ejecutar localmente](#4-compilar-y-ejecutar-localmente)
5. [Ejecutar pruebas unitarias](#5-ejecutar-pruebas-unitarias)
6. [Desarrollar un nuevo microservicio](#6-desarrollar-un-nuevo-microservicio)
7. [Convenciones de código](#7-convenciones-de-código)
8. [Variables de entorno](#8-variables-de-entorno)
9. [Contribuir al proyecto](#9-contribuir-al-proyecto)
10. [Troubleshooting frecuente](#10-troubleshooting-frecuente)

---

## 1. Requisitos de entorno

### Herramientas obligatorias

| Herramienta | Versión | Instalación |
|-------------|---------|-------------|
| Java OpenJDK | 21 LTS | `sudo apt install openjdk-21-jdk` |
| Maven | 3.9+ | `sudo apt install maven` |
| Docker Engine | 26.x | [docs.docker.com](https://docs.docker.com/engine/install/) |
| Docker Compose v2 | 2.x | Incluido con Docker Desktop |
| Git | 2.x | `sudo apt install git` |

### Herramientas opcionales (recomendadas)

| Herramienta | Propósito |
|-------------|-----------|
| IntelliJ IDEA / VSCode | IDE con soporte Java/Spring |
| Postman / Bruno | Probar endpoints REST |
| DBeaver | Gestión de PostgreSQL |
| RabbitMQ Management UI | Ver mensajes en el broker |

### Verificar instalación

```bash
java -version    # openjdk 21
mvn -version     # Apache Maven 3.9.x
docker version   # 26.x
docker compose version  # v2.x
```

---

## 2. Clonar y configurar

```bash
# Clonar el repositorio
git clone https://github.com/AlexGarzonSoto/MonitoreoRedInfra.git
cd MonitoreoRedInfra

# Crear archivo de variables de entorno
cp .env.example .env

# Generar claves JWT seguras (requiere openssl)
echo "JWT_SECRET=$(openssl rand -hex 64)"
echo "JWT_REFRESH_SECRET=$(openssl rand -hex 64)"
# Copiar los valores generados al .env
nano .env
```

### Archivo `.env` — campos requeridos

```bash
# Claves JWT (mínimo 64 caracteres cada una)
JWT_SECRET=<valor generado con openssl rand -hex 64>
JWT_REFRESH_SECRET=<valor diferente generado con openssl rand -hex 64>

# Base de datos
POSTGRES_DB=netwatch
POSTGRES_USER=netwatch
POSTGRES_PASSWORD=<password seguro>

# RabbitMQ
RABBITMQ_USER=netwatch
RABBITMQ_PASSWORD=<password seguro>

# Valkey/Redis
REDIS_PASSWORD=<password seguro>

# Grafana
GRAFANA_PASSWORD=<password seguro>
```

---

## 3. Estructura del proyecto

```
proyectoFinal/
├── pom.xml                          # POM padre multi-módulo Maven
├── .env.example                     # Plantilla de variables (sin valores reales)
├── docker-compose.yml               # 13 servicios Docker
├── README.md                        # Documentación principal
│
├── netwatch-api-gateway/            # Microservicio: API REST (puerto 8080)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/netwatch/gateway/
│       ├── config/                  # RabbitMQConfig, SecurityConfig
│       ├── controller/              # AuthController, EventController, AlertController
│       ├── consumer/                # ThreatEventConsumer (RabbitMQ)
│       ├── dto/                     # LoginRequest, LoginResponse, EventDTO
│       ├── model/                   # User, NetworkEvent, Alert
│       ├── repository/              # Interfaces JPA
│       ├── security/                # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
│       └── service/                 # AuthService, EventService, AlertService
│
├── netwatch-worker-analysis/        # Microservicio: Motor de detección (puerto 8081)
│   └── src/main/java/com/netwatch/analysis/
│       ├── consumer/PacketConsumer.java
│       └── service/ThreatDetectionService.java
│
├── netwatch-worker-capture/         # Microservicio: Captura de paquetes (puerto 8082)
│   └── src/main/java/com/netwatch/capture/
│       ├── producer/PacketProducer.java
│       └── service/NetworkCaptureService.java
│
├── netwatch-worker-alerts/          # Microservicio: Notificaciones (puerto 8083)
│   └── src/main/java/com/netwatch/alerts/
│       ├── consumer/AlertConsumer.java
│       └── service/NotificationService.java
│
├── netwatch-worker-osint/           # Microservicio: Enriquecimiento OSINT (puerto 8084)
│   └── src/main/java/com/netwatch/osint/
│       ├── consumer/OsintConsumer.java
│       └── service/GeoIpService.java
│
├── infrastructure/
│   ├── sql/init.sql                 # Schema inicial de PostgreSQL + TimescaleDB
│   ├── terraform/                   # IaC con OpenTofu
│   ├── ansible/                     # Configuración de servidores
│   └── k8s/                         # Manifiestos Kubernetes
│
├── monitoring/
│   ├── prometheus/prometheus.yml    # Configuración de scrape
│   ├── grafana/dashboards/          # Dashboards JSON
│   ├── loki/                        # Configuración de logs
│   └── falco/falco-rules.yml        # Reglas de seguridad en runtime
│
└── .github/workflows/
    ├── ci.yml                       # Pipeline CI: 7 etapas de seguridad
    └── deploy.yml                   # Deploy a Docker Hub
```

---

## 4. Compilar y ejecutar localmente

### Opción A: Docker Compose (recomendado)

```bash
# Levantar todos los servicios
docker compose up -d

# Ver logs de un servicio específico
docker compose logs -f netwatch-api-gateway

# Detener todo
docker compose down

# Detener y borrar volúmenes (reinicio limpio)
docker compose down -v
```

### Opción B: Compilar con Maven

```bash
# Compilar todos los módulos
mvn clean package -DskipTests

# Compilar solo el gateway
mvn clean package -DskipTests -pl netwatch-api-gateway -am

# Ejecutar el gateway localmente (requiere PostgreSQL y RabbitMQ corriendo)
cd netwatch-api-gateway
mvn spring-boot:run
```

### Opción C: Solo la infraestructura, gateway en IDE

Útil para desarrollo: levantar solo las dependencias con Docker y ejecutar el servicio a desarrollar desde el IDE.

```bash
# Levantar solo las dependencias
docker compose up -d postgres rabbitmq valkey

# Ejecutar gateway desde el IDE con estas variables:
# DB_HOST=localhost, DB_USER=netwatch, DB_PASS=<tu_password>
# RABBITMQ_HOST=localhost, RABBITMQ_USER=netwatch, RABBITMQ_PASS=<tu_password>
# JWT_SECRET=<tu_secreto_de_64_chars>
# JWT_REFRESH_SECRET=<tu_refresh_secreto_de_64_chars>
```

---

## 5. Ejecutar pruebas unitarias

### Ejecutar todos los tests

```bash
mvn verify -pl netwatch-api-gateway,netwatch-worker-analysis -am
```

### Ejecutar tests de un módulo

```bash
# Solo el gateway
mvn test -pl netwatch-api-gateway

# Solo el worker-analysis
mvn test -pl netwatch-worker-analysis
```

### Ver reporte de cobertura JaCoCo

```bash
mvn verify -pl netwatch-api-gateway
# Abrir en browser:
xdg-open netwatch-api-gateway/target/site/jacoco/index.html
```

### Requisito de cobertura

El proyecto exige **≥ 70% de cobertura de líneas** (configurado en el POM padre con JaCoCo). Si la cobertura cae por debajo, `mvn verify` falla.

### Tests existentes

| Módulo | Clase de test | Casos |
|--------|--------------|-------|
| api-gateway | `AuthServiceTest` | Login, refresh, logout (8 casos) |
| api-gateway | `JwtTokenProviderTest` | Generación y validación JWT (12 casos) |
| api-gateway | `EventServiceTest` | CRUD de eventos (7 casos) |
| api-gateway | `AlertServiceTest` | CRUD de alertas (9 casos) |
| worker-analysis | `ThreatDetectionServiceTest` | Detección de amenazas (8 casos) |

---

## 6. Desarrollar un nuevo microservicio

### Pasos para agregar un módulo Maven

1. Crear la carpeta `netwatch-worker-nuevo/`
2. Crear `netwatch-worker-nuevo/pom.xml` con parent apuntando al POM raíz
3. Agregar `<module>netwatch-worker-nuevo</module>` en el POM raíz
4. Crear `Dockerfile` siguiendo el patrón multi-stage de los servicios existentes
5. Agregar el servicio en `docker-compose.yml`
6. Agregar el job en `.github/workflows/ci.yml` bajo la matrix de `build-and-scan`

### Patrón de RabbitMQ (igual en todos los módulos)

```java
// Constantes compartidas
EXCHANGE = "netwatch.direct"
QUEUES: netwatch.packets.raw, netwatch.threats.detected,
        netwatch.alerts.notify, netwatch.osint.enrich

// Consumer
@RabbitListener(queues = "netwatch.<cola>", containerFactory = "rabbitListenerContainerFactory")
public void procesar(MiModelo payload) { ... }

// Producer
rabbitTemplate.convertAndSend("netwatch.direct", "routing.key", miObjeto);
```

---

## 7. Convenciones de código

### Java

- Estilo: Google Java Style Guide
- Anotaciones Lombok: `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`
- Transacciones: `@Transactional(readOnly = true)` por defecto; `@Transactional` solo para escrituras
- Manejo de errores: no capturar `Exception` genérica en lógica de negocio; usar excepciones específicas de Spring

### Naming

| Elemento | Convención | Ejemplo |
|----------|-----------|---------|
| Clases | PascalCase | `ThreatDetectionService` |
| Métodos | camelCase | `detectPortScan()` |
| Constantes | UPPER_SNAKE_CASE | `SYN_FLOOD_THRESHOLD` |
| Variables de entorno | UPPER_SNAKE_CASE | `JWT_SECRET` |
| Propiedades Spring | kebab-case | `netwatch.jwt.secret` |

### Commits

Seguir el estándar Conventional Commits:

```
tipo(alcance): descripción breve

feat: nueva funcionalidad
fix: corrección de error
test: agregar o corregir tests
docs: documentación
ci: cambios en pipeline
refactor: refactorización sin cambio de comportamiento
```

---

## 8. Variables de entorno

### Mapa completo de variables

| Variable | Servicio | Descripción |
|----------|---------|-------------|
| `JWT_SECRET` | api-gateway | Clave HMAC para access tokens (≥64 chars) |
| `JWT_REFRESH_SECRET` | api-gateway | Clave HMAC para refresh tokens (≥64 chars) |
| `DB_HOST` | api-gateway, analysis | Host de PostgreSQL |
| `DB_PORT` | api-gateway, analysis | Puerto de PostgreSQL (default: 5432) |
| `DB_NAME` | api-gateway, analysis | Nombre de la base de datos |
| `DB_USER` | api-gateway, analysis | Usuario de PostgreSQL |
| `DB_PASS` | api-gateway, analysis | Contraseña de PostgreSQL |
| `RABBITMQ_HOST` | todos | Host de RabbitMQ |
| `RABBITMQ_USER` | todos | Usuario de RabbitMQ |
| `RABBITMQ_PASS` | todos | Contraseña de RabbitMQ |
| `REDIS_HOST` | worker-osint | Host de Valkey/Redis |
| `REDIS_PASSWORD` | worker-osint | Contraseña de Valkey/Redis |
| `SMTP_HOST` | worker-alerts | Servidor de correo |
| `SMTP_USER` | worker-alerts | Usuario SMTP |
| `SMTP_PASSWORD` | worker-alerts | Contraseña SMTP |
| `CAPTURE_INTERFACE` | worker-capture | Interfaz de red a capturar (ej: eth0) |

### Flujo de variables en docker-compose

```
.env → docker-compose.yml → variables de entorno del contenedor → application.properties
```

Ejemplo para el gateway:
```yaml
# docker-compose.yml
environment:
  DB_HOST: postgres
  DB_USER: ${POSTGRES_USER}   # lee de .env
  JWT_SECRET: ${JWT_SECRET}   # lee de .env
```

```properties
# application.properties del gateway
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:netwatch}
netwatch.jwt.secret=${JWT_SECRET:dev-secret-minimo-64-caracteres}
```

---

## 9. Contribuir al proyecto

### Flujo de trabajo

```bash
# 1. Crear una rama para el cambio
git checkout -b feat/mi-nueva-funcionalidad

# 2. Realizar los cambios
# ...

# 3. Ejecutar los tests antes de commitear
mvn verify -pl netwatch-api-gateway,netwatch-worker-analysis -am

# 4. Commitear con mensaje descriptivo
git commit -m "feat(gateway): agregar endpoint de búsqueda por IP"

# 5. Subir la rama
git push origin feat/mi-nueva-funcionalidad

# 6. Crear Pull Request en GitHub hacia main
```

### Checklist antes de crear un PR

- [ ] Los tests pasan (`mvn verify`)
- [ ] La cobertura JaCoCo es ≥ 70%
- [ ] No hay secretos en el código (Gitleaks lo verifica en CI)
- [ ] El Dockerfile compila correctamente
- [ ] Se actualizó `.env.example` si se añadió una nueva variable

---

## 10. Troubleshooting frecuente

### Error: `Cannot connect to RabbitMQ`

```bash
# Verificar que RabbitMQ está corriendo
docker compose ps rabbitmq

# Ver logs del broker
docker compose logs rabbitmq

# Healthcheck manual
docker compose exec rabbitmq rabbitmq-diagnostics ping
```

### Error: `JWT secret must be at least 32 characters`

La clave JWT debe tener mínimo 64 caracteres (512 bits para HMAC-SHA512). Generar con:

```bash
openssl rand -hex 64
```

### Error: `contextLoads` falla en tests

Los tests de contexto Spring usan `@MockBean` para evitar conexiones reales. Si falla:

1. Verificar que las claves JWT en `@TestPropertySource` tienen ≥ 64 caracteres
2. Verificar que los repositorios están mockeados con `@MockBean`
3. Verificar que `JpaRepositoriesAutoConfiguration` está en la lista de exclusiones

### Error: `ORA-00001 hypertable` en PostgreSQL

TimescaleDB requiere que la extensión esté instalada. El `init.sql` la instala automáticamente, pero si la BD se creó antes del SQL de inicialización:

```bash
docker compose down -v  # Borra los volúmenes
docker compose up -d postgres
# Esperar a que init.sql se ejecute automáticamente
```

### Error: `Coverage check failed`

La cobertura cayó por debajo del 70%. Para identificar qué falta cubrir:

```bash
mvn verify -pl netwatch-api-gateway
xdg-open netwatch-api-gateway/target/site/jacoco/index.html
```

El reporte HTML muestra las líneas sin cubrir en rojo.
