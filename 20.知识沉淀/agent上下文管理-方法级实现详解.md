# DeepHarness 上下文管理 · 方法级实现详解

> 源码来源：`github.com/deepseek-ai/deepseek-harness`（MIT，master 分支）。
> 对照对象：Claude Code（逆向重建，无逐行可验证源码，仅作对照）。
> 配套笔记：`agent上下文管理-claude-code与deepharness.md`（架构总览）。

本文按包（package）逐方法拆解。核心前提：**事件溯源（event sourcing）**——`Session` 是 append-only 日志（raw log），模型实际看到的是 `surface`（投影）。上下文管理 = 在不改日志的前提下改写 surface 投影。

---

## 0. 全局不变量（理解一切的前提）

| 概念 | 含义 |
|---|---|
| `Session` | append-only 事件日志，每个事件有连续 `seq` |
| `surface` | 模型可见的消息投影，由 `user/message`/`assistant/message`/`tool/result` 等"消息事件"构成；`chunk`、usage、hook 只在 raw log |
| `surface.nodes` | 当前 surface 各节点的 **log seq 数组**（顺序是"位置"，不是数值大小） |
| `replaceGeneration` | surface 被改写（replace）的次数；压缩/裁剪后 +1 |
| `surfaceOp: {op:'replace', startSeq, endSeq}` | 一条事件如何改写 surface 的契约：把 `[startSeq,endSeq]` 这段替换成自己 |
| 阴影（shadowed） | 被替换掉的原始节点不删，只是不进 surface，可恢复 |

**压缩的本质 = 追加几条 log-only 事件（锁+摘要）+ 一条带 `surfaceOp.replace` 的 `user/message` 把原区间从 surface 抹掉。日志永远增长，surface 收缩。**

---

## 1. `@deepseek-ai/dsh-compaction`（压缩抽象层）

### 1.1 `types.ts` — 词汇与结果类型

**`SessionEventMap` 声明合并**（关键：这些事件是 log-only，不进 surface）：

- `compaction/start { compactionId, sourceCommandId?, turn: number|null }`：压缩事务起点，持锁直到 `compaction/end`。`turn` 为 `null` 表示 turn 之间的独立手动事务。
- `compaction/summary { compactionId, summary, shadowedRange, shadowedSeqs, shadowedTokenCount, provider, model, ... }`：完成的摘要，**不带 surfaceOp**。真正改写 surface 的是紧随其后的 `user/message`。
- `compaction/end { compactionId, turn, error? }`：释放锁。
- `compaction/prune { shadowedRange, shadowedSeqs, shadowedTokenCount }`：无模型剪枝（pruner）的"阴影计价"事件，与紧随其后的 replace 同步相邻。

**`CompactionResult`**（一次成功压缩的返回值）：`compactionId / startSeq / summarySeq / endSeq / summary / shadowedRange{start,end} / shadowedSeqs[] / shadowedTokenCount`。
注意 `shadowedRange` 是**位置区间**而非数值区间——因为新摘要节点 seq 更高但位置更早，`start` 可能 > `end`。

### 1.2 `checkpoint.ts` — 摘要节点身份

```ts
const COMPACT_CHECKPOINT_MARKER = Object.freeze({ kind:'plugin', plugin:'compact' })
export function compactCheckpointSource(id, cmd?) { /* 冻结 {marker, compactionId, sourceCommandId?} */ }
export function isCompactCheckpointSource(source) {
  return source.kind === 'plugin' && source.plugin === COMPACT_CHECKPOINT_MARKER.plugin
}
```
- **逻辑**：用一个后端无关的固定 marker 标记"这是压缩摘要"，让消费方（invariant、回放）能独立识别，无需加载宿主插件。
- **实现要点**：`Object.freeze` 保证不可变；`sourceCommandId` 可选，用于把手动压缩关联到发起命令。

### 1.3 `tool-pairing.ts` — 工具调用配对平衡检查

压缩不能把"一次工具调用 + 它的结果"从中间切开。这个模块判断某个 surface 切割点是否"平衡"（没有未闭合的 tool-call 跨过）。

```ts
function eventDelta(event): number {
  // assistant/message → +其内 tool-call 数量；tool/result → -1；其他 → 0
}
```

