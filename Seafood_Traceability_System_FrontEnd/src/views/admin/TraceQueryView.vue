<template>
  <!--
    管理端批次追溯查询（/sys/trace）。

    与消费者端的区别是"不做状态门槛"：管理端要能查到已下架批号、
    要看得到链路断在哪一级、要看得到各环节检测记录——这些在消费者端是刻意隐藏的。
    因此这里展示的信息比商品溯源页更全。
  -->
  <div class="admin-page">
    <header class="admin-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <h1>冷冻对虾全产业链溯源系统 · 批次追溯查询</h1>
      <div class="header-actions">
        <el-link class="nav-link" @click="router.push('/sys/nodes')">节点企业管理</el-link>
        <el-link class="nav-link" @click="router.push('/sys/dashboard')">可视化大屏</el-link>
        <button type="button" class="header-logout" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <el-main class="admin-body">
      <div class="container">
        <!-- 追溯概况 -->
        <div class="stat-row">
          <div v-for="s in statCards" :key="s.label" class="stat-card">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>

        <!-- 查询区 -->
        <el-card shadow="never" class="card">
          <template #header><span class="section-title">按溯源码 / 批号 / 产品编号查询</span></template>
          <div class="search-row">
            <el-input
              v-model="keyword"
              placeholder="如 SHZ202601010001 / RETA20260101 / 产品编号"
              clearable
              @keyup.enter="handleSearch"
              @clear="reset"
            />
            <el-button type="primary" :loading="loading" @click="handleSearch">查 询</el-button>
          </div>

          <!-- 命中多条时列出候选，由管理员选中具体批次 -->
          <div v-if="candidates.length" class="candidate-list">
            <div class="candidate-tip">命中 {{ candidates.length }} 条，点击查看完整链路：</div>
            <div
              v-for="item in candidates"
              :key="item.retaBatchId"
              class="candidate"
              :class="{ active: chain && chain.batchNo === item.batchNo }"
              @click="openChain(item.retaBatchId)"
            >
              <span class="c-batch">{{ item.batchNo }}</span>
              <span class="c-code">{{ item.traceCode || '未生成溯源码' }}</span>
              <span class="c-breed">{{ item.breed || '—' }}</span>
              <el-tag :type="batchStatusType(item.status)" size="small">
                {{ statusText(4, item.status) }}
              </el-tag>
              <QualityTag :status="item.qualityStatus" />
            </div>
          </div>
        </el-card>

        <!-- 链路详情 -->
        <template v-if="chain">
          <el-alert
            v-if="!chain.complete"
            title="链路不完整：上游某一级批号缺失，可能是历史数据未接上链路"
            type="warning"
            :closable="false"
            show-icon
            class="alert"
          />

          <el-card shadow="never" class="card">
            <template #header><span class="section-title">四级流转链路</span></template>
            <el-table :data="chain.chain || []" border stripe size="small">
              <el-table-column prop="stage" label="环节" width="120" align="center" />
              <el-table-column prop="nodeName" label="企业" min-width="180" />
              <el-table-column prop="nodeCode" label="登录编码" width="110" />
              <el-table-column prop="batchNo" label="产品批号" min-width="150" />
              <el-table-column prop="breed" label="品种" width="110" />
              <el-table-column prop="productType" label="品类/形态" width="110" />
              <el-table-column label="质量" width="90" align="center">
                <template #default="{ row }">
                  <QualityTag :status="row.qualityStatus" />
                </template>
              </el-table-column>
              <el-table-column prop="time" label="建号时间" min-width="160" />
            </el-table>
          </el-card>

          <el-card shadow="never" class="card">
            <template #header><span class="section-title">各环节检测记录</span></template>
            <InspectionTable :records="chain.inspections || []" :show-inspector="true" />
          </el-card>

          <el-card
            v-if="chain.processRecords && chain.processRecords.length"
            shadow="never"
            class="card"
          >
            <template #header><span class="section-title">冷冻加工工序记录</span></template>
            <el-table :data="chain.processRecords" border stripe size="small">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="step" label="工序" width="90" align="center" />
              <el-table-column prop="stepTime" label="工序时间" min-width="160" />
              <el-table-column prop="temperature" label="工艺参数" min-width="130" />
              <el-table-column prop="operator" label="操作人" width="100" />
            </el-table>
          </el-card>
        </template>

        <el-empty
          v-else-if="searched && !loading"
          description="没有匹配的批次，换个溯源码或批号试试"
        />
      </div>
    </el-main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QualityTag from '../../components/QualityTag.vue'
