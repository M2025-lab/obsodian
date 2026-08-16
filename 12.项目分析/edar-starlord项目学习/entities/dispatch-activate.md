---
title: DispatchActivateService - 任务调度激活服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags:
  - task-dispatch
  - activate
  - scheduled
  - service
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/DispatchActivateService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/DispatchActivateServiceImpl.java
confidence: high
---

# DispatchActivateService 任务调度激活服务

## 概述
定时扫描所有计划激活时间为当天的 TaskDispatch（任务调度单），调用 DispatchCreateService 触发实际的任务创建流程。是交付流程的"定时启动器"，确保配置好的物料任务按计划时间自动生效。搭配 [[task-dispatch-v2]] 和 [[material-schedule]] 使用。

## Key Methods

### activateTaskDispatch()
定时激活任务。逻辑：
1. 查询 material_task 表中所有计划激活时间（plan_activate_start_date）为今天的记录
2. 关联 task_dispatch 表，找到对应的 TaskDispatch
3. 对每个待激活的 TaskDispatch，调用 DispatchCreateService 走完整创建流程
4. 激活失败的记录日志但不中断后续处理
^[service/impl/DispatchActivateServiceImpl.java:50-93]

## Entry Points

### REST Controllers
- 无直接 Controller 入口。通过以下方式触发：

### Scheduled Tasks
- MaterialTaskSchedule → `activateTaskDispatch()` — 定时任务，每天执行
  → `code-path: edar-starlord-service/.../service/schedule/MaterialTaskSchedule.java`

### 跨Service调用
- TaskDispatchActivateService (V2) → 同样是激活逻辑的 V2 版本
  → `code-path: edar-starlord-service/.../service/v2/TaskDispatchActivateService.java`

## Dependencies
- **DAO**: TaskDispatchDao, MaterialTaskDao
- **Service**: DispatchCreateService, TaskProcessBatchService

## 激活流程
```
MaterialTaskSchedule (定时触发)
  → activateTaskDispatch()
    → 查询 plan_activate_start_date = today 的 material_task
    → 关联 task_dispatch
    → DispatchCreateService.createTaskDispatch() (创建调度实例)
    → TaskProcessBatchService (批量处理)
```

## 相关页面
- [[task-dispatch-v2]] — 任务调度V2
- [[material-schedule]] — 物料排期
