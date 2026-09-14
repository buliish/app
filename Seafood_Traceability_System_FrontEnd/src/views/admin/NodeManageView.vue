<template>
  <!--
    节点企业注册信息管理界面（3.2.8）
    左侧：节点企业管理 CRUD（模糊查询、表格、分页、新建/详情/编辑/删除）
    右侧：注册信息统计（指标卡 + 注册趋势折线图 + 省分组饼图 + 类型分组饼图 + 各省柱状图）
  -->
  <div class="admin-page">
    <header class="admin-header">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
      <h1>冷冻对虾全产业链溯源系统 · 节点企业注册信息管理</h1>
      <div class="header-actions">
        <button
          type="button"
          class="header-logout"
          @click="handleLogout"
          @mouseenter="logoutHover = true"
          @mouseleave="logoutHover = false"
        >
          <img class="logout-icon" :src="logoutHover ? logoutActiveIcon : logoutIcon" alt="退出登录" />
          <span>退出登录</span>
        </button>
      </div>
    </header>

    <el-main class="admin-body">
      <div class="admin-container">
        <!-- 左侧：节点企业注册信息管理 -->
        <div class="admin-left">
          <el-card shadow="never" class="crud-card">
            <template #header>
              <div class="card-head">
                <span class="card-title">节点企业注册信息管理</span>
              </div>
            </template>

            <!-- 模糊查询 -->
            <el-form inline class="search-form">
              <el-form-item label="名称">
                <el-input v-model="query.name" placeholder="模糊查询" clearable style="width: 160px" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
                  <el-option v-for="(name, key) in TYPE_NAME" :key="key" :label="name" :value="Number(key)" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属省">
                <el-select v-model="query.provId" placeholder="全部" clearable style="width: 150px" @change="onProvChange">
                  <el-option v-for="p in provinces" :key="p.provId" :label="p.provName" :value="p.provId" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属市">
                <el-select v-model="query.cityId" placeholder="全部" clearable style="width: 150px">
                  <el-option v-for="c in cities" :key="c.cityId" :label="c.cityName" :value="c.cityId" />
                </el-select>
              </el-form-item>
              <el-form-item class="search-actions">
                <el-button @click="resetQuery">清 空</el-button>
                <el-button type="primary" @click="search">查 询</el-button>
                <el-button type="primary" @click="openDialog('create')">新 建</el-button>
              </el-form-item>
            </el-form>

            <!-- 表格 -->
            <el-table v-loading="tableLoading" :data="list" border stripe>
              <el-table-column prop="nodeId" label="编号" width="70" align="center" />
              <el-table-column prop="name" label="企业名称" min-width="180" />
              <el-table-column label="企业类型" width="120" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :style="typeTagStyle(row.nodeType)">
                    {{ TYPE_NAME[row.nodeType] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="所属省" width="120" align="center">
                <template #default="{ row }">{{ provName(row) }}</template>
              </el-table-column>
              <el-table-column label="所属市" width="120" align="center">
                <template #default="{ row }">{{ cityName(row) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openDialog('detail', row)">详情</el-button>
                  <el-button link type="primary" @click="openDialog('edit', row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <el-pagination
              class="pager"
              background
              layout="total, prev, pager, next"
              :total="total"
              :current-page="query.current"
              :page-size="query.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onPageChange"
              @size-change="onSizeChange"
            />
          </el-card>
        </div>

        <!-- 右侧：统计图表 -->
        <div class="admin-right">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="section-title">近十二个月节点企业注册数量趋势</span></template>
            <div ref="trendRef" class="chart chart-lg"></div>
          </el-card>

          <div class="chart-row">
            <el-card shadow="never" class="chart-card">
              <template #header><span class="section-title">按省分组注册数量分布</span></template>
              <div ref="provPieRef" class="chart"></div>
            </el-card>

            <el-card shadow="never" class="chart-card">
              <template #header><span class="section-title">按企业类型分组注册数量分布</span></template>
              <div ref="typePieRef" class="chart"></div>
            </el-card>
          </div>

          <el-card shadow="never" class="chart-card">
            <template #header><span class="section-title">各省节点企业注册数量统计</span></template>
            <div ref="provBarRef" class="chart chart-lg"></div>
          </el-card>
        </div>
      </div>
    </el-main>

    <!-- 新建 / 编辑 / 详情 对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :disabled="dialogMode === 'detail'"
        label-width="110px"
      >
        <el-form-item label="登录编码" prop="code">
          <el-input v-model="form.code" :disabled="dialogMode !== 'create'" placeholder="如 farm001 / froz001 / whol001 / reta001" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="登录密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入初始登录密码" />
        </el-form-item>
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="企业类型" prop="nodeType">
          <el-select v-model="form.nodeType" placeholder="请选择企业类型" style="width: 100%">
            <el-option v-for="(name, key) in TYPE_NAME" :key="key" :label="name" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在省" prop="provId">
          <el-select v-model="form.provId" placeholder="请选择省" style="width: 100%" @change="onFormProvChange">
            <el-option v-for="p in provinces" :key="p.provId" :label="p.provName" :value="p.provId" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在市" prop="cityId">
          <el-select v-model="form.cityId" placeholder="请选择市" style="width: 100%">
            <el-option v-for="c in formCities" :key="c.cityId" :label="c.cityName" :value="c.cityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="营业执照号" prop="businessId">
          <el-input v-model="form.businessId" placeholder="请输入营业执照编号" />
        </el-form-item>
        <el-form-item label="企业法人" prop="corporation">
          <el-input v-model="form.corporation" placeholder="请输入企业法人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="telephone">
          <el-input v-model="form.telephone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="注册时间" prop="regDate">
          <el-date-picker v-model="form.regDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择注册时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template v-if="dialogMode !== 'detail'" #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </template>
      <template v-else #footer>
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  adminNodeDeleteApi,
  adminNodePageApi,
  adminNodeSaveApi,
  adminNodeUpdateApi,
  adminStatsApi
} from '../../api/admin'
import { citiesApi, provincesApi } from '../../api/region'
import { TYPE_NAME, typeTagStyle } from '../../utils/nodeType'
import seafoodLogo from '../../assets/images/海鲜.png'
import logoutIcon from '../../assets/images/登出.png'
import logoutActiveIcon from '../../assets/images/登出 (高亮).png'

const router = useRouter()

// 退出按钮悬停状态：默认显示普通图标，悬停时切换为高亮图标
const logoutHover = ref(false)

// ========== 左侧：节点企业管理 CRUD ==========
const provinces = ref([])
const cities = ref([])
const formCities = ref([])
const cityMap = ref(new Map()) // key: `${provId}-${cityId}` -> cityName

const query = reactive({ current: 1, size: 10, name: '', type: null, provId: null, cityId: null })
const list = ref([])
const total = ref(0)
const tableLoading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create') // create / edit / detail
const saving = ref(false)
const formRef = ref()
const form = reactive({
  nodeId: null,
  code: '',
  password: '',
  name: '',
  nodeType: 1,
  provId: null,
  cityId: null,
  address: '',
  businessId: '',
  corporation: '',
  telephone: '',
  regDate: ''
})

const dialogTitle = computed(() => {
  return dialogMode.value === 'create' ? '新建节点企业' : dialogMode.value === 'edit' ? '编辑节点企业' : '节点企业详情'
})

const formRules = {
  code: [{ required: true, message: '请输入登录编码', trigger: 'blur' }],
  password: [{
    validator: (rule, value, callback) => {
      if (dialogMode.value === 'create' && !value) {
        callback(new Error('请输入登录密码'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }],
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
  provId: [{ required: true, message: '请选择所在省', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择所在市', trigger: 'change' }]
}

onMounted(async () => {
  try {
    const res = await provincesApi()
    provinces.value = res.data || []
  } catch (e) {
    provinces.value = []
  }
  loadList()
  loadStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ;[trendChart, provPieChart, typePieChart, provBarChart].forEach((c) => c && c.dispose())
})

async function loadList() {
  tableLoading.value = true
  try {
    const res = await adminNodePageApi({
      current: query.current,
      size: query.size,
      name: query.name || undefined,
      type: query.type || undefined,
      provId: query.provId || undefined,
      cityId: query.cityId || undefined
    })
    const records = res.data.records || []
    // 缓存表格中涉及到的城市名称
    const provIds = [...new Set(records.map((r) => r.provId).filter(Boolean))]
    await Promise.all(
      provIds.map(async (pid) => {
        const hasThisProv = [...cityMap.value.keys()].some((k) => k.startsWith(`${pid}-`))
        if (hasThisProv) return
        try {
          const cityRes = await citiesApi(pid)
          ;(cityRes.data || []).forEach((c) => {
            cityMap.value.set(`${pid}-${c.cityId}`, c.cityName)
          })
        } catch (e) {
          // 单个省市查询失败不影响列表展示
        }
      })
    )
    list.value = records
    total.value = res.data.total || 0
  } finally {
    tableLoading.value = false
  }
}

function search() {
  query.current = 1
  loadList()
}

function resetQuery() {
  Object.assign(query, { current: 1, name: '', type: null, provId: null, cityId: null })
  cities.value = []
  loadList()
}

async function onProvChange(provId) {
  query.cityId = null
  if (!provId) {
    cities.value = []
    return
  }
  const res = await citiesApi(provId)
  cities.value = res.data || []
}

function onPageChange(page) {
  query.current = page
  loadList()
}

function onSizeChange(size) {
  query.size = size
  query.current = 1
  loadList()
}

function provName(row) {
  return provinces.value.find((p) => p.provId === row.provId)?.provName || row.provId || '-'
}

function cityName(row) {
  return cityMap.value.get(`${row.provId}-${row.cityId}`) || row.cityId || '-'
}

async function openDialog(mode, row) {
  dialogMode.value = mode
  formCities.value = []
  formRef.value && formRef.value.resetFields()
  if (mode === 'create') {
    Object.assign(form, {
      nodeId: null,
      code: '',
      password: '123456',
      name: '',
      nodeType: 1,
      provId: null,
      cityId: null,
      address: '',
      businessId: '',
      corporation: '',
      telephone: '',
      regDate: new Date().toISOString().slice(0, 10)
    })
  } else {
    Object.assign(form, {
      nodeId: row.nodeId,
      code: row.code,
      password: '',
      name: row.name,
      nodeType: row.nodeType,
      provId: row.provId,
      cityId: row.cityId,
      address: row.address,
      businessId: row.businessId,
      corporation: row.corporation,
      telephone: row.telephone,
      regDate: row.regDate
    })
    if (row.provId) {
      const res = await citiesApi(row.provId)
      formCities.value = res.data || []
    }
  }
  dialogVisible.value = true
}

async function onFormProvChange(provId) {
  form.cityId = null
  if (!provId) {
    formCities.value = []
    return
  }
  const res = await citiesApi(provId)
  formCities.value = res.data || []
}

function handleSave() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { ...form }
      if (dialogMode.value === 'edit') {
        delete payload.password
        await adminNodeUpdateApi(payload)
        ElMessage.success('节点企业信息更新成功！')
      } else {
        await adminNodeSaveApi(payload)
        ElMessage.success('节点企业注册成功！')
      }
      dialogVisible.value = false
      loadList()
      loadStats()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除节点企业「${row.name}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await adminNodeDeleteApi(row.nodeId)
      ElMessage.success('删除成功！')
      loadList()
      loadStats()
    })
    .catch(() => {})
}

// ========== 右侧：统计图表 ==========
const stats = ref({ typeDist: [], provDist: [], trend: { months: [], counts: [] } })

const trendRef = ref()
const provPieRef = ref()
const typePieRef = ref()
const provBarRef = ref()

let trendChart = null
let provPieChart = null
let typePieChart = null
let provBarChart = null

async function loadStats() {
  const res = await adminStatsApi()
  stats.value = res.data || {}
  await nextTick()
  renderCharts()
}

function handleResize() {
  ;[trendChart, provPieChart, typePieChart, provBarChart].forEach((c) => c && c.resize())
}

// 折线图 / 柱状图 tooltip：同样限制在卡片内显示
const axisTooltip = {
  trigger: 'axis',
  confine: true,
  backgroundColor: 'rgba(255,255,255,0.98)',
  borderColor: '#c9dcf2',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#12315a', fontSize: 12 },
  extraCssText: 'border-radius:8px;box-shadow:0 4px 14px rgba(11,79,140,0.18);'
}

// 汇总某项饼图数据的总数
function sumOf(list) {
  return (list || []).reduce((s, d) => s + Number(d.value || 0), 0)
}

// 饼图 tooltip：confine 保证浮层完整显示在卡片内不被裁剪，同时展示名称/数量/占比/总数
function pieTooltip(totalOf) {
  return {
    trigger: 'item',
    confine: true,
    enterable: true,
    backgroundColor: 'rgba(255,255,255,0.98)',
    borderColor: '#c9dcf2',
    borderWidth: 1,
    padding: [8, 12],
    textStyle: { color: '#12315a', fontSize: 12 },
    extraCssText: 'border-radius:8px;box-shadow:0 4px 14px rgba(11,79,140,0.18);max-width:220px;white-space:normal;',
    formatter: (p) => {
      const total = sumOf(totalOf()) || 1
      const percent = ((Number(p.value) / total) * 100).toFixed(1)
      return `<div style="font-weight:600;margin-bottom:4px">${p.name}</div>
        <div>注册数量：<span style="font-weight:600;color:#0b4f8c">${p.value}</span> 家</div>
        <div>所占比例：<span style="font-weight:600;color:#0b4f8c">${percent}%</span>（${p.value} / ${total} 家）</div>`
    }
  }
}

function renderCharts() {
  const trend = stats.value.trend || { months: [], counts: [] }

  // 1）注册数量趋势折线图
  trendChart = trendChart || echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: axisTooltip,
    grid: { left: 45, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: trend.months, axisLabel: { rotate: 40 } },
    yAxis: { type: 'value', name: '注册数量', minInterval: 1 },
    series: [
      {
        name: '注册数量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.counts,
        itemStyle: { color: '#1d6fb8' },
        areaStyle: { color: 'rgba(29,111,184,0.15)' }
      }
    ]
  })

  // 2）省分组注册数量分布饼图
  provPieChart = provPieChart || echarts.init(provPieRef.value)
  provPieChart.setOption({
    tooltip: pieTooltip(() => stats.value.provDist),
    legend: { bottom: 0, type: 'scroll', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        name: '省分布',
        type: 'pie',
        radius: ['35%', '60%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c} 家', fontSize: 11 },
        data: stats.value.provDist || []
      }
    ]
  })

  // 3）企业类型分组注册数量分布饼图
  typePieChart = typePieChart || echarts.init(typePieRef.value)
  typePieChart.setOption({
    tooltip: pieTooltip(() => stats.value.typeDist),
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
    color: ['#0f9d58', '#e8a33d', '#1d6fb8', '#8e44ad'],
    series: [
      {
        name: '类型分布',
        type: 'pie',
        radius: '55%',
        center: ['50%', '45%'],
        label: { formatter: '{b}: {c}', fontSize: 11 },
        data: stats.value.typeDist || []
      }
    ]
  })

  // 4）各省注册数量柱状图
  const provBar = stats.value.provBar || []
  provBarChart = provBarChart || echarts.init(provBarRef.value)
  provBarChart.setOption({
    tooltip: axisTooltip,
    grid: { left: 45, right: 20, top: 30, bottom: 50 },
    xAxis: { type: 'category', data: provBar.map((d) => d.name), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '注册数量', minInterval: 1 },
    series: [
      {
        name: '注册数量',
        type: 'bar',
        barWidth: '45%',
        itemStyle: { color: '#0f9d58', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', fontSize: 11 },
        data: provBar.map((d) => d.value)
      }
    ]
  })
}

function handleLogout() {
  localStorage.removeItem('adminToken')
  router.replace('/sys/login')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f0f4f8;
}

.admin-header {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 64px;
  padding: 0 28px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  color: #fff;
}

.logo {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #fff;
  padding: 2px;
  object-fit: cover;
}

.admin-header h1 {
  font-size: 19px;
  letter-spacing: 1px;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

/* 右上角退出登录按钮 */
.header-logout {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.header-logout:hover {
  background: #fff;
  color: #0b4f8c;
  border-color: #fff;
}

/* 图标默认反色为白，保证在蓝色标题栏上清晰可见；悬停白底时还原原色 */
.logout-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  filter: brightness(0) invert(1);
  transition: filter 0.2s ease;
}

.header-logout:hover .logout-icon {
  filter: none;
}

.admin-body {
  padding: 18px 24px 30px;
}

/* 左右分栏布局：顶部对齐，并让右列高度跟随左列，底部齐平 */
.admin-container {
  display: flex;
  gap: 18px;
  align-items: stretch;
  flex-wrap: wrap;
}

.admin-left {
  flex: 1.8;
  min-width: 680px;
  max-width: 100%;
}

.admin-right {
  flex: 1;
  min-width: 360px;
  max-width: 100%;
  display: flex;
  flex-direction: column;
}

/* 左侧 CRUD */
.crud-card {
  border-radius: 12px;
}

.card-head {
  display: flex;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #0b4f8c;
}

.search-form {
  margin-bottom: 6px;
}

.search-actions {
  margin-left: auto;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 右侧统计图表 */
.chart-card {
  margin-top: 14px;
  border-radius: 12px;
}

/* 第一张图与左侧卡片顶部对齐 */
.admin-right > .chart-card:first-child {
  margin-top: 0;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #0b4f8c;
}

.chart {
  height: 240px;
  width: 100%;
}

.chart-lg {
  height: 260px;
}

/* 右列最后一张图撑满剩余高度，使左右两列底部齐平 */
.admin-right > .chart-card:last-child {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
}

.admin-right > .chart-card:last-child :deep(.el-card__body) {
  flex: 1 1 auto;
  display: flex;
}

.admin-right > .chart-card:last-child .chart {
  flex: 1 1 auto;
  height: auto;
  min-height: 220px;
}
</style>
