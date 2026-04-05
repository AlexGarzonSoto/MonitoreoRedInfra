import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { eventsAPI, alertsAPI } from '@/services/api'

export const useEventsStore = defineStore('events', () => {
  const events     = ref([])
  const alerts     = ref([])
  const summary    = ref(null)
  const loading    = ref(false)
  const error      = ref(null)
  const totalPages = ref(0)
  const filters    = ref({ page: 0, size: 50, sort: 'timestamp,desc' })

  let pollingTimer = null

  const criticalCount   = computed(() => summary.value?.critical   ?? 0)
  const highCount       = computed(() => summary.value?.high        ?? 0)
  const unresolvedCount = computed(() => summary.value?.unresolved  ?? 0)
  const totalCount      = computed(() => summary.value?.total       ?? 0)

  async function fetchEvents() {
    loading.value = true
    error.value   = null
    try {
      const { data } = await eventsAPI.getAll(filters.value)
      events.value     = data.content
      totalPages.value = data.totalPages
    } catch (err) {
      error.value = 'Error cargando eventos'
      console.error(err)
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary() {
    try {
      const { data } = await eventsAPI.getSummary()
      summary.value = data
    } catch (err) {
      console.error('Error cargando resumen:', err)
    }
  }

  async function fetchAlerts(status = 'OPEN') {
    loading.value = true
    try {
      const { data } = await alertsAPI.getAll({ status, size: 50 })
      alerts.value = data.content
    } catch (err) {
      error.value = 'Error cargando alertas'
    } finally {
      loading.value = false
    }
  }

  async function resolveEvent(id) {
    try {
      const { data } = await eventsAPI.resolve(id)
      const idx = events.value.findIndex(e => e.id === id)
      if (idx !== -1) events.value[idx] = data
    } catch (err) {
      console.error('Error resolviendo evento:', err)
    }
  }

  async function acknowledgeAlert(id) {
    try {
      const { data } = await alertsAPI.acknowledge(id)
      const idx = alerts.value.findIndex(a => a.id === id)
      if (idx !== -1) alerts.value[idx] = data
    } catch (err) {
      console.error('Error reconociendo alerta:', err)
    }
  }

  async function resolveAlert(id) {
    try {
      const { data } = await alertsAPI.resolve(id)
      alerts.value = alerts.value.filter(a => a.id !== id)
      return data
    } catch (err) {
      console.error('Error resolviendo alerta:', err)
    }
  }

  function startPolling(intervalMs = 10000) {
    fetchEvents()
    fetchSummary()
    pollingTimer = setInterval(() => {
      fetchEvents()
      fetchSummary()
    }, intervalMs)
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  return {
    events, alerts, summary, loading, error, totalPages, filters,
    criticalCount, highCount, unresolvedCount, totalCount,
    fetchEvents, fetchSummary, fetchAlerts,
    resolveEvent, acknowledgeAlert, resolveAlert,
    startPolling, stopPolling
  }
})
