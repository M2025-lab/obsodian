---
title: 系统边界 — API 层
type: concept
tags: [api, feign, system-boundary, data-contract]
---

# 系统边界 — API 层

> 本文档分析 edar-starlord（家装交付中台）的 Feign API 层，明确系统对外暴露的数据接口与依赖的外部系统。

## 对外暴露的 Feign API（其他系统调我们）

所有 Feign 接口均定义在 `edar-starlord-api` 模块，`@FeignClient(value = "edar-starlord")`，由 `edar-starlord-service` 模块实现。

### 1. 任务调度域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **TaskDispatchFeign** | task-dispatch-feign | foreman-api / workbench / 供应链 | processStatus, processDetail, handle, batchHandle, taskDetail, listOwnerTask, calendarTaskList, createSingleTask 等 50+ 方法 | 主材进度状态、主材变更、任务列表、任务详情、节点处理、改派、日程、安装通知 |
| **TaskDispatchV2Feign** | task-dispatch-v2-feign | foreman-api / 作业中心 | taskDetail, pageTaskDetail, nodeCfgDetail, checkCancel, directSaveMaterialTask, searchProject(ES), searchTaskOrder(ES), activeCondition | V2 版本任务详情/分页查询、ES 搜索、排程模式激活时间计算 |
| **DispatchCommonFeign** | dispatch-common-feign | 多端通用 | installTimeLimit, changeAppoint, batchChangeAppoint, visitTimeLimit, packageAppointTimeLimit | 通知安装时间限制、任务改约、施工包预约限制 |
| **MaterialTaskFeign** 🔴 | MaterialTaskFeign | 外部系统（已废弃） | create, cancel | 创建/取消主材任务（已标记 @Deprecated） |
| **MaterialTaskBizV2Feign** | material-task-biz-feign | 作业中心首页 / C端 | recentAppointWorkerUncompleted, queryMaterialDispatchList, recheckScaleTaskPage | 预约任务列表（工人/送货/测量）、可派单任务、零售任务详情 |
| **WorkbenchTaskFeign** | WorkbenchTaskFeign | workbench | queryByTaskTypeAndProjectOrderId, cancelPlaceOrder | 工作台任务信息查询、下单任务取消 |

### 2. 送货与交付域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **MaterialDeliveryFeign** | material-delivery-feign | 供应链 / 多端 | listDeliveryTime, listBatchTask, submit, deliveryNoticeTime | 送货时间、批次任务查询/提交、送货时间同步 |
| **MaterialDeliveryV2Feign** | material-delivery-feign-v2 | 供应链 | queryBatchMaterialInfo | V2 批次物料信息查询 |
| **CategoryProcessFeign** | CategoryProcessInfoFeign | 排程 / 履约配置 | categoryProcessInfo, deliveryProcessSync, queryConfig, materialScheduleSwitch, materialDispatchSwitch | 品类流程信息、履约配置查询、排程同步、灰度开关 |
| **DeliveryFlowRuleFeign** | DeliveryFlowRuleFeign | 排程管理 | updateDeliveryFlowRuleInfo, effectDeliveryProcessCfgState | 排程规则更新、材料流程规则生效 |
| **MaterialFlowRuleQueryFeign** | material-flow-query-feign | 流程融合页面 | queryLatestCategoryIdById, queryMaterialTemplateTaskList, queryCategoryInfoByCategoryId, isSupplierExecutorByCategoryId | 品类规则查询、用工执行角色判断 |

### 3. 自检验收域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **AuditFeign** | audit-feign | 供应商 / foreman-api | auditPredicate, getAuditSupplierList, materialAuditBatchHandle, dispatchNodeReport | 审核条件、供应商验收列表、主材验收批量处理、节点报告 |
| **AcceptanceReportFeign** | acceptance-report-fegin | foreman-api / 施工包 | getAcceptanceTemplate, submitInstallerAcceptanceReport, packageAcceptanceSubmit, batchAcceptanceSubmit | 验收模板、安装自检提交/暂存/查询、施工包验收 |
| **AcceptanceReportFixFeign** | acceptance-report-fix-fegin | 数据迁移 | queryMapByRange | 验收报告数据修复 |

