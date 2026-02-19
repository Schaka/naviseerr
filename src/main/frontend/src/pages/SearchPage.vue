<template>
  <q-page class="q-pa-md">
    <div class="row justify-center">
      <div class="col-12 col-md-10 col-lg-8">
        <div class="text-h5 q-mb-md">Search Music</div>

        <div class="row q-gutter-sm q-mb-md">
          <q-input
            v-model="query"
            placeholder="Search for artists or albums..."
            outlined
            dense
            class="col"
            @keyup.enter="doSearch"
          >
            <template #prepend>
              <q-icon name="mdi-magnify" />
            </template>
            <template #append>
              <q-icon
                v-if="query"
                name="mdi-close"
                class="cursor-pointer"
                @click="query = ''"
              />
            </template>
          </q-input>
          <q-btn-toggle
            v-model="searchType"
            toggle-color="primary"
            :options="[
              { label: 'Artists', value: 'artists' },
              { label: 'Albums', value: 'albums' },
            ]"
            dense
            no-caps
          />
          <q-btn
            label="Search"
            color="primary"
            :loading="loading"
            @click="doSearch"
            no-caps
          />
        </div>

        <div v-if="loading" class="row justify-center q-pa-lg">
          <q-spinner-dots size="40px" color="primary" />
        </div>

        <div v-else-if="searchType === 'artists' && artistResults.length > 0">
          <div class="text-caption text-grey-7 q-mb-sm">
            {{ totalCount }} results found
          </div>
          <q-list separator>
            <q-item v-for="artist in artistResults" :key="artist.musicbrainzId">
              <q-item-section avatar>
                <q-avatar color="dark" text-color="white" icon="mdi-account-music" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ artist.name }}</q-item-label>
                <q-item-label caption>
                  <span v-if="artist.type">{{ artist.type }}</span>
                  <span v-if="artist.country"> &middot; {{ artist.country }}</span>
                  <span v-if="artist.disambiguation"> &middot; {{ artist.disambiguation }}</span>
                </q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-chip
                  v-if="artist.status === 'AVAILABLE'"
                  color="positive"
                  text-color="white"
                  icon="mdi-check-circle"
                  dense
                >
                  Available
                </q-chip>
                <q-btn
                  v-else
                  label="Request"
                  color="primary"
                  dense
                  no-caps
                  :loading="requestingId === artist.musicbrainzId"
                  @click="requestArtist(artist)"
                />
              </q-item-section>
            </q-item>
          </q-list>
        </div>

        <div v-else-if="searchType === 'albums' && albumResults.length > 0">
          <div class="text-caption text-grey-7 q-mb-sm">
            {{ totalCount }} results found
          </div>
          <q-list separator>
            <q-item v-for="album in albumResults" :key="album.musicbrainzId">
              <q-item-section avatar>
                <q-avatar color="dark" text-color="white" icon="mdi-album" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ album.title }}</q-item-label>
                <q-item-label caption>
                  {{ album.artistName }}
                  <span v-if="album.primaryType"> &middot; {{ album.primaryType }}</span>
                  <span v-if="album.firstReleaseDate"> &middot; {{ album.firstReleaseDate }}</span>
                </q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-chip
                  v-if="album.status === 'AVAILABLE'"
                  color="positive"
                  text-color="white"
                  icon="mdi-check-circle"
                  dense
                >
                  Available
                </q-chip>
                <q-btn
                  v-else
                  label="Request"
                  color="primary"
                  dense
                  no-caps
                  :loading="requestingId === album.musicbrainzId"
                  @click="requestAlbum(album)"
                />
              </q-item-section>
            </q-item>
          </q-list>
        </div>

        <div v-else-if="searched && !loading" class="text-center text-grey-7 q-pa-lg">
          No results found. Try a different search term.
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { searchApi, type ArtistSearchResult, type AlbumSearchResult } from 'src/api/search'
import { requestsApi } from 'src/api/requests'

const $q = useQuasar()

const query = ref('')
const searchType = ref<'artists' | 'albums'>('artists')
const loading = ref(false)
const searched = ref(false)
const requestingId = ref<string | null>(null)
const totalCount = ref(0)

const artistResults = ref<ArtistSearchResult[]>([])
const albumResults = ref<AlbumSearchResult[]>([])

async function doSearch() {
  if (!query.value.trim()) return

  loading.value = true
  searched.value = true

  try {
    if (searchType.value === 'artists') {
      const result = await searchApi.searchArtists(query.value)
      artistResults.value = result.results
      totalCount.value = result.totalCount
      albumResults.value = []
    } else {
      const result = await searchApi.searchAlbums(query.value)
      albumResults.value = result.results
      totalCount.value = result.totalCount
      artistResults.value = []
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Search failed' })
  } finally {
    loading.value = false
  }
}

async function requestArtist(artist: ArtistSearchResult) {
  requestingId.value = artist.musicbrainzId
  try {
    await requestsApi.requestArtist(artist.musicbrainzId, artist.name)
    $q.notify({ type: 'positive', message: `Requested artist '${artist.name}'` })
    artist.status = 'MONITORED'
  } catch (error: unknown) {
    const status = (error as { response?: { status?: number } })?.response?.status
    if (status === 409) {
      $q.notify({ type: 'warning', message: 'Already in library' })
    } else {
      $q.notify({ type: 'negative', message: 'Request failed' })
    }
  } finally {
    requestingId.value = null
  }
}

async function requestAlbum(album: AlbumSearchResult) {
  requestingId.value = album.musicbrainzId
  try {
    await requestsApi.requestAlbum(
      album.artistMusicbrainzId,
      album.musicbrainzId,
      album.artistName,
      album.title,
    )
    $q.notify({ type: 'positive', message: `Requested album '${album.title}'` })
    album.status = 'MONITORED'
  } catch (error: unknown) {
    const status = (error as { response?: { status?: number } })?.response?.status
    if (status === 409) {
      $q.notify({ type: 'warning', message: 'Already in library' })
    } else {
      $q.notify({ type: 'negative', message: 'Request failed' })
    }
  } finally {
    requestingId.value = null
  }
}
</script>
