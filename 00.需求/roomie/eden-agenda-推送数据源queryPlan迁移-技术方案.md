---
tags:
  - 需求
  - 云助理
  - Roomie
date: 2026-11-19
status: draft
source:
  - "[[00.需求/roomie/项目过滤/eden-agenda-大区分组分析-技术方案]]"
  - "[[00.需求/roomie/eden-agenda-Excel推送链路迁移-技术方案]]"
  - "[[20.知识沉淀/📚 常用接口手册]]"
---

# eden-agenda-推送数据源 queryPlan 迁移-技术方案

> 目标：与 Excel 路径**并行**新增一条接口数据源路径——starlord `com.ke.utopia.api.TaskSupplierFeign#queryPlan`（表 `task_supplier_plan`）→ 四板块分析（含一直缺失的「送货计划」）→ 大区分组 → 推送，**对外独立 Controller 暴露**；Excel 路径零改动保留，双跑对账稳定后退役。

## 1、背景

三板块推送链路（在库未送 / 正单预计入库 / 反补预计到工地）已上线多轮迭代（`b12fc51`），数据源是 kdocs 两份 clean 文件（`KdocsDownloadService` 定时下载，cookies 失效断供、99MB 文件解析、表头漂移都是持续维护面）。云助理侧以 caorui037 为主在 starlord 落了 `task_supplier_plan` 台账（供应商配送时间计划，`feat/50559773/suppliertime` 分支），把排线/入库等进展数据结构化成接口 `TaskSupplierFeign#queryPlan`（2026-11-17/18 交付，测试环境已可调）。

台账数据与 Excel 数据**同源**（都来自供应商同步），结构化接口替代 Excel 解析后：文件下载链路、表头漂移、99MB 解析内存全部退役；「送货计划」板块（排线表人工入口，从未接入）由送货行免费补齐。

**架构决策：并行双路径、双 Controller 对外暴露**（用户定稿口径，替代单链路开关切换）——Excel 路径零改动，接口路径新建独立 Controller；不共用取数入口、不做运行时开关。理由：① 对账期两个 URL 天然可比（同一天各跑一次，输出并排评审），不依赖开关切换的时序纪律；② 接口路径上线后 Excel 路径是否退役是独立决策，删路径 = 删代码 + 删 URL，无「开关忘了切」的暗坑；③ 两条路径共享渲染/推送/大区编排（复用在 service 内部），Controller 只薄封装——分叉成本只有取数层。

## 2、需求理解

| # | 需求点 | 口径 |
|---|---|---|
| 1 | **接口路径独立成链** | queryPlan 取数 → 四板块组装 → 大区分组 → 渲染推送，整条新链路落新 Controller（`/api/v1/open/plan-push/*`）；不与 Excel 路径共享取数代码，共享编排/渲染/推送 |
| 2 | **Excel 路径零行为改动** | 现有 `/api/v1/open/excel-push/*` 两个接口、`ExcelPushAnalyzeServiceImpl` 判据、kdocs 下载链路全部不动（不删代码、不加开关、不动判据）；共享类统一重命名（4.4 命名表）带来的只是 import 级机械变更，行为与 JSON 输出零变化 |
| 3 | **送货计划板块补齐** | 一直缺失的第四板块（排线表）由送货行（taskType=5）承载：`expectedTime 今天之后`（含当日、无上界，多日逐行展示）——板块名由「明日送货」调整为「送货计划」（待确认④；原 Excel 方案的 `DispatchSourceProvider` 入口抽象作废） |
| 4 | 业务口径对齐 | 四板块「哪些单该提醒」的业务语义与 Excel 版对齐，入板判据按台账字段重新定义（见 4.2） |
| 5 | **去重规则保留** | 原规则「出现在明日送货的项目，在库未送不再提醒」被新判据天然吸收——凡有送货行的单都不进在库未送（见 4.2），渲染层无需跨板块过滤 |
| 6 | 展示列不缩水 | 渲染文本的「供应商 / 品类 / 业主 / 工长」列在接口源下走 enrich 补齐（台账没有这些列） |
| 7 | 双跑对账 → 退役 | 对账期两路径并行运行输出对照；接口路径稳定一个考核周期后 Excel 路径退役（另期删代码，见 Check List） |

## 3、现状

### 3.1 推送链路现状（Excel 路径，本次零行为改动；类名为现状名，重命名映射见 4.4）

```
GET /api/v1/open/excel-push/analyze                ExcelPushController.java:44
  └─ ExcelPushAnalyzeServiceImpl.analyze()          ExcelPushAnalyzeServiceImpl.java:107
       ├─ 读两份 clean 文件三个 sheet（EasyExcel 无模型流式）
       ├─ 三板块过滤（在库未送/备货中+预计入库/≥今日）
       ├─ ConstructionManager.queryRegionByProjectOrderIds → Map<项目, 大区>（跟单范围即名单）
       ├─ 按大区分桶 + renumber + enrichReworkPersons（订单 API 补业主/工长）
       └─ 返回 LinkedHashMap<大区code, ExcelPushAnalyzeVO>
POST /api/v1/open/excel-push/push
  └─ ExcelPushPushServiceImpl.pushDailyReportOnce() → ExcelPushTextRenderer.renderRegionReport → RoomiePushService.pushToGroups
```

