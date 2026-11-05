---
source: code-deep-dive
date: 2026-11-04
class: com.ke.utopia.scm.workbench.controller.ToolController
method: refreshEvalScheduleProjectOrder
format: 自顶向下逐层递归（功能是什么→如何实现→由哪几个功能块构成→下钻）
related_methods:
  - IndicatorScheduleDAGService.isScheduleProject
  - IndicatorScheduleDAGService.checkBasicDiff
  - IndicatorScheduleDAGService.getBaseEventsBySchedule
  - IndicatorScheduleDAGService.getScheduleDAG
  - IndicatorScheduleDAGService.rebuild
  - IndicatorScheduleDAGService.getFigureNodeList
  - IndicatorScheduleDAGService.getDataNodeList
  - EvalScheduleOrderService.calculateScheduleIndicatorByEvents
  - EvalScheduleOrderService.doScheduleEval
  - EvalScheduleOrderService.figureCore
  - EvalScheduleOrderService.calculateTimelines
  - EvalScheduleOrderService.getScheduleTimeTrack
  - SyncMaterialTaskService.syncAssessmentTime
  - IndicatorChangedListener.handleIndicatorChangedEvent
---

# 排程考核重算接口完全解析（refresh-eval-schedule 全链路·自顶向下递归版）

> **阅读方式**：本文按层级组织——第 1 层先讲接口本身"功能是什么、怎么实现"，然后分解出它由哪几个功能块构成；进入第 2 层，每个功能块同样先讲功能与实现、再分解出下一层功能块；如此递归直到叶子（一条 SQL / 一次 RPC / 一个纯函数）。每层开头固定三问：**功能是什么？如何实现？由哪几个功能块构成？**
> 依据：2026-11-04 对 `/Users/mirror/IdeaProjects/scm-workbench` 工作区代码逐行走读；Feign URL 从 m2 仓库 jar 反编译核实（artisan-construction-api 2.5.4、edar-starlord-api 2.6.11、shuttle-order-api 4.1.17、scm-ofc-api 2.8.19）。
> 关联笔记：[[排程考核（ScheduleIndicator）刷新与规则构建业务流梳理]]、[[考核系统刷数接口使用指南（重算入口×批量调用SOP）]]、[[考核时间计算逻辑梳理（工作日自然日与节假日日历）]]、[[考核系统上下游交互清单（对端系统×交互信息总表）]]

---

## 〇、功能树总览（全文地图）

先给全景：这个接口的功能分解树长这样，每个节点就是下文的一节（编号对应）。

```text
L0  POST /api/refresh/eval/schedule/projectOrder —— 新排程考核·项目级重算后门
│
├─ ① 项目准入（§二）—— 决定"这个项目该不该算"
│   ├─ 1a 本地白名单查询 ── DB schedule_project
│   └─ 1b starlord 排程开关查询 ── Feign materialScheduleSwitch
│
├─ ② 重算范围准备（§三）—— 决定"这次要重算哪些东西"
│   ├─ 2a 基装包任务拉取 ── Feign queryScheduleNode（只拉基装包类型）
│   ├─ 2b 基装包 diff 判定 checkBasicDiff ── Redis 比对（forceAll=true 全量 diff）
│   └─ 2c 计算范围圈定 getBaseEventsBySchedule ── DAG 上选 startNodes/extNodes
│       ├─ 2c-1 签前节点收集 getNodesBeforeContract ── Feign taskDetail
│       └─ 2c-2 三类节点归位（工期→start / 签前→ext / 基装包diff→start）
│
├─ ③ 考核 DAG 构建（§四）—— 共享基建：把排程规则模板变成"谁依赖谁"的图
│   ├─ 3a 版本号获取 getIndicatorVersion ── Feign queryTemplateIdByProjectOrderId
│   ├─ 3b 两级缓存取图 getScheduleDAG ── Guava(version→JSON, 10min) + 反序列化重建引用
│   └─ 3c 图构建 rebuild（缓存 miss 时）
│       ├─ 3c-1 基础事件字典 ── DB indicator_base_event
│       ├─ 3c-2 材料考核规则 ── Feign queryAssessmentRuleCfgList
│       ├─ 3c-3 返补考核规则 ── Feign queryRebateRuleList
│       └─ 3c-4 组装（品类×事件=节点 / 规则行=边 / convertNodeCode 映射）
│
└─ ④ 考核计算引擎（§五）calculateScheduleIndicatorByEvents —— 真正算数的地方
    ├─ 4a 载体装载 getFigureNodeList（§五.2）
    │   ├─ 4a-1 货单载体 initOrderNode ── DB×4（indicator_order/project_order/task/ext）
    │   ├─ 4a-2 用工载体 initDispatchNode ── Feign×2（taskDetail + replacement/get）
    │   └─ 4a-3 DFS 正向遍历（hasFilterNode 载体阀门）
    ├─ 4b 数据节点圈定 getDataNodeList（§五.3）── 反向遍历入边
    ├─ 4c 数据拉取 getScheduleTimeTrack（§五.4）
    │   ├─ 4c-1 五路数据收敛 buildScheduleTimeDataContext ── Feign×4 + DB×1
    │   ├─ 4c-2 任务耗时与关联修正 initInnerIntervalAndRelateNodeId
    │   └─ 4c-3 轨迹抽取 TimeExtractor×8（策略模式）→ NodeTimeTrackV2
    ├─ 4d 核心算数 doScheduleEval（§五.5）
    │   ├─ 4d-1 规则粗过滤 flatRules（打平入边规则 + cross 标记）
    │   ├─ 4d-2 细过滤 filterRuleByOrderNode（品类/供应商/履约/重启/返补 六元匹配）
    │   ├─ 4d-3 时间线计算 figureCore
    │   │   ├─ ruleId 分组 + 精确优先排序 + 取第一组
    │   │   ├─ calculateTimelines（前置时间四级选取）
    │   │   ├─ TimeProcessor.processTime（之前/之后/当天 ± N，跳假日）
    │   │   ├─ 汰换兜底 adjustExpectTimeForReplacement
    │   │   └─ selectFinalTimeline（最早→min / 最晚→max）
    │   └─ 4d-4 结果结构化 createIndicatorEvalSDO
    └─ 4e 落库与同步（§五.6）
        ├─ 4e-1 货单载体落库 dealOrderNode ── DB×3 + 下单及时回填
        ├─ 4e-2 用工载体落库 dealDispatchNode ── upsert indicator_task_dispatch_node
        ├─ 4e-3 三条同步通道
        │   ├─ Kafka schedule_data_recovery（无条件，回排程）
        │   ├─ Kafka workbench_indicator_change（送达&灰度，回供应商，Spring事件解耦）
        │   └─ 异步 RPC directSaveEstimatedTime（灰度/mode7，回 starlord）
        └─ 4e-4 防写放大 updateFulfillmentTask（变化才写）
```

**一句话总纲**：这个接口 = 把一个项目的排程考核"推倒重算一遍"——过两道准入开关 → 搭一张规则 DAG → 圈出要算的节点 → 一次性拉齐时间轨迹 → 逐节点套规则算出"最晚/最早应该何时完成" → 写回 4 张表 + 发 2 条 Kafka + 异步回写 starlord。

---

## 一、第 1 层：接口整体 — ToolController.refreshEvalScheduleProjectOrder

**功能是什么？** 一个手动重算后门：传入项目 ID 列表，对每个项目把"新排程考核"（mode=7，围绕品类×供应商×履约方式的规则 DAG）**全量重算一遍**。业务背景——装修主材要经历设计→测量→复尺→下单→接单→备货→入仓→通知送货→送达→签收的长链条，每个环节有"应该多久完成"的要求，且要求是链式依赖的（送达的考核时间取决于通知送货，通知送货又取决于前面的工序……整条链是一张 DAG）。正常链路是 Kafka 事件驱动的**增量**重算；但改规则配置、修数据、排查异常时需要**全量**重算——这就是这个接口存在的意义（刷数 SOP 见 [[考核系统刷数接口使用指南（重算入口×批量调用SOP）]]）。

