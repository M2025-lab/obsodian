---
title: Day1：Python 基础语法
tags: [AI, Agent, Python, 第一阶段, Day1]
created: 2026-08-27
updated: 2026-08-27
---

# Day1：Python 基础语法（4 小时）

> 学习目标：掌握 Agent 开发中最常用的 Python 语法，能独立写出带继承和异常处理的 class。
>
> 你是 Java 后端，所以每个概念都会给 Java 对照，帮你快速建立心智模型。

## 1. 知识点清单

今天要掌握的核心知识：

```text
变量与基本类型      str / int / float / bool / None
容器             list / dict / set / tuple
字符串           f-string / 常用方法
流程控制          if / for / while
函数             def / 默认参数 / *args / **kwargs
类与对象          class / __init__ / self / 继承
异常             try / except / finally / raise
模块             import / from...import / __name__
入口             if __name__ == "__main__"
```

学完这些，你就能写出 Agent 里最基本的 Tool 类。

## 2. 怎么学

**环境准备**（5 分钟）：

```bash
python3 --version      # 确认 Python 3.10+，没有就去 python.org 装
mkdir -p ~/agent-day1 && cd ~/agent-day1
python3 -m venv .venv
source .venv/bin/activate
python --version       # 激活后应显示 .venv 里的 python
```

> 今天不用装任何第三方库，纯标准库练习。

**学习方法**：

1. 每个知识点先看下面的「详解」+ Java 对照
2. 在 REPL（交互式终端）里敲一遍：`python` 进交互模式，边读边试
3. 写成 `.py` 文件用 `python 文件名.py` 跑
4. 做完「编码练习」全部任务
5. 对照「验收标准」自查

**时间分配**：

```text
0:00-0:30  类型 + 容器 + 字符串（REPL 敲）
0:30-1:00  流程控制 + 函数
1:00-2:00  类与继承（重点，Agent 核心）
2:00-2:30  异常处理
2:30-3:00  模块 + 入口
3:00-4:00  编码练习（写 Tool 框架）
```

## 3. 知识点详解

### 3.1 变量与基本类型

```python
name = "construction-agent"   # str
count = 100                    # int
price = 99.9                   # float
enabled = True                 # bool  注意首字母大写
nothing = None                 # None  类似 Java null
```

**与 Java 最大区别**：Python 变量不声明类型，赋值时自动推断。但可以加类型注解（Day3 详讲）：

```python
project_id: str = "826051217000001469"
```

> 注解不强制，运行时 `project_id = 123` 也不会报错。它只是给人和 IDE 看。

**类型检查**：

```python
type(name)         # <class 'str'>
isinstance(name, str)   # True，类似 Java instanceof
```

### 3.2 容器

| 类型     | 字面量              | Java 对应          | 特点                |
| ------ | ---------------- | ---------------- | ----------------- |
| list   | `[1, 2, 3]`      | `ArrayList`      | 有序可变，可混类型         |
| dict   | `{"a": 1}`       | `HashMap`        | 键值对，键一般用 str      |
| set    | `{1, 2, 3}`      | `HashSet`        | 去重，无序             |
| tuple  | `(1, 2)`         | 不可变 List         | 不可变，常用于多返回值       |

```python
projects = ["A", "B", "C"]
projects.append("D")          # 加
projects[0]                  # 取第一个
projects[-1]                 # 取最后一个（Java 没有，很常用）
"D" in projects              # 是否包含

task = {"id": "123", "done": False}
task["id"]                   # 取值（键不存在会 KeyError）
task.get("name", "默认")      # 取值，不存在返回默认（类似 Map.getOrDefault）
task["name"] = "新任务"        # 加/改

ids = {1, 2, 3, 2}           # {1, 2, 3} 自动去重
```

### 3.3 字符串 f-string

Agent 拼提示词、拼日志全靠 f-string，必须熟：

```python
project_id = "826051217000001469"
msg = f"查询项目 {project_id} 的复尺任务"
# 类似 Java: "查询项目 " + project_id + " 的复尺任务"
# 但更接近 Java 的 String.format，却不需要 %s 占位

# 表达式可以直接写
msg = f"任务数={len(projects)}, 第一个={projects[0]}"

# 多行
prompt = f"""
你是一个项目助手。
项目ID: {project_id}
任务列表: {projects}
"""
```

