# 云助理Roomie-群场景提效-技术方案

> 🔗 飞书：https://beike.feishu.cn/docx/FOJJdi1I1okZNUx9cIscPra4nMd

## 1、背景[必选]

- 需求文档：[[00.需求/roomie/架构整理后]]、[[00.需求/roomie/云助理群场景提效·Roomie接入初版方案]]（§3 已按**功能点维度**重写：要实现什么功能 → 具体实现方式）、[[00.需求/roomie/数据架构和流向]]（2026-11-05 会议「云助理群场景提效方案讨论」结论整理）
- Roomie 接口依据：**Roomie 侧提供的开放 API 接口文档（接入用）** https://beike.feishu.cn/wiki/JFf1wFteCi4gGik4io0crh15nRm ——覆盖机器人、会话、消息服务、聊天记录、聊天消息事件、群、加微各域接口（含测试/生产域名 `huiju-chat-session/workbench/manage` 与 IAM/kepass 鉴权接入方式），各接口详细文档见该页内 weapons.ke.com 链接；后续接口变更关注 weapons 项目 23425
- 范围：**云助理 × Roomie 群场景提效**。三路数据（群消息 / 群内 Excel / 现有系统）落库为数据基础，其上以 LLM 清洗分析，向云助理提供 6 项能力。

全北京推广云助理后测算需 84 人（会议记录），通过提升自动化/AI 占比降低人力投入。云助理当前作业：项目经理在企微外部群提问 → 云助理人工查鲁班/台账/接口 → 回复或转供应商群、超时外呼。本次把其中可自动化的环节（台账可答的直答、问题归类、转发、数据汇总）系统化。

**分工总原则（已确认）**：
- 涉及鲁班操作的，接入现有鲁班能力，**不重新开发**；
- 数据层（消息拉取、Excel 落表、清洗打标）在新服务中实现；
- 操作类能力一期**不做自动化**，只给出入口供云助理使用；
- 转发能力给出**自动化 + 半自动**两套方案（默认半自动）。

**云助理定位（功能补全主线，已确认）**：云助理 = 交付过程**全场景的信息服务台 + 问题闭环的推动者 + 辅助判断决策 + 群内任务的执行者**（送货、改约等可在群内完成的操作）。系统围绕这个定位给它减负提效：**吃掉信息搬运**（查询直答、每日汇总推送、转发自动化）、**辅助它判断**（打标建议、工地就绪度、送货时间参考）、**供它执行**（操作入口深链、外呼承接）；关系维护与最终拍板留人——减负不减责。后续功能按此定位补全。

## 2、需求理解[必选]

| # | 需求点 | 口径（已与需求方确认） |
|-|-|-|
| 1 | 覆盖范围 | 6 项能力全覆盖，每个功能点给出：实现方式、数据来源、数据表、依赖系统、具体实现步骤 |
| 2 | 鲁班操作 | 接入现有鲁班能力（送装改约、取消等），不重新开发 |
| 3 | 数据层 | 消息拉取、Excel 落表、LLM 清洗打标 → 新建服务实现 |
| 4 | 操作类能力 | 一期不做自动化，只提供入口（跳转/预填）供云助理人工执行 |
| 5 | 转发@ | 自动化与半自动两套方案都要给出，**默认半自动**（系统产出转发文案+归属，云助理一键发） |
| 6 | 消息处理主线 | 拿到消息 → 结合上下文识别 → 写问题归类标签（半自动：标签供云助理确认/修正） |
| 7 | 外呼 | 一期不自动化：云助理人工触发，系统承接通话并留存记录 |
| 8 | 摄像头/视频接入（新增） | 项目详情查询中接入工地摄像头/视频：辅助云助理判断工地进展、决定是否可发任务通知（催办/送货通知的就绪判断） |
| 9 | 供应商信息入项目详情（新增） | 供应商信息聚合进项目详情，辅助云助理判断送货时间 |
| 10 | 定位主线（已确认） | 云助理 = 全场景信息服务台 + 问题闭环推动者 + 辅助判断决策 + 群内任务执行者（见 §1），按定位补功能 |
| 11 | Excel 每日汇总（已确认） | 现有工作台小工具已实现：汇总拼接**业务逻辑照搬迁移**至新服务 + Roomie 承载，不重新构思规则；改造仅触发与承载（人工上传→自动抓取） |
| 12 | 超时提醒（新增，双向） | 供应商超时未回提醒 + **云助理漏答提醒**（哪个群的什么消息长时间没回复，防消息多忘记回） |
| 13 | 方案维度（已确认） | 需求与方案均以**功能点**为维度（查询项目进展、消息转发、问题归类汇总…），不再按工作场景维度组织；实现优先封装现有能力（主材订单考核/送货/供应链接口） |
| 14 | 问题汇总展示（已确认） | 归类汇总后**以人为维度**将所有问题集中在一个页面（侧边栏）对云助理展示 + 待办提醒 |
| 15 | 清洗归类规则（已确认，三层） | 层1 问题归类（是否问题/问题类型/归属）→ 层2 **解决方式归类**（归类的落点不是问题类型，而是解决方式：查询项目信息/问题转接/业务操作/外呼）→ 层3 **具体行为放最后**（查询哪个订单什么信息，或转哪个供应商群，作为执行参数在层2 后填充） |
| 16 | 拉取偏移量（已确认） | 拉取上下文必须**记录本次拉取位置（偏移量/游标），且以群为维度关联**——哪个群拉到哪个位置，逐群独立游标、独立推进；只拉增量，不做每次全量拉取；同一内容不允许被反复拉取、反复识别问题（唯一键 + classify_status 单向流转双保险） |
| 17 | 问题汇总演进（新增，已确认） | 3.1 问题归类汇总按三期演进：**一期事后汇总分析**（拉取上下文+解决方式做汇总，拿数据与归类数量，AI 不介入处理）→ **二期 AI 辅助处理**（结合 AI 辅助处理，即三层清洗归类落地）→ **三期配置化自动回复**（特定问题可配置，AI 自动回复和处理） |
| 18 | 外呼接入形态（新增，已确认） | 外呼能力接入形态两种：对外提供接入 Roomie 的 Agent，或由 Roomie 原住民 Agent 直接查询调用 |

## 3、现状

### 3.1 云助理的定位与工作全景（已确认口径）

> 云助理 = 交付过程**全场景的信息服务台 + 问题闭环的推动者 + 辅助判断决策 + 群内任务的执行者**。本次所有能力围绕这个定位设计：系统吃掉信息搬运、辅助判断、供执行入口；关系维护与最终拍板留人。

### 3.2 工作场景与耗时点（群作业链路，全部人工）

```
项目经理（企微外部群：赵公口/永外/鲁谷，每区一群）
   │ 群内提问：定制柜进度、主材进度、送装改约、问题整改…
   ▼
云助理（人工）
   ├─ 台账可答 ──▶ 人工查鲁班/接口/SKILL 台账 → 群内回复
   ├─ 系统无数据 ─▶ 转发二三十个供应商微信群 @ 对接人 → 等 SOP 超时
   │                 └─ 超时 → 人工打电话（微信电话/台账紧急联系人）
   │                 └─ 仍无果 → 找北京供应链人工介入
   └─ 数据汇总 ──▶ 盯排线表群/实际入库群/衣柜更新群消息 → 人工传 Excel
                    → MOMA 小工具拼接另 3 个在线文档 → 人工发「明日送货」汇总到项目群
```

**耗时点（痛点 → 提效方向，全场景共同点：多源查询 + 消息盯守 + 手工搬运）**：

