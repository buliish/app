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
        <button type="button" class="header-dash" @click="router.push('/sys/trace')">
          <span>批次追溯查询</span>
        </button>
        <button type="button" class="header-dash" @click="router.push('/sys/dashboard')">
          <span>可视化大屏</span>
        </button>
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
              layout="total, sizes, prev, pager, next"
              :total="total"
              :current-page="query.current"
              :page-size="query.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onPageChange"
              @size-change="onSizeChange"
            />
          </el-card>
        </div>

        <!-- 右侧：统计图表（抽为 StatsPanel 组件，与独立大屏共用同一套逻辑） -->
        <div class="admin-right">
          <StatsPanel ref="statsPanelRef" theme="light" chart-height="200px" />
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

        <!--
          行业许可证：按企业类型条件展示，与库表注释一致
          （ep_id 养殖必填 / eia_id 养殖+加工 / cir_id 批发 / fb_id 批发+零售）
        -->
        <el-form-item v-if="showCert.ep" label="动物防疫条件合格证" prop="epId">
          <el-input v-model="form.epId" placeholder="请输入动物防疫条件合格证编号" />
        </el-form-item>
        <el-form-item v-if="showCert.eia" label="环评资质证书" prop="eiaId">
          <el-input v-model="form.eiaId" placeholder="请输入环境影响评价资质证书编号" />
        </el-form-item>
        <el-form-item v-if="showCert.cir" label="食品流通许可证" prop="cirId">
          <el-input v-model="form.cirId" placeholder="请输入食品流通许可证编号" />
        </el-form-item>
        <el-form-item v-if="showCert.fb" label="食品经营许可证" prop="fbId">
          <el-input v-model="form.fbId" placeholder="请输入食品经营许可证编号" />
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
import { onMounted, reactive, ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminNodeDeleteApi,
  adminNodeDetailApi,
  adminNodePageApi,
  adminNodeSaveApi,
  adminNodeUpdateApi
} from '../../api/admin'
import { citiesApi, provincesApi } from '../../api/region'
import StatsPanel from '../../components/StatsPanel.vue'
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
  epId: '',
  eiaId: '',
  cirId: '',
  fbId: '',
  corporation: '',
  telephone: '',
  regDate: ''
})

// 各证照按企业类型显隐（见库表 node_info 字段注释）
const showCert = computed(() => {
  const t = form.nodeType
  return {
    ep: t === 1,
    eia: t === 1 || t === 2,
    cir: t === 3,
    fb: t === 3 || t === 4
  }
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

const statsPanelRef = ref()

onMounted(async () => {
  // 需求 3.2.8.1：初始化时同时填充"省下拉列表"与"市下拉列表"，且都不设初始选中项。
  // 市列表一次性取全部（不带 provId），这样用户不选省也能直接按市模糊查询。
  try {
    const [provRes, cityRes] = await Promise.all([provincesApi(), citiesApi()])
    provinces.value = provRes.data || []
    cities.value = cityRes.data || []
  } catch (e) {
    provinces.value = []
    cities.value = []
  }
  loadList()
  // 图表加载、resize 监听与销毁由 StatsPanel 组件自行负责
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

async function resetQuery() {
  Object.assign(query, { current: 1, name: '', type: null, provId: null, cityId: null })
  // 重置后市下拉要回到"全部市"，而不是留空
  const res = await citiesApi()
  cities.value = res.data || []
  loadList()
}

//选中省后把市下拉收窄到该省；清空省则恢复为全部市（初始化时已加载，无需再请求）
async function onProvChange(provId) {
  query.cityId = null
  if (!provId) {
    const res = await citiesApi()
    cities.value = res.data || []
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
      epId: '',
      eiaId: '',
      cirId: '',
      fbId: '',
      corporation: '',
      telephone: '',
      regDate: new Date().toISOString().slice(0, 10)
    })
  } else {
    // 详情/编辑都从后端取最新数据（需求 3.2.8.1：详情按钮应"查询当前节点企业信息"）
    let d = row
    try {
      const res = await adminNodeDetailApi(row.nodeId)
      if (res.data) d = res.data
    } catch (e) {
      // 详情接口异常时退回表格行数据，保证对话框仍可用
    }
    Object.assign(form, {
      nodeId: d.nodeId,
      code: d.code,
      password: '',
      name: d.name,
      nodeType: d.nodeType,
      provId: d.provId,
      cityId: d.cityId,
      address: d.address,
      businessId: d.businessId,
      epId: d.epId || '',
      eiaId: d.eiaId || '',
      cirId: d.cirId || '',
      fbId: d.fbId || '',
      corporation: d.corporation,
      telephone: d.telephone,
      regDate: d.regDate
    })
    if (d.provId) {
      const res = await citiesApi(d.provId)
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
      statsPanelRef.value && statsPanelRef.value.refresh()
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
      statsPanelRef.value && statsPanelRef.value.refresh()
    })
    .catch(() => {})
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

/* 进入可视化大屏的入口按钮 */
.header-dash {
  display: flex;
  align-items: center;
  padding: 6px 16px;
  background: #3adc9a;
  border: 1px solid #3adc9a;
  border-radius: 6px;
  color: #06281a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.header-dash:hover {
  background: #2cc487;
  border-color: #2cc487;
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

/*
 * 右侧统计图表（.chart-card / .chart 等）的样式已随图表逻辑一并移入
 * StatsPanel.vue，这里不再保留，避免出现指向已不存在元素的选择器。
 */
</style>
