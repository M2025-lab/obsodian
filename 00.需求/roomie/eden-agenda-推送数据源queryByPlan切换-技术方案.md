# eden-agenda-推送数据源queryByPlan切换-技术方案

> 飞书文档：<链接占位，用户有则填>

## 1、背景

[[eden-agenda-推送数据源queryProgress迁移-技术方案]] 已把日报推送取数源迁到 starlord `queryProgress`（分页翻全量 + 单号分批 + 护栏三件套封装）。starlord 侧随后把 `query-by-plan` 扩成了**通用接口**（[[starlord-queryByPlan单号条件扩展-技术方案]]：orderNos/projectOrderIds 单号条件 + plan 条件 AND 落同一 plan 行，出参 plans 元素与旧接口 8 字段同构，无分页一次全量返回）。agenda 三段拉取（池A/池B 时间窗、批查C 纯单号）全部可被新接口表达，本方案把 `SupplierProgressManager#query` 换源到新接口并**删掉全部分页配套**（不翻页、不分批、不护栏——用户定稿 2026-11-27：根本用不上）。

## 2、需求理解

| # | 需求点 | 说明 |
|---|---|---|
| 1 | 取数换源 | `queryProgress`（分页）→ `queryProgressByPlan`（无分页一次全量） |
| 2 | 删分页配套 | 翻页循环、单号 500 分批、20000 护栏全删，Manager 变单次直调 |
| 3 | 判据零改动 | 板块判据全部 plan 行级（starlord 出参 plans 元素已补齐 flowType/remark，与旧接口同构） |

## 3、现状

现取数链路（老接口分页三件套）：

```
GET /api/v1/open/plan-push/{analyze,push,push-image}     DailyPushPlanController.java:49,60,72
  └─ DailyPushPlanAnalyzeServiceImpl.analyze()           :73
       ├─ 池A  manager.query(expectedTimeFrom=today)                    :81
       ├─ 池B  manager.query(factTimeFrom=staleStart, factTimeTo=昨天)   :86
       ├─ 批查C manager.query(orderNos=池B货单号集)                     :99
       ├─ Assembler 四板块组装（plan 行级判据）                          :115-118
       └─ 大区批查/分桶/enrich（不动）
```

Manager 三件套（`SupplierProgressManagerImpl`）：

| 配套 | 位置 | 语义 |
|---|---|---|
| `PAGE_SIZE=2000` 翻页 | :35、queryAll :68-96 | 老接口服务端 pageSize 上限 2000，按 total 翻页拉全量 |
| `ORDER_IN_BATCH=500` 分批 | :38、:55-63 | 老接口 orderNos IN 上限 500，超限切批每批独立翻页 |
| `TOTAL_GUARD=20000` 护栏 | :41、:71-74 | 第一页 total 超 2 万抛错（条件失效防呆） |

配套辅助方法：`withPage`/`copyOf`（分页/分批条件拷贝）、`invoke`（feign 调用 + 异常口径）。

出参消费对照（agenda 已消费字段 → 新接口对应）：

| 消费点 | 旧 `SupplierProgressPlanVO` | 新 `SupplierProgressByPlanVO` |
|---|---|---|
| Assembler 判据 plan.getTaskType/getFlowType/getExpectedTime/getFactTime/getRemark | plans 元素（`SupplierProgressPlanItemVO` 8 字段） | plans 元素（`SupplierProgressByPlanItemVO` 8 字段，同构已补齐） |
| Assembler toRow row.getProjectOrderId/getOrderNo/getSupplierName/getSupplierCode/getMaterialName/getMaterialCode | 行本体直取 | 同名字段直取 |
| Analyze 批查C row.getOrderNo | 行本体 | 同名 |
| 未消费（新接口砍掉） | taskType（当前阶段列）/supplierOrderNo/waybillNo/progressCode/progressDetail | progress（齐了/没齐标签）——无消费点，无影响 |

## 4、详细设计

