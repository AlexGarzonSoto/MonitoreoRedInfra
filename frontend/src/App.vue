<template>
  <div class="min-h-screen bg-netwatch-dark">
    <NavBar v-if="auth.isLoggedIn" />
    <main :class="auth.isLoggedIn ? 'pt-16' : ''">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import NavBar from '@/components/NavBar.vue'

const auth = useAuthStore()
</script>
