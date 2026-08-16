---
title: 交付流程配置驱动 - DeliveryFlow 规则引擎
created: 2026-07-01
updated: 2026-07-01
type: concept
tags: [delivery-flow, rule-engine, config-driven]
confidence: high
---

# 交付流程配置驱动 (DeliveryFlow)

## 概述
V2 交付流程采用配置驱动模式：通过"品类规则引擎"将"什么品类 x 什么分公司 x 什么套餐"映射到"什么物料流程模板"。模板定义了流程节点、节点间的转移条件、每个节点的执行人角色和时间规则。

## 配置层次

```
DeliveryFlowCategory (分类)
  └─ 定义大类（如"定制柜类"、"地板"、"涂料"）
     └─ CategoryProcessRoute (分类流程路由)
        └─ 定义该分类下的流程节点编排

MaterialFlowRule (物料流程规则)
  └─ 匹配条件：分公司(branch) + 品类(category) + 套餐(combo) + 单据类型
     └─ MaterialFlowRuleUnit (品类规则单元)
        └─ 指定流程模板(NMaterialTemplate)

NMaterialTemplate (物料模板)
  └─ 定义流程节点(NMaterialProcessDefine)
     └─ 定义节点转移条件(NMaterialNodeTransferCondition)
        └─ 定义时间计算规则(NMaterialTimeCalculateRuleCfg)
```

## 规则匹配流程

```
请求到达 (projectOrderId + sku + branch + combo)
  ↓
DeliveryFlowRuleService.ruleSaveCheck()
  ↓
匹配 MaterialFlowRule:
  1. 匹配分公司 (branch)
  2. 匹配品类 (category)  
  3. 匹配套餐 (combo)
  4. 匹配单据类型
  ↓
获取 NMaterialTemplate (流程模板)
  ↓
MaterialFlowQueryService.queryMaterialConfigData()
  ↓ 返回完整流程配置
应用到 TaskDispatchCreateService 生成调度实例
```

## 核心服务

| 服务 | 职责 |
|------|------|
| [[delivery-flow-category]] | 品类分类管理 |
| [[category-process]] | 分类流程路由 |
| [[delivery-flow-rule]] | 交付流程规则CRUD |
| [[delivery-flow-task-node]] | 交付流程任务节点管理 |
| [[material-flow-rule]] | 物料流程规则引擎 |
| [[material-flow-query]] | 统一配置查询 |

## 相关页面
- [[delivery-architecture]] — 架构总览
- [[state-machines]] — 状态机
