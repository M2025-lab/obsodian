# 📚 agenda 接入 Leonis 私聊推送手册（OpenPush 通道）

> 沉淀对象：eden-agenda → construction-agent `LeonisPersonalPusher#push` 的完整接入链路（含选型、前置配置、纯新增代码清单、排查手册）
> 一句话结论：**agenda 无法直调 `LeonisPersonalPusher#push`（那是小师父的内部 Spring Bean），唯一正确入口是小师父已上线的开放端点 `POST /api/v1/open/push`——agenda 侧纯新增「DTO + Feign 方法 + Manager 封装」三件套即可拥有该能力，小师父侧零代码改动，只补一条任务配置数据 + Apollo 白名单。**
> 沉淀日期：2026-11-26
> 关联笔记：[[📚 starlord Kafka消息接入配置手册]]（同为跨服务接入手册）、[[📚 常用接口手册]]（open/push 可考虑登记）、[[📚 后端项目索引]]

## 1. 全景链路：一条推送从 agenda 走到 Leonis 私聊

```text
┌─ eden-agenda（纯新增三件套）─────────────────────────────────────────┐
│ 业务代码 → LeonisPushManager.send(...)                                │
│   └→ ConstructionAgentFeign.pushOpen(...)   ← 本手册 §4 新增         │
│        POST {construction.agent.url}/api/v1/open/push                 │
└──────────────────────┬────────────────────────────────────────────────┘
                       ▼
┌─ construction-agent（零代码改动，现有链路）───────────────────────────┐
│ OpenPushController.push (OpenPushController.java:27)                 │
│   └→ ChannelTaskService.triggerByBusinessAsync (:216)                │
│        ① 查 construction_channel_task（BUSINESS_TRIGGER+enabled+env）│
│        ② 异步执行 @Async("openPushExecutor")，返回 jobId             │
│        ③ ChannelTargetExpander.expandRefs (:125)                     │
│             sourceType=LEONIS + targetType=PERSONAL                   │
│             → leonis_private_binding 按 ucid+env+sourceKey 查绑定     │
│             → ResolvedTarget(targetRef=ucid, userClientId=…)          │
│        ④ ChannelDispatcher.dispatch (ChannelDispatcher.java:27)      │
│             taskType=TEMPLATE → 变量替换生成正文                      │
│             taskType=AGENT    → 调大模型生成正文                       │
│        ⑤ ChannelPushRouter.route (:18)                               │
│             LEONIS+PERSONAL → LeonisPersonalPusher                    │
│        ⑥ LeonisPersonalPusher.push (LeonisPersonalPusher.java:29)    │
│             → KhalaReplyParser.parse(正文)  text 或 card             │
│             → LeonisManager.sendCustomerText / sendCustomerCard      │
│             → Leonis 私聊消息落到目标员工的企微私聊                     │
└──────────────────────────────────────────────────────────────────────┘
```

分层职责一览（每层只管自己的事，接错层就是白干）：

| 层 | 归属 | 管什么 | 关键代码 |
|-|-|-|-|
| 通信层 | agenda | Feign 声明 + Manager 校验封装 | `ConstructionAgentFeign`、`ConstructionAgentManager` |
| 开放层 | 小师父 | 参数校验、查任务、异步化、发 jobId | `OpenPushController.java:27` |
| 目标层 | 小师父 | ucid → 绑定 → ResolvedTarget | `ChannelTargetExpander.java:125` |
| 内容层 | 小师父 | TEMPLATE 变量替换 / AGENT 大模型生成 | `ChannelContentResolver.java:45` |
| 路由层 | 小师父 | sourceType+targetType → 具体 Pusher | `ChannelPushRouter.java:18` |
| 发送层 | 小师父 | 解析 card/text → 调 Leonis 发消息 | `LeonisPersonalPusher.java:29` |

## 2. 先选对门：为什么必须走 `/api/v1/open/push`

小师父对外有三个推送入口，只有一个能到达 `LeonisPersonalPusher`：

| 入口 | 路由树 | 能到 LeonisPersonalPusher 吗 |
|-|-|-|
| `POST /api/v1/open/push` | ChannelTaskService → ChannelPushRouter | ✅ **能**（LEONIS+PERSONAL 即路由到它） |
| `POST /push/send` | PushDispatchService → PushChannelManager 体系 | ❌ 不能（只有 APP_PUSH / WECOM / SMS 三棵子树，`PushChannelEnum` 无 LEONIS 值） |
| 直调 `LeonisPersonalPusher#push` | — | ❌ 不存在这条路（纯内部 Spring Bean，无 Controller 暴露；且 push 前还必须备好 `ResolvedTarget`+`KhalaReplyParser` 等一整套依赖） |

