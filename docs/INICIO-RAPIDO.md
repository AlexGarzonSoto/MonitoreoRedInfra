# Guía de inicio rápido — NetWatch

Esta guía te lleva desde cero hasta tener NetWatch funcionando en tu computador, paso a paso, sin conocimientos técnicos previos.

**Tiempo estimado:** 15-20 minutos (la mayor parte es espera mientras descarga)

---

## Antes de empezar — ¿qué es NetWatch?

NetWatch es una aplicación que **monitorea tu red en tiempo real** y te avisa cuando detecta actividad sospechosa, como alguien intentando adivinar contraseñas, escanear tus puertos o realizar un ataque. Todo se visualiza en un panel web desde tu navegador.

---

## Lo que necesitas instalar (solo una vez)

### Docker

Docker es el programa que ejecuta NetWatch. Sin él no funciona nada.

**¿Cómo instalarlo?**

| Sistema operativo | Instrucciones |
|-------------------|---------------|
| Windows 10/11 | Descarga [Docker Desktop para Windows](https://docs.docker.com/desktop/install/windows-install/) e instálalo como cualquier programa |
| macOS | Descarga [Docker Desktop para Mac](https://docs.docker.com/desktop/install/mac-install/) e instálalo |
| Ubuntu / Debian / MX Linux | Abre una terminal y copia este comando: `curl -fsSL https://get.docker.com \| sh && sudo usermod -aG docker $USER` — luego **cierra sesión y vuelve a entrar** |

**¿Cómo sé que está instalado correctamente?**

Abre una terminal y escribe:
```
docker --version
```
Deberías ver algo como: `Docker version 26.x.x`

Si ves eso, Docker está listo.

---

## Paso 1 — Descargar NetWatch

Abre una terminal y copia estas dos líneas, una por una:

```bash
git clone https://github.com/AlexGarzonSoto/MonitoreoRedInfra.git
cd MonitoreoRedInfra
```

> **¿No tienes git?** En Windows puedes descargarlo desde [git-scm.com](https://git-scm.com/downloads). En Linux: `sudo apt install git`

Cuando termines, habrás descargado el proyecto en una carpeta llamada `MonitoreoRedInfra`.

---

## Paso 2 — Configurar (solo la primera vez)

Dentro de la carpeta del proyecto, ejecuta:

```bash
make configurar
```

Este comando hace dos cosas por ti de forma automática:
- Crea el archivo de configuración con todos los valores necesarios
- Genera claves de seguridad únicas para tu instalación

**Deberías ver al final:**
```
✓ Archivo .env creado con claves de seguridad generadas automáticamente
✓ Configuración lista.

  Próximo paso:
    make iniciar
```

> Si ves algún error en rojo, probablemente Docker no está instalado o no está ejecutándose. Asegúrate de que Docker Desktop esté abierto (en Windows/Mac) o que el servicio esté activo (en Linux).

---

## Paso 3 — Arrancar NetWatch

```bash
make iniciar
```

La **primera vez** este proceso descarga los componentes necesarios de internet. Dependiendo de tu conexión puede tardar entre 3 y 10 minutos. Las veces siguientes será mucho más rápido (menos de 1 minuto).

Verás muchas líneas de texto. Eso es normal, no es un error. Cuando termine verás:

```
✓ NetWatch arrancado.

  Espera 2 minutos y luego accede a:
    Dashboard:  http://localhost:3000
```

**Espera 2 minutos** para que todos los servicios internos terminen de arrancar, y luego abre tu navegador.

---

## Paso 4 — Abrir el panel de control

Abre tu navegador (Chrome, Firefox, Edge) y ve a:

```
http://localhost:3000
```

Verás la pantalla de inicio de sesión de NetWatch.

**Credenciales iniciales:**

| Campo | Valor |
|-------|-------|
| Usuario | `admin@netwatch.local` |
| Contraseña | `NetWatch2024!` |

---

## Lo que verás al entrar

Una vez dentro del panel verás:

- **Dashboard**: resumen con el número de amenazas detectadas, alertas activas y eventos recientes
- **Eventos**: tabla con cada paquete de red analizado y su clasificación
- **Alertas**: notificaciones de amenazas que requieren atención
- **Gráficas**: distribución de tipos de amenaza en el tiempo

Los datos empiezan a aparecer en los primeros minutos porque NetWatch genera tráfico de prueba automáticamente.

---

## Comandos del día a día

Una vez que tienes NetWatch instalado, estos son los únicos comandos que necesitas:

| Qué quieres hacer | Comando |
|-------------------|---------|
| Arrancar NetWatch | `make iniciar` |
| Parar NetWatch | `make detener` |
| Ver si todo funciona | `make estado` |
| Abrir el panel en el navegador | `make abrir` |
| Ver mensajes internos (para diagnóstico) | `make logs` |
| Ver todos los comandos disponibles | `make` |

---

## Solución de problemas frecuentes

### "No puedo abrir http://localhost:3000"

NetWatch puede tardar hasta 3 minutos en estar completamente listo. Espera un poco y recarga la página. Si sigue sin funcionar:

```bash
make estado
```

Revisa que todos los servicios digan `running` o `healthy`. Si alguno dice `exited`, ejecuta `make logs` para ver qué ocurrió.

---

### "Veo muchos errores en la terminal al iniciar"

Los primeros 2 minutos es normal ver mensajes de error mientras los servicios se conectan entre sí. Si después de 5 minutos el panel sigue sin abrir, ejecuta:

```bash
make logs
```

Busca líneas que digan `ERROR` en mayúsculas y comparte esos mensajes si necesitas ayuda.

---

### "Olvidé la contraseña de administrador"

Las credenciales iniciales siempre son:
- Usuario: `admin@netwatch.local`
- Contraseña: `NetWatch2024!`

Si las cambiaste y las olvidaste, la única forma de recuperar el acceso es limpiar los datos y empezar de nuevo:

```bash
make limpiar
make configurar
make iniciar
```

> **Advertencia:** `make limpiar` borra todos los datos guardados (eventos, alertas, historial).

---

### "Quiero parar NetWatch para no consumir recursos"

```bash
make detener
```

Esto para todos los servicios pero conserva todos tus datos. La próxima vez que ejecutes `make iniciar` todo estará exactamente como lo dejaste.

---

## Para las personas más curiosas

Si quieres entender en detalle cómo funciona NetWatch por dentro, los manuales técnicos están en la carpeta `docs/`:

| Manual | Contenido |
|--------|-----------|
| [Manual de Usuario](user-manual.md) | Cómo usar cada función del dashboard |
| [Manual de Desarrollo](development-manual.md) | Cómo modificar el código fuente |
| [Manual de Despliegue](deployment-manual.md) | Instalación avanzada en servidores |
| [Manual de Seguridad](security-manual.md) | Análisis de amenazas y controles |

---

## ¿Algo no funcionó?

Abre un issue en el repositorio del proyecto:
[github.com/AlexGarzonSoto/MonitoreoRedInfra/issues](https://github.com/AlexGarzonSoto/MonitoreoRedInfra/issues)

Incluye la salida del comando `make estado` y las últimas líneas de `make logs`.