| # | 耗时点 | 场景 | 提效方向 |
|-|-|-|-|
| 1 | 多系统跳转查询 | 每次答疑要查鲁班/台账/接口多个系统（定制柜进度还可能卡工人或 PM，多方查） | 查询直答（能力1：Agent+Skill 聚合） |
| 2 | 手工转发与盯守 | 每个供应商问题：判断归属 → 手写转发文案 → 转群 @ 人 → 记着谁没回 | 归属路由+文案预生成+一键转发（能力3）+ 超时提醒 |
| 3 | 忘了回复（丢项漏项） | 消息多，有的提问没回就沉了（会议原话「都丢了几个，没回答人家」） | 漏答提醒（3.4 新增：哪群哪条多久没回 → 提醒云助理） |
| 4 | 每日固定数据搬运 | 每天固定时段盯 3 个数据群、传 Excel、拼 3 个在线文档、发汇总 | 汇总自动化（4.2 落库+定时推送） |
| 5 | 心智负担 | 「记着哪个供应商超时了该打电话了」全靠人脑记 | 超时监控 + 侧边栏提醒 |
| 6 | 操作链路长 | 改约/取消要打开鲁班跳多个页面 | 操作入口深链预填（能力4） |

### 3.3 数据回收群现状

| 群 | 内容 | 频次 | 现状 |
|-|-|-|-|
| 排线表群 | 次日全北京派车送货 Excel（主材/辅材/定制柜） | 每日 18–19 点 | 人工上传小工具 → 拼 3 个在线文档（返补预计到工地/在库库存/预计入库）→ 人工推汇总 |
| 实际入库群 | 金牌/伊莱利/博洛尼/志邦/索菲亚/兔宝宝当天实际入库 | 每晚 | 云助理盯群取数 |
| 衣柜更新群 | 内部群（工程经理/交付专家/仓管） | 每日 | 微信遗留群，数据未回收 |

> Excel 汇总：现有 MOMA 工作台小工具**已实现**（表上传 + 3 个在线文档拼接 + 输出汇总）。本次不做新构思，**业务逻辑照搬迁移**：汇总拼接规则原样迁移到新服务，改造仅触发与承载（人工上传 → 机器人自动抓取落库；小工具页面 → Roomie 承载推送/侧边栏查询）。见 4.2。

### 3.4 超时提醒（双向，本次新增明确）

| 方向 | 触发 | 动作 |
|-|-|-|-|
| 供应商超时未回 | 问题已转发，SLA 到点供应商群无回复 | 侧边栏提醒云助理 → 人工决定外呼（4.6） |
| **云助理漏答提醒** | PM 作业群里被识别为问题的消息，超过 N 小时无云助理回复且问题未闭环 | 侧边栏/工作台提醒：哪个群、哪条消息、多久没回——防消息多忘记回（对应现状耗时点 #3 丢项漏项） |

漏答判定依据：问题库（4.4 清洗产物）`status=PENDING` 且 `ctime` 超阈值——数据已在问题表，只需超时监控定时任务加一条扫描规则，无新增表。

### 3.5 已定结论（会议 2026-11-05）

| 事项 | 结论 |
|-|-|
| 群接入分工 | 围绕云助理作业的群走 Roomie；纯数据回收群走企微群 + 自建机器人 |
| Excel 落库 | 「需要一个服务承载」→ 新建服务 |
| 外呼 | 接 Roomie 外呼能力，**不自动化**，人工触发、系统承接通话（含通话记录） |
| 工作台嵌入 | MOMA 工作台嵌 Roomie 侧边栏——「到时候看」，非核心，列二期待定 |
| 消息数据 | 需存表：既要提效也要评估云助理服务能力（丢项漏项、解决率、时效） |

## 4、详细设计

### 4.0 总体架构与系统分工

```
┌──────────────────────────────────────────────────────────────────────┐
│ 交互层（Roomie 平台，复用已有能力）                                      │
│   群内 Agent 问答 · 侧边栏实时查询 · 外呼 · 消息上下文接口 · 群发消息     │
└───────────────▲──────────────────────────────▲───────────────────────┘
                │ HTTP（能力接口/上下文拉取）      │ HTTP（转发/外呼/推送）
┌───────────────┴──────────────────────────────┴───────────────────────┐
│ 服务层（新建：roomie-assistant-service，Java/Spring Boot）             │
│  ① 消息拉取定时任务 → ② Excel 抓取定时任务 → ③ LLM 清洗打标定时任务     │
│  ④ 问题流转（转发@/超时提醒/升级）⑤ 汇总推送 ⑥ 能力接口（供 Agent 调用）│
│  ⑦ 工地快照拉取+视觉判断定时任务（4.3）                                │
└───────────────▲──────────────────────────────▲───────────────────────┘
                │                                │
┌───────────────┴──────────────┐   ┌────────────┴─────────────────────┐
│ 数据层（MySQL，本服务新建）    │   │ 现有系统（复用，不开发）           │
│ 消息库/问题库/进展库/工地快照  │   │ 鲁班（节点进度/送装预约/取消）      │
│                              │   │ 工地监控（摄像头/视频进展判断）     │
│                              │   │ 台账（供应商联系人）·内部接口(项目)  │
└──────────────────────────────┘   └──────────────────────────────────┘
```

系统分工原则（已确认）：**鲁班操作接入现有能力不开发；数据层与清洗在新服务实现；交互复用 Roomie 平台能力。**

新服务 `roomie-assistant-service`（名字待定）技术栈：Java 8+ / Spring Boot / Apollo 配置中心 / MySQL / 分布式定时任务（XXL-Job 或 ElasticJob，选型见 TODO）。调 Roomie 三服务（session/workbench/manage）统一走 IAM 加签（kepass 申请，见第 6 节 #7），域名走 Apollo 配置区分测试/生产。

### 4.1 数据来源①：群聊消息获取（事件推送为主 + 定时拉取兜底）

**怎么拿消息**：Roomie 开放服务提供两条通道（接口文档见第 1 节）：
- **推送**：「企微群聊聊天消息事件」——PM 作业群消息实时推给新服务（webhook/消息订阅，具体接入形式见 weapons 文档 1994890）；
- **拉取**：「查询群聊聊天记录」`/api/common/chat/group/pageQueryChatRecord`——分页查询历史消息（文档 1995213）。

设计为**推送为主、拉取兜底**，拉取通道严格按**偏移量增量拉取**（已确认口径：记录本次拉取的位置/偏移量，只拉新增部分，不做全量重复拉取）：

```
Roomie 企微群聊消息事件（实时推送）
  │ 新服务消息接收接口（事件回调）
  │ 原始消息逐条 insert 消息表（幂等键：群ID+msgId，重复跳过）
  ▼
roomie_chat_message（全量原始消息，未清洗）

定时兜底任务（每 30 分钟，可配）—— **按群逐个拉取，每个群独立游标**
  │ ① 取订阅群列表（Apollo 配置），逐群处理：
  │    读该群游标 roomie_msg_pull_cursor（group_id → last_msg_id /
  │    last_msg_time = 该群上次拉取到的位置，即偏移量）
  │ ② 调 /api/common/chat/group/pageQueryChatRecord（必传该群 group_id），
  │    从**该群自己的**偏移量之后增量拉取
  │    （按 msgId/时间排序，分页翻页，只拉新增）
  │ ③ 逐条 insert（uk_group_msg 唯一索引 = 群ID+msgId，重复 insert ignore）
  │ ④ 该群全部页拉取成功后，才把**该群**游标推进到最后一条的位置
  │    （失败不推进，下次从原位重拉；A 群失败不影响 B 群游标）
  ▼
roomie_chat_message（补齐推送丢失的消息）
```

**偏移量（游标）管理规则（已确认）**：

| 规则 | 说明 |
|-|-|
| **按群隔离（关联维度）** | 偏移量**以群为维度**记录：哪个群拉到哪个位置，逐群独立游标、独立推进——A 群的进度与 B 群互不影响。游标表 `uk_group (group_id)` 唯一键保证每群一行；拉取请求必传 group_id，拉回的消息也带 group_id 落库 |
| 记录位置 | 每群一行游标：`last_msg_id`（消息唯一 ID，首选偏移量）+ `last_msg_time`（辅助，接口若不支持按 msgId 过滤则按时间窗） |
| 只拉增量 | 每次拉取从游标位置之后开始，**禁止每次全量拉取** |
| 游标推进时机 | 一轮分页拉取**全部成功**才推进该群游标；中途失败游标不动，下轮从原位续拉（可能重拉一小段，靠唯一键幂等去重） |
| 单群失败隔离 | 某群拉取失败只跳过该群（游标不动，下轮重试），不影响同轮其他群的处理 |
| 游标回退 | 仅在推送中断超过 N 小时（可配）时人工/自动回退重拉，正常流程不回退 |
| 防重复识别 | 清洗任务只扫 `classify_status=0` 的消息，同一条消息**不会被二次清洗/重复识别**——即使被推送+拉取双通道重复落库，唯一键保证只有一条记录，且已清洗的消息不再进入 LLM |

