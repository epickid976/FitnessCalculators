<script setup lang="ts">
import { ref } from 'vue'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import FloatLabel from 'primevue/floatlabel'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import 'chart.js/auto'


const tdee = ref<number | null>(null)
const name = ref('')
const weight = ref(80.0)
const height = ref(180.0)
const age = ref(25)
const sex = ref(0) // 0=male, 1=female
const activity = ref(1.55)
const error = ref('')

// 1RM data
const ormWeight = ref(100)
const ormReps = ref(5)
const orm = ref<number | null>(null)

async function calcOrm() {
  const res = await fetch('/api/one-rep-max', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ weight: ormWeight.value, reps: ormReps.value })
  })
  if (!res.ok) return
  const json = await res.json()
  orm.value = json.oneRepMax
  // refresh analytics
  loadAnalytics()
}

// Share functionality
function shareResults() {
  const tdeeText = tdee.value ? `My TDEE: ${tdee.value.toFixed(0)} kcal/day` : ''
  const ormText = orm.value ? `My 1RM: ${orm.value.toFixed(2)}` : ''
  const shareData = [tdeeText, ormText].filter(Boolean).join(' | ')
  
  if (navigator.share && shareData) {
    navigator.share({
      title: 'Fitness Calculator Results',
      text: shareData,
      url: window.location.href
    })
  } else {
    // Fallback to clipboard
    const fullText = `${shareData}\nCalculated at: ${window.location.href}`
    navigator.clipboard.writeText(fullText).then(() => {
      alert('Results copied to clipboard!')
    }).catch(() => {
      alert(`${shareData}`)
    })
  }
}

// analytics
type TdeeRow = {
  id: number
  createdAt: string
  sex: number
  weightKg: number
  heightCm: number
  ageYears: number
  activity: number
  tdeeKcal: number
}
type OrmRow = {
  id: number
  createdAt: string
  weight: number
  reps: number
  oneRm: number
}

const recentTdee = ref<TdeeRow[]>([])
const recentOrm = ref<OrmRow[]>([])
const loadingAnalytics = ref(false)

async function loadAnalytics() {
  loadingAnalytics.value = true
  try {
    console.log('Loading analytics...')
    const [tdeeRes, ormRes] = await Promise.all([
      fetch('/api/tdee/recent?limit=100'),
      fetch('/api/one-rep-max/recent?limit=100')
    ])
    console.log('TDEE response:', tdeeRes.status, tdeeRes.ok)
    console.log('ORM response:', ormRes.status, ormRes.ok)
    if (tdeeRes.ok) recentTdee.value = await tdeeRes.json()
    if (ormRes.ok) recentOrm.value = await ormRes.json()
    console.log('TDEE data:', recentTdee.value.length, 'rows')
    console.log('ORM data:', recentOrm.value.length, 'rows')
  } finally {
    loadingAnalytics.value = false
  }
}

loadAnalytics()

async function calc() {
  try {
    const params = new URLSearchParams({
      weightKg: weight.value.toString(),
      heightCm: height.value.toString(),
      age: age.value.toString(),
      sex: sex.value.toString(),
      activity: activity.value.toString()
    })
    const result = await fetch(`/api/tdee?${params}`);
    if (!result.ok) throw new Error(`HTTP error! status: ${result.status}`)
    const json = await result.json();
    tdee.value = json.tdee;
    error.value = ''
    loadAnalytics()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to calculate TDEE'
  }
}
</script>

