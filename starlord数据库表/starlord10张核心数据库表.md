# StarLord 核心数据表梳理

## 概述

- 分析依据：基于 MyBatis Generator 自动生成的 model/DAO 层代码
- 数据库：MySQL，库名 `starlord`
- 核心数据表数量：10 张
- 通用规范：所有表均使用 InnoDB 引擎，主键 `id` 为自增 BIGINT；时间字段 `gmt_create/gmt_modified` 或 `create_time/update_time` 为 MyBatis Generator 标准审计列；以下索引为基于查询模式推断，实际索引以数据库 DDL 为准（项目中未提交 DDL 文件）

---

## 1. project_info（项目信息表）

**表描述**：存储家装项目 / 订单的核心信息，是整个系统的中心实体。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_project_order_id (project_order_id)` — 最常查询字段
    - `idx_home_order_no (home_order_no)`
    - `idx_customer_ucid (customer_ucid)`
    

### 字段清单

表格

| 字段               | 类型        | 说明                 |
| ---------------- | --------- | ------------------ |
| id               | BIGINT    | 主键，自增              |
| home_order_no    | VARCHAR   | 主订单号               |
| project_order_id | BIGINT    | 项目订单 ID            |
| gb_code          | VARCHAR   | 城市编码               |
| mdm_code         | VARCHAR   | 门店所属主体 / 分公司编码     |
| address          | VARCHAR   | 地址                 |
| customer_name    | VARCHAR   | 客户姓名               |
| customer_ucid    | VARCHAR   | 客户 UCID            |
| province         | VARCHAR   | 省                  |
| province_code    | VARCHAR   | 省编码                |
| city             | VARCHAR   | 市                  |
| city_code        | VARCHAR   | 市编码                |
| country          | VARCHAR   | 县 / 区              |
| country_code     | VARCHAR   | 区县编码               |
| has_lift         | TINYINT   | 有无电梯 (0 = 无，1 = 有) |
| floor            | INTEGER   | 楼层                 |
| merchant_id      | INTEGER   | 商户 ID              |
| create_time      | TIMESTAMP | 创建时间               |
| modify_time      | TIMESTAMP | 修改时间               |

### 状态枚举

- `has_lift`: {0: 无，1: 有}

### 表间关联

- `project_order_id` → `task_dispatch.project_order_id`（1:N）
- `project_order_id` → `task_node_progress.project_order_id`（1:N）
- `project_order_id` → `coordinator_task_order.project_order_id`（1:N）
- `project_order_id` → `measure_apply.project_order_id`（1:N）

---

## 2. task_dispatch（任务派发表）

**表描述**：运行时任务派发主表，每条记录代表一个具体项目下的某一品类的任务实例。由模板 ( `n_material_define` ) 实例化而来。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_project_order_id`
    - `idx_home_order_no`
    - `idx_order_no`
    - `idx_material_code + idx_supplier_code`
    - `idx_process_code`
    - `idx_task_type`
    - `idx_process_status + idx_state`（复合查询高频）
    - `idx_template_id`
    

### 字段清单

表格