编排三段式（取数 / 公共编排 / 渲染推送）——接口路径替换第一段、复用后两段，是本方案的结构基础。

### 3.2 queryPlan 契约（edar-starlord-api，feat/50559773/suppliertime）

| 项 | 内容 |
|-|-|
| 入口 | `TaskSupplierFeign#queryPlan`（`@FeignClient("edar-starlord")`），POST `/task-supplier/plan/query` |
| 出参 | `TaskSupplierPlanPageVO{pageNo, pageSize, total, totalPages, list<TaskSupplierPlanQueryVO>}` |
| 行结构 | `id / projectOrderId / orderNo / taskDispatchId / waybillNo / supplierOrderNo / taskType / flowType / expectedTime / factTime / createTime / modifyTime / remark / status` |
| taskType | **8 备货 / 5 送货 / 14 进仓**（枚举同 `task_dispatch.task_type`；7 接单不属于本表） |
| flowType | **0 正单 / 1 返补** |
| 时间语义 | 备货行 expectedTime=备货预计；送货行 expectedTime=送货预计；进仓行 expectedTime=进仓预计 + **factTime=实际进仓**（备货/送货 factTime 恒 null） |
| 占位约定 | 库里 1900-01-01 = 没有时间，出参统一转 null；`taskDispatchId=0` = 无关联任务；`orderNo` 空串 = 上游没给货单号 |
| 分页 | pageNo 从 1，pageSize 默认 500 **上限 2000（超了报错不截断）**；orderBy 白名单 id/createTime/modifyTime/expectedTime/factTime |
| 一行 = | 一个货单下的一个具体任务实例，业务键 `orderNo+flowType+taskType+taskDispatchId`；同键可多行（一单多次返补） |

**测试环境实测**（2026-11-19，`skills/starlord-supplier-plan-test`）：全表 16502 行——送货行 7941 / 进仓行 4471 / 备货行 4090；正单 12437 / 返补 4065。**当日的预计进仓仅个位数行**（当日日期窗内 5 行），数据稀疏是常态（台账按供应商同步节奏写入，非每日全量）。

**首次联调对账实录**（2026-11-19，接口路径 analyze 全链跑通，直查测试库核因）：四板块判据全按设计工作（查① 7 行剔 5 剩 2 但项目不在跟单范围、查③ 0 行、查② 140→10 行、查④ 237→11 行；大区分组链路正确）。**enrich 全空为测试数据脱节，非代码问题**：台账（11-17~19 三天导入）4176 个项目在 `task_dispatch` 有任务的仅 1 个；13172 行引用的 `task_dispatch_id`（43xxxx 段）在 dispatch 表/bak 库/数仓表全部零命中（导入数据引用的疑为线上任务 id）；业主通道同因命中 0（Excel 路径同款调用也是命中 0）。**测试要出完整展示列，需用「跟单范围 ∩ 有任务」的 6 个真实项目造台账行**（826011217000000823 等，任务数 13~621）；线上正式数据两端同源，此问题不会重现。

### 3.3 依赖版本链（卡上线的硬依赖）

agenda 根 pom `dependencyManagement` 锁 `com.ke.utopia:edar-starlord-api:2.4.5`（pom.xml:183），该版本 jar **无 TaskSupplierFeign**（已核 m2 仓库 2.4.5 及 2.5.9/2.6.10/2.6.11 均无）。`TaskSupplierFeign` 首现在 starlord `feat/50559773/suppliertime` 分支的 `2.7.6-SNAPSHOT`（未发版）。上线前置：**starlord 侧发一版含 queryPlan 的正式 api 包，agenda 侧升版**。

## 4、详细设计

### 4.1 总体链路（双路径并行）