要点：
- **全量落库、清洗后置**：原始消息先无差别入库，清洗由 4.4 独立任务做，两阶段解耦（推送/拉取失败不影响已落库数据重清洗）。
- **幂等**：`uk_group_msg` 唯一索引（group_id + msg_id），推送与兜底拉取重叠靠唯一键去重，两条通道天然互备。
- 【假设】事件推送接入形式（MQ/webhook 回调）以 weapons 文档 1994890 为准，评审前确认回调协议；若事件推送短期内接不了，可先只用拉取通道跑通（拉取间隔调密，如每 5 分钟）。
- 私聊消息同理（IM/企微私聊消息事件 + 私聊聊天记录查询），一期先覆盖群聊。

> ⚠️ TODO（待与 Roomie 平台方确认）：企微群聊消息事件的回调协议与接入登记方式（weapons 文档 1994890）——4.1 推送通道的接入契约；确认前先只走拉取通道（`/api/common/chat/group/pageQueryChatRecord`）。

### 4.2 数据来源②：群内 Excel 抓取落库 + 每日汇总推送（小工具逻辑迁移）

**怎么拿 Excel**：数据回收群（排线表/实际入库/衣柜更新群）按已定结论切企微群 + 自建机器人。机器人收文件消息 → 回调新服务 → 解析落库。

**迁移口径（已确认）**：现有 MOMA 工作台小工具已实现「表上传 + 3 个在线文档拼接 + 汇总输出」，本节**业务逻辑照搬**（汇总拼接规则、3 个在线文档取数方式原样迁移），只改触发与承载。不重新构思。

```
【落库链路】
企微群自建机器人（新申请）
  │ 收到文件类消息（.xlsx/.xls）
  │ ① 按群ID路由解析器：排线表群→排线解析器；入库群→入库解析器；衣柜群→衣柜解析器
  │ ② 下载文件（企微 Media API）
  │ ③ EasyExcel 逐行解析 → 校验（表头识别 + 必填列非空）
  │ ④ 按行 insert 进展表（幂等键：业务日期+业务主键列，insert-or-update）
  ▼
roomie_delivery_plan（排线）/ roomie_stock_in（实际入库）

【汇总推送链路（小工具逻辑迁移，每日定时）】
定时任务（每日 19:30，排线表发出后）
  │ ① 查当日落库的排线数据 + 入库数据
  │ ② 按小工具既有拼接规则汇总「明日送货」
  │    （3 个在线文档数据源照搬小工具现有取数方式——在线文档 API 拉取
  │      或人工上传，迁移时按现状定，不改规则）
  │ ③ 调 Roomie /api/msg/pushMsg 定时推送到各 PM 作业群
  ▼
PM 群每日自动收到「明日送货」汇总（云助理不再手工搬运）
+ 侧边栏实时查询同一份汇总数据（PM/云助理随时可查，不等推送）
```

要点：
- **每日定时兜底任务**：除机器人回调实时落库外，每日 23 点跑一次兜底任务，扫描当天群里未落库的文件消息补解析（防回调丢失）——对应需求「每日定时抓取任务」。
- **不改变发消息习惯**：仓管/供应链照常发 Excel，机器人静默收数（会议结论）。
- 解析器按群注册（Apollo 配置群ID → 解析器类型映射），新增群零代码。
- 表头容错：列名模糊匹配（同义词映射表），列结构变更告警通知开发。
- 【假设】排线表/入库表列结构以真实样例为准；本方案 DDL（7.2）按需求文档口径设计，**评审时需拿真实 Excel 样例校正列**。
- 【假设】汇总推送时间初定 19:30（排线表 18–19 点发出后），具体以排线表实际发出时间为准调整。

> ⚠️ TODO（评审前确认）：① 数据回收群走企微自建机器人是会议口径，但需与 Roomie 平台方比对——若群消息事件推送通道（4.1）能直接覆盖文件类消息，可复用 Roomie 事件而不另建企微机器人（少一套接入）；② 拿排线表/入库表真实 Excel 样例逐列校正 7.2 DDL。

### 4.3 数据来源③：现有系统项目信息 + 工地摄像头/视频

不自建项目库，**查询时实时调现有系统**（会议结论：拉取对应项目信息作为数据来源的一部分）。工地现场数据（摄像头快照/视频）作为新数据源接入，服务「判断工地进展」场景。

| 数据 | 来源 | 用途 |
|-|-|-|
| 项目/订单基础信息（项目ID→大区、地址、PM） | 内部接口（具体接口待调研） | 问题归属识别（问题→项目→大区→群） |
| 节点进度（定制柜/主材任务节点） | 鲁班 | Agent 查询回答 |
| 送装预约信息（预约时间/可改约状态） | 鲁班 | 查询回答 + 操作入口预填 |
| 供应商信息（品牌→供应商→对接人/紧急联系人/群） | 台账 | 转发@路由、外呼号码、**送货时间判断（供应商产能/排期视角）** |
| **工地摄像头快照/视频（新增）** | 工地监控系统（摄像头平台） | 工地进展判断 + 任务通知就绪判断（4.5/4.9） |

**工地快照获取与视觉判断链路（新增）**：

```
定时任务（每日 1–2 次，可配；或任务通知触发时按需拉取）
  │ ① 场景：次日有排线送货/安装计划的项目（从 roomie_delivery_plan 取）
  │ ② 按项目ID调工地监控平台接口 → 拉取摄像头最新快照（或近 N 小时视频关键帧）
  │ ③ 快照送视觉模型（LLM 多模态 / 视觉服务，选型随 4.4 LLM 一并确认）
  │ ④ 产出结构化判断落工地快照表：
  │    { site_stage: 工地当前阶段推断, ready_hint: 可送货/可安装建议,
  │      blockers: 观察到的障碍（堆料/无人在场/地面未完成…） }
  ▼
roomie_site_snapshot（工地快照与视觉判断）
```

要点：
- **判断辅助而非指令**：视觉判断结果供云助理参考（侧边栏展示 + 催办建议），不直接自动发任务通知——与一期「操作不做自动化」口径一致。
- 快照原片留存（OSS 地址），判断可回溯；视觉判断置信度低时仅存快照不出建议。
- 拉取频次控制：仅对「次日有送货/安装计划」的项目拉，不做全量摄像头轮询（成本控制）。

缓存策略：项目→供应商映射数据量小且稳定，服务启动 + 每日刷新拉一份全量缓存本地（供应商映射表，见 7.1），避免每条消息清洗都远程调用。

> ⚠️ TODO（评审前调研）：① 工地监控/摄像头平台是否已有对外接口（按项目ID查摄像头列表、取快照/视频流），覆盖了哪些大区多少工地；② 视觉模型选型（多模态 LLM vs 专用视觉服务）随 4.4 LLM 网关一并确认；③ 无摄像头覆盖的工地降级策略（纯靠鲁班节点进度判断）。

### 4.4 消息清洗与问题打标（LLM）

**演进路径（初步方案，已确认）——本节按二期落地形态设计**：

