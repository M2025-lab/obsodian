# starlord-推送日报聚合接口queryPushPools-技术方案

> 飞书文档：<链接占位，用户有则填>

## 1、背景

[[eden-agenda-推送数据源queryByPlan切换-技术方案]] 把日报推送取数换到 `query-by-plan` 后，agenda 侧演化出两个问题：① 板块判据（taskType/flowType/时间窗/哨兵扣减）散在 agenda 客户端——**1900 占位值是 starlord 的存储约定**（SupplierPlan.java:61），agenda 被迫引入 `NULL_DATE` 哨兵，且该哨兵经服务端 `validTime` 归一（SupplierProgressServiceImpl.java:486、:1004-1009 `≤1900-01-01 → null`）后是静默 no-op，"未实际进仓"条件在通用入参通道上**表达不出来**；② 四板块四查 + 批查C 反查 = agenda 每次 analyze 发 5 次 feign，库里数据量大（用户 2026-11-27 定稿四查拆分的原因）。本方案把板块拉取编排整体下沉 starlord，出一个**推送专用聚合接口**，agenda 缩成单次调用。

## 2、需求理解

| # | 需求点 | 说明 |
|---|---|---|
| 1 | 拉取编排下沉 | agenda 四查 + 批查C 的板块条件编排移到 starlord 服务端 |
| 2 | 已排送扣减下沉 | "明日送货的货单从在库未送剔除"（用户定稿 2026-11-27）改为服务端集合差 ④−③ |
| 3 | 出参按板块分池 | 返回四个池子（正单/返补/送货/在库未送），agenda 免拼池 |
| 4 | queryByPlan 通用接口不动 | 聚合接口是编排层新增，不碰既有通用语义 |

## 3、现状

agenda 侧现行编排（DailyPushPlanAnalyzeServiceImpl.java:85-131，本轮四查拆分后）：

```
analyze()
  ├─ ① queryByPlan(taskType=14, flowType=0, expectTime≥今日, factTime≤NULL_DATE)   :87-91
  ├─ ② queryByPlan(taskType=14, flowType=1, expectTime≥今日, factTime≤NULL_DATE)   :94-98
  ├─ ③ queryByPlan(taskType=5,  expectTime≥今日, factTime≤NULL_DATE)               :101-104
  ├─ ④ queryByPlan(taskType=14, factTime∈[今日-staleDays, 今日])                   :107-110
  └─ 批查C queryByPlan(orderNos=④货单号集) → 内存扫 taskType==5 得已排送集          :112-131
```

其中 `factTimeLessThanOrEqualTo(NULL_DATE)` 为**静默 no-op**（现状 bug）：NULL_DATE=1900-01-01 哨兵被服务端 validTime 归一成 null，条件不进 SQL——① 靠 Assembler 客户端 `factTime != null` 兜底剔除（DailyPushPlanAssembler.java:69），②③ 无兜底。plan 表 fact_time 存储：进仓行=真实时间，其余=1900 占位（SupplierPlan.java:61）；服务端出参组装时 validTime 已把 1900 洗成 null（SupplierProgressServiceImpl.java:411-412）——**"没实际进仓"= 出参 factTime==null，agenda 侧本可判 null，无需哨兵**。

## 4、详细设计

### 4.1 入参出参（api 模块新增两个 DTO）

入参 `SupplierProgressPushPoolsQueryParam`（edan-starlord-api dto/request）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `staleDays` | Integer | 在库未送陈旧行护栏（天），null 默认 30；调方传 agenda Apollo `planpush.stale-days` |

无单号、无板块开关——四池子全量返回，要哪个池是 agenda 组装层的事。

出参 `SupplierProgressPushPoolsVO`（dto/response）：

| 字段 | 类型 | 语义 |
|---|---|---|
| `mainOrderInbound` | `List<SupplierProgressByPlanVO>` | ① 正单预计进仓（taskType=14 ∧ flowType=0 货单级 ∧ expectedTime≥今日 ∧ factTime 无效值） |
| `reworkArrival` | `List<SupplierProgressByPlanVO>` | ② 返补预计到工地（taskType=14 ∧ flowType=1 ∧ expectedTime≥今日） |
| `tomorrowDelivery` | `List<SupplierProgressByPlanVO>` | ③ 明日送货（taskType=5 ∧ expectedTime≥今日，不区分正单/返补） |
| `stockUnsent` | `List<SupplierProgressByPlanVO>` | ④ 在库未送（taskType=14 ∧ factTime∈[今日-staleDays, 今日]，**再剔除③货单号集**） |

行结构复用 `SupplierProgressByPlanVO`（含 plans 8 字段元素）——agenda Assembler 已按该 VO 消费，零适配。四池行内的 plans 只含各自板块判据命中的 plan 行（复用 queryByPlan 的组装语义）。

### 4.2 服务端编排（SupplierProgressServiceImpl 新增 queryPushPools）

