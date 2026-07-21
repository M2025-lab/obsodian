********
这套 `starlord` 库是贝壳家装主材履约调度系统（星爵）的核心数据库，围绕**「静态规则配置 → 项目主数据 → 任务节点实例化 → 全流程流转 → 异常售后闭环」**的主线设计。下面按业务逻辑分层，逐一说明每张表的存储内容与表间关联关系。

---

## 整体分层总览
全库共分为 8 大层级，核心全局关联键为 `project_order_id`（项目订单号），串联所有业务数据；配置层以 `process_code + version` 为唯一标识，驱动所有运行时逻辑。

---

## 一、流程配置层（静态规则，业务运行的底层依据）
所有表均为后台运维配置的静态规则，不随单个项目变动，是任务自动生成、节点流转、时间计算的唯一依据。

### 1. 模板与流程定义
- **`n_material_template`（用工任务模板主表）**
  - 存储内容：流程模板的顶层基础信息，包括模板名称、编码、适用范围（内部/外部）、销售类型、版本、状态。
  - 关联关系：通过 `id` 关联 `n_material_template_unit`（维度绑定）、`n_material_process_define`（流程定义）。

- **`n_material_template_unit`（模板维度绑定表）**
  - 存储内容：按「城市+分公司+产品套餐+主材+供应商+销售类型」维度绑定模板，定义“什么业务场景用哪套流程”。
  - 关联关系：通过 `template_id` 关联模板主表；项目下单时按业务维度匹配此表，定位对应流程。

- **`n_material_process_define`（主材任务流程定义表）**
  - 存储内容：每套模板下的具体流程规则，核心字段为 `process_code`（流程唯一编码），包含任务类型、批次属性、配送方式、工种配置、底线流程关联、版本状态。是流程规则的核心入口。
  - 关联关系：通过 `template_id` 关联模板；通过 `process_code + version` 关联所有节点、路由、时间、推送等明细配置。

- **`n_material_process_template`（调度流程模板配置表）**
  - 存储内容：调度层面的流程模板基础配置，包含模板ID、名称、任务类型、排序序号。
  - 关联关系：通过 `template_id` 关联下级节点模板。

- **`n_material_task_process_template`（任务节点模板配置表）**
  - 存储内容：细粒度任务节点模板，包含节点编码、重启节点、激活配置、作业模式、画布坐标。
  - 关联关系：通过 `process_template_id` 关联调度流程模板。

### 2. 节点与流转规则
- **`n_material_node_cfg`（主材节点定义表）**
  - 存储内容：每个流程下的所有节点配置，包括节点类型、激活方式、关联依赖、时间间隔、扩展属性。定义了流程包含哪些节点。
  - 关联关系：通过 `process_code + version` 关联流程定义；通过 `template_node_id` 关联通用模板节点。

- **`n_material_task_cfg`（主材节点业务配置表）**
  - 存储内容：节点的业务执行规则，包括执行角色、重启规则、合并规则、是否送装一体、作业耗时类型、派单角色。
  - 关联关系：通过 `process_code + version + node_id` 关联节点定义表。

- **`n_material_route`（主材流程路由表）**
  - 存储内容：节点间的流转连线规则，定义“源节点满足什么条件后，流转到目标节点”，包含规则表达式、配送方式适配。是流程流转的判断依据。
  - 关联关系：通过 `process_code + version` 关联流程定义；`source_code`/`target_code` 对应源/目标节点编码。

- **`n_material_route_time`（路由时间配置表）**
  - 存储内容：流转连线上的时间规则，如“前置节点结束后N天激活下一节点”，支持自然日/工作日、依赖类型。
  - 关联关系：通过 `route_id` 关联路由表。

- **`n_material_node_transfer_condition`（节点流转条件表）**
  - 存储内容：节点激活的前置条件，支持跨任务的节点依赖（如复尺节点依赖测量节点完成）。
  - 关联关系：通过 `node_id` 关联节点定义表。

- **`cfg_material_task_route`（通用任务路由配置表）**
  - 存储内容：轻量化的任务路由规则，源节点到目标节点的映射与规则表达式。
  - 关联关系：通过 `source_id`/`target_id` 关联节点配置。

### 3. 时间与考核配置
- **`n_material_time_cfg`（考核时间配置表）**
  - 存储内容：每个节点的考核时间规则，分为平台考核、首次考核、重启考核三类，定义间隔天数、时间类型（自然日/工作日）、截止时点、附加条件。
  - 关联关系：通过 `process_code + version + node_code` 关联节点定义。

