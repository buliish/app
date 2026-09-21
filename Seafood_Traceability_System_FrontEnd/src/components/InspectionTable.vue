<template>
  <!--
    检测记录表（按报告号分组展示）
    消费者端商品详情、管理端追溯页、节点端批号详情三处共用。
    一条记录 = 一份报告的一个检测项，因此按 reportNo 分组更贴近真实检测报告的样子。
  -->
  <div class="inspection-wrap">
    <el-empty v-if="!records || records.length === 0" description="暂无检测记录" :image-size="70" />

    <div v-for="group in grouped" :key="group.key" class="report-block">
      <!-- 报告抬头：报告号 + 机构 + 整批结论 -->
      <div class="report-head">
        <span class="report-no">{{ group.reportNo || '未标注报告号' }}</span>
        <span v-if="group.orgName" class="report-org">{{ group.orgName }}</span>
        <QualityTag v-if="group.conclusion != null" :status="group.conclusion" />
      </div>

      <table class="insp-table">
        <thead>
          <tr>
            <th v-if="showStage">环节</th>
            <th>检测项目</th>
            <th>检测值</th>
            <th>标准限值</th>
            <th>判定</th>
            <th>检测日期</th>
            <th v-if="showInspector">检测人</th>
            <th v-if="deletable">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in group.items" :key="row.inspectionId">
            <td v-if="showStage">{{ stageName(row.stageType) }}</td>
            <td>{{ row.itemName }}</td>
            <td>{{ row.itemValue || '—' }}</td>
            <td>{{ row.standardValue || '—' }}</td>
            <td>
              <el-tag :type="row.result === 2 ? 'danger' : 'success'" size="small" effect="plain">
                {{ row.result === 2 ? '不合格' : '合格' }}
              </el-tag>
            </td>
            <td>{{ row.inspectDate || '—' }}</td>
            <td v-if="showInspector">{{ row.inspector || '—' }}</td>
            <td v-if="deletable">
              <el-button link type="danger" size="small" @click="$emit('delete', row)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import QualityTag from './QualityTag.vue'

const props = defineProps({
  /** 检测记录列表（同一批号或整条链路的都可） */
  records: { type: Array, default: () => [] },
  /** 是否展示"环节"列（整条链路查看时才有意义） */
  showStage: { type: Boolean, default: false },
  /** 是否展示"检测人"列（消费者端隐藏，减少无关信息） */
  showInspector: { type: Boolean, default: true },
  /** 是否展示"操作"列（仅节点端自己的批号可维护） */
  deletable: { type: Boolean, default: false }
})

defineEmits(['delete'])

const STAGE_NAME = { 1: '养殖', 2: '冷冻加工', 3: '批发', 4: '零售' }
function stageName(t) {
  return STAGE_NAME[t] || '—'
}

/**
 * 按 环节+报告号 分组：跨环节查看时，不同环节可能恰好用到同一个报告号，
 * 只用报告号分组会把它们错误地合并成一份报告。
 */
const grouped = computed(() => {
  const map = new Map()
  for (const r of props.records || []) {
    const key = `${r.stageType ?? ''}-${r.reportNo ?? ''}`
    if (!map.has(key)) {
      map.set(key, { key, reportNo: r.reportNo, orgName: r.orgName, conclusion: r.conclusion, items: [] })
    }
    const g = map.get(key)
    g.items.push(r)
    // 同一份报告里只要有结论就取用（后端写入时同报告的 conclusion 一致）
    if (g.conclusion == null && r.conclusion != null) g.conclusion = r.conclusion
  }
  return [...map.values()]
})
</script>

<style scoped>
.report-block {
  margin-bottom: 14px;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  overflow: hidden;
}

.report-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f2f7fc;
  font-size: 13px;
}

.report-no {
  font-weight: 600;
  color: #0b4f8c;
}

.report-org {
  color: #606266;
  font-size: 12px;
  margin-right: auto;
}

.report-head :deep(.el-tag) {
  margin-left: auto;
}

.insp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.insp-table th,
.insp-table td {
  padding: 8px 12px;
  text-align: left;
  border-top: 1px solid #eef2f7;
}

.insp-table th {
  color: #909399;
  font-weight: 500;
  background: #fafcfe;
}

.insp-table tbody tr:hover {
  background: #f7fbff;
}
</style>
