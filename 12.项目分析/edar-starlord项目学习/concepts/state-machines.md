---
title: 物料任务状态机
created: 2026-07-01
updated: 2026-07-01
type: concept
tags: [state-machine, material-task, task-dispatch]
confidence: high
---

# 物料任务状态机

## TaskDispatch 状态 (任务调度)

| 状态 | 枚举值 | 含义 | 触发来源 |
|------|--------|------|----------|
| 待激活 | WAIT_ACTIVATE | 任务已创建但未到激活时间 | MaterialCreateV2Service |
| 已激活 | ACTIVATED | 任务已激活，进入调度 | DispatchActivateService / MaterialActivateV2Service |
| 进行中 | IN_PROGRESS | 任务正在执行 | TaskDispatchV2Service |
| 已完成 | COMPLETED | 所有节点完成 | TaskDispatchCompleteService |
| 已取消 | CANCELLED | 任务被取消 | TaskDispatchCancelService / MaterialCancelV2Service |

^[enumeration/TaskDispatchStatusEnum.java]

## TaskDispatchNode 状态 (任务节点)

| 状态 | 含义 |
|------|------|
| 待启动 | 前置节点未完成 |
| 可启动 | 前置条件满足，等待执行人接单 |
| 进行中 | 正在执行 |
| 已完成 | 节点完工 |
| 已取消 | 节点被取消 |

^[enumeration/TaskDispatchNodeStatusEnum.java]

## MaterialDelayProcess 状态 (延期处理)

```
新建(未确认) → [confirmDelayProcess] → 已确认
     ↓
  删除 → Kafka 通知删除
```

^[enumeration/MaterialDelayConfirmStateEnum.java]

## ProcessStatusEnum (流程状态)

| 状态 | 含义 |
|------|------|
| NOT_START | 未开始 |
| IN_PROGRESS | 进行中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

## 节点类型 (NodeTypeEnum)

| 类型 | 含义 |
|------|------|
| MATERIAL_SEND | 主材送货 |
| MATERIAL_INSTALL | 主材安装 |
| MEASURE | 量房 |
| ACCEPTANCE | 验收 |
| DESIGN | 设计 |
| ... | ... |

## 状态流转规则
- 任务激活：plan_activate_start_date ≤ today → DispatchActivateService 自动激活
- 节点启动：前置节点状态均为 COMPLETED → 自动变为"可启动"
- 节点完成：执行人提交完成 → 检查是否有后续节点 → 触发下一节点
- 任务完成：所有节点 COMPLETED → TaskDispatchCompleteService 标记任务完成
- 延期确认：MaterialDelayProcessService.confirmDelayProcess → 计算新承诺日期 → 推送通知

## 相关页面
- [[task-dispatch-v2]] — 任务调度V2
- [[material-delay-process]] — 延期处理
- [[acceptance-report]] — 验收报告
