<template>
  <!--
    更新密码页 —— 参考原型图设计：
    顶部标题栏、banner 大图、纵向表单（旧密码/新密码/再次输入新密码）、
    更新密码与退出登录两个全宽按钮、底部导航（更新密码高亮）。
    应用场景：养殖企业、屠宰（冷冻加工）企业、批发商、零售商均可自行更新密码。
  -->
  <div class="updatepwd-page">
    <!-- 顶部标题栏：Logo + 系统名称居中 -->
    <header class="app-header">
      <img :src="seafoodLogo" alt="logo" class="app-header-logo" />
      <h1 class="app-header-title">冷冻对虾全产业链溯源系统</h1>
    </header>

    <main class="updatepwd-body">
      <!-- 横幅大图 -->
      <div class="banner-wrap">
        <img :src="bannerBg" alt="海鲜产品溯源" class="banner-img" />
      </div>

      <!-- 更新密码表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="pwd-form"
        @submit.prevent
      >
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="form.oldPwd" type="password" show-password placeholder="请输入旧密码" size="large" />
        </el-form-item>

        <el-form-item label="新密码" prop="newPwd">
          <el-input
            v-model="form.newPwd"
            type="password"
            show-password
            placeholder="请输入新密码（至少6位）"
            size="large"
          />
        </el-form-item>

        <el-form-item label="再次输入新密码" prop="confirmPwd">
          <el-input
            v-model="form.confirmPwd"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            size="large"
          />
        </el-form-item>

        <el-button
          class="wide-btn submit-btn"
          type="primary"
          size="large"
          :loading="loading"
          @click="handleSubmit"
        >
          更新密码
        </el-button>

        <el-button class="wide-btn logout-btn" type="primary" size="large" @click="handleLogout">
          退出登录
        </el-button>
      </el-form>
    </main>

    <!-- 底部导航：更新密码为当前高亮项 -->
    <BottomNav active="pwd" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BottomNav from '../components/BottomNav.vue'
import seafoodLogo from '../assets/images/海鲜.png'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import { updatePwdApi, logoutApi } from '../api/node'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

// 当前登录企业信息（登录时已存入本地）；未登录则回登录页
const node = JSON.parse(localStorage.getItem('nodeInfo') || 'null')
if (!node) {
  router.replace('/login')
}

const form = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

// 自定义校验：确认密码需与新密码一致
const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPwd) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPwd: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPwd: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

// 提交更新密码
function handleSubmit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await updatePwdApi({
        oldPwd: form.oldPwd,
        newPwd: form.newPwd,
        rePwd: form.confirmPwd
      })
      ElMessage.success('密码更新成功，下次登录请使用新密码！')
      router.push('/menu')
    } finally {
      loading.value = false
    }
  })
}

// 退出登录
async function handleLogout() {
  try {
    await logoutApi()
  } finally {
    localStorage.removeItem('nodeInfo')
    ElMessage.success('已退出登录！')
    router.replace('/login')
  }
}
</script>

<style scoped>
.updatepwd-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
}

/* 顶部标题栏（与功能菜单页一致） */



/* 主体可滚动区 */
.updatepwd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 540px;
  width: 100%;
  margin: 0 auto;
}

/* banner */


/* 表单 */
.pwd-form {
  margin-top: 22px;
}

.pwd-form :deep(.el-form-item__label) {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  padding-bottom: 4px;
}

/* 两个全宽按钮 */
.wide-btn {
  width: 100%;
  margin: 0 0 14px;
  height: 48px;
  font-size: 16px;
  letter-spacing: 2px;
}

.submit-btn {
  margin-top: 6px;
}

.logout-btn {
  background: #3aa0dc;
  border-color: #3aa0dc;
}

.logout-btn:hover,
.logout-btn:focus {
  background: #5db2e4;
  border-color: #5db2e4;
}
</style>
