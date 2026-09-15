<template>
  <!--
    节点企业注册信息统计面板
    同一套图表逻辑既用于管理页右栏（light），也用于独立可视化大屏（dark）
  -->
  <div class="stats-panel" :class="[theme, { fill }]" v-loading="loading">
    <!-- 顶部指标卡（仅大屏使用） -->
    <div v-if="showSummary" class="summary-row">
      <div v-for="item in summary" :key="item.label" class="summary-card">
        <div class="summary-value">{{ item.value }}</div>
        <div class="summary-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 图 1：近十二个月注册数量趋势（折线图） -->
    <div class="chart-card" :class="theme">
      <div class="chart-title">近十二个月企业注册数量趋势</div>
      <div ref="trendRef" class="chart" :style="{ height: chartHeight }"></div>
    </div>

    <div class="chart-row" :style="{ gridTemplateColumns: rowColumns }">
      <!-- 图 2：按省分组注册数量分布（饼图） -->
      <div class="chart-card" :class="theme">
        <div class="chart-title">按省分组注册数量分布</div>
        <div ref="provPieRef" class="chart" :style="{ height: chartHeight }"></div>
      </div>

      <!-- 图 3：按企业类型分组注册数量分布（饼图） -->
      <div class="chart-card" :class="theme">
        <div class="chart-title">按企业类型分组注册数量分布</div>
        <div ref="typePieRef" class="chart" :style="{ height: chartHeight }"></div>
      </div>
    </div>

    <!-- 图 4：各省注册数量统计（柱状图） -->
    <div class="chart-card" :class="theme">
      <div class="chart-title">各省节点企业注册数量统计</div>
      <div ref="provBarRef" class="chart" style="height: 260px"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { adminStatsApi } from '../api/admin'
import {
  THEMES,
  TYPE_COLORS,
  PROV_COLORS_LIGHT,
  PROV_COLORS_DARK,
  axisTooltip,
  pieTooltip,
  axisBase
} from '../utils/chartTheme'

const props = defineProps({
  /** light：管理页右栏；dark：可视化大屏 */
  theme: { type: String, default: 'light' },
  /** 是否展示顶部指标卡（大屏用） */
  showSummary: { type: Boolean, default: false },
  /** 图表高度 */
  chartHeight: { type: String, default: '210px' },
  /** 自动刷新间隔（毫秒），0 表示不刷新 */
  pollMs: { type: Number, default: 0 }
})

const loading = ref(false)
const stats = ref({ typeDist: [], provDist: [], provBar: [], trend: { months: [], counts: [] } })

const trendRef = ref()
const provPieRef = ref()
const typePieRef = ref()
const provBarRef = ref()

let trendChart = null
let provPieChart = null
let typePieChart = null
let provBarChart = null
let timer = null

const theme = computed(() => THEMES[props.theme] || THEMES.light)

// 大屏两列，管理页右栏也是两列；窄屏时改为单列
const rowColumns = ref('1fr 1fr')

// 顶部指标卡：企业总数 + 四类企业数 + 覆盖省份数。
// 总数优先用后端返回的 total（本地求和遇到 type 有未知值时会对不上），
// 缺失时再退回按类型分布求和。
const summary = computed(() => {
  const dist = stats.value.typeDist || []
  const total = stats.value.total ?? dist.reduce((s, d) => s + Number(d.value || 0), 0)
  const find = (name) => Number((dist.find((d) => d.name === name) || {}).value || 0)
  return [
    { label: '注册企业总数', value: total },
    { label: '养殖企业', value: find('养殖企业') },
    { label: '冷冻加工企业', value: find('冷冻加工企业') },
    { label: '批发商', value: find('批发商') },
    { label: '零售商', value: find('零售商') },
    { label: '覆盖省份', value: (stats.value.provDist || []).length }
  ]
})

