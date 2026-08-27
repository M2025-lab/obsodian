---
title: Agent 学习计划（8 周 · Python-first）
tags:
  - AI
  - Agent
  - 学习计划
  - Python
  - Java
created: 2026-08-27
updated: 2026-08-27
---

# Agent 学习计划（8 周 · Python-first）

> **一句话路线**：`Python → LLM API → Structured Output → Tool Calling → RAG → MCP → LangGraph → Agent 工程化 → Java/Spring Boot 集成 → 企业级 Agent`
>
> **定位**：Python 学 AI，Java 学业务，最终用 **Python Agent + Java 微服务** 做企业级 Agent。Java 不丢掉。

---

## 0. 路线设计思路

结合你现在已经有的 **Java / Spring Boot / Kafka / Redis / MySQL / ES** 基础，**不要**走「从 Python 学起 → 学机器学习 → 学深度学习 → 最后才碰 Agent」这条路线。

你的路线应该是：

> **Python 基础 → LLM API → Tool Calling → RAG → MCP → LangGraph → Agent 工程化 → Java 业务集成 → 企业级 Agent**

**核心原则**：

- Python 只学 Agent 需要的部分（async / Pydantic / FastAPI），**不**学 Django / 数据科学 / NumPy / Pandas / ML 算法 / 深度学习。
- Java **不丢掉**，最终负责业务逻辑、权限、事务、数据库、Kafka、Redis、ES、微服务、已有系统。
- 不一上来学 LangChain，先直接理解 LLM API 原生调用。
- 不钻 Transformer 源码 / CUDA / 模型训练 / 深度学习数学（除非目标是算法岗）。

---

## 1. 整体路线

```text
                    Agent 开发能力
                         │
        ┌────────────────┼────────────────┐
        │                │                │
      AI 基础          Agent 核心        工程能力
        │                │                │
   LLM API           Tool Calling      FastAPI
   Prompt            LangGraph         Docker
   Embedding         MCP               Redis
   Structured        Memory            Kafka
   Output            RAG               MySQL/ES
        │                │                │
        └────────────────┼────────────────┘
                         │
                  企业 Agent 项目
                         │
                 Python + Java
```

最终形成：

```text
                用户
                 │
                 ▼
          Python Agent
                 │
        ┌────────┼────────┐
        ▼        ▼        ▼
       LLM      RAG      Tools
        │        │        │
        │        │        ▼
        │        │   Java Spring Boot
        │        │        │
        │        │   ┌────┼────┐
        │        │   ▼    ▼    ▼
        │        │ Kafka Redis  DB/ES
        │        │
        └────────┴───────────┐
                             ▼
                           答案
```

---

## 2. 第一阶段：Python —— 只学 Agent 需要的

**目标：1 周左右**

你不是转 Python 开发，所以不要把时间花在特别深的 Python 语法上。

### 必学清单

```text
变量 / List / Dict / Set
函数
class
异常处理
模块
pip
venv
JSON
HTTP
async / await
类型注解
Pydantic
```

尤其注意：

```python
async def
await
```

因为后面 Agent 大量涉及：

```text
LLM 请求
Tool 调用
HTTP
Streaming
并发
```

### 学习结果

你能写：

```text
Python
   ↓
FastAPI
   ↓
HTTP API
   ↓
调用 Java 服务
```

### 不需要现在学习

- Django
- 数据科学
- NumPy
- Pandas
- 机器学习算法
- 深度学习

---

## 3. 第二阶段：LLM API —— 真正进入 AI

**目标：1 周**

这一阶段非常重要。**不要一上来就学 LangChain**，先直接理解：

```text
User
 ↓
Prompt
 ↓
LLM
 ↓
Response
```

然后学习：

### 3.1 Chat

```text
messages
   ↓
LLM
   ↓
answer
```

### 3.2 Streaming

```text
LLM
 ↓
token
 ↓
token
 ↓
token
 ↓
前端实时显示
```

### 3.3 Structured Output

例如：

```json
{
  "taskType": 3,
  "judgeResult": 1,
  "reason": "满足通知条件"
}
```

而不是让模型随便输出一段文字。

### 3.4 Tool Calling

这是后面 Agent 的核心。你需要真正理解：

```text
用户：
帮我查询项目 123 的复尺情况

        ↓

LLM 判断

        ↓

我要调用：
query_recheck_task(projectId=123)

        ↓

Java API

        ↓

返回数据

        ↓

LLM

        ↓

最终回答
```

**这一阶段结束，你应该已经能写一个「半自动 Agent」。**

---

## 4. 第三阶段：Tool Calling —— Agent 的核心

**目标：1 周**

