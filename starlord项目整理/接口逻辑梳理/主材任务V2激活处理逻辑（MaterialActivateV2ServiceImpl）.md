---
source: code-flow
date: 2026-07-16
class: com.ke.utopia.servicev2.impl.MaterialActivateV2ServiceImpl
method: activateTaskDispatch(TaskDispatch)
---
新版主材任务激活主要分为材料进排程反补的情况、以及这种情况之外的「材料进排程」和「其他的」三种。
材料进排程反补--直接激活。
非材料进排程反补-「查询配置」判断能否激活。 查询配置查询所有有效的和无效的，若查不到则查询已删除的，若还查不到就跳过。查到之后按照版本号降序
材料进排程不反补--根据最新模板的激活条件的判断，若差不到就返回。
	并且验证订单状态大于主材任务配置的节点状态 且 订单不是取消状态 ，可以激活
以及else--进行shouldActive拦截和尾款拦截，
进行激活操作、查第一个未激活的节点，并激活
# 主材任务V2激活处理逻辑（MaterialActivateV2ServiceImpl）

## 功能概述

`activateTaskDispatch(TaskDispatch)` 是 v2 版的主材任务**通用激活**方法。

它接收一个已创建但未激活的 `TaskDispatch`（必须有 `processCode`），经历**三层检查**后，调用 `doActivateTaskDispatch` 完成最终激活。核心职责是：

> 根据任务所属的模式和流程定义，判断是否满足激活条件，满足则激活第一个节点，并触发后续的批次检查、OMS 同步、推送通知、时间计算。

---

## 入口信息

- **类**: `MaterialActivateV2ServiceImpl`（`edar-starlord-service/.../servicev2/impl/MaterialActivateV2ServiceImpl.java`）
- **方法**: `activateTaskDispatch(TaskDispatch taskDispatch)`，第 272-343 行
- **注解**: `@Transactional`
- **接口**: `MaterialActivateV2Service`（`edar-starlord-service/.../servicev2/MaterialActivateV2Service.java`）

### 调用来源

| 调用方 | 触发时机 |
|--------|---------|
| `activateTaskDispatchByPlanTime(taskDispatch)` | 定时任务扫描（批量激活满足 planActivateTime 的任务） |
| `activateTaskDispatch(Long taskDispatchId)` | 外部按 ID 激活 |
| `activateNextTaskDispatch(taskDispatch)` | 前置任务完成时触发的**链式激活** |
| `DispatchCreateServiceImpl.activateTaskDispatch()`（v1） | 旧版激活入口，判断有 processCode 则委托至此 |

---

## 主干流程

```
activateTaskDispatch(taskDispatch)
  │
  ├─ 守卫①：processCode 必须非空 → 否则直接 return
  │
  ├─ 守卫②：排程返补任务 → 跳过所有检查，直接激活
  │
  ├─ 步骤③：查节点配置（NMaterialNodeCfg）
  │   └─ processCode + nodeCode=1000
  │   └─ 查不到 ACTIVE/INVALID 则查 DELETE（取最新版本）
  │
  ├─ 分支④-甲：排程材料任务（DELIVERY_FLOW）？
  │   ├─ 是 → 查最新激活条件（getLastestConditionList）
  │   │   └─ PLAN_TIME 条件逐条检查订单状态
  │   │       ├─ 全部满足 → 进入激活
  │   │       └─ 任一不满足 → SUSPEND_ACTIVE, return
  │   │
  │   └─ 否（全国模式）→ 分支④-乙
  │       ├─ shouldActivate() 检查
  │       │   ├─ 不满足 → return（已内部设 SUSPEND_ACTIVE）
  │       │   └─ 满足 → 尾款检查
  │       │       ├─ 被拦截 → SUSPEND_ACTIVE, return
  │       │       └─ 通过 → 进入激活
  │
  └─ 步骤⑤：doActivateTaskDispatch(taskDispatch) — 正式激活
```

---

## 详细分支拆解

### 守卫 ①：processCode 非空检查

```
taskDispatch.processCode 为空?
  └─ 是 → 直接 return（不处理）
```

**判断依据**：`task_dispatch.process_code` 字段。这是 v2 激活的标志——没有 processCode 的任务不走此方法（应由 v1 处理）。

