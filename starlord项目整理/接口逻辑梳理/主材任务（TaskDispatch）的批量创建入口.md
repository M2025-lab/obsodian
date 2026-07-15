主要要做的分为：
查询任务模板
根据模板构建任务框架 taskDispatch、taskDispatchNode
批量创建 taskDispatch
批量创建 taskDispatchNode
对已激活的节点推送消息通知

关键入参只有订单号、供应商-品类列表，主要是根据供应商-品类列表进行模板创建。
1.根据供应商-品类列表查询模板，并且过滤掉实例表中的已经生成实例的任务（还是依据供应商-品类）
2.根据主材任务模板id查询任务节点配置，然后组合任务配置和节点配置
3.查询并加入订单信息。
4.buildWithConfig在这个方法中以供应商-品类的维度去依据主材任务配置和节点配置、订单信息生成主材任务实例和节点实例
5.根据订单中的信息给任务实例和节点实例进行创建人的配置
6.根据依赖类型计算激活时间。
7.生成主材任务实例
8.根据生成后的实例id与节点进行绑定
9.节点实例落库
10.对已经激活的发送消息---逻辑未知、入参位置、发给谁未知
11.事务提交后异步激活任务
12.发送"任务创建"状态变更消息到 MQ



> 类路径：`com.ke.utopia.service.v2.impl.TaskDispatchBatchCreateServiceImpl`
> 入口方法：`#invoke(MaterialBatchCreateBO)`

## 整体定位

根据主材任务的**模板配置**（`MaterialTask`），为某个**施工项目**批量生成**具体的调度任务实例**（`TaskDispatch` + `TaskDispatchNode`）。

---

## 方法泳道图

```
invoke(MaterialBatchCreateBO)
  │
  ├─ ① prepareData()
  │    ├─ prepareCfg()          查询主材任务模板 (MaterialTask)
  │    ├─ 过滤已生成的实例       避免重复生成
  │    └─ 获取订单详情          (ProjectOrder)
  │
  ├─ ② buildWithConfig()       构建 TaskDispatch + TaskDispatchNode 框架。 根据模板来创建任务实例
  │    ├─ 遍历 MaterialTask 模板
  │    ├─ 构建 TaskDispatch（主表）
  │    └─ 构建 TaskDispatchNode（子表节点，按模板节点顺序排序）
  │
  ├─ ③ buildTaskDispatch()     填充 task_dispatch 的补充字段
  │    ├─ buildWithCreator()          设置操作人
  │    └─ buildTaskDispatchWithTime() 计算计划激活时间（核心业务逻辑）
  │
  ├─ ④ 批量保存 task_dispatch       ← 事务内
  │
  ├─ ⑤ buildTaskDispatchNode()     填充 task_dispatch_node
  │    ├─ fillWithTaskDispatchId()      关联外键
  │    ├─ buildWithExecutor()           分配执行人/角色
  │    └─ buildTaskDispatchNodeWithTime()  计算预计时间 & 平台审核时间
  │
  ├─ ⑥ 批量保存 task_dispatch_node   ← 事务内
  │
  ├─ ⑦ pushMsg()                   对已激活的节点推送消息通知
  │
  ├─ ⑧ activateTaskDispatchAsync() 事务提交后异步激活任务
  │    └─ afterCommit → CompletableFuture 逐个激活
  │
  └─ ⑨ afterTransactional()        发送"任务创建"状态变更消息到 MQ
```

---

## 各步骤详解

### ① `prepareData` — 准备数据

**`prepareCfg`** — 查询主材任务模板：

1. 根据 `productComboId`（产品套餐）、`sourceType`（来源）、`supplier_code + material_code` 列表，批量查询 `MaterialTask`（主材任务配置模板）
2. 过滤（`queryExistCfgTaskId`）：查询当前项目下**已经生成过**的任务实例 `TaskDispatch`，排除已存在的 `templateId`，避免重复生成
3. 对剩余有效的 `MaterialTask`，查询其关联的节点配置 `MaterialTaskNode`，组装 `materialTask.setMaterialTaskNodeList(taskNodes)`

**获取订单信息**：调用 `projectOrderManager.getProjectOrder()` 获取 `ProjectOrderDetailBO`，用于后续填充操作人、项目编号等信息

### ② `buildWithConfig` — 构建任务框架

遍历每个有效的 `MaterialTask` 模板：

