# NetWatch — Explicación Completa del Proyecto

<p align="center">
  <img src="imagenes/Logo.png" width="300">
</p>

> Documento de estudio para entender y explicar cada parte del sistema.
> Cubre: arquitectura, código, base de datos, infraestructura y pipeline DevSecOps.

---

## Tabla de contenidos

1. [¿Qué es NetWatch?](#1-qué-es-netwatch)
2. [Arquitectura general](#2-arquitectura-general)
3. [La base de datos — PostgreSQL + TimescaleDB](#3-la-base-de-datos--postgresql--timescaledb)
4. [El broker de mensajes — RabbitMQ](#4-el-broker-de-mensajes--rabbitmq)
5. [Microservicios — explicación detallada](#5-microservicios--explicación-detallada)
   - 5.1 [worker-capture](#51-worker-capture-puerto-8082)
   - 5.2 [worker-analysis](#52-worker-analysis-puerto-8081)
   - 5.3 [api-gateway](#53-api-gateway-puerto-8080)
   - 5.4 [worker-alerts](#54-worker-alerts-puerto-8083)
   - 5.5 [worker-osint](#55-worker-osint-puerto-8084)
   - 5.6 [worker-scanner](#56-worker-scanner-sin-puerto-expuesto)
6. [Frontend — Vue.js 3](#6-frontend--vuejs-3)
7. [Infraestructura de soporte](#7-infraestructura-de-soporte)
8. [El pipeline DevSecOps — CI/CD](#8-el-pipeline-devsecops--cicd)
9. [Seguridad — modelo STRIDE](#9-seguridad--modelo-stride)
10. [Lo que falta en el proyecto](#10-lo-que-falta-en-el-proyecto)
11. [Cómo explicar el proyecto en 5 minutos](#11-cómo-explicar-el-proyecto-en-5-minutos)
12. [Puertos de todos los servicios](#12-puertos-de-todos-los-servicios)
13. [Credenciales de prueba](#13-credenciales-de-prueba)

---

## 1. ¿Qué es NetWatch?

NetWatch es una plataforma de monitoreo de red orientada a la detección temprana de amenazas, diseñada bajo un enfoque DevSecOps. Su propósito principal es demostrar cómo la seguridad puede integrarse de forma continua en todas las etapas del ciclo de vida del software, desde el desarrollo hasta la operación en producción. A nivel funcional, el sistema permite analizar tráfico de red en tiempo real, identificar comportamientos sospechosos y generar alertas automáticas que facilitan la respuesta ante incidentes de seguridad.

### En términos simples

NetWatch hace cinco cosas:

1. **Escucha** el tráfico de la red (captura paquetes TCP/UDP)
2. **Detecta** patrones maliciosos (SYN Flood, Brute Force, Port Scan, etc.)
3. **Notifica** al equipo de seguridad (email + webhook)
4. **Geolocaliza** los atacantes (país, ciudad, ASN)
5. **Muestra todo** en un dashboard web en tiempo real

### Contexto académico

El trabajo demuestra un pipeline DevSecOps con las siguientes fases y herramientas:

```
PLAN    → OWASP Threat Dragon (modelado de amenazas STRIDE)
CODE    → Gitleaks, SpotBugs, Semgrep OSS
BUILD   → Trivy (escaneo de imágenes Docker)
TEST    → JUnit 5, JaCoCo (cobertura ≥70%), OWASP ZAP Baseline (DAST)
RELEASE → Checkov (IaC: Dockerfiles, docker-compose)
DEPLOY  → OpenTofu + Ansible → Docker Hub
OPERATE → Prometheus + Grafana + Loki + Promtail + Falco
```

---

## 2. Arquitectura general

### Diagrama de flujo

```
INTERNET / RED LOCAL
       │
       ▼
┌──────────────────────┐
│   worker-capture     │  Pcap4J captura paquetes de la interfaz de red
│   (puerto 8082)      │  Si no hay interfaz disponible → modo simulación
└──────────┬───────────┘
           │ publica PacketMessage
           ▼
┌──────────────────────┐
│      RabbitMQ        │  Cola: netwatch.packets.raw
│  (broker de AMQP)    │  El broker distribuye los mensajes
└──────────┬───────────┘
           │ consume PacketMessage
           ▼
┌──────────────────────┐
│   worker-analysis    │  Motor de detección (6 reglas STRIDE)
│   (puerto 8081)      │  Clasifica: SYN_FLOOD, BRUTE_FORCE, PORT_SCAN…
└─────┬────────┬────────┘
      │        │
      │        │ si HIGH/CRITICAL → publica también a:
      │        ├─────────────────────────────────────────┐
      │        │                                         │
      │ threats.detected                        alerts.notify + osint.enrich
      │        │                                         │
      ▼        ▼                              ┌──────────┴────────────┐
┌──────────────────────┐              ┌───────┴──────┐  ┌────────────┴────────┐
│    api-gateway       │              │worker-alerts │  │  worker-osint       │
│   (puerto 8080)      │              │ (pto. 8083)  │  │  (puerto 8084)      │
│  persiste en BD      │              │ email+webhook│  │  ip-api.com → cache │
│  expone REST API     │◄─────────────┴──────────────┘  └────────────┬────────┘
└──────────┬───────────┘       eventos enriquecidos con geodatos      │
           │ REST API                                                  │
           ▼                                                           │
┌──────────────────────┐                                   publica ThreatMessage
│     frontend         │                                   enriquecido de vuelta
│  Vue.js (pto. 3000)  │                                   → threats.detected
└──────────┬───────────┘
           │ solicita escaneo
           ▼
┌──────────────────────┐
│   worker-scanner     │  Nmap detecta versiones
│  (sin puerto HTTP)   │  NvdCorrelation busca CVEs en NIST NVD
└──────────────────────┘
```

### Resumen de componentes

| Componente | Tecnología | Puerto | Rol |
|-----------|-----------|--------|-----|
| worker-capture | Java 21 + Pcap4J | 8082 | Captura paquetes de red |
| worker-analysis | Java 21 + Spring | 8081 | Detecta amenazas |
| api-gateway | Java 21 + Spring Boot | 8080 | API REST + autenticación |
| worker-alerts | Java 21 + JavaMail | 8083 | Notificaciones |
| worker-osint | Java 21 + ip-api.com | 8084 | Geolocalización |
| worker-scanner | Java 21 + Nmap | — | Escaneo de vulnerabilidades |
| frontend | Vue.js 3 + Nginx | 3000 | Dashboard web |
| PostgreSQL + TimescaleDB | DB relacional + series temporales | 5432 | Persistencia |
| RabbitMQ | Broker AMQP | 5672 / 15672 | Mensajería asíncrona |
| Valkey | Cache Redis-compatible | 6379 | Cache de geolocalizaciones |
| Prometheus | Recolección de métricas | 9090 | Observabilidad |
| Grafana | Dashboards | 3001 | Visualización de métricas |
| Loki | Almacenamiento de logs | 3100 | Centralización de logs |
| Falco | Seguridad en runtime | — | Detección de comportamiento anómalo |

---

## 3. La base de datos — PostgreSQL + TimescaleDB

### ¿Por qué TimescaleDB?

El tráfico de red genera millones de registros ordenados en el tiempo. TimescaleDB es una
**extensión de PostgreSQL** que convierte tablas normales en "hypertables": tablas que se
**particionan automáticamente por tiempo** (por día, por hora, etc.).

Beneficios concretos en NetWatch:
- Query "todos los eventos críticos de las últimas 2 horas" → muy rápida (solo lee las
  particiones de esas 2 horas, no toda la tabla)
- Inserciones rápidas aunque la tabla tenga millones de filas
- Compatible 100% con SQL estándar y con Spring Data JPA

### Tablas y su propósito

#### Tabla `users` — Gestión de acceso

```sql
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,       -- siempre BCrypt, NUNCA texto plano
    role          VARCHAR(20)  DEFAULT 'ANALYST'
                  CHECK (role IN ('ADMIN', 'ANALYST', 'VIEWER')),
    active        BOOLEAN      DEFAULT true,   -- desactivar sin borrar
    created_at    TIMESTAMPTZ  DEFAULT NOW()
);
```

Puntos clave:
- La contraseña **nunca** se guarda en texto plano. Se guarda el hash BCrypt con strength 12
  (cuanto mayor el número, más costoso computacionalmente es hacer fuerza bruta)
- El campo `active=false` permite suspender una cuenta sin eliminarla (mantiene el historial)
- Tres roles: ADMIN (gestión total), ANALYST (lectura + resolución), VIEWER (solo lectura)

#### Tabla `network_events` — El corazón del sistema (hypertable, particionada por día)

```sql
CREATE TABLE network_events (
    id            UUID         DEFAULT gen_random_uuid(),
    src_ip        VARCHAR(45)  NOT NULL,    -- IP atacante
    dst_ip        VARCHAR(45),              -- IP objetivo
    src_port      INTEGER,
    dst_port      INTEGER,
    protocol      VARCHAR(10),              -- TCP o UDP
    flags         VARCHAR(30),             -- SYN, ACK, PSH, etc.
    packet_length INTEGER,
    ttl           INTEGER,
    threat_type   VARCHAR(30),             -- PORT_SCAN, BRUTE_FORCE, SYN_FLOOD…
    severity      VARCHAR(10),             -- INFO, LOW, MEDIUM, HIGH, CRITICAL
    description   TEXT,
    country       VARCHAR(100),            -- ← rellena worker-osint
    city          VARCHAR(100),
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    asn           VARCHAR(100),            -- ej: "AS13335 Cloudflare"
    abuse_score   INTEGER DEFAULT 0,
    resolved      BOOLEAN DEFAULT false,   -- ← marca el analista
    timestamp     TIMESTAMPTZ DEFAULT NOW()
);
```

Esta tabla tiene **7 índices** para acelerar las consultas más frecuentes del dashboard:
- `idx_events_src_ip` → buscar por IP atacante
- `idx_events_severity` → filtrar por gravedad
- `idx_events_threat` → filtrar por tipo de amenaza
- `idx_events_resolved` → ver no resueltos
- `idx_events_unresolved_severity` → índice parcial solo sobre `resolved=false`
- `idx_events_severity_timestamp` → conteo por severidad en ventana de tiempo
- `idx_events_srcip_threat_time` → buscar patrones de una IP

#### Tabla `raw_packets` — Paquetes crudos antes del análisis (hypertable, por hora)

```sql
CREATE TABLE raw_packets (
    id            UUID        DEFAULT gen_random_uuid(),
    src_ip        VARCHAR(45) NOT NULL,
    dst_ip        VARCHAR(45),
    src_port      INTEGER,
    dst_port      INTEGER,
    protocol      VARCHAR(10) NOT NULL,
    flags         VARCHAR(30),
    packet_length INTEGER,
    captured_at   TIMESTAMPTZ DEFAULT NOW()
);
```

- Particionada por **hora** (más granular que network_events) porque el volumen es mayor
- worker-capture escribe aquí antes de que worker-analysis procese el paquete

#### Tabla `threat_events` — Registro del motor de análisis (hypertable, por día)

```sql
CREATE TABLE threat_events (
    id          UUID        DEFAULT gen_random_uuid(),
    src_ip      VARCHAR(45) NOT NULL,
    threat_type VARCHAR(30) NOT NULL,
    severity    VARCHAR(10) NOT NULL,
    description TEXT,
    notified    BOOLEAN     DEFAULT false,  -- ¿ya lo procesó worker-alerts?
    enriched    BOOLEAN     DEFAULT false,  -- ¿ya lo geolocalizó worker-osint?
    detected_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### Tabla `alert_logs` — Auditoría de notificaciones

```sql
CREATE TABLE alert_logs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    threat_id     UUID        NOT NULL,
    src_ip        VARCHAR(45) NOT NULL,
    threat_type   VARCHAR(30) NOT NULL,
    severity      VARCHAR(10) NOT NULL,
    channel       VARCHAR(10) CHECK (channel IN ('EMAIL', 'WEBHOOK')),
    success       BOOLEAN     DEFAULT false,
    error_message TEXT,
    sent_at       TIMESTAMPTZ DEFAULT NOW()
);
```

Registro **inmutable** de cada intento de notificación. Permite auditar: "¿se envió el
email de la alerta X? ¿Falló? ¿Por qué?"

#### Tabla `osint_records` — Cache persistente de geolocalizaciones

```sql
CREATE TABLE osint_records (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    threat_id   UUID         NOT NULL,
    ip          VARCHAR(45)  NOT NULL,
    country     VARCHAR(100),
    city        VARCHAR(100),
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    asn         VARCHAR(100),
    enriched_at TIMESTAMPTZ  DEFAULT NOW()
);
```

### Limitación de TimescaleDB: sin Foreign Keys a hypertables

```sql
-- alerts NO puede tener FK a network_events porque es hypertable
-- Por eso se usa event_id UUID sin restricción de integridad referencial
event_id UUID,  -- sin FK
```

TimescaleDB no soporta claves foráneas hacia hypertables (limitación del motor de
particionamiento). Se documenta explícitamente en el código para que no sorprenda.

---

## 4. El broker de mensajes — RabbitMQ

RabbitMQ es el **sistema nervioso** de NetWatch. Los microservicios **no se llaman
directamente entre sí** (no hay HTTP de servicio a servicio). En su lugar, publican
mensajes a colas y los consumidores los procesan de forma **asíncrona**.

### ¿Por qué asíncrono?

Si worker-analysis estuviera lleno de trabajo y worker-capture llamara directamente
al análisis vía HTTP, los paquetes se perderían o el sistema fallaría. Con RabbitMQ, los
paquetes **se acumulan en la cola** hasta que worker-analysis pueda procesarlos. No se
pierde nada.

### Configuración de colas

```
Exchange: netwatch.direct (tipo DirectExchange, durable=true)

Cola: netwatch.packets.raw      ← routing key: packets.raw
Cola: netwatch.threats.detected ← routing key: threats.detected
Cola: netwatch.alerts.notify    ← routing key: alerts.notify
Cola: netwatch.osint.enrich     ← routing key: osint.enrich
Cola: netwatch.scan.requests    ← routing key: scan.requests
Cola: netwatch.scan.results     ← routing key: scan.results
```

**DirectExchange**: el mensaje llega al exchange con una routing key y RabbitMQ lo
entrega exactamente a la cola que tiene ese binding key. Es el tipo más simple y eficiente.

**durable=true**: si RabbitMQ se reinicia (o cae), las colas y mensajes pendientes
sobreviven. Sin durable, al reiniciar se perderían todos los mensajes en cola.

### Flujo completo de un mensaje

```
1. worker-capture → publica PacketMessage → cola: netwatch.packets.raw

2. worker-analysis → consume de netwatch.packets.raw
   → analiza el paquete con ThreatDetectionService
   → si detecta amenaza:
       → publica ThreatMessage → cola: netwatch.threats.detected
   → si severidad es HIGH o CRITICAL:
       → publica ThreatMessage → cola: netwatch.alerts.notify
       → publica ThreatMessage → cola: netwatch.osint.enrich

3. api-gateway → consume de netwatch.threats.detected
   → persiste el evento en la tabla network_events de PostgreSQL

4. worker-alerts → consume de netwatch.alerts.notify
   → si CRITICAL: envía email + webhook
   → si HIGH: envía solo webhook

5. worker-osint → consume de netwatch.osint.enrich
   → consulta ip-api.com con la IP del atacante
   → guarda resultado en caché Valkey (TTL 1 hora)
   → enriquece el ThreatMessage con country, city, lat, lon, asn
   → publica de vuelta → cola: netwatch.threats.detected

6. api-gateway → consume nuevamente el evento enriquecido
   → actualiza network_events con los datos de geolocalización
```

### Configuración de rendimiento del consumer

```java
factory.setConcurrentConsumers(3);      // mínimo 3 hilos procesando en paralelo
factory.setMaxConcurrentConsumers(10);  // escala hasta 10 bajo alta carga
factory.setPrefetchCount(20);           // cada hilo pre-carga 20 mensajes
factory.setDefaultRequeueRejected(false); // mensajes fallidos → no vuelven a la cola
```

El sistema puede procesar hasta **200 mensajes simultáneamente** (10 hilos × 20 mensajes
cada uno) sin drops. Si la carga supera esto, los mensajes se acumulan en la cola hasta
que haya capacidad.

### RabbitMQ Management UI

Accesible en `http://localhost:15672`. Permite ver en tiempo real:
- Cuántos mensajes están en cada cola
- Tasa de publicación y consumo por segundo
- Estado de los consumers conectados

---

## 5. Microservicios — explicación detallada

Todos los microservicios están escritos en **Java 21 con Spring Boot 3.2.5**. Comparten
el mismo POM padre (`pom.xml` raíz) que centraliza las versiones de todas las dependencias.

### 5.1 worker-capture (puerto 8082)

**Propósito:** Capturar paquetes de la interfaz de red y enviarlos a RabbitMQ.

#### Modo real (producción)

Usa **Pcap4J** — wrapper Java de libpcap (la biblioteca de captura de paquetes del
sistema operativo). Opera en **modo promiscuo**, lo que significa que captura TODO el
tráfico de la red, no solo el dirigido al propio host.

Requisitos del modo real:
- La interfaz de red debe existir (ej: `eth0`, `wlan0`)
- El contenedor debe tener `cap_add: [NET_RAW, NET_ADMIN]` en docker-compose
- `network_mode: host` para ver el tráfico del host

Usa **hilo virtual de Java 21** (`Thread.ofVirtual().name("packet-capture").start(...)`).
Los hilos virtuales (Project Loom) son ultraligeros comparados con los OS-threads
tradicionales, ideales para I/O bloqueante como la captura de paquetes.

#### Modo simulación (desarrollo / CI)

Se activa automáticamente cuando:
- La interfaz configurada no existe en el sistema
- Pcap4J lanza una excepción (entorno sin libpcap)
- `CAPTURE_ENABLED=false` en las variables de entorno

```java
// Genera 5 paquetes aleatorios cada 5 segundos
String[] ips    = {"10.0.0.1", "10.0.0.2", "192.168.1.10", "172.16.0.5", "10.10.10.20"}
int[]    ports  = {22, 80, 443, 3389, 53, 8080, 5432, 3306}
String[] protos = {"TCP", "UDP"}
String[] flags  = {"SYN", "SYN,ACK", "ACK", "PSH,ACK", "RST"}
```

Los paquetes simulados cubren todos los tipos de amenaza que el motor de análisis puede
detectar: SYN floods (flag=SYN), brute force (puerto 22), DNS tunneling (UDP puerto 53).

#### Configuración especial en docker-compose

```yaml
worker-capture:
  network_mode: host        # comparte la red del host, ve todo el tráfico
  cap_add:
    - NET_RAW               # permiso para captura raw de paquetes
    - NET_ADMIN             # permiso para configurar interfaces
```

`network_mode: host` implica que este servicio no está en la red `netwatch-net`. Se
comunica con RabbitMQ usando `localhost` en lugar del nombre del servicio `rabbitmq`.

#### API de control

```
GET  /api/capture/interfaces  → lista las interfaces disponibles en el host
POST /api/capture/interface   → cambia la interfaz activa sin reiniciar
GET  /actuator/health         → incluye estado de la captura (running: true/false)
```

---

### 5.2 worker-analysis (puerto 8081)

**Propósito:** Analizar paquetes y detectar patrones de amenazas usando reglas estáticas
basadas en el modelo STRIDE.

#### El motor de detección — 6 reglas

```java
// Archivo: ThreatDetectionService.java
// Evalúa cada paquete contra 6 reglas en orden de prioridad

// 1. SYN_FLOOD — Denial of Service
// Condición: flag TCP es exactamente "SYN" (sin ACK)
// Lógica: en un handshake TCP normal, el cliente envía SYN, el servidor responde
// SYN,ACK, y el cliente confirma con ACK. Un flood de SYN sin ACK agota
// los recursos del servidor (half-open connections).
if ("SYN".equals(packet.flags())) → HIGH

// 2. BRUTE_FORCE — Elevation of Privilege
// Condición: TCP a puertos de administración conocidos
// Puertos sensibles: 22 (SSH), 23 (Telnet), 3389 (RDP), 5900 (VNC),
//                   21 (FTP), 5985 (WinRM)
if (TCP && dstPort in ADMIN_PORTS) → MEDIUM

// 3. MALWARE_C2 — Elevation of Privilege
// Condición: destino a puertos usados por Command & Control de malware
// Puertos C2: 4444 (Metasploit default), 1337 (leet), 8888, 9999,
//             6666, 31337 (elite), 12345
if (dstPort in C2_PORTS) → CRITICAL

// 4. DNS_TUNNELING — Information Disclosure
// Condición: UDP puerto 53 con payload mayor a 512 bytes
// Lógica: los paquetes DNS normales son pequeños (<512 bytes).
// Un atacante puede tunelizar datos exfiltrándolos codificados en queries DNS.
if (UDP && dstPort == 53 && packetLength > 512) → HIGH

// 5. DATA_EXFILTRATION — Tampering
// Condición: puerto destino > 1024 (no estándar) y payload > 8192 bytes
// Lógica: tráfico saliente muy grande a puertos no estándar sugiere
// transferencia de datos sensibles.
if (dstPort > 1024 && packetLength > 8192) → HIGH

// 6. PORT_SCAN — Information Disclosure
// Condición: destino a puerto < 1024 (privilegiado) desde puerto efímero
// Lógica: el atacante tiene un puerto fuente alto (efímero) y explora
// servicios estándar (HTTP=80, SSH=22, FTP=21, etc.)
if (dstPort < 1024 && srcPort > 1024) → MEDIUM
```

**Limitación importante (documentada en el código):**

Este es un motor de detección por paquete individual. No mantiene estado entre paquetes.
Un sistema profesional necesitaría análisis de series temporales para detectar patrones
como "esta IP intentó 50 puertos distintos en 60 segundos" (port scan real). Eso
requeriría un framework como Apache Flink con estado compartido distribuido.

#### PacketConsumerService

El consumidor procesa cada mensaje de RabbitMQ:

```java
@RabbitListener(queues = "netwatch.packets.raw")
public void process(PacketMessage packet) {
    Optional<DetectionResult> result = detectionService.analyze(packet);

    if (result.isPresent()) {
        DetectionResult detection = result.get();
        // Siempre publicar la amenaza detectada (la persistirá el api-gateway)
        rabbitTemplate.convertAndSend(EXCHANGE, RK_THREATS, buildThreatMessage(...));

        // Si es grave, también notificar y enriquecer con OSINT
        if (isHighOrCritical(detection.severity())) {
            rabbitTemplate.convertAndSend(EXCHANGE, RK_ALERTS, buildThreatMessage(...));
            rabbitTemplate.convertAndSend(EXCHANGE, RK_OSINT,  buildThreatMessage(...));
        }
    }
    // Si no hay detección → tráfico normal, se descarta silenciosamente
}
```

---

### 5.3 api-gateway (puerto 8080)

**Propósito:** Punto de entrada único de la API. Maneja autenticación JWT, expone los
endpoints REST, persiste eventos en PostgreSQL, y consume mensajes de RabbitMQ para
guardarlos.

#### Sistema de autenticación JWT — Flujo completo

```
PASO 1: Login
   Cliente POST /api/v1/auth/login { email, password }
   Sistema:
     a. Busca usuario en BD por email
     b. Verifica BCrypt.matches(passwordIngresada, hashAlmacenado)
     c. Si coincide → genera access_token + refresh_token
     d. Responde { accessToken, refreshToken, tokenType: "Bearer", expiresIn: 1800, role }

PASO 2: Uso del access token
   Cliente: GET /api/v1/events
   Header:  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   Sistema:
     a. JwtAuthFilter intercepta TODAS las requests
     b. Extrae el token del header Authorization
     c. Valida firma HMAC-SHA256 con la clave secreta
     d. Verifica que el claim "type" sea "access"
     e. Extrae userId y role del token
     f. Crea UsernamePasswordAuthenticationToken con ROLE_{role}
     g. Lo pone en el SecurityContext para que @PreAuthorize funcione

PASO 3: Renovación del token (refresh)
   Cliente (cuando access_token expira):
   POST /api/v1/auth/refresh
   Header: X-Refresh-Token: eyJhbGciOiJIUzI1NiJ9...
   Sistema:
     a. Verifica que el refresh_token no esté en la blacklist (ya usado)
     b. Valida firma HMAC-SHA256 con la clave de refresh
     c. Extrae userId
     d. Añade el refresh_token actual a la blacklist
     e. Genera nuevo access_token + nuevo refresh_token
     f. Responde con los nuevos tokens

PASO 4: Logout
   POST /api/v1/auth/logout
   Header: Authorization: Bearer <refresh_token>
   Sistema: añade el token a la blacklist para invalidarlo
```

#### Tokens JWT — Especificaciones técnicas

```
Algoritmo:      HMAC-SHA256 (simétrico)
Clave mínima:   64 caracteres (512 bits para SHA-256)
Access token:   TTL 30 minutos (1800000 ms)
Refresh token:  TTL 7 días (604800000 ms)
Claims:         sub=userId, type="access"|"refresh", role=ADMIN|ANALYST|VIEWER
```

**Por qué dos claves distintas para access y refresh?**
Si la clave fuera la misma, un atacante que obtuviera un access token podría usarlo como
refresh token (ya que la firma sería válida con la misma clave). Con claves distintas, los
tokens son intercambiables entre sí.

#### Blacklist de refresh tokens

```java
// En memoria — ConcurrentHashMap thread-safe
private final ConcurrentHashMap<String, Long> usedRefreshTokens = new ConcurrentHashMap<>();

// El valor es el timestamp de expiración en epoch segundos
// Cuando se usa un refresh token, se marca como "ya usado"
usedRefreshTokens.put(refreshToken, Instant.now().getEpochSecond() + 604800L);

// Limpieza automática cada hora para evitar fuga de memoria
@Scheduled(fixedRate = 3_600_000)
public void limpiarBlacklist() {
    usedRefreshTokens.entrySet().removeIf(e -> e.getValue() < now);
}
```

**Limitación:** Esta blacklist está en memoria de la JVM. Si el api-gateway se reinicia,
la blacklist se pierde. En producción enterprise se usaría Redis/Valkey para persistir
la blacklist. Para el alcance académico, esta implementación es suficiente y está
documentada.

#### Control de acceso por roles (RBAC)

```java
// En SecurityConfig.java
.requestMatchers(HttpMethod.GET,   "/api/v1/events/**").hasAnyRole("VIEWER","ANALYST","ADMIN")
.requestMatchers(HttpMethod.PATCH, "/api/v1/events/**").hasAnyRole("ANALYST","ADMIN")
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")

// En los controllers con @PreAuthorize
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
public ResponseEntity<EventDTO> resolveEvent(@PathVariable UUID id) { ... }
```

RBAC (Role-Based Access Control): el control de acceso se basa en el rol del usuario,
no en el usuario específico. Agregar un nuevo ANALYST automáticamente le da todos los
permisos del rol, sin configurar permisos individuales.

#### Endpoints REST disponibles

```
── Autenticación ──────────────────────────────────────────────────────────────
POST   /api/v1/auth/login          → { accessToken, refreshToken, role }
POST   /api/v1/auth/refresh        → { accessToken, refreshToken, role }
POST   /api/v1/auth/logout         → 204 No Content

── Eventos de red ─────────────────────────────────────────────────────────────
GET    /api/v1/events              → Page<EventDTO> (paginado, ordenado por timestamp)
GET    /api/v1/events/{id}         → EventDTO
PATCH  /api/v1/events/{id}/resolve → EventDTO (marca como resuelto)
GET    /api/v1/events/stats/summary → {
                                        totalEvents, criticalCount, highCount,
                                        unresolvedCount, recentEvents (última 1h)
                                      }

── Alertas ────────────────────────────────────────────────────────────────────
GET    /api/v1/alerts              → Page<Alert> filtrado por status=OPEN|ACK|RESOLVED
PATCH  /api/v1/alerts/{id}/acknowledge → Alert (pasa a ACKNOWLEDGED)
PATCH  /api/v1/alerts/{id}/resolve     → Alert (pasa a RESOLVED)

── Escáner de vulnerabilidades ────────────────────────────────────────────────
POST   /api/v1/scan                → lanza escaneo Nmap (async)
GET    /api/v1/scan/results        → lista de ScanResult con CVEs encontrados

── Control de captura ─────────────────────────────────────────────────────────
GET    /api/v1/capture/interfaces  → lista interfaces de red del host
POST   /api/v1/capture/interface   → cambia interfaz activa

── Reportes ───────────────────────────────────────────────────────────────────
GET    /api/v1/reports/summary     → resumen estadístico del período

── Remediación ────────────────────────────────────────────────────────────────
GET    /api/v1/remediation/{id}    → guía de remediación para la amenaza

── Gestión de usuarios (solo ADMIN) ───────────────────────────────────────────
GET    /api/v1/users               → lista de usuarios
POST   /api/v1/users               → crear usuario
PATCH  /api/v1/users/{id}          → actualizar usuario
DELETE /api/v1/users/{id}          → desactivar usuario

── Salud y métricas (públicos) ────────────────────────────────────────────────
GET    /actuator/health            → estado del servicio
GET    /actuator/prometheus        → métricas para Prometheus
```

#### HikariCP — Pool de conexiones a la BD

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

HikariCP mantiene un pool de conexiones TCP pre-establecidas a PostgreSQL. Abrir una
conexión TCP nueva cuesta ~50-100ms. Con el pool, el costo es ~0.1ms (reusa una conexión
existente). Para una API que maneja muchos requests simultáneos, esto marca una diferencia
enorme en latencia.

---

### 5.4 worker-alerts (puerto 8083)

**Propósito:** Enviar notificaciones cuando se detectan amenazas de alta gravedad.

#### Lógica de enrutamiento de notificaciones

```java
@RabbitListener(queues = "netwatch.alerts.notify")
public void procesarAlerta(ThreatMessage amenaza) {
    switch (amenaza.severity()) {
        case "CRITICAL" → {
            emailService.send(amenaza);    // email al equipo
            webhookService.send(amenaza);  // webhook (Slack/Teams/PagerDuty)
        }
        case "HIGH" → webhookService.send(amenaza);  // solo webhook
        default     → log.debug("Amenaza {} registrada, sin notificación", amenaza.threatType());
    }
}
```

#### Canal Email (SMTP)

Usa `JavaMailSender` de Spring. El email tiene formato:
```
Asunto: [NetWatch] MALWARE_C2 - CRITICAL
Cuerpo:
  Tipo de amenaza: MALWARE_C2
  Severidad:       CRITICAL
  IP origen:       185.220.101.45
  IP destino:      192.168.1.10
  Puerto destino:  4444
  Descripción:     Posible comunicación C2 al puerto 4444
  Detectado:       2025-04-11T14:32:10
```

Si `ALERT_EMAIL_ENABLED=false` o el servidor SMTP no está configurado, el servicio
simplemente registra en el log que no envió email, sin fallar.

#### Canal Webhook

POST HTTP al endpoint configurado con payload JSON compatible con Slack/Teams:
```json
{
  "text": "[NetWatch] MALWARE_C2 (CRITICAL) desde 185.220.101.45 → 192.168.1.10:4444"
}
```

---

### 5.5 worker-osint (puerto 8084)

**Propósito:** Enriquecer las amenazas con información de geolocalización de la IP atacante.

#### API ip-api.com

```
GET http://ip-api.com/json/185.220.101.45?fields=status,country,city,lat,lon,as,query

Respuesta:
{
  "status": "success",
  "country": "Germany",
  "city": "Frankfurt am Main",
  "lat": 50.1109,
  "lon": 8.6821,
  "as": "AS9136 Wobserver",
  "query": "185.220.101.45"
}
```

Plan gratuito: 45 requests/minuto. Sin API key. Sin registro necesario.

El campo `as` es el ASN (Autonomous System Number) — identifica el bloque de IPs y la
organización que lo controla. Útil para investigar si la IP pertenece a un proveedor VPN,
un datacenter conocido por actividad maliciosa, o una red residencial.

#### Caché en Valkey

```java
private static final String CACHE_PREFIX = "geoip:";
private static final Duration CACHE_TTL  = Duration.ofHours(1);

// Flujo de lookup:
public GeoIpData lookup(String ip) {
    // 1. ¿Está en caché?
    String cached = redisTemplate.opsForValue().get("geoip:" + ip);
    if (cached != null) return deserialize(cached);  // cache HIT

    // 2. Consultar ip-api.com
    GeoIpData data = restClient.get().uri(IP_API_URL, ip).retrieve().body(GeoIpData.class);

    // 3. Guardar en caché por 1 hora
    redisTemplate.opsForValue().set("geoip:" + ip, serialize(data), Duration.ofHours(1));

    return data;
}
```

Si la misma IP ataca 1000 veces en una hora, ip-api.com solo se consulta UNA vez (el
resto son cache hits en Valkey). Esto respeta el rate-limit de 45 req/min.

#### ¿Por qué Valkey y no Redis?

Redis cambió su licencia en la versión 7.4 a RSALv2/SSPLv1, que NO son licencias
Open Source Initiative (OSI). Esto viola los requisitos del trabajo (100% herramientas OSS).

Valkey es el fork comunitario de Redis creado por la Linux Foundation tras ese cambio.
Es **protocolo 100% compatible** con Redis, lo que significa que todo el código Java que
usa `StringRedisTemplate` funciona sin cambios.

---

### 5.6 worker-scanner (sin puerto HTTP)

**Propósito:** Escanear hosts de la red buscando puertos abiertos y correlacionar los
servicios detectados con CVEs conocidos.

#### Nmap — Detección de servicios

```bash
# Comando generado internamente por NmapScannerService
nmap -sV --open -T4 --host-timeout 120s -p22,80,443,8080 192.168.1.0/24
```

Flags explicados:
- `-sV` → **version scan**: no solo detecta si el puerto está abierto, sino qué versión
  del servicio corre ("OpenSSH 7.4p1 Debian", "Apache httpd 2.4.41")
- `--open` → solo muestra puertos abiertos (filtra los cerrados/filtrados)
- `-T4` → velocidad agresiva (T1=paranoico→lento, T5=insane→ruidoso)
- `--host-timeout 120s` → abandona hosts que no responden en 2 minutos

#### NvdCorrelationService — Correlación con CVEs

Toma la versión detectada ("Apache httpd 2.4.41") y consulta la NVD (National
Vulnerability Database) del NIST para encontrar CVEs conocidos para esa versión:

```
https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch=Apache+2.4.41
```

Si encuentra CVEs, los incluye en el `ScanResult` que se muestra en el frontend.

#### Modo dry-run

Si `SCANNER_DRY_RUN=true` (activo en CI/CD por defecto), el scanner devuelve datos
simulados sin ejecutar Nmap real:

```java
// Simula puertos típicos de un servidor Linux expuesto
List.of(
    "22/tcp   open  ssh      OpenSSH 7.4p1 Debian",
    "80/tcp   open  http     Apache httpd 2.4.41",
    "443/tcp  open  ssl/http Apache httpd 2.4.41",
    "5432/tcp open  postgresql PostgreSQL DB 12.8"
)
```

#### Aviso legal importante

Escanear redes o hosts **sin autorización explícita del propietario** es ilegal en la
mayoría de jurisdicciones (Colombia incluida). El scanner debe usarse exclusivamente en:
- Redes propias o bajo administración
- Laboratorios de prueba con autorización documentada
- Entornos de CTF (Capture The Flag) con reglas claras

---

## 6. Frontend — Vue.js 3

**Stack completo:**
- **Vue 3** (Composition API con `<script setup>`)
- **Vite** (bundler — 10-100x más rápido que Webpack en desarrollo)
- **Pinia** (gestión de estado global, reemplaza Vuex)
- **Axios** (cliente HTTP con interceptores)
- **Chart.js + vue-chartjs** (gráficas)
- **Tailwind CSS** (utilidades CSS, sin CSS personalizado)
- **Vue Router 4** (enrutamiento SPA)

### Vistas del dashboard

| Ruta | Vista | Contenido |
|------|-------|-----------|
| `/login` | LoginView | Formulario de autenticación |
| `/dashboard` | DashboardView | 4 KPIs + gráfica de amenazas + tabla últimos eventos |
| `/events` | EventsView | Tabla paginada con filtros, botón "Resolver" |
| `/alerts` | AlertsView | Alertas por estado, botones Acknowledge/Resolve |
| `/scanner` | SettingsView | Lanzar escaneo Nmap, ver resultados y CVEs |
| `/settings` | SettingsView | Cambiar interfaz de captura activa |
| `/reports` | ReportsView | Estadísticas del período |
| `/topology` | TopologyView | Visualización del grafo de red |

### Gestión de estado con Pinia

```javascript
// stores/auth.js
export const useAuthStore = defineStore('auth', () => {
    const user        = ref(null)
    const accessToken = ref(localStorage.getItem('accessToken'))
    const isLoggedIn  = computed(() => !!accessToken.value)

    async function login(email, password) {
        const resp = await authAPI.login(email, password)
        accessToken.value = resp.data.accessToken
        localStorage.setItem('accessToken',  resp.data.accessToken)
        localStorage.setItem('refreshToken', resp.data.refreshToken)
        router.push('/dashboard')
    }

    async function logout() {
        await authAPI.logout()
        accessToken.value = null
        localStorage.clear()
        router.push('/login')
    }
})

// stores/events.js
export const useEventsStore = defineStore('events', () => {
    const events     = ref([])
    const summary    = ref(null)
    let   pollingId  = null

    function startPolling() {
        fetchEvents()
        fetchSummary()
        pollingId = setInterval(() => fetchEvents(), 30_000)  // cada 30 segundos
    }

    function stopPolling() {
        clearInterval(pollingId)
    }
})
```

### Interceptores Axios — manejo automático de tokens

```javascript
// services/api.js
api.interceptors.response.use(
    response => response,  // éxito: retorna la respuesta
    async error => {
        if (error.response?.status === 401) {
            // Token expirado → intentar renovar
            const refreshToken = localStorage.getItem('refreshToken')
            try {
                const resp = await authAPI.refresh(refreshToken)
                // Guardar nuevos tokens y reintentar la request original
                localStorage.setItem('accessToken', resp.data.accessToken)
                error.config.headers['Authorization'] = 'Bearer ' + resp.data.accessToken
                return api(error.config)
            } catch {
                // Refresh también falló → logout forzado
                localStorage.clear()
                router.push('/login')
            }
        }
        return Promise.reject(error)
    }
)
```

### Nginx — Servidor del frontend en producción

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;

    # SPA routing: rutas como /dashboard no son archivos → devolver index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy al api-gateway para evitar CORS
    location /api {
        proxy_pass http://api-gateway:8080;
    }

    # Cache agresivo para assets estáticos (JS, CSS, imágenes)
    location ~* \.(js|css|png|svg|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

`try_files $uri /index.html` es el truco clave para que Vue Router funcione correctamente.
Si el usuario accede directamente a `/dashboard`, Nginx no tiene ese archivo — sirve
`index.html` y Vue Router toma el control del enrutamiento.

---

## 7. Infraestructura de soporte

### Docker Compose — Gestión de contenedores

El archivo `docker-compose.yml` define 15 servicios con:

**Logging configurado globalmente con YAML anchors:**
```yaml
x-logging: &default-logging
  logging:
    driver: "json-file"
    options:
      max-size: "50m"   # cada archivo de log máximo 50MB
      max-file: "3"     # mantener 3 archivos rotados (total: 150MB/servicio)
```

El anchor `&default-logging` y la referencia `<<: *default-logging` es una característica
de YAML que evita repetir la misma configuración en cada servicio.

**Healthchecks:** Todos los servicios tienen un healthcheck configurado. `depends_on` con
`condition: service_healthy` garantiza que el servicio dependiente solo arranque cuando la
dependencia esté lista, no solo cuando el contenedor haya iniciado.

### docker-compose.prod.yml — Hardening para producción

Se aplica **sobre** el docker-compose.yml base:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Añade:
- **Caddy** como reverse proxy con TLS automático (Let's Encrypt)
- **Límites de memoria y CPU** por servicio (`mem_limit`, `cpus`)
- `read_only: true` en contenedores que no necesitan escribir al filesystem
- `no-new-privileges: true` en el security_opt de todos los contenedores

### Prometheus + Grafana — Observabilidad de métricas

Prometheus recopila métricas de todos los microservicios cada 15 segundos:

```yaml
# monitoring/prometheus/prometheus.yml
scrape_configs:
  - job_name: netwatch-api-gateway
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: /actuator/prometheus

  - job_name: rabbitmq
    static_configs:
      - targets: ['rabbitmq:15692']
```

Spring Boot Actuator expone automáticamente en `/actuator/prometheus`:
- Métricas JVM: `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live`
- Métricas HTTP: `http_server_requests_seconds` (latencia por endpoint)
- Métricas de HikariCP: `hikaricp_connections_active`, `hikaricp_connections_pending`
- Métricas de RabbitMQ: mensajes publicados/consumidos, tamaño de colas

**8 reglas de alerta en Prometheus:**

| Nombre | Condición | Severidad |
|--------|-----------|-----------|
| NetWatchServiceDown | servicio sin responder 2 min | CRITICAL |
| NetWatchHighMemoryUsage | heap JVM > 80% | WARNING |
| NetWatchHighHTTPErrors | > 10% de requests con 5xx | CRITICAL |
| NetWatchHighResponseTime | P95 latencia > 2s | WARNING |
| NetWatchRabbitMQQueueHigh | cola con > 100 mensajes | WARNING |
| NetWatchRabbitMQConsumerAbsent | cola sin consumers | CRITICAL |
| NetWatchDatabaseConnectionPool | pool al 90% | WARNING |
| NetWatchDatabaseConnectionErrors | errores de conexión | CRITICAL |

### Loki + Promtail — Centralización de logs

Promtail (agente) lee los logs de todos los contenedores Docker y los envía a Loki.
Loki los indexa (solo metadatos como timestamps y labels) y los comprime eficientemente.
Desde Grafana se pueden buscar con LogQL:

```logql
{job="netwatch-api-gateway"} |= "ERROR"           # todos los errores del gateway
{container="netwatch-worker-analysis"} | json      # logs del motor de análisis como JSON
{job=~"netwatch.*"} |= "Credenciales inválidas"   # todos los intentos de login fallidos
```

### Falco — Seguridad en runtime

Falco usa eBPF para monitorear las syscalls del kernel. Detecta comportamiento anómalo
**en tiempo de ejecución**, no en el código estático:

```yaml
# monitoring/falco/falco-rules.yml

# Detecta si algo que no es worker-capture hace captura de red
- rule: NetWatch captura no autorizada
  condition: >
    spawned_process and proc.name in (tcpdump, tshark, scapy)
    and not container.name = "netwatch-worker-capture"
  priority: CRITICAL

# Detecta escalada de privilegios en cualquier contenedor de NetWatch
- rule: NetWatch escalada de privilegios
  condition: >
    container and container.name startswith "netwatch"
    and proc.name in (su, sudo)
  priority: WARNING
```

### Caddy — Reverse proxy con TLS automático

Caddy es el proxy inverso de producción. Su característica clave es obtener y renovar
certificados TLS de Let's Encrypt **automáticamente**, sin configuración manual:

```caddyfile
# Caddyfile
netwatch.tudominio.com {
    reverse_proxy frontend:80
}

api.netwatch.tudominio.com {
    reverse_proxy api-gateway:8080
}
```

Con solo esto, Caddy:
1. Detecta que no tiene certificado para `netwatch.tudominio.com`
2. Genera un CSR y lo envía a Let's Encrypt vía ACME
3. Let's Encrypt verifica que el dominio apunte a ese servidor
4. Emite el certificado → HTTPS habilitado automáticamente
5. Renueva el certificado antes de que expire (90 días)

---

## 8. El pipeline DevSecOps — CI/CD

### GitHub Actions — ci.yml

Se ejecuta en cada `push` a `main`/`develop` o en cada Pull Request.

```
Push to main
    │
    ├────────────────────────────────────────┐
    ▼                                        ▼
secrets-scan (Gitleaks)              iac-scan (Checkov)
    │                                    (Dockerfiles,
    │                                     docker-compose.yml,
    │                                     docker-compose.prod.yml)
    │
    ├────────────────────┐
    ▼                    ▼
sast                    sca
(SpotBugs + Semgrep)   (OWASP Dep-Check + CycloneDX SBOM)
    │                    │
    └─────────┬──────────┘
              ▼
         build-and-scan
    (6 servicios Java en paralelo — GitHub matrix)
    (Docker build + Trivy por cada imagen)
              │
              ▼
         unit-tests
    (mvn verify → JUnit 5 + JaCoCo ≥ 70%)
    (Con PostgreSQL TimescaleDB + RabbitMQ como servicios)
              │
              ▼
            dast
    (Levanta el stack con docker compose)
    (OWASP ZAP Baseline Scan contra http://localhost:8080)
    (Baja el stack)
```

### Herramientas por fase — explicación detallada

#### Gitleaks — Secretos en el código

Escanea el historial Git completo (`fetch-depth: 0`) buscando patrones de:
- API keys (AWS, GCP, Azure, GitHub)
- Tokens JWT hardcodeados
- Contraseñas en archivos de configuración
- Claves privadas SSH/RSA

El archivo `.gitleaks.toml` define supresiones para falsos positivos (como contraseñas
de ejemplo en la documentación).

#### SpotBugs + FindSecBugs — SAST de bytecode

Analiza el bytecode Java compilado (archivos `.class`) buscando:
- SQL Injection: concatenación de strings en queries SQL
- XSS: salida de datos sin sanitizar en respuestas HTTP
- Deserialización insegura
- Criptografía débil (DES, MD5 para contraseñas)
- Uso de `Random` en vez de `SecureRandom`

#### Semgrep OSS — SAST semántico

Analiza el código fuente (no el bytecode) con reglas:
- `p/java` → patrones Java genéricos
- `p/owasp-top-ten` → los 10 riesgos más críticos según OWASP
- `p/spring-boot` → vulnerabilidades específicas de Spring

Se usa SIN SEMGREP_APP_TOKEN (versión gratuita con reglas públicas), sin enviar código
a servidores de Semgrep.

#### OWASP Dependency-Check — SCA

Descarga la National Vulnerability Database (NVD) del NIST y cruza todas las dependencias
del `pom.xml` contra esa base de datos. Si encuentra un CVE con CVSS ≥ 9.0 (CRITICAL),
el build falla.

El archivo `dependency-check-suppressions.xml` documenta los CVEs suprimidos:
- Falsos positivos (CVE de otra librería con nombre similar)
- CVEs sin parche disponible + justificación de por qué el vector de ataque no aplica

#### CycloneDX — SBOM (Software Bill of Materials)

Genera un inventario completo de todas las dependencias en formato estándar:
- `bom.xml` (formato XML)
- `bom.json` (formato JSON)

El SBOM incluye para cada dependencia: nombre, versión, licencia, y CVEs conocidos.
Obligatorio para cumplimiento normativo (NIST SSDF, Executive Order 14028 de EE.UU.).

#### Trivy — Escaneo de imágenes Docker

Escanea la imagen Docker construida buscando:
- CVEs en la imagen base (`eclipse-temurin:21-jre-alpine`)
- CVEs en las dependencias empaquetadas en el JAR
- Configuraciones inseguras en el Dockerfile

Si encuentra un CVE CRITICAL, el build falla (con `exit-code: '1'`). El resultado se
sube a GitHub Code Scanning en formato SARIF.

#### JaCoCo — Cobertura de tests

```xml
<!-- pom.xml — regla de cobertura mínima -->
<rule>
    <element>BUNDLE</element>
    <limits>
        <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.70</minimum>  <!-- 70% de líneas deben estar cubiertas -->
        </limit>
    </limits>
</rule>
```

Si `mvn verify` encuentra que la cobertura es < 70%, falla con:
```
Coverage check failed for project netwatch-api-gateway...
Lines covered ratio is 0.65, expected minimum is 0.70
```

#### OWASP ZAP Baseline — DAST

ZAP (Zed Attack Proxy) hace pruebas de seguridad sobre la aplicación **corriendo**:
- Verifica headers de seguridad HTTP (CSP, X-Frame-Options, HSTS)
- Busca endpoints vulnerables a XSS
- Detecta información sensible en respuestas
- Verifica configuración de sesiones y cookies

Es la única herramienta que prueba la aplicación en ejecución real (las otras prueban
código estático o dependencias).

#### Checkov — IaC Scan

Analiza la infraestructura como código buscando configuraciones inseguras:

**En Dockerfiles:**
- `USER root` → el proceso corre como root (mal)
- Sin `HEALTHCHECK` definido
- Imagen base sin versión fija (`:latest` es impredecible)
- `ADD` en vez de `COPY` (ADD puede descomprimir, más superficie de ataque)

**En docker-compose.yml:**
- `privileged: true` → contenedor con todos los privilegios del host
- Sin límites de memoria (`mem_limit`)
- Puertos sensibles expuestos innecesariamente

### GitHub Actions — deploy.yml

Se activa solo con tags semánticos (`v1.0.0`, `v2.3.1`, etc.):

```
git tag v1.0.0
git push origin v1.0.0
         │
         ▼
  publish (matrix × 6 servicios Java + 1 frontend, todos en paralelo)
         │
         ▼ para cada servicio:
    1. mvn package -DskipTests (compilar JAR)
    2. docker/login-action → autenticar en Docker Hub
    3. docker/metadata-action → calcular tags (v1.0.0, v1.0, latest)
    4. docker/build-push-action → construir y publicar imagen
```

Las imágenes publicadas en Docker Hub tienen tres tags:
- `usuario/netwatch-api-gateway:1.0.0` (versión exacta)
- `usuario/netwatch-api-gateway:1.0` (minor)
- `usuario/netwatch-api-gateway:latest` (siempre la más reciente)

---

## 9. Seguridad — modelo STRIDE

STRIDE es un modelo de análisis de amenazas desarrollado por Microsoft. Cada letra
corresponde a una categoría de amenaza. Las contramedidas de NetWatch responden
sistemáticamente a cada categoría:

### S — Spoofing (Suplantación de identidad)

**Amenaza:** Un atacante se hace pasar por un usuario legítimo del sistema.

**Contramedidas en NetWatch:**
- JWT con HMAC-SHA256 firmado con clave secreta de 64+ caracteres
- Refresh tokens de un solo uso (se invalidan al usarse)
- BCrypt strength 12 para contraseñas (hace inviable el cracking por fuerza bruta)
- Mismo mensaje de error para "email no existe" y "contraseña incorrecta" (evita
  enumeración de usuarios — un atacante no sabe si el email existe o no)

### T — Tampering (Manipulación de datos)

**Amenaza:** Un atacante modifica datos en tránsito o en reposo.

**Contramedidas:**
- HTTPS obligatorio en producción (Caddy con TLS)
- Validación de entrada con `@Valid` + Bean Validation en todos los DTOs
- `@NotBlank`, `@Email`, `@Size` en campos de entrada
- Base de datos no accesible directamente desde internet (solo api-gateway tiene acceso)

### R — Repudiation (Repudio)

**Amenaza:** Un usuario realiza una acción maliciosa y luego niega haberla hecho.

**Contramedidas:**
- Logs inmutables en Loki (centralizados, no modificables por los contenedores)
- Tabla `alert_logs` registra cada notificación enviada con timestamp
- Spring Security registra todos los intentos de login fallidos
- El userId está en el JWT → cada acción queda trazada al usuario

### I — Information Disclosure (Divulgación de información)

**Amenaza:** Información sensible se expone a usuarios no autorizados.

**Contramedidas:**
- RBAC: los endpoints filtran campos según el rol del usuario
- Los logs no incluyen contraseñas ni tokens completos
- Las respuestas de error no revelan detalles internos del sistema
- Variables de entorno para todas las credenciales (nunca hardcodeadas en el código)
- `.gitignore` excluye `.env`, `*.pem`, `*.key`, `*.jks`

### D — Denial of Service (Denegación de servicio)

**Amenaza:** Un atacante agota los recursos del sistema para que no pueda atender
peticiones legítimas.

**Contramedidas:**
- RabbitMQ con `prefetchCount=20` limita cuántos mensajes procesa cada hilo
- HikariCP con pool limitado (`maximum-pool-size=20`) previene agotamiento de BD
- Alertas Prometheus si una cola acumula > 100 mensajes (`NetWatchRabbitMQQueueHigh`)
- Límites de memoria y CPU por contenedor en `docker-compose.prod.yml`
- worker-capture con `network_mode: host` + filtro BPF puede limitar el tráfico capturado

### E — Elevation of Privilege (Elevación de privilegios)

**Amenaza:** Un usuario obtiene más permisos de los que debería tener.

**Contramedidas:**
- RBAC con tres roles (VIEWER, ANALYST, ADMIN) estrictamente separados
- `@PreAuthorize` en cada endpoint sensible
- Los contenedores corren como usuario no-root (`adduser -S netwatch`)
- `no-new-privileges: true` en docker-compose.prod.yml
- Falco detecta intentos de `sudo`/`su` en contenedores

---

## 10. Lo que falta en el proyecto

### Pendientes de alta prioridad

| # | Elemento | Impacto en nota |
|---|---------|----------------|
| 1 | `docs/architecture/component-diagram.puml` | 20% de la nota |
| 2 | `docs/architecture/deployment-diagram.puml` | 20% de la nota |
| 3 | `docs/architecture/sequence-auth.puml` | 20% de la nota |
| 4 | `docs/architecture/use-cases.puml` | 20% de la nota |
| 5 | `docs/architecture/threat-model.json` | 20% de la nota |
| 6 | `infrastructure/terraform/` (main.tf, variables.tf, outputs.tf) | Fase DEPLOY |
| 7 | `infrastructure/ansible/` (site.yml + roles/) | Fase DEPLOY |
| 8 | `infrastructure/k8s/` (namespace.yaml, deployments/, services/) | Checkov IaC |
| 9 | Tests unitarios para `netwatch-worker-scanner` | Cobertura JaCoCo |

### Los 5 diagramas PlantUML requeridos

```
docs/architecture/component-diagram.puml  → visión general de todos los microservicios
docs/architecture/deployment-diagram.puml → contenedores, redes, volúmenes Docker
docs/architecture/sequence-auth.puml      → flujo completo de autenticación JWT
docs/architecture/use-cases.puml          → actores: Admin, Analista, Viewer
docs/architecture/threat-model.json       → OWASP Threat Dragon DFD nivel 0 y 1
```

---

## 11. Cómo explicar el proyecto en 5 minutos

### Narrativa sugerida para la sustentación

> "NetWatch monitorea el tráfico de una red empresarial en tiempo real. El sistema
> tiene siete microservicios Java que se comunican de forma asíncrona a través de
> RabbitMQ.
>
> El flujo empieza con el worker-capture, que captura paquetes de red usando la
> biblioteca Pcap4J. Cada paquete se envía a una cola de RabbitMQ y el worker-analysis
> lo analiza con seis reglas basadas en el modelo STRIDE: detecta SYN floods, ataques
> de fuerza bruta, comunicaciones con servidores de malware, túneles DNS y exfiltración
> de datos.
>
> Cuando detecta una amenaza CRITICAL, el sistema simultáneamente notifica al equipo
> por email y webhook, y geolocaliza la IP atacante consultando ip-api.com con caché en
> Valkey para no superar el rate-limit. Todo queda persistido en PostgreSQL con
> TimescaleDB, que es ideal para datos de series temporales como el tráfico de red.
>
> El analista puede ver todo en tiempo real en un dashboard Vue.js, marcar alertas como
> resueltas, y lanzar escaneos de vulnerabilidades Nmap que correlacionan los servicios
> detectados con CVEs de la base de datos del NIST.
>
> Lo que hace especial al proyecto desde el punto de vista DevSecOps es que la seguridad
> está en cada fase del pipeline de GitHub Actions: Gitleaks detecta credenciales en el
> código, SpotBugs y Semgrep analizan el código estático, OWASP Dependency-Check busca
> CVEs en las dependencias, Trivy escanea las imágenes Docker, JaCoCo garantiza 70%
> de cobertura de tests, OWASP ZAP prueba la aplicación corriendo, y Checkov valida la
> infraestructura como código. En producción, Prometheus, Grafana, Loki y Falco
> garantizan la observabilidad y seguridad en runtime."

### Preguntas frecuentes en sustentaciones y sus respuestas

**P: ¿Por qué RabbitMQ y no llamadas HTTP directas entre servicios?**
> Porque RabbitMQ desacopla los servicios temporalmente. Si worker-analysis está
> sobrecargado, los paquetes se acumulan en la cola y se procesan cuando haya
> capacidad. Con HTTP directo, si el receptor está lento, el emisor también se bloquea
> y los paquetes se perderían.

**P: ¿Por qué TimescaleDB y no PostgreSQL normal?**
> El tráfico de red es datos de series temporales: millones de registros ordenados
> por tiempo. TimescaleDB particiona automáticamente la tabla por días u horas, así
> que una query "eventos de las últimas 2 horas" solo lee las particiones de ese
> período en lugar de escanear toda la tabla.

**P: ¿Por qué dos tokens (access + refresh)?**
> El access token dura 30 minutos y se envía en cada request — si se intercepta,
> tiene vida corta. El refresh token dura 7 días pero solo se usa una vez para
> renovar el access token. Al rotar el refresh token, si alguien lo robó y lo usa,
> el sistema lo detecta (ya está en la blacklist) y puede invalidar la sesión.

**P: ¿Cuál es la diferencia entre SAST y DAST?**
> SAST (Static Application Security Testing) analiza el código sin ejecutarlo:
> SpotBugs, Semgrep. DAST (Dynamic Application Security Testing) prueba la
> aplicación mientras corre: OWASP ZAP. SAST detecta problemas en el código fuente;
> DAST detecta cómo se comporta la aplicación real ante ataques.

**P: ¿Por qué Valkey en vez de Redis?**
> Redis cambió su licencia en la versión 7.4 a RSALv2/SSPLv1, que no son licencias
> aprobadas por la Open Source Initiative. El requisito del trabajo es usar
> herramientas 100% open source. Valkey es el fork comunitario creado por la Linux
> Foundation con licencia BSD, protocolo 100% compatible con Redis.

---

## 12. Puertos de todos los servicios

| Servicio | Puerto | Descripción |
|---------|--------|-------------|
| API Gateway | 8080 | REST API principal |
| Worker Analysis | 8081 | Motor de detección (solo interno) |
| Worker Capture | 8082 | Captura de red (network_mode: host) |
| Worker Alerts | 8083 | Notificaciones (solo interno) |
| Worker OSINT | 8084 | Geolocalización (solo interno) |
| Frontend | 3000 → 80 | Vue.js via Nginx |
| PostgreSQL | 5432 | Base de datos |
| RabbitMQ AMQP | 5672 | Protocolo de mensajería |
| RabbitMQ UI | 15672 | Interfaz web de administración |
| Valkey | 6379 | Cache Redis-compatible |
| Prometheus | 9090 | Recolección de métricas |
| Grafana | 3001 → 3000 | Dashboards de métricas |
| Loki | 3100 | Almacenamiento de logs |
| Promtail | 9080 | Agente de logs |

---

## 13. Credenciales de prueba

### Acceso al dashboard

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin@netwatch.local` | `NetWatch2024!` | ADMIN |
| `analista@netwatch.local` | `NetWatch2024!` | ANALYST |

### Acceso a servicios de infraestructura

| Servicio | URL | Usuario | Contraseña |
|---------|-----|---------|-----------|
| RabbitMQ UI | http://localhost:15672 | Del `.env` (RABBITMQ_USER) | Del `.env` |
| Grafana | http://localhost:3001 | admin | Del `.env` (GRAFANA_PASSWORD) |
| Prometheus | http://localhost:9090 | — | — |

### Cómo probar la API con curl

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@netwatch.local","password":"NetWatch2024!"}' \
  | jq -r '.accessToken')

# 2. Obtener eventos
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/events?size=5

# 3. Obtener resumen del dashboard
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/events/stats/summary

# 4. Ver alertas abiertas
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/alerts?status=OPEN"
```

---

*Documento generado el 2026-04-11. Para actualizaciones del proyecto, revisar el
repositorio en https://github.com/AlexGarzonSoto/MonitoreoRedInfra*
