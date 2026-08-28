---
title: Day5：HTTP（httpx async）
tags: [AI, Agent, Python, 第一阶段, Day5]
created: 2026-08-27
updated: 2026-08-27
---

# Day5：HTTP —— httpx 异步调用 Java（4 小时）

> 学习目标：用 httpx 异步调用 Java Spring Boot 接口，封装成可复用 Client。这是 Tool 调用 Java 服务的底层通道。

## 1. 知识点清单

今天要掌握的核心知识：

```text
httpx.AsyncClient      异步 HTTP 客户端
GET / POST / PUT / DELETE
params                 query 参数
json                   请求体（自动序列化）
headers                鉴权 / Content-Type
timeout                超时
raise_for_status       状态码非 2xx 抛异常
async with             连接池生命周期
重试（tenacity 或手动）
JavaServiceClient 封装
```

学完这些，你就能封装 Tool 调用 Java 服务的底层通道——这是 Python Agent → Java 微服务的关键一环。

## 2. 怎么学

**准备**：今天需要一个能调的 HTTP 服务。两个选择：

1. 有真实的 Java Spring Boot 服务就调它
2. 没有就起一个 FastAPI mock（Day6 会学，今天先用现成代码）

**mock 服务**（存 `mock_java.py`，另开终端 `uvicorn mock_java:app --port 8080` 跑）：

```python
from fastapi import FastAPI
app = FastAPI()

@app.get("/project/{pid}")
async def get_project(pid: str):
    return {"projectId": pid, "name": "XX项目", "notifiable": True}

@app.get("/task")
async def get_task(projectId: str):
    return {"projectId": projectId, "tasks": [{"taskId": "t1", "canNotify": True}]}

@app.post("/notify/submit")
async def submit(projectId: str, taskIds: list):
    return {"success": True, "projectId": projectId, "submitted": taskIds}
```

**时间分配**：

```text
0:00-0:30  httpx vs requests / 同步 vs 异步
0:30-1:30  AsyncClient GET / POST / 异常
1:30-2:00  连接池 / async with / 超时
2:00-2:30  重试 / 鉴权头
2:30-4:00  封装 JavaServiceClient + 并发调用
```

**学习方法**：

1. 先用同步 `httpx.get` 跑通，再换 `AsyncClient`
2. 每个方法都 `raise_for_status()` 看错误分支
3. 重点封装成可复用 `JavaServiceClient`
4. 结合 Day4 的 `asyncio.gather` 并发调多个 Java 接口

## 3. 知识点详解

### 3.1 为什么选 httpx 不选 requests

| 特性      | requests     | httpx                |
| ------- | ------------ | -------------------- |
| 异步      | 不支持（阻塞事件循环）  | 原生 `AsyncClient`     |
| HTTP/2  | 不支持          | 支持                   |
| API 兼容  | —            | 几乎同 requests，迁移成本极低   |
| 连接复用    | `Session`     | `AsyncClient` 长连接    |
| 超时      | 全局/请求级        | 同                    |

**最关键**：`requests` 是同步库，在 async 函数里调会**卡死事件循环**（Day4 坑 3）。Agent 全是 async，必须用 `httpx.AsyncClient`。

> Java 对比：`requests` 像 `RestTemplate`（同步阻塞），`httpx.AsyncClient` 像 `WebClient`（异步响应式）。

### 3.2 第一个 AsyncClient

```python
import asyncio
import httpx

async def query_project(pid: str):
    async with httpx.AsyncClient(base_url="http://localhost:8080", timeout=5.0) as client:
        resp = await client.get(f"/project/{pid}")
        resp.raise_for_status()         # 4xx/5xx 抛 HTTPStatusError
        return resp.json()

asyncio.run(query_project("826051217000001469"))
```

**`async with` 的作用**：

```text
AsyncClient 内部维护连接池
async with 确保用完释放连接
不释放 → 连接泄漏 → Java 端连接池打满
```