| 构建对象 | 关键操作 |
|---------|---------|
| **`TaskDispatch`** | 填充模板 ID、物料编码、供应商、任务类型、流程批次；初始状态统一为 `UN_ACTIVE`；节点类型排序后存到 `nodeTask` 字段，`currentNodeType` 取第一个节点 |
| **`TaskDispatchNode`** | 每个 `MaterialTaskNode` → 一个 `TaskDispatchNode`；初始状态 `UN_ACTIVE`；如果是第一个节点且任务已激活，该节点设为 `UNCOMPLETED`；设置执行角色（优先 `roleTypes`） |

### ③ `buildTaskDispatch` — 填充补充字段

**`buildWithCreator`**：设置创建人 ID。

**`buildTaskDispatchWithTime`**（核心业务逻辑）—— 根据 `activateMode` 计算计划激活时间：

| 激活模式 | 计算逻辑 |
|---------|---------|
| **`PLAN_TIME`（排期时间）** | 根据 `activateRelationCode` 查找项目排期中的工序，取该工序的 `开始时间 + 偏移量(activateDuration)` |
| **`IMMEDIATE`（立即激活）** | 设置为 `new Date()` |
| **`DEPENDENT_NODE`（节点依赖）** | 查询依赖的前置节点任务：① 如果有未完成的 → 不激活（`initDate`）② 全部完成 → 取前置节点最后修改时间作为激活时间 |
| **其他** | 默认 `initDate`（无限制） |

### ⑤ `buildTaskDispatchNode` — 填充子节点

| 子步骤 | 操作 |
|-------|------|
| `fillWithTaskDispatchId` | 设置 `taskDispatchNode.setTaskDispatchId(x.getId())` 外键 |
| `buildWithExecutor` | 调用 `installerTaskService.assignExecutor()` 分配执行人/角色 |
| `buildTaskDispatchNodeWithTime` | 调用 `estimatedTimeService` 计算 `estimatedTime`（预计开始时间）和 `platformCheckTime`（平台审核时间） |

### ⑧ `activateTaskDispatchAsync` — 事务后异步激活

注册 `TransactionSynchronizationAdapter`，在**事务提交后**（`afterCommit`），通过 `CompletableFuture.runAsync` **异步**逐个调用 `dispatchCreateService.activateTaskDispatch(taskDispatch)`。

> 关键设计：任务的实际激活在事务提交后异步执行，避免长事务阻塞。

### ⑨ `afterTransactional` — 发送消息通知

对每个新创建的 `TaskDispatch`，调用 `materialTaskProducer.publishTaskStateChange()` 发送 `StateChangeType.CREATE` 消息到 MQ，下游系统消费。

---

## 数据流总结

```
输入:
  MaterialBatchCreateBO
    ├─ productComboId    产品套餐 ID
    ├─ projectOrderId    项目 ID
    ├─ sourceType        来源
    └─ createList        供应商+物料+订单号 列表

输出:
  task_dispatch 表          N 条记录（每个物料一个调度任务）
  task_dispatch_node 表      N×M 条记录（每个任务 M 个节点）
  MQ 消息                   创建事件通知
  推送通知                   已激活节点的即时推送
```

---

## 业务场景示例

```
装修项目"套餐A"
  ├─ 瓷砖  → TaskDispatch(调度任务)
  │           ├─ Node: 量尺  → UNCOMPLETED (激活)
  │           ├─ Node: 送货  → UN_ACTIVE
  │           ├─ Node: 安装  → UN_ACTIVE
  │           └─ Node: 验收  → UN_ACTIVE
  ├─ 地板  → TaskDispatch(...)
  ├─ 卫浴  → TaskDispatch(...)
  └─ ...
```

- 每个 `TaskDispatch` 代表一个主材品类的施工调度任务
- 根据项目排期或依赖条件，计算合适的激活时间
- 激活后，安装师傅/协调员就能看到并开始处理

---

## 激活时间计算逻辑详解（`buildTaskDispatchWithTime`）

该方法在 `③ buildTaskDispatch` 中被调用，核心逻辑是根据 `MaterialTask` 模板上配置的 `activateMode`（激活模式）分四种情况计算 `TaskDispatch` 的 `planActivateTime`（计划激活时间）：