<template>
  <main class="min-h-[100svh] bg-slate-100 dark:bg-slate-900 p-4 sm:p-6">
    <div class="w-full max-w-7xl mx-auto">
      
      <!-- Desktop: Side by side, Mobile: Stacked -->
      <div class="flex flex-col xl:flex-row gap-8 xl:gap-12">
        
        <!-- Calculator Section -->
        <div class="w-full xl:w-96 xl:flex-shrink-0">
          <div class="rounded-2xl shadow-lg p-6 bg-white dark:bg-slate-800 flex flex-col gap-8">

      <!-- Title -->
      <header class="text-center space-y-2">
        <h1 class="text-3xl font-bold text-slate-800 dark:text-slate-100">
          TDEE Calculator
        </h1>
        <p class="text-slate-600 dark:text-slate-300 text-sm">
          Calculate your Total Daily Energy Expenditure and learn your daily calorie needs.
        </p>
      </header>

      <hr class="border-slate-300 dark:border-slate-600" />

      <!-- Name -->
      <FloatLabel class="w-full">
        <InputText v-model="name" id="name" class="w-full" />
        <label for="name">Name</label>
      </FloatLabel>

      <!-- Weight -->
      <FloatLabel class="w-full">
        <InputNumber
            v-model.number="weight"
            inputId="weight"
            :minFractionDigits="2"
            suffix=" kg"
            class="w-full"
        />
        <label for="weight">Weight</label>
      </FloatLabel>

      <!-- Height -->
      <FloatLabel class="w-full">
        <InputNumber
            v-model.number="height"
            inputId="height"
            :minFractionDigits="2"
            suffix=" cm"
            class="w-full"
        />
        <label for="height">Height</label>
      </FloatLabel>

      <!-- Age -->
      <FloatLabel class="w-full">
        <InputNumber
            v-model.number="age"
            inputId="age"
            :min="0"
            :max="120"
            class="w-full"
        />
        <label for="age">Age</label>
      </FloatLabel>

      <!-- Button -->
      <Button
          label="Calculate TDEE"
          @click="calc"
          class="w-full"
          size="large"
          raised
      />

      <!-- Results -->
      <div v-if="tdee !== null" class="p-4 rounded-lg bg-green-100 text-green-800 text-center font-semibold">
        Hey {{ name }}, your TDEE is {{ tdee.toFixed(0) }} kcal/day.
      </div>

      <div v-if="error" class="p-4 rounded-lg bg-red-100 text-red-800 text-center font-semibold">
        {{ error }}
      </div>

      <!-- One Rep Max -->
      <section class="space-y-4">
        <h2 class="text-xl font-semibold text-slate-800 dark:text-slate-100 pb-4">One Rep Max</h2>
        <div class="grid gap-10">
          <FloatLabel class="w-full">
            <InputNumber v-model.number="ormWeight" inputId="ormWeight" :min="1" class="w-full" />
            <label for="ormWeight">Weight</label>
          </FloatLabel>
          <FloatLabel class="w-full">
            <InputNumber v-model.number="ormReps" inputId="ormReps" :min="1" class="w-full" />
            <label for="ormReps">Reps</label>
          </FloatLabel>
          <Button label="Calculate 1RM" class="w-full" size="large" raised @click="calcOrm" />
          <div v-if="orm !== null" class="p-3 rounded bg-blue-50 text-blue-800 text-center font-medium">
            Estimated 1RM: {{ orm.toFixed(2) }}
          </div>
        </div>
      </section>

      <!-- Share Button -->
      <div v-if="tdee !== null || orm !== null" class="pt-4">
        <Button 
          label="Share Results" 
          icon="pi pi-share-alt" 
          @click="shareResults"
          class="w-full" 
          severity="secondary" 
          variant="outlined"
        />
      </div>

          </div>
        </div>

        <!-- Charts Section -->
        <div class="flex-1 min-w-0">
          <section class="space-y-6">
            <h2 class="text-xl font-semibold text-slate-800 dark:text-slate-100 text-center xl:text-left">Analytics</h2>
            <div v-if="loadingAnalytics" class="text-slate-500 text-center">Loading analytics…</div>
            <div v-else class="grid gap-6">
              
              <!-- Charts arranged in 2x2 grid on large screens -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <!-- TDEE over time -->
                <div class="h-72 lg:h-80 bg-white dark:bg-slate-800 rounded-xl p-4 sm:p-6 shadow-lg">
                  <h3 class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-3">TDEE Over Time</h3>
                  <Chart type="line" :data="{
                    labels: recentTdee.map(r => new Date(r.createdAt).toLocaleString()),
                    datasets: [{ label: 'TDEE (kcal)', data: recentTdee.map(r => r.tdeeKcal), borderColor: '#38bdf8', backgroundColor: 'rgba(56,189,248,0.3)', fill: false }]
                  }" :options="{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }" />
                  <div v-if="recentTdee.length === 0" class="text-center text-slate-500 mt-2">No TDEE data yet</div>
                </div>

                <!-- TDEE sex distribution -->
                <div class="h-72 lg:h-80 bg-white dark:bg-slate-800 rounded-xl p-4 sm:p-6 shadow-lg">
                  <h3 class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-3">Gender Distribution</h3>
                  <Chart type="pie" :data="{
                    labels: ['Male', 'Female'],
                    datasets: [{ data: [recentTdee.filter(r => r.sex === 0).length, recentTdee.filter(r => r.sex === 1).length], backgroundColor: ['#38bdf8', '#f87171'] }]
                  }" :options="{ responsive: true, maintainAspectRatio: false }" />
                  <div v-if="recentTdee.length === 0" class="text-center text-slate-500 mt-2">No TDEE data yet</div>
                </div>

                <!-- 1RM over time -->
                <div class="h-72 lg:h-80 bg-white dark:bg-slate-800 rounded-xl p-4 sm:p-6 shadow-lg">
                  <h3 class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-3">1RM Progress</h3>
                  <Chart type="line" :data="{
                    labels: recentOrm.map(r => new Date(r.createdAt).toLocaleString()),
                    datasets: [{ label: '1RM', data: recentOrm.map(r => r.oneRm), borderColor: '#34d399', backgroundColor: 'rgba(52,211,153,0.3)', fill: false }]
                  }" :options="{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }" />
                  <div v-if="recentOrm.length === 0" class="text-center text-slate-500 mt-2">No 1RM data yet</div>
                </div>

                <!-- 1RM reps distribution -->
                <div class="h-72 lg:h-80 bg-white dark:bg-slate-800 rounded-xl p-4 sm:p-6 shadow-lg">
                  <h3 class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-3">Reps Distribution</h3>
                  <Chart type="bar" :data="{
                    labels: [...new Set(recentOrm.map(r => r.reps))].sort((a,b)=>a-b),
                    datasets: [{
                      label: 'Count by reps',
                      backgroundColor: '#60a5fa',
                      data: [...new Set(recentOrm.map(r => r.reps))].sort((a,b)=>a-b).map(rep => recentOrm.filter(r => r.reps === rep).length)
                    }]
                  }" :options="{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, precision: 0 } } }" />
                  <div v-if="recentOrm.length === 0" class="text-center text-slate-500 mt-2">No 1RM data yet</div>
                </div>
              </div>

            </div>
          </section>
        </div>
        
      </div>
    </div>

    <!-- Footer with Tech Stack -->
    <footer class="bg-white dark:bg-slate-800 border-t border-slate-200 dark:border-slate-700 mt-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 py-8">
        <div class="text-center space-y-6">
          <!-- Main heading -->
          <div class="space-y-2">
            <h3 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Built with Modern Technologies</h3>
            <p class="text-sm text-slate-600 dark:text-slate-400">A full-stack fitness calculator application</p>
          </div>

          <!-- Tech stack grid -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-4xl mx-auto">
            <!-- Frontend -->
            <div class="space-y-3">
              <h4 class="text-sm font-medium text-slate-700 dark:text-slate-300 uppercase tracking-wide">Frontend</h4>
              <div class="space-y-1 text-xs text-slate-600 dark:text-slate-400">
                <div>Vue.js 3</div>
                <div>TypeScript</div>
                <div>Vite</div>
                <div>PrimeVue</div>
                <div>Tailwind CSS</div>
                <div>Chart.js</div>
              </div>
            </div>

            <!-- Backend -->
            <div class="space-y-3">
              <h4 class="text-sm font-medium text-slate-700 dark:text-slate-300 uppercase tracking-wide">Backend</h4>
              <div class="space-y-1 text-xs text-slate-600 dark:text-slate-400">
                <div>Spring Boot</div>
                <div>Java 24</div>
                <div>Spring Data JPA</div>
                <div>Hibernate</div>
                <div>Gradle</div>
              </div>
            </div>

            <!-- Database -->
            <div class="space-y-3">
              <h4 class="text-sm font-medium text-slate-700 dark:text-slate-300 uppercase tracking-wide">Database</h4>
              <div class="space-y-1 text-xs text-slate-600 dark:text-slate-400">
                <div>PostgreSQL</div>
                <div>Flyway</div>
                <div>Database Migration</div>
                <div>Analytics Storage</div>
              </div>
            </div>

            <!-- Features -->
            <div class="space-y-3">
              <h4 class="text-sm font-medium text-slate-700 dark:text-slate-300 uppercase tracking-wide">Features</h4>
              <div class="space-y-1 text-xs text-slate-600 dark:text-slate-400">
                <div>TDEE Calculator</div>
                <div>1RM Calculator</div>
                <div>Data Analytics</div>
                <div>Responsive Design</div>
                <div>Dark Mode</div>
                <div>Share Results</div>
              </div>
            </div>
          </div>

          <!-- Divider -->
          <div class="border-t border-slate-200 dark:border-slate-700 pt-6">
            <div class="flex flex-col sm:flex-row justify-between items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
              <p>© 2025 Fitness Calculator App. Built for health and performance optimization.</p>
              <div class="flex items-center gap-4">
                <span class="flex items-center gap-1">
                  <span class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
                  Live Analytics
                </span>
                <span>Full-Stack Application</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  </main>
</template>

