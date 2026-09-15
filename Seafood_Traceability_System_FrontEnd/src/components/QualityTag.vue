<template>
  <!--
    质量状态标签。

    质量状态是"本环节自己的检测结论"（0待检 / 1合格 / 2不合格），
    不随批号在链路上继承——养殖环节合格不代表零售环节也合格，
    因此每一级都要各自展示自己的那一档。
    取值与配色都取自 utils/nodeType，避免和别处的写法漂移。
  -->
  <el-tag :type="tagType" :size="size" :effect="effect">{{ text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { qualityTagType, qualityText } from '../utils/nodeType'

const props = defineProps({
  /** 质量状态：0待检 / 1合格 / 2不合格 */
  status: { type: [Number, String], default: null },
  /** el-tag 尺寸 */
  size: { type: String, default: 'small' },
  /** el-tag 视觉风格 */
  effect: { type: String, default: 'light' }
})

const text = computed(() => qualityText(Number(props.status)))
const tagType = computed(() => qualityTagType(Number(props.status)))
</script>
