# 考核系统事件映射梳理（Handler 对照表与 Key 拼接规则）

> 项目：scm-workbench（`/Users/mirror/IdeaProjects/scm-workbench`）
> 整理时间：2026-11-02
> 关联笔记：[[排程考核（ScheduleIndicator）刷新与规则构建业务流梳理]]、[[考核系统开发维护注意事项（上下游变更适配手册）]]

## 一、总体链路

```
MQ 消息
  → EventDrivenSubscriberV2.persistAndHandleMessage()      # 落 event_sub_2025Qx 表（幂等去重）
  → eventDispatcher.dispatch(eventSub)                     # 按 @EventType(bizType) 路由
  → XxxHandler.handleBiz(payloadData, eventContextKey)     # ★ 事件映射发生在这里
  → 拼 key 去 DAG 里 filter 命中事件节点（startNodes / extNodes）
  → evalOrderService.calculateIndicatorByEvents(...)       # DFS 沿边计算时效指标
```

- 事件接入：`scm-workbench-service/.../mq/EventDrivenSubscriberV2.java`
- Handler 目录：`scm-workbench-service/.../mq/listener/handler/`
- 映射方法所在：`IndicatorDAGService`（指标 DAG）、`IndicatorScheduleDAGService`（排程 DAG）

**结论：事件映射逻辑确实在各 Handler 里触发，但"拼 key + 匹配"的具体实现集中在两个 DAG Service 的 4 个方法里**（见下表"映射方法"列）。

## 二、Handler 对照表

### 2.1 走 DAG 事件映射的 Handler（考核计算主链路）

| Handler                                      | bizType（来源系统）                                             | 触发场景                                | 映射方法（在哪个类）                                                                                                                                                          | Key 拼法                                                                            |
| -------------------------------------------- | --------------------------------------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| `OfcFulfillmentEventHandler`                 | `ofc_fulfillment_node`（scm-ofc）                           | OFC 履约节点事件（下单/接单/备货/送货等 actionType） | 排程项目：`IndicatorScheduleDAGService.transEventNodeByOrder()` → `getBaseEventsByActionType()`；非排程：`IndicatorDAGService.transEventNodeByOrder()` → `filterEventNodes()` | 排程 DAG：`{categoryId}_{actionType}`；指标 DAG：`l_{categoryId}_{actionType}` / `f_...` |
| `MaterialTaskCreateHandler`                  | `material-task-dispatch-state-change-v2`（edar-starlord）   | 主材任务**生成**                          | `IndicatorScheduleDAGService.getBaseEventsByDispatch(..., CREATE_TIME)`                                                                                             | `{taskType}_{nodeType}_{timeType}` 查字典 → `{categoryId}_{code}`                    |
| `MaterialTaskStateChangeHandler`             | `material-task-dispatch-change`（edar-starlord）            | 主材任务**激活**（含重启）                     | `getBaseEventsByDispatch(..., ACTIVE_TIME)`；重启任务一律视为激活事件                                                                                                            | 同上                                                                                |
| `MaterialTaskCompleteHandler`                | `material-task-node-complete`（edar-starlord）              | 主材任务节点**完成**                        | `getBaseEventsByDispatch(...)`：完成节点传 FACT_TIME，重启场景传激活节点 ACTIVE_TIME                                                                                                | 同上                                                                                |
| `ConstructionScheduleTaskChangeEventHandler` | `delivery-process-task-change-type`（artisan-construction） | 排程任务变更（工期差异 / 基装包差异 / 签前节点）         | `IndicatorScheduleDAGService.getBaseEventsBySchedule(projectOrderId, constructionDiff, beforeContract, basicNodeCodeSet)`                                           | 工期节点：eventType 直接过滤；基装包：`{scheduleCode}_{categoryId}`                             |