---

### 守卫 ②：排程返补任务直接激活

```
mode == DELIVERY_FLOW(7) && flowType == REVERSE_ORDER(1)?
  └─ 是 → doActivateTaskDispatch(taskDispatch), return
```

- `mode = 7`（排程材料任务模式）
- `flowType = 1`（返补单——补货/增项单据）
- 这些任务不需要任何条件检查，创建即激活

---

### 步骤 ③：查询节点配置

```sql
-- 先查 ACTIVE / INVALID 状态的节点配置
SELECT * FROM n_material_node_cfg
WHERE process_code = #{taskDispatch.processCode}
  AND node_code = 1000
  AND state IN (1, 2)  -- ACTIVE=1, INVALID=2

-- 查不到时再查 DELETE 状态的
SELECT * FROM n_material_node_cfg
WHERE process_code = #{taskDispatch.processCode}
  AND node_code = 1000
  AND state = 3  -- DELETE=3
-- 取 version 最大的那条
```

**为什么查 `nodeCode = 1000`？** 在 v2 的流程定义中，固定值 1000 表示流程节点（开始节点），从这里可以获取该流程的激活方式配置（`activateMode`、`activateRelationCode`、`activateScheduleRelation` 等）。

**查不到时的兜底**：
```
能找到 ACTIVE/INVALID 的节点配置?
  └─ 否 → 再查 DELETE 状态的配置
      ├─ 也没有 → return（无声跳过）
      └─ 有 → 按 version 降序排序，取最新版本
```

---

### 分支 ④-甲：排程材料任务（DELIVERY_FLOW）激活判断

适用条件：`mode = 7`（排程材料配置模式）

```
查最新激活条件（getLastestConditionList）
  │
  ├─ 第1步：查项目排程最新模板ID
  │   └─ deliveryProcessCfgManager.queryNewestTemplateIdByProjectId(projectOrderId)
  │   └─ 查不到? → return null → 外层 return
  │
  ├─ 第2步：按 processCode + templateId 查激活条件
  │   └─ materialNodeTransferConditionDao.queryByProcessCodeList(processCode, templateId)
  │
  ├─ 第3步（兜底）：查不到则按 processCode 查全局条件（不限定模板）
  │   └─ materialNodeTransferConditionDao.queryByProcessCodeList(processCode)
  │   └─ 还查不到? → return null → 外层 return
  │
  ├─ 第4步：取 max(templateId) 对应的条件
  │   └─ 多个模板命中时，取最新的（templateId 最大）
  │
  └─ 返回条件列表
      │
      ├─ 过滤出 conditionMode == PLAN_TIME(0) 的条件
      │   └─ 没有 PLAN_TIME 条件 → 跳过订单状态检查
      │
      └─ 有 PLAN_TIME 条件 → 对每个条件检查订单状态
          │ 检查方式: MajorSequenceEnum.canActivateByProjectOrderStatus(
          │              projectOrder.status, condition.relationCode)
          │ 判断: 订单状态 >= 配置工序状态 && 订单未取消
          │
          ├─ 全部满足? → 继续执行（进入 doActivateTaskDispatch）
          │
          └─ 任一不满足?
              └─ updateTaskSuspendActive(taskDispatch)
                  → taskDispatch.processStatus: UN_ACTIVE(1) → SUSPEND_ACTIVE(4)
                  → return
```

---

### 分支 ④-乙：全国模式激活判断

适用条件：`mode != 7`（非排程模式，如 HOME2.5、HOME2.5_MANPOWER 等）