- **`n_material_time_relation`（考核时间依赖表）**
  - 存储内容：节点考核时间的计算锚点，如“本节点承诺时间 = 某前置节点结束时间 + 周期”。
  - 关联关系：通过 `time_id` 关联考核时间配置表。

- **`n_material_time_calculate_rule_cfg`（节点耗时计算规则表）**
  - 存储内容：节点作业耗时的计算规则，支持固定耗时、按面积递增、按卫生间个数、按户型结构、按特殊工艺等维度。
  - 关联关系：通过 `node_id` 关联节点定义表。

- **`n_material_period_calculate_hit_craft_cfg`（工艺耗时规则表）**
  - 存储内容：命中特殊工艺时的耗时增量，是耗时计算规则的明细补充。
  - 关联关系：通过 `node_period_calculate_rule_id` 关联耗时计算规则表。

- **`n_material_period_calculate_hit_house_cfg`（户型耗时规则表）**
  - 存储内容：不同房屋类型/户型结构对应的耗时增量，是耗时计算规则的明细补充。
  - 关联关系：同上，关联耗时计算规则表。

- **`stock_up`（备货周期主表）**
  - 存储内容：按「主材+供应商」维度配置的标准周期，覆盖测量、复尺、备货、送货、安装五大类型，区分有无条件。
  - 关联关系：通过 `id` 关联 `stock_up_condition_rule` 条件明细。

- **`stock_up_condition_rule`（备货周期条件规则表）**
  - 存储内容：带条件的周期规则，如“某节点完成后N天本节点必须完成”，支持自然日/工作日。
  - 关联关系：通过 `stock_up_id` 关联备货周期主表。

- **`n_holiday_cfg` / `n_holiday_cfg_detail`（假期配置主表/明细表）**
  - 存储内容：节假日、请假配置，按城市、分公司、供应商、品类、节点维度设置假期，用于工作日计算时剔除假期。
  - 关联关系：明细表通过 `holiday_cfg_id` 关联主表。

### 4. 通知与推送配置
- **`n_material_push_cfg`（节点推送配置表）**
  - 存储内容：节点的通知规则，包括通知时机（首次激活/重启/完成/即将超期）、通知对象、实时/定时、提前量。
  - 关联关系：通过 `process_code + version + node_code` 关联节点定义。

### 5. 分支与扩展配置
- **`n_material_branch`（任务分支表）**
  - 存储内容：流程中的分支定义，支持多选分支，按主材、任务类型维度配置。
  - 关联关系：通过 `branch_code` 关联 `n_material_branch_condition` 分支条件表。

- **`n_material_branch_condition`（分支条件表）**
  - 存储内容：每个分支的条件枚举、规则表达式、默认值、数据源。
  - 关联关系：通过 `branch_code` 关联分支主表。

### 6. 测量专项配置
- **`cfg_measure_template`（测量模板配置表）**
  - 存储内容：按前台类目、组合空间配置的测量属性模板，定义测量时需要填写的属性项。
  - 关联关系：通过 `category_id`、`combinatorial_space_id` 关联类目和空间。

- **`cfg_measure_attr_used`（历史测量属性表）**
  - 存储内容：历史使用过的测量属性枚举，用于属性联想、历史数据回溯。

- **`material_measure_interface_config`（测量交界面配置主表）**
  - 存储内容：测量/复尺环节的交界面要求、示例图片，是测量作业的标准规范。
  - 关联关系：通过 `id` 关联类目、分公司关系表。

- **`material_measure_interface_config_category_rel`（交界面-类目节点关系表）**
  - 存储内容：交界面规则适用的类目、节点范围。
  - 关联关系：通过 `rule_id` 关联交界面主表。

- **`material_measure_interface_config_mdm_rel`（交界面-分公司关系表）**
  - 存储内容：交界面规则适用的分公司范围。
  - 关联关系：通过 `rule_id` 关联交界面主表。

- **`measure_apply_range_type_config`（测量范围配置表）**
  - 存储内容：签前/签后场景下，哪些主材、供应商需要纳入测量范围。

### 7. 其他配置
- **`material_payment_intercept_config`（付款拦截配置表）**
  - 存储内容：哪些品类、套餐需要校验尾款状态，未付清则拦截送货安装预约。

- **`n_material_edge`（流程画布坐标表）**
  - 存储内容：流程图的画布拐点、节点坐标数据，纯前端可视化使用，无业务逻辑。
  - 关联关系：通过 `process_code + version` 关联流程定义。

- **`n_material_cfg_log`（配置变更日志表）**
  - 存储内容：所有流程、模板、节点配置的修改记录，用于审计回溯。

- **`n_material_template_copy_log`（模板复制日志表）**
  - 存储内容：模板复制操作的记录，包含改动描述。