**坑①**：看到 `PushController#send` 就往里冲是本能反应，但 `PushSendParam.channel` 是 `PushChannelEnum` 枚举，压根没有 Leonis 私聊这个值——这棵树是 IM/企微/短信推送体系，和 `ChannelPusher` 渠道体系是**两套平行架构**，别混。

**坑②**：`LeonisPersonalPusher#push` 的入参 `targetRef` 填的是 **ucid（系统员工ID）**，不是 userClientId。pusher 内部自己按 `ucid + activeProfile + sourceKey` 查 `leonis_private_binding` 拿 userClientId。agenda 侧调用时只要传 ucid——绑定解析是小师父的事，**不要**自己先去查绑定再传 userClientId（那是 `listLeonisBindings` 接口的用途，属于另一个功能）。

> **调用方三段姿势（谁在什么时候查绑定）**：
> - **运行时不需要预查**：`pushLeonisPersonal(taskCode, sourceKey, [ucid], context, ucid)` 传 ucid 即可——绑定翻译（ucid→userClientId/robotWeworkId）发生在小师父内部的 `expandRefs`/`LeonisPersonalPusher`，调用方全程无需知道绑定字段。
> - **接入前一次性核对**：确认目标人绑定挂在「你要传的 sourceKey + 当前 env + enabled」下——这是数据核对（管理台/一次性 SQL），不是每次推送的运行时步骤。
> - **排障时才查明细**：`listLeonisBindings` 用于「发了没收到」时看绑定指向哪个企微账号。注意该方法**入参是 userClientId 不是 ucid**（名字带 ByUcid 但参数实际为 userClientId，`ConstructionAgentManager.java:108`），别被方法名误导。

## 3. 小师父侧前置条件（零代码，三件事）

小师父侧不改一行代码，但以下三样必须先备好，否则调用直接失败：

### 3.1 `construction_channel_task` 插一条 BUSINESS_TRIGGER 任务

`triggerByBusinessAsync` 第一步就查这张表（`ChannelTaskService.java:203-213`），查不到直接抛「任务不存在或未启用」：

| 字段 | 填什么 | 说明 |
|-|-|-|
| `task_code` | 自定，如 `agenda-xxx-notify` | 调用方传的 taskCode 要和它**完全一致** |
| `trigger_type` | `BUSINESS_TRIGGER` | 写死这个值，查表条件硬编码 |
| `enabled` | `1` | 否则查不到 |
| `env` | `test` / `prod` | 要和调用环境**一致**，查表条件含 env |
| `space_id` | agenda 自己的空间，如 `sp_xxx`（见下方说明；调用时传 null 可跳过该条件，但同名 taskCode 会串台） | 任务归属隔离 |
| `task_type` | `TEMPLATE` 或 `AGENT` | TEMPLATE=模板变量替换；AGENT=调大模型生成 |
| `prompt_template` | 模板正文，如 `【${title}】${content}` | TEMPLATE 模式下的变量替换源 |

**`space_id`（spaceId）是什么**：小师父的**多租户隔离单位「空间」的 ID**——`ConstructionChannelTask.java:39` 注释原文「空间ID，多租户隔离」。

| 维度 | 事实 |
|-|-|
| 格式 | `sp_` + 空间名 MD5 前 16 位（`SpaceServiceImpl.java:164` 生成），如打卡推送默认空间 `sp_035fdd9245519bdb` |
| 谁创建 | 小师父管理台「空间管理」页（`/admin-space`），或 `POST /api/v1/space/create`；建空间自动生成 ID |
| 隔离范围 | Agent 配置、Skill、知识库、渠道任务（`construction_channel_task`）、技能文件目录（`/pvc/{spaceId}/skills/...`）等全部按空间归属 |
| 查任务时干嘛用 | `getBusinessTriggerTask`（`ChannelTaskService.java:209-211`）把它加进查询条件——taskCode + spaceId **联合定位**，防两个空间同名 taskCode 串台 |
| 不传行吗 | 行（代码是 `if (spaceId != null)` 才拼条件），但任务表里若有同名 taskCode 的任务会查错行，推荐传 |
| 怎么拿自己的 | 管理台 `/admin-space` 页查看；或 `GET /api/v1/space/my` 列当前账号所在空间（`SpaceController.java:38`） |

