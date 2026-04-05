import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
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
  acknowledge: (id) =>
    http.patch(`/api/v1/alerts/${id}/acknowledge`),
  resolve: (id) =>
    http.patch(`/api/v1/alerts/${id}/resolve`),
  markFalsePositive: (id) =>
    http.patch(`/api/v1/alerts/${id}/false-positive`),
  getSummary: () =>
    http.get('/api/v1/alerts/stats/summary')
}

export default http