| 字段                 | 类型        | 说明                                              |
| ------------------ | --------- | ----------------------------------------------- |
| id                 | BIGINT    | 主键，自增                                           |
| template_id        | BIGINT    | 主材任务模板 ID，FK → n_material_template.id           |
| project_order_id   | BIGINT    | 项目订单 ID，FK → project_info.project_order_id      |
| home_order_no      | VARCHAR   | 主订单号                                            |
| order_no           | VARCHAR   | 主材订单号                                           |
| source_type        | INTEGER   | 创建来源 (0 = 供应链订单，1 = 测量申请单)                      |
| service_mode       | INTEGER   | 服务模式                                            |
| gb_code            | VARCHAR   | 城市编码                                            |
| material_code      | VARCHAR   | 主材编码                                            |
| material_name      | VARCHAR   | 主材名称                                            |
| supplier_code      | VARCHAR   | 供应商编码                                           |
| supplier_name      | VARCHAR   | 供应商名称                                           |
| task_type          | INTEGER   | 任务类型 → TaskTypeEnum                             |
| process_batch      | INTEGER   | 批量批次号                                           |
| plan_activate_time | TIMESTAMP | 预计激活时间                                          |
| node_task          | VARCHAR   | 子节点序列（按序排列）                                     |
| current_node_type  | INTEGER   | 当前子节点类型                                         |
| current_node_time  | TIMESTAMP | 当前子节点激活时间                                       |
| process_status     | TINYINT   | 进程状态 → ProcessStatusEnum                        |
| state              | TINYINT   | 状态 (0 = 无效，1 = 有效)                              |
| process_code       | VARCHAR   | 流程定义唯一 code                                     |
| version            | INTEGER   | 版本号                                             |
| node_path          | VARCHAR   | 节点路径                                            |
| condition_code     | VARCHAR   | 条件枚举值                                           |
| mode               | INTEGER   | 模式 (1 = 北京，2 = 圣都)                              |
| sale_type          | INTEGER   | 销售类型                                            |
| mdm_code           | VARCHAR   | 分公司 code                                        |
| mdm_company_name   | VARCHAR   | 分公司名称                                           |
| business_id        | VARCHAR   | 零售业务 ID                                         |
| attachment         | VARCHAR   | 任务附件                                            |
| biz_relation_id    | VARCHAR   | 业务关联 ID（JSON）                                   |
| sync_to_sdm        | INTEGER   | 推送 SDM 标识 (0 = 未知，1 = 老 VSS, 2 = 新 SDM, 3 = 刷数) |
| purchase_order_no  | VARCHAR   | 采购单号                                            |
| flow_type          | INTEGER   | 业务类型 (0 = 正单，1 = 返补)                            |
| create_by          | VARCHAR   | 创建人                                             |
| modify_by          | VARCHAR   | 修改人                                             |
| modify_name        | VARCHAR   | 修改人姓名                                           |
| gmt_create         | TIMESTAMP | 创建时间                                            |
| gmt_modified       | TIMESTAMP | 修改时间                                            |

### 状态枚举

- `task_type` (TaskTypeEnum): {1: 测量，2: 预定，3: 复尺，4: 下单，5: 送货，6: 安装，7: 接单，8: 备货，9: 预埋件安装，10: 设计，11: 设计复核 / 报价变更，12: 排产，13: 下生产单，14: 进仓，15: 报价，16: 确认方案，17: 下测量单，99: 供应链，200: 变更单，201: 撤销单}
- `process_status` (ProcessStatusEnum): {1: 未激活，2: 激活未完成，3: 激活已完成，4: 暂停激活}
- `state`: {0: 无效，1: 有效}
- `source_type`: {0: 供应链订单，1: 测量申请单}
- `mode`: {1: 北京，2: 圣都}

### 表间关联

- `template_id` → `n_material_template.id`（N:1）
- `project_order_id` → `project_info.project_order_id`（N:1）
- → `task_dispatch_node.task_dispatch_id`（1:N）
- → `task_dispatch_extend.task_dispatch_id`（1:1）
- → `task_handle_extension.task_dispatch_id`（1:N）

---

## 3. task_dispatch_node（任务派发节点表）

