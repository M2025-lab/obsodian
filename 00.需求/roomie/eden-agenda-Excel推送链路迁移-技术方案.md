# eden-agenda-Excel推送链路迁移-技术方案

> 飞书文档：<链接占位>

## 1、背景[必选]

- 需求文档：[[00.需求/roomie/文档处理逻辑]]（前端全链路代码的阶段摘要）、[[00.需求/roomie/功能-报表群内通知]]（四类报表与去重规则、方案 1/2 分歧）、[[00.需求/roomie/云助理群场景提效·Roomie接入初版方案]] §3.2（迁移口径：业务逻辑照搬不重构）
- 总方案：[[10.工作/技术方案/云助理Roomie-群场景提效-技术方案]] §4.2（Excel 抓取落库 + 汇总推送）、§7.2（进展库 DDL 草案）
- 范围：把云助理工作平台「每日推送」链路（上传排线表 → 解析 → 落库 → 读取 → 展示数据 → 微调）从浏览器端 JS 迁为 **eden-agenda 内的 Java 服务**（对应需求文档 §3 方案 2：agenda 独立成套），本期**只做数据 API**，不含前端页面、图片生成、自动推群。

现状：云助理每天手动完成整条链路——上传排线表 Excel 到工作台小工具 → 浏览器内 JS 解析（板块裁剪/列识别/名单匹配）→ 落 `wecom_push_orders`（Supabase，对方平台库）→ 回读渲染四板块表格 → 微调隐藏行 → 截图发企微群（人工）。本次把「上传→解析→落库→查询→微调」五段迁入 agenda 服务侧，推群能力复用已落地的 `RoomiePushService`（[[00.需求/roomie/功能-报表群内通知]] §3.1），替代每日手工搬运。

> ⚠️ 原链路实现文件（`excel-upload-logic-deep-dive.md` / `push-full-chain.js` / `push-core-logic.js` 等 5 份）未在 vault 找到，本方案依据 [[00.需求/roomie/文档处理逻辑]] 的六阶段摘要 + 两个 kdocs 文档实测表头 + 推送截图（2026-11-12 赵公口+永外汇总）反推；解析算法细节（板块关键词表、ID 列三策略、展示列三段匹配）列 TODO，编码前需补投原始文件核对。

## 2、需求理解[必选]

| # | 需求点 | 口径（已与需求方确认） |
|-|-|-|
| 1 | 功能范围 | Excel 上传后如何解析、如何展示、如何落库，整条链路转为 agenda 内的功能 Service（对应需求文档 §3 方案 2：agenda 独立成套，download+分析+推送收敛在一个服务） |
| 2 | 数据模型 | **窄表 + 行级冗余结构化字段**：窄表（section + headers + cells JSON）保展示零加工，行上冗余 project_id 等查询列，换后续实时查询/聚合能力 |
| 3 | 排线表入口 | **留入口、实现再定**：本期定义 `DispatchSourceProvider` 接入契约（见 4.2），后续排线表技术方案（HTTP 上传 / roomie 群文件抓取）定稿后实现该接口即接入，解析与落库零改动；本期先落调试用 HTTP 上传实现 |
| 4 | 本期交付 | **只做数据 API**：解析落库、快照查询、行微调（隐藏/恢复）；前端 React 页面、服务端图片生成、自动推群另期（推送形态「图片 vs 文本」本身是需求文档待确认项） |
| 5 | 数据源 | 三个在线文档读 `KdocsDownloadService` 已落的本地 clean 文件，排线表为上传入口 |
| 6 | 迁移原则 | 业务逻辑照搬不重构（总方案 §4.2 已确认口径）：四板块拼接规则、「明日送货优先、在库未送去重」规则原样迁移 |

## 3、现状

### 3.1 原前端链路（迁移对象，六阶段）

```
【阶段 0】上传触发：隐藏 file input + handleUpload（开遮罩、委托解析、toast 反馈）
    ↓
【阶段 1】解析引擎（纯前端，浏览器内）：
    1a 板块关键词/展示列定义/正则常量表
    1b cleanId 清洗 / 日期归一三层 / Sheet 名读取
    1c Sheet 级：readSheetMatrix 四方向裁剪 + 板块子表贪心匹配
    1d 列识别：ID 列三策略仲裁 / 展示列三段匹配 / 投票制定位
    1e 名单查询：项目单号名单 + 工长回查
    ↓
【阶段 2】落库：窄表行 50 条/批 insert wecom_push_orders（Supabase）
    ↓
【阶段 3】读取回流：refresh() → useSnapshot → fetchTodaySnapshot（查最新批次 → 判今天 → 等值拉整批）
    ↓
【阶段 4】展示：PushPage → PushGroup → SnapshotTable → SectionTable × 4（cells 直接渲染零加工）
    ↓
【阶段 5】微调闭环：setRowHidden（软删除 update）→ refresh → .neq("hidden",true) 过滤
```

