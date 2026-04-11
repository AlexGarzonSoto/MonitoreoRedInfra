<template>
  <div class="pt-20 px-6 pb-10 min-h-screen bg-netwatch-dark text-slate-200">
    <!-- ── Cabecera ─────────────────────────────────────────────────────────── -->
    <div class="flex flex-col md:flex-row md:items-center justify-between mb-6 gap-4">
      <div>
        <h1 class="text-2xl font-bold text-netwatch-accent mb-1">Topología de Red</h1>
        <p class="text-slate-400 text-sm">
          Visualización de las conexiones IP detectadas. Tamaño del nodo = frecuencia. Color = severidad máxima.
        </p>
      </div>
      <div class="flex gap-3 items-center flex-wrap">
        <!-- Leyenda de colores -->
        <div class="flex gap-3 text-xs">
          <span v-for="s in severities" :key="s.key" class="flex items-center gap-1">
            <span class="w-3 h-3 rounded-full inline-block" :style="{ background: s.color }"></span>
            {{ s.label }}
          </span>
        </div>
        <button @click="loadAndRender"
                class="px-4 py-2 rounded-lg bg-netwatch-panel border border-netwatch-border text-sm hover:bg-netwatch-dark transition-all">
          Actualizar
        </button>
      </div>
    </div>

    <!-- ── Estadísticas rápidas ─────────────────────────────────────────────── -->
    <div class="grid grid-cols-3 md:grid-cols-6 gap-3 mb-6">
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-lg font-bold text-white">{{ stats.nodes }}</p>
        <p class="text-xs text-slate-400">IPs únicas</p>
      </div>
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-lg font-bold text-white">{{ stats.links }}</p>
        <p class="text-xs text-slate-400">Conexiones</p>
      </div>
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-lg font-bold text-red-400">{{ stats.critical }}</p>
        <p class="text-xs text-slate-400">Críticas</p>
      </div>
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-lg font-bold text-orange-400">{{ stats.high }}</p>
        <p class="text-xs text-slate-400">Altas</p>
      </div>
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-lg font-bold text-white">{{ stats.events }}</p>
        <p class="text-xs text-slate-400">Eventos</p>
      </div>
      <div class="bg-netwatch-panel border border-netwatch-border rounded-lg p-3 text-center">
        <p class="text-xs font-medium" :class="loading ? 'text-yellow-400' : 'text-green-400'">
          {{ loading ? 'Cargando...' : 'Listo' }}
        </p>
        <p class="text-xs text-slate-400">Estado</p>
      </div>
    </div>

    <!-- ── Canvas SVG ───────────────────────────────────────────────────────── -->
    <div class="bg-netwatch-panel border border-netwatch-border rounded-xl overflow-hidden relative">
      <svg ref="svgRef" class="w-full" :style="{ height: svgHeight + 'px' }">
        <defs>
          <marker id="arrow" markerWidth="6" markerHeight="6" refX="6" refY="3" orient="auto">
            <path d="M0,0 L6,3 L0,6 Z" fill="#475569" />
          </marker>
        </defs>
      </svg>

      <!-- Tooltip flotante -->
      <div
        v-if="tooltip.visible"
        class="absolute pointer-events-none bg-netwatch-dark border border-netwatch-border rounded-lg p-3 text-xs shadow-xl z-10 min-w-48"
        :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
      >
        <p class="font-mono font-bold text-netwatch-accent mb-1">{{ tooltip.ip }}</p>
        <p class="text-slate-400">Eventos:
          <span class="text-white">{{ tooltip.count }}</span>
        </p>
        <p class="text-slate-400">Severidad:
          <span :style="{ color: severityColor(tooltip.severity) }">{{ tooltip.severity }}</span>
        </p>
        <p v-if="tooltip.country" class="text-slate-400">País:
          <span class="text-white">{{ tooltip.country }}</span>
        </p>
        <p class="text-slate-400 mt-1">Amenazas:
          <span class="text-white text-xs">{{ tooltip.threats }}</span>
        </p>
      </div>
    </div>

    <!-- ── Tabla de nodos ───────────────────────────────────────────────────── -->
    <div class="mt-6 bg-netwatch-panel border border-netwatch-border rounded-xl p-4">
      <h2 class="text-sm font-semibold text-white mb-3">Nodos detectados</h2>
      <div class="overflow-x-auto">
        <table class="w-full text-xs">
          <thead>
            <tr class="text-left text-slate-400 border-b border-netwatch-border">
              <th class="pb-2 pr-4">IP</th>
              <th class="pb-2 pr-4">Eventos</th>
              <th class="pb-2 pr-4">Severidad máx.</th>
              <th class="pb-2 pr-4">Tipos de amenaza</th>
              <th class="pb-2">País</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="node in sortedNodes"
              :key="node.id"
              class="border-b border-netwatch-border/40 hover:bg-netwatch-dark/50"
            >
              <td class="py-1.5 pr-4 font-mono text-netwatch-accent">{{ node.id }}</td>
              <td class="py-1.5 pr-4 text-white">{{ node.count }}</td>
              <td class="py-1.5 pr-4">
                <span class="px-2 py-0.5 rounded text-xs" :style="{ background: severityBg(node.severity), color: severityColor(node.severity) }">
                  {{ node.severity || 'INFO' }}
                </span>
              </td>
              <td class="py-1.5 pr-4 text-slate-400">{{ [...node.threats].join(', ') }}</td>
              <td class="py-1.5 text-slate-400">{{ node.country || '' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import * as d3 from 'd3'
import { eventsAPI } from '@/services/api'

const svgRef  = ref(null)
const loading = ref(false)
const tooltip = ref({ visible: false, x: 0, y: 0, ip: '', count: 0, severity: '', threats: '', country: '' })
const nodes   = ref([])
const links   = ref([])
const svgHeight = ref(580)

let simulation = null

const severities = [
  { key: 'CRITICAL', label: 'Crítica',  color: '#ef4444' },
  { key: 'HIGH',     label: 'Alta',     color: '#f97316' },
  { key: 'MEDIUM',   label: 'Media',    color: '#f59e0b' },
  { key: 'LOW',      label: 'Baja',     color: '#22c55e' },
  { key: 'INFO',     label: 'Info',     color: '#6b7280' }
]

const SEVERITY_ORDER = ['INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const stats = computed(() => ({
  nodes:    nodes.value.length,
  links:    links.value.length,
  events:   nodes.value.reduce((s, n) => s + n.count, 0),
  critical: nodes.value.filter(n => n.severity === 'CRITICAL').length,
  high:     nodes.value.filter(n => n.severity === 'HIGH').length
}))