类似 Java 的 try-with-resources 关 `CloseableHttpClient`。

### 3.3 GET / POST / 参数

```python
# GET 带 query 参数
resp = await client.get("/task", params={"projectId": "123", "status": "notify"})

# POST JSON 请求体（自动序列化 + Content-Type: application/json）
resp = await client.post("/notify/submit", json={
    "projectId": "123",
    "taskIds": ["t1", "t2"],
})

# POST 表单
resp = await client.post("/login", data={"user": "admin", "pwd": "x"})

# 带鉴权头
resp = await client.get(
    "/project/123",
    headers={"Authorization": "Bearer eyJ..."},
)
```

**`resp` 常用方法**：

```python
resp.status_code       # 200 / 404 / 500
resp.json()            # 解析 JSON 成 dict（类似 FastJSON.parseObject）
resp.text              # 原始文本
resp.headers           # 响应头
resp.raise_for_status()  # 非 2xx 抛异常
```

### 3.4 异常体系

```python
import httpx

try:
    resp = await client.get("/project/123", timeout=2.0)
    resp.raise_for_status()
except httpx.ConnectError:
    print("连接失败（Java 服务挂了）")
except httpx.ReadTimeout:
    print("读取超时")
except httpx.HTTPStatusError as e:
    print(f"HTTP 错误 {e.response.status_code}: {e.response.text}")
except httpx.RequestError as e:
    print(f"其他请求错误: {e}")
```

| 异常                  | 触发场景          |
| ------------------- | ------------- |
| `ConnectError`      | 连不上服务器        |
| `ReadTimeout`       | 读取超时          |
| `ConnectTimeout`    | 连接超时          |
| `HTTPStatusError`   | 状态码非 2xx（需手动 raise_for_status） |
| `RequestError`      | 所有请求错误的基类     |

> **关键**：4xx/5xx **不会自动抛异常**，必须调 `raise_for_status()`。否则你以为成功，实际拿到的是错误响应体。

### 3.5 超时配置

```python
# 全局超时
async with httpx.AsyncClient(timeout=5.0) as client: ...

# 分阶段超时
async with httpx.AsyncClient(timeout=httpx.Timeout(
    connect=2.0,    # 连接 2s
    read=10.0,      # 读取 10s
    write=2.0,      # 发送 2s
    pool=1.0,       # 从连接池拿连接 1s
)) as client: ...
```

> Agent 调 LLM / Java 接口都要设超时，避免一个慢接口拖死整个请求。

### 3.6 连接复用（性能关键）

```python
# ❌ 每次新建 client（连接不复用，慢）
async def query1():
    async with httpx.AsyncClient() as client:
        return await client.get(url)

# ✅ 复用同一个 client（长连接，快）
class JavaServiceClient:
    def __init__(self, base_url):
        self._client = httpx.AsyncClient(base_url=base_url, timeout=5.0)

    async def query(self, pid):
        return await self._client.get(f"/project/{pid}")

    async def close(self):
        await self._client.aclose()    # 程序退出前关
```

> 对比 Java：一个 `CloseableHttpClient` 配连接池复用，和这个一样。Agent 长期运行，**必须复用 client**，不能每次 new。

### 3.7 重试

httpx 不自带重试。两种方式：

**手动重试**（简单）：

```python
async def call_with_retry(client, url, retries=3):
    for i in range(retries):
        try:
            resp = await client.get(url)
            resp.raise_for_status()
            return resp.json()
        except httpx.RequestError as e:        # 网络错误才重试
            if i == retries - 1:
                raise
            await asyncio.sleep(0.5 * (i + 1))  # 退避
```

**tenacity**（推荐，功能强）：

```python
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=0.5),
    retry=retry_if_exception_type(httpx.RequestError),  # 只重试网络错误
)
async def query_project(client, pid):
    resp = await client.get(f"/project/{pid}")
    resp.raise_for_status()
    return resp.json()
```