```
【Excel 路径（现状，零行为改动）】
GET/POST /api/v1/open/excel-push/{analyze,push}
  └─ ExcelPushController → ExcelPushAnalyzeServiceImpl.analyze()（重命名后 DailyPushExcelAnalyzeServiceImpl，见 4.4 命名表）
       ├─ 取数：kdocs 两份 clean 文件三 sheet 读取过滤
       └─ 编排：大区批查 → 分桶 → enrichReworkPersons → 渲染推送
                    ▲ 共享层（两路径复用同一套，路径中立命名，见 4.4 命名表）
                    │  DailyPushReportVO / DailyPushRegionBuckets / DailyPushTextRenderer / RoomiePushService

【接口路径（新建，本方案主体）】
GET  /api/v1/open/plan-push/analyze     → DailyPushPlanController（新）
POST /api/v1/open/plan-push/push         → DailyPushPlanController（新）
  └─ DailyPushPlanAnalyzeServiceImpl.analyze()（新，编排照抄 Excel 版结构）
       ├─ ★ 取数（新）：TaskSupplierPlanManager 五次拉取（服务端条件过滤 + 翻页，口径用户定稿）
       │     ① 正单进仓：taskType=14, flowType=0, expectedTime ≥ 今日
       │        （正单预计入库 板块的行来源）
       │     ② 在库候选：taskType=14, factTime ≤ 昨日（30 天下界护栏，见下）
       │        （在库未送 的候选行——实际进仓发生在今天之前）
       │     ③ 返补进仓：taskType=14, flowType=1, expectedTime ≥ 今日
       │        （返补预计到工地 板块的行来源）
       │     ④ 送货行：taskType=5, expectedTime ≥ 今日（今天之后含当日）
       │        （送货计划 板块的行来源，多日计划按行展示）
       │     ⑤ 送货关联批查：taskType=5, orderNos = ②候选行的单号集
       │        （判「查不到对应的送货任务」——② 剔除 ⑤ 命中的单，余下即在库未送）
       ├─ ★ 板块组装（新判据，见 4.2）：DailyPushPlanAssembler（新类：台账行 → DailyPushRowVO）
       ├─ 大区批查（复用）：queryRegionByProjectOrderIds → Map<项目, 大区>
       ├─ ★ 展示字段补齐（新，三板块公共，见 4.5）：DailyPushPlanEnricher
       └─ 分桶/renumber/渲染/推送（复用：DailyPushTextRenderer + RoomiePushService）
```

> 取数口径说明（用户定稿 2026-11-19）：四次板块查询 + 一次关联批查，全部服务端过滤；「今天之后」统一按**含当日**落地（From=当日 00:00:00，对齐返补板块 Excel 版 H 列 ≥ 今日 口径——当日未到的也是提醒对象），「今天之前」= To=昨日 23:59:59（当日刚进仓的先不提醒）。**在库未送不再自己定时间窗碰运气**：② 按 factTime 捞「今天之前实际进仓」的行，再拿这些行的单号批查送货行（⑤），查不到送货任务的才留在板块——原 Excel 版「明日送货 → 在库未送不再提醒」的去重规则在该判据下天然成立（有送货任务的单根本不进板块）。护栏：② 的 factTime 加 30 天下界防陈旧行（`planpush.stale-days`，待确认①）；①③④ 无上界（远期行量级靠 20000 护栏兜底，见风险 10）。

### 4.2 板块口径映射（核心）

Excel 版口径 → 台账字段口径的一一对应（业务语义「哪些单该提醒」对齐，判据字段全换；**此口径仅作用于接口路径，Excel 路径判据不动**）：

| 板块 | Excel 版判据（现状代码，不动） | 接口版判据（四次查询，用户定稿口径） | 说明 |
|---|---|---|---|
| **送货计划** | （从未接入，排线表缺失） | **查④**：`taskType=5` ∧ `expectedTime ≥ 今日`（含当日，无上界） | 送货行的 expectedTime 就是派车计划日；「今天之后」逐行展示（不只明日）。flowType 不限（正单返补都送） |
| **在库未送** | sheet D 列 = 「在库未送」 | **查②**：`taskType=14` ∧ `factTime ≤ 昨日`（实际进仓发生在今天之前）∧ **查不到对应的送货行**（查⑤按单号批查送货行，命中即剔） | 「进仓已发生（factTime 有值）+ 送货任务不存在」拼出在库未送；有送货行的单说明已排送——原「明日送货去重」规则被天然吸收 |
| **正单预计入库** | 订单状态=备货中 ∧ E 列含「预计入库」 | **查①**：`taskType=14` ∧ `flowType=0`（正单）∧ `expectedTime ≥ 今日` | 台账无订单状态列；「备货中+预计入库」≈ 进仓预计时间已给且还没到。查①结果中 factTime 已有值的行是已进仓（实际罕见，服务端无该组合过滤条件，客户端剔除兜底） |
| **返补预计到工地** | H 列（预计到工地）≥ 今日 | **查③**：`taskType=14` ∧ `flowType=1`（返补）∧ `expectedTime ≥ 今日` | 返补行直接按 flowType 区分，无需状态判断（原口径「已扣款不剔」保留：台账无金额字段，天然不剔） |

**「在库未送」判据细则**（最复杂的板块，单独交代）：

- 候选行（查②）：`taskType=14` + `factTime` 有值且 **在今天之前**（当日刚进仓的不提醒，明日再看）；factTime 加 30 天下界护栏（`planpush.stale-days`，超期陈旧行不再提醒，待确认①）。
- 送货任务存在性（查⑤）：拿②候选行的 `orderNo` 集合走 `orderNos` 批查 `taskType=5` 行——**查得到任何送货行的单 = 已排送（无论 scheduled 未来还是过去）→ 从在库未送剔除**；查不到的留在板块。orderNo 空串的行退化用 `projectOrderId` 关联（同项目有送货行即视为已排送）。
- 送货行本身没有 factTime（送货完成不回写本表），「已送完成」不可从台账单独判断——**送货完成态依赖任务系统**（`taskDispatchId` 关联 task_dispatch 的 state，本期不做，见风险 3）。这是接口版与 Excel 版口径**已知不可完全对齐**的点（Excel 版 D 列「在库已送」自愈，接口版靠 30 天护栏兜），双跑对账时按此预期评估差异。

