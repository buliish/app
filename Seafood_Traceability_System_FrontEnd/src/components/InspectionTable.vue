<template>
  <!--
    检测记录列表。

    页面是手机宽度（内容区最大 540px），十来个字段塞进 el-table 会挤成一团，
    因此每条记录渲染成一张小卡片：抬头是检测项目与判定结论，
    下面是"字段名 + 值"的对齐行，与详情页的 detail-table 视觉一致。

    消费者端与管理端复用同一份数据，只是不显示"删除"与"检测人"
    （消费者不需要知道是谁检的，也不该有编辑入口）。
  -->
  <div class="insp-table">
    <el-empty
      v-if="!records || records.length === 0"
      description="暂无检测记录"
      :image-size="70"
    />

    <div v-for="(rec, index) in records" :key="rec.inspectionId || index" class="insp-card">
      <div class="insp-head">
        <span class="insp-item">{{ rec.itemName || '未命名检测项' }}</span>
        <el-tag :type="judgeTagType(rec.result)" size="small" effect="dark">
          {{ judgeText(rec.result) }}
        </el-tag>
        <button
          v-if="deletable"
          type="button"
          class="mini-btn delete"
          @click="$emit('delete', rec)"
        >
          删除
        </button>
      </div>

      <div class="insp-body">
        <div class="insp-row">
          <span class="k">检测值</span>
          <span class="v">{{ rec.itemValue || '—' }}</span>
        </div>
        <div class="insp-row">
          <span class="k">标准限值</span>
          <span class="v">{{ rec.standardValue || '—' }}</span>
        </div>
        <div class="insp-row">
          <span class="k">整批结论</span>
          <span class="v">{{ judgeText(rec.conclusion) }}</span>
        </div>
        <div class="insp-row">
          <span class="k">报告编号</span>
          <span class="v">{{ rec.reportNo || '—' }}</span>
        </div>
        <div class="insp-row">
          <span class="k">检测机构</span>
          <span class="v">{{ rec.orgName || '—' }}</span>
        </div>
        <div v-if="showInspector" class="insp-row">
          <span class="k">检测人</span>
          <span class="v">{{ rec.inspector || '—' }}</span>
        </div>
        <div class="insp-row">
          <span class="k">检测日期</span>
          <span class="v">{{ formatDate(rec.inspectDate) }}</span>
        </div>
        <div v-if="rec.remark" class="insp-row">
          <span class="k">备注</span>
          <span class="v">{{ rec.remark }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { JUDGE_RESULTS } from '../utils/nodeType'

defineProps({
  /** 检测记录列表（后端 inspection_record 的 JSON 结构） */
  records: { type: Array, default: () => [] },
  /** 是否显示检测人（管理端/节点端显示，消费者端隐藏） */
  showInspector: { type: Boolean, default: true },
  /** 是否显示删除按钮（已下架批号或消费者端为 false） */
  deletable: { type: Boolean, default: false }
})

defineEmits(['delete'])

function judgeText(value) {
  return JUDGE_RESULTS[Number(value)] || '未判定'
}

function judgeTagType(value) {
  return Number(value) === 2 ? 'danger' : Number(value) === 1 ? 'success' : 'info'
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '—'
}
</script>

<style scoped>
.insp-card {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fff;
  margin-bottom: 10px;
  overflow: hidden;
}

.insp-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  background: #f7f9fc;
  border-bottom: 1px solid var(--border-light);
}

.insp-item {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}

.mini-btn {
  border-radius: 5px;
  padding: 3px 10px;
  font-size: 12px;
  background: #fff;
  cursor: pointer;
}

.mini-btn.delete {
  border: 1px solid #f56c6c;
  color: #f56c6c;
}

.insp-body {
  padding: 6px 0;
}

.insp-row {
  display: flex;
  font-size: 12px;
  line-height: 1.9;
}

.insp-row .k {
  width: 84px;
  flex-shrink: 0;
  padding-left: 12px;
  color: var(--text-muted);
}

.insp-row .v {
  flex: 1;
  padding-right: 12px;
  color: var(--text-main);
  word-break: break-all;
}
</style>
