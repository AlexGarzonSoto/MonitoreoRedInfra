# Diagramas de Arquitectura — NetWatch

Repositorio: `AlexGarzonSoto/MonitoreoRedInfra` · rama `main`

Los diagramas están escritos en **PlantUML** (`.puml`). Se renderizan automáticamente
abajo gracias al proxy público de plantuml.com. Si el proxy tarda, abre el `.puml`
en VS Code con la extensión **PlantUML** (jebbs) o en <https://www.plantuml.com/plantuml/uml/>.

---

## 1. Diagrama de Componentes

Visión general de todos los microservicios, flujos de mensajes y servicios externos.

![Diagrama de componentes](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AlexGarzonSoto/MonitoreoRedInfra/main/docs/architecture/component-diagram.puml)

Fuente: [component-diagram.puml](component-diagram.puml)

---

## 2. Diagrama de Despliegue

Contenedores Docker, red `netwatch-net`, volúmenes y mapeo de puertos.

![Diagrama de despliegue](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AlexGarzonSoto/MonitoreoRedInfra/main/docs/architecture/deployment-diagram.puml)

Fuente: [deployment-diagram.puml](deployment-diagram.puml)

---

## 3. Diagrama de Secuencia — Autenticación JWT

Flujo completo: login → emisión de tokens → request autenticado → refresh → logout.

![Diagrama de secuencia](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AlexGarzonSoto/MonitoreoRedInfra/main/docs/architecture/sequence-auth.puml)

Fuente: [sequence-auth.puml](sequence-auth.puml)

---

## 4. Diagrama de Casos de Uso

Actores ADMIN, ANALYST y VIEWER con sus 22 casos de uso y relaciones de herencia.

![Diagrama de casos de uso](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AlexGarzonSoto/MonitoreoRedInfra/main/docs/architecture/use-cases.puml)

Fuente: [use-cases.puml](use-cases.puml)

---

## 5. Modelo de Amenazas — OWASP Threat Dragon

El archivo [`threat-model.json`](threat-model.json) contiene el modelo de amenazas en formato
**OWASP Threat Dragon v2.0** con:

- DFD Nivel 0: visión general del sistema
- DFD Nivel 1: flujo detallado de detección de amenazas
- 10 amenazas STRIDE categorizadas (Spoofing, Tampering, Repudiation, Information Disclosure, DoS, EoP)

**Para visualizarlo:**
1. Abre <https://www.threatdragon.com/> (gratuito, sin registro)
2. Elige **Open Existing Threat Model**
3. Carga el archivo `threat-model.json`

| ID | Categoría STRIDE | Componente | Severidad |
|----|-----------------|-----------|-----------|
| T-01 | Spoofing | API Gateway (JWT) | Alta |
| T-02 | Elevation of Privilege | API Gateway (RBAC) | Alta |
| T-03 | Repudiation | PostgreSQL | Media |
| T-04 | Information Disclosure | API Gateway (respuestas) | Media |
| T-05 | Tampering | API Gateway (entrada) | Alta |
| T-06 | Tampering | RabbitMQ (mensajes) | Alta |
| T-07 | Denial of Service | Worker Captura | Alta |
| T-08 | Elevation of Privilege | Worker Captura (privilegios) | Crítica |
| T-09 | Information Disclosure | Worker OSINT (GeoIP) | Baja |
| T-10 | Information Disclosure | Worker Alertas (SMTP) | Baja |
