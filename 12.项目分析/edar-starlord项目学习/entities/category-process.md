---
title: CategoryProcessService - 品类流程作业配置服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, process, node, workflow, state-machine]
sources: [edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/CategoryProcessService.java, edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/impl/CategoryProcessServiceImpl.java]
confidence: high
---

## 概述

品类流程作业配置服务（CategoryProcessService）管理每个品类规则下的**作业流程定义（ProcessDefine）**，包括节点配置、连线关系、时间约束、激活条件、通知推送等完整工序信息。它是交付流程三层配置（规则 → 品类 → 作业流程）的最底层，直接定义"某品类物料在某个配送方式下要经过哪些节点、每个节点由谁执行、耗时多久、有什么前置条件"。

核心数据结构：
- **ProcessDefine**：一个品类 + 一个任务类型 + 一个配送方式的作业流程定义
- **Node**：流程中的节点（如开始、通知可开始、作业节点、验收节点等），每个节点下挂 Task 配置
- **Route**：节点间连线，三种类型：NODE（任务内连线）、RESTART_ROUTE（重启连线）、PROCESS_NODE（任务间连线）
- **Edge**：前端画布坐标数据
- **Time/TimeRelation**：节点时间配置及与上下游的时间关系
- **ActivateCondition**（NMaterialNodeTransferCondition）：节点激活条件
^[CategoryProcessServiceImpl.java]

## 核心方法

### 查询类

#### getProcessDefineInfoAndRouteInfoList
`GET /category/process/define/list`

查询品类下所有流程定义及完整连线信息（任务间连线）。返回包含节点列表和连线边信息的 CategoryProcessInfoDto。^[CategoryProcessServiceImpl.java:L131-L143]

#### processDefineInfoListByTaskType
`GET /category/process/define/list/tasktype`

按任务类型查询品类的流程定义信息，用于前端按任务 Tab 展示。^[CategoryProcessServiceImpl.java:L146-L157]

#### processEdgeInfoList
`GET /category/process/edge/list`

查询品类的完整连线信息，按配送方式分组返回。^[CategoryProcessServiceImpl.java:L160-L165]

#### getProcessDefineInfoByCategoryIds
内部查询方法，批量获取多个品类的流程定义。支持全流程模板ID过滤（用于排程激活条件区分）。^[CategoryProcessServiceImpl.java:L168-L176]

#### getCategoryProcessInfo
`POST /category/process/info`（Feign 接口）

获取品类的完整流程信息（包含节点列表 + 任务间连线）。核心查询链路：
1. 根据 deliveryProcessCfgId → 查 Rule → 根据单据类型/系统版本过滤 → 得到 RuleId
2. 根据 RuleId + 物料编码（可选）→ 查 Category
3. 根据 CategoryId → 查 ProcessDefine（含节点列表）
4. 根据 CategoryId → 查 Route（任务间连线 type=3）
5. 拼装返回 ^[CategoryProcessServiceImpl.java:L1221-L1246]

#### getCategoryProcessList
`POST /category/process/list`（Feign 接口）

仅获取品类列表信息（含供应商），不返回节点和连线详情。^[CategoryProcessServiceImpl.java:L1249-L1259]

### 保存/创建类

#### saveMaterialProcessDefineNew（保存作业流程）
`POST /category/process/define/save`

保存品类的作业流程定义。核心逻辑：
1. **校验**：检查节点是否重复
2. **删除旧数据**（如果 deleteOldData=true）：按 processCode 删除 define、node、task、time、timeRelation、edge、node_transfer_condition 等一系列相关表
3. **保存新数据**：区分草稿保存（有 id 的 processDefine）和新建保存（无 id）
   - 保存 ProcessDefine 主表
   - 构建 MaterialNodeForm（节点 + 任务 + 时间 + 激活条件 + 耗时规则）
   - 保存 Node、Task、Time/TimeRelation、Push、ActivateCondition、TimeCalculateRule
   - 保存 Route（节点间连线）和 Edge（坐标）