**去重规则**：原「出现在明日送货的项目，在库未送不再提醒」在接口版判据下**天然成立**（有送货行的单在查②+⑤环节即被剔除，根本进不了在库未送板块），渲染层无需跨板块过滤逻辑——这是查②⑤口径对比原设计的核心简化。

**行粒度差异**：Excel 版一行 = sheet 一行（项目×品类×订单）；台账一行 = 任务实例。同一项目同品类多行（多次返补/多货单）**保留多行**（推送展示「几单几行」比 Excel 版更准），不对项目去重——仅板块内排序（planDate 升序、同日按 id 稳定序）。

### 4.3 manager 层：TaskSupplierPlanManager

新增 manager（对齐 `ConstructionManager` 同包 feign 封装先例，eden-agenda-manager 模块）：

```java
/**
 * 按条件查询供应商配送时间计划（task_supplier_plan 台账）。
 * 一次调用 = 服务端分页翻完该条件的全部行（pageSize=2000，orderBy=id 升序）。
 *
 * @param taskType 8 备货 / 5 送货 / 14 进仓（必传）
 * @param window  时间窗（可空）：expectedTime 窗或 factTime 窗（进仓行按 factTime 查时 expectedTime 条件不传——服务端契约「为空不参与 WHERE」）
 * @param flowType 单据类型（可空）：0 正单 / 1 返补
 * @param orderNos 货单号集合（可空）：批查送货行判「查不到对应送货任务」用
 * @return 条件命中的全部行（跨页合并；空条件返回空列表）
 */
List<TaskSupplierPlanQueryVO> query(TaskSupplierPlanQuery query);
```

- `TaskSupplierPlanQuery`（manager dto，参数对象替代多方法重载——四次板块查询 + 一次 orderNos 批查共两种形态，多字段重载可读性差）：`taskType`（必传）/ `flowType` / `orderNos` / 时间窗（`expectedTimeFrom/To` 或 `factTimeFrom/To` 二选一，闭区间，`yyyy-MM-dd HH:mm:ss` 边界在 manager 内拼装：自然日语义 From=当日 00:00:00 / To=当日 23:59:59）。
- 实现落 `TaskSupplierPlanManagerImpl`：`@Resource TaskSupplierFeign`，照 `ConstructionManagerImpl` 异常口径——调用异常 / 返回非 success 抛 `UtopiaBussinessException(ERROR_INVOKE)`（数据源失败 = 分析失败，不降级，见 4.7）。
- **翻页实现**：首页带条件查（pageSize=2000，orderBy=`id` 升序），按 `total` 算页数逐页取，页与页之间无 sleep（服务端有界查询，非外部爬虫）。页间防漂移：**每页校验 `list` 首行 id > 上一页末行 id**（写入并发下 offset 翻页可能重复/漏行，出现即告警 log 并重查当前页一次；二次仍漂移则抛错——宁可失败不静默漏行）。
- 量级护栏：单次 query 命中 total > 20000 时直接抛错（正常单板块查询量级 <100；护栏是防「条件写错查成全表」的错误放大——全表 16502 行一次错查能接受但翻 10 页 + 2 万行入内存说明条件失效）。
- 不做通用「全字段透传」封装：agenda 只按时间窗 + taskType 查，其余条件（单号组/状态/排序）不透传——**封装窄口径**，需求变宽时再扩接口，避免把 Feign 契约整个抄进 manager。

### 4.4 service 层：DailyPushPlanAnalyzeService（新链路独立 service）+ 共享层重命名

接口路径**不复用 Excel 路径的 AnalyzeServiceImpl**（避免把两源判据耦合进同一类），新建平行 service——编排结构照抄 Excel 版（熟悉度 + 对称性），取数与板块组装换成台账口径。

**共享层重命名**（两路径共用的类从「ExcelPush*」改为路径中立的「DailyPush*」——名字带上数据源会在双源架构里误导：读 `ExcelPushTextRenderer` 会以为接口路径不该用它。**改名是纯机械变更：类名/包名/import 换，方法体零改动**，Excel 路径的 JSON 输出逐字节不变。**范围已确认（2026-11-19，用户定稿）：全量重命名，不走「仅新代码用中立名、旧名不动」的保守退路**）：