- **`extendCache(session, cache, seqs)`**：把 cache 未覆盖的 surface 尾部 fold 进来，维护 `inProgressToolCalls`（进行中的工具调用数）。**先校验再改 cache**——遇到 `tool/result` 但 `inProgressToolCalls<0` 直接抛"corrupt surface"，避免半更新状态。
- **`balanceCache(session)`**：带 `WeakMap<Session, BalanceCache>` 缓存；按 `surface.replaceGeneration` 判断缓存是否失效，失效则从头重建。返回每个切割点的平衡布尔数组（N 个节点有 N+1 个切割点）。
- **`cutBalance(cache, seq, offset)`**：取 `seq` 前后切割点的平衡性。
- **`toolPairingBalancedBefore/After(session, seq)`**：导出给 region 用的两个判断——切割点前/后是否平衡。**实现要点**：基于"当前 surface 顺序"而非 step marker 推导，因为压缩会移动位置。

### 1.4 `invariant.ts` — 压缩日志流不变量

这是"事故防止网"。它在 `session/event`（pre-commit 通过 `internal/dispatch` 预校验）上安装检查器，保证压缩事务的日志永远合法：

- **`validateCompactionEvent`**：对 `compaction/start|summary|end|prune` 和摘要 checkpoint 逐一校验：
  - `start` 时不能有未闭合的压缩（`open !== undefined` 就 fail）；
  - `summary` 的 `compactionId` 必须匹配 `start`，且 `open.summarized` 不能为 true（防重复摘要）；
  - `end` 必须匹配 `start`，成功结束（`error` 未定义）必须有且仅有一个 `summary`；
  - 校验 `shadowedSeqs` 必须精确等于当前 surface 对应区间的每个节点（漏一个都不行）。
- **`validateOwner(owner, openTurn, type)`**：编号事务必须包在精确那个 turn 内；`null` 事务必须在无 open turn 时（turn 之间）。
- **`validateTurnBoundary`**：`turn/start|end` 不能跨过未闭合的压缩括号。
- **`inheritedOrphanStartSeqs`**：处理"跨会话种子（session/end-seed）让旧的 start 变陈旧"——若 start 之后遇到 end-seed，该 start 被记为 stale，不算未闭合锁。
- **`apply`**：`ctx.invariants.register(...)`，对现有 session 回放 + 监听未来事件。

### 1.5 `index.ts` — `CompactionEngine`（抽象基类）

```ts
abstract class CompactionEngine extends Service {
  abstract compactIfNeeded(agent, trigger, signal): Promise<CompactionResult|null>
  abstract compactNow(agent, signal, sourceCommandId?): Promise<CompactionResult|null>
  abstract compactRegion(start, end, agent, signal?): Promise<CompactionResult>
}
```

- **`compactIfNeeded`**：对一次显式触发考虑自动压缩。`trigger` 为 `'pressure'`（token 超阈值）或 `'context-overflow'`（服务端窗口溢出）。返回 `null` 表示无需压缩。
- **`compactNow`**：显式手动压缩（即使低于阈值），要求 agent 空闲，先写独立的 `compaction/start` 锁。
- **`compactRegion(start, end)`**：强制压缩一段 surface 为单个摘要节点。start/end 是**位置**（surface 顺序）而非数值；两端必须 `toolPairingBalanced`；拒绝 active / missing / reversed / unbalanced 区间。
- **`ManualCompactionError`**：6 类预期失败码 `busy / cancelled / changed / summary / commit / persistence`。

---

## 2. `@deepseek-ai/dsh-compaction-basic`（具体后端）

### 2.1 触发器解析（私有辅助）

- **`routedTarget(session)`**：从 `session.requestHeader().config` 取"最近一次持久化路由请求"的 `{provider, model}`；为空返回 `undefined`。用于"压力策略按真实路由模型计价"。
- **`conversationTarget(agent)`**：优先 `routedTarget` → 否则 `agent.options` 里的 provider/model。

### 2.2 `_registerAutomaticCompaction`（自动压缩注册）

构造函数里 `if (this.config.auto) this._registerAutomaticCompaction()`。它挂三个监听：