```
取 materialNodeCfgs.get(0) → NMaterialNodeCfg（取第一条节点配置）
  │
  ├─ shouldActivate(taskDispatch, materialNodeCfg)?
  │   │
  │   ├─ 前置守卫：参数为空 → true（放行）
  │   │
  │   ├─ 子分支①：PLAN_TIME(0) 且时间关系是 TODAY(2) 或 AFTER(3)
  │   │   ├─ 查 orderManager.getProjectOrder(projectOrderId) 获取项目订单
  │   │   │
  │   │   ├─ mode == HOME2_5(5) ?
  │   │   │   ├─ 是 → 双路径检查:
  │   │   │   │   ├─ 路径A：订单状态大于配置工序 && 未取消?
  │   │   │   │   │   └─ 满足 → true
  │   │   │   │   └─ 路径B：不满足 → 查验收报告
  │   │   │   │       ├─ 开工交底 → 查开工报告状态 > TEMPORARY_STORAGE
  │   │   │   │       ├─ 技术交底 → 查技术交底报告状态 > TEMPORARY_STORAGE
  │   │   │   │       └─ 其他工序 → 查验收主报告有 SUBMITTING~CANCEL 之间的记录
  │   │   │   │   └─ 报告通过 → true
  │   │   │   └─ 否 → 只走路径A
  │   │   │
  │   │   ├─ 条件满足 → true
  │   │   │
  │   │   └─ 条件不满足?
  │   │       └─ updateTaskSuspendActive(taskDispatch) → UN_ACTIVE→SUSPEND_ACTIVE
  │   │       └─ return false
  │   │
  │   ├─ 子分支②：DEPENDENT_NODE(2)
  │   │   ├─ 查路由表 n_material_route (targetCode=processCode, state=ACTIVE)
  │   │   │   → 取 sourceCode 列表（前置流程code）
  │   │   ├─ 查 task_dispatch (processCodes + projectOrderId + materialCode + supplierCode)
  │   │   │   → 取前置任务实例
  │   │   ├─ 无前置任务 → true（放行）
  │   │   ├─ 前置任务全部 FINISHED(3) → true
  │   │   └─ 存在未完成的前置任务 → false
  │   │
  │   └─ 其他模式（IMMEDIATELY 等） → true
  │
  ├─ shouldActivate 返回 false?
  │   └─ return（不激活）
  │
  └─ shouldActivate 返回 true?
      │
      └─ checkInterceptConfigure(taskDispatch) 尾款拦截检查
          │
          ├─ 跳过检查的条件（以下任一满足直接返回 checkResult=true）:
          │   ├─ mode == SD(2)（圣都）
          │   ├─ mode == XLS（轩朗仕）
          │   └─ mode == SELF_BUY（自购）
          │
          ├─ 北京 2.5 模式（processVersion + businessModel 判断）:
          │   ├─ taskType == ENTER(5) 或 INSTALL(6)?
          │   │   ├─ 查 material_payment_intercept_config:
          │   │   │   条件: materialCode + productComboId
          │   │   ├─ 有配置且 finalPaymentLimit=true?
          │   │   │   ├─ 是 → paymentManager.balancePaymentIsClosingV2()
          │   │   │   │   ├─ 尾款已结清 → checkResult=true
          │   │   │   │   └─ 尾款未结清 → checkResult=false
          │   │   │   └─ 否 → checkResult=true
          │   │   └─ 不是送货/安装任务 → checkResult=true
          │   └─ 非北京2.5 → 走到下方通用检查
          │
          ├─ 2.0 搭售模式（SDV2_0）:
          │   └─ 直接返回 checkResult=true（不拦截）
          │
          └─ 通用检查（从 Apollo 配置读取）:
              ├─ apolloConfig.getInterceptConfigure() → JSON 配置
              ├─ 配置为空? → checkResult=true
              ├─ 按 taskType 取拦截配置
              │   ├─ 无该任务类型的配置 → checkResult=true
              │   ├─ 有 notInMaterialCodes 排除列表
              │   │   ├─ 当前品类在排除列表中 → checkResult=true
              │   │   └─ 不在排除列表 → 查支付比例 checkPaymentResult()
              │   └─ 有 inMaterialCodes 限制列表
              │       ├─ 当前品类不在限制列表中 → checkResult=true
              │       └─ 在限制列表中 → 查支付比例 checkPaymentResult()
              │
              └─ checkPaymentResult():
                  ├─ paymentManager.queryTradeConfig() 查尾款交易配置
                  ├─ 无配置 → true
                  └─ 有配置 → paymentManager.balancePaymentIsClosing() 查尾款是否结清
      
      ├─ checkResult = false（被拦截）?
      │   ├─ updateTaskDispatch(taskDispatch)
      │   │   → taskDispatch.processStatus: UN_ACTIVE(1) → SUSPEND_ACTIVE(4)
      │   └─ return
      │
      └─ checkResult = true（通过）?
          └─ 进入 doActivateTaskDispatch
```