这部分建议重点学。因为你本身就是 Java 后端，所以这个东西你会非常容易理解。

例如 Java：

```java
@GetMapping("/project/{id}")
public Project getProject(Long id)
```

对于 Agent 来说，它就是：

```text
Tool:
query_project
```

Agent 的核心其实变成：

```text
用户问题
   ↓
LLM
   ↓
判断需要什么信息
   ↓
选择 Tool
   ↓
调用 Tool
   ↓
得到结果
   ↓
继续思考
   ↓
调用下一个 Tool
   ↓
最终答案
```

### 需要理解的关键概念

- Function Calling
- Tool Schema
- Tool 参数
- Tool 返回值
- Tool Error
- Tool 权限
- Tool 超时
- Tool 重试

这时候你就已经开始真正理解 Agent 了。

---

## 5. 第四阶段：RAG

**目标：1～2 周**

你之前也已经接触过 RAG，所以这里可以直接深入。

### RAG 主链路

```text
知识库
 ↓
Document
 ↓
Chunk
 ↓
Embedding
 ↓
Vector DB
 ↓
Retriever
 ↓
相关知识
 ↓
LLM
 ↓
答案
```

### 基础 RAG

```text
Query
 ↓
Embedding
 ↓
Vector Search
 ↓
Top K
 ↓
LLM
```

### 进阶技术清单

- Chunk
- Embedding
- Vector Database
- Metadata
- Hybrid Search
- Rerank
- Query Rewrite
- Multi Query
- Context Compression

---

## 6. 第五阶段：MCP

**目标：3～5 天理解，之后持续使用**

这个建议你一定学。因为 MCP 可以理解成：

> **Agent 世界里的标准化 Tool 接口。**

以前：

```text
Agent
 ↓
自己定义 Tool
 ↓
调用 Java API
```

MCP：

```text
Agent
 ↓
MCP Client
 ↓
MCP Server
 ↓
Tools
```

例如：

```text
MCP Server
 ├── query_project
 ├── query_task
 ├── query_es
 ├── query_redis
 └── search_knowledge
```

这和你现在后端微服务的思想其实非常接近。

### 重点理解

```text
MCP Client
MCP Server
Tools
Resources
Prompts
Transport
```

### 学习目标

**不用一开始钻 MCP 协议源码。** 先做到：

> 能自己写一个 MCP Server，并让 Agent 调用它。

---

## 7. 第六阶段：LangGraph

**这是你真正应该重点投入的 Agent 框架。目标：1～2 周**

你可以把 LangGraph 理解成：

> **给 Agent 搭建「状态机 + 工作流」的框架。**

例如：

```text
START
  │
  ▼
分析问题
  │
  ▼
查询项目
  │
  ▼
查询识别结果
  │
  ├── 可通知 ──────────┐
  │                    │
  └── 不可通知          │
       │                │
       ▼                │
    查询原因            │
       │                │
       └──────┬─────────┘
              ▼
            总结
              │
              ▼
             END
```

### 需要掌握的概念

- State
- Node
- Edge
- Conditional Edge
- Checkpoint
- Memory
- Human in the Loop
- Streaming
- Retry
- Interrupt

到了这里，你就开始具备真正的 Agent 开发能力。

---

## 8. 第七阶段：Agent 工程化

这是很多教程不会讲，但你作为 Java 后端一定要学的。

### 8.1 状态管理

```text
Redis
```

保存：

```text
conversation
agent state
task state
```

### 8.2 数据

```text
MySQL
```

保存：

```text
Agent Task
Tool Call
Audit
User Action
```

### 8.3 搜索

```text
ES
```

或者向量数据库。

### 8.4 异步

```text
Kafka
```

例如：

```text
Agent Task
    ↓
Kafka
    ↓
Worker
    ↓
Agent
    ↓
Tool
```

### 8.5 可观测性

必须能够看到：

```text
用户问题
 ↓
LLM 请求
 ↓
LLM Response
 ↓
Tool Call
 ↓
Tool Response
 ↓
下一轮 LLM
 ↓
最终结果
```

这对于线上排查非常重要。

#### 工具链

```text
LangSmith / Langfuse   — Agent Trace 可视化、Prompt 回放、成本统计
OpenTelemetry          — 跨服务 Tracing（Python Agent → Java 微服务）
结构化日志             — 每轮 LLM / Tool 的 input/output 落库
```

### 8.6 评测（Eval）

光能跑起来不够，必须能衡量「Agent 好不好」。

```text
离线评测    — 构建测试集，跑准确率 / 工具选择正确率 / 幻觉率
在线评测    — 用户反馈（点赞/点踩）、重写率、人工抽检
回归评测    — Prompt / 模型升级后，跑同一套测试集对比
```