1. **`agent/pre-step`**：每步前若未 abort，调 `compactIfNeeded(agent,'pressure')`；失败只 warn 并继续（"压缩失败不让回合崩"）。遇到 `TargetPressureConfigError` 用 `warnedPressureConfigTargets` 去重告警。
2. **`agent/status` idle**：清掉该 agent 的 overflow 重试计数。
3. **`agent/request-error`**：若失败码是 `CONTEXT_WINDOW_EXCEEDED_CODE`（上下文溢出），且重试未超 `maxOverflowRetries`，调 `compactIfNeeded(agent,'context-overflow')`，返回 `{kind:'retry'}`。**关键点**：溢出时即使工具调用还在继续同一个回合也强制一次有用压缩；若压缩中途第二部分失败但第一部分（无模型剪枝）已持久化，仍算有效重试证明。

### 2.3 `summarize`（唯一子类定制钩子）

```ts
protected async summarize(input, agent, signal?) {
  const target = conversationTarget(agent)
  const config = target === undefined ? this.config : resolveTargetPolicy(this.config, target)
  return summarizeWithLlm(this.ctx, config, input, agent, signal)
}
```
- **逻辑**：后端把"总结"做成唯一可覆写的钩子；重放与持久化策略固定，保证所有计价都走同一个 `ctx.tokenMeter` 单例。
- **实现要点**：总结调用的 system prompt / tools / 前缀消息**复用对话自身**，使这次辅助调用成为"最后一次路由请求的真正前缀"，从而复用 provider 的 KV cache（不失效）。

### 2.4 `compactIfNeeded`（override）— 自动压缩主决策

```
1. routedTarget 为空 → return null
2. policy = resolveTargetPolicy(config, target)
3. meter = ctx.tokenMeter; measurement = meter.measure(session)
4. trigger === 'context-overflow':
     - 若 pruner 存在 → pruneSession 后重测
     - selectCompactableRange(..., retainTokens=0) → 强制选一段 → compactRegion
5. trigger === 'pressure':
     - 解析模型 contextWindow；无则抛 TargetPressureConfigError
     - spec = resolveCompactSpec(policy, contextWindow)
     - totalTokens < threshold → null
     - 先跑模型无关 prune，再测一次；仍低于阈值 → null
     - for attempt in 0..compactionRetries:
         range = selectCompactableRange(..., retainTokens)
         null →（已无安全段）return null 或跳出
         result = compactRegion(range)
         重测；低于阈值 → return result
     - 超过重试仍超阈值 → throw
```
**核心经济学**：先免费剪枝（prune），不够才上 LLM 摘要。这就是"懒惰降级"。

### 2.5 `compactRegion`（override）

直接委托给 `compactSurfaceRegion(this.regionDependencies(), session, start, end, agent, {owner:'current-turn', stability:'whole-surface'}, signal)`。

### 2.6 `compactNow`（override）— 手动压缩

```
signal.throwIfAborted()
agent.runMaintenance(async agentSignal => {     // 只在 agent 空闲时跑
  operationSignal = AbortSignal.any([agentSignal, signal])
  range = selectCompactableRange(session, meter.measure, 0)
  if null → return null
  compactSurfaceRegion(..., {owner:null, stability:'selected-span', flush: ()=>sessions.flush(session)})
})  // 若 agent 不空闲 → 抛 ManualCompactionError('busy')
```
- 手动事务 `owner:null` → 独立括号；`stability:'selected-span'` → 只要求被选区间稳定，区间外新增消息不失效；`flush` 在摘要写完后持久化检查点。

### 2.7 `regionDependencies` — 依赖注入闭包

把 `meter`、`summarize`（绑定本实例钩子）、`recover`（瀑布式 `compaction/summary-error` 恢复）打包给 region 函数，使 region.ts 保持 cordis-free、可被客户端/线程序序引用。

---

## 3. `compaction-basic/src/region.ts`（事务与选段核心）

### 3.1 `selectCompactableRange(session, measurement, retainTokens)`

```
1. pricedNodes = measurement.nodes；surfaceNodes = session.surface.nodes
2. 两者长度/seq 必须一致，否则抛 "token-meter surface does not match"（一致性闸门）
3. firstIdx = systemHead(node0) 不存在 ? 0 : 1   // system/message 永不进压缩区间
4. 从尾向前累加 tokens，直到累计 >= retainTokens → keepFromIdx
5. 若 keepFromIdx<=firstIdx → null（没有可压的）
6. 向前回溯：若 keepFromIdx 处 toolPairingBalancedBefore 不满足 → keepFromIdx--
   直到平衡（不在工具调用中间切）
7. return {start: surfaceNodes[firstIdx], end: surfaceNodes[keepFromIdx-1]}
```
**逻辑**：保留最近 `retainTokens` 的尾部原文，把更早的压成摘要；且绝不把"工具调用/结果对"切开。

