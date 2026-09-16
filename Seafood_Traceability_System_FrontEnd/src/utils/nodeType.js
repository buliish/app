/**
 * 产业链角色与批号状态通用映射
 * 产业链：养殖企业(1) -> 冷冻加工企业(2) -> 批发商(3) -> 零售商(4)
 */

// 企业类型中文名
export const TYPE_NAME = {
  1: '养殖企业',
  2: '冷冻加工企业',
  3: '批发商',
  4: '零售商'
}

// 企业类型标签配色（与管理端统计图表「类型分布」饼图的配色一一对应）
export const TYPE_COLOR = {
  1: '#0f9d58', // 养殖企业：绿
  2: '#e8a33d', // 冷冻加工企业：橙
  3: '#1d6fb8', // 批发商：蓝
  4: '#8e44ad' // 零售商：紫
}

// 生成企业类型 el-tag 的内联样式，使四种企业类型各有专属颜色
export function typeTagStyle(nodeType) {
  const color = TYPE_COLOR[Number(nodeType)] || '#909399'
  return { backgroundColor: color, borderColor: color, color: '#fff' }
}

// 企业类型对应的后端接口前缀
export const TYPE_PREFIX = {
  1: 'farm',
  2: 'froz',
  3: 'whol',
  4: 'reta'
}

// 各角色的上游企业类型（加工企业的上游是养殖企业，以此类推）
export const UPSTREAM_TYPE = { 2: 1, 3: 2, 4: 3 }

// 上游企业类型中文名
export const UPSTREAM_NAME = { 1: '养殖企业', 2: '冷冻加工企业', 3: '批发商' }

// 上游区域标签
export const UPSTREAM_REGION_LABEL = {
  2: '本批号进场信息：养殖企业所在区域',
  3: '本批号进场信息：冷冻加工企业所在区域',
  4: '本批号进场信息：批发商所在区域'
}

// 批号生产阶段（仅养殖企业）
export const BREED_STAGE = ['虾苗', '成虾']

// 加工工序步骤（仅冷冻加工企业）
export const PROCESS_STEPS = ['清洗', '分级', '冷冻', '包装']

// 产品类型（加工/批发/零售）
export const PRODUCT_TYPES = ['冷冻整虾', '冷冻虾仁', '冰鲜整虾', '虾滑', '虾饺', '虾丸']

// 来源方式（仅养殖企业）：溯源链最源头是养殖地还是打捞地
export const SOURCE_TYPES = ['人工养殖', '海洋捕捞']

// 产品形态（加工环节定型，下游继承）
export const PRODUCT_FORMS = ['鲜虾', '冻虾']

// 规格等级（加工环节定型，下游继承）
export const SPEC_GRADES = ['30-40只/斤', '40-50只/斤', '50-60只/斤', '60-70只/斤']

// 本环节质量状态（各环节独立填写，不继承）
export const QUALITY_STATUS = { 0: '待检', 1: '合格', 2: '不合格' }

export function qualityText(status) {
  return QUALITY_STATUS[status] || '待检'
}

// 质量状态标签颜色，与 el-tag 的 type 对应
export function qualityTagType(status) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[status] || 'info'
}

// 检测项目候选（检测记录录入用）
export const INSPECT_ITEMS = ['感官', '菌落总数', '大肠菌群', '氯霉素', '重金属镉', '水分']

// 注意：不往数据库存图片路径。前端源码路径（/src/assets/...）在开发模式由
// Vite 处理、构建后又会变成 /assets/xxx-hash.png，存进库里必然有一边失效。
// 因此 image_url 留空，由组件按 import 结果兜底选图。

// 单项判定 / 整批结论
export const JUDGE_RESULTS = { 1: '合格', 2: '不合格' }

// 批号状态：养殖企业为 1待发布/2已发布/3已下架，其余角色为 1新建/2待确认/3已确认/4已下架
const FARM_STATUS = { 1: '待发布', 2: '已发布', 3: '已下架' }
const BATCH_STATUS = { 1: '新建', 2: '待确认', 3: '已确认', 4: '已下架' }

export function statusText(nodeType, status) {
  const map = Number(nodeType) === 1 ? FARM_STATUS : BATCH_STATUS
  return map[status] || '未知'
}

// 列表页可选状态项（已下架不可浏览）
export function statusTabs(nodeType) {
  if (Number(nodeType) === 1) {
    return [
      { label: '待发布', value: 1 },
      { label: '已发布', value: 2 }
    ]
  }
  return [
    { label: '新建', value: 1 },
    { label: '待确认', value: 2 },
    { label: '已确认', value: 3 }
  ]
}

// 状态标签颜色
export function statusTagType(status) {
  return { 1: 'info', 2: 'warning', 3: 'success', 4: 'danger' }[status] || 'info'
}

// 读取本地登录企业信息
export function getLoginNode() {
  return JSON.parse(localStorage.getItem('nodeInfo') || 'null') || {}
}

// 当前登录企业类型对应的接口前缀
export function prefixOf(nodeType) {
  return TYPE_PREFIX[nodeType] || 'farm'
}
