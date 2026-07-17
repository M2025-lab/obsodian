---
source: code-flow
date: 2026-07-17
class: com.ke.utopia.service.listener.handler.scm.ScmMeasureApplyEventHandler
method: handleBiz
---

## 功能概述

消费来自 SCM（供应链管理）的测量申请单事件，根据测量结果生成或取消主材任务。核心能力为：收到测量申请单后，为测量结果中新增的物料创建主材任务（含下单任务和货前服务），为被取消的物料撤销对应任务。这是一个**事件驱动的异步消费者**，不是同步 API。

## 入口

- **入口方法**: `ScmMeasureApplyEventHandler#handleBiz(ScmMeasureApplyBo payloadData, String eventContextKey)`
- **触发方式**: Domain Event 框架消费，`@EventType(bizType = "measure-apply-order", serverName = "utopia-construction-scm-merchant")` — SCM 商家服务发出的测量申请单事件
- **接口**: `DomainEventHandler<ScmMeasureApplyBo>` — 领域事件处理器通用接口
- **关键注解**: 无 `@Transactional` / `@Async`

> 一句话：消费 SCM 测量申请单事件，为新增物料创建主材任务和下单任务，为取消物料撤销已有任务。

---

## handleBiz 处理逻辑

### 前置校验

- **payloadData == null**？→ return
- **addRangeList 和 cancelRangeList 都为空**？→ return
- 整个 try 块包裹，异常只打日志不抛出

> 一句话：校验事件数据是否有效，空数据直接返回，异常不中断主流程。

### 参数转换 trans()

- projectOrderId → 项目订单 ID
- ucid / username → 操作人
- orderCheck → 是否检测重复下单
- addRangeList → convertToSupplierMaterialPairs → **createList**
  - 每个元素提取：materialCode / materialName / supplierCode / supplierName / configurationId
- cancelRangeList → convertToSupplierMaterialPairs → **cancelList**

> 一句话：将事件载荷的测量申请数据转换为内部参数格式。

### createTask() — 生成货前服务任务

- **查项目订单详情**（projectOrderManager.getProjectOrder）
- **判断是否下服务单开城**（isDownServiceOrder）
  - 非北京 → true
  - 北京 + apollo 开关打开 → true
  - 北京 + 开关关 → false
- **判断是否材料进排程开城**（isMaterialSchedule）
- **未开排程？** → `createTaskOld(param, false)` 走旧逻辑生成下单任务
- **未开下服务单？** → return（北京未开城不继续）

**分布式锁**（projectOrderId 粒度，5 秒超时）

**CREATE 分支：**

- 查项目施工排期
- 对每个 createList 中的物料：
  - **检测是否已下过单**（查 ENTER / INSTALL 类型的有效任务）：已有 + orderCheck=true → 跳过
  - **创建 ProcessCreateV2Context**：
    - supportMode 优先级：HOME2.5（默认）→ 新版 V2.5 项目 → HOME2.5_MANPOWER → 排程开城 → DELIVERY_FLOW
    - logicSourceType = MEASURE_APPLY_FORM（测量申请单来源）
  - **materialCreateV2Service.createMaterialTask()** 创建主材任务
  - **新复尺打标**：判断是否新复尺流程 + 供应商执行人
    - 满足条件 → 插入 TaskDispatchExtend（bizVersion=复尺V2）
  - **双写 OMS 服务单**（createServiceOrder）

**CANCEL 分支：**

- 查需取消的任务（未激活/未完成/挂起）→ `taskDispatchCancelService.batchCancelTaskDispatch()`

> 一句话：在主流程中创建主材任务（含排程/新版判断），已开城的北京也支持。同时处理撤销逻辑。

### createOrderTask() — 生成下单任务（八合一设计/报价变更）

- **查项目订单详情**
- **下服务单未开城？** → return（北京未开城且开关未开）
- **分布式锁**（projectOrderId 粒度）
- **组装 DispatchTaskDealParam**：
  - createList → taskInitParamList（含 configurationId）
  - cancelList → taskCancelParamList
