---
title: MaterialFlowQueryService - 物料流程统一查询服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [material-flow, query, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/MaterialFlowQueryService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/impl/MaterialFlowQueryServiceImpl.java
confidence: high
---

# MaterialFlowQueryService 物料流程统一查询服务

## 概述
提供物料流程的统一配置查询接口，支持按条件查询物料配置数据（模板+节点+规则）和应用层数据（指定任务类型的调度详情）。是交付流程的数据透视层，整合了 [[material-flow-rule]]、[[material-template-v2]] 的配置数据。

## Key Methods

### queryMaterialConfigData(param)
统一配置查询。根据 MaterialConfigDataParam（包含 projectOrderId、businessType、taskType 等条件）查询物料配置树的完整数据：模板定义 + 流程节点 + 规则配置。返回 List<MaterialConfigDataDTO>。^[servicev2/impl/MaterialFlowQueryServiceImpl.java]

### queryMaterialApplyData(param)
查询应用层数据。根据 MaterialTaskDispatchDataParam 查询指定任务类型下的 TaskDispatchDetailDTO 列表（调度实例数据，而非配置数据）。用于查看当前订单已应用的物料流程实例。^[servicev2/impl/MaterialFlowQueryServiceImpl.java]

### sendApplyMsgHandler(param)
发送应用消息处理。根据 TaskDispatchDetailParam 触发消息通知（如物料流程变更通知给相关角色）。^[servicev2/impl/MaterialFlowQueryServiceImpl.java]

## Entry Points

### REST Controllers
- MaterialFlowQueryController → `queryMaterialConfigData()`, `queryMaterialApplyData()`
  → `code-path: edar-starlord-web/.../web/materialflow/MaterialFlowQueryController.java`
- MaterialFlowSelectQueryController → `queryMaterialApplyData()` — 下拉选择查询
  → `code-path: edar-starlord-web/.../web/materialflow/MaterialFlowSelectQueryController.java`

### 被其他 Service 调用
- 多个 V2 Service（MaterialTaskBizV2Service, MaterialConfigV2Service 等）内部调用本服务获取配置数据

## Dependencies
- **DAO**: NMaterialDefineDao, NMaterialNodeDao, NMaterialTemplateDao, TaskDispatchDao
- **Manager**: ScmProductManager (获取物料产品信息)
- **Service**: NMaterialProcessDefineService（获取流程定义）

## 相关页面
- [[material-flow-rule]] — 物料流程规则管理
- [[material-flow-category]] — 物料流程分类
- [[material-template-v2]] — 物料模板V2
