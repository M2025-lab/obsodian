# eden-agenda-推送数据源queryProgress迁移-技术方案

> 飞书文档：<链接占位，用户有则填>

## 1、背景

日报推送接口路径（plan-push）已按 [[eden-agenda-推送数据源queryPlan迁移-技术方案]] 落地，数据源为 starlord `TaskSupplierFeign#queryPlan`（表 `task_supplier_plan`）。starlord 侧台账演进：`task_supplier_plan` 拆成 `supplier_progress`（关系：一货单一行）+ `supplier_plan`（时间：一货单多任务多版本）两张表，对外合并接口换为 `SupplierProgressFeign#queryProgress`（`/task-supplier/progress/query`，feat/50559773/suppliertime 分支已交付）。agenda 侧 `DailyPushPlanAnalyzeServiceImpl` 取数逻辑需按新契约重写；working tree 已有一版迁移半成品，取数与时间定位姿势错误，本方案在纠正口径后重写。

## 2、需求理解

| # | 需求点 | 说明 |
|---|---|---|
| 1 | 取数换源 | `TaskSupplierFeign#queryPlan` → `SupplierProgressFeign#queryProgress`（返回 rows=货单行，时间在 `row.plans` 列表） |
| 2 | 时间定位 | 板块要的实际时间（进仓/入仓）必须从 `row.plans` 里按 taskType 定位对应记录取，不能在 row 本体取 |
| 3 | 翻页护栏 | 保留老 `TaskSupplierPlanManager` 的翻页全量拉取 + 总量护栏能力（随迁移已删，需重建；页间防漂移不保留） |

用户定稿口径（2026-11-23）：

| 口径 | 结论 |
|---|---|
| 服务端类型过滤 | 四查**不传** taskType/flowType，板块判据全部在 agenda 侧按 plans 精滤（progress.task_type 是「当前阶段」语义，服务端过滤会漏行） |
| 板块行粒度 | 按 plan 行炸开：一货单的 N 条满足判据的 plan → N 个板块行（多次返补多版本保留多行） |
| 翻页 | 移植老翻页逻辑（pageSize=2000 翻全量 + 护栏；防漂移校验不移植） |


## 3、现状

接口路径现状链路（working tree 半成品，未提交）：

```
GET/POST /api/v1/open/plan-push/{analyze,push,push-image}   DailyPushPlanController.java:33
  └─ DailyPushPlanAnalyzeServiceImpl.analyze()
       ├─ 取数：supplierProgressFeign.queryProgress(...) 五次调用          ← ★ 本方案重写点
       │    ①正单进仓(taskType=14,flowType=0,expectedTime≥今日)
       │    ②在库候选(taskType=14,factTime∈[今日-30天,昨日])
       │    ③返补进仓(taskType=14,flowType=1,expectedTime≥今日)
       │    ④送货(taskType=5,expectedTime≥今日)
       │    ⑤批查(②的orderNos集,taskType=5)
       ├─ DailyPushPlanAssembler 四板块组装 ← ★ 取时间姿势错误（见 3.3）
       ├─ ConstructionManager.queryRegionByProjectOrderIds 大区分组
       ├─ 分桶 bucket() + renumber
       └─ DailyPushPlanEnricher.enrichPersonNames（订单 API 批查人员三列）
```

### 3.1 新契约核心事实（starlord 侧）