> **铁律**：只重试**网络错误**（`RequestError`），**不重试 4xx**（业务错误重试也是失败）。5xx 视情况重试。

### 3.8 并发调用多个接口

结合 Day4 的 gather：

```python
async def fetch_all(client, pid):
    project, task, rule = await asyncio.gather(
        client.get(f"/project/{pid}"),
        client.get("/task", params={"projectId": pid}),
        client.get("/rule"),
    )
    return project.json(), task.json(), rule.json()
```

**同一个 AsyncClient 并发多个请求**：连接池会自动复用，性能最佳。这是 Agent 同时调多个 Tool 的典型模式。

## 4. 编码练习

建 `java_client.py`，封装 `JavaServiceClient`。

### 任务 1：客户端类 + 鉴权头

```python
import httpx
import asyncio
from typing import Optional

class JavaServiceClient:
    def __init__(self, base_url: str, token: Optional[str] = None):
        headers = {"Authorization": f"Bearer {token}"} if token else {}
        self._client = httpx.AsyncClient(
            base_url=base_url.rstrip("/"),
            timeout=httpx.Timeout(connect=2.0, read=5.0),
            headers=headers,
        )

    async def close(self):
        await self._client.aclose()
```

### 任务 2：三个业务方法

```python
    async def query_project(self, project_id: str) -> dict:
        resp = await self._client.get(f"/project/{project_id}")
        resp.raise_for_status()
        return resp.json()

    async def query_task(self, project_id: str) -> dict:
        resp = await self._client.get(
            "/task", params={"projectId": project_id}
        )
        resp.raise_for_status()
        return resp.json()

    async def submit_notify(self, project_id: str, task_ids: list[str]) -> dict:
        resp = await self._client.post(
            "/notify/submit",
            json={"projectId": project_id, "taskIds": task_ids},
        )
        resp.raise_for_status()
        return resp.json()
```

### 任务 3：统一异常转换

把 httpx 异常转成业务可读错误：

```python
class JavaServiceError(Exception):
    pass

async def safe_call(coro):
    try:
        return await coro
    except httpx.ConnectError:
        raise JavaServiceError("Java 服务连不上")
    except httpx.ReadTimeout:
        raise JavaServiceError("Java 服务超时")
    except httpx.HTTPStatusError as e:
        raise JavaServiceError(f"Java 返回 {e.response.status_code}: {e.response.text}")
```

### 任务 4：并发调用 + 超时

```python
async def main():
    client = JavaServiceClient("http://localhost:8080", token="test-token")
    pid = "826051217000001469"
    try:
        # 并发查项目 + 任务，整体超时 3 秒
        project, task = await asyncio.wait_for(
            asyncio.gather(
                safe_call(client.query_project(pid)),
                safe_call(client.query_task(pid)),
            ),
            timeout=3.0,
        )
        print("项目:", project)
        print("任务:", task)
    except asyncio.TimeoutError:
        print("整体超时")
    except JavaServiceError as e:
        print(f"业务错误: {e}")
    finally:
        await client.close()

asyncio.run(main())
```

### 任务 5：重试（选做）

用 `tenacity` 给 `query_project` 加重试：网络错误重试 3 次，指数退避。

### 验证

- 正常调 mock 服务能返回数据
- 把 base_url 改成错端口 → 打印「Java 服务连不上」
- 把超时改成 0.01 → 触发超时
- 并发调用耗时远小于串行

## 5. 常见坑

### 坑 1：用 requests 而不是 httpx

```python
async def query():
    resp = requests.get(url)   # ❌ 阻塞事件循环
```

async 函数里**绝不用 requests**。用 `httpx.AsyncClient`。

### 坑 2：忘了 raise_for_status

```python
resp = await client.get("/project/123")
return resp.json()   # ❌ 404 时返回的是错误页 JSON，你以为成功了
```

