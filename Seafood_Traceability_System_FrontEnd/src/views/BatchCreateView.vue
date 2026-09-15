<template>
  <!--
    新建 / 更新产品批号界面（参考原型图设计）
    2.4.2 养殖企业：产品批号、产品品种、动物检验检疫合格证、官方检疫员名称 + 新建按钮；
         产品批号输入框 blur 时校验批号是否已存在。
    3.2.4~3.2.6 冷冻加工企业/批发商/零售商：先录入上游企业进场信息，再录入本企业批号信息。
  -->
  <div class="page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="page-header">
      <img :src="seafoodLogo" alt="logo" class="header-logo" />
      <h1 class="header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="batch-form" @submit.prevent>
        <!-- ============ 上游企业进场信息（加工企业/批发商/零售商） ============ -->
        <template v-if="nodeType !== 1">
          <el-divider content-position="left">本批号进场信息</el-divider>

          <el-form-item :label="upstreamRegionLabel" prop="provId">
            <div class="region-row">
              <el-select v-model="form.provId" placeholder="请选择省" class="double" @change="onProvChange">
                <el-option v-for="p in provinces" :key="p.provId" :label="p.provName" :value="p.provId" />
              </el-select>
              <el-select v-model="form.cityId" placeholder="请选择市" class="double" @change="onCityChange">
                <el-option v-for="c in cities" :key="c.cityId" :label="c.cityName" :value="c.cityId" />
              </el-select>
            </div>
          </el-form-item>

          <el-form-item :label="upstreamName + '名称'" prop="upNodeId">
            <el-select v-model="form.upNodeId" placeholder="请先选择省、市" @change="onUpNodeChange">
              <el-option v-for="n in upNodes" :key="n.nodeId" :label="n.name" :value="n.nodeId" />
            </el-select>
          </el-form-item>

          <el-form-item :label="upstreamName + '产品批号'" prop="upBatchNo">
            <el-select v-model="form.upBatchNo" placeholder="请先选择上游企业" @change="onUpBatchChange">
              <el-option
                v-for="b in upBatches"
                :key="b.batchNo"
                :label="`${b.batchNo}（${b.breed || ''}${b.productType ? ' / ' + b.productType : ''}）`"
                :value="b.batchNo"
              />
            </el-select>
          </el-form-item>

        </template>

        <!-- ============ 本企业产品批号信息 ============ -->
        <el-divider content-position="left">本企业产品批号信息</el-divider>

        <!-- 产品批号：新建时可录入并 blur 校验是否已存在；更新时禁用（事件表：产品批号-禁用） -->
        <el-form-item label="产品批号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="请输入产品批号" :disabled="isUpdate" @blur="checkBatchNo" />
        </el-form-item>

        <!-- 产品品种（所有角色通用；上游环节选择批号后自动带出，可手工修正） -->
        <el-form-item label="产品品种" prop="breed">
          <el-input
            v-model="form.breed"
            :placeholder="nodeType === 1 ? '请输入产品品种' : '选择上游批号后自动带出，可手工修正'"
          />
        </el-form-item>

        <!-- 养殖企业专属：养殖阶段（虾苗 / 成虾） -->
        <el-form-item v-if="nodeType === 1" label="养殖阶段" prop="breedStage">
          <el-select v-model="form.breedStage" placeholder="请选择养殖阶段">
            <el-option v-for="s in BREED_STAGE" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>

        <!-- 加工企业专属：产品类型 -->
        <el-form-item v-if="nodeType === 2" label="产品类型" prop="productType">
          <el-select v-model="form.productType" placeholder="请选择产品类型">
            <el-option v-for="t in PRODUCT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <!-- 批发商 / 零售商产品类型以上游环节信息为准 -->
        <el-form-item v-if="nodeType === 3 || nodeType === 4" label="产品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="选择上游批号后自动带出，可手工修正" />
        </el-form-item>

        <!-- 合格证：养殖为动物检验检疫合格证，加工为产品检验检疫合格证 -->
        <el-form-item
          v-if="nodeType === 1 || nodeType === 2"
          :label="nodeType === 1 ? '动物检验检疫合格证' : '产品检验检疫合格证'"
          prop="quarantineNo"
        >
          <el-input v-model="form.quarantineNo" placeholder="请输入检验检疫合格证编号" />
        </el-form-item>

        <el-form-item
          v-if="nodeType === 1 || nodeType === 2"
          :label="nodeType === 1 ? '官方检疫员名称' : '官方检验员名称'"
          prop="inspector"
        >
          <el-input v-model="form.inspector" placeholder="请输入官方检验（检疫）员姓名" />
        </el-form-item>

        <!-- 状态选择：仅在更新时展示（新建保存后为"待发布/新建"状态，在批号管理中再发布） -->
        <el-form-item v-if="isUpdate" :label="nodeType === 1 ? '是否发布' : '发送确认请求'">
          <el-checkbox v-model="publishNow">
            {{ nodeType === 1 ? '保存并发布（发布后下游企业才能看到本批号）' : '向上一环节企业发送进场确认请求' }}
          </el-checkbox>
        </el-form-item>

        <el-button class="wide-btn" type="primary" size="large" :loading="loading" @click="handleSubmit">
          {{ isUpdate ? '更 新' : '新 建' }}
        </el-button>
      </el-form>
    </main>

    <BottomNav active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import {
  batchDetailApi,
  batchSaveApi,
  batchUpdateApi,
  batchCheckApi
} from '../api/batch'
import { provincesApi, citiesApi, nodesApi, upBatchesApi } from '../api/region'
import {
  BREED_STAGE,
  PRODUCT_TYPES,
  TYPE_NAME,
  UPSTREAM_NAME,
  UPSTREAM_REGION_LABEL,
  UPSTREAM_TYPE,
  getLoginNode
} from '../utils/nodeType'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const publishNow = ref(false)

