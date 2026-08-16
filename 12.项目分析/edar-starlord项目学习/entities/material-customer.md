---
title: MaterialCustomerService - 客户主材进展服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, material, customer-facing, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/MaterialCustomerService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/MaterialCustomerServiceImpl.java
confidence: high
---

# MaterialCustomerService 客户主材进展服务

## 概述
面向 C 端客户的主材进展查询服务。将 TaskDispatch（任务调度）数据按物料维度（materialCode + supplierCode）聚合为"主材进展"视图，供客户查看家中主材的排产、生产、送货、安装进度。支持按 projectOrderId 查询全量进展、按物料+供应商查询单个物料详情、全量扫描索引。

## Key Methods

### listTaskDispatch(projectOrderId)
查询项目订单下所有主材进展。将 TaskDispatch 按 materialCode + supplierCode 聚合分组，每个组返回最新一条 TaskDispatch 的状态，构建 MaterialTaskDispatchItemDTO 列表。包含排产、生产、发货、安装等多个阶段的进度信息。^[service/impl/MaterialCustomerServiceImpl.java:55-150]

### taskDispatchDetail(materialCode, supplierCode, orderNo)
单个物料详情。查询指定物料+供应商+订单号下的完整 TaskDispatch 列表（全部阶段节点），构建 MaterialTaskDispatchInfoDTO。^[service/impl/MaterialCustomerServiceImpl.java:150+]

### listProjectOrderIds(taskTypeIn, processStatusIn, pageSize, ...)
全量扫描 projectOrderId。用于定时任务/批量刷新场景，分页获取有指定任务类型和状态的 projectOrderId 列表。^[service/impl/MaterialCustomerServiceImpl.java:350+]

## Entry Points

### REST Controllers
- MaterialCustomerController → `listTaskDispatch()`, `taskDispatchDetail()`
  → `code-path: edar-starlord-web/.../web/MaterialCustomerController.java`

### Feign Interfaces
- MaterialCustomerFeign → C端远程调用
  → `code-path: edar-starlord-api/.../api/MaterialCustomerFeign.java`

### Kafka Producer
- MaterialCustomerProducer → C端数据生产者（推送到客户可见的数据总线）
  → `code-path: edar-starlord-service/.../service/producer/MaterialCustomerProducer.java`

### Scheduled Tasks
- MaterialVssSchedule → 定时刷新 C 端进展数据
  → `code-path: edar-starlord-service/.../service/schedule/MaterialVssSchedule.java`

### 跨Service调用
- CommonTaskModuleServiceImpl → `listTaskDispatch()` — 通用任务模块调用
  → `code-path: edar-starlord-service/.../servicev2/impl/CommonTaskModuleServiceImpl.java`

## Dependencies
- **DAO**: TaskDispatchDao, TaskDispatchNodeDao
- **Manager**: RedisService (缓存 C 端数据)
- **Enum**: TaskTypeEnum, TaskTypeSortEnum

## 相关页面
- [[task-dispatch-v2]] — 后端任务调度（数据来源）
