---
title: Day4：async / await
tags: [AI, Agent, Python, 第一阶段, Day4]
created: 2026-08-27
updated: 2026-08-27
---

# Day4：async / await（4 小时）

> 学习目标：理解 Python 异步并发，能用 asyncio.gather 并发执行多个协程。这是 Agent 并发调 Tool 的核心。

## 1. 知识点清单

今天要掌握的核心知识：

```text
协程 (coroutine)     async def 定义的函数
await                等待一个 awaitable
事件循环 (event loop) 单线程调度器
asyncio.run          启动事件循环跑顶层协程
asyncio.gather       并发执行多个协程（汇总结果）
asyncio.create_task  把协程包成 Task 并发跑
asyncio.sleep        异步 sleep（不阻塞事件循环）
asyncio.wait_for     超时控制
as_completed         谁先完成谁先返回
阻塞 vs 非阻塞        为什么不能用 time.sleep / requests
async context manager  async with
```

学完这些，你就能用 `asyncio.gather` 并发调多个 Tool——这是 Agent 并发的核心。

## 2. 怎么学

**时间分配**：

```text
0:00-0:30  同步 vs 异步（为什么需要 async）
0:30-1:00  async def / await 基础
1:00-1:30  asyncio.run / 事件循环
1:30-2:30  asyncio.gather 并发（重点）
2:30-3:00  超时 / Task / 阻塞陷阱
3:00-4:00  编码练习：并发调多个 Tool
```

**学习方法**：

1. **先建立心智模型**：async 不是多线程，是单线程内切换任务——这是 Java 转 Python 最难扭过来的认知
2. 每个例子都跑，**打印耗时**对比同步/异步差异
3. 重点对比 Java 的 `CompletableFuture`
4. 故意写阻塞代码（`time.sleep`）观察事件循环卡死
5. 编码练习模拟「Agent 并发调 3 个 Tool」

## 3. 知识点详解

### 3.1 为什么 Agent 需要 async

场景：用户问「查项目 123 的通知任务 + 复尺结果 + 业务规则」。

```text
同步写法：
query_project()  ──200ms──►
query_task()      ──200ms──►   总 600ms

异步写法：
await gather(query_project(), query_task(), query_rule())
   └ 三个并行，总 200ms
```

Agent 里这种「同时调多个 Tool」太常见，async 是刚需。

### 3.2 async 不是多线程（核心认知）

**这是今天最重要的一句话**：

> **async 是单线程 + 事件循环切换任务，不是多线程。**

```text
多线程（Java 经典）：
  线程1: query_project ──IO 等待──►
  线程2: query_task     ──IO 等待──►
  → 多个 OS 线程，每个阻塞等待，靠线程数换并发

async（Python）：
  单线程，事件循环：
    跑 query_project → 遇到 IO → 让出 → 跑 query_task → 遇到 IO → 让出 → 回来收结果
  → 一个线程，IO 等待时切去干别的，靠切换换并发
```

**为什么 Python 选 async 而不是多线程**：

- Python 有 GIL（全局解释器锁），多线程在 CPU 密集任务上没优势
- IO 密集场景（LLM 请求、HTTP 调用），async 单线程就够，省线程开销
- LLM Agent 几乎全是 IO（等模型、等接口），async 完美匹配

**关键理解**：async 只对 **IO 密集**有用。CPU 密集（如算 sha256）该用多进程（`multiprocessing`），async 帮不了。

### 3.3 第一个 async 函数

```python
import asyncio

async def query_project(project_id):      # async def 定义协程
    print(f"开始查 {project_id}")
    await asyncio.sleep(0.2)              # await 等待异步操作
    print(f"查完 {project_id}")
    return f"项目 {project_id} 数据"

# 调用 async 函数不能直接 query_project("123")
# 它返回的是「协程对象」，不会执行
coro = query_project("123")
print(coro)        # <coroutine object query_project at 0x...>

# 必须用事件循环跑它
asyncio.run(query_project("123"))
```

**三个铁律**：

