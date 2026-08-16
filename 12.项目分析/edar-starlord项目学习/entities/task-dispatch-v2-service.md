---
title: TaskDispatchV2Service - 任务派发V2服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [task-dispatch, material-task, service, delivery]
sources:
  - edar-starlord-service/.../servicev2/TaskDispatchV2Service.java
  - edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java
confidence: high
---

# TaskDispatchV2Service

## 概述

`TaskDispatchV2Service` 是主材任务派发的核心服务，负责主材任务实例（`TaskDispatch`）和任务节点（`TaskDispatchNode`）的直接保存、更新、查询操作。它是 V2 版本的任务派发入口，替代了旧版的 `TaskDispatchService`，主要服务于 2.5 模式和交付流（Delivery Flow）模式的任务生命周期管理。

核心职责：
- **任务与节点的直接持久化**：不经过流程引擎，直接写入数据库（`directSaveMaterialTask`、`saveMaterialTaskV2`、`updateMaterialTaskV2`）
- **任务详情查询**：按项目、供应商、物料等维度分页查询任务及节点详情
- **任务批次管理**：管理批次任务（`TaskProcessBatch`）的创建、更新及与节点的关联关系
- **节点配置查询**：查询节点的流程配置、模板信息
- **采购单号管理**：为任务生成和更新采购单号

^[edar-starlord-service/.../servicev2/TaskDispatchV2Service.java]

## 关键方法

### 1. 任务直接保存 — `directSaveMaterialTask(MaterialTaskDirectSaveParam)`

核心的幂等保存方法。根据项目的物料+供应商+任务类型组合查询已有任务：
- **无已存在任务**：新增 `TaskDispatch` + `TaskDispatchNode`，发送 CREATE 消息
- **已存在任务**：更新已有的任务和节点。对于下单任务（ORDER）且状态非完成的场景，置空订单号。

特殊处理：
- 2.5 模式任务（`ModeEnum.isHome2_5_MODE`）才允许更新，非 2.5 模式直接返回
- 节点不存在时新增，已存在时根据状态发送不同的变更消息（CANCEL、节点完成、时间变更）

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:516-606]

### 2. 任务存储 V2 — `saveMaterialTaskV2(MaterialTaskDirectSaveParam)`

事务方法，内部调用 `innerSaveMaterialTaskV2` 完成新增逻辑。区别于 `directSaveMaterialTask`：**仅新增，不做更新判断**。落库后：
- 通过 `materialCreateV2Service.buildTaskPurchaseOrderNo()` 创建采购单号
- 发送 `StateChangeType.CREATE` 消息
- 如果是交付流模式（`DELIVERY_FLOW`），异步计算并更新计划激活时间

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:608-649]

### 3. 任务更新 — `updateMaterialTaskV2(MaterialTaskDirectSaveParam, TaskDispatch, List<TaskDispatchNode>)`

更新已有任务及节点。特殊逻辑：
- 下单任务未完成时清空订单号
- 完成后发送 `materialCustomerProducer.publishMaterialCustomer` 
- 报价变更（`DESIGN_REVIEW`）任务完成后同步 SDM（发送 VSS 完成消息）
- 报价变更任务取消时，同步取消 SDM 采购单（带降级重试）

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:813-918]

### 4. 任务详情查询 — `taskDetail(TaskDispatchDetailSimpleParam)`

已标记 `@Deprecated`，建议新场景不使用。分页查询任务列表并组装节点详情。支持多种过滤条件：项目 ID、物料编码、供应商、任务类型、节点状态等。对于复尺任务（`RECHECK_SCALE`），额外填充测量表单的附件和备注信息。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:206-376]

### 5. 节点查询

- **`taskNodeDetail(QuerySameBatchTaskNodeParam)`**：查询同批次的所有节点
- **`matchingNodeDetail(QueryMatchingTaskNodeParam)`**：查询匹配的完成节点（FINISH_START），用于匹配 START → FINISH_START 的节点链
- **`nodeCfgDetail(QueryNodeCfgDetailParam)`**：查询节点的流程配置和分支信息

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:393-496]

### 6. 任务批次管理

- **`saveTaskProcessBatch(TaskProcessBatchDirectSaveParam)`**：创建任务批次
- **`updateTaskProcessBatch(TaskProcessBatch, TaskProcessBatchDirectSaveParam)`**：更新批次及其关联的通知送货节点，支持增量更新节点关联关系（新增/删除）

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:651-748]

### 7. 采购单号更新 — `updatePurchaseOrderNo(List<Long>)`

为指定任务或所有缺失采购单号的任务批量生成采购单号，并同步到 SDM。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java:947-966]

## 入口点

| 入口类型 | 位置 | 说明 |
|---------|------|------|
| Controller | `TaskDispatchV2Controller` | 任务派发 V2 REST 接口 |
| Controller | `MaterialTaskOpenController` | 对外开放接口 |
| Controller | `MaterialConstructionTaskController` | 施工包任务相关 |
| Controller | `ToolsController` | 工具类接口（采购单号修复） |
| Feign | 通过 `MaterialCreateV2Service` | 任务创建时调用 |
| Feign | 通过 `MaterialMeasureServiceImpl` | 测量任务处理 |
| 数据刷新 | `RefreshDataServiceImpl` / `RefreshProcessCodeService` / `RefreshEsTimeService` | 数据修复任务 |

^[edar-starlord-web/.../TaskDispatchV2Controller.java, edar-starlord-service/.../impl/MaterialCreateV2ServiceImpl.java]

## 依赖

| 依赖 | 用途 |
|------|------|
| `TaskDispatchDao` | 任务主表数据访问 |
| `TaskDispatchNodeDao` | 任务节点表数据访问 |
| `TaskProcessBatchDao` | 任务批次表数据访问 |
| `TaskBatchNodeRelationDao` | 批次-节点关联表数据访问 |
| `MaterialConfigV2Service` | 流程配置查询 |
| `MaterialCreateV2Service` | 采购单号生成 |
| `MaterialCommonService` | 计划激活时间计算 |
| `MaterialTaskProducer` | 任务状态变更消息发送 |
| `MaterialCustomerProducer` | 客户关联消息发送 |
| `ScmManager` | 供应链管理（SDM 采购单取消） |
| `OmsMessageSyncService` / `SdmMessageSyncService` | 消息同步到 OMS/SDM |
| `ConfigQueryService` | 交付流排程开关查询 |
| `SeqService` | 采购单号序列生成 |

## 状态机 / 工作流

该服务不实现状态机，而是直接操作任务和节点数据。状态变更通过消息生产者（`MaterialTaskProducer`）发出事件：
- `StateChangeType.CREATE` — 任务/节点创建
- `StateChangeType.CANCEL` — 任务/节点取消
- `TimeChangeType.ESTIMATED_TIME` — 预计时间变更
- `TimeChangeType.APPOINTMENT_TIME` — 预约时间变更

消息由下游监听器消费，驱动后续的业务流程处理。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchV2ServiceImpl.java]

## 参见

- [[task-dispatch-complete-service]] — 任务完成服务，处理各业务类型任务的完成逻辑
- [[task-dispatch-common-service]] — 任务公共工具服务
- [[material-task-biz-v2-service]] — 主材业务查询服务
- [[material-schedule-service]] — 主材排程服务
