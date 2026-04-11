<template>
  <div class="pt-20 px-6 pb-10 min-h-screen bg-netwatch-dark text-slate-200">
    <h1 class="text-2xl font-bold text-netwatch-accent mb-2">Configuración</h1>
    <p class="text-slate-400 text-sm mb-8">Gestión de la captura de paquetes y configuración del sistema.</p>

    <!-- ── Estado de captura ─────────────────────────────────────────────── -->
    <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-white">Estado de captura</h2>
        <button @click="loadInterfaces" class="text-xs text-netwatch-accent hover:underline">
          Actualizar
        </button>
      </div>

      <div v-if="loading" class="text-center py-8 text-slate-500">Cargando...</div>

      <div v-else-if="status" class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-netwatch-dark rounded-lg p-4 text-center">
          <p class="text-xs text-slate-400 mb-1">Estado</p>
          <span
            class="px-3 py-1 rounded-full text-xs font-bold"
            :class="status.captureRunning ? 'bg-green-900/50 text-green-400' : 'bg-slate-700 text-slate-400'"
          >
            {{ status.captureRunning ? 'ACTIVO' : 'DETENIDO' }}
          </span>
        </div>
        <div class="bg-netwatch-dark rounded-lg p-4 text-center">
          <p class="text-xs text-slate-400 mb-1">Interfaz actual</p>
          <p class="text-white font-mono font-bold">{{ status.current || 'eth0' }}</p>
        </div>
        <div class="bg-netwatch-dark rounded-lg p-4 text-center">
          <p class="text-xs text-slate-400 mb-1">Modo promiscuo</p>
          <p class="text-white">{{ status.promiscuousMode ? 'Activado' : 'Desactivado' }}</p>
        </div>
        <div class="bg-netwatch-dark rounded-lg p-4 text-center">
          <p class="text-xs text-slate-400 mb-1">Worker</p>
          <span
            class="text-xs"
            :class="status.workerStatus === 'no disponible' ? 'text-yellow-400' : 'text-green-400'"
          >
            {{ status.workerStatus === 'no disponible' ? 'Simulación' : 'Conectado' }}
          </span>
        </div>
      </div>
    </div>

    <!-- ── Selección de interfaz ─────────────────────────────────────────── -->
    <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-6 mb-6" v-if="auth.isAdmin">
      <h2 class="text-lg font-semibold text-white mb-4">Interfaz de red</h2>
      <p class="text-sm text-slate-400 mb-4">
        Selecciona la interfaz en la que el worker de captura escuchará el tráfico de red.
        Requiere rol ADMIN.
      </p>

      <div class="flex gap-3 flex-wrap mb-4">
        <button
          v-for="iface in interfaces"
          :key="iface.name"
          @click="selectedInterface = iface.name"
          class="px-4 py-2 rounded-lg border text-sm font-mono transition-all"
          :class="selectedInterface === iface.name
            ? 'border-netwatch-accent bg-netwatch-accent/20 text-netwatch-accent'
            : 'border-netwatch-border bg-netwatch-dark text-slate-300 hover:border-slate-500'"
        >
          {{ iface.name }}
          <span class="ml-1 text-xs opacity-50 font-sans">{{ iface.description }}</span>
        </button>
      </div>

      <div class="flex gap-3 items-center">
        <button
          @click="applyInterface"
          :disabled="!selectedInterface || saving"
          class="px-6 py-2 rounded-lg bg-netwatch-accent text-black font-semibold text-sm disabled:opacity-50 hover:brightness-110 transition-all"
        >
          {{ saving ? 'Aplicando...' : 'Aplicar interfaz' }}
        </button>
        <span v-if="saveResult" class="text-sm" :class="saveResult.ok ? 'text-green-400' : 'text-red-400'">
          {{ saveResult.message }}
        </span>
      </div>
    </div>

    <!-- ── Control de captura ────────────────────────────────────────────── -->
    <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-6" v-if="auth.isAdmin">
      <h2 class="text-lg font-semibold text-white mb-4">Control de captura</h2>
      <div class="flex gap-4">
        <button
          @click="controlCapture('start')"
          :disabled="controlling"
          class="px-5 py-2 rounded-lg bg-green-700 hover:bg-green-600 text-white text-sm font-medium disabled:opacity-50 transition-all"
        >
          Iniciar captura
        </button>
        <button
          @click="controlCapture('stop')"
          :disabled="controlling"
          class="px-5 py-2 rounded-lg bg-red-800 hover:bg-red-700 text-white text-sm font-medium disabled:opacity-50 transition-all"
        >
          Detener captura
        </button>
      </div>
      <p v-if="controlResult" class="mt-3 text-sm" :class="controlResult.ok ? 'text-green-400' : 'text-red-400'">
        {{ controlResult.message }}
      </p>
    </div>

    <!-- ── Lista de interfaces disponibles ──────────────────────────────── -->
    <div class="mt-6 bg-netwatch-panel border border-netwatch-border rounded-xl p-6">
      <h2 class="text-lg font-semibold text-white mb-4">Interfaces detectadas</h2>
      <div v-if="interfaces.length === 0" class="text-slate-500 text-sm">
        No hay interfaces disponibles.
      </div>
      <table v-else class="w-full text-sm">
        <thead>
          <tr class="text-left text-slate-400 border-b border-netwatch-border">
            <th class="pb-2">Nombre</th>
            <th class="pb-2">Descripción</th>
            <th class="pb-2">Fuente</th>
            <th class="pb-2">Activa</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="iface in interfaces"
            :key="iface.name"
            class="border-b border-netwatch-border/50 hover:bg-netwatch-dark/50"
          >
            <td class="py-2 font-mono text-netwatch-accent">{{ iface.name }}</td>
            <td class="py-2 text-slate-300">{{ iface.description }}</td>
            <td class="py-2 text-slate-500 text-xs">{{ iface.source }}</td>
            <td class="py-2">
              <span v-if="status && iface.name === status.current"
                    class="px-2 py-0.5 rounded text-xs bg-green-900/50 text-green-400">
                Activa
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { captureAPI } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth              = useAuthStore()
const loading           = ref(true)
const saving            = ref(false)
const controlling       = ref(false)
const interfaces        = ref([])
const status            = ref(null)
const selectedInterface = ref('')
const saveResult        = ref(null)
const controlResult     = ref(null)

