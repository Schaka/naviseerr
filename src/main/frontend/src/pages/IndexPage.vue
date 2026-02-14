<template>
  <q-page class="q-pa-lg">
    <div class="text-h4 text-white q-mb-md">
      Welcome back, {{ authStore.user?.username }}
    </div>
    <div class="text-body1 text-grey-6">
      Your music dashboard is coming soon.
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { useAuthStore } from 'stores/auth'

const authStore = useAuthStore()
</script>