### 8. 兼容/旧版规则体系
系统存在新老两套流程规则架构（过渡阶段），逻辑与 `n_` 前缀体系一致，表名不带 `n_` 前缀：
- `material_flow_rule` / `material_flow_rule_category` / `material_flow_rule_unit`：旧版材料流程规则主表、品类表、维度表
- `material_flow_rule_template_mapping` / `material_flow_rule_template_record`：旧规则与新模板映射、修改记录
- `delivery_flow_rule` / `delivery_flow_rule_category` / `delivery_flow_rule_unit`：排程流程规则体系
- `material_task` / `material_task_node` / `material_task_node_time` / `material_task_node_time_relation`：旧版任务模板与节点时间配置

---

## 二、项目主数据层（全局根实体）
- **`project_info`（项目信息表）**
  - 存储内容：家装项目的核心主数据，包括项目订单号、主订单号、城市/分公司、房屋地址、客户信息、楼层电梯等基础属性。
  - 关联关系：**全库核心根表**，所有业务表通过 `project_order_id` 字段关联此表，是数据串联的唯一全局标识。

---

## 三、任务运行核心层（系统核心，运行时实例数据）
由配置层规则实例化生成，是业务执行的核心载体，数据随流程流转实时更新。核心关联标识：`task_dispatch_id`（任务ID）、`task_dispatch_node_id`（节点ID）。

### 1. 任务与节点主表
- **`task_dispatch`（任务派发表 / 任务主表）**
  - 存储内容：每个项目下，每个主材对应一条任务记录（如橱柜安装任务、瓷砖送货任务），包含任务类型、流程编码、当前节点、整体状态、批次、主材/供应商信息、激活时间。
  - 关联关系：通过 `project_order_id` 关联项目；通过 `template_id` / `process_code` 关联配置层模板；通过 `id` 关联所有节点、扩展、进度表。

- **`task_dispatch_node`（任务节点表 / 执行主表）**
  - 存储内容：每个任务下的全量节点实例（初始未激活，按序激活），包含节点类型、进程状态、审核结果、执行人、四类时间（预估/承诺/实际开始/实际结束）、延期天数、附件备注。是全流程最核心的执行载体。
  - 关联关系：通过 `task_dispatch_id` 关联任务主表；通过 `template_node_id` 关联配置层节点；通过 `project_order_id` 关联项目。

### 2. 任务扩展与明细
- **`task_dispatch_extend`（任务扩展表）**
  - 存储内容：任务的非高频核心字段，包括用量确认、变更单号、SKU信息、是否下单标识。与任务主表 1:1 对应，避免主表字段冗余。
  - 关联关系：通过 `task_dispatch_id` 唯一关联任务主表。

- **`task_handle_extension`（任务处理扩展表）**
  - 存储内容：节点处理时的扩展数据，如打卡经纬度、位置精度、高度、速度，用于外勤校验。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

- **`design_review_alteration`（设计复核变更表）**
  - 存储内容：设计复核节点的专属属性，标记是否需要变更。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

### 3. 批量调度
- **`task_process_batch`（任务批次表）**
  - 存储内容：同一项目下，同类型、同批次的节点统一打包调度，支持批量激活、批量派工。
  - 关联关系：通过 `project_order_id` 关联项目；通过 `task_batch_node_relation` 关联具体节点。

- **`task_batch_node_relation`（批次-节点关联表）**
  - 存储内容：批次与节点的多对多映射关系。
  - 关联关系：`task_batch_id` 关联批次表，`task_dispatch_node_id` 关联节点表。

### 4. 进度与异常跟进
- **`task_node_progress`（节点进度跟进表）**
  - 存储内容：异常/延期节点的跟进记录，每次督办跟进生成一条，记录状态流转、延期原因、责任方、下次跟进时间。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

- **`task_dispatch_node_delay_reason`（延期原因记录表）**
  - 存储内容：节点延期的标准化原因枚举、备注说明，是延期数据的结构化存储。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

- **`task_estimated_time_change_log`（考核时间变更日志）**
  - 存储内容：节点预估/考核时间的调整记录，用于追溯时间变更历史。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

### 5. 通知与定时任务
- **`message_send_job`（通知发送任务表）**
  - 存储内容：节点触发的通知定时任务，如节点激活提醒、即将超期提醒、完成提醒。
  - 关联关系：通过 `task_dispatch_id` / `task_dispatch_node_id` 关联任务与节点。

- **`notice_time_history`（通知时间修改历史）**
  - 存储内容：通知时间人工调整的记录，用于审计。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

- **`notice_task_info`（通知任务信息表）**
  - 存储内容：送货异常等专项通知的发送记录。
  - 关联关系：通过 `project_order_id` 关联项目。

