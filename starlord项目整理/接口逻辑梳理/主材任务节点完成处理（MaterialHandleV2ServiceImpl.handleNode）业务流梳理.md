---
source: code-flow
date: 2026-07-17
class: com.ke.utopia.servicev2.impl.MaterialHandleV2ServiceImpl
method: handleNode
---

## 功能概述

主材任务节点完成处理的核心入口。处理一个任务节点（TaskDispatchNode）的完成逻辑，包括：自动完成前置未激活的依赖、激活当前任务、标记当前节点完成、决定下一节点（合格进入下一节点 / 不合格回退重启）、通知供应链系统、发MQ消息、激活下一任务。

## 入口

- **入口方法**: `MaterialHandleV2ServiceImpl.handleNode(DispatchHandleParam handleParam, OperatorDTO operator)`
- **返回类型**: `Boolean` — 数据库更新是否成功
- **触发方式**: 内部调用（被 `handleTask`、`batchHandleNode`、`handleWithoutTask` 调用），最终由 Controller 触发
- **关键注解**: `@Transactional(rollbackFor = Exception.class)` 整个方法在一个事务内
- **核心参数**:
  - `taskDispatchNodeId` — 要完成的节点ID（与 `salesSubOrderNo` 二选一）
  - `qualified` — 1=合格 / 2=不合格，决定下一节点走向
  - `noticeRetainTime` — 通知类时间字段（如安装时间）
  - `salesSubOrderNo` — 零售子单号（无节点ID时走零售无任务路径）

---

## handleNode 处理逻辑

### 一、前置检查（参数 → 查节点 → 查任务 → 零售特殊路径）

```
┌─────────────────────────────────────────────────────┐
│ 前置检查                                              │
│                                                     │
│ ① 参数校验                                           │
│ ② 查节点 → 查任务 → 零售特殊路径                       │
│                                                     │
│ 一句话：先校验参数合法性，再查节点和任务是否存在，零售   │
│ 无模板配置的单子走创建任务的特殊分支。                   │
└─────────────────────────────────────────────────────┘
```

- **参数校验**：`taskDispatchNodeId` 和 `salesSubOrderNo` 至少一个不为空
- **taskDispatchNodeId 为空**？→ `handleWithoutTask()` 零售无任务路径
  - 查零售单详情 → 查订单详情 → 查商品信息 → 查分公司
  - 有配置模板 → 创建任务并递归进入 handleNode
  - 无配置模板 → createMaterialTaskWithDefaultParam 默认生成
- **查节点** TaskDispatchNode（按 `taskDispatchNodeId`）
  - 不存在 → 抛异常 `TASK_NONE_EXIST`
- **查任务** TaskDispatch（按 `dispatchNode.getTaskDispatchId()`）
  - 不存在 → 抛异常 `TASK_NONE_EXIST`
- **零售单（RETAIL）且 processCode 为空**？
  - 复制 `XlsOrderInfoBo` → `completeTaskDispatchNode` 完成节点 → `createMaterialTaskWithDefaultParam` 创建任务

> 一句话：如果当前节点还没激活，先把前置任务和前置节点全部自动完成，再激活当前任务，然后继续处理当前节点。

### 二、节点状态判断

- **节点已完成（COMPLETED）**？→ 直接 `return true`（幂等，避免重复处理）
- **节点未激活（UN_ACTIVE）**？→ 执行三步曲：
  1. **completePreTask(taskDispatch, operator)**
     - 递归完成前置任务（沿路由链向前追溯，逐一完成前置节点和前置任务）
  2. **materialActivateV2Service.doActivateTaskDispatch(taskDispatch)**
     - 激活当前任务（taskDispatch 的 `processStatus` 从 UN_ACTIVE → ACTIVE）
  3. **completePreTaskNode(taskDispatch, dispatchNode, operator)**
     - 完成当前任务中所有 `nodeType` < 当前节点的未完成节点

> 一句话：如果当前节点还没激活，先把前置任务和前置节点全部自动完成，再激活当前任务，然后继续处理当前节点。

### 三、完成当前节点（completeTaskDispatchNode）

