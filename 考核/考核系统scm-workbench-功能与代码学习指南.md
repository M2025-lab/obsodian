# 考核系统 scm-workbench — 功能与代码学习指南

> 项目路径:`/Users/mirror/IdeaProjects/scm-workbench`
> Git:`https://git.lianjia.com/utopia/supply-chain/scm-workbench.git`
> 别名:workbench / 考核(见 `📚 后端项目索引.md`)
> 整理时间:2026-10-30(基于代码全量扫描:28 个 Controller、20 个 API 接口、32 张 MySQL 表 + 1 张 Hive 表)

---

## 一、这个系统是干什么的?

**一句话:scm-workbench 是供应链履约时效考核系统 —— 对家装建材供应链全链路各环节(下单、接单、备货、报价、通知送货、送达、签收等)的完成时效进行自动化考核评分,并管理对应的履约任务(待办卡片)。**

### 1.1 考核谁?

| configType | 考核对象 |
|---|---|
| 1 | 贝壳内部人员(如采购下单员) |
| 2 | 外部供应商 / 商家 |

### 1.2 考核什么?(10 个核心时效指标)

下单及时、接单及时、备货及时、入仓及时、测量报价及时、报价确认及时、报价审核通过及时、通知送货及时、送达及时、签收及时。

每个指标 = **时间参考点 + 时间间隔 + 日历类型(工作日/自然日)**,例如"接单节点起,自然日 3 天内完成送达"。计算时通过假期字典跳过节假日。

### 1.3 两套考核模型(重点,新老并行)

| 模型 | 场景 | 核心机制 | 核心类 |
|---|---|---|---|
| **旧模型:全国材料考核** | 非排程项目 | DAG(有向无环图)+ 事件节点驱动 | `EvalOrderService` + `IndicatorDAGService` |
| **新模型:排程材料考核** | 排程项目(白名单 `schedule_project`) | 施工工期排程节点 + 配送流程节点配置驱动 | `EvalScheduleOrderService` + `IndicatorScheduleDAGService` |

两套都由 OFC(履约中控)的 Kafka 节点变更消息实时触发。区分入口:`EdarStarlordRpc.queryProjectInScheduleSwitch()` 判断项目是否走排程。

### 1.4 谁在用?

- **供应链管理人员**:配底线指标(全国统一底线,如"自然日 3 天")
- **城市运营人员**:配一城一策指标(按战区/大区/分公司/店铺/送货方维度的个性化配置,个性化间隔不能大于底线)
- **端上用户(管家/供应商等)**:看履约任务待办卡片
- **数据分析团队**:通过 ES / Hive / StarRocks 报表看考核结果

### 1.5 附带的第二块业务:履约任务管理

除考核外,系统还负责**履约任务(待办)的全生命周期**:生成/激活/完成/取消接单、备货、进仓、通知送货、批次配送、签收等任务卡片,并同步到 VSS、OFW、主材任务(starlord)等外部系统。

---

## 二、工程结构(六个 Maven 模块)

| 模块 | 职责 | 关键包 |
|---|---|---|
| `scm-workbench-api` | 对外 Feign 接口定义(发布 jar 供上游调用),20 个接口 | `api/read/`、`api/write/` |
| `scm-workbench-base` | 公共 DTO/VO/Param/枚举/常量(纯 POJO) | `base/dto/`、`base/param/`、`base/constant/`(**所有枚举都在这**) |
| `scm-workbench-dao` | 数据访问:MyBatis Mapper + XML + DO + DAO 封装 | `mapper/`、`domain/`、`dao/`、`condition/`、`resources/mapper/` |
| `scm-workbench-service` | 核心业务逻辑(269 个文件) | `service/read/`、`service/write/`、`service/rpc/`(外部RPC)、`service/mq/`+`service/listener/`(消息)、`service/redis/`、`service/config/`(Apollo) |
| `scm-workbench-job` | **独立 Spark 批处理项目**(不在父 POM modules 中,spark-submit 提交) | `job/JavaJobDemo`(批量重算)、`stream/Demo`(样例) |
| `scm-workbench-start` | 启动入口 + 全部 Controller + 拦截器 | `controller/read/`、`controller/write/`、`controller/write/back/`(后门)、`configuration/` |

