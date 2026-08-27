# MaterialHandleV2ServiceImpl#handleNode 完整业务执行说明

> 递归追踪目标函数及其整条调用链，还原"完成一个主材任务节点"背后的全部业务分支、数据流与状态流转。 所有结论均来自源码逐行核对（edar-starlord-service / edar-starlord-dao / edar-starlord-base）。

---

## 一、业务背景

贝壳/链家装修供应链的**主材任务配置与派发系统（Starlord）**中，每一项主材（瓷砖、橱柜、定制等）在不同城市、不同业务模式（被窝 BW、整装 HOME2.0、新零售 XLS、用工管理 HOME2.5 等）下，都会被拆解成一条**流程（process）**，流程由若干**任务（TaskDispatch）**组成，每个任务又由若干**节点（TaskDispatchNode）**组成（如：约工→派单→提交自检→实地验收）。

`handleNode` 就是**"作业人员在前端/B 端完成（提交）某一个节点"的统一入口**：它既要推进本地数据库里的节点/任务状态机，又要向外同步供应链（OMS/VSS/SDM）、推送 App 消息、发 Kafka 事件、联动 C 端客户。它是整个主材履约"状态驱动 + 事件外发"的中枢。

---

## 二、目标函数定位

**关键入参（DispatchHandleParam）**

- `taskDispatchNodeId`：要完成的节点 ID（与 `salesSubOrderNo` 二选一）

- `salesSubOrderNo`：零售单号（无任务实例时用来反查建任务）

- `qualified`：合格 `QualifiedEnum.QUALIFIED(1)` / 不合格 `UNQUALIFIED(2)`

- `noticeRetainTime`：预约时间（触发时间变更消息）

- `endTime` / `images` / `location` / `attachmentList` / `remarks` / `remarksFilter`

**返回值**：`Boolean`（事务是否产生了写库结果；幂等/分支提前 return 时也是 `true`）。

---

## 三、第 1 层：handleNode（本类主方法）

### 3.1 这一层负责什么

**把"一次节点提交"翻译成一整条状态推进 + 对外同步的业务流程**：先校验与分支，再完成当前节点，再决定"合格→流转下一节点 / 不合格→重启流程 / 变更单→直接完结任务"，最后更新任务主表并外发所有消息与事件。

### 3.2 这一层做了哪几件事

|     |                      |                                                                                                           |                                           |
| --- | -------------------- | --------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| 序号  | 事情                   | 负责函数 / 逻辑                                                                                                 | 业务目的                                      |
| ①   | 参数校验 + 空节点分支         | `Preconditions` + `handleWithoutTask`                                                                     | 节点ID与零售单号至少给一个；无节点ID则走零售建任务路径             |
| ②   | 查节点/任务并校验存在性         | `taskDispatchNodeDao.getById` / `taskDispatchDao.getById`                                                 | 不存在抛 `TASK_NONE_EXIST`                    |
| ③   | 零售无模板特例分支            | 行164-173 内联                                                                                               | 新零售非模板单：直接完成节点+补建默认流程任务                   |
| ④   | 幂等判断                 | 行176 `COMPLETED`                                                                                          | 已完成节点直接 return，防重复提交                      |
| ⑤   | 未激活先收口前置             | `completePreTask` + `doActivateTaskDispatch` + `completePreTaskNode`                                      | 任务未激活时，先把依赖的前置任务/前置节点自动收口并激活当前任务          |
| ⑥   | 完成当前节点               | `completeTaskDispatchNode`                                                                                | 节点置 COMPLETED，写提交人/图/附件/位置扩展              |
| ⑦   | 完成后续收尾               | 重查 + `pushMessageWhenTaskDispatchNodeProcessChange` + `publishTaskDispatchTimeChange` + `updateDelayDays` | 推 App 消息、发时间变更、算延期天数                      |
| ⑧   | 变更单直接完结              | 行207 `nodeType>=CHANGE(200)`                                                                              | 变更类节点完成=整个任务完成，不推下一节点                     |
| ⑨   | 下一节点决策               | `restartProcess`(不合格) / `getNextNodeType`+`activateNextNode`(合格)                                          | 决定流程走向                                    |
| ⑩   | 更新任务主表               | `prepareHandleDispatch` + `updateById`                                                                    | 写 currentNodeType（1000=流程走完）、可置 COMPLETED |
| ⑪   | 供应链同步                | `sendVssFinish`/`sendVssNew` 或 `sendOmsMsg`                                                               | 按业务体系分流同步用工/主材订单侧                         |
| ⑫   | Kafka 事件 + C端 + 后置激活 | `publishTaskNodeChange`/`publishTaskDispatchChange`/`publishMaterialCustomer`/`activateNextTaskDispatch`  | 节点/任务变更事件、客户完成消息、拉起后置任务                   |
| ⑬   | 事务后处理                | `afterHandle`                                                                                             | 服务单签收、Redis 进展提示、考核/计划时间重算                |

### 3.3 执行顺序（主路径）