关键衔接口径（迁移必须保住的行为）：
- 落库的是**窄表**：cells 只投影展示列、headers 存展示表头——展示侧逐格渲染零加工
- **批次标识 created_at 贯穿三阶段**：整批同一时间戳 → 读取等值拉回整批 → 微调恢复面板按它查隐藏行
- 上传完不直接渲染，**落库后从库读回**；微调（隐藏/恢复）是 update hidden + 重读

### 3.2 四板块数据来源与展示列（实测）

四板块汇总（2026-11-12 推送截图「赵公口+永外汇总」验证）：

| 板块 | 数据来源 | 说明 |
|-|-|-|
| 明日送货 | **排线表**（上传入口，唯一的人工输入） | 次日全北京派车送货 |
| 在库未送 | 文档① `衣柜在库&出库信息*` sheet（名称带日期后缀，前缀匹配） | 参与去重：出现在明日送货的项目不重复提醒 |
| 正单预计入库 | 文档① `衣柜接龙正单` sheet | 预计到货时间（在施订单）列 |
| 反补预计到工地 | 文档② `反补到货时间（新）` sheet | 8.28 后新流程数据 |

实测 sheet 表头（`~/Downloads/衣柜到货进度.xlsx`、`橱木设计师返补下单台账.xlsx`，2026-11-12 版本）：

| Sheet | 表头行 | 关键列 |
|-|-|-|
| `衣柜在库&出库信息11.10工作日更新` | 第 1 行 | 品牌、项目单号、订单类型、姓名、地址、预计件数、实收件数、到货日期、送货日期、套餐、订单大部、管家姓名、工长姓名、工程经理、木作设计师、客户名称 |
| `衣柜接龙正单` | 第 1 行（61 列） | 到库日、预计到货时间（在施订单）、订单大部、交付专家、工程经理、履约子订单号、项目单号、交易父/子订单号、订单状态、渠道来源、订单主/子类型、配送方式、商户名称… |
| `反补到货时间（新）` | 第 1 行（23 列） | 销售单号、项目单号、品类、创建时间、供应商、工厂补货单号、工厂交期、预计到工地时间、状态、工单提交原因… |

### 3.3 agenda 侧既有地基

| 能力 | 现状 | 位置 |
|-|-|-|
| kdocs 文档下载 | `KdocsDownloadService.downloadAll()` 定时导出文档①②到 `/pvc/tmp/*.clean.xlsx`，贝壳调度平台任务 `syncKdocsExcel` 驱动 | KdocsDownloadServiceImpl.java:111（downloadAll）、KdocsSyncSchedule.java:31 |
| roomie 推群 | `RoomiePushService.pushToGroups(groupIds, content)` 已落地，Apollo 全配置化 | RoomiePushService.java:29 |
| roomie 文件消息地基 | kafka 消费链路已解析 msgType=6 FILE 的 mediaUrl（`fileMsg.href`），仅落库未下载 | RoomieMessageInspector.java:170（extractMediaUrl case 6）、RoomieGroupMessage.java:41（mediaUrl 字段） |
| Excel 读写 | EasyExcel 3.3.2 已引入（service 模块），现仅用于写（S3 上传）；**读裸 sheet（表头行不定、列按名匹配）需用其无模型读法** | eden-agenda-service/pom.xml:28 |

## 4、详细设计

### 4.1 数据模型（窄表 + 行级冗余）

设计原则：**窄表结构照搬原 `wecom_push_orders`**（展示零加工 + 批次等值拉取 + hidden 软删除三个行为不动），**行上冗余结构化查询列**（从解析出的行数据里抽固定字段独立成列），让这张表同时服务「展示快照」和「实时查询」两种读法。

```
上传排线表 ──┐
             ├─→ 解析引擎（Java 重写）──→ 汇总四板块 ──→ 窄表行 + 冗余列
kdocs ①② ───┘      （同一批次 batchId）              insert（50 条/批照搬）
```