| 期 | 形态 | 落地内容 |
|-|-|-|
| 一期 · 事后汇总分析 | **AI 不介入处理链路** | 拉取上下文消息 + 云助理的实际解决方式，事后做汇总分析——拿到各类型问题的**数据与归类数量**（问题量、解决方式分布、时效），为二期 AI 辅助提供数据底座 |
| 二期 · AI 辅助处理 | 即本节设计 | 结合 AI 对问题辅助处理：三层清洗归类打标 → 半自动确认池 → 流转 |
| 三期 · 配置化自动回复 | 特定问题配置化 | 支持对某些特定问题进行配置，这类问题由 AI **自动回复和处理**（如固定口径的进度类问题自动直答） |

> 一期不接入 LLM 处理链路意味着：消息拉取落库（4.1）、问题人工归集（以人为维度汇总页）、双向待办提醒**先行上线**；4.4 的 LLM 打标链路随二期接入。一期「问题记录 + 解决方式」可先由云助理在汇总页人工标记（半自动确认池的 confirm_status 链路一期即具备，只是标签来源从 LLM 换成人工），表结构与流转链路三期通用——避免二期上 LLM 时改表。

**拿到消息后怎么做**：定时任务逐条扫描未清洗消息，拼接上下文（同群前后 N 条 + 该问题涉及项目的基础信息）喂给 LLM，产出结构化标签落问题表。

```
定时任务（每 5 分钟，可配）
  │ ① 扫描 roomie_chat_message 中 classify_status=0（未清洗）的消息
  │    （清洗侧增量保证：classify_status 只从 0 → 1/9 单向流转，
  │      已清洗消息永不重扫——同一条消息不会被反复识别）
  │ ② 过滤噪音：纯表情/转发/系统消息/机器人自己的消息 → 直接标 status=9 跳过
  │ ③ 每条消息构造 LLM 输入：
  │    · 消息本体
  │    · 上下文：同群前后各 5 条消息（时间正序）
  │    · 项目线索：从消息中抽取的房号/项目名/业主姓名 → 查项目映射
  │ ④ LLM 输出（JSON，三层归类结构）：
  │    { is_question,
  │      question_type, project_id, supplier_id,          ← 层1 问题归类
  │      resolve_method,                                  ← 层2 解决方式
  │      action_param,                                    ← 层3 具体行为（执行参数）
  │      need_assistant, reason, confidence }
  │ ⑤ 问题类消息 insert 问题表 roomie_issue（status=待处理，uk_msg 唯一键防重）
  │    消息行随即标 classify_status=1（本轮已消费）
  ▼
roomie_issue（问题库：一条问题消息一条记录）
```

**三层清洗归类规则（已确认）**：识别上下文后对问题归类，归类的落点**不是问题类型，而是该问题的解决方式**；具体行为放最后填充——由解决方式决定执行参数。

| 层 | 字段 | 归什么 | 例子 |
|-|-|-|-|
| 层1 问题归类 | question_type / project_id / supplier_id | 描述性问题属于哪类（哪类问题、归属项目与供应商） | 主材进度确认 / 刘一彤家 / 霍尔茨 |
| **层2 解决方式** | **resolve_method** | **该问题的解决方式（不是问题类型）** | QUERY_INFO=查询项目信息 / TRANSFER=问题转接（转供应商群）/ OPERATION=业务操作 / CALL=外呼 / NONE=无需处理 |
| **层3 具体行为**（放最后） | **action_param** | 该问题解决的**具体行为**，作为执行参数在层2 决定后填充 | 查询订单 BJ12345 的送装预约信息 / 转霍尔茨群 @张三 |

层1 问题类型枚举（question_type，对应 5 类问题 + 兜底）：

| code | 含义 | 典型层2 解决方式 |
|-|-|-|
| MATERIAL_RECTIFY | 主材问题整改 | TRANSFER（转供应商要结论） |
| DELIVERY_ADJUST | 送装预约与调整 | OPERATION（改约入口）/ QUERY_INFO（查预约状态） |
| BIZ_VERIFY | 业务办理与信息核实 | QUERY_INFO |
| CUSTOM_CABINET | 定制柜进度咨询 | QUERY_INFO（鲁班可查直答） |
| MATERIAL_PROGRESS | 主材进度确认 | QUERY_INFO（台账可查部分）/ TRANSFER（系统无数据部分） |
| OTHER | 其他/闲聊 | NONE（不流转） |

> 注：层1 → 层2 是**多对多映射**而非固定（如 MATERIAL_PROGRESS 可走查询也可走转接，取决于系统里有没有这单数据）——这正是把解决方式独立成层2 的原因：同类型问题分流到不同处理链路，层3 只跟层2 走。

> ⚠️ TODO（待与产品确认）：OTHER 兜底类消息是否需要云助理每日兜底浏览（防 LLM 漏识别），还是完全信任 LLM。

> ⚠️ TODO（评审前确认）：LLM 服务选型——公司内 LLM 网关（模型、调用方式、QPS 配额、计费）未定，清洗 prompt 与置信度阈值无法定稿；本节链路为选型无关设计，网关确定后补充调用细节。

**半自动确认机制（默认模式）**：LLM 打标结果不直接驱动自动化动作，先进**待确认池**：

- 问题表记录带 `confirm_status` 字段（0=机器打标待确认 / 1=云助理已确认 / 2=云助理修正标签 / 9=机器高置信跳过）。
- 云助理在 Roomie 侧边栏 / 新服务提供的确认工作列表里看到待确认问题，一键确认或改标签——**确认的主要对象是层2 解决方式与层3 具体行为**（层1 问题类型描述性较强，修正频率低）。
- 确认后进入流转（层2=QUERY_INFO → 直答；TRANSFER → 转发@；OPERATION → 操作入口）。
- 高置信阈值（LLM 返回 confidence ≥ 0.9 且 is_question 明确）可直接进入流转，人工仅抽检。

**问题汇总展示（以人为维度，已确认口径）**：问题库的展示形态 = **按提问人归集其名下全部待处理问题**，集中在一个页面（Roomie 侧边栏）对云助理展示——同一个人的问题连续跟进、批量处理，不用在群消息流里翻找。页面分组结构：

```
Roomie 侧边栏 · 问题汇总页（以人为维度）
├─ 张三（PM，赵公口群）── 4 个待处理
│   ├─ [定制柜进度｜解决:查询] 刘一彤家合金门啥时候安装？
│   │    └ 行为：查订单 BJ12345 定制柜节点进度 → Agent 直答
│   ├─ [主材进度｜解决:转接] ……  → 行为：转霍尔茨群 @张三
│   │    （2h 供应商未回 → 超时提醒标记）
│   └─ ……
├─ 李四（PM，鲁谷群）── 2 个待处理
└─ 待办提醒区：超时未回 / 漏答清单（哪群哪条多久）
```

实现：问题表已有 `sender_id`（消息表带出），列表接口 `GET /api/assistant/issue/list` 按 `sender_id` 分组聚合返回（GROUP BY sender_id + 问题明细），无新增表、无新增定时任务。

### 4.5 能力一：查询项目详细信息、回答问题

**实现方式**：Roomie 自定义 Agent 挂载 Skill，Skill 调用新服务能力接口；新服务聚合现有系统数据返回。**平台编排是 Roomie 已有能力，本工程开发量 = 能力接口。**

**「项目详情」聚合视图（本能力的核心资产，云助理判断的信息底座）**——一次查询聚合六路数据，支撑「云助理能做什么」的判断：

```
项目经理群内提问（Roomie 群）
  │ Roomie Agent 识别咨询类问题
  │ Agent 调 Skill「项目进度查询」
  ▼
Skill → HTTP 调新服务 GET /api/assistant/project/progress
        （入参：project_id + 问题类型；鉴权：服务间 token）
  │ 服务内路由（项目详情聚合视图）：
  ├─ CUSTOM_CABINET → 调鲁班接口查节点进度 → 组装回答
  ├─ MATERIAL_PROGRESS(台账可查) → 调鲁班/内部接口 → 组装回答
  ├─ DELIVERY_ADJUST(查询部分) → 调鲁班送装预约接口 → 组装回答
  ├─ 进展类（明日送什么货/是否入库）→ 查本服务进展库(4.2落库数据) → 组装回答
  ├─ 工地现场（新增）→ 查工地快照表(4.3视觉判断) → 进展描述/可施工建议
  └─ 供应商视角（新增）→ 供应商映射(7.1) + 排线/入库数据
  │     → 该项目各供应商的送货/排期状态 → 送货时间判断参考
  ▼
Agent 群内直接回复 PM（云助理无需人工查台账）
```

