<template>
  <!--
    商品溯源详情（免登录，入口是 /product/:code）。
    参数既可以是溯源码，也可以是对外产品编号——消费者可能输包装上的产品编号。

    与 /trace 溯源查询页的区别：这里以"商品"为主体，
    先给商品信息（图、价、质量），再展开四级链路、检测记录与加工工序。
  -->
  <div class="detail-page">
    <header class="detail-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <div class="title-wrap">
        <h1>冷冻对虾全产业链溯源系统</h1>
        <p>商品溯源详情</p>
      </div>
      <el-link class="back" @click="router.push('/')">返回首页</el-link>
    </header>

    <main class="detail-body" v-loading="loading">
      <el-empty v-if="!loading && !data" description="未找到该产品，请核对编码后重试">
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </el-empty>

      <template v-if="data">
        <!-- 下架提示：链接仍可打开，但明确告知该批次已不在售 -->
        <el-alert
          v-if="data.offline"
          title="该批次已下架，以下为历史溯源信息"
          type="warning"
          :closable="false"
          show-icon
          class="offline-alert"
        />

        <!-- 商品信息 -->
        <el-card shadow="never" class="card">
          <div class="product-head">
            <div class="thumb">
              <img v-if="data.imageUrl" :src="data.imageUrl" :alt="data.breed" />
              <div v-else class="thumb-fallback">{{ data.productType || '对虾' }}</div>
            </div>
            <div class="head-info">
              <div class="name-row">
                <span class="name">{{ data.breed || '—' }}</span>
                <QualityTag :status="data.qualityStatus" />
                <el-tag v-if="!data.complete" type="warning" size="small" effect="dark">
                  链路不完整
                </el-tag>
              </div>
              <p class="line">产品编号：{{ data.productCode || '—' }}</p>
              <p class="line">规格：{{ data.specGrade || '—' }}</p>
              <p class="line">
                形态：{{ data.productForm || '—' }}
                <template v-if="data.sourceType"> · 来源：{{ data.sourceType }}</template>
              </p>
              <p class="line">产地：{{ farmNode.name || '—' }}</p>
              <div class="price">
                <template v-if="data.price !== null && data.price !== undefined">
                  ¥{{ Number(data.price).toFixed(2) }}
                </template>
                <template v-else>价格面议</template>
              </div>
            </div>
          </div>

          <el-descriptions :column="2" border class="desc">
            <el-descriptions-item label="溯源标识码">
              <el-tag v-if="data.traceCode" type="success" effect="dark">{{ data.traceCode }}</el-tag>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="零售批号">{{ data.batchNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="品类">{{ data.productType || '—' }}</el-descriptions-item>
            <el-descriptions-item label="整体质量结论">
              <QualityTag :status="data.overallQuality" />
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 四级流转链路 -->
        <el-card shadow="never" class="card">
          <template #header><span class="section-title">全产业链流转链路</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in data.chain"
              :key="index"
              :timestamp="item.time"
              placement="top"
              :type="index === 0 ? 'success' : 'primary'"
            >
              <div class="chain-item">
                <h4>{{ item.stage }} · {{ item.nodeName }}</h4>
                <p>
                  产品批号：{{ item.batchNo }}
                  <span v-if="item.breed"> · 品种：{{ item.breed }}</span>
                  <span v-if="item.productType"> · {{ item.productType }}</span>
                </p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 各环节企业 -->
        <el-card shadow="never" class="card">
          <template #header><span class="section-title">各环节企业信息</span></template>
          <el-table :data="nodeRows" border stripe size="small">
            <el-table-column prop="stage" label="环节" width="120" align="center" />
            <el-table-column prop="name" label="企业名称" min-width="200" />
            <el-table-column prop="address" label="地址" min-width="180" />
            <el-table-column prop="corporation" label="联系人" width="100" />
            <el-table-column prop="telephone" label="联系电话" width="130" />
          </el-table>
        </el-card>

        <!-- 检测记录 -->
        <el-card shadow="never" class="card">
          <template #header><span class="section-title">各环节检测记录</span></template>
          <InspectionTable :records="data.inspections || []" :show-inspector="false" />
        </el-card>

        <!-- 加工工序 -->
        <el-card v-if="data.processRecords && data.processRecords.length" shadow="never" class="card">
          <template #header>
            <span class="section-title">冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）</span>
          </template>
          <el-table :data="data.processRecords" border stripe size="small">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="step" label="工序" width="90" align="center" />
            <el-table-column prop="stepTime" label="工序时间" min-width="160" />
            <el-table-column prop="temperature" label="工艺参数" min-width="130" />
            <el-table-column prop="operator" label="操作人" width="100" />
          </el-table>
        </el-card>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QualityTag from '../components/QualityTag.vue'
import InspectionTable from '../components/InspectionTable.vue'
import { productDetailApi } from '../api/trace'
import seafoodLogo from '../assets/images/海鲜.png'

const route = useRoute()
const router = useRouter()

const data = ref(null)
const loading = ref(false)

const farmNode = computed(() => (data.value && data.value.farm) || {})

// 各环节企业行：与消费者溯源页一致，直接读后端 render() 出来的四个 key
const nodeRows = computed(() => {
  if (!data.value) return []
  const d = data.value
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

onMounted(async () => {
  const code = route.params.code
  if (!code) {
    return
  }
  loading.value = true
  try {
    const res = await productDetailApi(code)
    data.value = res.data
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--bg-page);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 24px;
  height: 68px;
  background: var(--brand-gradient);
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

.title-wrap h1 {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.title-wrap p {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.back {
  margin-left: auto;
  color: #fff !important;
  font-size: 13px;
}

.detail-body {
  max-width: 960px;
  margin: 0 auto;
  padding: 18px 24px 40px;
}

.offline-alert {
  margin-bottom: 14px;
}

.card {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--brand-primary-dark);
}

.product-head {
  display: flex;
  gap: 18px;
  margin-bottom: 14px;
}

.thumb {
  width: 130px;
  height: 130px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #eef4fa;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-primary);
  font-size: 13px;
  text-align: center;
  padding: 6px;
}

.head-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.line {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.9;
}

.price {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.desc {
  margin-top: 6px;
}

.chain-item h4 {
  font-size: 14px;
  color: var(--text-main);
}

.chain-item p {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 2px;
}
</style>
