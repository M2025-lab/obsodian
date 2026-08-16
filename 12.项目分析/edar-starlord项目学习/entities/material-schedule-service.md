---
title: MaterialScheduleService - 主材排程服务
created: 2026-07-01
updated: 2026-07-01
type: entity
tags: [material-task, delivery, scheduler, service]
sources:
  - edar-starlord-service/.../servicev2/MaterialScheduleService.java
  - edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java
confidence: high
---

# MaterialScheduleService

## 概述

`MaterialScheduleService` 是主材排程与日历服务，提供主材任务的日历视图、日报生成、数据清理和 Push 推送等功能。它帮助管理者（工长、管家等角色）按天查看任务的完成情况，并支持每日自动推送任务日报。

^[edar-starlord-service/.../servicev2/MaterialScheduleService.java]

## 关键方法

### 1. 主材日历 — `getMaterialSchedule(MaterialScheduleReq)`

查询指定日期的主材任务概览。

**逻辑**：
1. 解析请求日期，计算日期的起止时间（`00:00:00` ~ `23:59:59`）
2. 查询「预计考核时间在当天但未完成」的任务（`getTodayUnCompleteTask`）
3. 查询「当天已完成」的任务（`getTodayCompleteTask`）
4. 合并后按 `materialCode + supplierCode + taskType + nodeType + processStatus + qualified` 分组
5. 每组的首个元素作为代表，附加 `count` 数量字段

返回结果为每个唯一维度组合的去重汇总列表，避免前端展示重复项。

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:89-119]

### 2. 今日未完成任务 — `getTodayUnCompleteTask(Date, Date, Long, List<Long>)`

查询预计时间在指定范围内且未完成的任务节点。条件：`estimatedTime` 在起止时间内、`processStatus != FINISHED`。

支持可选过滤：项目 ID、指定任务节点 ID 列表。

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:169-187]

### 3. 今日已完成任务 — `getTodayCompleteTask(Date, Date, Long, List<Long>)`

查询完成时间在指定范围内且已完成的任务节点。条件：`endTime` 在起止时间内、`processStatus = FINISHED`。

支持可选过滤：项目 ID、指定任务节点 ID 列表。

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:499-517]

### 4. 主材日报 — `getMaterialDailyReport(Long ucId)`

查询指定用户（及其灰度子账号）前一天的主材任务日报。

**流程**：
1. 通过 Push 客户端获取灰度 ucId 列表
2. 查询 `MessageSendJob` 表中该用户的待通知任务节点
3. 分别查询这些节点中前一天未完成和已完成的任务
4. 组装 `MaterialDailyReportDTO`：
   - 按「角色类型 + 执行人 ID + 执行人姓名」分组
   - 每组包含任务明细（物料、供应商、节点名、超期状态、驳回原因等）
   - 统计每组数量和总数

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:195-220]

### 5. 数据清理 — `cleanData()`

定时清理过期或已完成的任务通知数据。

**逻辑**：
1. 查询所有待通知的 `MessageSendJob` 记录
2. 对每条记录：
   - 如果对应的任务节点已不存在 → 标记为 `FAIL`
   - 如果节点已完成且完成时间距今超过 1 天 → 标记为 `FAIL`

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:227-249]

### 6. 每日 Push 推送 — `dayPush()`

每日向管理者推送其下级的任务日报。

**流程**：
1. 查询所有待通知的 `MessageSendJob` 记录
2. 按父级执行人（`parentExecutorId + parentExecutorType`）分组
3. 对每个父级：
   - 调用 `getMaterialDailyReport` 生成日报数据
   - 组装 Push 标题（`【主材任务日报】出炉啦，共X条，查看详情`）和内容
   - 通过 `pushClient.sendPushMsg` 发送 Push 消息

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:256-306]

### 7. 任务节点施工包绑定 — `fillTaskNodePackageCode()` (已废弃)

已标记 `@Deprecated`。历史数据修复方法，用于将 2.5 模式/交付流模式的任务节点与施工包编码（`packageCode`）建立关联。

通过分页游标遍历所有任务：
1. 查询任务的订单号
2. 通过订单号查施工包材料需求表
3. 将施工包编码回写到所有未绑定的任务节点

^[edar-starlord-service/.../servicev2/impl/MaterialScheduleServiceImpl.java:308-374]

## 入口点

| 入口类型 | 位置 | 说明 |
|---------|------|------|
| Controller | `MaterialScheduleController` | 主材日历 REST 接口 |
| Scheduled | `MaterialTaskSchedule` | 定时任务：`dayPush()`、`cleanData()` |

^[edar-starlord-web/.../MaterialScheduleController.java, edar-starlord-service/.../schedule/MaterialTaskSchedule.java]

## 依赖

| 依赖 | 用途 |
|------|------|
| `TaskDispatchNodeDao` | 任务节点查询 |
| `TaskDispatchDao` / `TaskDispatchMapper` | 任务主表查询 |
| `MessageSendJobDao` | 消息发送任务数据 |
| `NewMessagePushService` | 消息推送服务 |
| `PushClient` | Push 客户端（灰度、发送） |
| `ProjectOrderManager` | 项目订单查询 |
| `PackageConstructionManager` | 施工包材料需求查询 |

## 参见

- [[task-dispatch-v2-service]] — 任务派发 V2 核心服务
- [[material-task-biz-v2-service]] — 主材业务查询服务
