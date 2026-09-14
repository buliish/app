<template>
  <!--
    产品批号管理界面 —— 参考原型图设计（2.4.2 浏览产品批号列表用例）
    单选切换浏览「待发布（新建）」「已发布」两种状态的批号列表（已下架不可浏览）；
    待发布列表每项提供「更新」「删除」按钮，已发布列表每项仅提供「下架」按钮；
    点击列表项跳转产品批号详情页。
  -->
  <div class="page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="page-header">
      <img :src="seafoodLogo" alt="logo" class="header-logo" />
      <h1 class="header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 状态单选：待发布 / 已发布 -->
      <div class="status-radio">
        <el-radio-group v-model="activeStatus" @change="loadList">
          <el-radio value="1">待发布</el-radio>
          <el-radio value="2">已发布</el-radio>
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
          </div>

          <!-- 右侧：操作按钮（待发布→更新/删除；已发布→下架） -->
          <div class="action-block">
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
            <template v-else>
              <button type="button" class="mini-btn offline" @click.stop="handleOffline(row)">下架</button>
            </template>
          </div>
        </div>
      </div>
    </main>

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
import { getLoginNode } from '../utils/nodeType'

const router = useRouter()
const node = getLoginNode()
const nodeType = node.nodeType || 1

// 批号主键字段名按企业类型区分，取当前登录企业类型对应的字段
const ID_KEY = { 1: 'farmBatchId', 2: 'frozBatchId', 3: 'wholBatchId', 4: 'retaBatchId' }
const idOf = (row) => row[ID_KEY[nodeType]]

// 浏览状态：1 待发布（新建，默认）、2 已发布；已下架（3）不提供浏览入口
const activeStatus = ref('1')
const list = ref([])
const loading = ref(false)

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
</style>