> 接入动作：让小师父超管给 agenda 建（或分配）一个空间 → 记下 `sp_xxx` → 渠道任务配置和后续 Agent/Skill 资源都挂这个空间下 → 调用时 `spaceId` 就传它。参考现状：打卡推送的空间是 Apollo 默认值 `sp_035fdd9245519bdb`（`ApolloConfig.java:157`）。

> 模板变量：内置 `date` / `time` / `weekday`，其余来自请求里的 `context` Map，占位语法 `${key}`（`ChannelContentResolver.java:45-58`）。

> **⚠️ 请求里没有「自由正文」字段——`context` 不是 content**：推送请求体的字段全是路由参数 + 变量池，**正文本体永远在任务表 `prompt_template` 里**，由 `task_type` 二选一生成（TEMPLATE=模板替换 / AGENT=大模型，`ChannelDispatcher.java:31-42`）。`context` 的每个 key 只会被塞进模板的 `${key}` 占位符——模板里没有的占位符，传了也被丢弃（实测：模板「系统测试请忽略」+ context `{"title":...}` → 实收五个字原文）。三个推论：
> ① 同一 taskCode 的所有推送**共用一个模板**，换文案=换 taskCode 或改模板（改模板全局生效，影响所有调用方）；
> ② 动态内容全靠「模板挖 `${key}` 占位符 + context 填值」；
> ③ **应急透传模式**：模板配成 `${content}`，调用传 `context={"content":"任意全文"}`——模板降级为透传管道，自由正文即恢复，通知类接入常用这招。

### 3.2 目标人要有 Leonis 私聊绑定

`leonis_private_binding` 表里必须已有该员工 ucid + 对应 env + sourceKey 的绑定记录且 enabled，userClientId 非空。没有绑定 → `expandRefs` 抛 SilentSkipException → **目标被静默跳过，job 状态显示 SKIP 而非 FAIL**。绑定通常由 Leonis 71001 私聊事件自动产生，或经管理台 `POST /api/v1/admin/leonis/bindings` 手工补。

**绑定归属是命门（体系不允许跨渠道自选绑定）**：绑定三元组 `ucid + env + sourceKey` 硬过滤（`LeonisPrivateBindingService.java:80-93`），**没有「传 ucid 自动挑任一可用绑定」的语义**——目标人绑定挂哪个 sourceKey 下，推送管道就必须传哪个，挂别处则静默跳过。绑定关系天生属于用户（他的企微账号与哪个机器人的私聊），调用方不能替他选。两条路线：

| 路线 | 做法 | 渠道选择权 | 何时用 |
|-|-|-|-|
| A 管道跟随绑定 | 推送前查该 ucid 的绑定 source_key（查库/让小师父开 findByUcids 为 open 端点），用查到的值调推送 | 在用户的绑定 | 短期联调、目标人绑定挂在别人的渠道下时 |
| B 渠道自持绑定 | 自建 sourceKey（`source_agent_config` 注册）+ 把推送目标的绑定都建到它下面（管理台/71001） | 在自己，推送永远命中，**无需任何前置查询** | 最终形态，不依赖小师父渠道下数据归属 |

> 小师父侧 `findByUcids(ucids, env)` 不带 sourceKey 的批量绑定查询已存在（注释标注「供 /api/v1/open/push 私聊兼容路径使用」），但未暴露为 open HTTP 端点——路线A 想优雅实现可推动小师父加一个（纯新增）。

**`sourceKey` 是什么**：**渠道实例的唯一标识**——`source_agent_config` 表（小师父的「渠道配置表」）的 `source_key` 字段，DO 注释原文「来源唯一标识」。**一个 sourceKey = 一个具体的消息渠道实例**，如 `WEIXIN_XIAOSHIFU`（小师父 Leonis 渠道，打卡推送用的默认值，`ApolloConfig.java:164`）。

在这条链路里它决定的事（LEONIS PERSONAL 路径，均有源码依据）：

| 决定什么 | 代码依据 | 说明 |
|-|-|-|
| 用哪套私聊绑定 | `LeonisPersonalPusher.java:31` | `findByUcid(ucid, env, sourceKey)` 查 `leonis_private_binding` 带 source_key 条件——绑定挂渠道下，同员工不同渠道可有不同绑定 |
| 谁收 + 哪个机器人发 | `LeonisPersonalPusher.java:37-41` | **都从命中的那条绑定记录读**：`binding.getUserClientId()`（收件账号）+ `binding.getRobotWeworkId()`（发送机器人）——本路径**不查 source_agent_config** |
| （其他渠道才查配置表） | `WechatBotPusher.java:35` | 仅 WECHAT_BOT 等渠道按 sourceKey 查 `source_agent_config` 拿 webhookUrl；LEONIS PERSONAL 用不到 |