技术栈:Spring Boot + MyBatis-Plus(链家封装)+ Feign/Eureka/Ribbon(**无 Dubbo**)+ Kafka(utopia-event-driven 事件框架)+ Redis(Redisson)+ ES + Apollo + Sentinel + lianjia-schedule。

- 启动类:`scm-workbench-start/.../workbench/Application.java`
- 服务名:`scm-workbench`,端口 8080
- **鉴权**:内部微服务,不校验登录。上游网关鉴权,`OperatorInterceptor` 从请求头(`X-KE-UCID`、`X-KE-USER-NAME`、`X-KE-SELECT-ROLE` 等)提取用户信息放入 ThreadLocal,Feign 调用时透传。

---

## 三、核心接口(28 个 Controller / 9 大业务域)

**路径约定**:所有 Controller 无类级 `@RequestMapping`,路径全在方法级;业务接口前缀 `/api/workbench/...` 或 `/v2/api/workbench/...`(v2 = 2.5 重构后新版);后门接口前缀 `/api/scm-workbench/back-door/...` 和 `/api/refresh-data/...`。

### ★ 最核心的几组接口(优先掌握)

#### 1. 指标配置(考核规则怎么配)

| 接口 | 方法 | 作用 |
|---|---|---|
| `/v2/api/workbench/indicator-base-config/createOrUpdate` | POST | **创建/编辑底线指标**(新版) |
| `/v2/api/workbench/indicator-cus-config/createOrUpdate` | POST | **创建/编辑一城一策指标**(新版) |
| `/v2/api/workbench/indicator-config/delIndicatorConfig` | POST | 删除指标配置 |
| `/v2/api/workbench/indicator/base/list` (及 `/export`) | POST | 底线指标列表/导出 |
| `/v2/api/workbench/indicator/cus/list` (及 `/export`) | POST | 一城一策指标列表/导出 |
| `/v2/api/workbench/indicator/base/detail`、`/cus/detail` | GET | 指标详情 |
| `/v2/api/workbench/indicator/computeRule` | GET | 查询品类计算规则 |
| `/v2/api/workbench/indicator/version`、`/history` | GET | 规则版本/变更历史 |
| `/api/workbench/indicator-config/page-query` 等 | POST | 旧版配置查询(`IndicatorConfigQueryController`,17 个接口) |
| `/api/workbench/indicator/history/list` | POST | 指标版本历史列表 |

对应 Controller:`IndicatorQueryController`(V2 查询)、`IndicatorBaseConfigWriteController`、`IndicatorCusConfigWriteController`、`IndicatorConfigQueryController`(旧版)、`IndicatorHistoryController`。

#### 2. 考核计算与结果(考核怎么跑、结果在哪)

| 接口 | 方法 | 作用 |
|---|---|---|
| `/api/workbench/order/indicator/computeAndSaveEvalResult` | POST | **计算并保存指标考核结果**(核心计算入口,可手动触发) |
| `/api/workbench/order/indicator/eval/result` | GET | 查询订单指标计算结果 |
| `/api/workbench/order/indicator/order` | GET | 查询订单指标已计算结果 |
| `/api/workbench/indicator/groupingEval/query` | POST | 按条件分组查询指标评估结果 |
| `/api/workbench/indicator/count/group` | POST | 订单总数分组统计 |
| `/api/workbench/biz/queryOrderTime` | POST | 查询项目/销售单考核时间(给业务方用) |
| `/api/workbench/biz/queryOrderNodeRule`、`queryDispatchNodeRule` | GET | 查询货单/用工节点考核规则 |
| `/api/workbench/schedule/indicator/queryEval` | POST | 查询排程任务考核结果(新模型) |

对应 Controller:`OrderEvalController`、`IndicatorResultQueryController`、`BizQueryIndicatorOrderController`、`WorkbenchForScheduleController`。

#### 3. 履约任务(待办卡片怎么生成)

| 接口 | 方法 | 作用 |
|---|---|---|
| `/api/workbench/fulfillmentTask/create` | POST | **创建履约任务** |
| `/api/workbench/fulfillmentTask/cancel` | POST | 取消任务 |
| `/api/workbench/fulfillmentTask/complete` | POST | 完成待办任务 |
| `/api/workbench/fulfillmentTask/init` | POST | 初始化排程主材任务(货后) |
| `/api/workbench/fulfillmentTask/active` | POST | 激活排程主材任务 |
| `/api/workbench/fulfillmentTask/deal/dispatchTask` | POST | 创建下单/报价变更/设计任务(调度中轴) |
| `/api/workbench/fulfillmentTask/deal/replacement/refresh` | POST | 汰换场景考核重算 |
| `/api/workbench/fulfillmentTask/queryRole/subOrderNo`(及 batch/V2) | GET/POST | 查询订单待办角色 |
| `/api/workbench/todoTask/query`、`/frontend/todoTask/query` | POST | 待办任务查询(后端/前端) |
| `/api/workbench/todoTask/batchDelivery/finish` | POST | 完成批次送货任务 |

