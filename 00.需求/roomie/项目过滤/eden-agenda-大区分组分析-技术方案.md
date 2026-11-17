---
tags:
  - 需求
  - 云助理
  - Roomie
date: 2026-11-16
status: draft
source:
  - "[[字段映射-四板块汇总与源表对照]]"
  - "[[eden-agenda-Excel推送链路迁移-技术方案]]"
---

# eden-agenda-大区分组分析-技术方案

> 目标：`ExcelPushAnalyzeServiceImpl#analyze` 的三板块分析结果从「全量一份」改为「按施工大区分组」。
> 大区来源：施工侧 feign `FollowUpTaskFeignService#listScopeDataByProjectOrderIds`（跟单范围数据，含大区）。

## 1、背景

三板块分析（正单预计入库 / 在库未送 / 反补预计到工地）现返回全量一份 `ExcelPushAnalyzeVO`，不区分大区。业务侧需要按**施工大区**维度拆分结果——每个大区一份独立的三板块汇总，后续支撑按大区分别推送/查看。大区映射来自施工侧跟单范围数据，不在 kdocs 源表内，须调外部接口获取。

## 2、需求理解

| # | 需求点 | 口径 |
|---|---|---|
| 1 | analyze 返回值改为 `Map<大区code, ExcelPushAnalyzeVO>` | 每个大区一份独立三板块结果 |
| 2 | 大区来源 = `com.ke.utopia.construction.api.FollowUpTaskFeignService#listScopeDataByProjectOrderIds` | 传项目单号列表，manager 归一成「项目→大区」map 返回 |
| 3 | 调接口前先做三板块过滤（配货中/项目中过滤等既有口径，**不再做项目名单筛选**） | 控制入参量级（板块过滤后 ~300），只传过滤后存活行的项目单号 |
| 4 | 项目与大区 **n:1** | 多个项目归同一大区；一个项目只属一个大区 |
| 5 | **接口未返回的项目直接剔除** | **跟单范围即项目名单**（白名单 303 硬编码退役）：接口返回 = 在跟进范围内；未返回 = 剔除（无兜底桶） |
| 6 | **manager 层返回「项目→大区」** | key=项目单号，value=大区（code+name）；传 50 返 25 → map 里只有返回的 25 个，**后续链路（分桶 + 反补订单 API 人员补齐）都只碰返回的项目**（分桶 O(1) 直查，无需倒转） |

> 链路定稿（用户五步）：①读全量数据分析 → ②板块过滤（不含白名单）→ ③拿分析后项目 ID 调大区映射（返回「项目→大区」，④逐项目判大区即 O(1) map 直查）→ ⑤赋值进大区对应 map。跟单范围接口**既是分组依据也是名单过滤器**——板块过滤后的项目传进去，返回谁谁留下。

## 3、现状

```
GET /api/v1/open/excel-push/analyze            ExcelPushController.java:41
  └─ ExcelPushAnalyzeServiceImpl.analyze()     ExcelPushAnalyzeServiceImpl.java:171
       ├─ 文档① 在库 sheet  → filterStock   （白名单 ∧ D列=在库未送）
       ├─ 文档① 正单 sheet  → filterMainOrder（白名单 ∧ O列=备货中 ∧ E列含「预计入库」 ∧ 日期可解析）
       ├─ 文档② 反补 sheet  → filterRework  （白名单 ∧ H列 ≥ 今日）
       ├─ enrichReworkPersons()               过滤后调订单 API 补业主/工长（ExcelPushAnalyzeServiceImpl.java:307）
       └─ 返回一份 ExcelPushAnalyzeVO（三板块行集全量混装，无大区分组）
POST /api/v1/open/excel-push/push
  └─ ExcelPushPushServiceImpl.pushDailyReportOnce()   分析 → 渲染 → RoomiePushService.pushToGroups
```

