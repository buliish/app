<template>
  <!--
    登录界面（3.2.2 登录用例）
    应用场景：养殖企业、冷冻加工企业、批发商、零售商统一在此登录，
    输入登录编码 + 登录密码后进入功能菜单首页。
    另提供消费者溯源查询与管理端入口。
  -->
  <div class="login-page">
    <!-- 左侧：品牌宣传区（使用 assets/images 中的横幅图） -->
    <div class="login-banner" :style="{ backgroundImage: `url(${bannerBg})` }">
      <div class="banner-mask"></div>
      <div class="banner-text">
        <h2>冷冻对虾全产业链溯源系统</h2>
        <p>养殖 · 加工 · 批发 · 零售 —— 一码溯源全链路</p>
      </div>
    </div>

    <!-- 右侧：登录卡片 -->
    <div class="login-panel">
      <el-card class="login-card" shadow="always">
        <div class="login-title">
          <img :src="seafoodLogo" alt="冷冻对虾全产业链溯源系统" class="login-logo" />
          <h2>冷冻对虾全产业链溯源系统</h2>
          <p>流通节点端 · 欢迎登录</p>
        </div>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="登录编码" prop="loginCode">
            <el-input v-model="loginForm.loginCode" placeholder="请输入企业登录编码" clearable>
              <template #prefix>
                <img :src="userIcon" alt="编码" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="pwd">
            <el-input v-model="loginForm.pwd" type="password" placeholder="请输入登录密码" show-password>
              <template #prefix>
                <img :src="pwdIcon" alt="密码" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 演示账号快捷填充（与数据库中的实际账号一致，密码均为 123456） -->
        <el-divider content-position="center">演示账号（密码均为 123456）</el-divider>
        <div class="demo-tips">
          <el-tag size="small" effect="plain" @click="fillDemo('farm101')">farm101 养殖企业</el-tag>
          <el-tag size="small" effect="plain" @click="fillDemo('froz001')">froz001 冷冻加工企业</el-tag>
          <el-tag size="small" effect="plain" @click="fillDemo('whol101')">whol101 批发商</el-tag>
          <el-tag size="small" effect="plain" @click="fillDemo('reta101')">reta101 零售商</el-tag>
        </div>

        <div class="other-entry">
          <el-link type="primary" @click="router.push('/')">消费者端首页</el-link>
          <el-divider direction="vertical" />
          <el-link type="info" @click="router.push('/sys/login')">管理端入口</el-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi, getNodeInfoApi } from '../api/node'
import bannerBg from '../assets/images/海鲜产品溯源.jpeg'
import seafoodLogo from '../assets/images/海鲜.png'
import userIcon from '../assets/images/用户.png'
import pwdIcon from '../assets/images/密码.png'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const loginForm = reactive({
  loginCode: '',
  pwd: ''
})

const rules = {
  loginCode: [{ required: true, message: '请输入登录编码', trigger: 'blur' }],
  pwd: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

function fillDemo(code) {
  loginForm.loginCode = code
  loginForm.pwd = '123456'
}

// 根据登录编码前缀推测企业类型（farm=养殖、froz=冷冻加工、whol=批发、reta=零售）
function guessNodeType(code) {
  const c = (code || '').toLowerCase()
  if (c.startsWith('farm')) return 1
  if (c.startsWith('froz')) return 2
  if (c.startsWith('whol')) return 3
  if (c.startsWith('reta')) return 4
  return 1
}

// 登录操作：登录成功后查询企业完整信息（编码、名称、类型）并缓存
async function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await loginApi({ code: loginForm.loginCode, password: loginForm.pwd })
      const token = res.data || ''
      let nodeInfo = {
        code: loginForm.loginCode,
        name: loginForm.loginCode,
        nodeType: guessNodeType(loginForm.loginCode),
        token
      }
      // 先写入本地：axios 请求拦截器从 localStorage 读取 token，
      // 若放到 getInfo 之后再存，本次查询会因缺少 Authorization 被后端拦截器判为 401，
      // 触发全局跳转把用户打回登录页（表现为“第一次登录总是退回登录界面”）。
      localStorage.setItem('nodeInfo', JSON.stringify(nodeInfo))
      try {
        const info = await getNodeInfoApi(loginForm.loginCode)
        if (info.data) {
          nodeInfo = {
            id: info.data.nodeId,
            code: info.data.code,
            name: info.data.name || info.data.code,
            nodeType: info.data.nodeType || nodeInfo.nodeType,
            token
          }
          localStorage.setItem('nodeInfo', JSON.stringify(nodeInfo))
        }
      } catch (e) {
        /* 查询失败时沿用编码前缀推测结果 */
      }
      ElMessage.success('登录成功！')
      router.push('/menu')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.login-banner {
  flex: 1;
  position: relative;
  background-size: contain;          
  background-repeat: no-repeat;
  background-position: center;
  background-color: #7eadea;
}

.banner-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(11, 79, 140, 0.35), rgba(29, 111, 184, 0.15));
}

.banner-text {
  position: absolute;
  left: 48px;
  bottom: 56px;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.banner-text h2 {
  font-size: 32px;
  letter-spacing: 3px;
}

.banner-text p {
  margin-top: 12px;
  font-size: 15px;
  letter-spacing: 2px;
}

.login-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 24px;
  overflow: auto;
}

.login-card {
  width: 100%;
  border-radius: 12px;
  padding: 6px 8px 8px;
}

.login-title {
  text-align: center;
  margin-bottom: 16px;
}

.login-logo {
  width: 72px;
  height: 72px;
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
}

.input-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.login-btn {
  width: 100%;
}

.demo-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.demo-tips .el-tag {
  cursor: pointer;
}

.other-entry {
  margin-top: 16px;
  text-align: center;
}

@media (max-width: 860px) {
  .login-banner {
    display: none;
  }
  .login-panel {
    width: 100%;
  }
}
</style>