对应 Controller:`FulfillmentTaskController`(写)、`FulfillmentTaskQueryController`(读)、`TodoTaskWriteController`、`ToDoTaskQueryController`、`TodoTaskFrontEndController`(端上)。

#### 4. ES 履约进度查询(报表页面数据源)

| 接口 | 作用 |
|---|---|
| `/api/workbench/order/fulfillment/progress/es/query` | 订单指标履约进度分页查询 |
| `/api/workbench/order/fulfillment/progress/es/status/num/query` | 履约状态数量统计 |
| `/api/workbench/order/fulfillment/es/indicator/delay/num/query` | 指标延期数量统计 |
| `/api/workbench/order/fulfillment/trace/query` | 订单指标轨迹详情 |
| `/api/workbench/es/indicator/rePush/byIndicatorId`(及 subOrderList) | ES 数据重推(修数据用) |

对应 Controller:`IndicatorOrderQueryController`、`IndicatorEsQueryController`(在 `controller/read/es/` 包)。

### 其余业务域(了解即可)

| 业务域 | Controller | 代表接口 |
|---|---|---|
| 通用字典 | `CommonController`、`DicBranchCompanyQueryController` | 指标名 sug、字典查询、战区/大区/分公司树查询 |
| 外部集成 | `WorkbenchForVssController`、`HomeOrderChangeController` | 给 VSS 的履约进度查询;精装全案主订单号换绑回调 |
| 开城检查 | `IndicatorOpenCityCheckController` | `/api/workbench/indicator/open-city-check` 下单审批配置 |
| BCP 校验 | `BCPCheckController` | `/api/workbench/bcp/order/validate` |
| **运维后门** | `IndicatorBackDoorController`(21 个)、`FulfillmentTaskBackDoorController`(24 个) | `/api/scm-workbench/back-door/**`、`/api/refresh-data/**`:刷新/删除指标配置、重推事件、刷新签收、修任务执行人、手动完成下单任务等 —— **线上修数据全靠这两组** |
| 开发工具 | `ToolController`(20 个)、`TestController` | `/api/refresh/eval/order`(按货单重算)、`/api/refresh/eval/schedule/projectOrder`(重算新排程考核)、`/api/tool/projectAssessment`(查项目考核信息,供 agent 用)、节假日缓存管理、DAG 缓存清理 |

### 对外 Feign API(scm-workbench-api 模块,20 个)

上游系统引 jar 直接调,读接口在 `api/read/`,写在 `api/write/`。与 Controller 一一对应(Controller 实现 Api 接口),如 `IndicatorQueryApi`→`IndicatorQueryController`、`FulfillmentTaskWriteApi`→`FulfillmentTaskController`。**看某个接口给谁用、参数含义,直接看 api 模块里的接口定义和 DTO 最快。**

---

## 四、数据表(32 张 MySQL + 1 张 Hive)

### 4.1 表清单总览(按业务分组)

**① 指标配置——旧版(2.0 模式,JSON 内容存储)**

| 表 | 存什么 |
|---|---|
| `indicator_config` | 节点指标配置(指标名+订单模式+配置方,content 为 JSON) |
| `indicator_base_config` | 底线指标配置(按品类+订单模式) |
| `indicator_cus_config` | 一城一策配置(品类+战区/大区/区域/分公司/店铺/送货方多维颗粒度) |
| `indicator_branch_config` | 城市指标 ↔ 分公司关联(`ict_id`→cus_config.id) |
| `indicator_category_config` | 城市指标品类规则汇总 |
| `indicator_config_history` | 旧版配置变更历史(old_content/new_content/version_key) |

**② 指标配置——新版(2.5 重构,`doc/refactor2.5.sql`)**

