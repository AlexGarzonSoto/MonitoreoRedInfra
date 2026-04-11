FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copiar todos los POM del multi-módulo (mismo patrón que los demás servicios)
COPY pom.xml                          ./pom.xml
COPY netwatch-api-gateway/pom.xml     ./netwatch-api-gateway/pom.xml
COPY netwatch-worker-analysis/pom.xml ./netwatch-worker-analysis/pom.xml
COPY netwatch-worker-capture/pom.xml  ./netwatch-worker-capture/pom.xml
COPY netwatch-worker-alerts/pom.xml   ./netwatch-worker-alerts/pom.xml
COPY netwatch-worker-osint/pom.xml    ./netwatch-worker-osint/pom.xml
COPY netwatch-worker-scanner/pom.xml  ./netwatch-worker-scanner/pom.xml

RUN mvn dependency:go-offline -pl netwatch-worker-scanner -am -B -q || true

COPY netwatch-worker-scanner/src ./netwatch-worker-scanner/src

RUN mvn package -pl netwatch-worker-scanner -am -DskipTests -B -q

RUN java -Djarmode=layertools \
    -jar netwatch-worker-scanner/target/netwatch-worker-scanner-*.jar extract \
    --destination netwatch-worker-scanner/layers

# ── Imagen final ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Nmap necesario para escaneo de puertos y detección de servicios
RUN apk add --no-cache nmap nmap-scripts

RUN addgroup -S netwatch && adduser -S netwatch -G netwatch
WORKDIR /app

COPY --from=builder /build/netwatch-worker-scanner/layers/dependencies/          ./
COPY --from=builder /build/netwatch-worker-scanner/layers/spring-boot-loader/    ./
COPY --from=builder /build/netwatch-worker-scanner/layers/snapshot-dependencies/ ./
COPY --from=builder /build/netwatch-worker-scanner/layers/application/           ./

RUN chown -R netwatch:netwatch /app
USER netwatch

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