```java
for (TaskDispatch taskDispatch : context.getInserts()) {
    MaterialTask materialTask = materialTasksMap.get(taskDispatch.getTemplateId());

    if (ActivateModeEnum.isPlanTime(materialTask.getActivateMode().intValue())) {
        // 情况一：排期时间依赖
    } else if (LangUtils.isImmediatelyActivate(materialTask.getActivateMode())) {
        // 情况二：立即激活
    } else if (ActivateModeEnum.isDependentNode(materialTask.getActivateMode().intValue())) {
        // 情况三：节点依赖
    } else {
        // 情况四：其他（兜底）
    }
}
```

### 情况一：`PLAN_TIME` — 按排期工序时间激活

**适用场景**：任务激活依赖于项目排期中某个工序的完成时间（如"瓦工完工后3天"）。

```
查项目排期 → 找到模板配置的关联工序 → 取该工序的开始时间 + 偏移量(方向+天数) → 设为激活时间
```

```java
ProjectScheduleItemBO projectScheduleItemBO = projectScheduleItemMap.get(materialTask.getActivateRelationCode());
if (Objects.isNull(projectScheduleItemBO)) {
    // 排期里找不到对应的工序 → initDate（暂不激活）
    taskDispatch.setPlanActivateTime(CommonConstant.getInitDate());
} else {
    // 取排期工序的开始时间 + 偏移量（方向由 ScheduleRelationEnum 控制）
    Date planActivateTime = DateUtil.getEstimatedTime(
        projectScheduleItemBO.getStartDate(),
        relationEnum,                         // 偏移方向（提前/延后）
        materialTask.getActivateDuration()    // 偏移天数
    );
    taskDispatch.setPlanActivateTime(planActivateTime);
}
```

> **例子**：排期里"瓦工完工"是 7月20日，模板配了延后2天 → 激活时间 = 7月22日

### 情况二：`IMMEDIATE` — 立即激活

**适用场景**：任务创建后马上就需要处理（如"量尺"通常创建即激活）。

```java
taskDispatch.setPlanActivateTime(new Date());
```

### 情况三：`DEPENDENT_NODE` — 按前置节点依赖激活

**适用场景**：当前任务依赖另一个任务实例的完成（如"送货"依赖"量尺"完成）。

```
查前置任务 →
  ├─ 没有前置任务 → initDate（暂不激活）
  ├─ 有前置任务且存在未完成的 → initDate（暂不激活）
  └─ 有前置任务且全部完成 → 取前置任务的最后修改时间作为激活时间
```

```java
List<TaskDispatch> taskDispatchList = dispatchCreateService.queryDependentNodeTask(taskDispatch);

if (CollectionUtils.isEmpty(taskDispatchList)) {
    taskDispatch.setPlanActivateTime(CommonConstant.getInitDate());
} else {
    List<TaskDispatch> unFinishedList = taskDispatchList.stream()
        .filter(e -> ProcessStatusEnum.FINISHED.getValue().byteValue() != e.getProcessStatus())
        .collect(Collectors.toList());

    if (!CollectionUtils.isEmpty(unFinishedList)) {
        // 有未完成的 → 不激活
        taskDispatch.setPlanActivateTime(CommonConstant.getInitDate());
        return;
    }
    // 全部完成 → 取前置任务的最后修改时间
    taskDispatch.setPlanActivateTime(taskDispatchList.get(0).getGmtModified());
}
```

### 情况四：其他（默认兜底）

```java
taskDispatch.setPlanActivateTime(CommonConstant.getInitDate());
```

`CommonConstant.getInitDate()` 是一个早期固定日期（通常为 `1970-01-01` 或 `2000-01-01`），表示**暂不激活**，后续由 `activateTaskDispatchAsync` 或其他激活机制触发。

### 关键设计要点

| 要点 | 说明 |
|------|------|
| **计算的是"计划激活时间"，不是"实际激活"** | 实际激活动作是在 `activateTaskDispatchAsync` 中异步执行的，这里只算出什么时候可以激活 |
| `initDate` 含义 | 不等于"永远不激活"，而是"暂时不触发激活条件"，可能在后续通过其他入口手动激活 |
| `switch.immediately.active.after.create` | 类顶部的开关配置，控制创建后是否立即激活，和这里的激活模式是两个独立维度 |

---

## pushMsg 推送消息逻辑详解

### 代码