- **工地摄像头/视频（新增）**：项目详情返回工地快照（最新一帧 + 视觉判断 site_stage/ready_hint/blockers）。PM 问「工地进展到哪了/能不能送货」时 Agent 可直答现场状态；云助理发任务通知前在侧边栏看同一份判断，**辅助决定是否可以通知**（如现场堆料未清 → 建议暂缓送货通知）。
- **供应商信息入项目详情（新增）**：项目详情带该项目全部在途供应商列表（对接人/电话/群 + 最近排线/入库记录 + 待答复问题），云助理判断送货时间时一站式看齐（供应商排期 + 工地就绪度 + 在库库存三个视角），不再分别翻台账/排线表/群记录。

数据怎么来：**不新建查询库**——定制柜/主材/送装查询实时调鲁班与内部接口；进展类查询查 4.2 落库的排线/入库表；工地现场查 4.3 快照表；供应商视角查 7.1 映射 + 4.2 进展库。项目基础信息走 4.3 缓存。

> ⚠️ TODO（评审前调研）：鲁班节点进度、送装预约查询接口清单（服务名/URL/入参出参/鉴权）与内部项目基础信息接口——4.5 查询路由与 4.3 缓存刷新的数据源，接口清单定稿后才能排接口适配开发量。

### 4.6 能力二：供应商电话外呼（人工触发）

**实现方式（一期不做自动化，已确认）**：云助理在 Roomie 作业端人工发起外呼，系统承接通话并留存记录。

**接入形态（两种，已确认）**：外呼能力对外有两种接入方式——**A. 对外提供接入 Roomie 的 Agent**（新服务把外呼能力封装成 Roomie 可挂载的 Agent/Skill，云助理在作业端调用）；**B. 由 Roomie 原住民 Agent 直接查询调用**（新服务只提供查询/发起的外呼能力接口，Roomie 侧已有 Agent 配置调用）。两形态共用同一套外呼能力接口与回调链路（下同），差异仅在调用入口是自建 Agent 还是原生 Agent 配置——与 4.5 查询能力的「平台编排是 Roomie 已有能力，本工程开发量 = 能力接口」口径一致。

```
云助理看到超时未回的问题（Roomie 侧边栏问题列表）
  │ 人工点击「外呼」按钮
  ▼
Roomie 外呼能力 → 拨打台账紧急联系人电话
  │ 通话结束
  ▼
通话记录回流：新服务接口接收 Roomie 回调 → 更新问题表
   roomie_issue.action=CALL，记录通话时长/录音/结论摘要
```

数据怎么来：外呼号码 = 台账供应商紧急联系人（4.3）；通话记录 = Roomie 外呼能力回调（会议确认能拿到通话记录）。

新服务开发量：一个回调接口（接收通话结果）+ 问题表动作记录。外呼发起、通话本身都是 Roomie 已有能力，不开发。

> ⚠️ TODO（待与 Roomie 平台方确认）：外呼接口未发布在 Roomie 开放服务接口文档中（文档仅覆盖机器人/会话/消息/聊天记录/消息事件/群/加微，见第 1 节）。需单独确认：外呼发起接口、通话记录回调的接入方式与鉴权、上线排期——本能力硬依赖（第 6 节 #5）；确认前 `POST /api/assistant/call/callback`（第 5 节）的回调契约无法定稿。

### 4.7 能力三：转发问题到供应商群并 @ 对接人（双方案）

**前置**：问题 → 供应商的路由。4.4 清洗打标已产出 `supplier_id`（从消息项目线索 → 项目→材料→供应商映射）。转发文案由 LLM 生成（剥离群内无关上下文，保留问题原文 + 项目信息 + 期望回复时间）。

**方案 A：自动化**（供应商群切企微后可启用）

```
问题确认（4.4 高置信 或 云助理确认）→ 触发自动流转
  │ 服务内组装转发文案
  │ 调 Roomie「发送消息（私聊、群聊）」/api/msg/pushMsg → 发到对应供应商企微群
  │ 文案 @ 对接人（对接人从台账映射取）
  │ 问题表记录 action=FORWARD、目标群、目标人、转发时间
  ▼
启动超时监控（超时监控定时任务，见第 5 节）：SLA 到点未回 → 侧边栏提醒云助理 → 外呼（4.6）/升级北京供应链
```

**方案 B：半自动（默认）**

```
问题确认后进入「待转发列表」（Roomie 侧边栏）
  │ 云助理看到：问题原文 + 归属供应商 + 目标群 + @谁 + 已生成的转发文案
  │ 人工点「一键转发」→ 新服务调 /api/msg/pushMsg 发出
  │ （可先改文案再发）
  ▼
问题表记录 action=FORWARD（确认人=云助理）→ 进入超时监控
```

两方案差异只有「谁按下发送键」；文案生成、归属路由、@人员、发送接口、超时监控全部系统化。供应商群目前是纯微信群（部分 500 人已进不去），**一期默认 B**，企微群迁移完成后切 A（Apollo 开关 `forward.auto.enable` 控制，代码两套都实现，开关切换无发版）。

发送通道补充：群建群/拉人可用开放服务「群」域接口（群创建/邀请成员，huiju-chat-manage 服务）辅助供应商企微群迁移，属一次性建群工具，不在主链路。

> ⚠️ TODO（评审前确认）：`/api/msg/pushMsg` 的 @ 人能力（@ 指定对接人）与供应商群消息事件订阅登记——weapons 文档 1879071 出参入参逐字段核对，@ 语法（mention 列表/文本占位符）确认后才能定稿转发文案模板。

### 4.8 能力四：进行操作（送装预约、复尺任务通知等）——只给入口

> 云助理定位中的「群内任务执行者」落点：一期系统不代执行，把执行路径缩到最短（入口+预填），执行动作留给云助理（减负不减责）。

**实现方式（一期不做自动化，已确认）**：Agent 识别到操作类问题（DELIVERY_ADJUST 等）后，不做任何写操作，回复中附带操作入口。

```
PM 群内提出改约/取消
  │ Agent/清洗任务识别为操作类问题
  │ Agent 回复：该单当前预约信息（查鲁班，只读）
  │             + 操作入口（鲁班页面深链，带单号预填）
  ▼
云助理点击入口 → 跳转鲁班现有页面完成改约/取消（复用现有能力，不开发）
  │ 操作完成
  ▼
云助理在群内/工作台标记问题已解决 → 问题表闭环
```

深链规范：`luban://order/change?orderNo=xxx` 或鲁班 Web 页面 URL + 参数预填（具体 URL 规则待调研鲁班前端，见 TODO）。复尺任务通知同理：识别到复尺类问题 → 通知入口（复尺通知若有现成页面/接口则复用，否则二期做）。

> ⚠️ TODO（待调研）：鲁班改约/取消页面的 URL 与入参规则；复尺任务通知现有承载方式。

### 4.9 能力五：供应商计划与排期查询 + 通知/回答

**实现方式**：数据来自 4.2 落库的排线表（供应商计划送货排期）+ 4.4 清洗出的供应商答复沉淀。

```
场景a：业主/PM 问「供应商什么时间送货」
  │ Agent Skill → 新服务 GET /api/assistant/supplier/plan
  │ 查 roomie_delivery_plan（排线表落库数据）
  │    命中 → 直接回答（某供应商 X 月 X 日送货到某项目）
  │    未命中 → 返回「排线未出」+ 转能力三问供应商
  ▼
场景b：通知供应商在特定时间内完成任务（催办）
  │ 云助理/规则触发（如安装前 1 天排线未确认）
  │ 就绪判断（新增）：项目详情聚合视图（4.5）给出
  │   · 工地快照 ready_hint（现场可施工/有障碍）
  │   · 供应商排期 + 在库库存状态
  │ → 云助理据此决定是否发通知（系统给建议，人拍板）
  │ 复用能力三链路：生成催办文案 → 转发供应商群 @（自动或半自动）
  ▼
场景c：供应商答复沉淀
  │ 4.4 清洗任务同样扫描供应商群消息（落库后打标）
  │ 供应商答复 → 关联到原问题（reply_to_msg_id 关联）
  │ 答复内容抽取（送货时间/整改结论）→ 更新问题表 + 沉淀进展表
```

