---
title: CoordinatorTaskService - 返补单协调器服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, coordinator, replenish, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/coordinator/CoordinatorTaskService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/coordinator/impl/CoordinatorTaskServiceImpl.java
confidence: high
---

# CoordinatorTaskService 返补单协调器服务

## 概述
管理供应商返补单（Replenish Order）跟单任务的生命周期。在家装交付中，当物料出现缺货、破损或客户变更时，需要供应商"返补"发货。本服务协调整个返补流程：下单、分配协调人、跟进进度、修改状态、查询返补任务列表。与 [[material-delivery]] 中的 DeliveryMaterialBizService 和 SCM 订单系统紧密交互。

## Key Methods

### placeOrder(param)
创建返补跟单任务。接收 ReplenishInfPlaceOrderParam（品牌订单号、子订单号、返补单号、物料信息），调用 CoordinatorPlaceOrderService 实际下单，设置计划提货时间。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### replenishOrderAssignmentToCoordinator(param)
返补单分配协调人。给返补跟单任务指派协调员（人），用于跟进返补进度。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### listReplenishCoordinatorTask(param)
查询返补跟单任务列表。支持按项目订单ID、品牌订单号、返补单号多维度搜索。分为整装（project）和零售（retail）两个版本。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### taskProcessSubmit(param)
返补单跟进进度提交。协调员提交跟单进度（CoordinatorProgressSubmitParam），返回处理结果。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### handleWorkOrderNotice(payloadData)
处理工单创建通知。当外部施工系统创建工单时，检查是否需要创建返补跟单任务。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### handleWorkOrderChangeNotice(payloadData)
处理工单变更通知。工单状态变更时同步更新返补跟单任务状态。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### coordinatorTaskFollowUpRefresh()
跟单任务跟进刷新。定时刷新返补跟单任务的状态（从 SCM 拉取最新状态）。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### checkReplenishOrder(brandOrderNo, ...)
校验返补单号合法性，防止重复下单。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

### setPlanPickupTime(param)
设置计划提货时间。返补跟单中的关键节点时间。^[coordinator/impl/CoordinatorTaskServiceImpl.java]

## Entry Points

### REST Controllers
- ReplenishCoordinatorTaskController → CoordinatorTaskService（列表查询、下单、分配、提货时间）
  → `code-path: edar-starlord-web/.../web/coordinator/ReplenishCoordinatorTaskController.java`

### Feign Interfaces
- ReplenishCoordinatorTaskFeign → 远程查询返补跟单任务
  → `code-path: edar-starlord-api/.../api/ReplenishCoordinatorTaskFeign.java`

### Kafka Listeners
- WorkOrderCreateEventHandler → `handleWorkOrderNotice()` — 工单创建事件触发返补检查
  → `code-path: edar-starlord-service/.../listener/handler/workorder/WorkOrderCreateEventHandler.java`
- WorkOrderChangeEventHandler → `handleWorkOrderChangeNotice()` — 工单变更同步
  → `code-path: edar-starlord-service/.../listener/handler/workorder/WorkOrderChangeEventHandler.java`

### Scheduled Tasks
- CoordinatorTaskSchedule → `coordinatorTaskFollowUpRefresh()` — 定时刷新返补单状态
  → `code-path: edar-starlord-service/.../service/schedule/CoordinatorTaskSchedule.java`

### 内部调用
- PlaceOrderCommander → `placeOrder()` — Commander 模式的下单执行器
  → `code-path: edar-starlord-service/.../coordinator/PlaceOrderCommander.java`

## Dependencies
- **DAO**: CoordinatorTaskOrderDao, CoordinatorTaskProgressDao
- **Manager**: ScmManager, ProjectOrderManager, AthenaManager
- **Service**: CoordinatorPlaceOrderService, CoordinatorSetSuccessService
- **Commander**: PlaceOrderCommander, PlaceTimeCommander, FollowUpCommander

## 返补单状态流转
```
工单创建事件 → handleWorkOrderNotice → 检查是否需要返补
          ↓
placeOrder (创建返补跟单) → 分配协调人 → 跟进中
          ↓
taskProcessSubmit (提交进度) → checkReplenishOrder (校验) → 完成
          ↓
coordinatorTaskFollowUpRefresh (定时同步SCM状态)
```
