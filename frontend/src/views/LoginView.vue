<template>
  <div class="min-h-screen flex items-center justify-center px-4">
    <div class="w-full max-w-md">
      <!-- Logo / título -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-netwatch-accent tracking-widest">NetWatch</h1>
        <p class="text-slate-400 mt-1 text-sm">Sistema de Monitoreo de Amenazas</p>
      </div>

      <!-- Card de login -->
      <div class="bg-netwatch-panel border border-netwatch-border rounded-xl p-8 shadow-2xl">
        <h2 class="text-xl font-semibold mb-6 text-slate-100">Iniciar sesión</h2>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Email -->
          <div>
            <label class="block text-sm text-slate-400 mb-1">Correo electrónico</label>
            <input
              v-model="email"
              type="email"
              autocomplete="email"
              required
              placeholder="admin@netwatch.local"
              class="w-full bg-netwatch-dark border border-netwatch-border rounded-lg px-4 py-2.5
                     text-slate-100 placeholder-slate-600 focus:outline-none
                     focus:ring-2 focus:ring-netwatch-accent focus:border-transparent"
            />
          </div>

          <!-- Contraseña -->
          <div>
            <label class="block text-sm text-slate-400 mb-1">Contraseña</label>
            <input
              v-model="password"
              type="password"
              autocomplete="current-password"
              required
              placeholder="••••••••"
              class="w-full bg-netwatch-dark border border-netwatch-border rounded-lg px-4 py-2.5
                     text-slate-100 placeholder-slate-600 focus:outline-none
                     focus:ring-2 focus:ring-netwatch-accent focus:border-transparent"
            />
          </div>

          <!-- Error -->
          <div v-if="auth.error" class="bg-red-900/40 border border-red-700 rounded-lg px-4 py-2.5 text-red-300 text-sm">
            {{ auth.error }}
          </div>

          <!-- Botón -->
          <button
            type="submit"
            :disabled="auth.loading"
            class="w-full bg-netwatch-accent hover:bg-sky-400 disabled:opacity-50
                   text-netwatch-dark font-semibold rounded-lg px-4 py-2.5
                   transition-colors duration-200"
          >
            {{ auth.loading ? 'Autenticando...' : 'Entrar' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth     = useAuthStore()
const email    = ref('')
const password = ref('')

function handleLogin() {
  auth.login(email.value, password.value)
}
</script>
