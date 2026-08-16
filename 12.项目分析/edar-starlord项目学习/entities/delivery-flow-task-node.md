---
title: DeliveryFlowTaskNodeService - 任务节点配置查询服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, config, node, template]
sources: [edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/DeliveryFlowTaskNodeService.java, edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/impl/DeliveryFlowTaskNodeServiceImpl.java]
confidence: high
---

## 概述

任务节点配置查询服务（DeliveryFlowTaskNodeService）为前端品类流程编辑页面提供**查询型**支撑服务，返回节点模式、角色类型、工种列表和时间条件选项。它是纯粹的读服务，不涉及数据写入操作，主要服务于品类作业流程配置的前端渲染和数据选择。

与其他交付流程服务的关系：此服务被 `CategoryProcessController` 直接调用，返回值用于前端构建品类流程编辑界面的下拉选项和选择器数据。^[DeliveryFlowTaskNodeServiceImpl.java]

## 核心方法

### queryModeType（查询节点模式/聚合节点配置）
`POST /delivery/flow/task/node/mode/query`

根据任务类型（taskType）和虚拟节点（virtualNode）查询聚合节点配置。聚合节点配置来自应用配置 `new.material.config.apply.aggregation.node.config`，定义了特定任务类型下哪些节点支持聚合模式以及每种模式包含哪些子节点。

还会为每个模式的每个节点补充 unifiedNodeCode（通过 `UnifiedNodeCodeType` 枚举映射）。^[DeliveryFlowTaskNodeServiceImpl.java:L74-L98]

### queryRoleTypeList（查询角色类型列表）
`GET /delivery/flow/task/role/query`

根据任务类型、节点类型、配送方式、销售类型等参数查询可选的执行角色。逻辑分支：
- **安装任务 + 开始节点**：有工种编码时按工种映射角色，无则默认定制家具安装工
- **备货任务**：固定返回供应商角色
- **送货任务 + 开始节点**：供应商配送 → 供应商角色；承运商配送 → 承运商角色
- **其他**：从配置 `delivery-flow.role` 的 JSON Map 中按 `销售类型-任务类型-节点类型` 查找
  - 找不到时：一次性安装的通知可开始节点 → 从 Ceres 系统获取角色列表（含管家、工长）
  - 找不到时（其他）：使用 `rescale_role_type_list` 配置的默认角色列表 ^[DeliveryFlowTaskNodeServiceImpl.java:L101-L154]

### queryWorkTypeList（查询作业工种列表）
`GET /delivery/flow/task/work-type/query`

查询可用工种列表，数据来源：
1. 硬编码补充：供应商角色 + 项目经理（工长）角色
2. Ceres 系统（人力系统）→ `ceresManager.listPositions("worker")` 获取工人岗位列表
3. 按 rangeType（内部/外部）区分，内部工种补充 positionModes（人事模式）^[DeliveryFlowTaskNodeServiceImpl.java:L192-L240]

### timeConditionLevelOneSelections（时间条件一级选项列表）
`POST /delivery/flow/task/time/condition/level1`

构建时间条件级联选择的一级选项。逻辑：
1. 最早通知时间类型 → 添加"供应商最早通知时间"和"当前时间"两个系统级选项
2. 查询该 ruleId 下所有有效的品类规则
3. 按物料编码分组，构建一级选项（品类维度）
4. 每个品类下按 categoryId 分组供应商 → 二级选项
5. 二级选项查询：根据 categoryId 查询 ProcessDefine → 获取所有 nodeCfg → 按 processCode 构建任务节点选项
   - 通知类节点（NOTIFY_CAN_START、NOTIFY_START）额外生成"可上门时间"选项 ^[DeliveryFlowTaskNodeServiceImpl.java:L243-L314]

### queryLevelTwoSelectionsByCategoryId
内部方法，根据品类ID和时间类型查询二级选项。遍历品类的所有 ProcessDefine 和 NodeCfg，排除 NODE_START 节点，按 TaskType/Nodetype 排序。^[DeliveryFlowTaskNodeServiceImpl.java:L316-L372]

## 入口点

### Controller（HTTP REST）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/delivery/flow/task/node/mode/query` | POST | [[delivery-flow-task-node#queryModeType\|queryModeType]] |
| `/delivery/flow/task/role/query` | GET | [[delivery-flow-task-node#queryRoleTypeList\|queryRoleTypeList]] |
| `/delivery/flow/task/work-type/query` | GET | [[delivery-flow-task-node#queryWorkTypeList\|queryWorkTypeList]] |
| `/delivery/flow/task/time/condition/level1` | POST | [[delivery-flow-task-node#timeConditionLevelOneSelections\|timeConditionLevelOneSelections]] |

Controller: `DeliveryFlowTaskController`^[edar-starlord-web/.../DeliveryFlowTaskController.java]

### Feign / MQ / Schedule
未发现 Feign 接口、MQ 监听器或定时任务直接调用此服务。该服务仅为纯 HTTP REST 查询服务。

## 依赖

- **DAO 层**：`DeliveryFlowRuleCategoryDao`（品类规则查询）、`NMaterialProcessDefineDao`（流程定义查询）、`NMaterialNodeDao`（节点配置查询）
- **Manager 层**：`CeresManager`（人力系统岗位查询）
- **配置**：
  - `new.material.config.apply.aggregation.node.config` — 聚合节点配置（JSON 格式）
  - `delivery-flow.role` — 角色类型映射（JSON Map 格式，key = "销售类型-任务类型-节点类型"）
  - `rescale_role_type_list` — 默认角色类型列表

## 与其他服务的关系

本服务是品类流程编辑页面的数据源之一，提供前端下拉选项：
- 被 `DeliveryFlowTaskController` 直接调用
- 提供的数据用于 [[category-process|CategoryProcessService]] 的 `saveMaterialProcessDefineNew` 方法中节点配置的填充
- `timeConditionLevelOneSelections` 的返回值用于节点时间配置的级联选择器