一张主表 `excel_push_snapshot_row`（对应原 wecom_push_orders，新增）：

| 字段 | 类型 | 说明 |
|-|-|-|
| id | BIGINT PK | 主键 |
| batch_id | VARCHAR(32) | **批次标识**（沿用原 created_at 语义：一次上传解析 = 一批，取解析时刻 `yyyyMMddHHmmss`），查询/微调都按它等值拉整批 |
| biz_date | DATE | 业务日期（推送对应的「明日」= 上传日 +1，批次维度） |
| section_code | VARCHAR(32) | 板块：`tomorrow_delivery` 明日送货 / `stock_unsent` 在库未送 / `main_order_inbound` 正单预计入库 / `rework_arrival` 反补预计到工地 |
| row_no | INT | 板块内行序（展示顺序） |
| headers_json | JSON | 展示列表头（板块维度，每行冗余存一份，对齐原窄表「headers 存展示表头」） |
| cells_json | JSON | 展示列单元格值（只投影展示列，逐格渲染零加工——照搬） |
| project_id | VARCHAR(64) | **冗余列**：ID 列三策略仲裁结果（清洗后项目单号），实时查询/聚合用 |
| district | VARCHAR(32) | **冗余列**：大区/订单大部（赵公口、永外等——推送截图按区分板块标题） |
| supplier | VARCHAR(64) | **冗余列**：品牌/供应商（金牌、欧派、志邦…） |
| plan_date | DATE | **冗余列**：业务日期型时间列（明日送货日/预计到工地日/预计入库日，按板块含义不同） |
| hidden | TINYINT | 软删除标记（微调隐藏行，默认 0；照搬 setRowHidden 语义） |
| gmt_create / gmt_modified | DATETIME | 常规 |

索引：`uk_batch_section_row (batch_id, section_code, row_no)`（整批幂等重放）、`idx_bizdate_section (biz_date, section_code)`、`idx_project (project_id)`（冗余列查询入口）。

一张批次表 `excel_push_batch`（新增，批次生命周期）：

| 字段 | 类型 | 说明 |
|-|-|-|
| id / batch_id | PK / VARCHAR(32) UK | 批次标识 |
| biz_date | DATE | 推送业务日期 |
| source_file_name | VARCHAR(256) | 排线表原始文件名 |
| upload_channel | VARCHAR(16) | 来源标记：`HTTP` / `ROOMIE_FILE`（本期调试实现为 HTTP，正式入口接入后按实际落值） |
| row_total / row_hidden | INT | 总行数 / 当前隐藏行数（微调不用 count 查询） |
| parse_status | TINYINT | 0 解析中 1 成功 2 失败 |
| parse_error | VARCHAR(512) | 失败原因（表头识别失败等） |
| gmt_create / gmt_modified | DATETIME | 常规 |

> 为什么不沿用总方案 7.2 的 `roomie_delivery_plan`/`roomie_stock_in` 两张结构化表：那是「实时查询侧」的模型（execl和项目信息接入.md 的 dispatch_plan/inbound_fact 需求线，全列结构化、服务 Agent 查询）；本方案是「每日推送快照侧」的模型（窄表保展示）。两线各一张表、不强行合并——快照行上的 project_id 等冗余列已给出两线 join 的锚点，后续查询侧可从快照行反解全量列（cells_json 留底）。

### 4.2 排线表接入预留（入口抽象）

排线表数据源本期只定**接入契约**，不定实现：解析引擎只认一个数据入口接口，后续排线表技术方案（HTTP 上传 / roomie 群文件抓取 / 其他）定了之后实现该接口即可接入，解析与落库零改动。

```
排线表数据源（后续定）                  三个 kdocs 板块（已就绪）
      │                                      │
      ▼                                      ▼
DispatchSourceProvider                  KdocsSourceProvider（本期实现）
（入口接口，本期仅定义；                   读本地 clean 文件
 调试用 HttpUploadDispatchProvider）     （路径取 kdocs.export-path* 配置，
      │                                   各环境位置不一致）
      │                                      │
      │                                      │
      └──────────────┬───────────────────────┘
                     ▼
          解析引擎（Java 重写，同一批次 batchId）
                     ▼
   汇总四板块（明日送货 project_id 集合 → 在库未送去重）
                     ▼
          窄表行 + 冗余列 insert（50 条/批照搬）
```

