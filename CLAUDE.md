# NetWatch — CLAUDE.md
# Contexto completo del proyecto para Claude Code
# ESTE ARCHIVO ESTÁ EN .gitignore — NO SE SUBE AL REPOSITORIO

---

## 1. Descripción del proyecto

**NetWatch** es un sistema de monitoreo y análisis de amenazas en infraestructura de red.
Es el trabajo final de la Especialización en Ciberseguridad con énfasis en DevSecOps.
El objetivo es construir un pipeline DevSecOps de ciclo completo para una aplicación contenerizada con arquitectura de microservicios.

- **Repositorio:** https://github.com/AlexGarzonSoto/MonitoreoRedInfra
- **Ruta local:** /home/Alex/Documentos/especializacion/proyectoFinal
- **Licencia:** Apache 2.0
- **OS de desarrollo:** MX Linux (basado en Debian 12)
- **Herramientas:** 100% open source con licencias OSI aprobadas

---

## 2. Stack tecnológico completo

| Capa | Tecnología | Versión | Licencia |
|------|-----------|---------|---------|
| Lenguaje backend | Java OpenJDK | 21 LTS | GPL v2 + Classpath Exception |
| Framework backend | Spring Boot | 3.2.5 | Apache 2.0 |
| Build | Maven | 3.9+ | Apache 2.0 |
| Broker mensajes | RabbitMQ | 3.12-management-alpine | Mozilla PL 2.0 |
| Base de datos | PostgreSQL + TimescaleDB | 15 | PostgreSQL License |
| Caché | Valkey (fork OSS de Redis) | 7.2-alpine | BSD 3-Clause |
| Captura de red | Pcap4J | 2.0.0-alpha.6 | MIT |
| JWT | jjwt | 0.12.5 | Apache 2.0 |
| Frontend | Vue.js 3 + Vite | 3.x / 5.x | MIT |
| Router frontend | Vue Router | 4 | MIT |
| Estado global | Pinia | 2 | MIT |
| HTTP cliente | Axios | 1.x | MIT |
| Gráficas | Chart.js + vue-chartjs | 4.x | MIT |
| Servidor frontend | Nginx | alpine | BSD |
| Contenedores | Docker + Docker Compose v2 | 26.x | Apache 2.0 |
| Orquestación | K3s / Docker Swarm | latest | Apache 2.0 |
| IaC | OpenTofu (fork OSS de Terraform) | 1.7 | Mozilla PL 2.0 |
| Config mgmt | Ansible Core | latest | GPL v3 |
| CI/CD | GitHub Actions | - | Gratis repos públicos |
| SAST | SpotBugs + Find Security Bugs | 4.8.3.1 / 1.13.0 | LGPL |
| SAST semántico | Semgrep OSS sin token | latest | LGPL |
| SCA | OWASP Dependency-Check | 9.0.9 | Apache 2.0 |
| Escaneo imágenes | Trivy | latest | Apache 2.0 |
| DAST | OWASP ZAP Baseline | latest | Apache 2.0 |
| Secretos en código | Gitleaks | 8.18.2 | MIT |
| IaC scan | Checkov | latest | Apache 2.0 |
| Modelado amenazas | OWASP Threat Dragon | latest | Apache 2.0 |
| Métricas | Prometheus | latest | Apache 2.0 |
| Dashboards | Grafana OSS | latest | AGPL v3 |
| Logs | Loki + Promtail | latest | AGPL v3 |
| Runtime security | Falco | latest | Apache 2.0 |
| Diagramas UML | PlantUML | latest | MIT |

**Decisiones de licencia importantes:**
- Valkey reemplaza Redis — Redis v7.4+ cambió a RSALv2/SSPLv1 (no OSI)
- OpenTofu reemplaza Terraform — HashiCorp v1.6+ cambió a BSL 1.1 (no OSI)
- ip-api.com reemplaza GeoLite2 — no requiere registro ni base de datos local
- Semgrep se usa SIN SEMGREP_APP_TOKEN — solo reglas públicas OSS gratuitas

---

## 3. Arquitectura de microservicios

### Estructura de directorios completa esperada

```
proyectoFinal/
├── pom.xml                                    ← POM padre multi-módulo
├── .gitignore                                 ← incluye CLAUDE.md, .env, target/
├── .env.example                               ← plantilla variables sin valores
├── docker-compose.yml                         ← 13 servicios
├── README.md
│
├── netwatch-api-gateway/                      ← Puerto 8080
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/netwatch/gateway/
│       │   ├── NetwatchGatewayApplication.java
│       │   ├── config/
│       │   │   ├── RabbitMQConfig.java
│       │   │   └── SecurityConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── EventController.java
│       │   │   └── AlertController.java
│       │   ├── dto/
│       │   │   ├── LoginRequest.java
│       │   │   ├── LoginResponse.java
│       │   │   └── EventDTO.java
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   ├── NetworkEvent.java
│       │   │   └── Alert.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── EventRepository.java
│       │   │   └── AlertRepository.java
│       │   ├── security/
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   └── UserDetailsServiceImpl.java
│       │   └── service/
│       │       ├── AuthService.java
│       │       └── EventService.java
│       ├── main/resources/application.yml
│       └── test/java/com/netwatch/gateway/
│           ├── AuthServiceTest.java
│           └── EventControllerTest.java
│
├── netwatch-worker-capture/                   ← Puerto 8082
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/netwatch/capture/
│       ├── NetwatchCaptureApplication.java
│       ├── config/RabbitMQConfig.java
│       ├── model/PacketMessage.java
│       ├── producer/PacketProducer.java
│       └── service/NetworkCaptureService.java
│
├── netwatch-worker-analysis/                  ← Puerto 8081
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/netwatch/analysis/
│       │   ├── NetwatchAnalysisApplication.java
│       │   ├── config/RabbitMQConfig.java
│       │   ├── consumer/PacketConsumer.java
│       │   ├── engine/ThreatDetectionEngine.java
│       │   └── model/
│       │       ├── PacketMessage.java
│       │       └── ThreatEvent.java
│       ├── main/resources/application.yml
│       └── test/java/com/netwatch/analysis/
│           └── ThreatDetectionEngineTest.java
│
├── netwatch-worker-alerts/                    ← Puerto 8083
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/netwatch/alerts/
│       ├── NetwatchAlertsApplication.java
│       ├── config/RabbitMQConfig.java
│       ├── consumer/AlertConsumer.java
│       ├── model/ThreatEvent.java
│       └── service/NotificationService.java
│
├── netwatch-worker-osint/                     ← Puerto 8084
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/netwatch/osint/
│       ├── NetwatchOsintApplication.java
│       ├── config/RabbitMQConfig.java
│       ├── consumer/OsintConsumer.java
│       ├── model/
│       │   ├── ThreatEvent.java
│       │   └── GeoIpResult.java
│       └── service/GeoIpService.java
│
├── frontend/                                  ← Puerto 3000
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js
│       ├── stores/
│       │   ├── auth.js
│       │   └── events.js
│       ├── services/api.js
│       ├── views/
│       │   ├── LoginView.vue
│       │   ├── DashboardView.vue
│       │   ├── EventsView.vue
│       │   └── AlertsView.vue
│       └── components/
│           ├── NavBar.vue
│           ├── StatCard.vue
│           ├── EventTable.vue
│           ├── ThreatChart.vue
│           └── AlertPanel.vue
│
├── infrastructure/
│   ├── sql/init.sql
│   ├── terraform/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── ansible/
│   │   ├── site.yml
│   │   └── roles/
│   └── k8s/
│       ├── namespace.yaml
│       ├── deployments/
│       └── services/
│
├── monitoring/
│   ├── prometheus/prometheus.yml
│   ├── grafana/dashboards/netwatch-dashboard.json
│   ├── loki/loki-config.yml
│   ├── loki/promtail-config.yml
│   └── falco/falco-rules.yml
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── deploy.yml
│
├── .zap/rules.tsv
│
└── docs/
    ├── architecture/
    │   ├── component-diagram.puml
    │   ├── deployment-diagram.puml
    │   ├── sequence-auth.puml
    │   ├── use-cases.puml
    │   └── threat-model.json
    ├── development-manual.md
    ├── deployment-manual.md
    ├── security-manual.md
    └── user-manual.md
```