async function loadStats() {
  loading.value = true
  try {
    const res = await adminStatsApi()
    stats.value = res.data || {}
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const t = theme.value
  const base = axisBase(t)
  const trend = stats.value.trend || { months: [], counts: [] }

  // 1）折线图
  trendChart = trendChart || echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: axisTooltip(t),
    grid: { left: 45, right: 20, top: 30, bottom: 45 },
    xAxis: { type: 'category', data: trend.months, axisLabel: { ...base.axisLabel, rotate: 40 }, axisLine: base.axisLine },
    yAxis: { type: 'value', name: '注册数量', minInterval: 1, ...base },
    series: [
      {
        name: '注册数量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.counts,
        itemStyle: { color: t.lineColor },
        areaStyle: { color: t.lineArea }
      }
    ]
  })

  // 2）省分布饼图：7 个省名较长，饼上直接标名称会互相重叠，
  //    因此标签只留数量、省份名称交给底部图例（配合 tooltip 看全名）
  provPieChart = provPieChart || echarts.init(provPieRef.value)
  provPieChart.setOption({
    tooltip: pieTooltip(t, () => stats.value.provDist),
    color: props.theme === 'dark' ? PROV_COLORS_DARK : PROV_COLORS_LIGHT,
    legend: {
      bottom: 0,
      type: 'scroll',
      itemWidth: 10,
      itemHeight: 10,
      // 省名普遍 3~6 字，默认图例宽度会换行挤压饼图
      itemGap: 8,
      textStyle: { color: t.legendColor, fontSize: 11 }
    },
    series: [
      {
        name: '省分布',
        type: 'pie',
        radius: ['38%', '62%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: { formatter: '{c} 家', fontSize: 11, color: t.axisColor },
        labelLine: { length: 8, length2: 8 },
        data: stats.value.provDist || []
      }
    ]
  })

  // 3）类型分布饼图（固定配色，与管理端类型标签一致）
  typePieChart = typePieChart || echarts.init(typePieRef.value)
  typePieChart.setOption({
    tooltip: pieTooltip(t, () => stats.value.typeDist),
    legend: { bottom: 0, type: 'scroll', itemWidth: 10, itemHeight: 10, textStyle: { color: t.legendColor, fontSize: 11 } },
    color: TYPE_COLORS,
    series: [
      {
        name: '类型分布',
        type: 'pie',
        radius: '55%',
        center: ['50%', '44%'],
        label: { formatter: '{b}: {c}', fontSize: 11, color: t.axisColor },
        data: stats.value.typeDist || []
      }
    ]
  })

  // 4）各省柱状图
  const provBar = stats.value.provBar || []
  provBarChart = provBarChart || echarts.init(provBarRef.value)
  provBarChart.setOption({
    tooltip: axisTooltip(t),
    grid: { left: 45, right: 20, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: provBar.map((d) => d.name),
      axisLabel: { ...base.axisLabel, rotate: 30, fontSize: 11 },
      axisLine: base.axisLine
    },
    yAxis: { type: 'value', name: '注册数量', minInterval: 1, ...base },
    series: [
      {
        name: '注册数量',
        type: 'bar',
        barWidth: '45%',
        itemStyle: { color: t.barColor, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', fontSize: 11, color: t.axisColor },
        data: provBar.map((d) => d.value)
      }
    ]
  })
}

function handleResize() {
  ;[trendChart, provPieChart, typePieChart, provBarChart].forEach((c) => c && c.resize())
}

// 主题切换时用新的配色重绘
watch(theme, () => {
  renderCharts()
})

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
  if (props.pollMs > 0) {
    timer = setInterval(loadStats, props.pollMs)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (timer) clearInterval(timer)
  ;[trendChart, provPieChart, typePieChart, provBarChart].forEach((c) => c && c.dispose())
  trendChart = provPieChart = typePieChart = provBarChart = null
})

defineExpose({ refresh: loadStats })
</script>

<style scoped>
.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-card {
  border-radius: 8px;
  padding: 10px 12px 4px;
  background: #fff;
  border: 1px solid #e6ebf2;
}

/* 深色主题卡片 */
.chart-card.dark {
  background: rgba(13, 36, 64, 0.72);
  border: 1px solid rgba(58, 160, 220, 0.28);
  box-shadow: inset 0 0 24px rgba(58, 160, 220, 0.08);
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: #12315a;
  padding: 2px 0 6px;
}

.chart-card.dark .chart-title {
  color: #dceaf7;
}

.chart-row {
  display: grid;
  gap: 12px;
}

.chart {
  width: 100%;
  height: 210px;
}

/* 顶部指标卡（大屏）：6 张（总数/四类企业/覆盖省份），窄屏自动降列 */
.summary-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.summary-card {
  border-radius: 8px;
  padding: 14px 10px;
  text-align: center;
  background: rgba(13, 36, 64, 0.72);
  border: 1px solid rgba(58, 160, 220, 0.28);
  transition: border-color 0.25s, box-shadow 0.25s;
}

.summary-card:hover {
  border-color: rgba(58, 160, 220, 0.7);
  box-shadow: 0 0 18px rgba(58, 160, 220, 0.25);
}

.summary-value {
  font-size: 26px;
  font-weight: 700;
  color: #3adc9a;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.summary-label {
  margin-top: 4px;
  font-size: 12px;
  color: #8fa9c4;
}

@media (max-width: 900px) {
  .chart-row {
    grid-template-columns: 1fr !important;
  }
}
</style>
