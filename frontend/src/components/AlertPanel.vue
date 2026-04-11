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

        <!-- IP de origen + tipo de amenaza -->
        <div class="flex items-center gap-4 flex-wrap">
          <p v-if="alert.event?.srcIp" class="text-xs font-mono text-slate-400">
            IP: {{ alert.event.srcIp }}
          </p>
          <p v-if="alert.event?.threatType" class="text-xs font-mono text-slate-500">
            Amenaza: <span class="text-slate-300">{{ alert.event.threatType }}</span>
          </p>
          <p v-if="alert.event?.country" class="text-xs text-slate-500">
            País: <span class="text-slate-300">{{ alert.event.country }}</span>
          </p>
        </div>

        <!-- Acciones -->
        <div class="flex flex-wrap gap-3 pt-1 items-center">
          <template v-if="canAct && alert.status === 'OPEN'">
            <button @click="$emit('acknowledge', alert.id)"
              class="text-xs text-yellow-400 hover:underline">
              Reconocer
            </button>
            <button @click="$emit('resolve', alert.id)"
              class="text-xs text-green-400 hover:underline">
              Resolver
            </button>
          </template>
          <!-- Botón de remediación -->
          <button
            v-if="alert.event?.threatType"
            @click="toggleRemediation(alert.id, alert.event.threatType)"
            class="text-xs text-netwatch-accent hover:underline ml-auto"
          >
            {{ activeRemediation === alert.id ? 'Ocultar guía' : 'Ver guía de remediación' }}
          </button>
        </div>

        <!-- Panel de remediación desplegable -->
        <RemediationPanel
          v-if="activeRemediation === alert.id && remediationData"
          :info="remediationData"
          :closable="true"
          @close="activeRemediation = null"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { remediationAPI } from '@/services/api'
import RemediationPanel from '@/components/RemediationPanel.vue'

defineProps({
  alerts:  { type: Array,   default: () => [] },
  loading: { type: Boolean, default: false },
  canAct:  { type: Boolean, default: false }
})

defineEmits(['acknowledge', 'resolve'])

const activeRemediation = ref(null)
const remediationData   = ref(null)

async function toggleRemediation(alertId, threatType) {
  if (activeRemediation.value === alertId) {
    activeRemediation.value = null
    remediationData.value   = null
    return
  }
  try {
    const { data } = await remediationAPI.getByThreatType(threatType)
    remediationData.value   = data
    activeRemediation.value = alertId
  } catch (e) {
    console.error('Error cargando remediación:', e)
  }
}

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
