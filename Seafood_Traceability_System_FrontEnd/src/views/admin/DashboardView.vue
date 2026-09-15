<template>
  <!--
    可视化大屏 —— 独立全屏页面（/sys/dashboard）
    深色科技风：顶部标题栏 + 指标卡 + 四张统计图，30 秒自动刷新
    设计稿按 1600px 宽排版，用 transform: scale 自适应窗口
  -->
  <div class="dash-page">
    <div class="dash-scale" :style="scaleStyle">
      <header class="dash-header">
        <div class="header-left">
          <img :src="seafoodLogo" alt="logo" class="header-logo" />
        </div>
        <h1 class="header-title">冷冻对虾全产业链溯源系统 · 数据可视化大屏</h1>
        <div class="header-right">
          <span class="clock">{{ now }}</span>
          <button type="button" class="ghost-btn" @click="router.push('/sys/trace')">批次追溯查询</button>
          <button type="button" class="ghost-btn" @click="router.push('/sys/nodes')">企业注册管理</button>
          <button type="button" class="ghost-btn" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="dash-body">
        <StatsPanel ref="panelRef" theme="dark" show-summary poll-ms="30000" chart-height="190px" />
      </main>

      <footer class="dash-footer">
        <span>数据每 30 秒自动刷新</span>
        <span class="dot">·</span>
        <span>最后更新：{{ lastUpdate }}</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatsPanel from '../../components/StatsPanel.vue'
import seafoodLogo from '../../assets/images/海鲜.png'

const router = useRouter()
const panelRef = ref()

// ---------- 自适应缩放 ----------
const DESIGN_WIDTH = 1600
const scale = ref(1)

function updateScale() {
  scale.value = Math.min(window.innerWidth / DESIGN_WIDTH, 1)
}

// ---------- 时钟 ----------
const now = ref('')
const lastUpdate = ref('')
let clockTimer = null

function pad(n) {
  return String(n).padStart(2, '0')
}

function tick() {
  const d = new Date()
  now.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  lastUpdate.value = `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const scaleStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transformOrigin: 'top center',
  width: `${DESIGN_WIDTH}px`
}))

function handleLogout() {
  localStorage.removeItem('adminToken')
  router.replace('/sys/login')
}

onMounted(() => {
  updateScale()
  tick()
  window.addEventListener('resize', updateScale)
  clockTimer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateScale)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.dash-page {
  min-height: 100vh;
  background: radial-gradient(circle at 50% 0%, #0d2440 0%, #061726 60%, #030b14 100%);
  overflow-x: hidden;
  padding-top: 12px;
}

.dash-scale {
  margin: 0 auto;
}

/* 顶部标题栏 */
.dash-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 62px;
  padding: 0 24px;
  background: linear-gradient(90deg, rgba(13, 36, 64, 0.2), rgba(29, 111, 184, 0.45), rgba(13, 36, 64, 0.2));
  border-bottom: 1px solid rgba(58, 160, 220, 0.4);
}

.header-logo {
  width: 34px;
  height: 34px;
  border-radius: 6px;
}

.header-left {
  position: absolute;
  left: 24px;
}

.header-right {
  position: absolute;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #dceaf7;
  text-shadow: 0 0 18px rgba(58, 160, 220, 0.7);
}

.clock {
  font-size: 13px;
  color: #8fa9c4;
  font-variant-numeric: tabular-nums;
}

.ghost-btn {
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
  color: #8fc4ec;
  background: transparent;
  border: 1px solid rgba(58, 160, 220, 0.5);
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.ghost-btn:hover {
  background: rgba(58, 160, 220, 0.18);
  color: #fff;
}

/* 主体：左右留白，四张图铺满 */
.dash-body {
  padding: 16px 24px 8px;
}

.dash-footer {
  padding: 8px 24px 18px;
  text-align: center;
  font-size: 12px;
  color: #5f7a95;
}

.dot {
  margin: 0 8px;
}
</style>
