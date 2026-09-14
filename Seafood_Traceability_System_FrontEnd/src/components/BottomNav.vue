<template>
  <!--
    流通节点端通用底部导航栏（首页 / 我的 / 更新密码）
  -->
  <nav class="bottom-nav">
    <router-link to="/menu" class="nav-item" :class="{ active: active === 'home' }">
      <img class="nav-icon" :src="active === 'home' ? homeActiveIcon : homeIcon" alt="首页" />
      <span class="nav-label">首页</span>
    </router-link>

    <router-link to="/mine" class="nav-item" :class="{ active: active === 'mine' }">
      <img class="nav-icon" :src="active === 'mine' ? mineActiveIcon : mineIcon" alt="我的" />
      <span class="nav-label">我的</span>
    </router-link>

    <router-link to="/updatePwd" class="nav-item" :class="{ active: active === 'pwd' }">
      <div class="nav-icon-wrap">
        <img class="nav-icon" :src="active === 'pwd' ? pwdActiveIcon : pwdIcon" alt="更新密码" />
      </div>
      <span class="nav-label">更新密码</span>
    </router-link>
  </nav>
</template>

<script setup>
import homeIcon from '../assets/images/首页.png'
import homeActiveIcon from '../assets/images/首页(高亮).png'
import mineIcon from '../assets/images/用户.png'
import mineActiveIcon from '../assets/images/用户(高亮).png'
import pwdIcon from '../assets/images/密码.png'
import pwdActiveIcon from '../assets/images/密码(高亮).png'

defineProps({
  active: { type: String, default: '' }
})
</script>

<style scoped>
.bottom-nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 64px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 33.33%;
  height: 100%;
  text-decoration: none;
  color: #909399;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-item:hover {
  color: #1d6fb8;
}

.nav-item.active {
  color: #1d6fb8;
}

.nav-icon {
  width: 26px;
  height: 26px;
  object-fit: contain;
}

.nav-icon-wrap {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.nav-item.active .nav-icon-wrap {
  background: rgba(29, 111, 184, 0.12);
}

.nav-label {
  font-size: 12px;
}
</style>