> ⚠️ 需求与代码现状不一致：现行 Manager 三件套（翻页/分批/护栏）围绕老接口分页契约搭建；用户定稿 2026-11-27 **全部不要**——新接口无分页一次全量，直调即得全量。以需求为准，配套全删。

### 4.1 Manager 换源重写（SupplierProgressManagerImpl）

方法签名更换（接口 + 实现同步）：

```
旧：List<SupplierProgressPlanVO>      query(SupplierProgressQueryParam param)
新：List<SupplierProgressByPlanVO>     queryByPlan(SupplierProgressByPlanQueryParam param)
```

实现（全量代码形态）：

```
queryByPlan(param)
  ├─ param == null → new ByPlanQueryParam()（空入参；服务端守卫会拦：至少一个条件）
  ├─ feign.queryProgressByPlan(param) 单次调用（不翻页、不分批、不护栏）
  ├─ 异常口径（保留，对齐 ConstructionManagerImpl）：
  │    result == null → 抛 ERROR_INVOKE "调用供应商货单进度查询返回为空"
  │    !result.isSuccess() → 抛 ERROR_INVOKE(result.getMessage())
  │    调用异常 catch → log.error + 抛 ERROR_INVOKE(e)
  └─ return result.getData().getProgressList()（null 兜底空列表）
```

**删除**：`PAGE_SIZE`/`ORDER_IN_BATCH`/`TOTAL_GUARD` 三常量（:35-41）、`queryAll` 翻页循环、`query` 方法（含单号分批）、`withPage`/`copyOf` 辅助、`SupplierProgressPageVO` 相关 import。

**保留**：`invoke` 的异常口径三段式（调用纪律非分页配套）。

**行为变化（写死在方案）**：批查C 货单集 >500 时不再客户端切批，直接透传——starlord 入参校验 `limitIn` 超限报 IllegalArgumentException（服务端 4004），由调用方感知量级异常。池B 30 天窗口正常量级 <100 货单，500 上限余量充足。

### 4.2 Analyze 三段拉取适配（DailyPushPlanAnalyzeServiceImpl:81-101）

三段拉取字段名映射（语义不变）：

| 段 | 旧入参 | 新入参 |
|---|---|---|
| 池A | `expectedTimeFrom(today)` | `expectTimeGreaterThanOrEqualTo(today)` |
| 池B | `factTimeFrom(staleStart)`、`factTimeTo(yesterdayEnd)` | `factTimeGreaterThanOrEqualTo(staleStart)`、`factTimeLessThanOrEqualTo(yesterdayEnd)` |
| 批查C | `orderNos(candidateOrderNos)` | `orderNos`（同名直传） |

行类型同步：`List<SupplierProgressPlanVO>` → `List<SupplierProgressByPlanVO>`（池A/池B/批查C 三处局部变量 + dispatchedCandidates 循环 :105-112）。

批查C 已排送判据（:107 `plan.getTaskType() == 5`）：新接口单号单查场景 plans 为**全部** plan 行（starlord 方案 4.2 组合语义表），判「有无送货行」语义成立，逻辑零改动。

### 4.3 Assembler 类型替换（DailyPushPlanAssembler）

- 方法签名入参类型：`List<SupplierProgressPlanVO>` → `List<SupplierProgressByPlanVO>`（assembleMainOrderInbound :55、assembleStockUnsent :87、assembleReworkArrival :115、assembleTomorrowDelivery :135）
- plans 元素类型：`SupplierProgressPlanItemVO` → `SupplierProgressByPlanItemVO`（:59、:92、:118、:138 及 toRow 签名 :157）
- **判据/取值逻辑零改动**：getter 名全部同名（getTaskType/getFlowType/getExpectedTime/getFactTime/getRemark/getOrderNo/getProjectOrderId/getSupplierCode/getSupplierName/getMaterialCode/getMaterialName）

### 4.4 无改动部分