### 4. 安装/用工任务域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **InstallerTaskFeign** | installer-task-feign | 安装工APP | listTaskByType, completeAll, passAll, taskDetail, projectCompleteStatus, statusAmount | 安装/进场任务列表、一键完成/合格、安装自检详情 |
| **ManpowerTaskFeign** | manpower | PC后台 | queryTaskByPage, queryProjectDetail, queryTaskDetails, queryTaskLogs | 用工任务分页查询、项目详情、节点列表、日志 |
| **ManpowerTaskOrderFeign** | manpower | SDM | queryTaskOrderDetail, queryByPurchaseOrderNo, queryMeasureDetailByRecheck | 用工单详情、通过采购单号查询 |
| **ManpowerCfgFeign** | manpower | 配置后台 | templateList, templateCreate, processDefineSave, templatePublish, exportTemplateList | 用工配置模板 CRUD、流程保存/发布 |
| **ManpowerNodeCfgFeign** | manpower | 配置后台 | commonSelectOption, timeActivateOption, examineOneLevelOption, examineTwoLevelOption | 节点配置枚举、激活时间选项、考核时间选项 |
| **MaterialMeasureTaskFeign** | manpower | 测量工具 | handle, saveMaterialMeasure, queryTaskDetail, saveUsageConfirm, saveTaskSkuInfo | 测量复尺任务处理、数据保存/查询、用量确认 |
| **MaterialConstructionTaskFeign** | MaterialConstructionTaskFeign | SDM / 供应链 | create, cancel, activity, getMdmCodeMergeSwitch | 用工任务创建/取消/激活（SDM 调用） |

### 5. 模板与配置域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **MaterialTemplateFeign** | material-template-feign | 配置后台 | companySug, productComboSug, templateList, templateCreate, processDefineSave, templatePublish, queryFinalPaymentConfig | 公司/套餐/品类/供应商 sug、模板 CRUD、流程保存、尾款配置 |
| **TaskTemplateFeign** | material-task-feign | 配置后台 | createTaskTemplate, editTaskTemplate, list, listByMaterials, selectOption, relatedNode | 旧版主材任务模板 CRUD、考核时间选项、关联节点 |
| **MaterialTaskConfigFeign** | MaterialTaskConfigFeign | 履约 / 排程 | queryMaterialConfig, queryPaymentInterceptConfigure, queryTaskActivatePreCondition, queryMaterialConfigData | 任务配置信息查询、尾款拦截配置、激活前置条件、履约配置数据 |
| **MaterialScheduleFeign** | MaterialScheduleFeign | 首页日历 | getMaterialSchedule, getMaterialDailyReport | 主材日历、主材日报 |
| **StockUpCycleFeign** | stock-up-cycle-feign | 后台配置 | addStockUpCycle, statusUpdate, updateStockUpCycle, listForPage | 备货周期 CRUD |

### 6. 测量域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **MeasureApplyFeign** | startlord-measure-apply-service | 设计师端 | operate, getDetail, autoCommitMeasureApply | 测量申请单操作/详情、自动提交 |
| **MeasureApplyRangeConfigFeign** | startlord-measure-apply-range-config-service | 后台配置 | add, modify, delete, list, listAll | 测量范围配置 CRUD |
| **MeasureConfigRuleFeign** | measure-config-rule-feign | 配置后台 | queryPage, detail, checkAndSave, create, update, delete, export, queryRequirement | 测量复尺交界面配置 CRUD、导出 |
| **MeasureFormTemplateFeign** | measure-form-template-feign | 测量工具 | submit, measureDetail, update | 测量表单提交/详情/修改 |
| **MeasureFormTemplateConfigurationFeign** | measure-form-template-configuration-feign | 配置后台 | list, queryPage, submit, update, importMeasureTemplate | 测量模板配置 CRUD、导入 |