```java
private void pushMsg(TaskDispatchCreateContext context){
    // ① 收集所有新创建的 TaskDispatch ID
    List<Long> taskDispatchIds = context.getInserts().stream()
        .map(TaskDispatch::getId).collect(Collectors.toList());

    // ② 查询这些任务中状态为 UNCOMPLETED（未完成）的节点
    ArrayList<Byte> unCompletedList = Lists.newArrayList(TaskDispatchNodeStatusEnum.UNCOMPLETED.getValue().byteValue());
    List<TaskDispatchNode> activeDispatchNodeList = taskDispatchNodeDao.listByParam(taskDispatchIds, null, unCompletedList);

    // ③ 逐个推送消息
    for (TaskDispatchNode node : activeDispatchNodeList) {
        messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(node.getId(), TaskDispatchNodeStatusEnum.UNCOMPLETED);
    }
}
```

### 逻辑拆解

| 步骤 | 做了什么 |
|------|---------|
| ① **取 ID 列表** | 从刚创建的 `TaskDispatch` 列表中提取所有主键 ID |
| ② **查已激活的节点** | 筛选这批任务中状态为 `UNCOMPLETED`（未完成）的节点。注意：只有 `IMMEDIATE` 模式的第一个节点会在此阶段被设为 `UNCOMPLETED` |
| ③ **推送通知** | 对每个已激活的节点，调用 `messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange()` 发推送 |

### 业务含义

只在任务创建时，对**已经自动激活了的节点**发推送通知。比如 `IMMEDIATE` 模式的"量尺"任务，创建后第一个节点就是 `UNCOMPLETED` 状态，此时给执行人发通知。

而那些 `PLAN_TIME` 或 `DEPENDENT_NODE` 模式的任务，所有节点都是 `UN_ACTIVE`，不会被查到，所以**不会在这里推送**。

---

### 推送底层完整调用链

```
pushMsg(context)
  → messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(nodeId, UNCOMPLETED)
      │
      ├─ ① newMessagePushService.push(nodeId)        ← 新版推送（2023+）
      │
      └─ ② 遍历 TaskNodeMsgEnum.values()             ← 旧版推送
              └─ pushMsg(taskDispatch, node, enum)
                    │
                    ├─ PushClient.sendPushMsg()       ← IM/App 推送
                    └─ PushClient.sendPushNotice()    ← 短信推送
```

#### 入口方法 `pushMessageWhenTaskDispatchNodeProcessChange`

```java
public void pushMessageWhenTaskDispatchNodeProcessChange(Long taskDispatchNodeId,
        TaskDispatchNodeStatusEnum progressStatus) {
    // 1. 查节点
    TaskDispatchNode node = taskDispatchNodeDao.getById(taskDispatchNodeId);
    // 2. 查主任务
    TaskDispatch taskDispatch = taskDispatchService.getById(node.getTaskDispatchId());
    // 3. 新版推送
    taskExecutorHelper.awaitExecutor(Lists.newArrayList(node.getId()),
        (t) -> newMessagePushService.push(t));
    // 4. 旧版推送：遍历枚举，匹配则发
    for (TaskNodeMsgEnum taskNodeMsgEnum : TaskNodeMsgEnum.values()) {
        if (taskNodeMsgEnum.shouldPushMsg(taskDispatch, node)) {
            pushMsg(taskDispatch, node, taskNodeMsgEnum);
        }
    }
}
```

---

### 分支 1：新版推送（NewMessagePushService.push）

```java
push(nodeId) {
    TaskDispatchNode node = taskDispatchNodeDao.getById(nodeId);
    NMaterialTaskForm taskForm = getTaskForm(node);   // 查模板配置
    pushMessage(taskForm, node);                       // 遍历 materialPushCfgList
        → message()                                    // 按配置发推送
            → PushClient.sendPushMsg(collect, param)   // → IM 推送
}
```

配置从模板表 `NMaterialTaskCfg` 的 `materialPushCfgList` 中读取：

| pushCfg 字段 | 含义 |
|-------------|------|
| `noticeTimeType` | 何时推送：完成时/开始时/将延期/已延期 |
| `relationCode` | 发给谁：关联任务节点 / 特定角色 |
| `noticeParent` | 是否同时发给上级 |

---

### 分支 2：旧版推送（TaskNodeMsgEnum）

`TaskNodeMsgEnum` 是一个枚举，**硬编码**了每种场景的消息模板。每个枚举值包含：