**如何实现？** `scm-workbench-start/.../controller/ToolController.java:230`。实现就是一个**串行 for 循环**：每个项目依次过准入→准备范围→算数，没有任何事务、没有并发、没有分页：

```java
@PostMapping("/api/refresh/eval/schedule/projectOrder")
@ResponseBody
public ResultDTO<Void> refreshEvalScheduleProjectOrder(@RequestBody List<String> projectOrderIds) {
    // 新排程考核
    ResultDTO<Void> resultDTO = new ResultDTO<>();

    for (String projectOrderId : projectOrderIds) {
        try {
            // 白名单开关
            Boolean scheduleInit = indicatorScheduleDAGService.isScheduleProject(projectOrderId);
            // 排程开关
            Boolean inSchedule = edarStarlordRpc.queryProjectInScheduleSwitch(projectOrderId);
            if (scheduleInit || inSchedule) {
                // 获取基装包任务
                List<String> targetTaskTypeList = new ArrayList<>(EnumTransformUtil.BASIC_PACKAGE_ACTION_TYPE_SET);
                List<DeliveryProcessTaskDTO> tasks = constructionScheduleRpcService.queryScheduleNode(projectOrderId, null, targetTaskTypeList);
                tasks = tasks.stream()
                        .filter(t -> DeleteStatusEnum.NO.getCode().equals(t.getStandardDeleteStatus()))
                        .collect(Collectors.toList());

                // 判断基装包节点是否有变化
                Set<String> basicNodeCodeSet = indicatorScheduleDAGService.checkBasicDiff(projectOrderId, tasks, true);

                FigureScheduleParam scheduleParam = indicatorScheduleDAGService.getBaseEventsBySchedule(projectOrderId, true, true, basicNodeCodeSet);
                if (scheduleParam != null) {
                    evalScheduleOrderService.calculateScheduleIndicatorByEvents(projectOrderId, scheduleParam.getStartNodes(), scheduleParam.getExtNodes(), inSchedule);
                }
            }
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.RPC_CALL_FAILURE.getCode(), "排程考核后门计算失败", e);
        }
    }
    resultDTO.success();
    return resultDTO;
}
```

**由哪几个功能块构成？** Controller 本身不含业务逻辑，它编排了四个功能块（对应功能树的 ①②③④，下文依次下钻）：

| 功能块 | 代码入口 | 回答的问题 |
|--------|---------|-----------|
| ① 项目准入 | `isScheduleProject` + `queryProjectInScheduleSwitch` | 这个项目该不该算？ |
| ② 重算范围准备 | `queryScheduleNode` + `checkBasicDiff` + `getBaseEventsBySchedule` | 这次要重算哪些东西？ |
| ③ 考核 DAG 构建 | `getScheduleDAG`（②③④共享的基建） | "谁依赖谁、依赖多紧"的图从哪来？ |
| ④ 考核计算引擎 | `calculateScheduleIndicatorByEvents` | 怎么算出考核时间并落地？ |

**第 1 层必须记住的三个行为细节**（编排层的隐含契约，后面所有层都受它约束）：

1. **异常即断整批**：catch 里是 `throw` 不是 `continue`——第 3 个项目抛异常，后面全部不跑。对比旧接口 `/api/refresh/eval/projectOrder`（`ToolController.java:178`）是 catch 后记日志继续。刷大批量务必拆小批。
2. **无事务**：接口无 `@Transactional`，算到一半挂掉，前面已写库的数据不回滚——落库层（§五.6）的"变化才写"机制部分缓解了这一点。
3. **静默跳过**：不在名单/开关内、没配排程规则（`scheduleParam==null`）的项目直接跳过，返回值不体现。**接口返回 success ≠ 每个项目都算了**——排查第一步永远先看日志确认项目真的进了计算。

另外注意第 4 参语义：`calculateScheduleIndicatorByEvents(..., inSchedule)` 把"排程开关是否打开"作为 `syncMaterialTask` 下传——**排程开关开的单才算灰度单，才会在算完后同步考核时间回 starlord**。这个布尔值会一路穿透到 §五.6 的同步通道开关上。

---

## 二、第 2 层 · 功能块 ①：项目准入（两个开关）

**功能是什么？** 决定"这个项目该不该走新排程考核"。新考核上线是灰度推进的，必须有一个闸门区分"走新算的项目"和"走旧算的项目"。

**如何实现？** 两个独立开关取**或**：`scheduleInit || inSchedule` 命中任一即放行。两个查询无条件都会执行（短路只发生在 if 判断，两个 get 早已跑完）。

### 功能块 1a：本地白名单查询（isScheduleProject）

**功能**：查这个项目在不在本地灰度名单。**实现**：`IndicatorScheduleDAGService.java:1008`，一张表的存在性查询：

```java
public boolean isScheduleProject(String projectOrderId) {
    ScheduleProjectExample example = new ScheduleProjectExample();
    example.createCriteria()
            .andProjectOrderIdEqualTo(projectOrderId);
    ScheduleProject exist = scheduleProjectMapper.selectOneByExample(example);
    return exist != null;   // 表里有记录 = 白名单内
}
```

**叶子**：DB `schedule_project` 单表 selectOne。这是新排程考核逐城市/逐项目放量的灰度控制表。

### 功能块 1b：starlord 排程开关查询（queryProjectInScheduleSwitch）

**功能**：问上游 starlord"这个项目的材料排程开关开了吗"。**实现**：`EdarStarlordRpc.java:107`，一次 Feign 调用：

```java
public Boolean queryProjectInScheduleSwitch(String projectOrderId) throws RpcException {
    MaterialScheduleSwitchParam param = MaterialScheduleSwitchParam.builder()
            .projectOrderId(projectOrderId)
            .build();
    return RpcHelper.unwrap(() -> categoryProcessFeign.materialScheduleSwitch(param), "categoryProcessFeign.materialScheduleSwitch");
}
```

**叶子**：`POST /api/config/materialScheduleSwitch`（edar-starlord，jar 反编译核实）。

### 两开关的语义差异（这一层最容易混的点）

- `scheduleInit || inSchedule` 是**或**——命中任意一个就算；
- 但只有 `inSchedule` 被下传当 `syncMaterialTask`。**白名单单 ≠ 灰度单**：只命中白名单、排程开关没开的项目，考核照算，但**不同步考核时间回 starlord**（历史灰度期数据走这条）。`EvalScheduleOrderService.java:127` 注释原话："syncMaterialTask说明是灰度单, 需要同步主材任务"。

---

## 三、第 2 层 · 功能块 ②：重算范围准备（基装包 diff + 圈计算范围）

**功能是什么？** 决定"这次要重算哪些东西"。全量重算太贵（一个项目的 DAG 上百节点），事件驱动链路靠 diff 只重算变化波及的子图；这个后门走的是"**强制全量**"路线，但复用了同一套 diff 机制来表达范围。

**如何实现？** 三步：拉基装包任务 → 判定哪些基装包节点有 diff → 在 DAG 上圈出 startNodes/extNodes。由三个功能块构成。

### 功能块 2a：基装包任务拉取（queryScheduleNode）

**功能**：从排程系统拉这个项目的**基装包任务**（水电、瓦工、木工等主体施工环节——它们是主材依赖链的源头）。**实现**：`ConstructionScheduleRpcService.java:89`，带类型过滤的查询：

```java
public List<DeliveryProcessTaskDTO> queryScheduleNode(String projectOrder, String nodeCode, List<String> taskTypes) throws RpcException {
    DeliveryProcessTaskQueryParam param = new DeliveryProcessTaskQueryParam();
    param.setProjectOrderId(projectOrder);
    if (StringUtils.isNotEmpty(nodeCode)) { param.setNodeCode(nodeCode); }      // 本入口传 null = 不限节点
    if (taskTypes != null && !taskTypes.isEmpty()) { param.setTaskTypes(taskTypes); }  // 只拉基装包类型
    return RpcHelper.unwrap(() -> deliveryProcessQueryFeignService.queryList(param), "deliveryProcessQueryFeignService.queryList");
}
```

Controller 侧再过滤一道 `standardDeleteStatus == NO`（剔掉标准侧已删除的任务）。

