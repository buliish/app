<template>
  <!--
    产品批号管理界面 —— 参考原型图设计（2.4.2 浏览产品批号列表用例）
    单选切换浏览「待发布（新建）」「已发布」两种状态的批号列表（已下架不可浏览）；
    待发布列表每项提供「更新」「删除」按钮，已发布列表每项仅提供「下架」按钮；
    点击列表项跳转产品批号详情页。
  -->
  <div class="page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="app-header">
      <img :src="seafoodLogo" alt="logo" class="app-header-logo" />
      <h1 class="app-header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 状态单选：按企业类型渲染（养殖为 待发布/已发布，其余为 新建/待确认/已确认） -->
      <div class="status-radio">
        <el-radio-group v-model="activeStatus" @change="loadList">
          <el-radio v-for="tab in tabs" :key="tab.value" :value="String(tab.value)">
            {{ tab.label }}
          </el-radio>
        </el-radio-group>
      </div>

      <!-- 批号卡片列表 -->
      <div v-loading="loading" class="batch-list">
        <el-empty v-if="!loading && list.length === 0" description="暂无该状态的产品批号" />

        <div
          v-for="row in list"
          :key="idOf(row)"
          class="batch-card"
          @click="router.push(`/batch/detail/${idOf(row)}`)"
        >
          <!-- 左侧：创建日期 -->
          <div class="date-block">
            <span class="date-day">{{ splitDate(row.createTime).day }}</span>
            <span class="date-month">{{ splitDate(row.createTime).month }}</span>
          </div>

          <!-- 中间：批号信息（养殖显示检疫合格证；其余环节显示进场批号，与原型图一致） -->
          <div class="info-block">
            <p class="info-line">产品批号：{{ row.batchNo }}</p>
            <p class="info-line">品种：{{ row.breed || '—' }}</p>
            <p class="info-line">
              {{ nodeType === 1 ? '检疫合格证' : '进场批号' }}：{{ nodeType === 1 ? row.quarantineNo || '—' : row.upBatchNo || '—' }}
            </p>
            <!-- 零售商批号经上游确认后会生成溯源标识码，这里直接展示出来 -->
            <p v-if="row.traceCode" class="info-line trace-line">
              溯源标识码：<span class="trace-code">{{ row.traceCode }}</span>
            </p>
          </div>

          <!-- 右侧：操作按钮按状态区分 -->
          <div class="action-block">
            <!-- 新建 / 待发布：可更新、删除 -->
            <template v-if="row.status === 1">
              <button
                type="button"
                class="mini-btn update"
                @click.stop="router.push(`/batch/update/${idOf(row)}`)"
              >
                更新
              </button>
              <button type="button" class="mini-btn delete" @click.stop="handleDelete(row)">删除</button>
            </template>
            <!-- 待确认：等待上游处理，不可操作 -->
            <template v-else-if="row.status === 2">
              <span class="status-hint">等待上游确认</span>
            </template>
            <!-- 已确认：可下架；零售商另可查看溯源二维码 -->
            <template v-else>
              <button
                v-if="row.traceCode"
                type="button"
                class="mini-btn trace"
                @click.stop="showQrcode(row)"
              >
                溯源码
              </button>
              <button type="button" class="mini-btn offline" @click.stop="handleOffline(row)">下架</button>
            </template>
          </div>
        </div>
      </div>
    </main>

    <!-- 溯源二维码弹窗 -->
    <el-dialog v-model="qrVisible" title="溯源二维码" width="340px" align-center>
      <div v-if="qrRow" class="qr-box">
        <img :src="qrUrl(qrRow.traceCode)" alt="溯源二维码" class="qr-img" />
        <p class="qr-code-text">{{ qrRow.traceCode }}</p>
        <p class="qr-tip">消费者扫码即可查看从养殖到零售的全链路信息</p>
      </div>
      <template #footer>
        <button type="button" class="mini-btn update" @click="downloadQr(qrRow.traceCode)">
          下载二维码
        </button>
        <button type="button" class="mini-btn delete" @click="qrVisible = false">关闭</button>
      </template>
    </el-dialog>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { batchListApi, batchDeleteApi, batchOfflineApi } from '../api/batch'
import { getLoginNode, statusTabs } from '../utils/nodeType'

const router = useRouter()
const node = getLoginNode()
const nodeType = node.nodeType || 1

// 可选状态项按企业类型生成：
// 养殖企业为「待发布 / 已发布」，其余为「新建 / 待确认 / 已确认」
const tabs = statusTabs(nodeType)

// 批号主键字段名按企业类型区分，取当前登录企业类型对应的字段
const ID_KEY = { 1: 'farmBatchId', 2: 'frozBatchId', 3: 'wholBatchId', 4: 'retaBatchId' }
const idOf = (row) => row[ID_KEY[nodeType]]

