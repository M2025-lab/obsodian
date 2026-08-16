---
title: DeliveryFlowRuleService - 配送流程规则服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, rule, config]
sources: [edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/DeliveryFlowRuleService.java, edar-starlord-service/src/main/java/com/ke/utopia/servicev2/deliveryflow/impl/DeliveryFlowRuleServiceImpl.java]
confidence: high
---

## 概述

配送流程规则服务（DeliveryFlowRuleService）管理交付流程三层配置体系的最顶层——**规则（Rule）**。Rule 定义了"在什么分公司、什么套餐、什么销售类型、什么单据类型下，应该使用哪套品类配置"。一份 Rule 关联多个 RuleUnit（规则明细组合）和 Category（品类规则），最终通过 deliveryProcessCfgId 关联到全流程配置（排程）。

核心概念：
- **Rule**：顶层匹配规则，关联一个全流程配置（deliveryProcessCfgId）
- **RuleUnit**：规则的笛卡尔积明细（分公司 × 套餐 × 单据类型 × 销售类型）
- **Rule 状态**：有效（VALID）→ 失效（INVALID），通过 `DeliveryRuleStateEnum` 和 `DeliveryStateEnum` 双重状态管理
^[DeliveryFlowRuleServiceImpl.java]

## 核心方法

### 查询类

#### queryFlowRuleList
`POST /delivery/flow/rule/query/list`

分页查询流程配置规则列表。查询链路：
1. 根据条件联表查询 Rule 基础信息
2. 按 ruleIdList 批量查询 RuleUnit（分公司、套餐、销售类型、单据类型）
3. 按 ruleIdList 批量查询 Category（品类规则列表）
4. 组装返回 RuleDTO（含 categoryList）^[DeliveryFlowRuleServiceImpl.java:L140-L165]

#### queryFlowRuleSimpleInfo
`GET /delivery/flow/rule/info/simple`

查询规则简单详情（不包含品类规则列表），仅返回 Rule 基本信息 + RuleUnit 维度。^[DeliveryFlowRuleServiceImpl.java:L464-L481]

#### queryFlowRulePageInfo
`GET /delivery/flow/rule/info`

查询规则页面详情（包含品类规则列表）。在简单详情基础上，额外查询 Category 列表并填充。^[DeliveryFlowRuleServiceImpl.java:L484-L495]

#### queryDeliveryProcessInfo
`GET /delivery/flow/delivery/process/info`

根据全流程配置ID（deliveryProcessCfgId）查询排程配置的基本信息，包括套餐列表、分公司列表、销售类型（根据 decorateBusinessType 和 fulfillmentMode 映射）。^[DeliveryFlowRuleServiceImpl.java:L73-L97]

### 生命周期类

#### ruleSave
`POST /delivery/flow/rule/save`

新增规则：
1. 校验是否存在重复规则（ruleSaveCheck）
2. 保存 Rule 主记录（状态为 VALID）
3. 根据入参的 documentTypeList × mdmCompanyList × productComboList 生成笛卡尔积 RuleUnit 列表并批量插入 ^[DeliveryFlowRuleServiceImpl.java:L378-L411]

#### ruleSaveCheck
`POST /delivery/flow/rule/check/save`

新增规则前校验：检查是否存在条件（分公司、套餐、销售类型、单据类型、项目版本）完全一致的已有规则。^[DeliveryFlowRuleServiceImpl.java:L444-L461]

#### ruleUpdate
`POST /delivery/flow/rule/info/update`

更新规则：
1. 校验重复规则（排除自身）
2. 更新 Rule 主记录的 modifyBy/modifyTime
3. 删除旧的 RuleUnit，重新生成并批量插入 ^[DeliveryFlowRuleServiceImpl.java:L499-L534]

#### ruleExpire
`GET /delivery/flow/rule/expire`

规则失效（级联操作）：
1. Rule 状态 → INVALID
2. 有效状态的 RuleUnit → INVALID
3. 有效/编辑/生效态的 Category → INVALID
4. 遍历每个 categoryId，调用 `CategoryProcessService#modifyCategoryProcessState` 失效流程配置（容错处理）
5. 查询被全流程引用的品类，发送群消息提醒 ^[DeliveryFlowRuleServiceImpl.java:L307-L374]

#### ruleExpireCheck
`GET /delivery/flow/rule/expire/check`

规则失效前校验：检查规则是否存在、是否已失效、是否被全流程引用（通过品类规则的 categoryCode 到排程系统查询引用关系）。^[DeliveryFlowRuleServiceImpl.java:L275-L303]

#### queryFlowUpdateRecordList
`POST /delivery/flow/rule/query/updaterecord`

查询规则修改记录（当前返回空分页，功能预留）。^[DeliveryFlowRuleServiceImpl.java:L537-L540]

### 排程同步类

#### updateDeliveryFlowRuleInfo
`POST /delivery/rule/update/info`（Feign 接口）

排程侧更新分公司的套餐信息后，同步更新 RuleUnit：
1. 根据 deliveryProcessCfgId 查询所有有效规则
2. 遍历每个规则，将新的分公司 × 套餐组合重新生成 RuleUnit（保留原有的销售类型和单据类型）
3. 先删后建 ^[DeliveryFlowRuleServiceImpl.java:L544-L619]

#### mainProcessSync
`POST /main/process/sync`（Feign 接口）

