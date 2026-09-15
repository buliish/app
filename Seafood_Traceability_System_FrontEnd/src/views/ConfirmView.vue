<template>
  <!--
    下游企业进场确认界面 —— 参考原型图设计
    初始化：查询下游企业以本企业批号作为「进场批号」、且状态为待确认的批号列表；
    查找按钮：按下游企业名称模糊查询；
    确认按钮：将下游企业产品批号状态更新为已确认。
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

      <!-- 查询行：下游企业名称输入框 + 查找按钮 -->
      <div class="search-row">
        <el-input
          v-model="downName"
          placeholder="下游企业名称"
          class="search-input"
          clearable
          @keyup.enter="loadList"
        />
        <button type="button" class="search-btn" @click="loadList">查找</button>
      </div>

      <!-- 待确认卡片列表 -->
      <div class="confirm-list">
        <el-empty v-if="!loading && list.length === 0" description="暂无待确认的下游企业进场请求" :image-size="80" />

        <div v-for="row in list" :key="row.id" class="confirm-card">
          <div class="info-block">
            <p class="info-line name">{{ row.downName || '—' }}</p>
            <p class="info-line">进场批号：{{ row.upBatchNo || '—' }}</p>
            <p class="info-line">进场品种：{{ row.breed || row.productType || '—' }}</p>
            <p class="info-line">所属{{ typeName }}产品批号：{{ row.downBatchNo || '—' }}</p>
          </div>
          <button type="button" class="confirm-btn" @click="handleConfirm(row)">确认</button>
        </div>
      </div>
    </main>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { confirmApi, confirmListApi } from '../api/batch'
import { TYPE_NAME, getLoginNode } from '../utils/nodeType'

const node = getLoginNode()
const nodeType = node.nodeType || 1
// 本企业类型名（卡片第四行「所属XX企业产品批号」用）
const typeName = computed(() => TYPE_NAME[nodeType] || '')

const downName = ref('')
const list = ref([])
const loading = ref(false)

// 初始化：查询下游企业进场批号为本企业批号、且状态为待确认的列表
onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const res = await confirmListApi(nodeType, downName.value || undefined)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 确认：将下游企业产品批号状态更新为已确认
function handleConfirm(row) {
  ElMessageBox.confirm(
    `确认「${row.downName}」的进场请求吗？确认后该批号状态将更新为已确认。`,
    '进场确认',
    { confirmButtonText: '确认进场', cancelButtonText: '取消', type: 'info' }
  )
    .then(async () => {
      await confirmApi(nodeType, row.id)
      ElMessage.success('已确认下游企业进场！')
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


/* 查询行：输入框 + 查找按钮 */
.search-row {
  display: flex;
  gap: 10px;
  margin: 18px 0 14px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 6px;
}

.search-btn {
  flex-shrink: 0;
  width: 84px;
  height: 32px;
  align-self: center;
  border: none;
  border-radius: 6px;
  background: #2e9fd8;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.search-btn:hover {
  background: #1d6fb8;
}

/* 待确认卡片列表 */
.confirm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.confirm-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(11, 79, 140, 0.05);
}

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

.info-line.name {
  font-size: 15px;
  color: #1a1a1a;
}

/* 胶囊确认按钮（参考图） */
.confirm-btn {
  flex-shrink: 0;
  width: 64px;
  height: 40px;
  border: 1px solid #c0c4cc;
  border-radius: 20px;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn:hover {
  background: #0f9d58;
  border-color: #0f9d58;
  color: #fff;
}
</style>
