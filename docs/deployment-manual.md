# Manual de Despliegue — NetWatch

## Tabla de contenidos

1. [Requisitos de infraestructura](#1-requisitos-de-infraestructura)
2. [Despliegue local con Docker Compose](#2-despliegue-local-con-docker-compose)
3. [Despliegue en producción con IaC](#3-despliegue-en-producción-con-iac)
4. [Configuración de seguridad en producción](#4-configuración-de-seguridad-en-producción)
5. [Monitoreo y observabilidad](#5-monitoreo-y-observabilidad)
6. [Gestión de imágenes Docker](#6-gestión-de-imágenes-docker)
7. [Backup y recuperación](#7-backup-y-recuperación)
8. [Actualización del sistema](#8-actualización-del-sistema)

---

## 1. Requisitos de infraestructura

### Servidor mínimo (desarrollo / demo)

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| CPU | 2 cores | 4 cores |
| RAM | 4 GB | 8 GB |
| Disco | 20 GB | 50 GB |
| OS | Ubuntu 22.04 / Debian 12 | Ubuntu 22.04 LTS |
| Red | 1 NIC | 2 NICs (1 para gestión, 1 para captura) |

### Servidor producción

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| CPU | 4 cores | 8 cores |
| RAM | 8 GB | 16 GB |
| Disco | 100 GB SSD | 500 GB SSD |
| Red | 1 GbE | 10 GbE |

### Permisos de red requeridos

El worker de captura necesita acceso al raw socket de red:
- `CAP_NET_ADMIN` — para abrir interfaces en modo promiscuo
- `CAP_NET_RAW` — para capturar tráfico a nivel de paquetes
- `network_mode: host` — para ver todo el tráfico del host

---

## 2. Despliegue local con Docker Compose

### Paso 1: Preparar el entorno

```bash
# Clonar repositorio
git clone https://github.com/AlexGarzonSoto/MonitoreoRedInfra.git
cd MonitoreoRedInfra

# Copiar y configurar variables de entorno
cp .env.example .env
nano .env  # Completar todos los valores requeridos
```

### Paso 2: Generar claves seguras

```bash
# Generar claves JWT (ejecutar dos veces, usar valores distintos)
openssl rand -hex 64  # Para JWT_SECRET
openssl rand -hex 64  # Para JWT_REFRESH_SECRET

# Generar contraseñas
openssl rand -base64 32  # Para POSTGRES_PASSWORD, RABBITMQ_PASSWORD, etc.
```

### Paso 3: Iniciar los servicios

```bash
# Levantar en orden (las dependencias se manejan con depends_on + healthcheck)
docker compose up -d

# Verificar que todos los servicios están corriendo
docker compose ps

# Esperar hasta que el gateway esté listo (~60 segundos)
until curl -sf http://localhost:8080/actuator/health; do
  echo "Esperando API Gateway..."
  sleep 5
done
echo "API Gateway listo"
```

### Paso 4: Verificar la instalación

```bash
# Estado de salud de todos los servicios
docker compose ps

# Login de prueba
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@netwatch.local","password":"NetWatch2024!"}'

# Dashboard web
xdg-open http://localhost:3000
```

### Orden de inicio de servicios

```
postgres (healthcheck pg_isready)
    ↓
rabbitmq (healthcheck rabbitmq-diagnostics ping)
    ↓
valkey (healthcheck valkey-cli ping)
    ↓
api-gateway + worker-analysis + worker-alerts
    ↓
worker-osint (también necesita valkey)
    ↓
worker-capture (network_mode: host, necesita rabbitmq)
    ↓
frontend + prometheus + grafana + loki + promtail
```

---

## 3. Despliegue en producción con IaC

### OpenTofu (fork OSS de Terraform)

El directorio `infrastructure/terraform/` contiene la configuración de IaC para provisionar servidores en la nube.

```bash
# Instalar OpenTofu
curl -fsSL https://get.opentofu.org/install-opentofu.sh | bash

# Inicializar
cd infrastructure/terraform
tofu init

# Ver plan de cambios
tofu plan -var-file="production.tfvars"

# Aplicar cambios
tofu apply -var-file="production.tfvars"
```

### Ansible

Después de provisionar la infraestructura, Ansible configura los servidores:

```bash
# Instalar Ansible
pip install ansible-core

# Ver el playbook
cat infrastructure/ansible/site.yml

# Ejecutar (ajustar el inventario según la infraestructura)
ansible-playbook -i infrastructure/ansible/inventory.ini \
  infrastructure/ansible/site.yml \
  --ask-become-pass
```

El playbook de Ansible:
1. Instala Docker y Docker Compose en el servidor
2. Copia el repositorio al servidor
3. Configura el archivo `.env` con las variables de producción
4. Inicia los servicios con `docker compose up -d`
5. Configura el firewall (UFW)
6. Configura Nginx como reverse proxy si aplica

---

## 4. Configuración de seguridad en producción

### Variables de entorno críticas

```bash
# NUNCA usar los valores de ejemplo en producción
# JWT: siempre generar claves únicas de 64+ caracteres
JWT_SECRET=$(openssl rand -hex 64)
JWT_REFRESH_SECRET=$(openssl rand -hex 64)

# Base de datos: usuario no-root con permisos mínimos
POSTGRES_USER=netwatch_app     # NO usar postgres (superusuario)
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# RabbitMQ: usuario dedicado
RABBITMQ_USER=netwatch_mq
RABBITMQ_PASSWORD=$(openssl rand -base64 32)
```

### Firewall (UFW)

```bash
# Solo exponer los puertos necesarios al exterior
ufw allow 22/tcp      # SSH (restringir a IPs conocidas en producción)
ufw allow 80/tcp      # HTTP → redirigir a HTTPS con Nginx
ufw allow 443/tcp     # HTTPS

# Puertos internos — NO exponer al exterior
# 8080 (API), 5432 (PostgreSQL), 5672/15672 (RabbitMQ), 6379 (Valkey)
# 9090 (Prometheus), 3001 (Grafana), 3100 (Loki) — acceso solo desde VPN/LAN
```

### TLS/HTTPS

En producción, el frontend y la API deben estar detrás de un proxy inverso con TLS:

```nginx
# Ejemplo de configuración Nginx con TLS
server {
    listen 443 ssl http2;
    server_name netwatch.tudominio.com;

    ssl_certificate     /etc/letsencrypt/live/netwatch.tudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/netwatch.tudominio.com/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
    }

    # API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Authorization $http_authorization;
    }
}
```

### Seguridad del worker-capture

El worker de captura tiene privilegios elevados (`NET_ADMIN`, `NET_RAW`). Mitigar riesgos:

1. Usar un perfil AppArmor/seccomp restrictivo
2. Limitar `network_mode: host` solo al contenedor de captura
3. Monitorear con Falco (las reglas están en `monitoring/falco/falco-rules.yml`)
4. Rotar claves periódicamente

---

## 5. Monitoreo y observabilidad

### Prometheus — métricas

URL: http://localhost:9090 (o el puerto configurado en producción)

Endpoints de métricas expuestos por cada microservicio:
- `http://api-gateway:8080/actuator/prometheus`
- `http://worker-analysis:8081/actuator/prometheus`
- `http://worker-alerts:8083/actuator/prometheus`
- `http://worker-osint:8084/actuator/prometheus`

### Grafana — dashboards

URL: http://localhost:3001

Credenciales: `admin` / `<GRAFANA_PASSWORD del .env>`

Dashboards disponibles:
- **NetWatch Overview:** eventos por severidad, alertas activas, throughput de mensajes
- **JVM Metrics:** heap, GC, threads de cada microservicio
- **RabbitMQ:** profundidad de colas, tasa de mensajes, consumers activos

### Loki — logs centralizados

Loki + Promtail recolectan automáticamente los logs de todos los contenedores Docker. Los logs se consultan desde Grafana usando el explorador de Loki.

Queries útiles:
```
# Ver logs de un servicio
{job="netwatch-api-gateway"}

# Filtrar por nivel ERROR
{job="netwatch-api-gateway"} |= "ERROR"

# Ver amenazas detectadas
{job="netwatch-worker-analysis"} |= "amenaza detectada"
```

### Falco — runtime security

Falco genera alertas cuando detecta comportamiento anómalo en los contenedores (definido en `monitoring/falco/falco-rules.yml`).

Reglas configuradas:
- Captura de red desde contenedor que no es worker-capture
- Escalada de privilegios (su/sudo) en contenedores NetWatch
- Escritura en directorios del sistema (`/etc`) desde contenedores NetWatch

---

## 6. Gestión de imágenes Docker

### Build manual de imágenes

```bash
# El contexto debe ser la raíz del proyecto (no la carpeta del servicio)
docker build \
  -f netwatch-api-gateway/Dockerfile \
  -t netwatch-api-gateway:1.0.0 \
  .

# Para todos los servicios
for service in netwatch-api-gateway netwatch-worker-analysis \
               netwatch-worker-capture netwatch-worker-alerts \
               netwatch-worker-osint; do
  docker build -f ${service}/Dockerfile -t ${service}:1.0.0 .
done
```

> **Importante:** El contexto de build es siempre el directorio raíz del proyecto (`.`), no la carpeta del servicio. Los Dockerfiles copian el `pom.xml` padre y los módulos necesarios para el build multi-módulo de Maven.

### Pipeline de publicación (GitHub Actions)

El workflow `deploy.yml` se activa con tags semver:

```bash
# Crear un tag de release
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# GitHub Actions automáticamente:
# 1. Compila todos los módulos
# 2. Publica imágenes a Docker Hub con tags: 1.0.0, 1.0, latest
```

Secrets requeridos en GitHub:
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN` (Access Token, no contraseña)

---

## 7. Backup y recuperación

### Backup de PostgreSQL

```bash
# Backup completo
docker compose exec postgres pg_dump \
  -U netwatch netwatch \
  > backup-$(date +%Y%m%d).sql

# Backup comprimido
docker compose exec postgres pg_dump \
  -U netwatch netwatch \
  | gzip > backup-$(date +%Y%m%d).sql.gz
```

### Restaurar backup

```bash
# Detener los servicios que escriben en la BD
docker compose stop api-gateway worker-analysis

# Restaurar
docker compose exec -i postgres psql \
  -U netwatch netwatch \
  < backup-20240101.sql

# Reiniciar
docker compose start api-gateway worker-analysis
```

### Backup de volúmenes Docker

```bash
# Backup del volumen de PostgreSQL
docker run --rm \
  -v proyectofinal_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres-data-$(date +%Y%m%d).tar.gz /data

# Backup de Grafana (dashboards y configuración)
docker run --rm \
  -v proyectofinal_grafana_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/grafana-data-$(date +%Y%m%d).tar.gz /data
```

---

## 8. Actualización del sistema

### Actualización con downtime mínimo

```bash
# 1. Descargar nuevas imágenes
git pull origin main
docker compose pull

# 2. Reconstruir imágenes locales
docker compose build

# 3. Reiniciar servicios uno por uno (evita downtime total)
docker compose up -d --no-deps api-gateway
docker compose up -d --no-deps worker-analysis
docker compose up -d --no-deps worker-alerts
docker compose up -d --no-deps worker-osint
# worker-capture: requiere reinicio del host o permisos especiales
```

### Rollback

```bash
# Volver a la versión anterior
git checkout v1.0.0

# Reconstruir y desplegar
docker compose build
docker compose up -d
```

### Migración de base de datos

Cuando se actualiza el esquema de la BD:

1. El esquema se gestiona con `spring.jpa.hibernate.ddl-auto=update` (desarrollo)
2. En producción, cambiar a `validate` y manejar migraciones manualmente o con Flyway/Liquibase
3. Siempre hacer backup antes de aplicar cambios de esquema
4. El archivo `infrastructure/sql/init.sql` solo se ejecuta al crear la BD por primera vez
