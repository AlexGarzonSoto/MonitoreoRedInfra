# NetWatch — Sistema de Monitoreo y Análisis de Amenazas en Red

Sistema de detección de amenazas en tiempo real con arquitectura de microservicios, pipeline DevSecOps completo y observabilidad integrada. Trabajo final de la Especialización en Ciberseguridad con énfasis en DevSecOps.

## Requisitos técnicos

| Herramienta | Versión mínima | Propósito |
|-------------|---------------|-----------|
| Docker Engine | 26.x | Contenedores |
| Docker Compose v2 | 2.x | Orquestación local |
| Java OpenJDK | 21 LTS | Compilación (solo desarrollo) |
| Maven | 3.9+ | Build (solo desarrollo) |
| Git | 2.x | Control de versiones |

> **Nota:** Para ejecutar el sistema en modo producción, solo se necesitan Docker y Docker Compose. Java y Maven son opcionales (solo se usan para desarrollo y compilación local).

## Inicio rápido

### 1. Clonar el repositorio

```bash
git clone https://github.com/AlexGarzonSoto/MonitoreoRedInfra.git
cd MonitoreoRedInfra
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env con tus valores reales:
nano .env
```

Variables obligatorias a cambiar en `.env`:

```bash
# Generar claves JWT (64+ caracteres):
JWT_SECRET=$(openssl rand -hex 64)
JWT_REFRESH_SECRET=$(openssl rand -hex 64)

# Contraseñas seguras:
POSTGRES_PASSWORD=tu_password_seguro
RABBITMQ_PASSWORD=tu_password_seguro
REDIS_PASSWORD=tu_password_seguro
GRAFANA_PASSWORD=tu_password_seguro
```

### 3. Iniciar la aplicación

```bash
# Levantar todos los servicios
docker compose up -d

# Ver estado de los servicios
docker compose ps

# Ver logs en tiempo real
docker compose logs -f api-gateway
```

### 4. Verificar que funciona

```bash
# Healthcheck del API Gateway
curl http://localhost:8080/actuator/health

# Login con usuario de prueba
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@netwatch.local","password":"NetWatch2024!"}'
```

## Servicios y puertos

| Servicio | URL | Descripción |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | REST API principal |
| Frontend | http://localhost:3000 | Dashboard Vue.js |
| RabbitMQ UI | http://localhost:15672 | Gestión del broker |
| Prometheus | http://localhost:9090 | Métricas |
| Grafana | http://localhost:3001 | Dashboards |
| Loki | http://localhost:3100 | Logs centralizados |

## Credenciales de prueba

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| admin@netwatch.local | NetWatch2024! | ADMIN |
| analista@netwatch.local | NetWatch2024! | ANALYST |

> Cambiar en producción modificando la tabla `users` en PostgreSQL.

## Arquitectura

```
Worker Captura (8082) → [packets.raw] → Worker Analysis (8081)
                                              ↓
                                    [threats.detected] → API Gateway (8080) → PostgreSQL
                                    [alerts.notify]   → Worker Alerts (8083) → Email/Webhook
                                    [osint.enrich]    → Worker OSINT (8084)  → ip-api.com
                                                              ↓
                                                    [threats.detected] → API Gateway (enriquecido)
```

## Documentación

| Documento | Descripción |
|-----------|-------------|
| [Manual del Desarrollador](docs/development-manual.md) | Setup, compilación, contribución |
| [Manual de Despliegue](docs/deployment-manual.md) | Producción, IaC, Ansible |
| [Manual de Seguridad](docs/security-manual.md) | STRIDE, herramientas DevSecOps, CVE |
| [Manual de Usuario](docs/user-manual.md) | Uso del dashboard, flujos de trabajo |
| [Diagrama de Componentes](docs/architecture/component-diagram.puml) | Arquitectura general |
| [Diagrama de Despliegue](docs/architecture/deployment-diagram.puml) | Contenedores y redes |
| [Diagrama de Secuencia](docs/architecture/sequence-auth.puml) | Flujo autenticación JWT |

## Pipeline DevSecOps (GitHub Actions)

El pipeline ejecuta 7 etapas de seguridad en cada push:

```
secrets-scan → sast + sca → build-and-scan → unit-tests → dast → iac-scan
    ↓              ↓              ↓               ↓          ↓        ↓
 Gitleaks    SpotBugs/FSB    Trivy (CRITICAL)  JaCoCo≥70% ZAP    Checkov
             Semgrep OSS     (5 servicios)
             OWASP Dep-Check
```

## Tecnologías de seguridad

| Fase | Herramienta | Propósito |
|------|------------|-----------|
| Código | Gitleaks | Detección de secretos en git |
| Código | SpotBugs + Find Security Bugs | SAST Java |
| Código | Semgrep OSS | SAST semántico (OWASP Top 10) |
| Dependencias | OWASP Dependency-Check | SCA (CVEs en librerías) |
| Build | Trivy | Escaneo de imágenes Docker |
| Tests | JaCoCo | Cobertura ≥ 70% |
| Runtime | OWASP ZAP | DAST baseline scan |
| IaC | Checkov | Dockerfiles y docker-compose |
| Observabilidad | Falco | Runtime security en contenedores |

## Stack tecnológico

- **Backend:** Java 21 + Spring Boot 3.2.5
- **Mensajería:** RabbitMQ 3.12
- **Base de datos:** PostgreSQL 15 + TimescaleDB
- **Caché:** Valkey 7.2 (fork OSS de Redis)
- **Frontend:** Vue.js 3 + Vite + Pinia
- **Observabilidad:** Prometheus + Grafana + Loki + Promtail + Falco
- **IaC:** OpenTofu + Ansible

## Licencia

Apache License 2.0 — ver [LICENSE](LICENSE)
