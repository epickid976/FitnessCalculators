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
    const [tdeeRes, ormRes] = await Promise.all([
      fetch('/api/tdee/recent?limit=100'),
      fetch('/api/one-rep-max/recent?limit=100')
    ])
    if (tdeeRes.ok) recentTdee.value = await tdeeRes.json()
    if (ormRes.ok) recentOrm.value = await ormRes.json()
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
  <main class="min-h-[100svh] flex justify-center bg-slate-100 dark:bg-slate-900 p-6">
    <div class="w-full max-w-5xl flex flex-col gap-10">

      <!-- Calculator Card -->
      <div class="w-full max-w-md self-center rounded-2xl shadow-lg p-6 bg-white dark:bg-slate-800 flex flex-col gap-8">

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
        <h2 class="text-xl font-semibold text-slate-800 dark:text-slate-100">One Rep Max</h2>
        <div class="grid gap-4">
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

      <!-- Analytics Charts (at the bottom) -->
      </div>
      <section class="space-y-6">
        <h2 class="text-xl font-semibold text-slate-800 dark:text-slate-100">Analytics</h2>
        <div v-if="loadingAnalytics" class="text-slate-500">Loading analytics…</div>
        <div v-else class="grid gap-10">
          <!-- TDEE over time -->
          <div class="w-full h-72 bg-white/50 dark:bg-slate-800/50 rounded-xl p-4">
            <Chart type="line" :data="{
              labels: recentTdee.map(r => new Date(r.createdAt).toLocaleString()),
              datasets: [{ label: 'TDEE (kcal)', data: recentTdee.map(r => r.tdeeKcal), borderColor: '#38bdf8', backgroundColor: 'rgba(56,189,248,0.3)', fill: false }]
            }" :options="{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }" />
            <div v-if="recentTdee.length === 0" class="text-center text-slate-500 mt-2">No TDEE data yet</div>
          </div>

          <!-- TDEE sex distribution -->
          <div class="w-full h-72 bg-white/50 dark:bg-slate-800/50 rounded-xl p-4">
            <Chart type="pie" :data="{
              labels: ['Male', 'Female'],
              datasets: [{ data: [recentTdee.filter(r => r.sex === 0).length, recentTdee.filter(r => r.sex === 1).length], backgroundColor: ['#38bdf8', '#f87171'] }]
            }" :options="{ responsive: true, maintainAspectRatio: false }" />
            <div v-if="recentTdee.length === 0" class="text-center text-slate-500 mt-2">No TDEE data yet</div>
          </div>

          <!-- 1RM over time -->
          <div class="w-full h-72 bg-white/50 dark:bg-slate-800/50 rounded-xl p-4">
            <Chart type="line" :data="{
              labels: recentOrm.map(r => new Date(r.createdAt).toLocaleString()),
              datasets: [{ label: '1RM', data: recentOrm.map(r => r.oneRm), borderColor: '#34d399', backgroundColor: 'rgba(52,211,153,0.3)', fill: false }]
            }" :options="{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }" />
            <div v-if="recentOrm.length === 0" class="text-center text-slate-500 mt-2">No 1RM data yet</div>
          </div>

          <!-- 1RM reps distribution -->
          <div class="w-full h-72 bg-white/50 dark:bg-slate-800/50 rounded-xl p-4">
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
      </section>
    </div>
  </main>
</template>

<script lang="ts">
export default {
  data() {
    return {
      ormWeight: 100 as number,
      ormReps: 5 as number,
      orm: null as number | null
    }
  },
  methods: {
    async calcOrm() {
      const res = await fetch('/api/one-rep-max', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ weight: this.ormWeight, reps: this.ormReps })
      })
      if (!res.ok) return
      const json = await res.json()
      this.orm = json.oneRepMax
      ;(this as any).$nextTick(() => {})
      // refresh analytics
      ;(loadAnalytics as any)()
    }
  }
}
</script>