4. 备货/送货任务会删除所有未提交的配送类型数据 ^[CategoryProcessServiceImpl.java:L180-L208]

### 状态管理类

#### modifyCategoryProcessState（单个品类状态修改）
修改品类下所有流程配置的状态，事务性更新以下表：
- `n_material_process_define`（流程定义）
- `n_material_node_cfg`（节点）
- `n_material_task_cfg`（任务）
- `n_material_time_calculate_rule_cfg`（耗时规则）
^[CategoryProcessServiceImpl.java:L1418-L1467]

#### modifyCategoryProcessStateBatch（批量修改状态）
批量修改多个品类的流程配置状态，支持每批 200 条分批处理。比单个方法多了 time 和 timeRelation 两张表的状态更新。^[CategoryProcessServiceImpl.java:L1471-L1555]

#### modifyCategoryRouteInfoState（修改连线状态）
修改品类下所有 Route 和 RouteTime 的状态。^[CategoryProcessServiceImpl.java:L1559-L1584]

#### deleteCategoryProcess（删除）
物理删除品类下所有流程配置数据：define、edge、route、route_time、node_cfg、task_cfg、time_calculate_rule_cfg、node_transfer_condition。^[CategoryProcessServiceImpl.java:L1587-L1630]

#### effectiveCategoryProcess（生效品类流程）
品类规则编辑生效时的流程配置处理：
1. 旧的流程信息状态不直接修改（注释掉了）
2. 将入参的 CategoryProcessDefineSaveParam 列表逐个设置到 newCategoryId 上，清空 id/processCode，以 INIT_ACTIVE 状态重新保存
3. 同时保存完整连线信息 ^[CategoryProcessServiceImpl.java:L1191-L1218]

### 复制类

#### copyCategoryProcess（复制品类流程）
从源品类复制全部流程配置到新品类，包括：
1. ProcessDefine（流程定义，修改 processCode）
2. Route + RouteTime（连线和时间）
3. Edge（坐标）
4. Node → Task（节点和任务配置）
5. ActivateCondition（激活条件）
6. TimeCfg + TimeRelation（时间配置）
7. TimeCalculateRule + HitHouse/HitCraft（耗时计算规则及房屋/工艺配置）
^[CategoryProcessServiceImpl.java:L1640-L1680]

### 校验类

#### checkCategoryProcessByCategoryId
检查单个品类 ID 是否有关联的流程定义，返回 null 表示存在。^[CategoryProcessServiceImpl.java:L1683-L1693]

#### checkCategoryProcess
批量检查，确保所有 categoryId 都有对应的流程定义，返回缺失的 ID 列表。^[CategoryProcessServiceImpl.java:L1696-L1712]

### 排程同步类

#### deliveryProcessSync
`POST /delivery/process/sync`（Feign 接口）

接收排程系统的配置同步：
1. 更新品类的 deliveryTemplateId（标记被哪个排程模板引用）
2. 调用 `DeliveryFlowCategoryService#updateCategoryStateByDeliveryConfigId` 更新品类状态
3. 删除旧的排程激活条件（按 templateId + nodeId 精确删除）
4. 保存新的排程激活条件（NMaterialNodeTransferCondition），与品类自身的激活条件独立存储（通过 deliveryProcessTemplateId 区分）^[CategoryProcessServiceImpl.java:L2034-L2138]

## 状态机

品类流程配置的状态变化与品类规则同步：

```
品类规则状态 → 品类流程配置状态
EDIT        →  EDIT（草稿）
INIT_ACTIVE →  INIT_ACTIVE（生效）
DELIVERY_ACTIVE →  DELIVERY_ACTIVE（排程引用）
INVALID     →  INVALID（失效）
DELETE      →  DELETE（删除）
```

状态变更通过 `modifyCategoryProcessState[Batch]` 方法事务性更新 define/node/task/time 等所有关联表。

## 入口点