```java
/** 排线表数据源入口：后续排线表方案定了之后实现此接口注册为 Spring Bean 即接入。 */
public interface DispatchSourceProvider {
    /** 提供明日送货板块的原始行（矩阵 + 来源元信息）；无数据时返回空（板块降级为空，不阻断其他板块）。 */
    DispatchSheet fetch();
}
```

- 契约要点：**fetch 只负责拿数据**（下载/解析触发时机、重试都在实现方），板块组装、去重、落库全在引擎侧——入口可替换
- 本期落地**调试用 HTTP 上传实现**：`HttpUploadDispatchProvider` 对齐现状「云助理把表一上传」（未上传时返回空）；正式方案定稿后按同接口替换，`upload_channel` 字段标记来源
- 排线表无数据/接入未就绪时：`tomorrow_delivery` 板块空 + 在库未送不去重（整表提醒），批次正常落——**入口缺失不阻断推送链路**

### 4.3 解析引擎（Java 重写，结构对齐阶段 1）

模块内分层（service 层新增 `excelpush` 包）：

```
com.ke.utopia.agenda.service.excelpush
├── ExcelPushService            // 门面：parseAndStore(provider 数据) / 微调 / 查询
├── impl/ExcelPushServiceImpl
├── source/
│   ├── DispatchSourceProvider  // 排线表入口契约（4.2 预留，后续方案实现）
│   ├── KdocsSourceProvider     // 三个 kdocs 板块数据源（读本地 clean 文件）
│   └── HttpUploadDispatchProvider // 调试用上传实现（对齐现状人工上传）
├── parser/
│   ├── SectionKeywordConstants // 1a：板块关键词/展示列定义/正则常量（迁移 JS 常量表）
│   ├── CellCleaner             // 1b：cleanId 清洗 + 日期归一三层
│   ├── SheetMatrixReader       // 1c：EasyExcel 无模型读 sheet → 二维矩阵 + 四方向裁剪 + 板块子表贪心匹配
│   ├── ColumnResolver          // 1d：ID 列三策略仲裁 + 展示列三段匹配 + 投票定位行
│   ├── RosterMatcher           // 1e：名单查询（项目单号名单 + 工长回查）——语义以原始 JS 为准（见 TODO）
│   └── SectionAssembler        // 2：窄表行组装 + 冗余列抽取（消费名单做去重过滤）
└── bo/                         // SectionRow / ParseResult 等过程对象
```

- **读 Excel**：EasyExcel `read(file, new AnalysisEventListener<Map<Integer,String>>)` 无模型逐行读（表头行位置不定、列按名匹配，注解模型不适配）；kdocs 两个文档读 `KdocsDownloadService` 落的 clean 文件——**文件位置各环境不一致**（本地 test yml 是 Desktop、测试/线上容器是 `/pvc/tmp`），`KdocsSourceProvider` 一律从 `kdocs.export-path*` 配置取路径、不硬编码（下载方与读取方同源配置，天然对齐）
- **Sheet 定位**：文档① sheet 名带日期后缀，前缀匹配（`衣柜在库&出库信息` / `衣柜接龙正单`——与 skill 脚本 `_find_sheet` 同口径，kdocs_fanbu_search.py:124）
- **去重规则（照搬，已确认口径）**：**项目出现在「明日送货」→「在库未送」不再提醒**。实现：以明日送货板块清洗后的 project_id 集合为名单，在库未送板块按 project_id 命中即过滤（不落行）；排线表无数据时名单为空 = 不去重（整表提醒），与原工具行为一致
- **批次写入**：50 条/批 insert 照搬；先写 `excel_push_batch(parse_status=0)` → 解析成功后批量 insert 行 + 更新批次状态，失败落 parse_error（事务边界：批次行状态更新与窄表 insert 同事务）
- **幂等**：同一 batch_id 重放走「删旧行再插」（uk_batch_section_row 防半批残留）；防重复上传由上传入口的文件名+日期判断（TODO 评审定口径）

> ⚠️ TODO（编码前必核对）：解析算法细节（板块关键词表、ID 列三策略、展示列三段匹配、投票制定位、**名单查询的语义——项目单号名单从哪来（是否明日送货集合之外的源）、工长回查查什么表/接口**）以原始 JS 文件为准——需补投 `push-full-chain.js` 等 5 份文件；未补前按 3.2 实测表头先实现，逐板块单测对齐截图。

