---
title: MaterialFlowRuleService - 物料流程规则服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [material-flow, rule, config, service]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/MaterialFlowRuleService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/servicev2/impl/MaterialFlowRuleServiceImpl.java
confidence: high
---

# MaterialFlowRuleService 物料流程规则服务

## 概述
管理物料流程规则的完整生命周期：创建、更新、失效、生效、设为默认、复制、查询。物料流程规则定义了"什么品类 x 什么分公司 x 什么套餐"使用哪套物料流程模板，是交付流程配置的核心规则引擎。与 [[material-flow-category]]（品类规则）和 [[material-template-v2]]（模板）紧密关联。

## Key Methods

### queryFlowRuleList(param)
规则列表分页查询。支持按分公司、品类、规则状态、创建时间等多条件过滤。返回 Page<MaterialFlowRuleDTO>。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### ruleSave(param) / ruleUpdate(param)
创建/更新规则。包含品类规则（MaterialFlowRuleUnit）的数组配置，可同时创建多个品类的规则。保存时进行规则生效校验。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### ruleSaveCheck(param)
规则生效校验。保存前检查规则是否与已有规则冲突（同分公司+同品类+同套餐不能重复生效）。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### ruleEffective(ruleId) / ruleExpire(ruleId)
规则生效/失效。切换规则的生效状态，控制哪些规则在运行时被匹配。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### ruleSetDefault(ruleId)
设为默认规则。指定规则作为该分公司+品类的默认兜底规则。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### queryFlowRulePageInfo(ruleId)
查询规则页面详情，根据状态查询品类规则数据。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### ruleAddCompany(param) / ruleAllCopy(param) / ruleAllEffective(param)
刷数操作：批量新增分公司、复制规则、批量生效所有品类。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

### queryFlowUpdateRecordList(param)
查询操作记录分页列表。^[servicev2/impl/MaterialFlowRuleServiceImpl.java]

## Entry Points

### REST Controllers
- MaterialFlowRuleInfoController → 规则 CRUD
  → `code-path: edar-starlord-web/.../web/materialflow/MaterialFlowRuleInfoController.java`
- MaterialFlowRuleQueryController → 规则查询（分页列表、详情）
  → `code-path: edar-starlord-web/.../web/materialflow/MaterialFlowRuleQueryController.java`
- MaterialFlowSelectQueryController → 规则下拉选择查询
  → `code-path: edar-starlord-web/.../web/materialflow/MaterialFlowSelectQueryController.java`

### Feign Interfaces
- MaterialFlowRuleQueryFeign → 远程规则查询
  → `code-path: edar-starlord-api/.../api/MaterialFlowRuleQueryFeign.java`

## Dependencies
- **DAO**: MaterialFlowRuleDao, MaterialFlowRuleUnitDao, MaterialFlowRuleCategoryDao, MaterialFlowRuleTemplateMappingDao, MaterialFlowRuleTemplateRecordDao
- **Service**: MaterialFlowRuleTemplateMappingService, MaterialFlowCategoryService, MaterialFlowMixTemplateService

## 规则生命周期
```
ruleSave (创建/更新) → ruleSaveCheck (校验冲突) → ruleEffective (生效)
                                                    ↓
                                          ruleExpire (失效) → ruleSetDefault (默认兜底)
```