**taskCode 与 sourceKey 的分工（最易混）**：taskCode 只管**内容**（TEMPLATE 模板变量替换 / AGENT 大模型生成），完全不参与推送路由；sourceKey 只管**选绑定**（决定谁收、哪个机器人发）。两者正交——换文案改 taskCode/模板，换收件账号/机器人换 sourceKey（前提是目标人的绑定挂在那）。同一 ucid+env+sourceKey 只应有一条启用绑定：`findByUcid` 取 `list.get(0)`，多条时命中哪条不确定。

**「任务挂在某 sourceKey 下」并不存在**：`construction_channel_task` **没有 sourceKey 字段**（DO 与实际导表均核实）。sourceKey 只存在于两处——① API 请求参数（传了 targetRefList 即以它为唯一权威，`ChannelTaskService.java:158-163`）；② 任务的 target 行（仅 targetRefList 为空才读，此时被完全旁路）。任务查询五条件（taskCode/trigger_type/enabled/spaceId/enabled）里也没有 sourceKey。因此**「任务配在小师父渠道 + 我传自己绑定渠道」可以任意组合**：内容用任务的模板，管道走你 sourceKey 命中的绑定（含 robotWeworkId），推送照常成功。边界：纯推送不查 `source_agent_config`（发送路径不碰）；但机器人**收消息**按 robotWeworkId 反查该表路由（`LeonisRobotAccountService.java:36`）——只发不收没问题，要对话闭环须把渠道注册进 `source_agent_config`。

> 它从哪来：管理台渠道配置页维护；Leonis 消息回调进来时按 `robotWeworkId` 反查 `SourceAgentConfig` 得到 sourceKey（`LeonisRobotAccountService.java:36-51`），随链路下传。
> 对接入的约束：agenda 传的 sourceKey 须 ① `source_agent_config` 有此配置且 enabled；② 目标员工的 `leonis_private_binding.source_key` 与它一致（否则查空 → SKIP）。**复用已有渠道最省事**；要自建渠道实例则绑定也得挂到它下面。
> 和 spaceId 的分工：spaceId 管**资源归属**（配置挂在哪个空间），sourceKey 管**消息从哪个渠道实例进出**——`KhalaRoleSourceMappingProperties.java:20` 的 Apollo 示例正好展示两者成对出现。

### 3.2.1 路由参数：sourceType / targetType（渠道体系 × 群/个人）

**两个字段合起来回答「走哪种渠道体系、发给群还是个人」——是 `ChannelPushRouter.route()` 选择具体 Pusher 实现的二维坐标。**

| 字段 | 回答什么 | 取值 |
|-|-|-|
| `sourceType` | 走哪种**渠道体系**（协议层） | `LEONIS` / `KHALA` / `WECHAT_BOT` |
| `targetType` | 目标是**群还是个人**（形态层） | `PERSONAL` / `GROUP` |

路由矩阵（`ChannelPushRouter.java:18-31`，全量核实）：

| sourceType | targetType | 路由到的 Pusher | targetRef 填什么 |
|-|-|-|-|
| **LEONIS** | **PERSONAL** | **LeonisPersonalPusher ← 本接入目标** | 员工 **ucid** |
| LEONIS | GROUP | LeonisGroupPusher | 群 **chatId** |
| KHALA | PERSONAL | KhalaPersonalPusher | 员工 ucid |
| WECHAT_BOT | （不看 targetType） | WechatBotPusher | 群 chatId |
| 其他组合 | | NonRetryableException「不支持的渠道类型」 | — |

三个关键联动（均有源码依据）：

1. **targetRef 语义随组合变**：同为「目标标识」，LEONIS+PERSONAL 是 ucid，LEONIS+GROUP 是 chatId——没有「ref 字段说明」，只有「组合下的 ref 说明」。
2. **sourceType 在展开阶段就分叉**：`expandRefs` 仅 `LEONIS+PERSONAL` 组合才查 `leonis_private_binding` 拿 userClientId（`ChannelTargetExpander.java:131`）。
3. **targetType 决定身份注入**：GROUP 注入 `ucid=0/username="群聊"` 假身份；PERSONAL 用绑定表真实员工身份（`ChannelTargetExpander.java:157-170`）。

> 与 sourceKey 的关系最易串：sourceType 是**种类**（LEONIS 这类体系），sourceKey 是**实例**（WEIXIN_XIAOSHIFU 这套配置）——类比「手机号 vs 某个具体号码」。OpenPushRequest 两个都要传：种类决定路由到哪个 Pusher，实例决定查哪套配置和绑定。