**叶子**：`POST /api/construction/delivery-process/queryList`（artisan-construction）。类型集合 `EnumTransformUtil.BASIC_PACKAGE_ACTION_TYPE_SET` = 指派工人/施工排期/施工开始/施工/施工验收。

### 功能块 2b：基装包 diff 判定（checkBasicDiff）

**功能**：判断基装包节点的时间有没有变化，返回变化的节点码集合。**实现**：`IndicatorScheduleDAGService.java:986`，逐节点与 Redis 里上次记录的时间比对：

```java
public Set<String> checkBasicDiff(String projectOrderId, List<DeliveryProcessTaskDTO> tasks, boolean forceAll) {
    Set<String> res = new HashSet<>();
    for (DeliveryProcessTaskDTO task : tasks) {
        if (!EnumTransformUtil.BASIC_PACKAGE_ACTION_TYPE_SET.contains(task.getTaskType())) continue;
        String time;
        if (EnumTransformUtil.BASE_CONSTRUCTION_WORK_START.equals(task.getTaskType())) {
            time = !DEFAULT_TIME.equals(task.getFactStartTime()) ? task.getFactStartTime() : task.getPredictStartTime();  // 开工节点: 实际优先
        } else {
            time = !DEFAULT_TIME.equals(task.getFactEndTime()) ? task.getFactEndTime() : task.getPredictEndTime();        // 其余: 实际完成优先
        }
        String key = "scm-workbench:schedule-basic-package-diff:" + projectOrderId + ":" + task.getNodeCode();
        boolean diff = forceAll || !redisClient.existsKey(key) || !time.equals(redisClient.get(key));   // 强制 or 缓存无值 or 值变了
        redisClient.set(key, time, apolloConfig.getScheduleInterval());                                  // 顺带刷新缓存
        if (diff) { res.add(task.getNodeCode()); }
    }
    return res;
}
```

**本后门传 `forceAll=true` 的含义**：`diff = forceAll || ...` 短路——**全部基装包节点直接视为有 diff**，Redis 比对逻辑被绕过（但缓存照写，TTL 由 Apollo `schedule_interval` 默认 7200s 刷新）。换句话说这个后门不依赖 diff 省力，每次都从基装包源头全量波及。正常事件驱动链路（Kafka 触发的那条）才真正依赖这个 diff 只算变化的子图。

**叶子**：Redis get/set（key 模式 `schedule-basic-package-diff:{projectOrderId}:{nodeCode}`）+ Apollo `schedule_interval`。

### 功能块 2c：计算范围圈定（getBaseEventsBySchedule）

**功能**：在 DAG（§四构建的）上选出本次计算的起点与附加节点，输出 `FigureScheduleParam{startNodes, extNodes}`。**实现**：`IndicatorScheduleDAGService.java:878`，按三个入参布尔把三类节点归位：

```java
if (constructionDiff) {   // 入口传 true
    // 全部工期节点 → startNodes（开工交底/技术交底/水电验收/木作验收/竣工…是依赖链的源头）
    res.getStartNodes().addAll(eventNodeList.stream()
            .filter(eventNode -> eventNode.getEventType().equals(EventTypeEnum.CONSTRUCTION_SCHEDULE.getValue()))
            .collect(Collectors.toList()));
}
if (beforeContract) {     // 入口传 true
    Set<String> extCodeSet = getNodesBeforeContract(projectOrderId);   // 功能块 2c-1
    for (ScheduleEventNode en : eventNodeList) {
        if (extCodeSet.contains(en.getUniqCode())) { res.getExtNodes().add(en); }
    }
}
if (CollectionUtils.isNotEmpty(basicNodeCodeSet)) {   // 2b 的输出（本后门=全部）
    // 基装包 diff 节点 → startNodes
    res.getStartNodes().addAll(eventNodeList.stream()
            .filter(en -> en.getEventType().equals(EventTypeEnum.BASIC_PACKAGE_SCHEDULE.getValue()))
            .filter(en -> basicNodeCodeSet.contains(String.format("%s_%s", en.getScheduleCode(), en.getCategoryId())))
            .collect(Collectors.toList()));
}
return res;   // DAG 为 null 时（没配排程规则）上方已 return null → 项目静默跳过
```

**startNodes 与 extNodes 的语义差别**（决定它们在引擎里的不同待遇，§五.2）：startNodes 是 **DFS 起点**，从它们出发会波及全部下游；extNodes 只是**直接塞进计算名单**的额外节点、不当起点。

**三类节点的业务含义**：

| 类别 | 归宿 | 为什么 |
|------|------|--------|
| 工期节点（`CONSTRUCTION_SCHEDULE`） | startNodes | 依赖链源头，从它们正向 DFS 才能波及全部品类节点 |
| 签前节点（设计/测量/复尺/下单中**未完成**的） | extNodes | 没有完成时间、不能出考核结论，但先预计算存着，实际完成时间一到即出结果 |
| 基装包 diff 节点（`BASIC_PACKAGE_SCHEDULE`） | startNodes | 验收时间变了的新起点；本后门 forceAll=全部 |

#### 功能块 2c-1：签前节点收集（getNodesBeforeContract）

**功能**：找出设计/测量/复尺/下单四类任务里"还没出结果"的节点。**实现**：`IndicatorScheduleDAGService.java:920`，查 starlord 任务明细，挑非重启且无预计时间的：

```java
param.setTaskTypeIn(Arrays.asList(TaskTypeEnum.DESIGN.getValue(), TaskTypeEnum.MEASURE.getValue(),
        TaskTypeEnum.RECHECK_SCALE.getValue(), TaskTypeEnum.ORDER.getValue()));
List<TaskDispatchDetailSimpleDTO> tasks = edarStarlordRpc.taskDetail(param);
for (TaskDispatchDetailSimpleDTO task : tasks) {
    for (TaskDispatchDetailSimpleDTO.TaskNodeDetail node : task.getTaskNodeDetailList()) {
        if (!node.getRestartCount().equals(0)) continue;              // 重启任务不算"签前"
        if (node.getEstimatedTime().before(defaultDate)) {            // 没有预计时间 = 未完成
            String key = String.format("%s_%s_%s", taskType, nodeType, TimeTypeEnum.FACT_TIME.getValue());
            extCodeSet.add(task.getMaterialCode() + "_" + NodeToCodeMap.get(key));   // 品类_事实时间事件
        }
    }
}
```

**叶子**：`POST /material-task/v2/task-detail`（edar-starlord）+ DB `indicator_base_event`（taskType_nodeType_timeType → 事件 code 的映射字典）。

---

## 四、第 2 层 · 功能块 ③（共享基建）：考核 DAG 构建 — getScheduleDAG

**功能是什么？** 把排程系统里的**规则模板**（一堆"品类A·供应商X的送达依赖通知送货、之后3个工作日"这样的配置行）变成一张内存里的**有向无环图**：节点=事件（`瓷砖_送达`、`开工交底`），边=依赖+时间规则。这是功能块②④共同依赖的基建——②在图上圈范围，④在图上算数。

**如何实现？** 三段式：版本号当钥匙 → 两级缓存（Guava 按 version 存 JSON）→ miss 时 rebuild 从头组装。由三个功能块构成。

### 功能块 3a：版本号获取（getIndicatorVersion）

**功能**：拿到该项目绑定的排程规则模板 ID——它既是"有没有配规则"的判据，也是缓存 key。**实现**：`ConstructionScheduleRpcService.java:83`，一次 Feign：

```java
public Long getIndicatorVersion(String projectOrderId) throws RpcException {
    return RpcHelper.unwrap(() -> deliveryProcessCfgTemplateFeignService.queryTemplateIdByProjectOrderId(projectOrderId), ...);
}
```

**叶子**：`POST /api/construction/delivery-process-cfg-template/assess-rule/query-templateId-by-projectOrderId`（artisan-construction）。**返回 null = 项目没配排程规则** → `getScheduleDAG` 返回 null → 上游 `scheduleParam==null` → 项目静默跳过（日志："无法构建考核DAG, projectOrderId={} 未配置排程规则"）。