常用方法：

```python
"  hello  ".strip()           # "hello"
"a,b,c".split(",")            # ["a", "b", "c"]
"-".join(["a", "b", "c"])     # "a-b-c"
"Hello".lower()               # "hello"
"hello".upper()               # "HELLO"
"abc".replace("b", "X")       # "aXc"
```

### 3.4 流程控制

```python
# if / elif / else —— 注意冒号和缩进
if count > 100:
    print("多")
elif count > 10:
    print("中")
else:
    print("少")

# for 遍历
for p in projects:
    print(p)

for i, p in enumerate(projects):   # 带下标，类似 Java for(int i=0;...)
    print(i, p)

# while
while count > 0:
    count -= 1

# 推导式（Python 特色，要会读）
squares = [x * x for x in range(5)]           # [0, 1, 4, 9, 16]
evens = [x for x in range(10) if x % 2 == 0] # [0, 2, 4, 6, 8]
```

**Python 用缩进表示代码块，没有花括号 `{}`**。缩进必须一致（推荐 4 空格）。这是从 Java 转过来最容易忘的。

### 3.5 函数

```python
# 基本定义
def query(project_id):
    return f"项目 {project_id}"

# 默认参数（类似 Java 无默认参数，Python 有）
def query(project_id, recheck_only=False):
    if recheck_only:
        return f"只查复尺 {project_id}"
    return f"查全部 {project_id}"

query("123")                  # 用默认 False
query("123", recheck_only=True)

# *args 收集位置参数（可变参数）
def sum_all(*nums):
    return sum(nums)
sum_all(1, 2, 3)              # 6

# **kwargs 收集关键字参数（类似 Map）
def call_tool(name, **params):
    print(name, params)
call_tool("query_project", projectId="123", recheck=True)
# name="query_project", params={"projectId":"123", "recheck":True}
```

> `*args` / `**kwargs` 在 Agent 框架里到处都是（Tool 调用传参），必须看懂。

### 3.6 类与继承（重点）

这是今天最核心的部分，Agent 的 Tool 就是 class。

```python
class Tool:
    def __init__(self, name: str, description: str = ""):
        self.name = name                # 实例属性，类似 Java this.name
        self.description = description

    def run(self, input: dict) -> str:
        raise NotImplementedError("子类必须实现")

    def __str__(self):
        return f"Tool({self.name})"


class QueryProjectTool(Tool):           # 继承，括号里是父类
    def __init__(self):
        super().__init__(name="query_project", description="查询项目")

    def run(self, input: dict) -> str:
        project_id = input.get("projectId")
        if not project_id:
            raise ValueError("projectId 不能为空")
        return f"项目 {project_id} 的数据"
```

**与 Java 的关键区别**：

1. **`self` 必须显式写**：Java 的 `this` 隐式，Python 每个方法的第一个参数必须是 `self`（实例引用），调用时不用传。
2. **构造方法是 `__init__`**，不是类名。
3. **没有真正的接口**：靠「鸭子类型」——只要 `run` 方法存在就能当 Tool 用，不需要 `implements`。
4. **属性不用提前声明**：`self.name = ...` 直接赋值就创建了。
5. **多继承**：`class A(B, C)` 合法，Java 不行（只能继承一个类 + 多接口）。

**私有**：Python 没有 `private` 关键字。约定 `_name` 受保护，`__name` 会改名（弱封装），但都能访问。

### 3.7 异常处理

```python
try:
    result = risky_operation()
except ValueError as e:
    print(f"值错误: {e}")
except (KeyError, IndexError) as e:
    print(f"键或下标错误: {e}")
except Exception as e:           # 兜底，类似 Java catch(Exception)
    print(f"其他错误: {e}")
else:
    print("没异常才执行")
finally:
    print("一定执行（关连接等）")

# 主动抛
def check(project_id):
    if not project_id:
        raise ValueError("projectId 不能为空")
```

**与 Java 区别**：

| Java                          | Python                          |
| ----------------------------- | ------------------------------- |
| `try / catch / finally`       | `try / except / finally`        |
| `throw new RuntimeException()` | `raise ValueError()`            |
| 异常必须声明或捕获（受检异常）         | 无受检异常，全靠自觉                     |
| `Exception` 是基类              | `Exception` 是基类，常用 `ValueError`/`KeyError`/`TypeError` |