- `DailyPushPlanController` 三入口（编排签名不变）
- 板块判据（四板块判据全部 plan 行级，出参同构后无需任何调整）
- 大区批查/分桶/renumber/enrich/渲染链路
- `planpush.stale-days` 等 Apollo 配置

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|
| `GET /api/v1/open/plan-push/analyze` | DailyPushPlanController.java:49 | 无代码改动：analyze 内部取数换源（DailyPushPlanAnalyzeServiceImpl.java:81-101 三段拉取换 queryByPlan + 字段名映射），入参出参零变化 |  |
| `POST /api/v1/open/plan-push/push` | DailyPushPlanController.java:60 | 无代码改动：消费 analyze 结构不变 |  |
| `POST /api/v1/open/plan-push/push-image` | DailyPushPlanController.java:72 | 无代码改动：同上 |  |
| 新建内部方法 `SupplierProgressManager#queryByPlan`（非 HTTP 入口） | eden-agenda-manager 模块 | 换源重写 + 三件套删除（4.1）；改动入口唯一消费方 DailyPushPlanAnalyzeServiceImpl |  |

入口行为变化（入参出参零变化）：无（三段拉取语义不变，板块行集等价——池A/池B 单时间窗条件下新旧接口行为等价，批查C 单号单查 plans 全量行为一致）。

## 6、上游依赖

| 依赖 | 内容 | 卡上线 |
|---|---|---|
| `com.ke.utopia:edar-starlord-api`（须为含 queryByPlan 单号扩展的版本） | `SupplierProgressFeign#queryProgressByPlan`（SupplierProgressFeign.java:63-65）+ `SupplierProgressByPlanQueryParam`（含 orderNos/projectOrderIds）+ `SupplierProgressByPlanQueryVO`/`SupplierProgressByPlanVO`/`SupplierProgressByPlanItemVO`（8 字段版） | **是——starlord 先 install SNAPSHOT 联调、上线前发正式版**；当前本地 m2 的 2.7.6-SNAPSHOT 不含本轮新字段 |
| starlord 服务部署 queryByPlan 通用版 | 测试/线上数据源 | 是（与发版同窗口） |

## 7、表结构改动

无（agenda 内存态不落库）。

## 8、风险评估

| # | 风险点 | 评估 |
|---|---|---|
| 1 | 无护栏后池A 全量放大：`expectTimeGreaterThanOrEqualTo=今日` 无上界窗，远期单全捞、一次单次返回 | 量级依赖线上核查（旧方案风险 #10 同口径遗留）；护栏已按用户定稿删除，若线上量级失控再补护栏属回退动作 |
| 2 | 批查C 货单集 >500 直接 4004（不再客户端分批） | 池B 30 天窗口正常量级 <100，余量充足；超限报错由调用方感知，属显式失败优于静默切批 |
| 3 | Assembler 类型替换的编译面 | 纯类型替换 + getter 同名，编译期全量暴露，无运行时风险 |

## 9、Check List

- [ ] Apollo 配置：无新增/变更（`planpush.stale-days` 既有，不动）
- [ ] SQL 变更：无（agenda 不落库）
- [ ] 上线顺序：starlord 发版（api jar + 服务，见 [[starlord-queryByPlan单号条件扩展-技术方案]] Check List）→ agenda 升 jar → 联调（测试环境三段拉取跑通四板块）→ 上线 → 双跑对账期（沿用旧方案纪律：对账期只 analyze 不 push）
- [ ] 回滚方案：agenda 侧 revert 单 commit（Manager/Analyze/Assembler 三文件）；starlord 侧改动独立无耦合可单独回滚
- [ ] 测试要点：①池A/池B 时间窗拉取结果与旧接口等价（同日双跑 diff）；②批查C 单号单查返回全部 plan 行、已排送判据不漏判；③三入口 analyze 入出参回归；④货单集 >500 显式报错路径
- [ ] TODO（与产品/QA 确认）：①新接口出参 `progress` 为「齐了/没齐」标签，旧接口 `progressCode` 三态（PREPARING/INCOMPLETE/COMPLETE）——agenda 现无消费点，若后续板块要按齐件三态细分属新需求；②池A 无上界窗线上量级核查（护栏删除后唯一兜底是人工观察）

## 10、评审会议纪要

（待评审）