### 功能块 3b：缓存取图（getScheduleDAG 本体）

**功能**：同规则模板的项目共享一张图骨架，避免重复构建。**实现**：`IndicatorScheduleDAGService.java:268/286`：

```java
// key = 版本号(模板ID), 不是项目ID！
private static final Cache<String, String> SCHEDULE_DAG_CACHE = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

public ScheduleDAG getScheduleDAG(String projectOrderId) throws RpcException {
    Long version = constructionScheduleRpcService.getIndicatorVersion(projectOrderId);   // 3a
    if (version == null) { ...return null; }
    String cacheString = SCHEDULE_DAG_CACHE.getIfPresent(version.toString());
    res = StringUtils.isBlank(cacheString) ? rebuild(version) : JsonUtil.jsonStr2Obj(cacheString, ScheduleDAG.class);  // miss→3c
    // ……反序列化是深拷贝, 后面一大段都在重建引用:
    res.setScheduleNodeMap(res.getScheduleEventNodes().stream().collect(toMap(ScheduleEventNode::getUniqCode, e -> e)));
    for (ScheduleEdgeRule edgeRule : res.getScheduleEdgeRules()) {
        for (String codeFrom : edgeRule.getCodeFromList()) {
            // 重建 edgeMap(key=codeFrom,codeTo), 把边对象挂回 nodeFrom.edgeOutList / nodeTo.edgeInList
        }
    }
    // 过滤入度出度双0的孤立节点
    res.setScheduleEventNodes(res.getScheduleEventNodes().stream()
            .filter(en -> !(en.getEdgeOutList().isEmpty() && en.getEdgeInList().isEmpty()))
            .collect(Collectors.toList()));
    return res;
}
```

**两个设计点**：

- **缓存 key=version 而非项目**：规则是模板级的，同模板下所有项目共享骨架——100 容量服务海量项目。项目级差异在"挂哪些任务载体"（§五.2），不在图结构。
- **JSON 序列化存缓存**：取出来是深拷贝，节点/边对象引用全部断开，所以每次都要重建 edgeMap 和入出度挂载——这是"用序列化换安全"的代价。

**配套后门**：`POST /api/remove/schedule/dag/cache`（`ToolController.java:266`）——改了规则配置后先清缓存再刷数，否则拿旧图算。

### 功能块 3c：图构建（rebuild，缓存 miss 时）

**功能**：从三份数据源组装出图。**实现**：`IndicatorScheduleDAGService.java:337`，分四步：

#### 3c-1/2/3：三份原料

```java
// 原料1: 基础事件字典（本地库）—— 定义"系统里都有哪些事件"
List<IndicatorBaseEvent> allBaseEvents = getAllBaseEvents();   // DB indicator_base_event

// 原料2+3: 考核规则（排程系统）—— 定义"谁依赖谁、时限多紧"
List<ProcessNodeCfgDTO> processNodeCfgList = constructionScheduleService.getIndicatorRules(version);
// ↑ ConstructionScheduleService.java:103: 材料考核规则 queryAssessmentRuleCfgList(version)
//   + 追加返补考核规则 queryRebateRuleList() —— 两份合并
processNodeCfgList = processNodeCfgList.stream()
        .filter(cfg -> StringUtils.isNotEmpty(cfg.getMaterialCode()))   // 只留带品类的规则
        .collect(Collectors.toList());
```

叶子：DB `indicator_base_event`；`POST .../assess-rule/query-assess-rule-by-template-id` 与 `POST .../rebate-rule/query`（artisan-construction）。

#### 3c-4：组装逻辑（人话版）

1. **品类清单**：从规则收集全部 materialCode（含引用的 relationMaterialCode）→ 每个品类 × 每个非工期基础事件 = 一个品类节点，`uniqCode = 品类ID_事件code`；工期事件单独建节点（不带品类）。
2. **规则行 → 边**：每行规则的 `relationNodeCode+relationMaterialCode` 是前置（`codeFrom`），`nodeCode+materialCode` 是被考核节点（`codeTo`）。`convertNodeCode()`（463 行）负责排程侧 nodeCode → workbench 事件 code 的翻译，其中 `task_generate` → `品类_事件_generate`（生成类）、`task_activate` → `品类_事件_active`（激活类）。
3. **ruleId 聚合**：`getRuleId = 品类_供应商_履约方式_任务类型_重启类型`（482 行）——同一逻辑规则的多条前置行落同一 ruleId，后面 §五.5 按它分组。
4. **规则语义字段**：`funcName`（"最晚" if valueRetrievalLogic==0 或 RESTART，否则"最早"）、`deliveryId`（供应商码，`9999999`→`-1` 不限制）、`deliveryType`（`-1` 不限制）、`restart/replenish`、`timeRef`（之前/之后/当天）、`interval`（天数）、`timeType`（工作日/自然日）。

组装完序列化进缓存，返回 `ScheduleDAG{scheduleEventNodes, scheduleEdgeRules}`。

---

## 五、第 2 层 · 功能块 ④：考核计算引擎 — calculateScheduleIndicatorByEvents

**功能是什么？** 整个新排程考核的计算心脏：给定项目 + 计算范围（start/ext 节点），算出每个节点上每个任务实例的"期望开始/完成时间"，落库并同步下游。

**如何实现？** `EvalScheduleOrderService.java:129`，一条固定的四步流水线：

```java
public void calculateScheduleIndicatorByEvents(String projectOrderId, List<ScheduleEventNode> startNodes,
                                               List<ScheduleEventNode> extNodes, Boolean syncMaterialTask) {
    // 1. 初始化项目上下文（贯穿全程的容器）
    ScheduleAllNode scheduleAllNode = ScheduleAllNode.builder()
            .projectOrderId(projectOrderId)
            .orderNodeMap(new HashMap<>()).dispatchNodeMap(new HashMap<>())
            .orderMap(new HashMap<>()).projectOrderMap(new HashMap<>())
            .build();

    // 2. 装载载体 + DFS 正向遍历 → 待计算节点列表
    List<ScheduleEventNode> figureNodeList = indicatorScheduleDAGService.getFigureNodeList(startNodes, scheduleAllNode);

    // 3. extNodes 去重后直接塞进名单（不当 DFS 起点）
    if (extNodes != null && !extNodes.isEmpty()) { ... }

    // 4. 反向遍历入边 → 数据节点列表（算 A 需要哪些前置的时间）
    List<ScheduleEventNode> dataNodeList = indicatorScheduleDAGService.getDataNodeList(figureNodeList, scheduleAllNode);

    // 5. 一次性拉全外部数据 → 时间轨迹
    List<NodeTimeTrackV2> nodeTrackList = getScheduleTimeTrack(projectOrderId, scheduleAllNode, dataNodeList);

    // 6. 逐节点套规则算数 + 落库同步
    doScheduleEval(figureNodeList, nodeTrackList, scheduleAllNode, syncMaterialTask);
}
```

**`ScheduleAllNode` 是"项目上下文"**，五个字段贯穿全程：`orderNodeMap/dispatchNodeMap`（品类→任务类型→载体列表，DFS 与落库都用）、`orderMap/projectOrderMap`（**数据库实体的同一份引用**，落库时复用防止覆盖并发修改）、`baseDataContext`（外部数据上下文）。

**由哪几个功能块构成？** 五个，正好对应流水线 2/4/5/6 步（extNodes 合并是第 3 步的三行代码，不单列）：

| 功能块 | 回答的问题 |
|--------|-----------|
| 4a 载体装载与 DFS（getFigureNodeList） | 图上的节点，这个项目实际挂了哪些任务实例？要算哪些节点？ |
| 4b 数据节点圈定（getDataNodeList） | 算这些节点，需要先拿到哪些节点的时间？ |
| 4c 数据拉取（getScheduleTimeTrack） | 那些时间从哪些系统、怎么一次性拉齐？ |
| 4d 核心算数（doScheduleEval） | 规则怎么挑、时间怎么算？ |
| 4e 落库与同步 | 结果写哪、同步给谁？ |

### 功能块 4a：载体装载与 DFS（getFigureNodeList）