| 表 | 存什么 |
|---|---|
| `n_indicator_base_config` | 新版底线维度表(指标名+配置方+考核节点+订单类型+角色) |
| `n_indicator_cus_config` | 新版城市维度表(+颗粒度等级/区域/分公司/店铺/送货方) |
| `indicator_compute_rule` | **新版核心规则表**:`source_id`+`source_type`(0=底线→n_base,1=城市→n_cus)关联维度表;`from_nodes`/`to_node` 定义节点间规则;`base_compute_rule`/`cus_compute_rule` 存计算规则 JSON;`version`+`start_time`/`end_time` 做版本管理 |
| `indicator_compute_rule_history` | 计算规则变更历史 |

**③ 考核订单与结果(考核引擎的输入输出)**

| 表 | 存什么 |
|---|---|
| `indicator_order` | **货单级考核宽表(最核心)**:订单号/品类/配送方式/分公司/商家/供应商/当前节点/订单状态 + `base_eval_result(_new)`、`cus_eval_result(_new)` 存底线/城市考核结果 JSON 数组 |
| `indicator_project_order` | 项目订单级考核结果(按项目+品类+配送维度聚合,"下单及时"在这一级) |
| `indicator_task_dispatch_node` | 用工任务节点维度考核结果(`eval_result` JSON,task_id→fulfillment_task) |

**④ 考核事件与 DAG**

| 表 | 存什么 |
|---|---|
| `indicator_event` | DAG 事件节点定义(code=品类+ofcCode,event_type,task_info) |
| `indicator_base_event` | 排程考核基础事件(按工序编码 ofcCode) |
| `construction_schedule_snapshot` | 项目工期变更快照 |

**⑤ 履约任务**

| 表 | 存什么 |
|---|---|
| `fulfillment_task` | **履约任务主表**:任务类型/状态/执行角色/ucid/考核时间窗(assessment_start_time、assessment_end_time、assessment_time)/完成取消时间 |
| `fulfillment_task_ext` | 任务扩展(replacement 汰换标记等) |
| `batch_delivery_info` | 批次送货信息(project_code+batch_order+预计到达时间) |

**⑥ 字典**

| 表 | 存什么 |
|---|---|
| `dic_branch_company` | 分公司→大区→战区层级字典 |
| `dic_holiday` | 假期字典(按日标记,考核算工作日时跳过) |
| `indicator_holiday` | 考核引擎专用节假日配置 |
| `dic_rule_node` | 计算规则节点字典(node_type 工期/订单节点、order_mode 2.0/2.5、是否测量节点、配送模式、节点 code+name) |

**⑦ 基础设施**

| 表 | 存什么 |
|---|---|
| `event_pub` / `event_sub` | utopia-event-driven 可靠事件表(发布/订阅,消息落地去重) |
| `event_sub_2025_q1~q4` | event_sub 按季度归档分表 |
| `redo_log` | 通用数据变更日志(补偿/审计) |
| `sync_task` | 异步数据同步任务(重试次数/状态/结果) |
| `schedule_project` | 排程考核白名单(project_order_id,在名单内才走新模型) |

**⑧ 离线**

| 表 | 存什么 |
|---|---|
| Hive/StarRocks `rpt.rpt_indicator_eva_da` | 指标评估日报宽表:把 `indicator_order` 的 eval_result JSON explode 打平成指标粒度,JOIN 子单维度,供报表/大屏(DDL 在 `doc/hive/`) |

### 4.2 核心关联关系

```
【配置层(新版)】
n_indicator_base_config ─(id=source_id, source_type=0)─┐
                                                       ├→ indicator_compute_rule → (变更) → indicator_compute_rule_history
n_indicator_cus_config ──(id=source_id, source_type=1)─┘        (from_nodes/to_node 引用 dic_rule_node.code)
        ↑ ict_id
indicator_branch_config (关联分公司 ← dic_branch_company)

【执行层】
indicator_event (DAG节点) + indicator_compute_rule (DAG边) ⇒ 内存 DAG
        ↓ OFC消息触发计算
indicator_order (货单级结果 base/cus_eval_result_new)
        ├→ indicator_project_order (项目级结果, project_order_id 关联)
        ├→ indicator_task_dispatch_node (用工任务结果, order_no 关联)
        └→ fulfillment_task (考核时间同步 assessment_time; task_id 被 dispatch_node 引用)
                ├→ fulfillment_task_ext (task_id)
                └→ batch_delivery_info (project_code)

【基础设施】
Kafka → event_sub(去重落地) → Handler;  event_sub → event_sub_2025_qN(季度归档)
indicator_order → (Hive ods 同步) → rpt.rpt_indicator_eva_da(离线报表)
```

