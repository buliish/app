<template>
  <!--
    流通节点端通用顶部标题栏（3.2.2）
    左侧：平台 Logo + 平台名称
    右侧：当前登录企业图标、企业名称、企业类型标签
  -->
  <header class="page-header">
    <div class="header-left">
      <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="header-logo" />
      <h1 class="header-title">冷冻对虾全产业链溯源系统</h1>
    </div>

    <div class="header-right" v-if="node && node.code">
      <img :src="userHighlightIcon" alt="当前企业" class="header-avatar" />
      <div class="header-info">
        <div class="header-company">{{ node.name || node.code }}</div>
        <el-tag v-if="typeName" size="small" type="warning" effect="dark">{{ typeName }}</el-tag>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import seafoodLogo from '../assets/images/海鲜.png'
import userHighlightIcon from '../assets/images/用户(高亮).png'
import { TYPE_NAME } from '../utils/nodeType'

const props = defineProps({
  // 当前登录企业信息
  node: { type: Object, default: () => ({}) }
})

const typeName = computed(() => TYPE_NAME[props.node.nodeType] || '')
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: linear-gradient(90deg, #0b4f8c 0%, #1d6fb8 100%);
  color: #fff;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-logo {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background-color: #fff;
  padding: 2px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #fff;
  padding: 3px;
  object-fit: contain;
}

.header-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.header-company {
  font-size: 14px;
  font-weight: 500;
}
</style>