**功能**：① 把这个项目的**实际任务实例**（货单/用工）挂到 DAG 节点上——图是模板级的骨架，载体才是项目的血肉；② 从 startNodes 出发 DFS，找出被波及的全部待计算节点。

**实现**：`IndicatorScheduleDAGService.java:493`：

```java
public List<ScheduleEventNode> getFigureNodeList(List<ScheduleEventNode> startNodes, ScheduleAllNode scheduleAllNode) {
    initOrderNode(scheduleAllNode);      // 4a-1 装载货单载体
    initDispatchNode(scheduleAllNode);   // 4a-2 装载用工载体

    List<ScheduleEventNode> res = new ArrayList<>();
    for (ScheduleEventNode startNode : startNodes) {
        Stack<String> circleVisited = new Stack<>();
        List<ScheduleEventNode> figureNodeList = new ArrayList<>();
        dfs(startNode, scheduleAllNode, circleVisited, figureNodeList);   // 4a-3
        res.addAll(distinctNodes(figureNodeList));
    }
    return distinctNodes(res);   // 去重保留靠后出现的
}
```

#### 4a-1 货单载体装载（initOrderNode）

**功能**：把项目下每张货单展开成"每张货单 × 6 种货单任务"的载体列表。**实现**：`IndicatorScheduleDAGService.java:597`，DB 四连查：

```java
// ① indicator_order：项目下全部货单（品类明细，orderMap 同时存实体供落库复用）
//    每张货单 × ORDER_NODE_TASK_TYPES(接单/备货/入仓/通知送货/送达/签收) → ScheduleOrderNode
//    关键字段 fulfillmentmentDeliveryId：仓配(WAREHOUSE)取 deliveryId(仓库) 否则取 supplierId(供应商)
//    ——决定后面规则匹配用哪个 ID 对供应商
// ② indicator_project_order：项目下单维度（品类×供应商×履约方式）
// ③ fulfillment_task：只留 taskType=PLACE_ORDER 且未取消的
// ④ fulfillment_task_ext：replacement=1 的汰换标记 → extSet
// 下单载体只保留"存在对应主材任务"的（否则算出结果没地方存）：
projectOrders = projectOrders.stream()
        .filter(o -> existsMap.containsKey(o.getCategoryId() + "_" + o.getDeliveryId()))
        .collect(Collectors.toList());
```

叶子：DB `indicator_order` / `indicator_project_order` / `fulfillment_task` / `fulfillment_task_ext` 四张表。

#### 4a-2 用工载体装载（initDispatchNode）

**功能**：把 starlord 主材任务（测量/设计/复尺/安装……）的**节点**展开成载体，带重启与汰换信息。**实现**：`IndicatorScheduleDAGService.java:709`，RPC 两连查：

```java
taskDispatchDetailSimpleDTOS = edarStarlordRpc.taskDetail(param);              // POST /material-task/v2/task-detail
replaceTaskIds = new HashSet<>(edarStarlordRpc.getReplacementTaskIds(...));    // GET  /material-task/v2/replacement/get
// 过滤 DISPATCH_NODE_TASK_TYPES(测量/设计/复尺/报价审核/一次安装/安装/打磨/结晶 8类)
// 每个任务的每个 TaskNodeDetail → ScheduleDispatchNode(dispatchId/nodeId/nodeType/restartCount/mode/replenish/replacement)
```

异常处理：RpcException 时 `return`（dispatchNodeMap 留空）——用工侧查询失败会导致该项目用工节点全部没有载体、全部不算，但不中断整个项目（容错降级）。

#### 4a-3 DFS 正向遍历（dfs + hasFilterNode）

**功能**：从起点沿出边传播，找出全部受波及节点。**实现**：`IndicatorScheduleDAGService.java:551/571`：

```java
private void dfs(ScheduleEventNode current, ..., Stack<String> circleVisited, List<ScheduleEventNode> figureNodeList) {
    if (circleVisited.contains(current.getUniqCode())) return;   // 判环终止（Stack 当 visited 用）
    circleVisited.push(current.getUniqCode());
    for (ScheduleNodeEdge edge : current.getEdgeOutList()) {
        ScheduleOrderDispatchNode od = getOrderDispatchNode(edge.getNodeTo(), scheduleAllNode);
        if (hasFilterNode(edge.getRuleList(), od)) {      // 载体阀门：下个节点有能被边规则命中的载体才继续
            figureNodeList.add(nextNode);
            dfs(edge.getNodeTo(), ...);
        }
    }
    circleVisited.pop();
}
```

**`hasFilterNode` 是传播阀门**——下个节点挂的载体（货单按品类/供应商/履约方式，用工按品类/供应商/重启次数）能被边规则命中，才会被波及。通俗讲：**"瓷砖送达"规则不会波及"没有瓷砖货单"的项目节点**——有载体才有考核对象。

### 功能块 4b：数据节点圈定（getDataNodeList）

**功能**：找出算 figureNodeList 需要哪些**前置节点的时间数据**。**实现**：`IndicatorScheduleDAGService.java:508`，反向遍历入边：

```java
for (ScheduleEventNode node : fugureNodeList) {
    for (ScheduleNodeEdge edge : node.getEdgeInList()) {
        if (hasFilterNode(edge.getRuleList(), od)) {
            dataNodeSet.add(edge.getNodeFrom());   // 前置节点需要时间
            dataNodeSet.add(node);                // 自己也要（实际完成时间要写进考核结果）
        }
    }
}
```

关系：**figureNodeList（要算结果的）⊆ dataNodeList（要拉数据的）**，后者通常更大——"要算送达"只需送达在 figure 里，但"算送达需要通知送货的数据"把通知送货拉进了 data。

### 功能块 4c：数据拉取（getScheduleTimeTrack）

**功能**：把 dataNodeList 涉及的全部时间轨迹，**一次性**从四个系统拉齐，转成统一结构。设计宣言写在代码注释里："一次性拉取所有的外部数据，多次RPC->5次"。

**实现**：`EvalScheduleOrderService.java:633`，由三个子功能块构成：

#### 4c-1 五路数据收敛（buildScheduleTimeDataContext）

**功能**：构建 `BaseDataContext` 数据上下文。**实现**：`EvalScheduleOrderService.java:776`：

| 数据 | 来源 | 用途 |
|------|------|------|
| 订单详情 | `shuttle-order` `POST /api/order/project/detail/getByProjectOrderId` | 上下文头信息 |
| 全量任务 `tasks` | `artisan-construction` `POST /api/construction/delivery-process/queryList`（**无类型过滤**，与入口那次不同） | 工期数据 + 基装包 + 任务耗时 period |
| 主材任务明细 | `edar-starlord` `POST /material-task/v2/task-detail`（taskStates=1,nodeStates=1 只要有效的） | 用工节点时间轨迹 |
| OFC 货单轨迹 | `scm-ofc` `POST /api/workbench/dispatch/query/flowMainNoList` | 货单节点完成时间 |
| 履约任务 | 本地 DB `fulfillment_task`（按 品类_任务类型 分组） | 货单侧完成/激活时间 |

两个细节：

- **按需收敛第二层**：只有 dataNodeList 里存在 OFC 事件类型节点时，其品类对应的货单号才去查 OFC（orderNos 为空整次 RPC 跳过）。
- **口径归一化**（839 行）：OFC 轨迹里 `TMS_NOTIFY_DRIVER_TRANSIT_FINISH(5020)` 统一转 `SDM_NOTIFY_TRANSIT_SUCCESS(6166)`——"司机送达"与"系统送达"在考核里算同一个送达事件。

注意：**RpcException 被外层 catch 住只记日志，dataContext 带着已拉到的部分继续走**——数据不全时部分节点算不出（返回 null 的 figureRes），不会整单失败。

#### 4c-2 任务耗时与关联修正（initInnerIntervalAndRelateNodeId）

**功能**：给载体赋两个计算必需字段。**实现**：`EvalScheduleOrderService.java:685`，遍历有耗时或有关联的任务：