### 3.2 `compactSurfaceRegion`（单事务编排）

```
1. assertStable 选择：whole-surface 或 selected-span
2. selection = validateSurfaceRegion(session, start, end)
3. entryState = inspectCompactionEntryState(session)
4. assertCompactionInactive(...)   // 锁检查
5. 决定 owner（current-turn 需 openTurn；null 需无 openTurn）
6. compactionId = randomUUID()
7. startEvent = session.append('compaction/start', lifecycle)   // 锁在此刻生效
8. try {
     prepared = prepareCompaction(...)
     summarized = await summarizeCompaction(...)   // 异步总结
     assertStable(...)                              // 总结期间 surface 是否仍稳定
     pending = commitCompactionBody(...)            // 写 summary + replace user/message
     endEvent = session.append('compaction/end', lifecycle)
     result = completeCompaction(pending, endEvent)
   } catch {
     若未 closing → 补一个带 error 的 compaction/end（保证锁一定释放）
   }
9. flush（手动事务的持久化检查点）
10. 失败分类抛 ManualCompactionError
```
**实现要点（硬核锁）**：`compaction/start`（同步）与总结（异步）之间，其他消息可能插入，但只有被选区间必须稳定。每个失败都恰好产生一个 `compaction/end`；若崩溃留下"有 start 无 end"，`assertCompactionInactive` 会阻塞所有入口 → 不会有两个压缩并发操作同一会话。

### 3.3 `validateSurfaceRegion` — 只读校验

- `startIdx/endIdx = nodes.indexOf(start/end)`；任一找不到 → 抛。
- `startIdx > endIdx` → 抛"位置反转"。
- `toolPairingBalancedBefore(start)` 与 `toolPairingBalancedAfter(end)` 必须都为 true，否则抛"会切开工具调用对"。
- 返回 `{start, end, startIdx, endIdx, shadowedSeqs}`。

### 3.4 `prepareCompaction` — 计价与重放输入

- 重新 `meter.measure(session)`，切片被选节点；与 selection 比对，不一致 → `SurfaceChangedError`。
- 计算 `shadowedTokenCount`（fixed-heuristic 计价，用于 O(1) 投影折叠自洽）与 `shadowedRouteTokenCount`（路由计价 tokens，用于真实压力比较）。
- `input = buildSummarizationInput(...)`。

### 3.5 `summarizeCompaction` — 总结+收敛校验

```
for(;;){
  try { summaryResult = await dependencies.summarize(input, agent, signal); break }
  catch {
    if aborted → throw
    assertStable(...)                       // 表面是否仍稳定
    if !recover(error,...) → throw          // 瀑布恢复，决策持久化
    prepared = prepareCompaction(validateSurfaceRegion(...))  // 重选并重测
  }
}
checkpointMessage = createUserMessage({content: frameSummary(summary), source: compactCheckpointSource(id)})
framedTokens = meter.estimateMessage(checkpointMessage)
if framedTokens >= shadowedRouteTokenCount → throw "summary not smaller"   // 摘要必须更小，否则无意义
```
**逻辑**：摘要若比原文还大，直接拒绝（fail-closed）。

### 3.6 `assertWholeSurfaceUnchanged` vs `assertSelectedSpanStable`

- **whole-surface**（自动，current-turn）：总结期间整个 surface 节点序列必须 `isDeepStrictEqual` 不变。
- **selected-span**（手动）：只要求被选区间仍是合法、连续、同价、平衡的目标；区间外新增消息保持可见、不失效。

### 3.7 `commitCompactionBody` — 落盘摘要+替换

```
summaryEvent = session.append('compaction/summary', {compactionId, summary, shadowedRange, shadowedSeqs, shadowedTokenCount, provider, model, ...})
session.append('user/message', checkpointMessage, {
  surfaceOp: {op:'replace', startSeq:start, endSeq:end},
  sourceEventSeqs: [startEvent.seq, summaryEvent.seq, ...shadowedSeqs]
})
```
**关键契约**：`compaction/summary`（log-only）**紧邻**带 `surfaceOp.replace` 的 `user/message`。消费方用"replace 事件前一条的阴影计价事件"配对，无需保存每节点状态。

### 3.8 `buildSummarizationInput` — 复用前缀缓存