1. `async def` 定义的函数，调用时**返回协程对象，不执行**。
2. 协程必须被 `await` 或 `asyncio.run` 或包成 `Task` 才会跑。
3. `await` 只能在 `async def` 函数里用。

### 3.4 asyncio.run —— 启动事件循环

```python
async def main():
    result = await query_project("123")
    print(result)

asyncio.run(main())     # 顶层入口，创建事件循环跑完 main 再关
```

`asyncio.run` 是程序的**唯一顶层入口**——它自己创建事件循环。一个程序通常只有一个 `asyncio.run`，在 `main()` 最外层。

> 对比 Java：Java 的 `CompletableFuture` 不需要「启动事件循环」，因为 JVM有线程池。Python 的 async 必须有事件循环驱动，`asyncio.run` 就是启动器。

### 3.5 asyncio.gather —— 并发汇总（重点）

```python
import asyncio
import time

async def query_project(pid):
    await asyncio.sleep(0.2)
    return f"项目{pid}"

async def query_task(pid):
    await asyncio.sleep(0.2)
    return f"任务{pid}"

async def query_rule():
    await asyncio.sleep(0.2)
    return "规则"

async def main():
    start = time.time()
    # 并发三个
    project, task, rule = await asyncio.gather(
        query_project("123"),
        query_task("123"),
        query_rule(),
    )
    print(f"耗时 {time.time()-start:.2f}s")   # ≈0.2s，不是 0.6s
    print(project, task, rule)

asyncio.run(main())
```

**Java 对比**：

| Python                  | Java                          |
| ----------------------- | ----------------------------- |
| `asyncio.gather(a, b, c)` | `CompletableFuture.allOf(cf1, cf2, cf3)` |
| `await gather(...)`     | `.join()` 等全部完成            |
| 单线程事件循环            | ForkJoinPool 线程池             |

**`gather` 的顺序**：返回值顺序和传入顺序一致，不管谁先完成。

**异常处理**：

```python
# 默认：一个抛异常，整个 gather 抛
try:
    await asyncio.gather(query_project("123"), query_task("123"))
except Exception as e:
    print(e)

# return_exceptions=True：异常不抛，作为结果返回
results = await asyncio.gather(
    query_project("123"),
    fail_sometimes(),
    return_exceptions=True,
)
# results[1] 可能是 Exception 对象，而不是抛出
```

> Agent 场景常用 `return_exceptions=True`：一个 Tool 失败不影响其他 Tool，最后统一处理。

### 3.6 asyncio.create_task —— 手动并发

```python
async def main():
    # 不 await，先包成 Task，让它在后台跑
    task1 = asyncio.create_task(query_project("123"))
    task2 = asyncio.create_task(query_task("123"))

    # 这里有事件循环驱动，两个 Task 并发执行
    # 可以做点别的
    print("两个任务已提交")

    # 现在等结果
    p = await task1
    t = await task2
```

**`gather` vs `create_task`**：

- `gather`：一行搞定，返回值聚合。**99% 场景用这个。**
- `create_task`：更细粒度控制——先提交、中间干别的、后 await。复杂调度才用。

### 3.7 阻塞陷阱（最大坑）

```python
import time

async def bad_query():
    time.sleep(1)      # ❌ 同步阻塞，卡死整个事件循环！
    return "结果"

async def main():
    await asyncio.gather(bad_query(), bad_query(), bad_query())
    # 不是 1s，是 3s！因为 time.sleep 不让出事件循环

asyncio.run(main())
```

**为什么**：`time.sleep` 是同步阻塞，它不告诉事件循环「我让出」，所以事件循环干瞪眼等它睡完，别的协程全卡住。

**正确**：

```python
async def good_query():
    await asyncio.sleep(1)   # ✅ 异步 sleep，让出事件循环
    return "结果"
```

**致命推论**：在 async 函数里用 `requests.get(...)`（同步 HTTP 库）也会卡死事件循环。**必须用异步库**（`httpx.AsyncClient`，Day5 详讲）。