- **workbenchManager.dealDispatchTask()** 调用工作台服务处理下单任务
- **双写 OMS**：materialCreateV2Service.syncCreateOrderTask()
  - 异步执行，查询项目下全部未完成的设计&报价变更，再按物料过滤

> 一句话：调用工作台服务处理下单任务（八合一下单），并异步双写 OMS。

---

## 核心子逻辑

### createTaskOld — 旧逻辑（未开排程模式）

```
未开排程的项目，走旧逻辑生成主材任务（下单任务）
```

- **分布式锁**（projectOrderId 粒度）
- **isMaterialSchedule = true**？→ return（排程开城的跳过旧逻辑）
- 对每个 createList 中的物料：
  - 检测是否已下过单（有 ENTER / INSTALL 任务 + orderCheck=true 则跳过）
  - 有 configurationId 且非北京单子 → **跳过**（非北京单子不处理）
  - 检查 HOME2.5 配置是否存在
    - 存在 → processWithExistConfig() 创建下单任务（ORDER 类型）
    - 不存在 → 不做任何操作
- 撤销分支同 createTask

> 一句话：未开排程的项目走旧逻辑——对每个物料检查是否有 HOME2.5 配置，有就生成下单任务，有 configurationId 的非北京单子直接跳过。

### processWithExistConfig — 创建有配置的主材任务

组装 ProcessCreateV2Context（taskType=ORDER，写死生成下单任务）→ `materialCreateV2Service.createMaterialTask()` 创建主材任务

### getCancelTasks — 获取可取消的任务列表

按 (supplierCode, materialCode, businessId) 组合匹配 → 筛选 processStatus ∈ {NOT_ACTIVE, UNFINISHED, SUSPEND_ACTIVE} → 按 projectOrderId 过滤 → 返回 List<TaskDispatch>

### 新复尺打标逻辑

- 调用 materialMeasureService.judgeNewFuchiProcess() 判断是否新复尺流程
- 如果符合 + 复尺任务的 START 节点执行人为供应商 / 大宗设计师类型
- → 插入 TaskDispatchExtend，bizVersion = "复尺版本V2"

---

## 条件分支汇总

| 位置 | 条件 | 走向 | 结果 |
|------|------|------|------|
| handleBiz | payload 为空或 create/cancel 都为空 | return | 跳过处理 |
| createTask | 未开排程 | createTaskOld 旧逻辑 | 生成旧版下单任务 |
| createTask | 未开下服务单 | return | 未开城不继续 |
| createTask | 获取分布式锁失败 | return | 防并发 |
| createTask create循环 | 已有 ENTER/INSTALL 任务 + orderCheck=true | continue | 避免重复下单 |
| createTask create循环 | 新版 V2.5 项目 | supportMode=HOME2.5_MANPOWER | 新版人力模式 |
| createTask create循环 | 排程开城 | supportMode=DELIVERY_FLOW | 排程材料模式 |
| createTask create循环 | 新复尺流程 + 供应商执行人 | 插入 TaskDispatchExtend | bizVersion=复尺V2 |
| createTaskOld | isMaterialSchedule=true | return | 排程开城跳过旧逻辑 |
| createTaskOld create循环 | 已有任务 + orderCheck | continue | 防重复 |
| createTaskOld create循环 | 有 configurationId + 非北京 | continue | 非北京单不处理 |
| createTaskOld create循环 | HOME2.5 配置存在 | processWithExistConfig | 创建下单任务 |
| createOrderTask | 未开城（北京 + 开关关） | return | 不下单 |
| createOrderTask | 获取分布式锁失败 | return | 防并发 |

---

## 数据查询汇总