- **`innerInterval`（任务耗时）**：来自 `DeliveryProcessTaskDTO.period`——§五.5 里"期望开始时间 + 耗时 = 期望完成时间"的那个耗时。匹配条件：品类+供应商+履约方式（下单任务放宽履约方式）+ 已设置过的跳过。
- **`relateNodeId`（关联节点）**：解决"前置时间按哪个具体实例取"。**下单找复尺**：下单任务的规则前置是"复尺"，但项目里可能多人多次复尺，`relationTaskId` 指明依赖哪次——复尺已完成→指向复尺任务节点，未完成→指向通知复尺。用工节点同理修正（755-767 行三分支）。

#### 4c-3 轨迹抽取（TimeExtractor 策略家族）

**功能**：把五路原始数据按事件类型转成统一轨迹结构。**实现**：`TimeExtractorFactory`（构造器注入全部 `TimeExtractor` 实现，按 `getSupportedType()` 建 map）——标准策略模式：

| 事件类型 | 抽取器 | 从哪取时间 |
|---------|--------|-----------|
| 1 工期 | `ConstructionTimeExtractor` | constructionList（queryList 整理出的工期节点表单/预计时间） |
| 2 主材任务 | `MaterialTaskTimeExtractor` | taskDispatchDetailSimpleDTOS 节点时间（noticeRetainTime/gmtCreate/startTime/endTime） |
| 3 OMS_OFC | `OfcTimeExtractor` | orderFlowTimes 的 actionType 时间 |
| 4 OMS_OMS / 5 返补 / 6 施工包 | 对应抽取器 | 预留，目前空实现 |
| 7 fulfillment | `FulfillmentTimeExtractor` | fulfillmentTasks（激活/完成时间） |
| 8 基装包 | `BasicPackageTimeExtractor` | basicPackageTasks |

统一产出 `NodeTimeTrackV2`：`{code: 节点uniqCode, timeInfos: [{orderNo/dispatchId/nodeId/restartCount/supplierId, completeTime 实际完成, assessmentTime 考核时间}]}`。

收尾清洗（672-679 行）：`filterInvalidDate` 把 1970/2000 占位日期洗成 null + **双 null 的轨迹条目直接剔除**——保证每条轨迹至少有一个可用时间。

### 功能块 4d：核心算数（doScheduleEval）

**功能**：对 figureNodeList 每个节点、挂载的每个任务实例，挑出适用规则，取前置时间，套偏移，得期望时间线。**实现**：四层漏斗（`EvalScheduleOrderService.java:172` 起）：

```java
for (ScheduleEventNode figureNode : figureNodeList) {
    List<ScheduleEdgeRule> ruleList = flatRules(figureNode.getEdgeInList());  // 4d-1 粗过滤
    if (!EVENT_ENTITY.containsKey(figureNode.getName())) continue;            // 无载体映射的节点(纯工期)跳过
    // EVENT_ENTITY: 事件名→(货单0/用工1, taskType, nodeType) 的硬编码翻译字典(40+事件全枚举)
    if (entityType.equals(ORDER_NODE)) {
        for (ScheduleOrderNode orderNode : 该品类该任务类型的全部货单载体) {
            dealOrderNode(ruleList, orderNode, ...);    // → 4e-1
        }
    }
    if (entityType.equals(DISPATCH_NODE)) {
        for (ScheduleDispatchNode dispatchNode : ...) {
            if (nodeType.equals(dispatchNode.getNodeType())) dealDispatchNode(...);   // 用工还要匹配 nodeType
        }
    }
}
```

**一条完整示例**（把 4d 整体串起来）：节点`瓷砖_送达`挂货单 D（供应商S、仓配）。规则"通知送货之后 3 个工作日送达"。轨迹里 D 的通知送货完成时间=11月1日 → 期望开始 11月1日 → 期望完成 11月4日 23:59:59（工作日跳周末）。D 实际送达 11月6日 → 超期 → `evaluation=1`。

#### 4d-1 规则粗过滤（flatRules）

**功能**：把节点全部入边的规则打平成候选清单。**实现**：618 行，`flatMap` 入边 → 每条边规则复制一份并补两个运行时字段：`codeFrom`（边的起点节点）和 `cross`（**跨品类标记**：边起点的品类 ≠ 规则品类，如"下单及时依赖开工交底"——品类节点依赖工期节点）。

#### 4d-2 规则细过滤（filterRuleByOrderNode）

**功能**：从候选清单里选出**这个任务实例**真正适用的规则。**实现**：593 行，六元匹配：

```java
return rules.stream()
        .filter(r -> r.getIndicatorName().equals(name)           // 事件名
                && r.getCategoryId().equals(categoryId)          // 品类
                && (r.getDeliveryType().equals((byte) -1) || r.getDeliveryType().equals(deliveryType))  // 履约方式 -1不限制
                && (r.getDeliveryId().equals("-1") || r.getDeliveryId().equals(deliveryId))             // 供应商 -1不限制
                && isRestart.equals(r.getRestart())              // 重启任务↔重启规则
                && isReplenish.equals(r.getReplenish()))         // 返补单↔返补规则
        .collect(Collectors.toList());
```

#### 4d-3 时间线计算（figureCore）

**功能**：细过滤后可能剩多条规则（如"瓷砖仓配一条、瓷砖不限供应商一条"），挑出**最精确的那组**，算出期望时间线。**实现**：375 行，五步：

**(1) ruleId 分组 + 精确优先排序 + 取第一组**：

```java
List<List<ScheduleEdgeRule>> sortedGroups = rules.stream()
        .collect(Collectors.groupingBy(ScheduleEdgeRule::getRuleId))   // ruleId=品类_供应商_履约_任务类型_重启类型
        .values().stream()
        .sorted(Comparator.comparing(group -> group.get(0).getDeliveryType(), nullsLast(reverseOrder()))  // 履约精确优先,-1垫底
                .thenComparing(group -> group.get(0).getDeliveryId(), nullsLast(reverseOrder())))         // 供应商精确优先,-1垫底
        .collect(Collectors.toList());
List<ScheduleEdgeRule> usedRules = sortedGroups.get(0);   // 只取第一组 = 最精确的适用规则
```

排序保证**配置越精确优先级越高**（Byte/String 排序下 -1 天然靠后，reverseOrder 后精确值在前）——无需显式优先级字段就实现"最精确规则生效"。

**(2) 前置时间选取（calculateTimelines，467 行）**——对组内每条边规则，取 `codeFrom` 节点轨迹：

```java
if (!completeTrackRecords.isEmpty()) {
    completeTrackRecords.sort(comparing(getTime).reversed());
    timeTrackRecord = completeTrackRecords.get(0);    // 有实际完成时间: 取最新一条
} else {
    timeTrackRecord = assessmentTrackRecords.get(0);  // 只有考核时间: 取最新一条
}
...
result.sort(Comparator.comparing(CalculatedTimeline::getStartTime));
return result.get(0);   // 多条边(多个前置): 取最早
```

结合 `filterTimeInfos`（532 行）的**四级优先**：

1. `relateNodeId` 命中 → 直接取关联实例的轨迹（"下单找复尺"）；
2. `cross` 跨品类 → 不过滤全要；
3. 同供应商/同货单号过滤（为空时允许 crossSupplier 兜底）；
4. `dispatchId+restartCount` 命中 → 重启任务取自己那轮。

**Apollo 开关 `figure_by_assessment_v2`（默认 false）**：货单节点关闭时只保留有实际完成时间的轨迹——**货单的前置时间只认"真发生了"**，不拿别的算出来的考核时间当输入。用工节点不受此开关限制。

**(3) 时间偏移（TimeProcessor.processTime，`TimeProcessor.java:13`）**：

```java
// 期望开始 = 前置时间 + 规则偏移（当天→0 / 之前→-N / 之后→+N）
Date expectStartTime = TimeProcessor.processTime(rule.getTimeRef(), rule.getInterval(),
        calculateTime.getStartTime(), skipBitmask, calendars);
// 期望完成 = max(期望开始, 实际开始) + 任务耗时
Date startTime = innerInterval > 0 && factStartTime != null ? factStartTime : expectStartTime;
Date expectEndTime = TimeProcessor.processTime("之后", innerInterval, startTime, skipBitmask, calendars);
```

