<template>
  <!--
    商品详情页（消费者端，免登录）
    以"产品"为主角：产品卡 + 合格总览 + 逐级流转链路 + 各环节企业 + 检测记录。
    注意：任务书 3.2.7 要求展示养殖/加工/批发/零售四级企业，
    因此"各环节企业信息"区块完整保留，不能因为改产品视角就去掉。
  -->
  <div class="pd-page">
    <header class="app-header">
      <img :src="seafoodLogo" alt="logo" class="app-header-logo" />
      <h1 class="app-header-title">冷冻对虾全产业链溯源系统</h1>
      <el-link class="pd-back" @click="router.push('/')">返回商城</el-link>
    </header>

    <main class="pd-body" v-loading="loading">
      <template v-if="data">
        <el-alert
          v-if="data.offline"
          title="该产品已下架，以下为历史溯源信息"
          type="warning"
          show-icon
          :closable="false"
          class="pd-alert"
        />

        <!-- 产品卡 -->
        <el-card shadow="never" class="pd-card">
          <div class="pd-hero">
            <img :src="data.imageUrl || defaultProductImg" alt="产品图片" class="pd-img" />
            <div class="pd-hero-info">
              <h2>{{ productName }}</h2>
              <div class="pd-tags">
                <el-tag :type="data.productForm === '鲜虾' ? 'success' : 'primary'" effect="plain">
                  {{ data.productForm || '冻虾' }}
                </el-tag>
                <QualityTag :status="data.overallQuality" effect="dark" />
                <el-tag v-if="data.sourceType" type="info" effect="plain">{{ data.sourceType }}</el-tag>
              </div>
              <div class="pd-price">{{ data.price != null ? `¥${data.price}` : '价格待定' }}</div>
              <div class="pd-codes">
                <span>产品编号：<b>{{ data.productCode || '—' }}</b></span>
                <span>溯源标识码：<b>{{ data.traceCode }}</b></span>
              </div>
            </div>
            <div class="pd-qr">
              <img :src="qrUrl" alt="溯源二维码" class="pd-qr-img" />
              <span>扫码溯源</span>
            </div>
          </div>
        </el-card>

        <!-- 合格总览：每个环节各自的合格状态 -->
        <el-card shadow="never" class="pd-card">
          <template #header><span class="pd-title">各环节质量总览</span></template>
          <div class="quality-grid">
            <div v-for="s in qualityStages" :key="s.label" class="quality-cell">
              <div class="quality-stage">{{ s.label }}</div>
              <QualityTag :status="s.value" />
            </div>
          </div>
        </el-card>

        <!-- 逐级流转链路 -->
        <el-card shadow="never" class="pd-card">
          <template #header><span class="pd-title">全产业链流转链路</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in data.chain"
              :key="index"
              :timestamp="fmt(item.time)"
              placement="top"
            >
              <div class="chain-item">
                <h4>{{ item.stage }} · {{ item.nodeName }}</h4>
                <p>批号：{{ item.batchNo }}</p>
                <p v-if="item.breed">品种：{{ item.breed }}</p>
                <p v-if="item.productType">品类：{{ item.productType }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 各环节企业信息（任务书 3.2.7 明确要求展示四级企业） -->
        <el-card shadow="never" class="pd-card">
          <template #header><span class="pd-title">各环节企业信息</span></template>
          <el-table :data="nodeRows" border stripe>
            <el-table-column prop="stage" label="环节" width="130" />
            <el-table-column prop="name" label="企业名称" min-width="200" />
            <el-table-column prop="address" label="地址" min-width="200" />
            <el-table-column prop="corporation" label="联系人" width="110" />
            <el-table-column prop="telephone" label="联系电话" width="140" />
          </el-table>
        </el-card>

        <!-- 检测记录 -->
        <el-card shadow="never" class="pd-card">
          <template #header><span class="pd-title">检测记录</span></template>
          <InspectionTable :records="data.inspections || []" show-stage :show-inspector="false" />
        </el-card>

        <!-- 加工工序 -->
        <el-card v-if="(data.processRecords || []).length" shadow="never" class="pd-card">
          <template #header><span class="pd-title">冷冻加工工序</span></template>
          <el-table :data="data.processRecords" border stripe>
            <el-table-column prop="step" label="工序" width="120" />
            <el-table-column label="工序时间" width="180">
              <template #default="{ row }">{{ fmt(row.stepTime) }}</template>
            </el-table-column>
            <el-table-column prop="temperature" label="工艺参数" min-width="160" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="remark" label="备注" min-width="180" />
          </el-table>
        </el-card>

        <!--
          同源产品：与本商品出自同一个养殖批号的其他商品。
          一批虾可以同时被加工成虾滑、虾丸、冷冻整虾，消费者扫其中一件时
          应该知道"这批虾还做成了什么"，点卡片可跳到那件商品。
          数据来自后端 relatedProducts —— 只含零售层的公开信息，
          中间环节（谁加工的、谁批发的）属于管理端视野，不在这里暴露。
        -->
        <el-card v-if="(data.relatedProducts || []).length" shadow="never" class="pd-card">
          <template #header>
            <span class="pd-title">同源产品（这批虾还做成了）</span>
          </template>
          <div class="related-grid">
            <div
              v-for="item in data.relatedProducts"
              :key="item.retaBatchId"
              class="related-card"
              @click="openRelated(item)"
            >
              <div class="related-head">
                <span class="related-type">{{ item.productType || '—' }}</span>
                <QualityTag :status="item.qualityStatus" />
              </div>
              <p class="related-line">品种：{{ item.breed || '—' }}</p>
              <p class="related-line" v-if="item.productForm">形态：{{ item.productForm }}</p>
              <p class="related-line muted">{{ item.retailerName || '—' }}</p>
              <p class="related-code">{{ item.traceCode || item.batchNo }}</p>
            </div>
          </div>
        </el-card>
      </template>

      <el-empty v-else-if="!loading" description="未找到该产品，请核对编码后重试" />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productDetailApi } from '../api/trace'
