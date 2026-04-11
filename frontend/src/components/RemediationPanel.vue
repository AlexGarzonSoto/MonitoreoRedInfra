<template>
  <div v-if="info" class="bg-netwatch-panel border rounded-xl p-5 mt-4"
       :class="borderColor">

    <!-- ── Cabecera ───────────────────────────────────────────────────────── -->
    <div class="flex items-start justify-between mb-4">
      <div>
        <div class="flex items-center gap-2 mb-1">
          <span class="text-xs font-mono px-2 py-0.5 rounded" :class="badgeColor">
            {{ info.riskLevel }}
          </span>
          <span class="text-xs text-slate-400">{{ info.mitreTechnique }}</span>
        </div>
        <h3 class="text-white font-semibold">{{ info.threatName }}</h3>
        <p class="text-sm text-slate-400 mt-1">{{ info.description }}</p>
      </div>
      <button v-if="closable" @click="$emit('close')"
              class="text-slate-500 hover:text-white text-lg leading-none ml-4">×</button>
    </div>

    <!-- ── Pasos de remediación ───────────────────────────────────────────── -->
    <div class="mb-4">
      <h4 class="text-sm font-semibold text-netwatch-accent mb-2">Pasos de remediación</h4>
      <ol class="space-y-2">
        <li
          v-for="(step, i) in info.remediationSteps"
          :key="i"
          class="flex gap-3 text-sm text-slate-300"
        >
          <span class="flex-shrink-0 w-6 h-6 rounded-full bg-netwatch-dark border border-netwatch-border text-center text-xs leading-6 text-netwatch-accent font-bold">
            {{ i + 1 }}
          </span>
          <span>{{ step }}</span>
        </li>
      </ol>
    </div>

    <!-- ── CVEs locales ───────────────────────────────────────────────────── -->
    <div v-if="info.relatedCves && info.relatedCves.length" class="mb-4">
      <h4 class="text-sm font-semibold text-yellow-400 mb-2">CVEs relacionados</h4>
      <ul class="space-y-1">
        <li v-for="cve in info.relatedCves" :key="cve"
            class="text-xs text-slate-400 flex items-start gap-2">
          <span class="text-yellow-500 mt-0.5">▸</span>
          <span>{{ cve }}</span>
        </li>
      </ul>
    </div>

    <!-- ── CVEs en vivo desde NVD ────────────────────────────────────────── -->
    <div v-if="liveCves.length" class="mb-4">
      <h4 class="text-sm font-semibold text-orange-400 mb-2">CVEs recientes (NVD)</h4>
      <ul class="space-y-2">
        <li v-for="cve in liveCves" :key="cve.id"
            class="text-xs bg-netwatch-dark rounded p-2 border border-netwatch-border">
          <p class="text-orange-300 font-mono font-bold">{{ cve.id }}</p>
          <p class="text-slate-400 mt-0.5">{{ cve.description }}</p>
        </li>
      </ul>
    </div>

    <!-- ── Referencias ────────────────────────────────────────────────────── -->
    <div v-if="info.references && info.references.length">
      <h4 class="text-sm font-semibold text-slate-300 mb-2">Referencias</h4>
      <div class="flex flex-wrap gap-2">
        <a
          v-for="ref in info.references"
          :key="ref"
          :href="ref"
          target="_blank"
          rel="noopener noreferrer"
          class="text-xs text-blue-400 hover:text-blue-300 underline break-all"
        >
          {{ refLabel(ref) }}
        </a>
      </div>
    </div>

    <!-- ── Consulta NVD en tiempo real ───────────────────────────────────── -->
    <div class="mt-4 pt-4 border-t border-netwatch-border flex items-center gap-3">
      <button
        @click="fetchLiveCves"
        :disabled="loadingCves"
        class="text-xs px-3 py-1.5 rounded bg-orange-800/40 hover:bg-orange-700/50 text-orange-300 border border-orange-700/30 transition-all disabled:opacity-50"
      >
        {{ loadingCves ? 'Consultando NVD...' : 'Consultar CVEs en NVD' }}
      </button>
      <span class="text-xs text-slate-500">Requiere conexión a internet</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { remediationAPI } from '@/services/api'

const props = defineProps({
  info:     { type: Object, default: null },
  closable: { type: Boolean, default: false }
})
defineEmits(['close'])

const liveCves   = ref([])
const loadingCves = ref(false)

const borderColor = computed(() => {
  const map = {
    CRITICAL: 'border-red-700/60',
    HIGH:     'border-orange-700/60',
    MEDIUM:   'border-yellow-700/60',
    LOW:      'border-blue-700/60',
    INFO:     'border-slate-700'
  }
  return map[props.info?.riskLevel] || 'border-netwatch-border'
})

const badgeColor = computed(() => {
  const map = {
    CRITICAL: 'bg-red-900/60 text-red-400',
    HIGH:     'bg-orange-900/60 text-orange-400',
    MEDIUM:   'bg-yellow-900/60 text-yellow-400',
    LOW:      'bg-blue-900/60 text-blue-400',
    INFO:     'bg-slate-700 text-slate-400'
  }
  return map[props.info?.riskLevel] || 'bg-slate-700 text-slate-400'
})

async function fetchLiveCves() {
  if (!props.info?.threatType) return
  loadingCves.value = true
  try {
    const { data } = await remediationAPI.getCves(props.info.threatType)
    liveCves.value = data.liveCves || []
  } catch {
    liveCves.value = []
  } finally {
    loadingCves.value = false
  }
}

function refLabel(url) {
  try {
    const u = new URL(url)
    return u.hostname + u.pathname.slice(0, 30) + (u.pathname.length > 30 ? '...' : '')
  } catch {
    return url.slice(0, 40)
  }
}
</script>
