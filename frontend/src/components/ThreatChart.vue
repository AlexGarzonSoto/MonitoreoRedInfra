<template>
  <div class="relative h-48">
    <Bar v-if="hasData" :data="chartData" :options="options" />
    <div v-else class="flex items-center justify-center h-full text-slate-500 text-sm">
      Sin datos para mostrar
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS, BarElement, CategoryScale,
  LinearScale, Tooltip, Legend
} from 'chart.js'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const props = defineProps({
  events: { type: Array, default: () => [] }
})

const COLORS = {
  PORT_SCAN:        '#38bdf8',
  BRUTE_FORCE:      '#fb923c',
  SYN_FLOOD:        '#f87171',
  DNS_TUNNELING:    '#a78bfa',
  DATA_EXFILTRATION:'#fbbf24',
  MALWARE_C2:       '#f43f5e'
}

const chartData = computed(() => {
  const counts = {}
  props.events.forEach(e => {
    if (e.threatType && e.threatType !== 'NORMAL') {
      counts[e.threatType] = (counts[e.threatType] ?? 0) + 1
    }
  })
  const labels = Object.keys(counts)
  return {
    labels,
    datasets: [{
      label: 'Amenazas',
      data: labels.map(l => counts[l]),
      backgroundColor: labels.map(l => COLORS[l] ?? '#64748b')
    }]
  }
})

const hasData = computed(() => chartData.value.labels.length > 0)

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: '#94a3b8', font: { size: 10 } }, grid: { color: '#1e293b' } },
    y: { ticks: { color: '#94a3b8', font: { size: 10 } }, grid: { color: '#334155' } }
  }
}
</script>
