---
title: TaskDispatchCompleteService - 任务完成服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [task-dispatch, material-task, service, delivery, event-driven]
sources:
  - edar-starlord-service/.../servicev2/TaskDispatchCompleteService.java
  - edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java
confidence: high
---

# TaskDispatchCompleteService

## 概述

`TaskDispatchCompleteService` 是主材任务完成服务的核心实现，负责根据外部业务事件（供应链订单状态变更、验收报告状态变更、施工包状态变更等）来驱动主材任务节点的完成。它作为事件驱动的桥梁，将 SCM（供应链管理）、验收、施工包等系统的状态变更，转化为主材任务系统中的节点完成操作。

该服务不直接操作数据库，而是通过 `MaterialHandleV2Service.handleTask` / `handleNode` 统一处理任务完成逻辑。

^[edar-starlord-service/.../servicev2/TaskDispatchCompleteService.java]

## 关键方法

### 1. 通知类任务完成

#### `completeNoticeMeasureTask(MaterialOrderInfoBO)`
完成通知测量任务。当供应链订单中给出了"期望测量时间"时，先更新通知时间，再完成通知节点。

#### `completeNoticeEnterOrInstallTask(MaterialOrderInfoBO)`
完成通知送装任务。根据 `needInstall` 判断是安装还是送货类型，完成对应的通知节点。

核心逻辑（`completeNoticeNodeTask`）：查询对应项目、订单、物料、供应商的**通知可开始**（`NOTIFY_CAN_START`）节点，先调用 `dispatchCommonService.changeAppoint` 更新通知时间，再调用 `materialHandleV2Service.handleNode` 完成节点。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:80-151]

### 2. 供应链订单驱动的任务完成

这些方法响应供应链子订单（SCM SubOrder）的状态变更：

| 方法 | 触发事件 | 对应目标节点 |
|------|---------|-------------|
| `completeOrderTakingTask` | 订单接单（`TAKE_ORDER`）或拒单（`REJECT`） | 接单任务完成（拒单标记不合格） |
| `completeQuoteTask` | 报价审核通过 / 驳回 | 报价 60（START）或 80（FINISH_START）节点 |
| `completeRetailOrderTask` | 零售订单推送（`PUSH_ORDER`） | 零售下单任务完成 |
| `completeEnterTask` | 待签收 / 已完成 / 拒绝签收 / 送达 | 送货 60/80 节点 |
| `completeStockUpTask` | 备货完成 / 出库 | 备货任务完成 |
| `completeWareHouseEnterTask` | 备货完成 / 出库 | 进仓任务完成 |

每种任务都通过 `materialHandleV2Service.handleTask(handleParam, taskDispatchParam, operatorDTO)` 执行完成操作。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:262-479]

### 3. 验收报告驱动的安装任务完成 — `completeInstallTask(AcceptanceReportStatusBO)`

根据验收报告状态驱动安装任务：
- `FOREMAN_CONFIRMING`（待审核）→ 完成安装 60 节点
- `COMPLETE`（已确认）→ 完成安装 80 节点（合格）
- `NOT_APPROVED`（驳回）→ 完成安装 80 节点（不合格，记录驳回原因）

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:482-528]

### 4. 施工包驱动的任务完成

#### `completeHome2_5InstallTask(String packageCode, Integer packageStatus, ...)`
处理 2.5 模式（自营）施工包状态变更：
- 先判断模式，如果是 2.5 或供应商模式 → 委托给 `completePackageTask` 走新逻辑
- 否则按老逻辑：根据施工包状态（预约/派单/自检/验收/完成）逐级完成对应节点

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:539-627]

#### `completePackageTask(String packageCode, Integer newVal, Integer oldVal, ...)`
2.5 新逻辑的施工包状态驱动。核心状态机：

| 施工包状态 | 完成节点 | 说明 |
|-----------|---------|------|
| 取消（`ORDER_CANCEL`~） | — | 触发主材任务取消 |
| `RESERVING` → `DISPATCHING` | `NOTIFY_CAN_START` | 完成预约 |
| `DISPATCHING` → `WAIT_APPROACH` | `NOTIFY_START` | 完成派单 |
| `WAIT_APPROACH` → `PROCESSING` | `SELF_CHECK` | 完成进场 |
| `PROCESSING` | `SELF_CHECK_ACCEPTANCE` | 完成自检验收 |
| `PROCESSING`（驳回） | `SELF_CHECK_ACCEPTANCE` / `FINISH_START` | 不合格完成 |
| `BUTLER_CHECK` | `FINISH_START` | 完成管家验收 |
| `SELF_CHECK` | `START`（60节点） | 完成自检 |
| `COMPLETE`~ | `OWNER_CONFIRM` + `FINISH_START` | 完成业主确认 |
| 状态回退（`newVal < oldVal`） | `SELF_CHECK` + `START` | 施工包约工驳回，标记不合格 |

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:764-868]