构造重放前缀：system head（node0 的 `system/message`）+ header 的 tools + 被选区间的派生消息（surface 顺序）。**末尾才追加压缩指令**，于是这次调用是上一次请求的真正前缀 → 复用 KV cache。

### 3.9 `inspectCompactionEntryState` — 反向扫描锁状态

从 `seq-1` 倒扫，找最近的 `compaction/start`（无匹配 end 即未闭合锁）、`turn/start|end`（openTurn）、`session/end-seed`（最新种子边界）。三者都找到即停。

### 3.10 `systemHead`, `assertCompactionInactive`, `assertNoActiveCompaction`, `throwManualFailure`

- `systemHead`：surface node0 是否为 `system/message`，是则返回它（永不进压缩区间）。
- `assertCompactionInactive`：有未闭合 `compaction/start` 且其后无 `end-seed` 更新 → 抛 `busy`。
- `assertNoActiveCompaction`：异步策略决策后复查锁。
- `throwManualFailure`：把失败分类为 `commit`（未干净提交）/ `changed`（surface 变了）/ `summary`（没产出更小摘要）。

---

## 4. `compaction-basic/src/summarizer.ts`（总结实现）

- **`COMPACTION_INSTRUCTION`**：以**最后一条 user 消息**形式追加的压缩指令（不是独立 system prompt），要求模型输出固定 Markdown 结构（Primary Request / Key Technical Concepts / Files and Code / Errors and Fixes / Pending Jobs / Current Work / Next Step / Critical Context）。这样辅助调用是前缀、复用 KV cache。
- **`CHECKPOINT_PREAMBLE`**：告诉模型"这是自动生成的检查点，把捕获内容当既定背景，直接继续，不要提压缩"。
- **`summarizeWithLlm`**：解析 target（config > 最近路由 > agent options）；用 `BlockAssembler` 流式收集；`finishError` 把截断/中止映射为 fail-closed 错误；`summaryText` 拒绝图像输出（只留文本）；返回 `{summary, rawOutput, llmStreamCall:true, provider, model, usage?}`。
- **`frameSummary(summary)`**：把摘要包进 `<compacted-summary>` 标签 + preamble，作为替换 `user/message` 的内容。
- **`finishError` / `summaryText`**：终止态→错误映射；图像→`UNSUPPORTED_CONTENT` 错误。

---

## 5. `@deepseek-ai/dsh-compaction-tool-result-pruner`（无模型剪枝）

**设计哲学**：比 LLM 摘要更便宜、确定性的压缩——把超大工具结果的中间裁掉，保留 head/tail。

- **`measureContent(blocks)`**：按 Unicode code point 计文本长度（非文本块计 0）。
- **`pruneContent(blocks)`**：
  ```
  总 chars <= thresholdChars → return null（无需剪）
  removedStart = headChars; removedEnd = total - tailChars
  逐块按 code point 切（用 Array.from 防代理对切断）：
    保留 [0, headEnd) + PRUNE_MARKER + [tailStart, end)
  校验：marker 必须插入；剪后 chars 必须 < 阈值且 < 原文
  ```
- **`pruneSession(session)`**：遍历当前 surface 所有 `tool/result` 节点，超预算的逐个剪：
  ```
  append('compaction/prune', {shadowedRange, shadowedSeqs, shadowedTokenCount: meter.estimateMessage(original)})
  append('tool/result', {...event.data, message: pruned}, {surfaceOp:{op:'replace', startSeq:seq, endSeq:seq}})
  ```
  **阴影计价协议**：计价事件与 replace 同步相邻，纯消费方无需保存每节点价格即可减掉。已提交的替换即使后面失败也保留。

**配置默认值**：`thresholdChars=8192`、`headChars=4096`、`tailChars=1024`（Unicode code points）。

---

## 6. Spill（超大结果外置）

### 6.1 `@deepseek-ai/dsh-spill` — 抽象存储缝

```ts
abstract class SpillStore extends Service {
  abstract saveText(input: SaveTextSpill): Promise<SpillRef>
}
```
- `SaveTextSpill { owner:{sessionId}, source:{kind:'tool'|'session-reference',...}, suggestedName, content }`
- `SpillRef { locator, bytes, retrievalHint }`
- **语义**：`saveText` 原样持久化全文，返回不透明 locator（本地=路径，远程=URI）；存储失败必须 reject，让调用方决定降级（spill-policy 把失败当 best-effort，保留内联结果）。

