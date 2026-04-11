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

RUN mvn dependency:go-offline -pl netwatch-worker-osint -am -B -q || true

COPY netwatch-worker-osint/src ./netwatch-worker-osint/src
RUN mvn package -pl netwatch-worker-osint -am -DskipTests -B -q

RUN java -Djarmode=layertools \
    -jar netwatch-worker-osint/target/netwatch-worker-osint-*.jar extract \
    --destination netwatch-worker-osint/layers

# ── Etapa 2: Imagen final ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S netwatch && adduser -S netwatch -G netwatch

WORKDIR /app

COPY --from=builder /build/netwatch-worker-osint/layers/dependencies/          ./
COPY --from=builder /build/netwatch-worker-osint/layers/spring-boot-loader/    ./
COPY --from=builder /build/netwatch-worker-osint/layers/snapshot-dependencies/ ./
COPY --from=builder /build/netwatch-worker-osint/layers/application/           ./

RUN chown -R netwatch:netwatch /app
USER netwatch

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