#### `completeConstructionPackageInstallTask`
处理施工包（Construction Package）的安装任务完成，逻辑与 `completeHome2_5InstallTask` 类似但更简洁。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:670-742]

### 5. 自动完成 40 节点 — `completeCanStartNodeByStartNodeExecuorterId(Long)`

优化逻辑：如果任务的 60 节点（`START`）已经赋值了执行人，则自动完成该任务的 40 节点（`NOTIFY_START`），减少人工操作步骤。在任务生成时调用。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:749-762]

### 6. 已完成任务

#### `completeMeasureTask(MaterialOrderInfoBO)`
直接完成测量任务的 START 节点。

#### `completeOrderTask(MaterialOrderInfoBO)`
完成下单任务，同时更新关联的订单号。

#### `completeDesignReview(MaterialOrderInfoBO)`
完成报价变更任务。特殊逻辑：同步到 SDM（通过 `omsMessageSyncService.sendVssFinish`）。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:154-251]

### 7. 同步施工包任务状态 — `syncPackageTaskStatus(String)`

根据施工包当前状态，逐级同步完成所有应该完成的节点。用于修复/补齐场景。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java:910-982]

## 入口点

| 入口类型 | 位置 | 说明 |
|---------|------|------|
| MQ Listener | `ScmOrderEventHandler` | 监听供应链子订单状态变更消息 |
| MQ Listener | `ProjectVssEventHandler` | 监听 VSS 项目事件（设计变更） |
| MQ Listener | `AcceptanceReportStatusChangeEventHandler` | 监听验收报告状态变更 |
| MQ Listener | `WorkCenterProcessChangedEventHandler` | 监听作业中心流程变更 |
| MQ Listener | `WorkCenterTaskChangedEventHandler` | 监听作业中心任务变更 |
| Controller | `MaterialConstructionTaskController` | 施工包任务管理 |
| Controller | `RefreshDataController` | 数据修复 |
| 内部调用 | `MaterialCreateV2ServiceImpl` | 任务创建后自动完成 40 节点 |
| 内部调用 | `DeliveryMaterialBizServiceImpl` | 交付物料业务 |

^[edar-starlord-service/.../listener/handler/ScmOrderEventHandler.java]

## 依赖

| 依赖 | 用途 |
|------|------|
| `MaterialHandleV2Service` | 统一的任务处理（handleTask / handleNode） |
| `DispatchCommonService` | 通用派发操作（changeAppoint） |
| `TaskDispatchNodeDao` / `TaskDispatchDao` | 任务节点/主表查询 |
| `MaterialCancelV2Service` | 施工包取消时批量取消主材任务 |
| `PackageConstructionManager` / `PackageConstructionQueryManager` | 施工包信息查询 |
| `ProjectOrderManager` | 项目订单查询 |
| `AcceptanceReportManager` | 验收报告查询 |
| `OmsMessageSyncService` | OMS 消息同步 |
| `ScmManager` | 供应链管理接口 |

## 状态机 / 工作流

该服务本身不维护状态机，而是作为外部事件和内部任务完成之间的适配层：

```
外部事件（SCM 状态变更 / 验收状态变更 / 施工包状态变更）
        │
        ▼
TaskDispatchCompleteService（事件→任务操作映射）
        │
        ▼
MaterialHandleV2Service.handleTask / handleNode（统一任务处理）
        │
        ▼
任务节点状态变更 + MQ 消息发送
```

核心映射关系：SCM 子订单状态 ↔ 主材任务节点类型：
- `TAKE_ORDER` → 接单任务 60 节点
- `MEASUREMENT_QUOTATION` → 报价任务 60 节点
- `QUOTATION_PASSED` → 报价任务 80 节点（合格）
- `QUOTATION_REJECTED` → 报价任务 80 节点（不合格）
- `WAITING_SIGN` / `TRANSIT_FINISH` → 送货 60 节点
- `FINISHED` → 送货 80 节点
- `SIGN_REJECTED` → 送货 80 节点（不合格）

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCompleteServiceImpl.java]

## 参见

- [[task-dispatch-v2-service]] — 任务派发 V2 核心服务
- [[material-handle-v2-service]] — 任务处理统一入口
- [[material-cancel-v2-service]] — 任务取消服务