> `OfcFulfillmentEventHandler` 内部有两条分支：
> - `receiptType = RECEIPT_TASK` 且 actionType ∈ {通知复尺、复尺完成、设计、报价变更}：走**旧考核**（依赖用工条件），调 `IndicatorDAGService.transEventNodeByOrder(null, projectOrderId, categoryId, actionType)`；
> - 其余货单/履约事件（L727+）：先 `isScheduleProject()` 判断，排程项目走 `IndicatorScheduleDAGService`，否则走 `IndicatorDAGService`。

### 2.2 不走 DAG 事件映射的 Handler（做别的业务，容易混淆，一并列出）

| Handler | bizType（来源系统） | 实际做的事 |
|---|---|---|
| `AthenaDomainEntityEventHandler` | `utopia-athena-domain-entity-bizType`（utopia-athena） | 下单指标计算（placeOrderIndicatorService）、设计任务处理、选材数据 |
| `AtomProjectChangeEventHandler` | utopia-athena | 项目原子变更处理 |
| `ConstructionDelayProcessChangeEventHandler` | artisan-construction | 排期变化处理 |
| `ConstructionScheduleChangeEventHandler` | `fact-schedule-changed-biztype`（artisan-construction） | 实际工期变更 → 下单指标计算 + 激活选材任务 |
| `ConstructionScheduleCreateUpdateEventHandler` | `schedule-create-update-biz`（artisan-construction） | 工期创建/更新 → 同上 |
| `CubeTimeChangeEventHandler` | `butler-appoint-expect-time`（utopia-cube） | 管家预约期望时间变更 |
| `CustomerHomeReassignEventHandler` | `update_maintain_relation`（i-customer-home） | 维护关系改派 |
| `FullServiceFulfillmentOrderInfoChangeHandler` | `plan-wood-time-notify`（scm-full-service-fulfillment） | 全屋定制木作时间 → 下单指标计算 + 激活选材任务 |
| `HomeOrderServerChangeHandler` | `home-order-server-change`（utopia-nrs-sales-project） | 团装角色变更 → 改派主材任务执行人 |
| `MaterialTaskEstTimeUpdateHandler` | `material-task-dispatch-time-change`（edar-starlord） | 履约任务时间更新（fulfillmentTaskWriteService.updateTask） |
| `MerchantReceiptOrderChangeEventHandler` | utopia-construction-scm-merchant | 商家货单变更 → 绑定子订单号、下单任务变更 |
| `MerchantReceiptOrderEndEventHandler` | utopia-construction-scm-merchant | 商家货单完结 → 下单任务变更 |
| `OfcDispatchLinkNodeChangeHandler` | `ofc-dispatch-link-node-changed`（scm-ofc） | 履约任务更新 |
| `OfcDispatchNodeChangeHandler` | `ofc-dispatch-node-changed`（scm-ofc） | 履约任务创建/更新/重启（fulfillmentTaskWriteService） |
| `OmsSubOrderEventHandler` | `oms#order-changed`（scm-order） | 订单状态更新、下单任务、复尺任务绑定、通知送货任务创建 |
| `ProjectReassignServerEventHandler` | `assign-project-server-info`（shuttle-order） | 项目服务者改派 |
| `ProjectStatusChangeHandler` | `order-status-change`（shuttle-order） | 项目状态变更 → 通知送货任务取消/完成、设计任务完成、下单指标计算 |

## 三、Key 拼接详解（4 个映射方法逐一拆解）

### 3.1 指标 DAG：`IndicatorDAGService.filterEventNodes()`（L539）

**节点 key 来源**：`indicator_event.code` 表里直接存完整格式 `{l|f}_{品类ID}_{ofcCode}`（l=最后一次，f=首次；工期事件例外，code 就是工期码）。rebuild 时原样当 `EventNode.code`，并 split("_") 反解出 pre 和 ofcCode。

**查询 key 拼法**：

```java
List<String> targetEvent = Arrays.asList(
        String.format("l_%s_%s", categoryId, actionType),   // 最后一次
        String.format("f_%s_%s", categoryId, actionType)    // 首次
);
```

**举例**：品类 `029006005`（橱柜）收到 actionType=`3005`（下单）事件：

