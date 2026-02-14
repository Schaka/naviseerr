import { configure } from 'quasar/wrappers'

export default configure((/* ctx */) => {
  return {
    boot: [
      'axios',
      'auth',
    ],

    css: [
      'app.scss',
    ],

    extras: [
      'roboto-font',
      'material-icons',
      'mdi-v7',
    ],

    build: {
      target: {
        browser: ['es2022', 'firefox115', 'chrome115', 'safari14'],
        node: 'node20',
      },

      typescript: {
        strict: true,
        vueShim: true,
      },

      vueRouterMode: 'history',

      vitePlugins: [
        ['vite-plugin-checker', {
          vueTsc: true,
        }, { server: false }],
      ],
    },

    devServer: {
      open: false,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },

    framework: {
      config: {
        dark: true,
      },
      plugins: [
        'Notify',
        'Loading',
      ],
    },

    animations: [],

    ssr: {
      pwa: false,
      prodPort: 3000,
      middlewares: [
        'render',
      ],
    },

    pwa: {
      workboxMode: 'GenerateSW',
    },
  }
})
