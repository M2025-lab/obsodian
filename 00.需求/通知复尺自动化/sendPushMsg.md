# com.ke.utopia.manager.push.PushClient#sendPushMsg 业务执行说明

> 本文按"递归分层 + 业务语义"格式还原，所有结论均基于源码逐行核对（无猜测）。 配套分析见 `doc/handleNode_business_analysis.md`（handlerNode 是它的最上游业务驱动方之一）。

---

# 一、业务背景

在主材任务流转（`handleNode` 等）过程中，每当节点状态变化（完成 / 激活 / 延期 / 复尺打回等），系统需要把"有新任务""任务已完成""复尺被驳回"等消息**推送给对应角色的执行人**（工长、安装工、管家、供应商等）。

推送分两条物理通道：

- **IM/Push 消息**（App 内任务提醒、工作助手）：由 `PushClient#sendPushMsg` 负责，最终调用链家 IM 服务（HTTP 表单 POST）。

- **短信**：由 `PushClient#sendPushNotice` 负责（本文聚焦 sendPushMsg，仅在第 2 层对照说明）。

`sendPushMsg` 解决的核心业务问题是：**把一次业务事件，转换为一条发往链家 IM 服务的群发 Push 请求，并做接收人灰度映射、渠道路由、异步发送与失败兜底。**

---

# 二、目标函数定位

> **模块边界注意**：`PushClient` 在 **manager** 模块（集成层），不在 service。它本身**不查数据库、不读写 Redis、不发 Kafka**——它的"下游"只有一次 RPC（链家 IM HTTP）和一次 RPC（Ceres 人事服务做 UCID 映射）。DB/Redis/Kafka 全部在更上层的 `MessagePushClient` 完成。本文递归到这两个 RPC 即停止（第三方服务内部无法继续追踪）。

---

# 三、第 1 层：sendPushMsg（manager 集成层）

## 3.1 这一层负责什么（业务语言）

> 这一层负责**把"谁（接收人 UCID 集合）+ 发什么（消息标题/内容/跳转 URL）+ 走哪条渠道"三要素，组装成一条发往链家 IM 的群发 Push 请求，并完成接收人灰度映射、渠道配置注入与异步投递。**

## 3.2 这一层做了哪几件事情

|   |   |   |   |
|---|---|---|---|
|序号|事情|负责函数|业务目的|
|①|入参合法性校验|内置 `checkArgument`（行 96–98）|拦截空接收人 / 超 100 人 / 空消息，防止脏请求打到 IM|
|②|构建群发参数骨架|`PushGroupSendParam.builder().build()`（行 100）|准备 IM 协议报文容器|
|③|按渠道读取 IM 配置|`getConfig(param.getChannel())`（行 102→74）|不同渠道（任务提醒/工作助手）对应不同的 appId/passcode/url|
|④|配置注入到报文|`BeanUtils.copyProperties(config, groupSendDto)`（行 103）|把 appid/passcode/url/msg_type 等协议字段从配置复制到报文|
|⑤|接收人灰度映射 + 拼串|`getGrayUcIds(...)` + `StringUtils.join(..., ",")`（行 106）|把业务 UCID 映射成实际推送账号（B/C 端），逗号分隔写入 `to_ucids`|
|⑥|清空渠道 + 序列化消息体|`param.setChannel(null)`（行 108）、`JSON.toJSONString(param)`（行 110）|渠道已用于选配置，不必进报文；业务消息体作为 `msg_payload`|
|⑦|异步发送|`sendPushGroupMsg(groupSendDto, config)`（行 112→120）|真正把请求投递到 IM（异步线程池）|

## 3.3 执行顺序

## 3.4 分支逻辑（第 1 层内部）