| 现名（Excel 路径独占期命名） | 新名（共享层） | 内容与改动 |
|---|---|---|
| `ExcelPushAnalyzeVO` | `DailyPushReportVO` | 四板块汇总 VO；新增 `tomorrowDelivery` list + `sourceType`（`excel`/`plan`）两字段，既有字段不动 |
| `ExcelPushRowVO` | `DailyPushRowVO` | 板块统一行结构；字段零改动（`progressSuffix`/`typeDesc` 仅 Excel 路径赋值，接口路径置空，渲染不依赖） |
| `ExcelPushRegionBucketSupport`（现 `bucketByRegion`/`collectProjectIds`/`renumber`，private static 于 AnalyzeServiceImpl） | `DailyPushRegionBuckets` | 从 `ExcelPushAnalyzeServiceImpl` 平移抽出的 static 支撑类；两条链路各自调用 |
| `ExcelPushTextRenderer` | `DailyPushTextRenderer` | 渲染器；`renderRegionReport` 增加第四板块段（null 分支兼容，见 4.6） |
| `ExcelPushPushResult` | `DailyPushPushResult` | 推送结果回显 VO；字段零改动 |
| —— | `RoomiePushService`（不动） | 推群底层，路径无关，名字本来就中立 |

> 包结构：共享类落 `service.dailypush`（新包）；Excel 路径独占类留 `service.excelpush`（`ExcelPushAnalyzeServiceImpl`/`ExcelPushPushServiceImpl` + kdocs 解析）；接口路径独占类落 `service.dailypush.plan`（`DailyPushPlanAnalyzeServiceImpl`/`DailyPushPlanAssembler`/`DailyPushPlanEnricher`）。Controller 各自独立（`ExcelPushController` 不动 / 新建 `DailyPushPlanController`）。**Excel 路径代码受改名影响的只有 import 行**（`excelpush` → `dailypush`），编译期即全部暴露，无运行时风险。

```java
// eden-agenda-service 新增（dailypush.plan 包）
public interface DailyPushPlanAnalyzeService {
    /** 四板块分析（接口源），按施工大区分组返回——结构与 DailyPushExcelAnalyzeService.analyze() 一致 */
    Map<String, DailyPushReportVO> analyze();
}
```

（`ExcelPushAnalyzeService` 接口同步更名 `DailyPushExcelAnalyzeService`——同属机械变更表。）

```
DailyPushPlanAnalyzeServiceImpl.analyze()
  ├─ 取数（4.1/4.3）：TaskSupplierPlanManager 五次拉取——查①正单进仓 / 查②在库候选 /
  │     查③返补进仓 / 查④送货计划（均服务端条件 + 翻页）+ 查⑤按②单号集批查送货行
  ├─ 板块组装：DailyPushPlanAssembler（新类，独立于 Excel 版 filterXxx）
  │     台账行 → DailyPushRowVO：projectId←projectOrderId、planDate←expectedTime/factTime、
  │     orderNo←orderNo(空则supplierOrderNo)、sectionCode 照旧四值；
  │     progressSuffix/typeDesc 置空（仅 Excel 源用的字段，渲染不依赖）
  │     在库未送 = 查② 剔除查⑤命中单（含 orderNo 空退化 project 关联）后的余集
  ├─ 大区批查/分桶/renumber（方法级复用）：DailyPushRegionBuckets，口径同 Excel 版
  ├─ enrich（4.5，新：DailyPushPlanEnricher 四板块公共补齐）
  └─ 返回 LinkedHashMap<大区code, DailyPushReportVO>（sourceType="plan"）
```

- **`DailyPushReportVO` 共享而非新建**：字段结构两路径完全同构（四板块 list + 大区字段 + 元数据），新建 VO 只会让前端渲染/推送渲染写两遍。`tomorrowDelivery` 在 Excel 路径恒空（Excel 版无此板块，前端向后兼容）。
- `DailyPushPlanPushServiceImpl`（新）编排照抄 `ExcelPushPushServiceImpl`（同步更名 `DailyPushExcelPushServiceImpl`）：analyze → `DailyPushTextRenderer.renderRegionReport` → `RoomiePushService.pushToGroups`；`roomie.push.group-ids` 与发送间隔配置共用（同一批目标群——**推送纪律**：对账期接口路径只 analyze 不 push，日报仍走 Excel 路径；差异收敛后接口路径接手日报，Excel 路径停推，见风险 7 与 Check List）。

### 4.5 展示字段 enrich（接口路径三板块公共）

台账行只有单号/类型/时间，渲染需要的「供应商 / 品类 / 业主 / 工长」全靠 enrich——**接口路径这步是三板块公共的新增步骤**（Excel 路径这些列源表自带；原 Excel 版仅反补板块走订单 API 回查业主/工长）。

字段来源（**口径定稿 2026-11-19**：品类/供应商台账自带、人员三列订单 API 反查；**仅接口路径调用，Excel 路径不动**）：

