<template>
  <div :class="['bg-netwatch-panel border rounded-xl p-5 flex flex-col gap-1', borderColor]">
    <span class="text-xs text-slate-400 uppercase tracking-wider">{{ titulo }}</span>
    <span :class="['text-3xl font-bold tabular-nums', textColor]">{{ valor }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  titulo: { type: String, required: true },
  valor:  { type: Number, default: 0 },
  color:  { type: String, default: 'blue' }
})

const colorMap = {
  blue:   { border: 'border-blue-800',   text: 'text-blue-400'   },
  yellow: { border: 'border-yellow-800', text: 'text-yellow-400' },
  red:    { border: 'border-red-800',    text: 'text-red-400'    },
  orange: { border: 'border-orange-800', text: 'text-orange-400' },
  green:  { border: 'border-green-800',  text: 'text-green-400'  }
}

const borderColor = computed(() => colorMap[props.color]?.border ?? 'border-netwatch-border')
const textColor   = computed(() => colorMap[props.color]?.text   ?? 'text-slate-100')
</script>
