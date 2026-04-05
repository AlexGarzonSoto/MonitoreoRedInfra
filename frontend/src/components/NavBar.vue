<template>
  <nav class="fixed top-0 left-0 right-0 z-50 bg-netwatch-panel border-b border-netwatch-border h-16 flex items-center px-6">
    <!-- Logo -->
    <span class="text-netwatch-accent font-bold text-lg tracking-widest mr-8">NetWatch</span>

    <!-- Links -->
    <div class="flex gap-1 flex-1">
      <RouterLink
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="px-4 py-2 rounded-lg text-sm transition-colors hover:bg-netwatch-dark"
        active-class="bg-netwatch-dark text-netwatch-accent"
      >
        {{ link.label }}
      </RouterLink>
    </div>

    <!-- Usuario -->
    <div class="flex items-center gap-4">
      <span class="text-xs text-slate-400 uppercase tracking-wider">{{ auth.role }}</span>
      <button
        @click="auth.logout()"
        class="text-sm text-slate-400 hover:text-red-400 transition-colors"
      >
        Salir
      </button>
    </div>
  </nav>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/events',    label: 'Eventos'   },
  { to: '/alerts',    label: 'Alertas'   }
]
</script>
