# 通知复尺日报-数据查询接口-技术方案

> 飞书文档：<链接占位，同步后回填>

## 1、背景[必选]

- 消费方：「通知复尺新增日报」脚本 `notify_recheck_ledger.py`，每天 8:30 推送两个企微群；现直连 KE Cloud MySQL DQL 执行 5 条 SQL（S1 明细 / S2 接收总数 / S3 四类判定计数 查 `starlord.material_site_recognize_result`，S4 项目→项目经理 查 `shuttle_order.project_order`，S5 项目经理→工号 查 `ruban.personal_info`），跨库结果在脚本内存拼接
- 问题：脚本强耦合库表与 KE 登录态（cookie 自愈约 120 行）；跨库关联无法复用；S2/S3 与 S1 的口径一致性（均不过滤 `notify_execute_type`）靠人记
- 本期：edar-starlord 新增 **3 个只读后门拉数接口**替代 5 条 SQL，调用方零数据库知识、零 cookie 维护
- 范围：仅 edar-starlord 本工程；脚本侧台账 diff 逻辑不变，仅切换取数通道（4.5）

## 2、需求理解[必选]

| # | 需求 | 口径 |
|---|---|---|
| 1 | 明细接口（替代 S1） | 接收时间下界必填（日报固定 `2026-09-18 00:00:00`）、上界可空（空=至当前）；`notifyExecuteType` 可选过滤（日报传 1=一键通知，空=不过滤）；分页；每行附带项目经理展示名与项目城市（可关，均来自项目订单 RPC） |
| 2 | 汇总接口（替代 S2+S3） | 合一保证同口径：`totalRecv` 与判定分类计数**均不过滤 `notify_execute_type`**（分母=全量接收，与明细"可通知项"形成对照）；服务端保证分类计数之和+NULL 数 == totalRecv，供调用方自检 |
| 3 | 项目经理映射接口（替代 S4） | 直接用项目订单上的项目经理（`display=foremanName`）；**S5 工号查询砍掉（已确认）**：不再查 ruban，展示名不带工号；批量入参上限 1000；查不到的进 `missing` |
| 4 | 5 条 SQL 收敛为 3 个接口 | 明细独立：明细持续增长（当前全量接收 3678 条、日报口径 49 条），汇总只要 count，合并会迫使轻量场景拉全量 |
| 5 | 形态 | POST + JSON 后门拉数，挂既有 BackDoorController（工程惯例：Controller 直连 Dao/Manager 无 Service 层，先例 `/backdoor/resetNodeAndRecognizeByResultTableId`，BackDoorController.java:154-159）；调用方为内网脚本，不新增鉴权 |

## 3、现状

### 3.1 识别结果表既有访问链路

```
BackDoorController(:99-100)
        │  已注入 MaterialNotifyTaskAutomationDao(:151)
        │  先例：/backdoor/resetNodeAndRecognizeByResultTableId(:154)
        │        Controller 直连 Dao.selectByEntity(:159)，无 Service 层
        ▼
MaterialNotifyTaskAutomationDao(:10-39)  ──▶  MaterialNotifyTaskAutomationMapper.xml
  ⚠️ 未暴露 countByExample/listByExample        selectByExample(:83, 支持 orderBy+limit)
                                                countByExample(:219)
ext/MaterialNotifyTaskAutomationExtMapper.xml —— 空壳（仅 namespace，可挂手写聚合 SQL）
```

- Model `MaterialNotifyTaskAutomation.java` 字段（本方案用到的）：`id:22`(Long)、`projectOrderId:27`(Long)、`materialCode:32`、`judgeResult:42`(Byte)、`receiveTime:57`(Date)、`notifyExecuteType:72`(Byte)、`aiAdoptionType:77`(Byte)
- 既有查询均走 `(project_order_id, material_code, task_type)` 精确维度（`queryLatestNotifyUnDoneByDimension`，Dao :24，DaoImpl :55 按 500 维度分批 :26/:61-66），**无按 receive_time 范围拉明细/聚合的先例**，本方案新增
- 时间字段序列化惯例：@JsonFormat `yyyy-MM-dd HH:mm:ss` GMT+8（MeasureApply.java:44）

