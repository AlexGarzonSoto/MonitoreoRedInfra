# ── Etapa 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml                          ./pom.xml
COPY netwatch-api-gateway/pom.xml     ./netwatch-api-gateway/pom.xml
COPY netwatch-worker-analysis/pom.xml ./netwatch-worker-analysis/pom.xml
COPY netwatch-worker-capture/pom.xml  ./netwatch-worker-capture/pom.xml
COPY netwatch-worker-alerts/pom.xml   ./netwatch-worker-alerts/pom.xml
COPY netwatch-worker-osint/pom.xml    ./netwatch-worker-osint/pom.xml
COPY netwatch-worker-scanner/pom.xml  ./netwatch-worker-scanner/pom.xml

RUN mvn dependency:go-offline -pl netwatch-worker-capture -am -B -q || true

COPY netwatch-worker-capture/src ./netwatch-worker-capture/src
RUN mvn package -pl netwatch-worker-capture -am -DskipTests -B -q

RUN java -Djarmode=layertools \
    -jar netwatch-worker-capture/target/netwatch-worker-capture-*.jar extract \
    --destination netwatch-worker-capture/layers

# ── Etapa 2: Imagen final ───────────────────────────────────────────────────
# Nota: este worker requiere en docker-compose.yml:
#   network_mode: host
#   cap_add: [NET_ADMIN, NET_RAW]
# para que Pcap4J pueda abrir la interfaz de red en modo promiscuo.
FROM amazoncorretto:21-alpine3.21

RUN apk upgrade --no-cache && apk add --no-cache libpcap

RUN addgroup -S netwatch && adduser -S netwatch -G netwatch

WORKDIR /app

COPY --from=builder /build/netwatch-worker-capture/layers/dependencies/          ./
COPY --from=builder /build/netwatch-worker-capture/layers/spring-boot-loader/    ./
COPY --from=builder /build/netwatch-worker-capture/layers/snapshot-dependencies/ ./
COPY --from=builder /build/netwatch-worker-capture/layers/application/           ./

RUN chown -R netwatch:netwatch /app
USER netwatch

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
