// main.ts
import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
//import './output.css'

import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import { definePreset } from '@primeuix/themes'
import 'primeicons/primeicons.css'

// PWA will be configured manually

const app = createApp(App)

// Create a blue/teal flavored preset based on Aura
const AuraTeal = definePreset(Aura, {
    semantic: {
        primary: {
            50: '{teal.50}',
            100: '{teal.100}',
            200: '{teal.200}',
            300: '{teal.300}',
            400: '{teal.400}',
            500: '{teal.500}',
            600: '{teal.600}',
            700: '{teal.700}',
            800: '{teal.800}',
            900: '{teal.900}',
            950: '{teal.950}'
        }
    }
})

app.use(PrimeVue, {
    ripple: true,
    theme: {
        preset: AuraTeal,
        options: {
            cssLayer: false
        }
    }
})

app.mount('#app')

// Simple service worker registration for PWA
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').then(() => {
        console.log('Service Worker registered for PWA functionality')
    }).catch((error) => {
        console.log('Service Worker registration failed:', error)
    })
}