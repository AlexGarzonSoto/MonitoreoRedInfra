import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  timeout: 10000
})

// ── Interceptor de petición: añade el JWT ──────────────────────────────────
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Interceptor de respuesta: refresca token en 401 ───────────────────────
let isRefreshing = false
let refreshQueue = []

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      clearSession()
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return http(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post(
        `${http.defaults.baseURL}/api/v1/auth/refresh`,
        null,
        { headers: { 'X-Refresh-Token': refreshToken } }
      )
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)

      refreshQueue.forEach(({ resolve }) => resolve(data.accessToken))
      refreshQueue = []

      original.headers.Authorization = `Bearer ${data.accessToken}`
      return http(original)
    } catch {
      refreshQueue.forEach(({ reject }) => reject(error))
      refreshQueue = []
      clearSession()
      return Promise.reject(error)
    } finally {
      isRefreshing = false
    }
  }
)

function clearSession() {
  localStorage.clear()
  window.location.href = '/login'
}

// ── Auth API ────────────────────────────────────────────────────────────────
export const authAPI = {
  login: (email, password) =>
    http.post('/api/v1/auth/login', { email, password }),
  logout: () =>
    http.post('/api/v1/auth/logout'),
  refresh: (refreshToken) =>
    http.post('/api/v1/auth/refresh', null, {
      headers: { 'X-Refresh-Token': refreshToken }
    })
}

// ── Events API ──────────────────────────────────────────────────────────────
export const eventsAPI = {
  getAll: (params = {}) =>
    http.get('/api/v1/events', { params }),
  getById: (id) =>
    http.get(`/api/v1/events/${id}`),
  resolve: (id) =>
    http.patch(`/api/v1/events/${id}/resolve`),
  getSummary: () =>
    http.get('/api/v1/events/stats/summary')
}

// ── Alerts API ──────────────────────────────────────────────────────────────
export const alertsAPI = {
  getAll: (params = {}) =>
    http.get('/api/v1/alerts', { params }),
  getById: (id) =>
    http.get(`/api/v1/alerts/${id}`),
  acknowledge: (id) =>
    http.patch(`/api/v1/alerts/${id}/acknowledge`),
  resolve: (id) =>
    http.patch(`/api/v1/alerts/${id}/resolve`),
  markFalsePositive: (id) =>
    http.patch(`/api/v1/alerts/${id}/false-positive`),
  getSummary: () =>
    http.get('/api/v1/alerts/stats/summary')
}

// ── Reports API ─────────────────────────────────────────────────────────────
export const reportAPI = {
  downloadEvents: (format = 'json') =>
    http.get('/api/v1/reports/events', {
      params: { format },
      responseType: 'blob'
    }),
  downloadAlerts: (format = 'json') =>
    http.get('/api/v1/reports/alerts', {
      params: { format },
      responseType: 'blob'
    })
}

// ── Capture API ─────────────────────────────────────────────────────────────
export const captureAPI = {
  listInterfaces: () =>
    http.get('/api/v1/capture/interfaces'),
  getStatus: () =>
    http.get('/api/v1/capture/status'),
  changeInterface: (interfaceName) =>
    http.patch('/api/v1/capture/interface', { interface: interfaceName }),
  start: () =>
    http.post('/api/v1/capture/start'),
  stop: () =>
    http.post('/api/v1/capture/stop')
}

// ── Remediation API ─────────────────────────────────────────────────────────
export const remediationAPI = {
  getAll: () =>
    http.get('/api/v1/remediation'),
  getByThreatType: (threatType) =>
    http.get(`/api/v1/remediation/${threatType}`),
  getCves: (threatType) =>
    http.get(`/api/v1/remediation/${threatType}/cves`)
}

// ── Scan API (vulnerabilidades estilo OpenVAS) ────────────────────────────────
export const scanAPI = {
  requestScan: (targetIp, targetPorts = null) =>
    http.post('/api/v1/scan/request', { targetIp, targetPorts }),
  listResults: () =>
    http.get('/api/v1/scan/results'),
  getResult: (scanId) =>
    http.get(`/api/v1/scan/results/${scanId}`)
}

export default http