### 4.4 查询与微调 API（本期交付）

| 接口 | 行为（对齐原链路阶段 3/5） |
|-|-|
| `GET /api/v1/open/excel-push/snapshot/latest` | 查最新批次（对齐 fetchTodaySnapshot：查最新 batch → 判是否今天 → 返回整批四板块 + headers + cells，`hidden!=1` 过滤） |
| `GET /api/v1/open/excel-push/snapshot/{batchId}` | 指定批次快照（历史回看） |
| `POST /api/v1/open/excel-push/row/hide` | 行隐藏（对齐 setRowHidden：body batchId+sectionCode+rowNo 列表 → update hidden=1） |
| `POST /api/v1/open/excel-push/row/restore` | 行恢复（update hidden=0，对齐恢复面板按批次查隐藏行） |
| `GET /api/v1/open/excel-push/row/hidden?batchId=` | 查批次隐藏行（恢复面板数据源） |

推送（推群）不在本期，但预留：`RoomiePushService.pushToGroups` 已就绪，后续「快照→文案/图片→pushToGroups」是纯增量。

## 5、接口改动

全部为**新增**接口（对齐 RoomieAdminController 的 `/api/v1/...` 路径风格，RoomieAdminController.java:31）：

| 接口路径 | 接口文档 | 改动点 | 估时 |
|-|-|-|-|
| `POST /api/v1/open/excel-push/upload` | 上传排线表并触发解析落库；body=multipart 文件（调试入口，对齐现状人工上传） | 新增 `ExcelPushController.upload`（web）→ `ExcelPushService.uploadAndParse` | |
| `GET /api/v1/open/excel-push/snapshot/latest` | 最新批次四板块快照（展示数据源） | 新增 `ExcelPushController.latestSnapshot` → 查批次表+行表组装 | |
| `GET /api/v1/open/excel-push/snapshot/{batchId}` | 指定批次快照 | 同上，batchId 入参 | |
| `POST /api/v1/open/excel-push/row/hide` | 隐藏行（微调）；body=batchId+rows | 新增 → update hidden=1 | |
| `POST /api/v1/open/excel-push/row/restore` | 恢复行；body=batchId+rows | 新增 → update hidden=0 | |
| `GET /api/v1/open/excel-push/row/hidden` | 批次隐藏行列表（恢复面板） | 新增 → select hidden=1 | |

> 后续排线表方案若选 roomie 群文件抓取：新增一个 `DispatchSourceProvider` 实现（kafka FILE 消息 handler，topic 见 RoomieKafkaConfig，下载文件后走同一 `uploadAndParse`），不新增 HTTP 接口，解析引擎零改动。

无既有接口改动：`KdocsDownloadService`、`RoomiePushService` 均为只读复用。

## 6、上游依赖

| # | 依赖 | 用途 | 卡上线 |
|-|-|-|-|
| 1 | EasyExcel 3.3.2（已引入，eden-agenda-service/pom.xml:28） | 无模型逐行读 sheet | 否 |
| 2 | `KdocsDownloadService` 落盘的 clean 文件 | 板块二/三/四数据源 | 否（已运行）；**路径按环境走配置**（Apollo `kdocs.export-path*`：本地 test yml 为 Desktop、测试/线上容器为 `/pvc/tmp`，各环境不一致——`KdocsSourceProvider` 只从配置读、不硬编码）；cookies 失效会断供，监控已有 403 日志 |
| 3 | 排线表 Excel（人工输入） | 板块一数据源 + 上传触发 | 是（业务前置，非系统依赖） |
| 4 | 排线表方案若为 roomie 群文件抓取：企微文件下载（mediaUrl 为 roomie 侧 URL，下载方式待核对 Roomie 文件接口） | 群文件入口 | 待排线表方案定稿 + 群迁移前置 |

## 7、表结构改动

两张新表（agenda 库，对齐 roomie_group 系列建表口径：gmt_create/gmt_modified、无 creator 列）：