- 三板块过滤各自独立完成，互不感知；过滤后行集里 `projectId` 齐备。项目名单靠 `PROJECT_WHITELIST` 303 个硬编码 ID（方案前状态——本方案将其退役）。
- 已有「过滤后调外部接口补数据」的先例：`enrichReworkPersons` 收集反补行的项目单号批量调订单 API（`ProjectOrderManager.batchQueryProjectOrderList(null, orderIds)`）——本方案大区查询插同一位置，模式一致。
- `ExcelPushRowVO.district`（源表「订单大部」：东部区域/西部区域/大宅）与本次的施工大区（workRegionCode）是**两个维度两个值域**，互不替代。
- push 链路 `ExcelPushPushServiceImpl.java:44` 消费单份 VO 渲染三条消息连发。

## 4、详细设计

### 4.1 总体链路

```
analyze()
  ├─ 三板块过滤（板块口径不动：备货中+预计入库/在库未送/≥今日；白名单退役）
  ├─ ★ 收集三板块存活行的 projectId 并集（LinkedHashSet 去重保序，~300 量级）
  ├─ ★ ConstructionManager.queryRegionByProjectOrderIds(ids)
  │      └─ FollowUpTaskFeignService.listScopeDataByProjectOrderIds(ids)
  │           → LinkedHashMap<projectOrderId, RegionDTO{regionCode, regionName}>（项目→大区，n:1）
  │           跟单范围即名单：传 50 返 25 → map 里只有这 25 个，分桶 O(1) 直查
  ├─ ★ 按大区分桶：每大区 new 一份 ExcelPushAnalyzeVO（regionCode/regionName 回填），
  │      接口未返回的项目 → 行剔除（不在跟进范围，无兜底桶）
  └─ ★ enrichReworkPersons()（订单 API 补业主/工长——移到大区过滤后，只查返回的项目）
       返回 LinkedHashMap<workRegionCode, ExcelPushAnalyzeVO>
```

### 4.2 manager 层：ConstructionManager 新增方法

```java
/**
 * 按项目单号批查施工大区（跟单范围数据）。
 *
 * @return 项目单号 → 大区（LinkedHashMap，接口返回序）；只含接口实际返回的项目——
 *         传 50 返 25 → map 里只有这 25 个，后续链路只处理返回的项目
 */
Map<String, RegionDTO> queryRegionByProjectOrderIds(List<String> projectOrderIds);
```

- `RegionDTO`：`regionCode` / `regionName`（key 已承载项目单号，不再重复持有项目列表）。
- 实现落 `ConstructionManagerImpl`（该类已有同包 feign 注入先例），`@Resource FollowUpTaskFeignService`，照 `ProjectOrderManagerImpl.batchQueryProjectOrderList` 的异常口径：调用异常 `UtopiaBussinessException(ERROR_INVOKE)`。
- DTO 返回 `workRegionCode` 为空字符串/null 的行跳过（视为无大区，等同未返回）。
- n:1 落地：接口若对同一 projectOrderId 返回多条，**取第一条**（`collectedIds.add()` 先到先得，与「业务上一项目一大区」一致）。
- 入参去重后单次调用不分批：板块过滤后项目量级 ~300（白名单退役后为全量存活行量级），单次 POST 可承受，加请求量级 log；量级异常（>500）再补分批——本期不做。

### 4.3 service 层：analyze 返回结构

```java
/** key = workRegionCode；LinkedHashMap，遍历序 = 首次出现序（板块行序稳定） */
LinkedHashMap<String, ExcelPushAnalyzeVO> analyze();
```

- `ExcelPushAnalyzeVO` 新增字段：`regionCode`、`regionName`（渲染标题/前端展示需要大区名）；三板块列表字段、analyzeTime/stockSheetName/stockFileTime 语义不变（各桶独立持有，值同源复制）。
- 分桶规则：行的 `projectId` 在大区 map 里 → 该行进对应大区 VO 的板块列表；不在 → **剔除**（已确认口径）。三板块互不影响（同一项目可能在多个板块各有一行，归同一大区）。
- 排序/行序：各桶内沿用既有板块排序（planDate 升序），`renumber` 在分桶后按桶内重排。
- 大区 VO 的 `rowNo` 重排后，log 汇总打印：`大区 {code}({name}): 正单 x / 在库 y / 反补 z` + 总剔除行数。

