---
title: Day7：整合 —— FastAPI 调用 Java 服务
tags: [AI, Agent, Python, 第一阶段, Day7]
created: 2026-08-27
updated: 2026-08-27
---

# Day7：整合 —— FastAPI 调用 Java 服务（4 小时）

> 学习目标：把前 6 天的成果串起来，完成本阶段输出项目 —— Python API 调用 Java Spring Boot。

## 1. 知识点清单

今天是整合日，把前 6 天串成一个完整项目。涉及：

```text
Day1 class + 异常       → Tool 基类
Day2 venv + 模块         → 项目结构
Day3 Pydantic           → 请求/响应模型
Day4 async/await        → 并发调 Java
Day5 httpx              → JavaServiceClient
Day6 FastAPI            → 对外 API
```

本阶段最终形态：

```text
用户/前端
   ↓ HTTP
Python FastAPI（async + Pydantic）
   ↓ httpx async
Java Spring Boot（业务逻辑 / 权限 / 事务）
   ↓
MySQL / Redis / Kafka / ES
```

## 2. 怎么学

**时间分配**：

```text
0:00-0:30  搭项目结构（把前 6 天文件归位）
0:30-1:00  起 Java mock 服务
1:00-2:00  串 Python → Java 全链路
2:00-2:30  并发调多个 Java 接口
2:30-3:00  异常 / 超时 / 日志
3:00-4:00  跑通端到端 + 写 README
```

**学习方法**：

1. 今天主要是**整合**，不学新知识点
2. 每一步都实际跑通，不是写完就算
3. 重点验证「删 venv → 还原 → 跑通」的全链路可复现
4. 完成后对照阶段验收清单逐项打勾

**核心理解**：今天打通的「Python FastAPI → Java Spring Boot」链路，就是后面 Tool Calling 的本质——Tool 内部就是 httpx 调 Java。今天通了，后面 Tool Calling 只是在外面包一层「LLM 决定调哪个 Tool」。

## 3. 知识点详解

### 3.1 项目结构

```text
construction-agent/
├── .venv/
├── requirements.txt
├── .gitignore
├── README.md
├── app.py                    # FastAPI 入口 + lifespan
├── config.py                 # 配置（base_url / token）
├── models.py                 # Pydantic 模型（Day3）
├── java_client.py            # JavaServiceClient（Day5）
├── mock_java.py              # Java 端 mock（测试用）
└── tools/
    ├── __init__.py
    ├── base.py               # Tool 基类（Day1）
    ├── query_project.py
    └── query_task.py
```

**职责对应主计划第 9 章**：

- `app.py` / `tools/` → Python 负责 AI、Agent、Tool 调用入口
- `java_client.py` → 连接 Java 业务的通道
- `mock_java.py` → 模拟 Java 微服务（真实场景换成你的 Spring Boot）

### 3.2 config.py —— 配置集中

```python
import os

BASE_URL = os.getenv("JAVA_BASE_URL", "http://localhost:8080")
TOKEN = os.getenv("JAVA_TOKEN", "test-token")
PORT = int(os.getenv("PORT", "8000"))
```

> 对比 Java：`@Value("${java.url}")` + Apollo 配置中心。Python 用环境变量 + `python-dotenv` 读 `.env`。Apollo 的配置哲学一致，本阶段先用环境变量。

### 3.3 tools/ —— Tool 层（包 Java 调用）

`tools/base.py`（Day1 升级为 async）：

```python
class Tool:
    def __init__(self, name: str, description: str = ""):
        self.name = name
        self.description = description

    async def run(self, **kwargs):
        raise NotImplementedError
```

`tools/query_project.py`：

```python
from .base import Tool

class QueryProjectTool(Tool):
    def __init__(self, java):
        super().__init__(name="query_project", description="根据 projectOrderId 查询项目信息")
        self._java = java

    async def run(self, project_id: str) -> dict:
        return await self._java.query_project(project_id)
```

`tools/query_task.py`：

```python
from .base import Tool

class QueryTaskTool(Tool):
    def __init__(self, java):
        super().__init__(name="query_task", description="查询项目的通知复尺任务")
        self._java = java

    async def run(self, project_id: str) -> dict:
        return await self._java.query_task(project_id)
```