```sql
-- 每日推送批次表
CREATE TABLE excel_push_batch (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_id VARCHAR(32) NOT NULL COMMENT '批次标识（解析时刻 yyyyMMddHHmmss）',
  biz_date DATE NOT NULL COMMENT '推送业务日期（明日送货日=上传日+1）',
  source_file_name VARCHAR(256) DEFAULT NULL COMMENT '排线表原始文件名',
  upload_channel VARCHAR(16) NOT NULL DEFAULT 'HTTP' COMMENT '入口：HTTP上传 / ROOMIE_FILE群文件',
  row_total INT NOT NULL DEFAULT 0 COMMENT '窄表总行数',
  row_hidden INT NOT NULL DEFAULT 0 COMMENT '当前隐藏行数',
  parse_status TINYINT NOT NULL DEFAULT 0 COMMENT '0解析中 1成功 2失败',
  parse_error VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch (batch_id),
  KEY idx_bizdate (biz_date)
) COMMENT='Excel每日推送批次';

-- 推送快照窄表（原 wecom_push_orders 的 agenda 版）
CREATE TABLE excel_push_snapshot_row (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_id VARCHAR(32) NOT NULL COMMENT '批次标识',
  biz_date DATE NOT NULL COMMENT '推送业务日期',
  section_code VARCHAR(32) NOT NULL COMMENT '板块：tomorrow_delivery/stock_unsent/main_order_inbound/rework_arrival',
  row_no INT NOT NULL COMMENT '板块内行序',
  headers_json JSON COMMENT '展示列表头（板块维度）',
  cells_json JSON COMMENT '展示列单元格值（只投影展示列）',
  project_id VARCHAR(64) DEFAULT NULL COMMENT '冗余：清洗后项目单号（ID列仲裁结果）',
  district VARCHAR(32) DEFAULT NULL COMMENT '冗余：大区/订单大部',
  supplier VARCHAR(64) DEFAULT NULL COMMENT '冗余：品牌/供应商',
  plan_date DATE DEFAULT NULL COMMENT '冗余：业务日期列（送货日/到工地日/入库日）',
  hidden TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：1=隐藏（微调）',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_section_row (batch_id, section_code, row_no),
  KEY idx_bizdate_section (biz_date, section_code),
  KEY idx_project (project_id)
) COMMENT='Excel每日推送快照窄表：cells直接渲染，冗余列供查询';
```

## 8、风险评估

| # | 风险点 | 评估 |
|-|-|-|
| 1 | **kdocs 文档体积**：文档② clean 文件 136MB，EasyExcel 全量无模型读入再裁剪，单次解析内存峰值 | 流式逐行读（AnalysisEventListener 不整 sheet 载入），矩阵按需缓存已裁剪区段；解析线程与 web 线程隔离（上传接口异步触发，sync 回批次状态） |
| 2 | **上传并发/重复触发**：同一文件重复上传生成多批次，快照 latest 取错 | batch_id 时间戳精确到秒 + 上传接口串行化（同 biz_date 已有成功批次时拒绝/确认覆盖，口径 TODO 评审定） |
| 3 | **表头漂移**：kdocs 文档 sheet 名带日期后缀、列增减常态 | sheet 前缀匹配已有；列识别按名匹配 + 未命中列告警日志（对齐总方案 4.2 表头容错） |

## 9、Check List

- [ ] Apollo 配置：**有**——`excelpush.enabled`（总开关）、kdocs 文件路径复用既有 `kdocs.export-path*`、`excelpush.upload.max-size`（上传大小限制）、板块关键词表初版硬编码后移 Apollo（`@ApolloJsonValue`，对齐总方案 4.2 可配口径）
- [ ] SQL 变更：**有**——agenda 库新增 2 张表（excel_push_batch / excel_push_snapshot_row），DDL 见第 7 节
- [ ] 上线顺序：建表 → 服务发布（开关默认关）→ 手动上传接口联调（调试入口）→ 对照推送截图校验四板块 → 开 `excelpush.enabled` → （排线表方案定稿后）实现 DispatchSourceProvider 替换调试入口
- [ ] 回滚：`excelpush.enabled` 关闭即回到无此功能状态；两表数据只增不改既有业务表，无回滚脚本需求
- [ ] TODO 汇总：① 排线表正式方案定稿 → 实现 `DispatchSourceProvider` 接入（4.2 已预留入口，本期调试用 HTTP 上传实现先行）；② 5 份原始 JS 文件补投核对解析算法；③ 重复上传口径；④ 展示列清单按截图逐板块定稿（板块→展示列映射表）

## 10、评审会议纪要

### 参会人员

QA:
开发:

### Todo List

> 记录评审改进意见。