### 6. 生成与重试支撑
- **`task_create_fail_reason`（任务创建失败表）**
  - 存储内容：任务自动生成失败的错误记录，包含入参、报错原因，用于排查补单。
  - 关联关系：通过 `project_order_id` 关联项目。

- **`retry_delay_queue`（重试延迟队列表）**
  - 存储内容：异步失败的重试任务，如回调第三方失败、消息发送失败，支持延迟重试、计数重试次数。
  - 关联关系：通过 `process_code` 关联流程定义。

- **`refresh_data_log`（刷数操作日志表）**
  - 存储内容：所有数据修复、批量刷数的操作记录，包含入参、上下文，用于数据问题追溯。
  - 关联关系：通过 `project_order_id` / `task_dispatch_id` 关联业务数据。

---

## 四、售后与异常履约层
独立于主流程的售后、延期、返补业务闭环。

### 1. 返补跟单（CT单）
- **`coordinator_task_order`（返补跟单主表）**
  - 存储内容：售后返补跟单的主单信息，包括CT单号、问题描述、解决方案、返补类型、跟单状态、主材/供应商信息、关联订单号。
  - 关联关系：通过 `project_order_id` 关联项目；通过 `id` 关联明细、进展表。

- **`coordinator_task_order_unit`（跟单单元表）**
  - 存储内容：跟单下的主材明细单元，一条跟单可对应多个主材。
  - 关联关系：通过 `coordinator_task_order_id` 关联跟单主表。

- **`coordinator_task_progress`（跟单进展表）**
  - 存储内容：每条跟单的多次跟进记录，记录状态流转、备注、附件、下次跟进时间。
  - 关联关系：通过 `coordinator_task_id` 关联跟单主表。

- **`coordinator_retail_record`（纯零售跟单记录表）**
  - 存储内容：零售场景下的专属跟单记录，独立于整装场景。

### 2. 正式延期单
- **`material_delay_process`（主材延期单主表）**
  - 存储内容：正式提交的延期申请单，带审批流程，包含延期天数、原计划/新计划时间、提交/审批人、确认状态。
  - 关联关系：通过 `project_order_id` 关联项目；通过 `id` 关联日志、原因表。

- **`material_delay_process_log`（延期单操作日志）**
  - 存储内容：延期单的提交、审批、驳回、确认等全流程操作记录。
  - 关联关系：通过 `material_delay_process_id` 关联延期单主表。

- **`material_delay_process_reason`（延期单原因明细表）**
  - 存储内容：一条延期单对应的多条原因明细，包含原因分类、延期天数、备注图片。
  - 关联关系：通过 `material_delay_process_id` 关联延期单主表。

### 3. 延期原因治理
- **`delay_reason_recovery_record`（延期原因回收表）**
  - 存储内容：通过LLM对非结构化延期原因进行标准化归类的结果，包含原始信息、标准化原因、追踪日志，用于数据治理。
  - 关联关系：通过 `recovery_key` 关联对应节点/延期单。

---

## 五、业务辅助明细层
### 1. 测量专项
- **`measure_apply`（测量申请单表）**
  - 存储内容：项目的测量申请记录，分签前/签后类型，包含测量时间、测量范围。
  - 关联关系：通过 `project_order_id` 关联项目。

- **`measure_apply_operate_log`（测量申请操作日志）**
  - 存储内容：测量申请的增删改操作记录。
  - 关联关系：通过 `project_order_id` 关联项目。

- **`measure_material_detail`（测量详情表）**
  - 存储内容：测量节点提交的图片、备注、筛选结果。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

- **`measure_material_unit`（测量单元表）**
  - 存储内容：测量的具体SKU明细、属性值、空间、类目信息。
  - 关联关系：通过 `task_dispatch_id` 关联任务表。

### 2. 通话与沟通
- **`call_record`（通话记录表）**
  - 存储内容：项目相关的所有通话记录，包括呼入呼出、接通状态、时长、录音链接、主被叫信息。
  - 关联关系：通过 `project_order_id` 关联项目。

### 3. 其他业务辅助
- **`task_select_category`（选材明细表）**
  - 存储内容：项目的选材记录，关联主材、SKU、选材时间。
  - 关联关系：通过 `project_order_id` 关联项目。

- **`self_buy_remind_record`（业主自购提醒记录表）**
  - 存储内容：向业主发送自购提醒的发送状态记录。
  - 关联关系：通过 `project_order_id` 关联项目。

---