必须 `resp.raise_for_status()` 才能把 4xx/5xx 转成异常。

### 坑 3：每次 new AsyncClient

```python
async def query(url):
    async with httpx.AsyncClient() as client:   # ❌ 不复用，慢
        return await client.get(url)
```

长期运行的 Agent 要复用同一个 client 实例（连接池）。只有在「短脚本」里才每次 new。

### 坑 4：忘了 close client

```python
client = httpx.AsyncClient()
# 用完没 aclose() → 连接泄漏
```

要么 `async with`，要么程序结束前 `await client.aclose()`。

### 坑 5：重试了业务错误

```python
@retry(retry=retry_if_exception_type(httpx.HTTPStatusError))  # ❌ 400 也重试
```

4xx 是业务错误，重试还是失败。只重试 `httpx.RequestError`（网络层）。5xx 看情况。

### 坑 6：超时设太大或不设

```python
client = httpx.AsyncClient()   # 默认 5s，但没有 read 超时控制
```

LLM / Java 接口都要显式设超时。一个慢接口不超时会拖垮整个 gather。

### 坑 7：base_url 末尾斜杠

```python
base_url="http://localhost:8080/"
# get("/project/123") → http://localhost:8080//project/123  双斜杠
```

`rstrip("/")` 去掉末尾斜杠，或拼接时注意。

### 坑 8：JSON 解析失败

```python
data = resp.json()   # 如果 Java 返回非 JSON（如 HTML 错误页），抛 JSONDecodeError
```

线上 Java 服务可能返回 502 的 HTML 页。`raise_for_status` 先拦截状态码，再解析 JSON 更稳。

## 6. 验收标准

- [ ] 能解释为什么用 `httpx.AsyncClient` 而不是 `requests`
- [ ] 能用 `async with` 或复用 client，并正确 `aclose`
- [ ] 能写带 `params` / `json` / `headers` / `timeout` 的请求
- [ ] 能解释为什么必须调 `raise_for_status()`（4xx 不自动抛）
- [ ] 能区分 `httpx.RequestError` 和 `httpx.HTTPStatusError`，知道该重试哪个
- [ ] 能用 `asyncio.gather` 在同一个 client 上并发调多个 Java 接口
- [ ] 能把 httpx 异常转成业务可读错误
- [ ] 编码练习 4 个任务跑通，连接失败 / 超时 / 正常三种情况都能正确处理

**最终产出**：`java_client.py`，封装了 `JavaServiceClient`，含 `query_project` / `query_task` / `submit_notify`，带超时、鉴权、异常转换。这是 Day7 整合的关键组件。

## 7. 自测题

> 1. 为什么 async 函数里不能用 `requests`？用 `requests` 会发生什么？
>
> 2. `httpx.AsyncClient` 和 Java 的 `WebClient` / `RestTemplate` 分别对应什么关系？
>
> 3. `async with httpx.AsyncClient() as client` 保证了什么？不写会怎样？
>
> 4. 为什么必须调 `raise_for_status()`？不调会怎样？4xx 默认抛异常吗？
>
> 5. `params` 和 `json` 参数分别用来传什么？
>
> 6. `httpx.RequestError` 和 `httpx.HTTPStatusError` 有什么区别？重试时该重试哪个？为什么 4xx 不重试？
>
> 7. 为什么长期运行的 Agent 要复用同一个 `AsyncClient` 实例？
>
> 8. 连接超时、读取超时、整体超时（`asyncio.wait_for`）有什么区别？分别防什么？
>
> 9. 怎么用同一个 client 并发调 3 个 Java 接口？为什么要用同一个？
>
> 10. Java 端返回 502 的 HTML 错误页，你的代码会怎样？怎么防？
>
> 11. base_url 末尾带斜杠会有什么问题？
>
> 12. 写一个带重试的 `query_project`：网络错误重试 3 次，指数退避，业务错误不重试。

全部能答 → 进入 [[day6]]。