|   |   |   |
|---|---|---|
|条件|业务含义|处理|
|`ucIdList` 为空|没有接收人，推送无意义|`checkArgument` 抛 `IllegalArgumentException`|
|`ucIdList.size() > 100`|IM 群发单次上限 100 人|抛 `IllegalArgumentException`（超量拒绝）|
|`param == null`|消息体缺失|抛 `IllegalArgumentException`|
|`param.getChannel()` 为空|老调用方未指定渠道（兼容逻辑）|`getConfig` 内兜底为 `RENWUTIXING`（任务提醒渠道）|
|`param.getChannel()` 非空|明确渠道（如 GONGZUOZHUSHOU）|按枚举 code 精确匹配配置|

---

# 四、第 2 层：getConfig(String channel)

## 4.1 这一层负责什么

> 这一层负责**根据业务指定的渠道 code，从 Spring 注入的 `configPropertiesMap` 中找出对应的 IM 服务配置（appId / passcode / url / 协议版本等）**；同时承担"老逻辑兼容"——渠道为空时默认任务提醒渠道。

## 4.2 这一层做了哪几件事情

|   |   |   |   |
|---|---|---|---|
|序号|事情|负责函数|业务目的|
|①|渠道空兜底|行 77–79|未指定渠道 → 默认 `RENWUTIXING`（任务提醒），兼容历史调用方|
|②|从配置 Map 流查找|`configPropertiesMap.values().stream().filter(...).findFirst()`（行 82–83）|按 `channel` 字段精确匹配配置 bean|
|③|取出配置|`optional.get()`（行 85）|返回匹配的配置对象|

## 4.3 配置来源（DB/配置中心链路）

`configPropertiesMap` 是 Spring 容器按类型注入的 **`Map<String, PushConfigProperties>`**，key 为 bean 名，value 为实现类。当前有两个实现类，均被 `@EnableConfigurationProperties` 启用：

|   |   |   |   |
|---|---|---|---|
|实现类|`@ConfigurationProperties` 前缀|`getChannel()` 返回|对应业务渠道|
|`PushMaterialTaskCenterProperties`|`push.material-task-center.property`|`RENWUTIXING`|任务提醒（App 内任务中心）|
|`PushMaterialOrderNoticeProperties`|`push.material-order-notice.property`|`GONGZUOZHUSHOU`|工作助手（订单/工单通知）|

> 两个实现类除了 `getChannel()` 硬编码不同（分别对应 `PushChannelEnum.RENWUTIXING` / `GONGZUOZHUSHOU`），其余字段（`appid`/`passcode`/`url`/`im_protocal_version`/`from_ucid`/`msg_type`/`push_option`/`push_content`）全部来自 Apollo 配置中心对应前缀。**短信渠道 `NOTICE` 没有对应 PushConfigProperties 实现**（走 `sendPushNotice` 短信通道，不进本方法）。

## 4.4 分支逻辑

|   |   |   |
|---|---|---|
|条件|业务含义|处理|
|channel 为空|历史调用方未传渠道|兜底 `RENWUTIXING`|
|channel = RENWUTIXING|任务提醒|命中 `PushMaterialTaskCenterProperties`|
|channel = GONGZUOZHUSHOU|工作助手|命中 `PushMaterialOrderNoticeProperties`|
|channel = NOTICE（短信）|短信渠道|⚠️ 无匹配配置，`optional.get()` 抛 `NoSuchElementException`（见第十三节）|
|channel 为其他任意值|无效渠道|同上，`findFirst` 为空 → `optional.get()` 抛异常|

---

# 五、第 3 层：getGrayUcIds(List ucIdSet)

## 5.1 这一层负责什么

> 这一层负责**接收人账号的"灰度/映射"**：把业务侧传入的 UCID（可能是 B 端工长/管家账号），通过 Ceres 人事服务查询其对应的 **B 端 UCID（`bUcId`）和 C 端 UCID（`cUcId`）**，并把两者合并后返回，作为最终推送给 IM 的接收人集合。

## 5.2 业务含义（隐含规则）

