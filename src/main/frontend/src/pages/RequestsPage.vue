<template>
  <q-page class="q-pa-md">
    <div class="row justify-center">
      <div class="col-12 col-md-10 col-lg-8">
        <div class="text-h5 q-mb-md">Requests</div>

        <q-tabs v-model="tab" dense class="q-mb-md" active-color="primary" indicator-color="primary" align="left" no-caps>
          <q-tab name="requests" label="My Requests" />
          <q-tab name="downloads" label="Downloads" />
        </q-tabs>

        <q-tab-panels v-model="tab" animated>
          <q-tab-panel name="requests" class="q-pa-none">
            <div v-if="requestsLoading" class="row justify-center q-pa-lg">
              <q-spinner-dots size="40px" color="primary" />
            </div>

            <div v-else-if="requests.length === 0" class="text-center text-grey-7 q-pa-lg">
              No requests yet. Search for music to get started.
            </div>

            <q-list v-else separator>
              <q-item v-for="request in requests" :key="request.id">
                <q-item-section avatar>
                  <q-avatar
                    :color="request.albumTitle ? 'dark' : 'dark'"
                    text-color="white"
                    :icon="request.albumTitle ? 'mdi-album' : 'mdi-account-music'"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ request.artistName }}</q-item-label>
                  <q-item-label caption>
                    {{ request.albumTitle || 'All Albums' }}
                  </q-item-label>
                </q-item-section>
                <q-item-section side top>
                  <q-chip
                    :color="statusColor(request.status)"
                    text-color="white"
                    dense
                  >
                    {{ request.status }}
                  </q-chip>
                  <q-item-label caption class="q-mt-xs">
                    {{ formatDate(request.createdAt) }}
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-tab-panel>

          <q-tab-panel name="downloads" class="q-pa-none">
            <div v-if="downloadsLoading" class="row justify-center q-pa-lg">
              <q-spinner-dots size="40px" color="primary" />
            </div>

            <div v-else-if="downloads.length === 0" class="text-center text-grey-7 q-pa-lg">
              No active downloads.
            </div>

            <q-list v-else separator>
              <q-item v-for="item in downloads" :key="item.id">
                <q-item-section avatar>
                  <q-avatar color="dark" text-color="white" icon="mdi-download" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ item.albumTitle }}</q-item-label>
                  <q-item-label caption>{{ item.artistName }}</q-item-label>
                  <q-linear-progress
                    :value="item.progress / 100"
                    color="primary"
                    class="q-mt-sm"
                    rounded
                    size="8px"
                  />
                </q-item-section>
                <q-item-section side>
                  <q-item-label>{{ Math.round(item.progress) }}%</q-item-label>
                  <q-item-label v-if="item.timeleft" caption>{{ item.timeleft }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-tab-panel>
        </q-tab-panels>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { requestsApi, type MediaRequestDto } from 'src/api/requests'
import { downloadsApi, type DownloadItem } from 'src/api/downloads'

const $q = useQuasar()

const tab = ref('requests')
const requests = ref<MediaRequestDto[]>([])
const downloads = ref<DownloadItem[]>([])
const requestsLoading = ref(false)
const downloadsLoading = ref(false)

let requestsTimer: ReturnType<typeof setInterval> | null = null
let downloadsTimer: ReturnType<typeof setInterval> | null = null

function statusColor(status: string): string {
  switch (status) {
    case 'PENDING': return 'grey'
    case 'PROCESSING': return 'info'
    case 'AVAILABLE': return 'positive'
    case 'FAILED': return 'negative'
    default: return 'grey'
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString()
}

async function loadRequests() {
  try {
    requestsLoading.value = requests.value.length === 0
    requests.value = await requestsApi.getMyRequests()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load requests' })
  } finally {
    requestsLoading.value = false
  }
}

async function loadDownloads() {
  try {
    downloadsLoading.value = downloads.value.length === 0
    const queue = await downloadsApi.getQueue()
    downloads.value = queue.items
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load downloads' })
  } finally {
    downloadsLoading.value = false
  }
}

function startRequestsPolling() {
  stopRequestsPolling()
  requestsTimer = setInterval(loadRequests, 30000)
}

function stopRequestsPolling() {
  if (requestsTimer) {
    clearInterval(requestsTimer)
    requestsTimer = null
  }
}

function startDownloadsPolling() {
  stopDownloadsPolling()
  downloadsTimer = setInterval(loadDownloads, 10000)
}

function stopDownloadsPolling() {
  if (downloadsTimer) {
    clearInterval(downloadsTimer)
    downloadsTimer = null
  }
}

watch(tab, (newTab) => {
  if (newTab === 'requests') {
    loadRequests()
    startRequestsPolling()
    stopDownloadsPolling()
  } else {
    loadDownloads()
    startDownloadsPolling()
    stopRequestsPolling()
  }
})

onMounted(() => {
  loadRequests()
  startRequestsPolling()
})

onUnmounted(() => {
  stopRequestsPolling()
  stopDownloadsPolling()
})
</script>