### 3.4 分支逻辑（第1层）

|                                                 |            |                                                             |
| ----------------------------------------------- | ---------- | ----------------------------------------------------------- |
| 条件                                              | 业务含义       | 处理                                                          |
| `taskDispatchNodeId==null && salesSubOrderNo` 空 | 两个定位参数都没给  | `Preconditions` 抛 `IllegalArgumentException`                |
| `taskDispatchNodeId==null` 但零售单号有               | 零售复尺无任务实例  | 转 `handleWithoutTask` 先建任务再递归                               |
| 节点/任务查不到                                        | 数据不存在      | 抛 `TASK_NONE_EXIST`(ERROR_PARAM_ILLEGAL)                    |
| `RETAIL` 且 `processCode==""`                    | 非模板配置的新零售单 | 完成节点 + `createMaterialTaskWithDefaultParam` 补建默认任务 → return |
| 节点 `processStatus==COMPLETED(3)`                | 已提交过       | 直接 return（幂等）                                               |
| 节点 `processStatus==UN_ACTIVE(1)`                | 任务尚未激活     | 收口前置任务/节点 + 激活当前任务                                          |
| `nodeType >= CHANGE(200)`                       | 变更单节点      | 任务直接置 COMPLETED → return（不推下一节点）                            |
| `qualified==UNQUALIFIED(2)`                     | 复尺/验收不合格   | 走重启流程 `restartProcess`                                      |
| `qualified` 为合格(1)/null                         | 正常流转       | 计算下一节点并激活                                                   |
| `nextNode==null`（无下一节点）                         | 任务走到末尾     | TaskDispatch 置 COMPLETED；发 C 端客户消息；激活后置任务                   |

---

## 四、第 2 层：被 handleNode 直接调用的业务函数

### 4.1 handleWithoutTask（零售无任务补建）

**职责**：零售订单提交复尺时，系统内还没有主材任务实例，要先根据零售单反查并创建任务，再递归回到 `handleNode`。

调用链：

- `generateOrderInfo(salesSubOrderNo)`：
    
    - RPC `retailManager.batchQueryRetail` 查零售单详情；取 `HardDecorationModule.projectOrderId`；`projectOrderManager.getByProjectOrderId` 取楼盘/分公司；`scmManager.queryMerchantByMerchantId` 取分公司编码；组装 `XlsOrderInfoBo`（projectOrderId、mdmCode、materialCode、supplierCode、gbCode 等）。
    
    - 任一步查不到 → 抛 `ERROR_BUSINESS`（业务异常，阻断）。

- `taskDispatchDao.listByCondition(...RECHECK_SCALE, HOME2_5)`：已存在任务实例 → 抛"任务已存在，不能重复生成"。

- `isTemplateExist(...)`：查 `n_material_template_unit`（materialCode+supplierCode+mdmCode+mode=HOME2_5+VALID）。
    
    - **有模板** → `projectOrderManager.getProjectOrder` + `businessService.queryProjectSchedule` 取排期 → 组装 `ProcessCreateV2Context` → `materialCreateV2Service.createMaterialTask(context)` 建任务 → 查回 START 节点 → 设 `taskDispatchNodeId` → **递归 `handleNode`**。
    
    - **无模板** → `materialCreateV2Service.createMaterialTaskWithDefaultParam` 按默认参数直接建任务（节点为空，无模板流转）。

### 4.2 completePreTask（收口前置任务，私有）

**职责**：当前任务还没激活时，按流程路由把**依赖的前置任务**整体自动收口（递归向上），保证"前置环节全部完成"才推进当前任务。

逻辑：

1. 任务不是 `UN_ACTIVE(1)` → 直接 return（已激活过就不重复）。

2. `nMaterialRouteDao.queryList(targetCode=当前processCode, type=PROCESS, state=VALID)`：查"谁是我的前置流程"。
    - 空 / `sourceCode==null` → return（无前置）。

3. `taskDispatchDao.listByExample(sourceCode+projectOrderId+materialCode+supplierCode[+orderNo])`：查前置任务实例。
    - 空 → return。

4. **前置任务仍是 `UN_ACTIVE` → 递归 `completePreTask(preTaskDispatch)`**（沿路由向上一直收口）。

5. 查前置任务下所有"未完成"的节点（`processStatus != COMPLETED`），按 `startTime` 是否等于初始值(`CommonConstant.getInitDate()`) 分组：
    
    - 已开始过的节点（false 组）：直接置 `COMPLETED` + `endTime`/`submitTime`=now。
    
    - 从未开始的节点（true 组）：额外把 `startTime` 也补成 `endTime`（模拟它"此刻开始并结束"）。
    
    - 备注统一写"上游系统已对应任务"。

6. `taskDispatchDao.updateByExampleSelective` 把前置任务置 `COMPLETED` + `currentNodeType=1000`。

7. `materialTaskProducer.publishTaskNodeChange` / `publishTaskDispatchChange` 发 Kafka。

### 4.3 doActivateTaskDispatch + activateTaskDispatchNode（激活当前任务，跨服务）