## 六、跨系统集成与同步层
### 1. 上下游系统同步
- **`oms_message_sync`（OMS消息同步表）**
  - 存储内容：从OMS订单系统同步过来的节点状态、到货时间、配送确认、审核结果等消息。
  - 关联关系：通过 `project_order_id` / `order_no` 关联项目与主材订单。

- **`supplier_sync_info`（供应商数据同步表）**
  - 存储内容：供应商侧同步的接单时间、配送时间、上门备注等信息。
  - 关联关系：通过 `task_dispatch_node_id` 关联对应节点。

### 2. 事件驱动架构
- **`event_pub`（事件发布表）**
  - 存储内容：本地消息表，本系统产生的业务事件（如节点完成、任务创建），用于异步通知下游系统，保证分布式事务最终一致性。
  - 关联关系：`event_context_key` 通常对应 `project_order_id` 或任务ID。

- **`event_sub`（事件订阅表）**
  - 存储内容：接收上游系统的事件消息，用于驱动本系统业务逻辑（如下单成功触发任务生成）。

### 3. 通用第三方数据
- **`thirdparty_data`（第三方数据表）**
  - 存储内容：对接外部系统的临时/缓存数据，按类型+业务键存储。

---

## 七、系统通用支撑层
- **`operation_log`（操作日志表）**
  - 存储内容：全系统用户操作的通用日志，包含请求响应、IP、trace_id、资源路径，用于排查问题。

- **`operation_audit`（操作审核表）**
  - 存储内容：需要审批的业务操作，关联流程实例、审批状态、业务数据。

- **`task_dispatch_node_report_relation`（节点验收报告关联表）**
  - 存储内容：任务节点与验收报告的映射关系，关联工艺、报告标准。
  - 关联关系：通过 `task_dispatch_node_id` 关联节点表。

---

## 八、废弃/无数据表（可忽略）
表注释已标注无用，线上无数据，为历史遗留表：
- `n_material_define`：旧版物料定义表，已被新配置体系替代
- `quotation_booking_scope`：旧版报价下单范围表，已废弃

---

## 核心数据流转链路
### 1. 从配置到任务生成
`n_material_template_unit`（维度匹配）→ 定位 `n_material_template` 模板 → 读取 `n_material_process_define` 流程定义 → 解析 `n_material_node_cfg` 节点列表 → 生成 `task_dispatch` 任务主单 → 批量生成 `task_dispatch_node` 节点实例 → 结合 `n_material_time_cfg` + `stock_up` 计算节点时间

### 2. 节点流转执行
前置节点提交完成 → 匹配 `n_material_route` 路由规则 → 激活下一节点 → 更新 `task_dispatch_node` 状态与实际时间 → 同步更新 `task_dispatch` 当前节点 → 触发 `message_send_job` 通知任务

### 3. 全局关联锚点
- 项目维度：所有业务表通过 `project_order_id` 串联
- 任务维度：节点、扩展、进度、日志通过 `task_dispatch_id` 串联
- 配置维度：所有流程规则通过 `process_code + version` 串联


## 一、核心问题：主材流程配置路由表（`n_material_route`）的作用
这张表是**主材履约可视化流程编排的核心“连线”配置表**，对应流程图画布上节点之间的“箭头”，用来定义流程节点的流转路径与触发条件。

### 具体职能
1. **定义节点流转方向**
    通过 `source_code`（来源节点编码）和 `target_code`（目标节点编码），明确“当前节点完成后，下一步流转到哪个节点”，是流程串起来的基础。`type` 字段进一步区分：
    - `1`：小节点路由，即同一个任务内部的节点流转
    - `2`：大节点路由，即不同任务之间的流转
2. **控制分支流转条件**
    `rule_expression` 字段存储规则表达式，系统在节点完成后会执行该表达式，判断是否满足走这条路径的条件，以此实现分支流程（例如：验收合格走下一节点，不合格走重启/返工节点）。
3. **支持差异化配置**
    可按 `category_id`（品类规则）、`delivery_type`（配送方式）配置不同的流转路径，实现不同主材、不同配送模式下的流程差异。
4. **标准化节点映射**
    `source_unified_code` / `target_unified_code` 存储统一节点编码，用于跨流程、跨系统的节点标准化对齐。

### 关联关系
- 通过 `process_code` + `version` 关联 `n_material_process_define`（流程定义主表），归属于某一套具体的流程版本
- `source_code` / `target_code` 关联 `n_material_node_cfg`（节点定义表）的节点编码
- `category_id` 关联品类规则表，实现按品类差异化路由

---

## 二、全库表结构整体解析与关联关系
这套库是**家装主材履约调度系统（Starlord）**，覆盖家装订单中主材从测量、设计、复尺、下单、生产、送货到安装的全流程调度、节点管控、周期考核、异常跟单等业务。下面按模块逐一说明：

