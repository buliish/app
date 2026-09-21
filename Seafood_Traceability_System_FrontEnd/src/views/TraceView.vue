<template>
  <!--
    消费者端：对虾食品溯源查询界面（3.2.7）
    无需登录，输入溯源标识码即可查看该批对虾从养殖、冷冻加工、批发到零售的全链路信息。
  -->
  <div class="trace-page">
    <header class="trace-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <div class="title-wrap">
        <h1>冷冻对虾全产业链溯源系统</h1>
        <p>来源可查 · 去向可追 · 责任可究</p>
      </div>
      <el-link class="back-login" @click="router.push('/login')">返回登录</el-link>
    </header>

    <main class="trace-body">
      <el-card shadow="never" class="search-card">
        <h2>溯源码查询</h2>
        <p class="tip">请输入对虾产品包装上的溯源标识码（或产品编号），例如：SHZ202601010001</p>
        <div class="search-row">
          <el-input
            v-model="traceCode"
            size="large"
            placeholder="请输入溯源标识码"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" size="large" :loading="loading" @click="handleSearch">查 询</el-button>
        </div>

        <!--
          二维码跟着输入框联动：查询成功后显示该溯源码的专属二维码。
          手机扫它 → 打开 /trace?code=SHZ... → 页面自动带码查询 → 直接出结果。
          图片由后端 /trace/qrcode/{code} 直接吐 PNG，前端不做生成。

          地址里的主机名来自后端配置 trace.qrcode.base-url（环境变量 TRACE_QR_BASE_URL），
          演示时用 start-demo 脚本自动填本机局域网 IP —— 否则手机扫到 localhost 会指向手机自己。
        -->
        <div v-if="data && data.traceCode" class="qrcode-block">
          <img
            class="qrcode-img"
            :src="`/trace/qrcode/${encodeURIComponent(data.traceCode)}`"
            :alt="`溯源二维码 ${data.traceCode}`"
          />
          <p class="qrcode-code">{{ data.traceCode }}</p>
          <p class="qrcode-hint">手机扫码直达溯源结果</p>
        </div>
        <p v-else class="qrcode-hint qrcode-hint--idle">查询后显示该产品的溯源二维码</p>
      </el-card>

      <template v-if="data">
        <!-- 产品基本信息 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <span class="section-title">产品溯源信息</span>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="溯源标识码">
              <el-tag type="success" effect="dark">{{ data.traceCode }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="零售产品批号">{{ data.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="产品品种">{{ data.breed }}</el-descriptions-item>
            <el-descriptions-item label="产品类型">{{ data.productType }}</el-descriptions-item>
            <el-descriptions-item label="溯源标识生成时间">{{ data.traceTime }}</el-descriptions-item>
            <el-descriptions-item label="流通环节">养殖 → 冷冻加工 → 批发 → 零售</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 全链路时间轴 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <span class="section-title">全产业链流转链路</span>
          </template>
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
                  <span class="gap"></span>
                  品种：{{ item.breed || '-' }}
                  <span v-if="item.productType"> / {{ item.productType }}</span>
                </p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 各环节企业信息 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <span class="section-title">各环节企业信息</span>
          </template>
          <el-table :data="nodeRows" border stripe size="small">
            <el-table-column prop="stage" label="环节" width="130" align="center" />
            <el-table-column prop="name" label="企业名称" min-width="220" />
            <el-table-column prop="address" label="地址" min-width="200" />
            <el-table-column prop="corporation" label="联系人" width="110" />
            <el-table-column prop="telephone" label="联系电话" width="140" />
          </el-table>
        </el-card>

        <!-- 加工工序记录 -->
        <el-card v-if="data.processRecords && data.processRecords.length" shadow="never" class="result-card">
          <template #header>
            <span class="section-title">冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）</span>
          </template>
          <el-table :data="data.processRecords" border stripe size="small">
            <el-table-column type="index" label="序号" width="70" align="center" />
            <el-table-column prop="step" label="工序" width="110" align="center" />
            <el-table-column prop="stepTime" label="工序时间" min-width="170" />
            <el-table-column prop="temperature" label="工艺参数" min-width="140" />
            <el-table-column prop="operator" label="操作人" width="110" />
            <el-table-column prop="remark" label="备注" min-width="180" />
          </el-table>
        </el-card>
      </template>

      <el-empty v-else-if="searched" description="未查询到该溯源标识码，请核对后重试" />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { traceApi } from '../api/trace'
import seafoodLogo from '../assets/images/海鲜.png'

const router = useRouter()
const route = useRoute()
const traceCode = ref('')
const data = ref(null)
const loading = ref(false)
const searched = ref(false)

// 支持从二维码扫码进入：/trace?code=SHZ...
onMounted(() => {
  const code = route.query.code
  if (code) {
    traceCode.value = String(code)
    handleSearch()
  }
})

// 各环节企业信息行
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

async function handleSearch() {
  const code = traceCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入溯源标识码！')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await traceApi(code)
    data.value = res.data
    ElMessage.success('查询成功！')
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.trace-page {
  min-height: 100vh;
  background: #f0f4f8;
}

.trace-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 40px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  color: #fff;
}

.logo {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: #fff;
  padding: 3px;
  object-fit: cover;
}

.title-wrap h1 {
  font-size: 21px;
  letter-spacing: 1px;
}

.title-wrap p {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.85;
  letter-spacing: 2px;
}

.back-login {
  margin-left: auto;
  color: #fff !important;
}

.trace-body {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.search-card {
  border-radius: 12px;
  text-align: center;
}

.search-card h2 {
  color: #0b4f8c;
  font-size: 22px;
}

.tip {
  margin: 10px 0 18px;
  color: #909399;
  font-size: 13px;
}

.search-row {
  display: flex;
  gap: 12px;
  max-width: 620px;
  margin: 0 auto;
}

.result-card {
  margin-top: 20px;
  border-radius: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #0b4f8c;
}

.chain-item h4 {
  color: #303133;
  font-size: 15px;
}

.chain-item p {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.gap {
  display: inline-block;
  width: 18px;
}

/* 二维码：查询成功后出现在输入框下方 */
.qrcode-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-top: 18px;
}

.qrcode-img {
  width: 180px;
  height: 180px;
  border: 1px solid var(--border-light, #e4e7ed);
  border-radius: 8px;
  background: #fff;
  padding: 6px;
}

.qrcode-code {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #1d6fb8;
}

.qrcode-hint {
  font-size: 12px;
  color: #909399;
}

.qrcode-hint--idle {
  display: block;
  text-align: center;
  margin-top: 14px;
}
</style>
