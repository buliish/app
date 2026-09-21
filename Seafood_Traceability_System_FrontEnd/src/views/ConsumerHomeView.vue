<template>
  <!--
    消费者端首页（3.2.7.1 第一步）
    事件表：「消费者端首页」的「朔源」按钮 Click → 跳转肉类食品朔源信息页面。
    因此这里只做入口宣传 + 一个「朔源」按钮，真正的查询在 /trace 页面完成。
  -->
  <div class="consumer-home">
    <header class="home-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <div class="title-wrap">
        <h1>冷冻对虾全产业链溯源系统</h1>
        <p>来源可查 · 去向可追 · 责任可究</p>
      </div>
      <el-link class="back-login" @click="router.push('/login')">返回登录</el-link>
    </header>

    <main class="home-body">
      <!-- 主视觉 -->
      <section class="hero">
        <img :src="bannerBg" alt="海鲜产品溯源" class="hero-bg" />
        <div class="hero-mask"></div>
        <div class="hero-content">
          <h2>扫码溯源头，吃得放心</h2>
          <p>输入包装上的溯源标识码，即可查看该批对虾从养殖、冷冻加工、批发到零售的完整流转链路。</p>
          <el-button type="primary" size="large" class="trace-btn" @click="goTrace">
            溯 源
          </el-button>
        </div>
      </section>

      <!-- 在售产品：消费者看到的是"产品"本身，而不是流通企业 -->
      <section class="products">
        <div class="products-head">
          <h3>在售产品</h3>
          <div class="products-filter">
            <el-select v-model="form" placeholder="全部形态" clearable size="default" style="width: 130px" @change="loadProducts">
              <el-option v-for="f in PRODUCT_FORMS" :key="f" :label="f" :value="f" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="搜索品种 / 品类"
              clearable
              size="default"
              style="width: 200px"
              @keyup.enter="loadProducts"
              @clear="loadProducts"
            />
            <el-button type="primary" @click="loadProducts">搜索</el-button>
          </div>
        </div>

        <div v-loading="loadingProducts" class="product-grid">
          <div v-for="p in products" :key="p.retaBatchId" class="product-card" @click="goProduct(p)">
            <div class="product-img-wrap">
              <img :src="p.imageUrl || defaultProductImg" alt="产品图片" class="product-img" />
              <QualityTag :status="p.qualityStatus" size="small" effect="dark" class="product-quality" />
            </div>
            <div class="product-info">
              <h4 class="product-name">{{ productName(p) }}</h4>
              <div class="product-meta">
                <el-tag size="small" :type="p.productForm === '鲜虾' ? 'success' : 'primary'" effect="plain">
                  {{ p.productForm || '冻虾' }}
                </el-tag>
                <span v-if="p.specGrade" class="product-spec">{{ p.specGrade }}</span>
              </div>
              <div class="product-origin">
                产地：{{ (p.originProvName || '') + (p.originCityName || '') || '—' }}
                <template v-if="p.sourceType"> · {{ p.sourceType }}</template>
              </div>
              <div class="product-foot">
                <span class="product-price">{{ p.price != null ? `¥${p.price}` : '—' }}</span>
                <span class="product-btn">查看溯源 →</span>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-if="!loadingProducts && products.length === 0" description="暂无在售产品" />

        <el-pagination
          v-if="total > size"
          class="products-pager"
          background
          layout="total, prev, pager, next"
          :total="total"
          :current-page="current"
          :page-size="size"
          @current-change="onPageChange"
        />
      </section>

      <!-- 全链路说明 -->
      <section class="chain">
        <h3>全产业链追溯环节</h3>
        <div class="chain-steps">
          <div v-for="(s, i) in STEPS" :key="s.title" class="step">
            <div class="step-index">{{ i + 1 }}</div>
            <div class="step-title">{{ s.title }}</div>
            <div class="step-desc">{{ s.desc }}</div>
            <div v-if="i < STEPS.length - 1" class="step-arrow">→</div>
          </div>
        </div>
      </section>

      <!-- 查询入口（页面中部也给一个，方便不滚动时直接点） -->
      <section class="quick">
        <el-input
          v-model="traceCode"
          size="large"
          :placeholder="tracePlaceholder"
          clearable
          @keyup.enter="goTrace"
        >
          <template #append>
            <el-button type="primary" @click="goTrace">溯 源</el-button>
          </template>
        </el-input>
      </section>
    </main>

    <footer class="home-footer">
      <p>© 冷冻对虾全产业链溯源系统 · 消费者端</p>
      <p class="footer-links">
        <el-link type="info" @click="router.push('/trace')">溯源查询</el-link>
        <el-divider direction="vertical" />
        <el-link type="info" @click="router.push('/login')">流通节点端登录</el-link>
        <el-divider direction="vertical" />
        <el-link type="info" @click="router.push('/sys/login')">管理端入口</el-link>
      </p>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { productListApi } from '../api/trace'
