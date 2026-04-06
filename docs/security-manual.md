# Manual de Seguridad — NetWatch DevSecOps

## Tabla de contenidos

1. [Modelo de amenazas STRIDE](#1-modelo-de-amenazas-stride)
2. [Pipeline de seguridad CI/CD](#2-pipeline-de-seguridad-cicd)
3. [Gestión de secretos](#3-gestión-de-secretos)
4. [Seguridad de autenticación JWT](#4-seguridad-de-autenticación-jwt)
5. [Control de acceso RBAC](#5-control-de-acceso-rbac)
6. [Gestión de vulnerabilidades](#6-gestión-de-vulnerabilidades)
7. [Hardening de contenedores](#7-hardening-de-contenedores)
8. [Monitoreo de seguridad en runtime](#8-monitoreo-de-seguridad-en-runtime)
9. [Respuesta a incidentes](#9-respuesta-a-incidentes)

---

## 1. Modelo de amenazas STRIDE

El análisis de amenazas sigue la metodología **STRIDE** (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

### Diagrama de Flujo de Datos (DFD Nivel 0)

```
[Usuario]  →  [Frontend]  →  [API Gateway]  →  [PostgreSQL]
                                   ↕
                              [RabbitMQ]
                              ↙    ↓    ↘
                  [W-Analysis] [W-Alerts] [W-OSINT]
                       ↑
                  [W-Capture]  ←  [Red de red]
```

### Análisis de amenazas por componente

#### API Gateway (punto de entrada principal)

| Categoría | Amenaza | Riesgo | Contramedida |
|-----------|---------|--------|-------------|
| **Spoofing** | Suplantación de identidad de usuario | ALTO | JWT con TTL corto (30 min) + refresh tokens rotados |
| **Spoofing** | Suplantación de servicio interno | MEDIO | Autenticación en RabbitMQ; red Docker aislada |
| **Tampering** | Modificación de tokens JWT | ALTO | Firma HMAC-SHA512 con clave de 64+ bytes |
| **Tampering** | Inyección SQL en filtros de eventos | ALTO | Queries parametrizadas con JPA/JPQL nativo |
| **Repudiation** | Negar acciones administrativas | MEDIO | Logs de Spring Security con timestamp e IP |
| **Information Disclosure** | Enumeración de usuarios válidos | MEDIO | Mismo mensaje de error para email/password inválidos |
| **Information Disclosure** | Datos sensibles en logs | BAJO | Nivel de log WARN para eventos de seguridad |
| **DoS** | Flood de peticiones de login | ALTO | (Pendiente: rate limiting en producción) |
| **Elevation of Privilege** | Analista accede a rutas de admin | ALTO | RBAC con `@PreAuthorize` + `SecurityConfig` |

#### Worker Captura (mayor superficie de ataque)

| Categoría | Amenaza | Riesgo | Contramedida |
|-----------|---------|--------|-------------|
| **Tampering** | Inyección de paquetes falsos | ALTO | Worker en red aislada; validación de estructura del mensaje |
| **Information Disclosure** | Captura de tráfico cifrado | BAJO | TLS en las APIs internas (pendiente en producción) |
| **DoS** | Flood de paquetes llena la cola | ALTO | Prefetch count 20; TTL de mensajes en RabbitMQ |
| **Elevation of Privilege** | Escape del contenedor con NET_RAW | CRÍTICO | Falco monitorea accesos no autorizados; SecComp profile |

#### RabbitMQ (broker de mensajes)

| Categoría | Amenaza | Riesgo | Contramedida |
|-----------|---------|--------|-------------|
| **Spoofing** | Servicio falso publicando en colas | ALTO | Credenciales únicas por servicio (pendiente) |
| **Tampering** | Manipulación de mensajes en tránsito | MEDIO | TLS en AMQP (pendiente en producción) |
| **DoS** | Llenado de colas con mensajes basura | MEDIO | `setDefaultRequeueRejected(false)` evita bucles |

---

## 2. Pipeline de seguridad CI/CD

El pipeline de GitHub Actions ejecuta 7 etapas de seguridad en cada push a `main` o `develop`.

### Etapa 1: Gitleaks — Detección de secretos

**Herramienta:** [Gitleaks](https://github.com/gitleaks/gitleaks)  
**Propósito:** Escanear el historial completo de Git en busca de secretos (API keys, contraseñas, tokens).

```yaml
- uses: gitleaks/gitleaks-action@v2
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Gitleaks usa el historial completo (`fetch-depth: 0`) para encontrar secretos que pudieron haber sido commiteados y luego eliminados.

**Si falla:** Verificar que ningún archivo commiteado contiene secretos reales. El archivo `.gitignore` debe incluir `.env`.

### Etapa 2: SAST — Análisis estático

**Herramientas:** SpotBugs + Find Security Bugs + Semgrep OSS

#### SpotBugs + Find Security Bugs

Analiza el bytecode Java compilado buscando patrones de seguridad:
- Inyección SQL
- XSS
- Path traversal
- Criptografía débil
- Deserialización insegura

```bash
mvn spotbugs:spotbugs -B
# Genera: target/spotbugsXml.xml
```

#### Semgrep OSS (sin token)

Análisis semántico con reglas de OWASP Top 10 para Java/Spring Boot:

```bash
semgrep \
  --config p/java \
  --config p/owasp-top-ten \
  --config p/spring-boot \
  --json --output semgrep-report.json .
```

### Etapa 3: SCA — Análisis de dependencias

**Herramienta:** OWASP Dependency-Check

Cruza las dependencias Maven contra la base de datos de vulnerabilidades del NIST (NVD).

```bash
mvn dependency-check:check -B
# Falla el build si hay CVE con CVSS ≥ 9
```

**Configurar NVD API Key** (acelera las actualizaciones):
1. Registrarse en nvd.nist.gov (gratuito)
2. Agregar como secret en GitHub: `NVD_API_KEY`

### Etapa 4: Build + Trivy — Escaneo de imágenes

**Herramienta:** [Trivy](https://trivy.dev/)

Escanea las imágenes Docker en busca de CVEs en el sistema operativo base y las dependencias.

```yaml
- uses: aquasecurity/trivy-action@master
  with:
    image-ref: netwatch-api-gateway:ci-${{ github.sha }}
    severity: CRITICAL
    exit-code: '1'
```

El pipeline falla si hay vulnerabilidades **CRITICAL** sin justificación documentada.

**Para justificar un falso positivo o una excepción:**
Crear un archivo `.trivyignore` en la raíz con el CVE-ID y la justificación.

### Etapa 5: Tests + JaCoCo

```bash
mvn verify -B -pl netwatch-api-gateway,netwatch-worker-analysis -am
# Falla si cobertura < 70%
```

### Etapa 6: DAST — OWASP ZAP Baseline

Levanta el stack de staging y ejecuta un escaneo de seguridad pasivo de la API expuesta.

El archivo `.zap/rules.tsv` configura qué hallazgos causan fallo (`FAIL`), advertencia (`WARN`) o se ignoran (`IGNORE`).

### Etapa 7: IaC Scan — Checkov

Analiza los Dockerfiles y docker-compose.yml en busca de malas configuraciones de seguridad:
- Contenedores corriendo como root
- Puertos expuestos innecesariamente
- Variables de entorno con valores hardcoded
- Imágenes sin etiqueta específica de versión

---

## 3. Gestión de secretos

### Principios

1. **Nunca commitear secretos** — El archivo `.env` está en `.gitignore`
2. **Usar `.env.example`** — Solo contiene placeholders, es seguro commitear
3. **Rotación periódica** — Rotar claves JWT y contraseñas cada 90 días en producción
4. **Secretos en CI/CD** — Usar GitHub Secrets, nunca hardcodear en `ci.yml`

### Secrets requeridos en GitHub Actions

| Secret | Descripción | Cómo obtenerlo |
|--------|-------------|---------------|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub | Tu cuenta en hub.docker.com |
| `DOCKERHUB_TOKEN` | Access Token (no contraseña) | hub.docker.com → Account Settings → Security |
| `NVD_API_KEY` | API key del NIST NVD | nvd.nist.gov → Request an API Key |

### Claves JWT

```bash
# Generar clave segura (64 bytes = 512 bits para HMAC-SHA512)
openssl rand -hex 64

# NUNCA usar claves de menos de 64 caracteres
# NUNCA usar la misma clave para access y refresh tokens
```

---

## 4. Seguridad de autenticación JWT

### Configuración

| Parámetro | Valor | Razón |
|-----------|-------|-------|
| Algoritmo | HMAC-SHA512 | Resistente a ataques de fuerza bruta en la clave |
| TTL Access Token | 30 minutos | Limita el impacto de un token comprometido |
| TTL Refresh Token | 7 días | Balance entre seguridad y UX |
| Clave mínima | 64 caracteres | 512 bits para HMAC-SHA512 |
| Claim `type` | `"access"` / `"refresh"` | Previene uso de refresh token como access token |

### Flujo de autenticación

```
1. POST /api/v1/auth/login
   → Verifica email + BCrypt(password, hash)
   → Genera access token (sub=userId, role=ROLE, type=access, exp=30min)
   → Genera refresh token (sub=userId, type=refresh, exp=7d)
   ← Retorna ambos tokens

2. Request autenticado:
   → Header: Authorization: Bearer <access_token>
   → JwtAuthFilter valida firma y claim type=="access"
   → SecurityContextHolder.setAuthentication(userId, ROLE_ADMIN)

3. Cuando access token expira:
   → POST /api/v1/auth/refresh
   → Header: X-Refresh-Token: <refresh_token>
   → Valida claim type=="refresh"
   → Genera nuevos access + refresh tokens (rotación)

4. Logout:
   → POST /api/v1/auth/logout
   → En producción: agregar token a blacklist en Valkey/Redis
```

### Protección contra enumeración de usuarios

El `AuthService` retorna el mismo mensaje de error para email no encontrado y contraseña incorrecta:

```java
// CORRECTO: mismo mensaje de error en ambos casos
throw new BadCredentialsException("Credenciales inválidas");

// INCORRECTO: revela que el email existe
throw new BadCredentialsException("Contraseña incorrecta para " + email);
```

---

## 5. Control de acceso RBAC

### Configuración de rutas

```java
// SecurityConfig.java — rutas públicas
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

// Solo lectura → VIEWER, ANALYST, ADMIN
.requestMatchers(HttpMethod.GET, "/api/v1/events/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")
.requestMatchers(HttpMethod.GET, "/api/v1/alerts/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")

// Escritura → ANALYST, ADMIN
.requestMatchers(HttpMethod.PATCH, "/api/v1/events/**").hasAnyRole("ANALYST", "ADMIN")
.requestMatchers(HttpMethod.PATCH, "/api/v1/alerts/**").hasAnyRole("ANALYST", "ADMIN")

// Administración → solo ADMIN
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")
```

### Verificación en método (`@PreAuthorize`)

Además de la seguridad a nivel de URL, los métodos críticos usan `@PreAuthorize` para validación en tiempo de ejecución.

---

## 6. Gestión de vulnerabilidades

### Proceso cuando OWASP Dependency-Check detecta un CVE

1. **Evaluar severidad:** CVSS ≥ 9 → el build falla automáticamente
2. **Identificar la dependencia afectada** en el reporte HTML
3. **Verificar si hay versión parcheada disponible:**
   ```bash
   # Verificar versiones disponibles
   mvn versions:display-dependency-updates -pl netwatch-api-gateway
   ```
4. **Actualizar la versión** en el pom.xml del módulo correspondiente
5. **Si no hay parche disponible:** agregar una excepción documentada en `dependency-check-suppressions.xml`

### Proceso cuando Trivy detecta un CVE CRITICAL en una imagen

1. **Identificar la capa afectada:** ¿sistema base o dependencia Java?
2. **Si es sistema base:** actualizar la imagen base en el Dockerfile
   ```dockerfile
   # Actualizar de una versión específica a la más reciente de la misma línea
   FROM eclipse-temurin:21-jre-alpine  # (ya usa alpine, el más ligero)
   ```
3. **Si es dependencia Java:** seguir el proceso de Dependency-Check
4. **Si no hay parche:** documentar en `.trivyignore` con justificación y fecha de revisión

### Plantilla de `.trivyignore`

```
# CVE-XXXX-YYYY — Librería X v1.0
# Estado: No hay parche disponible
# Evaluación: La funcionalidad afectada no está expuesta en NetWatch
# Próxima revisión: 2025-06-01
CVE-XXXX-YYYY
```

---

## 7. Hardening de contenedores

### Principios aplicados en los Dockerfiles

```dockerfile
# 1. Imagen base mínima (Alpine Linux)
FROM eclipse-temurin:21-jre-alpine

# 2. Usuario no-root
RUN addgroup -S netwatch && adduser -S netwatch -G netwatch
USER netwatch

# 3. Permisos mínimos en archivos
RUN chown -R netwatch:netwatch /app

# 4. Sin shell interactivo en producción (usa exec form)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", ...]

# 5. Healthcheck para detección de fallos
HEALTHCHECK --interval=30s --timeout=10s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

### Opciones de JVM para contenedores

```
-XX:+UseContainerSupport       # Detecta límites de RAM del contenedor (no del host)
-XX:MaxRAMPercentage=75.0      # Usa máximo el 75% de la RAM disponible
-Djava.security.egd=file:/dev/./urandom  # Generación de entropía no bloqueante
```

---

## 8. Monitoreo de seguridad en runtime

### Falco — reglas de seguridad

Las reglas en `monitoring/falco/falco-rules.yml` detectan:

1. **Captura no autorizada:** Si un proceso diferente al worker-capture ejecuta `tcpdump`, `tshark` o `scapy`
2. **Escalada de privilegios:** Si se ejecuta `su` o `sudo` dentro de un contenedor NetWatch
3. **Escritura en `/etc`:** Si un contenedor NetWatch modifica archivos de configuración del sistema

### Alertas de seguridad en Prometheus/Grafana

Configurar alertas para:
- Tasa de logins fallidos > 10/min desde la misma IP (posible fuerza bruta)
- Profundidad de cola `netwatch.threats.detected` > 1000 (posible flood)
- Errores HTTP 401 > 100/min (posible ataque de autenticación)
- Latencia del API Gateway > 2s (posible DoS)

---

## 9. Respuesta a incidentes

### Niveles de severidad

| Severidad | Descripción | Tiempo de respuesta |
|-----------|-------------|---------------------|
| CRITICAL | Brecha activa, exfiltración en curso | Inmediato (< 15 min) |
| HIGH | Indicador de compromiso, ataque activo | < 1 hora |
| MEDIUM | Actividad sospechosa, posible reconocimiento | < 4 horas |
| LOW | Anomalía menor, revisar en próximo turno | < 24 horas |
| INFO | Evento registrado, sin acción requerida | — |

### Playbook de respuesta a BRUTE_FORCE

```bash
# 1. Identificar la IP atacante
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/events?threatType=BRUTE_FORCE&severity=CRITICAL"

# 2. Bloquear la IP en el firewall del host (inmediato)
sudo ufw deny from <IP_ATACANTE>

# 3. Verificar si hubo acceso exitoso (revisar logs)
docker compose exec api-gateway grep "Login exitoso" /app/logs/app.log | grep "<IP_ATACANTE>"

# 4. Resolver los eventos y alertas en NetWatch
# (usar la interfaz web o los endpoints PATCH)

# 5. Documentar el incidente
```

### Playbook de respuesta a SYN_FLOOD

```bash
# 1. Identificar magnitud del ataque
docker compose logs netwatch-worker-analysis | grep "SYN_FLOOD"

# 2. Verificar si el servicio está respondiendo
curl -sf http://localhost:8080/actuator/health

# 3. Activar rate limiting temporal (nginx o iptables)
sudo iptables -A INPUT -p tcp --syn -m limit \
  --limit 25/s --limit-burst 50 -j ACCEPT
sudo iptables -A INPUT -p tcp --syn -j DROP

# 4. Notificar al upstream ISP si el ataque es externo

# 5. Documentar y ajustar umbrales en ThreatDetectionService si es necesario
```
