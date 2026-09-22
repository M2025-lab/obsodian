# Mission: 理解生产级 Agent 的上下文管理（以 Claude Code 与 DeepHarness 为样本）

## Why

用户是后端工程师，正在做"安装拆除经营主体用工改造"等 Agent 相关系统，也需要把知识沉淀成可复用的能力。ta 需要真正搞懂工业级 Agent 如何在**有限的、昂贵的 LLM 上下文窗口**里持续工作——不只是"知道有这回事"，更要能讲清机制、能照着写出一个最小实现。落地后可用于：给自己系统加上下文压缩、给团队讲清楚、或评审他人设计。

## Success looks like

- 能用一句话 + 一张图说清"上下文管理解决什么问题"
- 能画出 Raw Log vs Surface 的关系，并解释为什么这样设计
- 能默写出 DeepHarness 的 `compactIfNeeded` 决策树
- 能写一段最小 `CompactionEngine` 伪代码（measure → prune → summarize → replace surface）
- 能对比 Claude Code 与 DeepHarness 两套思路的取舍

## Constraints

- 用户偏好中文、结构清晰、逻辑连贯、循序渐进
- 以真实开源源码为依据（DeepHarness 是 MIT 开源；Claude Code 为社区逆向，需标注可信度）
- 一次不要堆太多方法细节——先建立心智模型，再讲实现

## Out of scope

- 把通用机制套到某个自家业务系统写代码（先把机制讲透）
- 向量库 / RAG 检索（属于"外置知识"，不在本轮压缩机制范围）
- Claude Code 逐方法源码（逆向、不可逐行验证，仅作对照）
