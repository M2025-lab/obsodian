---
title: Day6：FastAPI
tags: [AI, Agent, Python, 第一阶段, Day6]
created: 2026-08-27
updated: 2026-08-27
---

# Day6：FastAPI（4 小时）

> 学习目标：用 FastAPI 把 Python 能力暴露成 HTTP API，作为 Java / 前端调 Agent 的入口。

## 1. 知识点清单

今天要掌握的核心知识：

```text
FastAPI()              应用实例
@app.get / @app.post    路由装饰器
Pydantic 请求体         自动校验（复用 Day3）
response_model          响应自动序列化
路径参数 / query 参数
Depends()               依赖注入
HTTPException           抛业务 HTTP 错误
BackgroundTasks         后台异步任务
async def 路由           原生异步
uvicorn                 ASGI 服务器启动
/docs                   内置 Swagger
lifespan                应用启动/关闭钩子
```

学完这些，你就能把 Python 能力暴露成 HTTP API——作为 Java / 前端调 Agent 的入口。

## 2. 怎么学

**时间分配**：

```text
0:00-0:30  FastAPI vs Spring Boot
0:30-1:30  路由 + 请求/响应模型（复用 Day3）
1:30-2:00  路径参数 / query 参数 / 异常
2:00-2:30  Depends 依赖注入
2:30-3:00  lifespan / BackgroundTasks
3:00-4:00  编码：完整 Agent API 骨架
```

**学习方法**：

1. 重点对比 Spring Boot 的 `@RestController` / `@Valid` / `@Autowired`
2. 复用 Day3 的 Pydantic 模型做请求/响应——感受 Pydantic 在 FastAPI 里的无缝集成
3. 每个路由写完立刻 `uvicorn` 起来，在 `/docs` 测
4. 重点理解 FastAPI 为什么天然 async

## 3. 知识点详解

### 3.1 FastAPI vs Spring Boot

| 概念          | Spring Boot              | FastAPI                |
| ----------- | ----------------------- | ---------------------- |
| 路由          | `@GetMapping`            | `@app.get`             |
| 请求体校验       | `@Valid` + DTO           | Pydantic BaseModel 自动 |
| 响应序列化       | Jackson                 | `response_model` 自动 |
| 依赖注入        | `@Autowired`            | `Depends()`            |
| 异步          | WebFlux / 虚拟线程           | 原生 `async def`         |
| 启动          | Tomcat 内嵌               | uvicorn (ASGI)         |
| API 文档      | Swagger（需引入）             | 内置 `/docs`             |

**为什么 Agent 用 FastAPI**：

- 原生 async → 路由直接 `async def`，`await` 调 LLM 不阻塞
- Pydantic 集成 → 请求/响应自动校验，复用 Day3 模型
- 内置 Swagger → Java 端看 `/docs` 就能对接

### 3.2 第一个接口

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Construction Agent API")

class QueryRequest(BaseModel):
    question: str
    projectId: str

class QueryResponse(BaseModel):
    answer: str
    data: dict

@app.post("/agent/query", response_model=QueryResponse)
async def query(req: QueryRequest):
    return QueryResponse(answer=f"你问的是 {req.question}", data={"projectId": req.projectId})
```

启动：

```bash
uvicorn main:app --reload --port 8000
# 打开 http://localhost:8000/docs
```

**发生了什么**：

1. 请求体 JSON → Pydantic `QueryRequest`（自动校验，复用 Day3）
2. 校验失败 → 自动返回 422 + 错误详情
3. 业务逻辑返回 `QueryResponse` → `response_model` 自动序列化成 JSON

> 对比 Java：要写 `@RestController` + `@PostMapping` + `@Valid @RequestBody` + DTO。FastAPI 一个函数 + 两个模型搞定。

### 3.3 路径参数 / query 参数

```python
# 路径参数
@app.get("/project/{project_id}")
async def get_project(project_id: str):
    return {"projectId": project_id}

# query 参数
@app.get("/task")
async def list_task(projectId: str, status: str | None = None):
    return {"projectId": projectId, "status": status}

# 混合：路径 + query + 请求体
@app.put("/project/{project_id}")
async def update(project_id: str, force: bool = False, body: UpdateReq = None):
    ...
```

**参数类型决定来源**：

| 参数声明                  | 来源        |
| --------------------- | --------- |
| 在路径 `{project_id}`    | 路径参数      |
| 简单类型 `str/int/bool`   | query 参数  |
| `BaseModel`           | 请求体       |
| `Depends(...)`        | 依赖注入      |

### 3.4 异常 → HTTP 错误

```python
from fastapi import HTTPException

@app.get("/project/{project_id}")
async def get_project(project_id: str):
    if not project_id.isdigit():
        raise HTTPException(status_code=400, detail="projectId 必须是数字")
    # 模拟找不到
    if project_id == "0":
        raise HTTPException(status_code=404, detail="项目不存在")
    return {"projectId": project_id}