---

## 正式激活 — doActivateTaskDispatch

```
doActivateTaskDispatch(taskDispatch)
  │
  ├─ 守卫：processStatus 必须是 UN_ACTIVE(1) 或 SUSPEND_ACTIVE(4)
  │   └─ 不是 → return（幂等性保护）
  │
  ├─ 更新 TaskDispatch 主表:
  │   ├─ currentNodeTime = new Date()
  │   └─ processStatus = UNCOMPLETED(2)
  │   └─ updateByExampleSelective（带 processStatus 和 id 作为条件，乐观锁）
  │
  ├─ 查第一个未激活的节点:
  │   └─ 条件: taskDispatchId + currentNodeType + processStatus=UN_ACTIVE(1)
  │   └─ 查不到? → return（无可激活节点）
  │
  ├─ 重新查 taskDispatch（获取最新数据）
  │
  ├─ activateTaskDispatchNode(taskDispatch, node)
  │   ├─ 守卫: node 非空且 processStatus == UN_ACTIVE(1)
  │   │
  │   ├─ 更新节点:
  │   │   ├─ processStatus: UN_ACTIVE(1) → UNCOMPLETED(2)
  │   │   ├─ startTime = new Date()
  │   │   └─ gmtModified = new Date()
  │   │
  │   ├─ 重新查 taskDispatch（获取最新数据）
  │   │
  │   ├─ dispatchCreateService.completeAssignerTaskWhenActivate()
  │   │   └─ 处理派单员任务（查施工包 → 自动完成派单或原方式处理）
  │   │
  │   ├─ materialTaskProducer.publishTaskDispatchChange(taskDispatch)
  │   │   └─ → MQ: 发布任务变更消息
  │   │
  │   ├─ OMS 同步（按 mode 分支）:
  │   │   ├─ HOME2_5_MANPOWER(6) 或 HOME2_5(5)?
  │   │   │   └─ omsMessageSyncService.sendVssNew() → 旧版 VSS 接口
  │   │   └─ mode != HOME2_5(5)?
  │   │       └─ omsMessageSyncService.sendOmsMsg() → 新版 OMS 接口
  │   │
  │   ├─ messagePushClient.pushMessage() → IM/App 推送通知执行人
  │   │
  │   └─ 重新计算预计时间:
  │       └─ estimatedTimeV2Service.calculateEstimatedTime(node, FIRST_CHECK_TIME)
  │       └─ taskDispatchNodeDao.updateEstimatedTime() 更新节点时间
  │
  ├─ 排程任务且非家装任务类型?
  │   ├─ 条件: !HomeTaskList.contains(taskType) && isDelivery_Flow_MODE(mode)
  │   └─ 是 → workbenchManager.activeTask()
  │       └─ 激活工作台货单任务（ScheduleActiveParam 含项目、品类、供应商、订单号等）
  │
  └─ materialBatchV2Service.doActivateTaskBatch(taskDispatch, node)
      └─ 检查送货批次状态：是否需要创建/加入/完成批次
```

---

## 状态变更汇总

| 对象 | 变更前 | 变更后 | 触发条件 |
|------|--------|--------|---------|
| `task_dispatch.processStatus` | `UN_ACTIVE(1)` | `UNCOMPLETED(2)` | 所有检查通过，正式激活 |
| `task_dispatch.processStatus` | `UN_ACTIVE(1)` | `SUSPEND_ACTIVE(4)` | 订单状态未达标 或 尾款被拦截 |
| `task_dispatch.processStatus` | `SUSPEND_ACTIVE(4)` | `UNCOMPLETED(2)` | 拦截条件解除后重新触发激活 |
| `task_dispatch.currentNodeTime` | 旧值 | `new Date()` | 激活时设当前时间 |
| `task_dispatch_node.processStatus` | `UN_ACTIVE(1)` | `UNCOMPLETED(2)` | 激活第一个节点时 |
| `task_dispatch_node.startTime` | 初值 | `new Date()` | 激活节点时记录开始时间 |
| `task_dispatch_node.estimatedTime` | 初值 | 计算结果 | 激活后重新计算预计时间 |

