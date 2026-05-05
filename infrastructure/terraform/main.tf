terraform {
  required_version = ">= 1.7.0"
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  host = "unix:///var/run/docker.sock"
}

# ── Red interna ───────────────────────────────────────────────────────────────
resource "docker_network" "netwatch" {
  name   = var.netwatch_network
  driver = "bridge"
}

# ── Volúmenes persistentes ────────────────────────────────────────────────────
resource "docker_volume" "postgres_data"  { name = "postgres_data" }
resource "docker_volume" "rabbitmq_data"  { name = "rabbitmq_data" }
resource "docker_volume" "valkey_data"    { name = "valkey_data" }
resource "docker_volume" "grafana_data"   { name = "grafana_data" }
resource "docker_volume" "loki_data"      { name = "loki_data" }

# ── PostgreSQL + TimescaleDB ──────────────────────────────────────────────────
resource "docker_container" "postgres" {
  name  = "netwatch-postgres"
  image = "timescale/timescaledb:latest-pg15"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  env = [
    "POSTGRES_DB=netwatch",
    "POSTGRES_USER=netwatch",
    "POSTGRES_PASSWORD=${var.postgres_password}"
  ]

  volumes {
    volume_name    = docker_volume.postgres_data.name
    container_path = "/var/lib/postgresql/data"
  }

  volumes {
    host_path      = "${path.module}/../../infrastructure/sql/init.sql"
    container_path = "/docker-entrypoint-initdb.d/init.sql"
    read_only      = true
  }

  restart = "unless-stopped"

  healthcheck {
    test         = ["CMD-SHELL", "pg_isready -U netwatch"]
    interval     = "10s"
    timeout      = "5s"
    retries      = 5
    start_period = "30s"
  }
}

# ── RabbitMQ ──────────────────────────────────────────────────────────────────
resource "docker_container" "rabbitmq" {
  name  = "netwatch-rabbitmq"
  image = "rabbitmq:3.12-management-alpine"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  env = [
    "RABBITMQ_DEFAULT_USER=netwatch",
    "RABBITMQ_DEFAULT_PASS=${var.rabbitmq_password}",
    "RABBITMQ_DEFAULT_VHOST=netwatch"
  ]

  volumes {
    volume_name    = docker_volume.rabbitmq_data.name
    container_path = "/var/lib/rabbitmq"
  }

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "rabbitmq-diagnostics", "ping"]
    interval = "10s"
    timeout  = "10s"
    retries  = 10
  }
}

# ── Valkey (Redis-compatible) ─────────────────────────────────────────────────
resource "docker_container" "valkey" {
  name    = "netwatch-valkey"
  image   = "valkey/valkey:7.2-alpine"
  command = ["valkey-server", "--requirepass", var.redis_password]

  networks_advanced {
    name = docker_network.netwatch.name
  }

  volumes {
    volume_name    = docker_volume.valkey_data.name
    container_path = "/data"
  }

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "valkey-cli", "-a", var.redis_password, "ping"]
    interval = "10s"
    timeout  = "5s"
    retries  = 5
  }
}

# ── API Gateway ───────────────────────────────────────────────────────────────
resource "docker_container" "api_gateway" {
  name  = "netwatch-api-gateway"
  image = "${var.dockerhub_username}/netwatch-api-gateway:${var.image_tag}"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  ports {
    internal = 8080
    external = 8080
  }

  env = [
    "DB_HOST=netwatch-postgres",
    "DB_PORT=5432",
    "DB_NAME=netwatch",
    "DB_USER=netwatch",
    "DB_PASS=${var.postgres_password}",
    "RABBITMQ_HOST=netwatch-rabbitmq",
    "RABBITMQ_USER=netwatch",
    "RABBITMQ_PASS=${var.rabbitmq_password}",
    "RABBITMQ_VHOST=netwatch",
    "JWT_SECRET=${var.jwt_secret}",
    "JWT_REFRESH_SECRET=${var.jwt_refresh_secret}"
  ]

  restart = "unless-stopped"

  depends_on = [
    docker_container.postgres,
    docker_container.rabbitmq
  ]
}

# ── Frontend ──────────────────────────────────────────────────────────────────
resource "docker_container" "frontend" {
  name  = "netwatch-frontend"
  image = "${var.dockerhub_username}/netwatch-frontend:${var.image_tag}"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  ports {
    internal = 80
    external = 3000
  }

  restart = "unless-stopped"

  depends_on = [docker_container.api_gateway]
}

# ── Prometheus ────────────────────────────────────────────────────────────────
resource "docker_container" "prometheus" {
  name  = "netwatch-prometheus"
  image = "prom/prometheus:latest"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  volumes {
    host_path      = "${path.module}/../../monitoring/prometheus/prometheus.yml"
    container_path = "/etc/prometheus/prometheus.yml"
    read_only      = true
  }

  command = [
    "--config.file=/etc/prometheus/prometheus.yml",
    "--storage.tsdb.retention.time=15d"
  ]

  restart = "unless-stopped"
}

# ── Grafana ───────────────────────────────────────────────────────────────────
resource "docker_container" "grafana" {
  name  = "netwatch-grafana"
  image = "grafana/grafana-oss:latest"

  networks_advanced {
    name = docker_network.netwatch.name
  }

  ports {
    internal = 3000
    external = 3001
  }

  env = [
    "GF_SECURITY_ADMIN_PASSWORD=${var.grafana_password}",
    "GF_USERS_ALLOW_SIGN_UP=false",
    "GF_ANALYTICS_REPORTING_ENABLED=false"
  ]

  volumes {
    volume_name    = docker_volume.grafana_data.name
    container_path = "/var/lib/grafana"
  }

  restart    = "unless-stopped"
  depends_on = [docker_container.prometheus]
}
