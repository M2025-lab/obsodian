# eden-agenda-issue自动闭环异步化-技术方案

> 飞书文档：（占位，同步飞书时填）

## 1、背景[必选]

- 需求来源：[[00.需求/roomie/群消息汇总/LLM聊天记录分析-测试方案]] §5.25 issue 自动闭环链路
- 工程范围：**仅 eden-agenda**，construction-agent（小师父）侧零改动（异步提交 + 回调能力已具备，`DirectSubAgentService.java:176`）
- 现状问题：`MsgAnalyzeServiceImpl#reply` 同步等待 AI 返回——提交小师父后 `awaitAgentResult` 轮询（间隔 3s、超时 5min，`MsgAnalyzeServiceImpl.java:651,654,841`），一条回复占住 HTTP 连接/任务线程 1~2min。本方案**纯新增**一套异步闭环口：新增 `replyAsync` 方法提交即返回 messageId，AI 回复完成后由小师父回调收口（推回原群 + issue 终态落库）；`reply` 及全链路存量代码零改动。

## 2、需求理解[必选]

| # | 需求点 | 口径 |
|-|-|-|
| 1 | 异步提交口 | **新增 `replyAsync` 方法**（动作同 reply 前半段：取 issue → 拼正文 → 提交小师父），提交后立即返回 messageId；`reply` 本体零改动 |
| 2 | 回调接口 | 小师父 Agent 跑完后回调本服务做收口：查结果 → 推回原群 → issue 终态/回滚落库；报文规范按 construction-agent 现行回调实现 |
| 3 | 回调规范 | `POST` JSON `{messageId, status: SUCCESS\|ERROR, agentCode}`；小师父侧失败重试 3 次、间隔 2s（`DirectSubAgentService.java:723-750`） |
| 4 | messageId↔issue 关联 | `roomie_msg_issue` 加列存 messageId（用户已确认，DB 方案） |
| 5 | 定时 sweep 链路 | 本次不动，二期限（用户已确认）——`autoReplySweep` 仍走同步 `replyAndPush` |
| 6 | 零改动边界 | `reply` / `replyAndPush` / `autoReplySweep` / `analyzeSweep` / `AGENT_CALLBACK_URL` 常量全部不动，本期纯新增（方法 + 接口路径） |

## 3、现状

### 3.1 issue 自动闭环同步链路

```
手动按钮/定时 sweep（MsgAnalyzeController.java:96,107 / autoReplySweep:757）
   │ issueId
   ▼
reply(issueId)  MsgAnalyzeServiceImpl.java:680
   ├─ ① 取 issue（getById）
   ├─ ② buildReplyContent 拼正文（群ID/时间/提问人/业主/detail）
   ├─ ③ chatDirectAsyncByAgent 提交小师父 → messageId（ConstructionAgentManager.java:49）
   │      回调地址传的是常量 AGENT_CALLBACK_URL="/api/roomie/agent/chat"（相对路径，:663）
   ├─ ④ awaitAgentResult 轮询：3s 间隔 / 5min 超时（:651,654,841-867）← 占线程 1~2min 的根源
   ▼
IssueReplyVO{reply, messageId, costMs}
   └─ replyAndPush（:717）再加推群 pushToGroups → pushItems 明细
```

⚠️ 现状回调实际失效：`AGENT_CALLBACK_URL` 是相对路径（`/api/roomie/agent/chat`），小师父 `RestTemplate` 发不出去（发往 `http://api/roomie/agent/chat`）；全靠 ④ 轮询兜着。本期**不动这个常量**（同步链路轮询口径维持现状），新异步方法回调 URL 走 bootstrap 按环境写死（4.3）。RoomieDebugController 已有回调样例接口 `/api/v1/open/roomie/construction-agent-callback`（`RoomieDebugController.java:114`），ledger 链路的回调先例 `agenda.ledger.callback-url` 指向它（`bootstrap-preview.properties:37`）——但该接口**只记日志不做事**（ledger 语义「回调只当信号用、结果靠轮询」，`LedgerParseSettings.java:63`），issue 收口要在回调内做实事（查结果+推群+终态），复用它要么改存量代码（违背纯新增边界）、要么退化回轮询模式，故本方案新增专用回调口（§5 row 2），既有接口不动。

