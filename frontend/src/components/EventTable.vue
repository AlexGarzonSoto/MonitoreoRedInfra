<template>
  <!-- Cargando -->
  <div v-if="loading" class="flex justify-center items-center py-12 text-slate-400 text-sm">
    Cargando eventos...
  </div>

  <!-- Sin datos -->
  <div v-else-if="!events.length" class="flex justify-center items-center py-12 text-slate-500 text-sm">
    No se encontraron eventos
  </div>

  <!-- Tabla -->
  <div v-else class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead>
        <tr class="text-slate-400 text-xs uppercase tracking-wider border-b border-netwatch-border">
          <th class="text-left py-3 px-4">Timestamp</th>
          <th class="text-left py-3 px-4">IP Origen</th>
          <th class="text-left py-3 px-4">Tipo</th>
          <th class="text-left py-3 px-4">Severidad</th>
          <th class="text-left py-3 px-4">Estado</th>
          <th class="text-left py-3 px-4">Acción</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-netwatch-border">
        <tr v-for="event in events" :key="event.id"
            class="hover:bg-netwatch-dark/50 transition-colors">
          <td class="py-3 px-4 text-slate-400 font-mono text-xs whitespace-nowrap">
            {{ formatDate(event.timestamp) }}
          </td>
          <td class="py-3 px-4 font-mono text-xs">{{ event.srcIp }}</td>
          <td class="py-3 px-4 text-xs">{{ event.threatType }}</td>
          <td class="py-3 px-4">
            <span :class="['px-2 py-0.5 rounded text-xs font-semibold', severityClass(event.severity)]">
              {{ event.severity }}
            </span>
          </td>
          <td class="py-3 px-4 text-xs">
            <span :class="event.resolved ? 'text-green-400' : 'text-yellow-400'">
              {{ event.resolved ? 'Resuelto' : 'Pendiente' }}
            </span>
          </td>
          <td class="py-3 px-4">
            <button v-if="!event.resolved"
              @click="$emit('resolve', event.id)"
              class="text-xs text-netwatch-accent hover:underline">
              Resolver
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  events:  { type: Array,   default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['resolve'])

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleString('es-CO', {
    dateStyle: 'short', timeStyle: 'medium'
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