> **关键认知**：`QueryProjectTool.run()` 内部就是「httpx 调 Java」。这就是后面 Tool Calling 的全部本质——LLM 决定调哪个 Tool，Tool 内部调 Java。今天把这条路打通。

### 3.4 app.py —— 入口整合

```python
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, Depends, HTTPException

from config import BASE_URL, TOKEN, PORT
from models import ProjectQuery, Project
from java_client import JavaServiceClient, JavaServiceError
from tools.query_project import QueryProjectTool
from tools.query_task import QueryTaskTool

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.java = JavaServiceClient(base_url=BASE_URL, token=TOKEN)
    yield
    await app.state.java.close()

app = FastAPI(title="Construction Agent V0", lifespan=lifespan)

def get_java(request: Request) -> JavaServiceClient:
    return request.app.state.java

@app.post("/agent/query", response_model=Project)
async def query(req: ProjectQuery, java: JavaServiceClient = Depends(get_java)):
    """并发查项目 + 任务，返回整合结果"""
    project_tool = QueryProjectTool(java)
    task_tool = QueryTaskTool(java)
    try:
        project, task = await asyncio.gather(
            project_tool.run(project_id=req.projectId),
            task_tool.run(project_id=req.projectId),
        )
        project["tasks"] = task.get("tasks", [])
        return Project.model_validate(project)
    except JavaServiceError as e:
        raise HTTPException(status_code=502, detail=str(e))
```

### 3.5 mock_java.py —— Java 端替身

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/project/{pid}")
async def get_project(pid: str):
    return {"projectId": pid, "name": "XX项目", "notifiable": True}

@app.get("/task")
async def get_task(projectId: str):
    return {
        "projectId": projectId,
        "tasks": [
            {"taskId": "t1", "canNotify": True, "reason": "复尺通过"},
            {"taskId": "t2", "canNotify": False, "reason": "未复尺"},
        ],
    }

@app.post("/notify/submit")
async def submit(projectId: str, taskIds: list):
    return {"success": True, "projectId": projectId, "submitted": taskIds}
```

> 真实场景换成你的 Spring Boot 服务，接口对齐即可。Python 端不用改。

## 4. 编码练习

### 任务 1：搭目录

按 3.1 结构把前 6 天的文件归位（复用，不重写）。

### 任务 2：起 Java mock

```bash
# 终端 1
uvicorn mock_java:app --port 8080 --reload
```

### 任务 3：起 Python Agent

```bash
# 终端 2
uvicorn app:app --port 8000 --reload
```

### 任务 4：端到端测试

打开 `http://localhost:8000/docs`：

1. POST `/agent/query`，body：
   ```json
   {"projectId": "826051217000001469"}
   ```
   → 返回项目 + tasks 整合结果

2. 故意停掉 Java mock，再调 → 返回 502

3. 把 `projectId` 改成非 18 位 → 422 校验失败

### 任务 5：依赖还原验证

```bash
deactivate
rm -rf .venv
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --port 8000
```

确认能从零还原并跑通——这是项目可交付的最低标准。

### 任务 6：写 README

`README.md` 含：启动命令、接口说明、环境变量、依赖还原步骤。

### 任务 7：加日志（选做）

在 `app.py` 路由里加：

```python
import logging
logger = logging.getLogger("agent")

@app.post("/agent/query")
async def query(req, java=Depends(get_java)):
    logger.info(f"query projectId={req.projectId}")
    start = time.time()
    ...
    logger.info(f"done in {time.time()-start:.2f}s")
```

### 验证

- 两个服务（Java mock + Python Agent）都起来
- `/docs` 调通，返回整合数据
- 停 Java → 502，不是 500 崩溃
- `requirements.txt` 能从零还原环境

## 5. 常见坑

### 坑 1：两个服务端口搞混

- Java mock：8080
- Python Agent：8000
- `java_client.py` 的 `BASE_URL` 指向 8080，不是 8000

### 坑 2：循环导入

```python
# app.py
from tools.query_project import QueryProjectTool
# tools/query_project.py
from app import app   # ❌ 循环
```

Tool 只依赖 `java_client`，不要反向依赖 `app`。依赖方向单向：app → tools → java_client。

### 坑 3：mock_java 和 app 抢同名变量

两个文件都有 `app = FastAPI()`，别在同一个进程跑。分两个 `uvicorn` 跑。

