---
title: MaterialButlerService - 管家派工服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, butler, assign-worker, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/MaterialButlerService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/MaterialButlerServiceImpl.java
confidence: high
---

# MaterialButlerService 管家派工服务

## 概述
管家（Butler）角色的派工工作台。查询待派工任务、可改约任务、派工统计、历史派工记录。支持整装项目（project）和中央仓（central）两种模式。是交付模块的派工决策入口。

## Key Methods

### recentAppointUncompleted(operator)
查询当前操作员近期未完成的派工任务。按项目订单聚合，返回 AppointWorkerTaskDTO 列表。^[service/impl/MaterialButlerServiceImpl.java:51+]

### recentCanChangeAppoint(operator)
查询可改约的派工任务。已派工但尚未开始执行的节点，支持管家重新调整安装工。^[service/impl/MaterialButlerServiceImpl.java:100+]

### appointWorkerTaskCount(operator)
派工任务数量统计。统计待派工、已派工、改约中、已完工等各状态的任务数量。^[service/impl/MaterialButlerServiceImpl.java]

### projectAppointWorker(operator, param)
项目维度的派工查询。根据 ProjectOrderInfoParam（项目ID、小区、管家等条件）查询待派工任务。^[service/impl/MaterialButlerServiceImpl.java]

### projectAppointTask(operator, param) / projectAppointHistory(operator, param)
项目任务查看 / 历史派工记录。^[service/impl/MaterialButlerServiceImpl.java]

### getPackageRelationTask(taskIdList)
查询套餐关联的任务。输入 TaskDispatch ID 列表，返回哪些套餐（package）关联了这些任务。^[service/impl/MaterialButlerServiceImpl.java]

### centralProjectChangeAppoint(projectOrderId)
中央仓项目改约查询。查询 central 模式下可改约的派工信息。^[service/impl/MaterialButlerServiceImpl.java]

## Entry Points

### REST Controllers
- MaterialButlerController → 全部方法
  → `code-path: edar-starlord-web/.../web/MaterialButlerController.java`

### Feign Interfaces
- MaterialButlerFeign → 远程调用管家派工服务
  → `code-path: edar-starlord-api/.../api/MaterialButlerFeign.java`

### 跨Service调用
- StarlordServiceImpl → `projectAppointWorker()`, `projectAppointTask()`, `projectAppointHistory()`, `centralProjectChangeAppoint()` — 管家主服务转发
  → `code-path: edar-starlord-service/.../service/butler/impl/StarlordServiceImpl.java`

## Dependencies
- **DAO**: TaskDispatchNodeDao, SupplierSyncInfoDao
- **Manager**: ProjectOrderManager, ScmManager, SupplierManager

## 相关页面
- [[starlord-service]] — 管家主服务（调用本服务）
- [[task-dispatch-v2]] — 任务调度