| 渲染列 | Excel 路径来源（不动） | 接口路径来源 | 批查方式 |
|---|---|---|---|
| 品类 | sheet 品类/套餐列 | **台账自带**：`task_supplier_plan` 加 `material_code/material_name/supplier_code/supplier_name` 四列（starlord 侧 DDL + VO 加字段，写入方 batchSave 从任务行带出） | Assembler 直接映射，零反查 |
| 供应商 | sheet 品牌/商户列 | 同上（台账自带） | 同上 |
| 工长 | sheet 工长姓名列（反补走订单 API） | `ProjectOrderInfoDTO.foremanName` | `ProjectOrderManager.batchQueryProjectOrderList(null, orderIds)` 按 projectId 批查（不依赖 taskDispatchId） |
| 业主 | sheet 姓名/业主列（反补走订单 API） | `ProjectOrderInfoDTO.homeownerName` | 同上（同一次调用带回） |
| 工程经理 | sheet 工程经理列 | `ProjectOrderInfoDTO.assistantName`（管家即工程经理，Excel 版同口径） | 同上 |

- **enrich 只剩订单 API 一条通道**（口径定稿 2026-11-19）：按 `projectId` 批查 `batchQueryProjectOrderList` 一次带回业主/工长/工程经理三列，不依赖 taskDispatchId、不反查任务系统。品类/供应商由台账自带（上表首两行）——**原 MaterialTaskManager 主通道已删除**（首次联调发现测试环境台账与任务系统 ID 脱节，且台账加列后无需反查；`DailyPushPlanEnricher` 只留 enrichPersonNames 一条路径）。
- 业主通道沿用 `enrichReworkPersons` 的既有实现模式（异常 log 后跳过，人员列可缺省——降级语义与大区失败语义不同，见 4.7）。**新代码不碰 `DailyPushExcelAnalyzeServiceImpl.enrichReworkPersons`**——enrich 逻辑落在新链路侧（`DailyPushPlanEnricher` 新类），Excel 路径的反补回查原样保留，两路径互不牵动。
- ~~MaterialTaskManager 返回 null 坑~~（通道已删，仅 Excel 路径 enrichReworkPersons 沿用订单 API，无 null 坑面）。
- enrich 编排位置：板块组装之后、大区分桶之前（接口路径自己的编排序；Excel 路径 enrich 在分桶后，互不影响）。

### 4.6 渲染与推送（渲染器共享、四板块扩展）

- **`DailyPushTextRenderer.renderRegionReport`（原 `ExcelPushTextRenderer`，重命名后共享）**：方法内增加第四板块段——`🚚 反补预计到工地 → 📦 在库未送 → 🏬 正单预计入库 → 🚛 送货计划`（新板块排最后，日期语义标签「预计发车」）。**Excel 路径调用同一方法**：其 VO 的 `tomorrowDelivery` 恒为 null → **null 门控跳过第四段，输出三板块原格式（与重命名前逐字节一致）**；接口路径恒非 null（空板块也置空 list），空板块保留「0 条」标题行。门控信号可靠性：共享分桶代码只在收到 delivery 行时才初始化字段，Excel 路径无 delivery 行 → 恒 null；接口路径分析实现建桶后规整 pass 统一补空 list。
- **送货计划板块列**：与在库/正单同构六列（`预计发车 | 供应商 | 品类 | 业主 | 工长 | 工程经理`）；工程经理列台账/任务系统无直接来源，**置 -**（列占位不塌，后续要时从订单 API butler_manager 项取——口径同反补板块「先不要」）。
- `RoomiePushService.pushToGroups` / `roomie.push.group-ids` / 发送间隔配置：两路径共用（同一批目标群）。**对账期推送纪律**：同一天只推一条路径的日报（人工错峰触发；两路径连续推会让群里收到两版日报），Check List 钉死。
- 前端：`planpush.html`（新，复制 `excelpush.html` 改造——URL 指向 `/api/v1/open/plan-push/*` + 四板块渲染 + source 标记）；`excelpush.html` 零改动（VO 新增字段对旧渲染无感）。

### 4.7 失败语义（接口路径）

| 故障 | 语义 | 理由 |
|---|---|---|
| queryPlan 拉取失败（翻页中途 / 异常） | analyze 直接抛（对齐大区接口口径） | 数据源失败推不出真数据，静默降级等于推空壳——比失败糟糕 |
| MaterialTaskManager enrich 失败 | ~~已删除该通道~~ → 订单 API enrich 失败：log 跳过，业主/工长/工程经理显示 - | 展示增强可缺省（对齐 Excel 版 enrichReworkPersons 语义） |
| ProjectOrderManager enrich 失败 | log 跳过，业主显示 - | 同上 |
| 窗口内 total 异常（>20000 护栏） | 抛错 | 条件写错查成全表的错误信号，不静默截断（对齐 queryPlan pageSize 上限语义） |
| 台账某板块空（当日无预计进仓行） | 正常返回（板块标题 0 条保留） | 数据稀疏是常态（实测当日 5 行），空板块推送「没有相关单」的语义（渲染现状口径） |
| 查⑤ orderNos 集合过大（在库候选行极多） | orderNos 去重后分批批查（每批 ≤500 单号） | queryPlan 对 orderNos 批量查询无显式条数上限，但 Feign 请求体过大有超时风险；候选行正常 ~百级对应单号 ~百级，分批兜底 |
| **Excel 路径任何故障** | 与现状一致（两路径独立，互不影响） | 双 Controller 的架构红利：接口路径挂了 Excel 照常推（反之亦然） |