### 坑 4：环境变量没设

```bash
uvicorn app:app
# config.py 读不到 JAVA_BASE_URL → 用默认 localhost:8080
```

默认值兜底，但要清楚线上要传环境变量。别硬编码线上地址。

### 坑 5：Tool 里忘了 await

```python
async def run(self, project_id):
    return self._java.query_project(project_id)   # ❌ 没 await，返回协程不是结果
```

`JavaServiceClient` 的方法都是 async，必须 `await`。

### 坑 6：gather 里一个失败全崩

```python
project, task = await asyncio.gather(
    project_tool.run(...),
    task_tool.run(...),
)
# Java 挂了，两个都失败，抛异常
```

Agent 场景若要容错，用 `return_exceptions=True` 或分别 try。本阶段全失败返回 502 可接受。

### 坑 7：lifespan 没关 client

```python
@asynccontextmanager
async def lifespan(app):
    app.state.java = JavaServiceClient(...)
    yield
    # ❌ 忘 await app.state.java.close()
```

uvicorn 关闭时报 warning「Unclosed client」。补上 close。

### 坑 8：requirements.txt 不全或带版本不锁

- 漏装 `uvicorn` → `uvicorn` 命令找不到
- 不锁版本 → 别人还原装新版，API 变了挂掉

`pip freeze > requirements.txt` 一次，别手写。

## 6. 验收标准

### 第一阶段整体验收

完成以下全部，第一阶段（Python 底座）才算过关：

- [ ] 项目按 3.1 结构组织，`from tools import ...` 正常
- [ ] `requirements.txt` 锁版本，能从零 `pip install -r` 还原
- [ ] `.gitignore` 含 `.venv/` 和 `__pycache__/`
- [ ] Java mock + Python Agent 两个服务都起来
- [ ] `/agent/query` 端到端调通，返回项目 + tasks 整合数据
- [ ] 用 `asyncio.gather` 并发调两个 Java 接口（耗时验证）
- [ ] Java 端挂掉返回 502，不是 500 堆栈崩溃
- [ ] 请求体校验生效（非法 projectId → 422）
- [ ] `lifespan` 正确建/释放 `JavaServiceClient`
- [ ] README 含启动命令和还原步骤

### 最终产出

**Construction-Agent V0（Python 底座）**：

```text
前端/HTTP → Python FastAPI → httpx async → Java Spring Boot(mock) → 返回
```

能跑通端到端，依赖可从零还原。

### 进入下一阶段的前提

回到 [[第一阶段]] 的自测题，以及以下问题都能答：

- 能画出「前端 → Python → Java → 中间件」完整链路
- 能解释 Tool 内部为什么就是「httpx 调 Java」
- 能解释这条链路和后面 Tool Calling 的关系

## 7. 自测题

> 1. 画出「前端 → Python FastAPI → Java Spring Boot → 中间件」的完整链路，每一层在做什么？
>
> 2. `QueryProjectTool.run()` 内部本质在做什么？这和后面 Tool Calling 有什么关系？
>
> 3. 为什么「先打通 Python→Java」再学 LLM？不通会怎样？
>
> 4. 本阶段 Python 和 Java 各负责什么？（对应主计划第 9 章职责划分）
>
> 5. `app.py` 里 `lifespan` 建的 `JavaServiceClient`，为什么不能每个请求 new 一个？
>
> 6. `/agent/query` 里 `asyncio.gather` 并发调两个 Tool，如果其中一个失败会怎样？怎么容错？
>
> 7. Java 端挂掉，前端看到什么？为什么不是 500 堆栈？
>
> 8. 别人 clone 你的项目，怎么从零跑起来？（说出完整命令）
>
> 9. `requirements.txt` 为什么要锁版本？不锁线上会出什么问题？
>
> 10. 本阶段产出的 `Construction-Agent V0`，后面几周会在它上面叠加什么能力？（对应主计划 10.3 项目演进）
>
> 11. 如果把 mock_java 换成真实的 Spring Boot 服务，Python 端要改什么？
>
> 12. 今天打通的这条链路，后面接入 LLM 后，LLM 在链路的哪个位置？

---

**全部能答 + 项目跑通 → 第一阶段完成。**

进入 [[90.AI学习/Agent学习计划|Agent学习计划]] 第二阶段：LLM API（第 3 章）。