数据怎么来：排期 = 排线表每日落库；供应商答复 = 供应商群消息清洗（同 4.4 链路，群范围扩大到供应商群后生效）。

> ⚠️ TODO（待与产品确认 + 评审前调研）：① 供应商群（20–30 个）接入 Roomie/企微的排期与消息事件订阅范围——场景 c 的数据源，一期供应商答复只能靠云助理人工回复沉淀；② 场景 b 催办触发规则的口径（安装前 N 天、哪些品类），待产品给规则。

### 4.10 能力六：便捷统一接口

**实现方式**：新服务对外暴露一套 REST 能力接口，统一鉴权、统一出参，供 Roomie Agent/Skill、侧边栏、未来其他消费方调用。

| 接口 | 用途 | 消费方 |
|-|-|-|
| GET /api/assistant/project/progress | 项目进度查询（能力1） | Agent Skill |
| GET /api/assistant/supplier/plan | 供应商排期查询（能力5） | Agent Skill |
| GET /api/assistant/issue/list | 待确认/待转发/超时问题列表（4.4/4.7） | 侧边栏 |
| POST /api/assistant/issue/confirm | 云助理确认/修正标签（4.4 半自动） | 侧边栏 |
| POST /api/assistant/issue/forward | 一键转发（4.7 方案B）/自动转发（方案A内部调用） | 侧边栏/内部 |
| POST /api/assistant/call/callback | 外呼结果回流（能力2） | Roomie 回调 |
| POST /api/assistant/excel/notify | 企微机器人 Excel 落库回调（4.2） | 企微回调 |

统一规范：服务间 token 鉴权；出参 `{code, message, data}`；列表类接口分页；全部接口打 EntryTrace 日志。

### 4.11 一期 / 二期 / 三期拆分

> 功能点优先级（需求文档 §3 已确认排序）：① 群消息问题归类汇总（4.4）→ ② 材料送货进展（4.2）→ ③ 消息转发 + 供应商外呼（4.7/4.6）→ ④ 项目信息聚合查询（4.5）→ ⑤ 业务操作（4.8）。

| 功能点 | 一期 | 二期 | 三期 |
|-|-|-|-|
| 消息拉取落库（4.1） | ✅ | — | — |
| Excel 抓取落库 + **每日汇总自动推送**（4.2，小工具逻辑迁移） | ✅ | — | — |
| 问题汇总：拉取 + 以人为维度归集 + 双向待办提醒（4.4 展示层） | ✅ | — | — |
| 问题汇总：LLM 三层清洗归类 + 半自动确认池（4.4） | 人工标记（数据积累） | ✅ AI 辅助处理 | — |
| 查询回答 Agent（4.5，含供应商视角聚合） | ✅ | — | — |
| 工地摄像头快照 + 视觉判断（4.3/4.5） | ⚠️ 待定（依赖摄像头平台接口调研结论，见 4.3 TODO） | 覆盖率不足时逐步铺 | — |
| 外呼人工触发 + 记录回流（4.6，接入形态 A/B） | ✅ | — | 自动外呼（超时自动拨打） |
| 转发@（4.7） | ✅ 半自动（B），自动（A）代码就绪开关关 | 企微群迁移完成切 A | — |
| 操作入口（4.8） | ✅ 只给入口 | — | Agent 直接执行写操作 |
| **双向超时提醒**（4.7 供应商超时 + 3.4 漏答提醒） | ✅ | — | — |
| 排期查询/通知（4.9，含快照就绪判断辅助） | ✅ | 供应商答复自动沉淀（依赖供应商群接入） | — |
| 特定问题配置化自动回复（4.4 演进路径三期） | — | — | ✅ 特定问题配置后 AI 自动回复/处理 |
| MOMA 工作台嵌 Roomie | — | 待定（会议结论「到时候看」） | — |

## 5、接口改动

新服务全量新建，无存量接口改动。「接口改动」列全部为新服务新增入口：

| 接口路径 | 接口文档 | 改动点 | 估时 |
|-|-|-|-|
| POST /api/assistant/roomie/msg/event | Roomie 企微群聊消息事件回调（weapons 1994890，4.1 推送通道） | 新增：接收事件 → 幂等落消息表 |  |
| 定时任务：消息兜底拉取（4.1） | 调 `/api/common/chat/group/pageQueryChatRecord`（weapons 1995213） | 新增：游标管理 + 增量拉取 + 幂等落库 |  |
| 定时任务：Excel 兜底解析（4.2） | — | 新增：扫描当日未落库文件消息补解析 |  |
| 定时任务：工地快照拉取 + 视觉判断（4.3） | 调工地监控平台接口 + 视觉模型 | 新增：次日有送货计划的项目 → 拉快照 → 视觉判断落工地快照表（依赖 4.3 TODO 调研结论，未定则不上） |  |
| 定时任务：消息清洗打标（4.4） | — | 新增：未清洗消息扫描 + 上下文构造 + LLM 调用 + 问题表写入 |  |
| 定时任务：超时监控（4.7 + 3.4 漏答提醒） | — | 新增：双向扫描——① 已转发问题供应商 SLA 未回 → 提醒云助理外呼；② 问题库 PENDING 超时无回复 → 漏答提醒（哪群哪条多久没回） |  |
| 定时任务：每日汇总推送（4.2，小工具逻辑迁移） | 内调 `/api/msg/pushMsg`（weapons 1879071） | 新增：汇总拼接规则照搬现有 MOMA 小工具 → 定时推送 PM 作业群（19:30 可配）；3 个在线文档取数方式随迁移按现状定 |  |
| POST /api/assistant/excel/notify | 企微回调：群文件消息 → 解析落库 | 新增：按群路由解析器 + EasyExcel 解析 + 幂等落库 |  |
| GET /api/assistant/project/progress | 能力1：项目进度查询 | 新增：路由鲁班/内部接口/进展库聚合查询 |  |
| GET /api/assistant/supplier/plan | 能力5：供应商排期查询 | 新增：查排线落库表 + 未命中提示 |  |
| GET /api/assistant/issue/list | 侧边栏：问题列表（待确认/待转发/超时） | 新增：问题表分页查询 + **以人为维度分组聚合**（按提问人归集，4.4） |  |
| POST /api/assistant/issue/confirm | 侧边栏：确认/修正标签（4.4 半自动） | 新增：更新 confirm_status + 触发流转 |  |
| POST /api/assistant/issue/forward | 侧边栏：一键转发（4.7 方案B）；内调 `/api/msg/pushMsg`（weapons 1879071） | 新增：组装文案 + 调 Roomie 发消息接口 + 问题表闭环 |  |
| POST /api/assistant/call/callback | Roomie 回调：外呼结果回流（能力2） | 新增：通话记录落问题表 |  |

> ⚠️ Roomie 平台侧需要的能力配合（消息事件/聊天记录/发消息/外呼/侧边栏功能位）除**外呼外**均为开放服务已发布接口（见第 1 节接口依据）；外呼接口未在开放服务文档中，见第 6 节 #5。

## 6、上游依赖

