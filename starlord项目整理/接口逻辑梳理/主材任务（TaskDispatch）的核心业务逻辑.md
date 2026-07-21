# 主材任务（TaskDispatch）批量创建与核心业务逻辑

> 本文档全面梳理 **主材任务批量创建** 的核心业务逻辑，重点回答：
> 1. **每个判断是根据什么来判断的？**（判断条件/字段来源）
> 2. **查配置是查什么配置？**（查了哪些配置表、哪些字段、条件是什么）
> 3. **数据如何映射到实例表？**（配置字段 → 实例字段的映射关系）

---
主材任务激活处理逻辑：已激活或已完成以及不在今天内激活的均跳过，根据任务判断有processCode，走新版激活流程。没有的走旧版激活流程
新版激活：先查询流程定义，然后按流程定义中的节点顺序激活，其中又「处理多分支、条件路由等复杂场景」这部分未清楚。
旧版激活：进行processCode二次检查，和should Activate检查。并进行尾款拦截，同意之后正式激活主材任务，根据拿到的taskdispatch查询节点列表，通过get(0)的方式查询并激活第一个节点，然后进行激活后处理
应该激活逻辑：根据配置的激活依赖分为节点验收和主材任务节点依赖。节点验收依赖需要验证订单不是取消状态以及此时订单位于的流程要先于当前配置的节点依赖。
主材任务节点依赖：查询全部依赖的节点，如果依赖节点有一个任务未完成，则不激活。全部完成则返回对应任务
## 目录

