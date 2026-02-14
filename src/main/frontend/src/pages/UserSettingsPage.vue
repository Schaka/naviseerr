<template>
  <q-page class="q-pa-md">
    <div class="row justify-center">
      <div class="col-12 col-md-8 col-lg-6">
        <q-card>
          <q-card-section>
            <div class="text-h5">User Settings</div>
            <div class="text-subtitle2 text-grey-7">{{ username }}</div>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <q-form @submit="onSubmit" class="q-gutter-md">
              <!-- Last.fm Connection -->
              <div class="q-mb-md">
                <div class="text-subtitle1 q-mb-sm">
                  <q-icon name="mdi-lastfm" class="q-mr-sm" />
                  Last.fm
                </div>
                <div v-if="formData.lastFmSessionKey" class="q-gutter-sm">
                  <div class="row items-center q-gutter-sm">
                    <q-chip color="positive" text-color="white" icon="mdi-check-circle">
                      Connected
                    </q-chip>
                    <span class="text-grey-7">Your account is connected to Last.fm</span>
                  </div>
                  <q-toggle
                    v-model="formData.lastFmScrobblingEnabled"
                    label="Enable Last.fm Scrobbling"
                    color="primary"
                    class="q-mt-sm"
                  />
                </div>
                <div v-else class="row items-center q-gutter-sm">
                  <q-btn
                    label="Connect to Last.fm"
                    color="primary"
                    icon="mdi-lastfm"
                    @click="connectToLastFm"
                    :loading="connectingLastFm"
                  />
                  <span class="text-grey-7">Connect your Last.fm account to enable scrobbling</span>
                </div>
              </div>

              <!-- ListenBrainz Token -->
              <div class="q-mb-md">
                <div class="text-subtitle1 q-mb-sm">
                  <q-icon name="mdi-music-circle" class="q-mr-sm" />
                  ListenBrainz
                </div>
                <q-input
                  v-model="formData.listenBrainzToken"
                  label="ListenBrainz Token"
                  hint="Your ListenBrainz user token for scrobbling"
                  outlined
                  clearable
                >
                  <template #prepend>
                    <q-icon name="mdi-music-circle" />
                  </template>
                </q-input>
                <q-toggle
                  v-if="formData.listenBrainzToken"
                  v-model="formData.listenBrainzScrobblingEnabled"
                  label="Enable ListenBrainz Scrobbling"
                  color="primary"
                  class="q-mt-sm"
                />
              </div>

              <div class="row justify-end q-gutter-sm">
                <q-btn
                  label="Cancel"
                  color="grey"
                  flat
                  :to="'/'"
                />
                <q-btn
                  label="Save"
                  type="submit"
                  color="primary"
                  :loading="loading"
                />
              </div>
            </q-form>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { userApi, type UserSettings, type UpdateUserSettingsRequest } from 'src/api/user'

const $q = useQuasar()
const router = useRouter()
const route = useRoute()

const username = ref('')
const loading = ref(false)
const connectingLastFm = ref(false)
const formData = ref<UserSettings>({
  username: '',
  lastFmSessionKey: undefined,
  listenBrainzToken: undefined,
  lastFmScrobblingEnabled: false,
  listenBrainzScrobblingEnabled: false,
})

async function loadSettings() {
  try {
    loading.value = true
    const settings = await userApi.getUserSettings()
    username.value = settings.username
    formData.value = {
      username: settings.username,
      lastFmSessionKey: settings.lastFmSessionKey || undefined,
      listenBrainzToken: settings.listenBrainzToken || undefined,
      lastFmScrobblingEnabled: settings.lastFmScrobblingEnabled,
      listenBrainzScrobblingEnabled: settings.listenBrainzScrobblingEnabled,
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to load user settings',
    })
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  try {
    loading.value = true

    // Build the update request
    const updateRequest: UpdateUserSettingsRequest = {
      listenBrainzToken: formData.value.listenBrainzToken,
      // Only send scrobbling settings if the corresponding keys exist
      lastFmScrobblingEnabled: formData.value.lastFmSessionKey
        ? formData.value.lastFmScrobblingEnabled
        : false,
      listenBrainzScrobblingEnabled: formData.value.listenBrainzToken
        ? formData.value.listenBrainzScrobblingEnabled
        : false,
    }

    await userApi.updateUserSettings(updateRequest)
    $q.notify({
      type: 'positive',
      message: 'Settings saved successfully',
    })
    await router.push('/')
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to save settings',
    })
  } finally {
    loading.value = false
  }
}

function connectToLastFm() {
  connectingLastFm.value = true
  // Construct the callback URL to return to this page
  const callbackUrl = `${window.location.origin}/profile`
  // Redirect to Last.fm auth flow
  const authUrl = userApi.getLastFmAuthUrl(callbackUrl)
  window.location.href = authUrl
}

async function handleLastFmCallback(token: string) {
  try {
    loading.value = true
    const result = await userApi.handleLastFmCallback(token)

    if (result.success) {
      $q.notify({
        type: 'positive',
        message: result.message,
      })
      // Reload settings to show the new session key
      await loadSettings()
    } else {
      $q.notify({
        type: 'negative',
        message: result.message,
      })
    }

    // Clean up the URL by removing the token parameter
    await router.replace({ query: {} })
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to complete Last.fm authentication',
    })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // Check if we're returning from Last.fm OAuth callback
  const token = route.query.token as string | undefined
  if (token) {
    await handleLastFmCallback(token)
  } else {
    await loadSettings()
  }
})
</script>
