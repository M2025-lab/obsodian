---
title: 系统边界 — Controller 层
type: concept
tags: [controller, rest-api, system-boundary]
---

# 系统边界 — Controller 层

> edar-starlord 共 **80 个 Controller**，分布在根目录及 11 个子包中。绝大部分 Controller 通过实现 Feign 接口对外暴露 REST API。
> 系统是「家装交付中台」，核心围绕**主材任务全生命周期管理**，涵盖从测量、下单、排产、送货、安装、验收到延期的完整链路。

---

## 业务域分组

### 域 1：主材任务调度（Material Task Dispatch）
**Controller 文件：** `TaskDispatchController`, `TaskDispatchV2Controller`, `DispatchCommonController`, `TaskProgressTipController`, `CommonTaskModuleController`, `WorkbenchTaskController`

**用途：** 系统核心——主材任务的全生命周期调度引擎。处理任务进度、状态变更、处理、改约、批量操作、ES 搜索等。

**消费者：** 前端页面（材料员工作台、工长端、安装工端、跟单员工作台）、其他系统（通过 Feign 调用）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/material-task/dispatch/process-status` | 主材进度状态图 |
| POST | `/material-task/dispatch/change-status` | 主材变更状态 |
| POST | `/material-task/dispatch/process-detail` | 主材进度明细 |
| POST | `/material-task/dispatch/material-clerk/task-list` | 材料员任务列表（分页） |
| POST | `/material-task/dispatch/handle` | 主材任务处理 |
| POST | `/material-task/batch/dispatch/handle` | 主材任务批量处理 |
| POST | `/material-task/dispatch/task-detail` | 获取任务详情 |
| POST | `/material-task/dispatch/material-task-detail` | 获取主材所有任务及详情 |
| POST | `/material-task/dispatch/material-task-order-detail` | 主材订单维度详情 |
| POST | `/material-task/dispatch/measure-reject-detail` | 测量/复尺驳回详情 |
| POST | `/material-task/dispatch/package-construction-handle` | 施工包维度处理主材 |
| POST | `/material-task/dispatch/batch-handle` | 合并主材任务处理 |
| POST | `/material-task/dispatch/completeOrderTask` | 完成下单任务 |
| POST | `/material-task/dispatch/reassignExecute` | 改派接口 |
| POST | `/dispatch/common/change-appoint` | 主材任务改约 |
| POST | `/dispatch/common/visit-time-limit` | 通知安装上门时间限制 |
| POST | `/dispatch/common/batch-change-appoint` | 批量改约 |
| GET  | `/material-task/dispatch/material-progress` | 订单下主材任务进展 |
| GET  | `/material-task/dispatch/list-task-node` | 已激活待完成主材任务结点 |
| POST | `/material-task/dispatch/create-single-task` | 单接口创建任务 |
| POST | `/material-task/v2/task-detail` | V2 任务详情 |
| POST | `/material-task/v2/save` | 直接保存主材任务 |
| POST | `/material-task/dispatch/design-review/submit` | 提交设计复核任务 |
| POST | `/material-task/measure-form-change` | 测量申请单变更触发主材任务 |
| GET  | `/material-task/dispatch/measure-module` | 测量模块任务 |
| GET  | `/material-task/workbench/query/projectOrderIdAndType` | 工作台主材任务信息 |
| GET  | `/dispatch/common/query-node-list` | 主材任务节点通用查询 |
| GET  | `/api/common-module/get-measure-module` | 通用-测量模块 |
| GET  | `/api/common-module/get-material-delivery-module` | 通用-送货模块 |

**核心 DTO：** `MaterialProcessParam`, `DispatchHandleParam`, `TaskDispatchDetailParam`, `TaskDispatchNodeItemDTO`, `ChangeAppointParam`


### 域 2：主材任务业务（Material Task Biz V2）
**Controller 文件：** `MaterialTaskBizV2Controller`

**用途：** 面向 B 端用户的业务级任务视图——近期待完成预约任务、可派单任务列表、复尺任务、销售首页卡片。

**消费者：** B 端前端（管家端、销售端首页）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/material-biz/recent-appoint-worker-uncompleted-list` | 预约工人-近三天待完成 |
| GET | `/material-biz/recent-appoint-delivery-uncompleted-list` | 预约送货-近三天待完成 |
| GET | `/material-biz/recent-appoint-measure-uncompleted-list` | 测量任务-近三天待完成 |
| GET | `/material-biz/recent-appoint-order-uncompleted-list-new` | 下单任务-近三天待完成 |
| POST | `/material-biz/dispatch-list` | 可派单主材任务列表 |
| GET | `/material-biz/recheck-scale-task-page` | 复尺任务分页列表 |
| GET | `/material-biz/retail-material-task-detail` | 零售主材任务详情 |
| GET | `/material-biz/query-home-count` | 查询主材任务数量（首页卡片） |

