<template>
  <q-page class="q-pa-md">
    <div class="row justify-center">
      <div class="col-12 col-md-8 col-lg-6">
        <q-card>
          <q-card-section>
            <div class="text-h5">Advanced Options</div>
            <div class="text-subtitle2 text-grey-7">Manual job triggers and advanced operations</div>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <div class="q-gutter-md">
              <!-- Backfill Activity Section -->
              <div>
                <div class="text-h6 q-mb-sm">
                  <q-icon name="mdi-database-sync" class="q-mr-sm" />
                  Backfill Listening History
                </div>
                <div class="text-body2 text-grey-7 q-mb-md">
                  Import your recent listening history from Navidrome into the local database.
                  This is useful for populating scrobbling data or after connecting new services.
                </div>

                <div class="row items-center q-gutter-md">
                  <q-input
                    v-model.number="backfillCount"
                    type="number"
                    label="Number of plays to fetch"
                    hint="Maximum: 500"
                    outlined
                    dense
                    style="width: 200px"
                    :min="1"
                    :max="500"
                  />

                  <q-btn
                    label="Run Backfill"
                    color="primary"
                    icon="mdi-play"
                    @click="runBackfill"
                    :loading="backfillLoading"
                  />
                </div>

                <q-banner v-if="backfillResult" :class="backfillResult.success ? 'bg-positive text-white' : 'bg-negative text-white'" class="q-mt-md">
                  <template #avatar>
                    <q-icon :name="backfillResult.success ? 'mdi-check-circle' : 'mdi-alert-circle'" />
                  </template>
                  <div>{{ backfillResult.message }}</div>
                  <div v-if="backfillResult.success" class="text-caption">
                    New plays imported: {{ backfillResult.newPlaysCount }} / {{ backfillResult.requestedCount }} requested
                  </div>
                </q-banner>
              </div>

              <q-separator />

              <!-- Placeholder for future manual job triggers -->
              <div class="text-caption text-grey-7 text-center q-pa-md">
                Additional manual operations will appear here in future updates
              </div>
            </div>
          </q-card-section>

          <q-separator />

          <q-card-actions align="right">
            <q-btn
              label="Back to Home"
              color="grey"
              flat
              :to="'/'"
            />
          </q-card-actions>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { userApi, type BackfillResult } from 'src/api/user'

const $q = useQuasar()

const backfillCount = ref(100)
const backfillLoading = ref(false)
const backfillResult = ref<BackfillResult | null>(null)

async function runBackfill() {
  try {
    backfillLoading.value = true
    backfillResult.value = null

    const result = await userApi.backfillActivity(backfillCount.value)
    backfillResult.value = result

    if (result.success) {
      $q.notify({
        type: 'positive',
        message: `Successfully imported ${result.newPlaysCount} plays`,
        position: 'top',
      })
    } else {
      $q.notify({
        type: 'negative',
        message: result.message,
        position: 'top',
      })
    }
  } catch (error) {
    backfillResult.value = {
      success: false,
      message: 'Failed to run backfill operation',
      newPlaysCount: 0,
      requestedCount: backfillCount.value
    }
    $q.notify({
      type: 'negative',
      message: 'Failed to run backfill operation',
      position: 'top',
    })
  } finally {
    backfillLoading.value = false
  }
}
</script>