| 事实 | 代码依据 |
|---|---|
| rows 一行 = 一个货单（`supplier_progress` 一货单一行，orderNo 身份键） | SupplierProgressServiceImpl.java:366 progressKey 注释 |
| 时间不在 row 本体，全挂 `row.plans`：一个货单按 taskType 各一条（5送货/8备货/14进仓），多次返补同 taskType 多版本，taskType 升序 | SupplierProgressPlanVO.java:67-70、SupplierProgressServiceImpl.java:239-289 |
| `factTime` 只有备货(8)/进仓(14) 有值；1900 占位出参转 null | SupplierProgressPlanItemVO.java:45、SupplierProgressServiceImpl.java:477 validTime |
| **progress.task_type 不是关系键，是「当前阶段」**——sync 写新阶段覆盖旧值，历史在 plan 表按 orderNo+taskType 存全量 | SupplierProgressServiceImpl.java:360-368 |
| 服务端时间窗过滤**同步过滤 plans 列表**（传了时间窗只回落在窗内的 plan 行） | SupplierProgressServiceImpl.java:291-313 toPlanItems/matchTimeRange |
| 服务端 taskType/flowType 过滤的是 progress 的「当前阶段」列 | SupplierProgressMapper.xml:94-99 |
| 分页默认 500 上限 2000（超限报错）；无 orderBy 参数（仅 `order by id asc`） | SupplierProgressQueryParam.java:40-42、SupplierProgressMapper.xml:134 |
| **三个单号条件都不传 → 静默返回空页**（守卫，用户口径：后续下掉） | SupplierProgressServiceImpl.java:207-213 |

### 3.2 与老接口（queryPlan）的契约差异

| 维度 | 老 queryPlan（已迁移走） | 新 queryProgress |
|---|---|---|
| 行粒度 | 一行 = 一个任务实例（orderNo+flowType+taskType） | 一行 = 一个货单，plans 挂全部计划行 |
| 时间字段 | row.expectedTime / row.factTime 直接取 | 在 row.plans 里按 taskType 定位取 |
| 类型过滤语义 | 行本身就是任务实例，服务端过滤精确 | 服务端过滤命中「当前阶段」列，**非全量任务实例，会漏行** |
| 分页 | pageNo/pageSize/totalPages/orderBy | pageNo/pageSize/total，无 orderBy |
| 单号守卫 | 无 | 三单号全空 → 空页（守卫待下掉） |

### 3.3 working tree 半成品三处硬伤（本方案重写范围）

| # | 位置 | 问题 |
|---|---|---|
| 1 | DailyPushPlanAssembler.java:43（assembleMainOrderInbound） | 取 plans 里 taskType 匹配行做剔除后，`toRow(..., factTime)` 传的是**剔除逻辑后恒为 null** 的 factTime → 正单板块日期全空；且 `findFirst().get()` 无空兜底，plans 为空直接 NoSuchElement 异常 |
| 2 | DailyPushPlanAssembler.java:80/91/100 | `row.getPlans().get(0)` 取时间——plans 是全部 taskType 升序列表，`get(0)` 大概率取到送货行（taskType=5 排最前）的时间，不是板块要的进仓/返补那条 |
| 3 | DailyPushPlanAnalyzeServiceImpl.java:81-112 | 四查仍传 taskType/flowType 服务端过滤（「当前阶段」语义，漏行）+ 只取第一页 500 条不翻页（超量静默丢行）+ 沿用 `getData().getRows()` 无 null/success 校验 |


## 4、详细设计

> ⚠️ 需求与代码现状不一致：原方案四查口径（taskType/flowType 服务端过滤）基于老接口「行=任务实例」契约；新接口 progress.task_type 是「当前阶段」列，服务端类型过滤命中错位语义会漏行。以新契约为准，本方案四查不传类型条件，板块判据全部客户端精滤（用户定稿 2026-11-23）。

### 4.1 取数：五次拉取收敛为三次（两池 + 一批查）

服务端只传时间窗/单号（粗筛，不传类型条件），板块判据客户端精滤：

| 拉取 | 服务端条件 | 用途 |
|---|---|---|
| 池A | `expectedTimeFrom = 今日00:00`（翻页全量） | ①正单进仓 ③返补 ④送货（①③④判据只差类型/flow 条件，池化一次拉取） |
| 池B | `factTimeFrom = 今日-30天`、`factTimeTo = 昨日23:59:59`（翻页全量） | ②在库候选 |
| 批查C | 仅 `orderNos = 池B行orderNo集`（翻页全量，**不传时间窗不传类型**） | ⑤判「有无送货行」（taskType=5 的 plan 行存在即已排送） |