```
queryPushPools(param)
  ├─ today = 当日 0 点；staleStart = today - staleDays
  ├─ pools = 各板块按 queryByPlan 内部调用（复用 ①-④ 四次，入参 SupplierProgressByPlanQueryParam.builder()）：
  │    ① taskType=14, flowType=0, expectTimeGreaterThanOrEqualTo=today
  │    ② taskType=14, flowType=1, expectTimeGreaterThanOrEqualTo=today
  │    ③ taskType=5,  expectTimeGreaterThanOrEqualTo=today
  │    ④ taskType=14, factTimeGreaterThanOrEqualTo=staleStart, factTimeLessThanOrEqualTo=today
  ├─ ① 池行集后滤：plans 里剔除 factTime 非无效值（≠1900 占位）的进仓行——「预计」进仓不含已进仓
  │    （复用 validTime 判据：factTime==null 即无效值；服务端对 plan 模型直判，或按 1900 getTime 比较）
  ├─ ④−③ 集合差：④ 行 orderNo ∈ ③ 行 orderNo 集 → 从 stockUnsent 剔除（用户定稿 2026-11-27：
  │    明日送货的货单从在库未送删掉；替代 agenda 批查C 内存扫 taskType==5）
  └─ return 四池 VO
```

编排实现要点：

- **内部调用不走 feign 自调**，直接调本类 `queryByPlan(param)`（:357）——四次复用同一 SQL（SupplierPlanMapper.xml:184 selectForProgressQuery），无新 SQL。
- ① 的「未实际进仓」后滤在服务端做（factTime 哨兵判断归数据方）：plan 模型上 factTime 为 1900 占位（SupplierPlan.java:61），`validTime` 同款判据。
- ④ 的扣减：`Set<String> deliveryOrderNos = ③池 orderNo 集`（trim 后），④池行 orderNo 命中即剔除；orderNo 空的行无关联键不参与剔除（对齐 agenda 现行口径，DailyPushPlanAssembler.java:100-105）。
- 无 500 上限问题：内部无 orderNos IN 入参；④ SQL 命中量由 staleDays 窗控制。

### 4.3 agenda 侧消费（另见 agenda 方案更新，此处只述依赖）

`SupplierProgressManager#queryPushPools(param)` 单次 feign 调用替换四查 + 批查C 全部逻辑（DailyPushPlanAnalyzeServiceImpl.java:85-131 缩成一次取数）；`NULL_DATE` 常量、`staleDays` Apollo 字段与批查C 整段删除（staleDays 改传参）。板块判据仍由 Assembler 按 plans 精滤（服务端已滤过一遍，客户端判据保留为兜底，不冲突）。

### 4.4 无需改动部分

- `queryByPlan` 通用接口及 SQL（SupplierPlanMapper.xml selectForProgressQuery）：零改动——聚合编排只是新调用方
- `queryByCondition`/`countByCondition` 翻译链路：不动（queryPushPools 不含单号入参）
- mock-data/sync/cleanup 等接口：零改动

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|
| `POST /task-supplier/progress/query-push-pools`（新增） | SupplierProgressFeign.java:66 后新增方法 / SupplierProgressController.java:79 后新增端点 | 新增 api 入/出参 DTO（4.1）+ service 接口方法 queryPushPools + 编排实现（4.2）：四查复用 queryByPlan + ①后滤 + ④−③ 集合差；Controller 薄透传 success(...) |  |
| `POST /task-supplier/progress/query-by-plan` | 既有 | 无代码改动（聚合编排复用其内部方法，不动本体） |  |

## 6、上游依赖

无（本工程是服务提供方；agenda 侧消费见 [[eden-agenda-推送数据源queryByPlan切换-技术方案]] jar 升级条目）。

## 7、表结构改动

无（supplier_plan/supplier_progress 现有列足够）。

## 8、风险评估

| # | 风险点 | 评估 |
|---|---|---|
| 1 | ①②③ 无上界时间窗（expectedTime≥今日）单次全量，服务端四次串行查询放大 | 与 queryByPlan 单查同量级（agenda 四查拆分前的池A 就是这个形态）；taskType 列条件已窄化各自命中量；上线前 EXPLAIN 核实 supplier_plan (task_type, expected_time) 索引 |
| 2 | ④−③ 集合差扣减口径变化：由「批查C 全量 plans 扫送货行存在性」改为「③池货单号集」 | 语义差异：批查C 判「任意送货行」（含 expectedTime<今日 的历史送货行），集合差只扣「明日送货」——用户定稿采后者（"直接把明日送货的货单删掉"）；历史送货行不在 ③ 窗内的单会留在在库未送，属口径放宽，若需对齐旧口径再补 |
| 3 | 四池出参总量 = 四次查询行集和，单次 HTTP 响应体放大 | 量级同 agenda 四查分别返回的总和，无净增；feign 读超时配置需容四个池总和 |

## 9、Check List

- [ ] Apollo 配置：无新增/变更（staleDays 由 agenda Apollo 透传入参，starlord 侧只给 null 默认值 30）
- [ ] SQL 变更：无表结构变更；上线前 EXPLAIN supplier_plan (task_type, expected_time) 组合索引（风险 #1）
- [ ] 上线顺序：starlord 本方案（api jar + 服务）→ agenda 升 jar 切聚合接口（见 agenda 方案更新）→ 测试环境联调四板块对账 → 上线
- [ ] 回滚方案：新增接口无存量调用方，revert 单 commit 即净回滚；agenda 回退到四查直调 queryByPlan 形态可独立回滚
- [ ] 测试要点：①四池各自判据正确（正单/返补分 flowType、送货 taskType=5、在库 factTime 窗）；②①池不含已实际进仓行（factTime 1900 占位后滤）；③④池不含③池货单（集合差生效）+ orderNo 空行保留；④staleDays null 默认 30 / 传参覆盖；⑤出参行结构与 queryByPlan 一致（plans 8 字段）

## 10、评审会议纪要

（待评审）