**核心 DTO：** `ProjectTaskCardInfo`, `MaterialDispatchDetailDTO`, `MaterialTaskBizParam`, `SaleHomeCard`


### 域 3：主材任务模板与配置（Template & Config）
**Controller 文件：** `TaskTemplateController`, `MaterialTemplateV2Controller`, `MaterialTaskConfigController`, `MaterialTaskOpenController`

**用途：** 主材任务模板的 CRUD、流程定义、考核时间配置、激活条件配置、尾款拦截配置。运营后台核心配置能力。

**消费者：** 内部管理（运营后台）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/material-task/template/create` | 任务模版创建 |
| POST | `/material-task/template/edit` | 任务模版编辑 |
| POST | `/material-task/template/delete` | 任务模版删除 |
| POST | `/material-task/template/list` | 任务模版列表 |
| POST | `/material-task/template/supplier-list` | 供应商列表选择 |
| POST | `/material-task/template/examine-one-option` | 考核时间一级选项 |
| POST | `/material-task/template/examine-two-option` | 考核时间二级选项 |
| POST | `/material-task/config/template-list` | 配置模板分页列表 |
| POST | `/material-task/config/template-create` | 配置模板创建 |
| POST | `/material-task/config/process-define-save` | 流程图保存 |
| POST | `/material-task/config/template-publish` | 模板生效发布 |
| POST | `/material-task/config/final-payment/query` | 尾款配置查询 |
| POST | `/material-task/config/final-payment/save` | 尾款配置保存 |
| POST | `/api/config/query` | 查询主材任务配置信息 |
| GET  | `/api/config/measure/query` | 获取测量申请单配置 |
| POST | `/api/config/task/query-task-activate-pre-condition` | 查询任务激活前置条件 |
| POST | `/material-task/template/queryActivateLevelDropDownList` | 激活列表下拉选项 |

**核心 DTO：** `MaterialTaskAddParam`, `ListTemplateParam`, `TemplateCreateParam`, `TemplateListDTO`, `MaterialTaskConfigDTO`


### 域 4：主材交付/送货（Material Delivery）
**Controller 文件：** `MaterialDeliveryController`, `MaterialDeliveryV2Controller`

**用途：** 批次送货任务管理——送货时间查询、批次提交/更新、预计时间查询、送货通知同步。

**消费者：** 前端页面、供应链系统（送货时间同步由供应链调用）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/material-delivery/first-second-time` | 一次、二次送货时间 |
| POST | `/material-delivery/batch-task/list` | 批次任务查询 |
| GET  | `/material-delivery/batch-task/detail` | 批次任务详情 |
| POST | `/material-delivery/batch-task/submit` | 批次任务信息提交 |
| POST | `/material-delivery/notice-time/sync` | 送货时间同步（供应链调用） |
| POST | `/batch/material/list` | V2 批次物料列表 |
| POST | `/batch/material/submit` | V2 批次物料提交 |
| POST | `/batch/material/update` | V2 批次物料更新 |
| POST | `/batch/material/expect_time/query` | 预计时间查询 |
| GET  | `/batch/material/detail` | 批次物料详情 |
| GET  | `/batch/material/info` | 批次物料基本信息 |
| GET  | `/batch/material/list/need_arrival_notify` | 需要到货通知的批次 |

**核心 DTO：** `DeliveryDateTimeDTO`, `TaskProcessBatchQueryParam`, `SubmitBatchMaterialTimeParam`, `QueryBatchMaterialListDTO`


### 域 5：主材进度/日历（Material Schedule）
**Controller 文件：** `MaterialScheduleController`

**用途：** 主材日历视图和日报，供运营/管家查看整体主材进度。

