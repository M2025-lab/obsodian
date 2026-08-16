---
title: DeliveryFlowCategoryService - 品类规则服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, rule, config]
sources: [edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/DeliveryFlowCategoryService.java, edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/impl/DeliveryFlowCategoryServiceImpl.java]
confidence: high
---

## 概述

品类规则服务（DeliveryFlowCategoryService）是交付流程配置管理的核心服务，负责管理货物配送流程中的**品类规则（Category）**的完整生命周期。品类规则定义了某个品类物料在特定供应商下应遵循的节点流程模板，是规则 → 品类 → 作业流程三层配置体系的中间层。

品类规则核心概念：
- **品类规则**：物料品类 + 供应商 + 节点流程模板的组合，一份规则下可以有多条品类规则
- **品类状态**：编辑态（EDIT） → 生效未引用（INIT_ACTIVE） → 排程引用（DELIVERY_ACTIVE） → 失效（INVALID）/ 删除（DELETE）
- **品类编码（categoryCode）**：同一业务品类共享同一个 categoryCode，但可以存在多个 categoryId（版本）^[DeliveryFlowCategoryServiceImpl.java]

## 核心方法

### 查询类

#### queryCategoryListByCondition
`GET /delivery/flow/category/query/list/bycondition`

按条件分页查询品类规则列表，支持按 ruleId、物料编码、供应商、状态等过滤。结果按状态排序（排程引用 → 生效 → 编辑 → 失效），同状态按创建时间倒序，并使用内存分页。^[DeliveryFlowCategoryServiceImpl.java:L98-L122]

#### queryCategoryInfo
`GET /delivery/flow/category/query/info`

查询品类规则详情，返回品类基本信息、供应商列表、任务类型列表，并从关联的 `DeliveryFlowRule` 中补充：分公司、套餐、销售类型、单据类型等信息。还判断是否允许排程引用（仅特定 documentType 支持）和是否为北京地区。^[DeliveryFlowCategoryServiceImpl.java:L412-L450]

#### queryApplyCategory
`POST /delivery/flow/category/query/apply/material`

查询可供套用的品类规则列表。根据当前规则的匹配条件（分公司、物料编码、销售类型、单据类型），在有效状态的品类规则中搜索可套用的品类。^[DeliveryFlowCategoryServiceImpl.java:L453-L479]

#### buildCategoryPageInfo
将 `DeliveryFlowRuleCategory` 列表按 categoryId 分组构建页面信息，聚合同一品类下的多个供应商。^[DeliveryFlowCategoryServiceImpl.java:L134-L162]

#### buildTaskTypeList
根据节点流程模板ID获取该模板下支持的任务类型列表。^[DeliveryFlowCategoryServiceImpl.java:L598-L607]

#### queryDeliveryCategoryCode
`GET /delivery/rule/delivery/category/query/code`（Feign 接口）

根据品类ID列表批量查询对应的 categoryCode 映射。供外部系统通过 Feign 调用。^[DeliveryFlowCategoryServiceImpl.java:L1000-L1009]

### 生命周期类

#### categorySave
`POST /delivery/flow/category/save`

新建品类规则。使用雪花ID生成器生成 categoryId，初始状态为 **编辑态（EDIT）**。支持一个品类下多个供应商。^[DeliveryFlowCategoryServiceImpl.java:L336-L367]

#### categorySaveCheck
保存前校验，检查规则是否存在、是否存在相同物料+供应商组合的重复品类。返回 errorMsg 或空 Map 表示通过。^[DeliveryFlowCategoryServiceImpl.java:L310-L332]

#### categoryEffective（品类生效）
`GET /delivery/flow/category/effective`

品类规则从编辑态激活为**生效未引用态（INIT_ACTIVE）**，同时将关联的任务配置状态同步更新。生效后发送群消息通知。^[DeliveryFlowCategoryServiceImpl.java:L263-L306]

#### categoryEffectiveCheck
`GET /delivery/flow/category/effective/check`

生效前校验：规则必须处于编辑态、无重复品类、关联的任务配置必须存在。^[DeliveryFlowCategoryServiceImpl.java:L370-L408]

#### categoryExpire（品类失效）
`GET /delivery/flow/category/expire`