**职责**：把 `UN_ACTIVE/SUSPEND_ACTIVE` 的任务**正式激活**——任务置 `UNCOMPLETED`，并把"当前节点类型"对应的第一个未激活节点置为 `UNCOMPLETED + startTime`。

- `doActivateTaskDispatch`（MaterialActivateV2ServiceImpl）：
    
    - 状态不是 1/4 → return。
    
    - 任务置 `UNCOMPLETED(2)` + `currentNodeTime=now`（按"当前 processStatus 等值"条件更新，乐观锁语义）。
    
    - 查 `currentNodeType` 对应的 `UN_ACTIVE` 节点；空 → return。
    
    - 调 `activateTaskDispatchNode`（第一个节点 UN_ACTIVE→UNCOMPLETED+startTime）。
    
    - 若 `DELIVERY_FLOW` 模式且非家居任务 → 异步 `workbenchManager.activeTask` 激活货单。
    
    - `materialBatchV2Service.doActivateTaskBatch` 激活任务批次。

- `activateTaskDispatchNode`：节点 UN_ACTIVE→UNCOMPLETED+startTime；`dispatchCreateService.completeAssignerTaskWhenActivate`（安装工派单）；`publishTaskDispatchChange`；HOME2.5 体系 `sendVssNew`、非 HOME2.5 `sendOmsMsg`；`pushMessageWhenTaskDispatchNodeProcessChange(UNCOMPLETED)`；按 `FIRST_CHECK_TIME` 重算考核时间并写库。

> 【追踪终点】`checkInterceptConfigure`（尾款拦截）、`shouldActivate`（激活条件：计划时间/节点依赖/订单状态/验收报告）、`dispatchCreateService.completeAssignerTaskWhenActivate`、`materialBatchV2Service.doActivateTaskBatch`、`workbenchManager.activeTask` 等内部逻辑属于更深的激活子系统，本分析确认其"被调用且影响激活结果"，其逐行分支不再展开（可视为激活子系统的业务叶子）。

### 4.4 completeTaskDispatchNode（完成当前节点，私有）

**职责**：把"被提交的节点"真正落库为已完成，并保存量尺/复尺表单。

1. **操作人兜底**：`operator.ucid/username` 为空时，取节点既有 `executorId/executorName`（自动完成场景）。

2. 组装更新：`processStatus=COMPLETED(3)`、`readinessStatus=READY`、`submitTime/submitBy/submitName=now`、`images`(逗号拼接)、`noticeRetainTime`、`attachment`(attachmentList 序列化为 JSON，非空才写)；`BeanUtils.copyProperties(handleParam, updateNode)` 透传其余字段。

3. `handleParam.location != null` → 写 `task_handle_extension` 表（项目/任务/节点 + 定位信息）。

4. `endTime` 为空 → 取 `submitTime`；仍空 → `new Date()`。

5. `taskDispatchNodeDao.updateById`。

6. `taskType==RECHECK_SCALE(复尺) || MEASURE(量尺)` → `saveMeasureInfo` → `measureFormTemplateService.submit`：查重（已存在直接返回），写 `measure_material_detail` + `measure_material_unit`（测量/复尺表单明细）。

### 4.5 completePreTaskNode（收口同任务前置节点，私有）

**职责**：当前节点完成时，把**同一个任务里 nodeType 小于当前节点**的所有未完成节点一并置完成（跳过被跳过的节点）。

- 查 `nodeType < 当前节点 && processStatus != COMPLETED && VALID`。

- 同样按 `startTime` 是否初始值分组批量更新为 COMPLETED（备注"上游系统已对应任务"）。

- 若有节点被收口 → `publishTaskNodeChange`（发节点完成事件）。

### 4.6 updateDelayDays / calDelayDays（延期天数，跨服务）

**职责**：节点完成后，算"实际完成时间"相对"考核时间"超了多少天（延期天数）。

- `updateDelayDays(node)`：`calDelayDays` → `taskDispatchNodeDao.updateDelayDay`。

- `calDelayDays`：
    
    - `estimatedTime`（考核时间）晚于/等于 `endTime`（实际完成）→ 返回 **0（未延期）**。
    
    - 否则 RPC `constructionManager.queryDaysBetweenTwoDateIncludeSkipDates(projectOrderId, type=2自然日, estimatedTime, endTime, skipDates)` → 返回 `intervalDays`（已扣节假日/顺延日）。

- **重要前提**：只有 `currentNewNode.getProcessCode()` 非空（即模板配置的任务）才计算延期；新零售无模板单 `processCode=""` 不计算。

### 4.7 restartProcess / generateRestartNodes（不合格→重启，跨服务）

**职责**：复尺/验收**不合格**时，根据模板配置**重新生成新一轮节点**（整轮或跨任务回退），并打回通知。