**消费者：** 前端页面

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/material-biz/get-material-schedule` | 获取主材日历 |
| GET | `/material-biz/get-material-dailyReport` | 获取主材日报 |

**核心 DTO：** `MaterialScheduleReq`, `MaterialScheduleResDTO`, `MaterialDailyReportDTO`


### 域 6：安装工任务（Installer Task）
**Controller 文件：** `InstallerTaskController`, `foreman/MaterialTaskInstallerTaskController`

**用途：** 安装工/工长侧——安装/进场任务列表、任务详情、一键完成、一键合格、自检提交、合并安装任务。

**消费者：** 安装工/工长前端（foreman-api）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/installer-task/list-install-task` | 获取安装/进场任务列表 |
| POST | `/installer-task/complete-all` | 一键完成进场任务 |
| POST | `/installer-task/pass-all` | 一键合格 |
| GET  | `/installer-task/list-task` | 分页获取任务列表 |
| GET  | `/installer-task/detail` | 获取任务详情 |
| GET  | `/installer-task/acceptance-detail` | 获取安装任务及自检信息 |
| GET  | `/installer-task/status-amount` | 获取完成状态数量统计 |
| GET  | `/installer-task/install-type-amount` | 获取类型分类数量 |
| GET  | `/installer-task/combine-install-task` | 合并任务列表 |
| GET  | `/api/material-task/installer-task/list-install-task` | 工长端-安装任务 |
| POST | `/api/material-task/installer-task/complete-all` | 工长端-一键完成 |
| GET  | `/api/material-task/installer-task/detail` | 工长端-任务详情 |
| POST | `/api/material-task/dispatch/handle` | 工长端-派单处理 |
| POST | `/api/material-task/dispatch/task-detail` | 工长端-任务详情 |
| POST | `/api/material-task/dispatch/material-task-detail` | 工长端-主材任务详情 |
| POST | `/api/material-task/dispatch/batch-handle` | 工长端-批量处理 |
| POST | `/api/material-task/dispatch/package-construction-handle` | 工长端-施工包处理 |

**核心 DTO：** `ListInstallParam`, `TaskInstallDTO`, `TaskDetailDTO`, `CompleteAllTaskParam`, `DispatchHandleParam`


### 域 7：管家端（Butler）
**Controller 文件：** `butler/StarlordController`, `butler/MaterialTaskController`

**用途：** 管家视角——预约工人任务列表、可改约任务、测量预约、设计复核、自检验收。

**消费者：** 管家前端

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/material-task/dispatch/list-task-node` | 已激活待完成任务结点 |
| GET  | `/api/material-task/dispatch/material-delivery/recent-tasks` | 送货最近任务 |
| POST | `/api/material-task/page/list-material-task` | 分页物料任务 |
| GET  | `/api/material-task/measure/query-appointment-info` | 测量预约信息 |
| POST | `/api/material-task/measure/batch-submit` | 批量提交测量 |
| POST | `/api/material-task/measure/batch-change-appoint` | 批量改约测量 |
| POST | `/api/material-task/design-review/submit` | 设计复核提交 |
| GET  | `/api/material/self-check/acceptance-query` | 自检验收查询 |
| POST | `/api/material/self-check/acceptance-pass` | 自检通过 |
| POST | `/api/material/self-check/acceptance-fail` | 自检不通过 |

**核心 DTO：** `MaterialTaskNodeItemParam`, `MeasureFormSubmitParam`, `DesignReviewSubmitParam`


### 域 8：业主端（Customer）
**Controller 文件：** `MaterialCustomerController`

**用途：** C 端业主查看订单下主材进展和详情。

**消费者：** 业主端前端（C 端）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/material-customer/dispatch/list-task-dispatch` | 获取订单下主材进展列表 |
| GET | `/material-customer/dispatch/list-task-dispatch/detail` | 主材进展列表（带详情） |
| GET | `/material-customer/dispatch/task-dispatch-detail` | 主材进展详情 |

**核心 DTO：** `MaterialTaskDispatchItemDTO`, `MaterialTaskDispatchInfoDTO`


### 域 9：交付工程师（Delivery Engineer）
**Controller 文件：** `MaterialDeliverEngineerController`

**用途：** 交付工程师视角查看测量/复尺相关主材任务。