三个口径依据：
- **池A时间窗与板块判据同向**：服务端 `toPlanItems` 同步过滤 plans（SupplierProgressServiceImpl.java:291-313），只回落在窗内的 plan 行；①③④判据本就是「今日之后」，被滤掉的正是判据要排除的行，无副作用。
- **池B plans 不做跨类型判断**：池B的 factTime 窗会把 plans 里送货行滤掉（送货行 factTime 恒 null 不落窗）——池B的 plans 只用于进仓行取值，送货存在性判断走批查C（全量 plans 不受窗污染）。
- **批查C不传 taskType**：判「该货单有没有送货行」须看 plan 行存在性，而 progress.task_type=5 只是「当前阶段=送货」（SupplierProgressServiceImpl.java:360-368），阶段已推进的货单送货 plan 行仍在；传类型会漏判。不传时间窗：送货行 expectedTime < 今日 的也该命中「已排送」语义。

### 4.2 板块判据（客户端精滤，按 plan 行炸开）

板块行粒度 = plan 行（用户定稿）：一货单的 N 条满足判据的 plan → N 个板块行。判据全部作用于 `SupplierProgressPlanItemVO`（plans 列表元素）：

| 板块 | 判据（plan 行级） | planDate 取值 |
|---|---|---|
| ①正单预计入库 | `taskType=14 ∧ flowType=0 ∧ expectedTime ≥ 今日00:00 ∧ factTime == null` | 该 plan 行 expectedTime |
| ②在库未送 | `taskType=14 ∧ factTime ∈ [今日-30天, 昨日]`（服务端已滤）∧ 池B货单**无任何** taskType=5 plan 行（批查C未命中） | 该 plan 行 factTime（到库时间） |
| ③返补预计到工地 | `taskType=14 ∧ flowType=1 ∧ expectedTime ≥ 今日00:00` | 该 plan 行 expectedTime |
| ④送货计划 | `taskType=5 ∧ expectedTime ≥ 今日00:00`（服务端已滤） | 该 plan 行 expectedTime |

客户端精滤补充规则：
- ①剔除 factTime 有值行（已实际进仓的不算「预计」——服务端无该组合过滤条件，客户端兜底，原方案口径保留）
- ②在库候选行若同货单 plans 里出现 taskType=5 行 → 整单剔除（已排送）——但池B的 plans 被时间窗过滤不可用于此判断，用批查C结果按 orderNo 判
- orderNo 空的行无关联键，不参与②的批查C剔除（留在库未送板块，原口径）
- 1900 占位已由服务端 validTime 转 null，客户端无需再判

### 4.3 Manager 翻页封装（重建 SupplierProgressManager）

新建 `SupplierProgressManager`（manager 模块，替换已删的 `TaskSupplierPlanManager` 同款能力）：

```
List<SupplierProgressPlanVO> query(SupplierProgressQueryParam param)
```

翻页实现（移植老 TaskSupplierPlanManagerImpl 两件套）：
- pageSize=2000（服务端上限）翻页拉全量，按 total 算页数逐页取
- 总量护栏：命中 total > 20000 抛错（防条件失效查成全表——单号守卫下掉后此护栏是唯一防线）
- 单页调用异常口径对齐 ConstructionManagerImpl：null 返回/非 success/调用异常全抛 UtopiaBussinessException
- 分批调用：单号 IN 上限 500（服务端 limitIn，SupplierProgressServiceImpl.java:505）——批查C的 orderNos 集 >500 时按 500 分批调用后合并（**顺序：先翻页后分批，每批独立翻页**）
- 不做页间防漂移校验（用户定稿 2026-11-23：offset 翻页漂移基本不会出现；老逻辑的 id 单调校验不移植）

### 4.4 Assembler 重写（时间定位 + plan 行炸开）

`DailyPushPlanAssembler` 四个组装方法签名统一改为「货单行集 + 板块判据 → 板块行集」：

