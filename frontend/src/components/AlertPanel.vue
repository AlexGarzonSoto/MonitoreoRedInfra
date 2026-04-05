<template>
  <div>
    <!-- Cargando -->
    <div v-if="loading" class="flex justify-center py-12 text-slate-400 text-sm">
      Cargando alertas...
    </div>

    <!-- Sin datos -->
    <div v-else-if="!alerts.length" class="flex justify-center py-12 text-slate-500 text-sm">
      No hay alertas en este estado
    </div>

    <!-- Lista de alertas -->
    <div v-else class="space-y-3">
      <div v-for="alert in alerts" :key="alert.id"
           class="bg-netwatch-panel border border-netwatch-border rounded-xl p-4 space-y-2">
        <!-- Cabecera -->
        <div class="flex items-start justify-between gap-2">
          <div>
            <span :class="['inline-block px-2 py-0.5 rounded text-xs font-semibold mr-2',
              severityClass(alert.event?.severity)]">
              {{ alert.event?.severity ?? '—' }}
            </span>
            <span class="text-sm font-medium text-slate-100">{{ alert.title }}</span>
          </div>
          <span class="text-xs text-slate-500 whitespace-nowrap">
            {{ formatDate(alert.createdAt) }}
          </span>
        </div>

        <!-- Detalles -->
        <p v-if="alert.details" class="text-xs text-slate-400 line-clamp-2">{{ alert.details }}</p>

        <!-- IP de origen -->
        <p v-if="alert.event?.srcIp" class="text-xs font-mono text-slate-400">
          IP: {{ alert.event.srcIp }}
        </p>

        <!-- Acciones -->
        <div v-if="canAct && alert.status === 'OPEN'" class="flex gap-3 pt-1">
          <button @click="$emit('acknowledge', alert.id)"
            class="text-xs text-yellow-400 hover:underline">
            Reconocer
          </button>
          <button @click="$emit('resolve', alert.id)"
            class="text-xs text-green-400 hover:underline">
            Resolver
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  alerts:  { type: Array,   default: () => [] },
  loading: { type: Boolean, default: false },
  canAct:  { type: Boolean, default: false }
})

defineEmits(['acknowledge', 'resolve'])

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleString('es-CO', {
    dateStyle: 'short', timeStyle: 'short'
  })
}

function severityClass(severity) {
  return {
    CRITICAL: 'bg-red-900/60 text-red-300',
    HIGH:     'bg-orange-900/60 text-orange-300',
    MEDIUM:   'bg-yellow-900/60 text-yellow-300',
    LOW:      'bg-blue-900/60 text-blue-300',
    INFO:     'bg-slate-700 text-slate-300'
  }[severity] ?? 'bg-slate-700 text-slate-300'
}
</script>