常见内置异常：

```text
ValueError      值不对（如 int("abc")）
KeyError        dict 键不存在
IndexError      list 下标越界
TypeError       类型操作错误
AttributeError  属性不存在
FileNotFoundError
```

### 3.8 模块与入口

```python
# tool.py
class Tool: ...

def helper():
    ...
```

```python
# main.py
from tool import Tool, helper     # 导入类和函数
import tool                       # 导入整个模块
tool.helper()

t = Tool()
```

**入口判断**：

```python
# main.py
def main():
    print("启动")

if __name__ == "__main__":
    main()
```

**为什么需要 `if __name__ == "__main__"`**：

- 直接 `python main.py` 跑时，`__name__` 等于 `"__main__"`，进入 if
- 被 `import` 时，`__name__` 等于模块名（如 `"main"`），不进 if
- 作用：让文件既能独立运行，又能被安全导入而不自动执行

> 对比 Java：Java 的 `public static void main` 永远是入口。Python 没有固定入口，靠这个判断。

### 3.9 鸭子类型（Agent 里的关键思想）

```python
class QueryProjectTool:
    def run(self, input): ...

class QueryTaskTool:
    def run(self, input): ...

def execute(tool, input):
    return tool.run(input)    # 不管 tool 是什么类，有 run 就能调
```

> Java 要 `tool instanceof Tool` 或声明接口；Python 不需要。这就是「鸭子类型」——走起来像鸭子，就是鸭子。

后面 Agent 框架（LangChain）注册 Tool，靠的就是每个 Tool 都有 `run` / `name` / `description` 这几个约定属性，不需要继承统一接口。

## 4. 编码练习

建文件 `tool_framework.py`，完成以下任务（一步一步来）：

### 任务 1：定义 Tool 基类

```python
class Tool:
    def __init__(self, name: str, description: str = ""):
        self.name = name
        self.description = description

    def run(self, input: dict) -> str:
        raise NotImplementedError("子类必须实现 run")

    def __str__(self):
        return f"Tool(name={self.name})"
```

### 任务 2：两个具体 Tool

实现 `QueryProjectTool` 和 `QueryTaskTool`，继承 `Tool`：

- `QueryProjectTool.run({"projectId": "826051217000001469"})` 返回 `项目 826051217000001469 的数据`
- `QueryTaskTool.run({"projectId": "...", "status": "notify"})` 返回对应任务列表字符串
- 参数缺失时抛 `ValueError` 并带友好信息

### 任务 3：ToolRegistry（注册表）

```python
class ToolRegistry:
    def __init__(self):
        self._tools = {}

    def register(self, tool: Tool):
        self._tools[tool.name] = tool

    def get(self, name: str) -> Tool:
        if name not in self._tools:
            raise KeyError(f"Tool {name} 未注册")
        return self._tools[name]

    def list_tools(self):
        return list(self._tools.keys())
```

### 任务 4：异常处理 + 入口

```python
def main():
    registry = ToolRegistry()
    registry.register(QueryProjectTool())
    registry.register(QueryTaskTool())

    # 模拟 LLM 决定调用 query_project
    tool_name = "query_project"
    try:
        tool = registry.get(tool_name)
        result = tool.run({"projectId": "826051217000001469"})
        print(f"结果: {result}")
    except KeyError as e:
        print(f"Tool 不存在: {e}")
    except ValueError as e:
        print(f"参数错误: {e}")
    except Exception as e:
        print(f"未知错误: {e}")

if __name__ == "__main__":
    main()
```

### 验证

- 正常调用 → 打印项目数据
- 把 `tool_name` 改成 `"query_xxx"` → 打印 `Tool 不存在`
- 把 `projectId` 去掉 → 打印 `参数错误`
- `print(QueryProjectTool())` → 打印 `Tool(name=query_project)`

### 进阶（选做）

- 给 `Tool` 加一个 `_timeout` 私有属性（约定用下划线）
- 用 `*args` 让 `ToolRegistry.register` 一次注册多个 Tool

## 5. 常见坑