import InspectionTable from '../../components/InspectionTable.vue'
import {
  adminTraceChainApi,
  adminTraceChainByIdApi,
  adminTraceSearchApi,
  adminTraceStatsApi
} from '../../api/admin'
import { statusTagType, statusText } from '../../utils/nodeType'
import seafoodLogo from '../../assets/images/海鲜.png'

const router = useRouter()

const keyword = ref('')
const candidates = ref([])
const chain = ref(null)
const stats = ref({})
const loading = ref(false)
const searched = ref(false)

const statCards = computed(() => [
  { label: '零售批号总数', value: stats.value.total ?? '—' },
  { label: '已确认批次', value: stats.value.confirmed ?? '—' },
  { label: '已生成溯源码', value: stats.value.withTraceCode ?? '—' },
  { label: '质量不合格批次', value: stats.value.rejected ?? '—' },
  { label: '抽样链路完整数', value: stats.value.completeChain ?? '—' }
])

onMounted(loadStats)

async function loadStats() {
  try {
    const res = await adminTraceStatsApi()
    stats.value = res.data || {}
  } catch (e) {
    /* 概况拉取失败不影响查询功能 */
  }
}

function batchStatusType(status) {
  return statusTagType(status)
}

async function handleSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入查询关键词')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await adminTraceSearchApi(kw)
    candidates.value = res.data || []
    if (candidates.value.length === 1) {
      // 唯一命中时直接展开链路，省一次点击
      await openChain(candidates.value[0].retaBatchId)
    } else if (candidates.value.length === 0) {
      // 搜索接口没命中时，再试一次"直接按关键词取链路"（支持精确录入的溯源码）
      try {
        const direct = await adminTraceChainApi(kw)
        chain.value = direct.data
      } catch (e) {
        chain.value = null
      }
    } else {
      chain.value = null
    }
  } finally {
    loading.value = false
  }
}

async function openChain(retaBatchId) {
  try {
    const res = await adminTraceChainByIdApi(retaBatchId)
    chain.value = res.data
  } catch (e) {
    chain.value = null
  }
}

function reset() {
  candidates.value = []
  chain.value = null
  searched.value = false
}

function handleLogout() {
  localStorage.removeItem('adminToken')
  ElMessage.success('已退出登录')
  router.replace('/sys/login')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: var(--bg-page);
}

.admin-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  height: 56px;
  background: var(--brand-gradient);
  color: #fff;
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #fff;
  padding: 2px;
  object-fit: cover;
}

.admin-header h1 {
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.nav-link {
  color: #fff !important;
  font-size: 13px;
}

.header-logout {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: transparent;
  color: #fff;
  border-radius: 5px;
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
}

.header-logout:hover {
  background: rgba(255, 255, 255, 0.15);
}

.admin-body {
  padding: 18px;
}

.container {
  max-width: 1280px;
  margin: 0 auto;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 14px 18px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.card {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--brand-primary-dark);
}

.search-row {
  display: flex;
  gap: 12px;
}

.candidate-list {
  margin-top: 14px;
}

.candidate-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.candidate {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  cursor: pointer;
  background: #fff;
}

.candidate:hover,
.candidate.active {
  border-color: var(--brand-primary);
  background: #f5f9fd;
}

.c-batch {
  font-weight: 600;
  color: var(--text-main);
  min-width: 130px;
}

.c-code {
  color: var(--brand-primary);
  min-width: 150px;
}

.c-breed {
  color: var(--text-sub);
  flex: 1;
}

.alert {
  margin-bottom: 14px;
}
</style>