**表描述**：任务派发下的各节点实例，如 "通知启动"→"启动"→"启动完成" 等节点流转记录。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_task_dispatch_id`
    - `idx_project_order_id`
    - `idx_order_no`
    - `idx_node_type + idx_process_status`
    - `idx_executor_id`
    - `idx_u_task_code`（唯一索引）
    - `idx_estimated_time`
    

### 字段清单

表格

| 字段                  | 类型        | 说明                            |
| ------------------- | --------- | ----------------------------- |
| id                  | BIGINT    | 主键，自增                         |
| task_dispatch_id    | BIGINT    | 任务调度 ID，FK → task_dispatch.id |
| template_node_id    | BIGINT    | 模板节点 ID                       |
| order_no            | VARCHAR   | 主材订单号                         |
| project_order_id    | BIGINT    | 项目订单 ID                       |
| home_order_no       | VARCHAR   | 主单号                           |
| package_code        | VARCHAR   | 施工包 code                      |
| node_type           | INTEGER   | 节点类型 → NodeTypeEnum           |
| remarks             | VARCHAR   | 备注                            |
| images              | VARCHAR   | 图片（逗号分隔）                      |
| notice_retain_time  | TIMESTAMP | 通知保留时间                        |
| platform_check_time | TIMESTAMP | 平台考核时间                        |
| estimated_time      | TIMESTAMP | 预估开始时间                        |
| promise_time        | TIMESTAMP | 对客承诺考核时间                      |
| start_time          | TIMESTAMP | 实际开始时间                        |
| end_time            | TIMESTAMP | 实际结束时间（表单时间）                  |
| submit_time         | TIMESTAMP | 提交时间（系统时间）                    |
| submit_name         | VARCHAR   | 提交人姓名                         |
| submit_by           | VARCHAR   | 提交人 ID                        |
| executor_types      | VARCHAR   | 多角色类型（逗号分隔）                   |
| executor_type       | INTEGER   | 执行人角色 → RoleTypeEnum          |
| executor_id         | VARCHAR   | 执行人 ID                        |
| executor_name       | VARCHAR   | 执行人姓名                         |
| process_status      | TINYINT   | 进程状态 → ProcessStatusEnum      |
| qualified           | INTEGER   | 审核结果 → QualifiedEnum          |
| state               | TINYINT   | 状态 (0 = 无效，1 = 有效)            |
| process_code        | VARCHAR   | 流程 code                       |
| node_code           | INTEGER   | 节点编码                          |
| task_code           | VARCHAR   | 任务编码                          |
| u_task_code         | VARCHAR   | 唯一任务编码                        |
| restart             | TINYINT   | 重启次数                          |
| restart_remind      | INTEGER   | 重启是否提示                        |
| workbench_sync      | INTEGER   | 工作台同步 (0 = 否，1 = 是)           |
| remind              | INTEGER   | 是否提示                          |
| dispatch_types      | VARCHAR   | 派出角色类型                        |
| delay_day           | INTEGER   | 延期天数                          |
| readiness_status    | INTEGER   | 就绪状态 → ReadinessStatusEnum    |
| attachment          | VARCHAR   | 附件                            |
| process_batch       | INTEGER   | 批次号                           |
| remarks_filter      | VARCHAR   | 无法复尺原因筛选项                     |
| create_by           | VARCHAR   | 创建人                           |
| modify_by           | VARCHAR   | 修改人                           |
| modify_name         | VARCHAR   | 修改人姓名                         |
| gmt_create          | TIMESTAMP | 创建时间                          |
| gmt_modified        | TIMESTAMP | 修改时间                          |

### 状态枚举

- `node_type` (NodeTypeEnum): {1: 开始节点，20: 通知可启动 (约工), 40: 通知启动 (派单), 50: 进场，60: 启动 (提交自检), 65: 自检验收，80: 启动验收 (实地验收), 85: 业主确认}；变更类型: {200: 变更单，201: 撤销单}
- `process_status` (ProcessStatusEnum): {1: 未激活，2: 激活未完成，3: 激活已完成，4: 暂停激活}
- `qualified` (QualifiedEnum): {1: 合格，2: 不合格}
- `executor_type` (RoleTypeEnum): {0: 暂无，1: 业主，2: 项目经理，3: 管家，4: 家装设计师，5: 材料员，6: 供应商，7: 全能安装工，8: 安装组长，9: 承运商，10: 安装工派单员，11: 仓库管理员，12: 保洁工，13~29: 各类工种，30: 交付工程师，31: 定制设计师，32: 定制派单员，33: 质检员，34: 跟单员，35: 橱柜设计师，36: 橱柜经理，37~50: 综合工 / 团装角色，97~112: 销售角色...}
- `readiness_status` (ReadinessStatusEnum): {0: 未就绪，1: 已就绪}

### 表间关联

- `task_dispatch_id` → `task_dispatch.id`（N:1）
- `project_order_id` → `project_info.project_order_id`（N:1）
- → `task_node_progress.task_dispatch_node_id`（1:N）
- → `task_dispatch_extend.task_dispatch_node_id`（1:1）
- → `task_handle_extension.task_dispatch_node_id`（1:N）
- → `measure_material_detail.task_dispatch_node_id`（1:N）

---

## 4. task_dispatch_extend（任务扩展表）

**表描述**：`task_dispatch` / `task_dispatch_node` 的扩展信息表，存放用量确认、变更单关联、SKU 等信息。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_task_dispatch_id`
    - `idx_task_dispatch_node_id`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|task_dispatch_id|BIGINT|任务 ID，FK → task_dispatch.id|
|task_dispatch_node_id|BIGINT|任务节点 ID，FK → task_dispatch_node.id|
|usage_confirm|INTEGER|用量确认|
|usage_confirm_time|TIMESTAMP|用量确认时间|
|project_change_no|VARCHAR|变更单号|
|project_change_status|INTEGER|变更单状态|
|has_order|INTEGER|是否下单|
|biz_version|INTEGER|业务版本|
|sku_info|VARCHAR|SKU 信息|
|create_time|TIMESTAMP|创建时间|
|update_time|TIMESTAMP|更新时间|