```

**全局异常处理**（把业务异常统一转 HTTP）：

```python
from fastapi import Request
from fastapi.responses import JSONResponse
from java_client import JavaServiceError

@app.exception_handler(JavaServiceError)
async def java_error_handler(request: Request, exc: JavaServiceError):
    return JSONResponse(
        status_code=502,
        content={"error": "JavaServiceError", "detail": str(exc)},
    )
```

> 对比 Java：`@ControllerAdvice` + `@ExceptionHandler`。FastAPI 的 `@app.exception_handler` 同理。

### 3.5 Depends —— 依赖注入

```python
from fastapi import Depends

def get_java_client() -> JavaServiceClient:
    return JavaServiceClient(base_url="http://localhost:8080", token="xxx")

@app.post("/agent/query")
async def query(req: QueryRequest, java: JavaServiceClient = Depends(get_java_client)):
    project = await java.query_project(req.projectId)
    return {"project": project}
```

**为什么用 Depends**：

- **复用**：多个路由共享同一个依赖（如鉴权、客户端）
- **测试**：测试时注入 mock 依赖
- **生命周期**：FastAPI 自动调 `get_java_client`，用完自动释放

> 对比 Java `@Autowired`：注入的是 Spring 容器管理的 bean。FastAPI 的 Depends 是**函数**，更灵活，但默认每次请求都新建——长生命周期的 client 要用 `lifespan` 管理（见 3.7）。

### 3.6 Depends 的依赖链

```python
def get_token(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "未授权")
    return authorization[7:]

def get_java_client(token: str = Depends(get_token)) -> JavaServiceClient:
    return JavaServiceClient(base_url="...", token=token)

@app.get("/project/{pid}")
async def get_project(pid: str, java: JavaServiceClient = Depends(get_java_client)):
    # 依赖链：get_java_client → get_token → 解析鉴权头
    return await java.query_project(pid)
```

> 对比 Java：一层层 `@Autowired`。FastAPI 的 Depends 可以嵌套，FastAPI 自动解析顺序。

### 3.7 lifespan —— 应用级生命周期

`Depends` 是**每请求**新建。但 `AsyncClient` 要复用，应该在应用启动时建、关闭时释放：

```python
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动：建连接池
    app.state.java_client = JavaServiceClient(base_url="http://localhost:8080")
    yield
    # 关闭：释放连接池
    await app.state.java_client.close()

app = FastAPI(lifespan=lifespan)

def get_java_client(request: Request) -> JavaServiceClient:
    return request.app.state.java_client

@app.get("/project/{pid}")
async def get_project(pid: str, java: JavaServiceClient = Depends(get_java_client)):
    return await java.query_project(pid)
```

> 对比 Java：`@PostConstruct` 建资源，`@PreDestroy` 释放。FastAPI 的 lifespan 等价。

### 3.8 BackgroundTasks —— 后台任务

```python
from fastapi import BackgroundTasks

async def write_log(project_id: str):
    await asyncio.sleep(1)   # 慢操作
    print(f"已记录 {project_id}")

@app.post("/agent/query")
async def query(req: QueryRequest, bg: BackgroundTasks):
    bg.add_task(write_log, req.projectId)   # 不阻塞响应
    return {"status": "accepted"}
```

> 对比 Java：`@Async` / 线程池提交任务。FastAPI 的 BackgroundTasks 在响应返回**之后**异步执行，适合日志、通知。

## 4. 编码练习

建 `app.py`，用 Day3 模型 + Day5 的 JavaServiceClient 写 Agent API 骨架。

### 任务 1：模型复用

```python
from models import ProjectQuery, Project
```

直接复用 Day3 的 Pydantic 模型做请求/响应。

### 任务 2：应用 + lifespan

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from java_client import JavaServiceClient

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.java = JavaServiceClient(base_url="http://localhost:8080", token="test")
    yield
    await app.state.java.close()

app = FastAPI(title="Construction Agent", lifespan=lifespan)
```

### 任务 3：查询接口

```python
from fastapi import Depends, HTTPException
import asyncio

def get_java(request: Request) -> JavaServiceClient:
    return request.app.state.java

@app.post("/agent/query", response_model=Project)
async def query(req: ProjectQuery, java: JavaServiceClient = Depends(get_java)):
    try:
        project, task = await asyncio.gather(
            java.query_project(req.projectId),
            java.query_task(req.projectId),
        )
        project["tasks"] = task.get("tasks", [])
        return Project.model_validate(project)
    except Exception as e:
        raise HTTPException(status_code=502, detail=str(e))
```

### 任务 4：错误接口

```python
@app.get("/project/{pid}")
async def get_project(pid: str, java: JavaServiceClient = Depends(get_java)):
    if not pid.isdigit():
        raise HTTPException(400, "projectId 必须是数字")
    try:
        return await java.query_project(pid)
    except Exception as e:
        raise HTTPException(502, detail=str(e))
```

### 任务 5：跑通 + Swagger

