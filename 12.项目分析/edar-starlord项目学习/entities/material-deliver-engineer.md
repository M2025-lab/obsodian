---
title: MaterialDeliverEngineerService（交付工程师主材任务服务）
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [delivery, service, 主材, 交付工程师, 测量, 复尺]
sources:
  - edar-starlord-service/src/main/java/com/ke/utopia/service/MaterialDeliverEngineerService.java
  - edar-starlord-service/src/main/java/com/ke/utopia/service/impl/MaterialDeliverEngineerServiceImpl.java
confidence: high
---

# MaterialDeliverEngineerService（交付工程师主材任务服务）

## 概述

交付工程师主材任务查询服务，专门为**交付工程师**角色提供。核心功能是查询指定项目订单下、类型为**测量**或**复尺**的主材进展任务列表。查询条件限定为：激活未完成状态、节点类型为"通知可启动"/"通知启动"/"启动"、任务类型为测量或复尺。

实现类为 `MaterialDeliverEngineerServiceImpl`，位于 `edar-starlord-service` 模块。

## 关键方法

### listMaterialTaskDispatchNodeMeasureOrRecheck(Long projectOrderId)
获取订单下类型为测量和复尺的主材进展列表。查询逻辑：
1. 构造查询参数 `TaskDispatchParam`：
   - 节点类型限定为：`NOTIFY_CAN_START`（通知可启动）、`NOTIFY_START`（通知启动）、`START`（启动）
   - 任务类型限定为：`MEASURE`（测量）、`RECHECK_SCALE`（复尺）
   - 状态限定为：激活未完成（`UNCOMPLETED`）
   - 若传入 `projectOrderId` 则按订单查询；否则按当前登录用户的 `ucid` 查询，且预估时间限定为近 3 天内
   - 按预估时间升序排列
2. 通过 `TaskDispatchNodeDao.listTaskByCondition` 查询节点 DTO 列表
3. 批量查询对应的 `TaskDispatch`（主材任务）信息，补全供应商、材料名称、材料编码等字段
4. 转换为 `TaskDispatchNodeItemDTO` 列表返回

## 入口点

### REST 控制器

| 控制器 | 方法 | 端点 | 调用链 |
|--------|------|------|--------|
| `MaterialDeliverEngineerController.listMaterialTaskDispatchNodeMeasureOrRecheck()` | `GET /material-deliver-engineer/dispatch/list-task-dispatch/measure-or-recheack` | → `service.listMaterialTaskDispatchNodeMeasureOrRecheck(projectOrderId)` |

### Feign 接口

- **MaterialDeliverEngineerFeign**（`@FeignClient(value="edar-starlord", contextId="material-deliverEngineer-feign")`）：对外暴露上述 REST 端点，供其他微服务远程调用

## 依赖

| 依赖 | 类型 | 用途 |
|------|------|------|
| `TaskDispatchNodeDao` | DAO | 按条件查询任务调度节点列表（`listTaskByCondition`） |
| `TaskDispatchDao` | DAO | 根据任务 ID 批量查询 `TaskDispatch`，补全供应商/材料信息（`listByExample`） |
| `OperatorContextHandler` | 工具 | 获取当前登录用户上下文，用于按执行人过滤任务 |

## 状态机

不涉及状态变更，为纯查询服务。

## 查询条件详解

```
projectOrderId 存在？
  ├── 是 → 按订单 ID 精确查询
  └── 否 → 按当前用户 ucid 查询 + estimatedTime 限定近 3 天

通用条件：
  - processStatus = UNCOMPLETED（激活未完成）
  - nodeType IN [NOTIFY_CAN_START, NOTIFY_START, START]
  - taskType IN [MEASURE, RECHECK_SCALE]
  - orderBy: estimated_time ASC
```

## 数据流

```
交付工程师请求
       │
       ▼
MaterialDeliverEngineerController
       │
       ▼
MaterialDeliverEngineerService.listMaterialTaskDispatchNodeMeasureOrRecheck()
       │
       ├──▶ TaskDispatchNodeDao.listTaskByCondition()  ──▶ List<TaskDispatchDTO>
       │         │
       │         └──▶ 提取 taskDispatchId 列表
       │
       ├──▶ TaskDispatchDao.listByExample()  ──▶ List<TaskDispatch>
       │         │
       │         └──▶ 构建 taskDispatchId → TaskDispatch 映射
       │
       └──▶ toTaskDispatchNodeItemDTO() ──▶ 合并节点信息 + 任务信息 ──▶ List<TaskDispatchNodeItemDTO>
```