### 模块1：项目/订单基础层
#### project_info（项目信息表）
- **存储内容**：家装项目的基础档案，包含主订单号、项目订单号、省市区县编码、地址、客户姓名与UCID、楼层、电梯、商户ID等项目维度基础属性。
- **核心地位**：全库的顶层锚点，几乎所有业务表都通过 `project_order_id` 关联项目。

### 模块2：通话沟通类
#### call_record（通话记录表）
- **存储内容**：项目关联的全量通话记录，含呼入/呼出/转接类型、通话时间、接通状态、时长、主被叫号码、虚拟号、坐席与客户UCID、城市、挂断方、通话渠道、录音URL。
- **关联**：通过 `project_order_id` 挂载到项目上，属于项目沟通附属数据。

### 模块3：测量业务类
管理测量申请、模板、详情、交界面配置。

| 表名 | 存储内容 | 核心关联 |
|------|----------|----------|
| `measure_apply` | 每个项目的测量申请单，区分签前/签后测量，记录测量范围、测量时间、操作人 | `project_order_id` 关联项目，一个项目对应一条申请单 |
| `measure_apply_operate_log` | 测量申请范围内主材/供应商的增删改操作日志，记录操作类型、来源、操作人、城市 | `project_order_id` 关联测量申请单 |
| `measure_apply_range_type_config` | 后台配置的测量范围规则，按签前/签后类型配置对应主材、供应商 | 配置级规则表，业务逻辑中用于默认范围判断 |
| `cfg_measure_template` | 按前台类目、组合空间配置的测量属性模板，JSON存储字段配置 | 被测量详情、测量单元表业务引用 |
| `cfg_measure_attr_used` | 历史使用过的供应链属性ID与名称沉淀 | 属性字典表 |
| `measure_material_detail` | 具体测量节点的详情数据：测量图片、备注、过滤后备注 | `task_dispatch_id` / `task_dispatch_node_id` 关联任务节点实例 |
| `measure_material_unit` | 测量任务下按类目、空间维度的SKU测量结果与属性值 | `task_dispatch_id` 关联任务实例 |
| `material_measure_interface_config` | 测量/复尺交界面要求、示例图片配置主表 | 主键被两张关系表引用 |
| `material_measure_interface_config_category_rel` | 交界面规则适用的履约类目+任务节点 | `rule_id` 关联交界面主表 |
| `material_measure_interface_config_mdm_rel` | 交界面规则适用的分公司范围 | `rule_id` 关联交界面主表 |
| `design_review_alteration` | 设计复核环节是否需要变更的结论 | `task_dispatch_id` / `task_dispatch_node_id` 关联任务节点 |

### 模块4：返补跟单类
管理主材返补（原厂返补/当场返补）的跟单任务与进展。

| 表名 | 存储内容 | 核心关联 |
|------|----------|----------|
| `coordinator_task_order` | 返补跟单主任务，包含CT单号、问题单、主材/供应商、返补类型、状态、问题描述、解决方案、下单出入参 | `project_order_id` 关联项目；`ct_order_no` 关联CT单 |
| `coordinator_task_order_unit` | 返补跟单明细单元，冗余主材、供应商信息 | `coordinator_task_order_id` 关联跟单主表 |
| `coordinator_task_progress` | 跟单状态流转日志：前后状态、处理类型、下次跟进时间、备注、处理人、完单时间 | `coordinator_task_id` 关联跟单主表 |
| `coordinator_retail_record` | 纯零售场景跟单记录：地址、客户、大区、品类 | 通过 `ct_order_no` 与其他跟单表关联 |

### 模块5：延期管理类
管理延期申请、原因、标准化回收。

| 表名 | 存储内容 | 核心关联 |
|------|----------|----------|
| `material_delay_process` | 主材延期申请单主表：定制品信息、延期角色、原/预计进场日期、延期天数、提交/审批/确认人、确认状态 | `project_order_id` 关联项目 |
| `material_delay_process_log` | 延期单操作日志：提交、通过、驳回、确认，含驳回原因 | `material_delay_process_id` 关联延期单 |
| `material_delay_process_reason` | 延期单对应的分级原因明细、延期天数、备注、图片 | `material_delay_process_id` 关联延期单 |
| `delay_reason_recovery_record` | LLM标准化延期原因回收表：原始信息、标准化原因、LLM追踪、提醒状态 | `recovery_type`+`recovery_key` 关联各业务延期记录 |
| `task_dispatch_node_delay_reason` | 任务节点维度的延期原因登记、原因枚举组合 | `task_dispatch_node_id` 关联节点实例 |