### 3.2 issue 状态机（roomie_msg_issue.status）

| 值 | 语义 | 落点 |
|-|-|-|
| 0 | 待回复（队列口径） | `listPendingAutoReply` 捞它（`RoomieMsgIssueDaoImpl.java:91`） |
| 1 | 已解决（LLM 归类时 resolved） | 落库 `toModel:461` |
| 2 | 回复中占位（CAS 0→2 防并发） | `casStatus`（`RoomieMsgIssueDaoImpl.java:105`） |

回调收口复用这套语义：占位 2 保持（终态=已回复，同 sweep 成功路径），失败回滚 2→0 放行下轮 sweep 重试。

## 4、详细设计

> 本节全部为**新增代码**：`replyAsync` / `handleReplyCallback` 两个新 Service 方法 + 两个新 Controller 端点 + 两个新 dao 方法；复用 `buildReplyContent` / `chatDirectAsyncByAgent` / `casStatus` / `pushToGroups` 均为调用，不修改。

### 4.1 总链路（异步版）

```
POST /api/v1/open/msg-analyze/issue-reply-async?issueId=xxx        （新增）
   ├─ ① 取 issue + CAS 0→2 占位（防手动按钮/sweep 并发重复回复）
   ├─ ② buildReplyContent 拼正文（调用现有方法）
   ├─ ③ chatDirectAsyncByAgent 提交，callbackUrl = bootstrap 环境配置（见 4.3）
   ├─ ④ 落 reply_message_id（在途标记）
   └─ ⑤ 立即返回 {issueId, messageId, status=PENDING}

（1~2min 后，小师父侧 Agent 跑完）
POST {callback-url}  body={messageId, status: SUCCESS|ERROR, agentCode}    （新增）
   ├─ ① 按 messageId 反查 issue（getByReplyMessageId，idx 命中）
   ├─ ② 幂等闸：反查 miss（marker 已清）或 status≠2 → 直接 ack 返回
   ├─ ③ status=SUCCESS：queryMessageByAgent 查回答全文
   │      ├─ 回答非空 → pushToGroups 推回 issue.groupId → status 保持 2（终态）→ 清 marker
   │      └─ 回答为空 → 推送无意义 → 回滚 2→0 + 清 marker 放行下轮
   └─ ④ status=ERROR：回滚 2→0 + 清 marker 放行下轮 sweep 重试，errorMsg 落日志

（恢复路径：不新增兜底任务，全部落回现有链路）
回调 handler 收口异常 → 回滚 2→0 + 清 marker → 下轮 sweep 现有同步链路捞 0 重试（零改动）
回调永久丢失（3 次重试全败/网络故障）→ issue 卡 status=2 + marker 残留：
   不影响其它 issue 回复（listPendingAutoReply 只捞 0），测试期 reset 一轮一清可恢复；二期 sweep 改造加超时兜底
```

**marker 幂等语义**：`reply_message_id` 非空 = 在途（已提交未收口），收口即清（置空串）。小师父重试回调 / 晚到的重复回调反查 miss → ack 天然幂等；sweep 同步链路不写 marker，晚到回调也命中不了它的 issue，不会二次推群。

### 4.2 表结构（见 §7 DDL）

`roomie_msg_issue` 新增 `reply_message_id VARCHAR(64)`：异步提交后落库（在途标记 + 回调反查锚点）；多实例部署天然安全（回调打任意实例都能命中）。配套 dao 新增两方法（Example API 风格，沿 `RoomieMsgIssueDaoImpl` 现有写法，不拆 ext XML——Invalid bound statement 教训）：

| 方法 | 用途 |
|-|-|
| `updateReplyMessageId(issueId, messageId)` | 提交口落 marker；收口清 marker 复用同方法传空串（selective update 置 NULL 走不了，空串等效 miss） |
| `getByReplyMessageId(messageId)` | 回调按 messageId 反查 issue |

### 4.3 回调地址（bootstrap 按环境写死，沿 ledger 先例）

不走 Apollo、不走请求派生——`bootstrap-*.properties` 按环境各加一行绝对 URL（用户已确认；ledger 同模式先例 `agenda.ledger.callback-url`，`bootstrap-preview.properties:37`）：

