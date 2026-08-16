---
title: DeliveryMaterialBizService（配送主材业务服务）
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, v2, 主材, 施工包, 事件处理]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/DeliveryMaterialBizService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/impl/DeliveryMaterialBizServiceImpl.java
confidence: high
---

# DeliveryMaterialBizService（配送主材业务服务）

## 概述

配送主材业务核心服务（V2 版本），负责**根据供应链（SCM）事件消息驱动主材任务的创建与取消**。是连接供应链系统（OMS）和主材任务系统的桥梁。

主要处理两类 SCM 事件：
1. **子订单变更事件**（`oms#order-changed`）：根据货单（子订单）的生成/取消状态，创建或取消主材送货任务、生成施工包、双写 OMS 服务单
2. **超级订单变更事件**（`oms#super-order-changed`）：根据超级订单（父单）的生成/审核通过事件，为安装类 SKU 按品类+供应商维度生成施工包

实现类 `DeliveryMaterialBizServiceImpl` 位于 `edar-starlord-service/servicev2/impl` 包下。

## 关键方法

### createByDelivery(ScmOrderMsgBO payloadData)
根据货单（子订单）生成主材任务。完整流程：
1. **消息校验**：仅处理事件为 `GENERATE` 且履约环节为 `DELIVERY`（送货）的消息
2. **查询货单详情**（`ScmManager.querySubOrderBaseInfo`）和货单 SKU 明细（`querySubOrderItems`）
3. **过滤逻辑**：
   - 仓库单（`supplierId` 长度 > 10）不处理
   - 北京地区且 Apollo 开关关闭时不处理
   - 方太电器特定品类过滤（`filterFtDqTask`）
4. **构建上下文**（`ProcessCreateV2Context`）：包含项目订单信息、货单信息、排期信息、业务关联 ID 等
5. **模式判断**：
   - 北京：`HOME2_5` 模式
   - 非北京：`HOME2_5_MANPOWER` 模式
   - 若项目排程开关开启：`DELIVERY_FLOW` 模式
6. **创建主材任务**（`MaterialCreateV2Service.createMaterialTask`）
7. **生成施工包**（`installTaskCreate` 私有方法）：根据任务类型（安装/一次安装/售后）调用施工包管理器创建对应类型的施工包
8. **更新施工包编码**到 `taskDispatchNode` 表
9. **自动补齐状态**：若施工包状态已超过"处理中"，则调用 `taskDispatchCompleteService.completePackageTask` 补齐
10. **双写 OMS 服务单**（`materialCreateV2Service.createServiceOrder`）

### cancelByDelivery(ScmOrderMsgBO payloadData)
根据货单取消状态取消主材任务：
1. **状态校验**：仅处理 `SubOrderStateEnum.CANCELED` 状态的消息
2. **取消施工包**（`PackageConstructionManager.cancelPackage`）
3. **查询关联主材任务**：按 `orderNo`（货单号）查询未完成的有效 `TaskDispatch`
4. **批量取消**任务（`TaskDispatchCancelService.batchCancelTaskDispatch`），附带操作人信息和备注

### installTaskCreateBySupperOrder(SuperOrderAllDto superOrderAllDto, TaskTypeEnum taskTypeEnum)
根据超级订单（零售父单）生成安装施工包：
1. **消息校验**：仅处理 `GENERATE` 或 `REVIEW_PASS` 事件
2. **获取履约品类**（`ScmProductManager.getPerformance`）和配送源信息（`queryDefaultDeliverySourceList`）
3. **按品类+供应商分组** SKU：同一品类+同一供应商的 SKU 合并到一个施工包
4. **生成施工包**（`PackageConstructionManager.createPackage`）
5. 注意：注释显示线下履约的纯零售场景暂不双写 OMS 服务单

## 入口点

### 领域事件处理器（Kafka/消息队列）

| 事件处理器 | 事件类型 | 触发方法 |
|------------|----------|----------|
| `ScmOrderEventHandler.handleBizNew()` | `oms#order-changed`（子订单变更） | → `deliveryMaterialBizService.createByDelivery(payloadData)` |
| `ScmOrderEventHandler.handleBizNew()` | `oms#order-changed`（子订单变更） | → `deliveryMaterialBizService.cancelByDelivery(payloadData)` |
| `ScmSuperOrderEventHandler.handleBiz()` | `oms#super-order-changed`（超级订单变更） | → `deliveryMaterialBizService.installTaskCreateBySupperOrder(superOrderAllDto, TaskTypeEnum.INSTALL)` |