**消费者：** 交付工程师前端

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/material-deliver-engineer/dispatch/list-task-dispatch/measure-or-recheack` | 订单下测量/复尺主材进展 |

**核心 DTO：** `TaskDispatchNodeItemDTO`


### 域 10：自检验收（Self-check & Acceptance）
**Controller 文件：** `AcceptanceReportController`, `AuditController`, `transfer/AcceptanceReportFixController`

**用途：** 验收报告模板管理、安装工自检提交/暂存、验收审核、施工包申请验收。支持有模板和无模板两种处理路径。

**消费者：** 前端（安装工、工长、审核员）、Acceptance 服务

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/acceptance-report/acceptance-template` | 获取验收模板 |
| POST | `/acceptance-report/installer/acceptance-submit` | 安装自检提交/暂存 |
| GET  | `/acceptance-report/installer/acceptance-template-query` | 安装自检去处理查询 |
| GET  | `/acceptance-report/installer/acceptance-info` | 安装自检已提交查询 |
| POST | `/acceptance-report/batch-acceptance-submit` | 合并安装自检提交 |
| POST | `/acceptance-report/package-acceptance-submit` | 施工包申请验收 |
| POST | `/audit/audit-predicate` | 获取审核条件信息 |
| POST | `/audit/supplier-list` | 供应商验收列表 |
| POST | `/audit/material/batch-handle` | 主材验收批量处理 |
| POST | `/audit/material/package-construction-handle` | 施工包维度验收处理 |

**核心 DTO：** `InstallerAcceptanceSubmitParam`, `AcceptanceSubmitParam`, `AuditMaterialParam`, `DispatchNodeAcceptanceParam`


### 域 11：延期管理（Delay Management）
**Controller 文件：** `DelayController`, `MaterialDelayProcessController`, `MaterialDelayApproveController`

**用途：** 延期原因查询、延期单创建/修改/删除/审批、延期补录、企微通知。

**消费者：** 前端页面、企业微信 Bot

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/delay/delay-reason` | 获取延期原因标签 |
| GET  | `/delay/delay-task` | 企微通知已延期任务 |
| GET  | `/delay/about-to-delay-task` | 企微通知将要延期任务 |
| GET  | `material-delay-process/delay-sku-list` | 查询 SKU 延期列表 |
| POST | `material-delay-process/create-material-delay-process` | 创建延期单 |
| GET  | `material-delay-process/detail` | 延期单详情 |
| POST | `material-delay-process/update` | 延期单修改 |
| GET  | `material-delay-process/delete` | 延期单删除 |
| POST | `material-delay-process/confirm` | 确认延期单 |
| GET  | `material-delay/approve/getUnApproveDelayProcessByUcId` | 获取待审批延期单 |
| POST | `material-delay/approve/approveDelayProcess` | 审批延期单 |

**核心 DTO：** `DelayReasonParam`, `MaterialDelayProcessCreateParam`, `MaterialDelayProcessDTO`, `MaterialDelayApproveParam`


### 域 12：用工管理（Manpower）
**Controller 文件：** `manpower/ManpowerTaskController`, `manpower/ManpowerCfgController`, `manpower/ManpowerNodeCfgController`, `manpower/ManpowerTaskOrderController`, `manpower/MaterialMeasureTaskController`

**用途：** 用工任务的分页查询、详情、日志；用工配置的模板 CRUD、流程定义、生效发布；测量任务创建。

**消费者：** 内部管理（人力调度后台）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/manpower/task/queryTaskByPage` | 分页查询用工任务 |
| GET  | `/manpower/task/queryProjectDetail` | 查询项目详情 |
| GET  | `/manpower/task/queryTaskDetails` | 查询用工任务节点列表 |
| GET  | `/manpower/task/queryTaskLogs` | 查询用工任务日志 |
| POST | `/manpower/cfg/templateList` | 分页查询用工设置列表 |
| POST | `/manpower/cfg/templateCreate` | 用工配置创建 |
| POST | `/manpower/cfg/processDefineSave` | 用工流程保存 |
| POST | `/manpower/cfg/templatePublish` | 用工配置模板生效 |
| POST | `/manpower/cfg/templateDelete` | 用工配置模板删除 |
| POST | `/manpower/cfg/exportTemplateList` | 用工配置信息导出 |
| GET  | `/manpower/cfg/roleTypeList` | 考核角色查询 |
| POST | `/manpower/cfg/workTypeList` | 作业工种查询 |
| POST | `/manpower/task/measureApply` | 测量申请触发用工任务 |
| POST | `/manpower/task/invokeRecheckAgain` | 再次复尺触发用工任务 |
| POST | `/manpower/task/createByDelivery` | 交付触发创建用工任务 |