链家体系内一个自然人可能同时有 **B 端账号（bUcId，作业 App）** 和 **C 端账号（cUcId，用户 App）**。推送时把两个账号都带上，确保"无论接收人用哪个 App 登录都能收到 Push"。这是隐藏在代码里的关键业务规则：**推送账号不是 1:1，而是 1:N 展开**。

## 5.3 这一层做了哪几件事情

|   |   |   |   |
|---|---|---|---|
|序号|事情|负责函数/API|业务目的|
|①|入参空判断|行 169|空集合直接返回空列表（不查 RPC）|
|②|字符串 UCID → Long|`stream().filter(isNumeric).map(Long::parseLong)`（行 172）|IM 只认数字账号；非数字被过滤丢弃|
|③|调 Ceres 人事服务查映射|`personHighServiceApi.queryUcIdMapping(ucIdList)`（行 173）|远程查询 B/C 端账号映射|
|④|RPC 失败兜底|行 174–177|失败或非空则**原样返回入参 ucIdSet**（不展开，但至少还能推到原账号）|
|⑤|提取 bUcId + cUcId 合并|行 178–182|把 bUcId 集合与（非空的）cUcId 集合合并去重返回|

## 5.4 分支逻辑

|   |   |   |   |
|---|---|---|---|
|条件|业务含义|处理|最终结果|
|ucIdSet 空|无接收人|返回空 List|上游 `to_ucids` 拼为空串 → 第 4 层拦截 return|
|含非数字 UCID|脏数据/特殊账号|`isNumeric` 过滤掉|非数字账号不进 IM 请求|
|RPC 失败 / data 空|人事服务不可用或查不到|**返回原始 ucIdSet**（兜底，不展开）|仍推原账号（可能漏推 C 端）|
|RPC 成功且有映射|正常|合并 bUcId + cUcId 返回|两账号都推，覆盖率最高|

## 5.5 远程调用展开（RPC）

---

# 六、第 4 层：sendPushGroupMsg(PushGroupSendParam, PushConfigProperties)

> 这是**真正的发送执行层**，但注意它先有"黑名单/空值拦截"，再异步发 HTTP。

## 6.1 这一层负责什么

> 这一层负责**最终把请求投递给链家 IM 服务，但投递前先做"接收人黑名单/空值拦截"（避免给系统账号/空账号发 Push 导致 IM 报错），投递本身放到独立线程池异步执行，并对 IM 返回 errno 与本地异常做统一异常转换（统一抛 `UtopiaBussinessException(ERROR_INVOKE)`）。**

## 6.2 这一层做了哪几件事情

|   |   |   |   |
|---|---|---|---|
|序号|事情|负责函数/API|业务目的|
|①|接收人黑名单/空值拦截|行 123–128|拦截空、`"0"`、`"1000000001"`、`"1000000003"` 等系统/无效账号，直接 return 不发|
|②|异步投递|`CompletableFuture.runAsync(() -> pushHttpUtil.postJson(...), msgThreadPoolExecutor)`（行 129）|不阻塞业务线程，丢到消息线程池|
|③|解析 IM 返回|`JSON.parseObject(responseMsg, PushDTO.class)`（行 133）|解析 errno/error|
|④|errno 非 0 抛业务异常|行 134–137|IM 侧业务失败 → `UtopiaBussinessException(ERROR_INVOKE, errorMsg)`|
|⑤|异步异常兜底|`.exceptionally(...)`（行 139–142）|异步任务抛任何异常 → 记 error 日志 + 抛 `UtopiaBussinessException(ERROR_INVOKE)`|
|⑥|同步异常兜底|`catch (Exception)`（行 143–146）|线程提交/参数构造阶段异常 → 记日志 + 抛同款异常|

## 6.3 分支逻辑（第 4 层）