### 7. 消息同步域（第三方回调 Feign）

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **OmsMessageSyncFeign** | oms-messageSync-feign | OMS（订单管理系统） | messageSync, imageSync, delayReasonSync, acceptanceTemplateResultSync | 消息同步、图片/备注同步、延期原因同步、验收结果同步 |
| **SdmMessageSyncFeign** | sdm-message-sync-fegin | SDM（供应链配送管理） | syncOrderStatus | 同步订单状态（采购单/服务单操作） |
| **SupplierMessageSyncFeign** | supplier-messageSync-feign | 供应商系统 | receiveStoreMsg | 供应商接单/出库消息同步 |

### 8. 其他域

| Feign 接口 | contextId | 调用方系统 | 核心方法 | 传输数据简述 |
|---|---|---|---|---|
| **StarlordMetadataFeignService** | starlord-metadata | 多端公共 | queryMetaDataByCode | 元数据编码查询（任务节点类型等） |
| **MaterialButlerFeign** | material-butler-feign | 管家端 / 中控 | recentAppointUncompleted, appointWorkerTaskCount, projectAppointWorker | 预约工人任务列表、改约列表、历史预约 |
| **MaterialCustomerFeign** | material-customer-feign | C端业主 | listTaskDispatch, taskDispatchDetail | 订单主材进展列表、主材进展详情 |
| **MaterialDeliverEngineerFeign** | material-deliverEngineer-feign | 交付工程师 | listMaterialTaskDispatchNodeMeasureOrRecheck | 订单下测量/复尺主材进展 |
| **MaterialDelayApproveFeign** | material-delay-approve-feign | OA审批 | getUnApproveDelayProcessByUcId, approveDelayProcess | 延期单审批 |
| **MaterialDelayProcessFeign** | material-delay-process-feign | 多端 | queryDelaySkuList, createMaterialDelayProcess, confirmDelayProcess | 延期单 CRUD、预览、补录 |
| **DelayFeign** | delay-feign | 企微通知 | getDelayReason, getAlreadyDelay, getAboutToDelay, pushDelayTaskListByChatId | 延期原因、企微通知延期/将要延期任务 |
| **TaskProgressTipFeign** | task-progress-feign | C端 | taskCodeMapping, hasProjectTip, hasRulerTip, clearUserTip | 任务进展红点提示 |
| **ProductScheduleFeign** | product-schedule-feign | 排产 | listTask, complete | 排产任务列表、完成 |
| **HolidayCfgFeign** | holiday-cfg-feign | 后台配置 | pageSearch, save, queryById | 请假配置 CRUD |
| **ReplenishCoordinatorTaskFeign** | starlord-coordinator-task | 跟单工作台 | replenishOrderAssignmentToCoordinator, listReplenishCoordinatorTask | 返补订单分流、跟单任务列表 |
| **MaterialSelfBuyFeign** | MaterialSelfBuyFeign | C端/管家端 | getSelfBuyConfigList, getTOBSelfBuyList, addSelfBuyList, delSelfBuyList | 业主自购配置/清单 CRUD |

---

## 依赖的外部系统（我们调别人）

定义在 `edar-starlord-manager` 模块的 `feign/` 下。