import InspectionTable from '../components/InspectionTable.vue'
import QualityTag from '../components/QualityTag.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import defaultProductImg from '../assets/images/海鲜产品溯源.jpeg'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const loading = ref(false)

const productName = computed(() => {
  const d = data.value
  if (!d) return ''
  return [d.specGrade, d.productForm, d.productType, d.breed].filter(Boolean).join(' · ') || '对虾产品'
})

const qrUrl = computed(() => `/trace/qrcode/${encodeURIComponent(route.params.code)}`)

// 各环节质量状态：取自链路上四个批号
const qualityStages = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: '养殖', value: d.farmBatch?.qualityStatus },
    { label: '冷冻加工', value: d.frozBatch?.qualityStatus },
    { label: '批发', value: d.wholBatch?.qualityStatus },
    { label: '零售', value: d.retaBatch?.qualityStatus }
  ]
})

// 与既有溯源页保持一致：从四个环节 key 里取企业信息
const nodeRows = computed(() => {
  const d = data.value
  if (!d) return []
  const rows = []
  const push = (stage, node) => {
    if (node && node.name) rows.push({ stage, ...node })
  }
  push('养殖企业', d.farm)
  push('冷冻加工企业', d.processor)
  push('批发商', d.wholesaler)
  push('零售商', d.retailer)
  return rows
})

function fmt(t) {
  return String(t || '').replace('T', ' ')
}

/**
 * 跳到同源商品。
 * 路由参数要同时兼容对外产品编号与溯源码 —— 前者可能为空（老数据没填），
 * 所以按"产品编号优先、没有就用溯源码"的顺序取。
 */
function openRelated(item) {
  const code = item.traceCode || item.batchNo
  if (!code) {
    return
  }
  router.push(`/product/${encodeURIComponent(code)}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await productDetailApi(route.params.code)
    data.value = res.data || null
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.pd-page {
  min-height: 100vh;
  background: var(--c-bg-page);
}

.pd-back {
  margin-left: auto;
  color: #fff !important;
}

.pd-body {
  max-width: 1080px;
  margin: 0 auto;
  padding: 20px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pd-alert {
  margin-bottom: 4px;
}

.pd-card {
  border-radius: var(--r-lg);
}

.pd-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-primary);
}

/* 产品卡 */
.pd-hero {
  display: flex;
  gap: 22px;
  align-items: flex-start;
}

.pd-img {
  width: 230px;
  height: 170px;
  object-fit: cover;
  border-radius: var(--r-md);
  flex-shrink: 0;
}

.pd-hero-info {
  flex: 1;
  min-width: 0;
}

.pd-hero-info h2 {
  font-size: 20px;
  color: var(--c-text);
  line-height: 1.4;
}

.pd-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.pd-price {
  margin-top: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #e6653a;
}

.pd-codes {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--c-text-sub);
}

.pd-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.pd-qr-img {
  width: 110px;
  height: 110px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
}

/* 环节质量总览 */
.quality-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quality-cell {
  text-align: center;
  padding: 14px 8px;
  border: 1px solid #eef2f7;
  border-radius: var(--r-md);
  background: #fafcfe;
}

.quality-stage {
  font-size: 13px;
  color: var(--c-text-sub);
  margin-bottom: 8px;
}

.chain-item h4 {
  font-size: 15px;
  color: var(--c-text);
}

.chain-item p {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-sub);
}

@media (max-width: 820px) {
  .pd-hero {
    flex-direction: column;
  }

  .pd-img {
    width: 100%;
    height: 200px;
  }

  .quality-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 同源产品卡片组 */
.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.related-card {
  border: 1px solid var(--c-border, #e4e7ed);
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.related-card:hover {
  border-color: #1d6fb8;
  box-shadow: 0 4px 14px rgba(11, 79, 140, 0.12);
}

.related-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.related-type {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.related-line {
  font-size: 12px;
  line-height: 1.8;
  color: var(--c-text-sub);
}

.related-line.muted {
  color: #909399;
}

.related-code {
  margin-top: 6px;
  font-size: 12px;
  color: #1d6fb8;
  word-break: break-all;
}
</style>
