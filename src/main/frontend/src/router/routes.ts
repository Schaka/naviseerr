import type { RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'search', component: () => import('pages/SearchPage.vue') },
      { path: 'requests', component: () => import('pages/RequestsPage.vue') },
      { path: 'profile', component: () => import('pages/UserSettingsPage.vue') },
      { path: 'advanced', component: () => import('pages/AdvancedOptionsPage.vue') },
    ],
  },
  {
    path: '/:catchAll(.*)*',
    redirect: '/',
  },
]

export default routes