---

## 4. Flujo de mensajes RabbitMQ

```
Exchange: netwatch.direct (DirectExchange, durable=true)

Worker Captura   --[packets.raw]------> netwatch.packets.raw   --> Worker Analysis
Worker Analysis  --[threats.detected]-> netwatch.threats.detected --> API Gateway (persiste)
Worker Analysis  --[alerts.notify]----> netwatch.alerts.notify  --> Worker Alerts
Worker Analysis  --[osint.enrich]-----> netwatch.osint.enrich   --> Worker OSINT
Worker OSINT     --[threats.detected]-> netwatch.threats.detected --> API Gateway (persiste enriquecido)
```

### Constantes RabbitMQ (iguales en todos los servicios)

```java
EXCHANGE    = "netwatch.direct"
Q_PACKETS   = "netwatch.packets.raw"
Q_THREATS   = "netwatch.threats.detected"
Q_ALERTS    = "netwatch.alerts.notify"
Q_OSINT     = "netwatch.osint.enrich"
RK_PACKETS  = "packets.raw"
RK_THREATS  = "threats.detected"
RK_ALERTS   = "alerts.notify"
RK_OSINT    = "osint.enrich"
```

### Configuración de rendimiento del consumer (SimpleRabbitListenerContainerFactory)

```java
setConcurrentConsumers(3)       // mínimo 3 hilos
setMaxConcurrentConsumers(10)   // hasta 10 bajo carga
setPrefetchCount(20)            // 20 mensajes por hilo
setDefaultRequeueRejected(false)
setMessageConverter(new Jackson2JsonMessageConverter())
```

---

## 5. Estado actual — archivos existentes y pendientes

### ✅ COMPLETADOS

#### netwatch-api-gateway — paquete com.netwatch.gateway

**NetwatchGatewayApplication.java**
```java
@SpringBootApplication @EnableAsync @EnableScheduling
public class NetwatchGatewayApplication { main() }
```

**config/RabbitMQConfig.java**
- DirectExchange "netwatch.direct" durable
- 4 queues durable: packets.raw, threats.detected, alerts.notify, osint.enrich
- 4 bindings con sus routing keys
- Jackson2JsonMessageConverter
- RabbitTemplate con converter
- SimpleRabbitListenerContainerFactory con concurrencia 3-10, prefetch 20

