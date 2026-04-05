<template>
  <div class="p-6 space-y-6">
    <h1 class="text-2xl font-bold text-slate-100">Dashboard</h1>

    <!-- Tarjetas de resumen -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard titulo="Total eventos"  :valor="store.totalCount"      color="blue"   />
      <StatCard titulo="Sin resolver"   :valor="store.unresolvedCount"  color="yellow" />
      <StatCard titulo="Críticos"       :valor="store.criticalCount"    color="red"    />
      <StatCard titulo="Altos"          :valor="store.highCount"        color="orange" />
    </div>

    <!-- Gráfico + tabla últimos eventos -->
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-4">
        <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
          Amenazas por tipo
        </h2>
        <ThreatChart :events="store.events" />
      </div>

      <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-4">
        <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
          Últimos 10 eventos
        </h2>
        <EventTable
          :events="store.events.slice(0, 10)"
          :loading="store.loading"
          @resolve="store.resolveEvent"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useEventsStore } from '@/stores/events'
import StatCard   from '@/components/StatCard.vue'
import ThreatChart from '@/components/ThreatChart.vue'
import EventTable  from '@/components/EventTable.vue'

const store = useEventsStore()

onMounted(() => store.startPolling(10000))
onUnmounted(() => store.stopPolling())
</script>