> 这是 Java 转 Python 最容易犯的错：Java 里阻塞 IO 无所谓（有线程池兜底），Python async 里阻塞 IO 会拖垮整个事件循环。

### 3.8 async with —— 异步上下文管理

```python
async with httpx.AsyncClient() as client:
    resp = await client.get(url)
# 离开 with 块自动关闭 client（释放连接池）
```

类似 Java 的 try-with-resources，但用于异步资源（连接池、文件、锁）。Day5 的 httpx 就要这么用。

### 3.9 超时控制

```python
# 单个超时
try:
    result = await asyncio.wait_for(query_project("123"), timeout=2.0)
except asyncio.TimeoutError:
    print("查询超时")

# gather 整体超时
try:
    await asyncio.wait_for(
        asyncio.gather(query_project("123"), query_task("123")),
        timeout=3.0,
    )
except asyncio.TimeoutError:
    print("整体超时")
```

> Agent 调 LLM 必须设超时，否则 LLM 卡住会拖死整个请求。Tool 也要设超时。

## 4. 编码练习

建 `async_tools.py`，把 Day1 的 Tool 改成 async。

### 任务 1：async Tool 基类

```python
import asyncio
import time

class AsyncTool:
    def __init__(self, name: str):
        self.name = name

    async def run(self, input: dict) -> str:
        raise NotImplementedError
```

### 任务 2：三个异步 Tool（模拟网络 IO）

```python
async def query_project(pid: str) -> dict:
    await asyncio.sleep(0.2)        # 模拟 HTTP 等待
    return {"projectId": pid, "name": "XX项目"}

async def query_task(pid: str) -> dict:
    await asyncio.sleep(0.2)
    return {"projectId": pid, "tasks": ["t1", "t2"]}

async def query_rule() -> str:
    await asyncio.sleep(0.2)
    return "通知规则：复尺通过且未被通知"
```

### 任务 3：并发调用对比

```python
async def sync_style():
    """串行调用"""
    start = time.time()
    p = await query_project("826051217000001469")
    t = await query_task("826051217000001469")
    r = await query_rule()
    print(f"串行耗时 {time.time()-start:.2f}s")    # ≈0.6s
    return p, t, r

async def async_style():
    """并发调用"""
    start = time.time()
    p, t, r = await asyncio.gather(
        query_project("826051217000001469"),
        query_task("826051217000001469"),
        query_rule(),
    )
    print(f"并发耗时 {time.time()-start:.2f}s")    # ≈0.2s
    return p, t, r

async def main():
    await sync_style()
    await async_style()

asyncio.run(main())
```

### 任务 4：return_exceptions=True

```python
async def maybe_fail():
    await asyncio.sleep(0.1)
    raise ValueError("故意失败")

async def main():
    results = await asyncio.gather(
        query_project("123"),
        maybe_fail(),
        query_rule(),
        return_exceptions=True,
    )
    for i, r in enumerate(results):
        if isinstance(r, Exception):
            print(f"[{i}] 失败: {r}")
        else:
            print(f"[{i}] 成功: {r}")

asyncio.run(main())
```

### 任务 5：超时控制

```python
async def slow_tool():
    await asyncio.sleep(5)
    return "慢结果"

async def main():
    try:
        await asyncio.wait_for(slow_tool(), timeout=1.0)
    except asyncio.TimeoutError:
        print("Tool 超时，降级处理")

asyncio.run(main())
```

### 验证

- 串行 ≈ 0.6s，并发 ≈ 0.2s（3 倍差距）
- `return_exceptions=True` 时失败的不影响成功的
- 超时能被捕获并降级

## 5. 常见坑

### 坑 1：直接调用 async 函数

```python
result = query_project("123")    # ❌ 返回协程对象，没执行
print(result)                    # <coroutine object>
```

必须 `await` 或 `asyncio.run`。IDE 有时会警告「coroutine never awaited」。

### 坑 2：在同步函数里 await

```python
def normal_func():
    result = await query_project("123")   # ❌ SyntaxError: await outside async
```

`await` 只能在 `async def` 里。要从同步代码调 async，用 `asyncio.run(coro)`。

