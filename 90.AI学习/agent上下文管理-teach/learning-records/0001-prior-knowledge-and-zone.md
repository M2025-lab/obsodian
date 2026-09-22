# 前置知识与本轮学习区（zone of proximal development）

用户在上一轮已读过 Claude Code / DeepHarness 上下文管理的**方法级源码详解**（compaction / spill / agent-instructions / token-meter 各包的方法签名与类型），即已"接触过"细节。
本轮明确诉求是"讲清楚逻辑、明白怎么做"——说明 gap 不在术语与方法名，而在**逻辑主线与最小实现**：为何要管理、Raw Log≠Surface 这一总开关、决策树、事务锁、可逆性，以及如何照着写出一个 CompactionEngine。

**Implications**：后续教学应继续"先心智模型后实现"，少罗列方法签名；下一可教节点可选：① agent-loop 主循环如何挂监听触发压缩；② Cordis 微内核插件机制；③ 对比 Codex / LangGraph 的上下文处理。避免一上来再堆方法清单。