```
assembleMainOrderInbound(List<SupplierProgressPlanVO> rows)   // 池A行集
    for row → for plan in row.plans:
        plan.taskType==14 && plan.flowType==0 && plan.expectedTime>=今日 && plan.factTime==null
        → toRow(row, plan, SECTION_ORDER, plan.expectedTime)

assembleStockUnsent(List<SupplierProgressPlanVO> 池B行集, Set<String> 已排送orderNo集(批查C命中集))
    for row → for plan in row.plans:
        plan.taskType==14（factTime 服务端已窗滤）∧ row.orderNo ∉ 已排送集
        → toRow(row, plan, SECTION_STOCK, plan.factTime)

assembleReworkArrival / assembleTomorrowDelivery 同款：for plan 双层循环 + 判据
```

时间定位修正（3.3 硬伤 1/2 的解法）：**planDate/remark 直接取命中的那条 plan 行**（`toRow(row, plan, section, planDate)` 增 plan 参数），不再 `plans.get(0)` 不再取错源；正单板块 expectedTime 取判据行自身的 expectedTime（修硬伤 1 的「恒 null」）。

### 4.5 AnalyzeServiceImpl 编排重写

```
analyze()
  ├─ today/yesterdayEnd/staleStart 日期窗计算（保留）
  ├─ 池A = manager.query(expectedTimeFrom=today)                    // 翻页全量
  ├─ 池B = manager.query(factTimeFrom=staleStart, factTimeTo=yesterdayEnd)
  ├─ 批查C = manager.query(orderNos=池B.orderNo集)                  // >500 分批
  ├─ 已排送集 = 批查C行集里「存在 taskType=5 plan 行」的 orderNo 集
  ├─ orderBoard   = assembler.assembleMainOrderInbound(池A)
  ├─ stockBoard   = assembler.assembleStockUnsent(池B, 已排送集)
  ├─ reworkBoard  = assembler.assembleReworkArrival(池A)
  ├─ deliveryBoard = assembler.assembleTomorrowDelivery(池A)
  ├─ 大区批查/分桶/renumber/enrich（现状链路不动，DailyPushPlanAnalyzeServiceImpl.java:120-170）
  └─ 返回 Map<大区code, DailyPushReportVO>
```

- Feign 直调换 Manager 封装：`supplierProgressFeign` 字段删除，注入 `SupplierProgressManager`（异常/翻页/护栏收敛在 Manager，Analyze 只管编排）
- 删除 TASK_TYPE_ENTER/TASK_TYPE_DELIVERY/FLOW_TYPE_MAIN/FLOW_TYPE_REWORK 服务端过滤常量的查询入参用途（常量本身保留，判据客户端用）
- 日志：完成行数统计加「池A/池B/批查C」三段拉取量级 + 各板块行数（对账期双跑对比用）

### 4.6 无改动部分

- `DailyPushPlanController`（三入口零改动：编排链路签名不变）
- `DailyPushPlanPushServiceImpl`（analyze 返回结构不变）
- `DailyPushPlanEnricher`（消费 DailyPushRowVO，不受取数改动影响）
- `DailyPushRegionBuckets` / 渲染器 / RoomiePushService（共享层不动）
- Excel 路径（excelpush 包）零改动

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|
| `GET /api/v1/open/plan-push/analyze` | DailyPushPlanController.java:46 | 无代码改动：编排链路签名不变，analyze 内部取数换 Manager + 判据精滤（DailyPushPlanAnalyzeServiceImpl.java:74 重写） |  |
| `POST /api/v1/open/plan-push/push` | DailyPushPlanController.java:57 | 无代码改动：消费 analyze 结构不变 |  |
| `POST /api/v1/open/plan-push/push-image` | DailyPushPlanController.java:69 | 无代码改动：消费 analyze 结构不变 |  |
| 新建 `SupplierProgressManager`（内部 Manager，非 HTTP 入口） | eden-agenda-manager 模块 | 翻页/护栏封装（老 TaskSupplierPlanManagerImpl 能力移植，4.3 节） |  |