### 表间关联

- `task_dispatch_id` → `task_dispatch.id`（N:1）
- `task_dispatch_node_id` → `task_dispatch_node.id`（N:1）

---

## 5. task_node_progress（节点进度跟进表）

**表描述**：记录任务节点的处理进度和延期跟踪，用于客服 / 管家跟进问题节点。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_project_order_id`
    - `idx_task_dispatch_node_id`
    - `idx_cur_task_status`
    - `idx_creator_id`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|project_order_id|BIGINT|项目订单 ID|
|task_dispatch_node_id|BIGINT|主材任务节点 ID，FK → task_dispatch_node.id|
|task_handle_type|INTEGER|处理类型 {0: 待处理，1: 已处理，2: 挂起，3: 关单，4: 无需处理}|
|pre_task_status|INTEGER|原任务状态 {0: 待处理，1: 处理中，2: 已关单，3: 待跟进}|
|cur_task_status|INTEGER|当前任务状态 {0: 待处理，1: 处理中，2: 已关单，3: 待跟进}|
|next_trace_time|VARCHAR|下次跟进时间 (yyyy-MM-dd)|
|image_url|VARCHAR|附件图片|
|remark|VARCHAR|备注|
|status|INTEGER|状态 (0 = 无效，1 = 有效)|
|creator_id|BIGINT|处理人 ID|
|creator_name|VARCHAR|处理人名称|
|finish_time|VARCHAR|完成时间 (yyyy-MM-dd)|
|delay_reason|VARCHAR|超期原因|
|responsible_party|VARCHAR|责任方|
|handle_type|INTEGER|处理结果|
|delay_plan_handle_time|VARCHAR|延期预估处理时间 (yyyy-MM-dd)|
|delay_reason_type|INTEGER|延期原因分类|
|ctime|TIMESTAMP|创建时间|
|mtime|TIMESTAMP|修改时间|

### 表间关联

- `task_dispatch_node_id` → `task_dispatch_node.id`（N:1）
- `project_order_id` → `project_info.project_order_id`（N:1）

---

## 6. task_process_batch（任务批次表）

**表描述**：管理批量任务的批次生命周期，将同一项目的同类型任务归入批次统一激活调度。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_project_order_id`
    - `idx_task_type + idx_batch_type`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|project_order_id|BIGINT|项目订单 ID|
|task_type|INTEGER|任务类型 → TaskTypeEnum|
|batch_type|INTEGER|批次类型 {1: 第一批，2: 第二批}|
|task_dispatch_node_ids|VARCHAR|主材任务节点 ID（逗号分隔）|
|process_status|TINYINT|进程状态 → ProcessStatusEnum|
|node_type|INTEGER|节点类型 {20: 通知可启动，40: 通知启动，60: 启动，80: 启动完成，200: 变更单，201: 撤销单}|
|state|TINYINT|状态 (0 = 无效，1 = 有效)|
|notice_retain_time|TIMESTAMP|通知保留时间|
|active_time|TIMESTAMP|激活时间|
|estimated_time|TIMESTAMP|预估开始时间|
|executor_type|TINYINT|执行人类型|
|executor_id|VARCHAR|执行人 ID|
|executor_name|VARCHAR|执行人名称|
|remarks|VARCHAR|备注|
|create_by|VARCHAR|创建人|
|modify_by|VARCHAR|修改人|
|gmt_create|TIMESTAMP|创建时间|
|gmt_modified|TIMESTAMP|修改时间|

### 表间关联

- `project_order_id` → `project_info.project_order_id`（N:1）
- → `task_batch_node_relation.task_batch_id`（1:N，关联具体节点）

---

## 7. n_material_define（物料定义表）