- **补齐 operator**：空时用节点执行人信息
- **组装更新数据**：提交时间、完成状态（COMPLETED）、就绪状态（READY）、图片、附件JSON、noticeRetainTime
- **有 location 位置数据**？→ 插入 `TaskHandleExtension`
- **endTime 为空**？→ 用 `submitTime` 或 `new Date()`
- **执行数据库更新**：`taskDispatchNodeDao.updateById`
- **量尺 / 复尺任务**？→ `saveMeasureInfo()` 保存复尺模板数据

> 一句话：将当前节点标记为已完成（processStatus=COMPLETED），并保存提交数据（图片、位置、复尺模板）到数据库。

### 四、完成后置处理

- **重新查节点最新数据**（currentNewNode）
- **发 IM push 通知**：节点状态变为 COMPLETED
- **有 noticeRetainTime**？→ 发时间变更 MQ 消息
- **有 processCode**？→ `estimatedTimeV2Service.updateDelayDays()` 计算本任务延期天数
- **变更单（nodeType >= CHANGE）**？
  - 直接完成任务 taskDispatch（processStatus = COMPLETED）
  - `return true`（不走后续的下一节点逻辑）

> 一句话：节点完成后发通知、计算延期天数；如果是变更单节点则直接结束整个任务，不继续激活下一节点。

### 五、决定下一节点（合格 → 激活下一节点 / 不合格 → 回退重启）

- **不合格（UNQUALIFIED）**？
  - `materialRestartV2Service.restartProcess()` — 重启并激活流程第一个节点
  - 不修改 taskDispatch 状态
  - 复尺驳回 → push 通知给通知节点执行人
  - `nextNode` = 重启后第一个节点的 nodeType
- **合格 / 未传 qualified**？
  - `NodeTypeUtil.getNextNodeType()` — 根据 `nodeTask` 配置找下一节点类型
  - `taskDispatchNodeDao.activateNextNode()` — 激活该任务的下一节点
  - `nextNode` = 计算出的下一节点类型

> 一句话：合格则根据 nodeTask 配置激活下一个节点，不合格则回退重启到流程的第一个节点。

### 六、通知下一节点 × 修改 currNode × 通知供应链

- **有 nextNode**？
  - 查询该类型的 TaskDispatchNode 列表
  - 逐个发 IM push（状态变为 UNCOMPLETED）
- **prepareHandleDispatch()** 组装 TaskDispatch 更新：
  - `currentNodeTime` = now
  - `currentNodeType` = nextNode（无下一节点则 1000）
  - `nextNode == null` → `processStatus` = COMPLETED
- **taskDispatchDao.updateById** 更新 TaskDispatch
- **通知供应链**：
  - **HOME2.5 模式** → `sendVssFinish()` + `sendVssNew()`
  - **其他模式** → `sendOmsMsg()`

> 一句话：激活下一节点后发通知，更新任务的当前节点信息，然后根据业务模式（HOME2.5/其他）通知供应链系统。

### 七、MQ 消息 × C端通知 × 激活下一任务

- **发 TaskNodeChange MQ**（节点完成消息）
- **发 TaskDispatchChange MQ**（任务变更消息）
- **无后续节点（nextNode == null）**？
  - `materialCustomerProducer.publishMaterialCustomer()` 给 C 端发送任务完成通知
- **task 已完成（COMPLETED）**？
  - `materialActivateV2Service.activateNextTaskDispatch(task)` 激活路由链中的下一个 TaskDispatch
- **afterHandle(currentNewNode, task)** — 后置异步操作

> 一句话：发送节点和任务变更的 MQ 消息，如果任务全部完成则通知 C 端并尝试激活路由链中的下一个任务。

---

## 核心子逻辑

### completePreTask — 递归完成前置任务

```
完整前置任务 → 标记前置已完成 → 激活当前任务
```

- **taskDispatch 已激活（!= UN_ACTIVE）**？→ 直接 return
- **查路由** NMaterialRoute，找 `sourceCode`（前置 processCode）
  - 无路由 / 无 sourceCode → return
- **找前置 TaskDispatch**：根据 `processCode + 订单ID + 主材 + 供应商`
  - 找不到 → return
- **前置任务也未激活（UN_ACTIVE）**？
  - `completePreTask(preTaskDispatch, operator)` 递归继续找更前置的
- **批量完成前置节点**：
  - 前置任务的所有未完成节点 → 标记为 COMPLETED
  - 区分 `startTime`=初始值的节点（开始时间置为 endTime）
