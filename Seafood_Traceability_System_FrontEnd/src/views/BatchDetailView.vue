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
        <div class="process-head">
          <span class="cell-label process-title">加工工序记录</span>
          <!-- 仅在批号尚未下架时可维护工序 -->
          <button v-if="canEditProcess" type="button" class="mini-btn add" @click="openProcessDialog">
            + 新增工序
          </button>
        </div>
        <el-empty v-if="records.length === 0" description="暂无工序记录" :image-size="80" />
        <div v-else class="detail-table process-table">
          <div v-for="rec in records" :key="rec.recordId" class="cell process-cell">
            <div class="cell-label">{{ rec.step }}（{{ formatTime(rec.stepTime) }}）</div>
            <div class="cell-value">
              {{ rec.temperature || '—' }}<template v-if="rec.operator"> · 操作人：{{ rec.operator }}</template><template v-if="rec.remark"> · {{ rec.remark }}</template>
            </div>
            <button
              v-if="canEditProcess"
              type="button"
              class="mini-btn del"
              @click="handleDeleteProcess(rec)"
            >
              删除
            </button>
          </div>
        </div>
      </template>
    </main>

    <!-- 新增工序弹窗 -->
    <el-dialog v-model="processVisible" title="新增加工工序" width="420px" align-center>
      <el-form :model="processForm" label-width="90px">
        <el-form-item label="工序" required>
          <el-select v-model="processForm.step" placeholder="请选择工序" style="width: 100%">
            <el-option v-for="s in PROCESS_STEPS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序时间">
          <el-date-picker
            v-model="processForm.stepTime"
            type="datetime"
            placeholder="默认为当前时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="工艺参数">
          <el-input v-model="processForm.temperature" placeholder="如：冷冻温度 -18℃" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="processForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="processForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="processSaving" @click="submitProcess">保存</el-button>
      </template>
    </el-dialog>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchDetailApi, processAddApi, processDeleteApi, processListApi } from '../api/batch'
import { citiesApi, nodesApi, provincesApi } from '../api/region'
import {
  PROCESS_STEPS,
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

// ---------------- 加工工序维护 ----------------
const processVisible = ref(false)
const processSaving = ref(false)
const processForm = reactive({
  step: '',
  stepTime: '',
  temperature: '',
  operator: '',
  remark: ''
})

// 批号下架后不再允许增删工序（后端同样会拦截，这里是前端体验层）
const canEditProcess = computed(() => nodeType === 2 && detail.status !== 4)

// 时间显示去掉秒与 T，形如 2026-02-16 10:00
function formatTime(t) {
  return String(t || '').replace('T', ' ').slice(0, 16)
}

function openProcessDialog() {
  processForm.step = ''
  processForm.stepTime = ''
  processForm.temperature = ''
  processForm.operator = ''
  processForm.remark = ''
  processVisible.value = true
}

async function submitProcess() {
  if (!processForm.step) {
    ElMessage.warning('请选择工序')
    return
  }
  processSaving.value = true
  try {
    await processAddApi({
      frozBatchId: Number(route.params.id),
      step: processForm.step,
      stepTime: processForm.stepTime || null,
      temperature: processForm.temperature || null,
      operator: processForm.operator || null,
      remark: processForm.remark || null
    })
    ElMessage.success('工序添加成功！')
    processVisible.value = false
    await loadProcess()
  } finally {
    processSaving.value = false
  }
}

async function handleDeleteProcess(rec) {
  try {
    await ElMessageBox.confirm(`确定删除工序「${rec.step}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await processDeleteApi(rec.recordId)
  ElMessage.success('删除成功！')
  await loadProcess()
}

// 拉取工序列表（独立成函数，便于增删后刷新）
async function loadProcess() {
  if (nodeType !== 2) return
  const res = await processListApi(route.params.id)
  records.value = res.data || []
}

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

    await loadProcess()
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
  padding: 0 2px;
}

/* 工序区块标题行：左侧标题 + 右侧新增按钮 */
.process-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22px;
}

.process-table {
  margin-top: 8px;
}

/* 单条工序：右下角放删除按钮 */
.process-cell {
  position: relative;
}

.mini-btn.add {
  width: auto;
  padding: 0 14px;
  height: 30px;
  background: #1d6fb8;
  border-radius: 15px;
  color: #fff;
  font-size: 13px;
}

.mini-btn.add:hover {
  background: #0b4f8c;
}

.mini-btn.del {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: auto;
  padding: 0 12px;
  height: 26px;
  border: 1px solid #f56c6c;
  border-radius: 13px;
  color: #f56c6c;
  font-size: 12px;
  background: #fff;
}

.mini-btn.del:hover {
  background: #f56c6c;
  color: #fff;
}
</style>