品类规则失效。失效状态根据当前品类编码下其他版本是否存在生效/引用状态来决定：
- 如果仅有当前一条 → **失效（INVALID）**
- 如果还有别的生效/引用版本 → **删除（DELETE）**

同时失效关联的任务配置和连线信息。如果品类被全流程引用，发送群消息提醒。^[DeliveryFlowCategoryServiceImpl.java:L187-L242]

#### categoryExpireCheck
`GET /delivery/flow/category/expire/check`

失效前校验：规则不能为空、不能已失效。排程引用状态不可直接失效。^[DeliveryFlowCategoryServiceImpl.java:L165-L183]

#### categoryDelete
`GET /delivery/flow/category/delete`

物理删除品类规则（仅编辑态可删除），同时删除关联的任务配置。^[DeliveryFlowCategoryServiceImpl.java:L578-L595]

### 编辑/套用类

#### categoryUpdate
`POST /delivery/flow/category/update`

编辑品类规则——核心逻辑为"旧版本失效 + 新版本生效"：
1. 如果原来存在生效未引用态的配置，将其置为删除态
2. 创建新的品类规则（新 categoryId，但 categoryCode 不变），状态为生效未引用
3. 根据旧 categoryId 和新 categoryId，将任务配置也进行"生效化"复制
4. 如果被全流程引用，发送群消息提醒^[DeliveryFlowCategoryServiceImpl.java:L806-L891]

#### categoryUpdateCheck
`POST /delivery/flow/category/update/check`

编辑前校验：检查品类规则是否存在、是否与被使用的配置重复、任务配置内容是否正确。^[DeliveryFlowCategoryServiceImpl.java:L610-L650]

#### categorySupplierUpdate
`POST /delivery/flow/category/supplier/update`

更新品类关联的供应商和节点流程。先删后建，如果节点流程发生变化，会物理删除已减少的任务类型对应的配置数据（含 define、edge、route、node、task、time 等表）。^[DeliveryFlowCategoryServiceImpl.java:L654-L720]

#### applyCategoryCopy（套用）
`POST /delivery/flow/category/query/apply/copy`

从已有品类规则套用创建新品类，生成新 categoryId，复制物料、节点流程等信息，并复制任务配置。^[DeliveryFlowCategoryServiceImpl.java:L524-L567]

#### applyCategoryCheck
`POST /delivery/flow/category/query/apply/judge`

套用前校验：检查规则、源品类是否存在，目标规则下是否有重复品类，任务配置是否正确。^[DeliveryFlowCategoryServiceImpl.java:L482-L520]

#### categoryCopy（复制）
`POST /delivery/flow/category/copy`

品类规则复制，与套用逻辑相同。^[DeliveryFlowCategoryServiceImpl.java:L571-L574]

### 排程同步

#### updateCategoryStateByDeliveryConfigId
由排程系统调用，根据排程模板参数更新品类规则状态：
1. 将已存在的排程引用状态品类置为删除
2. 将当前被排程引用的品类置为排程引用状态（DELIVERY_ACTIVE）
3. 批量更新关联的任务配置状态^[DeliveryFlowCategoryServiceImpl.java:L901-L997]

## 状态机

```
                    ┌──────────┐
                    │   EDIT   │  (编辑/草稿态)
                    └────┬─────┘
                         │ categoryEffective()
                    ┌────▼─────────┐
                    │ INIT_ACTIVE  │  (生效未引用态)
                    └────┬─────────┘
                         │ categoryUpdate()  → 创建新版本，旧版变 DELETE
                    ┌────▼──────────┐         或被排程同步引用
                    │DELIVERY_ACTIVE│ (排程引用态)
                    └────┬──────────┘
                         │ categoryExpire()  →  检查是否有同 code 的其他版本
              ┌──────────┴──────────┐
              ▼                     ▼
        ┌─────────┐           ┌─────────┐
        │ INVALID │           │ DELETE  │ (软删除/不可见)
        └─────────┘           └─────────┘

        编辑态 → categoryDelete() → 物理删除
```

^[DeliveryFlowCategoryServiceImpl.java + DeliveryStateEnum]

## 入口点