### 3.2 项目经理既有链路（RPC，非跨库 SQL）

> ⚠️ 原设计稿的 S4/S5 跨库 SQL（直查 `shuttle_order.project_order`、`ruban.personal_info`）不可行：本工程不直连这两个库。S4 复用既有 Manager RPC；S5（工号）已确认砍掉，展示名直接用项目上的项目经理姓名。

```
ProjectOrderManagerImpl.getProjectOrders(Set<Long>)(:277)
  ──▶ this.getProjectDetails(:284, 实现 :293)
      Lists.partition 100/批(:295) ──▶ ProjectOrderService.getProjectDetails(:298)
      （shuttle-order-api Feign，注入 :63；异常/失败抛 UtopiaBussinessException :300-306）
  ──▶ ProjectOrderConvert.convert(:287→:23-27)
  ──▶ ProjectOrderDetailBO.foremanId(String, :43) / foremanName(:45) ──▶ display=foremanName
```

- 单查带三层缓存的 `getProjectOrder(Long, Map)`(:254，Redis TTL 120s :269) 可作兜底，批量场景直接用 `getProjectOrders`

### 3.3 码值枚举（以工程枚举为准）

> ⚠️ 原设计稿与工程枚举不一致，以代码为准：`notify_execute_type` 原稿漏 **3=手动通知**（日报过滤传 1 不受影响；明细不过滤时会出该码值，desc 转换需覆盖）。`judge_result`/`ai_adoption_type` 与原稿一致。

| judge_result（NotifyJudgeResultEnum.java） | ai_adoption_type（AiAdoptionTypeEnum.java） | notify_execute_type（NotifyExecuteTypeEnum.java） |
|---|---|---|
| 1 可通知(:13) | 0 未处理(:11) | 0 未执行(:15) |
| 2 不可通知(:15) | 1 采纳(:12) | 1 一键通知(:17) |
| 3 无法判断(:17) | 2 未采纳(:13) | 2 自动通知(:19) |
| 4 现场已完成，任务需关闭(:19) | 3 图片采纳(:14) | 3 手动通知(:21) |
| | 4 上门时间采纳(:15) | |

出参 `*_desc` 一律取枚举 desc（如 ai_adoption_type=1 出"采纳"，不再用原稿"图片和上门时间均采纳"）。

## 4、详细设计

### 4.1 公共约定与新增类落点

- 包络：`ResultDTO<T>`（com.ke.utopia.common.dto，BackDoorController.java:47），`new ResultDTO<T>().success(data)` / `.fail(msg)`
- 时间格式：入参/出参统一 `yyyy-MM-dd HH:mm:ss` GMT+8（@JsonFormat，仿 MeasureApply.java:44）；原稿的 `since=2026-09-18` 日期格式口径化为 `2026-09-18 00:00:00`
- 原稿 `notify_execute_type=-1 表示不过滤` 口径化为 **null=不过滤**（与工程可选筛选惯例一致）

