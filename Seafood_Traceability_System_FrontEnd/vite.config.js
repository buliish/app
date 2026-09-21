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

// 普通前缀：这些前缀既是接口前缀，也没有同名的前端页面路由，直接整段代理
;['/user', '/farm', '/froz', '/whol', '/reta', '/region', '/admin', '/inspection'].forEach((prefix) => {
  proxy[prefix] = {
    target,
    changeOrigin: true
  }
})

/*
 * /trace 要特殊处理 —— 它既是后端接口前缀，又是前端页面路由。
 *
 *   页面地址：/trace          （可带 ?code=SHZ... 供扫码直达）
 *   接口地址：/trace/{code}、/trace/products、/trace/product/{code}、/trace/qrcode/{code}
 *             —— 共同点是"后面还有一段路径"
 *
 * 若按普通前缀整段代理，浏览器打开 /trace 会被转发到后端，
 * 拿到 401 或一段 JSON 而不是页面 —— 表现为"扫码打不开"
 * （手机扫二维码走的正是这种直接导航，不是前端路由跳转，绕不过去）。
 *
 * 所以用正则要求"后面必须还有一段路径"：只代理接口，页面交给 SPA 路由。
 * 管理端当初也是同类问题，用把页面挪到 /sys 前缀解决的；
 * /trace 是消费者扫码头号，不能随便改，故用正则区分。
 */
proxy['^/trace/'] = {
  target,
  changeOrigin: true
}

export default defineConfig({
  plugins: [vue()],
  server: {
    // host 必须是 true（= 监听 0.0.0.0）而不是 'localhost'：
    // 设成 localhost 时 Vite 只绑定 127.0.0.1，手机等局域网设备连不进来，
    // 扫码演示会直接连不上。改成本机开发无影响，只是多监听一个网卡。
    host: true,
    port: 5173,
    open: false,
    proxy
  }
})
