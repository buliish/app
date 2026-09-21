<template>
  <!--
    管理端批次追溯查询（3.2.8 扩展）
    输入产品编号 / 溯源码 / 任意环节批号 → 查看该产品在每一个环节的详细信息：
    产品编号、环节、企业、批号、形态、规格、日期、是否合格，以及检测记录明细。
  -->
  <div class="admin-page">
    <header class="admin-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <h1>冷冻对虾全产业链溯源系统 · 批次追溯查询</h1>
      <div class="header-actions">
        <button type="button" class="header-dash" @click="router.push('/sys/nodes')">企业注册管理</button>
        <button type="button" class="header-dash" @click="router.push('/sys/dashboard')">可视化大屏</button>
        <button type="button" class="header-logout" @click="handleLogout">
          <span>退出登录</span>
        </button>
      </div>
    </header>

    <el-main class="admin-body">
      <div class="trace-container">
        <!-- 查询条 -->
        <el-card shadow="never" class="crud-card">
          <template #header>
            <span class="card-title">按产品追溯</span>
          </template>
          <el-form inline class="search-form">
            <el-form-item label="关键词">
              <el-input
                v-model="keyword"
                placeholder="产品编号 / 溯源标识码 / 任一环节批号"
                clearable
                style="width: 320px"
                @keyup.enter="doSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="searching" @click="doSearch">查 询</el-button>
              <el-button @click="resetAll">清 空</el-button>
            </el-form-item>
          </el-form>
          <p class="hint">
            示例：<el-link type="primary" @click="quick('SHZ202601010001')">SHZ202601010001</el-link>
            （溯源码）或
            <el-link type="primary" @click="quick('SP202601070303')">SP202601070303</el-link>
            （产品编号）—— 用养殖环节批号也能一路查到最终零售端。
          </p>
        </el-card>

        <!-- 候选区：命中多条时让用户先选 -->
        <el-card v-if="candidates.length > 1" shadow="never" class="crud-card">
          <template #header>
            <span class="card-title">匹配到 {{ candidates.length }} 个批次，请选择</span>
          </template>
          <el-table :data="candidates" border stripe @row-click="loadChainById">
            <el-table-column prop="productCode" label="产品编号" width="150" />
            <el-table-column prop="traceCode" label="溯源标识码" width="180" />
            <el-table-column prop="batchNo" label="零售批号" width="150" />
            <el-table-column prop="breed" label="品种" width="120" />
            <el-table-column label="形态" width="80" align="center">
              <template #default="{ row }">{{ row.productForm || '—' }}</template>
            </el-table-column>
            <el-table-column prop="specGrade" label="规格" width="120" />
            <el-table-column label="质量" width="90" align="center">
              <template #default="{ row }"><QualityTag :status="row.qualityStatus" /></template>
            </el-table-column>
            <el-table-column prop="retailerName" label="零售企业" min-width="180" />
          </el-table>
        </el-card>

        <!-- 明细区 -->
        <template v-if="chain">
          <!-- 产品概要 -->
          <el-card shadow="never" class="crud-card">
            <template #header>
              <div class="chain-head">
                <span class="card-title">产品信息</span>
                <div class="chain-tags">
                  <el-tag :type="chain.complete ? 'success' : 'danger'" size="small">
                    {{ chain.complete ? '链路完整' : '链路不完整' }}
                  </el-tag>
                  <QualityTag :status="chain.overallQuality" size="small" effect="dark" />
                </div>
              </div>
            </template>
            <el-descriptions :column="4" border>
              <el-descriptions-item label="产品编号">{{ chain.productCode || '—' }}</el-descriptions-item>
              <el-descriptions-item label="溯源标识码">{{ chain.traceCode }}</el-descriptions-item>
              <el-descriptions-item label="产品品种">{{ chain.breed }}</el-descriptions-item>
              <el-descriptions-item label="产品类型">{{ chain.productType }}</el-descriptions-item>
              <el-descriptions-item label="产品形态">{{ chain.productForm || '—' }}</el-descriptions-item>
              <el-descriptions-item label="规格等级">{{ chain.specGrade || '—' }}</el-descriptions-item>
              <el-descriptions-item label="零售单价">
                {{ chain.price != null ? `¥${chain.price}` : '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="上市时间">{{ fmt(chain.traceTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 全链路环节明细 -->
          <el-card shadow="never" class="crud-card">
            <template #header><span class="card-title">全链路环节明细</span></template>
            <el-table :data="chain.stages" border stripe>
              <el-table-column prop="stageName" label="环节" width="100" align="center" />
              <el-table-column prop="nodeName" label="企业名称" min-width="200" />
              <el-table-column prop="batchNo" label="产品批号" width="150" />
              <el-table-column label="进场批号" width="150">
                <template #default="{ row }">{{ row.upBatchNo || '—' }}</template>
              </el-table-column>
              <el-table-column label="形态" width="80" align="center">
                <template #default="{ row }">{{ row.productForm || '—' }}</template>
              </el-table-column>
              <el-table-column label="规格" width="120">
                <template #default="{ row }">{{ row.specGrade || '—' }}</template>
              </el-table-column>
              <el-table-column label="来源方式" width="100" align="center">
                <template #default="{ row }">{{ row.sourceType || '—' }}</template>
              </el-table-column>
              <el-table-column label="是否合格" width="100" align="center">
                <template #default="{ row }"><QualityTag :status="row.qualityStatus" /></template>
              </el-table-column>
              <el-table-column label="检测" width="110" align="center">
                <template #default="{ row }">
                  <span v-if="summaryOf(row.stageType)">{{ summaryOf(row.stageType).total }} 项</span>
                  <el-tag
                    v-if="summaryOf(row.stageType) && summaryOf(row.stageType).fails"
                    type="danger"
                    size="small"
                    effect="plain"
                    style="margin-left: 4px"
                  >
                    {{ summaryOf(row.stageType).fails }} 项不合格
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="日期" width="120">
                <template #default="{ row }">{{ fmt(row.time).slice(0, 10) }}</template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 检测记录明细 -->
          <el-card shadow="never" class="crud-card">
            <template #header>
              <div class="chain-head">
                <span class="card-title">检测记录明细</span>
                <el-select v-model="stageFilter" size="small" style="width: 140px" clearable placeholder="全部环节">
                  <el-option label="养殖" :value="1" />
                  <el-option label="冷冻加工" :value="2" />
                  <el-option label="批发" :value="3" />
                  <el-option label="零售" :value="4" />
                </el-select>
              </div>
            </template>
            <InspectionTable :records="filteredInspections" show-stage />
          </el-card>

          <!-- 加工工序 -->
          <el-card v-if="chain.processRecords && chain.processRecords.length" shadow="never" class="crud-card">
            <template #header><span class="card-title">冷冻加工工序</span></template>
            <el-table :data="chain.processRecords" border stripe>
              <el-table-column prop="step" label="工序" width="110" align="center" />
              <el-table-column label="工序时间" width="180">
                <template #default="{ row }">{{ fmt(row.stepTime) }}</template>
              </el-table-column>
              <el-table-column prop="temperature" label="工艺参数" min-width="150" />
              <el-table-column prop="operator" label="操作人" width="110" />
              <el-table-column prop="remark" label="备注" min-width="180" />
            </el-table>
          </el-card>

          <!--
            产业链树：上面那几张卡是"从这件商品往上游的一条线"，
            这里给的是整棵树 —— 一批虾派生出的虾滑、虾丸等各条分支都在里面。
            标了「当前链」的是用户正在看的那一条。
          -->
          <el-card v-if="tree" shadow="never" class="crud-card">
            <template #header>
              <div class="tree-head">
                <span class="card-title">产业链树</span>
                <span class="tree-sub">同一批原料派生出的全部产品与流向</span>
              </div>
            </template>
            <el-tree
              :data="[tree]"
              default-expand-all
              :expand-on-click-node="false"
              class="chain-tree"
            >
              <template #default="{ data: node }">
                <span class="tree-node" :class="{ 'is-current': node.onCurrentChain }">
                  <el-tag size="small" :type="stageTagType(node.stageType)" effect="plain">
                    {{ node.stage }}
                  </el-tag>
                  <span class="tree-batch">{{ node.batchNo }}</span>
                  <span class="tree-product">{{ node.productType || '—' }}</span>
                  <span class="tree-org">{{ node.node ? node.node.name : '—' }}</span>
                  <QualityTag :status="node.qualityStatus" />
                  <el-tag v-if="node.onCurrentChain" size="small" type="success" effect="dark">
                    当前链
                  </el-tag>
                </span>
              </template>
            </el-tree>
          </el-card>
        </template>

        <!--
          只有"确实一条都没匹配到"才显示空态。
          命中多条时上方已列出候选等用户点选，此时若还显示"未找到"自相矛盾。
        -->
        <el-empty
          v-else-if="searched && candidates.length === 0"
          description="未找到匹配的批次"
        />
      </div>
    </el-main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  adminTraceChainApi,
  adminTraceChainByIdApi,
  adminTraceSearchApi,
  adminTraceStatsApi,
  adminTraceTreeApi
} from '../../api/admin'
import InspectionTable from '../../components/InspectionTable.vue'
import QualityTag from '../../components/QualityTag.vue'
import seafoodLogo from '../../assets/images/海鲜.png'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const candidates = ref([])
const chain = ref(null)
const tree = ref(null)
const searching = ref(false)
const searched = ref(false)
const stageFilter = ref(null)
const traceStats = ref(null)

function fmt(t) {
  return String(t || '').replace('T', ' ')
}

const filteredInspections = computed(() => {
  const all = chain.value?.inspections || []
  return stageFilter.value ? all.filter((i) => i.stageType === stageFilter.value) : all
})

// 环节检测汇总：{ stageType: {total, fails} }
function summaryOf(stageType) {
  return (chain.value?.inspectionSummary || {})[String(stageType)]
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入产品编号、溯源标识码或批号')
    return
  }
  searching.value = true
  searched.value = true
  chain.value = null
  candidates.value = []
  stageFilter.value = null
  try {
    const res = await adminTraceSearchApi(kw)
    candidates.value = res.data || []
    if (candidates.value.length === 1) {
      // 唯一命中直接展开明细，省一次点击
      await loadChainById({ retaBatchId: candidates.value[0].retaBatchId })
    } else if (candidates.value.length === 0) {
      ElMessage.warning('未找到匹配的批次')
    }
  } finally {
    searching.value = false
  }
}