| # | 依赖方 | 依赖内容 | 是否卡上线 |
|-|-|-|-|
| 1 | Roomie `huiju-chat-workbench`（消息服务） | **企微群聊聊天消息事件**（weapons 1994890，4.1 实时推送）+ **查询群聊聊天记录** `/api/common/chat/group/pageQueryChatRecord`（weapons 1995213，4.1 兜底拉取）；单条消息查询（2003503/2003512）备用 | 是（4.1 数据源头） |
| 2 | Roomie `huiju-chat-workbench`（消息服务） | **发送消息（私聊、群聊）** `/api/msg/pushMsg`（weapons 1879071，4.7 转发、4.9 催办通知） | 是（4.7/4.9 主链路） |
| 3 | Roomie `huiju-chat-session`（会话服务） | 会话分配/指定坐席发起会话/查询在途会话（1855925/1999277/2009563，云助理作业端会话承接）；会话事件回调（1993996/1995532/1994022） | 一期否（云助理人工在 Roomie 工作台作业即可）；二期自动化接入则需要 |
| 4 | Roomie `huiju-chat-manage`（群服务） | 群创建/邀请成员/群成员列表/群详情（2060185/2060182/2060188/2060455）——供应商企微群迁移建群辅助 + 群元数据同步 | 否（辅助工具，非主链路） |
| 5 | Roomie 平台 | **外呼电话能力 + 通话记录回调**——⚠️ **未在本接口文档中**（文档只覆盖机器人/会话/消息/聊天记录/群/加微），需单独向 Roomie 平台确认接口与接入方式（会议已口头确认能力存在、能拿通话记录） | 是（4.6） |
| 6 | Roomie 平台 | Agent/Skill 配置 + 数据库 + 侧边栏功能位（已有能力，配置工作） | 否（配置项） |
| 7 | IAM/kepass | 三个服务（huiju-chat-session / huiju-chat-workbench / huiju-chat-manage）均需 **IAM 鉴权**：kepass 申请接入（https://paas.ke.com/subability/iam/applier/list）+ 加签（文档 https://paas.ke.com/document/doc/iam/275）；另需申请机器人账号（accountBusinessId）、业务方客户类型 businessCustomerCode | 是（所有 Roomie 接口调用的前置） |
| 8 | 鲁班 | 节点进度、送装预约查询接口（只读复用）；改约/取消页面 URL 规则 | 查询：是（4.5）；URL 规则：否（4.8 仅为跳转入口） |
| 8.5 | 工地监控平台（新增） | 按项目ID查摄像头列表 + 拉取快照/视频关键帧的接口；工地覆盖率数据——⚠️ 平台是否有对外接口、覆盖多少工地均未调研（4.3 TODO） | 待定（若接口存在且覆盖够 → 一期上；否则二期随覆盖率铺） |
| 9 | 台账/内部接口 | 项目基础信息（项目→大区→PM→群）、供应商映射（品牌→供应商→对接人→紧急联系人→群） | 是（4.3/4.4/4.7 路由基础；具体接口清单待调研） |
| 10 | 企微 | 数据回收群自建机器人申请 + 文件消息回调 | 是（4.2） |
| 11 | LLM 服务 | 群消息清洗打标（4.4）、转发文案生成（4.7）——公司内 LLM 网关选型待确认 | 是（4.4 核心链路） |
| 12 | 业务侧 | 供应商微信群 → 企微群迁移（20–30 个群，业务/供应链团队动作；可用 #4 群接口辅助建群） | 否（一期方案 B 不依赖；切方案 A 前必须完成） |

Roomie 环境域名：测试 `http://huiju-chat-session.huiju-chat.ttb.test.ke.com` / `http://huiju-chat-workbench.ttb.test.ke.com` / `http://huiju-chat-manage.ttb.test.ke.com`；生产 `http://huiju-chat-session.ke.com` / `http://huiju-chat-workbench.ke.com` / `http://huiju-chat-manage.ke.com`。接口变更通知关注 weapons 项目 23425（Roomie 开放服务新项目）。

## 7、表结构改动

新服务全新建库，共 8 张表（7.0 工地快照 1 张 + 7.1 消息 4 张 + 7.2 进展 2 张 + 供应商映射 1 张）。

### 7.0 工地快照表（4.3 视觉判断产物）

> ⚠️ 依赖 4.3 TODO 调研结论（工地监控平台接口/覆盖率）；调研不通过则本表不上，项目详情工地现场视角降级为仅鲁班节点进度。

```sql
-- 工地快照与视觉判断表（摄像头快照 + 视觉模型判断）
CREATE TABLE roomie_site_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  project_id VARCHAR(64) NOT NULL COMMENT '项目ID',
  camera_id VARCHAR(64) DEFAULT NULL COMMENT '摄像头ID（工地监控平台侧标识）',
  snapshot_url VARCHAR(512) NOT NULL COMMENT '快照OSS地址（原片留存，判断可回溯）',
  snapshot_time DATETIME NOT NULL COMMENT '快照时间',
  site_stage VARCHAR(64) DEFAULT NULL COMMENT '视觉判断：工地当前阶段推断（水电/瓦工/木工/油工/安装等）',
  ready_hint VARCHAR(32) DEFAULT NULL COMMENT '可送货/可安装建议：READY=可送 CAN_NOT=不建议（现场障碍） UNKNOWN=置信度低不判断',
  blockers VARCHAR(512) DEFAULT NULL COMMENT '观察到的障碍描述：堆料/无人在场/地面未完成等',
  vision_confidence DECIMAL(4,3) NOT NULL DEFAULT 0 COMMENT '视觉判断置信度',
  biz_date DATE NOT NULL COMMENT '业务日期（与排线/安装计划关联）',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_project_time (project_id, snapshot_time),
  KEY idx_date (biz_date)
) COMMENT='工地快照表：摄像头快照+视觉判断，辅助任务通知就绪决策';
```

### 7.1 消息库（数据来源① + 问题库）

