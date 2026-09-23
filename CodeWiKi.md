[CodeWiki.md](https://github.com/user-attachments/files/32543982/CodeWiki.md)
# 冷冻对虾全产业链溯源系统 · Code Wiki

> 本文档是对 `fish` 仓库的结构化代码百科，覆盖项目整体架构、模块职责、关键类与函数、依赖关系与运行方式。
> 仓库由两部分组成：后端 `Seafood_Traceability_System`（Spring Boot）与前端 `Seafood_Traceability_System_FrontEnd`（Vue 3）。

---

## 目录

1. [项目概述](#1-项目概述)
2. [项目整体架构](#2-项目整体架构)
3. [技术栈与依赖关系](#3-技术栈与依赖关系)
4. [数据模型与领域设计](#4-数据模型与领域设计)
5. [后端模块详解](#5-后端模块详解)
6. [前端模块详解](#6-前端模块详解)
7. [关键业务流程](#7-关键业务流程)
8. [项目运行方式](#8-项目运行方式)
9. [关键设计决策与约定](#9-关键设计决策与约定)

---

## 1. 项目概述

**系统名称**：冷冻对虾全产业链溯源系统（Seafood Traceability System）

**业务目标**：覆盖「养殖企业 → 冷冻加工企业 → 批发商 → 零售商 → 消费者」整条对虾产业链，实现产品批号逐级流转、上游对下游进场确认、消费者凭溯源标识码扫码查询全链路信息。

**三类用户角色**：

| 角色 | 入口 | 能力 |
|------|------|------|
| 消费者 | 首页 `/`、溯源页 `/trace` | 免登录浏览在售商品、扫码/输码查询全链路溯源、查看同源产品 |
| 流通节点企业 | 登录 `/login` | 新建/管理本企业批号、向上游发起确认、确认下游进场、录入加工工序与检测记录、下架批号 |
| 系统管理员 | 管理端登录 `/sys/login` | 节点企业注册信息 CRUD、注册统计大屏、批次追溯查询（含产业链树） |

---

## 2. 项目整体架构

### 2.1 顶层结构

```
fish/
├── Seafood_Traceability_System/              # 后端（Spring Boot 3 + MyBatis-Plus）
│   ├── pom.xml                                # Maven 依赖与构建
│   ├── mvnw / mvnw.cmd                        # Maven Wrapper
│   ├── src/main/java/com/gec/seafood_traceability_system/
│   │   ├── SeafoodTraceabilitySystemApplication.java   # 启动类
│   │   ├── config/                            # WebMvc、MyBatis-Plus 配置
│   │   ├── controller/                        # REST 控制器（10 个）
│   │   ├── service/                           # 服务接口
│   │   │   └── impl/                          # 服务实现
│   │   ├── mapper/                            # MyBatis-Plus Mapper 接口
│   │   ├── pojo/                              # 实体/DTO/异常/统一响应
│   │   ├── interceptors/                      # 登录与权限拦截器
│   │   ├── handler/                           # 全局异常处理
│   │   └── utils/                             # JWT、密码、二维码、ThreadLocal 等工具
│   └── src/main/resources/
│       ├── application.yaml                   # 应用配置
│       ├── mapper/*.xml                       # 关联查询与批量操作 SQL
│       └── sql/*.sql                          # 建库与演示数据脚本
│
├── Seafood_Traceability_System_FrontEnd/      # 前端（Vue 3 + Vite + Element Plus）
│   ├── package.json
│   ├── vite.config.js                         # 开发服务器 + 后端代理
│   ├── index.html
│   └── src/
│       ├── main.js                            # 应用入口
│       ├── App.vue
│       ├── router/index.js                    # 路由表 + 守卫
│       ├── api/*.js                           # 后端接口封装（5 个文件）
│       ├── utils/                             # request、nodeType、chartTheme
│       ├── views/                             # 页面（消费者端/节点端/管理端）
│       │   └── admin/                         # 管理端页面
│       ├── components/                        # 复用组件
│       ├── styles/theme.css                   # 主题变量
│       └── assets/images/                     # 图标与图片
│
├── start-demo.ps1                             # 一键启动脚本（含手机扫码演示）
└── 实践任务书.docx
```

### 2.2 分层架构

后端采用经典四层结构，前端采用「路由 → 视图 → API 封装 → 请求工具」的结构：

```
┌───────────────────────── 前端 (Vue 3) ─────────────────────────┐
│  Router (守卫)  →  Views  →  API 封装  →  request.js (axios)    │
│       │                              ↑                           │
│       └── localStorage 存 token      │                           │
└───────────────────────────────────────┼─────────────────────────┘
                                        │ HTTP / JSON (JWT)
┌─────────────────────────── 后端 (Spring Boot) ─────────────────┐
│  Interceptors (Login→Admin/Node)                                 │
│       ↓                                                          │
│  Controller  →  Service(impl)  →  Mapper (MyBatis-Plus)          │
│       ↑              ↑                  ↑                        │
│  GlobalException   TraceChainLoader   XML (JOIN/批量)            │
│  Handler          BatchQuantityService  QrCodeUtil               │
└──────────────────────────────────┬───────────────────────────────┘
                                   │
                            MySQL: seafood_traceability_system
```

### 2.3 请求流转链路

1. 前端 `request.js` 从 `localStorage` 取 token（`adminToken` 或 `nodeInfo.token`），注入 `Authorization` 头。
2. Vite 开发服务器把接口前缀代理到后端 `8080`（见 [6.2](#62-请求封装-requestjs)）。
3. 后端三道拦截器依次执行（[5.3](#53-拦截器与鉴权体系)）：`LoginInterceptor` 验签写 ThreadLocal → `AdminAuthInterceptor` 或 `NodeAuthInterceptor` 校验角色。
4. 进入 Controller，业务异常由 `GlobalExceptionHandler` 统一转 `Result{code,message,data}`。
5. 响应回到前端，`request.js` 拦截器按 `code` 分发：`0` 放行、非 `0` 弹错、`401` 跳登录页。

---

## 3. 技术栈与依赖关系

### 3.1 后端依赖（[pom.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/pom.xml)）

| 依赖 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-parent | 3.2.2 | Spring Boot 父 POM |
| spring-boot-starter-web | 随父 | REST 控制器 |
| spring-boot-starter-validation | 随父 | `@Valid`/`@Pattern` 参数校验 |
| mybatis-plus-spring-boot3-starter | 3.5.7 | ORM（适配 Spring Boot 3） |
| spring-security-crypto | 随父 | BCrypt 密码哈希（**仅 crypto 模块，不引 starter，避免触发 Spring Security 自动配置**） |
| zxing core | 3.5.3 | 溯源二维码生成（只引 core，自行绘制 PNG） |
| mysql-connector-j | 随父 | MySQL 驱动 |
| lombok | 随父 | 实体 getter/setter |
| java-jwt (auth0) | 4.4.0 | JWT 签发与验证 |

- **Java 版本**：17
- **未使用 Spring Security**：鉴权完全由自定义拦截器 + JWT 实现。

### 3.2 前端依赖（[package.json](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/package.json)）

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.5.13 | 框架 |
| vue-router | ^4.5.0 | SPA 路由 |
| element-plus | ^2.9.3 | UI 组件库 |
| @element-plus/icons-vue | ^2.3.1 | 图标 |
| echarts | ^5.5.1 | 管理端统计图表 |
| axios | ^1.7.9 | HTTP 客户端 |
| vite | ^6.0.7 | 构建工具 |
| @vitejs/plugin-vue | ^5.2.1 | Vue SFC 支持 |

- **无 Pinia/Vuex**：状态以 `localStorage`（`nodeInfo`、`adminToken`）+ 组件内状态管理。
- **无独立二维码库**：二维码由后端生成 PNG，前端用 `<img src>` 引用。

### 3.3 模块间依赖（关键链路）

```
TraceController ──► TraceService ──► TraceChainLoader ◄──┐
                                          │              │
AdminTraceController ──► AdminTraceService┘              │
                                                          │
TraceChainLoader ──► (Farm/Froz/Whol/Reta)BatchService ──► NodeInfoService
                  ──► ProcessRecordService

InspectionController ──► OwnedBatchRegistry ──► (四批号 Service).requireOwned
                     ──► InspectionService.refreshBatchQuality ──► 回写批号 qualityStatus

(四批号 Service impl) ──► BatchQuantityService.assertCanTake (超领防护)
                       ──► 下游 Service.listDownConfirm (确认列表)
```

> 注：`BatchQuantityServiceImpl` **刻意注入 Mapper 而非 Service**，以打断 `Farm↔Froz↔BatchQuantity` 的循环依赖（Spring Boot 2.6+ 默认禁止循环依赖）。

---

## 4. 数据模型与领域设计

### 4.1 核心领域：四级批号与产业链

系统的核心是**四张批号表**，分别对应产业链的四个环节。下游批号通过 `up_batch_no` + `up_node_id` 联合指向上游批号（**同时限定批号与企业编号，防止不同企业同号批号串链**）：

```
farm_batch(养殖)  ◄── up_batch_no/up_node_id ──  froz_batch(冷冻加工)
                                                       │
              froz_batch  ◄── up ──  whol_batch(批发)
                                              │
              whol_batch  ◄── up ──  reta_batch(零售)  ── 生成 trace_code(溯源码)
```

### 4.2 批号状态机

| 环节 | 状态值 | 触发动作 |
|------|--------|----------|
| 养殖 `farm_batch` | 1 待发布 → 2 已发布 → 3 已下架 | 发布、下架 |
| 加工/批发/零售 | 1 新建 → 2 待确认 → 3 已确认 → 4 已下架 | 发送确认、上游确认、下架 |

- **零售批号**变为「已确认(3)」时，系统自动生成**溯源标识码 `trace_code`**（唯一键）。
- 下架状态值各环节不同（养殖是 3，其余是 4），由 [OwnedBatchRegistry](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/OwnedBatchRegistry.java) 统一收口判断。

### 4.3 主要数据表（[seafood_traceability_system.sql](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/sql/seafood_traceability_system.sql)）

| 表 | 说明 | 关键字段 |
|----|------|----------|
| `admin` | 系统管理员 | admin_id, admin_name, password |
| `node_info` | 节点企业（四种类型） | node_id, code, password, name, type(1养殖/2加工/3批发/4零售), prov_id, city_id, 各类资质证 |
| `province` / `city` | 行政区划字典 | |
| `farm_batch` | 养殖批号 | farm_batch_id, node_id, batch_no(唯一), breed, breed_stage, source_type, quantity_kg, quality_status, status |
| `froz_batch` | 冷冻加工批号 | froz_batch_id, node_id, batch_no, up_node_id, up_batch_no, product_type, product_form, spec_grade, product_code, quantity_kg, up_quantity_kg |
| `whol_batch` | 批发批号 | 同上结构 |
| `reta_batch` | 零售批号 | 同上 + trace_code(唯一), trace_time, price, image_url |
| `process_record` | 冷冻加工工序 | record_id, froz_batch_id, step(清洗/分级/冷冻/包装), temperature, operator |
| `inspection` | 各环节检测记录 | inspection_id, stage_type, batch_id, batch_no(冗余), item_name, result, conclusion, report_no, org_name |

**多态关联**：`inspection` 表用 `stage_type + batch_id` 关联四张批号表之一（不能用 `batch_no` 关联，因批号只在单表内唯一、跨表可重号）。

### 4.4 数量与领用模型

每张下游批号有两个数量字段：
- `quantity_kg`：本批产出量
- `up_quantity_kg`：自上游批号领用量

校验规则：**同一上游批号的所有下游领用量之和 ≤ 上游总量**（`Σ(领用) ≤ 上游 quantity_kg`）。未登记数量（null）则不限制（兼容历史数据）。

---

## 5. 后端模块详解

### 5.1 启动与配置

#### [SeafoodTraceabilitySystemApplication.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/SeafoodTraceabilitySystemApplication.java)
- `@SpringBootApplication` + `@MapperScan("...mapper")` 注册 MyBatis-Plus Mapper。

#### [application.yaml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/application.yaml)
- 数据源：`jdbc:mysql://localhost:3306/seafood_traceability_system`，root/123456
- 服务端口：`8080`
- JWT：`jwt.secret`（可用环境变量 `JWT_SECRET` 覆盖）、`jwt.ttl-hours: 12`
- 溯源二维码：`trace.qrcode.base-url`（可用 `TRACE_QR_BASE_URL` 覆盖，必须用局域网 IP 而非 localhost，否则手机扫码连不上）
- MyBatis-Plus：下划线转驼峰、控制台 SQL 日志、主键自增、mapper XML 在 `classpath*:/mapper/**/*.xml`

#### [MybatisPlusConfig.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/config/MybatisPlusConfig.java)
- 注册分页插件 `PaginationInnerInterceptor`（管理端节点列表分页依赖）。

#### [WebMvcConfig.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/config/WebMvcConfig.java)
- 注册三道拦截器（顺序不可调换，见 [5.3](#53-拦截器与鉴权体系)）。
- 配置全局 CORS（允许所有来源/方法/头，`allowCredentials=true`）。

### 5.2 统一响应与异常

#### [Result.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/Result.java)
统一响应体 `{code, message, data}`，状态码常量：`0` 成功、`1` 业务失败、`401` 未登录、`403` 无权限。静态工厂 `success(data)` / `error(msg)` / `error(code,msg)`。

#### [BizException.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/BizException.java)
业务异常，携带 `code`。重写 `fillInStackTrace()` 返回 this（业务异常不需堆栈，省开销）。

#### [GlobalExceptionHandler.java](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/handler/GlobalExceptionHandler.java)
`@RestControllerAdvice`，统一把异常转 `Result`：
- `BizException` → 按其 code（401/403 不打堆栈避免噪音）
- `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` → 参数校验错误
- `HttpMessageNotReadableException` → 请求体格式错误
- `DuplicateKeyException` → 唯一键冲突（批号/溯源码重复）
- `Exception` → 兜底「服务器繁忙」

> 401 不在此处理——它发生在拦截器进入 Controller 之前，由拦截器自己写响应。

### 5.3 拦截器与鉴权体系

三道拦截器按注册顺序执行：

| 顺序 | 拦截器 | 职责 | 拦截路径 |
|------|--------|------|----------|
| 1 | [LoginInterceptor](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/interceptors/LoginInterceptor.java) | 验签 JWT，claims 写入 ThreadLocal；失败返 401 | 全部，排除 `/user/login`、`/user/logout`、`/admin/login`、`/trace/**` |
| 2 | [AdminAuthInterceptor](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/interceptors/AdminAuthInterceptor.java) | 要求 claims `role=admin`，否则 403 | `/admin/**`，排除 `/admin/login` |
| 3 | [NodeAuthInterceptor](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/interceptors/NodeAuthInterceptor.java) | 要求 `role=node`（防 admin token 被当企业编号用）；`role=null` 视为历史 token 放行 | `/user/**`、`/farm/**`、`/froz/**`、`/whol/**`、`/reta/**`、`/inspection/**` |

> `/region/**` 刻意不做角色校验：管理端页面（省市区联动）也用它，而管理端发请求带的是 adminToken。

JWT claims 结构：`{role: "admin"|"node", id: 主键, username: 登录编码}`。`id` 在节点端即 `node_info.node_id`，在管理端即 `admin_id`。

### 5.4 Controller 层（API 清单）

#### 消费者端 [TraceController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/TraceController.java) — `/trace`（免登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/trace/products` | 在售商品分页（关键词/形态/省筛选） |
| GET | `/trace/product/{code}` | 商品详情（四级链+检测+工序） |
| GET | `/trace/qrcode/{code}` | 溯源二维码 PNG（`<img>` 引用，出错返占位图） |
| GET | `/trace/{traceCode}` | 溯源查询（`@Pattern` 校验码格式） |

#### 管理端 [AdminController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/AdminController.java) — `/admin`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/login` | 管理员登录（密码平滑升级 BCrypt） |
| GET | `/admin/node/page` | 节点企业分页+模糊查询 |
| GET/POST/PUT/DELETE | `/admin/node[/{id}]` | 节点企业 CRUD |
| GET | `/admin/stats` | 注册统计（类型分布/省分布/12 月趋势） |

#### 管理端追溯 [AdminTraceController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/AdminTraceController.java) — `/admin/trace`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/trace/search` | 关键词搜候选批次 |
| GET | `/admin/trace/chain` | 按关键词取完整链路 |
| GET | `/admin/trace/chain/{retaBatchId}` | 按主键取完整链路 |
| GET | `/admin/trace/tree/{retaBatchId}` | 完整产业链树（一批多品） |
| GET | `/admin/trace/stats` | 各环节合格率与形态分布 |

> 挂在 `/admin/**` 下而非 `/trace/**`，避免未登录用户看到全链路企业信息与检测报告。

#### 节点端共通 [NodeController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/NodeController.java) — `/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/login` | 企业登录（BCrypt 平滑升级） |
| POST | `/user/logout` | 退出（JWT 无状态，前端清本地） |
| GET | `/user/getInfo/{code}` | 按编码查企业信息 |
| PATCH | `/user/updatePwd` | 改密码（校验原密码） |
| GET | `/user/userInfo` | 当前登录企业信息 |
| PUT | `/user/update` | 更新企业信息 |

#### 四个批号 Controller（结构同构）

[FarmBatchController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/FarmBatchController.java) `/farm`、[FrozBatchController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/FrozBatchController.java) `/froz`、[WholBatchController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/WholBatchController.java) `/whol`、[RetaBatchController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/RetaBatchController.java) `/reta`

每个都提供：
- `GET /{prefix}/batch/list` 批号列表（按状态）
- `GET /{prefix}/batch/{id}` 详情（`requireOwned` 归属校验）
- `GET /{prefix}/batch/check` 批号唯一性校验
- `POST /{prefix}/batch` 新建
- `PUT /{prefix}/batch` 更新（`publish`/`sendConfirm` 参数控制发布或发起确认）
- `DELETE /{prefix}/batch/{id}` 删除（仅初始状态）
- `PUT /{prefix}/batch/offline/{id}` 下架
- `GET /{prefix}/confirm/list` 下游进场确认列表
- `PUT /{prefix}/confirm/{id}` 确认下游进场

`FrozBatchController` 额外提供加工工序接口：`GET/POST/DELETE /froz/process[...]`、`POST /froz/process/batch`（批量录入整套工序）。

> 所有按 id 操作的端点都先经 `requireOwned(id, currentNodeId[, 允许状态...])` 校验归属与状态，防 IDOR 越权。

#### 检测记录 [InspectionController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/InspectionController.java) — `/inspection`（四环节共用，`stageType` 区分）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/inspection/list` | 某批号检测记录（归属校验） |
| POST | `/inspection` | 新增单条（回填 nodeId/batchNo，刷新批号质量） |
| POST | `/inspection/batch` | 批量新增一份报告的多个检测项 |
| DELETE | `/inspection/{id}` | 删除（归属校验+刷新质量） |

`prepare()` 方法刻意**后端回填 nodeId/batchNo**，不接受前端传入，防伪造 batchNo 绕过列表校验。

#### 行政区划 [RegionController](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/controller/RegionController.java) — `/region`

`/provinces`、`/cities`、`/nodes`（按类型+区域查上游企业）、`/batches`（查某企业可流通的上游批号，含剩余可领用量）。

### 5.5 Service 层关键服务

#### [TraceChainLoader](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/TraceChainLoaderImpl.java) — 溯源链路核心

消费者端与管理端**共用**同一走查实现，是系统最核心的算法类。

关键方法：
- `loadByRetaBatchId(retaBatchId)` → `TraceChain`：从零售批号**逐级上溯**。
- `resolveCandidates(keyword)` → `List<Integer>`：关键词→候选零售批号。支持溯源码/产品编号/零售批号（直接命中），以及批发/加工/养殖批号（**正向追踪**，逐级向下找到零售端）。
- `loadTreeByRetaBatchId(retaBatchId)` → `ChainTreeNode`：产业链树。先 `walkUp` 得当前链路径，再从能上溯到的最上游开始**向下递归展开**整棵树。

走查口径（防串链核心）：每级匹配同时限定 `batch_no` 与 `node_id`：

```java
// walkUp：零售 → 批发 → 加工 → 养殖
WholBatch whol = wholBatchService.lambdaQuery()
    .eq(WholBatch::getBatchNo, reta.getUpBatchNo())
    .eq(reta.getUpNodeId() != null, WholBatch::getNodeId, reta.getUpNodeId())
    .one();
```

#### [TraceServiceImpl](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/TraceServiceImpl.java) — 消费者门面

- `trace(traceCode)`：查入口批号 → **施加消费者侧状态门槛**（已下架不可溯源，此门槛留在门面层不下沉到 Loader，否则管理端也看不到已下架批号）→ 渲染 Map。
- `productDetailByTraceCode(code)`：商品详情，已下架链接仍可打开但前端提示 `offline`。
- `relatedProducts(chain)`：同源产品——同一养殖批号派生的其他零售商品，**只回零售层公开信息**，不返回中间环节。
- `listProducts(...)`：商品列表走 [RetaBatchMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/RetaBatchMapper.xml) 的一条四级 LEFT JOIN，避免 N+1。

#### [AdminTraceServiceImpl](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/AdminTraceServiceImpl.java) — 管理端门面

复用 `TraceChainLoader`，但**不做下架状态门槛**，渲染含四环节明细/检测/质量汇总/加工记录的运营视角结构。

#### [OwnedBatchService](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/OwnedBatchService.java) — 归属校验基接口

`default` 方法 `requireOwned(id, currentNodeId[, 允许状态...])`：取出数据校验归属（不匹配抛 403），并可选限定状态白名单。四批号 Service 接口继承它即获得能力，无需实现。

#### [OwnedBatchRegistry](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/OwnedBatchRegistry.java) — 环节路由注册表

按 `stageType` 路由到对应批号 Service 的 `requireOwned`，统一防 IDOR。`requireOwnedForInspection` 额外禁止已下架批号录入检测（下架值各环节不同，统一收口）。

#### [BatchQuantityServiceImpl](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/BatchQuantityServiceImpl.java) — 超领防护

- `assertCanTake(下游环节, 上游批号, 上游企业, 领用量[, 自身批号])`：**悲观锁** `SELECT ... FOR UPDATE` 锁上游批号行，再算 `剩余 = 上游总量 - Σ下游领用`，超量抛异常。必须在事务内调用且覆盖落库。
- `remaining(...)`：只读算剩余量（上游下拉展示用）。
- **注入 Mapper 而非 Service** 以打断循环依赖。
- 下游批号**删除**时领用行消失、额度自动回落；**下架**不算释放（货已交付）。

#### 批号 Service 实现（以 [FarmBatchServiceImpl](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/FarmBatchServiceImpl.java) 为例）

- `listByNodeAndStatus`：不传状态时排除已下架。
- `confirmDownstream(下游批号id, 本企业id)`：只能确认「以本企业批号为进场批号」的下游，防跨企业误确认；将下游状态从「待确认(2)」置「已确认(3)」。
- `listPendingConfirm`：委托给**下游环节** Service 的 `listDownConfirm`（统一约定：每个环节只查自己那张批号表）。

#### [InspectionServiceImpl](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/service/impl/InspectionServiceImpl.java)

- `refreshBatchQuality(stageType, batchId)`：有不合格项→整批不合格(2)；有记录→合格(1)；无记录→待检(0)。回写批号表 `quality_status`（窄更新，只设主键+qualityStatus）。
- `listByRefs(refs)`：用一次 IN 查询取回一条链上全部检测记录（非逐环节查）。

### 5.6 POJO/实体层

| 类 | 角色 | 要点 |
|----|------|------|
| [NodeInfo](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/NodeInfo.java) | 节点企业实体 | `type` 列映射 `nodeType` 字段；`password` 标 `@JsonProperty(WRITE_ONLY)` 序列化不外泄 |
| [FarmBatch](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/FarmBatch.java) / [FrozBatch](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/FrozBatch.java) / [WholBatch](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/WholBatch.java) / [RetaBatch](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/RetaBatch.java) | 四级批号实体 | 均 `implements NodeOwned`；质量状态字段刻意命名 `qualityStatus`（**不能叫 status**，否则 Lombok 生成的 `getStatus()` 覆盖 `NodeOwned.getStatus()` 致状态校验静默失效） |
| [NodeOwned](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/NodeOwned.java) | 归属接口 | `getNodeId()` / `getBatchNo()` / `getStatus()` 默认方法，统一归属校验的抽象 |
| [TraceChain](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/TraceChain.java) | 完整溯源链 | 含四级批号+四级企业+工序+检测；`isComplete()`/`overallQuality()`/`refs()` 派生方法 |
| [ChainTreeNode](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/ChainTreeNode.java) | 产业链树节点 | `children` 递归结构；`onCurrentChain` 标记当前链路径 |
| [BatchRef](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/BatchRef.java) | 批号轻量引用 | `stageType+batchId+batchNo`，攒成列表一次 IN 查检测记录 |
| [ConfirmVO](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/ConfirmVO.java) | 确认列表项 | `downNode` 标 `@JsonIgnore`，仅承映射结果；`downName` 派生输出 |
| [Inspection](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/Inspection.java) | 检测记录实体 | 刻意无 `status` 字段，`NodeOwned.getStatus()` 返 null |
| [ProcessRecord](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/ProcessRecord.java) | 加工工序实体 | `getBatchNo()` 标 `@JsonIgnore` 避免输出无意义 null |
| [Admin](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/pojo/Admin.java) | 管理员实体 | `password` 标 `@JsonProperty(WRITE_ONLY)` |

### 5.7 工具类

| 类 | 职责 |
|----|------|
| [JwtUtil](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/utils/JwtUtil.java) | HMAC256 签发/验证 JWT；显式判空区分「没带 token」与「签名错误」；兼容 `Bearer ` 前缀 |
| [PasswordUtil](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/utils/PasswordUtil.java) | BCrypt；**平滑升级**：库里是明文则按明文比对，登录成功即改写为哈希 |
| [QrCodeUtil](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/utils/QrCodeUtil.java) | zxing 生成 PNG；`placeholderPng` 生成占位图（接口被 `<img>` 引用，出错必须返合法 PNG 而非 JSON） |
| [ThreadLocalUtil](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/utils/ThreadLocalUtil.java) | 存取 JWT claims；`afterCompletion` 清除防内存泄漏 |
| [InterceptorUtil](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/java/com/gec/seafood_traceability_system/utils/InterceptorUtil.java) | 拦截器写标准 `Result` JSON 响应（拦截器进不了 Controller，拿不到全局异常处理） |

### 5.8 Mapper XML

位于 `src/main/resources/mapper/`。单表 CRUD 由 BaseMapper 自动生成，XML 只写关联与批量：

- [RetaBatchMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/RetaBatchMapper.xml)：`selectProductPage`（四级 LEFT JOIN 取商品卡全字段）、`selectDownConfirmList`（`<association>` 一次 JOIN 查下游企业，非 N+1）。
- [FrozBatchMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/FrozBatchMapper.xml) / [WholBatchMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/WholBatchMapper.xml)：同构的 `selectDownConfirmList`。
- [InspectionMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/InspectionMapper.xml)：`selectByRefs`（按 BatchRef 列表 IN 查询）、`insertBatch`（`<foreach>` 批量插入）。
- [ProcessRecordMapper.xml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/mapper/ProcessRecordMapper.xml)：批量插入工序。

---

## 6. 前端模块详解

### 6.1 入口与路由

#### [main.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/main.js)
注册 Element Plus（中文 locale）+ 全局图标 + router。**未注册 Pinia/Vuex**。

#### [router/index.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/router/index.js)

路由分三组，用 `meta.requiresAuth` / `meta.requiresAdmin` 标识权限：

**消费者端（免登录）**
- `/` ConsumerHomeView 首页
- `/trace` TraceView 溯源查询
- `/product/:code` ProductDetailView 商品详情

**节点端（requiresAuth，需 `localStorage.nodeInfo`）**
- `/login`、`/menu`、`/mine`、`/updatePwd`
- `/batch/create`、`/batch/update/:id`、`/batch/list`、`/batch/detail/:id`
- `/confirm` 下游进场确认

**管理端（requiresAdmin，需 `localStorage.adminToken`，统一 `/sys` 前缀）**
- `/sys/login`、`/sys/nodes`、`/sys/dashboard`、`/sys/trace`

> 管理端页面用 `/sys` 前缀而非 `/admin`：因 Vite 把 `/admin` 代理给后端，页面路由若也用 `/admin`，刷新会被代理转发返 401。

路由守卫 `beforeEach`：设标题 + 按 meta 检查 token，缺失则跳对应登录页。

### 6.2 请求封装 [request.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/utils/request.js)

- `baseURL: '/'`、timeout 10s。
- **请求拦截器**：`/admin` 接口用 `adminToken`；其余用 `nodeToken`，未登录节点端但已登录管理端时用 `adminToken` 兜底（管理端页面的 `/region/**` 请求）。
- **响应拦截器**：`code===0` 放行；非 0 弹 `ElMessage` 并 reject；`401` 按身份清 token 跳对应登录页。

[vite.config.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/vite.config.js) 把 `/user`、`/farm`、`/froz`、`/whol`、`/reta`、`/region`、`/admin`、`/inspection` 整段代理到 `8080`；`/trace/` 用正则只代理接口（后面还有路径段），保留 `/trace` 页面交给 SPA 路由。`host: true` 监听 0.0.0.0 供手机扫码访问。

### 6.3 API 层（[src/api/](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api)）

| 文件 | 覆盖接口 |
|------|----------|
| [admin.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api/admin.js) | 管理员登录、节点 CRUD、统计、批次追溯（search/chain/tree/stats） |
| [trace.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api/trace.js) | 溯源查询、商品列表、商品详情 |
| [batch.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api/batch.js) | 四角色批号 CRUD（按 `prefixOf(nodeType)` 切前缀）、确认、加工工序、检测记录 |
| [region.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api/region.js) | 省市区/上游企业/上游批号联动 |
| [node.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/api/node.js) | 节点端登录/登出/企业信息/改密 |

### 6.4 视图层

**消费者端**
- [ConsumerHomeView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/ConsumerHomeView.vue) 首页：在售商品卡片列表（分页/筛选），点商品进详情
- [TraceView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/TraceView.vue) 溯源查询：输码/扫码查全链路，展示四级企业链+检测+工序+同源产品
- [ProductDetailView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/ProductDetailView.vue) 商品详情

**节点端**
- [LoginView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/LoginView.vue) 登录
- [MenuView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/MenuView.vue) 功能菜单
- [MineView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/MineView.vue) 我的企业信息
- [UpdatePwdView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/UpdatePwdView.vue) 改密
- [BatchCreateView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/BatchCreateView.vue) 新建/更新批号（省→市→上游企业→上游批号联动）
- [BatchListView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/BatchListView.vue) 批号管理
- [BatchDetailView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/BatchDetailView.vue) 批号详情（含检测/工序）
- [ConfirmView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/ConfirmView.vue) 下游进场确认

**管理端**（[src/views/admin/](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/admin)）
- [AdminLoginView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/admin/AdminLoginView.vue) 登录
- [NodeManageView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/admin/NodeManageView.vue) 节点企业 CRUD + 统计
- [DashboardView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/admin/DashboardView.vue) 数据可视化大屏（ECharts）
- [TraceQueryView.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/views/admin/TraceQueryView.vue) 批次追溯查询（含产业链树）

### 6.5 组件与工具

| 文件 | 用途 |
|------|------|
| [components/BottomNav.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/components/BottomNav.vue) | 底部导航栏 |
| [components/StatsPanel.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/components/StatsPanel.vue) | 统计面板 |
| [components/InspectionTable.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/components/InspectionTable.vue) | 检测记录表格 |
| [components/QualityTag.vue](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/components/QualityTag.vue) | 质量状态标签 |
| [utils/nodeType.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/utils/nodeType.js) | 角色/状态/前缀/产品类型等通用映射与配色 |
| [utils/chartTheme.js](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/utils/chartTheme.js) | ECharts 主题 |
| [styles/theme.css](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System_FrontEnd/src/styles/theme.css) | 主题 CSS 变量 |

---

## 7. 关键业务流程

### 7.1 批号生命周期与上游确认

以「养殖 → 加工」为例：

1. 养殖企业新建批号（状态 1 待发布）→ 发布（2 已发布）。
2. 加工企业新建批号，填写上游养殖企业+批号（状态 1 新建）→ 点「发送确认」（2 待确认）。
3. 养殖企业在「下游进场确认」列表看到该加工批号 → 确认（加工批号变 3 已确认，**加工环节定型 product_form/spec_grade/product_code**）。
4. 同理加工→批发→零售逐级确认。
5. **零售批号被上游确认（3 已确认）时，系统自动生成溯源标识码 `trace_code`**。
6. 任意环节可下架（养殖→3，其余→4）。

### 7.2 消费者溯源查询

1. 消费者输溯源码（或扫二维码）→ `GET /trace/{traceCode}`。
2. `TraceServiceImpl.trace()`：先按溯源码查零售批号，退回按产品编号查 → **已下架抛异常**。
3. `TraceChainLoader.loadByRetaBatchId()` → `walkUp()` 逐级上溯装配 `TraceChain`。
4. `render()` 渲染成前端契约 Map（四级企业、批号、检测、工序、同源产品）。
5. 二维码 `GET /trace/qrcode/{code}` 用 `existsCode`（不看状态）校验，出错返占位 PNG。

### 7.3 产业链树（管理端）

`loadTreeByRetaBatchId`：先 `walkUp` 得当前链路径（用于打 `onCurrentChain` 标记），再从能上溯到的最上游**向下递归**展开整棵树（一批虾派生虾滑/虾丸等多分支）。前端递归渲染，高亮当前链。

### 7.4 超领防护

下游新建/更新批号时调 `saveWithQuantityCheck` / `updateWithQuantityCheck` → `BatchQuantityService.assertCanTake`：
- `SELECT ... FOR UPDATE` 锁上游批号行
- 算 `剩余 = 上游 quantity_kg - Σ(同源下游 up_quantity_kg)`（更新时排除自身）
- 领用量超剩余抛异常；未登记数量（null）放行

### 7.5 检测记录与质量回写

录入检测记录后 `refreshBatchQuality` 重算批号 `quality_status`：有不合格项→2，有记录→1，无记录→0。整链质量 `TraceChain.overallQuality()`：任一环节不合格即不合格。

---

## 8. 项目运行方式

### 8.1 环境要求

- JDK 17、Maven 3.6+、MySQL 8、Node.js 18+、npm
- 后端 Java 17，前端 Vite 6

### 8.2 数据库准备

1. 创建/确认 MySQL 库 `seafood_traceability_system`（连接串见 [application.yaml](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/application.yaml)，默认 root/123456）。
2. 执行 [seafood_traceability_system.sql](file:///c:/Users/31218/Desktop/fish/Seafood_Traceability_System/src/main/resources/sql/seafood_traceability_system.sql)（增量、可重复执行，含建表、字典、演示节点与业务数据）。
3. 默认账号：管理员 `admin/123456`；节点企业 `froz001`、`farm101` 等（密码均 `123456`）。

### 8.3 一键启动（推荐，含手机扫码演示）

右键 [start-demo.ps1](file:///c:/Users/31218/Desktop/fish/start-demo.ps1) →「使用 PowerShell 运行」。脚本会：
1. 探测本机局域网 IP（排除虚拟网卡）；
2. 注入 `TRACE_QR_BASE_URL` 拉起后端（`mvn spring-boot:run`，8080）；
3. 拉起前端（`npm run dev`，5173，`host:true` 监听所有网卡）；
4. 打印电脑/手机访问地址与扫码演示步骤。

### 8.4 手动分别启动

**后端**：
```bash
cd Seafood_Traceability_System
mvn spring-boot:run          # 端口 8080
```

**前端**：
```bash
cd Seafood_Traceability_System_FrontEnd
npm install
npm run dev                  # 端口 5173，开发代理到 8080
```

访问 `http://localhost:5173`（消费者首页）；手机扫码需用局域网 IP，并放行防火墙 5173/8080。

### 8.5 构建产物

- 后端：`mvn package` → `target/Seafood_Traceability_System-0.0.1-SNAPSHOT.jar`
- 前端：`npm run build` → `dist/`

### 8.6 关键配置项

| 配置 | 环境变量 | 默认 | 说明 |
|------|----------|------|------|
| JWT 密钥 | `JWT_SECRET` | seafood-traceability-system-2026 | 改后旧 token 全失效 |
| 二维码基址 | `TRACE_QR_BASE_URL` | http://10.48.129.152:5173/trace?code= | 必须用局域网 IP，换网络后重跑 start-demo.ps1 |

---

## 9. 关键设计决策与约定

1. **批号联合匹配防串链**：上下游关联同时限定 `batch_no + node_id`，因批号只在单表内唯一、跨表可重号。溯源上溯、正向追踪、产业链树下展、商品 JOIN 全用同一口径。
2. **消费者/管理端共用 TraceChainLoader**：状态门槛（已下架不可溯源）留在消费者门面层，不下沉到 Loader，保证管理端仍可查已下架批号。
3. **归属校验统一抽象**：`NodeOwned` 接口 + `OwnedBatchService.requireOwned` + `OwnedBatchRegistry` 路由，四批号 × 多端点复用同一套 IDOR 防护。
4. **超领防护悲观锁**：`FOR UPDATE` 锁上游行，必须在覆盖落库的事务内调用。
5. **注入 Mapper 打断循环依赖**：`BatchQuantityServiceImpl` 注入 Mapper 而非 Service，避免 `Farm↔Froz↔BatchQuantity` 环。
6. **密码平滑升级**：明文/BCrypt 共存，登录成功即改写哈希，无需一次性迁移。
7. **质量状态字段命名**：必须叫 `qualityStatus` 不能叫 `status`，否则 Lombok 覆盖 `NodeOwned.getStatus()` 致状态校验失效。
8. **二维码接口契约**：被 `<img src>` 引用，出错必须返合法 PNG 占位图，不能返 JSON（否则破图）。
9. **管理端页面用 `/sys` 前缀**：避免与 Vite 的 `/admin` 接口代理冲突。
10. **`/trace` 代理用正则**：只代理 `/trace/`（带后续路径的接口），保留 `/trace` 页面给 SPA 路由，解决扫码直达问题。


