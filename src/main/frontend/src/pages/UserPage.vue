<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from 'boot/axios'
import { UserData } from 'components/dto/UserData.ts';

const user = ref<UserData>( {
  lastFmKey: '',
  lastFmUsername: ''
});

onMounted(async () => {
  user.value = await api.get<UserData>('/user').then(res => res.data)
})

const sendUpdate = async () => {
  user.value = await api.put<UserData>('/user', user.value).then(res => res.data)
}

defineOptions({
  name: 'UserPage'
});
</script>

<template>
  <q-page class="row items-center justify-evenly">
    <q-form v-model="user" @submit="sendUpdate">
      <q-icon name="info">
        <q-tooltip class="text-body1">
          This is required for specific recommendations from Last.FM. By setting it, you can get similar artists or tracks to the ones in your library
        </q-tooltip>
      </q-icon>
      <q-input v-model="user.lastFmKey" label="Last.FM API Key" />
      <q-icon name="info">
        <q-tooltip class="text-body1">
          Knowing your Last.FM username, Naviseerr can pull Last.FM's recommendations for your user. Those depend on what's been scrobbled.
          If you don't already scrobble your listened songs, you need to enable API access so Naviseerr can take the from Navidrome and send it there.
        </q-tooltip>
      </q-icon>
      <q-input v-model="user.lastFmUsername" label="Last.FM Username" />

      <div>
        <q-btn label="Submit" type="submit" color="primary"/>
      </div>
    </q-form>
  </q-page>
</template>