**表描述**：物料配置的核心表，定义 "产品套餐 × 主材 × 供应商 × 任务类型" 维度的流程模板映射。是 `task_dispatch` 创建时的配置来源。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_material_code + idx_supplier_code`
    - `idx_product_combo_id`
    - `idx_process_code`
    - `idx_template_id`
    - 复合 key: `(material_code, supplier_code, task_type, mode, mdm_code)`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|product_combo_id|BIGINT|产品套餐 ID|
|product_combo_name|VARCHAR|套餐名|
|material_code|VARCHAR|主材编码|
|material_name|VARCHAR|主材名称|
|supplier_code|VARCHAR|供应商编码|
|supplier_name|VARCHAR|供应商名称|
|task_type|INTEGER|任务类型 → TaskTypeEnum|
|process_code|VARCHAR|流程定义唯一 code|
|source_name|VARCHAR|流程名|
|version|INTEGER|编辑版本号|
|process_version|INTEGER|流程版本号|
|state|TINYINT|状态 (0 = 无效，1 = 有效)|
|process_batch|INTEGER|批量批次号 {0: 单独处理，1: 第一批，2: 第二批}|
|source_type|TINYINT|来源类型 (0 = 供应链，1 = 测量申请单)|
|mode|INTEGER|模式 (1 = 北京，2 = 圣都)|
|sale_type|INTEGER|销售类型|
|mdm_code|VARCHAR|分公司 code|
|mdm_company_name|VARCHAR|分公司名称|
|work_type|VARCHAR|作业工种|
|template_id|BIGINT|模板 ID，FK → n_material_template.id|
|service_mode|INTEGER|服务模式 → ServiceModeEnum|
|create_by|VARCHAR|创建人|
|modify_by|VARCHAR|修改人|
|create_time|TIMESTAMP|创建时间|
|update_time|TIMESTAMP|修改时间|

### 表间关联

- `template_id` → `n_material_template.id`（N:1）
- → `material_task`（1:N，由物料定义展开的配置任务）

---

## 8. n_material_template（物料模板表）

**表描述**：物料流程模板定义表，是最顶层的配置实体。定义了一套完整的物料任务流程。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_template_code`（唯一）
    - `idx_state`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|name|VARCHAR|模板名称|
|template_code|VARCHAR|模板 code（唯一标识）|
|description|VARCHAR|描述|
|work_type|VARCHAR|工种描述（JSON）|
|range_type|TINYINT|范围类型 {1: 内部，2: 外部}|
|sale_type|INTEGER|销售类型（位运算，[整装，零售，搭售]）|
|version|INTEGER|版本号|
|state|TINYINT|状态 {0: 草稿，1: 有效，2: 删除，3: 失效，4: 审核中}|
|create_by|VARCHAR|创建人|
|modify_by|VARCHAR|修改人|
|create_time|TIMESTAMP|创建时间|
|update_time|TIMESTAMP|修改时间|

### 状态枚举

- `state` (TemplateStateEnum): {0: 草稿，1: 有效，2: 删除，3: 失效，4: 审核中，5: 草稿}
- `range_type`: {1: 内部，2: 外部}

### 表间关联

- → `n_material_define.template_id`（1:N）
- → `task_dispatch.template_id`（1:N）
- → `n_material_template_unit`（1:N，模板单元节点）
- → `n_material_process_template`（1:N，流程模板映射）

---

## 9. coordinator_task_order（跟单任务表 / CT 单）