### 模块6：流程配置层（规则大脑）
分为老版 `material_*` 和新版 `n_material_*` 两套体系，新版为当前可视化流程编排主力。

#### 6.1 新版流程配置（n_material_* 系列）
对应流程图画布的完整元素：流程→节点→路由→时间→推送。

| 表名 | 存储内容 | 核心关联 |
|------|----------|----------|
| `n_material_process_define` | 流程定义主表，一套完整流程的根：流程编码、版本、任务类型、配送方式、状态、底线流程、工种描述 | `process_code`+`version` 是整套流程唯一标识 |
| `n_material_node_cfg` | 流程中的“节点方块”配置：节点编码、类型、激活条件、扩展字段、备注 | `process_code`+`version` 关联流程定义 |
| `n_material_route` | 流程中的“连线箭头”：节点流转方向、规则表达式、品类/配送方式维度 | 上文已详述，关联流程与节点 |
| `n_material_route_time` | 连线上的时间依赖规则：前置条件、时间关系、间隔天数 | `route_id` 关联路由表 |
| `n_material_edge` | 流程画布的坐标、拐点信息，用于前端渲染 | `process_code`+`version` 关联流程 |
| `n_material_task_cfg` | 节点的任务属性：角色、操作项、重启规则、合并规则、送装一体 | `node_id` 关联节点定义 |
| `n_material_node_transfer_condition` | 节点激活详细条件：履约类型、触发方式、时间间隔 | `node_id` 关联节点定义 |
| `n_material_time_cfg` | 节点考核时间配置：平台/首次/重启考核、时间基准、间隔天数 | `process_code`+`version` 关联流程 |
| `n_material_time_relation` | 考核时间的依赖关系：依赖哪个节点的哪种时间 | `time_id` 关联时间配置 |
| `n_material_push_cfg` | 节点消息推送配置：通知时机、通知上级、实时/批量、提前量 | `process_code`+`version` 关联流程 |
| `n_material_time_calculate_rule_cfg` | 节点作业耗时计算规则：固定、按面积、按户型、按工艺递增 | `node_id` 关联节点定义 |
| `n_material_period_calculate_hit_craft_cfg` | 命中特殊工艺的耗时增量 | 关联耗时配置主表 |
| `n_material_period_calculate_hit_house_cfg` | 不同房屋/户型的耗时配置 | 关联耗时配置主表 |
| `n_material_branch` | 任务分支配置：分支编码、适用主材/任务 | 被分支条件表引用 |
| `n_material_branch_condition` | 分支下的条件枚举、规则表达式、默认值 | `branch_code` 关联分支主表 |
| `n_material_template` | 用工任务模板：名称、工种、范围、销售类型 | 被模板单元表引用 |
| `n_material_template_unit` | 用工模板适用维度：城市、分公司、套餐、主材、供应商 | `template_id` 关联用工模板 |
| `n_material_cfg_log` | 所有主材配置的变更日志 | 全量配置操作留痕 |

#### 6.2 老版流程配置（material_flow_* / material_task_*）
早期流程规则体系，功能与新版对应。

| 表名 | 存储内容 |
|------|----------|
| `material_flow_rule` | 流程规则主表 |
| `material_flow_rule_category` | 按品类/供应商配置节点流程 |
| `material_flow_rule_unit` | 规则适用范围：订单版本、业务类型、分公司、套餐 |
| `material_flow_rule_template_mapping` | 规则与模板映射 |
| `material_task` | 任务模板配置：激活规则、批次、来源 |
| `material_task_node` | 任务模板节点配置：关联、时间、角色、重启 |
| `material_task_node_time` | 节点考核时间配置 |
| `material_task_node_time_relation` | 考核时间依赖条件 |

#### 6.3 交付/排程流程规则（delivery_flow_*）
面向材料排程、送货的流程规则。
- `delivery_flow_rule`：排程流程规则主表
- `delivery_flow_rule_category`：按品类配置节点流程与交付模板
- `delivery_flow_rule_unit`：规则适用范围维度

#### 6.4 其他业务配置
| 表名 | 存储内容 |
|------|----------|
| `cfg_material_task_route` | 材料任务来源→目标路由配置 |
| `material_payment_intercept_config` | 尾款拦截配置：哪些品类/套餐限制送货安装预约 |
| `n_holiday_cfg` | 假期/请假配置主表：城市、组织、时间段 |
| `n_holiday_cfg_detail` | 请假影响范围：分公司、供应商、品类、任务、节点 |
| `stock_up` | 主材各环节周期配置：测量/复尺/备货/送货/安装 |
| `stock_up_condition_rule` | 备货周期的条件规则：条件节点、时间关系、天数 |

