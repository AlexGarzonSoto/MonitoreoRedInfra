# Manual de Seguridad — NetWatch DevSecOps

## Tabla de contenidos

1. [Modelo de amenazas STRIDE](#1-modelo-de-amenazas-stride)
2. [Pipeline de seguridad CI/CD](#2-pipeline-de-seguridad-cicd)
3. [Gestión de secretos](#3-gestión-de-secretos)
4. [Seguridad de autenticación JWT](#4-seguridad-de-autenticación-jwt)
5. [Rotación de tokens y blacklist en memoria](#5-rotación-de-tokens-y-blacklist-en-memoria)
6. [Control de acceso RBAC](#6-control-de-acceso-rbac)
7. [Observabilidad de seguridad — Prometheus + alertas](#7-observabilidad-de-seguridad--prometheus--alertas)
8. [Gestión de vulnerabilidades](#8-gestión-de-vulnerabilidades)
9. [Hardening de contenedores](#9-hardening-de-contenedores)
10. [Monitoreo de seguridad en runtime — Falco](#10-monitoreo-de-seguridad-en-runtime--falco)
11. [Respuesta a incidentes](#11-respuesta-a-incidentes)

---

## 1. Modelo de amenazas STRIDE

El análisis de amenazas sigue la metodología **STRIDE** (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege). El modelado completo se documenta en `docs/architecture/threat-model.json` (formato OWASP Threat Dragon).

### Diagrama de Flujo de Datos — DFD Nivel 0

```
Actores externos:
  [Navegador/Analista]    → HTTPS → [Caddy TLS] → [Frontend :80]
  [Frontend]              → HTTP  → [API Gateway :8080]

Flujo interno (dentro de netwatch-net):
  [API Gateway]           ↔       [PostgreSQL + TimescaleDB]
  [API Gateway]           →       [RabbitMQ] → [Worker Analysis]
  [Worker Analysis]       →       [RabbitMQ] → [Worker Alerts, Worker OSINT]
  [Worker OSINT]          →       [ip-api.com (externo, HTTPS)]
  [Worker OSINT]          ↔       [Valkey :6379 — caché]
  [Worker Capture]        ←       [Red del host (CAP_NET_RAW)]
  [Worker Scanner]        →       [nvd.nist.gov (externo, HTTPS)]
```

### Análisis de amenazas por componente

#### API Gateway (punto de entrada principal)

| Categoría STRIDE | Amenaza | Riesgo | Contramedida implementada |
|-----------------|---------|--------|--------------------------|
| **Spoofing** | Suplantación de identidad de usuario | ALTO | JWT HMAC-SHA256 con TTL corto (30 min) + refresh con rotación |
| **Spoofing** | Suplantación de servicio interno | MEDIO | Red Docker aislada (`netwatch-net`), credenciales en RabbitMQ |
| **Tampering** | Modificación de tokens JWT en tránsito | ALTO | Firma HMAC-SHA256 con clave de 64+ bytes; cualquier modificación invalida el token |
| **Tampering** | Inyección SQL en filtros de eventos | ALTO | Queries parametrizadas con JPA/JPQL; sin concatenación de strings |
| **Repudiation** | Negar acciones administrativas | MEDIO | Logs de Spring Security con timestamp, IP de origen y userId |
| **Information Disclosure** | Enumeración de usuarios válidos via login | MEDIO | Mismo mensaje de error para email no encontrado y password incorrecto |
| **Information Disclosure** | Datos sensibles en logs | BAJO | Nivel WARN para eventos de seguridad; no se loguean contraseñas ni tokens completos |
| **Information Disclosure** | Endpoints de Actuator expuestos | MEDIO | Solo `/health` y `/prometheus` son públicos; el resto requiere autenticación (Caddy bloquea en prod) |
| **DoS** | Flood de peticiones de login | ALTO | (Pendiente en producción: rate limiting a nivel Caddy o iptables) |
| **Elevation of Privilege** | Analista accede a rutas de admin | ALTO | RBAC con `@PreAuthorize` + validación en `SecurityConfig`; doble verificación URL + método |

#### Worker Captura (mayor superficie de ataque)

| Categoría STRIDE | Amenaza | Riesgo | Contramedida implementada |
|-----------------|---------|--------|--------------------------|
| **Spoofing** | Proceso malicioso suplantando al worker en RabbitMQ | ALTO | Credenciales únicas de RabbitMQ por instancia |
| **Tampering** | Inyección de paquetes falsos en la cola | ALTO | Worker aislado en `netwatch-net`; validación de estructura del `PacketMessage` |
| **Information Disclosure** | Captura de tráfico cifrado (pasivo) | BAJO | TLS en APIs internas (pendiente producción); el worker solo analiza metadatos de paquetes |
| **DoS** | Flood de paquetes llena la cola y el disco | ALTO | `prefetchCount=20` limita mensajes en vuelo; `setDefaultRequeueRejected(false)` evita bucles |
| **Elevation of Privilege** | Escape del contenedor con `NET_RAW` | CRÍTICO | Falco monitorea accesos no autorizados; volumen `/sys/class/net` montado solo en lectura |

#### RabbitMQ (broker de mensajes)

| Categoría STRIDE | Amenaza | Riesgo | Contramedida implementada |
|-----------------|---------|--------|--------------------------|
| **Spoofing** | Servicio externo publicando mensajes en colas NetWatch | ALTO | Credenciales de RabbitMQ requeridas; red interna no accesible desde exterior |
| **Tampering** | Manipulación de mensajes en tránsito interno | MEDIO | Red Docker interna; TLS en AMQP pendiente para producción |
| **DoS** | Llenado de colas con mensajes basura | MEDIO | `setDefaultRequeueRejected(false)` — mensajes rechazados no se reencolan indefinidamente |
| **Information Disclosure** | UI de administración expuesta | ALTO | Puerto 15672 NO expuesto en producción; acceso solo via túnel SSH |

---

## 2. Pipeline de seguridad CI/CD

El pipeline ejecuta **7 etapas de seguridad** en cada push a `main` o `develop`. Todas las herramientas tienen licencia OSI aprobada.

### Etapa 1 — Gitleaks (Detección de secretos)

**Propósito:** Escanear el historial completo de Git en busca de secretos commiteados (API keys, contraseñas, tokens, certificados privados).

```yaml
- uses: gitleaks/gitleaks-action@v2
  with:
    fetch-depth: 0   # historial completo, no solo el último commit
```

**Qué detecta:** Patrones de secretos conocidos (AWS keys, Google credentials, JWT secrets, contraseñas en archivos de configuración, etc.).

**Si falla:**
1. Verificar qué archivos contienen secretos: `gitleaks detect --source . --verbose`
2. Si es un falso positivo, agregar excepción en `.gitleaks.toml`
3. Si es un secreto real → **rotar inmediatamente** el secreto comprometido antes de hacer el fix

> **Importante:** El archivo `.env` está en `.gitignore`. Nunca commitear `.env` ni archivos con credenciales reales.

### Etapa 2 — SAST (SpotBugs + Semgrep OSS)

**Propósito:** Analizar el código fuente Java en busca de vulnerabilidades de seguridad conocidas.

#### SpotBugs + Find Security Bugs

Analiza el **bytecode Java compilado** buscando patrones de seguridad problemáticos:
- Inyección SQL (concatenación de strings en queries)
- XSS (valores de usuario en respuestas HTML sin escapar)
- Path traversal (manipulación de rutas de archivos)
- Criptografía débil (MD5, SHA1, DES, semillas predecibles)
- Deserialización insegura

```bash
mvn spotbugs:spotbugs -B
# Genera: target/spotbugsXml.xml
# Ver en HTML: mvn spotbugs:gui (abre interfaz gráfica)
```

#### Semgrep OSS (sin SEMGREP_APP_TOKEN)

Análisis **semántico** con reglas de seguridad para Java/Spring Boot basadas en OWASP Top 10:

```bash
semgrep \
  --config p/java \
  --config p/owasp-top-ten \
  --config p/spring-boot \
  --json --output semgrep-report.json .
```

Reglas que cubre:
- SQL Injection en Spring Data/JDBC
- Expression Language Injection en Spring
- Deserialización insegura con Jackson
- Secretos hardcodeados en código
- Manejo inseguro de cookies
- Uso de algoritmos criptográficos débiles

### Etapa 3 — SCA (OWASP Dependency-Check)

**Propósito:** Cruzar todas las dependencias Maven contra la base de datos de vulnerabilidades del NIST (NVD).

```bash
mvn dependency-check:check -B
# Genera: target/dependency-check-report.html y .json
# Falla el build si hay CVE con CVSS ≥ 9.0
```

**Proceso cuando detecta un CVE:**
1. Identificar la dependencia afectada en el reporte HTML
2. Verificar si hay versión parcheada: `mvn versions:display-dependency-updates -pl netwatch-api-gateway`
3. Actualizar la versión en el `pom.xml` correspondiente
4. Si no hay parche disponible: documentar en `dependency-check-suppressions.xml` con fecha de revisión

**Acelerar las actualizaciones de la base NVD** (la descarga puede tardar varios minutos sin API key):
1. Registrarse en nvd.nist.gov (gratuito)
2. Agregar como secret en GitHub: `NVD_API_KEY`

### Etapa 4 — Build + Trivy (Escaneo de imágenes Docker)

**Propósito:** Detectar CVEs en el sistema operativo base (`eclipse-temurin:21-jre-alpine`) y en las dependencias Java dentro de la imagen.

```yaml
- uses: aquasecurity/trivy-action@master
  with:
    image-ref: netwatch-api-gateway:ci-SHA
    severity: CRITICAL
    exit-code: '1'     # falla el build si hay CRITICAL
    format: sarif
```

**Si hay CVE CRITICAL:**
1. **CVE en imagen base Alpine:** actualizar a la versión más reciente del mismo tag (`eclipse-temurin:21-jre-alpine`)
2. **CVE en dependencia Java:** seguir el proceso de OWASP Dependency-Check
3. **Sin parche disponible:** crear `.trivyignore` con el CVE-ID y una justificación documentada

Plantilla de `.trivyignore`:
```
# CVE-XXXX-YYYY — Librería X v1.0
# Estado: Sin parche disponible (upstream pendiente)
# Evaluación: La funcionalidad afectada no está expuesta en NetWatch
# Revisión programada: 2025-09-01
CVE-XXXX-YYYY
```

### Etapa 5 — Tests unitarios + JaCoCo

```bash
mvn verify -B -pl netwatch-api-gateway,netwatch-worker-analysis -am
# Falla si la cobertura de líneas < 70%
```

El **umbral del 70%** obliga a que la lógica crítica (detección de amenazas, autenticación, validación de tokens) esté cubierta por tests automatizados. Los tests se ejecutan contra servicios reales de PostgreSQL y RabbitMQ (Docker Services en GitHub Actions) para detectar problemas de integración que los mocks ocultarían.

### Etapa 6 — DAST (OWASP ZAP Baseline)

**Propósito:** Escaneo pasivo de seguridad de la API mientras está corriendo (similar a lo que haría un atacante con reconocimiento básico).

```yaml
- uses: zaproxy/action-baseline@v0.10.0
  with:
    target: http://localhost:8080
    rules_file_name: .zap/rules.tsv
    fail_action: warn
```

El archivo `.zap/rules.tsv` configura qué hallazgos generan error vs advertencia:

| Regla | Acción | Descripción |
|-------|--------|-------------|
| Anti-clickjacking Header | FAIL | X-Frame-Options debe estar presente |
| X-Content-Type-Options | FAIL | Header nosniff obligatorio |
| SQL Injection | FAIL | Falla el build si ZAP detecta inyección SQL |
| XSS Reflected/Persistent | FAIL | Falla el build si hay XSS |
| Cache-control | IGNORE | No relevante para una API JSON |
| CSP Header | WARN | Advertencia (pendiente configuración) |

### Etapa 7 — IaC Scan (Checkov)

**Propósito:** Detectar malas configuraciones de seguridad en los archivos de infraestructura.

```yaml
- uses: bridgecrewio/checkov-action@master
  with:
    directory: '.'
    framework: dockerfile
```

Qué verifica en los Dockerfiles:
- Contenedores corriendo como `root` (debe usar usuario no-root)
- `COPY` de archivos sensibles (`.env`, `*.key`)
- Uso de `latest` como tag de imagen (no reproducible)
- Variables de entorno con valores hardcodeados

---

## 3. Gestión de secretos

### Principios

1. **Nunca commitear secretos** — El archivo `.env` está en `.gitignore`
2. **Usar `.env.example`** — Solo contiene placeholders, seguro de commitear y versionar
3. **Rotación periódica** — Rotar claves JWT y contraseñas cada 90 días en producción
4. **Secretos en CI/CD** — Usar GitHub Secrets exclusivamente; nunca hardcodear en workflows
5. **Principio de mínimo privilegio** — Cada servicio tiene solo las credenciales que necesita

### Secrets requeridos en GitHub Actions

| Secret | Descripción | Cómo obtenerlo |
|--------|-------------|---------------|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub | hub.docker.com → tu cuenta |
| `DOCKERHUB_TOKEN` | Access Token Docker Hub | hub.docker.com → Security → New Access Token |
| `NVD_API_KEY` | API key del NIST NVD | nvd.nist.gov → Request an API Key (gratuito) |

### Generación de claves JWT seguras

```bash
# Access token secret (64 bytes = 512 bits)
openssl rand -hex 64

# Refresh token secret (diferente al anterior — SIEMPRE distintos)
openssl rand -hex 64

# Verificar la longitud (debe ser 128 caracteres hex = 64 bytes)
echo -n "tu-clave-aqui" | wc -c
# Debe mostrar: 128
```

### Por qué mínimo 64 bytes para las claves JWT

El proyecto usa HMAC-SHA256. La RFC 7518 recomienda que la clave tenga al menos la misma longitud que el hash de salida (256 bits = 32 bytes). Sin embargo, usamos **64 bytes (512 bits)** como margen de seguridad adicional para:
- Resistir ataques de fuerza bruta futura en hardware especializado
- Cumplir con requisitos de seguridad de grado empresarial
- Prevenir ataques de colisión en implementaciones de HMAC

---

## 4. Seguridad de autenticación JWT

### Configuración

| Parámetro | Valor | Justificación |
|-----------|-------|--------------|
| Algoritmo | HMAC-SHA256 | Estándar de la industria, sin vulnerabilidades conocidas |
| TTL Access Token | 30 minutos (1800000 ms) | Limita la ventana de un token comprometido |
| TTL Refresh Token | 7 días (604800000 ms) | Balance entre seguridad y experiencia de usuario |
| Clave mínima | 64 caracteres (512 bits) | Por encima del mínimo RFC 7518 |
| Claim `type` | `"access"` / `"refresh"` | Previene uso de un refresh token como access token |
| Header | `Authorization: Bearer <token>` | Estándar OAuth 2.0 / RFC 6750 |
| Refresh header | `X-Refresh-Token: <token>` | Header personalizado — evita confusión con el access token |

### Flujo completo de autenticación

```
1. POST /api/v1/auth/login
   → Validar formato del email (regex) y longitud de password
   → findByEmailAndActiveTrue(email) → si no existe, MISMO error que password incorrecto
   → BCrypt.checkpw(password, hash) con strength 12
   → Si falla, MISMO error genérico (anti-enumeración)
   → Generar access token: sub=userId, type=access, role=ADMIN, exp=+30min
   → Generar refresh token: sub=userId, type=refresh, exp=+7dias
   ← 200 OK {accessToken, refreshToken, tokenType, expiresIn, role}

2. Request autenticado:
   → Header: Authorization: Bearer <accessToken>
   → JwtAuthFilter.doFilterInternal():
     a. Extraer token del header (substring(7) después de "Bearer ")
     b. validateAccessToken(token): verificar firma HMAC y claim type=="access"
     c. Si válido: construir UsernamePasswordAuthenticationToken con ROLE_{role}
     d. SecurityContextHolder.setAuthentication(auth)
   → El request continúa con la identidad del usuario

3. Access token expirado (30 min):
   → POST /api/v1/auth/refresh
   → Header: X-Refresh-Token: <refreshToken>
   → Validar firma y claim type=="refresh"
   → Verificar que el token NO está en la blacklist en memoria
   → Marcar el refresh token como usado (agregar a blacklist)
   → Generar nuevos access + refresh tokens
   ← 200 OK {nuevos tokens}

4. Logout:
   → POST /api/v1/auth/logout
   → Header: Authorization: Bearer <accessToken>
   ← 204 No Content
   (Los access tokens expirarán en máximo 30 min)
```

### Protección contra enumeración de usuarios

El `AuthService` usa el **mismo mensaje de error** para email no encontrado y contraseña incorrecta:

```java
// CORRECTO — no revela si el email existe
throw new BadCredentialsException("Credenciales inválidas");

// INCORRECTO — revela que el email existe en el sistema
throw new BadCredentialsException("Contraseña incorrecta para " + email);
```

Esto previene que un atacante enumere emails válidos mediante el comportamiento del sistema.

---

## 5. Rotación de tokens y blacklist en memoria

### Problema que resuelve

Sin rotación, un refresh token robado puede usarse indefinidamente (hasta 7 días) incluso después de que el usuario ha cerrado sesión o renovado sus credenciales.

### Implementación actual

`AuthService.java` implementa una **blacklist en memoria** usando `ConcurrentHashMap<String, Long>`:

```java
// Estructura: token → timestamp de expiración Unix
private final ConcurrentHashMap<String, Long> usedRefreshTokens = new ConcurrentHashMap<>();

// Al usar un refresh token:
usedRefreshTokens.put(refreshToken, Instant.now().getEpochSecond() + 604800L);

// Al validar un refresh token:
if (usedRefreshTokens.containsKey(refreshToken)) {
    log.warn("Intento de reutilizar refresh token ya rotado — posible token robado");
    throw new BadCredentialsException("Refresh token ya fue utilizado");
}

// Limpieza automática cada hora (Spring @Scheduled):
@Scheduled(fixedRate = 3_600_000)
public void limpiarBlacklist() {
    long ahora = Instant.now().getEpochSecond();
    usedRefreshTokens.entrySet().removeIf(e -> e.getValue() < ahora);
}
```

### Limitaciones y evolución futura

| Aspecto | Implementación actual | Versión futura |
|---------|----------------------|----------------|
| Persistencia | Solo en memoria (se pierde al reiniciar) | Valkey/Redis con TTL automático |
| Distribución | Solo un nodo | Multi-nodo con Redis Cluster |
| Revocación explícita | No soportada (logout no invalida tokens) | Blacklist en Redis consultada en cada request |

### Monitorear intentos de reutilización de tokens

```bash
# Ver en los logs intentos sospechosos
docker compose logs api-gateway | grep "Intento de reutilizar refresh token"

# Correlacionar con IP de origen (con Spring Access Log habilitado)
docker compose logs api-gateway | grep "refresh token" | awk '{print $1, $NF}'
```

---

## 6. Control de acceso RBAC

### Roles del sistema

| Rol | Descripción | Usuarios típicos |
|-----|-------------|-----------------|
| `ADMIN` | Acceso total — gestión de usuarios y configuración | Administradores del sistema |
| `ANALYST` | Gestión de eventos y alertas — ciclo de vida completo | Equipo SOC, analistas de seguridad |
| `VIEWER` | Solo lectura — visualización de eventos y alertas | Directivos, auditores externos |

### Configuración de rutas en `SecurityConfig.java`

```java
// Rutas públicas (sin autenticación)
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

// Lectura → cualquier rol autenticado
.requestMatchers(HttpMethod.GET, "/api/v1/events/**")
    .hasAnyRole("VIEWER", "ANALYST", "ADMIN")
.requestMatchers(HttpMethod.GET, "/api/v1/alerts/**")
    .hasAnyRole("VIEWER", "ANALYST", "ADMIN")

// Escritura → requiere ANALYST o ADMIN
.requestMatchers(HttpMethod.PATCH, "/api/v1/events/**")
    .hasAnyRole("ANALYST", "ADMIN")
.requestMatchers(HttpMethod.PATCH, "/api/v1/alerts/**")
    .hasAnyRole("ANALYST", "ADMIN")

// Administración → solo ADMIN
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")
.requestMatchers("/api/v1/capture/**").hasRole("ADMIN")
```

### Doble validación (defensa en profundidad)

Además de la seguridad a nivel de URL en `SecurityConfig`, los métodos críticos usan `@PreAuthorize` como segunda línea de defensa:

```java
@PreAuthorize("hasRole('ADMIN')")
public void cambiarInterfazCaptura(String interfaz) { ... }

@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
public EventDTO resolveEvent(UUID id) { ... }
```

---

## 7. Observabilidad de seguridad — Prometheus + alertas

### Reglas de alerta configuradas

Las reglas están en `monitoring/prometheus/alert-rules.yml` y se cargan automáticamente en Prometheus.

#### Grupo: Disponibilidad de servicios

```yaml
# Servicio NetWatch sin responder más de 1 minuto
ServicioNetWatchCaido:
  expr: up{job=~"netwatch-.*"} == 0
  for: 1m
  severity: critical

# RabbitMQ sin responder más de 1 minuto
RabbitMQCaido:
  expr: up{job="rabbitmq"} == 0
  for: 1m
  severity: critical
```

#### Grupo: Recursos JVM

```yaml
# Heap JVM por encima del 85% durante 5 minutos → posible memory leak
MemoriaJVMAlta:
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
  for: 5m
  severity: warning

# Heap JVM por encima del 95% durante 2 minutos → riesgo de OutOfMemoryError
MemoriaJVMCritica:
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.95
  for: 2m
  severity: critical
```

#### Grupo: Errores HTTP

```yaml
# Tasa de errores 5xx superior al 5% durante 3 minutos → posible incidente
TasaErroresHTTPAlta:
  expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
        / sum(rate(http_server_requests_seconds_count[5m])) by (job) > 0.05
  for: 3m
  severity: warning

# P99 de latencia superior a 2 segundos durante 5 minutos → degradación de servicio
LatenciaP99Alta:
  expr: histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)) > 2
  for: 5m
  severity: warning
```

#### Grupo: Colas RabbitMQ

```yaml
# Cola con más de 1000 mensajes pendientes → workers no consumen al ritmo de producción
ColaMensajesCreciendo:
  expr: rabbitmq_queue_messages{queue=~"netwatch\\..*"} > 1000
  for: 5m
  severity: warning

# Cola saturada con más de 5000 mensajes → posible pérdida de eventos de seguridad
ColaMensajesCritica:
  expr: rabbitmq_queue_messages{queue=~"netwatch\\..*"} > 5000
  for: 2m
  severity: critical
```

### Verificar alertas activas

```bash
# API de Prometheus
curl http://localhost:9090/api/v1/alerts | python3 -m json.tool

# Ver grupos de reglas cargados
curl http://localhost:9090/api/v1/rules | python3 -m json.tool | grep '"name"'

# Verificar que las reglas se cargaron correctamente
curl http://localhost:9090/api/v1/rules | python3 -c "
import sys, json
data = json.load(sys.stdin)
groups = data['data']['groups']
print(f'Grupos cargados: {len(groups)}')
for g in groups:
    print(f'  {g[\"name\"]}: {len(g[\"rules\"])} reglas')
"
```

---

## 8. Gestión de vulnerabilidades

### Proceso cuando OWASP Dependency-Check detecta un CVE

1. **Evaluar severidad** en el reporte HTML: CVSS ≥ 9.0 → el build falla automáticamente
2. **Identificar la dependencia afectada** y la versión vulnerable
3. **Verificar versiones parcheadas:**
   ```bash
   mvn versions:display-dependency-updates -pl netwatch-api-gateway
   ```
4. **Actualizar la versión** en el `pom.xml` del módulo afectado
5. **Si no hay parche disponible:** agregar excepción documentada:
   ```xml
   <!-- dependency-check-suppressions.xml -->
   <suppress>
     <notes>CVE-XXXX-YYYY — Librería X v1.0 — Sin parche. La funcionalidad
            afectada no está expuesta en NetWatch. Revisión: 2025-09-01.</notes>
     <cve>CVE-XXXX-YYYY</cve>
   </suppress>
   ```

### Proceso cuando Trivy detecta un CVE CRITICAL en imagen

1. **Identificar la capa:** ¿es del sistema base Alpine o de las dependencias Java?

   ```bash
   # Escanear localmente
   trivy image --severity CRITICAL netwatch-api-gateway:latest
   ```

2. **Si es del sistema base:** actualizar el Dockerfile
   ```dockerfile
   FROM eclipse-temurin:21-jre-alpine
   # Los tags sin versión específica se actualizan con docker pull
   ```
   ```bash
   docker compose build --no-cache api-gateway
   ```

3. **Si es una dependencia Java:** seguir el proceso de Dependency-Check

4. **Si no hay parche:** crear `.trivyignore` con justificación y fecha de revisión obligatoria

### SLA de remediación por severidad

| Severidad CVE | CVSS | Tiempo máximo de remediación |
|---------------|------|------------------------------|
| CRITICAL | 9.0 - 10.0 | 48 horas |
| HIGH | 7.0 - 8.9 | 7 días |
| MEDIUM | 4.0 - 6.9 | 30 días |
| LOW | 0.1 - 3.9 | 90 días |

---

## 9. Hardening de contenedores

### Principios implementados en los Dockerfiles

```dockerfile
# 1. Imagen base mínima — Alpine Linux (menor superficie de ataque)
FROM eclipse-temurin:21-jre-alpine

# 2. Usuario no-root dedicado al servicio
RUN addgroup -S netwatch && adduser -S netwatch -G netwatch

# 3. Directorio de trabajo con permisos mínimos
WORKDIR /app
RUN chown -R netwatch:netwatch /app

# 4. Cambiar al usuario no-root ANTES de cualquier operación
USER netwatch

# 5. No usar shell form en ENTRYPOINT (usa exec form — sin shell intermediario)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0",
            "-Djava.security.egd=file:/dev/./urandom",
            "org.springframework.boot.loader.launch.JarLauncher"]

# 6. Healthcheck incluido en la imagen
HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:PUERTO/actuator/health || exit 1
```

### Opciones de JVM para contenedores

| Flag | Propósito |
|------|-----------|
| `-XX:+UseContainerSupport` | Detecta límites de RAM del contenedor (no del host); sin esto la JVM usa el 25% de la RAM total del servidor |
| `-XX:MaxRAMPercentage=75.0` | Usa máximo el 75% de la RAM asignada al contenedor |
| `-Djava.security.egd=file:/dev/./urandom` | Generación de entropía no bloqueante en entornos sin input de hardware |

### Verificar que los contenedores corren como no-root

```bash
# Ver el usuario de cada contenedor en ejecución
docker ps --format "{{.Names}}" | xargs -I {} \
  docker inspect {} --format "{{.Name}}: {{.Config.User}}"

# Verificar dentro de un contenedor
docker compose exec api-gateway whoami
# Esperado: netwatch (no root)
```

---

## 10. Monitoreo de seguridad en runtime — Falco

Falco detecta comportamiento anómalo en contenedores durante la ejecución, basado en las reglas de `monitoring/falco/falco-rules.yml`.

### Reglas configuradas

**Regla 1: Captura no autorizada**
```yaml
rule: NetWatch captura no autorizada
condition: >
  spawned_process and proc.name in (tcpdump, tshark, scapy)
  and not container.name = "netwatch-worker-capture"
output: "Captura no autorizada (container=%container.name proc=%proc.name)"
priority: CRITICAL
```
Detecta si algún proceso diferente al worker-capture intenta capturar tráfico.

**Regla 2: Escalada de privilegios**
```yaml
rule: NetWatch escalada de privilegios
condition: >
  container and container.name startswith "netwatch"
  and proc.name in (su, sudo)
output: "Escalada de privilegios (container=%container.name proc=%proc.name)"
priority: WARNING
```
Detecta intentos de elevar privilegios dentro de cualquier contenedor NetWatch.

**Regla 3: Escritura en sistema**
```yaml
rule: NetWatch escritura en sistema
condition: >
  container and container.name startswith "netwatch"
  and write and fd.name startswith /etc
output: "Escritura en sistema (container=%container.name file=%fd.name)"
priority: WARNING
```
Detecta escrituras en `/etc` que podrían indicar compromiso del contenedor.

### Verificar que Falco está activo

```bash
docker compose logs falco | tail -20
# Debe mostrar: "Starting rule loader" y las reglas cargadas
```

---

## 11. Respuesta a incidentes

### Niveles de severidad

| Severidad | Descripción | Tiempo de respuesta objetivo |
|-----------|-------------|------------------------------|
| CRITICAL | Brecha activa, exfiltración en curso | Inmediato (< 15 min) |
| HIGH | Indicador de compromiso, ataque activo en progreso | < 1 hora |
| MEDIUM | Actividad sospechosa, posible reconocimiento | < 4 horas |
| LOW | Anomalía menor, tráfico inusual | < 24 horas |
| INFO | Evento registrado, sin acción requerida | — |

### Playbook: Respuesta a BRUTE_FORCE detectado

```bash
# 1. Identificar la IP atacante y los puertos objetivo
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@netwatch.local","password":"NetWatch2024!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/events?threatType=BRUTE_FORCE&severity=CRITICAL&size=10"

# 2. Bloquear la IP en el firewall del servidor host (acción inmediata)
IP_ATACANTE="10.0.0.5"   # reemplazar con la IP real del evento
sudo ufw deny from $IP_ATACANTE to any comment "NetWatch: BRUTE_FORCE detected"

# 3. Verificar si hubo acceso exitoso ANTES del bloqueo
docker compose logs api-gateway | grep "Login exitoso" | grep $IP_ATACANTE

# 4. Si hubo acceso: invalidar todas las sesiones activas (rotando el JWT_SECRET)
# ADVERTENCIA: Esto cierra la sesión de TODOS los usuarios
openssl rand -hex 64  # nueva clave
# Actualizar .env con la nueva JWT_SECRET y reiniciar el gateway:
docker compose up -d --no-deps api-gateway

# 5. Resolver los eventos en NetWatch
PATCH /api/v1/events/{id}/resolve

# 6. Documentar el incidente (qué pasó, cuándo, IP, acción tomada)
```

### Playbook: Respuesta a SYN_FLOOD detectado

```bash
# 1. Verificar la magnitud del ataque
docker compose logs worker-analysis | grep "SYN_FLOOD" | tail -20

# 2. Comprobar si el API Gateway sigue respondiendo
curl -sf http://localhost:8080/actuator/health
# Si timeout → el servicio está siendo afectado

# 3. Mitigación inmediata con iptables
# Limitar SYN a 25/segundo desde cualquier IP
sudo iptables -A INPUT -p tcp --syn -m limit \
  --limit 25/s --limit-burst 50 -j ACCEPT
sudo iptables -A INPUT -p tcp --syn -j DROP

# 4. Si el ataque proviene de una IP única — bloquear directamente
sudo ufw deny from $IP_ATACANTE

# 5. Si es un ataque DDoS distribuido — contactar al ISP/proveedor cloud

# 6. Ajustar el umbral de detección si es tráfico legítimo
# (modificar SYN_FLOOD_THRESHOLD en ThreatDetectionEngine.java y reconstruir)
```

### Playbook: Respuesta a alerta de Falco

```bash
# Ver alertas de Falco en tiempo real
docker compose logs -f falco

# Si detecta "captura no autorizada" en un contenedor inesperado:
# 1. Identificar el contenedor
docker ps | grep <nombre_del_contenedor>

# 2. Inspeccionar procesos activos
docker top <nombre_del_contenedor>

# 3. Si es sospechoso — aislar el contenedor inmediatamente
docker pause <nombre_del_contenedor>

# 4. Preservar evidencia (volúmenes, logs)
docker inspect <nombre_del_contenedor> > evidencia-$(date +%Y%m%d%H%M).json
docker logs <nombre_del_contenedor> > logs-$(date +%Y%m%d%H%M).txt

# 5. Notificar y escalar según el nivel de severidad
```
