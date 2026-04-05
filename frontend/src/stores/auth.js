import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()

  const accessToken = ref(localStorage.getItem('accessToken'))
  const role        = ref(localStorage.getItem('role'))
  const loading     = ref(false)
  const error       = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin    = computed(() => role.value === 'ADMIN')
  const isAnalyst  = computed(() => ['ADMIN', 'ANALYST'].includes(role.value))

  async function login(email, password) {
    loading.value = true
    error.value   = null
    try {
      const { data } = await authAPI.login(email, password)
      accessToken.value = data.accessToken
      role.value        = data.role
      localStorage.setItem('accessToken',  data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('role',         data.role)
      await router.push('/dashboard')
    } catch (err) {
      error.value = err.response?.status === 401
        ? 'Credenciales incorrectas'
        : 'Error de conexión. Intente de nuevo.'
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authAPI.logout()
    } catch { /* ignorar errores de red al cerrar sesión */ }
    accessToken.value = null
    role.value        = null
    localStorage.clear()
    await router.push('/login')
  }

  return { accessToken, role, loading, error, isLoggedIn, isAdmin, isAnalyst, login, logout }
})