async function loadChainById(row) {
  const res = await adminTraceChainByIdApi(row.retaBatchId)
  chain.value = res.data || null
  // 产业链树与链路明细是两个视角，一起加载；树失败不影响明细展示
  await loadTree(row.retaBatchId)
}

/**
 * 加载产业链树。
 * 与 chain 的区别：chain 是"从这件商品往上游的一条线"，
 * tree 是"同一批原料派生出的整棵树"。
 * 树拉取失败时静默降级（只显示 undefined），不打断主流程。
 */
async function loadTree(retaBatchId) {
  tree.value = null
  try {
    const res = await adminTraceTreeApi(retaBatchId)
    tree.value = res.data || null
  } catch (e) {
    tree.value = null
  }
}

/** 树节点上的环节标签配色，与节点类型一一对应 */
function stageTagType(stageType) {
  return { 1: 'success', 2: 'warning', 3: 'primary', 4: 'info' }[stageType] || 'info'
}

function quick(code) {
  keyword.value = code
  doSearch()
}

function resetAll() {
  keyword.value = ''
  candidates.value = []
  chain.value = null
  tree.value = null
  searched.value = false
  stageFilter.value = null
}

function handleLogout() {
  localStorage.removeItem('adminToken')
  router.replace('/sys/login')
}

