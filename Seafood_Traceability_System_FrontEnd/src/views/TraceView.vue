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
          <!--
            扫一扫：调起手机摄像头实时识别商品二维码。
            需要安全上下文（https 或 localhost）才能调用摄像头，
            vite 已启用 basic-ssl，手机首次访问需手动信任自签名证书。
          -->
          <el-button size="large" :icon="Camera" @click="openScan">扫一扫</el-button>
        </div>
        <p class="scan-tip">扫码需用手机访问（浏览器要求摄像头运行在 https 下）</p>
      </el-card>

      <!-- 扫码取景框：识别成功后自动关闭并展示结果 -->
      <el-dialog v-model="scanVisible" title="扫一扫" width="min(92vw, 420px)"
                 align-center :close-on-click-modal="false" @closed="stopScan">
        <div class="scan-wrap">
          <video ref="videoRef" class="scan-video" playsinline muted></video>
          <div class="scan-frame"></div>
          <p class="scan-status">{{ scanStatus }}</p>
        </div>
        <template #footer>
          <el-button @click="scanVisible = false">取 消</el-button>
        </template>
      </el-dialog>

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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import jsQR from 'jsqr'
import { traceApi } from '../api/trace'
import seafoodLogo from '../assets/images/海鲜.png'

const router = useRouter()
const route = useRoute()
const traceCode = ref('')
const data = ref(null)
const loading = ref(false)

const searched = ref(false)

// ---------------- 扫一扫 ----------------
const scanVisible = ref(false)
const scanStatus = ref('正在启动摄像头…')
const videoRef = ref(null)
let stream = null
let rafId = 0

/**
 * 打开扫码对话框并启动摄像头。
 *
 * 浏览器要求 getUserMedia 运行在**安全上下文**（https 或 localhost），
 * 因此手机必须通过 https://<局域网IP>:5173 访问 —— vite 已启用自签名证书，
 * 首次访问需手动点「高级 → 继续前往」。
 * 若在 http 下打开，navigator.mediaDevices 会是 undefined，这里给出明确提示。
 */
async function openScan() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    ElMessage.error('当前环境不支持调用摄像头，请用 https 访问本页')
    return
  }
  scanVisible.value = true
  scanStatus.value = '正在启动摄像头…'
  try {
    // 优先后置摄像头（手机扫码用后摄更顺手）
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' } }
    })
    await nextTick()
    const video = videoRef.value
    video.srcObject = stream
    await video.play()
    scanStatus.value = '将二维码对准取景框'
    loop()
  } catch (e) {
    scanStatus.value = '无法启动摄像头，请检查浏览器权限设置'
  }
}

/** 逐帧取景解码。用 requestAnimationFrame 控制节奏，不占用过多 CPU */
function loop() {
  const video = videoRef.value
  if (!video || !stream) return
  if (video.readyState === video.HAVE_ENOUGH_DATA) {
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    const img = ctx.getImageData(0, 0, canvas.width, canvas.height)
    const qr = jsQR(img.data, img.width, img.height)
    if (qr && qr.data) {
      onScanned(qr.data)
      return
    }
  }
  rafId = requestAnimationFrame(loop)
}

/**
 * 识别成功。
 * 二维码里可能是一段完整 URL（形如 https://<IP>:5173/trace?code=SHZ...），
 * 也可能直接就是溯源码本身 —— 两种都要能处理。
 */
function onScanned(text) {
  scanStatus.value = '识别成功！'
  const code = extractCode(text)
  scanVisible.value = false
  stopScan()
  if (!code) {
    ElMessage.warning('二维码内容无法识别为溯源标识码')
    return
  }
  traceCode.value = code
  ElMessage.success('已识别：' + code)
  handleSearch()
}

/** 从扫描结果里提取溯源码：是 URL 就取 code 参数，否则当作纯码 */
function extractCode(text) {
  const s = String(text || '').trim()
  const m = s.match(/[?&]code=([^&\s]+)/)
  if (m) return decodeURIComponent(m[1])
  if (/^[A-Za-z0-9_-]{6,50}$/.test(s)) return s
  return ''
}

/** 释放摄像头。对话框关闭时必须调用，否则摄像头指示灯会一直亮着 */
function stopScan() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

// 支持从二维码链接进入：/trace?code=SHZ...
onMounted(() => {
  const code = route.query.code
  if (code) {
    traceCode.value = String(code)
    handleSearch()
  }
})

// 离开页面时释放摄像头，避免指示灯常亮
onBeforeUnmount(stopScan)

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

/* 扫一扫 */
.scan-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.scan-wrap {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.scan-video {
  width: 100%;
  display: block;
}

/* 取景框：只是视觉引导，解码用的是整帧 */
.scan-frame {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 62%;
  aspect-ratio: 1 / 1;
  transform: translate(-50%, -50%);
  border: 2px solid #1d6fb8;
  border-radius: 8px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.35);
  pointer-events: none;
}

.scan-status {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  text-align: center;
}
</style>
