variable "dockerhub_username" {
  description = "Usuario de Docker Hub donde están publicadas las imágenes de NetWatch"
  type        = string
}

variable "image_tag" {
  description = "Tag de las imágenes Docker a desplegar"
  type        = string
  default     = "latest"
}

variable "postgres_password" {
  description = "Contraseña para PostgreSQL"
  type        = string
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "Contraseña para RabbitMQ"
  type        = string
  sensitive   = true
}

variable "redis_password" {
  description = "Contraseña para Valkey (Redis-compatible)"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secreto HMAC para firmar access tokens JWT (mínimo 64 caracteres)"
  type        = string
  sensitive   = true
}

variable "jwt_refresh_secret" {
  description = "Secreto HMAC para firmar refresh tokens JWT (mínimo 64 caracteres)"
  type        = string
  sensitive   = true
}

variable "grafana_password" {
  description = "Contraseña del admin de Grafana"
  type        = string
  sensitive   = true
  default     = "NetWatch2024!"
}

variable "netwatch_network" {
  description = "Nombre de la red Docker de NetWatch"
  type        = string
  default     = "netwatch-net"
}

variable "capture_interface" {
  description = "Interfaz de red para captura de paquetes"
  type        = string
  default     = "eth0"
}