**表描述**：处理返补、补货、问题工单的跟单任务。当安装 / 送货环节出现问题需要补发时创建。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_project_order_id`
    - `idx_ct_order_no`（唯一）
    - `idx_work_order_id`
    - `idx_order_no`
    - `idx_status`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|project_order_id|BIGINT|项目订单 ID|
|ct_order_no|VARCHAR|CT 单号|
|work_order_id|VARCHAR|问题单 ID|
|package_code|VARCHAR|施工包 code|
|material_code|VARCHAR|主材编码|
|material_name|VARCHAR|主材名称|
|supplier_code|VARCHAR|供应商编码|
|supplier_name|VARCHAR|供应商名称|
|brand_order_no|VARCHAR|工厂品牌主订单号|
|relate_order_no|VARCHAR|原销售订单号|
|order_no|VARCHAR|货单号|
|second_category_id|VARCHAR|二级分类 ID|
|compensation_type|INTEGER|返补类型 {1: 原厂返补，2: 当场返补}|
|third_replenish_order_no|VARCHAR|工厂子单号|
|sku_info|VARCHAR|SKU 信息|
|status|INTEGER|跟单任务状态 → CoordinatorTraceTaskStatusEnum|
|process_content|VARCHAR|工单内容描述|
|process_reason|VARCHAR|创建原因|
|solution|VARCHAR|解决方案|
|image_url|VARCHAR|附件图片|
|next_trace_time|TIMESTAMP|下次跟进时间|
|publish_user_id|VARCHAR|提出人 ID|
|publish_user_name|VARCHAR|提出人姓名|
|place_order_request|VARCHAR|下单请求入参|
|place_order_response|VARCHAR|下单返回|
|delivery_id|VARCHAR|送货方 ID|
|delivery_name|VARCHAR|送货方名称|
|remind_sub_order_no|VARCHAR|非一品多商子单号|
|if_multiple_supplier|INTEGER|是否命中一品多商|
|plan_time_status|INTEGER|计划提货时间状态 {0: 默认，1: 已提交}|
|create_time|TIMESTAMP|创建时间|
|update_time|TIMESTAMP|更新时间|

### 状态枚举

- `status` (CoordinatorTraceTaskStatusEnum): {0: 待处理，10: 处理中，20: 待跟进，30: 待下单，40: 已下单，50: 已提交提货时间，90: 已关单}
- `compensation_type`: {1: 原厂返补，2: 当场返补}
- `plan_time_status`: {0: 默认，1: 已提交}

### 表间关联

- `project_order_id` → `project_info.project_order_id`（N:1）
- → `coordinator_task_progress.coordinator_task_id`（1:N）

---

## 10. stock_up（备货周期表）

**表描述**：配置备货周期的核心表，定义物料 × 供应商维度的备货条件和时间规则。

### 基础属性

- 主键：`id` BIGINT，自增
- 索引（推断）：
    
    - `idx_material_code + idx_supplier_code`
    - `idx_cycle_type`
    

### 字段清单

表格

|字段|类型|说明|
|---|---|---|
|id|BIGINT|主键，自增|
|material_code|VARCHAR|主材编码|
|material_name|VARCHAR|主材名称|
|supplier_code|VARCHAR|供应商编码|
|supplier_name|VARCHAR|供应商名称|
|cycle_type|INTEGER|周期类型 {0: 未知，10: 测量周期，20: 复尺周期，30: 备货周期，40: 送货周期，50: 安装周期}|
|has_condition|INTEGER|有无条件 {0: 无条件，1: 有条件}|
|status|INTEGER|业务状态 {1: 待生效，2: 已生效}|
|state|INTEGER|记录状态 (0 = 无效，1 = 有效)|
|creator_id|BIGINT|创建人 ID|
|creator_name|VARCHAR|创建人名称|
|modifier_id|BIGINT|修改人 ID|
|modifier_name|VARCHAR|修改人名称|
|ctime|TIMESTAMP|创建时间|
|mtime|TIMESTAMP|修改时间|

### 状态枚举

- `cycle_type` (CycleTypeEnum): {0: 未知，10: 测量周期，20: 复尺周期，30: 备货周期，40: 送货周期，50: 安装周期}
- `has_condition` (HasConditionEnum): {0: 无条件，1: 有条件}
- `status` (StockUpStatusEnum): {1: 待生效，2: 已生效}
- `state`: {0: 无效，1: 有效}

### 表间关联

- → `stock_up_condition_rule.stock_up_id`（1:N，条件规则明细）

---

## 核心表 ER 关系图（文字版）

plaintext

```
n_material_template (模板)
    │ 1:N
    ▼
n_material_define (物料定义)
    │
    │ 配置驱动
    ▼
project_info (项目) ──1:N──▶ task_dispatch (任务派发)
                                    │ 1:N
                                    ▼
                            task_dispatch_node (任务节点)
                                    │ 1:N
                            ┌───────┼───────────┐
                            ▼       ▼           ▼
                    task_node       task_       measure_
                    _progress    dispatch_     material_
                    (进度跟踪)   extend       detail
                                (扩展信息)    (测量明细)

task_process_batch (批次)
    │ 1:N
    ▼
task_batch_node_relation (批次-节点关联)

coordinator_task_order (跟单CT单)
    │ 1:N
    ▼
coordinator_task_progress (跟单进度)

stock_up (备货周期)
    │ 1:N
    ▼