1. [整体架构与数据流](#1-整体架构与数据流)
2. [输入参数详解](#2-输入参数详解)
3. [配置表结构与字段含义](#3-配置表结构与字段含义)
4. [Step 1 — prepareData 准备数据](#4-step-1--preparedata-准备数据)
5. [Step 2 — 过滤已生成的实例](#5-step-2--过滤已生成的实例)
6. [Step 3 — buildWithConfig 构建任务框架](#6-step-3--buildwithconfig-构建任务框架)
7. [Step 4 — buildTaskDispatch 填充主表](#7-step-4--buildtaskdispatch-填充主表)
8. [Step 5 — 保存 task_dispatch](#8-step-5--保存-task_dispatch)
9. [Step 6 — buildTaskDispatchNode 填充子表](#9-step-6--buildtaskdispatchnode-填充子表)
10. [Step 7 — 保存 task_dispatch_node](#10-step-7--保存-task_dispatch_node)
11. [Step 8 — pushMsg 推送消息](#11-step-8--pushmsg-推送消息)
12. [Step 9 — activateTaskDispatchAsync 异步激活](#12-step-9--activatetaskdispatchasync-异步激活)
13. [Step 10 — afterTransactional 发送 MQ 消息](#13-step-10--aftertransactional-发送-mq-消息)
14. [激活时间计算的完整决策树](#14-激活时间计算的完整决策树)
15. [执行人分配逻辑与配置映射](#15-执行人分配逻辑与配置映射)
16. [实例表与配置表字段映射表](#16-实例表与配置表字段映射表)
17. [完整泳道图](#17-完整泳道图)

---

## 1. 整体架构与数据流

### 核心类

| 层级 | 类 | 职责 |
|------|-----|------|
| Controller（v2） | `MaterialBatchV2Service` | 批量送货批次管理（送货批次的聚合） |
| Service（v2 入口） | `TaskDispatchBatchCreateServiceImpl.invoke()` | **批量创建主材任务**的主入口 |
| Service（v1 激活） | `DispatchCreateServiceImpl` | 任务激活、排期查询、节点依赖查询 |
| Service（v2 激活） | `MaterialActivateV2ServiceImpl` | v2 新版本激活逻辑 |
| DAO 查询 | `MaterialTaskDaoImpl.listWithMultiFieldIn()` | 根据**组合字段**查询配置 |
| DAO 查询 | `TaskDispatchDaoImpl.listWithMultiFieldIn()` | 根据**组合字段**查询已存在的实例 |

### 数据流总览

```
输入: MaterialBatchCreateBO
  ├─ projectOrderId       → 项目订单 ID
  ├─ productComboId       → 产品套餐 ID
  ├─ sourceType           → 来源 (0=供应链, 1=测量申请单)
  └─ createList           → [{supplierCode, materialCode, orderNo}, ...]

                        ↓

Step 1  prepareData()
  ├─ 查配置表 material_task (条件: productComboId + sourceType + (supplierCode,materialCode)组合)
  ├─ 查实例表 task_dispatch (条件: 同项目下已有 templateId)
  └─ 查订单信息 ProjectOrderDetailBO

                        ↓

Step 2  buildWithConfig()
  └─ MaterialTask → TaskDispatch 映射
  └─ MaterialTaskNode → TaskDispatchNode 映射

                        ↓

Step 3  buildTaskDispatch()
  ├─ buildWithCreator()           设置创建人
  └─ buildTaskDispatchWithTime()  计算计划激活时间

                        ↓

Step 4  批量 INSERT task_dispatch

                        ↓

Step 5  buildTaskDispatchNode()
  ├─ fillWithTaskDispatchId()     关联外键
  ├─ buildWithExecutor()          分配执行人
  └─ buildTaskDispatchNodeWithTime() 计算预计时间

                        ↓

Step 6  批量 INSERT task_dispatch_node

                        ↓

Step 7  pushMsg()  对已激活节点推送通知

                        ↓  事务提交后

Step 8  activateTaskDispatchAsync()  异步激活

                        ↓

Step 9  afterTransactional()  MQ消息: 任务创建通知

输出: task_dispatch N条 + task_dispatch_node N×M条 + MQ消息 + 推送通知
```

---

## 2. 输入参数详解

### `MaterialBatchCreateBO`

```java
class MaterialBatchCreateBO {
    Long projectOrderId;          // 项目订单ID — 标识属于哪个装修项目
    Long productComboId;          // 产品套餐ID — 用于筛选配置模板
    Integer sourceType;           // 来源类型
                                  //   0=供应链订单生成
                                  //   1=测量申请单生成
    List<SupplierMaterialPair> createList;  // 需生成的供应商-品类列表
    Long operatorId;              // 操作人ID
    String operatorName;          // 操作人姓名
}

class SupplierMaterialPair {
    String supplierCode;     // 供应商编码 — 与 materialCode 共同作为组合键
    String supplierName;     // 供应商名称
    String materialCode;     // 主材编码（品类编码）
    String materialName;     // 品类名称
    String orderNo;          // 供应链订单号（用于区分同一材料的不同订单）
    String businessId;       // 零售子单ID
    String configurationId;  // 配置ID
}
```

### 输入来源

批量创建被以下场景触发：
1. **供应链拆单后** → `TaskDispatchCreateServiceImpl.createTaskDispatch(projectOrderId)`
2. **测量申请单完成后** → `TaskDispatchServiceImpl.createSingleTask(CreateSingleTaskParam)`
3. **需求变更** → `TaskDispatchCreateServiceImpl.needChange()`

---

## 3. 配置表结构与字段含义

### 3.1 `material_task` — 主材任务模板配置表

这是 **核心配置表**，定义了"某个套餐下某个供应商+品类应该生成什么类型的任务"。

**查询条件**（由 `QueryMaterialTaskCondition` 承载）：

| SQL 条件 | 字段来源 | 说明 |
|----------|---------|------|
| `product_combo_id = ?` | `MaterialBatchCreateBO.productComboId` | 产品套餐 |
| `source_type = ?` | `MaterialBatchCreateBO.sourceType` | 来源类型 |
| `state = 1` | 硬编码 `StateEnum.VALID` | 只查有效配置 |
| `(supplier_code, material_code) IN ((?,?), ...)` | `createList` 中的组合 | 多组合 IN 查询 |

**SQL 本质**（`MaterialTaskExtMapper.xml`）：
```sql
SELECT * FROM material_task
WHERE product_combo_id = #{productComboId}
  AND source_type = #{sourceType}
  AND state = #{state}
  AND (supplier_code, material_code) IN (
    (?, ?), (?, ?), ...  -- 每个 createList 项一个组合
  )
```

**核心字段 — 每个字段在后续逻辑中的用途**：

| 字段 | 类型 | 释义 | 在创建逻辑中的使用 |
|------|------|------|-------------------|
| `id` | BIGINT PK | 模板ID | → `task_dispatch.templateId` |
| `product_combo_id` | BIGINT | 产品套餐ID | 查询条件，不入实例 |
| `material_code` | VARCHAR | 主材编码（品类code） | → `task_dispatch.materialCode` |
| `material_name` | VARCHAR | 主材名称（品类名称） | → `task_dispatch.materialName` |
| `supplier_code` | VARCHAR | 供应商编码 | → `task_dispatch.supplierCode` |
| `supplier_name` | VARCHAR | 供应商名称 | → `task_dispatch.supplierName` |
| `task_type` | INTEGER | **任务类型** | → `task_dispatch.taskType`；**用于过滤已存在的实例**（`queryExistCfgTaskId` 中的 `taskTypeIn`） |
| `process_batch` | INTEGER | 批量批次(0=单独,1=第一批,2=第二批) | → `task_dispatch.processBatch` |
| `activate_mode` | TINYINT | **激活方式—核心判断字段** | 决定激活时间的计算逻辑（见第14节） |
| `activate_relation_type` | TINYINT | 激活关联类型 | 配合 activate_mode 使用 |
| `activate_relation_code` | VARCHAR | **激活关联编号** | `PLAN_TIME` 模式：对应项目排期的工序 code；`DEPENDENT_NODE` 模式：路由配置中的 sourceId |
| `activate_schedule_relation` | TINYINT | 激活时间关系(1=之前,2=当天,3=之后) | 配合 `activateDuration` 计算偏移 |
| `activate_duration` | INTEGER | 激活间隔天数 | 排期时间偏移量 |
| `source_type` | TINYINT | 来源类型(0=供应链,1=测量) | → `task_dispatch.sourceType` |
| `state` | TINYINT | 状态(0=无效,1=有效) | 查询条件，不入实例 |
| `version` | INTEGER | 版本号 | 保留，一般不使用 |

### 3.2 `material_task_node` — 主材任务节点配置表

每个 `material_task` 对应多条节点配置。查询时按 `material_task_id` 批量查询。

**查询条件**（`MaterialTaskNodeDao.listTaskNodeMap(materialTaskIds)`）：

```
material_task_id IN (?, ?, ...)  AND state = 1
```

**核心字段 — 每个字段在后续逻辑中的用途**：

| 字段 | 类型 | 释义 | 在创建逻辑中的使用 |
|------|------|------|-------------------|
| `id` | BIGINT PK | 节点配置ID | → `task_dispatch_node.templateNodeId` |
| `material_task_id` | BIGINT | 所属模板ID | 关联查询条件 |
| `node_type` | TINYINT | **节点类型—核心字段** | → `task_dispatch_node.nodeType`；决定节点顺序（排序后存入 `nodeTask`） |
| `role_type` | TINYINT | 执行角色（单角色时使用） | → `task_dispatch_node.executorType`（后备方案） |
| `role_types` | VARCHAR | **多角色，逗号分隔**（优先使用） | → `task_dispatch_node.executorTypes` |
| `relation_type` | TINYINT | 关联类型 | 保留字段 |
| `relation_code` | VARCHAR | 关联编号 | 节点级别的依赖关系 |
| `schedule_relation` | TINYINT | 时间关系(之前/当天/之后) | 节点级时间计算 |
| `duration` | INTEGER | 间隔时间 | 节点级时间计算 |
| `detail_item` | VARCHAR | 具体操作描述 | 保留字段 |
| `restart_role_type` | INTEGER | 二次启动角色（单角色） | 驳回重启时使用 |
| `restart_role_types` | VARCHAR | 多重启角色，逗号分隔 | 驳回重启时使用 |
| `restart_node_type` | INTEGER | 失败开启节点 | 驳回后回到哪个节点 |
| `state` | TINYINT | 状态(0=无效,1=有效) | 查询条件 |
| `dispatch_types` | VARCHAR | 派出角色类型 | 扩展字段 |
| `mode_type` | TINYINT | 模式类型 | 扩展字段 |

### 3.3 `material_task_route_instance` — 任务路由表（节点依赖）

**用于 `DEPENDENT_NODE` 激活模式**：查询当前任务依赖哪些前置任务。

| 字段 | 用途 |
|------|------|
| `target_id` | 当前模板ID（`taskDispatch.getTemplateId()`） |
| `source_id` | 前置依赖的模板ID |
| `state` | 状态 |

查询逻辑（`DispatchCreateServiceImpl.queryDependentNodeTask`）：
```sql
-- 1. 查路由：谁依赖谁
SELECT source_id FROM material_task_route_instance
WHERE target_id = #{taskDispatch.templateId} AND state = 1

-- 2. 查前置任务实例
SELECT * FROM task_dispatch
WHERE template_id IN (source_ids)
  AND project_order_id = #{taskDispatch.projectOrderId}
  AND material_code = #{taskDispatch.materialCode}
  AND supplier_code = #{taskDispatch.supplierCode}
  AND order_no = #{taskDispatch.orderNo}
```

### 3.4 `construction_schedule` — 项目施工排期

**用于 `PLAN_TIME` 激活模式**（通过 `constructionManager.queryConstructionSchedulesV2` 调用）。

返回的 `ConstructionScheduleDTO` 字段：

| 字段 | 对应 | 用途 |
|------|------|------|
| `sequenceCode` | `activateRelationCode` | 匹配模板配置的关联工序 |
| `sequenceName` | — | 工序名称 |
| `startTime` | 基准时间 | 用于计算激活时间 |
| `endTime` | — | 结束时间（备用） |

### 3.5 `project_order` — 项目订单信息

通过 `ProjectOrderManager.getProjectOrder()` 查询，获取 `ProjectOrderDetailBO`：

| 字段 | 在创建逻辑中的使用 |
|------|-------------------|
| `projectOrderId` | → `task_dispatch.projectOrderId` |
| `gbCode` | → `task_dispatch.gbCode`（城市编码） |
| `homeOrderNo` | → `task_dispatch_node.homeOrderNo` |
| `packageId` | 用于 `productComboId`（套餐ID） |
| `foremanId/Name` | 工长→执行人分配 |
| `assistantId/Name` | 管家→执行人分配 |
| `designerId/Name` | 设计师→执行人分配 |
| `homeownerId/Name` | 业主→执行人分配 |

---

## 4. Step 1 — prepareData 准备数据

### 4.1 prepareCfg — 查询配置

**代码路径**：`TaskDispatchBatchCreateServiceImpl.prepareCfg()`

**判断条件详解**：

```java
// ===== 构建组合查询条件 =====
QueryMaterialTaskCondition comboCondition = QueryMaterialTaskCondition.builder()
    .productComboId(createBO.getProductComboId())   // 条件1：产品套餐
    .sourceType(createBO.getSourceType())            // 条件2：来源类型
    .state(StateEnum.VALID.getValue())               // 条件3：有效状态（固定值1）
    .useMultiFields(Boolean.TRUE)                    // 启用组合字段查询
    .fields(TASK_CFG_QUERY_COLUMN)                   // = ["supplier_code", "material_code"]
    .fieldVals(buildComboCondition(...))              // = 由 createList 生成的 [(supplier1, material1), ...]
    .build();
```

**组合条件生成的逻辑**（`buildComboCondition`）：

```java
// TASK_CFG_QUERY_COLUMN = ["supplier_code", "material_code"]
// 以 createList 中的每个 SupplierMaterialPair 为一行，提取 supplierCode 和 materialCode

// 例如 createList = [
//   {supplierCode:"S001", materialCode:"M001"},
//   {supplierCode:"S002", materialCode:"M002"}
// ]
// 生成 fieldVals = [["S001","M001"], ["S002","M002"]]
// SQL 效果: WHERE (supplier_code, material_code) IN (('S001','M001'), ('S002','M002'))
```

**查询结果为空时的处理**：`context.setEnable(Boolean.FALSE)`，方法直接 return 0。

### 4.2 获取订单信息

```java
ProjectOrderDetailBO projectOrderDetailBO = projectOrderManager.getProjectOrder(
    context.getParam().getProjectOrderId());
context.setProjectOrder(projectOrderDetailBO);
```

这一步骤**不查配置**，直接从订单服务（Feign）获取订单详情，用于：
1. 填充 `task_dispatch` 的项目维度字段（`projectOrderId`, `gbCode` 等）
2. 执行人分配时获取工长/管家/设计师/业主的 ID

---

## 5. Step 2 — 过滤已生成的实例

### 5.1 过滤逻辑（`queryExistCfgTaskId`）

**核心目的**：避免为同一个项目+供应商+品类+订单号重复生成任务。

```java
QueryTaskDispatchCondition comboCondition = QueryTaskDispatchCondition.builder()
    .state(StateEnum.VALID.getValue())                  // 有效状态
    .sourceType(createBO.getSourceType())                // 来源类型
    .projectOrderId(createBO.getProjectOrderId())        // 同一项目
    .taskTypeIn(Lists.newArrayList(taskTypeSet))         // 匹配的 taskType
    .useMultiFields(Boolean.TRUE)
    .fields(TASK_INSTANCE_QUERY_COLUMN)                  // = ["supplier_code", "material_code", "order_no"]
    .fieldVals(buildComboCondition(...))                 // 同样的组合条件（多了一个 order_no）
    .build();
```

**SQL 本质**（`TaskDispatchExtMapper.xml`）：
```sql
SELECT * FROM task_dispatch
WHERE project_order_id = #{projectOrderId}
  AND source_type = #{sourceType}
  AND state = #{state}
  AND task_type IN (?, ?, ...)
  AND (supplier_code, material_code, order_no) IN (
    (?, ?, ?), (?, ?, ?), ...
  )
```

**返回**：已存在的 `templateId` 列表。

### 5.2 过滤后的处理

```java
// 从 materialTasks 中剔除已存在实例的模板
List<Long> materialTaskIds = materialTasks.stream()
    .filter(x -> !existTemplateIds.contains(x.getId()))
    .map(x -> x.getId())
    .collect(Collectors.toList());

if (CollectionUtils.isEmpty(materialTaskIds)) {
    context.setEnable(Boolean.FALSE);  // 全部已存在，跳过
    return;
}
```

### 5.3 查询节点配置并组合

```java
// 批量查询有效节点配置
Map<Long, List<MaterialTaskNode>> taskNodeMap = materialTaskNodeDao.listTaskNodeMap(materialTaskIds);

// SQL: SELECT * FROM material_task_node
//      WHERE material_task_id IN (?,?,...) AND state = 1

// 组合到 MaterialTask 对象
for (MaterialTask materialTask : materialTasks) {
    List<MaterialTaskNode> taskNodes = taskNodeMap.get(materialTask.getId());
    if (CollectionUtils.isEmpty(taskNodes)) continue;  // 无节点配置的模板跳过
    materialTask.setMaterialTaskNodeList(taskNodes);
}
```

---

## 6. Step 3 — buildWithConfig 构建任务框架

### 6.1 映射关系

#### 模板 → TaskDispatch 映射（`TaskDispatchBatchCreateServiceImpl.buildTaskDispatch()`）

```java
TaskDispatch taskDispatch = new TaskDispatch();
taskDispatch.setTemplateId(materialTask.getId());           // ← material_task.id
taskDispatch.setProjectOrderId(projectOrderDetailBO.getProjectOrderId()); // ← 订单信息
taskDispatch.setGbCode(projectOrderDetailBO.getGbCode());   // ← 订单信息
taskDispatch.setMaterialCode(materialTask.getMaterialCode()); // ← material_task.material_code
taskDispatch.setMaterialName(materialTask.getMaterialName()); // ← material_task.material_name
taskDispatch.setSupplierCode(materialTask.getSupplierCode()); // ← material_task.supplier_code
taskDispatch.setSupplierName(materialTask.getSupplierName()); // ← material_task.supplier_name
taskDispatch.setTaskType(materialTask.getTaskType());         // ← material_task.task_type
taskDispatch.setProcessBatch(materialTask.getProcessBatch()); // ← material_task.process_batch
taskDispatch.setCurrentNodeTime(DateUtil.getTimeEpoch());     // ← 固定初始值(纪元时间)
taskDispatch.setProcessStatus(TaskDispatchStatusEnum.UN_ACTIVE.getValue().byteValue()); // ← 固定=1(未激活)
```

**关键判断**：`processStatus` 固定为 `UN_ACTIVE(1)`，所有新创建的任务都是"未激活"状态。之前版本曾有过逻辑根据 `activateMode` 即时设置为 `UNCOMPLETED` 的旧代码（已注释掉）。

#### 节点顺序的构建

```java
// 从节点配置中提取 node_type，排序后组合成逗号分隔的字符串
// 例如：[20, 40, 60, 80] → nodeTask = "20,40,60,80"
List<Byte> nodeTypeList = materialTaskNodeList.stream()
    .map(MaterialTaskNode::getNodeType)
    .sorted()
    .collect(Collectors.toList());
taskDispatch.setNodeTask(StringUtils.join(nodeTypeList, ","));

// 第一个节点作为当前节点
taskDispatch.setCurrentNodeType(Integer.valueOf(nodeTypeList.get(0)));
```

#### 节点配置 → TaskDispatchNode 映射（`buildTaskDispatchNode()`）

```java
TaskDispatchNode taskDispatchNode = new TaskDispatchNode();
taskDispatchNode.setProjectOrderId(projectOrderDetailBO.getProjectOrderId()); // ← 订单信息
taskDispatchNode.setHomeOrderNo(projectOrderDetailBO.getHomeOrderNo());        // ← 订单信息
taskDispatchNode.setTemplateNodeId(materialTaskNode.getId());                  // ← material_task_node.id
taskDispatchNode.setNodeType(Integer.valueOf(materialTaskNode.getNodeType())); // ← material_task_node.node_type
taskDispatchNode.setProcessStatus(TaskDispatchNodeStatusEnum.UN_ACTIVE.getValue().byteValue());  // 默认=1
```

**第一个节点的特殊处理**：

```java
// 如果是第一个节点，且任务状态是 UNCOMPLETED，则激活该节点
if (Objects.equals(taskDispatch.getCurrentNodeType(),
        Integer.valueOf(materialTaskNode.getNodeType()))) {
    if (LangUtils.isUncompleted(taskDispatch.getProcessStatus())) {
        // 这里 taskDispatch.getProcessStatus() 始终是 UN_ACTIVE(1)
        // 而 isUncompleted 判断的是 == TaskDispatchStatusEnum.UNCOMPLETED(2)
        // 所以这段代码实际上永远不会执行（因为 create 时所有都是 UN_ACTIVE）
        taskDispatchNode.setProcessStatus(TaskDispatchNodeStatusEnum.UNCOMPLETED.getValue().byteValue());
        taskDispatchNode.setStartTime(new Date());
    }
}
```

> **注意**：由于第 6.1 节中 `taskDispatch.setProcessStatus` 固定为 `UN_ACTIVE(1)`，因此这里的第一个节点默认也是 `UN_ACTIVE(1)`，不会激活。后续在 Step 8 的激活逻辑中才会被激活。

#### 执行角色的设置

```java
// 优先使用 roleTypes（多角色），其次使用 roleType（单角色）
if (StringUtils.isNotBlank(materialTaskNode.getRoleTypes())) {
    taskDispatchNode.setExecutorTypes(materialTaskNode.getRoleTypes());  // 逗号分隔字符串
} else {
    RoleTypeEnum roleTypeEnum = RoleTypeEnum.getByValue(Integer.valueOf(materialTaskNode.getRoleType()));
    taskDispatchNode.setExecutorType(roleTypeEnum.getValue().byteValue());  // 单角色 byte
}
```

---

## 7. Step 4 — buildTaskDispatch 填充主表

### 7.1 buildWithCreator — 设置创建人

```java
taskDispatch.setCreateBy(param.getOperatorId() == null ? "" : param.getOperatorId().toString());
taskDispatchNode.setCreateBy(...);  // 同上
```

### 7.2 buildTaskDispatchWithTime — 计算计划激活时间（核心）

这是**最核心的业务逻辑**。详细决策树见[第 14 节](#14-激活时间计算的完整决策树)。

```java
for (TaskDispatch taskDispatch : context.getInserts()) {
    MaterialTask materialTask = materialTasksMap.get(taskDispatch.getTemplateId());

    if (ActivateModeEnum.isPlanTime(materialTask.getActivateMode().intValue())) {
        // 情况一：PLAN_TIME(0) — 按排期工序时间激活
    } else if (LangUtils.isImmediatelyActivate(materialTask.getActivateMode())) {
        // 情况二：IMMEDIATELY(1) — 立即激活
    } else if (ActivateModeEnum.isDependentNode(materialTask.getActivateMode().intValue())) {
        // 情况三：DEPENDENT_NODE(2) — 节点依赖
    } else {
        // 情况四：其他（兜底）
    }
}
```

**判断依据详解**：

| 判断 | 判断依据 | 取值来源 |
|------|---------|---------|
| 是否为 `PLAN_TIME` | `materialTask.activateMode == 0` | `material_task.activate_mode` |
| 是否为 `IMMEDIATELY` | `materialTask.activateMode == 1` | `material_task.activate_mode` |
| 是否为 `DEPENDENT_NODE` | `materialTask.activateMode == 2` | `material_task.activate_mode` |
| 默认为其他 | 以上都不匹配 | 兜底 |

各情况的详细计算逻辑见[第 14 节](#14-激活时间计算的完整决策树)。

---

## 8. Step 5 — 保存 task_dispatch

```java
taskDispatchDao.batchInsertSelectiveWithPrimaryKey(context.getInserts());
```

**关键行为**：
- MyBatis 的 `batchInsertSelectiveWithPrimaryKey` 会**自动设置主键 ID**
- 保存后，每个 `TaskDispatch` 对象被注入了数据库生成的 `id`
- 这个 `id` 将在 Step 6 中被用作 `task_dispatch_node` 的外键

---

## 9. Step 6 — buildTaskDispatchNode 填充子表

### 9.1 fillWithTaskDispatchId — 设置外键关联

```java
x.getList().forEach(y -> y.setTaskDispatchId(x.getId()));
```

### 9.2 buildWithExecutor — 分配执行人

**调用链**：`buildWithExecutor()` → `installerTaskService.assignExecutor()`

详见[第 15 节](#15-执行人分配逻辑与配置映射)。

### 9.3 buildTaskDispatchNodeWithTime — 计算预计时间

```java
// 计算首次考核时间（estimatedTime）
Date estimatedTime = estimatedTimeService.calculateEstimatedTime(
    taskDispatchNode, TimeTypeEnum.FIRST_CHECK_TIME);

// 计算平台考核时间（platformCheckTime）
Date platformCheckTime = estimatedTimeService.calculateEstimatedTime(
    taskDispatchNode, TimeTypeEnum.PLATFORM_CHECK_TIME);

taskDispatchNode.setEstimatedTime(estimatedTime == null ? CommonConstant.getInitDate() : estimatedTime);
taskDispatchNode.setPlatformCheckTime(platformCheckTime == null ? CommonConstant.getInitDate() : platformCheckTime);
```

**计算逻辑**（`EstimatedTimeService`）：
- 根据 `task_dispatch_node` 上的 `nodeType`、`executorType`、`estimatedTime` 等字段
- 查询节点对应的 `MaterialTaskNodeTime` 时间配置（`n_material_task_cfg` 或 `material_task_node_time`）
- 按配置规则（工作日/日历日、固定偏移、营业时间等）计算出预计时间

---

## 10. Step 7 — 保存 task_dispatch_node

```java
taskDispatchNodeDao.batchInsertSelective(getTaskDispatchNodeList(context));
```

将 Step 6 中构建好的所有 `TaskDispatchNode` 批量写入 `task_dispatch_node` 表。

---

## 11. Step 8 — pushMsg 推送消息

### 11.1 判断逻辑

```java
private void pushMsg(TaskDispatchCreateContext context) {
    // 收集所有新创建的 TaskDispatch ID
    List<Long> taskDispatchIds = context.getInserts().stream()
        .map(TaskDispatch::getId).collect(Collectors.toList());

    // 查询状态为 UNCOMPLETED(2) 的节点
    ArrayList<Byte> unCompletedList = Lists.newArrayList(
        TaskDispatchNodeStatusEnum.UNCOMPLETED.getValue().byteValue());  // = [2]
    List<TaskDispatchNode> activeDispatchNodeList = taskDispatchNodeDao.listByParam(
        taskDispatchIds, null, unCompletedList);

    // 对每个已激活的节点发推送
    for (TaskDispatchNode node : activeDispatchNodeList) {
        messagePushClient.pushMessageWhenTaskDispatchNodeProcessChange(
            node.getId(), TaskDispatchNodeStatusEnum.UNCOMPLETED);
    }
}
```

**判断条件**：`task_dispatch_node.process_status = 2（UNCOMPLETED）`

> **实际含义**：由于创建时所有节点都是 `UN_ACTIVE(1)`，所以这里的 `listByParam` 通常查不到数据，不会推送。推送实际上是由激活逻辑在后续异步触发（Step 9 中 `activateTaskDispatchAsync`）。

---

## 12. Step 9 — activateTaskDispatchAsync 异步激活

### 12.1 事务后异步激活

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
    @Override
    public void afterCommit() {
        CompletableFuture.runAsync(() -> {
            for (TaskDispatch taskDispatch : context.getInserts()) {
                dispatchCreateService.activateTaskDispatch(taskDispatch);  // v1激活
                // 或 materialActivateV2Service.activateTaskDispatch()  // v2激活（有processCode时）
            }
        });
    }
});
```

**设计意图**：
- **事务提交后才触发**：避免长事务，也确保数据已入库
- **异步执行**：不阻塞创建流程的返回
- **逐个激活**：每个 TaskDispatch 独立激活

### 12.2 激活判断条件（v1）

`DispatchCreateServiceImpl.activateTaskDispatch(taskDispatch)` 中的判断：

```java
// 判断①：状态必须是 UN_ACTIVE
if (TaskDispatchStatusEnum.UN_ACTIVE.getValue().byteValue() != taskDispatch.getProcessStatus()) {
    return;  // 已激活或已完成 → 跳过
}

// 判断②：计划激活时间必须在今天范围内
if (taskDispatch.getPlanActivateTime().before(PLAN_ACTIVATE_START_DATE)  // 早于2000-01-01
        || taskDispatch.getPlanActivateTime().after(DateUtil.getDayEnd())) {  // 晚于今天结束
    return;  // 不在今天内 → 跳过
}

// 判断③：有 processCode 走 v2 激活
if (!StringUtils.isEmpty(taskDispatch.getProcessCode())) {
    materialActivateV2Service.activateTaskDispatch(taskDispatch);
    return;
}

// 判断④：旧版激活逻辑
// ...
```

**核心判断汇总**：

| 判断 | 判断依据 | 影响 |
|------|---------|------|
| 状态为 UN_ACTIVE？ | `taskDispatch.processStatus == 1` | 否→跳过 |
| 激活时间在今天？ | `planActivateTime` 在 [2000-01-01, 今天结束) 内 | 否→跳过 |
| 有 processCode？ | `taskDispatch.processCode` 非空 | 是→走 v2 激活 |
| 满足激活条件？ | `shouldActivate()` 检查排期依赖 | 否→跳过 |
| 尾款拦截？ | `checkInterceptConfigure()` 查询未付款变更单 | 是→只更新时间，不激活节点 |

### 12.3 `shouldActivate()` 排期依赖判断

```java
private boolean shouldActivate(TaskDispatch taskDispatch, MaterialTask materialTask) {
    ScheduleRelationEnum relationEnum = ScheduleRelationEnum.getByValue(
        Integer.valueOf(materialTask.getActivateScheduleRelation()));

    // 只有排期时间模式(PLAN_TIME)且关系在 afterTodayRelationList 中的需要检查
    if (ActivateModeEnum.isPlanTime(materialTask.getActivateMode().intValue())
            && afterTodayRelationList.contains(relationEnum)) {
        ProjectOrderDetailBO projectOrder = projectOrderManager.getProjectOrder(
            taskDispatch.getProjectOrderId());
        return MajorSequenceEnum.canActivateByProjectOrderStatus(
            projectOrder.getStatus(), materialTask.getActivateRelationCode());
    }
    return true;
}
```

**判断条件**：
1. `activateMode == 0（PLAN_TIME）`
2. `activateScheduleRelation` 在 `afterTodayRelationList`（关系为"之后"）
3. 项目订单的状态 >= 配置工序对应的状态

### 12.4 v2 激活

当 `taskDispatch.processCode` 非空时，走 `MaterialActivateV2ServiceImpl`：

```java
// v2 激活逻辑（简化）
public void activateTaskDispatch(TaskDispatch taskDispatch) {
    // 查询流程定义
    // 按流程定义中的节点顺序激活
    // 处理多分支、条件路由等复杂场景
    doActivateTaskDispatch(taskDispatch);
}
```

---

## 13. Step 10 — afterTransactional 发送 MQ 消息

```java
public Integer afterTransactional(TaskDispatchCreateContext context) {
    context.getInserts().forEach(taskDispatch ->
        materialTaskProducer.publishTaskStateChange(
            taskDispatch,
            taskDispatch.getList(),   // TaskDispatchNode 列表
            StateChangeType.CREATE     // 创建事件
        )
    );
    return 0;
}
```

**发送的消息类型**（`MaterialTaskProducer` 内部的 Kafka 主题配置）：

| 消息类型 | Key | 消息体 |
|----------|-----|--------|
| `MATERIAL_TASK_DISPATCH_STATE_CHANGE` | `material-task-dispatch-state-change` | `MaterialTaskDispatchStateChangeMsgBO`（v1 格式） |
| `MATERIAL_TASK_DISPATCH_STATE_CHANGE_V2` | `material-task-dispatch-state-change-v2` | `MaterialTaskDispatchStateChangeMsgBOV2`（v2 格式） |

两个版本都会发送。下游系统（工作中心、OMS、供应链等）按自己的版本订阅消费。

---

## 14. 激活时间计算的完整决策树

```
buildTaskDispatchWithTime()
  │
  ├─ 遍历每个 taskDispatch
  │    ├─ 获取对应的 materialTask (通过 templateId)
  │    │
  │    ├─ ActivateModeEnum.isPlanTime(activateMode)?
  │    │   (判断条件: material_task.activate_mode == 0)
  │    │   │
  │    │   ├─ 查项目排期: listProjectSchedule(projectOrderId)
  │    │   │    └─ 调用 constructionManager.queryConstructionSchedulesV2()
  │    │   │         → 返回 Map<sequenceCode, ProjectScheduleItemBO>
  │    │   │
  │    │   ├─ 从排期 Map 中取 materialTask.activateRelationCode 对应的工序
  │    │   │
  │    │   ├─ 排期中找不到该工序?
  │    │   │   └─ → planActivateTime = INIT_DATE (2000年前，表示不激活)
  │    │   │
  │    │   └─ 找到该工序?
  │    │       ├─ 取 activateScheduleRelation 偏移方向
  │    │       │   (1=BEFORE之前, 2=TODAY当天, 3=AFTER之后)
  │    │       ├─ 取 activateDuration 偏移天数
  │    │       └─ 计算: 工序startDate + 偏移方向 + 偏移天数
  │    │           → planActivateTime = DateUtil.getEstimatedTime(...)
  │    │
  │    ├─ LangUtils.isImmediatelyActivate(activateMode)?
  │    │   (判断条件: material_task.activate_mode == 1)
  │    │   │
  │    │   └─ → planActivateTime = new Date() (当前时间)
  │    │
  │    ├─ ActivateModeEnum.isDependentNode(activateMode)?
  │    │   (判断条件: material_task.activate_mode == 2)
  │    │   │
  │    │   ├─ queryDependentNodeTask(taskDispatch):
  │    │   │    ├─ 查路由表: material_task_route_instance
  │    │   │    │   WHERE target_id = taskDispatch.templateId
  │    │   │    │   → 获取 source_id 列表(前置模板ID)
  │    │   │    │
  │    │   │    └─ 查实例表: task_dispatch
  │    │   │       WHERE template_id IN (source_ids)
  │    │   │         AND project_order_id = taskDispatch.projectOrderId
  │    │   │         AND material_code = taskDispatch.materialCode
  │    │   │         AND supplier_code = taskDispatch.supplierCode
  │    │   │         AND order_no = taskDispatch.orderNo
  │    │   │
  │    │   ├─ 没有前置任务?
  │    │   │   └─ → planActivateTime = INIT_DATE
  │    │   │
  │    │   ├─ 有前置任务但存在未完成的?
  │    │   │   (判断: processStatus != FINISHED(3))
  │    │   │   └─ → planActivateTime = INIT_DATE
  │    │   │
  │    │   └─ 前置任务全部完成?
  │    │       └─ → planActivateTime = 前置任务的 gmtModified (最后修改时间)
  │    │
  │    └─ 其他(activateMode不在0,1,2中)
  │        └─ → planActivateTime = INIT_DATE (默认不激活)
  │
  └─ 结束
```

### 各种模式的含义与典型场景

| `activateMode` | 枚举值 | 含义 | 典型场景 | planActivateTime 最终值 |
|----------------|--------|------|---------|------------------------|
| `PLAN_TIME` | 0 | 按项目施工排期激活 | 送货任务等排期工序完成后才能启动 | 排期工序开始时间 ± 偏移天数 |
| `IMMEDIATELY` | 1 | 创建后立即激活 | 量尺任务、简单的通知类任务 | `new Date()`（当前时间） |
| `DEPENDENT_NODE` | 2 | 依赖前置任务完成 | 复尺→下单（复尺不完成下单无法开始） | 前置任务完成时间 或 INIT_DATE |
| 其他/未配置 | — | 暂不激活 | 需要手动触发的特殊任务 | `INIT_DATE`（2000-01-01前） |

### 偏移方向（`ScheduleRelationEnum`）

| `activateScheduleRelation` | 枚举 | SQL 计算方式 |
|---------------------------|------|-------------|
| `1`（BEFORE/之前） | 之前 | `DATE_SUB(startDate, INTERVAL duration DAY)` |
| `2`（TODAY/当天） | 当天 | 取 `startDate` 当天 |
| `3`（AFTER/之后） | 之后 | `DATE_ADD(startDate, INTERVAL duration DAY)` |

---

## 15. 执行人分配逻辑与配置映射

### 15.1 调用入口

```java
installerTaskService.assignExecutor(taskDispatchNode, projectOrderDetailBO, materialTask, taskDispatch);
```

### 15.2 分配决策树

```
assignExecutor()
  │
  ├─ 获取角色列表
  │    ├─ 优先: taskDispatchNode.getExecutorTypes()
  │    │     ← material_task_node.role_types (逗号分隔的多角色)
  │    └─ 后备: taskDispatchNode.getExecutorType()
  │          ← material_task_node.role_type (单角色)
  │
  ├─ 多角色(>1个)?
  │    ├─ 按优先级排序 (RoleTypeUtil.orderRoleTypeList)
  │    ├─ 按优先级逐个尝试分配
  │    └─ 第一个成功分配到具体执行人的 → 选用该角色
  │
  ├─ 单角色(=1个)?
  │    └─ 直接按该角色分配
  │
  └─ 空角色?
       └─ 兼容旧逻辑: 取 executorType
```

### 15.3 各角色分配方式

| 角色 | 枚举值 | 取值来源 `ProjectOrderDetailBO` | 说明 |
|------|--------|-------------------------------|------|
| `HOMEOWNER`（业主） | 1 | `getHomeownerId/Name()` | 业主自己操作 |
| `FOREMAN`（工长/项目经理） | 2 | `getForemanId/Name()` | 现场施工负责人 |
| `BUTLER`（管家） | 3 | `getAssistantId/Name()` | 项目管家/协调员 |
| `DESIGNER`（设计师） | 4 | `getDesignerId/Name()` | 方案设计师 |
| `ORDER_CLERK`（下单员） | 5 | 从 `supplierManager` 查供应商对应下单员 | 供应商侧角色 |
| `SUPPLIER`（供应商） | 6 | 从 `supplierManager` 查供应商信息 | 供应商 |
| `INSTALL_WORKER`（安装工） | 7 | 从 `packageConstructionManager` 查施工包对应安装工 | 安装师傅 |
| `INSTALL_LEADER`（安装组长） | 8 | 从 `packageConstructionManager` 查施工包对应组长 | 安装团队 |
| `WOOD_DESIGNER`（木作设计师） | 31 | 从订单或角色映射 | 定制木作 |
| `CABINET_DESIGNER`（橱柜设计师） | 35 | 从订单或角色映射 | 橱柜设计 |

### 15.4 角色优先级

多角色时，按 `RoleTypeUtil.orderRoleTypeList()` 排序后依次尝试分配，一旦某角色分配到了具体的执行人，就停止尝试后面的角色。

优先级顺序（从高到低）：
```
FOREMAN(2) > BUTLER(3) > INSTALL_WORKER(7) > ...（按实际业务需求定义）
```

---

## 16. 实例表与配置表字段映射表

### 16.1 `task_dispatch` ← `material_task` + 订单信息

| task_dispatch 字段 | 来源 | 转换规则 |
|-------------------|------|---------|
| `template_id` | `material_task.id` | 直接赋值 |
| `project_order_id` | `ProjectOrderDetailBO.projectOrderId` | 订单服务返回 |
| `gb_code` | `ProjectOrderDetailBO.gbCode` | 城市编码 |
| `material_code` | `material_task.material_code` | 直接赋值 |
| `material_name` | `material_task.material_name` | 直接赋值 |
| `supplier_code` | `material_task.supplier_code` | 直接赋值 |
| `supplier_name` | `material_task.supplier_name` | 直接赋值 |
| `task_type` | `material_task.task_type` | 直接赋值 |
| `process_batch` | `material_task.process_batch` | 直接赋值 |
| `order_no` | `SupplierMaterialPair.orderNo` | createList 传入 |
| `source_type` | `MaterialBatchCreateBO.sourceType` | 直接赋值 |
| `node_task` | `material_task_node.node_type` 列表 | 排序后逗号拼接 |
| `current_node_type` | `material_task_node.node_type` 列表 | 取第一个（排序后） |
| `plan_activate_time` | （算法计算） | 见第14节决策树 |
| `current_node_time` | 固定值 | `DateUtil.getTimeEpoch()`（纪元时间） |
| `process_status` | 固定值 | `UN_ACTIVE(1)` |
| `state` | 默认 | `VALID(1)` |
| `create_by` | `MaterialBatchCreateBO.operatorId` | 操作人 |

### 16.2 `task_dispatch_node` ← `material_task_node` + 订单信息 + 算法

| task_dispatch_node 字段 | 来源 | 转换规则 |
|------------------------|------|---------|
| `task_dispatch_id` | `TaskDispatch.id` | 保存后回填 |
| `template_node_id` | `material_task_node.id` | 直接赋值 |
| `project_order_id` | `ProjectOrderDetailBO.projectOrderId` | 订单服务 |
| `home_order_no` | `ProjectOrderDetailBO.homeOrderNo` | 订单服务 |
| `order_no` | `SupplierMaterialPair.orderNo` | createList 传入 |
| `node_type` | `material_task_node.node_type` | 直接赋值 |
| `process_status` | 固定值 | `UN_ACTIVE(1)` |
| `executor_type` | `material_task_node.role_type` 或算法分配 | 单角色直接取，多角色算法分配 |
| `executor_types` | `material_task_node.role_types` | 逗号分隔字符串 |
| `executor_id` | 算法分配 | 见第15节 |
| `executor_name` | 算法分配 | 见第15节 |
| `estimated_time` | `EstimatedTimeService` 计算 | 首次考核时间 |
| `platform_check_time` | `EstimatedTimeService` 计算 | 平台考核时间 |
| `start_time` | 固定值 | `INIT_DATE`（未激活时） |
| `state` | 默认 | `VALID(1)` |
| `create_by` | `MaterialBatchCreateBO.operatorId` | 操作人 |

---

## 17. 完整泳道图

```
invoke(MaterialBatchCreateBO)
  │
  ├─ [数据准备阶段] ──────────────────────────────────────────
  │  │
  │  ├─ prepareCfg()
  │  │    ├─ 查 material_task
  │  │    │  条件: product_combo_id + source_type + state=1
  │  │    │        + (supplier_code,material_code) 组合IN查询
  │  │    │
  │  │    ├─ 过滤已存在的实例
  │  │    │  查 task_dispatch
  │  │    │  条件: project_order_id + source_type + state=1
  │  │    │        + task_type IN ∪ (supplier_code,material_code,order_no)组合IN
  │  │    │
  │  │    └─ 查 material_task_node
  │  │       条件: material_task_id IN (...) + state=1
  │  │       结果: 组合到 MaterialTask.materialTaskNodeList
  │  │
  │  └─ 查项目订单
  │      调用: projectOrderManager.getProjectOrder()
  │      结果: ProjectOrderDetailBO
  │
  ├─ [构建阶段] ──────────────────────────────────────────────
  │  │
  │  ├─ buildWithConfig()
  │  │    ├─ MaterialTask → TaskDispatch（主表框架）
  │  │    │   字段映射见 16.1
  │  │    ├─ MaterialTaskNode → TaskDispatchNode（子表框架）
  │  │    │   字段映射见 16.2
  │  │    └─ 计算 nodeTask 和 currentNodeType
  │  │
  │  ├─ buildTaskDispatch()
  │  │    ├─ buildWithCreator() — 设置创建人
  │  │    └─ buildTaskDispatchWithTime() — 核心逻辑
  │  │        ├─ PLAN_TIME → 查排期 + 偏移计算
  │  │        ├─ IMMEDIATELY → new Date()
  │  │        ├─ DEPENDENT_NODE → 查路由 → 查前置任务
  │  │        └─ 其他 → INIT_DATE
  │  │
  │  └─ [保存 task_dispatch] ─→ 批量 INSERT，回填 ID
  │
  ├─ [构建子表] ──────────────────────────────────────────────
  │  │
  │  ├─ buildTaskDispatchNode()
  │  │    ├─ fillWithTaskDispatchId() — 设置外键
  │  │    ├─ buildWithExecutor() — 分配执行人
  │  │    │    └─ 按配置角色 → 查订单 → 分配具体人
  │  │    └─ buildTaskDispatchNodeWithTime()
  │  │         └─ calculateEstimatedTime() → 预计时间
  │  │
  │  └─ [保存 task_dispatch_node] ─→ 批量 INSERT
  │
  ├─ [消息推送阶段] ──────────────────────────────────────────
  │  │
  │  ├─ pushMsg()
  │  │    └─ 查 status=UNCOMPLETED 的节点 → 推送到 IM/App
  │  │
  │  └─ [事务提交后异步] activateTaskDispatchAsync()
  │       ├─ 事务提交 → 异步 CompletableFuture
  │       ├─ 逐个激活 TaskDispatch
  │       │    ├─ 检查 status==UN_ACTIVE
  │       │    ├─ 检查 planActivateTime 在今天
  │       │    ├─ 有 processCode → v2激活
  │       │    └─ 无 processCode → v1激活
  │       │         ├─ shouldActivate() → 排期依赖
  │       │         ├─ checkInterceptConfigure() → 尾款拦截
  │       │         └─ 正式激活: UN_ACTIVE→UNCOMPLETED
  │       └─ 激活第一个节点
  │
  └─ afterTransactional()
       └─ 发送 MQ 消息: StateChangeType.CREATE
            ├─ material-task-dispatch-state-change (v1)
            └─ material-task-dispatch-state-change-v2 (v2)
```

---

## 附录：核心代码文件路径索引

| 文件 | 路径 |
|------|------|
| 批量创建入口（v2） | `edar-starlord-service/.../service/v2/impl/TaskDispatchBatchCreateServiceImpl.java` |
| 批量创建接口 | `edar-starlord-service/.../service/v2/TaskDispatchBatchCreateService.java` |
| 创建上下文 | `edar-starlord-service/.../service/v2/param/TaskDispatchCreateContext.java` |
| 创建参数 BO | `edar-starlord-service/.../service/bo/MaterialBatchCreateBO.java` |
| 供应商-品类 DTO | `edar-starlord-api/.../dto/SupplierMaterialPair.java` |
| v1 创建+激活服务 | `edar-starlord-service/.../service/impl/TaskDispatchCreateServiceImpl.java` |
| v1 激活接口 | `edar-starlord-service/.../service/DispatchCreateService.java` |
| v2 激活服务 | `edar-starlord-service/.../servicev2/impl/MaterialActivateV2ServiceImpl.java` |
| 模板配置 DAO | `edar-starlord-dao/.../dao/impl/MaterialTaskDaoImpl.java` |
| 模板节点 DAO | `edar-starlord-dao/.../dao/MaterialTaskNodeDao.java` |
| TaskDispatch DAO | `edar-starlord-dao/.../dao/impl/TaskDispatchDaoImpl.java` |
| 组合条件 SQL | `edar-starlord-dao/.../resources/mapper/ext/MaterialTaskExtMapper.xml` |
| TaskDispatch 组合SQL | `edar-starlord-dao/.../resources/mapper/ext/TaskDispatchExtMapper.xml` |
| 激活模式枚举 | `edar-starlord-base/.../enumeration/task/ActivateModeEnum.java` |
| 任务状态枚举 | `edar-starlord-base/.../enumeration/TaskDispatchStatusEnum.java` |
| 节点状态枚举 | `edar-starlord-base/.../enumeration/TaskDispatchNodeStatusEnum.java` |
| 角色枚举 | `edar-starlord-base/.../enumeration/RoleTypeEnum.java` |
| 节点类型枚举 | `edar-starlord-base/.../enumeration/NodeTypeEnum.java` |
| 执行人分配 | `edar-starlord-service/.../service/impl/InstallerTaskServiceImpl.java` |
| 预计时间计算 | `edar-starlord-service/.../service/biz/EstimatedTimeService.java` |
| 节点工具类 | `edar-starlord-service/.../service/util/NodeTypeUtil.java` |
| LangUtils | `edar-starlord-service/.../service/util/LangUtils.java` |
| MaterialTask 模型 | `edar-starlord-dao/.../model/MaterialTask.java` |
| MaterialTaskNode 模型 | `edar-starlord-dao/.../model/MaterialTaskNode.java` |
| TaskDispatch 模型 | `edar-starlord-dao/.../model/TaskDispatch.java` |
| TaskDispatchNode 模型 | `edar-starlord-dao/.../model/TaskDispatchNode.java` |
| 装修项目订单 BO | `edar-starlord-service/.../service/bo/ProjectOrderDetailBO.java` |
| 施工排期 BO | `edar-starlord-service/.../service/bo/ProjectScheduleItemBO.java` |
| 查询条件 | `edar-starlord-dao/.../bean/QueryMaterialTaskCondition.java` |
| 查询条件(TD) | `edar-starlord-dao/.../bean/QueryTaskDispatchCondition.java` |

---
## 相关文档

- [[starlord项目整理/接口逻辑梳理/主材任务V2激活处理逻辑（MaterialActivateV2ServiceImpl）]] — V2激活逻辑详解
- [[starlord项目整理/接口逻辑梳理/旧版激活方式方法逻辑]] — V1旧版激活逻辑对比
- [[starlord项目整理/接口逻辑梳理/主材任务节点完成处理（MaterialHandleV2ServiceImpl.handleNode）业务流梳理]] — 节点完成处理逻辑
- [[starlord项目整理/接口逻辑梳理/主材任务（TaskDispatch）的批量创建入口]] — 批量创建入口
- [[starlord项目整理/主材任务流程梳理]] — 主材任务全流程流转图
- [[项目分析/edar-starlord项目学习/entities/task-dispatch-v2-service]] — TaskDispatchV2服务实体分析
