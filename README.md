[README.md](https://github.com/user-attachments/files/32543811/README.md)
# 冷冻对虾全产业链溯源系统

> 覆盖「养殖企业 → 冷冻加工企业 → 批发商 → 零售商 → 消费者」整条对虾产业链的溯源系统。
> 流通节点企业逐级登记批号、上游对下游进场确认；消费者凭溯源标识码扫码查询全链路信息。

后端：`Seafood_Traceability_System`（Spring Boot 3 + MyBatis-Plus）
前端：`Seafood_Traceability_System_FrontEnd`（Vue 3 + Vite + Element Plus + ECharts）

完整架构与代码说明见 [CodeWiki.md](./CodeWiki.md)。

---

## 功能特性

- **三端协同**：消费者端（免登录）、流通节点企业端、系统管理端
- **四级产业链流转**：养殖 → 冷冻加工 → 批发 → 零售，下游批号通过 `up_batch_no + up_node_id` 关联上游
- **进场确认机制**：下游发起请求，上游确认后批号生效；零售批号确认时自动生成溯源标识码
- **扫码溯源**：消费者输入/扫描溯源码，查看四级企业链、检测报告、加工工序、同源产品
- **产业链树**：管理端可查看「一批虾派生虾滑/虾丸等多分支」的完整树形链路
- **超领防护**：悲观锁校验下游领用量之和不超过上游总量
- **质量回写**：检测记录自动汇总并回写各环节批号质量状态
- **可视化大屏**：管理端企业注册统计（类型/省份分布、12 月趋势）

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2.2、Java 17、MyBatis-Plus 3.5.7、MySQL 8、JWT(auth0)、BCrypt、zxing |
| 前端 | Vue 3.5、Vite 6、Vue Router 4、Element Plus 2.9、ECharts 5.5、Axios |
| 鉴权 | 自定义三道拦截器（登录 → 管理员/节点角色），无 Spring Security |

## 项目结构

```
fish/
├── Seafood_Traceability_System/              # 后端
│   ├── src/main/java/com/gec/seafood_traceability_system/
│   │   ├── controller/   service/   mapper/   pojo/
│   │   ├── interceptors/  config/    utils/   handler/
│   └── src/main/resources/
│       ├── application.yaml
│       ├── mapper/*.xml        # 关联查询与批量 SQL
│       └── sql/*.sql           # 建库与演示数据
├── Seafood_Traceability_System_FrontEnd/      # 前端
│   └── src/
│       ├── router/  api/  utils/  views/  components/
└── start-demo.ps1                             # 一键启动（含手机扫码演示）
```

## 快速开始

### 环境要求

JDK 17、Maven 3.6+、MySQL 8、Node.js 18+

### 1. 准备数据库

MySQL 中执行 [`Seafood_Traceability_System/src/main/resources/sql/seafood_traceability_system.sql`](./Seafood_Traceability_System/src/main/resources/sql/seafood_traceability_system.sql)。
脚本增量、可重复执行：建库 `seafood_traceability_system`、建表、写入省市区字典、演示节点企业与业务数据。

数据库连接配置见 [`application.yaml`](./Seafood_Traceability_System/src/main/resources/application.yaml)（默认 `localhost:3306`，root/123456）。

### 2. 启动（二选一）

**方式 A · 一键启动（推荐，含手机扫码演示）**

右键 [`start-demo.ps1`](./start-demo.ps1) →「使用 PowerShell 运行」。脚本自动探测局域网 IP、注入二维码地址、拉起前后端，并打印电脑/手机访问地址。

**方式 B · 手动启动**

```bash
# 后端（端口 8080）
cd Seafood_Traceability_System
mvn spring-boot:run

# 前端（端口 5173，开发代理到 8080）
cd Seafood_Traceability_System_FrontEnd
npm install
npm run dev
```

### 3. 访问

- 电脑：`http://localhost:5173`（消费者首页）
- 手机扫码：手机与电脑连同一 WiFi，访问脚本打印的局域网地址；首次运行需放行防火墙 5173/8080

## 默认账号

| 角色 | 账号 | 密码 | 入口 |
|------|------|------|------|
| 系统管理员 | `admin` | `123456` | `/sys/login` |
| 节点企业 | `froz001`、`farm101`、`whol001`、`reta001` 等 | `123456` | `/login` |

> 密码采用 BCrypt 平滑升级：库中明文登录成功后自动改写为哈希。

## 关键配置

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| JWT 密钥 | `JWT_SECRET` | 改后已签发 token 全部失效 |
| 二维码基址 | `TRACE_QR_BASE_URL` | 手机扫码必须用局域网 IP，不能用 localhost；换网络后重跑 `start-demo.ps1` |

## 构建产物

```bash
# 后端 jar
cd Seafood_Traceability_System && mvn package
# 前端 dist
cd Seafood_Traceability_System_FrontEnd && npm run build
```

## 更多文档

- 架构与代码详解：[CodeWiki.md](./CodeWiki.md)
