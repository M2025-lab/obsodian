---
title: 项目总览 (edar-starlord)
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, project-overview]
sources: [pom.xml]
confidence: high
---

# edar-starlord 交付中台

## 概述
贝壳/链家家装业务交付中台系统。管理从量房、物料任务创建、调度派工、安装施工、验收到延期的完整交付生命周期。基于 Spring Boot 2.1 + MyBatis + Kafka 事件驱动架构，通过 Feign 对外暴露 API。

## 模块划分

| 模块 | 职责 | 包路径 |
|------|------|--------|
| edar-starlord-web | REST Controller + 应用配置 | `com.ke.utopia.web` |
| edar-starlord-api | Feign 接口定义 + DTO/VO | `com.ke.utopia.api`, `com.ke.utopia.dto` |
| edar-starlord-service | 业务逻辑（接口+实现）+ MQ Listener + 定时任务 | `com.ke.utopia.service`, `com.ke.utopia.servicev2` |
| edar-starlord-manager | 外部服务集成与编排 | `com.ke.utopia.manager` |
| edar-starlord-dao | MyBatis 数据访问 | `com.ke.utopia.dao`, `com.ke.utopia.mapper`, `com.ke.utopia.model` |
| edar-starlord-base | 枚举和工具类 | `com.ke.utopia.enumeration`, `com.ke.utopia.util` |

## 核心业务域

| 业务域                        | 描述        | 关键服务                                                     |
| -------------------------- | --------- | -------------------------------------------------------- |
| [[material-delivery]]      | 物料交付管理    | MaterialDeliveryService, MaterialDeliverEngineerService  |
| [[delivery-flow-category]] | 交付流程分类与模板 | DeliveryFlowCategoryService, CategoryProcessService      |
| [[material-flow-query]]    | 物料流程统一查询  | MaterialFlowQueryService, MaterialFlowRuleService        |
| [[material-delay-process]] | 延期处理      | MaterialDelayProcessService, MaterialDelayApproveService |
| [[task-dispatch-v2]]       | 任务调度V2    | TaskDispatchV2Service, TaskDispatchCommonService         |
| [[material-task-biz-v2]]   | 物料任务业务    | MaterialTaskBizV2Service                                 |
| [[acceptance-report]]      | 验收报告      | AcceptanceReportService                                  |
| [[coordinator-task]]       | 返补单协调器    | CoordinatorTaskService                                   |
| [[material-schedule]]      | 物料排期      | MaterialScheduleService                                  |

## 事件驱动架构
- **Kafka**: 23个 Consumer Listener，监听 SCM 订单、施工进度、派工事件、BPM 流程等外部事件
- **Feign**: 46个 Feign 接口对外暴露服务
- **定时任务**: CoordinatorTaskSchedule、MaterialTaskSchedule、DelayTaskSchedule 等

## 入口点全景
- **REST**: 81个 Controller
- **Feign**: 46个 FeignClient 接口
- **Kafka**: 23个 KafkaListener
- **Schedule**: 多个定时任务

---
## 相关文档

- [[starlord项目整理/业务知识沉淀/edar-starlord系统新人学习文档]] — 系统新人学习入口（业务视角）
- [[starlord项目整理/主材全链路业务与系统配置总结]] — 全链路业务配置总览
- [[starlord项目整理/枚举类和上下游梳理-starlord系统]] — 系统职责与领域划分
- [[starlord项目整理/REST汇总/starlord接口字典文档]] — 748个REST接口总字典
- [[starlord数据库表/starlord10张核心数据库表]] — 10张核心表结构
- [[starlord数据库表/所有的starlord数据库表]] — 全库分层目录