### 坑 3：在 async 里用同步阻塞（最致命）

```python
import requests, time

async def bad():
    time.sleep(1)            # ❌ 卡死事件循环
    requests.get(url)        # ❌ 卡死事件循环
    # 所有并发全废，变串行
```

**铁律**：async 函数里只能用异步库。`time.sleep` → `asyncio.sleep`，`requests` → `httpx.AsyncClient`，`open` → `aiofiles.open`。

### 坑 4：忘了 `asyncio.run`

```python
async def main():
    ...

main()    # ❌ 没跑，只返回协程，程序直接退出
```

顶层必须 `asyncio.run(main())`。

### 坑 5：gather 里协程没立即调度

```python
# 传入的是「协程对象」，gather 会调度它们
await asyncio.gather(query_project("123"), query_task("123"))
```

这是对的。但如果先 `coro = query_project("123")` 存着不 await，它不会自己跑——**协程被创建但没调度就不执行**。

### 坑 6：以为 gather 会自动超时

```python
# gather 本身不超时，一个协程挂死就全挂
await asyncio.gather(query_project("123"), hang_forever())
```

必须用 `asyncio.wait_for(gather(...), timeout=...)` 包一层。

### 坑 7：在 async 里调 sync CPU 密集

```python
async def hash_big_file():
    data = load_huge()       # CPU 密集同步操作
    return sha256(data)      # 阻塞事件循环
```

CPU 密集任务会阻塞事件循环。要放线程池：`await asyncio.to_thread(sha256, data)`，或用 `run_in_executor`。

### 坑 8：嵌套 asyncio.run

```python
async def main():
    asyncio.run(something())   # ❌ 事件循环已在跑，报错 RuntimeError
```

`asyncio.run` 只能在最外层调一次。内部要并发用 `gather` / `create_task`。

## 6. 验收标准

- [ ] 能解释「async 不是多线程」——单线程事件循环切换任务
- [ ] 能解释为什么 async 对 IO 密集有用、对 CPU 密集没用
- [ ] 能用 `asyncio.run` 启动顶层协程
- [ ] 能用 `asyncio.gather` 并发多个 Tool 并打印耗时对比
- [ ] 能解释 `return_exceptions=True` 的作用，知道 Agent 场景为什么常用它
- [ ] 能用 `asyncio.wait_for` 做超时控制
- [ ] **能解释阻塞陷阱**：为什么 async 函数里不能用 `time.sleep` / `requests`
- [ ] 编码练习 5 个任务跑通，串行/并发耗时差距明显

**最终产出**：`async_tools.py`，能并发调 3 个 Tool、带异常隔离和超时。这是后面 Agent 并发调多个 Tool 的直接预演。

## 7. 自测题

> 1. 「async 不是多线程」——那它靠什么实现并发？
>
> 2. Python 有 GIL，这对 async 有什么影响？为什么 LLM Agent 场景 async 仍然合适？
>
> 3. `async def query(): ...` 直接调 `query()` 会发生什么？为什么不执行？
>
> 4. `await` 能写在普通 `def` 函数里吗？为什么？
>
> 5. `asyncio.run` 为什么一个程序通常只调一次？能在 async 函数里再调吗？
>
> 6. `asyncio.gather(a, b, c)` 的返回值顺序和完成顺序有关吗？
>
> 7. 默认 `gather` 里一个协程抛异常会怎样？`return_exceptions=True` 改变了什么？Agent 场景为什么常用后者？
>
> 8. **最致命的坑**：async 函数里写 `time.sleep(1)` 会怎样？为什么？正确写法？
>
> 9. 为什么 async 函数里不能用 `requests`？该用什么？
>
> 10. `asyncio.wait_for(gather(...), timeout=2)` 和给每个协程单独加超时，有什么区别？
>
> 11. CPU 密集任务在 async 函数里会怎样？怎么解决？
>
> 12. 写一段：并发调 `query_project` 和 `query_task`，一个失败不影响另一个，整体超时 3 秒。

全部能答 → 进入 [[day5]]。