三入口行为变化（入参出参零变化）：analyze 板块行粒度从「任务实例行」变为「plan 行」（多次返补多版本逐行展示）；正单板块行集 = plans 精滤结果（含「阶段已推进但计划满足判据」的此前漏掉的货单）。

## 6、上游依赖

| 依赖 | 内容 | 卡上线 |
|---|---|---|
| `com.ke.utopia:edar-starlord-api:2.7.6-SNAPSHOT`（pom.xml:183，已生效） | `SupplierProgressFeign#queryProgress`（SupplierProgressFeign.java:27）+ 请求/响应 DTO（SupplierProgressQueryParam / SupplierProgressPageVO / SupplierProgressPlanVO / SupplierProgressPlanItemVO） | jar 已含全部类（m2 已核 2026-11-23 构建）；SNAPSHOT 版上线前需 starlord 发正式版 |
| starlord 测试环境 queryProgress 接口 | 按单号守卫（SupplierProgressServiceImpl.java:207-213）——**agenda 三次拉取（池A/池B无单号）当前会被守卫拦截返回空页** | **卡联调与上线**：守卫下掉必须先行（见 9 上线顺序） |

> 用户口径（2026-11-23）：单号守卫「后续要下掉」——属 starlord 侧改动，不进本方案详细设计；agenda 侧依赖其下掉后才能全链路跑通，故列入上线顺序前置条件。

## 7、表结构改动

无（agenda 内存态不落库；starlord 侧表已建）。

## 8、风险评估

| # | 风险 | 评估 |
|---|---|---|
| 1 | 服务端查询量放大：池A不传类型条件，行集从「进仓行 ~4471」变为「今日之后有任一计划的全部货单」——含备货行关联货单，日窗内量级待观察（首查日志有 total 护栏 20000 兜底，超限抛错不静默） | 量级可控（测试环境 11-19 实测全表 16502 行、日窗进仓仅个位数；护栏兜底） |
| 2 | 客户端精滤内存量：一货单 plans 全量带出（≤6 行/货单典型），池化 20000 行护栏内 plans 总量 ~10 万级对象，无压力 | 无风险 |
| 3 | 翻页拉取由 5 次单页调用变为 3 次×N 页串行调用，单次 analyze 端到端耗时增加（每次翻页 ~百 ms 级） | 日报推送无时效敏感性（定时低峰执行），可接受 |

## 9、Check List

- [x] Apollo 配置：**无新增/变更**（`planpush.stale-days` 既有保留，30 天护栏口径不变）
- [x] SQL 变更：无（agenda 不落库）
- [ ] 上线顺序：
  1. starlord 下掉 queryProgress 单号守卫（SupplierProgressServiceImpl.java:207-213）**先行**（守卫在时池A/池B静默空页，板块全空）
  2. starlord 发正式版 api 包（2.7.6-SNAPSHOT → 正式版），agenda 升 pom.xml:183 依赖
  3. agenda 侧本方案代码上线（对账期双跑：analyze 对照 Excel 路径，只 analyze 不 push）
- [ ] 回滚方案：revert agenda 侧本方案改动即回到 queryPlan 版（接口入口/VO 零变化，回滚成本为单次 revert）；Excel 路径判据从未动过零回滚成本
- [ ] 对账口径预期差异：板块行粒度 plan 行 vs 任务实例行（多次返补多版本逐行）；正单板块多出「阶段已推进但计划在窗内」的货单（此前服务端类型过滤漏掉的行）——双跑对账按此预期评估
- [ ] TODO（待与 starlord 确认）：单号守卫下掉排期（6 节）
- [ ] TODO（待与用户确认）：对账期长短（沿用原方案「差异收敛 + 一个完整月度推送周期」预估）

## 10、评审会议纪要

| 项 | 内容 |
|---|---|
| 参会人员 |  |
| Todo List |  |
