import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器把后端接口请求代理到 SpringBoot(8080)
// 代理前缀与后端各 Controller 的 @RequestMapping 保持一致：
//   /user    流通节点端共通功能（登录、企业信息、更新密码）
//   /farm    养殖企业批号
//   /froz    冷冻加工企业批号与加工工序
//   /whol    批发商批号
//   /reta    零售商批号
//   /region  省市区与上游企业联动
//   /trace   消费者溯源查询
//   /admin   系统管理端
//   /inspection 各环节检测记录（四个角色共用，用 stageType 区分批号表）
//
// 注意：后端新增接口前缀时，这里必须同步添加。否则浏览器请求会打到 Vite
// 自己身上、返回 index.html（200），前端解析 JSON 失败并提示“操作失败”，
// 而后端日志里看不到任何请求 —— 这类问题最容易误判成后端故障。
const target = 'http://localhost:8080'
const proxy = {}
;['/user', '/farm', '/froz', '/whol', '/reta', '/region', '/trace', '/admin', '/inspection'].forEach((prefix) => {
  proxy[prefix] = {
    target,
    changeOrigin: true
  }
})

export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'localhost',
    port: 5173,
    open: false,
    proxy
  }
})
