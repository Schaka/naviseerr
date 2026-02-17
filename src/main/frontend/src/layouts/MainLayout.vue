<template>
  <q-layout view="lHh Lpr lFf">
    <q-header class="toolbar-blur" bordered>
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          class="lt-md"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title />

        <q-btn flat round dense>
          <q-avatar size="32px" color="primary" text-color="dark" class="text-weight-bold">
            {{ initial }}
          </q-avatar>

          <q-menu dark>
            <q-list style="min-width: 150px">
              <q-item>
                <q-item-section>
                  <q-item-label class="text-weight-bold">{{ authStore.user?.username }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-separator dark />
              <q-item clickable v-close-popup to="/profile">
                <q-item-section avatar>
                  <q-icon name="mdi-account-cog" />
                </q-item-section>
                <q-item-section>Profile Settings</q-item-section>
              </q-item>
              <q-item clickable v-close-popup to="/advanced">
                <q-item-section avatar>
                  <q-icon name="mdi-cog-play" />
                </q-item-section>
                <q-item-section>Advanced Options</q-item-section>
              </q-item>
              <q-item clickable v-close-popup @click="onLogout">
                <q-item-section avatar>
                  <q-icon name="logout" />
                </q-item-section>
                <q-item-section>Log out</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      :width="240"
      class="sidebar"
    >
      <q-list class="q-pt-md">
        <div class="text-h5 text-white text-weight-bold q-px-lg q-pb-md">
          Naviseerr
        </div>

        <q-item clickable v-ripple to="/" exact>
          <q-item-section avatar>
            <q-icon name="mdi-home" />
          </q-item-section>
          <q-item-section>Home</q-item-section>
        </q-item>

        <q-item clickable v-ripple to="/search">
          <q-item-section avatar>
            <q-icon name="mdi-magnify" />
          </q-item-section>
          <q-item-section>Search</q-item-section>
        </q-item>

        <q-item clickable v-ripple to="/requests">
          <q-item-section avatar>
            <q-icon name="mdi-playlist-music" />
          </q-item-section>
          <q-item-section>Requests</q-item-section>
        </q-item>

        <template v-if="authStore.isAdmin">
          <q-separator dark class="q-my-sm q-mx-md" />
          <q-item clickable v-ripple to="/settings">
            <q-item-section avatar>
              <q-icon name="mdi-cog" />
            </q-item-section>
            <q-item-section>Settings</q-item-section>
          </q-item>
        </template>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const leftDrawerOpen = ref(false)

const initial = computed(() =>
  authStore.user?.username.charAt(0).toUpperCase() ?? '?'
)

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

async function onLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>