### 6.2 `@deepseek-ai/dsh-spill-local` — 文件系统实现

- **`saveText`**：调 `saveTextFile`（在 `<root>/session-<hash>/` 下用不可预测名字、独占 0600 写、私有 0700 根）落盘，返回 `{locator: path, bytes, retrievalHint: 'Use read with offset/limit, or grep this path...'}`。
- **安全**：spilled 结果不能被其他本地用户读，也不能被植入的软链重定向。
- **`runCleanup`**：启动时一次 best-effort 清理，删 `mtime` 早于 `cleanupPeriodDays`（默认 30）天的文件；不阻塞服务可用，dispose 时 await 同一 promise 保证静默。

### 6.3 `@deepseek-ai/dsh-spill-policy` — 何时 spill（策略）

`tools/post-execute` 结果转换器（**不注册服务、不管存储、不管预览机制**）：

- `flattenPlainText`：全是文本块才处理，含非文本块则原样返回。
- `preview(text, budget)`：用 `TextRetainer({kind:'headTail', headBytes:budget/2, tailBytes:budget/2})` 生成头尾预览。
- `spillReplacement(...)`：
  ```
  无 session owner / 无 spillStore 后端 / saveText 失败 → return undefined（保留原文，绝不把成功调用变 isError）
  预留 notice 字节（最坏情况 upper bound），previewBudget = max(0, cap-reserve)
  replacedText = previewText + "\n\n" + notice
  if byteLength(replacedText) > cap → return undefined   // 保证替换绝不超 cap
  ```
- **两条臂**：
  1. **模型面**：`tools/post-execute` 监听，预处理结果超 `maxInlineBytes` 则 spill + 头尾预览 + locator（跳过 `read` 避免 read→spill→read 死循环）。
  2. **持久日志面**：`tools/ptc-dispatch-log` 限制 `tool/ptc-dispatch` 事件里超大 `run_code` 子调用结果的日志副本（程序返回值不动）。
- **窄化设计**：`maxInlineBytes` 省略=完全 no-op；配置在加载时校验（负数/小数→部署失败而非每次调用报错）。

---

## 7. `@deepseek-ai/dsh-context/agent-instructions`（注入什么）

决定"往上下文里放什么"——AGENTS.md 工作区指令、跨会话引用、时间上下文等。

- **`compose`**：核心投影函数。计算 `projectRoot`、baseline `identity`；加载基线指令集（带 `maxBytes` 字节预算截断）；与可见 baseline 比对，决定替换/追加/移除；调 `reconcileInstructionContext` 合并作用域变更；返回一条 `user/message`（source.kind='agent-instructions'）。
- **`syncInbox`**：把算出的 `desired` 同步进 agent 的 `inbox.nextStep`——若已存在等价消息则复用，否则替换/前插 pending 第一条，清掉其余。
- **`composeAndSync` / `queueProjection`**：异步投影用 `projectionTails`（Promise 链）串行化，避免并发投影交错。
- **`waitForProjections`**：pre-step 前 await 所有挂起投影完成。
- **`stepIsOpen`**：通过 `sessionProjections.stateOf(session,'turnBoundary')` 判断是否有 open turn 且最后 step 边界是 start。
- **`projectTouch`**：若 step 开着，累积 touch；否则排队投影。
- **`visibleBaselineSource`**：反向找最近的 baseline 指令消息（inbox 优先，再查 surface）。
- **`filePathFromExecution`**：从 `read/write/edit` 工具的 `file_path` 参数提取路径——文件被触碰后，相关指令集增量刷新进 inbox（"成功 fs 操作触发项目嵌套/变更/移除指令"）。
- **`agent/pre-step` 钩子**：先 `next()`，await 投影，compose 出新指令，折叠进 `decision.messages`（紧跟 claimed batch 之后，使直接 prompt 在前、运行时上下文在后）。

---

## 8. `@deepseek-ai/dsh-token-meter`（计价服务，真实源码在 `packages/llm/token-meter`）

这是整个压缩/裁剪决策的"压力计"——所有触发判断、选段、阴影计价、收敛比较都读这一个单例。固定启发式估算（无配置项，`TokenMeterConfig = Record<string,never>`）。

### 8.1 计价常量（`estimate.ts`）