- `restartProcess` → `generateRestartNodes(taskDispatch, currentNode)`：
    
    - `processCode` 空 → return null（不走重启）。
    
    - 查模板 `materialConfigV2Service.queryAllMaterialForm`：ACTIVE → 失效 INVALID 兜底 → 删除 DELETE 兜底（反转）。
    
    - 取 `currentNodeConfig.getMaterialTaskForm().getRestartTaskType()/RestartNodeType()`；`restartTaskType==0` → return null（该节点配置为"不重启"）。
    
    - **跨任务重启**（`restartTaskType != taskType`）：查同项目同材料同供应商的"重启目标任务"实例；**若存在任一未完成 → return 空（不重启，避免打断进行中的任务）**；否则把目标任务状态回退为 `UNCOMPLETED`，按目标任务模板生成重启节点（递归 `generateRestartNodes`）。
    
    - **任务内节点重启**：`nodeTask` 字符串按 `,` 拆分，`binarySearch` 当前下标 `curIndex` 与重启下标 `restartIndex`；`curIndex<0` 或 `restartIndex>curIndex` → return null；否则生成 `restartIndex..curIndex` 区间内的节点。
    
    - 生成的节点经 `initRestartNode`（清空时间/备注、置 UN_ACTIVE、restart+1）后，由 `createRestartNodesAndCalculateEstimatedTime`：**逐条 insert**、`checkMaterialDeliveryBatch`（建批量通知送货）、`publishTaskStateChange(CREATE)`、按 `RESTART_CHECK_TIME` 重算考核时间并 `publishTaskDispatchTimeChange(ESTIMATED_TIME)`。

- 返回新节点列表，`nextNode = 第一个节点.nodeType`；随后 `pushMessageWhenRecheckRejected`（仅测量/复尺）给通知节点执行人推"复尺打回"消息。

### 4.8 NodeTypeUtil.getNextNodeType + activateNextNode（合格→流转）

**职责**：合格场景下，计算"下一个节点类型"并把该类型节点激活。

- `getNextNodeType(nodeTask, currentNode)`：
    
    - `nodeTask` 空 → null（流程到此为止）。
    
    - 按 `,` 拆成有序数组，`binarySearch` 当前节点；**是最后一个 或 找不到 → 返回 null（流程走完）**，否则返回下一个 nodeType。

- `taskDispatchNodeDao.activateNextNode(taskDispatchId, nextNode)`（DAO 层 SQL）：
    
    - `UPDATE ... SET processStatus=UNCOMPLETED(2), startTime=now WHERE taskDispatchId=? AND nodeType=? AND processStatus=UN_ACTIVE(1)`。
    
    - **乐观条件更新**：只激活"仍未激活"的同类型节点——这就是并发安全的核心（批量提交时，重复/并发提交同一节点不会出现重复激活）。

### 4.9 prepareHandleDispatch（更新任务主表，私有）

**职责**：写回任务主表的"当前节点"指针与完成状态。

- `currentNodeType = nextNode==null ? 1000 : nextNode` —— **1000 是哨兵值，表示流程已走到末尾、没有下一个节点**。

- `currentNodeTime = now`；写 `modifyBy/Name`。

- `nextNode==null` → `processStatus = COMPLETED(3)`。

- `taskDispatchDao.updateById` → 重查 `task` 供后续消息使用。

### 4.10 供应链同步：sendOmsMsg / sendVssFinish / sendVssNew（跨服务）

**职责**：把节点完成/下一节点激活同步给供应链系统。按业务体系分流：

- 条件：`ModeEnum.isHome2_5_MODE(mode)`（6/7）**或** `mode==HOME2_5(5)` → 走 VSS/SDM；否则（BW/SD/XLS/SELF_BUY 老体系）→ 走 OMS。

**sendOmsMsg(taskDispatch, currentNode, nextNode)**（老体系，主材订单侧）：

|   |   |   |
|---|---|---|
|条件|业务含义|处理|
|`nextNode==null && INSTALL && FINISH_START(80)`|安装任务收尾节点完成|`saveSendOms` 保存并发送 OMS|
|`nextNode==null && ENTER && FINISH_START(80)`|进场任务收尾节点完成|`notifyOmsTaskFinshSingle` 给主材订单发完成通知|
|`nextNode!=null`|有下一节点要启动|`startNodeType` 通知启动|

**sendVssFinish(task, currentNewNode)**（HOME2.5 用工任务完成 → VSS/SDM）：

- 非用工任务（`!getHomeTaskList`）→ return。

- `syncToSdm==DEFAULT(0)` 且 设计/报价变更 → return（兼容旧数据）。

- 节点 `processStatus` 必须 == `FINISHED(3)`（即 COMPLETED=3，数值一致）→ 否则 return。

- 执行人类型必须 == `SUPPLIER(供应商)` → 否则 return。

- `INSTALL` / `ONCE_INSTALL` → return（安装类不同步完成）。

- `OLD_VSS` → `scmSupplierManager.addOrUpdateByOrderNo`（旧用工服务单）。

- `NEW_SDM` → `scmManager.batchQuery(PO)`；**若节点不合格(UNQUALIFIED) → 直接 return（PO 挂起，不同步完成）**；否则按 PO 当前状态推进到 COMPLETE（挂起→完成；其他→待接受到完成）。

