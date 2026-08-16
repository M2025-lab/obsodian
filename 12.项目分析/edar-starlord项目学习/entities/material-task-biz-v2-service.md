---
title: MaterialTaskBizV2Service - 主材业务查询服务V2
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [material-task, delivery, service, foreman, butler, designer]
sources:
  - edar-starlord-service/.../servicev2/MaterialTaskBizV2Service.java
  - edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java
confidence: high
---

# MaterialTaskBizV2Service

## 概述

`MaterialTaskBizV2Service` 是面向业务侧（工长、管家、设计师等角色）的主材任务查询服务。它聚合了多种维度的任务查询能力，服务于移动端首页卡片、任务列表、零售任务详情等前端场景。该服务的主要特点是将多个底层 DAO 查询与外部系统（项目订单、作业中心、客户中心、Ceres 组织架构等）的数据聚合为前端友好的 VO 对象。

^[edar-starlord-service/.../servicev2/MaterialTaskBizV2Service.java]

## 关键方法

### 1. 首页待处理任务卡片

以下五个方法按不同维度查询近 3 天待处理任务，返回 `List<ProjectTaskCardInfo>`：

| 方法 | 查询维度 | 节点过滤 |
|------|---------|---------|
| `recentAppointUncompleted` | 安装 / 一次性安装任务 | `NOTIFY_CAN_START` + `NOTIFY_START`（排除零售来源） |
| `recentAppointDeliveryUncompleted` | 送货任务 | `NOTIFY_CAN_START`（含批次配送任务） |
| `recentAppointMeasureUncompleted` | 测量 / 复尺任务 | `START`（60节点） |
| `recentAppointNoticeMeasureUncompleted` | 通知测量 | `NOTIFY_CAN_START` + `NOTIFY_START` |
| `recentAppointOrderUncompleted` | 下单任务 | `START`（60节点） |

每个方法的核心流程：
1. 按执行人（ucId）或项目 ID 列表查询
2. 过滤已关联施工包的任务
3. 按项目 ID 分组，取最早预计时间的任务组装卡片
4. 卡片内容格式：`「供应商-物料名-节点名」待处理`

^[edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java:148-272]

### 2. 物料派发列表 — `queryMaterialDispatchList(MaterialDispatchQueryReq)`

查询安装/送货/备货任务的派发时间线，按工种匹配。用于确定各节点的时间：
- `notifyCanStartDate`：备货任务 60 节点完成时间
- `notifyStartDate`：送货任务通知节点完成时间
- `startDate`：送货任务 60 节点完成时间
- `noticeRetainTime`：送货任务通知预约时间

按项目 ID 分组，取最早的时间返回。

^[edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java:280-354]

### 3. 复尺任务分页查询 — `recheckScaleTaskPage(MaterialTaskPageBizParam)`

这是最复杂的查询方法，服务于设计师/木作经理/橱柜经理的复尺任务列表。

**核心流程（多步骤数据聚合）**：
1. **下级员工查询**（`querySubOrganizationsAllUcId`）：管理者可查看下级员工的任务。支持定制门店负责人、经营部定制负责人、新零售组长/店长等角色
2. **任务查询**：分页查询未完成的复尺/报价变更任务
3. **项目订单批量查询**：获取地址、户型等信息
4. **零售/非零售分流**：判断订单是否为零售订单，分别处理 DF Code 获取
5. **客户委派编码查询**：非零售从 `customerHomeManager.getDFCodeByHomeOrderNos` 获取，零售从协调器 `retailManager.batchQueryRetail` 获取
6. **用户详情查询**：按委派编码查询维护人（木作设计师、定制业务员）
7. **DC Code 查询**：设计顾问编码
8. **业主电话脱敏**
9. **报价编辑权限**：并发检查变更报价编辑权限
10. **组装 VO**：填充地址、设计师、项目经理、客户经理、木作设计师、变更单等信息

^[edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java:357-550]

### 4. 零售任务详情 — `queryRetailMaterialTaskDetail(RetailMaterialTaskDetailBizParam)`

查询零售订单的主材任务详情。支持两种查询方式：
- 通过 `taskDispatchId` 直接查询
- 通过 `salesSubOrderNo`（销售子单号）查询

特殊处理：
- 如果通过销售子单号没查到任务，尝试通过项目 ID + 品类 + 供应商查询复尺任务（Apollo 白名单控制）
- 零售订单需调用协调器获取 DF Code
- 对复尺任务补充测量表单的驳回信息

^[edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java:803-983]

### 5. 首页任务计数

#### `querySaleHomeCount(SaleHomeCardReq)`
查询设计/复尺/报价变更等任务类型的未完成数量。报价变更任务额外统计 1.0 变更单的待处理数量。

#### `querySaleHomeCountV2(SaleHomeCardReq)`
V2 版本，专门查询近三天测量/复尺任务数量，过滤掉已竣工/取消/终止的项目。

^[edar-starlord-service/.../servicev2/impl/MaterialTaskBizV2ServiceImpl.java:991-1074]

## 入口点

| 入口类型 | 位置 | 说明 |
|---------|------|------|
| Controller | `MaterialTaskBizV2Controller` | 主材业务查询 REST 接口 |

^[edar-starlord-web/.../MaterialTaskBizV2Controller.java]

## 依赖

| 依赖 | 用途 |
|------|------|
| `TaskDispatchNodeDao` | 任务节点查询 |
| `TaskDispatchDao` | 任务主表查询 |
| `TaskDispatchV2Service` | 任务详情查询 |
| `TaskDispatchService`（V1） | 任务列表查询 |
| `MaterialBatchV2Service` | 批次配送任务查询 |
| `MaterialHandleV2Service` | 订单信息生成 |
| `ProjectOrderManager` | 项目订单详情查询 |
| `RetailManager` | 零售订单协调器查询 |
| `CustomerHomeManager` | 客户中心 DF Code / 维护人查询 |
| `WorkCenterManager` | 作业中心业主电话查询 |
| `CeresManagerImpl` | 组织架构查询 |
| `AtomManager` | 报价编辑权限查询 |
| `CipherManager` | 数据脱敏 |
| `NeedChangeOrderManager` | 变更单统计 |
| `ProjectChangeManager` | 变更单详情 |
| `HomeUnifyQueryManager` | 房屋统一查询 |
| `SaleHomeCardConfig` | 首页卡片按钮配置 |
| `ApolloConfig` | Apollo 配置（白名单等） |
| `FrontCategoryTransToLocalUtil` | 前端类目编码转换 |
| `MeasureFormTemplateDao` | 测量表单模板查询 |

## 参见

- [[task-dispatch-v2-service]] — 任务派发 V2 核心服务
- [[material-schedule-service]] — 主材排程/日历服务
- [[material-handle-v2-service]] — 任务处理统一入口
