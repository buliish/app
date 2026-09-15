<template>
  <!--
    首页（功能菜单页）—— 参考原型图重新设计：
    顶部标题栏、banner 大图、企业信息、纵向功能大按钮、底部导航。
  -->
  <div class="menu-page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="app-header">
      <img :src="seafoodLogo" alt="logo" class="app-header-logo" />
      <h1 class="app-header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="menu-body">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 企业信息 -->
      <section class="company-info">
        <h2 class="company-name">{{ node.name || node.code || '企业用户' }}</h2>
        <p class="company-type">
          当前溯源节点类型：<span>{{ typeName }}</span>
        </p>
      </section>

      <!-- 功能大按钮 -->
      <section class="menu-actions">
        <button
          v-for="item in currentMenus"
          :key="item.name"
          class="action-btn"
          @click="handleMenuClick(item)"
        >
          <span class="btn-text">{{ item.name }}</span>
          <span class="btn-arrow">›</span>
        </button>
      </section>
    </main>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { getNodeInfoApi } from '../api/node'
import { TYPE_NAME } from '../utils/nodeType'

const router = useRouter()
const node = ref({})

const typeName = computed(() => TYPE_NAME[node.value.nodeType] || '未知类型')

/**
 * 不同类型企业的功能菜单配置
 * 养殖企业(1)、冷冻加工企业(2)、批发商(3)：新建产品批号、产品批号管理、下游企业进场确认
 * 零售商(4)：新建产品批号、产品批号管理
 *   —— 零售商处于产业链末端，上游确认后由系统自动生成溯源标识码，因此没有"下游企业进场确认"
 */
const FULL_MENUS = [
  { name: '新建产品批号', path: '/batch/create' },
  { name: '产品批号管理', path: '/batch/list' },
  { name: '下游企业进场确认', path: '/confirm' }
]

const RETAIL_MENUS = FULL_MENUS.slice(0, 2)

const ALL_MENUS = {
  1: FULL_MENUS,
  2: FULL_MENUS,
  3: FULL_MENUS,
  4: RETAIL_MENUS
}

const currentMenus = computed(() => ALL_MENUS[node.value.nodeType] || FULL_MENUS)

onMounted(async () => {
  const local = JSON.parse(localStorage.getItem('nodeInfo') || 'null')
  if (!local) {
    router.replace('/login')
    return
  }
  node.value = local
  try {
    const res = await getNodeInfoApi(local.code)
    if (res.data) {
      const d = res.data
      node.value = {
        id: d.nodeId,
        code: d.code,
        name: d.name || d.code,
        nodeType: d.nodeType || local.nodeType,
        token: local.token
      }
      localStorage.setItem('nodeInfo', JSON.stringify(node.value))
    }
  } catch (e) {
    /* 接口异常时沿用本地缓存，不影响演示 */
  }
})

function handleMenuClick(item) {
  router.push(item.path)
}
</script>

<style scoped>
.menu-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏 */



/* 主体可滚动区 */
.menu-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 540px;
  width: 100%;
  margin: 0 auto;
}

/* banner */


/* 企业信息 */
.company-info {
  text-align: center;
  margin: 22px 0 18px;
}

.company-name {
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
  margin: 0;
}

.company-type {
  margin: 8px 0 0;
  font-size: 14px;
  color: #606266;
}

.company-type span {
  color: #0b4f8c;
  font-weight: 600;
}

/* 功能按钮 */
.menu-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 100%;
  height: 58px;
  padding: 0 20px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 16px;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: #1d6fb8;
  color: #1d6fb8;
  box-shadow: 0 4px 12px rgba(29, 111, 184, 0.12);
}

.btn-text {
  font-weight: 500;
}

.btn-arrow {
  position: absolute;
  right: 18px;
  font-size: 20px;
  color: #c0c4cc;
}

.action-btn:hover .btn-arrow {
  color: #1d6fb8;
}

</style>