工具：

```text
LangSmith Datasets
Langfuse Evaluations
自建评测脚本（LLM-as-a-Judge）
```

关键指标：

```text
Tool 选择准确率
最终答案准确率
平均轮数（防无限循环）
Token 成本 / 平均耗时
```

没有评测的 Agent，迭代全凭感觉，线上出问题也无法定位是哪一步退化。

### 8.7 安全

Agent 能调 Tool 就能改数据，安全必须前置。

```text
Prompt Injection   — 用户输入污染 Prompt，诱导调用危险 Tool
权限控制           — Tool 按用户角色授权，Agent 不能越权
输入审核           — 用户输入 / LLM 输出过审（敏感词、PII）
Sandbox            — 代码执行类 Tool 在沙箱里跑
```

原则：

```text
最小权限    — Agent 只拿到完成任务所需的最少 Tool
人在回路    — 写操作必须 Human Confirmation（见项目场景二）
审计落库    — 每次 Tool Call 记录 who / what / when / result
```

### 8.8 部署

```text
Docker      — Python Agent / Java 微服务各自镜像
K8s         — 弹性伸缩、滚动发布、资源限制
CI/CD       — Prompt 变更 → 测试集回归 → 灰度发布
```

注意 LLM 应用的部署特殊性：

```text
模型版本    — 升级模型可能改变输出，必须跑回归评测
成本控制    — 限流 + Token 上限 + 缓存
超时熔断    — LLM / Tool 调用都要有超时和降级
```

---

## 9. 第八阶段：Java + Python 双语言架构

这个阶段把你现有技术优势发挥出来。最终采用：

```text
             ┌──────────────┐
             │   Frontend   │
             └──────┬───────┘
                    │
                    ▼
          ┌──────────────────┐
          │  Python Agent    │
          │                  │
          │ LangGraph        │
          │ RAG              │
          │ Tool Calling     │
          │ MCP              │
          └────────┬─────────┘
                   │
              HTTP / MCP
                   │
                   ▼
          ┌──────────────────┐
          │ Java Spring Boot │
          │                  │
          │ Business Logic   │
          │ Permission       │
          │ Workflow         │
          │ Transaction      │
          └────────┬─────────┘
                   │
       ┌───────────┼───────────┐
       ▼           ▼           ▼
     MySQL       Redis       Kafka
                   │
                   ▼
                   ES
```

### Python 负责

```text
AI能力
Agent
Planning
RAG
Tool Calling
MCP
LLM
```

### Java 负责

```text
业务逻辑
权限
事务
数据库
Kafka
Redis
ES
微服务
已有系统
```

这对你来说是**性价比最高的路线**。

---

## 10. 最终项目：企业级 Agent ——「项目智能助手」

不要只做：

> 「你好，我是一个 AI 助手。」

这种项目价值比较低。建议直接做一个和你实际业务类似的。

### 10.1 场景一：查询与判断

用户：

> 帮我看看 826051217000001469 这个项目目前有哪些通知复尺任务，哪些可以一键通知？为什么？

Agent：

```text
1. 识别 projectOrderId
       ↓
2. queryProject()
       ↓
3. queryNotifyTask()
       ↓
4. queryRecognizeResult()
       ↓
5. 查询业务规则 RAG
       ↓
6. 判断每个任务
       ↓
7. 汇总
       ↓
8. 返回结果
```

### 10.2 场景二：执行（带 Human Confirmation）

> 帮我把可以通知的任务全部提交。

这时候：

```text
Agent
 ↓
查询任务
 ↓
判断
 ↓
Human Confirmation
 ↓
用户确认
 ↓
调用 Java Tool
 ↓
提交通知
 ↓
返回执行结果
```

这就已经是一个非常标准的**企业 Agent**了。

### 10.3 项目演进：Construction-Agent 连续项目思路

建议把最终项目命名为 **Construction-Agent**（或 Enterprise-Agent），不要每阶段做零散 Demo，而是在**同一个项目上逐阶段叠加能力**：

```text
P1  聊天 + Tool Calling + Memory
 ↓
P2  加入 LangGraph Workflow、Planner、Skills
 ↓
P3  接入 MCP Server、日志、SQL、Git 等企业工具
 ↓
P4  加入 RAG 知识库（设计文档、接口文档、DDD 文档）
 ↓
P5  拆分为 Planner / Retriever / Reviewer / Executor 多 Agent 协作
 ↓
P6  补充监控、评测、权限管理，演进成企业级 Agent 平台
```