**config/SecurityConfig.java**
- @EnableWebSecurity @EnableMethodSecurity
- SessionCreationPolicy.STATELESS (sin sesiones HTTP)
- CSRF desactivado
- Rutas públicas: /api/v1/auth/**, /actuator/health, /actuator/prometheus
- GET /api/v1/events/**, /api/v1/alerts/** → VIEWER, ANALYST, ADMIN
- PATCH /api/v1/events/**, /api/v1/alerts/** → ANALYST, ADMIN
- /api/v1/users/** → solo ADMIN
- JwtAuthFilter añadido antes de UsernamePasswordAuthenticationFilter
- BCryptPasswordEncoder strength 12
- DaoAuthenticationProvider con UserDetailsServiceImpl

**model/User.java**
```java
@Entity @Table(name="users")
UUID id, String email (unique), String passwordHash,
Role role (enum: ADMIN/ANALYST/VIEWER), boolean active, LocalDateTime createdAt
```

**model/NetworkEvent.java**
```java
@Entity @Table(name="network_events")
UUID id, String srcIp, String dstIp, Integer srcPort, Integer dstPort,
String protocol, String flags, Integer packetLength, Integer ttl,
ThreatType threatType (enum: PORT_SCAN/BRUTE_FORCE/SYN_FLOOD/DNS_TUNNELING/NORMAL),
Severity severity (enum: INFO/LOW/MEDIUM/HIGH/CRITICAL),
String description, String country, String city,
Double latitude, Double longitude, String asn, Integer abuseScore,
boolean resolved, LocalDateTime timestamp
Índices: idx_src_ip, idx_severity, idx_threat
```

**model/Alert.java**
```java
@Entity @Table(name="alerts")
UUID id, @ManyToOne NetworkEvent event (LAZY),
String title, String details,
AlertStatus status (enum: OPEN/ACKNOWLEDGED/RESOLVED/FALSE_POSITIVE),
boolean notificationSent, LocalDateTime createdAt
```

**dto/LoginRequest.java** — record con @Email @NotBlank email, @NotBlank @Size(min=6) password

**dto/LoginResponse.java** — record con accessToken, refreshToken, tokenType, expiresIn(long), role
Constructor simplificado: LoginResponse(accessToken, refreshToken, role) → tokenType="Bearer", expiresIn=1800L

**dto/EventDTO.java** — record con todos los campos de NetworkEvent como Strings
Método estático from(NetworkEvent e) que convierte la entidad al DTO

**repository/UserRepository.java**
```java
Optional<User> findByEmailAndActiveTrue(String email)
Optional<User> findByEmail(String email)
boolean existsByEmail(String email)
```

**repository/EventRepository.java**
```java
Page<NetworkEvent> findAllByOrderByTimestampDesc(Pageable pageable)
long countBySeverity(NetworkEvent.Severity severity)
long countByResolved(boolean resolved)
long countByThreatType(NetworkEvent.ThreatType threatType)
@Query("SELECT e FROM NetworkEvent e WHERE e.timestamp >= :since ORDER BY e.timestamp DESC")
List<NetworkEvent> findRecentEvents(@Param("since") LocalDateTime since)
```

**repository/AlertRepository.java**
```java
Page<Alert> findByStatusOrderByCreatedAtDesc(Alert.AlertStatus status, Pageable pageable)
long countByStatus(Alert.AlertStatus status)
```

**security/JwtTokenProvider.java**
- Constructor: @Value secret, refreshSecret, accessMs(1800000), refreshMs(604800000)
- Keys.hmacShaKeyFor() con StandardCharsets.UTF_8
- generateAccessToken(userId, role): subject=userId, claim type="access", claim role
- generateRefreshToken(userId): subject=userId, claim type="refresh"
- parseAccessToken(token): Jwts.parser().verifyWith(accessKey)
- parseRefreshToken(token): Jwts.parser().verifyWith(refreshKey)
- validateAccessToken(token): try/catch JwtException, verifica claim type=="access"
- validateRefreshToken(token): verifica claim type=="refresh"
- getUserIdFromToken(String), getRoleFromToken(String), getUserIdFromRefreshToken(String)

**security/JwtAuthFilter.java**
- extends OncePerRequestFilter
- Extrae "Bearer " del header Authorization (substring(7))
- Si token válido: crea UsernamePasswordAuthenticationToken con ROLE_{role}
- SecurityContextHolder.getContext().setAuthentication(auth)
- Siempre llama filterChain.doFilter()

**security/UserDetailsServiceImpl.java**
- implements UserDetailsService
- loadUserByUsername(email): findByEmailAndActiveTrue → User.withUsername().password().authorities("ROLE_"+role)

**service/AuthService.java**
- login(LoginRequest): findByEmailAndActiveTrue → BCrypt.matches → generateAccessToken + generateRefreshToken
- refresh(String refreshToken): validateRefreshToken → getUserIdFromRefreshToken → findById → generateAccessToken + generateRefreshToken
- logout(String): log INFO (blacklist en Redis en producción)
- Mismo mensaje de error para email no encontrado y password incorrecta (evita enumeración)

**service/EventService.java**
- @Transactional(readOnly=true) findAll(Pageable): Page<EventDTO>
- @Transactional(readOnly=true) findById(UUID): EventDTO
- @Transactional resolve(UUID): setResolved(true), save, return EventDTO
- @Transactional(readOnly=true) getSummary(): Map con totalEvents, criticalCount, highCount, unresolvedCount, recentEvents(última 1 hora)

**controller/AuthController.java**
```
POST /api/v1/auth/login         → @Valid @RequestBody LoginRequest → LoginResponse
POST /api/v1/auth/refresh       → @RequestHeader("X-Refresh-Token") → LoginResponse
POST /api/v1/auth/logout        → @RequestHeader("Authorization") → 204 No Content
```

**controller/EventController.java**
```
GET   /api/v1/events            → @PageableDefault(size=50, sort="timestamp") → Page<EventDTO>
GET   /api/v1/events/{id}       → EventDTO
PATCH /api/v1/events/{id}/resolve → EventDTO
GET   /api/v1/events/stats/summary → Map<String, Object>
Todos con @PreAuthorize apropiado
```

**controller/AlertController.java**
```
GET   /api/v1/alerts            → @RequestParam status="OPEN" → Page<Alert>
PATCH /api/v1/alerts/{id}/acknowledge → Alert
PATCH /api/v1/alerts/{id}/resolve     → Alert
```

**src/main/resources/application.yml**
```yaml
spring.application.name: netwatch-api-gateway
spring.datasource.url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/netwatch}
spring.datasource.username: ${SPRING_DATASOURCE_USERNAME:netwatch}
spring.datasource.password: ${SPRING_DATASOURCE_PASSWORD:netwatch}
spring.datasource.hikari.maximum-pool-size: 10
spring.jpa.hibernate.ddl-auto: validate
spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
spring.rabbitmq.host: ${SPRING_RABBITMQ_HOST:localhost}
spring.rabbitmq.port: 5672
spring.rabbitmq.username: ${SPRING_RABBITMQ_USERNAME:guest}
spring.rabbitmq.password: ${SPRING_RABBITMQ_PASSWORD:guest}
netwatch.jwt.secret: ${NETWATCH_JWT_SECRET:dev-secret-minimo-64-caracteres-para-hmac-sha256}
netwatch.jwt.refresh-secret: ${NETWATCH_JWT_REFRESH_SECRET:dev-refresh-minimo-64-caracteres}
netwatch.jwt.access-expiration-ms: 1800000
netwatch.jwt.refresh-expiration-ms: 604800000
server.port: 8080
management.endpoints.web.exposure.include: health,prometheus,info
management.endpoint.health.show-details: always
logging.level.com.netwatch: INFO
logging.level.org.springframework.security: WARN
```

---

#### netwatch-worker-analysis — paquete com.netwatch.analysis

**NetwatchAnalysisApplication.java**
```java
@SpringBootApplication @EnableScheduling
```

**model/PacketMessage.java** — @Data @NoArgsConstructor @AllArgsConstructor @Builder
```java
String srcIp, dstIp, protocol, flags
Integer srcPort, dstPort, packetLength, ttl
LocalDateTime timestamp
```

**model/ThreatEvent.java** — @Data @NoArgsConstructor @AllArgsConstructor @Builder
```java
String threatType, severity, srcIp, dstIp, description
Integer dstPort
LocalDateTime detectedAt
```

**engine/ThreatDetectionEngine.java** — @Component @Slf4j
```java
// Umbrales
PORT_SCAN_THRESHOLD   = 20
BRUTE_FORCE_THRESHOLD = 10
SYN_FLOOD_THRESHOLD   = 100
SENSITIVE_PORTS       = {22, 21, 23, 3389, 5432, 3306, 27017, 6379}

// Trackers thread-safe
Map<String, Set<Integer>>  portScanTracker   = new ConcurrentHashMap<>()
Map<String, AtomicInteger> bruteForceTracker = new ConcurrentHashMap<>()
Map<String, AtomicInteger> synFloodTracker   = new ConcurrentHashMap<>()

// Métodos públicos
List<ThreatEvent> analyze(PacketMessage p)    // llama los 4 detectores
void resetCounters()                           // limpia los 3 mapas

// Detecciones
detectPortScan:    portScanTracker[srcIp].add(dstPort) → si size > 20 → PORT_SCAN HIGH
detectBruteForce:  key=srcIp:dstPort, contador > 10 y dstPort in SENSITIVE → BRUTE_FORCE CRITICAL
detectSynFlood:    flags contiene S pero no A, contador > 100 → SYN_FLOOD CRITICAL
detectDnsTunneling: UDP dstPort=53 packetLength > 512 → DNS_TUNNELING HIGH
```

**consumer/PacketConsumer.java** — @Component @RequiredArgsConstructor @Slf4j
```java
@RabbitListener(queues="netwatch.packets.raw", containerFactory="rabbitListenerContainerFactory")
void procesarPaquete(PacketMessage paquete):
  for ThreatEvent amenaza in engine.analyze(paquete):
    rabbitTemplate.convertAndSend(EXCHANGE, RK_THREATS, amenaza)
    if severity in [HIGH, CRITICAL]:
      rabbitTemplate.convertAndSend(EXCHANGE, RK_ALERTS, amenaza)
      rabbitTemplate.convertAndSend(EXCHANGE, RK_OSINT, amenaza)

@Scheduled(fixedRate=60_000)
void limpiarContadores(): engine.resetCounters()
```

**config/RabbitMQConfig.java** — misma configuración que en api-gateway

**src/main/resources/application.yml**
```yaml
spring.application.name: netwatch-worker-analysis
spring.datasource: igual que gateway pero ddl-auto validate
spring.rabbitmq: igual que gateway
server.port: 8081
management.endpoints.web.exposure.include: health,prometheus
```

---

### ❌ PENDIENTES — crear estos archivos

#### netwatch-worker-capture — paquete com.netwatch.capture

**NetwatchCaptureApplication.java**
```java
@SpringBootApplication @EnableScheduling
```

**model/PacketMessage.java** — idéntico al de worker-analysis

**config/RabbitMQConfig.java** — mismo patrón que los otros servicios

**producer/PacketProducer.java** — @Component @RequiredArgsConstructor @Slf4j
```java
private final RabbitTemplate rabbitTemplate
void publish(PacketMessage msg):
  rabbitTemplate.convertAndSend("netwatch.direct", "packets.raw", msg)
  log.info("Paquete publicado: {}", msg.getSrcIp())
```

**service/NetworkCaptureService.java** — @Service @Slf4j
```java
@Value("${netwatch.capture.interface:eth0}") String interfaceName
private final PacketProducer packetProducer

@Scheduled(fixedDelay=5000)
void startCapture():
  try:
    Intentar captura real con Pcap4J
    NetworkInterface nif = Pcap4J.getNetworkInterfaceByName(interfaceName)
    // Capturar paquetes y publicar via packetProducer.publish()
  catch PcapNativeException | NotOpenException e:
    log.warn("Pcap4J no disponible, usando simulación: {}", e.getMessage())
    simulatePackets()

void simulatePackets():
  // Genera 5 paquetes aleatorios y los publica
  // IPs del rango 10.x.x.x o 192.168.x.x
  // Puertos aleatorios de: 22, 80, 443, 3389, 53, 8080, 5432
  // Protocolos: TCP o UDP
  // Flags TCP: S (SYN), SA (SYN-ACK), A (ACK), PA (PSH-ACK)
  // Si mismo srcIp aparece > 20 veces → aumentar probabilidad puerto 22 (simula brute force)
```

**src/main/resources/application.yml**
```yaml
spring.application.name: netwatch-worker-capture
spring.rabbitmq.host: ${SPRING_RABBITMQ_HOST:localhost}
spring.rabbitmq.port: 5672
spring.rabbitmq.username: ${SPRING_RABBITMQ_USERNAME:guest}
spring.rabbitmq.password: ${SPRING_RABBITMQ_PASSWORD:guest}
server.port: 8082
netwatch.capture.interface: ${NETWATCH_CAPTURE_INTERFACE:eth0}
management.endpoints.web.exposure.include: health,prometheus
logging.level.com.netwatch: INFO
```

---

#### netwatch-worker-alerts — paquete com.netwatch.alerts

**NetwatchAlertsApplication.java**
```java
@SpringBootApplication
```

**model/ThreatEvent.java** — idéntico al de worker-analysis

**config/RabbitMQConfig.java** — mismo patrón + agregar Bean RestTemplate

**service/NotificationService.java** — @Service @Slf4j
```java
@Value("${netwatch.alerts.email.to:admin@netwatch.local}") String alertEmail
@Value("${netwatch.alerts.webhook.url:}") String webhookUrl
private final JavaMailSender mailSender
private final RestTemplate restTemplate

void sendEmail(ThreatEvent threat):
  SimpleMailMessage msg = new SimpleMailMessage()
  msg.setTo(alertEmail)
  msg.setSubject("[NetWatch] " + threat.getThreatType() + " - " + threat.getSeverity())
  msg.setText(formato con todos los campos del threat)
  try: mailSender.send(msg); log.info(...)
  catch Exception: log.error(...)

void sendWebhook(ThreatEvent threat):
  if webhookUrl.isBlank() return
  Map payload = {"text": "[NetWatch] " + tipo + " (" + severidad + ") desde " + ip}
  try: restTemplate.postForEntity(webhookUrl, payload, String.class)
  catch Exception: log.error(...)
```

**consumer/AlertConsumer.java** — @Component @RequiredArgsConstructor @Slf4j
```java
@RabbitListener(queues="netwatch.alerts.notify")
void procesarAlerta(ThreatEvent amenaza):
  log.warn("Alerta: {} {} {}", amenaza.getThreatType(), amenaza.getSeverity(), amenaza.getSrcIp())
  switch amenaza.getSeverity():
    "CRITICAL" → notificationService.sendEmail(amenaza) + sendWebhook(amenaza)
    "HIGH"     → notificationService.sendWebhook(amenaza)
    default    → log.debug(solo registro)
```

**src/main/resources/application.yml**
```yaml
spring.application.name: netwatch-worker-alerts
spring.rabbitmq: (igual patrón)
spring.mail.host: ${SPRING_MAIL_HOST:smtp.gmail.com}
spring.mail.port: ${SPRING_MAIL_PORT:587}
spring.mail.username: ${SPRING_MAIL_USERNAME:}
spring.mail.password: ${SPRING_MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth: true
spring.mail.properties.mail.smtp.starttls.enable: true
server.port: 8083
netwatch.alerts.email.to: ${NETWATCH_ALERT_EMAIL:admin@netwatch.local}
netwatch.alerts.webhook.url: ${NETWATCH_WEBHOOK_URL:}
management.endpoints.web.exposure.include: health,prometheus
```

---

#### netwatch-worker-osint — paquete com.netwatch.osint

**NetwatchOsintApplication.java**
```java
@SpringBootApplication
```

**model/ThreatEvent.java** — igual que en analysis PERO con campos adicionales:
```java
// campos base (igual que analysis):
String threatType, severity, srcIp, dstIp, description
Integer dstPort
LocalDateTime detectedAt
// campos de enriquecimiento (nuevos):
String country, city, asn
Double latitude, longitude
Integer abuseScore
```

**model/GeoIpResult.java** — @Data @NoArgsConstructor @AllArgsConstructor @Builder
```java
String country, city, asn
Double latitude, longitude
Integer abuseScore

static GeoIpResult empty():
  return GeoIpResult.builder().country("Unknown").city("Unknown")
    .latitude(0.0).longitude(0.0).asn("Unknown").abuseScore(0).build()
```

**config/RabbitMQConfig.java** — mismo patrón + Bean RestTemplate + Bean ObjectMapper

**service/GeoIpService.java** — @Service @Slf4j
```java
private static final String IP_API_URL = "http://ip-api.com/json/{ip}?fields=country,city,lat,lon,as,query"
private final StringRedisTemplate redisTemplate
private final RestTemplate restTemplate
private final ObjectMapper objectMapper

GeoIpResult lookup(String ip):
  // 1. Buscar en caché Valkey
  String cached = redisTemplate.opsForValue().get("geoip:" + ip)
  if cached != null:
    return objectMapper.readValue(cached, GeoIpResult.class)

  // 2. Consultar ip-api.com
  try:
    Map response = restTemplate.getForObject(IP_API_URL, Map.class, ip)
    GeoIpResult result = GeoIpResult.builder()
      .country((String) response.get("country"))
      .city((String) response.get("city"))
      .latitude((Double) response.get("lat"))
      .longitude((Double) response.get("lon"))
      .asn((String) response.get("as"))
      .abuseScore(0)
      .build()

    // 3. Guardar en caché con TTL 1 hora
    redisTemplate.opsForValue().set("geoip:" + ip,
      objectMapper.writeValueAsString(result),
      Duration.ofHours(1))

    return result
  catch Exception e:
    log.warn("Error consultando GeoIP para {}: {}", ip, e.getMessage())
    return GeoIpResult.empty()
```

**consumer/OsintConsumer.java** — @Component @RequiredArgsConstructor @Slf4j
```java
@RabbitListener(queues="netwatch.osint.enrich")
void enriquecer(ThreatEvent threat):
  GeoIpResult geo = geoIpService.lookup(threat.getSrcIp())
  threat.setCountry(geo.getCountry())
  threat.setCity(geo.getCity())
  threat.setLatitude(geo.getLatitude())
  threat.setLongitude(geo.getLongitude())
  threat.setAsn(geo.getAsn())
  threat.setAbuseScore(geo.getAbuseScore())
  rabbitTemplate.convertAndSend("netwatch.direct", "threats.detected", threat)
  log.info("IP {} → {}, {} (abuso: {}%)", threat.getSrcIp(), geo.getCountry(), geo.getCity(), geo.getAbuseScore())
```

**src/main/resources/application.yml**
```yaml
spring.application.name: netwatch-worker-osint
spring.rabbitmq: (igual patrón)
spring.data.redis.host: ${SPRING_DATA_REDIS_HOST:localhost}
spring.data.redis.port: 6379
spring.data.redis.password: ${SPRING_DATA_REDIS_PASSWORD:}
server.port: 8084
management.endpoints.web.exposure.include: health,prometheus
```

---

#### frontend — Vue.js 3 + Vite

**package.json** — dependencias:
```json
"vue": "^3.4.0",
"vue-router": "^4.3.0",
"pinia": "^2.1.0",
"axios": "^1.6.0",
"chart.js": "^4.4.0",
"vue-chartjs": "^5.3.0"
devDependencies: vite, @vitejs/plugin-vue, tailwindcss
```

**vite.config.js**
```js
plugins: [vue()]
resolve.alias: {'@': '/src'}
server.proxy: {'/api': 'http://localhost:8080'}
```

**src/main.js**
```js
createApp(App).use(createPinia()).use(router).mount('#app')
```

**src/router/index.js**
```js
routes: [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'login', component: LoginView, meta: {requiresAuth: false} },
  { path: '/dashboard', component: DashboardView, meta: {requiresAuth: true} },
  { path: '/events', component: EventsView, meta: {requiresAuth: true} },
  { path: '/alerts', component: AlertsView, meta: {requiresAuth: true} }
]
// beforeEach: si requiresAuth y !auth.isLoggedIn → redirect /login
```

**src/services/api.js**
```js
// Axios con baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080'
// Interceptor request: agrega Authorization: Bearer {token} desde localStorage
// Interceptor response: si 401 → intenta refresh token → si falla → localStorage.clear() + redirect /login
// authAPI: login(email, password), logout(), refresh(token)
// eventsAPI: getAll(params), getById(id), resolve(id), getSummary()
// alertsAPI: getAll(params), acknowledge(id), resolve(id)
```

**src/stores/auth.js** — Pinia defineStore
```js
state: user(ref null), accessToken(ref localStorage), loading(ref false), error(ref null)
computed: isLoggedIn, isAdmin
actions:
  login(email, password): POST /api/v1/auth/login → localStorage tokens → router.push('/dashboard')
  logout(): POST /api/v1/auth/logout → localStorage.clear() → router.push('/login')
```

**src/stores/events.js** — Pinia defineStore
```js
state: events[], summary(null), loading(false), error(null), filters{}, totalPages(0)
computed: criticalCount, unresolvedCount
actions:
  fetchEvents(): GET /api/v1/events con filters como params
  fetchSummary(): GET /api/v1/events/stats/summary
  resolveEvent(id): PATCH /api/v1/events/{id}/resolve → actualizar en array local
  startPolling(): fetchEvents() + setInterval(fetchEvents, 10000)
  stopPolling(): clearInterval()
```

**src/views/LoginView.vue**
```vue
Template: form con v-model email y password, @submit.prevent handleLogin
Script: ref email='', password='', useAuthStore(), handleLogin() llama auth.login()
Muestra auth.error si hay error, deshabilita botón si auth.loading
```

**src/views/DashboardView.vue**
```vue
onMounted: events.startPolling() + events.fetchSummary()
onUnmounted: events.stopPolling()
Template: grid de 4 StatCard + ThreatChart + EventTable con últimos 10 eventos
```

**src/components/StatCard.vue** — props: titulo(String), valor(Number), color(String default 'blue')

**src/components/EventTable.vue**
```vue
props: events(Array), loading(Boolean)
emits: ['resolve']
v-if loading → spinner
v-else-if events.length===0 → mensaje vacío
v-else → tabla con v-for event in events :key="event.id"
Columnas: timestamp, srcIp, threatType, severity (badge coloreado), status, acciones
```

**src/components/ThreatChart.vue**
```vue
Chart.js Bar chart
computed chartData: agrupa events por threatType, cuenta ocurrencias
options: responsive, maintainAspectRatio false, legend disabled
```

**Dockerfile frontend** — multi-stage:
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**nginx.conf**
```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index index.html;
  location / { try_files $uri $uri/ /index.html; }
  location /api { proxy_pass http://api-gateway:8080; }
  location ~* \.(js|css|png|svg)$ { expires 1y; }
}
```

---

#### infrastructure/sql/init.sql

```sql
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'ANALYST'
    CHECK (role IN ('ADMIN','ANALYST','VIEWER')),
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS network_events (
  id UUID NOT NULL DEFAULT gen_random_uuid(),
  src_ip VARCHAR(45) NOT NULL,
  dst_ip VARCHAR(45), src_port INTEGER, dst_port INTEGER,
  protocol VARCHAR(10), flags VARCHAR(30), packet_length INTEGER,
  threat_type VARCHAR(30), severity VARCHAR(10),
  description TEXT, country VARCHAR(100), city VARCHAR(100),
  latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
  asn VARCHAR(100), abuse_score INTEGER DEFAULT 0,
  resolved BOOLEAN DEFAULT false,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('network_events','timestamp',
  chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_events_src_ip   ON network_events(src_ip, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_events_severity ON network_events(severity, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_events_threat   ON network_events(threat_type, timestamp DESC);

CREATE TABLE IF NOT EXISTS alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID REFERENCES network_events(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL, details TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
    CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','FALSE_POSITIVE')),
  notification_sent BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Usuarios iniciales (contraseña: NetWatch2024! — BCrypt strength 12)
INSERT INTO users (email, password_hash, role) VALUES
  ('admin@netwatch.local',
   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/i.8c/F5K2',
   'ADMIN'),
  ('analista@netwatch.local',
   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/i.8c/F5K2',
   'ANALYST')
ON CONFLICT (email) DO NOTHING;
```

---

#### docker-compose.yml — 13 servicios

```yaml
version: "3.9"
networks:
  netwatch-net:
    driver: bridge
volumes:
  postgres_data: rabbitmq_data: redis_data: grafana_data: loki_data:

services:
  postgres:
    image: timescale/timescaledb:latest-pg15
    networks: [netwatch-net]
    environment: POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
    volumes: postgres_data + ./infrastructure/sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports: 5432:5432
    healthcheck: pg_isready -U netwatch interval 10s retries 5

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    networks: [netwatch-net]
    environment: RABBITMQ_DEFAULT_USER, RABBITMQ_DEFAULT_PASS
    ports: 5672:5672, 15672:15672
    healthcheck: rabbitmq-diagnostics ping

  redis:
    image: valkey/valkey:7.2-alpine
    networks: [netwatch-net]
    command: valkey-server --requirepass ${REDIS_PASSWORD}
    healthcheck: valkey-cli -a ${REDIS_PASSWORD} ping

  api-gateway:
    build: ./netwatch-api-gateway
    networks: [netwatch-net]
    ports: 8080:8080
    environment: SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/netwatch,
      SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD,
      SPRING_RABBITMQ_HOST=rabbitmq, SPRING_RABBITMQ_USERNAME, SPRING_RABBITMQ_PASSWORD,
      NETWATCH_JWT_SECRET, NETWATCH_JWT_REFRESH_SECRET
    depends_on: postgres(healthy), rabbitmq(healthy)
    healthcheck: wget -qO- http://localhost:8080/actuator/health start-period 45s

  worker-capture:
    build: ./netwatch-worker-capture
    network_mode: host
    cap_add: [NET_ADMIN, NET_RAW]
    environment: SPRING_RABBITMQ_HOST=localhost, SPRING_RABBITMQ_USERNAME,
      SPRING_RABBITMQ_PASSWORD, NETWATCH_CAPTURE_INTERFACE=eth0
    depends_on: rabbitmq(healthy)

  worker-analysis:
    build: ./netwatch-worker-analysis
    networks: [netwatch-net]
    environment: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD,
      SPRING_RABBITMQ_HOST=rabbitmq, SPRING_RABBITMQ_USERNAME, SPRING_RABBITMQ_PASSWORD
    depends_on: postgres(healthy), rabbitmq(healthy)

  worker-alerts:
    build: ./netwatch-worker-alerts
    networks: [netwatch-net]
    environment: SPRING_RABBITMQ_HOST=rabbitmq, SPRING_RABBITMQ_USERNAME, SPRING_RABBITMQ_PASSWORD,
      SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD,
      NETWATCH_ALERT_EMAIL, NETWATCH_WEBHOOK_URL
    depends_on: rabbitmq(healthy)

  worker-osint:
    build: ./netwatch-worker-osint
    networks: [netwatch-net]
    environment: SPRING_RABBITMQ_HOST=rabbitmq, SPRING_RABBITMQ_USERNAME, SPRING_RABBITMQ_PASSWORD,
      SPRING_DATA_REDIS_HOST=redis, SPRING_DATA_REDIS_PASSWORD
    depends_on: rabbitmq(healthy), redis(healthy)

  frontend:
    build: ./frontend
    networks: [netwatch-net]
    ports: 3000:80
    depends_on: api-gateway

  prometheus:
    image: prom/prometheus:latest
    networks: [netwatch-net]
    ports: 9090:9090
    volumes: ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana-oss:latest
    networks: [netwatch-net]
    ports: 3001:3000
    environment: GF_SECURITY_ADMIN_PASSWORD, GF_USERS_ALLOW_SIGN_UP=false
    volumes: grafana_data + ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards

  loki:
    image: grafana/loki:latest
    networks: [netwatch-net]
    ports: 3100:3100
    volumes: loki_data + ./monitoring/loki/loki-config.yml:/etc/loki/local-config.yaml

  promtail:
    image: grafana/promtail:latest
    networks: [netwatch-net]
    volumes: /var/log:/var/log:ro + /var/lib/docker/containers:/var/lib/docker/containers:ro
      + ./monitoring/loki/promtail-config.yml:/etc/promtail/config.yml
```

---

#### Dockerfile patrón (mismo para los 5 servicios — cambiar PUERTO y ARTIFACT_NAME)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true
COPY src ./src
RUN mvn package -DskipTests -B
RUN java -Djarmode=layertools -jar target/ARTIFACT_NAME-*.jar extract

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S netwatch && adduser -S netwatch -G netwatch
WORKDIR /app
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./
RUN chown -R netwatch:netwatch /app
USER netwatch
EXPOSE PUERTO
HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:PUERTO/actuator/health || exit 1
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0",
  "-Djava.security.egd=file:/dev/./urandom",
  "org.springframework.boot.loader.launch.JarLauncher"]
```

Puertos por servicio: api-gateway=8080, worker-analysis=8081, worker-capture=8082, worker-alerts=8083, worker-osint=8084

---

#### monitoring/prometheus/prometheus.yml

```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: netwatch-api-gateway
    static_configs: [{targets: ['api-gateway:8080']}]
    metrics_path: /actuator/prometheus
  - job_name: netwatch-worker-analysis
    static_configs: [{targets: ['worker-analysis:8081']}]
    metrics_path: /actuator/prometheus
  - job_name: netwatch-worker-capture
    static_configs: [{targets: ['localhost:8082']}]
    metrics_path: /actuator/prometheus
  - job_name: netwatch-worker-alerts
    static_configs: [{targets: ['worker-alerts:8083']}]
    metrics_path: /actuator/prometheus
  - job_name: netwatch-worker-osint
    static_configs: [{targets: ['worker-osint:8084']}]
    metrics_path: /actuator/prometheus
  - job_name: rabbitmq
    static_configs: [{targets: ['rabbitmq:15692']}]
```

#### monitoring/loki/loki-config.yml

```yaml
auth_enabled: false
server:
  http_listen_port: 3100
ingester:
  lifecycler:
    ring:
      kvstore: {store: inmemory}
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s
schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index: {prefix: index_, period: 24h}
storage_config:
  boltdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/boltdb-cache
  filesystem:
    directory: /loki/chunks
limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h
```

#### monitoring/loki/promtail-config.yml

```yaml
server:
  http_listen_port: 9080
positions:
  filename: /tmp/positions.yaml
clients:
  - url: http://loki:3100/loki/api/v1/push
scrape_configs:
  - job_name: containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: [__meta_docker_container_name]
        target_label: container
      - source_labels: [__meta_docker_container_label_com_docker_compose_service]
        target_label: job
```

#### monitoring/falco/falco-rules.yml

```yaml
- rule: NetWatch captura no autorizada
  desc: Proceso de captura en contenedor que no es worker-capture
  condition: >
    spawned_process and proc.name in (tcpdump, tshark, scapy)
    and not container.name = "netwatch-worker-capture"
  output: "Captura no autorizada (container=%container.name proc=%proc.name)"
  priority: CRITICAL

- rule: NetWatch escalada de privilegios
  desc: Su o sudo en contenedor de netwatch
  condition: >
    container and container.name startswith "netwatch"
    and proc.name in (su, sudo)
  output: "Escalada de privilegios (container=%container.name proc=%proc.name)"
  priority: WARNING

- rule: NetWatch escritura en sistema
  desc: Escritura en directorio del sistema en contenedor netwatch
  condition: >
    container and container.name startswith "netwatch"
    and write and fd.name startswith /etc
  output: "Escritura en sistema (container=%container.name file=%fd.name)"
  priority: WARNING
```

---

#### .github/workflows/ci.yml — 7 jobs

```yaml
name: CI — NetWatch Security Pipeline
on:
  push: {branches: [main, develop]}
  pull_request: {branches: [main]}

jobs:

  secrets-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: {fetch-depth: 0}
      - uses: gitleaks/gitleaks-action@v2
        env: {GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}}

  sast:
    needs: secrets-scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '21', distribution: 'temurin', cache: maven}
      - name: SpotBugs
        run: mvn spotbugs:spotbugs -B
        continue-on-error: true
      - name: Semgrep OSS (sin token)
        run: |
          pip install semgrep --quiet
          semgrep --config p/java --config p/owasp-top-ten --config p/spring-boot \
            --json --output semgrep-report.json --error . || true
      - uses: actions/upload-artifact@v4
        if: always()
        with: {name: sast-reports, path: '**/target/spotbugsXml.xml'}

  sca:
    needs: secrets-scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '21', distribution: 'temurin', cache: maven}
      - name: OWASP Dependency-Check
        run: mvn dependency-check:check -B
        env: {NVD_API_KEY: ${{ secrets.NVD_API_KEY }}}
        continue-on-error: true
      - uses: actions/upload-artifact@v4
        if: always()
        with: {name: dependency-check-report, path: '**/target/dependency-check-report.*'}

  build-and-scan:
    needs: [sast, sca]
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [netwatch-api-gateway, netwatch-worker-capture,
                  netwatch-worker-analysis, netwatch-worker-alerts, netwatch-worker-osint]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '21', distribution: 'temurin', cache: maven}
      - name: Build JAR
        run: mvn package -DskipTests -B -pl ${{ matrix.service }}
      - name: Build imagen Docker
        run: |
          docker build -t ${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}:${{ github.sha }} \
            ./${{ matrix.service }}
      - name: Trivy escaneo imagen
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}:${{ github.sha }}
          severity: CRITICAL
          exit-code: 1
          format: sarif
          output: trivy-${{ matrix.service }}.sarif
      - uses: github/codeql-action/upload-sarif@v3
        if: always()
        with: {sarif_file: 'trivy-${{ matrix.service }}.sarif'}

  unit-tests:
    needs: build-and-scan
    runs-on: ubuntu-latest
    services:
      postgres:
        image: timescale/timescaledb:latest-pg15
        env: {POSTGRES_DB: netwatch_test, POSTGRES_USER: netwatch, POSTGRES_PASSWORD: testpass}
        ports: ['5432:5432']
        options: --health-cmd pg_isready --health-interval 10s --health-retries 5
      rabbitmq:
        image: rabbitmq:3.12-alpine
        ports: ['5672:5672']
        options: --health-cmd "rabbitmq-diagnostics ping" --health-interval 10s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '21', distribution: 'temurin', cache: maven}
      - name: Tests y cobertura JaCoCo
        run: mvn verify -B -pl netwatch-api-gateway,netwatch-worker-analysis
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/netwatch_test
          SPRING_DATASOURCE_USERNAME: netwatch
          SPRING_DATASOURCE_PASSWORD: testpass
          SPRING_RABBITMQ_HOST: localhost
          SPRING_RABBITMQ_USERNAME: guest
          SPRING_RABBITMQ_PASSWORD: guest
          NETWATCH_JWT_SECRET: test-secret-key-minimo-64-caracteres-para-hmac-sha256-test
          NETWATCH_JWT_REFRESH_SECRET: test-refresh-key-minimo-64-caracteres-para-hmac-sha256
      - uses: actions/upload-artifact@v4
        with: {name: jacoco-reports, path: '**/target/site/jacoco/'}

  dast:
    needs: unit-tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Preparar entorno staging
        run: |
          cp .env.example .env
          sed -i 's/REEMPLAZA_CON_CADENA_ALEATORIA_DE_64_CHARS/test-jwt-secret-64-caracteres-minimo-para-hmac-sha256/' .env
          sed -i 's/REEMPLAZA_CON_OTRA_CADENA_64_CHARS/test-refresh-secret-64-caracteres-minimo-para-hmac/' .env
          sed -i 's/REEMPLAZA/testpassword/g' .env
      - name: Levantar staging
        run: |
          docker compose up -d postgres rabbitmq redis api-gateway
          timeout 120 bash -c 'until curl -sf http://localhost:8080/actuator/health; do sleep 5; done'
      - name: OWASP ZAP Baseline
        uses: zaproxy/action-baseline@v0.10.0
        with:
          target: http://localhost:8080
          rules_file_name: .zap/rules.tsv
          fail_action: warn
          cmd_options: -I
      - name: Bajar staging
        if: always()
        run: docker compose down -v

  iac-scan:
    needs: secrets-scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Checkov Terraform
        uses: bridgecrewio/checkov-action@master
        with: {directory: infrastructure/terraform, framework: terraform, soft_fail: true}
      - name: Checkov Kubernetes
        uses: bridgecrewio/checkov-action@master
        with: {directory: infrastructure/k8s, framework: kubernetes, soft_fail: true}
      - name: Checkov Dockerfiles
        uses: bridgecrewio/checkov-action@master
        with: {directory: '.', framework: dockerfile, soft_fail: true}
