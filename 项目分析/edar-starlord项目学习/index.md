# Wiki Index

> Content catalog. Every wiki page listed under its type with a one-line summary.
> Read this first to find relevant pages for any query.
> Last updated: 2026-07-01 | Total pages: 24

## Entities (Services)

### 交付核心
- [[material-delivery]] — 物料送货任务查询，区分单独/批量送货模式。
- [[material-deliver-engineer]] — 工程师送货任务管理，交付流程节点处理。
- [[delivery-material-biz]] — 配送主材业务V2：SCM事件驱动的物料任务创建与取消。
- [[dispatch-activate]] — 任务调度激活：定时扫描到期任务并启动创建流程。

### 交付流程配置
- [[delivery-flow-category]] — 交付流程品类分类管理：创建、复制、应用、检查。
- [[category-process]] — 品类流程路由定义：节点编排、转移条件、时间规则。
- [[delivery-flow-rule]] — 交付流程规则生效/检查/查询。
- [[delivery-flow-task-node]] — 交付流程任务节点与调度模板管理。

### 物料流程
- [[material-flow-rule]] — 物料流程规则引擎：匹配分公司+品类+套餐→流程模板。
- [[material-flow-query]] — 物料流程统一配置查询，应用层数据查询。

### 任务调度
- [[task-dispatch-v2-service]] — 任务派发V2核心：任务/节点直接持久化与管理。
- [[task-dispatch-common-service]] — 任务调度通用服务：实例查询、详情组装、流程定义。
- [[task-dispatch-complete-service]] — 任务完成服务：单节点完成、整体任务完成。
- [[material-task-biz-v2-service]] — 物料任务业务V2：页面驱动的任务CRUD操作。
- [[material-schedule-service]] — 物料排程服务：日历视图、日报生成、Push推送。

### 延期与验收
- [[material-delay-process]] — 物料延期处理：创建、确认、撤销延期单及Kafka通知。
- [[acceptance-report]] — 验收报告：安装工验收模板获取、提交、合并、批量验收。

### 返补协调
- [[coordinator-task]] — 返补单协调器：下单、分配协调人、跟进、状态同步。

### 角色工作台
- [[material-customer]] — 客户主材进展：C端物料进度聚合查询。
- [[material-butler]] — 管家派工：待派工、可改约、派工统计、历史记录。

### 总览
- [[project-overview]] — edar-starlord 项目总览：模块结构、业务域划分、入口点全景。

## Concepts
- [[delivery-architecture]] — 交付流程架构：分层、V1/V2、入口点类型、关键调用链。
- [[state-machines]] — 状态机汇总：TaskDispatch/TaskDispatchNode/延期单流程状态。
- [[delivery-flow-config]] — 交付流程配置驱动：品类规则引擎匹配逻辑。
- [[system-boundary-api]] — 系统边界 API 层：46个Feign接口，对外暴露的数据与外部系统依赖。
- [[external-systems]] — 外部系统清单：12个依赖的第三方系统及回调关系。
- [[system-boundary-controller]] — 系统边界 Controller 层：80个Controller、430+ REST端点。
- [[system-responsibilities]] — 系统职责清单：从Controller+Service推导的10大核心业务能力。

## 相关外部文档

> 以下文档位于 `starlord项目整理/` 和 `starlord数据库表/` 下，与此Wiki内容互补。

- [[starlord项目整理/主材全链路业务与系统配置总结]] — 全链路业务与系统配置总览
- [[starlord项目整理/主材任务流程梳理]] — 主材任务全流程流转图（Mermaid）
- [[starlord项目整理/安装流程]] — 安装任务节点流转详析
- [[starlord项目整理/复尺流程]] — 复尺任务节点流转详析
- [[starlord项目整理/全部枚举类型梳理]] — 全量枚举类型分组一览
- [[starlord项目整理/接口-动作映射表]] — 接口与业务动作映射
- [[starlord项目整理/业务知识沉淀/edar-starlord系统新人学习文档]] — 系统新人学习入口（业务视角）
- [[starlord数据库表/所有的starlord数据库表]] — 全库82张表的分层目录
- [[starlord项目整理/REST汇总/starlord接口字典文档]] — 748个REST接口总字典