```java
MEASURE_START_UNCOMPLETED(
    TaskTypeEnum.MEASURE,                          // 任务类型：测量
    NodeTypeEnum.START,                            // 节点类型：启动
    TaskDispatchNodeStatusEnum.UNCOMPLETED,        // 节点状态：未完成
    PushChannelEnum.GONGZUOZHUSHOU,                // 推送渠道：工作助手
    null,                                          // receiverUcIds: 不指定（用节点执行人）
    "你收到一个新的测量任务",                        // 标题
    "push.measure.start.active.jump.url",          // 跳转链接 Key（从 Apollo 取）
    "项目地址：#{#projectOrderInfo.address}...",    // SpEL 模板内容
)
```

**判断是否推送（`shouldPushMsg`）**：通过 任务类型 + 节点类型 + 节点状态 三者匹配。子类可覆写添加额外条件。

**消息内容拼装**：使用 **SpEL（Spring Expression Language）** 模板引擎，从 Apollo 的 `message` namespace 读取跳转 URL，运行时注入 context 变量（`#taskDispatch`、`#taskDispatchNode`、`#projectOrderInfo`、`#messagePushClient`、`#esTime` 等）。

**接收人**：默认取 `node.getExecutorId()`（节点执行人 UCID）。

---

### 分支 1 和 2 合并后的最终发送：PushClient

新旧两个分支最终都走到 `PushClient.sendPushMsg()`：

```java
public void sendPushMsg(Set<String> ucIdList, TaskMessageParam param) {
    // 1. 按渠道获取配置
    PushConfigProperties config = getConfig(param.getChannel());
    //    渠道 RENWUTIXING  → PushMaterialTaskCenterProperties
    //    渠道 GONGZUOZHUSHOU → PushMaterialOrderNoticeProperties

    // 2. 组装请求参数（appId, passcode, 接收人, 消息体）
    PushGroupSendParam groupSendDto = ...;
    groupSendDto.setTo_ucids(ucIdList);
    groupSendDto.setMsg_payload(JSON.toJSONString(param));

    // 3. 异步发送
    CompletableFuture.runAsync(() -> {
        pushHttpUtil.postJson(config.getUrl(), paramMap, config);
    });
}
```

**`PushHttpUtil.postJson()`** — 实际的 HTTP 调用：

```
POST {config.url}
Headers:
  Content-Type: application/x-www-form-urlencoded; charset=utf-8
  Lianjia-Im-Protocal-Version: {version}
  Lianjia-App-Id: {appId}
  Lianjia-Im-Passcode: {passcode}
Body: FormBody（to_ucids, msg_payload, from_ucid, msg_type 等）
```

发送到**Lianjia 内部 IM 推送平台**（消息中心服务）。

---

### 三个推送渠道

| 渠道 | 枚举值 | 目标 | 配置方式 | 底层调用 |
|------|--------|------|---------|---------|
| 任务提醒 | `RENWUTIXING` | IM/App 推送 | `push.material-task-center.property.*` | `PushHttpUtil.postJson(url)` |
| 工作助手 | `GONGZUOZHUSHOU` | IM/App 推送 | `push.material-order-notice.property.*` | `PushHttpUtil.postJson(url)` |
| 短信 | `NOTICE` | 手机短信 | 模板 ID + 变量 | `smsService.publishSms()` |

---

### 涉及的外部服务

| 外部系统 | 用途 | 调用方式 |
|---------|------|---------|
| **IM 推送服务** | 发送 App 推送 | `PushHttpUtil` → OkHttp POST |
| **短信服务** | 发送短信通知 | `SmsService.publishSms()` |
| **shuttle-order（订单服务）** | 查询订单详情 | Feign → `ProjectOrderService` |
| **ceres（人员中心）** | UCID 映射、人员详情 | Feign → `PersonHighServiceApi` |
| **customer-home（客源）** | 家居顾问查询 | Feign → `CustomerHomeFeign` |

---

### 回到 pushMsg 方法本身

在第 ⑦ 步 `pushMsg()` 中，只对 `UNCOMPLETED` 状态的节点推送，也就是说：

- 只有 **立即激活（IMMEDIATE）** 模式的任务会在这里发推送
- 配置了排期时间或节点依赖的任务，它们的推送会在后续 `activateTaskDispatchAsync` 异步激活节点时才触发