import QualityTag from '../components/QualityTag.vue'
import { PRODUCT_FORMS } from '../utils/nodeType'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import defaultProductImg from '../assets/images/海鲜产品溯源.jpeg'

const router = useRouter()
const traceCode = ref('')

// ---------------- 在售产品 ----------------
const products = ref([])
const loadingProducts = ref(false)
const keyword = ref('')
const form = ref('')
const current = ref(1)
const size = ref(8)
const total = ref(0)

const STEPS = [
  { title: '养殖', desc: '养殖场投苗、检疫出证' },
  { title: '冷冻加工', desc: '进场确认、加工工序' },
  { title: '批发', desc: '批发商进场流通' },
  { title: '零售', desc: '零售商确认后生成溯源码' }
]

async function loadProducts() {
  loadingProducts.value = true
  try {
    const res = await productListApi({
      current: current.value,
      size: size.value,
      keyword: keyword.value.trim() || undefined,
      form: form.value || undefined
    })
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    products.value = []
    total.value = 0
  } finally {
    loadingProducts.value = false
  }
}

function onPageChange(page) {
  current.value = page
  loadProducts()
}

// 商品名由规格/形态/品类/品种拼出，避免库里再存一个会不一致的冗余字段
function productName(p) {
  return [p.specGrade, p.productForm, p.productType, p.breed].filter(Boolean).join(' · ') || '对虾产品'
}

function goProduct(p) {
  router.push(`/product/${encodeURIComponent(p.traceCode || p.productCode)}`)
}

// 提示码取自真实数据，避免写死后与实际库内容脱节（历史上就有过一次）
const tracePlaceholder = computed(() =>
  products.value.length
    ? `也可直接粘贴溯源标识码，如 ${products.value[0].traceCode}`
    : '也可直接粘贴溯源标识码'
)

//点击「朔源」跳转溯源查询页；若已输入标识码则一并带过去，省得再输一次
function goTrace() {
  const code = traceCode.value.trim()
  router.push(code ? { path: '/trace', query: { code } } : '/trace')
}

onMounted(loadProducts)
</script>

<style scoped>
.consumer-home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f4f8;
}

.home-header {
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

.home-body {
  flex: 1;
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

/* 主视觉 */
.hero {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(11, 79, 140, 0.18);
}

.hero-bg {
  width: 100%;
  height: 320px;
  object-fit: cover;
  display: block;
}

.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(7, 42, 74, 0.86) 0%, rgba(7, 42, 74, 0.45) 60%, rgba(7, 42, 74, 0.15) 100%);
}

.hero-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  padding: 0 56px;
  color: #fff;
  max-width: 620px;
}

.hero-content h2 {
  font-size: 30px;
  letter-spacing: 2px;
}

.hero-content p {
  font-size: 14px;
  line-height: 1.9;
  opacity: 0.9;
}

.trace-btn {
  align-self: flex-start;
  margin-top: 6px;
  height: 48px;
  padding: 0 52px;
  font-size: 17px;
  letter-spacing: 8px;
  border-radius: 8px;
}

/* 在售产品 */
.products {
  margin-top: 32px;
}

.products-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.products-head h3 {
  font-size: 17px;
  color: var(--c-primary);
}

.products-filter {
  display: flex;
  gap: 8px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 120px;
}

.product-card {
  background: var(--c-white);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--sh-card);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-float);
}

.product-img-wrap {
  position: relative;
  height: 150px;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-quality {
  position: absolute;
  top: 8px;
  right: 8px;
}

.product-info {
  padding: 12px 14px 14px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  line-height: 1.5;
  min-height: 42px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.product-spec {
  font-size: 12px;
  color: var(--c-text-sub);
}

.product-origin {
  margin-top: 6px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.product-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.product-price {
  font-size: 17px;
  font-weight: 700;
  color: #e6653a;
}

.product-btn {
  font-size: 12px;
  color: var(--c-primary-light);
}

.products-pager {
  margin-top: 18px;
  justify-content: center;
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 环节说明 */
.chain {
  margin-top: 32px;
  background: #fff;
  border-radius: 14px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(11, 79, 140, 0.06);
}

.chain h3 {
  font-size: 17px;
  color: #0b4f8c;
  margin-bottom: 20px;
}

.chain-steps {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.step {
  position: relative;
  flex: 1;
  text-align: center;
  padding: 0 6px;
}

.step-index {
  width: 38px;
  height: 38px;
  line-height: 38px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0b4f8c, #1d6fb8);
  color: #fff;
  font-weight: 600;
}

.step-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.step-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}

.step-arrow {
  position: absolute;
  right: -10px;
  top: 9px;
  color: #c0c4cc;
  font-size: 18px;
}

/* 快捷查询 */
.quick {
  margin-top: 24px;
}

.home-footer {
  padding: 20px 0 28px;
  text-align: center;
  color: #909399;
  font-size: 12px;
}

.footer-links {
  margin-top: 8px;
}
</style>