```
CHARS_PER_TOKEN = 4          // 固定密度：每 4 字符 ≈ 1 token
BLOCK_OVERHEAD = 4           // 每块 JSON 结构/类型标签开销
ROLE_OVERHEAD = 4            // 每条消息 role 字段框定开销
```

### 8.2 `estimate.ts` — 纯函数启发式计价

- **`estimateContent(blocks)`**：递归计价内容块。`text/reasoning` → `ceil(len/4)+4`；`tool-call` → 名字+参数密度+4；`tool-result` → 递归+4；未知块（含 image 引用）→ `estimateStructuralBlock`。
- **`estimateStructuralBlock(block)`**：结构 JSON 价格。`image` 块去掉 `offloaded` 标记后 `4 + ceil(JSON长度/4)`（图像引用本身的参考价；真实视觉 token 由路由计价负责）。
- **`estimateSystemMessage(message)`**：system 提示按纯文本密度+role 框定（无逐块开销）；空内容 → 0（"无 system prompt"）。
- **`estimateMessage(message)`**：`role==='system'` → `estimateSystemMessage`；否则 `estimateContent(content)+ROLE_OVERHEAD`。**这是 pruner 的 `compaction/prune` 阴影计价、摘要收敛校验 `estimateMessage(checkpointMessage)` 都调用的同一函数**。
- **`estimateToolsTokens(header)`**：canonical 请求信封里唯一的计价字段（工具 schema）→ `ceil(JSON长度/4)+4`；无工具 → 0。

### 8.3 `surface-fold.ts` — O(1) 重放折叠（plan/commit 对）

核心是"折叠"：把 surface 上每个事件**增量**地折进一个 priced node 数组，append 是 O(1) push，replace 是 O(1) splice。

- **`MeterSurfaceNode`**：每个 priced 节点携带 `seq / heuristicTokens / imageStructuralTokens / fileStructuralTokens / images[] / files[]`——保留附件细节供路由计价替换。
- **`planSurfaceTokens(nodes, event)`**（只读规划，先跑所有易错步）：
  ```
  node = analyzeNode(event.seq, deriveEventMessage(event))   // 用启发式估出本节点价
  if surfaceOp==='append' → {tokens, deltaTokens:tokens, target:'append'}
  else (replace): 在 nodes 里找 startSeq/endSeq 的 index
       若任一找不到或 startIdx>endIdx → 抛 "replace ... invalid current range"（日志损坏，响亮失败而非跳过）
       removed = 被替换区间 heuristicTokens 之和
       → {tokens, deltaTokens: tokens-removed, target:{startIdx,endIdx}}
  ```
- **`commitSurfaceTokens(nodes, plan)`**（in-place，绝不半应用）：`append` → `push`；`replace` → `splice(startIdx, 长度, node)`。
- **关键设计**：plan（只读、可抛）与 commit（in-place、 infallible）分离——畸形事件在每次重试都同样失败，绝不留下半更新 surface。

### 8.4 `route-pricing.ts` — 请求投影计价（`priceSurface`）

把"固定启发式"替换为"发给路由模型的真实附件表示"：

```
若 无图片定价 且 无文件 → 直接返回 heuristicTokens 作为 tokens
否则：
  images = 所有节点图片扁平化
  prices = pricing.priceImages(images)；若 prices.length != images.length → 抛（计价错位，响亮失败）
  逐节点：tokens = heuristicTokens
          有文件 → 减 fileStructuralTokens，加每个文件的估算文本价
          有图   → 减 imageStructuralTokens，加每图的 visualTokens + 文本价
  surfaceTokens = 各节点 tokens 之和
```
→ 返回 `{nodes: TokenSurfaceNode[], surfaceTokens}`。`TokenSurfaceNode = {seq, tokens(路由价), heuristicTokens(固定价)}`。

### 8.5 `index.ts` — `TokenMeter` 服务主类