**sendVssNew(task, nextNode, handleParam)**（HOME2.5 下一节点激活 → VSS/SDM）：

- `nextNode==null` → return。

- 非用工 / `INSTALL` / `ONCE_INSTALL` → return。

- `DESIGN_REVIEW` + `HOME2_5_MANPOWER` → **必须等同项目同材料同供应商的 RECHECK_SCALE（复尺）全部完成**才推送，否则 return（隐含规则：报价变更依赖复尺）。

- 查 `getByTaskIdAndNoteType(taskId, nextNode)`；过滤未激活节点；非 SUPPLIER 执行人 → return。

- 构造 `ServiceOrderParam`（IN_SERVICE 进行中），`supplierId` 非数字 → return。

- 按 `syncToSdm` 走旧 VSS 或新 SDM 同步。

> ⚠️ **基于代码推断**：`sendVssFinish` 用 `ProcessStatusEnum.FINISHED(3)` 校验节点，而节点完成写的是 `TaskDispatchNodeStatusEnum.COMPLETED(3)`，两者数值同为 3 才得以匹配。这是"节点状态枚举"与"任务流程状态枚举"混用同一数值的隐含约定，若任一方改值会静默失效。

### 4.11 消息推送 & Kafka 生产者

- `pushMessageWhenTaskDispatchNodeProcessChange(nodeId, status)`（MessagePushClient）：
    
    - 节点为空 / **状态与传入 status 不符 → return**（保证只针对"刚变成该状态"的节点推）。
    
    - `taskExecutorHelper.awaitExecutor` 异步把消息推给作业中心。
    
    - 遍历 `TaskNodeMsgEnum` 枚举，`shouldPushMsg(taskDispatch, node)` 逐条判定（不同任务类型/节点类型/模式对应不同文案与渠道），命中则 `pushMsg`（SpEL 渲染标题/内容/跳转链接，发给 `node.executorId`）。

- `pushMessageWhenRecheckRejected`（仅 MEASURE/RECHECK_SCALE）：拼"复尺被打回"标题/内容（含驳回人角色、材料名、地址），经工作助手渠道推给通知节点执行人。

- **Kafka 生产者（MaterialTaskProducer，均为持久化发布 `eventDrivenPublisher.persistPublishMessage`）**：
    
    - `publishTaskNodeChange` → 事件 `MATERIAL_TASK_NODE_COMPLETE`；**复尺任务的零售订单在 START/NOTIFY_CAN_START 节点完成时，异步 `retailManager.syncRetail` 同步定制全链数据**。
    
    - `publishTaskDispatchChange` → `MATERIAL_TASK_DISPATCH_CHANGE`（任务维度全字段）。
    
    - `publishTaskDispatchTimeChange` → `MATERIAL_TASK_DISPATCH_TIME_CHANGE`；预约时间/考核时间为 1900 年默认值则**过滤不发**。
    
    - `publishMaterialCustomer`（`nextNode==null` 时）→ `MATERIAL_TASK_COMPLETE_CUSTOMER`：详见 4.12。

### 4.12 publishMaterialCustomer（C 端客户完成消息，跨服务）

**职责**：整个任务完成时，按供应商类型过滤后给 C 端客户推"主材进展完成"。

- `taskDispatch` / `orderNo` 空 → return。

- **供应商类型分流**（隐藏规则）：
    
    - `supplierCode` 在"库存清单" → **库存品**：仅 `ENTER(进场)` / `INSTALL(安装)` 才推，其余 return。
    
    - 否则 → **代销品**：仅 `taskValueList`（测量/接单/备货/进场/一次安装/安装）才推。

- 取完成节点图片（进场/安装取 START 节点；库存进场取 FINISH_START）、安装工姓名；`getSumTaskDispatch`（按 材料+订单+供应商 分组计数）；组装 BO 发 Kafka。

### 4.13 afterHandle（事务后处理，私有）

**职责**：事务提交后才做"副作用重算"，避免半途失败留脏数据。

1. `taskDispatch.processStatus == FINISHED(3)`（即 COMPLETED）→ `scmManager.serviceOrderStatusChange(SIGN_FOR)` 双写同步 OMS 服务单**签收**。

2. `TransactionSynchronizationManager.isActualTransactionActive()`：
    
    - 在事务中 → `registerSynchronization(afterCommit)`：**提交成功后才执行**下面三项。
    
    - 不在事务中（如 `handleWithoutTask` 递归路径）→ 直接执行。

3. afterCommit 三项：
    
    - `taskProgressTipService.addCache(projectOrderId, materialCode#supplierCode, 3)`（异步线程）：写 Redis 进展提示（见第十一节）。
    
    - `updateNodeEstime(currentNewNode)`：重算三种考核时间（FIRST_CHECK/RESTART_CHECK/PROMISE_CHECK）的**父节点**考核时间。
    
    - `materialCommonService.calculatePlanCompleteTimeByUpdate(taskDispatch)`（专用线程池 `ACTIVIE_EXECUTOR`）：重算本项目未激活任务的**计划激活时间**。

