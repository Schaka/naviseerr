import { store } from 'quasar/wrappers'
import { createPinia } from 'pinia'

export default store(() => {
  const pinia = createPinia()
  return pinia
})