---

## 数据查询汇总

| 步骤 | 表/外部服务 | 查询条件 | 用途 |
|------|------------|---------|------|
| ③ | `n_material_node_cfg` | `processCode + nodeCode=1000 + state` | 获取节点配置（激活方式） |
| ④甲-1 | `delivery_process_template`（Feign） | `projectOrderId` | 查项目排程最新模板 |
| ④甲-2 | `n_material_node_transfer_condition` | `processCode + templateId` | 查排程任务激活条件 |
| ④乙-A | 项目订单服务（Feign） | `projectOrderId` | 查订单状态 |
| ④乙-A | 验收报告服务（Feign） | `projectOrderId` | 查验收状态（北京2.5） |
| ④乙-B | `n_material_route` | `targetCode=processCode + state=ACTIVE` | 查前置路由 |
| ④乙-B | `task_dispatch` | `processCodes + projectOrderId + materialCode + supplierCode` | 查前置任务状态 |
| ⑤ | `task_dispatch_node` | `taskDispatchId + currentNodeType + UN_ACTIVE` | 找第一个节点 |
| 尾款 | `material_payment_intercept_config` | `materialCode + productComboId` | 尾款拦截配置 |
| 尾款 | 支付中心（Feign） | `projectOrderId` | 查尾款结清状态 |
| 尾款 | Apollo 配置 | `interceptConfigure` JSON key | 通用拦截规则 |

---

## 外部调用和事件汇总

| 被调系统 | 调用方式 | 用途 | 调用时机 |
|---------|---------|------|---------|
| 项目订单服务 | Feign `projectOrderManager.getProjectOrder()` | 查订单状态 | shouldActivate / PLAN_TIME |
| 验收报告服务 | Feign `acceptanceReportManager.*` | 查验收状态 | shouldActivate / 北京2.5 |
| 排程配置服务 | Feign `deliveryProcessCfgManager.*` | 查最新模板ID | getLastestConditionList |
| 支付中心 | Feign `paymentManager.*` | 查尾款结清 | checkInterceptConfigure |
| OMS 系统 | `omsMessageSyncService.*` | 同步订单履约状态 | 激活节点后 |
| 工作台系统 | `workbenchManager.activeTask()` | 激活货单任务 | 排程任务激活后 |
| IM 推送平台 | `messagePushClient.pushMessage()` | 通知执行人 | 激活节点后 |
| MQ (Kafka) | `materialTaskProducer.publishTaskDispatchChange()` | 发布任务变更事件 | 激活节点后 |

---

## 关键设计要点

| 要点 | 说明 |
|------|------|
| **幂等性** | `doActivateTaskDispatch` 检查 `processStatus` 必须是 UN_ACTIVE 或 SUSPEND_ACTIVE，已激活的不会重复处理 |
| **乐观锁** | `taskDispatchDao.updateByExampleSelective` 以当前 `processStatus` 和 `id` 作为更新条件，避免并发冲突 |
| **SUSPEND_ACTIVE 是中间态** | 被条件拦截进入挂起状态后，不意味着永远不激活——外部事件（订单状态变更、付款成功）会重新触发激活 |
| **排程/全国两套逻辑** | `mode` 决定走哪套激活判断：DELIVERY_FLOW(7) 查 `n_material_node_transfer_condition`，非排程查 `n_material_node_cfg` |
| **北京2.5 验收报告双路径** | 订单状态满足即可激活（路径A）；订单状态不满足时再查验收报告（路径B），验收报告通过也可激活 |
| **尾款拦截多层配置** | 从硬编码模式（圣都/自购跳过）→ 数据库配置（北京2.5的 `material_payment_intercept_config`）→ Apollo 配置（通用拦截规则）依次判断 |

---
## 相关文档

- [[starlord项目整理/接口逻辑梳理/主材任务（TaskDispatch）的核心业务逻辑]] — 批量创建与整体业务逻辑
- [[starlord项目整理/接口逻辑梳理/旧版激活方式方法逻辑]] — V1旧版激活逻辑对比
- [[starlord项目整理/主材任务流程梳理]] — 主材任务全流程流转图
- [[starlord项目整理/Mode配置入口完整汇总]] — Mode配置影响激活路径选择