**核心 DTO：** `PcTaskQueryParam`, `ManpowerTaskDTO`, `TemplateCreateParam`, `TemplateListDTO`, `ManpowerTaskDetailVO`


### 域 13：测量申请单（Measure Apply）
**Controller 文件：** `MeasureApplyController`, `MeasureFormTemplateController`, `MeasureFormTemplateConfigurationController`

**用途：** 测量申请单的操作（提交/查询/自动提交）、测量表单数据的提交/查询/修改、表单模板配置。

**消费者：** 设计师前端

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/designer/measure-apply/operate` | 操作测量申请单 |
| POST | `/api/designer/measure-apply/detail` | 获取测量申请单信息 |
| POST | `/api/designer/measure-apply/autoCommitMeasureApply` | 自动提交测量申请单 |
| POST | `/measure/form/submit` | 测量表单提交 |
| POST | `/measure/form/get` | 获取测量表单详情 |
| POST | `/measure/form/get-v2` | V2 获取测量表单详情 |
| POST | `/measure/form/update` | 测量表单修改 |

**核心 DTO：** `MeasureApplyOperateParam`, `MeasureApplyDTO`, `MeasureFormSubmitParam`, `MaterialMeasureDTO`


### 域 14：测量交界面配置（Measure Config Rule）
**Controller 文件：** `MeasureConfigRuleController`, `MeasureConfigRuleExportController`, `MeasureApplyRangeConfigController`

**用途：** 测量复尺交界面规则配置（CRUD、导出、校验、应用层查询）、测量范围配置（增删改查）。

**消费者：** 内部管理（运营后台）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/material-measure/interface/config/list` | 分页查询配置列表 |
| GET  | `/material-measure/interface/config/info` | 配置详情 |
| POST | `/material-measure/interface/config/save` | 新增配置 |
| POST | `/material-measure/interface/config/update` | 更新配置 |
| POST | `/material-measure/interface/config/delete` | 删除配置 |
| POST | `/material-measure/interface/config/export` | 导出配置 |
| POST | `/material-measure/interface/config/export-file` | 流式导出配置 |
| POST | `/material-measure/interface/config/query/requirement` | 应用层查询交界面要求 |
| POST | `/api/designer/measure-apply-range-config/add` | 新增测量范围配置 |
| POST | `/api/designer/measure-apply-range-config/list` | 分页查询测量范围配置 |

**核心 DTO：** `MeasureConfigRuleQueryApiParam`, `MeasureConfigRuleSaveApiParam`, `MeasureConfigRuleApiDTO`, `MeasureApplyRangeConfigQueryParam`


### 域 15：材料流程 — 供应链履约（Material Flow）
**Controller 文件：** `materialflow/MaterialFlowCategoryController`, `materialflow/MaterialFlowRuleInfoController`, `materialflow/MaterialFlowRuleQueryController`, `materialflow/MaterialFlowQueryController`, `materialflow/MaterialFlowProcessController`, `materialflow/MaterialFlowSelectQueryController`

**用途：** 供应链履约流程管理——品类规则配置（CRUD/生效/失效/复制）、规则查询、流程信息、筛选选项（公司/物料/供应商/产品/商户/销售类型）。