### 3.3 Apollo 配置 `un.auth.urls` 加白

`/api/v1/open/push` 不带登录 token 时会被 `OperatorInterceptor` 拦（`OperatorInterceptor.java:118-123`，前缀匹配 `startsWith`）。两个选择：

- **方案A（推荐）**：agenda 的 Feign 调用带上 `X-KE-UCID` 请求头（与现有 `ConstructionAgentFeign` 同款鉴权写法，见 §4.2）——无需任何 Apollo 改动；
- **方案B**：Apollo `un.auth.urls` 列表加 `/api/v1/open/push`——完全免鉴权，需确认安全上可接受再走这条。

## 4. agenda 侧新增代码（纯新增三件套，不动任何现有逻辑）

落点全部在已有的 `manager/construction/` 包内（`~/IdeaProjects/eden-agenda`），与现有 `chatDirectAsync` 调用并列，零侵入：

### 4.1 新增 DTO：`OpenPushRequestDTO` + `OpenPushJobResultDTO`

路径：`eden-agenda-manager/src/main/java/com/ke/utopia/agenda/manager/construction/dto/`

```java
/** 小师父 POST /api/v1/open/push 的请求体。字段与小师父 OpenPushRequest 一一对应。 */
@Data
public class OpenPushRequestDTO {
    private String spaceId;      // 任务空间，须与 construction_channel_task.space_id 一致
    private String taskCode;     // 须与 construction_channel_task.task_code 一致
    private String sourceKey;    // 渠道标识，如 WEIXIN_XIAOSHIFU；决定用哪套 Leonis 绑定
    private String sourceType;  // Leonis 私聊固定填 LEONIS
    private String targetType;   // Leonis 私聊固定填 PERSONAL
    private List<String> targetRefList; // 目标员工 ucid 字符串列表，≤50 个
    private String operatorUserId;      // 操作人，仅记录不校验
    private Map<String, Object> context; // 模板变量，prompt_template 里的 ${key} 从这取
}
```

```java
/** 小师父 POST /api/v1/open/push 的返回：data = {"jobId":"push_yyyyMMdd_xxxxxxxx"}。 */
@Data
public class OpenPushSendResultDTO {
    private Integer code;
    private String message;
    private Boolean success;
    private Map<String, String> data;
}
```

```java
/** 小师父 GET /api/v1/open/push/{jobId} 的返回。 */
@Data
public class OpenPushJobResultDTO {
    private Integer code;
    private String message;
    private Boolean success;
    private JobResult data;

    @Data
    public static class JobResult {
        private String jobId;      // push_yyyyMMdd_xxxxxxxx
        private String status;     // RUNNING / SUCCESS / PARTIAL_SUCCESS / FAIL
        private Integer total;
        private Integer successCount;
        private Integer failCount;
        private List<TargetResult> details;

        @Data
        public static class TargetResult {
            private String targetRef;    // ucid
            private String status;       // SUCCESS / FAIL（SKIP 是日志层的标记）
            private String sentContent;  // 实际发出的正文
            private String errorMsg;
        }
    }
}
```

### 4.2 在现有 `ConstructionAgentFeign` 上加两个方法（纯追加）

路径：`eden-agenda-manager/src/main/java/com/ke/utopia/agenda/manager/construction/feign/ConstructionAgentFeign.java`

```java
    /** 发起 Leonis 等渠道推送（异步执行，立即返回 jobId）。 */
    @PostMapping(value = "/api/v1/open/push", consumes = MediaType.APPLICATION_JSON_VALUE)
    OpenPushSendResultDTO pushOpen(@RequestHeader(value = "X-KE-UCID", required = false) String ucid,
                                   @RequestBody OpenPushRequestDTO request);

    /** 按 jobId 查推送结果；未跑完 status=RUNNING。 */
    @GetMapping("/api/v1/open/push/{jobId}")
    OpenPushJobResultDTO queryPushJob(@RequestHeader(value = "X-KE-UCID", required = false) String ucid,
                                     @PathVariable("jobId") String jobId);
```

> 说明：返回外层包直接复用 4.1 的 `OpenPushJobResultDTO` 结构即可——小师父 `ResultDTO<...>` 反序列化后 `code/message/success/data` 字段形状和现有 `ChatAgentResultDTO` 一致。`X-KE-UCID` 鉴权头写法与该 Feign 已有的两个方法**完全同款**（«不配 ucid 时不发该头»），走的是 §3.3 方案A。

