---
title: MaterialDelayProcessService - 物料延期处理服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, delay, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/delay/MaterialDelayProcessService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/delay/impl/MaterialDelayProcessServiceImpl.java
confidence: high
---

# MaterialDelayProcessService 物料延期处理服务

## 概述
管理物料任务延期的完整生命周期：创建延期单、补录延期原因、确认延期、查询延期、删除延期。延期原因按角色（业主/非业主）分类，确认后推送通知。同时处理来自 WorkCenter 的延期撤销任务和管家/经理派工事件触发的延期处理。

## Key Methods

### createMaterialDelayProcess(param)
创建延期处理单。根据 businessId 查询物料信息，生成延期原因（按业主/非业主角色拆分成多条记录），关联 ProjectOrder 和 TaskDispatch。支持自定义内部品类（customInnerCategoryCodes），非定制品类跳过延期原因创建。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:200-350]

### updateMaterialDelayProcess(param)
更新延期处理。先删除未确认的旧延期单和原因（业主角色的发送 Kafka 删除通知），再重新创建。确保只修改未确认状态的记录。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:148-188]

### confirmDelayProcess(param)
确认延期。验证延期单存在且未确认 → 更新确认状态 → 计算承诺日期 → 记录日志 → 发送变更通知。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:350-420]

### queryReasonList(projectOrderId, businessId)
查询指定订单+业务ID的延期原因列表，按业主/非业主角色分组展示。区分补录类型（RELATION_POST_RECORD）和非补录类型。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:128-144]

### materialDelayProcessDetail(materialDelayProcessId)
延期单详情：查询延期单 + 延期原因 + 操作日志。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:191-215]

### previewMaterialDelayProcess(param)
预览延期：不保存，只返回预览的延期数据，用于前端展示确认前的结果。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:600+]

### createDelayUndoTasks(payloadData)
根据 WorkCenter 任务变更消息，撤销/删除相关的延期任务。处理 TaskInstance 的状态回退。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:800+]

### handleButlerManagerAssign(payloadData)
处理管家/经理派工事件：如果是项目经理指派 → 检查并处理延期。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java:900+]

### queryMaterialDelayReasonList()
查询所有延期原因枚举（MaterialDelayReasonEnum），按一级原因分组。^[servicev2/delay/impl/MaterialDelayProcessServiceImpl.java]

## Entry Points

### REST Controllers
- MaterialDelayProcessController → MaterialDelayProcessService（完整 CRUD + 查询导出）
  → `code-path: edar-starlord-web/.../web/MaterialDelayProcessController.java`

### Kafka Listeners（事件驱动）
- WorkCenterTaskChangedEventHandler → `createDelayUndoTasks()` — 工作中心任务变更时撤销延期
  → `code-path: edar-starlord-service/.../listener/handler/utopia/WorkCenterTaskChangedEventHandler.java`
- AssignProjectServerInfoEventHandler → `handleButlerManagerAssign()` — 管家/经理派工调整触发延期检查
  → `code-path: edar-starlord-service/.../listener/handler/AssignProjectServerInfoEventHandler.java`

### Feign Interfaces
- MaterialDelayProcessFeign（API 模块） → 远程调用本服务方法
  → `code-path: edar-starlord-api/.../api/MaterialDelayProcessFeign.java`

## Dependencies
- **DAO**: MaterialDelayProcessDao, MaterialDelayProcessReasonDao, MaterialDelayProcessLogDao, TaskDispatchNodeDao
- **Manager**: ProjectOrderManager, ConstructionManager, CeresManager, AtomManager, AthenaManager, PackageConstructionManager
- **Producer**: MaterialDelayProcessChangeProducer（Kafka 发送变更通知）
- **Service**: MaterialDelayProcessDateCalculateService（承诺日期计算）
- **Message**: MessagePushClient（消息推送）

## State Machine
```
新建(未确认) → [confirmDelayProcess] → 已确认 → 记录日志 + 推送通知
新建(未确认) → [updateMaterialDelayProcess] → 删除旧 + 重建新的未确认
新建(未确认) → [deleteMaterialDelayProcess] → 删除
```