### 4.3 新旧版本演进(读代码前必看)

1. **旧版(2.0)**:`indicator_config` / `indicator_base_config` / `indicator_cus_config`,配置内容整包 JSON 存 `content` 字段,用 `order_mode` 区分模式。
2. **2.5 重构**(`doc/refactor2.5.sql`):引入 `n_` 前缀维度表 + `indicator_compute_rule` 统一规则表,维度标准化(business_type/main_type/second_type/business_version)。
3. `indicator_order` 相应加了 `*_eval_result_new` 字段存新版结果,`dic_rule_node.code` 从 int 改 varchar。
4. **接口上 `/v2/` 前缀 = 新版**,无前缀多为旧版;`IndicatorConfigWriteController` 已 `@Deprecated`。改配置相关需求先确认走的是新版还是旧版链路。

---

## 五、核心业务链路(调用链)

### 链路 1:OFC 节点变更 → 考核计算(最核心)

场景:供应商完成接单,OFC 发 Kafka 消息,触发"接单及时"指标计算。

```
Kafka topic: ofc-fulfillment-node
 → ScmOfcListener (@KafkaListener)
 → EventDrivenSubscriberV2.persistAndHandleMessage()   [消息落地 event_sub 去重]
 → OfcFulfillmentEventHandler.handleBiz()              [@EventType(bizType="ofc_fulfillment_node")]
    ├─ OmsRpc.queryBaseByOrderNo()                     [取子单信息]
    ├─ EdarStarlordRpc.queryProjectInScheduleSwitch()  [判断是否排程项目]
    ├─ IndicatorOrderService.buildOrderBo()            [构建考核订单BO]
    ├─ IndicatorDAGService.transEventNodeByOrder()     [在DAG中定位起始节点]
    └─ EvalOrderService.calculateIndicatorByEvents()   [非排程;排程走 EvalScheduleOrderService]
        ├─ getFigureNodeList()  [DFS遍历DAG找待计算节点]
        ├─ buildTimeDataContext()  [批量拉5个数据源的轨迹:施工工期/OFC轨迹/OMS子单/主材任务/返补单]
        └─ doEval() → processOrder() → processRules()
            ├─ calculateTimelines() + TimeProcessor.processTime()  [按规则算时间轴,考虑工作日/自然日]
            ├─ selectFinalTimeline()  [最早/最晚策略]
            ├─ updateOrder() → indicatorOrderMapper.updateByPrimaryKeySelective()  [写 eval_result_new]
            ├─ updateFulfillmentTask()  [同步 fulfillment_task.assessment_time]
            └─ publishEvent(IndicatorChangedEvent) → IndicatorChangedListener  [指标变更后续处理]
```

### 链路 2:OFC 调度节点变更 → 履约任务生成

```
Kafka topic: ofc-dispatch-node-change
 → ScmOfcListener.ofcDispatchNodeChanged() → event_sub 落地
 → OfcDispatchNodeChangeHandler.handleBiz()
    ├─ NODE_START    → buildParam()[OfcRpc查调度单] → FulfillmentTaskWriteService.createTask()
    │                    → fulfillmentTaskMapper.insertSelective()
    │                    → SyncVssTaskService / SyncOFWTaskService / SyncMaterialTaskService [同步外部]
    ├─ NODE_COMPLETE → updateTask() [服务完成时同时完成对应通知类任务]
    └─ NODE_CANCEL   → updateTask() [状态置 CANCELED]
```

### 链路 3:一条消息同时触发任务 + 考核(最常见)

`OfcFulfillmentEventHandler.handleBiz()` 内部分三支:
- **任务类节点** → `doExecuteModifyTask()`:完成旧任务、创建/激活新任务(如测量报价 6101 → 完成报价任务、激活报价验收任务)
- **货单类节点** → `doExecuteGenerateTask()`:通过 OFC 流程配置取任务类型列表,循环创建接单/备货/进仓任务
- **核心考核** → `doExecute()`(异步线程池 indicatorCalcExecutor):走链路 1 的考核计算

### 履约任务的策略模式