- **`measure(session, requestHeader?): TokenMeasurement`**：
  ```
  state = _sync(session)                     // 把 fold 追上当前 durable tail
  header = requestHeader ?? state.header
  pricing = _routeImagePricing(header)       // 路由模型的图片计价（若有）
  fileText = _fileRequestText()              // 挂载的 LLM 服务的文件投影
  surface = priceSurface(state.surface, pricing, fileText)
  // baseline（锚点）：若 anchor 存在且 header 匹配 → 用上次成功调用的锚点重计价（usage 或 estimated）
  //                 否则 → estimated（tools + surface 全价）
  totalTokens = max(0, baseline.tokens + surfaceDeltaTokens)
  return deepFreeze(structuredClone({logRevision, baseline, surfaceDeltaTokens, totalTokens, surfaceTokens, nodes}))
  ```
  **surface 位置权威**：返回的 `nodes` 是当前 surface 的按位置节点数组；`tokens` 是路由计价（retention/选段/收敛用），`heuristicTokens` 是固定价（O(1) 折叠自洽用）。每次调用 clone 节点 → O(surface)。
- **`estimateMessage(message)`**：直接委托纯 `estimateMessage`。
- **`_sync(session)`**：用 `WeakMap<Session,ReplayState>` 缓存每个会话的重放状态；while `consumedEvents < session.seq` 逐个 `_foldEvent`。只对有消费者的会话建状态。
- **`_foldEvent(state, event)`**：处理 `request/header`（canonical 化）、`step/start|end`（校验配对）、`image/offload`（标记图片已卸载）；`assistant/message` 时若带 `usage` 且 header 匹配 → 设 anchor（含"assistant 消息前所有 surface 节点" + provider 输出价）；最后 `planSurfaceTokens` → `commitSurfaceTokens`。**先 plan 后 commit**，畸形事件不半应用。

### 8.6 `types.ts` — 测量词汇

- **`TokenMeasurement`**：`logRevision / baseline{kind:'none'|'estimated'|'usage'} / surfaceDeltaTokens / totalTokens / surfaceTokens / nodes[]`。detached、deepFreeze 不可变快照。
- **`TokenSurfaceNode`**：`seq / tokens(路由价) / heuristicTokens(固定价)`——触发器、保留、选段全读 `tokens`；阴影计价协议用 `heuristicTokens` 保证折叠自洽。

---

## 9. Claude Code 对照（逆向，无逐行源码）

Claude Code 没有事件溯源的"日志 vs 投影"二分，而是**单一 agentic loop + 5 层懒惰降级阶梯**（`QueryEngine` 在 loop 内监控 `COMPACT_THRESHOLD`，超阈值暂停循环做维护）：

| 层 | 操作 | 成本 |
|---|---|---|
| ⑤ Budget reduction | 超大输出换成"结果已存 X 路径"的引用指针 | 最低 |
| ④ Snip | SnipTool 删历史大块文本（如巨型 build 日志） | 低 |
| ③ Microcompact | 细粒度、缓存感知压缩 | 中 |
| ② Context collapse | 读取时投影合并（视觉合并不删） | 中 |
| ① Auto-compact | 调 LLM 把最早对话摘要成特殊 system message | 最高、有损 |

**关键经济学（逆向结论）**：`CLAUDE.md` 不进 system prompt，而是包在标签里作为 `messages` 一部分注入——因为 system prompt 对所有用户相同才能共享 90% 折扣的 prompt cache；若进 system prompt 则每个用户单独缓存，产品经济学崩塌。这与 DeepHarness 把"可复用前缀"放在摘要调用前（复用 KV cache）是同一思想的两种表达。

---

## 10. 可借鉴的设计清单（对应方法）

1. **懒惰降级**：`compactIfNeeded` 先 `pruneSession`（确定性、免费）→ 再 `selectCompactableRange` + LLM。Claude Code 同理逐级升级。
2. **超大输出外置**：`spill-policy` 的 `spillReplacement` + `LocalSpillStore.saveText` = 存全文 + 头尾预览 + locator。与 Claude Code 的 budget reduction 殊途同归。
3. **事务锁**：`compaction/start`→`summary`→`end` 三段式 + `assertCompactionInactive` 防并发；崩溃留"有 start 无 end"可检测。
4. **可逆压缩**：被替换节点不删（shadowed），只移出 surface，可恢复。
5. **前缀缓存复用**：`buildSummarizationInput` 把摘要调用构造成"上次请求的真正前缀"，`summarizeWithLlm` 末尾才追加指令。
6. **fail-closed 校验**：`summarizeCompaction` 拒绝"摘要不比原文小"；`spillReplacement` 拒绝"替换超 cap"；`invariant.ts` 全程锁日志合法性。
7. **不变量即测试**：`invariant.ts` 把压缩事务的所有不变量做成可注册检查器，而非散落断言。
