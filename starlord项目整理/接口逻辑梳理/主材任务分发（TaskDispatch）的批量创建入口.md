主要要做的分为：
查询任务模板
根据模板构建任务框架 taskDispatch、taskDispatchNode
批量创建 taskDispatch
批量创建 taskDispatchNode
对已激活的节点推送消息通知




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
  ├─ ② buildWithConfig()       构建 TaskDispatch + TaskDispatchNode 框架
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