const sortedNodes = computed(() =>
  [...nodes.value].sort((a, b) =>
    SEVERITY_ORDER.indexOf(b.severity) - SEVERITY_ORDER.indexOf(a.severity)
  )
)

function severityColor(sev) {
  const map = { CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#f59e0b', LOW: '#22c55e', INFO: '#6b7280' }
  return map[sev] || '#6b7280'
}

function severityBg(sev) {
  const map = { CRITICAL: 'rgba(239,68,68,0.15)', HIGH: 'rgba(249,115,22,0.15)', MEDIUM: 'rgba(245,158,11,0.15)', LOW: 'rgba(34,197,94,0.15)', INFO: 'rgba(107,114,128,0.15)' }
  return map[sev] || 'rgba(107,114,128,0.15)'
}

async function loadAndRender() {
  loading.value = true
  clearGraph()

  try {
    const { data } = await eventsAPI.getAll({ size: 300, sort: 'timestamp,desc' })
    const events = data.content || []
    processEvents(events)
    renderGraph()
  } catch (e) {
    console.error('Error cargando eventos para topología:', e)
  } finally {
    loading.value = false
  }
}

function processEvents(events) {
  const nodeMap = new Map()  // ip → { count, severity, threats, country }
  const linkMap = new Map()  // src→dst → { count, severity }

  for (const ev of events) {
    const src = ev.srcIp
    const dst = ev.dstIp || '10.0.0.100'

    // Nodo origen
    if (!nodeMap.has(src)) nodeMap.set(src, { count: 0, severity: 'INFO', threats: new Set(), country: ev.country })
    const srcNode = nodeMap.get(src)
    srcNode.count++
    if (ev.severity && SEVERITY_ORDER.indexOf(ev.severity) > SEVERITY_ORDER.indexOf(srcNode.severity)) {
      srcNode.severity = ev.severity
    }
    if (ev.threatType && ev.threatType !== 'NORMAL') srcNode.threats.add(ev.threatType)

    // Nodo destino
    if (!nodeMap.has(dst)) nodeMap.set(dst, { count: 0, severity: 'INFO', threats: new Set(), country: '' })
    nodeMap.get(dst).count++

    // Link
    const linkKey = `${src}→${dst}`
    if (!linkMap.has(linkKey)) linkMap.set(linkKey, { source: src, target: dst, count: 0, severity: 'INFO' })
    const lk = linkMap.get(linkKey)
    lk.count++
    if (ev.severity && SEVERITY_ORDER.indexOf(ev.severity) > SEVERITY_ORDER.indexOf(lk.severity)) {
      lk.severity = ev.severity
    }
  }

  nodes.value = Array.from(nodeMap.entries()).map(([id, d]) => ({ id, ...d }))
  links.value = Array.from(linkMap.values())
}

function clearGraph() {
  if (simulation) { simulation.stop(); simulation = null }
  if (svgRef.value) d3.select(svgRef.value).selectAll('*:not(defs)').remove()
}

function renderGraph() {
  const svg    = d3.select(svgRef.value)
  const width  = svgRef.value.getBoundingClientRect().width || 900
  const height = svgHeight.value

  const nodeData = nodes.value.map(n => ({ ...n }))
  const linkData = links.value.map(l => ({ ...l }))

  // Escala de radio según número de eventos
  const maxCount = Math.max(...nodeData.map(n => n.count), 1)
  const rScale   = d3.scaleSqrt().domain([1, maxCount]).range([5, 22])

  simulation = d3.forceSimulation(nodeData)
    .force('link', d3.forceLink(linkData).id(d => d.id).distance(100).strength(0.4))
    .force('charge', d3.forceManyBody().strength(-250))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collision', d3.forceCollide().radius(d => rScale(d.count) + 8))

  // Links
  const link = svg.append('g').attr('class', 'links')
    .selectAll('line')
    .data(linkData)
    .join('line')
    .attr('stroke', d => severityColor(d.severity))
    .attr('stroke-opacity', 0.35)
    .attr('stroke-width', d => Math.min(1 + Math.log(d.count), 5))
    .attr('marker-end', 'url(#arrow)')

  // Nodos
  const node = svg.append('g').attr('class', 'nodes')
    .selectAll('g')
    .data(nodeData)
    .join('g')
    .call(
      d3.drag()
        .on('start', (e, d) => { if (!e.active) simulation.alphaTarget(0.3).restart(); d.fx = d.x; d.fy = d.y })
        .on('drag',  (e, d) => { d.fx = e.x; d.fy = e.y })
        .on('end',   (e, d) => { if (!e.active) simulation.alphaTarget(0); d.fx = null; d.fy = null })
    )
    .on('mouseover', (event, d) => {
      const rect = svgRef.value.getBoundingClientRect()
      tooltip.value = {
        visible: true,
        x: event.clientX - rect.left + 15,
        y: event.clientY - rect.top  - 10,
        ip:       d.id,
        count:    d.count,
        severity: d.severity,
        threats:  [...d.threats].join(', ') || 'NORMAL',
        country:  d.country || ''
      }
    })
    .on('mousemove', (event) => {
      const rect = svgRef.value.getBoundingClientRect()
      tooltip.value.x = event.clientX - rect.left + 15
      tooltip.value.y = event.clientY - rect.top  - 10
    })
    .on('mouseout', () => { tooltip.value.visible = false })

  // Círculo exterior (glow para críticos)
  node.filter(d => d.severity === 'CRITICAL' || d.severity === 'HIGH')
    .append('circle')
    .attr('r', d => rScale(d.count) + 5)
    .attr('fill', 'none')
    .attr('stroke', d => severityColor(d.severity))
    .attr('stroke-opacity', 0.25)
    .attr('stroke-width', 2)

  // Círculo principal
  node.append('circle')
    .attr('r', d => rScale(d.count))
    .attr('fill', d => severityColor(d.severity))
    .attr('fill-opacity', 0.8)
    .attr('stroke', d => severityColor(d.severity))
    .attr('stroke-width', 1.5)

  // Etiqueta IP
  node.append('text')
    .attr('dy', d => rScale(d.count) + 12)
    .attr('text-anchor', 'middle')
    .attr('font-size', '9px')
    .attr('fill', '#94a3b8')
    .text(d => d.id)

  simulation.on('tick', () => {
    link
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)

    node.attr('transform', d =>
      `translate(${Math.max(25, Math.min(width - 25, d.x))},${Math.max(25, Math.min(height - 25, d.y))})`)
  })
}

onMounted(loadAndRender)
onUnmounted(() => { if (simulation) simulation.stop() })
</script>
