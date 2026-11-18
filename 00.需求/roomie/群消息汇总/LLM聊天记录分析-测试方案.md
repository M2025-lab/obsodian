---
tags:
  - 需求
  - 云助理
  - Roomie
  - LLM
date: 2026-11-17
status: draft
source:
  - "[[群消息问题归类汇总-技术方案]]"
---

# LLM 聊天记录分析-测试方案

> 目标：用已导入的真实聊天记录跑通「**拉取 → LLM 归类 → 落库 → 对比人工基线 → 调 prompt 重跑**」循环，用指标驱动 prompt 调优。
> 定位：测试期方案，机制从简（一个接口跑全链、一轮一清、不推进状态）；生产化的完整设计（Job 闭环/防重三闸/归并 CAS）仍以 [[群消息问题归类汇总-技术方案]] 为准，prompt 验证达标后再切生产链路。

## 1、现状盘点

| 资产 | 状态 | 说明 |
|-|-|-|
| LLM 能力层 | ✅ 已提交（HEAD `cb0bd4a`「LLM接入」） | manager/llm 六件套：LlmClient（schema 结构化解析 + 回喂重试）、AiModelConfig（Apollo ai-config 注册表）；okhttp/kotlin 已钉版；真实网关冒烟通过（qwen3.6-plus） |
| 测试数据 | ✅ 前 100 条已插入测试库 | `test-zgk-yw-matsup` 群，2026-09-15 起最早一段；全量文件 1447 条待续导（INSERT IGNORE 可重复执行） |
| 人工基线 | ✅ 在 Excel「问题明细」sheet | 208 条问题：问题概述/客户/提问人/提问内容([M00xx 溯源])/回复人/解决方案/**品类**(定制柜41·吊顶40·木门21…)/**问题类型**(进度确认145·主材问题整改58·云助理操作取消5)；赵公口 209 行里 208 有效 |
| 拉取 SQL | ✅ 已重建（2026-11-17） | `RoomieMsgAnalyzeMapper.selectPendingMessages` + Service.pagePending + Controller（`GET /api/v1/open/msg-analyze/pending?groupId=…` 正序游标分批） |
| §4.2 落库 | 🟡 表+mbg 已就绪 | `roomie_msg_issue` 建表 + RoomieMsgIssue model/Mapper/XML 已生成提交（`1a1daa7`，2026-11-17 用户执行）；落库 Dao 封装 + analyze_status 回写未写 |
| 分析主链路 | ❌ 未写 | PromptAssembler / 调用 / 对比工具 |

**数据侧已钉死的事实**（导入时处理过的坑，后续别再踩）：

- 云助理 3 账号（`sender_role_desc='云助理'`）：20090135 黄佳丽(闪闪/三千) / 31794230 罗泽丹 / 31764493 张皓寒——消息里他们是**答复方**，人工基线回复人 TOP3 正是这仨
- `quote_msg_key` 已全解析（213+16）：同题追问锚点，AI 识别归并的主要线索
- 可选字段一律空串不 NULL（表约束 NOT NULL）；外部人 sender_id=`WX-名字或号码`
- 消息类型实际分布：文本 1001 / 引用回复 213 / 图片类 127 / 视频 10 / 语音 10——**图片/视频/语音无正文内容**，prompt 里要交代「凭上下文理解，不要臆造内容」

## 2、测试链路（一个接口跑全链）

生产方案的 §4.0 Job 闭环对测试迭代太重（防重三闸、逐 issue 事务、CAS 换版都是为长期稳态设计的）。测试期用一个**同步分析接口**跑完整链，一轮一清：

```
POST /api/v1/open/msg-analyze/analyze-test
  ?groupId=910001000000000001&limit=100&batchSize=50&reset=true

  MsgAnalyzeService.analyzeOnce(param)
    ├─ reset=true 时：DELETE FROM roomie_msg_issue WHERE group_id=?  （清上轮结果）
    │                  UPDATE roomie_group_message SET analyze_status=0 WHERE group_id=?（重置状态）
    ├─ 拉取：selectPendingMessages 分批（**纯 id 正序游标** `WHERE id>? ORDER BY id`，batchSize 条/批；Excel 导入数据 id 与 msg_time 严格同步单调，二者等价；切 kafka 实时流时迟到消息会错位，生产链路另定游标口径）
    ├─ 每批：PromptAssembler 拼 prompt → LlmClient.chat(schema) → 解析 issues
    ├─ 落库：issue 逐条 INSERT（roomie_msg_issue）+ involved 消息 analyze_status=1、noise 打 9
    └─ 返回：本轮统计（批次数/LLM 调用次数/issue 数/耗时/token 用量）+ issue 列表预览

GET /api/v1/open/msg-analyze/analyze-test/result
  ?groupId=...  → 落库的 issue 全量（对比工具消费）
```

**与生产方案的取舍**（测试期从简的边界，都写死在方案里防止误解）：

| 机制 | 生产方案 | 测试期 | 理由 |
|-|-|-|-|
| 拉取防重 | onlyPending 闸 + uk_message_id 幂等 | 保留 onlyPending（拉取 SQL 复用）；issue 表不设 uk（一轮一清，reset 保证） | onlyPending 实现成本≈0 顺手防重；issue 防重靠 reset |
| 归并 | mergeInto CAS 换版 + 24h 窗口 | **一批内**同题合并（LLM 单批输出即全量快照）；**跨批**靠批次重叠上下文 + 归并候选清单，不做 CAS（多轮并发不存在——同步接口单线程） | 单轮串行执行，无并发冲突场景 |
| 分批边界 | 三层（重叠/归并/兜底） | 批间上下文重叠 30min + 喂「前批已建 issue 的 detail 摘要清单」 | 跨批合并退化为「prompt 候选 + 二次合并」 |
| 状态推进 | analyze_status 单向 0→1/9 | reset=true 全清重跑；prompt 改一版就 reset 重跑一轮 | 迭代节奏：改 prompt → 重跑 → 对比，一天可能十几轮 |
| Job 定时 | MsgAnalyzeJob 常驻 | 无 Job，手动触发接口 | 测试期不需要自动增量 |

**跨批二次合并**（测试期归并的简化实现）：全批跑完后，对相邻批次的 issue 做一次**二次 LLM 合并判定**——把「批 A 尾部 issue + 批 B 头部 issue」的 detail 两两喂给 LLM 问「是否同一问题的两半」，是则 UPDATE 合并（保留早的行，晚的 involved 并入）。可配置开关 `mergeCrossBatch=false` 默认关，先看单层效果。

## 3、输出 schema（对齐人工基线口径）

人工基线「问题明细」的核心列 → 我们输出字段的一一对应（对比就按这个对齐）：

| 人工基线列 | 输出字段 | 对比口径 |
|-|-|-|
| 问题概述（一句话） | `detail`（前半句即人读摘要） | 人工肉眼对比/LLM 辅助判相似 |
| 问题类型（进度确认/主材问题整改/云助理操作取消） | `question_type` | **硬指标**：枚举直接比对 |
| 提问的品类（定制柜/吊顶/木门…） | `detail` 内嵌品类（prompt 强制写入） | 硬指标：从 detail 抽取比对 |
| 问题对应的客户 | `detail` 内嵌客户名 | 软指标：从 detail 抽取比对 |
| 提问人 | `sender_name` | 硬指标 |
| 提问内容/回复内容（带 [M00xx] 溯源） | `group_message_ids`（join 消息表反查 msg_key） | **召回率核心**：基线 Q0001 锚 M00018 → 我们该有一条 issue 的消息 ID 对应 msg_key=M00018 |
| 回复的解决方案 | `detail` 尾句 + `status` | 软指标：语义比对 |
| 提问时间/最终回复时间 | group_message_ids 首末消息的时间（join 消息表） | 校验字段 |

表结构（2026-11-17 用户定稿：type 整数+名称快照双列、group_message_ids/group_id、品类优先枚举）——一行 = 一个问题循环：

| 需求口径 | 列 |
|-|-|
| 对应的消息id列表 | `group_message_ids` JSON（元素为 roomie_group_message.id 纯 ID 数组；**基线锚定 join 消息表反查 msg_key**） |
| 来源群 | `group_id`（roomie_group_message.group_id）+ `group_name` 快照 |
| 提问人 | `sender_id` / `sender_name` |
| 问题类型 | `type` TINYINT + `question_type_name` 名称快照；编码落 `MsgIssueTypeEnum`（base/enumeration，value+name+llmCode 三列，`fromLlmCode` 翻译 LLM 字符串枚举，认不出归 OTHER）；**品类优先**：橱柜/定制柜归 4，其余品类（含木门/木作）按性质 1/2/3，0=其他 |
| AI 整理的问题描述 | `detail` TEXT——**必须内嵌关键信息**：业主、项目标识、供应商、品类、任务类型、诉求与进展 |
| 解决状态 | `status` TINYINT（0=待解决 1=已解决）+ `resolve_time` |
| 问题生成时间 | `issue_time`（AI 首次归类出该问题的时间） |

```sql
CREATE TABLE roomie_msg_issue (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  group_message_ids JSON NOT NULL COMMENT '该问题对应的消息id列表（JSON数组，元素为roomie_group_message.id）',
  group_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源群ID（roomie_group_message.group_id，标识来自哪个群）',
  group_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '来源群名快照（带出便于人读）',
  sender_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '提问人ID',
  sender_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '提问人名称',
  type TINYINT NOT NULL DEFAULT 0 COMMENT '问题类型编码：1主材问题整改 2进度确认 3云助理操作取消 4橱木类进展 0其他（品类优先：橱柜/定制柜归4）',
  question_type_name VARCHAR(32) NOT NULL DEFAULT '' COMMENT '问题类型名称快照（人读，如"进度确认"）',
  detail TEXT NOT NULL COMMENT 'AI整理的问题描述：须包含业主、项目标识、供应商、品类、任务类型、诉求与当前进展等关键信息',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '解决状态：0=PENDING待解决 1=RESOLVED已解决',
  resolve_time DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '解决时间（status=1时有效）',
  issue_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '问题生成时间（AI首次归类出该问题的时间）',
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_group (group_id),
  KEY idx_type (type),
  KEY idx_sender (sender_id)
) COMMENT='问题汇总表（测试版）：一行=一个问题循环，AI归类产物';
```

> 关键信息（业主/品类/供应商/项目标识）**只存 detail 富文本里不拆独立列**——统计过滤测试期用不上，对比脚本从 detail 里按「括号标注」或 LLM 辅助抽取即可。基线锚定不设 msg_key 独立列，join 消息表由 group_message_ids 反查。

LLM 单批输出 JSON（能力层 schema 校验已内建；`detail` 为富描述直接落列，其余关键字段拆列）：

```json
{
  "issues": [
    {
      "detail": "客户吴建哲的定制柜满足送货条件，项目经理申请安排配送；云助理答复已约到9月17日送货。",
      "questionType": "MATERIAL_PROGRESS",
      "mergeInto": null,
      "involvedMsgKeys": ["TEST-ZGK-M00018", "TEST-ZGK-M00024"],
      "firstAskMsgKey": "TEST-ZGK-M00018",
      "resolved": true
    }
  ],
  "noiseMsgKeys": ["TEST-ZGK-M00021", "TEST-ZGK-M00028"]
}
```

关键字段去向：`customer/category/supplier/projectKeyword` 是 LLM 的**输出字段**（写进 detail 富描述），落库只存 detail 一列，不再拆独立列。`resolved` 落库映射 `status` 0/1。

`mergeInto`（批内）：指向同批输出里更早的 issue 的 firstAskMsgKey。noise = 与任何问题无关的消息（寒暄/收到/表情/纯图片无语义）。**部分处理协议**照搬生产方案：未列入 issues 且未列 noise 的消息保持 0（不落库不打标），下轮或 debug 用。

## 4、Prompt 设计

System prompt（写死在功能层，不进 Apollo——测试期高频改动，跟代码一起进 git 更快）：

```text
你是被窝家装「主材调度群」的消息分析助手。输入是一个群的一段时间内的消息流（按时间正序）。
你的任务：识别群里提出的【业务问题】并归类，输出 JSON。

## 什么是「业务问题」
项目经理/交付/外部经理向云助理或调度提出的具体诉求，包括：
- 进度确认：查询订单/补货/到货/送货/安装进度（"什么时候送""到哪了"）
- 主材问题整改：质量瑕疵/破损/尺寸不对/需维修/需退货重做
- 云助理操作取消：要求云助理代为取消/修改系统预约/减项
- 咨询核对：政策/费用/流程类询问
不算问题（→ noise）：
- 寒暄、感谢、"收到"、表情、纯图片/视频/语音无文字说明
- 云助理之间的内部协调沟通（不影响问题识别的消息）
- 对已有问题的重复追问（→ 合并进已有 issue，不新开）

## 同一问题的多条消息 → 合并为一条 issue
- 引用回复（quoteMsgKey 指向被引消息）大概率是同题追问或答复
- 同一客户同一品类在短时间内（同一天）的反复询问视为同一问题
- 追问/补充信息/云助理的答复 → 全部进 involvedMsgKeys，不拆开
- 跨客户的同一类问题【必须】拆开（李傲男的木门 和 王宸的木门是两个问题）

## 无人可读内容
图片/视频/语音消息 textContent 为空——凭前后文判断它属于哪个问题（如「这是地板划痕照片」），不要臆造内容；无法判断归属的归 noise。

## 归类枚举（questionType，必须从这个集合选）
WOOD_CABINET_PROGRESS（橱木类进展）/ MATERIAL_RECTIFY（主材问题整改）/ MATERIAL_PROGRESS（进度确认）/ CLOUD_CANCEL（云助理操作取消）/ OTHER

**判定优先级（品类优先）**：涉及**橱柜/定制柜**（仅这两个品类）→ 一律 WOOD_CABINET_PROGRESS，无论其性质是进度查询还是整改；其余品类（木门、木作、地板、吊顶等）按性质归 RECTIFY/PROGRESS/CLOUD_CANCEL。
映射基线：进度确认→PROGRESS、主材问题整改→RECTIFY、云助理操作取消→CLOUD_CANCEL（对比时注意基线「进度确认」里橱柜/定制柜部分会被我们判成 4，这是口径差不是错误）。

输出 JSON Schema（能力层强制）+ few-shot 由 PromptAssembler 注入。
```

User prompt 结构（PromptAssembler 拼装，四段）：

```
【群信息】赵公口&永外-主材调度群（群成员角色见消息流 senderRoleDesc=云助理 的人是调度方/答复方）

【本批消息】（时间正序，quote→ 表示引用了哪条）
M00018 09-15 17:01 被窝项目经理 李辉[外部]: 客户，赵庆钰 吴建哲 定制柜可以送货了
M00019 09-15 17:09 被窝胡中华[外部]: 客户闫立强 …下单了七片，运费200元…
M00021 09-15 17:12 张皓寒[云助理]: 各位经理好，我和佳丽正在返回公司途中…
M00024 09-15 18:24 张皓寒[云助理] quote→M00018: 您好，吴建哲--9.17送 赵庆钰--这位在系统里没有约时间…

【上文衔接】（只读，帮助你理解连续性；这些消息已分析，禁止输出为 involved 或 noise）
…前一时间段消息摘要或原文…

【归并候选】（本批之前的批次已建立的问题；新问题如果是它们的续聊，设 mergeInto 指向其 firstAskMsgKey）
- TEST-ZGK-M00186: 李傲男木门修复（MATERIAL_RECTIFY）
- TEST-ZGK-M00098: 王宸橱柜安装改期（MATERIAL_PROGRESS）
```

消息行格式刻意**紧凑**（token 成本）：`编号 时间 发送人[角色]: 正文 | quote→编号`。100 条消息 ≈ 3-5K token，qwen3.6-plus 单次调用余量充足。

## 5、对比与迭代

### 5.1 对比工具（一次性 Python 脚本，不进 Java）

`scripts/compare_baseline.py`（vault 或项目下均可）：

- **输入**：① 结果导出 JSON（`GET result` 接口或直接 SQL 导 roomie_msg_issue）② 人工基线（Excel「问题明细」sheet 解析）
- **锚定键**：基线每条问题的提问内容里带 `[M00xx …]` 溯源标记 → 提取首问 msgKey；我们的 issue 落库后按 group_message_ids join 消息表得 msg_key → **按 msgKey 直接对上**，不做语义模糊匹配（对不上就明说，宁可暴露不掩盖）
- **输出四张表**：
  1. **召回漏报**：基线有、我们没（按 msgKey 找不到对应 issue）——prompt 阈值调松的方向
  2. **误报**：我们有、基线没有——prompt 收紧方向；再分「真误报（基线人漏了）」和「假误报（其实该算问题）」人工标注
  3. **类型正确率**：对上的 issue 里 questionType 与基线「问题类型」映射（进度确认→MATERIAL_PROGRESS、主材问题整改→MATERIAL_RECTIFY、云助理操作取消→CLOUD_CANCEL）比对
  4. **合并质量**：基线多条问题行被我们合成 1 条 issue（over-merge）／基线 1 条被我们拆多条（under-merge）——分批边界与同题判定直接相关
- 汇总一行指标：`召回率 / 精确率 / 类型准确率 / 漏报N / 误报N / over-merge N / under-merge N`

### 5.2 迭代循环

```
改 prompt（SystemPrompt 常量或 few-shot）
  → reset=true 重跑 analyze-test
  → compare_baseline.py
  → 看四张表定位问题 → 下一轮
```

**调 prompt 的顺序约定**（先修数据面的硬伤，再修语义软伤）：

1. 第一轮先只看**召回/误报**（问题边界对不对），类型准确率先放一边
2. 边界对了再调 **类型映射**（few-shot 每类给 2-3 个正例+反例，直接从误判样本里捞）
3. 最后调**合并粒度**（under/over-merge：改「同题判定」段落的时间窗/品类+客户双键描述）

每轮改动记录在本文档末尾的**迭代日志表**（版本号+改动点+四指标），防止调着调着倒退了不知道。

### 5.3 基线的已知局限（对比时心里有数）

- 人工基线是**选择性收录**：208 条都是「已完结」的问题，群里大量寒暄/协调没收录——所以对比「误报」时，我们列出的基线没有的 issue 需要人工看一眼是不是真误报（有些是基线口径外但确实算问题）
- 基线「问题概述」近 200 个取值是人写的细分类，**不要求我们的 detail 跟它字面一致**，对比只看 questionType/品类/客户/涉及消息
- 基线截止 11-13（赵公口）/11-05（鲁谷），尾部几天消息可能没有基线对应——正常

## 6、落地清单（小步交付）

每步交付后先给你看核心片段再继续（吸取上轮「一次写一堆直接回退」的教训）：

| # | 步骤 | 内容 | 依赖 |
|-|-|-|-|
| 1 | ✅ 拉取三层（2026-11-17 重建） | Dao 三件 + Service.pagePending + Controller（`GET /pending`）；analyze-test 待落库层后补 | 无 |
| 2 | ✅ 已完成（2026-11-17 用户执行） | `roomie_msg_issue` 建表 + mbg 生成三件套已提交（`1a1daa7`） | - |
| 3 | issue 表 Dao + report 落库 | RoomieMsgIssueDao 封装（mbg 三件套已在）+ 逐 issue INSERT + analyze_status 回写 | 1,2 |
| 4 | PromptAssembler | §4 的 system prompt 常量 + user prompt 四段拼装 + 消息行紧凑格式 | 1 |
| 5 | analyze-test 接口 | Service.analyzeOnce（reset/分批/调用/落库/统计返回）+ result 接口（`GET /pending` 已有） | 3,4 |
| 6 | Apollo ai-config 配置 | apiKey/model（`msganalyze` key）——你配置，key 参考冒烟用过的那个 | 5 |
| 7 | 首轮跑通 + compare 脚本 | 前 100 条跑第一轮出指标四张表 | 5,6 |
| 8 | 迭代循环 | prompt 版本 v1→v2→…，指标记录在本文档迭代日志 | 7 |

迭代日志：

| 版本 | prompt 改动 | 召回 | 精确 | 类型准确 | merge 观察 |
|-|-|-|-|-|-|-|
| v0 | 首版（§4 全文） | - | - | - | - |