async function loadInterfaces() {
  loading.value = true
  try {
    const { data } = await captureAPI.listInterfaces()
    interfaces.value        = data.interfaces || []
    selectedInterface.value = data.current    || 'eth0'
    status.value            = data
  } catch (e) {
    interfaces.value = [
      { name: 'eth0', description: 'Interfaz principal', source: 'fallback' },
      { name: 'lo',   description: 'Loopback',           source: 'fallback' }
    ]
    status.value = { current: 'eth0', captureRunning: false, promiscuousMode: true, workerStatus: 'no disponible' }
  } finally {
    loading.value = false
  }
}

async function applyInterface() {
  saving.value    = true
  saveResult.value = null
  try {
    const { data } = await captureAPI.changeInterface(selectedInterface.value)
    saveResult.value = { ok: true, message: `Interfaz cambiada a: ${data.current}` }
    await loadInterfaces()
  } catch (e) {
    saveResult.value = { ok: false, message: `Error: ${e.response?.data?.error || e.message}` }
  } finally {
    saving.value = false
  }
}

async function controlCapture(action) {
  controlling.value  = true
  controlResult.value = null
  try {
    const fn = action === 'start' ? captureAPI.start : captureAPI.stop
    const { data } = await fn()
    controlResult.value = { ok: true, message: data.message }
    await loadInterfaces()
  } catch (e) {
    controlResult.value = { ok: false, message: `Error: ${e.response?.data?.error || e.message}` }
  } finally {
    controlling.value = false
  }
}

onMounted(loadInterfaces)
</script>
