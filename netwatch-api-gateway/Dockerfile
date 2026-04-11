# ── Etapa 1: Build con Maven (contexto = raíz del proyecto) ────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copiar todos los POM del multi-módulo para resolver dependencias con caché
COPY pom.xml                          ./pom.xml
COPY netwatch-api-gateway/pom.xml     ./netwatch-api-gateway/pom.xml
COPY netwatch-worker-analysis/pom.xml ./netwatch-worker-analysis/pom.xml
COPY netwatch-worker-capture/pom.xml  ./netwatch-worker-capture/pom.xml
COPY netwatch-worker-alerts/pom.xml   ./netwatch-worker-alerts/pom.xml
COPY netwatch-worker-osint/pom.xml    ./netwatch-worker-osint/pom.xml
COPY netwatch-worker-scanner/pom.xml  ./netwatch-worker-scanner/pom.xml

# 2. Descargar dependencias (capa reutilizable mientras no cambie el pom.xml)
RUN mvn dependency:go-offline -pl netwatch-api-gateway -am -B -q || true

# 3. Copiar fuentes y compilar solo este módulo
COPY netwatch-api-gateway/src ./netwatch-api-gateway/src
RUN mvn package -pl netwatch-api-gateway -am -DskipTests -B -q

# 4. Extraer capas Spring Boot para imagen final más pequeña
RUN java -Djarmode=layertools \
    -jar netwatch-api-gateway/target/netwatch-api-gateway-*.jar extract \
    --destination netwatch-api-gateway/layers

# ── Etapa 2: Imagen final mínima ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Usuario no-root
RUN addgroup -S netwatch && adduser -S netwatch -G netwatch

WORKDIR /app

# Capas en orden de menor a mayor frecuencia de cambio (mejor caché)
COPY --from=builder /build/netwatch-api-gateway/layers/dependencies/          ./
COPY --from=builder /build/netwatch-api-gateway/layers/spring-boot-loader/    ./
COPY --from=builder /build/netwatch-api-gateway/layers/snapshot-dependencies/ ./
COPY --from=builder /build/netwatch-api-gateway/layers/application/           ./

RUN chown -R netwatch:netwatch /app
USER netwatch

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