```
查询 key = ["l_029006005_3005", "f_029006005_3005"]
→ 命中 indicator_event 表中 code 等于这两个值的节点，作为 startNodes
```

**extNodes（兜底补算）**：写死的货单事件清单 `OfcFulfillmentEventHandler.ORDER_ACTION_TYPE_LIST = [900010, 3005, 6010, 6030, 4009, 3030, 6166, 3100]`，每个拼 `l_{categoryId}_{actionType}` 一并计算，防止某节点一次计算失败后永远算不到。

**边的 key**（rebuild L240-249）：规则表 `indicator_compute_rule` JSON 里 nodeTo 是裸码，统一拼 `l_{categoryId}_{nodeCode}`（以 "F" 开头先去前缀）；nodeFrom 本身就是完整 code。

**硬编码特例**：`CONSTRUCT_EXT_EVENT_CODE = ["l_029006005_3005", "l_029006010_3005", "l_029006013_3005"]` —— 橱柜/定制柜下单任务起点不是工期节点，项目全量计算时额外补充。

### 3.2 排程 DAG：`IndicatorScheduleDAGService.getBaseEventsByActionType()`（L729）

**节点 key 来源**（rebuild L313-355，笛卡尔展开）：

```java
// 工期事件（eventType=1）：不挂品类
uniqCode = event.getCode();                        // 工期码本身

// 品类事件：categoryMap（排程规则的 materialCode + relationMaterialCode）× 基础事件表
uniqCode = categoryId + "_" + event.getCode();     // event.code = ofcCode
```

**查询 key 拼法**：

```java
Set<String> targetEvents = new HashSet<>();
targetEvents.add(String.format("%s_%s", categoryId, actionType));
if (actionType == OMS_NOTIFY_READY_TO_ENTER) {     // 特例：通知送货 → 顺带触发上门预约
    targetEvents.add(String.format("%s_%s", categoryId, NOTIFY_TRANSIT_BOOKING));
}
```

**举例**：品类 `029006005` 收到 actionType=`3005`：

```
查询 key = "029006005_3005"
→ 命中 uniqCode 相同的 ScheduleEventNode（注意：没有 l_/f_ 前缀，与指标 DAG 区分）
```

**边的 key**（`convertNodeCode()` L407）：排程规则给的 nodeCode 是排程任务码，需翻译：

| 规则侧 nodeCode 形态 | 翻译结果 |
|---|---|
| categoryId 为空（工期节点） | `scheduleCodeToCode.get(nodeCode)`（排程码→ofc事件码，字典来自 `indicator_base_event.scheduleCode`） |
| `task_generate_{品类}` | `{categoryId}_{code(targetTaskType)}_generate` |
| `task_activate..._{品类}` | `{categoryId}_{code(targetTaskType)}_active` |
| 其他 `{taskType}_{品类}` | `{categoryId}_{code(taskType)}` |

### 3.3 主材任务映射：`getBaseEventsByDispatch()`（L757）

三段式，是唯一"查字典"式的映射（其余都是纯拼串）：

**① 字典**（`indicator_base_event` 表）：

```java
Map<String, String> NodeToCodeMap = getAllBaseEvents().stream()
        .collect(toMap(be -> be.getTaskInfo() + "_" + be.getTimeType(),   // key: "taskType,nodeType_时间类型"
                       IndicatorBaseEvent::getCode));                      // value: ofc事件码
```

**② 按任务节点"哪个时间字段有值"拼查询 key**（timeType：1 上门 / 2 生成 / 3 完成 / 4 激活）：

```java
if (noticeRetainTime 有效 && 需要上门时间)  key = taskType_nodeType_1
if (gmtCreate 有效 && 需要生成时间)         key = taskType_nodeType_2
if (endTime 有效 && 需要完成时间)           key = taskType_nodeType_3
if (startTime 有效 || restartCount > 0)    key = taskType_nodeType_4   // 重启一律视为激活
```

**③ 拼最终 uniqCode 并 filter**：

