
## 目录
1. [任务激活逻辑](#1-任务激活逻辑)
2. [节点完成任务流转](#2-节点完成任务流转)
3. [驳回/重启逻辑](#3-驳回重启逻辑)
4. [执行人分配逻辑](#4-执行人分配逻辑)
5. [MQ 消息发送机制](#5-mq-消息发送机制)

---

## 1. 任务激活逻辑

### 入口：`DispatchCreateService#activateTaskDispatch`

**文件**: `edar-starlord-service/src/main/java/com/ke/utopia/service/impl/DispatchCreateServiceImpl.java`

### 调用来源
创建入口（`TaskDispatchBatchCreateServiceImpl#invoke`）事务提交后，通过 `activateTaskDispatchAsync` 异步调用。

### 代码流程

```java
@Transactional(rollbackFor = Exception.class)
public void activateTaskDispatch(TaskDispatch taskDispatch) {
    // ① 只有 UN_ACTIVE 状态的任务才能激活
    if (TaskDispatchStatusEnum.UN_ACTIVE.getValue().byteValue() != taskDispatch.getProcessStatus()) {
        return;
    }
    // ② 计划激活时间必须在当天内（从远古日期到当天结束）
    if (taskDispatch.getPlanActivateTime().before(PLAN_ACTIVATE_START_DATE)
            || taskDispatch.getPlanActivateTime().after(DateUtil.getDayEnd())) {
        return;
    }
    // ③ 有 processCode 走 v2 激活
    if (!StringUtils.isEmpty(taskDispatch.getProcessCode())) {
        materialActivateV2Service.activateTaskDispatch(taskDispatch);
        return;
    }
    // ④ 旧版激活
    MaterialTask materialTask = materialTaskDao.getById(taskDispatch.getTemplateId());
    activateTaskDispatch(taskDispatch, materialTask);
}
```

### 核心激活方法

```java
public void activateTaskDispatch(TaskDispatch taskDispatch, MaterialTask materialTask) {
    // ① 检查是否满足激活条件（排期依赖判断）
    if (!shouldActivate(taskDispatch, materialTask)) return;

    // ② 尾款拦截（如果订单有未付款的变更单，只更新时间，不激活节点）
    if (!checkInterceptConfigure(taskDispatch)) {
        updateTaskDispatch(taskDispatch);
        return;
    }

    Date activateTime = new Date();

    // ③ 更新 TaskDispatch 状态
    taskDispatch.setCurrentNodeTime(activateTime);
    taskDispatch.setProcessStatus(TaskDispatchStatusEnum.UNCOMPLETED.getValue().byteValue());
    taskDispatchDao.updateById(taskDispatch);

    // ④ 查询并激活第一个节点
    TaskDispatchNodeExample example = new TaskDispatchNodeExample();
    example.createCriteria().andTaskDispatchIdEqualTo(taskDispatch.getId())
            .andNodeTypeEqualTo(taskDispatch.getCurrentNodeType())
            .andProcessStatusEqualTo(TaskDispatchNodeStatusEnum.UN_ACTIVE.getValue().byteValue())
            .andStateEqualTo(StateEnum.VALID.getValue().byteValue());
    List<TaskDispatchNode> nodeList = taskDispatchNodeDao.listByExample(example);

    if (!CollectionUtils.isEmpty(nodeList)) {
        TaskDispatchNode node = nodeList.get(0);
        // 激活节点：UN_ACTIVE → UNCOMPLETED
        TaskDispatchNode updateNode = TaskDispatchNode.builder()
                .id(node.getId())
                .processStatus(TaskDispatchNodeStatusEnum.UNCOMPLETED.getValue().byteValue())
                .startTime(activateTime)
                .gmtModified(new Date())
                .build();
        taskDispatchNodeDao.updateByPrimaryKeySelective(updateNode);

        // ⑤ 后续处理：派单员任务、同步OMS、发推送通知
        completeAssignerTaskWhenActivate(taskDispatchNode, taskDispatch);
        omsMessageSyncService.sendOmsMsg(taskDispatch, null, taskDispatchNode.getNodeType());
        messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(
                taskDispatchNode.getId(), TaskDispatchNodeStatusEnum.UNCOMPLETED);
    }
}
```

### 激活条件判断（`shouldActivate`）

```java
private boolean shouldActivate(TaskDispatch taskDispatch, MaterialTask materialTask) {
    ScheduleRelationEnum relationEnum = ScheduleRelationEnum.getByValue(
            Integer.valueOf(materialTask.getActivateScheduleRelation()));

    // 排期依赖类型的任务，需要判断订单状态是否已达到要求的工序
    if (ActivateModeEnum.isPlanTime(materialTask.getActivateMode().intValue())
            && afterTodayRelationList.contains(relationEnum)) {
        ProjectOrderDetailBO projectOrder = projectOrderManager.getProjectOrder(taskDispatch.getProjectOrderId());
        return MajorSequenceEnum.canActivateByProjectOrderStatus(
                projectOrder.getStatus(), materialTask.getActivateRelationCode());
    }
    return true;
}
```

### 激活流程图

```
activateTaskDispatch(taskDispatch)
  │
  ├─ 状态不是 UN_ACTIVE? → return（跳过）
  ├─ planActivateTime 不在今天? → return（跳过）
  │
  ├─ 有 processCode → materialActivateV2Service.activateTaskDispatch()
  │
  └─ 旧版激活:
       ├─ shouldActivate()? → 排期依赖：订单状态是否达标？
       │     ├─ 不达标 → return
       │     └─ 达标 → 继续
       ├─ 尾款拦截? → 有未付款变更单 → 只更新时间，不激活节点
       └─ 正式激活：
            ├─ taskDispatch: UN_ACTIVE → UNCOMPLETED
            └─ 第一个节点: UN_ACTIVE → UNCOMPLETED, 设 startTime
                 ├─ 派单员任务处理
                 ├─ 同步 OMS
                 └─ 发推送通知（给执行人）
```

任务激活必须是未激活且激活日期在当天。激活方式有两版，旧版无processCode，若有该字段则使用新版激活。


---

## 2. 节点完成任务流转

### 入口：`MaterialHandleV2Service#handleNode`

**文件**: `edar-starlord-service/src/main/java/com/ke/utopia/servicev2/impl/MaterialHandleV2ServiceImpl.java`

这是 **核心入口**，当执行人完成一个节点时调用。

```java
@Transactional(rollbackFor = Exception.class)
public Boolean handleNode(DispatchHandleParam handleParam, OperatorDTO operator) {
    // 查询节点和任务
    TaskDispatchNode dispatchNode = taskDispatchNodeDao.getById(handleParam.getTaskDispatchNodeId());
    TaskDispatch taskDispatch = taskDispatchDao.getById(dispatchNode.getTaskDispatchId());

    // 如果节点是 UN_ACTIVE 状态，先完成前置任务再激活
    if (dispatchNode.getProcessStatus().intValue() == TaskDispatchNodeStatusEnum.UN_ACTIVE.getValue()) {
        completePreTask(taskDispatch, operator);           // 完成前置任务
        materialActivateV2Service.doActivateTaskDispatch(taskDispatch);  // 激活当前任务
        completePreTaskNode(taskDispatch, dispatchNode, operator);       // 完成前置节点
    }

    // ① 完成当前节点（更新状态为 COMPLETED）
    completeTaskDispatchNode(taskDispatch, dispatchNode, handleParam, operator);

    // ② 推送 "节点完成" 通知
    messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(
            dispatchNode.getId(), TaskDispatchNodeStatusEnum.COMPLETED);

    // ③ 计算延期天数
    if (StringUtils.isNotBlank(dispatchNode.getProcessCode())) {
        estimatedTimeV2Service.updateDelayDays(dispatchNode);
    }

    // ④ 判断是驳回还是正常流转
    Integer nextNode = null;
    if (QualifiedEnum.UNQUALIFIED.getValue().equals(handleParam.getQualified())) {
        // 驳回：重启流程
        List<TaskDispatchNode> restartNodes = materialRestartV2Service.restartProcess(taskDispatch, dispatchNode);
        if (!CollectionUtils.isEmpty(restartNodes)) {
            nextNode = restartNodes.get(0).getNodeType();
            messagePushClient.pushMessageWhenRecheckRejected(taskDispatch, dispatchNode, restartNodes);
        }
    } else {
        // 正常完成：激活下一个节点
        nextNode = NodeTypeUtil.getNextNodeType(taskDispatch.getNodeTask(), dispatchNode.getNodeType());
        taskDispatchNodeDao.activateNextNode(taskDispatch.getId(), nextNode);
    }

    // ⑤ 激活下一个节点后发推送
    if (nextNode != null) {
        List<TaskDispatchNode> nodeList = taskDispatchNodeDao.getByTaskIdAndNoteType(taskDispatch.getId(), nextNode);
        for (TaskDispatchNode node : nodeList) {
            messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(
                    node.getId(), TaskDispatchNodeStatusEnum.UNCOMPLETED);
        }
    }

    // ⑥ 更新 TaskDispatch 表的 currentNodeType
    TaskDispatch updateDispatch = prepareHandleDispatch(nextNode, dispatchNode, operator);
    taskDispatchDao.updateById(updateDispatch);

    // ⑦ 如果任务所有节点都完成了（nextNode == null），标记 TaskDispatch 为 COMPLETED
    //    并激活下一个 TaskDispatch（流程链上的下一个任务）
    if (TaskDispatchStatusEnum.COMPLETED.getValue().byteValue() == task.getProcessStatus()) {
        materialActivateV2Service.activateNextTaskDispatch(task);
    }

    // ⑧ 后续处理：OMS同步、进展变更缓存、依赖任务考核时间变更
    afterHandle(currentNewNode, task);
}
```

### 节点流转流程图

```
handleNode(node, operator)
  │
  ├─ 节点 UN_ACTIVE? → 先完成前置任务 + 激活当前任务 + 完成前置节点
  │
  ├─ ① completeTaskDispatchNode() → 节点 COMPLETED
  │     ├─ 设 processStatus = COMPLETED
  │     ├─ 设 submitTime, endTime, submitBy
  │     └─ 保存附件、位置信息等
  │
  ├─ ② 发推送：节点完成通知
  ├─ ③ 计算延期天数
  │
  ├─ ④ 判断合格/驳回
  │     ├─ 驳回(UNQUALIFIED) → restartProcess() 重启第一个节点
  │     └─ 合格 → activateNextNode() 激活下一个节点
  │             └─ 发推送：新节点通知
  │
  ├─ ⑤ 更新 taskDispatch.currentNodeType
  │
  └─ ⑥ 任务全部完成?
        ├─ 是 → 标记 TaskDispatch COMPLETED
        │      └─ activateNextTaskDispatch() 激活流程链的下一个任务
        └─ 否 → 继续
```

### 节点类型流转规则

节点的流转顺序由 `TaskDispatch.nodeTask` 字段定义，它是一个逗号分隔的节点类型数字串，例如 `"20,40,60,80"`，表示节点按 通知可启动 → 通知启动 → 启动 → 验收启动 的顺序流转。

```java
// 获取下一个节点类型
Integer nextNode = NodeTypeUtil.getNextNodeType(taskDispatch.getNodeTask(), dispatchNode.getNodeType());
// 激活下一个节点
taskDispatchNodeDao.activateNextNode(taskDispatch.getId(), nextNode);
```

---

## 3. 驳回/重启逻辑

### 入口：`MaterialRestartV2Service#restartProcess`

当节点完成时 `QualifiedEnum.UNQUALIFIED`（不合格），触发驳回：

```java
// handleNode 中：
if (QualifiedEnum.UNQUALIFIED.getValue().equals(handleParam.getQualified())) {
    // 重启并激活第一个节点，不修改 taskDispatch
    List<TaskDispatchNode> restartNodes = materialRestartV2Service.restartProcess(taskDispatch, dispatchNode);
    if (!CollectionUtils.isEmpty(restartNodes)) {
        nextNode = restartNodes.get(0).getNodeType();
        // 复尺驳回：推送通知给通知节点执行人
        messagePushClient.pushMessageWhenRecheckRejected(taskDispatch, dispatchNode, restartNodes);
    }
}
```

### 驳回处理的关键逻辑

| 操作 | 说明 |
|------|------|
| 状态回退 | 将已完成的节点和后续节点回退到 UN_ACTIVE 或 UNCOMPLETED |
| 重启计数 | 节点上的 `restart` 字段递增，记录重启次数 |
| 不修改 TaskDispatch | 任务整体状态不变，只是节点重新激活 |
| 推送通知 | 通知相关执行人任务被驳回了 |
| 报告重新绑定 | `materialHandle` 方法中将旧的验收报告绑定到新的启动节点上 |

**调用方**: `TaskDispatchServiceImpl#materialHandle` 在 `materialHandleV2Service.handleNode()` 之后，处理验收报告的重新绑定。

---

## 4. 执行人分配逻辑

### 入口：`InstallerTaskService#assignExecutor`

**文件**: `edar-starlord-service/src/main/java/com/ke/utopia/service/impl/InstallerTaskServiceImpl.java`

### 调用时机
创建任务时在 `buildTaskDispatchNode` 的 `buildWithExecutor()` 中调用。

### 分配逻辑

```java
public void assignExecutor(TaskDispatchNode taskDispatchNode, ProjectOrderDetailBO projectOrderDetailBO,
                           MaterialTask materialTask, TaskDispatch taskDispatch) {
    // ① 获取角色列表
    List<Integer> roleTypeList = RoleTypeUtil.convertRoleTypes(taskDispatchNode.getExecutorTypes());

    if (CollectionUtils.isNotEmpty(roleTypeList)) {
        if (roleTypeList.size() > 1) {
            // 多个角色：按优先级排序，逐个尝试，第一个分配到具体执行人的就选
            roleTypeList = RoleTypeUtil.orderRoleTypeList(roleTypeList);
            for (Integer roleType : roleTypeList) {
                assignByRoleType(taskDispatchNode, projectOrderDetailBO,
                        RoleTypeEnum.getByValue(roleType), materialTask, taskDispatch);
                if (StringUtils.isNotBlank(taskDispatchNode.getExecutorId())) {
                    roleTypeEnum = RoleTypeEnum.getByValue(roleType);
                    break; // 高优先级分配到人则停止
                }
            }
        } else {
            // 单个角色：直接分配
            roleTypeEnum = RoleTypeEnum.getByValue(roleTypeList.get(0));
            assignByRoleType(taskDispatchNode, projectOrderDetailBO, roleTypeEnum, materialTask, taskDispatch);
        }
    } else {
        // 兼容旧逻辑：取 executorType
        roleTypeEnum = RoleTypeEnum.getByValue(taskDispatchNode.getExecutorType().intValue());
        assignByRoleType(taskDispatchNode, projectOrderDetailBO, roleTypeEnum, materialTask, taskDispatch);
    }

    // ② 设置最终的执行角色
    taskDispatchNode.setExecutorType(roleTypeEnum.getValue().byteValue());
}
```

### 各角色分配方式（`assignByRoleType`）

角色从项目订单 `ProjectOrderDetailBO` 中取对应人员信息：

| 角色 | 取值来源 | 说明 |
|------|---------|------|
| `HOMEOWNER`（业主） | `projectOrder.getHomeownerId/Name()` | 业主自己处理 |
| `FOREMAN`（工长） | `projectOrder.getForemanId/Name()` | 装修工长 |
| `BUTLER`（管家） | `projectOrder.getAssistantId/Name()` | 项目管家/协调员 |
| `DESIGNER`（设计师） | `projectOrder.getDesignerId/Name()` | 设计师 |
| `ORDER_CLERK`（下单员） | 从 `supplierManager` 查询供应商对应的下单员 | 供应商内部角色 |
| `INSTALL_LEADER`（安装组长） | 从 `packageConstructionManager` 查询施工包对应的组长 | 安装团队组长 |
| `INSTALL_WORKER`（安装工） | 从 `packageConstructionManager` 查询施工包对应的安装工 | 安装师傅 |
| `SUPPLIER`（供应商） | 从 `supplierManager` 查询供应商信息 | 供应商侧 |
| `WOOD_DESIGNER`（木作设计师） | 从订单信息或角色映射查询 | 定制木作设计师 |
| `CABINET_DESIGNER`（橱柜设计师） | 从订单信息或角色映射查询 | 橱柜设计师 |

### 角色优先级（多角色时）

如果节点配置了多个角色（如 `["FOREMAN", "BUTLER"]`），会按 `RoleTypeUtil.orderRoleTypeList()` 排序后依次尝试，第一个能分配到的就用它。

---

## 5. MQ 消息发送机制

### 生产者：`MaterialTaskProducer`

**文件**: `edar-starlord-service/src/main/java/com/ke/utopia/service/producer/MaterialTaskProducer.java`

### 消息类型一览

| 常量 | Key | 触发场景 | 消息体 |
|------|-----|---------|-------|
| `MATERIAL_TASK_NODE_COMPLETE` | `material-task-node-complete` | 节点完成时 | `MaterialTaskNodeChangeMsgBO` |
| `SUPPLIER_MATERIAL_SYNC` | `supplier-material-sync` | 供应商物料同步 | `SyncSupplierMaterialDTO` |
| `MATERIAL_TASK_CHANGE` | `material-task-change` | 主材变更（取消等） | `MaterialTaskChangeMsgBO` |
| `MATERIAL_TASK_EXECUTOR_TYPE_CHANGE` | `material-task-executor-type-change` | 执行人类型变更 | `ExecutorTypeChangeBO` |
| `MATERIAL_TASK_DISPATCH_CHANGE` | `material-task-dispatch-change` | TaskDispatch 信息变更 | `MaterialTaskDispatchChangeMsgBO` |
| `MATERIAL_TASK_DISPATCH_TIME_CHANGE` | `material-task-dispatch-time-change` | 节点时间变更 | `MaterialTaskDispatchTimeChangeMsgBO` |
| `MATERIAL_TASK_DISPATCH_STATE_CHANGE` | `material-task-dispatch-state-change` | 节点创建/取消（v1） | `MaterialTaskDispatchStateChangeMsgBO` |
| `MATERIAL_TASK_DISPATCH_STATE_CHANGE_V2` | `material-task-dispatch-state-change-v2` | 节点创建/取消（v2） | `MaterialTaskDispatchStateChangeMsgBOV2` |
| `MATERIAL_TASK_DISPATCH_RESTART_STATE_CHANGE` | `material-task-dispatch-restart-state-change` | 节点重启 | `MaterialTaskDispatchStateChangeMsgBOV2` |
| `TASK_DELIVER_BATCH_CHANGE` | `task-deliver-batch-change` | 批次变更（供应链） | `TaskDeliverBatchChanged` |
| `MATERIAL_TASK_ORDER_CHANGE` | `material-task-order-change` | 下单/安装订单变更 | `MaterialTaskOrderMsgBO` |

### 发布方式

所有消息通过统一的 `EventDrivenPublisher` 发布，底层使用 Kafka 发送到 `kafka.material.task.change.topic` 主题。

```java
@PostConstruct
private void registerPublisher() {
    EventDrivenPublisher.registerType(MATERIAL_TASK_NODE_COMPLETE,
        new KafkaMessageRoute(materialTaskNodeChangeTopic));
    // ... 其他类型类似
}
```

### 在创建流程中的调用

在 `TaskDispatchBatchCreateServiceImpl#afterTransactional()` 中：

```java
context.getInserts().forEach(taskDispatch ->
    materialTaskProducer.publishTaskStateChange(
        taskDispatch, taskDispatch.getList(), StateChangeType.CREATE)
);
```

这发出的是 `MATERIAL_TASK_DISPATCH_STATE_CHANGE` 和 `MATERIAL_TASK_DISPATCH_STATE_CHANGE_V2` 两类消息。

### 消息消费者

消费者在其他外部服务中（不在本项目源码内）：
- **工作中心（work-center）**：消费节点完成消息，驱动业务流程状态机
- **OMS 系统**：消费时间变更、任务状态变更，同步订单状态
- **供应链系统**：消费批次变更、供应商同步消息
- **其他内部服务**：如 Zeus、Ruban 等

---

## 整体业务流转关系图

```
                ┌─────────────────────────────────────┐
                │      TaskDispatch 生命周期           │
                │                                     │
    创建 ──────→ UN_ACTIVE ───激活──→ UNCOMPLETED ──→ COMPLETED
                │                    │                   │
                │                 节点流转               │
                │               ┌────┴────┐             │
                │           完成节点    驳回节点          │
                │               │         │             │
                │         激活下一节点   重启当前节点     │
                │               │         │             │
                │        全部完成?      重新激活          │
                │               │                       │
                │            COMPLETED                   │
                │               │                       │
                │      激活下一个 TaskDispatch            │
                └─────────────────────────────────────┘
```

### 与创建入口文档的关系

建议与 [主材任务（TaskDispatch）的批量创建入口.md](./主材任务（TaskDispatch）的批量创建入口.md) 配合阅读：

| 顺序 | 文档 | 覆盖的内容 |
|------|------|-----------|
| 1 | 创建入口 | 任务的创建、模板转实例、计划激活时间计算 |
| 2 | 核心业务逻辑（本文档） | 任务激活、节点完成流转、驳回重启、执行人分配、MQ 消息 |

```
时间线:
创建 → prepareData → buildWithConfig → 保存DB → pushMsg → 事务提交 → activateTaskDispatchAsync
                                                                          ↓
                                                                    正式激活
                                                                          ↓
                                                             执行人处理节点 → completeNode
                                                                          ↓
                                                                   激活下一节点
                                                                          ↓
                                                                     全部完成
```