4. **复尺特殊推送**：`taskType==RECHECK_SCALE && mode==HOME2_5_MANPOWER` → 查同项目同材料同供应商的 `DESIGN_REVIEW` 任务；未完成（非 FINISHED）的逐个 `sendVssNew(taskRecheck, 60, null)`——**复尺完成才允许报价变更推给供应商**（与 4.10 的等待逻辑呼应）。

### 4.14 activateNextTaskDispatch（拉起后置任务，跨服务）

**职责**：当前任务完成（`COMPLETED`）后，沿流程路由把**后置未激活任务**自动激活。

- `taskDispatch.processStatus != FINISHED(3)` → return。

- 查路由 `sourceCode=当前processCode`（ACTIVE/INVALID）；空 → return（无后置）。

- 取 `targetCode` 列表，按 `projectOrderId+materialCode+supplierCode[+orderNo]` 查 `NOT_ACTIVE(1)` 的后置任务。

- 对每个后置任务调 `activateTaskDispatch`（通用激活，含尾款拦截 `checkInterceptConfigure`：尾款未付清 → 置 `SUSPEND_ACTIVE(4)` 不激活）。

---

## 五、第 3 层（再下钻要点，部分标记待追踪）

以下为被第 2 层调用的、对业务结果有实质影响但属于独立子系统的函数，给出其"已知业务语义"，逐行分支不再展开：

- `materialCreateV2Service.createMaterialTask` / `createMaterialTaskWithDefaultParam`：建任务实例 + 节点。 【待追踪】模板解析、节点生成、执行人分配的内部逻辑（属创建子系统）。

- `measureFormTemplateService.submit`：量尺/复尺表单落库（查重 + 明细）。

- `materialConfigV2Service.queryAllMaterialForm`：按 processCode 查模板配置（ACTIVE/INVALID/DELETE 三级兜底）。

- `materialBatchV2Service.checkMaterialDeliveryBatch`：创建/校验"批量通知送货"任务。

- `materialRouteService.calculateNodeExecutePath`：重启时按条件重算节点执行路径。

- `estimatedTimeV2Service.calculateEstimatedTime` / `updateParentNodeEstimatedTime`：考核时间计算引擎（按模板时间配置 + 订单阶段 + 节假日）。

- `projectOrderManager.*` / `retailManager.*` / `scmManager.*` / `constructionManager.*` / `workbenchManager.*`：均为**远程 RPC/Feign 调用**（订单中心、零售、供应链 SCM、工期、货单工作台）。

---

## 六、完整调用树

---

## 七、完整业务流程

---

## 八、完整业务分支矩阵

|   |   |   |   |
|---|---|---|---|
|场景|判断条件|处理逻辑|最终结果|
|参数缺失|节点ID与零售单号都空|抛 IllegalArgumentException|调用失败|
|零售无实例|节点ID空但零售单号有|handleWithoutTask 建任务后递归|新建任务并继续流转|
|数据不存在|节点/任务查不到|抛 TASK_NONE_EXIST|调用失败|
|新零售无模板|RETAIL && processCode==""|完成节点 + 建默认任务|return（无模板流转）|
|重复提交|节点已 COMPLETED|直接 return true|幂等，无副作用|
|任务未激活|节点 UN_ACTIVE|收口前置 + 激活当前任务|依赖收口，任务转 UNCOMPLETED|
|变更单完成|nodeType>=200|任务置 COMPLETED|return（不推下一节点）|
|复尺/验收不合格|qualified==UNQUALIFIED|restartProcess 重启 + 打回通知|生成新节点，流程回退|
|正常合格|qualified 合格/null|getNextNodeType + activateNextNode|下一节点激活|
|流程走完|nextNode==null|任务 COMPLETED + 发 C 端 + 激活后置|任务完结，拉起后置任务|
|库存品客户消息|supplierCode 在库存清单|仅 ENTER/INSTALL 推 C 端|其余不发|
|代销品客户消息|非库存|仅 taskValueList 内类型推|其余不发|
|报价变更推送|DESIGN_REVIEW+HOME2_5|必须等 RECHECK_SCALE 完成|未完成则跳过推送|
|尾款未付清|activateNextTaskDispatch 中 checkInterceptConfigure|后置任务置 SUSPEND_ACTIVE|暂不激活|
|节点延期|endTime > estimatedTime|calDelayDays 写入 delayDay|记录延期天数|
|异常|任意 DB/RPC 失败|整事务回滚（@Transactional）|状态不落库|

---

## 九、核心数据流

**关键数据变化点**

- `operator.ucid/username`：可能被节点 executor 覆盖（自动完成场景）。

- `currentNewNode`：每次 `updateById` 后都重新 `getById`，保证后续消息基于**最新持久化数据**（先落库再发消息的顺序约定）。