|   |   |   |
|---|---|---|
|条件|业务含义|处理|
|`to_ucids` 空串|没有任何有效接收人（上游灰度返回空）|return，不发 Push|
|`to_ucids == "0"`|系统占位账号（全量/无效）|return，不发|
|`to_ucids == "1000000001"`|特定系统账号 A|return，不发|
|`to_ucids == "1000000003"`|特定系统账号 B|return，不发|
|正常账号串|真实接收人|进入异步发送|
|IM 返回 errno != 0|IM 侧拒绝（如账号非法/限流）|抛 `UtopiaBussinessException(ERROR_INVOKE)`|
|HTTP/解析异常|网络或序列化失败|`exceptionally` / catch 兜底抛同款异常|

## 6.4 异步线程池（并发/资源）

> ⚠️ **潜在风险（基于代码推断）**：`AbortPolicy` 在队列满时直接抛异常，会被 `catch` 捕获后转成 `UtopiaBussinessException(ERROR_INVOKE)` 向上抛——意味着**推送高峰期可能把"推送失败"异常冒泡到 `handleNode` 的事务内**，导致主流程回滚（除非上游 MessagePushClient 自己 catch 了，见十一节）。

---

# 七、第 5 层：pushHttpUtil.postJson(url, paramMap, config)

## 7.1 这一层负责什么

> 这一层负责**用 OkHttp 以 `application/x-www-form-urlencoded` 表单方式，把群发参数 POST 到链家 IM 服务，并在 Header 中带上 IM 协议版本、AppId、Passcode 鉴权信息；HTTP 失败或 IM 非 2xx 统一抛 RuntimeException。**

## 7.2 这一层做了哪几件事情

|   |   |   |   |
|---|---|---|---|
|序号|事情|负责函数/API|业务目的|
|①|参数 Map → 表单|`FormBody.Builder` 遍历 `paramMap`（行 34–38）|把对象字段拍平成表单键值对（null → 空串）|
|②|构造请求头|`getHttpHeaders(config)`（行 73–81）|注入 `Lianjia-Im-Protocal-Version` / `Lianjia-App-Id` / `Lianjia-Im-Passcode` 鉴权头|
|③|同步 HTTP POST|`syncCall(request)`（行 40→58）|OkHttp 5s 连接/读/写超时|
|④|响应处理|`response.isSuccessful()`（行 41）|2xx → 返回 body 字符串；否则抛 RuntimeException|

## 7.3 HTTP / RPC 链路

## 7.4 隐含规则

- `objectToMap(pushGroupSendParam)`（`ObjectUtils`）把第 1 层 copyProperties 进来的所有配置字段 + `to_ucids` + `msg_payload` 拍平成 Map，再逐个 `value.toString()`。

- `null` 值被转成空串 `""`（行 37），避免表单 NPE，但也会让 IM 收到空字段。

---

# 八、完整调用树

---

# 九、完整业务流程（业务视角）

---

# 十、完整业务分支矩阵

|   |   |   |   |
|---|---|---|---|
|场景|判断条件|处理逻辑|最终结果|
|无接收人|ucIdList 空|checkArgument 抛 IllegalArgumentException|调用方收到参数异常|
|接收人超量|size > 100|checkArgument 抛 IllegalArgumentException|调用方收到参数异常|
|消息体空|param == null|checkArgument 抛 IllegalArgumentException|调用方收到参数异常|
|渠道未指定|channel 空|getConfig 兜底 RENWUTIXING|走任务提醒配置|
|任务提醒渠道|channel=RENWUTIXING|命中 TaskCenter 配置|正常发 IM 任务中心|
|工作助手渠道|channel=GONGZUOZHUSHOU|命中 OrderNotice 配置|正常发 IM 工作助手|
|短信渠道|channel=NOTICE|⚠️ 无 PushConfigProperties 实现|optional.get() 抛 NoSuchElementException|
|未知渠道|其他任意值|无匹配配置|optional.get() 抛 NoSuchElementException|
|灰度 RPC 失败|queryUcIdMapping 失败/空|返回原始 ucIdSet 兜底|仍推原账号（可能漏 C 端）|
|含非数字 UCID|isNumeric=false|过滤丢弃|该账号不进 IM 请求|
|接收人为系统账号|to_ucids ∈ {"", "0", "1000000001", "1000000003"}|sendPushGroupMsg 拦截 return|不发 Push|
|IM 返回成功|errno == 0|仅记 info 日志|推送成功|
|IM 返回失败|errno != 0|抛 UtopiaBussinessException(ERROR_INVOKE)|推送失败冒泡|
|HTTP/解析异常|IOException / 非 2xx|exceptionally/catch 转同款异常|推送失败冒泡|
|线程池满|AbortPolicy 触发|RejectedExecutionException → catch 转异常|推送失败冒泡|