**消费者：** 内部管理（运营后台）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/material/flow/category/query/list/bycondition` | 品类规则查询列表 |
| GET  | `/material/flow/category/query/info` | 品类规则详情 |
| POST | `/material/flow/category/save` | 品类规则保存 |
| POST | `/material/flow/category/copy` | 品类规则复制 |
| GET  | `/material/flow/category/delete` | 品类规则删除 |
| GET  | `/material/flow/category/expire` | 品类规则失效 |
| POST | `/material/flow/rule/query/list` | 流程规则列表 |
| GET  | `/material/flow/rule/info` | 流程规则详情 |
| POST | `/material/flow/rule/save` | 流程规则保存 |
| POST | `/material/flow/process/task/info` | 流程处理任务信息 |
| POST | `/material/flow/process/task/save` | 流程处理任务保存 |
| GET  | `/material/flow/query/selected/material` | 筛选-物料 |
| POST | `/material/flow/query/selected/supplier` | 筛选-供应商 |
| POST | `/material/flow/query/selected/product` | 筛选-产品 |

**核心 DTO：** `MaterialFlowCategoryQueryParam`, `MaterialFlowCategoryInfoDTO`, `MaterialFlowRuleSaveParam`


### 域 16：交付流程 — 排程交付（Delivery Flow）
**Controller 文件：** `deliveryflow/DeliveryFlowCategoryController`, `deliveryflow/DeliveryFlowRuleInfoController`, `deliveryflow/DeliveryFlowSelectQueryController`, `deliveryflow/DeliveryFlowTaskController`, `deliveryflow/CategoryProcessController`, `deliveryflow/MaterialTaskProcessTemplateController`

**用途：** 排程交付流程配置——品类规则（CRUD/生效/失效/复制/供应商更新）、排程规则、履约配置查询、流程信息、筛选选项。

**消费者：** 内部管理（运营后台）

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/delivery/flow/category/query/list/bycondition` | 品类规则查询列表 |
| GET  | `/delivery/flow/category/query/info` | 品类规则详情 |
| POST | `/delivery/flow/category/save` | 品类规则保存 |
| POST | `/delivery/flow/category/copy` | 品类规则复制 |
| POST | `/delivery/flow/category/supplier/update` | 品类供应商更新 |
| POST | `/delivery/flow/rule/query/list` | 排程规则列表 |
| GET  | `/delivery/flow/rule/info` | 排程规则详情 |
| POST | `/delivery/flow/rule/save` | 排程规则保存 |
| POST | `/delivery/flow/rule/check/save` | 排程规则校验保存 |
| POST | `/delivery/flow/rule/send/push` | 排程规则推送 |
| POST | `/category/process/info` | 品类流程完整信息 |
| POST | `/delivery/process/query` | 履约配置查询 |
| POST | `/delivery/process/sync` | 排程信息同步 |
| POST | `/api/config/delivery/flow/calculatePlanActiveTime` | 计算计划激活时间 |
| POST | `/category/process/route/save` | 品类流程路由保存 |
| GET  | `/category/process/define/list` | 品类流程定义列表 |
| POST | `/delivery/flow/query/selected/companySug` | 筛选-分公司 |
| POST | `/delivery/flow/query/selected/supplier` | 筛选-供应商 |
| POST | `/delivery/flow/query/selected/activate/condition` | 筛选-激活条件 |
| POST | `/task/process/template/list` | 任务流程模板列表 |

**核心 DTO：** `DeliveryFlowCategoryQueryParam`, `DeliveryFlowRuleSaveParam`, `DeliveryFlowCategoryInfoDTO`, `CategoryProcessRequestParam`, `ConfigQueryParam`


### 域 17：跟单/协调员（Coordinator）
**Controller 文件：** `CoordinatorPlatformController`, `coordinator/ReplenishCoordinatorTaskController`

**用途：** 跟单员工作台——ES 检索任务列表、返补订单分流分配、返补单跟单任务查询、品牌订单查询、交付信息查询。

**消费者：** 跟单员前端

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/coordinator/platform/task/list-es-content` | 跟单工作台-任务ES检索 |
| POST | `/coordinator/platform/retail/list-es-content` | 跟单工作台-零售ES检索 |
| POST | `/coordinator/platform/retail/trigger-es-resync` | 触发零售跟单ES重新同步 |
| POST | `/dispatch/replenish-order/assignment-to-coordinator` | 返补订单分流分配 |
| POST | `/dispatch/replenish-order/list-task-by-material` | 查询返补单跟单任务 |
| GET  | `/dispatch/re-procurement/check/brand-sub-order` | 返补-品牌子单校验 |
| POST | `/dispatch/re-procurement/place-order` | 返补-下单 |
| POST | `/dispatch/re-procurement/plan-pickup-time` | 返补-计划取件时间 |
| POST | `/api/order/pre-allocate/list` | 预分配列表 |
| GET  | `/api/trace/delivery-info/query` | 交付信息查询 |

**核心 DTO：** `TraceTaskEsContentQueryDto`, `CoordinatorTaskEsContentDto`, `ReplenishOrderAssignmentParam`, `ReplenishOrderDetailDto`


### 域 18：排产（Product Schedule）
**Controller 文件：** `ProductScheduleController`

**用途：** 排产任务列表查询和完成操作。

**消费者：** 前端页面

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/product-schedule/list` | 获得所有排产任务列表 |
| POST | `/product-schedule/complete` | 完成排产任务 |