| 新增                        | 模块                                                                   | 说明                                                                                                                                                                                                                                                       |
| ------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| RecheckLedgerQueryParam   | dao `com.ke.utopia.model`                                            | 三端点共用入参：`startTime`(必填)/`endTime`(可空)/`sinceId`(可空，增量下界排他，仅 tasks 生效)/`notifyExecuteType`(可空)/`withManager`(可空，默认 true)/`pageNum`/`pageSize`/`orderIds`(List\<String\>，foremen 用)                                                                                                        |
| MaterialRecognizeResultVO | dao `com.ke.utopia.model`                                            | 明细出参：`id`/`projectOrderId` Long→String（雪花号超 JS Number.MAX_SAFE_INTEGER，防精度截断）；`receiveTime` @JsonFormat；`receiveDate`=DATE(receiveTime) 由 Controller 截取；`judgeResultDesc`/`aiAdoptionDesc`/`notifyExecuteDesc` 按 3.3 枚举转换；`projectManager` 展示名、`city` 项目城市（4.4 逻辑填充，均来自项目订单 RPC） |
| RecheckSummaryDTO         | dao `com.ke.utopia.model`                                            | 汇总出参（平铺，别名即属性名 resultType 直映）：`totalRecv`/`judgeCnt1`~`judgeCnt4`/`nullCount`/`sumCheck`                                                                                                                                                                 |
| Ext XML `statByExample` 语句  | dao `resources/mapper/ext/MaterialNotifyTaskAutomationExtMapper.xml` | 既有空壳挂手写聚合 SQL（4.3）；Mapper 接口追加 `statByExample(MaterialNotifyTaskAutomationExample)` 方法签名——命名与传参同 `selectByExample`/`countByExample` 惯例：WHERE 条件由 Example 通用传入，聚合列固定（GROUP BY 聚合超出 Example 表达力故手写，条件片段复用 `Example_Where_Clause`）                                                                                                                                               |
| Dao 追加方法                  | dao `MaterialNotifyTaskAutomationDao`(:10-39) + Impl                 | 透传 `statByExample`；暴露 `countByExample`/`listByExample`（BaseMapper 既有语句 :219/:83）                                                                                                                                                                           |
| Controller 3 端点           | web BackDoorController                                               | `/backdoor/recheck/**`（4.2-4.4），复用已注入的 materialNotifyTaskAutomationDao(:151) 与 projectOrderManager(:109)，**零新增注入**                                                                                                                                       |

### 4.2 明细接口 `/backdoor/recheck/tasks`

Controller 内拼 Example（无自定义 SQL）：`receiveTime >= startTime`（必填，缺失 fail）+ `endTime` 非空则 `<=` + `notifyExecuteType` 非空则等值 + `sinceId` 非空则 `id > sinceId` + `orderByClause("receive_time asc, id asc")` + `limitStart/limitSize`；`countByExample` 出 total（total=0 短路不查 rows）。出参 `{total, rows}`。

**增量拉取（sinceId）**：台账本是全量快照，每日真正新数据只有新写入部分。脚本在台账存 `lastMaxId`（上次成功拉取的最大 id），次日传 `sinceId=lastMaxId` 只拉增量。按自增 `id`（MaterialNotifyTaskAutomation.java:22）而非 `receive_time` 增量：补录/迟到数据 id 也一定更大，不会漏；receive_time 是业务时间可能乱序。不传 `sinceId`=全量，首次运行/台账损坏/数字对不上时回退。当前全量 3678 条成本本就低，此参数为明细涨至数万后准备。

分页：`pageNum` 默认 1；`pageSize` 默认 2000、上限 5000（调用方为脚本一次性拉全量）。

`withManager=true`（默认）：rows 收集 `projectOrderId` 去重 → 走 4.4 同一链路批量取 `display`/`city` → 回填 `projectManager`/`city`；`false` 则跳过（省 RPC，两字段为 null）。

rows 示例（单条）：

```json
{
  "receiveTime": "2026-10-20 14:32:11",
  "receiveDate": "2026-10-20",
  "projectOrderId": "1059247301893",
  "materialCode": "1020304050607080",
  "id": "8842513667123",
  "judgeResult": 1, "judgeResultDesc": "可通知",
  "aiAdoptionType": 1, "aiAdoptionDesc": "采纳",
  "notifyExecuteType": 1, "notifyExecuteDesc": "一键通知",
  "projectManager": "高武宽",
  "city": "北京市朝阳区xx小区"
}
```

### 4.3 汇总接口 `/backdoor/recheck/summary`

ext XML 语句 `statByExample`（入参 Example，Controller 只拼时间条件，**不拼 notifyExecuteType 段**——刻意口径，见 2 需求表 #2）：一条 SQL 单行返回，单次往返：

