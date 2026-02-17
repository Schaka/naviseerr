import { boot } from 'quasar/wrappers'
import { useAuthStore } from 'stores/auth'

export default boot(async ({ router }) => {
  const authStore = useAuthStore()

  await authStore.checkSession()

  router.beforeEach((to) => {
    if (to.meta.public) return true

    if (!authStore.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    return true
  })

  router.beforeEach((to) => {
    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/'
    }
    return true
  })
})
