<template>
  <!--
    质量状态徽标：0 待检 / 1 合格 / 2 不合格
    消费者端、管理端追溯页、节点端批号详情统一用它，避免各处重复写映射。
  -->
  <el-tag :type="tagType" :size="size" :effect="effect">
    {{ text }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { qualityTagType, qualityText } from '../utils/nodeType'

const props = defineProps({
  /** 质量状态：0 待检 / 1 合格 / 2 不合格 */
  status: { type: [Number, String], default: 0 },
  size: { type: String, default: 'small' },
  effect: { type: String, default: 'light' }
})

const tagType = computed(() => qualityTagType(Number(props.status)))
const text = computed(() => qualityText(Number(props.status)))
</script>