| 外部系统 | FeignClient | 用途 | 关键方法 |
|---|---|---|---|
| **big-c**（大C服务） | BigCFeignService `url=${big-c.url}` | B端获取指定城市小程序开城信息 | getConfigXcxbrand |
| **ke-diting**（谛听） | DiTingFeignService `url=${ke-diting.url}` | 推送微信群消息通知（延期/告警） | sendWeChatGroupMessage |
| **wechat-message**（微信消息服务） | WechatMessageFeignService `url=${wechat.message.url}` | 发送微信消息 | sendWechatMessage |
| **search-service**（ES搜索服务-跟单） | ESTraceTaskFeignService `url=${trace.task.search.api.url}` | 查询跟单任务 ES 全量/精简信息 | searchTraceTaskFullMessage, searchTraceTaskMessage |
| **search-service**（ES搜索服务-任务单） | ESTaskOrderFeignService `url=${task.order.search.api.url}` | 查询任务单 ES 信息 | searchTaskOrderFullMessage |
| **call-service**（司南语音服务） | CallToTextNewFeignService `url=${sinan.lianjia.url}` | 语音转文字 | callToTextNew |
| **cipher-feign**（隐私加密服务） | CipherFeign `url=${privacy-data-cipher.url}` | CRM 侧手机号加密 | cipher |
| **customer-home**（客户主页服务） | CustomerHomeFeign `url=${customer-home-host}` | 获取强弱耦合城市、维护人详情、DFcode→整装订单号、服务者运营配置 | getStrongCatenaCity, getMaintainUser, getProjectOrderInfo, getOperationConfig |
| **permission-service**（权限服务） | PermissionFeignService `url=${platform.permission.host}` | 获取用户角色权限 | getRoles |
| **utopia-nrs-sales-project**（HOME 资金服务） | HomePaymentApi `value=utopia-nrs-sales-project` | 查询项目款项、存管信息、通用节点 | getProjectFund, getFundDetailInfo, getProjectEscrowBasicInfo, queryGeneralNode |
| **scm-beiwo**（供应链-北沃） | DeliveryPreAllocateFeign `url=${scm.beiwo.web.host}` | （预留接口，当前为空） | — |
| **scm-beiwo**（供应链-北沃） | OfcDispatchConfigQueryService `url=${scm.beiwo.web.host}` | 获取供应链分公司列表、预分配送货单、返补订单查询 | getBranchcompanyList, deliveryPreAllocateOrderList, queryCustomizedThirdReplenishListByBrandOrderNo |

---

## 跨系统核心 DTO

### 主材任务传输协议

| DTO | 用途 | 关键字段 |
|---|---|---|
| `TaskDispatchDetailDTO` | 任务详情（最核心 DTO） | supplierName/Code, materialName/Code, taskType, state, estimatedTime, 节点信息 |
| `TaskDispatchNodeItemDTO` | 任务节点列表项 | taskDispatchNodeId, nodeType, processStatus, executor 信息 |
| `DispatchHandleParam` | 任务处理入参 | taskDispatchNodeId, action, remarks, images |
| `MaterialProcessParam` | 进度查询参数 | projectOrderId, supplierCode, materialCode |

### 消息同步传输协议

| DTO | 来源系统 | 关键字段 |
|---|---|---|
| `OfcMessageSyncParam` | OMS→starlord | projectOrderId, orderNo, supplierCode, taskType, nodeType, arrivalTime, images |
| `SdmOrderOperationParam` | SDM→starlord | purchaseOrderNo, orderNo, operation(事件类型), operationTime, installReportSubmitParamJsonStr |
| `OmsMessageSyncParam` | OMS→starlord | taskDispatchNodeId 等 |

### 测量传输协议

| DTO | 用途 | 关键字段 |
|---|---|---|
| `MeasureApplyDTO` | 测量申请单 | projectOrderId, measureApplyId, type(签前1/签后2), measureTimeBeforeSign, rangeBeforeSign/AfterSign |
| `MaterialMeasureDTO` | 测量表单数据 | 测量属性、测量值、关联的 taskDispatchNodeId |
| `CreateMaterialMeasureParam` | 保存测量数据 | 测量值列表 |

### 验收传输协议

| DTO | 用途 | 关键字段 |
|---|---|---|
| `InstallerAcceptanceDTO` | 安装自检验收结果 | hasAcceptanceTemplate, relationId, installerAcceptanceReportDTO, remarks |
| `InstallerAcceptanceSubmitParam` | 验收提交参数 | taskDispatchNodeId, reportData, remarks |

### 关键返回值

| DTO | 用途 | 关键字段 |
|---|---|---|
| `MaterialTaskCreateResultDTO` | 任务创建结果 | fulfillmentOrderNo(履约调度单号), details(ResultDetail: fulfillmentLink, taskId, processStatus, saleType, withDelivery, workerType) |
| `ResultDTO<T>` | 统一返回包装 | code, message, data |

---

> **统计**: 46 个对外暴露的 Feign API 接口，12 个外部系统依赖。所有 Feign 接口统一注册在 `eureka` / `nacos` 注册中心，服务名 `edar-starlord`。
