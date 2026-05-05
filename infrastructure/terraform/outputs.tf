output "frontend_url" {
  description = "URL del dashboard de NetWatch"
  value       = "http://localhost:3000"
}

output "grafana_url" {
  description = "URL del dashboard de Grafana (métricas)"
  value       = "http://localhost:3001"
}

output "api_gateway_url" {
  description = "URL base del API Gateway"
  value       = "http://localhost:8080"
}

output "rabbitmq_management_url" {
  description = "URL de la consola de administración de RabbitMQ"
  value       = "http://localhost:15672"
}

output "postgres_container_id" {
  description = "ID del contenedor de PostgreSQL"
  value       = docker_container.postgres.id
}

output "network_id" {
  description = "ID de la red Docker de NetWatch"
  value       = docker_network.netwatch.id
}
