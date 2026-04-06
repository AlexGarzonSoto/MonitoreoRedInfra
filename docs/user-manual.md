# Manual de Usuario — NetWatch Dashboard

## Tabla de contenidos

1. [Introducción](#1-introducción)
2. [Acceso al sistema](#2-acceso-al-sistema)
3. [Roles y permisos](#3-roles-y-permisos)
4. [Dashboard principal](#4-dashboard-principal)
5. [Gestión de eventos de amenaza](#5-gestión-de-eventos-de-amenaza)
6. [Gestión de alertas](#6-gestión-de-alertas)
7. [API REST — referencia rápida](#7-api-rest--referencia-rápida)
8. [Flujos de trabajo recomendados](#8-flujos-de-trabajo-recomendados)
9. [Preguntas frecuentes](#9-preguntas-frecuentes)

---

## 1. Introducción

**NetWatch** es un sistema de monitoreo de amenazas en infraestructura de red. Captura tráfico de red en tiempo real, analiza los paquetes para detectar patrones maliciosos y genera alertas que los analistas de seguridad pueden gestionar desde un dashboard web.

### Tipos de amenazas detectadas

| Tipo | Descripción | Severidad típica |
|------|-------------|-----------------|
| `PORT_SCAN` | Escaneo de múltiples puertos desde una misma IP | HIGH |
| `BRUTE_FORCE` | Intentos repetidos en puertos sensibles (SSH, RDP, etc.) | CRITICAL |
| `SYN_FLOOD` | Flood de paquetes SYN (ataque DoS) | CRITICAL |
| `DNS_TUNNELING` | Paquetes DNS con payload excesivo (posible exfiltración) | HIGH |
| `MALWARE_C2` | Conexiones a puertos conocidos de Command & Control | CRITICAL |
| `DATA_EXFILTRATION` | Transferencia masiva de datos sospechosa | HIGH |
| `NORMAL` | Tráfico normal sin amenaza detectada | INFO/LOW |

---

## 2. Acceso al sistema

### URLs de acceso

| Componente | URL |
|-----------|-----|
| Dashboard Web | http://localhost:3000 |
| API REST | http://localhost:8080 |
| Grafana (métricas) | http://localhost:3001 |

### Iniciar sesión

1. Navegar a `http://localhost:3000`
2. Ingresar credenciales en el formulario de login
3. El sistema emite un **access token** (válido 30 minutos) y un **refresh token** (válido 7 días)
4. El access token se renueva automáticamente al expirar

### Credenciales iniciales

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@netwatch.local | NetWatch2024! | ADMIN |
| analista@netwatch.local | NetWatch2024! | ANALYST |

> **Seguridad:** Cambiar las contraseñas predeterminadas en el primer acceso a producción.

### Cerrar sesión

Hacer clic en el botón "Salir" en la barra de navegación superior. El token se invalida en el servidor.

---

## 3. Roles y permisos

### Tabla de permisos

| Acción | VIEWER | ANALYST | ADMIN |
|--------|--------|---------|-------|
| Ver eventos de amenaza | ✓ | ✓ | ✓ |
| Ver alertas | ✓ | ✓ | ✓ |
| Resolver eventos | — | ✓ | ✓ |
| Gestionar alertas (acknowledge/resolve) | — | ✓ | ✓ |
| Marcar como falso positivo | — | ✓ | ✓ |
| Gestionar usuarios | — | — | ✓ |

### Descripción de roles

**VIEWER:** Solo lectura. Puede visualizar eventos y alertas pero no puede modificar su estado.

**ANALYST:** Puede gestionar el ciclo de vida completo de eventos y alertas. Rol recomendado para el equipo de respuesta a incidentes.

**ADMIN:** Acceso total, incluyendo gestión de usuarios del sistema.

---

## 4. Dashboard principal

El dashboard se actualiza automáticamente cada **10 segundos**.

### Panel de estadísticas

En la parte superior se muestran 4 tarjetas con contadores en tiempo real:

| Tarjeta | Descripción |
|---------|-------------|
| **Total eventos** | Número total de amenazas detectadas |
| **Sin resolver** | Eventos que aún no han sido atendidos |
| **Críticos** | Eventos con severidad CRITICAL |
| **Altos** | Eventos con severidad HIGH |

### Gráfico de amenazas

Muestra la distribución de amenazas por tipo en un gráfico de barras. Permite identificar de un vistazo qué tipo de ataque está siendo más frecuente.

### Tabla de eventos recientes

Lista los últimos 10 eventos detectados con:
- Timestamp
- IP de origen
- Tipo de amenaza
- Severidad (con código de color)
- Estado (resuelto/pendiente)
- Botón de acción rápida

---

## 5. Gestión de eventos de amenaza

### Visualizar eventos

Navegar a la sección **Eventos** en el menú lateral.

La tabla muestra todos los eventos con paginación (50 por página por defecto).

### Filtrar eventos

Se pueden aplicar filtros por:

| Filtro | Valores posibles |
|--------|-----------------|
| **Severidad** | INFO, LOW, MEDIUM, HIGH, CRITICAL |
| **Tipo de amenaza** | PORT_SCAN, BRUTE_FORCE, SYN_FLOOD, DNS_TUNNELING, MALWARE_C2, DATA_EXFILTRATION, NORMAL |
| **IP de origen** | Dirección IP exacta (ej: 192.168.1.10) |
| **Rango de fechas** | Desde / Hasta |

### Detalle de un evento

Hacer clic en cualquier fila para ver el detalle completo:

- IPs de origen y destino, puertos, protocolo
- Descripción de la amenaza
- Datos OSINT enriquecidos (país, ciudad, ISP, coordenadas, puntuación de abuso)
- Timestamp de detección
- Estado (resuelto o pendiente)

### Resolver un evento

1. Localizar el evento en la tabla
2. Hacer clic en **"Resolver"** (requiere rol ANALYST o ADMIN)
3. El evento queda marcado como resuelto y ya no aparece en los contadores de "sin resolver"

---

## 6. Gestión de alertas

Las alertas se generan automáticamente para eventos con severidad **HIGH** y **CRITICAL**.

### Ciclo de vida de una alerta

```
OPEN → ACKNOWLEDGED → RESOLVED
  ↓
FALSE_POSITIVE (si la amenaza fue descartada)
```

### Estados de las alertas

| Estado | Descripción | Color |
|--------|-------------|-------|
| **OPEN** | Alerta nueva, sin atender | Rojo |
| **ACKNOWLEDGED** | Un analista tomó nota del incidente | Amarillo |
| **RESOLVED** | El incidente fue investigado y cerrado | Verde |
| **FALSE_POSITIVE** | La alerta fue un falso positivo, descartada | Gris |

### Flujo de trabajo con alertas

1. **Revisar alertas OPEN:** Navegar a la sección **Alertas** → filtro por estado "OPEN"
2. **Reconocer el incidente:** Hacer clic en **"Reconocer"** para mover a ACKNOWLEDGED
3. **Investigar:** Revisar el evento asociado, consultar los datos OSINT
4. **Cerrar:** Marcar como **"Resolver"** (incidente confirmado y mitigado) o **"Falso positivo"**

### Filtrar alertas por estado

En la vista de Alertas, usar el selector de estado:
- `OPEN` — alertas pendientes (vista por defecto)
- `ACKNOWLEDGED` — en investigación
- `RESOLVED` — cerradas
- `FALSE_POSITIVE` — descartadas

---

## 7. API REST — referencia rápida

La API REST está disponible en `http://localhost:8080`. Todos los endpoints (excepto login) requieren autenticación JWT.

### Autenticación

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"analista@netwatch.local","password":"NetWatch2024!"}'

# Respuesta:
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "role": "ANALYST"
}
```

### Usar el token

```bash
# Incluir en todas las peticiones:
curl -H "Authorization: Bearer <accessToken>" http://localhost:8080/api/v1/events
```

### Endpoints principales

#### Eventos

```bash
# Listar eventos (paginado)
GET /api/v1/events?page=0&size=50&sort=timestamp,desc

# Filtrar eventos
GET /api/v1/events?severity=HIGH&threatType=BRUTE_FORCE&srcIp=10.0.0.1

# Detalle de un evento
GET /api/v1/events/{id}

# Resolver un evento
PATCH /api/v1/events/{id}/resolve

# Resumen estadístico
GET /api/v1/events/stats/summary
```

#### Alertas

```bash
# Listar alertas (filtradas por estado)
GET /api/v1/alerts?status=OPEN&page=0&size=20

# Reconocer una alerta
PATCH /api/v1/alerts/{id}/acknowledge

# Resolver una alerta
PATCH /api/v1/alerts/{id}/resolve

# Marcar como falso positivo
PATCH /api/v1/alerts/{id}/false-positive

# Resumen por estado
GET /api/v1/alerts/stats/summary
```

---

## 8. Flujos de trabajo recomendados

### Turno de guardia — revisión matutina

1. Abrir el **Dashboard** y revisar los contadores
2. Si hay eventos CRITICAL nuevos → acceder a **Alertas** → filtrar OPEN
3. Reconocer (ACKNOWLEDGED) todas las alertas que se van a investigar
4. Revisar los datos OSINT de las IPs involucradas
5. Resolver o marcar como falso positivo según el análisis
6. Documentar las conclusiones en el sistema de tickets corporativo

### Investigar un posible ataque de fuerza bruta

1. En **Eventos** → filtrar por `threatType=BRUTE_FORCE`
2. Identificar la IP de origen y los puertos destino
3. Ver el detalle del evento → revisar `abuseScore` (0-100, mayor = más sospechoso)
4. Si `abuseScore > 50` → considerar bloqueo en firewall
5. Resolver los eventos y documentar la acción tomada

### Investigar un posible port scan

1. En **Eventos** → filtrar por `threatType=PORT_SCAN`
2. Revisar la IP de origen → si es interna, puede ser una herramienta de inventario autorizada
3. Si es IP externa con alto `abuseScore` → posible reconocimiento previo a ataque
4. Crear ticket en el sistema ITSM con los detalles del evento

---

## 9. Preguntas frecuentes

**¿Por qué no veo eventos nuevos en el dashboard?**
El worker de captura (`netwatch-worker-capture`) necesita acceso `NET_RAW` al sistema operativo. En entornos de desarrollo puede estar en modo simulación. Verificar con:
```bash
docker compose logs netwatch-worker-capture
```

**¿Qué significa un `abuseScore` de 0?**
La IP no está en bases de datos de amenazas conocidas. Un score de 0 no garantiza que sea segura, solo que no ha sido reportada previamente.

**¿Puedo exportar los eventos?**
Actualmente la exportación se hace directamente por API. Ejemplo para exportar los últimos 1000 eventos:
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/events?size=1000&sort=timestamp,desc" \
  | python3 -m json.tool > eventos.json
```

**¿Con qué frecuencia se actualiza el dashboard?**
El dashboard hace polling automático cada 10 segundos al endpoint `/api/v1/events`.

**¿Por qué hay alertas con contadores en 0 para ACKNOWLEDGED, RESOLVED y FALSE_POSITIVE?**
Es comportamiento normal al inicio. Las alertas se crean como OPEN. Los contadores de otros estados suben cuando los analistas cambian el estado mediante los botones de la interfaz o los endpoints PATCH de la API.

**¿Cómo veo las métricas históricas?**
Acceder a Grafana en http://localhost:3001 con las credenciales configuradas en `GRAFANA_PASSWORD`. Los dashboards muestran métricas de los últimos 7 días por defecto.
