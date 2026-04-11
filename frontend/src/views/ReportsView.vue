<template>
  <div class="pt-20 px-6 pb-10 min-h-screen bg-netwatch-dark text-slate-200">
    <h1 class="text-2xl font-bold text-netwatch-accent mb-2">Reportes</h1>
    <p class="text-slate-400 text-sm mb-8">
      Descarga reportes de eventos y alertas en el formato que necesites.
    </p>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-8">

      <!-- ── Eventos ─────────────────────────────────────────────────────── -->
      <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-10 h-10 rounded-lg bg-blue-500/20 flex items-center justify-center text-blue-400 text-lg font-bold">E</div>
          <div>
            <h2 class="text-lg font-semibold text-white">Eventos de Red</h2>
            <p class="text-xs text-slate-400">Hasta 5 000 eventos más recientes</p>
          </div>
        </div>

        <p class="text-sm text-slate-400 mb-6">
          Incluye: IP origen/destino, puertos, protocolo, tipo de amenaza,
          severidad, geolocalización y estado de resolución.
        </p>

        <div class="space-y-3">
          <button
            v-for="fmt in formats"
            :key="'ev-' + fmt.key"
            @click="download('events', fmt.key)"
            :disabled="downloading['events-' + fmt.key]"
            class="w-full flex items-center justify-between px-4 py-3 rounded-lg border transition-all"
            :class="fmt.classes"
          >
            <div class="flex items-center gap-3">
              <span class="text-xl">{{ fmt.icon }}</span>
              <div class="text-left">
                <p class="font-medium text-sm">{{ fmt.label }}</p>
                <p class="text-xs opacity-70">{{ fmt.desc }}</p>
              </div>
            </div>
            <span v-if="downloading['events-' + fmt.key]" class="text-xs animate-pulse">Generando...</span>
            <span v-else class="text-xs opacity-60">Descargar</span>
          </button>
        </div>
      </div>

      <!-- ── Alertas ─────────────────────────────────────────────────────── -->
      <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-10 h-10 rounded-lg bg-red-500/20 flex items-center justify-center text-red-400 text-lg font-bold">A</div>
          <div>
            <h2 class="text-lg font-semibold text-white">Alertas de Seguridad</h2>
            <p class="text-xs text-slate-400">Hasta 5 000 alertas más recientes</p>
          </div>
        </div>

        <p class="text-sm text-slate-400 mb-6">
          Incluye: título, estado (OPEN/ACK/RESOLVED/FALSE_POSITIVE),
          evento asociado, detalles y fecha de creación.
        </p>

        <div class="space-y-3">
          <button
            v-for="fmt in formats"
            :key="'al-' + fmt.key"
            @click="download('alerts', fmt.key)"
            :disabled="downloading['alerts-' + fmt.key]"
            class="w-full flex items-center justify-between px-4 py-3 rounded-lg border transition-all"
            :class="fmt.classes"
          >
            <div class="flex items-center gap-3">
              <span class="text-xl">{{ fmt.icon }}</span>
              <div class="text-left">
                <p class="font-medium text-sm">{{ fmt.label }}</p>
                <p class="text-xs opacity-70">{{ fmt.desc }}</p>
              </div>
            </div>
            <span v-if="downloading['alerts-' + fmt.key]" class="text-xs animate-pulse">Generando...</span>
            <span v-else class="text-xs opacity-60">Descargar</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ── Historial de descargas ──────────────────────────────────────── -->
    <div v-if="history.length" class="mt-8 bg-netwatch-panel border border-netwatch-border rounded-xl p-6">
      <h2 class="text-base font-semibold text-white mb-4">Historial de descargas</h2>
      <div class="space-y-2">
        <div
          v-for="item in history"
          :key="item.id"
          class="flex items-center justify-between text-sm py-2 border-b border-netwatch-border last:border-0"
        >
          <div class="flex items-center gap-3">
            <span :class="item.ok ? 'text-green-400' : 'text-red-400'">{{ item.ok ? '✓' : '✗' }}</span>
            <span class="text-slate-300">{{ item.filename }}</span>
          </div>
          <span class="text-slate-500 text-xs">{{ item.time }}</span>
        </div>
      </div>
    </div>

    <!-- ── Error ───────────────────────────────────────────────────────── -->
    <div v-if="error" class="mt-4 bg-red-900/30 border border-red-700 rounded-lg p-4 text-red-400 text-sm">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { reportAPI } from '@/services/api'

const downloading = reactive({})
const history     = ref([])
const error       = ref(null)

const formats = [
  {
    key: 'json',
    label: 'JSON',
    icon: '{ }',
    desc: 'Formato estructurado para integración con herramientas SIEM',
    classes: 'border-blue-700/50 bg-blue-900/10 hover:bg-blue-900/30 text-blue-300'
  },
  {
    key: 'csv',
    label: 'CSV',
    icon: '⊞',
    desc: 'Compatible con Excel, LibreOffice Calc y herramientas de análisis',
    classes: 'border-green-700/50 bg-green-900/10 hover:bg-green-900/30 text-green-300'
  },
  {
    key: 'xml',
    label: 'XML',
    icon: '</>',
    desc: 'Formato estándar para intercambio con sistemas legacy y SOAP',
    classes: 'border-purple-700/50 bg-purple-900/10 hover:bg-purple-900/30 text-purple-300'
  }
]

async function download(type, format) {
  const key = `${type}-${format}`
  downloading[key] = true
  error.value = null

  try {
    const fn = type === 'events' ? reportAPI.downloadEvents : reportAPI.downloadAlerts
    const { data, headers } = await fn(format)

    const mime = {
      json: 'application/json',
      csv:  'text/csv',
      xml:  'application/xml'
    }[format] || 'application/octet-stream'

    const blob = new Blob([data], { type: mime })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    const date = new Date().toISOString().slice(0, 10)
    const filename = `netwatch-${type}-${date}.${format}`

    a.href     = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)

    history.value.unshift({
      id:       Date.now(),
      filename,
      ok:       true,
      time:     new Date().toLocaleTimeString()
    })
    if (history.value.length > 10) history.value.pop()

  } catch (e) {
    error.value = `Error al descargar ${type} en formato ${format}: ${e.message}`
    history.value.unshift({
      id:       Date.now(),
      filename: `netwatch-${type}-${format} (error)`,
      ok:       false,
      time:     new Date().toLocaleTimeString()
    })
  } finally {
    downloading[key] = false
  }
}
</script>
