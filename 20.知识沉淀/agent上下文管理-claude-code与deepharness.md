# Agent 上下文管理：Claude Code 与 DeepHarness 拆解

> 来源：Claude Code 逆向自 v2.1.88 npm `.map` 泄露（codezjx/claude-code-source-code 的 DeepWiki 分析）；DeepHarness 为 MIT 开源，源码在 github.com/deepseek-ai/deepseek-harness（docs/subsystems/compaction.zh.md、spill.zh.md、session-projection.zh.md、token-meter.zh.md）。

## 0. 核心矛盾

LLM 有固定上下文窗口（Claude 200K，可开 1M）。Agent 跑长任务时，对话历史 + 工具输出（bash 的 stdout、文件内容、搜索结果）很快把窗口塞满。满了之后：模型看不全、看不到关键东西、成本爆炸。

所有 agent harness 的核心矛盾：如何在一个有限、昂贵的窗口里，让模型始终看到"足够决策的最小信息"。

两套真实实现：
- Claude Code：懒惰降级阶梯（5 层）
- DeepHarness：事件溯源 + 能力 seam

## 1. Claude Code 的上下文管理（逆向）

核心思想：懒惰降级阶梯（lazy-degradation ladder）——永远先用最便宜的操作，逐级升级，平衡"最大化工作记忆"与"最小化缓存失效"。

五层（从便宜到贵）：
1. Budget reduction（预算削减）：把超大原始工具输出换成"引用指针"，模型看到路径而非 8000 行。最便宜。
2. Snip（裁剪）：用 SnipTool 删掉历史里特定大块文本（如巨型 build 日志），不总结整段。
3. Microcompact（微压缩）：细粒度、缓存感知压缩，优先保留 prompt caching 经济性。
4. Context collapse（上下文坍缩）：读取时投影合并多条消息，视觉合并不删。
5. Auto-compact（自动压缩）：调 LLM 把最早对话摘要成 context block，作为特殊 system message 插入，原始消息移出活动窗口。最贵、有损。

监控：`AppState.messages[]` 实时统计 token，设 `COMPACT_THRESHOLD`，由 `QueryEngine` 在 agent loop 超阈值时暂停循环做维护。

关键经济学洞察：CLAUDE.md 不进 system prompt，而是包在标签里作为 messages 数组的一部分注入，并附"可能相关也可能不相关"。原因：system prompt 对所有用户相同才能共享 90% 折扣的 prompt cache；CLAUDE.md 进 system prompt 会让每个用户单独缓存，产品经济学崩塌。

## 2. DeepHarness 的上下文管理（开源）

上下文管理拆成三个包，建立在事件溯源之上。

### 2.1 底层不变量：Raw Log ≠ Surface

Session 是 append-only 日志，每个事件有连续 seq。真正进入模型历史的只是 surface（表面投影）——只有 user/message、assistant/message、tool/result 进 surface；chunk、turn 边界、usage、hook 只留 raw log。

设计铁律：Model-visible means logged（模型看到的，必须从日志能重建）。任何新加的模型可见输入，必须扩展 Session event 或能从已有 durable event 派生。

两份视角：
- Raw log = 发生过什么（完整、可回放、可审计）
- Surface = 模型现在该看什么（投影）

压缩的本质 = 不改日志，只改 surface 投影。可逆、可审计。

### 2.2 三个包

- context（注入什么）：AGENTS.md 工作区指令、跨会话引用（带字节预算截断）、时间上下文。
- spill（超大结果挪出去）：工具结果超过 maxInlineBytes 时存全文，上下文只留 head/tail 预览 + locator，模型按 retrievalHint 去 read/grep。与 Claude Code 的 budget reduction 同一思想，但更显式、可审计。
- compaction（历史收缩）：见 2.3。

### 2.3 compaction 流水线（渐进式，最小损失）

触发：pressure（token 超阈值）或 context-overflow（服务端窗口溢出错误）。

流水线：
1. 检查压力/溢出是否满足
2. 可选 toolResultPruner 先确定性剪枝：超预算工具结果中间裁掉，保留 head/tail + rich block 顺序（按 Unicode code point 切，不切断代理对）
3. 用 tokenMeter 重新计量
4. 剪枝后够 → 直接推进 surface，不调 LLM
5. 不够 → 选 range，调 LLM 生成摘要

锁机制：compaction/start → 摘要 → compaction/summary → 写入带 surfaceOp:{op:'replace',startSeq,endSeq} 的 user/message → compaction/end。中途崩溃留"有 start 无 end"遗留锁，阻塞所有入口点，防并发。

shadowed（遮蔽）可逆：被替换的原始对话不删，仍在 raw log，只是不进 surface。shadowedRange 是 surface 位置跨度（非数值区间，新摘要节点 seq 更高但位置更早，start 可能 > end）。

手动压缩 6 类预期失败：busy / cancelled / changed / summary / commit / persistence，各自定义 surface 与日志影响。

### 2.4 配套

- token-meter：独立回放快照 TokenMeasurement，按位置算每个 surface 节点 token 数（surface 顺序权威）。触发判断、范围选择、替换节点遮蔽计价（shadow-price）全读它。
- goal（目标持久化）：完成目标持久化，带阶段 active/paused/blocked/complete，GoalRef.revision 乐观锁。goal/change 事件独立派生目标状态，与消息队列解耦。

## 3. 对比

| 维度 | Claude Code（逆向） | DeepHarness（开源） |
|---|---|---|
| 架构哲学 | 单一 agentic loop + 5 层降级阶梯 | 事件溯源 + 可插拔能力 seam |
| 压缩触发 | 阈值监控，agent loop 内暂停 | pressure / context-overflow 事件 |
| 压缩策略 | 5 层逐级升级，先便宜 | 先确定性剪枝，不够才 LLM 摘要 |
| 超大输出 | budget reduction（指针替换） | spill（存全文 + locator 预览） |
| 记忆/目标 | memdir（.claude/memory 注入未来 session） | goal 子系统（事件溯源持久化） |
| 可审计性 | 较隐式 | 极强：raw log 完整，surface 是投影 |
| 缓存经济 | CLAUDE.md 不进 system prompt，保共享 cache | request/header 单独记录，恢复/切模型清晰 |

## 4. 可借鉴的 5 条核心思想

1. 懒惰降级：永远先试最便宜的压缩手段，LLM 摘要是最后手段（贵 + 有损）。
2. 超大输出外置：别把 8000 行塞进上下文，存盘 + 预览 + locator。budget reduction 与 spill 殊途同归。
3. 事件溯源是可审计上下文管理的根基：把"模型看到的"和"发生过的"分开，压缩只是投影变更、日志不变 → 可回放、可逆、可调试。
4. 缓存经济优先于便利：共享 prompt cache 比把什么都塞进 system prompt 重要。
5. 长任务需要目标锚：session 可压缩、可分叉，但 objective 要独立于消息队列持久化，否则长程 agent 会迷失。