`FulfillmentTaskWriteService` 按 task_type 分发到 `AbstractTaskService` 子类:
`PlaceOrderTaskBizService`(下单)、`NotifyDeliveryTaskBizService`(通知送货)、`BatchDeliveryTaskBizService`(批次配送)、`DesignAndDesignReviewTaskBizService`(设计/报价变更)、`TwoZeroPlaceOrderTaskBizService`(2.0下单)、`RescaleTaskBizService`(复尺)。**新增任务类型 = 新增一个子类。**

### DAG 缓存机制

`IndicatorDAGService` 从 `indicator_event`(节点)+ `indicator_compute_rule`(边)组装 DAG,缓存在 JVM ConcurrentHashMap,用 Redisson `RAtomicLong` 版本号控制刷新;改了规则配置后 DAG 缓存不生效 → 调后门 `/dag/refresh` 或 `/api/remove/schedule/dag/cache`。

---

## 六、job 模块与定时任务

`scm-workbench-job` 是**独立 Spark 项目**(不在父 POM modules 内,无 Spring @Scheduled / xxl-job):

| 类 | 职责 |
|---|---|
| `JavaJobDemo` | Spark 批处理:从 Hive 查在途销售单(排除集采),按 `categoryId_branchCode` 分组,HTTP POST 推给考核服务批量重算;spark-submit 提交 |
| `Demo`(stream/) | Spark Streaming 消费 Kafka 写 ES,**样例性质非生产** |

在线服务里的调度用链家 `schedule-spring-boot-starter`(lianjia-schedule);离线报表靠 `doc/hive/rpt_indicator_eva_da.sql` 的 Hive 日任务。

---

## 七、外部依赖速查

**Feign RPC(`service/rpc/` 包,20+ 个)**:
OmsRpc(OMS订单)、OfcRpc(OFC履约中控)、EdarStarlordRpc(主材任务starlord)、ProjectOrderRpc(项目订单)、ConstructionScheduleRpcService(施工排程)、MerchantRpc/SupplierRpc(商家/供应商)、ArtisanWorkOrderRpc(工单/返补单)、CubeRpc(施工包)、NrsRpc、VSSRpc、CustomerHomeRpc(客户)、CipherFeign(KMS加解密)、IndicatorOrderEsSearchRpc(ES搜索)等。

**Kafka 消费**:
- `ofc-fulfillment-node`(核心,履约节点变更)、`ofc-dispatch-node-change`(调度节点变更)、`oms-sub-order-event`(子单事件)
- 独立 Listener:ConstructionScheduleListener(排程进度)、MaterialTaskChangeListener(主材任务)、ProjectStatusChangeListener、ScmMerchantEventListener、CustomerHomeListener、NrsSalesProjectListener、AthenaDomainEntityEventListener、ShuttleProjectListener、ScmFullServiceFulfillmentListener

**Kafka 生产**:`FulfillNodeChangePayload` 广播销售单节点变更。

**存储中间件**:MySQL(主)、Redis/Redisson(DAG版本、假日缓存、@TryLock 分布式锁)、ES(履约进度查询)、Doris/StarRocks(`DorisDriver` HTTP 查询,报表)、Hive(离线)。

---

## 八、怎么找接口?(代码导航手册)

### 8.1 已知 URL 找代码

1. **所有路径都在方法级注解上**(无类级前缀),直接全局搜路径片段:
   ```bash
   grep -rn "indicator-cus-config/page-query" scm-workbench-start/ --include="*.java"
   ```
2. URL 前缀判断归属:
   - `/api/workbench/**` → 业务接口
   - `/v2/api/workbench/**` → 2.5 新版接口
   - `/api/scm-workbench/back-door/**`、`/api/refresh-data/**` → 运维后门
   - `/api/test/**`、`/api/tool/**` → 开发工具
3. Controller 位置规律:`scm-workbench-start/.../controller/read/`(查询)、`controller/write/`(写入)、`controller/write/back/`(后门)、`controller/read/es/`(ES查询)、`controller/read/FrontEndContoller/`(端上,注意拼写就是 Contoller)。

### 8.2 已知功能找接口