```

#### .github/workflows/deploy.yml

```yaml
name: Deploy — Docker Hub + Ansible
on:
  push:
    tags: ['v*.*.*']
jobs:
  publish:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [netwatch-api-gateway, netwatch-worker-capture,
                  netwatch-worker-analysis, netwatch-worker-alerts, netwatch-worker-osint]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {java-version: '21', distribution: 'temurin', cache: maven}
      - run: mvn package -DskipTests -B -pl ${{ matrix.service }}
      - uses: docker/login-action@v3
        with: {username: ${{ secrets.DOCKERHUB_USERNAME }}, password: ${{ secrets.DOCKERHUB_TOKEN }}}
      - id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=raw,value=latest
      - uses: docker/build-push-action@v5
        with:
          context: ./${{ matrix.service }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
```

#### .zap/rules.tsv

```
10015	IGNORE	Incomplete or No Cache-control Header Set
10027	IGNORE	Information Disclosure - Suspicious Comments
10096	IGNORE	Timestamp Disclosure
10038	WARN	Content Security Policy Header Not Set
10020	FAIL	Anti-clickjacking Header
10021	FAIL	X-Content-Type-Options Header Missing
40012	FAIL	Cross Site Scripting Reflected
40014	FAIL	Cross Site Scripting Persistent
40018	FAIL	SQL Injection
40019	FAIL	SQL Injection MySQL
```

---

## 6. Pruebas unitarias — clases de test pendientes

### netwatch-api-gateway/src/test — AuthServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  // Mocks: UserRepository, JwtTokenProvider, PasswordEncoder
  // Tests:
  //   login_conCredencialesValidas_retornaTokens()
  //   login_conPasswordIncorrecto_lanzaException()
  //   login_conEmailNoExistente_lanzaException()
  //   login_conCuentaInactiva_lanzaException()
  //   refresh_conTokenValido_retornaTokens()
  //   refresh_conTokenInvalido_lanzaException()
}
```

### netwatch-worker-analysis/src/test — ThreatDetectionEngineTest.java

```java
class ThreatDetectionEngineTest {
  // Tests:
  //   detectaPortScan_cuandoSuperaUmbral()
  //   noDetectaPortScan_conTraficoNormal()
  //   detectaBruteForce_enPuertoSSH()
  //   detectaBruteForce_enPuertoRDP()
  //   noDetectaBruteForce_enPuertoNoSensible()
  //   detectaSynFlood_cuandoSuperaUmbral()
  //   detectaDnsTunneling_conPaqueteGrande()
  //   noDetectaDnsTunneling_conPaqueteNormal()
  //   resetContadores_limpiaTodosLosTrackers()
}
```

**Requisito JaCoCo:** cobertura mínima 70% — si es menor, `mvn verify` falla con:
`Coverage check failed for project... Lines covered ratio is X, expected minimum is 0.70`

---

## 7. Variables de entorno — convenciones

### Nombres exactos en docker-compose.yml

```
# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/netwatch
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}

# RabbitMQ
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_USERNAME=${RABBITMQ_USER}
SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}

# Valkey/Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}

# Email
SPRING_MAIL_HOST=${SMTP_HOST}
SPRING_MAIL_PORT=${SMTP_PORT}
SPRING_MAIL_USERNAME=${SMTP_USER}
SPRING_MAIL_PASSWORD=${SMTP_PASSWORD}

# JWT
NETWATCH_JWT_SECRET=${JWT_SECRET}
NETWATCH_JWT_REFRESH_SECRET=${JWT_REFRESH_SECRET}

# Específicas de servicios
NETWATCH_CAPTURE_INTERFACE=eth0
NETWATCH_ALERT_EMAIL=${SMTP_USER}
NETWATCH_WEBHOOK_URL=${NETWATCH_WEBHOOK_URL}

# Grafana
GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
GF_USERS_ALLOW_SIGN_UP=false
```

### .env.example — variables del archivo .env (sin valores reales)

```bash
# JWT (genera con: openssl rand -hex 64)
JWT_SECRET=REEMPLAZA_CON_CADENA_ALEATORIA_DE_64_CHARS
JWT_REFRESH_SECRET=REEMPLAZA_CON_OTRA_CADENA_64_CHARS

# PostgreSQL
POSTGRES_DB=netwatch
POSTGRES_USER=netwatch
POSTGRES_PASSWORD=REEMPLAZA

# RabbitMQ
RABBITMQ_USER=netwatch
RABBITMQ_PASSWORD=REEMPLAZA

# Valkey (reemplaza Redis)
REDIS_PASSWORD=REEMPLAZA

# Email SMTP
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=tu@email.com
SMTP_PASSWORD=REEMPLAZA_APP_PASSWORD_GMAIL

# Alertas
NETWATCH_ALERT_EMAIL=admin@netwatch.local
NETWATCH_WEBHOOK_URL=

# Grafana
GRAFANA_PASSWORD=REEMPLAZA

# Docker Hub
DOCKERHUB_USERNAME=tu-usuario-dockerhub
```

---

## 8. Información de seguridad JWT

```
Access token:    HMAC-SHA256, claim type="access",   TTL 30 min (1800000 ms)
Refresh token:   HMAC-SHA256, claim type="refresh",  TTL 7 días (604800000 ms)
Header petición: Authorization: Bearer <access_token>
Header refresh:  X-Refresh-Token: <refresh_token>
Clave mínima:    64 caracteres para HMAC-SHA256 (usar openssl rand -hex 64)
```

---

## 9. Credenciales de prueba iniciales

```
Usuario:    admin@netwatch.local     Contraseña: NetWatch2024!   Rol: ADMIN
Usuario:    analista@netwatch.local  Contraseña: NetWatch2024!   Rol: ANALYST
BCrypt hash (strength 12): $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/i.8c/F5K2
```

---

## 10. Puertos de todos los servicios

```
PostgreSQL:       5432
RabbitMQ AMQP:    5672
RabbitMQ UI:      15672
Valkey:           6379
API Gateway:      8080
Worker Analysis:  8081
Worker Capture:   8082  (network_mode: host — necesita NET_ADMIN, NET_RAW)
Worker Alerts:    8083
Worker OSINT:     8084
Frontend:         3000 → Nginx :80
Prometheus:       9090
Grafana:          3001 → Grafana :3000
Loki:             3100
```

---

## 11. Umbrales del motor de detección

```java
PORT_SCAN_THRESHOLD   = 20   // > 20 puertos distintos desde misma IP en ventana de 60s
BRUTE_FORCE_THRESHOLD = 10   // > 10 intentos al mismo puerto sensible
SYN_FLOOD_THRESHOLD   = 100  // > 100 paquetes SYN sin ACK desde misma IP
DNS_TUNNELING_BYTES   = 512  // paquete UDP puerto 53 con payload > 512 bytes
SENSITIVE_PORTS       = {22, 21, 23, 3389, 5432, 3306, 27017, 6379}
RESET_INTERVAL        = 60_000 ms  // @Scheduled limpia contadores cada minuto
```

---

## 12. GitHub Actions secrets requeridos

```
DOCKERHUB_USERNAME   → usuario de Docker Hub
DOCKERHUB_TOKEN      → Access Token Docker Hub (Read & Write)
JWT_SECRET           → openssl rand -hex 64
JWT_REFRESH_SECRET   → openssl rand -hex 64 (diferente al anterior)
NVD_API_KEY          → registrarse en nvd.nist.gov (gratis)
```

---

## 13. Criterios de evaluación del trabajo

| Criterio | Peso | Estado actual |
|---------|------|--------------|
| Aplicación funcional (6 componentes) | 15% | En progreso |
| Pipeline CI/CD completo (7 fases) | 25% | Pendiente |
| Herramientas de seguridad integradas | 25% | Pendiente |
| Documentación + 5 diagramas UML PlantUML | 20% | Pendiente |
| Repositorio GitHub público + Docker Hub | 10% | Repo creado |
| Observabilidad Prometheus+Grafana+Loki+Falco | +3% bonus | Pendiente |
| Video demostración 10-15 minutos | +2% bonus | Pendiente |

### Herramientas de seguridad obligatorias por fase del pipeline

```
PLAN:    OWASP Threat Dragon → DFD nivel 0 y 1, análisis STRIDE
CODE:    Gitleaks (pre-commit + CI), SpotBugs+FSB (SAST), Semgrep OSS (SAST), OWASP Dependency-Check (SCA)
BUILD:   Trivy → falla si CVE CRITICAL sin justificación documentada
TEST:    JUnit 5 + JaCoCo (cobertura ≥ 70%), OWASP ZAP Baseline (DAST)
RELEASE: Checkov (IaC: Terraform, K8s, Dockerfiles)
DEPLOY:  OpenTofu + Ansible
OPERATE: Prometheus + Grafana + Loki + Promtail + Falco
```

### Documentación obligatoria (docs/)

```
docs/architecture/component-diagram.puml    ← visión general microservicios
docs/architecture/deployment-diagram.puml   ← contenedores, redes, volúmenes
docs/architecture/sequence-auth.puml        ← flujo completo autenticación JWT
docs/architecture/use-cases.puml            ← actores: Admin, Analista, Viewer
docs/architecture/threat-model.json         ← OWASP Threat Dragon DFD 0 y 1
docs/development-manual.md                  ← clonar, configurar, ejecutar, contribuir
docs/deployment-manual.md                   ← despliegue desde cero con IaC
docs/security-manual.md                     ← STRIDE, herramientas, gestión vulnerabilidades
docs/user-manual.md                         ← guía de uso del dashboard con capturas
```

---

## 14. Modelado de amenazas STRIDE

| Categoría | Amenaza | Componente | Contramedida |
|-----------|---------|-----------|-------------|
| Spoofing | Suplantación de usuario | API Gateway | JWT TTL corto + refresh tokens rotados |
| Tampering | Manipulación paquetes en broker | RabbitMQ | TLS en AMQP + autenticación obligatoria |
| Repudiation | Negar acciones sobre datos | Base de datos | Audit log inmutable en PostgreSQL |
| Information Disclosure | Exposición IPs internas | API Gateway | Enmascaramiento por rol en respuestas |
| Denial of Service | Flood de paquetes | Worker Captura | Rate limiting + prefetch controlado |
| Elevation of Privilege | Analista accede a rutas admin | API Gateway | RBAC con @PreAuthorize + RLS en BD |
ENDOFFILE

echo "✓ CLAUDE.md creado"
wc -l /home/claude/CLAUDE.md
