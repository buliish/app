import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MenuView from '../views/MenuView.vue'
import MineView from '../views/MineView.vue'
import UpdatePwdView from '../views/UpdatePwdView.vue'
import BatchCreateView from '../views/BatchCreateView.vue'
import BatchListView from '../views/BatchListView.vue'
import BatchDetailView from '../views/BatchDetailView.vue'
import ConfirmView from '../views/ConfirmView.vue'
import TraceView from '../views/TraceView.vue'
import ConsumerHomeView from '../views/ConsumerHomeView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import NodeManageView from '../views/admin/NodeManageView.vue'

const BRAND = '冷冻对虾全产业链溯源系统'

/**
 * 前端路由配置
 * 消费者端：首页（3.2.7.1，免登录，根路径）→ 点「朔源」按钮跳转溯源查询页
 * 流通节点端：登录 / 功能菜单 / 更新密码 / 批号新建·管理·详情 / 下游进场确认
 * 管理端：管理员登录 / 节点企业注册信息管理（左侧 CRUD + 右侧注册信息统计，同一页面）
 *
 * 注意：管理端页面统一使用 /sys 前缀。
 * 后端接口前缀是 /admin，而 vite 开发服务器会把 /admin 开头的请求全部代理给 SpringBoot，
 * 若页面路由也用 /admin，浏览器刷新或直接在地址栏访问管理端页面时，
 * 请求会被代理转发到后端（返回 401/405）而不是加载 index.html，页面就会“被拦截”。
 */
const routes = [
  // 根路径直接进消费者端首页（3.2.7.1），点页面上的「朔源」按钮再跳 /trace
  { path: '/', name: 'ConsumerHome', component: ConsumerHomeView, meta: { title: '消费者端首页' } },
  { path: '/login', name: 'Login', component: LoginView, meta: { title: '登录' } },
  { path: '/menu', name: 'Menu', component: MenuView, meta: { title: '功能菜单', requiresAuth: true } },
  { path: '/mine', name: 'Mine', component: MineView, meta: { title: '我的', requiresAuth: true } },
  { path: '/updatePwd', name: 'UpdatePwd', component: UpdatePwdView, meta: { title: '更新密码', requiresAuth: true } },
  { path: '/batch/create', name: 'BatchCreate', component: BatchCreateView, meta: { title: '新建产品批号', requiresAuth: true } },
  { path: '/batch/update/:id', name: 'BatchUpdate', component: BatchCreateView, meta: { title: '更新产品批号', requiresAuth: true } },
  { path: '/batch/list', name: 'BatchList', component: BatchListView, meta: { title: '产品批号管理', requiresAuth: true } },
  { path: '/batch/detail/:id', name: 'BatchDetail', component: BatchDetailView, meta: { title: '浏览产品批号详情', requiresAuth: true } },
  { path: '/confirm', name: 'Confirm', component: ConfirmView, meta: { title: '下游企业进场确认', requiresAuth: true } },
  { path: '/trace', name: 'Trace', component: TraceView, meta: { title: '对虾食品溯源查询' } },
  // 商品详情（免登录，与 /trace 同属消费者端）
  { path: '/product/:code', name: 'ProductDetail', component: ProductDetailView, meta: { title: '商品溯源详情' } },
  { path: '/sys/login', name: 'AdminLogin', component: AdminLoginView, meta: { title: '管理端登录' } },
  { path: '/sys/nodes', name: 'AdminNodes', component: NodeManageView, meta: { title: '节点企业注册信息管理', requiresAdmin: true } },
  {
    path: '/sys/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/admin/DashboardView.vue'),
    meta: { title: '数据可视化大屏', requiresAdmin: true }
  },
  {
    path: '/sys/trace',
    name: 'AdminTrace',
    component: () => import('../views/admin/TraceQueryView.vue'),
    meta: { title: '批次追溯查询', requiresAdmin: true }
  },
  // 404 兜底：必须放在最后，匹配所有未定义的路径
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：节点端需登录，管理端需管理员 token
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${BRAND}` : BRAND
  if (to.meta.requiresAuth && !localStorage.getItem('nodeInfo')) {
    next('/login')
    return
  }
  if (to.meta.requiresAdmin && !localStorage.getItem('adminToken')) {
    next('/sys/login')
    return
  }
  next()
})

export default router
