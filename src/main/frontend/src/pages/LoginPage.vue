<template>
  <q-layout>
    <q-page-container>
      <q-page class="flex flex-center">
        <div
          id="background"
          :class="$q.dark.isActive ? 'dark_gradient' : 'normal_gradient'"
        ></div>
        <q-btn
          color="white"
          class="absolute-top-right"
          flat
          round
          @click="$q.dark.toggle()"
          :icon="$q.dark.isActive ? 'nights_stay' : 'wb_sunny'"
        />
        <q-card
          class="login-form"
          v-bind:style="
            $q.platform.is.mobile ? { width: '80%' } : { width: '30%' }
          "
        >
          <q-img src="/statics/images/pharmacy.jpg"></q-img>
          <q-card-section>
            <q-avatar
              size="74px"
              class="absolute"
              style="top: 0;right: 25px;transform: translateY(-50%);"
            >
              <img src="https://cdn.quasar.dev/img/boy-avatar.png" />
            </q-avatar>
            <div class="row no-wrap items-center">
              <div class="col text-h6 ellipsis">
                Log in to Dashboard
              </div>
            </div>
          </q-card-section>
          <q-card-section>
            <q-form class="q-gutter-md">
              <q-input filled v-model="username" label="Username" lazy-rules />

              <q-input
                type="password"
                filled
                v-model="password"
                label="Password"
                lazy-rules
              />

              <div>
                <q-btn
                  label="Login"
                  type="button"
                  color="primary"
                  @click="attemptLogin"
                />

              </div>
            </q-form>
          </q-card-section>
        </q-card>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">

import { ref } from 'vue';
import { api } from 'boot/axios'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { AxiosError } from 'axios';

const username = ref<string>('');
const password = ref<string>('');

const router = useRouter();
const $q = useQuasar();

const attemptLogin = async () => {

  const data = new URLSearchParams()
  data.append('username', username.value)
  data.append('password', password.value)

  await api.post('/login', data)
    .then(() => router.push('/'))
    .catch((err: AxiosError)  => {
      if(err.response?.status === 401) {
        $q.notify( {
          message: "Login failed. Please double check you're using the correct credentials for your Navidrome account.",
          position: 'top-right',
          color: 'red',
          badgeColor: 'red'
        })
      }

      // handle unexpected error?

    })
}

</script>

<style>
#background {
  position: absolute;
  width: 100%;
  height: 100%;
  background-repeat: no-repeat;
  background-size: cover;
  background-position: 50% 50%;
}
.normal_gradient {
  background: linear-gradient(145deg, rgb(74, 94, 137) 15%, #b61924 70%);
}
.dark_gradient {
  background: linear-gradient(145deg, rgb(11, 26, 61) 15%, #4c1014 70%);
}
.login-form {
  position: absolute;
}
</style>
