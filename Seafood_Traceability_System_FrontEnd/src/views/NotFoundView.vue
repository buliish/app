<template>
  <!-- 404 兜底页：未匹配的路由不再白屏 -->
  <div class="page">
    <header class="page-header">
      <img :src="seafoodLogo" alt="logo" class="header-logo" />
      <h1 class="header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="body">
      <div class="notfound-box">
        <div class="code">404</div>
        <p class="tip">抱歉，您访问的页面不存在</p>
        <div class="actions">
          <el-button type="primary" @click="router.push('/menu')">返回功能菜单</el-button>
          <el-button @click="router.push('/trace')">消费者溯源查询</el-button>
          <el-button text @click="router.push('/login')">返回登录</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import seafoodLogo from '../assets/images/海鲜.png'

const router = useRouter()
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f9fc;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 56px;
  background: linear-gradient(90deg, #0b4f8c, #1d6fb8);
}

.header-logo {
  width: 28px;
  height: 28px;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
}

.body {
  max-width: 540px;
  margin: 0 auto;
  padding: 80px 16px;
}

.notfound-box {
  background: #fff;
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.code {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
  color: #1d6fb8;
  letter-spacing: 4px;
}

.tip {
  margin: 16px 0 28px;
  font-size: 15px;
  color: #606266;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.actions :deep(.el-button) {
  margin-left: 0;
}
</style>