**核心 DTO：** `ProductScheduleListParam`, `ProductScheduleDTO`


### 域 19：业主自购（Self-buy）
**Controller 文件：** `selfbuy/MaterialSelfBuyController`

**用途：** 业主自购材料配置的增删查。

**消费者：** 前端页面

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/selfBuy/config/list` | 获取业主自购配置列表 |
| GET  | `/selfBuy/tob/list` | 获取业主自购清单（B端） |
| POST | `/selfBuy/add` | 添加自购材料 |
| POST | `/selfBuy/del` | 删除自购材料 |

**核心 DTO：** `MaterialSelfBuyAddDTO`, `MaterialSelfBuyListDTO`


### 域 20：施工任务（Construction Task）
**Controller 文件：** `MaterialConstructionTaskController`

**用途：** 主材施工任务的创建/取消/激活，供 SDM 及外部系统调用。

**消费者：** SDM 系统、外部系统

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/construction/task/create` | 创建主材施工任务 |
| POST | `/api/construction/task/cancel` | 批量取消主材任务 |
| GET  | `/api/construction/task/sdm/create` | SDM 创建增量任务 |
| GET  | `/api/construction/task/sdm/cancel` | SDM 取消任务 |
| POST | `/api/construction/task/activity` | 激活主材任务 |

**核心 DTO：** `MaterialConstructionTaskCreateParam`, `MaterialConstructionTaskCancelParam`


### 域 21：消息同步（Message Sync）
**Controller 文件：** `SdmMessageSyncController`, `OmsMessageSyncController`, `SupplierMessageSyncController`

**用途：** 接收外部系统的消息同步——SDM 订单状态、OMS 图片/备注/延期/验收结果、供应商消息。

**消费者：** SDM 系统、OMS 系统、供应链系统

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/starlord/sdm/status/sync` | SDM 同步订单状态 |
| POST | `/installer-task/message-sync` | OMS 消息同步 |
| POST | `/installer-task/image-sync` | OMS 同步图片和备注 |
| POST | `/installer-task/delay-reason-sync` | OMS 同步延期原因 |
| POST | `/installer-task/acceptance-template-result-sync` | OMS 同步验收模板结果 |
| POST | `/supplier-task/message-sync` | 供应商消息同步 |

**核心 DTO：** `SdmOrderOperationParam`, `OmsMessageSyncParam`, `OfcMessageSyncParam`


### 域 22：任务跟踪（Task Trace）
**Controller 文件：** `trace/DispatchTaskTraceController`

**用途：** 任务进度跟踪——项目任务节点进度查询、批量提交、数据下载。

**消费者：** 前端页面、内部管理

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/dispatchTask/trace/queryProjectTaskNodeProgress` | 查询项目任务节点进度 |
| POST | `/dispatchTask/trace/batchSubmit` | 批量提交 |
| POST | `/dispatchTask/trace/download` | 下载跟踪数据 |

**核心 DTO：** `DispatchTaskTraceDTO`


### 域 23：配置与元数据（Config & Metadata）
**Controller 文件：** `StarlordConfigController`, `StarlordMetadataController`, `HolidayCfgController`, `StockUpCycleController`, `MainOrderNoChangeController`, `TaskMaterialRulerController`

**用途：** 城市安装规则配置、系统元数据查询导览、节假日配置、备货周期配置、主订单号变更、物料标尺。

**消费者：** 内部管理、前端页面

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/starlord/config/cityInstallWorkerRule` | 城市安装工人规则 |
| GET  | `/api/task/material-ruler/list-completed-material-ruler` | 已完成物料标尺 |
| GET  | `/api/task/material-ruler/get-tech-disclosure-material-ruler` | 技术交底物料标尺 |

**核心 DTO：** 各类 Config DTO


### 域 24：通话记录（Call Record）
**Controller 文件：** `CallRecordController`

**用途：** 通话记录的查询和语音播放。

**消费者：** 前端页面

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/callRecord/queryCallRecordVoice` | 查询通话录音 |
| POST | `/callRecord/list` | 通话记录列表 |
| GET  | `/callRecord/getCallRecord` | 获取单条通话记录 |


