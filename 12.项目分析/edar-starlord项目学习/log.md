# Wiki Log

> Chronological record of all wiki actions. Append-only.
> Format: `## [YYYY-MM-DD] action | subject`

## [2026-07-01] create | Wiki initialized for edar-starlord delivery module
- Domain: 家装业务交付模块
- Structure created with SCHEMA.md, index.md, log.md

## [2026-07-01] ingest | edar-starlord 项目全面扫描
- Scanned 6 Maven modules: web, service, manager, dao, api, base
- Identified 81 REST Controllers, 46 Feign interfaces, 23 Kafka Listeners
- Created 21 entity pages covering core delivery services
- Created 3 concept pages: delivery-architecture, state-machines, delivery-flow-config
- Cross-references established between all pages
- Total pages: 24

### Pages created (entities):
- project-overview.md — 项目总览
- material-delivery.md — 物料送货服务
- material-deliver-engineer.md — 工程师送货服务
- delivery-material-biz.md — 配送主材业务V2
- dispatch-activate.md — 任务调度激活
- delivery-flow-category.md — 交付流程品类分类
- category-process.md — 品类流程路由
- delivery-flow-rule.md — 交付流程规则
- delivery-flow-task-node.md — 交付流程任务节点
- material-flow-rule.md — 物料流程规则引擎
- material-flow-query.md — 物料流程查询
- task-dispatch-v2-service.md — 任务派发V2
- task-dispatch-common-service.md — 任务调度通用
- task-dispatch-complete-service.md — 任务完成服务
- material-task-biz-v2-service.md — 物料任务业务V2
- material-schedule-service.md — 物料排程服务
- material-delay-process.md — 物料延期处理
- acceptance-report.md — 验收报告
- coordinator-task.md — 返补单协调器
- material-customer.md — 客户主材进展
- material-butler.md — 管家派工

### Pages created (concepts):
- delivery-architecture.md — 交付流程架构
- state-machines.md — 状态机汇总
- delivery-flow-config.md — 交付流程配置驱动

### Entry point tracing completed:
- Each service page documents all Controller, Feign, Kafka Listener, and Scheduled Task entry points
- Cross-service calls traced via search_files across modules

## [2026-07-01] create | 系统边界分析系列
- Created 4 new concept pages documenting system boundaries
- system-boundary-api.md — 系统边界 API 层：46个 Feign 接口，8个对外业务域 + 12个依赖外部系统
- external-systems.md — 外部系统清单：ke-diting(谛听)、wechat-message、call-service、scm-beiwo、ES、big-c、cipher-feign、customer-home、permission-service、utopia-nrs-sales-project
- system-boundary-controller.md — 系统边界 Controller 层：80个 Controller、430+ REST 端点、27个业务域
- system-responsibilities.md — 系统职责清单：10大核心业务能力（任务调度引擎、送货管理、模板配置、进度可视化、安装工/管家/业主端、验收、延期、自购、配置中心、外部对接）
- Updated index.md with 4 new entries under Concepts
