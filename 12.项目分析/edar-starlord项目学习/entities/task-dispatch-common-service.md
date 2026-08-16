---
title: TaskDispatchCommonService - 任务公共工具服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [task-dispatch, material-task, service, delivery]
sources:
  - edar-starlord-service/.../servicev2/TaskDispatchCommonService.java
  - edar-starlord-service/.../servicev2/impl/TaskDispatchCommonServiceImpl.java
confidence: high
---

# TaskDispatchCommonService

## 概述

`TaskDispatchCommonService` 是主材任务实例的公共工具服务，主要负责补全 `TaskDispatch` 对象的关联节点信息。它是一个轻量级的辅助服务，仅包含一个方法，用于在查询任务列表后，批量填充每个任务关联的 `TaskDispatchNode` 列表，避免多次查询导致 N+1 问题。

^[edar-starlord-service/.../servicev2/TaskDispatchCommonService.java]

## 关键方法

### `complementedTaskDispatchNode(List<TaskDispatch>)`

**功能**：为传入的 `TaskDispatch` 列表批量补全其关联的有效任务节点。

**实现逻辑**：
1. 提取所有任务 ID → `Set<Long>`
2. 调用 `TaskDispatchNodeDao.querylist` 一次性查询所有任务的有效节点（过滤 `StateEnum.VALID`）
3. 按 `taskDispatchId` 分组 → `Map<Long, List<TaskDispatchNode>>`
4. 遍历每个 `TaskDispatch`，通过 `setList()` 设置其节点列表

该方法将 N+1 查询优化为一次批量查询，是典型的补全（complement）模式。

^[edar-starlord-service/.../servicev2/impl/TaskDispatchCommonServiceImpl.java:32-47]

## 入口点

| 入口类型 | 位置 | 说明 |
|---------|------|------|
| 内部调用 | `FullCalculateServiceImpl` | 全量计算服务中，需要补全任务节点信息 |

^[edar-starlord-service/.../servicev2/impl/FullCalculateServiceImpl.java]

## 依赖

| 依赖 | 用途 |
|------|------|
| `TaskDispatchNodeDao` | 查询任务节点列表 |

## 参见

- [[task-dispatch-v2-service]] — 任务派发 V2 核心服务
- [[task-dispatch-complete-service]] — 任务完成服务
