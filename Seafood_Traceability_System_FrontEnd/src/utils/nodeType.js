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

// 产品类型（加工/批发/零售）
export const PRODUCT_TYPES = ['冷冻整虾', '冷冻虾仁', '虾滑', '虾饺', '虾丸']

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

// 当前登录企业类型与接口前缀
export function currentType() {
  return getLoginNode().nodeType || 1
}

export function prefixOf(nodeType) {
  return TYPE_PREFIX[nodeType] || 'farm'
}
