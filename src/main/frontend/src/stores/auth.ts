import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from 'boot/axios'

interface AuthUser {
  username: string
  roles: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const loading = ref(true)

  const isAuthenticated = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.roles.includes('ROLE_ADMIN') ?? false)

  async function login(username: string, password: string): Promise<void> {
    const { data } = await api.post<AuthUser>('/auth/login', { username, password })
    user.value = data
  }

  async function logout(): Promise<void> {
    await api.post('/auth/logout')
    user.value = null
  }

  async function checkSession(): Promise<void> {
    try {
      const { data } = await api.get<AuthUser>('/auth/me')
      user.value = data
    } catch {
      user.value = null
    } finally {
      loading.value = false
    }
  }

  return { user, loading, isAuthenticated, isAdmin, login, logout, checkSession }
})