onMounted(async () => {
  // 先处理 URL 上的关键词 —— 这是用户打开页面就是为了看的东西，
  // 不能排在统计请求后面（否则要等一个多余的网络往返才开始查）
  const kw = route.query.keyword
  if (kw) {
    keyword.value = String(kw)
    doSearch()
  }

  // 顶部统计留给页面扩展；失败不影响主流程
  try {
    const res = await adminTraceStatsApi()
    traceStats.value = res.data
  } catch (e) {
    traceStats.value = null
  }
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f0f4f8;
}

.admin-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 24px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  color: #fff;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fff;
  padding: 2px;
  object-fit: cover;
}

.admin-header h1 {
  font-size: 18px;
  letter-spacing: 1px;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.header-dash,
.header-logout {
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.header-dash:hover,
.header-logout:hover {
  background: rgba(255, 255, 255, 0.28);
}

.admin-body {
  padding: 16px;
}

.trace-container {
  max-width: 1320px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.crud-card {
  border-radius: 10px;
}

.card-title {
  font-weight: 600;
  color: #0b4f8c;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.chain-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chain-tags {
  display: flex;
  gap: 8px;
}

/* 产业链树 */
.tree-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.tree-sub {
  font-size: 12px;
  color: #909399;
}

.chain-tree {
  background: transparent;
}

/* 每个节点一行：环节 + 批号 + 品类 + 企业 + 质量 + 当前链标记 */
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 13px;
}

/* 当前查看的那条链：左侧色条 + 浅底，一眼看出自己在哪一支 */
.tree-node.is-current {
  background: #eef6fd;
  box-shadow: inset 3px 0 0 #1d6fb8;
}

.tree-batch {
  font-weight: 600;
  color: #303133;
  min-width: 150px;
}

.tree-product {
  color: #606266;
  min-width: 90px;
}

.tree-org {
  color: #909399;
}
</style>