`processTime` = `determineInterval`（符号化偏移）+ `RuleUnit.figureAssessmentTime`（逐日行走跳假日、钉在 23:59:59——详见 [[考核时间计算逻辑梳理（工作日自然日与节假日日历）]]）。`skipBitmask`：自然日规则用 `CALENDAR_DAY_BITMASK`（跳春节+法定春节）；工作日规则按指标名查 `HolidayService.indicatorHolidayMap`（每个指标跳的假日不一样）。**任务已实际开始时用实际开始时间算完成**——避免"实际开干了还按期望开始算"导致完成时间虚早。

**(4) 汰换兜底（adjustExpectTimeForReplacement，319 行）**：

```java
Date replaceAssessmentTime = RuleUnit.figureAssessmentTime(
        replaceDate, apolloConfig.getReplaceBufferDays(), CALENDAR_DAY_BITMASK, calendars);
if (replaceAssessmentTime.after(figureTime.getExpectTime())) {
    figureTime.setExpectStartTime(replaceAssessmentTime);
    figureTime.setExpectTime(replaceAssessmentTime);
}
```

考核时间 = **max(正常计算值, 汰换时间 + N 自然日)**，N=Apollo `replace.supplier.assessment.buffer.days`（默认 3）——换供应商不能让新供应商背旧账。触发时 `syncBaseEvalResult()` 把调整后时间回写结果对象。

**(5) 多时间线收敛（selectFinalTimeline，`EvalOrderService.java:950`）**：`funcName=="最早"` → 取 min（expectTime 再比 expectStartTime）；`"最晚"` → 取 max。

#### 4d-4 结果结构化（createIndicatorEvalSDO）

**功能**：把时间线转成固定考核结果结构。**实现**：875 行——indicatorName、期望开始/完成时间、interval、考核日类型、前置节点（matchStartNode）、节点链（indicatorNodeList）、realFlag（前置是否全为实际时间）；再从轨迹里找本实例的 completeTime 填 `realCompTime`，与 expectCompTime 比较得出 `evaluation`（0 合格 / 1 超期）。

### 功能块 4e：落库与同步（dealOrderNode / dealDispatchNode）

**功能**：把算出的 figureRes（考核结果 + 时间线）按载体类型写库，并触发三条同步通道。**原则：结果 JSON 落本地表（考核域自己持有），考核时间双向同步（回 starlord + 发 MQ）**。

#### 4e-1 货单载体落库（dealOrderNode，211 行）

```java
// 前置: 汰换调整 → 轨迹回写（把算出的考核时间写回内存轨迹,供后续节点当输入——链式传播的闭环）
adjustExpectTimeForReplacement(...); syncBaseEvalResult(...);
updateAssessmentTimeInTrack(nodeTimeTrackMap.get(figureNode.getUniqCode()), figureTime, orderNode, null);

boolean update = false;
if (PLACE_ORDER == orderNode.getTaskType()) {
    // 下单考核: 结果存 indicator_project_order.base_eval_result（项目下单维度）
    updateProjectOrder(projectOrder, mergeIndicator(旧结果, 新结果));
    update = updateFulfillmentTask(null, ..., figureTime, PLACE_ORDER, false);      // 考核时间写 fulfillment_task
    updateIndicatorOrder(projectOrder, figureRes.getBaseEvalResult(), scheduleAllNode);  // 下单及时回填到全部同品类货单
} else {
    updateOrder(order, mergeIndicator(...));   // 结果存 indicator_order.base_eval_result（货单维度）
    update = updateFulfillmentTask(order.getOrderNo(), ..., replenish);
    if (update && syncMaterialTask && DELIVERY_GOODS == taskType) {
        applicationEventPublisher.publishEvent(indicatorChangedEvent);   // → 4e-3 通道②
    }
}
if (update && syncMaterialTask) {
    rpcAsyncExecutor.execute(() -> syncMaterialTaskService.syncAssessmentTime(...));  // → 4e-3 通道③
}
sendAssessmentMessage(orderNode, null, expectStartTime, expectTime);   // → 4e-3 通道① 无条件
```

三个易忽略点：

- **`mergeIndicator`**（`EvalOrderService.java:670`）：新结果 merge 进旧 JSON（同 indicatorName 替换）——**增量合并而非覆盖**，一张货单可能有 6 个指标的结果共存。
- **`updateIndicatorOrder` 的下单及时回填**（1002 行）：项目下单维度算出的"下单及时"**复制到该项目全部同品类同供应商的货单**（返补单除外）——业务上看的是"品类下单及时"而非具体哪张单。特意用 `scheduleAllNode.orderMap` 的**同一份实体**改，防止覆盖其他指标的并发修改。
- **`updateAssessmentTimeInTrack` 的链式意义**：算出的考核时间写回轨迹 → 下游节点把"考核时间"当可选输入（4d-3 第 2 级）→ **考核结果沿 DAG 链式传播**。

#### 4e-2 用工载体落库（dealDispatchNode，285 行）

结构与货单版对称，差别：

- 结果 upsert 到 `indicator_task_dispatch_node`（按 nodeId 查，无则 insert 有则 update，含 `eval_result` 全量时间线 JSON）；
- **mode=7（DELIVERY_FLOW 排程流程模式）的用工节点无条件同步 starlord**——`syncMaterialTask || ModeEnum.DELIVERY_FLOW==mode`（注释：历史项目存在 mode=7 脏数据，借这条通道同步）。

#### 4e-3 三条同步通道

| 通道 | 触发条件 | 链路 | 受众 |
|------|---------|------|------|
| ① Kafka `schedule_data_recovery`（bizType `workbench-assessment-time-change`） | 每个算出结果的载体**无条件** | `sendAssessmentMessage` → `ConstructionScheduleService.sendAssessmentChange` → `Producers` | 排程系统（artisan-construction 消费），让排程侧预计时间/预警与考核对齐 |
| ② Kafka `workbench_indicator_change`（bizType `indicator_changed`） | 送达指标 + 灰度单 + 时间有变化 + 四道过滤 | `publishEvent(IndicatorChangedEvent)` → `IndicatorChangedListener.handleIndicatorChangedEvent`（`@Async("IndicatorChangeMsgExecutor")`）→ 逐指标拼 payload → `producers.send` | 供应商侧（送达考核同步）。过滤：订单创建时间≥`syn.synEffectiveTime` 且 `checkValidToProducer`（区域黑名单 `qvyuBlackList` / 精装全案 / 团装 / 翻新） |
| ③ 异步 RPC | 灰度单（或用工 mode=7）+ 时间有变化 | `rpcAsyncExecutor`（专用线程池）→ `SyncMaterialTaskService.syncAssessmentTime`（241 行）→ `POST /material-task/v2/estimatedTime/save` | starlord（主材任务的考核时间回写）。**异常仅记日志不抛不重试——异步失败不影响主流程，但可能丢同步**（无补偿机制） |

#### 4e-4 防写放大（updateFulfillmentTask，963 行）

**功能**：考核时间落 `fulfillment_task` 时只写有变化的行。**实现**：查询后先 `removeIf` 剔掉"三个考核字段都没变的"再批量 update，返回 `update` 布尔值控制通道②③是否触发——重算风暴下避免无效写放大，也避免无意义的下游同步噪音。

---

## 附录 A：内部调用接口总清单（RPC × DB × MQ）

### A.1 Feign RPC（10 个，按功能树编号）

