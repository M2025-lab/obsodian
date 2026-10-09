# 开发中可以直接模仿的模式

结合你的 Java/Spring Boot 背景，我把两套实现里"可以抄进自己代码"的东西按难度分成三档：

## 第一档：调 API 就能抄（不需要自研 harness）

**1. system prompt 冻结 + 动态内容后置**（第 28 行的经济学）

```
❌ 常见错误写法
system = "你是XX助手" + 当前时间 + 用户ID + 项目配置   ← 每次请求都变，缓存永远 miss

✅ 模仿 Claude Code 的分层
system = 固定人设（字节级不变，全用户共享）      ← cache_control 断点打在这
messages[0] = <user_context>项目配置、CLAUDE.md 类内容</user_context>（附"可能相关也可能不相关"）
messages[1..] = 真实对话
```

Spring Boot 里的具体做法：凡是拼 system prompt 的地方，**禁止注入 `LocalDateTime.now()`、UUID、traceId**。当前时间这类信息放到 messages 尾部的一条 user 消息里。然后用响应里的 `usage.cache_read_input_tokens` 做埋点验证——这个字段应该随会话轮次上涨，如果一直是 0，说明前缀被污染了。

**2. 超大工具输出外置（budget reduction / spill 的简化版）**

这是最容易落地、收益最大的一条：

```
工具执行返回 8000 行日志
    ↓
if (output.length() > MAX_INLINE_BYTES) {
    String id = store.save(output);          // 存盘/存 Redis
    return head(output, 50) + "\n...[截断]...\n" + tail(output, 50)
         + "\nlocator: result://" + id       // 模型可用 read_result 工具取回
}
```

注意模仿 DeepHarness 的两个细节：

- **head/tail 都保留**（错误信息通常在尾部），中间截断，而不是只留开头
- **按字符边界切**，别把 emoji / 中文代理对切一半（对应它"按 Unicode code point 切"的铁律）——Java 里就是别用 `substring` 按 char 切，用 `codePoints()`

**3. 确定性剪枝优先于 LLM 摘要**（第 2.3 节的顺序）

```
token 超阈值？
    ↓
第一步：代码删（免费、无损语义、可预测）
    - 旧的 tool_result 中间截断，留 head/tail + locator
    - 重复的文件读取结果只留最新一份
    ↓ 重新计量
还不够？第二步：才调 LLM 摘要（贵 + 有损，最后手段）
```

很多自研 agent 一上来就 LLM 摘要历史，其实 80% 的窗口是被工具输出撑爆的——先剪工具结果往往就够了。

## 第二档：架构级模仿（自研 agent 时抄）

**4. Raw Log ≠ Surface 的事件溯源**（第 2.1 节，最值得抄的架构）

```java
// append-only 事件表
session_event(id, session_id, seq, type, payload)
// type: USER_MESSAGE / ASSISTANT_MESSAGE / TOOL_RESULT / COMPACTION_SUMMARY / SURFACE_OP ...

// 模型看到的历史 = 从事件流投影出来，压缩只是插入一条 SURFACE_OP{replace, startSeq, endSeq}
```

java

好处直接对应你熟悉的后端世界：

- **压缩可逆**：原始消息不删，只是投影时被跳过——相当于逻辑删除 + 视图
- **可回放审计**：出问题重放事件流就能还原"模型当时看到了什么"
- **崩溃安全**：模仿它的锁协议——压缩开始写 `compaction/start` 事件，结束写 `compaction/end`，中途崩溃则启动时发现"有 start 无 end"直接拒绝并发压缩（这就是一个用事件表实现的分布式锁，你不需要 Redis）

**5. 乐观锁保护长程状态**（GoalRef.revision）

如果做长任务 agent，目标/计划单独存表，带 `revision` 字段：

```java
// goal(id, session_id, content, phase, revision)
UPDATE goal SET content=?, phase=?, revision=revision+1
WHERE id=? AND revision=?   // 冲突则重读再合并
```

java

对应你 vault 里第 5 条借鉴思想："objective 要独立于消息队列持久化"——消息可以被压缩掉，但目标不能被压没。Spring 的 `@Version` 就是现成的。

## 第三档：思维习惯（代码之外）

|习惯|出处|落地方式|
|---|---|---|
|降级阶梯|Claude Code 5 层|任何"资源快满了"的处理都设计成 N 层，从免费到有损排序，写清楚每层的触发条件|
|Model-visible means logged|DeepHarness 铁律|code review 规则：任何新增的"塞给模型看"的内容，必须能回答"它从哪个持久化事件派生"|
|失败分类|手动压缩 6 类预期失败|给压缩/摘要路径枚举预期失败（busy/cancelled/changed…），每类定义好状态回滚，而不是裸 try-catch|

## 优先级建议

如果只挑三件事做：**[#1（system](app://obsidian.md/index.html#1%EF%BC%88system) prompt 冻结，一天搞定，直接省钱）→ [#2（超大输出外置，防窗口爆炸）→](app://obsidian.md/index.html#2%EF%BC%88%E8%B6%85%E5%A4%A7%E8%BE%93%E5%87%BA%E5%A4%96%E7%BD%AE%EF%BC%8C%E9%98%B2%E7%AA%97%E5%8F%A3%E7%88%86%E7%82%B8%EF%BC%89%E2%86%92) [#3（确定性剪枝，压缩成本降一个量级）](app://obsidian.md/index.html#3%EF%BC%88%E7%A1%AE%E5%AE%9A%E6%80%A7%E5%89%AA%E6%9E%9D%EF%BC%8C%E5%8E%8B%E7%BC%A9%E6%88%90%E6%9C%AC%E9%99%8D%E4%B8%80%E4%B8%AA%E9%87%8F%E7%BA%A7%EF%BC%89)**。#4/#5 等你真的自研 agent harness 时再上，单纯调 API 用不到事件溯源。