### 模块7：任务实例层（运行时核心数据）
流程配置落地到具体订单的业务实例，是系统核心业务表。

| 表名 | 存储内容 | 核心地位与关联 |
|------|----------|----------------|
| `task_dispatch` | 任务主实例：每个项目下，每个主材+供应商+任务类型生成一条。含模板、流程、订单、主材、状态、批次、预计激活时间、当前节点等 | **任务维度主表**，`id` 被所有任务子表引用；`project_order_id` 关联项目 |
| `task_dispatch_node` | 节点实例：任务下每个节点的执行数据，含各时间字段（通知/考核/预估/承诺/实际开始结束）、执行人、状态、合格结果、重启次数、延期天数、就绪状态 | **节点维度主表**，流程调度、考核、通知的直接载体；`task_dispatch_id` 关联任务主表 |
| `task_dispatch_extend` | 任务扩展属性：用量确认、变更单号、是否下单、SKU信息 | 一对一关联任务主表 |
| `task_dispatch_node_report_relation` | 节点与验收报告关联：施工包、工艺、报告标准 | 关联节点实例 |
| `task_node_progress` | 节点人工跟进进展：状态流转、下次跟进时间、延期原因、责任方 | 关联节点实例 |
| `task_process_batch` | 批量任务批次：多个节点合并为一批处理，记录批次类型、执行人、激活时间 | 关联项目 |
| `task_batch_node_relation` | 批次与节点的关联关系 | 关联批次表与节点 |
| `task_estimated_time_change_log` | 节点考核时间变更历史 | 关联节点 |
| `task_handle_extension` | 节点处理定位数据：经纬度、速度、精度，用于上门打卡校验 | 关联节点 |
| `task_select_category` | 项目选材记录：主材、供应商、选材时间 | 关联项目 |
| `task_create_fail_reason` | 任务创建失败异常记录：供应链参数、错误原因、分公司 | 关联项目 |

### 模块8：通知、消息与事件
| 表名 | 存储内容 | 关联 |
|------|----------|------|
| `message_send_job` | 消息发送任务：通知时机、角色、提前量、运行状态 | 关联任务与节点 |
| `notice_task_info` | 业务通知：如送货异常提醒，含接收人、发送状态 | 关联项目 |
| `notice_time_history` | 通知时间变更历史：原时间、现时间、备注 | 关联节点 |
| `self_buy_remind_record` | 业主自购材料提醒发送记录 | 关联项目 |
| `event_pub` | 事件发布表：事件ID、类型、内容、状态，可靠事件投递 | 事件驱动架构的生产端 |
| `event_sub` | 事件订阅表：消费端事件落地与重试 | 与发布表结构对应 |
| `retry_delay_queue` | 延迟重试消息：消息类型、执行时间、重试次数、参数 | 关联流程编码 |

### 模块9：上下游同步
| 表名 | 存储内容 | 关联 |
|------|----------|------|
| `oms_message_sync` | OMS同步的节点消息：启动/完成、合格状态、到货/配送时间 | 关联项目、主材单、任务节点类型 |
| `supplier_sync_info` | 供应商同步数据：接单时间、配送确认时间、备注 | 关联节点、主材单 |
| `thirdparty_data` | 通用第三方数据存储，按类型+业务key存储 | 通用存储表 |

### 模块10：系统支撑
| 表名 | 存储内容 |
|------|----------|
| `operation_audit` | 审批流程表：业务数据、流程实例、状态、意见 |
| `operation_log` | 系统操作日志：用户、请求响应、IP、trace_id |
| `refresh_data_log` | 数据刷数/修复日志：事件类型、上下文、request_id |
| `quotation_booking_scope` | 历史遗留表，线上无数据 |

---

## 三、全库核心关联脉络
1. **顶层锚点**：`project_info.project_order_id` 贯穿所有业务表，代表一个家装项目。
2. **配置到执行**：流程定义 → 节点+路由配置 → 生成任务实例 → 生成节点实例，是系统核心执行链路。
3. **业务挂载**：通话、测量、延期、返补、通知、同步数据，全部通过项目ID或任务/节点ID挂载到主业务链路上。
4. **双配置体系**：老版配置与新版可视化流程配置并存，新版为当前主力。

---
## 相关文档

- [[starlord数据库表/starlord10张核心数据库表]] — 10张核心表分析
- [[starlord数据库表/数据库打标签]] — 表的A/B/C/D分类标签
- [[starlord数据库表/数据库个人总结]] — 个人接口→表映射速查
- [[starlord项目整理/材料配置问题]] — 配置表（n_material_*）的业务含义