1. 按业务域到 Controller 类名猜:指标配置→`Indicator*Controller`,考核结果→`OrderEvalController`/`IndicatorResultQueryController`,履约任务→`FulfillmentTask*Controller`/`TodoTask*Controller`,排程→`WorkbenchForScheduleController`,字典→`Dic*Controller`。
2. **`doc/http/` 下 4 个 .http 文件**(action/indicator/indicator2/init)有真实调用示例,含参数,IDEA 直接可执行——找指标配置类接口用法首选这里。
3. 上游系统怎么调的:去 `scm-workbench-api` 模块看 Feign 接口定义(20 个 Api 接口 + DTO),Controller 都 implements 对应 Api。

### 8.3 从 Controller 往下追链路

命名规律(以 Indicator 为例):

| 层 | 命名 | 位置 |
|---|---|---|
| Controller | `XxxController` | start 模块 `controller/` |
| Service | `XxxService` / `XxxServiceImpl`(读写分离:`service/read/`、`service/write/`) | service 模块 |
| DAO | `XxxDao` / `XxxDaoImpl` | dao 模块 `dao/` |
| Mapper | `XxxMapper`(生成)+ `XxxExtMapper`(手写扩展) | dao 模块 `mapper/` |
| DO 实体 | `Xxx` + `XxxExample`(MyBatis Generator 生成) | dao 模块 `domain/` |
| SQL XML | `resources/mapper/`(生成)、`resources/manualMappers/`(手写) | dao 模块 |
| 入参/出参 | `XxxParam`(入参)、`XxxDto`、`XxxVo` | base 模块 |
| 枚举/常量 | `XxxEnum` | base 模块 `base/constant/`(**全部在这**) |
| 外部调用 | `XxxRpc` / `XxxFeignService` | service 模块 `service/rpc/`、`service/feign/` |
| MQ | `XxxListener`(消费)、`service/mq/listener/handler/XxxHandler`(事件处理) | service 模块 |

### 8.4 已知表找代码

```bash
grep -rln "indicator_compute_rule" scm-workbench-dao/src/main/resources/   # 找 Mapper XML
grep -rln "IndicatorComputeRule" scm-workbench-dao/src/main/java/          # 找 DO/Mapper/Dao
```
表名 → DO 类名是驼峰直译(`indicator_order` → `IndicatorOrder.java`)。

### 8.5 排查/修数据入口

- **考核结果不对** → `/api/refresh/eval/order`(按货单重算旧考核)、`/api/refresh/eval/schedule/projectOrder`(重算新排程考核)、`/api/refresh/eval/order/latestVersion`(用最新规则重算);查现状用 `/api/tool/projectAssessment`
- **任务不对** → `FulfillmentTaskBackDoorController` 的 `/api/refresh-data/**` 系列(重新生成下单任务、刷新签收、修执行人、手动完成任务等)
- **配置改了不生效** → 刷 DAG 缓存 `/dag/refresh`、`/api/remove/schedule/dag/cache`;节假日缓存 `/api/holidayCache/delete`
- **ES 数据缺** → `/api/workbench/es/indicator/rePush/byIndicatorId` 重推
- **消息是否消费** → 查 `event_sub` 表(按季度分表 `event_sub_2025_qN`),event_status 字段

### 8.6 配置与开关

Apollo(app.id=`scm-workbench`)管理动态配置:考核开关、黑名单、外部 URL 等,代码里搜 `@ApolloJsonValue` / `ApolloConfig`(`service/config/` 包)。多环境配置在 `scm-workbench-start/src/main/resources/application-{test,prod,preview}.properties`。

---

## 九、快速上手建议(阅读顺序)

1. 先读本文件第一、四节,建立"配置 → 计算 → 结果 → 任务"的心智模型
2. 打开 `doc/http/indicator2.http`,照着调一遍 v2 配置接口,理解底线 vs 一城一策
3. 读 `OfcFulfillmentEventHandler.handleBiz()` —— 全系统的消息入口总闸,三个分支看懂就看懂了一半
4. 读 `EvalOrderService.calculateIndicatorByEvents()` + `IndicatorDAGService` —— 考核计算核心
5. 读 `FulfillmentTaskWriteService.createTask()` + `AbstractTaskService` 子类 —— 任务侧核心
6. 遇到不认识的节点/类型,去 `base/constant/` 翻枚举

## 相关笔记

- `📚 后端项目索引.md` — 项目别名映射
- `📚 常用接口手册.md` — 常用内部接口
- `📚 排程材料配置四表关联链路.md` — 排程侧配置链路
- `材料自流程支持纯零售-代码与库表学习指南.md` — starlord 侧学习指南