stock_up_condition_rule (备货条件规则)
```

下面以**家装主材全生命周期**为主线，按业务流转顺序拆解每个阶段的业务动作、对应数据表及数据流转逻辑，完整覆盖 10 张核心表，清晰呈现每张表在流程中的定位。

整体流程主线：

> 前置静态配置 → 项目订单创建 → 任务实例化生成 → 节点按序流转执行 → 批量统一调度 → 异常节点跟进 → 售后返补闭环

---

## 阶段一：前置静态配置阶段（项目上线前，运营 / 后台维护）

**业务场景**：提前配置好所有主材的流程规则、模板、周期标准，是所有业务运行的基础，不随单个项目变动。

**核心操作**：配置流程模板、绑定物料与模板的映射、设置各环节备货 / 履约周期

**涉及核心表**：

1. **`n_material_template`（物料模板表）**
    
    - 作用：定义一套完整的主材流程节点序列（比如 “橱柜标准安装流程”“瓷砖送货安装流程”），是流程的顶层配置实体。
    - 输出：模板 ID、节点规则、适用范围、版本状态。
    
2. **`n_material_define`（物料定义表）**
    
    - 作用：按「产品套餐 + 主材品类 + 供应商 + 任务类型 + 分公司」维度，绑定对应的流程模板，建立 “业务场景→流程模板” 的映射规则。
    - 输出：物料与模板的匹配关系，后续任务生成时按此规则自动匹配。
    
3. **`stock_up`（备货周期表）**
    
    - 作用：按「主材 + 供应商」维度，配置测量、复尺、备货、送货、安装各环节的标准周期天数与生效条件。
    - 输出：各环节标准周期，用于计算节点预估时间、对客承诺考核时间。
    

**附表关联**：`stock_up_condition_rule` 存储周期的条件规则明细。

---

## 阶段二：项目订单创建阶段（签单落位）

**业务场景**：客户签单后，在系统创建家装项目，生成项目主数据。

**核心操作**：录入项目基础信息、客户信息、房屋信息、订单信息

**涉及核心表**：

- **`project_info`（项目信息表）**
    
    - 作用：存储项目全生命周期的核心基础信息，生成全局唯一的 `project_order_id`，作为所有后续任务、工单的关联主键，是整个系统的中心实体。
    - 输出：项目主数据 + 全局关联键 `project_order_id`，全流程所有业务表均通过该字段关联项目。
    

---

## 阶段三：任务派发实例化阶段（项目下单后自动生成任务）

**业务场景**：项目下单后，系统根据项目包含的主材品类，自动匹配配置规则，批量生成各主材的任务单与流程节点。

**核心操作**：匹配物料配置 → 实例化任务主单 → 生成全量节点实例 → 计算节点时间

**涉及核心表**：

1. 读取配置源：`n_material_define` + `n_material_template`
    
    - 逻辑：根据项目的套餐、分公司、主材品类、供应商，匹配到对应的流程模板 ID 与节点规则。
    
2. 生成任务主记录：**`task_dispatch`（任务派发表）**
    
    - 作用：每个主材品类生成一条任务记录，关联项目 ID、模板 ID，记录任务类型、主材 / 供应商信息、流程状态等。
    - 数据来源：项目基础信息 + 物料定义配置规则。
    
3. 初始化节点实例：**`task_dispatch_node`（任务派发节点表）**
    
    - 作用：根据模板的节点序列，为每个任务生成全套流程节点（初始状态为 “未激活”）；同时读取 `stock_up` 的周期规则，计算每个节点的预估开始时间、对客承诺时间。
    - 关联关系：1 条 `task_dispatch` 对应 N 条 `task_dispatch_node`。
    
4. 生成扩展信息：**`task_dispatch_extend`（任务扩展表）**
    
    - 作用：同步生成任务 / 节点对应的扩展记录，后续存放用量确认、SKU、变更单等非高频核心字段。
    - 关联关系：与任务、节点为 1:1 对应关系。
    

---

## 阶段四：节点流转执行阶段（核心作业全流程）

**业务场景**：从测量开始，经复尺、下单、备货、送货、安装到最终验收，节点按预设顺序依次激活、处理、提交，状态逐层流转。

**核心操作**：节点激活 → 执行人处理 → 提交结果 → 状态更新 → 激活下一节点

**涉及核心表**：

1. **`task_dispatch_node`（任务派发节点表）**
    
    - 核心操作表：节点激活时更新 `process_status`（未激活→激活未完成）；处理完成提交时更新状态（激活未完成→激活已完成），同时写入实际开始 / 结束时间、执行人、审核结果、附件图片、备注等信息。
    - 关键枚举：节点类型、进程状态、审核结果、执行人角色。
    
2. **`task_dispatch`（任务派发表）**
    
    - 同步更新：节点流转时，同步更新任务主表的 `current_node_type`（当前节点）、`process_status`（整体进程状态），保持任务主表与节点状态一致。
    
3. **`task_dispatch_extend`（任务扩展表）**
    
    - 业务数据回填：测量节点回填用量确认、SKU 信息；发生变更时回填变更单号、变更状态；下单节点回填是否下单标识。
    

**附表关联**：

- `measure_material_detail`：测量节点提交时，写入具体测量尺寸、物料规格明细。
- `task_handle_extension`：节点处理过程中的扩展操作记录。

---

## 阶段五：批量调度阶段（同类型节点统一激活派工）

**业务场景**：同一项目下，多个同类型、同批次的节点（比如第一批所有主材的 “通知启动” 节点），不需要逐个激活，通过批次统一调度、统一派工。

**核心操作**：创建批次 → 关联节点 → 批量激活 → 指派执行人

**涉及核心表**：

- **`task_process_batch`（任务批次表）**
    
    - 作用：按「项目 + 任务类型 + 批次类型」创建批次，关联多个任务节点 ID，统一设置激活时间、执行人，批量更新节点状态。
    - 关联关系：1 个批次对应 N 个任务节点，通过附表 `task_batch_node_relation` 存储关联关系。
    

---

## 阶段六：异常节点跟进阶段（延期 / 问题督办）

**业务场景**：节点出现延期、异常、驳回等问题时，客服 / 管家创建跟进记录，跟踪处理进度，直到问题闭环。

**核心操作**：创建跟进工单 → 记录延期原因与责任方 → 设置下次跟进时间 → 更新处理状态

**涉及核心表**：

- **`task_node_progress`（节点进度跟进表）**
    
    - 作用：关联具体异常节点，记录每次跟进的详情，包括原状态、当前状态、延期原因、责任方、处理结果、下次跟进时间。
    - 关联关系：1 个任务节点可以对应 N 条跟进记录，支持多次跟进直到关单。
    

---

## 阶段七：售后返补阶段（问题工单闭环）

**业务场景**：送货、安装环节出现货损、缺件、尺寸错误等问题，需要补发、返工时，创建专属跟单任务处理售后。

**核心操作**：创建 CT 跟单 → 记录问题与方案 → 跟踪补货进度 → 关单闭环

**涉及核心表**：

- **`coordinator_task_order`（跟单任务表 / CT 单）**
    
    - 作用：关联项目 ID，记录返补类型、问题描述、解决方案、跟单状态、下次跟进时间等，是独立于主流程的售后闭环载体。
    

**附表关联**：`coordinator_task_progress` 存储每条跟单的多次跟进记录。

---

## 完整流程总览表

表格

|流程阶段|核心业务动作|核心数据表|辅助 / 关联附表|
|:--|:--|:--|:--|
|前置配置|流程模板、物料映射、周期规则维护|n_material_template、n_material_define、stock_up|stock_up_condition_rule|
|项目创建|签单生成项目主数据|project_info|-|
|任务生成|按配置实例化任务与节点|task_dispatch、task_dispatch_node、task_dispatch_extend|-|
|节点执行|测量→复尺→下单→送货→安装→验收全流转|task_dispatch_node、task_dispatch、task_dispatch_extend|measure_material_detail、task_handle_extension|
|批量调度|同批次节点统一激活派工|task_process_batch|task_batch_node_relation|
|异常跟进|延期 / 问题节点督办|task_node_progress|-|
|售后返补|货损缺件补发跟单|coordinator_task_order|coordinator_task_progress|

### 补充说明

整个流程以 `project_info.project_order_id` 为全局串联键，由配置层的静态规则驱动运行层的动态数据生成；主流程走「任务→节点」链路，异常和售后走独立跟进链路，数据分层清晰，关联关系明确。

---
## 相关文档

- [[starlord数据库表/所有的starlord数据库表]] — 全库82张表的分层完整目录
- [[starlord数据库表/数据库打标签]] — 表的A/B/C/D分类标签
- [[starlord数据库表/数据库个人总结]] — 个人接口→数据表映射速查
- [[starlord数据库表/starlord全量REST接口与数据表映射]] — REST接口与数据表关联
- [[starlord项目整理/业务知识沉淀/edar-starlord系统新人学习文档]] — 核心表对应的业务功能