### 4.3 在现有 `ConstructionAgentManager` 上加两个方法（纯追加）

路径：`eden-agenda-manager/src/main/java/com/ke/utopia/agenda/manager/construction/ConstructionAgentManager.java`
写法对齐同文件里 `chatDirectAsyncByAgent`：Builder 组请求 → 显式校验 `code=2000` → 失败抛 `UtopiaBussinessException`。

```java
    /**
     * 发起 Leonis 私聊推送：立即返回 jobId，实际发送在小师父侧异步执行（maxRetries=3）。
     *
     * @param taskCode  与 construction_channel_task.task_code 一致（BUSINESS_TRIGGER、enabled）
     * @param sourceKey 渠道标识，决定查哪套 leonis_private_binding
     * @param ucids     目标员工 ucid 列表（≤50）
     * @param context   模板变量，prompt_template 的 ${key} 从这取
     * @param ucid      身份头 X-KE-UCID；null 时不发该头
     * @return jobId，用于 queryPushJob 轮询
     */
    public String pushLeonisPersonal(String taskCode, String sourceKey, List<String> ucids,
                                     Map<String, Object> context, String ucid) {
        if (CollectionUtils.isEmpty(ucids)) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_PARAM_ILLEGAL, "ucids 不能为空");
        }
        OpenPushRequestDTO request = OpenPushRequestDTO.builder()
                .taskCode(taskCode).sourceKey(sourceKey)
                .sourceType("LEONIS").targetType("PERSONAL")
                .targetRefList(ucids).context(context)
                .operatorUserId(ucid)
                .build();
        log.info("调用小师父Leonis私聊推送, taskCode={}, sourceKey={}, ucids={}, ucid={}",
                taskCode, sourceKey, ucids, ucid);
        OpenPushSendResultDTO result = constructionAgentFeign.pushOpen(ucid, request);
        if (result == null) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_INVOKE, "小师父返回为空");
        }
        if (result.getCode() == null || result.getCode() != SUCCESS_CODE) {
            log.error("小师父推送提交失败, code={}, message={}", result.getCode(), result.getMessage());
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_INVOKE,
                    "小师父推送提交失败: " + result.getMessage());
        }
        String jobId = result.getData() == null ? null : result.getData().get("jobId");
        if (StringUtils.isBlank(jobId)) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_INVOKE, "小师父未返回 jobId");
        }
        return jobId;
    }

    /**
     * 查推送结果。返回 RUNNING / SUCCESS / PARTIAL_SUCCESS / FAIL + 每目标明细。
     */
    public OpenPushJobResultDTO.JobResult queryPushJob(String jobId, String ucid) {
        OpenPushJobResultDTO result = constructionAgentFeign.queryPushJob(ucid, jobId);
        if (result == null) return null;
        if (result.getCode() == null || result.getCode() != SUCCESS_CODE) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_INVOKE,
                    "查询推送结果失败: " + result.getMessage());
        }
        return result.getData();
    }
```

### 4.4 业务侧调用示例（agenda 任意 Service）

> **✅ 已落地（2026-11-26）**：三件套已写入 eden-agenda 分支 `feat/public/roomie-group` 并编译通过（BUILD SUCCESS）：
> ① DTO×3 → `manager/construction/dto/OpenPushRequestDTO / OpenPushSendResultDTO / OpenPushJobResultDTO`
> ② Feign 追加 → `ConstructionAgentFeign#pushOpen / queryPushJob`
> ③ Manager 追加 → `ConstructionAgentManager#pushLeonisPersonal / pushOpen / queryPushJob`（写法对齐同文件既有方法：Builder 组请求 + 校验 code=2000 + UtopiaBussinessException）
> ④ **单人直推版**（只持有 userClientId 场景）→ `ConstructionAgentManager#pushLeonisPersonalByUserClientId(taskCode, userClientId, context)`：两跳合一——先 `listLeonisBindingsByUcid` 查绑定（取 enabled 且 ucid 非空里 id 最大/最新的一条），拿命中记录的 `ucid`+`sourceKey` 复用 `pushLeonisPersonal` 提交。内容因人而异走逐人调用 + context（模板占位符或 `${content}` 透传）。无启用绑定直接抛异常（不静默）。
> 注意：本机编译须用 JDK8（`JAVA_HOME=$(/usr/libexec/java_home -v 1.8) sh mvnw compile -pl eden-agenda-manager -am`），JDK21 会因 lombok 反射限制失败。

