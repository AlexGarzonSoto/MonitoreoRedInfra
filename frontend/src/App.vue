<template>
  <div class="min-h-screen bg-netwatch-dark">
    <NavBar v-if="auth.isLoggedIn" />
    <main :class="auth.isLoggedIn ? 'pt-16' : ''">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import NavBar from '@/components/NavBar.vue'
import http from '@/services/api'

const auth = useAuthStore()

// Al arrancar: si hay token en localStorage, verificar que el backend lo acepta.
// Si el backend no está disponible o el token expiró → cerrar sesión y pedir login.
onMounted(async () => {
  if (!auth.isLoggedIn) return
  try {
    await http.get('/api/v1/events/stats/summary')
  } catch (err) {
    // 401 lo maneja el interceptor (intenta refresh → si falla → /login)
    // Error de red: backend no disponible → forzar login
    if (!err.response) {
      auth.logout()
    }
  }
})
</script>
