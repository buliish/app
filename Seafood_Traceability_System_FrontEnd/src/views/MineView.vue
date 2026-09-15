<template>
  <!--
    「我的」页面 —— 展示当前登录企业（流通节点）的详细信息，并提供退出登录入口。
  -->
  <div class="page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="app-header">
      <img :src="seafoodLogo" alt="logo" class="app-header-logo" />
      <h1 class="app-header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body" v-loading="loading">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 企业信息 -->
      <div class="detail-table">
        <div class="cell">
          <div class="cell-label">企业名称</div>
          <div class="cell-value">{{ info.name || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">溯源节点类型</div>
          <div class="cell-value">
            <el-tag size="small" effect="dark" :style="typeTagStyle(info.nodeType)">{{ typeName }}</el-tag>
          </div>
        </div>
        <div class="cell">
          <div class="cell-label">企业编码</div>
          <div class="cell-value">{{ info.code || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">营业执照号</div>
          <div class="cell-value">{{ info.businessId || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">企业法人</div>
          <div class="cell-value">{{ info.corporation || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">联系电话</div>
          <div class="cell-value">{{ info.telephone || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">所在区域</div>
          <div class="cell-value">{{ regionText }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">详细地址</div>
          <div class="cell-value">{{ info.address || '—' }}</div>
        </div>
        <div class="cell">
          <div class="cell-label">注册时间</div>
          <div class="cell-value">{{ formatDate(info.regDate) }}</div>
        </div>
      </div>
    </main>

    <BottomNav active="mine" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { getNodeInfoApi } from '../api/node'
import { citiesApi, provincesApi } from '../api/region'
import { TYPE_NAME, getLoginNode, typeTagStyle } from '../utils/nodeType'

const router = useRouter()
const node = getLoginNode()

const info = reactive({})
const loading = ref(false)
const provName = ref('')
const cityName = ref('')

const typeName = computed(() => TYPE_NAME[node.nodeType] || '未知类型')
const regionText = computed(() =>
  provName.value || cityName.value ? `${provName.value} ${cityName.value}` : '—'
)

onMounted(async () => {
  if (!node.code) {
    router.replace('/login')
    return
  }
  loading.value = true
  try {
    const res = await getNodeInfoApi(node.code)
    Object.assign(info, res.data || {}, { nodeType: (res.data || {}).nodeType || node.nodeType })

    // 省市区 ID 转名称
    const provId = res.data?.provId || node.provId
    const cityId = res.data?.cityId || node.cityId
    if (provId) {
      const provinces = await provincesApi()
      provName.value = ((provinces.data || []).find((p) => p.provId === provId) || {}).provName || ''
      if (cityId) {
        const cities = await citiesApi(provId)
        cityName.value = ((cities.data || []).find((c) => c.cityId === cityId) || {}).cityName || ''
      }
    }
  } finally {
    loading.value = false
  }
})

// 注册时间只保留日期部分
function formatDate(v) {
  return v ? String(v).slice(0, 10) : '—'
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏（与首页一致） */



/* 主体可滚动区 */
.body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 540px;
  width: 100%;
  margin: 0 auto;
}

/* banner */


/* 企业信息表格：字段名粗体 + 值行交替 */
.detail-table {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}

.cell {
  border-bottom: 1px solid #dcdfe6;
}

.cell:last-child {
  border-bottom: none;
}

.cell-label {
  padding: 10px 14px 4px;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.cell-value {
  padding: 2px 14px 10px;
  font-size: 15px;
  color: #303133;
  word-break: break-all;
}
</style>