```java
    @Resource
    private ConstructionAgentManager constructionAgentManager;

    public void notifyManager(String managerUcid, String orderNo) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", "衣柜到货提醒");
        context.put("orderNo", orderNo);
        // taskCode 对应小师父 DB 里那条 BUSINESS_TRIGGER 任务；sourceKey 对应 Leonis 渠道实例
        String jobId = constructionAgentManager.pushLeonisPersonal(
                "task-f7b69d40e3a14f1c", "WEIXIN_XIAOSHIFU",
                Collections.singletonList(managerUcid), context, managerUcid);
        log.info("Leonis私聊推送已提交 jobId={}", jobId);
        // 需要闭环的再轮询 queryPushJob(jobId, ucid)；fire-and-forget 可以不查
    }
```

> 配置项：服务地址沿用现有 Feign 上的 `${construction.agent.url:http://meta.home.ke.com}`（prod 已配 `https://construction-agent-web.ke.com`，test 环境走 Apollo 下发），**无需新增任何配置**。taskCode/sourceKey 建议像 `LedgerParseService` 那样写成常量，不进配置文件。

## 5. 调用与验证

### 5.1 联调前先用 curl 直打小师父（把 agenda 变量摘干净）

```bash
curl -X POST 'https://<construction-agent-host>/api/v1/open/push' \
  -H 'Content-Type: application/json' \
  -H 'X-KE-UCID: <任一有效ucid>' \
  -d '{
    "spaceId": "sp_xxx",
    "taskCode": "agenda-wardrobe-arrival-notify",
    "sourceKey": "WEIXIN_XIAOSHIFU",
    "sourceType": "LEONIS",
    "targetType": "PERSONAL",
    "targetRefList": ["1234567890"],
    "operatorUserId": "1234567890",
    "context": {"title": "联调测试", "orderNo": "PO-1"}
  }'
# 期望返回：{"code":2000,"data":{"jobId":"push_20261126_xxxxxxxx"},...}
```

### 5.2 轮询 jobId 直到非 RUNNING

```bash
curl 'https://<construction-agent-host>/api/v1/open/push/push_20261126_xxxxxxxx' \
  -H 'X-KE-UCID: <ucid>'
# 期望：status=SUCCESS / total=1 / successCount=1 / details[0].sentContent 为模板替换后正文
```

### 5.3 验收清单（全部通过才算接入完成）

| # | 检查项 | 通过标准 |
|-|-|-|
| 1 | curl 直打提交 | 返回 code=2000 + jobId |
| 2 | 轮询结果 | status=SUCCESS，sentContent 变量已替换 |
| 3 | 目标本人的企微 Leonis 私聊 | 收到文本（或卡片）消息 |
| 4 | agenda 侧调用 | pushLeonisPersonal 返回 jobId，无 UtopiaBussinessException |
| 5 | 无绑定场景（换一个未绑定 ucid） | 目标被 SKIP，job 为 PARTIAL_SUCCESS/SUCCESS，**不抛异常**——静默跳过是设计行为 |
| 6 | 未绑定任务场景（故意传错 taskCode） | agenda 抛「小师父推送提交失败: 任务不存在或未启用…」，任务配置回查 construction_channel_task 四字段 |

## 6. 排查手册

### 6.1 四症状决策树

| 症状 | 优先查 | 说明 |
|-|-|-|
| 提交即失败「任务不存在或未启用」 | `construction_channel_task` 四字段 | task_code 全等 + trigger_type=BUSINESS_TRIGGER + enabled=1 + env=环境。四字段任一不匹配都查不到（`ChannelTaskService.java:203-213`） |
| jobId 有了，目标被 SKIP（静默无消息） | `leonis_private_binding` | 该 ucid 无绑定 / enabled=0 / userClientId 空 / env 或 sourceKey 不匹配。**SKIP 不算 FAIL**，查 details 只有 FAIL 才进重试 |
| **任务表/job 显示 SUCCESS 但没收到消息** | 按下表四链路排查 | SUCCESS ≠ 发出。**三种静默机制**：① 绑定三元组（ucid+服务env+请求sourceKey）没精确命中同一条绑定 → expandRefs 逐个吞掉 → 空目标 → 0 个 FAIL → markSuccess（`ChannelTargetExpander.java:129-148` + `ChannelTaskService.java:387-399`）；② LeonisPersonalPusher 不检查 sendCustomerText 返回值，底层 doPost 把异常吞成 errcode=-1 的 Map，没有任何一层看 errcode → Leonis 拒收也 SUCCESS（`LeonisPersonalPusher.java:37` + `LeonisClientImpl.java:132-138`）；③ 同一 ucid 多条绑定挂不同企微账号（不同 userClientId/昵称）——消息发到了另一台手机的账号上。定位：jobId 查 details（空=链路①；有 SUCCESS+sentContent=链路②③）→ 日志 grep `[ChannelTargetExpander] 跳过目标` 和 `[Leonis] 响应`（body 里 errcode 是真实原因；无此行=根本没发） |