```bash
uvicorn app:app --reload --port 8000
# 打开 http://localhost:8000/docs
```

在 `/docs` 里：

- POST `/agent/query`，body 填 `{"projectId":"826051217000001469"}` → 返回项目数据
- GET `/project/abc` → 400
- 不启动 Java mock → 502

### 任务 6：全局异常处理（选做）

注册 `JavaServiceError` 的 handler，统一返回 502，不用每个路由 try/except。

### 验证

- `/docs` 能看到所有接口
- 请求体校验生效（缺字段返回 422 + 详情）
- `response_model` 序列化生效
- Java 端挂掉返回 502 而不是 500 崩溃

## 5. 常见坑

### 坑 1：路由函数写成同步 def

```python
@app.get("/project/{pid}")
def get_project(pid: str):           # ❌ 同步，阻塞事件循环
    return requests.get(...)         # 不能 await
```

**正确**：路由用 `async def`，内部用异步库。如果非要调同步库，用普通 `def`（FastAPI 会放到线程池跑，不阻塞事件循环）——但 Agent 场景几乎都是 async。

### 坑 2：在路由里 new JavaServiceClient

```python
@app.get("/project/{pid}")
async def get(pid: str):
    java = JavaServiceClient(...)    # ❌ 每次请求新建，连接不复用
```

用 `lifespan` 在应用级建一次，`Depends` 取出来复用。

### 坑 3：忘写 `response_model`

```python
@app.post("/query")
async def query(req): return some_dict   # 字段不受控，可能泄露内部字段
```

写 `response_model=Project`，输出自动按模型序列化、过滤多余字段。

### 坑 4：异常没转成 HTTPException

```python
@app.get("/project/{pid}")
async def get(pid):
    return await java.query_project(pid)   # Java 挂了 → 500 + 堆栈
```

要么 `try/except` 抛 `HTTPException`，要么注册全局 exception_handler。别让 500 把堆栈抛给前端。

### 坑 5：请求体校验以为是手动

```python
@app.post("/query")
async def query(req: QueryRequest):    # 不需要 @Valid，Pydantic 自动校验
```

FastAPI 看到 `BaseModel` 参数就自动校验。失败返回 422，不用手动 if。

### 坑 6：lifespan 里忘 close

```python
@asynccontextmanager
async def lifespan(app):
    app.state.java = JavaServiceClient(...)
    yield
    # ❌ 忘了 await app.state.java.close() → 连接泄漏
```

`yield` 之后必须释放资源。

### 坑 7：端口冲突 / uvicorn 路径错

```bash
uvicorn app:app --port 8000    # app:app = 文件名:变量名
uvicorn src.app:app            # 在子目录要带包路径
```

`uvicorn` 的参数是 `模块:app变量`，不是文件路径。

### 坑 8：`--reload` 在生产用

`--reload` 是开发热重载，**生产别用**（性能差）。生产用 `uvicorn app:app --workers 4`（多进程）。

## 6. 验收标准

- [ ] 能解释 FastAPI 为什么天然 async
- [ ] 能用 Pydantic 模型做请求体（自动校验）和 `response_model`（自动序列化）
- [ ] 能用路径参数 / query 参数 / 请求体
- [ ] 能用 `HTTPException` 抛业务错误，或注册全局 exception_handler
- [ ] 能用 `Depends` 做依赖注入，理解依赖链
- [ ] 能用 `lifespan` 管理应用级资源（建/释放 client）
- [ ] 能在 `/docs` 调通所有接口
- [ ] 编码练习 5 个任务跑通

**最终产出**：`app.py`，含 `/agent/query` 等接口，能调 Java mock 并返回。这是 Day7 整合的 Python 端入口。

## 7. 自测题

> 1. FastAPI 为什么天然 async？路由用 `def` 和 `async def` 有什么区别？
>
> 2. FastAPI 怎么知道哪个参数是请求体、哪个是 query 参数、哪个是路径参数？
>
> 3. 请求体校验需要 `@Valid` 吗？为什么不用？
>
> 4. `response_model` 不写会怎样？写了有什么好处？
>
> 5. 校验失败返回什么状态码？响应体长什么样？
>
> 6. `Depends` 和 Java 的 `@Autowired` 有什么异同？Depends 默认每次请求都新建吗？
>
> 7. 长生命周期的 `AsyncClient` 为什么不能用 `Depends` 每次新建？该用什么管？
>
> 8. `lifespan` 对应 Java 的什么？`yield` 前后分别做什么？
>
> 9. `HTTPException` 和全局 `@app.exception_handler` 各适合什么场景？
>
> 10. 路由里直接 `return await java.query_project()`，Java 挂了会怎样？怎么改？
>
> 11. `BackgroundTasks` 对应 Java 什么？适合做什么？不适合做什么？
>
> 12. `uvicorn app:app --reload` 里 `app:app` 是什么意思？生产为什么不能用 `--reload`？

全部能答 → 进入 [[day7]]。