- **发 TaskNodeChange MQ**（通知前置节点变更）
- **完成前置 TaskDispatch**：`processStatus=COMPLETED`, `currentNodeType=1000`
- **发 TaskDispatchChange MQ**（通知前置任务变更）

> 一句话：沿着路由链递归向前追溯，找到最前面的未激活前置任务，然后逐层往回将前置任务的所有节点和任务本身全部标记为已完成。

### completePreTaskNode — 完成当前任务的前置节点

- **查节点**：当前任务中所有 `nodeType` < 当前节点 且 未完成的节点
- **按 startTime 分组**：
  - 已有开始时间的 → 仅更新完成状态
  - 初始 startTime 的 → `startTime` 置为 `endTime` 再更新
- **批量更新**：节点标记为 COMPLETED
- **发 TaskNodeChange MQ**：通知前置节点变更

> 一句话：把当前任务中排在当前节点前面的所有未完成节点自动完成。

### afterHandle — 事务提交后的异步操作

- **currentNewNode 为空**？→ return
- **TaskDispatch 已完成（FINISHED）**？
  - 双写同步 OMS 签收状态（`scmManager.serviceOrderStatusChange`）
- **事务处于活跃状态**？
  - 注册 `TransactionSynchronizationAdapter`，事务提交后异步执行：
    - `taskProgressTipService.addCache()` — 更新 redis 进展缓存
    - `updateNodeEstime()` — 更新三种考核时间（首次检测、重启检测、承诺检测）
    - `calculatePlanCompleteTimeByUpdate()` — 计算计划完成时间
- **事务不活跃**？
  - 直接执行上述异步操作
- **复尺完成（HOME2.5_MANPOWER 模式）**？
  - 查找同项目同材料的 DESIGN_REVIEW 任务
  - 如有未完成的 → 主动推送 VSS 通知

> 一句话：事务提交后异步更新进展缓存、考核时间和计划完成时间；如果是复尺完成，还主动推送报价变更任务通知。

---

## 条件分支汇总

| 条件 | 走向 | 结果 |
|------|------|------|
| `taskDispatchNodeId == null` | → `handleWithoutTask` | 零售无任务路径 |
| 节点不存在 | → 抛异常 | `TASK_NONE_EXIST` |
| 任务不存在 | → 抛异常 | `TASK_NONE_EXIST` |
| RETAIL + `processCode==""` | → 特殊零售路径 | 完成节点 + createMaterialTask |
| 节点 COMPLETED | → 直接 return | 幂等保护 |
| 节点 UN_ACTIVE | → 前置完成 + 激活 | 自动完成前置依赖 |
| `nodeType >= CHANGE`（变更单） | → 完成任务，不激活下一节点 | taskDispatch 直接 COMPLETED |
| `qualified==UNQUALIFIED`（不合格） | → `restartProcess` 回退重启 | 回到流程第一个节点 |
| 合格 / 未传 qualified | → `activateNextNode` | 走正常下一节点 |
| `nextNode == null`（无下一节点） | → 通知 C 端 | 任务完成 |
| TaskDispatch COMPLETED | → `activateNextTaskDispatch` | 激活路由链下一任务 |
| HOME2.5 模式 | → sendVssFinish + sendVssNew | 新供应链通知 |
| 其他模式 | → sendOmsMsg | 老 OMS 通知 |

---

## 状态变更汇总

| 属主 | 字段 | 变更前 → 变更后 | 触发条件 | 方式 |
|------|------|----------------|---------|------|
| TaskDispatchNode | processStatus | UN_ACTIVE(1) → COMPLETED(3) | completePreTask / completePreTaskNode 自动完成 | 批量 update |
| TaskDispatchNode | processStatus | UNCOMPLETED(2) → COMPLETED(3) | completeTaskDispatchNode 完成当前节点 | updateById |
| TaskDispatchNode | readinessStatus | → READY(1) | 节点完成时 | updateById |
| TaskDispatch | processStatus | UN_ACTIVE(1) → ACTIVE(2) | doActivateTaskDispatch 激活任务 | updateById |
| TaskDispatch | processStatus | ACTIVE(2) → COMPLETED(3) | 变更单节点完成 / 无下一节点 | updateById |
| 前置 TaskDispatch | processStatus | → COMPLETED(3) | completePreTask 前置完成 | updateByExample |
| TaskDispatch | currentNodeType | → nextNode \| 1000 | 每次节点完成 | updateById |
| TaskDispatch | currentNodeTime | → new Date() | 每次节点完成 | updateById |
| TaskDispatchNode | processStatus | → UNCOMPLETED(2) | activateNextNode 激活下一节点 | 批量 update |