## 5、接口改动

**全新增 Controller（`DailyPushPlanController`，web 模块），既有接口零改动**：

| 接口路径 | 说明 | 估时 |
|---|---|---|
| `GET /api/v1/open/plan-push/analyze` | 四板块分析（接口源），返回 `Map<大区code, DailyPushReportVO>`（结构与 excel-push/analyze 一致 + `tomorrowDelivery`/`sourceType`） | 2.5d（含 Manager/Assembler/AnalyzeService 新建） |
| `POST /api/v1/open/plan-push/push?groupIds=` | 一键推送（分析 → 渲染 → 推群），返回逐大区推送结果（对齐 excel-push/push 结构） | 0.5d（编排照抄） |
| `planpush.html` | 新前端页（复制 excelpush.html 改 URL + 四板块渲染） | 0.5d |
| （既有）`/api/v1/open/excel-push/*` | **零行为改动**（路径、入参、返回 JSON、判据全不动；共享类重命名只影响代码组织，见 4.4 命名表） | 0（重命名机械变更 0.5d，编译期验证） |

## 6、上游依赖

| # | 依赖 | 用途 | 是否卡上线 |
|---|---|---|---|
| 1 | **`edar-starlord-api` 含 `TaskSupplierFeign` 的正式版本**（≥ 2.7.6 发版；现状 2.4.5 无此类） | Feign 客户端 + Param/VO 契约 | **是——starlord 发版是前置条件**（测试环境可用 SNAPSHOT 联调） |
| 2 | starlord 线上部署含 queryPlan 的服务版本（`feat/50559773/suppliertime` 已合并待上线） | 线上数据源 | 是（与 #1 同窗口上线） |
| 3 | ~~MaterialTaskManager~~（enrich 口径变更后不再使用） | ~~品类/供应商/工长 enrich~~（改台账自带 + 订单 API，见 4.5） | 否 |
| 4 | `ProjectOrderManager.batchQueryProjectOrderList`（既有） | 业主 enrich | 否 |
| 5 | `ConstructionManager.queryRegionByProjectOrderIds`（既有） | 大区分组（两路径共用） | 否 |
| 6 | kdocs 下载链路（既有） | Excel 路径数据源（持续运行至退役） | 否（接口路径稳定后退役，见 Check List） |

## 7、表结构改动

无（内存态版本不变；`task_supplier_plan` 是 starlord 侧表，agenda 只读不建）。

## 8、风险评估

| # | 风险点 | 评估 |
|---|---|---|
| 1 | **覆盖度不足（最大风险）**：台账 16502 行 vs Excel 版日均在库未送 233 行/正单在施数百行——台账是否已同步到能撑起四板块的量级，测试环境当日进仓仅 5 行，**不能证明线上够用**（测试环境数据稀疏 ≠ 线上稀疏，但也不能反证充足） | 上线前用 prod skill 对线上只读核一次量级（四个板块判据各跑一次，行数与 Excel 路径当日分析对照）；量级撑不起时**不退役 Excel 路径**（双 Controller 架构的红利——两路径长期并存无技术障碍，退役决策纯粹看数据成熟度） |
| 2 | **口径等价性**：「在库未送」Excel 版是 sheet 自维护状态列（D 列），接口版用「factTime 有值 ∧ 送货未发生」拼——**两版对同一项目可能判定不一致**（同步延迟、送货行缺失都造成偏差） | 双跑对账：对账期两 URL 同日各跑一次 analyze，板块行集按 projectId+orderNo 对齐出差异清单，评审通过才推群（对账期接口路径只 analyze 不 push，见 Check List 推送纪律） |
| 3 | **「已送」判断缺失**：台账送货行无 factTime，在库未送板块无法识别「已送完成」的行——已送未更新的行会被长期重复提醒（Excel 版 D 列「在库已送」自愈这个） | 本期护栏：查② factTime 30 天下界（`planpush.stale-days`，factTime 距今 >30 天的行不进板块）+ 超窗剔除行 log 留痕；长期：taskDispatchId 关联 task_dispatch.state 判断任务完成态（TODO，见 Check List） |
| 4 | 翻页漂移：翻页期间台账写入导致 offset 页错位 | 4.3 的页首 id 校验 + 单次重查 + 二次漂移抛错；分析是分钟级人工触发，窗口极小 |
| 5 | enrich 放大：四板块行全部走订单 API 批查（Excel 版只反补走订单 API）——量级从 ~13 行升到 ~300 行涉及 ~200 项目 | `batchQueryProjectOrderList` 按 projectId 集一次批查，单次调用；既有 enrichReworkPersons 同款调用模式已验证批查能力；失败 log 跳过不阻断（4.7） |
| 6 | 时间语义偏差：台账 expectedTime 全是 00:00:00（实测无时分秒），「今天之后/之前」按自然日切（From=当日 00:00:00 / To=当日 23:59:59）——与 Excel 版自然日语义一致 | 无偏差（两侧都是自然日语义）；闭区间边界已在 Param 契约写死 |
| 7 | 双路径并存期的推送纪律：两路径共用 `roomie.push.group-ids`——同日两路径都 push 会让群收到两版日报 | 对账期接口路径**只 analyze 不 push**（数据核对走返回值/前端页）；切正式日报前人工错峰；Check List 钉死纪律 |
| 8 | 渲染器共享的回归面：`renderRegionReport` 加第四板块段（Excel 路径同方法调用） | Excel 路径 VO 的 tomorrowDelivery 恒 null → **null 门控跳过第四段，输出与重命名前逐字节一致**（标题计数同样不带送货）；接口路径空板块保留「0 条」段；上线后用 excel-push/analyze 渲染文本 diff 验证一次 |
| 9 | **共享层重命名的回归面**：`ExcelPush*` → `DailyPush*` 扫过 Excel 路径全部引用（Controller/Service/Renderer/VO/html） | 纯机械变更（类名/包名/import，编译期全量暴露，无运行时风险）；验证口径：重命名后 excel-push/analyze 返回 JSON 与重命名前 diff 逐字节一致 |
| 10 | 「今天之后」无上界窗：查①③④不带 To 边界（远期单全捞）——极端时多页翻页+行数膨胀 | 20000 护栏兜底（超即抛错告警）；送货计划板块展示远期行本身是口径要求（待确认④）；线上量级核查时一并观察远期行数量分布 |