### 坑 1：忘了冒号和缩进

```python
if count > 10      # ❌ 漏冒号
    print("多")
```

Python 用缩进表示块，**冒号必须有，缩进必须一致**。报错 `SyntaxError` 基本都是这个。

### 坑 2：`None` / `False` / `0` / `""` 的真假

```python
if []:
    print("空 list 是 True？")   # 不会打印，空 list 是 False
```

**Python 里以下都是假**：`None`、`False`、`0`、`0.0`、`""`、`[]`、`{}`、`{}`、`()`。

写 `if project_id:` 时，空字符串也会进 else 分支，要心里有数。

### 坑 3：`is` 和 `==` 混用

```python
a = "hello"
b = "hello"
a == b    # True，值相等
a is b    # 可能 True 也可能 False，is 比的是身份（内存地址）
```

**`is` 只用于和 `None` 比较**：`if x is None`。其他都用 `==`。

### 坑 4：可变默认参数

```python
def add_tool(tool, registry=[]):   # ❌ 默认参数是 list（可变）
    registry.append(tool)
    return registry

add_tool("a")    # ["a"]
add_tool("b")    # ["a", "b"]  ← 不是 ["b"]！默认参数只创建一次
```

正确写法：

```python
def add_tool(tool, registry=None):
    if registry is None:
        registry = []
    registry.append(tool)
    return registry
```

> 这是 Python 最经典的坑，面试必考，Agent 框架里默认参数到处都是。

### 坑 5：浅拷贝

```python
a = [[1, 2], [3, 4]]
b = a.copy()         # 浅拷贝
b[0][0] = 99
print(a)             # [[99, 2], [3, 4]]  ← a 也变了！内层 list 是同一个
```

要深拷贝用 `import copy; copy.deepcopy(a)`。

### 坑 6：`self` 忘写

```python
class Tool:
    def run(input):          # ❌ 没写 self
        return input
Tool().run({})               # TypeError
```

调用 `Tool().run({})` 时 Python 自动把实例传成第一个参数，但方法定义没接，就乱套了。**实例方法第一个参数永远是 `self`**。

## 6. 验收标准

完成以下全部，Day1 才算过关：

- [ ] 不查文档能写出带 `__init__`、`self`、继承的 class
- [ ] 能解释 `self` 为什么必须显式写
- [ ] 能用 `try / except / finally` 捕获并区分多种异常
- [ ] 能解释 `if __name__ == "__main__"` 的作用
- [ ] 能解释「可变默认参数」坑，并给出正确写法
- [ ] 编码练习 4 个任务全部跑通：
  - [ ] Tool 基类 + 两个子类
  - [ ] ToolRegistry 注册和查找
  - [ ] 三种异常分支都能命中并打印友好信息
  - [ ] `__str__` 正常打印

**最终产出**：`tool_framework.py`，能跑通「注册 Tool → 按名查找 → 调 run → 异常处理」全流程。这是后面所有 Agent Tool 的骨架。

## 7. 自测题

能口述回答以下问题，才算真懂：

> 1. Python 变量为什么不用声明类型？类型注解 `x: str = 1` 运行时会报错吗？
>
> 2. `self` 和 Java 的 `this` 有什么区别？为什么 Python 必须显式写？
>
> 3. `list` / `dict` / `set` / `tuple` 各对应 Java 什么？`tuple` 有什么用？
>
> 4. f-string 相比字符串拼接有什么优势？写一句带表达式的 f-string。
>
> 5. Python 怎么表示代码块？没有花括号怎么判断块的归属？
>
> 6. `*args` 和 `**kwargs` 分别接收什么？在 Tool 调用场景里有什么用？
>
> 7. Python 的异常体系和 Java 有什么区别？`raise` 对应 Java 什么？
>
> 8. `if __name__ == "__main__"` 什么时候进、什么时候不进？
>
> 9. 「鸭子类型」是什么？为什么 Agent 的 Tool 不需要统一接口？
>
> 10. 可变默认参数为什么是坑？怎么改？
>
> 11. `is None` 和 `== None` 哪个对？为什么？
>
> 12. 写一个 `Tool` 基类 + 一个子类，子类必须实现 `run`，没实现时报什么错？

全部能答 → 进入 [[day2]]。