// 浏览状态：默认停留在第一个状态；已下架不提供浏览入口
const activeStatus = ref(String(tabs[0].value))
const list = ref([])
const loading = ref(false)
const qrVisible = ref(false)
const qrRow = ref(null)

onMounted(loadList)

// 查询当前状态的产品批号列表
async function loadList() {
  loading.value = true
  try {
    const res = await batchListApi(nodeType, Number(activeStatus.value))
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 解析创建时间：左侧日期块显示「日 + 月」
function splitDate(time) {
  const s = String(time || '').replace('T', ' ').slice(0, 10)
  const parts = s.split('-')
  if (parts.length < 3) return { day: '--', month: '' }
  return { day: parts[2], month: `${Number(parts[1])}月` }
}

// 删除待发布批号
function handleDelete(row) {
  ElMessageBox.confirm(`确定删除产品批号「${row.batchNo}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await batchDeleteApi(nodeType, idOf(row))
      ElMessage.success('删除成功！')
      loadList()
    })
    .catch(() => {})
}

// 查看溯源二维码（零售商批号经上游确认后生成）
function showQrcode(row) {
  qrRow.value = row
  qrVisible.value = true
}

// 二维码图片地址：走后端接口生成，同源代理由 vite 转发
function qrUrl(traceCode) {
  return `/trace/qrcode/${encodeURIComponent(traceCode)}`
}

// 下载二维码
function downloadQr(traceCode) {
  const a = document.createElement('a')
  a.href = qrUrl(traceCode)
  a.download = `溯源二维码-${traceCode}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

// 下架已发布批号（下架后不可再浏览）
function handleOffline(row) {
  ElMessageBox.confirm(`下架后该批号不再展示，且不能恢复。确定下架「${row.batchNo}」吗？`, '提示', {
    confirmButtonText: '确定下架',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await batchOfflineApi(nodeType, idOf(row))
      ElMessage.success('下架成功！')
      loadList()
    })
    .catch(() => {})
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏（与功能菜单页一致） */



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


/* 状态单选按钮 */
.status-radio {
  display: flex;
  justify-content: center;
  margin: 18px 0 14px;
}

.status-radio :deep(.el-radio__label) {
  font-size: 15px;
  color: #303133;
}

/* 批号卡片列表 */
.batch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.batch-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e4e9f1;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(11, 79, 140, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.batch-card:hover {
  border-color: #1d6fb8;
  box-shadow: 0 4px 12px rgba(29, 111, 184, 0.15);
}

/* 左侧日期块 */
.date-block {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 44px;
  color: #0b4f8c;
}

.date-day {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
}

.date-month {
  font-size: 13px;
  color: #606266;
}

/* 中间信息块 */
.info-block {
  flex: 1;
  min-width: 0;
}

.info-line {
  margin: 0;
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 右侧操作按钮 */
.action-block {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-btn {
  width: 56px;
  height: 32px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.mini-btn.update {
  border: 1px solid #1d6fb8;
  color: #1d6fb8;
}

.mini-btn.update:hover {
  background: #1d6fb8;
  color: #fff;
}

.mini-btn.delete {
  border: 1px solid #f56c6c;
  color: #f56c6c;
}

.mini-btn.delete:hover {
  background: #f56c6c;
  color: #fff;
}

.mini-btn.offline {
  width: 64px;
  height: 40px;
  border: 1px solid #c0c4cc;
  border-radius: 20px;
  color: #606266;
}

.mini-btn.offline:hover {
  background: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
}

/* 已确认状态下展示的溯源标识码 */
.info-line.trace-line {
  color: #0f9d58;
}

.trace-code {
  font-weight: 700;
  letter-spacing: 0.5px;
}

/* 待确认状态的说明文字 */
.status-hint {
  font-size: 12px;
  color: #909399;
}

/* 溯源按钮（绿色，与"已确认"状态呼应） */
.mini-btn.trace {
  width: 64px;
  height: 40px;
  background: #0f9d58;
  border-radius: 20px;
  color: #fff;
}

.mini-btn.trace:hover {
  background: #0b7a43;
}

/* 二维码弹窗 */
.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.qr-img {
  width: 220px;
  height: 220px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.qr-code-text {
  font-size: 15px;
  font-weight: 700;
  color: #0b4f8c;
  letter-spacing: 0.5px;
}

.qr-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* 弹窗底部按钮复用了列表的 mini-btn 样式，这里去掉固定宽度以便自适应 */
.qr-box ~ * .mini-btn,
:deep(.el-dialog__footer) .mini-btn {
  width: auto;
  padding: 0 18px;
  margin-left: 8px;
}
</style>