## 9、Check List

- [ ] Apollo 配置：**有新增**——`planpush.stale-days`（默认 30，在库未送 factTime 下界护栏——超期陈旧行不再提醒，`@Value` 已落 `DailyPushPlanAnalyzeServiceImpl`）；**不设 `planpush.enabled` 总开关**（落地决策：Excel 路径本无开关，双 Controller 并行下 URL 即隔离，与 roomie `group-ids` 空即关惯例一致——方案 v1 的 enabled 是单路径开关切换时代的残留，删除）；`roomie.push.*` 推送配置两路径共用不新增
- [ ] SQL 变更：无（agenda 不落库；starlord 侧 `task_supplier_plan` 已建）
- [ ] 上线顺序：共享层重命名先行（独立 commit，excel-push 输出 diff 验证回归）→ starlord 发版（api + 服务）→ agenda 升 api 依赖 → 联调（测试环境接口路径跑通四板块）→ 接口路径上线（无总开关，URL 即边界）→ **双跑对账期**（两 URL 同日各跑 analyze 出差异清单评审；期间日报仍走 Excel 路径推送）→ 差异收敛后接口路径开始推日报（Excel 路径停推）→ 稳定一个考核周期后 Excel 路径退役（删 Controller/Service/html + KdocsDownloadService 推送用途停用，另期单独变更）
- [ ] 回滚方案：接口路径无独立开关，**回滚 = revert 接口路径新增代码**（`plan-push` Controller/Service/manager + 渲染器 null 门控），Excel 路径判据从未动过零回滚成本；渲染器第四板块段如出问题，Excel 路径输出不受影响（null 门控跳过该段），单独 revert 渲染器即可；共享层重命名如出问题，编译期即暴露（不会带病上线）
- [ ] TODO（上线前必做）：prod skill 线上量级核查（风险 1）——四板块判据各跑一次与 Excel 路径对照
- [ ] TODO（后续需求）：① taskDispatchId 关联 task_dispatch.state 判断送货完成态（风险 3 长期解）；② 按大区定向分群推送（`roomie.push.group-ids` 升级，大区分组方案遗留 TODO 一并承接）；③ Excel 路径退役（删 `/excel-push/*` + `DailyPushExcelAnalyzeServiceImpl`（Excel 路径独占类）+ `excelpush.html`，KdocsDownloadService 是否保留取决于其他消费方；共享层 `DailyPush*` 类保留——接口路径在用）
- [ ] 待确认（评审）：① 在库未送板块 30 天护栏阈值（`planpush.stale-days`）；② 送货计划板块工程经理列是否本期就要从订单 API 取（方案按「置 -」）；③ 对账期长短（方案按「差异收敛 + 一个完整月度推送周期」预估）；④ 送货计划板块是「今天之后全部」还是收窄回「明日一天」（用户口径为今天之后，方案按此落地；若线上量级大再收窄）
- [x] ~~重命名范围确认~~（**已定稿 2026-11-19**：全量重命名 `ExcelPush*`→`DailyPush*`，不取保守退路）

## 10、评审会议纪要

（待评审）
