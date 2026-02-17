<template>
  <div class="login-page flex flex-center">
    <div class="login-card q-pa-xl">
      <div class="text-h4 text-white text-center q-mb-lg">Naviseerr</div>

      <q-form @submit="onSubmit" class="q-gutter-md">
        <q-input
          v-model="username"
          label="Username"
          filled
          dark
          :rules="[val => !!val || 'Username is required']"
        />

        <q-input
          v-model="password"
          label="Password"
          type="password"
          filled
          dark
          :rules="[val => !!val || 'Password is required']"
        />

        <div v-if="error" class="text-negative text-center text-body2">
          {{ error }}
        </div>

        <q-btn
          type="submit"
          label="Log In"
          color="primary"
          class="full-width"
          size="lg"
          rounded
          no-caps
          :loading="submitting"
        />
      </q-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function onSubmit() {
  error.value = ''
  submitting.value = true

  try {
    await authStore.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch {
    error.value = 'Invalid username or password'
  } finally {
    submitting.value = false
  }
}
</script>
