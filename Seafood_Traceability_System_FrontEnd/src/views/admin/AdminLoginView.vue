<template>
  <!--
    系统管理端登录界面（3.2.8）
    管理员登录后可进入节点企业注册信息管理与注册信息统计大屏。
  -->
  <div class="admin-login">
    <el-card class="login-card" shadow="always">
      <div class="login-title">
        <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="logo" />
        <h2>冷冻对虾全产业链溯源系统</h2>
        <p>系统管理端</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleLogin">
        <el-form-item label="管理员账号" prop="adminName">
          <el-input v-model="form.adminName" placeholder="请输入管理员账号" clearable />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入登录密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="center">演示账号：admin / 123456</el-divider>
      <div class="link-row">
        <el-link type="info" @click="router.push('/login')">返回流通节点端登录</el-link>
        <el-divider direction="vertical" />
        <el-link type="primary" @click="router.push('/trace')">消费者溯源查询</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLoginApi } from '../../api/admin'
import seafoodLogo from '../../assets/images/海鲜.png'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({ adminName: '', password: '' })

const rules = {
  adminName: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await adminLoginApi({ adminName: form.adminName, password: form.password })
      localStorage.setItem('adminToken', res.data || '')
      ElMessage.success('登录成功！')
      router.push('/sys/nodes')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.admin-login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0b4f8c 0%, #1d6fb8 60%, #2b8cd6 100%);
}

.login-card {
  width: 440px;
  border-radius: 14px;
  padding: 8px 12px 4px;
}

.login-title {
  text-align: center;
  margin-bottom: 18px;
}

.logo {
  width: 68px;
  height: 68px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(11, 79, 140, 0.25);
}

.login-title h2 {
  margin-top: 12px;
  font-size: 20px;
  color: #0b4f8c;
}

.login-title p {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
  letter-spacing: 2px;
}

.login-btn {
  width: 100%;
}

.link-row {
  text-align: center;
}
</style>