```
totalRecv  = COUNT(*)
judgeCntN  = IFNULL(SUM(judge_result = N), 0)      -- N ∈ 1..4（3.3 枚举全集）
nullCount  = IFNULL(SUM(judge_result IS NULL), 0)
```

`sumCheck` = judgeCnt1..4 之和（Controller 计算后出参）。**契约：`sumCheck + nullCount == totalRecv` 恒成立**（同一 SQL 行内必然），调用方断言不等即告警，防上游脏数据静默污染日报。

出参示例：

```json
{
  "totalRecv": 3678,
  "judgeCnt1": 1254, "judgeCnt2": 1119, "judgeCnt3": 789, "judgeCnt4": 516,
  "nullCount": 0,
  "sumCheck": 3678
}
```

### 4.4 项目经理映射接口 `/backdoor/recheck/foremen`

入参 `orderIds`（必填，上限 1000，超限 fail）。链路（3.2）：

1. `ProjectOrderManagerImpl.getProjectOrders(Set<Long>)`(:277) 批量取 `ProjectOrderDetailBO`（内部 getProjectDetails :293 按 100 一批 :295；RPC 失败抛 UtopiaBussinessException :300-306，Controller 捕获转 fail）
2. `display` = `foremanName`(:45)，姓名缺失 `"未知"`（与现有脚本行为一致）；不查 ruban 工号（S5 已砍）
3. `city` = `ProjectOrderDetailBO#address`(:33)，源自订单 `houseDetail.address`（ProjectOrderConvert.java:33），订单查不到为 null
4. `foremanId`(:43) 为 `String.valueOf(Long)` 拷贝，null 会产生字符串 `"null"`，视同缺失置 null

出参：`map`（key=projectOrderId 字符串，value={foremanName, foremanId, display, city}）+ `missing`（入参有但 RPC 查不到的 orderIds）。

### 4.5 脚本迁移与回滚

日报取数流程（脚本侧）：

| 步骤 | 内容 | 变化 |
|---|---|---|
| ① | `POST /backdoor/recheck/tasks`（startTime=2026-09-18 00:00:00，notifyExecuteType=1，无上界；日常传 `sinceId=台账.lastMaxId` 只拉增量，首次/台账异常不传=全量） | 替代 S1 |
| ② | `POST /backdoor/recheck/summary`（同 startTime）→ totalRecv + judgeCnt1..4，断言 sumCheck+nullCount==totalRecv | 替代 S2+S3 |
| ③④ | 读本地台账 → 「昨日新增」= ①集合 ∖ 台账（唯一键 projectOrderId+materialCode） | **不变**：必须集合差兜底（防重跑/边界重复），不因增量拉取而省 |
| ⑤ | 汇总文案推送企微 | 不变；明细已带 projectManager，**不再调 ③'** |
| ③' | `POST /backdoor/recheck/foremen` | 替代 S4+S5；仅当①以 withManager=false 拉取时由脚本单独调用 |
| ⑥ | 推送成功（errcode=0）才落盘新台账（台账=旧台账∪增量，并记录 `lastMaxId`=本次最大 id），按 ledger_date 每天只记首次成功 | 台账新增 lastMaxId 字段 |

脚本删除项：cookie 加载、`q()` 重试、`_selfheal_cookies()`、`_raw_query()`（约 120 行，KE 登录态维护职责收敛到服务端既有 RPC 通道，不再需要）。

回滚开关：脚本保留 `RECHECK_DATA_SOURCE=api|sql` 环境变量，`sql`（默认，现状直连 DQL）/`api`（新接口），任一异常即切回，无需发版。分阶段：P0 服务端 3 端点上线 → P1 脚本切 api 双跑核对数字 → P2 其它报表（广角、视频）复用 foremen 映射 → P3 删脚本直连 SQL 与 cookie 自愈代码。

## 5、接口改动