### 域 25：巡检与数据修复（Inspection & Data Fix）
**Controller 文件：** `StarlordInspectionController`, `RefreshDataController`, `MaterialMigrationV2Controller`, `fix/MeasureFormFixController`, `fix/MeasureConfigRuleMigrationController`, `fix/EventSubExtFixController`

**用途：** 订单全量/增量刷新、数据补偿修复、模板刷新、ES 同步、V1→V2 迁移、事件重试。

**消费者：** 内部运维

**核心端点（代表性）：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/starlord/refreshAll` | 全量刷新订单 |
| GET  | `/starlord/refreshByTaskType` | 按任务类型刷新 |
| POST | `/starlord/inspection` | 巡检任务 |
| POST | `/task-dispatch/refresh` | 刷新任务调度数据 |
| POST | `/task-dispatch/compensation/active-time` | 补偿激活时间 |
| POST | `/task-dispatch/compensation/push-enter-message` | 补偿推送进场消息 |
| GET  | `/migrationPurchaseOrder` | 迁移采购订单 |
| GET  | `/refresh-data/task-dispatch/complet-order` | 完成订单数据修复 |
| POST | `/event_pub/ext/send/retry` | 事件重试 |


### 域 26：工具与测试（Tools & Test）
**Controller 文件：** `ToolsController`, `TestController`, `BackDoorController`, `WelcomeController`, `BaseController`, `mcp/McpTestController`

**用途：** 运维工具（批量更新任务/节点/模板）、测试接口、后门操作、MCP 测试。

**消费者：** 内部开发/运维

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/tools/updateTaskById` | 按ID更新任务 |
| POST | `/tools/updateTaskNodeById` | 按ID更新任务节点 |
| POST | `/tools/delProcessByProcessCodeList` | 按流程编码删除 |
| GET  | `/mcp/query/match/task` | MCP 查询匹配任务 |
| GET  | `/mcp/query/match/task/detail` | MCP 查询任务详情 |
| GET  | `/mcp/query/match/task/process` | MCP 查询任务流程 |


### 域 27：外部 AI 对接（XCS — 小材神）
**Controller 文件：** `XcsController`

**用途：** 接收 AI 识别的延期原因恢复数据。

**消费者：** 小材神 AI 服务

**核心端点：**

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/xcs/delay-reason-recovery` | 延期原因恢复 |


---

## 端点总结

| 业务域 | 端点数（约） | 消费者类型 |
|--------|:----------:|-----------|
| 主材任务调度 | 60+ | 前端（多角色）、其他系统 |
| 主材任务业务 | 10+ | B端前端 |
| 主材模板与配置 | 40+ | 内部管理 |
| 主材交付/送货 | 15+ | 前端、供应链系统 |
| 主材进度/日历 | 2 | 前端 |
| 安装工任务 | 25+ | 安装工/工长前端 |
| 管家端 | 12+ | 管家前端 |
| 业主端 | 5 | C端前端 |
| 交付工程师 | 1 | 交付工程师前端 |
| 自检验收 | 12+ | 前端、Acceptance服务 |
| 延期管理 | 16+ | 前端、企微Bot |
| 用工管理 | 30+ | 内部管理 |
| 测量申请单 | 8 | 设计师前端 |
| 测量交界面配置 | 12+ | 内部管理 |
| 材料流程（履约） | 20+ | 内部管理 |
| 交付流程（排程） | 30+ | 内部管理 |
| 跟单/协调员 | 15+ | 跟单员前端 |
| 排产 | 2 | 前端 |
| 业主自购 | 4 | 前端 |
| 施工任务 | 5 | SDM/外部系统 |
| 消息同步 | 5 | SDM/OMS/供应商 |
| 任务跟踪 | 3 | 前端 |
| 配置/元数据 | 10+ | 内部管理 |
| 通话记录 | 3 | 前端 |
| 巡检/数据修复 | 50+ | 内部运维 |
| 工具/测试 | 25+ | 开发运维 |
| 外部AI | 1 | 小材神AI |
| **合计** | **~430+** | |