---

## 外部调用和事件

| 类型 | 目标 | 时机 | 内容 |
|------|------|------|------|
| IM Push | messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange | 节点完成时 | 通知节点状态→COMPLETED |
| IM Push | messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange | 激活下一节点 | 通知新节点状态→UNCOMPLETED |
| IM Push | messagePushClient.pushMessageWhenRecheckRejected | 复尺驳回重启时 | 通知复尺驳回 |
| MQ | materialTaskProducer.publishTaskDispatchTimeChange | 有 noticeRetainTime | 时间变更消息 |
| MQ | materialTaskProducer.publishTaskNodeChange | 节点完成时 | 节点状态变更消息 |
| MQ | materialTaskProducer.publishTaskDispatchChange | 任务状态变更 | 任务变更消息 |
| MQ | materialCustomerProducer.publishMaterialCustomer | 无下一节点（任务完成） | 给 C 端发消息 |
| Feign | estimatedTimeV2Service.updateDelayDays | 有 processCode | 计算延期天数 |
| Feign | estimatedTimeV2Service.updateParentNodeEstimatedTimeBatch | afterHandle | 更新三种考核时间 |
| Feign | scmManager.serviceOrderStatusChange | afterHandle + FINISHED | 双写同步 OMS 签收 |
| Feign | omsMessageSyncService.sendVssFinish/sendVssNew | 节点完成 | HOME2.5 模式通知供应链 |
| Feign | omsMessageSyncService.sendOmsMsg | 节点完成 | 其他模式通知供应链 |
| Redis | taskProgressTipService.addCache | 事务提交后异步 | 更新进展缓存 |
| 状态激活 | materialCommonService.calculatePlanCompleteTimeByUpdate | 事务提交后异步 | 计算计划完成时间 |

---

## 待澄清点

- [ ] `completePreTask` 中如果路由链形成环（A→B→A），递归没有深度保护，可能栈溢出
- [ ] `afterHandle` 中的 `updateParentNodeEstimatedTimeBatch` 三种考核时间调用参数一致，疑似重复调用（可确认是否三个时间点的值相同）
- [ ] `CompletableFutureUtils.supplyAsync` 异步调用 `calculatePlanCompleteTimeByUpdate` 的结果未被消费（fire-and-forget），异常被吞掉的风险
- [ ] `qualified` 为 null 时走向激活下一节点路径，未与合格的逻辑区分 — 这是一个"默认走合格"的兜底行为

---

## 总结

- **主干路径**: handleNode → 查节点和任务 → 未激活则自动完成前置依赖并激活 → 标记当前节点完成 → 合格则激活下一节点 / 不合格则回退重启 → 更新 currNode → 通知供应链 → 发 MQ → 无后续节点时通知 C 端并激活下一任务 → afterHandle 异步更新考核时间

- **关键关注点**:
  1. **幂等性**: 节点 COMPLETED 时直接 return，无副作用
  2. **事务边界**: 整个方法在事务内，后置异步操作注册了 TransactionSynchronizationAdapter，确保事务提交后才执行
  3. **并发风险**: batchHandleNode 使用固定 5 线程池并行处理不同节点，但未加锁；两个节点可能属于同一 TaskDispatch 的相同状态，存在竞争条件
  4. **递归深度**: completePreTask 沿路由链递归，无上限保护
  5. **回退路径**: 不合格时 restartProcess 重启到第一个节点，复尺驳回会额外推送通知

---
## 相关文档

- [[starlord项目整理/接口逻辑梳理/主材任务（TaskDispatch）的核心业务逻辑]] — 整体业务逻辑与节点流转
- [[starlord项目整理/接口逻辑梳理/主材任务V2激活处理逻辑（MaterialActivateV2ServiceImpl）]] — V2激活前置逻辑
- [[starlord项目整理/安装流程]] — 安装任务节点（handleNode的目标节点）
- [[starlord项目整理/复尺流程]] — 复尺任务节点（handleNode的目标节点）
