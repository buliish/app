<template>
  <!--
    浏览产品批号详情界面（参考原型图设计）
    初始化时查询该产品批号信息并以「字段名（粗体）/ 值」交替的表格样式展示；
    冷冻加工企业额外展示加工工序记录；零售商已确认批号展示溯源标识码。
  -->
  <div class="page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="page-header">
      <img :src="seafoodLogo" alt="logo" class="header-logo" />
      <h1 class="header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body" v-loading="loading">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 批号详情：字段名（粗体）+ 值 交替的边框表格 -->
      <div class="detail-table">
        <div v-for="item in infoItems" :key="item.label" class="cell">
          <div class="cell-label">{{ item.label }}</div>
          <div class="cell-value" :class="{ tag: item.tag }">
            <el-tag v-if="item.tag" :type="statusTagType(detail.status)" size="small">
              {{ statusText(nodeType, detail.status) }}
            </el-tag>
            <template v-else>{{ item.value || '—' }}</template>
          </div>
        </div>
      </div>

      <!-- 溯源标识码（零售商已确认批号） -->
      <div v-if="nodeType === 4 && detail.traceCode" class="trace-box">
        <div class="cell-label">溯源标识码</div>
        <div class="trace-row">
          <span class="code-text">{{ detail.traceCode }}</span>
          <button type="button" class="trace-btn" @click="router.push('/trace')">前往溯源查询</button>
        </div>
      </div>

      <!-- 加工工序记录（冷冻加工企业） -->
      <template v-if="nodeType === 2">
        <div class="cell-label process-title">加工工序记录</div>
        <el-empty v-if="records.length === 0" description="暂无工序记录" :image-size="80" />
        <div v-else class="detail-table process-table">
          <div v-for="(rec, i) in records" :key="rec.id || i" class="cell">
            <div class="cell-label">{{ rec.step }}（{{ rec.stepTime }}）</div>
            <div class="cell-value">
              {{ rec.temperature || '—' }}<template v-if="rec.operator"> · 操作人：{{ rec.operator }}</template><template v-if="rec.remark"> · {{ rec.remark }}</template>
            </div>
          </div>
        </div>
      </template>
    </main>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { batchDetailApi, processListApi } from '../api/batch'
import { citiesApi, nodesApi, provincesApi } from '../api/region'
import {
  UPSTREAM_NAME,
  UPSTREAM_TYPE,
  getLoginNode,
  statusTagType,
  statusText
} from '../utils/nodeType'

const route = useRoute()
const router = useRouter()

const node = getLoginNode()
const nodeType = node.nodeType || 1
const upstreamType = computed(() => UPSTREAM_TYPE[nodeType])
const upstreamName = computed(() => UPSTREAM_NAME[upstreamType.value] || '上游企业')

const detail = reactive({})
const records = ref([])
const loading = ref(false)
const provName = ref('')
const cityName = ref('')
const upNodeName = ref('')

// 详情字段按角色组装：养殖显示检疫合格证/官方检疫员，其余环节显示进场信息
const infoItems = computed(() => {
  const items = [
    { label: '产品批号', value: detail.batchNo },
    { label: '产品品种', value: detail.breed }
  ]
  if (nodeType === 1) {
    items.push(
      { label: '养殖阶段', value: detail.breedStage },
      { label: '动物检验检疫合格证', value: detail.quarantineNo },
      { label: '官方检疫员名称', value: detail.inspector }
    )
  } else {
    items.push(
      { label: '产品类型', value: detail.productType },
      { label: `${upstreamName.value}产品批号`, value: detail.upBatchNo },
      { label: `${upstreamName.value}名称`, value: upNodeName.value },
      {
        label: `${upstreamName.value}所在区域`,
        value: provName.value || cityName.value ? `${provName.value} ${cityName.value}` : ''
      }
    )
  }
  items.push(
    { label: '状态', value: detail.status, tag: true },
    { label: '创建时间', value: detail.createTime },
    { label: '更新时间', value: detail.updateTime }
  )
  return items
})

onMounted(async () => {
  // 初始化：查询该产品批号信息并展示
  loading.value = true
  try {
    const res = await batchDetailApi(nodeType, route.params.id)
    Object.assign(detail, res.data || {})

    if (nodeType !== 1 && detail.provId) {
      const provinces = await provincesApi()
      provName.value = ((provinces.data || []).find((p) => p.provId === detail.provId) || {}).provName || ''
      const cities = await citiesApi(detail.provId)
      cityName.value = ((cities.data || []).find((c) => c.cityId === detail.cityId) || {}).cityName || ''
      if (detail.upNodeId) {
        const nodes = await nodesApi(upstreamType.value, detail.provId, detail.cityId)
        upNodeName.value = ((nodes.data || []).find((n) => n.nodeId === detail.upNodeId) || {}).name || ''
      }
    }

    if (nodeType === 2) {
      const res2 = await processListApi(route.params.id)
      records.value = res2.data || []
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏（与功能菜单页一致） */
.page-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  border-bottom: 1px solid #0b4f8c;
}

.header-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 6px;
  background-color: #fff;
  padding: 2px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

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
.banner-wrap {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(11, 79, 140, 0.08);
}

.banner-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

/* 详情表格：外边框 + 分隔线，字段名粗体行 + 值行交替 */
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

/* 溯源标识码 */
.trace-box {
  margin-top: 18px;
  padding-bottom: 10px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.trace-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 2px 14px 10px;
}

.code-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #0f9d58;
}

.trace-btn {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #1d6fb8;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.trace-btn:hover {
  background: #0b4f8c;
}

/* 加工工序记录 */
.process-title {
  margin-top: 22px;
  padding: 0 2px;
}

.process-table {
  margin-top: 8px;
}
</style>