```properties
# bootstrap-test.properties
agenda.roomie.reply.callback-url=http://eden-agenda.pandi.ttb.test.ke.com/api/v1/open/msg-analyze/issue-reply-callback
# bootstrap-preview.properties
agenda.roomie.reply.callback-url=http://preview-eden-agenda.ke.com/api/v1/open/msg-analyze/issue-reply-callback
# bootstrap-prod.properties
agenda.roomie.reply.callback-url=http://i.agenda.home.ke.com/api/v1/open/msg-analyze/issue-reply-callback
```

- 环境值随发布包走（bootstrap 是打包时选环境的 properties），发版即正确环境回调，无运行时配置动作；换环境值 = 改 properties 发版。
- 不选请求派生的原因：工程未开 `server.use-forward-headers`（全工程零配置），KE 网关是否保留原始 Host 头代码侧无法核实，派生可能拼出 pod 内网地址；ledger 先例（同服务唯一回调消费方）也未用派生。
- `@Value("${agenda.roomie.reply.callback-url:}")` 注入到新方法，空 = 不序列化不回调（`ChatAgentDirectRequest.java:30` NON_NULL）——某个环境没配就退化纯异步提交（无收口），启动即见、不静默。
- 值落提交日志（`chatDirectAsyncByAgent` 现有 log 行已含 callbackUrl，`ConstructionAgentManager.java:64`）——联调肉眼核对。
- 新方法自带该配置注入，**不动** `reply`/`analyzeByAgent` 现用的 `AGENT_CALLBACK_URL` 常量（其失效与轮询兜底维持现状）。
- 路径走 `/api/v1/open/**`：服务间调用无登录态，open 路径与既有 `MsgAnalyzeController` 同口径（`MsgAnalyzeController.java:33`）。

### 4.4 回调丢失处置（本期）与超时兜底（二期）

| 场景 | 本期处置 |
|-|-|
| 回调 handler 收口异常 | 先回滚 2→0 + 清 marker 再返回 success → 下轮 sweep 现有同步链路（`listPendingAutoReply` 捞 0）自然重试 |
| 回调永久丢失（3 次重试全败/网络分区） | issue 卡 status=2 + marker 残留——不影响其它 issue（队列表只捞 0）；测试期 reset 一轮一清可人工恢复 |

超时兜底（捞 `status=2 AND gmt_modified < NOW()-10min` → 补收口/回滚）需在 sweep 收口段追加调用，即改 `analyzeSweep`——超出本期「纯新增」边界，**归二期**与 sweep 异步化一并做（10min = Agent 超时 5min 的两倍冗余，设计口径保留）。

### 4.5 幂等与并发

| 场景 | 处理 |
|-|-|
| 小师父回调重试 3 次（`DirectSubAgentService.java:731`） | 首次收口后 marker 已清，重试回调反查 miss → ack |
| 手动异步口 vs sweep 串行链同时打同一 issue | 提交口入口即 CAS 0→2，抢不到直接拒绝（同现有 `autoReplySweep` 语义 `MsgAnalyzeServiceImpl.java:765`） |
| 回调晚到（下轮 sweep 已同步重试完成） | marker 已清，反查 miss → ack，不会二次推群 |
| 收口中途异常（查结果/推群网络抖动） | 回滚 2→0 + 清 marker → 下轮 sweep 现有同步链路重试（现有代码零改动） |

### 4.6 回调 handler 响应约定

小师父 `sendCallback` 判定成功的口径是「HTTP 调用不抛异常」（`DirectSubAgentService.java:733`），不校验业务 code。本接口：

- 收口成功 / 幂等命中（反查 miss 或 status≠2）→ `ResultDTO.success("ok")`；
- 收口过程抛异常 → **先回滚 2→0 + 清 marker** 再返回 success（小师父重试只会撞幂等 miss，无害无意义）；恢复交给现有 sweep 同步链路，issue 不卡死。

## 5、接口改动