### Controller（HTTP REST）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/delivery/flow/category/query/list/bycondition` | GET | [[delivery-flow-category#queryCategoryListByCondition\|queryCategoryListByCondition]] |
| `/delivery/flow/category/query/info` | GET | [[delivery-flow-category#queryCategoryInfo\|queryCategoryInfo]] |
| `/delivery/flow/category/delete` | GET | [[delivery-flow-category#categoryDelete\|categoryDelete]] |
| `/delivery/flow/category/expire/check` | GET | [[delivery-flow-category#categoryExpireCheck\|categoryExpireCheck]] |
| `/delivery/flow/category/expire` | GET | [[delivery-flow-category#categoryExpire\|categoryExpire]] |
| `/delivery/flow/category/query/apply/material` | POST | [[delivery-flow-category#queryApplyCategory\|queryApplyCategory]] |
| `/delivery/flow/category/query/apply/judge` | POST | [[delivery-flow-category#applyCategoryCheck\|applyCategoryCheck]] |
| `/delivery/flow/category/query/apply/copy` | POST | [[delivery-flow-category#applyCategoryCopy\|applyCategoryCopy]] |
| `/delivery/flow/category/copy` | POST | [[delivery-flow-category#categoryCopy\|categoryCopy]] |
| `/delivery/flow/category/save` | POST | [[delivery-flow-category#categorySave\|categorySave]] |
| `/delivery/flow/category/effective/check` | GET | [[delivery-flow-category#categoryEffectiveCheck\|categoryEffectiveCheck]] |
| `/delivery/flow/category/effective` | GET | [[delivery-flow-category#categoryEffective\|categoryEffective]] |
| `/delivery/flow/category/supplier/update` | POST | [[delivery-flow-category#categorySupplierUpdate\|categorySupplierUpdate]] |
| `/delivery/flow/category/update` | POST | [[delivery-flow-category#categoryUpdate\|categoryUpdate]] |
| `/delivery/flow/category/update/check` | POST | [[delivery-flow-category#categoryUpdateCheck\|categoryUpdateCheck]] |

Controller: `DeliveryFlowCategoryController`^[edar-starlord-web/.../DeliveryFlowCategoryController.java]

### Feign（远程调用）
- `DeliveryFlowRuleFeign#queryDeliveryCategoryCode` → `queryDeliveryCategoryCode` ^[DeliveryFlowRuleFeign.java]

### 内部调用
- `DeliveryFlowRuleServiceImpl` → `buildCategoryPageInfo`（组装规则详情时填充品类列表）^[DeliveryFlowRuleServiceImpl.java]
- `CategoryProcessController#queryCategoryInfo`（Feign接口实现）→ `queryCategoryInfo`^[CategoryProcessController.java:L173-L176]
- `CategoryProcessServiceImpl` → `updateCategoryStateByDeliveryConfigId`（排程同步时更新品类引用状态）^[CategoryProcessServiceImpl.java:L2051]

## 依赖

- **DAO 层**：`DeliveryFlowRuleCategoryDao`（品类规则数据访问）、`DeliveryFlowRuleDao`（规则数据访问）、`MaterialRouteTimeDao`、`NMaterialRouteDao`、`NMaterialEdgeDao`、`NMaterialNodeDao`、`NMaterialTaskCfgDao`、`NMaterialProcessDefineDao`、`NMaterialTimeCalculateRuleCfgDao`、`NMaterialNodeTransferConditionDao`、`NMaterialProcessTemplateDao`
- **服务层**：[[delivery-flow-rule|DeliveryFlowRuleService]]（查询规则信息）、[[category-process|CategoryProcessService]]（品类流程配置的生效/失效/复制/删除）、`DeliveryFlowSelectQueryService`（流程模板查询）
- **Manager 层**：`DeliveryProcessCfgManager`（全流程配置查询，判断品类引用关系）
- **工具**：`MsgSendUtil`（群消息通知）、`TimestampIdGenerator`（分布式ID生成）
- **配置**：`delivery-flow.allow-delivery-process.document-types`（允许排程引用的单据类型）、`delivery_flow_check_switch`（任务配置校验开关）、`mdm-code.beijing`（北京分公司编码列表）