### 4.4 渲染与推送（本期行为不变）

- `ExcelPushTextRenderer.renderSections` 增加 `regionName` 参数，首条日报标题改为 `【衣柜到货进度日报·{大区名}】…`；其余渲染规则不动。
- `ExcelPushPushServiceImpl`：遍历大区 Map，每大区渲染三条消息**合并后仍推现有目标群**（`roomie.push.group-ids` 配置不按大区区分）——本期推送行为与现状等价，仅消息按大区多了份标题。按大区定向分群是后续需求（Check List TODO）。
- 前端 `excelpush.html`：`/analyze` 返回结构改为 Map 遍历渲染，每大区一组三板块表格，大区名进分组标题（轻量适配）。

### 4.5 接口失败语义

- feign 调用异常 → analyze 直接抛（manager 层统一 `ERROR_INVOKE`）——大区是本次分组的唯一依据，缺了大区无法分组，静默全量返回等于回到旧口径、口径不告警。
- 与 `enrichReworkPersons` 的降级语义（log 后跳过）**不同**：人员列是展示增强可缺省，大区是分组结构不可缺省。

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|
| `GET /api/v1/open/excel-push/analyze` | 自有（ExcelPushController.java:41） | 返回值 `ExcelPushAnalyzeVO` → `LinkedHashMap<String, ExcelPushAnalyzeVO>`（大区code→三板块）；同步 excelpush.html 渲染 | |
| `POST /api/v1/open/excel-push/push` | 自有（ExcelPushController.java:50 附近） | 行为不变（消息按大区分组渲染后仍推全量群）；返回结构不动 | |

## 6、上游依赖

| 依赖 | 说明 | 是否卡上线 |
|---|---|---|
| `com.ke.utopia.construction:artisan-construction-api:2.5.5` | `FollowUpTaskFeignService#listScopeDataByProjectOrderIds(List<String>) → ResultDTO<List<FollowUpProjectScopeDTO{projectOrderId, workRegionCode, workRegionName, ctime}>>`，POST `api/construction/follow-up/task/scope/list-data-by-order-ids`，`@FeignClient("artisan-construction")` | 否——`eden-agenda-manager/pom.xml:83` 已依赖此版本，`Application` `@EnableFeignClients("com.ke")` 已扫包，无需新增依赖/配置 |

## 7、表结构改动

无（内存态版本，不落库；落库链路见 [[eden-agenda-Excel推送链路迁移-技术方案]]，region_code 冗余列届时随分组一并入库）。

## 8、风险评估

- 板块过滤后项目量级 ~300（白名单退役后全量存活行），单次 feign 批查可承受；analyze 每次调用都触发一次大区查询（无缓存），调用量 = analyze 调用量（人工触发/推送链路，分钟级以下频次），可忽略。量级异常（>500）时再补分批。
- 分桶是纯内存操作，无放大。

## 9、Check List

- [x] Apollo 配置：无新增/变更（大区走接口实时查，不做缓存配置）
- [x] SQL 变更：无
- [ ] 上线顺序：eden-agenda 单服务发版即可；前端 excelpush.html 随服务静态资源同包发布
- [ ] 回滚方案：git revert 本次提交即回旧返回结构（接口消费方仅自有 html + push 链路，无外部调用方）
- [ ] TODO（后续需求）：按大区定向分群推送（`roomie.push.group-ids` 升级为大区→群映射配置）
- [ ] TODO（待确认）：接口对同一项目返回多条时的取舍（方案按「取第一条」，与上游确认是否稳定 1 条/项目）
- [ ] TODO（长期）：大区映射加本地缓存（TTL 分钟级），analyze 高频化后再做

## 10、评审会议纪要

（待评审）