主流程信息同步：
1. 根据 deliveryProcessCfgId 查询所有关联规则
2. 对每个规则，根据新的品牌公司和套餐组合重新生成 RuleUnit
3. 对比新旧 unit，删除过时的、新增不存在的 ^[DeliveryFlowRuleServiceImpl.java:L622-L632]

#### effectDeliveryProcessCfgState
`POST /delivery/rule/delivery/process/state/effect`（Feign 接口）

更新全流程配置的状态到关联的 Rule 记录上（deliveryProcessCfgState 字段）。^[DeliveryFlowRuleServiceImpl.java:L635-L644]

#### sendPushMsg
`GET /delivery/flow/rule/send/push`

发送流程配置变更消息通知。^[DeliveryFlowRuleServiceImpl.java:L765-L767]

### 辅助类

#### translateSaleType
根据全流程配置的 decorateBusinessType 和 fulfillmentMode 映射销售类型：
- 整装（WHOLE_DECORATE）→ 套餐（PACKAGE_TIED）
- 团装 + 硬装 → 团装（TUAN）
- 团装 + 非硬装 → 团装零售（TUAN_RETAIL）
- 局装 + 硬装 → 局装（PARTIAL_DECORATION）
- 局装 + 非硬装 → 精装全案（FINE_TYPE）^[DeliveryFlowRuleServiceImpl.java:L109-L136]

## 状态机

```
Rule 状态 (DeliveryRuleStateEnum):
  VALID ──ruleExpire()──► INVALID

关联的 Category 状态 (DeliveryStateEnum):
  DELIVERY_ACTIVE ──ruleExpire()──► INVALID
  INIT_ACTIVE     ──ruleExpire()──► INVALID
  EDIT            ──ruleExpire()──► INVALID
```

Rule 自身只有 VALID/INVALID 两种状态。规则失效时级联失效所有关联的 Category 及其流程配置。

## 入口点

### Controller（HTTP REST）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/delivery/flow/delivery/process/info` | GET | [[delivery-flow-rule#queryDeliveryProcessInfo\|queryDeliveryProcessInfo]] |
| `/delivery/flow/rule/query/list` | POST | [[delivery-flow-rule#queryFlowRuleList\|queryFlowRuleList]] |
| `/delivery/flow/rule/info` | GET | [[delivery-flow-rule#queryFlowRulePageInfo\|queryFlowRulePageInfo]] |
| `/delivery/flow/rule/info/simple` | GET | [[delivery-flow-rule#queryFlowRuleSimpleInfo\|queryFlowRuleSimpleInfo]] |
| `/delivery/flow/rule/expire/check` | GET | [[delivery-flow-rule#ruleExpireCheck\|ruleExpireCheck]] |
| `/delivery/flow/rule/expire` | GET | [[delivery-flow-rule#ruleExpire\|ruleExpire]] |
| `/delivery/flow/rule/save` | POST | [[delivery-flow-rule#ruleSave\|ruleSave]] |
| `/delivery/flow/rule/check/save` | POST | [[delivery-flow-rule#ruleSaveCheck\|ruleSaveCheck]] |
| `/delivery/flow/rule/info/update` | POST | [[delivery-flow-rule#ruleUpdate\|ruleUpdate]] |
| `/delivery/flow/rule/query/updaterecord` | POST | [[delivery-flow-rule#queryFlowUpdateRecordList\|queryFlowUpdateRecordList]] |
| `/delivery/flow/rule/send/push` | GET | [[delivery-flow-rule#sendPushMsg\|sendPushMsg]] |

Controller: `DeliveryFlowRuleInfoController`（同时实现 `DeliveryFlowRuleFeign`）^[edar-starlord-web/.../DeliveryFlowRuleInfoController.java]

### Feign（远程调用，通过 DeliveryFlowRuleFeign）
| 端点 | 方法 | 入口 |
|---|---|---|
| `/delivery/rule/update/info` | POST | [[delivery-flow-rule#updateDeliveryFlowRuleInfo\|updateDeliveryFlowRuleInfo]] |
| `/delivery/rule/delivery/process/state/effect` | POST | [[delivery-flow-rule#effectDeliveryProcessCfgState\|effectDeliveryProcessCfgState]] |
| `/delivery/rule/delivery/category/query/code` | GET | 委托给 `DeliveryFlowCategoryService#queryDeliveryCategoryCode` |

Feign 接口: `DeliveryFlowRuleFeign`^[edar-starlord-api/.../DeliveryFlowRuleFeign.java]

### 内部调用
- `DeliveryFlowCategoryServiceImpl` → `queryFlowRuleSimpleInfo`（查询品类详情时获取规则信息）、`queryFlowRuleList`（套用品类时搜索可套用的规则）、`queryFlowRulePageInfo`（校验时使用）
- `CategoryProcessController#mainProcessSync` → `mainProcessSync`^[CategoryProcessController.java:L65-L68]

## 依赖

- **DAO 层**：`DeliveryFlowRuleDao`（规则主表）、`DeliveryFlowRuleUnitDao`（规则明细表）、`DeliveryFlowRuleCategoryDao`（品类规则表）
- **服务层**：[[delivery-flow-category|DeliveryFlowCategoryService]]（品类规则组装）、[[category-process|CategoryProcessService]]（失效时级联失效品类流程配置）
- **Manager 层**：`DeliveryProcessCfgManager`（全流程配置查询、品类引用关系查询）
- **工具**：`MsgSendUtil`（群消息通知）
