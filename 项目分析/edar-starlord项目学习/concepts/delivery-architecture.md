---
title: 交付流程架构 - 代码分层与调用链
created: 2026-07-01
updated: 2026-07-01
type: concept
tags: [architecture, delivery, layer]
confidence: high
---

# 交付流程架构

## 分层架构

```
┌─────────────────────────────────────────────────────┐
│                  Entry Points                       │
│  REST Controllers (81) + Feign (46) + Kafka (23)   │
├─────────────────────────────────────────────────────┤
│              Service Layer (业务逻辑)                │
│     service/ (V1)  +  servicev2/ (V2)              │
│                                                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐        │
│  │ Material │ │  Task    │ │ DeliveryFlow │        │
│  │  Tasks   │ │ Dispatch │ │   Config     │        │
│  └──────────┘ └──────────┘ └──────────────┘        │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐        │
│  │  Delay   │ │Coordinator│ │  Acceptance  │        │
│  │ Process  │ │   Task    │ │   Report     │        │
│  └──────────┘ └──────────┘ └──────────────┘        │
├─────────────────────────────────────────────────────┤
│            Manager Layer (外部集成)                  │
│  SCM / Athena / Construction / Zeus / Ceres ...    │
├─────────────────────────────────────────────────────┤
│               DAO Layer (数据访问)                   │
│     MyBatis Mapper + DAO + Model                   │
└─────────────────────────────────────────────────────┘
```

## V1 vs V2

项目存在两套 Service 层：

| 版本 | 包路径 | 特点 |
|------|--------|------|
| V1 | `com.ke.utopia.service` | 原始版本，面向管家/安装工/客户的角色视图 |
| V2 | `com.ke.utopia.servicev2` | 重构版本，面向交付流(DeliveryFlow)配置驱动 + 品类规则引擎 |

V2 逐步替代 V1 的场景，但 V1 中面向角色的服务（MaterialButlerService、MaterialCustomerService）仍在使用。

## 入口点类型

### REST Controllers (81个)
直接面向前端/外部系统的 HTTP API。Controller 注入 Service 并委托业务逻辑。

### Feign Interfaces (46个)
微服务间 RPC 调用。定义在 `edar-starlord-api` 模块，实现在 `edar-starlord-web` 的 Controller 中（Controller 实现 Feign 接口）。

### Kafka Listeners (23个)
事件驱动的异步入口。监听外部系统（SCM、Athena、BPM、CRM 等）的事件并触发业务流程。

关键 Listener → Service 映射：
- ScmOrderEventListener → DeliveryMaterialBizService（子订单变更 → 创建/取消主材任务）
- WorkCenterTaskChangedEventHandler → MaterialDelayProcessService（工单变更 → 撤销延期）
- WorkOrderCreateEventHandler → CoordinatorTaskService（工单创建 → 返补检查）
- DeliveryProcessChangeListener → CategoryProcessService（交付流程变更）
- ProjectVssListener → MaterialVssSchedule（项目 VSS 变更 → C端数据刷新）
- AcceptanceReportChangeListener → AcceptanceReportService（验收报告变更）
- CubePackageCreateEventListener → MaterialHandleV2Service（套餐包创建 → 物料处理）

### Scheduled Tasks
- MaterialTaskSchedule → DispatchActivateService（每天激活到期任务）
- CoordinatorTaskSchedule → CoordinatorTaskService（定时刷新返补单状态）
- DelayTaskSchedule → MaterialDelayProcessService（延期任务定时检查）
- MaterialVssSchedule → MaterialCustomerService（定时刷新C端进展）
- SelfBuyTaskSchedule → MaterialSelfBuyService（自购任务定时处理）

## 关键上下游依赖

### 物料任务主链路
```
SCM订单事件 → DeliveryMaterialBizService
  → createMaterialTask()
    → MaterialCreateV2Service（创建物料任务模板）
    → MaterialActivateV2Service（激活任务）
    → TaskDispatchV2Service（生成调度实例）
      → MaterialTaskBizV2Service（业务操作）
        → MaterialHandleV2Service（节点处理）
```

### 延期处理链路
```
WorkCenter TaskChanged 事件 → MaterialDelayProcessService.createDelayUndoTasks()
管家派工事件 → MaterialDelayProcessService.handleButlerManagerAssign()
客户/管家操作 → MaterialDelayProcessController → createMaterialDelayProcess()
  → MaterialDelayApproveService（审批流）
  → MessagePushClient（推送通知）
```

### 返补单协调链路
```
工单创建事件 → CoordinatorTaskService.handleWorkOrderNotice()
  → PlaceOrderCommander（下单）
  → 分配协调人 → 跟单进度
    → CoordinatorTaskSchedule（定时刷新SCM状态）
```

## 相关页面
- [[project-overview]] — 项目总览
- [[event-driven-flow]] — 事件驱动流程

## 相关外部文档
- [[starlord项目整理/主材全链路业务与系统配置总结]] — 配置驱动架构的业务含义
- [[starlord项目整理/安装流程]] — 安装任务节点流转（V2架构的具体体现）
- [[starlord项目整理/复尺流程]] — 复尺任务节点流转