这样半年后你不仅掌握了各项技术，还拥有一个**持续演进、可展示**的代表性项目，非常适合作为求职 AI Agent 岗位的作品集。

### 10.4 终局展望：企业 Agent 平台

当单个 Agent 成熟后，最终演进成一个**企业 Agent 平台**：

```text
               Agent Platform
                     │
      ┌──────────────┼──────────────┐
      │              │              │
 Prompt Center   Skill Center   MCP Center
      │              │              │
      ├──────────────┼──────────────┤
      │              │              │
 Knowledge        Workflow       Memory
      │              │              │
      └──────────────┼──────────────┘
                     │
              Multi Agent Runtime
                     │
      ┌──────────────┼──────────────┐
      │              │              │
    Planner       Executor       Reviewer
                     │
             Enterprise Tools
```

核心模块：

```text
Prompt 管理    — 版本化、模板化、灰度切换
Tool / Skill 管理 — 注册、发现、权限、调用统计
Memory 管理   — 会话 / 长期记忆 / Profile
Knowledge 管理 — 知识库入库、Chunk、更新、Rerank
Workflow 管理  — 可视化编排、版本、回滚
Agent 评测     — 离线 / 在线 / 回归
```

---

## 11. 8 周学习计划表

| 周   | 学习内容             | 最终成果                              |
| --- | ---------------- | --------------------------------- |
| 第1周 | Python + FastAPI | 能写 Python API                     |
| 第2周 | LLM API          | 能调用模型、Streaming、Structured Output |
| 第3周 | Tool Calling     | 能让 LLM 调 Java API                 |
| 第4周 | RAG              | 能构建企业知识库                          |
| 第5周 | MCP              | 能自己写 MCP Server                   |
| 第6周 | LangGraph        | 能构建多步骤 Agent                      |
| 第7周 | Agent 工程化        | Redis/Kafka/DB/日志/权限              |
| 第8周 | 企业 Agent         | 完成一个可实际运行的 Agent                  |

---

## 12. 学习重点比例

如果你的目标是**开发级别**，建议：

```text
Python              10%
LLM                 15%
Prompt              5%
RAG                 15%
Tool Calling        15%
MCP                 10%
LangGraph           15%
Agent 工程化        15%
```

### 不要这样分配

```text
Python 学 50%
Prompt 学 30%
Agent 学 20%
```

也不要一开始就去学：

```text
Transformer源码
CUDA
模型训练
深度学习数学
```

除非你的目标是**训练模型 / 做算法岗**。

---

## 13. 最终能力自测

学习完以后，你应该可以独立回答这些问题：

### LLM

> 为什么需要 Structured Output？
>
> Streaming 是怎么实现的？
>
> Token 是什么？
>
> Context Window 是什么？

### RAG

> 为什么需要 Chunk？
>
> Embedding 到底是什么？
>
> 为什么向量搜索会搜不准？
>
> Rerank 解决什么问题？

### Agent

> Agent 和普通 Chat 有什么区别？
>
> Agent 怎么决定调用哪个 Tool？
>
> Tool Calling 的完整链路是什么？
>
> Agent 如何保存状态？
>
> 如何避免 Agent 无限循环？

### LangGraph

> State 是什么？
>
> Node 和 Edge 是什么？
>
> Conditional Edge 怎么实现？
>
> Checkpoint 干什么？
>
> Human-in-the-loop 怎么做？

### MCP

> MCP 为什么需要？
>
> MCP Server 和 Tool 是什么关系？
>
> MCP 和普通 HTTP API 有什么区别？

### 企业级

> Agent 如何接入 Kafka？
>
> Tool 如何做权限控制？
>
> Agent 如何保证幂等？
>
> Tool 调用失败怎么办？
>
> 如何记录 Agent Trace？
>
> 如何控制 LLM 成本？

### 评测与安全

> 没有标注数据，怎么做 Agent 评测？
>
> LLM-as-a-Judge 适合评什么、不适合评什么？
>
> Prompt Injection 如何防御？
>
> 写操作 Tool 如何防止被诱导误调用？
>
> 模型升级后怎么确认没有退化？

如果这些你都能回答，并且能自己写出来，基本就已经从：

**「会调用 AI API」**

进入：

**「能开发 Agent」**

这个阶段了。

---

## 14. 一句话路线总结

> **Python → LLM API → Structured Output → Tool Calling → RAG → MCP → LangGraph → Agent 工程化 → Java/Spring Boot 集成 → 企业级 Agent**

而且对你来说，**Java 不需要丢掉**。你的目标应该是：

> **Python 学 AI，Java 学业务，最终用 Python Agent + Java 微服务做企业级 Agent。**