### REST 控制器

| 控制器 | 方法 | 端点 | 调用链 |
|--------|------|------|--------|
| `MaterialMeasureTaskController.createByDelivery()` | `POST /manpower/task/createByDelivery` | 手动触发，构建 `ScmOrderMsgBO` 后 → `materialCreateBizService.createByDelivery(msgBO)` |

## 依赖

| 依赖 | 类型 | 用途 |
|------|------|------|
| `ScmManager` | Manager | 查询子订单基础信息、子订单 SKU 明细、构建 SKU 内控类目映射 |
| `MaterialCreateV2Service` | V2 Service | 创建主材任务（`createMaterialTask`）、双写 OMS 服务单（`createServiceOrder`）、方太电器过滤（`filterFtDqTask`） |
| `ProjectOrderManager` | Manager | 获取项目订单详情 |
| `BusinessService` | V2 Service | 查询项目施工排期 |
| `ConfigQueryService` | V2 Service | 判断项目是否开启排程配置（`materialScheduleSwitch`） |
| `TaskDispatchDao` | DAO | 按货单号查询关联的主材任务（用于取消场景） |
| `TaskDispatchNodeDao` | DAO | 更新节点施工包编码（`updatePackageCodeByTaskDispatchId`） |
| `PackageConstructionManager` | Manager | 创建施工包、创建售后施工包、创建预埋件施工包、取消施工包 |
| `TaskDispatchCompleteService` | V2 Service | 自动补齐施工包完成状态 |
| `TaskDispatchCancelService` | V2 Service | 批量取消主材任务 |
| `ApolloConfig` | 配置 | 读取北京服务单开关等配置 |
| `ProjectOrderUtil` | 工具 | 判断是否北京地区、获取办事处编码/名称 |
| `ScmProductManager` | Manager | 获取履约品类、查询配送源信息（纯零售场景） |

## 状态机

不直接管理状态机，但通过调用下游服务间接参与状态变更：
- **主材任务创建**：调用 `MaterialCreateV2Service.createMaterialTask` 创建 `TaskDispatch` + `TaskDispatchNode`，状态由 V2 流程管理
- **施工包状态补齐**：通过 `TaskDispatchCompleteService.completePackageTask` 将施工包从"已创建"状态补齐到目标状态
- **主材任务取消**：通过 `TaskDispatchCancelService.batchCancelTaskDispatch` 将任务状态变更至取消态

## 业务流程

### 创建流程
```
SCM 货单生成事件 (oms#order-changed)
       │
       ▼
ScmOrderEventHandler.handleBizNew()
       │
       ▼
DeliveryMaterialBizService.createByDelivery()
       │
       ├──▶ ScmManager.querySubOrderBaseInfo() ──▶ 货单详情
       ├──▶ ScmManager.querySubOrderItems() ──▶ SKU 明细
       ├──▶ ProjectOrderManager.getProjectOrder() ──▶ 项目订单
       ├──▶ BusinessService.queryProjectSchedule() ──▶ 施工排期
       │
       ├─ 过滤检查（仓库单、北京开关、方太电器等）
       │
       ├──▶ MaterialCreateV2Service.createMaterialTask() ──▶ 创建主材任务 TaskDispatch
       │
       ├──▶ installTaskCreate() ──▶ PackageConstructionManager ──▶ 施工包
       │
       ├──▶ taskDispatchNodeDao.updatePackageCodeByTaskDispatchId() ──▶ 关联施工包
       ├──▶ taskDispatchCompleteService.completePackageTask() ──▶ 自动补齐状态
       └──▶ materialCreateV2Service.createServiceOrder() ──▶ 双写 OMS
```

### 取消流程
```
SCM 货单取消事件 (oms#order-changed, status=CANCELED)
       │
       ▼
DeliveryMaterialBizService.cancelByDelivery()
       │
       ├──▶ PackageConstructionManager.cancelPackage() ──▶ 取消施工包
       ├──▶ TaskDispatchDao.listByExample() ──▶ 查询关联任务
       └──▶ TaskDispatchCancelService.batchCancelTaskDispatch() ──▶ 取消任务
```