| # | 入口类型 | 接口 | 改动点 | 估时 |
|-|-|-|-|-|
| 1 | HTTP（新增） | `POST /api/v1/open/msg-analyze/issue-reply-async?issueId=` | 异步提交口：`replyAsync(Long)` 新方法（`MsgAnalyzeService` + Impl 追加；Controller 在 `MsgAnalyzeController`（`MsgAnalyzeController.java:33`）同路由域追加新端点）：取 issue → CAS 0→2 → `buildReplyContent` → `chatDirectAsyncByAgent`（callbackUrl=`@Value` 注入 bootstrap 值，4.3）→ 落 reply_message_id → 返回 `{issueId, messageId, status=PENDING}` |  |
| 2 | HTTP（新增） | `POST /api/v1/open/msg-analyze/issue-reply-callback` | 回调收口口：`handleReplyCallback(messageId, status)` 新方法（同上追加）：body `{messageId, status, agentCode}` → `getByReplyMessageId` 反查 → 幂等闸 → SUCCESS 查结果+推群+终态清 marker / 失败回滚 2→0 清 marker |  |

`MsgAnalyzeController` / `MsgAnalyzeService` / `RoomieMsgIssueDao` 既有方法与既有接口路径（`issue-reply` / `issue-reply-push` 等）**零改动**；sweep 超时兜底归二期（4.4）。

## 6、上游依赖

| # | 依赖方 | 依赖内容 | 是否卡上线 |
|-|-|-|-|
| 1 | construction-agent | `POST /api/v1/chat-agent/direct-async`（异步提交，回调能力）——已上线，`ConstructionAgentFeign.java:33` 在用 | 否 |
| 2 | construction-agent | 回调机制：`sendCallback` POST JSON `{messageId, status, agentCode}`，失败重试 3 次间隔 2s（`DirectSubAgentService.java:723-750`） | 否 |
| 3 | 网关/域名 | 回调绝对 URL 可达性（agenda 三环境域名已定：测试 `eden-agenda.pandi.ttb.test.ke.com` / 预发 `preview-eden-agenda.ke.com` / 生产 `i.agenda.home.ke.com`；open 路径免登录态）——小师父测试/预发环境能否访问 agenda 对应域名，联调验证 | 是（联调验证项） |

## 7、表结构改动

```sql
-- roomie_msg_issue 增列（沿 homeowner_name / category 增列先例）
ALTER TABLE roomie_msg_issue
  ADD COLUMN reply_message_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '异步闭环在途标记：小师父messageId（回调反查锚点；收口即清空，空=无在途回复）' AFTER homeowner_name,
  ADD KEY idx_reply_message (reply_message_id);
```

mbg 三件套（model / Example / mapper XML）随新列重新生成（沿 homeowner_name 增列先例 `1a1daa7`，用户执行）；dao 侧新增 4.2 两个方法。

## 8、风险评估及对其它系统影响

- 回调 handler 内推群（`pushToGroups` 单群一次 HTTP）+ 查小师父（一次 HTTP）：均为同步外部调用，单次回调总耗时 <3s，量级 3 条/轮（reply-limit），无放大。
- 回调永久丢失时 issue 卡 status=2（本期无自动兜底，二期解）：不阻塞其它 issue（`listPendingAutoReply` 只捞 0），测试期 reset 一轮一清可恢复。
- 纯新增代码，存量同步链路（reply / replyAndPush / autoReplySweep）行为不变。
- 无其它。

## 9、Check List

- [x] **Apollo 配置**：无新增——回调 URL 走 `bootstrap-*.properties` 按环境写死（4.3，沿 ledger 先例）
- [x] **SQL 变更**：`roomie_msg_issue` 加列 + 索引（§7，一条 DDL）
- [ ] 上线顺序：DDL 加列（DEFAULT ''，无锁风险）→ mbg 重生成 → bootstrap 三环境各加一行回调 URL（4.3，域名已定）→ 发布（纯新增，不调新口不影响存量）→ 手动 `issue-reply-async` 联调（核对提交日志 callbackUrl + 小师父侧能否访问 agenda 域名）→ 回调收口链路验证 → 二期：sweep 异步化 + 超时兜底
- [ ] 回滚：纯新增代码不影响存量链路——停用新口即回退；DDL 列 DEFAULT '' 对存量行无感知，可留；bootstrap 行删除即退化不回调

## 10、评审会议纪要

### 参会人员

QA:
开发:

### Todo List

（评审时填）