- `nextNode`：`null` 经 `prepareHandleDispatch` 转成哨兵 `1000`，并触发任务 COMPLETED。

- `processCode` 空（新零售无模板）→ 跳过延期计算、跳过模板依赖的重启判定。

---

## 十、状态流转

### 节点（TaskDispatchNode）状态

### 任务（TaskDispatch）状态

### 重启回退（跨任务）

---

## 十一、数据库 / Redis / MQ / RPC 链路

### 数据库（MyBatis，DAO 层）

|   |   |   |
|---|---|---|
|表|读/写|业务含义|
|`task_dispatch_node`|读写|节点状态机核心；`activateNextNode` 按 nodeType+UN_ACTIVE 条件更新（并发安全）|
|`task_dispatch`|读写|任务主表；currentNodeType(1000哨兵)/processStatus/currentNodeTime/planActivateTime|
|`task_handle_extension`|写|节点提交时的定位信息（有 location 才写）|
|`n_material_route`|读|流程路由（前置/后置 processCode，type=PROCESS）|
|`n_material_template_unit`|读|新零售模板是否存在（建任务前判断）|
|`measure_material_detail` / `measure_material_unit`|写|量尺/复尺表单明细|
|`material_task_node` 等关联表|读|激活/重启时查未完成节点|

> 注意：`listByExample` 等 DAO 方法内部强制追加 `state=VALID` 条件（逻辑删除）；`activateNextNode` 的更新条件是 `processStatus=UN_ACTIVE`，保证只激活仍未开始的节点。

### Redis（TaskProgressTipServiceImpl.addCache）

- **Key**：`PREFIX + projectOrderId`（Hash 结构）。

- **Field**：工长/管家/设计师/客户经理的 userId。

- **Value**：`Set<String>`，元素形如 `material#materialCode#supplierCode`（type=3）。

- **写入时机**：事务提交后异步写入；前端首页据此展示"材料进展提示"角标。

- **读取**：前端/进展提示服务读 Hash 下各 userId 的 Set。

- **数据源**：用户角色 ID 来自 `projectOrderManager.queryAssistantProjectOrder`（RPC）；查失败兜底成 foremanId=10/assistantId=20（⚠️ 兜底硬编码，异常时会把提示挂到默认账号）。

### MQ / Kafka（MaterialTaskProducer，事件驱动）

- `MATERIAL_TASK_NODE_COMPLETE`：节点完成（含零售复尺 syncRetail）。

- `MATERIAL_TASK_DISPATCH_CHANGE`：任务变更（全字段）。

- `MATERIAL_TASK_DISPATCH_TIME_CHANGE`：时间变更（预约/考核，默认值过滤）。

- `MATERIAL_TASK_COMPLETE_CUSTOMER`：C 端客户完成。

- 均采用 `eventDrivenPublisher.persistPublishMessage`（先持久化再发，保证不丢）。

### RPC / Feign（远程服务，跨系统）

|   |   |   |
|---|---|---|
|调用|服务|用途|
|`retailManager.batchQueryRetail` / `syncRetail`|零售|反查零售单 / 同步定制全链|
|`projectOrderManager.getProjectOrder/getByProjectOrderId/queryByProjectOrderId/queryAssistantProjectOrder`|订单中心|订单详情、项目排期、人员|
|`scmManager.queryMerchantByMerchantId/serviceOrderStatusChange/batchQuery/changeStatus`|供应链 SCM|分公司、服务单签收、PO 状态|
|`constructionManager.queryDaysBetweenTwoDateIncludeSkipDates`|工期|计算延期天数（扣节假日）|
|`workbenchManager.activeTask`|货单工作台|激活货单（DELIVERY_FLOW 模式）|
|`businessService.queryProjectSchedule`|业务|项目排期|

---

## 十二、隐含业务规则

1. **幂等前置**：节点已 COMPLETED 直接 return，重复提交不产生副作用。

2. **流程依赖自动收口**：当前任务未激活时，会沿 `n_material_route` 把**所有前置任务/前置节点**递归自动完成（备注"上游系统已对应任务"），再激活当前任务。

3. **哨兵值 1000**：`currentNodeType=1000` 表示任务流程已走到末尾；`nextNode==null` 同时把任务置 COMPLETED。

4. **并发安全靠条件更新**：`activateNextNode` 仅更新 `UN_ACTIVE` 的同类型节点；批量提交 5 线程并发时，重复提交同一节点不会重复激活。

5. **模板三级兜底**：重启/激活查模板时 ACTIVE→INVALID→DELETE 依次兜底，保证"失效配置也能流转"。

6. **新零售无模板单**：`processCode=""` → 不计算延期、不走模板依赖重启、节点为空无流转。

7. **库存/代销客户消息分流**：库存品只推进场/安装；代销品只推指定类型列表，其余 C 端不感知。

8. **报价变更依赖复尺**：HOME2.5 下 DESIGN_REVIEW 必须等同项目同材料同供应商的 RECHECK_SCALE 全部完成才推 SDM；复尺完成后在 afterHandle 里主动补推。

