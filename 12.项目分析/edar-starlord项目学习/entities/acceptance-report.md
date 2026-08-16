---
title: AcceptanceReportService - 验收报告服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, acceptance, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/AcceptanceReportService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/AcceptanceReportServiceImpl.java
confidence: high
---

# AcceptanceReportService 验收报告服务

## 概述
管理安装工验收报告的全生命周期：获取验收模板、提交验收报告（单条/批量）、查询验收详情、合并验收报告。支持施工验收标准和套餐验收。与 [[task-dispatch-v2]] 的 TaskDispatchNode 紧密关联。

## Key Methods

### getAcceptanceTemplate(param)
根据任务调度节点获取验收模板。查询施工工序的验收标准配置，返回验收项+动作列表供安装工填写。^[service/impl/AcceptanceReportServiceImpl.java:150+]

### submitInstallerAcceptanceReport(param)
安装工提交验收报告。包含验收动作（合格/不合格）、验收标准、图片附件。提交后更新 TaskDispatchNode 状态。^[service/impl/AcceptanceReportServiceImpl.java:300+]

### queryInstallerAcceptanceReport(taskDispatchNodeId, taskDispatchId)
查询安装工验收报告详情，按 TaskDispatchNode + TaskDispatch 维度查询。^[service/impl/AcceptanceReportServiceImpl.java]

### combineInstallerAcceptanceReport(param)
合并安装工验收报告：将多个节点的验收数据合并为一张综合验收报告。用于项目经理/管家查看整体验收情况。^[service/impl/AcceptanceReportServiceImpl.java]

### batchAcceptanceSubmit(param)
批量验收提交：同一任务下多个节点的验收一次性提交。^[service/impl/AcceptanceReportServiceImpl.java]

### packageAcceptanceSubmit(param)
套餐验收提交：针对套餐类型的验收，关联套餐 package 信息。^[service/impl/AcceptanceReportServiceImpl.java]

### getReportRelation(taskDispatchNodeId)
获取验收报告的关联关系（TaskDispatchNodeReportRelation），包括前置节点报告等。^[service/impl/AcceptanceReportServiceImpl.java]

## Entry Points

### REST Controllers
- AcceptanceReportController → AcceptanceReportService（全部方法）
  → `code-path: edar-starlord-web/.../web/AcceptanceReportController.java`
- AcceptanceReportFixController（修复/迁移用）
  → `code-path: edar-starlord-web/.../web/transfer/AcceptanceReportFixController.java`

### Feign Interfaces
- AcceptanceReportFeign → 远程 RPC 调用验收服务
  → `code-path: edar-starlord-api/.../api/AcceptanceReportFeign.java`
- AcceptanceReportFixFeign → 修复工具远程调用
  → `code-path: edar-starlord-api/.../api/transfer/AcceptanceReportFixFeign.java`

### 跨 Service 调用
- InstallerTaskServiceImpl → `submitInstallerAcceptanceReport()` — 安装工任务服务内部调用验收提交流程
  → `code-path: edar-starlord-service/.../service/impl/InstallerTaskServiceImpl.java`

## Dependencies
- **Manager**: ProjectOrderManager, ConstructionManager, ZeusManager, AcceptanceReportManager
- **DAO**: TaskDispatchNodeDao, TaskDispatchNodeRelationDao
- **Service**: TaskDispatchService, MaterialHandleV2Service, MaterialMigrationV2Service

## 状态变更
```
验收模板获取 → 安装工填写 → submitInstallerAcceptanceReport → TaskDispatchNode 状态变更
                                                    ↓
                                          批量验收 → batchAcceptanceSubmit
                                                    ↓
                                          合并报告 → combineInstallerAcceptanceReport
```
