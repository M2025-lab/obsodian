# Agent 上下文管理 Resources

## Knowledge

- [deepseek-harness 仓库（GitHub, MIT 开源）](https://github.com/deepseek-ai/deepseek-harness)
  真实可逐行读的开源源码。Use for: compaction / spill / context / token-meter 各包的真实实现与方法级细节。`docs/subsystems/*.zh.md` 是官方设计文档。
- [compaction 设计文档](https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/subsystems/compaction.zh.md)
  压缩子系统权威说明：触发条件、流水线、shadowed 可逆、6 类失败处理。Use for: 理解压缩事务与可逆性。
- [spill 设计文档](https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/subsystems/spill.zh.md)
  超大工具结果外置。Use for: 理解"存盘 + 指针替换"而非把全文塞进窗口。
- [session-projection 设计文档](https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/subsystems/session-projection.zh.md)
  Raw Log ≠ Surface 双重视角。Use for: 整件事的基石概念。
- [token-meter 设计文档](https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/subsystems/token-meter.zh.md)
  按 surface 位置计价。Use for: 理解压缩基于真实 token 压力而非拍脑袋。
- [Claude Code 逆向源码（codezjx/claude-code-source-code）+ DeepWiki](https://github.com/codezjx/claude-code-source-code)
  社区基于 2026-03 npm `.map` 泄露重建。Use for: Claude Code 5 层降级阶梯对照。**注意：逆向、非官方，结论需标注。**

## Wisdom (Communities)

- 用户尚未指定社区，暂不推荐。未来若需可参考 Anthropic / DeepSeek 官方论坛与相应 GitHub Discussions。

## Gaps

- Claude Code 无官方可逐行验证源码，仅有逆向结论，细节可信度低于 DeepHarness。
- DeepHarness 的 `agent-loop` 主循环与 `Cordis` 微内核尚未在本课程覆盖，留待后续。