9. **节点状态枚举与流程状态枚举混用数值**：节点 COMPLETED(3) 被 `sendVssFinish` 用 `FINISHED(3)` 校验——靠两者同为 3 才匹配（脆弱约定）。

10. **先落库再发消息**：完成节点后多次 `getById` 重查，确保 Kafka/RPC 用的是最新持久化数据。

11. **重算类操作进 afterCommit**：进展提示、考核时间、计划激活时间等副作用全部注册到事务提交后执行，避免半途失败留脏数据。

12. **尾款拦截**：拉起后置任务/激活时若尾款未付清，任务置 `SUSPEND_ACTIVE(4)` 暂不激活。

13. **跨任务重启保护**：若目标任务仍有未完成实例，则不重启（避免打断进行中任务）。

---

## 十三、异常、兜底与边界情况

|   |   |   |   |
|---|---|---|---|
|情况|触发|业务含义|影响|
|参数非法|两个定位参数都空|无法定位节点|抛 IllegalArgumentException|
|数据缺失|节点/任务不存在|提交了一个不存在的节点|抛 TASK_NONE_EXIST|
|零售反查失败|零售单/订单/分公司查不到|数据链路断了|抛 ERROR_BUSINESS，阻断|
|任务已存在|零售重复提交建任务|防重复生成|抛"任务已存在"|
|节点已提交|COMPLETED|重复提交|幂等返回，无副作用|
|延期计算跳过|processCode 空|无模板配置|不写 delayDay（保持默认 0）|
|供应链同步跳过|非用工任务/非供应商执行人/安装类|不该同步的场景|return，不同步|
|报价变更跳过|复尺未完成|依赖未满足|return，不推 SDM|
|Redis 兜底|订单人员查询异常|查不到角色|写死 foreman=10/assistant=20（⚠️ 可能误挂提示）|
|供应商编码非数字|sendVssNew/sendVssFinish|脏数据|return，不同步|
|时间默认值|noticeRetainTime/estimatedTime 为 1900 年|未配置时间|时间变更消息过滤不发|
|事务回滚|任意 DB/RPC 在事务内抛异常|一致性保护|整 handleNode 回滚，状态不落库|
|重启下标非法|curIndex<0 或 restartIndex>curIndex|配置/数据异常|return null，不重启|

⚠️ **潜在风险（基于代码推断）**

- 节点状态枚举与流程状态枚举共用数值 3（`COMPLETED`/`FINISHED`），属隐式约定，任一方改值会静默破坏 `sendVssFinish` 的完成校验。

- `addCache` 在 RPC 异常时把进展提示挂到硬编码默认账号（10/20），可能导致无关人员收到提示或真实人员漏收。

- 批量 `batchHandleNode` 5 线程各自起独立事务并发提交，`activateNextNode` 的条件更新是主要并发保护；若后续新增"非条件更新"逻辑需重新评估并发安全。

---

## 十四、最终业务结论

**这个函数到底负责什么？**

`handleNode` 是主材履约系统里**"作业人员每完成一个节点"的业务中枢**。它把一次简单的"提交节点"动作，翻译成一整条**状态机推进 + 跨系统同步**的链路：

1. **状态推进（DB 内）**：当前节点 COMPLETED → 同任务下一节点（或重启节点）激活 → 任务主表 currentNodeType 推进（1000=完结）→ 必要时收口前置任务、拉起后置任务。

2. **事件外发（DB 外）**：App 推送（作业中心 + 各角色）、Kafka 节点/任务/时间/客户事件、供应链 OMS/VSS/SDM 同步、OMS 服务单签收、C 端客户进展。

3. **时间重算（afterCommit）**：延期天数、父节点考核时间、项目级计划激活时间。

**一句话**：它用"节点完成"这一个动作，驱动了整条主材任务流程的前后向联动与所有对外通知。

### 最核心的 10 条业务规则

1. **幂等**：已完成节点直接 return，重复提交零副作用。

2. **依赖自动收口**：未激活任务的提交会先递归收口所有前置任务/节点，再激活自身。

3. **哨兵 1000**：`currentNodeType=1000` = 任务流程走完，同时置 COMPLETED。

4. **合格→流转 / 不合格→重启**：`qualified` 决定走 `getNextNodeType+activateNextNode` 还是 `restartProcess`。

5. **变更单即完结**：`nodeType>=200` 的节点完成 = 整个任务完成，不推下一节点。

6. **并发靠条件更新**：`activateNextNode` 仅更新 UN_ACTIVE 同类型节点，是批量并发提交的安全基石。

7. **业务体系分流**：HOME2.5（5/6/7）走 VSS/SDM，老体系走 OMS。

8. **报价变更依赖复尺**：HOME2.5 下复尺未完成则报价变更不推 SDM，复尺完成后主动补推。

9. **库存/代销客户消息分流**：仅特定任务类型才向 C 端推送完成消息。

10. **重算进 afterCommit**：所有"重算类"副作用注册到事务提交后执行，保证一致性与不丢数据。