| # | 功能块 | 方法（封装层） | 对端服务 | URL |
|---|--------|---------------|---------|-----|
| 1 | 1b | `EdarStarlordRpc.queryProjectInScheduleSwitch` | edar-starlord | `POST /api/config/materialScheduleSwitch` |
| 2 | 2a / 4c-1 | `ConstructionScheduleRpcService.queryScheduleNode` | artisan-construction | `POST /api/construction/delivery-process/queryList`（**共 2 次**：入口拉基装包、数据层拉全量） |
| 3 | 3a | `ConstructionScheduleRpcService.getIndicatorVersion` | artisan-construction | `POST .../delivery-process-cfg-template/assess-rule/query-templateId-by-projectOrderId` |
| 4 | 3c-2 | `ConstructionScheduleRpcService.getIndicatorRules` | artisan-construction | `POST .../delivery-process-cfg-template/assess-rule/query-assess-rule-by-template-id` |
| 5 | 3c-3 | `ConstructionScheduleRpcService.queryRebateRuleList` | artisan-construction | `POST .../delivery-process-cfg-template/rebate-rule/query` |
| 6 | 2c-1 / 4a-2 / 4c-1 | `EdarStarlordRpc.taskDetail` | edar-starlord | `POST /material-task/v2/task-detail`（**共 3 次**，参数各异） |
| 7 | 4a-2 | `EdarStarlordRpc.getReplacementTaskIds` | edar-starlord | `GET /material-task/v2/replacement/get` |
| 8 | 4c-1 | `ProjectOrderRpc.getByProjectOrderId` | shuttle-order | `POST /api/order/project/detail/getByProjectOrderId` |
| 9 | 4c-1 | `OfcRpc.queryDispatchInfoByFlowMainNoList` | scm-ofc | `POST /api/workbench/dispatch/query/flowMainNoList`（5020→6166 归一） |
| 10 | 4e-3③ | `EdarStarlordRpc.directSaveEstimatedTime` | edar-starlord | `POST /material-task/v2/estimatedTime/save`（异步） |

RPC 统一走 `RpcHelper.unwrap()` 封装——异常转 `RpcException`。

### A.2 DB 表（7 张读 + 4 张写）

| 表 | 功能块 | 读/写 | 用途 |
|----|--------|------|------|
| `schedule_project` | 1a | 读 | 排程白名单 |
| `indicator_base_event` | 3c-1 / 2c-1 | 读 | 基础事件字典 |
| `indicator_order` | 4a-1 / 4e-1 | 读+写 | 货单载体 + `base_eval_result` 结果 JSON |
| `indicator_project_order` | 4a-1 / 4e-1 | 读+写 | 项目下单载体 + 下单及时结果 |
| `fulfillment_task` | 4a-1 / 4c-1 / 4e-4 | 读+写 | 履约任务 + `assessment_start/end_time` |
| `fulfillment_task_ext` | 4a-1 | 读 | 汰换标记（replacement=1） |
| `indicator_task_dispatch_node` | 4e-2 | 写 | 用工节点考核结果（upsert） |

Redis：`schedule-basic-package-diff:{id}:{nodeCode}`（2b）TTL=Apollo `schedule_interval`（默认 7200s）。

### A.3 Kafka 出口（2 条 topic）

| Topic | bizType | 功能块 | 触发条件 |
|-------|---------|--------|---------|
| `schedule_data_recovery` | `workbench-assessment-time-change` | 4e-3① | 无条件（每个算出结果的载体） |
| `workbench_indicator_change` | `indicator_changed` | 4e-3② | 送达指标 + 灰度单 + 时间有变化 + 区域/单据过滤 |

### A.4 Apollo 配置（6 个）

| Key | 默认 | 功能块 | 作用 |
|-----|------|--------|------|
| `schedule_interval` | 7200 | 2b | Redis diff 缓存 TTL |
| `replace.supplier.assessment.buffer.days` | 3 | 4d-3(4) | 汰换考核缓冲自然日 |
| `figure_by_assessment_v2` | false | 4d-3(2) | 货单前置时间是否允许用考核时间 |
| `syn.synEffectiveTime` | 2024-03-29 | 4e-3② | 供应商同步生效分界 |
| `qvyuBlackList` | — | 4e-3② | 区域黑名单（北京非排程不同步） |
| `kafka.topic.schedule_data_recovery` | schedule_data_recovery | 4e-3① | topic 名 |

---

## 附录 B：关键设计点与易踩坑

### B.1 设计点（为什么这么设计）

1. **DAG 按版本缓存而非按项目**（3b）：规则是模板级的，缓存 key=version 让 100 容量服务海量项目。代价是反序列化深拷贝后每次重建引用。
2. **数据拉取收敛**（4c-1）："多次RPC→5次"——逐节点查询翻成批量上下文构建，O(节点数) 次 RPC 变 O(1)。
3. **规则选择 = 排序取第一组**（4d-3(1)）：ruleId 分组 + 精确优先排序，无需优先级字段即实现"最精确规则生效"。
4. **变化才写**（4e-4）：removeIf 模式防重算风暴下的无效写放大，返回值同时控制下游同步触发。
5. **三条独立同步通道**（4e-3）：回排程走 MQ、回 starlord 走异步 RPC、回供应商走 Spring 事件+MQ——各自独立失败互不影响主计算。
6. **汰换兜底**（4d-3(4)）：max(计算值, 汰换+N自然日)——换供应商不背旧账。
7. **考核时间回写轨迹**（4e-1）：updateAssessmentTimeInTrack 让结果沿 DAG 链式传播——这是"下游用上游考核时间当输入"的闭环基础。

### B.2 易踩坑（症状 → 根因 → 处置）

| 症状 | 根因 | 处置 |
|------|------|------|
| 刷了但结果没变 | DAG 缓存 10 分钟未过期 | 先 `POST /api/remove/schedule/dag/cache` 再刷 |
| 刷数中途中断 | 单项目抛异常整批 `throw` | 拆小批重跑；日志找第一个失败项目 |
| 接口成功但项目没算 | 白名单/开关外、`version=null`、`scheduleParam=null` 全部**静默跳过** | 日志搜"未配置排程规则"；`GET /api/test/schedule/whiteList` 验准入 |
| 考核时间没同步 starlord | `syncMaterialTask=inSchedule=false`（白名单单≠灰度单）或用工节点非 mode=7 | 确认排程开关状态 |
| 供应商没收到考核变更 | 订单创建时间<synEffectiveTime / 区域黑名单 / 非送达指标 / 时间无变化 | 查 `IndicatorChangedListener.checkValidToProducer` 四道过滤 |
| 前置时间取了"未来"的考核时间 | `figure_by_assessment_v2` 只管**货单**节点，用工节点无开关限制 | 排查时分清载体类型 |
| 异步同步丢失 | `rpcAsyncExecutor` 回写失败仅记日志无重试 | 日志搜"syncAssessmentTime"错误手动补 |
| 结果 JSON 被覆盖 | 直接改 DB 的 `base_eval_result` 绕过 merge | 走接口重算，mergeIndicator 是增量合并 |
| 用工节点全部没算 | 4a-2 RpcException 容错 return，dispatchNodeMap 留空 | 日志搜"查询主材任务失败" |

---

## 附录 C：与旧考核接口对照（防拿错工具）

| 维度 | 旧 `/api/refresh/eval/projectOrder` | 新 `/api/refresh/eval/schedule/projectOrder`（本文） |
|------|-----------------------------------|---------------------------------------------|
| 载体 | 货单事件（transEventNodeByOrder） | 项目级规则 DAG |
| 准入 | 排程开关 true **跳过**（旧单才走旧算） | 白名单 **或** 排程开关 true 才算 |
| 异常行为 | catch 后 continue | catch 后 throw 中断整批 |
| 结果存储 | `baseEvalResult`+`cusEvalResult` | `baseEvalResult` / `indicator_task_dispatch_node` |
| 下游同步 | 有限 | MQ×2 + 异步 RPC 三通道 |

## 相关笔记

- [[排程考核（ScheduleIndicator）刷新与规则构建业务流梳理]] —— 同接口的早期方法分工版，本文是其自顶向下递归深化版
- [[考核系统刷数接口使用指南（重算入口×批量调用SOP）]] —— 本接口怎么调用、分批 SOP
- [[考核时间计算逻辑梳理（工作日自然日与节假日日历）]] —— 4d-3(3) 时间算法的底层细节
- [[考核系统上下游交互清单（对端系统×交互信息总表）]] —— 附录 A 清单的系统级全景
- [[考核系统scm-workbench-功能与代码学习指南]] —— 系统整体学习入口
- [[📚 排程材料配置四表关联链路]] —— 规则配置侧（artisan-construction）的表结构链路