---

# 十一、核心数据流

---

# 十二、状态流转 / 状态字段

`sendPushMsg` 本身是**无状态**的通道调用，没有业务状态机字段。它只关心"请求是否成功投递给 IM"：

> 注意：消息"是否已读 / 是否点击"等状态由 IM 服务端维护，**本函数不感知、不落库、不回查**。

---

# 十三、DB / Redis / MQ / RPC 链路

|   |   |   |
|---|---|---|
|类型|是否涉及|说明|
|DB|否|`PushClient` 不查任何数据库（接收人/消息体全由入参传入）|
|Redis|否|无缓存读写|
|MQ/Kafka|否|不走消息队列，直接同步 HTTP 到 IM|
|RPC-Ceres|是|`personHighServiceApi.queryUcIdMapping`：UCID → B/C 端账号映射；失败兜底返回原值|
|RPC-链家 IM|是|`pushHttpUtil.postJson`：OkHttp POST 表单；5s 超时；无重试|

**上游（MessagePushClient，service 层）**才涉及 DB/Redis/Kafka，典型如：

- `pushMessageWhenTaskDispatchNodeProcessChange`（行 634）：先 `if (PushChannelEnum.supportMessage(channel)) sendPushMsg(...)`，再 `if (supportNotice(channel)) sendPushNotice(...)`——**同一消息可同时走 IM + 短信双通道**。

- 该方法整体包在 `try/catch` 内（行 647–650），**推送异常被吞掉只记日志**，不会冒泡到 handleNode 事务（这是与第六节"潜在风险"对应的实际保护机制——但仅限该调用点；其他未 try-catch 的 sendPushMsg 调用点仍可能冒泡）。

---

# 十四、隐含业务规则

1. **渠道空兜底**：历史调用方不传 channel 时一律按"任务提醒（RENWUTIXING）"发送（兼容老逻辑，代码注释明确）。

2. **接收人 1:N 展开**：一个业务 UCID 经 Ceres 映射为 B 端 + C 端两个账号，保证多端触达（代码无注释，实际存在）。

3. **系统账号黑名单**：`"0"`、`"1000000001"`、`"1000000003"` 不发 Push（避免给系统/全员账号误推，IM 传 0 会报"接收用户类型非法"）。

4. **非空拦截**：`to_ucids` 为空串直接 return，不发（等价于无接收人）。

5. **非数字 UCID 过滤**：灰度映射阶段 `isNumeric` 过滤，非数字账号不进 IM 请求。

6. **灰度 RPC 失败兜底原账号**：Ceres 不可用时返回原始 ucIdSet，至少还能推到原账号（牺牲 C 端覆盖换可用性）。

7. **渠道字段不进报文**：`param.setChannel(null)` 在选完配置后清空，避免渠道 code 串进 `msg_payload`。

8. **短信与 IM 分治**：`NOTICE` 渠道走 `sendPushNotice`（SmsService），**不经过本 sendPushMsg 的 configPropertiesMap**，故本方法对 NOTICE 会找不到配置而抛异常——调用方必须用 `PushChannelEnum.supportMessage/supportNotice` 正确分流（见 MessagePushClient 行 633/638）。

