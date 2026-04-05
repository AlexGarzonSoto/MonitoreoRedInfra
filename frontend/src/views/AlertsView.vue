<template>
  <div class="p-6 space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-slate-100">Alertas</h1>
      <button
        @click="store.fetchAlerts(activeStatus)"
        class="text-sm bg-netwatch-panel border border-netwatch-border px-4 py-2 rounded-lg
               hover:border-netwatch-accent transition-colors"
      >
        Actualizar
      </button>
    </div>

    <!-- Tabs de estado -->
    <div class="flex gap-2 border-b border-netwatch-border pb-1">
      <button v-for="s in statuses" :key="s"
        @click="changeStatus(s)"
        :class="['px-4 py-1.5 text-sm rounded-t-lg transition-colors',
          activeStatus === s
            ? 'bg-netwatch-panel text-netwatch-accent border border-b-0 border-netwatch-border'
            : 'text-slate-400 hover:text-slate-200']"
      >
        {{ s }}
      </button>
    </div>

    <!-- Panel de alertas -->
    <AlertPanel
      :alerts="store.alerts"
      :loading="store.loading"
      :can-act="auth.isAnalyst"
      @acknowledge="store.acknowledgeAlert"
      @resolve="store.resolveAlert"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useEventsStore } from '@/stores/events'
import { useAuthStore }   from '@/stores/auth'
import AlertPanel from '@/components/AlertPanel.vue'

const store  = useEventsStore()
const auth   = useAuthStore()

const statuses     = ['OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_POSITIVE']
const activeStatus = ref('OPEN')

function changeStatus(s) {
  activeStatus.value = s
  store.fetchAlerts(s)
}

onMounted(() => store.fetchAlerts('OPEN'))
</script>