| # | 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|---|
| 1 | POST `/backdoor/recheck/tasks` | 后门拉数：复尺任务明细。body=RecheckLedgerQueryParam：`startTime` 必填（`yyyy-MM-dd HH:mm:ss`，缺失 fail）、`endTime/sinceId(增量下界排他)/notifyExecuteType/withManager(默认true)/pageNum(默认1)/pageSize(默认2000上限5000)` 可选；出参 `ResultDTO<{total, rows}>`，rows=MaterialRecognizeResultVO（Long id 转 String、日期 `yyyy-MM-dd HH:mm:ss`、`*_desc` 枚举文案、`projectManager` 展示名、`city` 项目城市） | BackDoorController 新增端点（仿 :154 直连 Dao 惯例，复用注入 :151）→ Dao 暴露 countByExample/listByExample → 纯 Example + VO 转换（4.2），无新增 SQL | |
| 2 | POST `/backdoor/recheck/summary` | 后门拉数：接收汇总。body 同 #1（仅 startTime/endTime 生效，**无 notifyExecuteType 过滤**）；出参 `ResultDTO<RecheckSummaryDTO>`：`totalRecv/judgeCnt1..judgeCnt4/nullCount/sumCheck`，契约 sumCheck+nullCount==totalRecv | BackDoorController 新增端点 → Dao.statByExample → ext XML `statByExample` 条件聚合单 SQL（Example 传时间条件，4.3） | |
| 3 | POST `/backdoor/recheck/foremen` | 后门拉数：项目经理映射。body：`orderIds` 必填（数组，上限1000，超限 fail）；出参 `ResultDTO<{map, missing}>`，map value=`{foremanName, foremanId, display, city}`，display=foremanName（缺失"未知"）、city=BO#address，见 4.4 | BackDoorController 新增端点（复用已注入 projectOrderManager :109，零新增注入）→ getProjectOrders(:277) + display 拼接（4.4），无新增 SQL | |

既有接口零改动；消息/定时任务零改动。tasks 的 withManager 复用 #3 链路（Controller 内私有方法，两处共用）。

## 6、上游依赖

均为工程既有依赖，无新增 jar、不卡上线：

| # | 依赖方 | 接口 / 位置 | 依赖字段/能力 |
|---|---|---|---|
| 1 | shuttle-order-api | `ProjectOrderService.getProjectDetails`（ProjectOrderManagerImpl.java:63 注入，:298 调用） | foremanId / foremanName / address（城市），批量（100/批 :295） |

## 7、表结构改动

无（三接口纯只读，无新表/新列/新索引；聚合 SQL 挂既有空壳 ext XML，无 DDL）。

## 8、风险评估

- tasks/summary 均带 `receive_time` 下界收敛扫描区间，但**该表 receive_time 索引情况未核实**（工程与 wiki 库表文档均无此表 DDL）。表当前千级（全量接收 3678 条）、日频一次拉数，即便全表扫亦可接受。
  > ⚠️ TODO（待与 DBA/线上确认）：`material_site_recognize_result` 是否有 `receive_time` 索引；若明细量长期增长至数万再评估补索引。
- getProjectOrders 对 RPC 失败直接抛异常（:300-306），无静默降级：foremen/tasks(withManager) 会在订单服务异常时整体 fail，调用方按重试处理；批量 100/批，上限 1000 时最多 10 次 RPC。
- summary 单 SQL 单次往返、tasks 为 count+list 两次查询，无循环查询放大。

## 9、Check List

- [ ] Apollo 配置：无新增/变更
- [ ] SQL 变更：无
- [ ] 上线顺序：发布本工程（3 端点，纯新增只读，可提前上线）→ 脚本 `RECHECK_DATA_SOURCE=api` 双跑核对（tasks 49 条 / totalRecv 3678 / sumCheck==totalRecv）→ 数字一致后切默认 → P3 删脚本直连代码
- [ ] 回滚：接口纯只读，回滚发布即下线；脚本侧切回 `RECHECK_DATA_SOURCE=sql` 即恢复现状，无需发版

## 10、评审会议纪要

### 参会人员

QA:  
开发:

### Todo List

> 记录评审改进意见。