```java
uniqCodeSet.add(categoryId + "_" + NodeToCodeMap.get(key));
// 当前完成节点(非重启)另放 extNodes 补算
extCodeSet.add(categoryId + "_" + NodeToCodeMap.get(taskType_nodeType_3));
```

**举例**（数值为示意）：安装任务 taskType=`5`、节点 nodeType=`2` 完成，品类 `029006005`：

```
key = "5,2_3"（taskInfo="5,2"，timeType=3 完成时间）
→ 查字典得 code = "6010"
→ uniqCode = "029006005_6010" → 命中排程 DAG 节点
```

各 Handler 传入的 timeTypes 决定映射到哪类事件：Create→[2 生成]、StateChange→[4 激活]、Complete→[3 完成]（+重启时 4 激活）。

### 3.4 工期/排程任务映射：`getBaseEventsBySchedule()`（L813）

不拼 actionType，按节点属性过滤：

```java
// 工期差异：eventType == CONSTRUCTION_SCHEDULE(1) 的全部节点作 startNodes
// 基装包差异：eventType == BASIC_PACKAGE_SCHEDULE(6) 且
//     basicNodeCodeSet.contains(String.format("%s_%s", en.getScheduleCode(), en.getCategoryId()))
//     → key 格式：{排程任务码}_{品类ID}（注意与 uniqCode 的 {品类}_{ofcCode} 顺序相反！）
// 签前节点：getNodesBeforeContract() 拉设计/测量/复尺/下单任务，走 3.3 同款字典映射放 extNodes
```

## 四、Key 格式速查表

| DAG | 节点 key 格式 | 示例 | 生成处 | 查询处 |
|---|---|---|---|---|
| 指标 DAG 品类事件 | `{l\|f}_{品类ID}_{ofcCode}` | `l_029006005_3005` | `indicator_event.code` 表里直接存 | `filterEventNodes()` 拼 `l_/f_{categoryId}_{actionType}` |
| 指标 DAG 工期事件 | 工期码（无前缀无品类） | — | 同上（eventType=1） | — |
| 排程 DAG 品类事件 | `{品类ID}_{ofcCode}` | `029006005_3005` | rebuild：品类 × 基础事件展开 | `getBaseEventsByActionType()` 拼 `{categoryId}_{actionType}` |
| 排程 DAG 工期事件 | 工期码 | — | rebuild：基础事件直接转 | `getBaseEventsBySchedule()` 按 eventType 过滤 |
| 主材任务映射 | `{taskType,nodeType}_{timeType}` → 字典 → `{品类ID}_{code}` | `"5,2_3"` → `029006005_6010` | `indicator_base_event.taskInfo/timeType/code` | `getBaseEventsByDispatch()` |
| 排程边（生成/激活类） | `{品类ID}_{code}_{generate\|active}` | `029006005_3005_generate` | `convertNodeCode()` | — |
| 基装包差异匹配 | `{scheduleCode}_{品类ID}` | — | — | `getBaseEventsBySchedule()` |

## 五、维护注意点

1. **映射靠"字符串拼接约定"，没有注册表**：建 DAG（rebuild）和查 DAG（Handler→getBaseEventsByXxx）必须遵守同一格式，改任何一头都要同步另一头。
2. **两套 DAG 的 key 差一个前缀**：指标 DAG 有 `l_/f_`，排程 DAG 没有；`transEventNodeByOrder` 在两个 Service 里同名但拼法不同，别搞混。
3. **基装包的 key 顺序是反的**（`{scheduleCode}_{categoryId}`），是历史约定。
4. **硬编码清单**：`ORDER_ACTION_TYPE_LIST`、`CONSTRUCT_EXT_EVENT_CODE`、"通知送货→上门预约"联动，新增品类/事件时要检查是否需要补。
5. **DAG 缓存**：排程 DAG 按 version 缓存在 `SCHEDULE_DAG_CACHE`，事件字典（DB 表）变更后需 rebuild 才生效。
