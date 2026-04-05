<template>
  <div class="p-6 space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-slate-100">Eventos de red</h1>
      <button
        @click="store.fetchEvents()"
        class="text-sm bg-netwatch-panel border border-netwatch-border px-4 py-2 rounded-lg
               hover:border-netwatch-accent transition-colors"
      >
        Actualizar
      </button>
    </div>

    <!-- Filtros -->
    <div class="flex flex-wrap gap-3">
      <select v-model="store.filters.severity" @change="store.fetchEvents()"
        class="bg-netwatch-panel border border-netwatch-border rounded-lg px-3 py-2 text-sm">
        <option value="">Todas las severidades</option>
        <option v-for="s in severities" :key="s" :value="s">{{ s }}</option>
      </select>

      <select v-model="store.filters.threatType" @change="store.fetchEvents()"
        class="bg-netwatch-panel border border-netwatch-border rounded-lg px-3 py-2 text-sm">
        <option value="">Todos los tipos</option>
        <option v-for="t in threatTypes" :key="t" :value="t">{{ t }}</option>
      </select>

      <input v-model="store.filters.srcIp" @keyup.enter="store.fetchEvents()"
        placeholder="Filtrar por IP origen"
        class="bg-netwatch-panel border border-netwatch-border rounded-lg px-3 py-2 text-sm
               placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-netwatch-accent" />
    </div>

    <!-- Tabla -->
    <div class="bg-netwatch-panel border border-netwatch-border rounded-xl">
      <EventTable
        :events="store.events"
        :loading="store.loading"
        @resolve="store.resolveEvent"
      />
    </div>

    <!-- Paginación -->
    <div v-if="store.totalPages > 1" class="flex justify-center gap-2">
      <button
        v-for="p in store.totalPages" :key="p"
        @click="goToPage(p - 1)"
        :class="['px-3 py-1 rounded text-sm border',
          store.filters.page === p - 1
            ? 'bg-netwatch-accent text-netwatch-dark border-netwatch-accent'
            : 'border-netwatch-border hover:border-netwatch-accent']"
      >
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useEventsStore } from '@/stores/events'
import EventTable from '@/components/EventTable.vue'

const store = useEventsStore()

const severities  = ['INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const threatTypes = ['PORT_SCAN', 'BRUTE_FORCE', 'SYN_FLOOD', 'DNS_TUNNELING',
                     'DATA_EXFILTRATION', 'MALWARE_C2']

function goToPage(page) {
  store.filters.page = page
  store.fetchEvents()
}

onMounted(() => store.fetchEvents())
</script>