const node = getLoginNode()
const nodeType = node.nodeType || 1
const isUpdate = computed(() => !!route.params.id)

//主键字段名按角色区分
const ID_KEY = { 1: 'farmBatchId', 2: 'frozBatchId', 3: 'wholBatchId', 4: 'retaBatchId' }

const typeName = computed(() => TYPE_NAME[nodeType])
const upstreamType = computed(() => UPSTREAM_TYPE[nodeType])
const upstreamName = computed(() => UPSTREAM_NAME[upstreamType.value] || '上游企业')
const upstreamRegionLabel = computed(() => UPSTREAM_REGION_LABEL[nodeType] || '上游企业所在区域')

const provinces = ref([])
const cities = ref([])
const upNodes = ref([])
const upBatches = ref([])

const form = reactive({
  batchNo: '',
  breed: '',
  breedStage: '',
  quarantineNo: '',
  inspector: '',
  productType: '',
  provId: null,
  cityId: null,
  upNodeId: null,
  upBatchNo: ''
})

const rules = {
  batchNo: [{ required: true, message: '请输入产品批号', trigger: 'blur' }],
  breed: [{ required: true, message: '请输入产品品种', trigger: 'blur' }],
  provId: [{ required: true, message: '请选择省', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择市', trigger: 'change' }],
  upNodeId: [{ required: true, message: '请选择上游企业', trigger: 'change' }],
  upBatchNo: [{ required: true, message: '请选择上游产品批号', trigger: 'change' }]
}

onMounted(async () => {
  //加载省列表（本企业所在省份默认选中，便于快速定位上游企业）
  const res = await provincesApi()
  provinces.value = res.data || []
  if (nodeType !== 1 && node.provId) {
    form.provId = node.provId
    onProvChange(node.provId)
  }
  if (isUpdate.value) {
    const detail = await batchDetailApi(nodeType, route.params.id)
    const d = detail.data || {}
    Object.keys(form).forEach((k) => {
      if (d[k] !== undefined && d[k] !== null) form[k] = d[k]
    })
    if (form.provId) {
      await onProvChange(form.provId)
      form.cityId = d.cityId
      await onCityChange(form.cityId)
      form.upNodeId = d.upNodeId
      await onUpNodeChange(d.upNodeId, false)
      form.upBatchNo = d.upBatchNo
    }
    publishNow.value = d.status === 2
  }
})

async function onProvChange(provId) {
  form.cityId = null
  form.upNodeId = null
  form.upBatchNo = ''
  upNodes.value = []
  upBatches.value = []
  if (!provId) return
  const res = await citiesApi(provId)
  cities.value = res.data || []
}

async function onCityChange(cityId) {
  form.upNodeId = null
  form.upBatchNo = ''
  upBatches.value = []
  if (!cityId) return
  const res = await nodesApi(upstreamType.value, form.provId, cityId)
  upNodes.value = res.data || []
}

async function onUpNodeChange(nodeId, clearBatch = true) {
  if (clearBatch) {
    form.upBatchNo = ''
  }
  upBatches.value = []
  if (!nodeId) return
  const res = await upBatchesApi(nodeId)
  upBatches.value = res.data || []
}

//选择上游批号后自动带出品种与产品类型
function onUpBatchChange(batchNo) {
  const target = upBatches.value.find((b) => b.batchNo === batchNo)
  if (!target) return
  if (target.breed) form.breed = target.breed
  if (target.productType) form.productType = target.productType
}

//批号唯一性校验（blur 事件，仅新建时）：已存在则提示重新输入；更新时批号禁用不校验
async function checkBatchNo() {
  if (!form.batchNo || isUpdate.value) return
  try {
    const res = await batchCheckApi(nodeType, form.batchNo)
    if (res.data) {
      ElMessage.warning('该产品批号已存在，请重新输入！')
    }
  } catch (e) {
    /* 校验接口异常时由提交时的后端校验兜底 */
  }
}

async function handleSubmit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = { ...form }
      if (nodeType === 1) {
        delete payload.provId
        delete payload.cityId
        delete payload.upNodeId
        delete payload.upBatchNo
        delete payload.productType
        // 注意：养殖阶段（breedStage）是养殖企业专属字段，必须保留提交，
        // 此前这里被误删导致该字段永远存不进库
      } else {
        delete payload.breedStage
        if (nodeType !== 2) delete payload.quarantineNo
        if (nodeType !== 2) delete payload.inspector
      }
      if (isUpdate.value) {
        payload[ID_KEY[nodeType]] = Number(route.params.id)
        await batchUpdateApi(nodeType, payload, publishNow.value)
        ElMessage.success('产品批号更新成功！')
      } else {
        await batchSaveApi(nodeType, payload)
        ElMessage.success('产品批号新建成功！')
      }
      router.push('/batch/list')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏（与功能菜单页一致） */
.page-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  border-bottom: 1px solid #0b4f8c;
}

.header-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 6px;
  background-color: #fff;
  padding: 2px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

/* 主体可滚动区 */
.body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 540px;
  width: 100%;
  margin: 0 auto;
}

/* banner */
.banner-wrap {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(11, 79, 140, 0.08);
}

.banner-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

/* 表单：标签在输入框上方 */
.batch-form {
  margin-top: 20px;
}

.batch-form :deep(.el-form-item__label) {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  padding-bottom: 4px;
}

.region-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.region-row .double {
  flex: 1;
}

/* 全宽新建/更新按钮 */
.wide-btn {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  font-size: 16px;
  letter-spacing: 6px;
  border-radius: 8px;
}
</style>