```sql
-- 群原始消息表（全量落库，未清洗）
CREATE TABLE roomie_chat_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  group_id VARCHAR(64) NOT NULL COMMENT '群ID（Roomie/企维侧标识）',
  group_type TINYINT NOT NULL DEFAULT 0 COMMENT '群类型：1=PM作业群 2=供应商群 3=数据回收群',
  msg_id VARCHAR(64) NOT NULL COMMENT '消息ID（平台侧唯一）',
  msg_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1=文本 2=文件 3=图片 4=其他',
  sender_id VARCHAR(64) NOT NULL COMMENT '发送人ID',
  sender_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '发送人名称',
  content TEXT COMMENT '消息内容（文本原文；文件消息存文件名）',
  file_key VARCHAR(128) DEFAULT NULL COMMENT '文件消息的存储key（4.2用）',
  msg_time DATETIME NOT NULL COMMENT '消息发送时间',
  classify_status TINYINT NOT NULL DEFAULT 0 COMMENT '清洗状态：0=未清洗 1=已清洗 9=噪音跳过',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_msg (group_id, msg_id),
  KEY idx_classify (classify_status, msg_time),
  KEY idx_group_time (group_id, msg_time)
) COMMENT='群聊原始消息全量表';

-- 拉取游标表（4.1 增量拉取的偏移量记录：每群一行，只拉游标之后的新增，禁止全量重拉）
CREATE TABLE roomie_msg_pull_cursor (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  group_id VARCHAR(64) NOT NULL COMMENT '群ID',
  last_msg_id VARCHAR(64) DEFAULT NULL COMMENT '偏移量：上次成功拉取的最后一条消息ID（游标，全部页成功才推进）',
  last_msg_time DATETIME DEFAULT NULL COMMENT '辅助偏移量：最后一条消息时间（接口不支持按msgId过滤时的时间窗起点）',
  last_pull_time DATETIME NOT NULL COMMENT '最后成功拉取时间',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group (group_id)
) COMMENT='消息拉取游标表：偏移量增量拉取，防全量重复拉取';

-- 问题表（清洗产物，一条问题一条记录，全流程状态机；三层归类：question_type=层1、resolve_method=层2、action_param=层3）
CREATE TABLE roomie_issue (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  msg_id VARCHAR(64) NOT NULL COMMENT '来源消息ID',
  group_id VARCHAR(64) NOT NULL COMMENT '来源群ID',
  question_type VARCHAR(32) NOT NULL COMMENT '层1-问题类型：MATERIAL_RECTIFY/DELIVERY_ADJUST/BIZ_VERIFY/CUSTOM_CABINET/MATERIAL_PROGRESS/OTHER',
  resolve_method VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '层2-解决方式（已确认，归类的落点）：QUERY_INFO=查询项目信息 TRANSFER=问题转接（转供应商群） OPERATION=业务操作 CALL=外呼 NONE=无需处理',
  action_param JSON COMMENT '层3-具体行为（放最后，层2决定后填充的执行参数）：如查询订单BJ12345送装预约信息/转霍尔茨群@张三',
  llm_confidence DECIMAL(4,3) NOT NULL DEFAULT 0 COMMENT 'LLM置信度',
  confirm_status TINYINT NOT NULL DEFAULT 0 COMMENT '确认状态：0=待云助理确认 1=已确认 2=已修正标签 9=高置信免确认（确认主要对象=层2/层3）',
  project_id VARCHAR(64) DEFAULT NULL COMMENT '归属项目ID（4.3映射）',
  supplier_id VARCHAR(64) DEFAULT NULL COMMENT '归属供应商ID（转发路由用）',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '流转状态（由层2驱动）：PENDING=待处理 ANSWERED=已直答(QUERY_INFO) FORWARDED=已转发(TRANSFER) CALLED=已外呼(CALL) RESOLVED=已闭环 ESCALATED=已升级人工',
  action_detail JSON COMMENT '动作明细：转发目标群/人、通话记录、回答内容摘要',
  reply_msg_id VARCHAR(64) DEFAULT NULL COMMENT '供应商/云助理答复消息ID',
  resolve_time DATETIME DEFAULT NULL COMMENT '闭环时间',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_msg (msg_id),
  KEY idx_type_status (question_type, status),
  KEY idx_resolve_status (resolve_method, status),
  KEY idx_confirm (confirm_status, ctime),
  KEY idx_supplier (supplier_id, status)
) COMMENT='问题库：三层清洗归类产物（问题类型/解决方式/具体行为）与流转状态机';

-- 供应商映射缓存表（4.3：每日刷新的本地缓存；项目详情供应商视角聚合的数据底座）
CREATE TABLE roomie_supplier_mapping (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  supplier_id VARCHAR(64) NOT NULL COMMENT '供应商ID',
  supplier_name VARCHAR(128) NOT NULL COMMENT '供应商/品牌名称',
  contact_user VARCHAR(64) NOT NULL DEFAULT '' COMMENT '对接人（@目标）',
  contact_phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '紧急联系人电话（外呼号码）',
  wecom_group_id VARCHAR(64) DEFAULT NULL COMMENT '供应商企微群ID（转发目标）',
  material_types VARCHAR(256) NOT NULL DEFAULT '' COMMENT '承接品类：木门/卫浴/吊顶等',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_supplier (supplier_id)
) COMMENT='供应商映射缓存：品牌→对接人→电话→群';

### 7.2 进展库（数据来源②，Excel 落库）

> ⚠️ 列结构为按需求文档口径的设计草案，**评审时拿排线表/入库表真实样例逐列校正**（见 4.2 假设）。

```sql
-- 排线表落库（每日派车送货计划）
CREATE TABLE roomie_delivery_plan (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_date DATE NOT NULL COMMENT '业务日期（排线对应送货日）',
  project_id VARCHAR(64) NOT NULL COMMENT '项目ID',
  project_address VARCHAR(256) DEFAULT NULL COMMENT '送货地址',
  supplier_id VARCHAR(64) DEFAULT NULL COMMENT '供应商/品牌',
  material_type VARCHAR(32) DEFAULT NULL COMMENT '品类：主材/辅材/定制柜',
  material_detail VARCHAR(256) DEFAULT NULL COMMENT '材料明细',
  plan_send_time VARCHAR(64) DEFAULT NULL COMMENT '计划送货时段',
  truck_no VARCHAR(32) DEFAULT NULL COMMENT '派车牌号',
  raw_row JSON COMMENT '原始行数据（容错留底，列变更可回放重解析）',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_date_project (biz_date, project_id, material_detail(128)),
  KEY idx_supplier_date (supplier_id, biz_date)
) COMMENT='排线表：每日派车送货计划';

-- 实际入库落库
CREATE TABLE roomie_stock_in (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_date DATE NOT NULL COMMENT '业务日期（入库日）',
  supplier_id VARCHAR(64) NOT NULL COMMENT '供应商/品牌：金牌/伊莱利/博洛尼/志邦/索菲亚/兔宝宝等',
  project_id VARCHAR(64) DEFAULT NULL COMMENT '项目ID（入库表能解析出项目时填）',
  material_detail VARCHAR(256) DEFAULT NULL COMMENT '入库材料明细',
  in_stock_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否实际入库：1=是 0=预计未到',
  raw_row JSON COMMENT '原始行数据留底',
  ctime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  mtime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_date_supplier_detail (biz_date, supplier_id, material_detail(128)),
  KEY idx_date (biz_date)
) COMMENT='实际入库表：供应商每日入库记录';
```

`raw_row JSON` 留底设计：列结构频繁变更时（Excel 表头增减），解析失败/容错的数据可由原始行回放重解析，不用回群里翻历史文件。

## 8、风险评估

| # | 风险点 | 评估 |
|-|-|-|
| 1 | **清洗任务批量 LLM 调用放大**：每 5 分钟扫未清洗消息逐条调 LLM，高峰期（PM 集中提问时段）积压时可能一次几百条 | 批量构造 prompt（一次带多条消息+上下文，LLM 返回 JSON 数组），单次调用处理 10–20 条；LLM 网关 QPS 调研后定并发数（第 6 节 #11） |
| 2 | **Roomie 消息事件回调与兜底拉取重叠**：双通道写同一张表 | uk_group_msg 唯一索引幂等去重，重叠无副作用 |
| 3 | **工地快照视觉调用成本**：每个待送货项目每日 1–2 次快照 + 多模态判断，工地数量放大 | 仅对「次日有送货/安装计划」的项目拉快照（不做全量轮询）；独立 Apollo 开关 + 每日上限数，超限自动停 |
| 4 | **Excel 解析失败积压**：表头变更导致整表解析失败 | raw_row JSON 留底 + 告警通知开发人工介入，兜底任务自动重试同表头规则不生效（避免错误数据入库） |

## 9、Check List

- [ ] Apollo 配置：**有**——群ID→解析器映射、`forward.auto.enable`（转发自动/半自动开关）、LLM 模型/prompt 版本、清洗扫描间隔、高置信阈值、各定时任务开关（含工地快照任务独立开关）、Roomie 三服务域名（测试/生产）与 IAM 加签密钥
- [ ] SQL 变更：**有**——新库 8 张表（7.1 消息 4 张 + 7.2 进展 2 张 + 工地快照 1 张 + 供应商映射 1 张），DDL 见第 7 节
- [ ] 前置接入（第 6 节 #7）：kepass 申请 IAM 接入（huiju-chat-session / huiju-chat-workbench / huiju-chat-manage 三服务）+ 机器人账号 accountBusinessId + businessCustomerCode 申请 + 消息事件回调登记
- [ ] 上线顺序：kepass/IAM/机器人账号申请 → 建库建表 → 服务发布（定时任务开关默认关）→ 灰度开 4.1 消息事件接收 + 兜底拉取（验证落库）→ 开 4.4 清洗打标（验证标签质量，人工抽检）→ 配 Roomie Agent/Skill（4.5 查询直答上线）→ 开 4.2 Excel 落库 → 上侧边栏半自动确认/转发 → （企微群迁移完成后）开 `forward.auto.enable` 切自动转发
- [ ] 回滚：各功能独立 Apollo 开关，按上线逆序逐个关闭即回滚；数据层落库数据保留不影响 Roomie 现有人工作业（云助理可随时回到全人工模式）

## 10、评审会议纪要

### 参会人员

QA:
开发:

### Todo List

> 记录评审改进意见。