> **实测案例（2026-11-26）**：请求 sourceKey 传 `ROOMIE-YUNZHULI`，但该 ucid 的 prod 绑定挂在 `WEIXIN_XIAOSHIFU` 下 → 三元组查空 → 目标被静默剔除 → 任务表 `SUCCESS`（15:23:22）但全程未调 Leonis 发送。修复 = sourceKey 改传绑定表里实际的 `WEIXIN_XIAOSHIFU`。**教训：sourceKey 不是「业务方想要的名字」，必须与 leonis_private_binding.source_key 逐字一致——排查时先查绑定表，再回填参数。**
| status=FAIL，details 里有 errorMsg | 小师父日志按 jobId 搜 | `ChannelTaskExecutor` 会先重试 3 次（间隔递增 1s/2s/3s）才落 FAIL；errorMsg 常见为 LeonisManager 调 Leonis 平台超时 |
| 返回 401/鉴权拦截 | 请求头 `X-KE-UCID` | 该接口不在默认放行名单，不带 ucid 头会被 OperatorInterceptor 拦（`OperatorInterceptor.java:118`） |

### 6.2 日志关键字速查

| 侧 | 关键字 | 出处 |
|-|-|-|
| 小师父 | `[ChannelTaskService] 异步触发任务 jobId=` | 提交成功即打，含 taskCode/targets 数量 |
| 小师父 | `[ChannelTargetExpander] 跳过目标 ref=` | 绑定缺失的静默跳过 |
| 小师父 | `[ChannelTaskExecutor] 执行推送 target=` | 每个 target 执行入口（含完整 ResolvedTarget JSON） |
| 小师父 | `[ChannelTaskExecutor] 失败重试 / 推送失败` | FAIL 前的重试与最终失败 |
| agenda | `调用小师父Leonis私聊推送, taskCode=` | Manager 层提交入参 |
| agenda | `小师父推送提交失败` | 提交侧异常（任务不存在也走这里） |

### 6.3 易混淆点

| 易混点 | 正解 |
|-|-|
| `pushOpen` vs `PushController#send`（/push/send） | 两套架构。/push/send 走 PushChannelManager（APP_PUSH/WECOM/SMS 枚举），**没有 Leonis**；本接入走 /api/v1/open/push |
| targetRef 填 ucid 还是 userClientId | 填 **ucid**。userClientId 由小师父 pusher 内部查绑定获得（`LeonisPersonalPusher.java:30-33`） |
| sourceType 填 LEONIS_PERSONAL 还是 LEONIS | 开放接口层填 `LEONIS`（`LEONIS_PERSONAL` 是 DB 渠道配置层的旧标识，`expandRefs`/`route` 匹配的是组合值 LEONIS+PERSONAL） |
| 同步还是异步 | 异步。接口立即返回 jobId，正文生成+发送都在小师父后台线程池跑（TEMPLATE 也有模板替换耗时，AGENT 更是大模型调用），**不要在 agenda 侧同步等结果** |
| jobId 查不到 | 提交后轮询过早（任务尚未落 RUNNING 日志）或 jobId 抄错；返回 fail 而非报错是正常行为 |
| status=PARTIAL_SUCCESS | 部分目标成功部分失败/跳过，看 details 逐个定位，job 级不用整体重发 |

## 7. 一句话记忆

> **口诀：内部 Bean 不直调，开放接口走 open/push；任务表配好四字段，ucid 即目标、绑定自己查。**

- **现象**：agenda 要 Leonis 私聊推送能力 → `LeonisPersonalPusher` 是小师父内部 Bean，没有 HTTP 入口
- **根因**：跨服务调用只能走对方暴露的端点；唯一能路由到 LeonisPersonalPusher 的开放端点是 `POST /api/v1/open/push`
- **陷阱**：① `/push/send` 是另一套推送体系（无 Leonis）；② targetRef 传 ucid 不是 userClientId；③ 任务表四字段（task_code/trigger_type/enabled/env）任一不符即「任务不存在」；④ 无绑定的目标静默 SKIP 不报错
- **定位**：curl 直打 → 轮询 jobId 看 details → 小师父日志搜 `[ChannelTaskExecutor]` + jobId

---
---
