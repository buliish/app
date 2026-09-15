<template>
  <!--
    消费者端首页（3.2.7.1，免登录，根路径）。
    展示"在售产品"商品墙，点商品进入商品溯源详情；
    页头的「朔源」按钮进入溯源码查询页（消费者手里有包装上的码时走那条路）。

    注意：本页是免登录的，只能调 /trace/** 下的接口。
    /region/** 需要登录（不在 LoginInterceptor 放行名单里），
    若在这里调 provincesApi() 会拿到 401，而 axios 响应拦截器遇到 401
    会执行 window.location.href='/login' —— 整个消费者首页会被弹回登录页。
    因此产地筛选没有做；后端 listProducts 仍支持 provId，将来若要开放，
    需要先把省份字典改成公开接口（如挂到 /trace/ 下）。
  -->
  <div class="home-page">
    <header class="home-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <div class="title-wrap">
        <h1>冷冻对虾全产业链溯源系统</h1>
        <p>来源可查 · 去向可追 · 责任可究</p>
      </div>
      <div class="header-actions">
        <el-button type="warning" @click="router.push('/trace')">朔 源</el-button>
        <el-link class="back-login" @click="router.push('/login')">流通节点端登录</el-link>
      </div>
    </header>

    <main class="home-body">
      <!-- 筛选区：关键词 + 形态 + 产地 -->
      <el-card shadow="never" class="filter-card">
        <div class="filter-row">
          <el-input
            v-model="query.keyword"
            placeholder="搜索品种 / 品类 / 商品编号"
            clearable
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="query.form" placeholder="产品形态" clearable @change="search">
            <el-option v-for="f in PRODUCT_FORMS" :key="f" :label="f" :value="f" />
          </el-select>
          <el-button type="primary" @click="search">查 询</el-button>
        </div>
      </el-card>

      <!-- 商品墙 -->
      <div v-loading="loading" class="product-grid">
        <el-empty
          v-if="!loading && products.length === 0"
          description="暂无在售产品"
        />

        <div
          v-for="item in products"
          :key="item.retaBatchId"
          class="product-card"
          @click="openProduct(item)"
        >
          <div class="thumb">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.breed" />
            <div v-else class="thumb-fallback">{{ item.productType || '对虾' }}</div>
          </div>

          <div class="info">
            <div class="name-row">
              <span class="name">{{ item.breed || '—' }}</span>
              <QualityTag :status="item.qualityStatus" />
            </div>
            <p class="line">品类：{{ item.productType || '—' }}</p>
            <p class="line">
              规格：{{ item.specGrade || '—' }}
              <template v-if="item.productForm"> · {{ item.productForm }}</template>
            </p>
            <p class="line">产地：{{ item.originProvince || '—' }}</p>
            <p class="line muted">{{ item.retailerName || '—' }}</p>
            <div class="price-row">
              <span class="price">
                <template v-if="item.price !== null && item.price !== undefined">
                  ¥{{ Number(item.price).toFixed(2) }}
                </template>
                <template v-else>价格面议</template>
              </span>
              <span class="trace-link">查看溯源 ›</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="total > query.size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :current-page="query.current"
          :page-size="query.size"
          @current-change="onPageChange"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import QualityTag from '../components/QualityTag.vue'
import { productListApi } from '../api/trace'
import { PRODUCT_FORMS } from '../utils/nodeType'
import seafoodLogo from '../assets/images/海鲜.png'

const router = useRouter()

const query = reactive({ current: 1, size: 12, keyword: '', form: '' })
const products = ref([])
const total = ref(0)
const loading = ref(false)

onMounted(loadProducts)

async function loadProducts() {
  loading.value = true
  try {
    const res = await productListApi({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      form: query.form || undefined
    })
    const data = res.data || {}
    products.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  loadProducts()
}

function onPageChange(page) {
  query.current = page
  loadProducts()
}

// 商品详情页的入口参数既接受对外产品编号，也接受溯源标识码
function openProduct(item) {
  const code = item.productCode || item.traceCode
  if (!code) {
    return
  }
  router.push(`/product/${encodeURIComponent(code)}`)
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-page);
}

.home-header {
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

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-login {
  color: #fff !important;
  font-size: 13px;
}

.home-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px 40px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-row .el-input {
  flex: 1 1 260px;
}

.filter-row .el-select {
  width: 150px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  min-height: 160px;
}

.product-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.product-card:hover {
  box-shadow: 0 6px 18px rgba(11, 79, 140, 0.12);
  transform: translateY(-2px);
}

.thumb {
  width: 84px;
  height: 84px;
  flex-shrink: 0;
  border-radius: 8px;
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
  font-size: 12px;
  color: var(--brand-primary);
  text-align: center;
  padding: 4px;
}

.info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line {
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line.muted {
  color: var(--text-muted);
}

.price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 4px;
}

.price {
  font-size: 15px;
  font-weight: 700;
  color: #f56c6c;
}

.trace-link {
  font-size: 12px;
  color: var(--brand-primary);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}
</style>