| 查询来源 | 查询条件 | 预期返回 | 空结果处理 |
|----------|---------|---------|-----------|
| projectOrderManager.getProjectOrder | projectOrderId | ProjectOrderDetailBO | → isDownServiceOrder=false, isMaterialSchedule 受影响 |
| businessService.queryProjectSchedule | projectOrderId | Map<String, ProjectScheduleItemBO> | → scheduleItemMap 可能为空 |
| taskDispatchDao.list(condition) | taskType=ENTER/INSTALL, projectOrderId, materialCode, supplierCode | List<TaskDispatch> | → 空：可以继续创建；非空+orderCheck：跳过 |
| materialConfigCheckService.home2_5ConfigExist | materialCode, supplierCode, mdmCode, taskType, gbCode | boolean | → false：不创建旧版任务 |
| taskDispatchDao.listByPair | supplierCode, materialCode, businessId, projectOrderId, processStatus | List<TaskDispatch> | → 空：无任务可取消 |
| taskDispatchExtDao.queryOne | taskDispatchId | TaskDispatchExtend | → null：插入新打标；非 null：不操作 |
| materialMeasureService.judgeNewFuchiProcess | projectOrder, materialCode | boolean | → false：不走新复尺打标 |

---

## 状态变更

| 属主 | 字段 | 变更前 → 变更后 | 触发条件 | 方式 |
|------|------|----------------|---------|------|
| TaskDispatchExtend | bizVersion | null → "复尺版本V2" | 新复尺流程 + 供应商执行人 | insertSelective |

其他状态变更由子调用（materialCreateV2Service.createMaterialTask、taskDispatchCancelService.batchCancelTaskDispatch）内部完成。

---

## 外部调用和事件

| 类型 | 目标 | 时机 | 用途 |
|------|------|------|------|
| Feign | projectOrderManager.getProjectOrder | createTask / createOrderTask | 查项目订单详情 |
| Feign | businessService.queryProjectSchedule | createTask | 查项目施工排期 |
| Feign | configQueryService.materialScheduleSwitch | createTask | 判断排程开城 |
| Feign | materialConfigCheckService.home2_5ConfigExist | createTaskOld | 检查 2.5 配置是否存在 |
| Feign | materialMeasureService.judgeNewFuchiProcess | createTask | 判断是否新复尺流程 |
| internal | materialCreateV2Service.createMaterialTask | createTask | 创建主材任务（核心） |
| internal | materialCreateV2Service.createServiceOrder | createTask | 双写 OMS 服务单 |
| internal | materialCreateV2Service.syncCreateOrderTask | createOrderTask | 异步双写入 OMS 下单任务 |
| Feign | workbenchManager.dealDispatchTask | createOrderTask | 八合一下单任务处理 |
| internal | taskDispatchCancelService.batchCancelTaskDispatch | createTask / createTaskOld | 批量取消任务 |
| Redis | redisService.tryGetDistributedLock | createTask / createTaskOld / createOrderTask | 分布式锁（projectOrderId 粒度） |

---

## 待澄清点

- [ ] createTask 中 isDownServiceOrder 判断：非北京直接为 true，北京需要 apollo 开关打开。如果北京开关未开且排程也未开，旧逻辑和新增逻辑都不走，测量单完全不生成任务
- [ ] createTask 中 context.setSupportMode(ModeEnum.DELIVERY_FLOW) 覆盖了前面 HOME2.5 和 HOME2.5_MANPOWER 的赋值，最终以排程判断为准
- [ ] createTaskOld 中"存在 configurationId 的非北京单子不处理"的原因不明确，可能是旧版行为兼容
- [ ] trans 方法中 formChangeParam.setUcid(formChangeParam.getUcid()) 是无效的自我赋值，应该是 payloadData.getUcid()

---

## 总结

**主干路径**: SCM 测量申请单事件 → handleBiz → trans 转换参数 → createTask（查项目 → 加锁 → 创建/取消任务 → 双写 OMS）→ createOrderTask（查项目 → 加锁 → 工作台处理下单 → 异步双写 OMS）

**架构特点**:
1. **事件驱动** — MQ 消费者模式，异步处理测量申请单结果，不阻塞上游
2. **双路径并行** — createTask（货前服务任务）和 createOrderTask（下单任务）独立运行，互不依赖
3. **分布式锁** — 以 projectOrderId 为粒度加锁，避免同一项目的并发处理
4. **模式判断链** — 根据项目区域、项目版本、排程开关等多维度判断走哪种模式（HOME2.5 / HOME2.5_MANPOWER / DELIVERY_FLOW）
5. **幂等保护** — createTask 和 createTaskOld 都检查已有任务防重复
6. **异常安全** — handleBiz 整体 try-catch，不抛异常到事件总线
