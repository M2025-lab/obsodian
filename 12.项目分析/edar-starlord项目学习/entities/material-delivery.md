---
title: MaterialDeliveryService - 物料送货服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, material, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/MaterialDeliveryService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/MaterialDeliveryServiceImpl.java
confidence: high
---

# MaterialDeliveryService 物料送货服务

## 概述
管理主材送货任务的查询和统计。区分"单独送货"和"批量送货"两种模式：单独送货通过 [[task-dispatch-v2]] 查询，批量送货通过 [[material-batch-v2]] 查询，最终合并展示。是交付模块中面向安装工和管家的送货任务列表视图。

## Key Methods

### listDeliveryTask(param)
列出送货任务。两种模式合并：
1. 单独送货：调用 TaskDispatchService 查询主材进场节点（TaskTypeEnum.ENTER）下当前执行人的送货任务
2. 批量送货：调用 MaterialBatchV2Service 查询批量送货的 TaskProcessBatch
合并后按预计时间排序返回 List<MaterialTaskCommonDTO>。^[service/impl/MaterialDeliveryServiceImpl.java:46-100]

### countDeliveryTask(param)
统计送货任务数量。逻辑同 listDeliveryTask，但只返回计数。^[service/impl/MaterialDeliveryServiceImpl.java:100+]

### listDeliveryTime(param)
按日期列出送货时间安排。根据 DeliveryDateTimeListParam 中的 projectOrderId 列表查询每个订单的预计送货时间，返回 List<DeliveryDateTimeDTO>。用于排期看板。^[service/impl/MaterialDeliveryServiceImpl.java:120+]

## Entry Points

### REST Controllers
- MaterialDeliveryController → `listDeliveryTask()`, `countDeliveryTask()`, `listDeliveryTime()`
  → `code-path: edar-starlord-web/.../web/MaterialDeliveryController.java`
- MaterialDeliveryV2Controller → V2 版本入口
  → `code-path: edar-starlord-web/.../web/MaterialDeliveryV2Controller.java`

### Feign Interfaces
- MaterialDeliveryFeign → 远程查询送货任务
  → `code-path: edar-starlord-api/.../api/MaterialDeliveryFeign.java`
- MaterialDeliveryV2Feign → V2远程调用
  → `code-path: edar-starlord-api/.../api/MaterialDeliveryV2Feign.java`

### 跨Service调用
- TaskDispatchServiceImpl → `listDeliveryTime()` — 任务调度服务内部调用送货时间查询
  → `code-path: edar-starlord-service/.../service/impl/TaskDispatchServiceImpl.java`
- MaterialTaskInstallerTaskServiceImpl → `listDeliveryTask()` — 安装工任务服务调用
  → `code-path: edar-starlord-service/.../service/foreman/impl/MaterialTaskInstallerTaskServiceImpl.java`

## Dependencies
- **Service**: TaskDispatchService, TaskProcessBatchService, MaterialBatchV2Service
- **DAO**: TaskDispatchNodeDao

## 相关页面
- [[task-dispatch-v2]] — 任务调度V2
- [[material-batch-v2]] — 物料批量操作V2