9. **异步即发即忘（fire-and-forget）**：投递在独立线程池，业务线程不等待 IM 结果；IM 成功/失败仅在第 4 层内部记日志/抛异常，不返回给 sendPushMsg 调用方成功/失败标志（返回类型是 `void`）。

---

# 十五、异常、兜底与边界情况

|   |   |   |   |
|---|---|---|---|
|项|触发条件|业务含义|影响|
|`IllegalArgumentException`|入参校验失败（空/超量/空消息）|调用方使用错误|同步抛出，调用方负责|
|`NoSuchElementException`|channel=NOTICE 或未知渠道，optional.get() 空|配置缺失 / 渠道分流错误|⚠️【代码明确表现】本方法未捕获，会向上冒泡|
|灰度 RPC 失败兜底|Ceres 不可用|人事服务降级|仅推原账号，C 端漏推（不抛异常）|
|IM errno!=0|IM 侧拒绝|账号非法/限流等|抛 `UtopiaBussinessException(ERROR_INVOKE)`|
|HTTP 异常|网络/超时（5s）|IM 不可达|转同款业务异常|
|线程池拒绝|队列满 + 线程满|推送洪峰|`AbortPolicy` → 异常冒泡|
|上游吞异常|MessagePushClient 多调用点 try/catch|推送非核心链路|推送失败不影响主业务（节点已完成）|
|异步不等待|void 返回|无法感知 IM 结果|调用方拿不到"是否真的发出去"|

⚠️ **潜在风险（基于代码推断）**：

- `NOTICE`/未知渠道经 `sendPushMsg` 会 `NoSuchElementException`；依赖上游用 `supportMessage` 正确分流，若上游误传则崩溃。

- 未被 try-catch 包裹的 `sendPushMsg` 调用点（如行 166/214/269 等部分路径），若遇线程池拒绝或 IM 异常，异常会冒泡到 `handleNode` 的 `@Transactional` 内导致**整个节点提交流程回滚**——主业务与推送耦合风险。

---

# 十六、最终业务结论

> **`PushClient#sendPushMsg` 在整个系统里是"主材任务消息触达的最后一公里"**：它把上游业务事件（节点完成/激活/延期/复尺打回/定时提醒）转换成的"接收人 + 消息体 + 渠道"，通过**接收人灰度展开（B/C 端双账号）+ 渠道配置注入 + 系统账号黑名单拦截 + 异步 OkHttp 投递链家 IM** 完成 App 内 Push 触达。

整条链路最核心的业务规则（5～10 条）：

1. **渠道驱动配置**：`RENWUTIXING`（任务提醒）/ `GONGZUOZHUSHOU`（工作助手）二选一，默认任务提醒；配置来自 Apollo，区分 appId/passcode/url。

2. **接收人 1:N 展开**：业务 UCID 经 Ceres 映射成 B 端 + C 端账号，确保多端触达；RPC 失败兜底原账号。

3. **系统账号黑名单**：`0` / `1000000001` / `1000000003` / 空串不发，避免误推系统账号。

4. **异步即发即忘**：投递在独立线程池，业务线程不阻塞、不等待结果；返回 void，调用方无法感知 IM 实际成败。

5. **失败统一转 `UtopiaBussinessException(ERROR_INVOKE)`**：IM errno≠0、HTTP 异常、线程池拒绝都归一化，由上游决定是否吞掉（多数调用点 try-catch 吞掉，少数未包裹有回滚风险）。

6. **IM 与短信分治**：`NOTICE` 渠道走 `sendPushNotice`（SmsService），不经本方法；本方法对短信渠道会配置缺失崩溃——必须上游用 `supportMessage/supportNotice` 分流。

7. **无状态通道**：本方法不查 DB、不写 Redis、不走 MQ，纯"入参 → 外部 RPC"的集成适配器，状态完全由上游（MessagePushClient）准备。

8. **强约束入参**：接收人非空且 ≤100，超限直接拒绝（IM 群发上限）。