### Controller（HTTP REST）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/category/process/define/list` | GET | [[category-process#getProcessDefineInfoAndRouteInfoList\|getProcessDefineInfoAndRouteInfoList]] |
| `/category/process/define/list/tasktype` | GET | [[category-process#processDefineInfoListByTaskType\|processDefineInfoListByTaskType]] |
| `/category/process/edge/list` | GET | [[category-process#processEdgeInfoList\|processEdgeInfoList]] |
| `/category/process/define/save` | POST | [[category-process#saveMaterialProcessDefineNew\|saveMaterialProcessDefineNew]] |
| `/category/process/route/save` | POST | 品类连线保存（通过 CategoryProcessRouteService）|
| `/category/process/default/route/info` | POST | 默认连线查询（通过 CategoryProcessRouteService）|
| `/category/process/edge/check` | POST | 连线校验（通过 CategoryProcessRouteService）|

Controller: `CategoryProcessController`^[edar-starlord-web/.../CategoryProcessController.java]

### Feign（远程调用，通过 CategoryProcessFeign）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/category/process/info` | POST | [[category-process#getCategoryProcessInfo\|getCategoryProcessInfo]] |
| `/category/process/list` | POST | [[category-process#getCategoryProcessList\|getCategoryProcessList]] |
| `/main/process/sync` | POST | 主流程同步 → `DeliveryFlowRuleService#mainProcessSync` |
| `/delivery/process/sync` | POST | [[category-process#deliveryProcessSync\|deliveryProcessSync]] |

Feign 接口: `CategoryProcessFeign`^[edar-starlord-api/.../CategoryProcessFeign.java]

### 内部调用
- `DeliveryFlowCategoryServiceImpl` → 品类生效/失效/编辑/复制时调用 CategoryProcessService 的各种方法：
  - `categoryEffective()` → `modifyCategoryProcessState(categoryId, INIT_ACTIVE)`
  - `categoryExpire()` → `modifyCategoryProcessState(categoryId, expireState)`
  - `categoryExpire()` → `modifyCategoryRouteInfoState(categoryId, INVALID)`
  - `categorySave()`（套用/复制）→ `copyCategoryProcess(srcId, newId)`
  - `categoryDelete()` → `deleteCategoryProcess(categoryId)`
  - `categoryUpdate()` → `effectiveCategoryProcess(oldId, newId, param)`
  - `categoryUpdate()` / `categoryEffectiveCheck()` → `checkCategoryProcess[ByCategoryId]`
  - `categorySupplierUpdate()` → `deleteProcessDetailByTask`（直接操作 DAO）
- `DeliveryFlowRuleServiceImpl#ruleExpire()` → `modifyCategoryProcessState(categoryId, INVALID)` ^[DeliveryFlowRuleServiceImpl.java:L347]
- `RefreshDataController` → 可能调用（待确认具体场景）^[RefreshDataController.java]

## 依赖

- **DAO 层**（核心配置表）：`NMaterialProcessDefineDao`、`NMaterialNodeDao`、`NMaterialTaskCfgDao`、`NMaterialRouteDao`、`NMaterialEdgeDao`、`MaterialRouteTimeDao`、`NMaterialTimeDao`、`NMaterialTimeRelationDao`、`NMaterialPushDao`、`NMaterialTimeCalculateRuleCfgDao`、`NMaterialNodeTransferConditionDao`、`NMaterialPeriodCalculateHitCraftCfgDao`、`NMaterialPeriodCalculateHitHouseCfgDao`、`NMaterialTaskProcessTemplateDao`
- **DAO 层**（规则/品类关联表）：`DeliveryFlowRuleDao`、`DeliveryFlowRuleUnitDao`、`DeliveryFlowRuleCategoryDao`
- **服务层**：`MaterialConfigV2Service`（流程定义查询）、`CategoryProcessRouteService`（连线管理服务）、[[delivery-flow-category|DeliveryFlowCategoryService]]（品类状态更新）
- **转换层**：`DeliveryFlowConvert`（DTO 转换）、`CodeJointConvert`（编码拼接）
- **工具类**：`LangUtils`（processCode 生成）、`SPrecondition`（断言校验）
