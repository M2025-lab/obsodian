---
title: Day3：类型注解 + Pydantic
tags: [AI, Agent, Python, 第一阶段, Day3]
created: 2026-08-27
updated: 2026-08-27
---

# Day3：类型注解 + Pydantic（4 小时）

> 学习目标：会用类型注解 + Pydantic 定义数据模型并自动校验。这是后面 Structured Output 和 Tool Schema 的基础。

## 1. 知识点清单

今天要掌握的核心知识：

```text
类型注解           str / int / float / bool / list[str] / dict[str,int]
Optional / Union   可空 / 联合类型
Pydantic v2
  BaseModel         数据模型基类
  Field             字段描述/默认值/约束
  model_validate    dict → 对象（校验）
  model_validate_json  json 字符串 → 对象
  model_dump        对象 → dict
  model_dump_json   对象 → json 字符串
  ValidationError   校验失败异常
嵌套模型
枚举 Enum
配置额外字段 / 禁止额外字段
```

学完这些，你就能把 LLM 返回的 JSON 安全地转成强类型对象——这是 Structured Output 和 Tool Schema 的基础。

## 2. 怎么学

**时间分配**：

```text
0:00-0:30  类型注解语法
0:30-1:30  Pydantic BaseModel + Field
1:30-2:00  校验与序列化（model_validate / model_dump）
2:00-2:30  嵌套模型 + 枚举
2:30-3:00  异常处理 + 编码练习
3:00-4:00  串联：LLM JSON → 对象
```

**学习方法**：

1. 先理解「为什么需要 Pydantic」——它解决 LLM 输出不可信的问题
2. 每个方法在 REPL 敲一遍，故意传错数据看报错
3. 重点对比 Java POJO + FastJSON 的写法
4. 编码练习模拟「LLM 返回 JSON → Pydantic 校验 → 业务对象」全链路

> Pydantic 现在是 v2 主流，v1 已过时。本笔记全部用 v2 语法（`model_validate` 而非 `parse_obj`）。

## 3. 知识点详解

### 3.1 为什么 Agent 必须 Pydantic

LLM 返回的是**字符串**，你 `json.loads` 后只是 `dict`，字段对不对、类型对不对全不知道：

```python
raw = '{"projectId": 123, "name": "XX"}'   # projectId 是 int，你以为是 str
data = json.loads(raw)
data["project_id"]    # KeyError！实际是 projectId
```

Pydantic 解决：**定义契约 → 自动校验 → 强类型对象**。

```text
LLM 输出 JSON
   ↓
Pydantic model_validate（校验类型、必填、约束）
   ↓
强类型对象 Project
   ↓
业务代码 .projectId 安全使用
```

**Java 对比**：

| 场景        | Java                | Python + Pydantic      |
| --------- | ------------------- | ---------------------- |
| 数据模型      | POJO / Record       | BaseModel              |
| JSON → 对象 | `JSON.parseObject(s, Project.class)` | `Project.model_validate_json(s)` |
| 对象 → JSON | `JSON.toJSONString(p)` | `p.model_dump_json()`  |
| 字段校验      | `@NotNull` `@Min` `@Valid` | `Field(..., constraints)` |
| 校验失败      | `MethodArgumentNotValidException` | `ValidationError`      |

### 3.2 类型注解语法

类型注解不影响运行，但 IDE、mypy、**Agent 框架**都靠它：

```python
project_id: str = "826051217000001469"
count: int = 0
price: float = 9.9
enabled: bool = True

# 容器类型
names: list[str] = ["a", "b"]               # Java List<String>
mapping: dict[str, int] = {"a": 1}          # Java Map<String,Integer>
unique: set[str] = {"a", "b"}

# 可空
from typing import Optional
name: Optional[str] = None                  # 等价于 str | None

# Python 3.10+ 更简洁
name: str | None = None                      # 同上，推荐写法
value: int | str = 1                         # 联合类型，可以是 int 或 str
```

> Java 是强类型必须声明；Python 是动态类型，注解「可选但强烈建议」。Agent 框架（LangChain）会**读取注解**自动生成 Tool 的参数 Schema 给 LLM 看，所以 Tool 参数必须带注解。

### 3.3 第一个 BaseModel

```python
from pydantic import BaseModel, Field

class Project(BaseModel):
    projectId: str
    name: str
    notifiable: bool = False                  # 有默认值 = 可选
    reason: str | None = None                # 可空

# 从 dict 构造
p = Project(projectId="826051217000001469", name="XX项目", notifiable=True)
print(p.projectId)                           # 826051217000001469
print(p.name)                                # XX项目

# 从 JSON 字符串构造（LLM 场景常用）
p2 = Project.model_validate_json(
    '{"projectId":"826051217000001469","name":"XX项目","notifiable":true}'
)

# 转回 dict / JSON
p.model_dump()                               # {'projectId': '...', 'name': '...', ...}
p.model_dump_json()                          # '{"projectId":"...","name":"...",...}'
```

**与 Java POJO 对比**：

```java
// Java
public class Project {
    private String projectId;   // 要写 getter/setter 或用 Lombok
    private String name;
    private boolean notifiable;
}
Project p = JSON.parseObject(json, Project.class);   // FastJSON
```

Pydantic 优势：不用写 getter/setter，构造自动校验，序列化一行搞定。

### 3.4 Field —— 字段描述与约束

```python
from pydantic import BaseModel, Field

class QueryRequest(BaseModel):
    projectId: str = Field(
        ...,                          # ... 表示必填（Ellipsis），类似 @NotNull
        description="项目订单ID，如 826051217000001469",
        pattern=r"^\d{18}$",          # 正则约束：18 位数字
        examples=["826051217000001469"],
    )
    recheckOnly: bool = Field(
        default=False,
        description="是否只查复尺任务",
    )
    limit: int = Field(
        default=10,
        ge=1, le=100,                # >=1 且 <=100，类似 @Min @Max
    )
```

**为什么 `description` 重要**：这段描述会**发给 LLM**，告诉模型这个字段是什么意思、填什么格式。写不好，LLM 就乱填。

**约束速查**：

| 约束         | 含义        | Java 对应       |
| ---------- | --------- | ------------- |
| `...`       | 必填        | `@NotNull`    |
| `default=x` | 默认值       | 字段初始化         |
| `ge=n`      | >= n      | `@Min(n)`     |
| `le=n`      | <= n      | `@Max(n)`     |
| `gt=n`      | > n       |               |
| `lt=n`      | < n       |               |
| `min_length` / `max_length` | 长度约束 | `@Size`       |
| `pattern`   | 正则        | `@Pattern`    |

### 3.5 校验失败 —— ValidationError

```python
from pydantic import ValidationError

try:
    p = Project.model_validate({"name": "XX"})   # 缺 projectId
except ValidationError as e:
    print(e.errors())
    # [{'type': 'missing', 'loc': ('projectId',), 'msg': 'Field required', ...}]
```

**`e.errors()` 返回结构化错误列表**，每条含：

- `type`：错误类型（missing / value_error / string_pattern_mismatch 等）
- `loc`：哪个字段（元组，嵌套字段是多级）
- `msg`：人类可读信息
- `input`：传进来的值

> 对比 Java `@Valid` 的 `BindingResult`：Pydantic 的错误信息更结构化，方便回传给 LLM 让它修正。后面 Agent 会把 ValidationError 喂回 LLM 重试。

### 3.6 嵌套模型

```python
class NotifyTask(BaseModel):
    taskId: str
    canNotify: bool
    reason: str | None = None

class ProjectDetail(BaseModel):
    projectId: str
    name: str
    tasks: list[NotifyTask]           # 嵌套模型列表

raw = """
{
  "projectId": "826051217000001469",
  "name": "XX项目",
  "tasks": [
    {"taskId": "t1", "canNotify": true, "reason": "满足条件"},
    {"taskId": "t2", "canNotify": false, "reason": "未复尺"}
  ]
}
"""
detail = ProjectDetail.model_validate_json(raw)
print(detail.tasks[0].taskId)         # t1
print(detail.tasks[1].reason)          # 未复尺
```

> 对比 Java：嵌套 POJO + `List<NotifyTask>`，Pydantic 同理，但自动校验每一层。

### 3.7 枚举

```python
from enum import Enum

class TaskType(str, Enum):
    NOTIFY = 1          # 通知
    RECHECK = 2         # 复尺
    RECOGNIZE = 3       # 识别

class JudgeResult(str, Enum):
    YES = 1
    NO = 0

class TaskJudge(BaseModel):
    taskType: TaskType
    judgeResult: JudgeResult
    reason: str

# LLM 返回的数字会自动转成枚举
t = TaskJudge.model_validate({
    "taskType": 3,       # 自动匹配 TaskType.RECOGNIZE
    "judgeResult": 1,
    "reason": "满足通知条件"
})
print(t.taskType)        # TaskType.RECOGNIZE
print(t.taskType.name)   # RECOGNIZE
print(t.taskType.value)  # 3
```

> 这正是主计划里提到的 Structured Output 示例：`{"taskType": 3, "judgeResult": 1, "reason": "..."}`。

### 3.8 额外字段控制

LLM 有时会多返回字段。默认行为：

```python
class Project(BaseModel):
    projectId: str

# 默认忽略多余字段
Project.model_validate({"projectId": "1", "extra": "x"})
# 不报错，extra 被丢掉
```

改成禁止多余（严格模式，推荐 Tool 参数用）：

```python
from pydantic import BaseModel, ConfigDict

class Project(BaseModel):
    model_config = ConfigDict(extra="forbid")    # 多字段直接报错
    projectId: str

Project.model_validate({"projectId": "1", "extra": "x"})   # ValidationError
```

> 为什么 Tool 参数建议 `extra="forbid"`：LLM 乱填字段时能立刻发现，而不是静默丢弃。

## 4. 编码练习

建 `models.py`，完成以下任务。

### 任务 1：基础模型

```python
from pydantic import BaseModel, Field
from enum import Enum

class TaskType(str, Enum):
    NOTIFY = 1
    RECHECK = 2
    RECOGNIZE = 3

class JudgeResult(str, Enum):
    YES = 1
    NO = 0

class ProjectQuery(BaseModel):
    projectId: str = Field(..., description="项目订单ID，18位数字", pattern=r"^\d{18}$")
    recheckOnly: bool = Field(default=False, description="是否只查复尺任务")
```

### 任务 2：嵌套业务模型

```python
class NotifyTask(BaseModel):
    taskId: str
    canNotify: bool
    reason: str | None = None

class Project(BaseModel):
    model_config = ConfigDict(extra="forbid")
    projectId: str
    name: str
    notifiable: bool = False
    tasks: list[NotifyTask] = []
```

### 任务 3：LLM 输出契约（Structured Output 雏形）

模拟 LLM 判断每个任务能不能通知，返回结构化结果：

```python
class TaskJudge(BaseModel):
    taskType: TaskType
    judgeResult: JudgeResult
    reason: str = Field(..., min_length=1, description="判断理由")
```

### 任务 4：解析函数 + 异常处理

```python
from pydantic import ValidationError

def parse_llm_output(raw_json: str) -> Project:
    """把 LLM 输出的 JSON 安全转成 Project"""
    try:
        return Project.model_validate_json(raw_json)
    except ValidationError as e:
        # 把错误信息收集起来，后面要喂回 LLM
        errors = [f"{'.'.join(str(x) for x in err['loc'])}: {err['msg']}" for err in e.errors()]
        raise ValueError(f"LLM 输出不符合契约: {errors}")
```

### 任务 5：跑测试用例

```python
# 正常
good = '{"projectId":"826051217000001469","name":"XX项目","notifiable":true,"tasks":[{"taskId":"t1","canNotify":true,"reason":"ok"}]}'
print(parse_llm_output(good))

# 缺字段
bad1 = '{"name":"XX"}'
try:
    parse_llm_output(bad1)
except ValueError as e:
    print(e)   # 应提示 projectId 缺失

# 多字段
bad2 = '{"projectId":"826051217000001469","name":"XX","extra":"x"}'
try:
    parse_llm_output(bad2)
except ValueError as e:
    print(e)   # 应提示 extra 不允许

# 格式不对（projectId 非18位）
bad3 = '{"projectId":"123","name":"XX"}'
try:
    parse_llm_output(bad3)
except ValueError as e:
    print(e)   # 应提示 pattern 不匹配
```

### 验证

- 正常 JSON 能转成 `Project` 对象并访问属性
- 三种错误情况都能被 `ValidationError` 捕获并转成可读信息
- 嵌套 `tasks` 里的 `NotifyTask` 也能自动校验

## 5. 常见坑

### 坑 1：Pydantic v1 / v2 语法混用

```python
# v1（已过时，别用）
Project.parse_obj(data)
Project.parse_raw(json_str)
p.dict()

# v2（当前标准）
Project.model_validate(data)
Project.model_validate_json(json_str)
p.model_dump()
```

网上很多教程还是 v1 语法。**装 `pydantic>=2.0`，只用 v2 方法**（带 `model_` 前缀）。

### 坑 2：类型注解写成 `list` 不带泛型

```python
class M(BaseModel):
    ids: list          # ❌ 没带元素类型，不会校验元素
    ids: list[str]     # ✅ 校验每个元素是 str
```

### 坑 3：可变默认值

```python
class M(BaseModel):
    tasks: list[Task] = []    # Pydantic 会保护，但普通 class 这样写就踩坑
```

Pydantic 内部会深拷贝默认值，所以 BaseModel 里可写；但**普通 class 别这么写**（Day1 坑 4）。别因为这个混淆。

### 坑 4：`Optional[str] = None` 不等于「可空必填」

```python
class M(BaseModel):
    name: str | None = None
```

这表示「可不传，传了可以是 str 或 None」。**不是「必须传，可以是空」**。要必填可空用 `name: str | None`（没默认值）。

### 坑 5：枚举值传错不报错

```python
class TaskType(str, Enum):
    NOTIFY = 1

TaskType.model_validate(99)   # ❌ 报错，但只在你校验时
```

枚举传非法值**只在 `model_validate` 时报错**，直接 `TaskType(99)` 会抛 `ValueError`。所以枚举必须走 Pydantic 校验才能稳定拦截。

### 坑 6：`extra` 默认忽略导致 LLM 乱填被静默

```python
class M(BaseModel):
    name: str
M.model_validate({"name":"a","haha":"x"})   # 默认不报错，haha 被丢
```

LLM 多吐字段你都不知道。Tool 参数建议 `model_config = ConfigDict(extra="forbid")`。

### 坑 7：以为类型注解会运行时校验

```python
def query(project_id: str):
    ...

query(123)    # 不报错！注解不强制
```

**普通函数的类型注解不校验**。要运行时校验必须用 Pydantic 或 `@pydantic.validate_call`。别以为写了 `: str` 传 int 就会挂。

## 6. 验收标准

- [ ] 能解释「LLM 输出 → Pydantic → 强类型对象」为什么必要
- [ ] 能写出带 `Field(description / pattern / ge / le)` 的 BaseModel
- [ ] 能用 `model_validate_json` 从 JSON 字符串构造对象
- [ ] 能用 `model_dump` / `model_dump_json` 序列化
- [ ] 能捕获 `ValidationError` 并提取 `loc / msg` 转成可读信息
- [ ] 能定义嵌套模型和枚举
- [ ] 能解释为什么 Tool 参数模型要写 `description`（会发给 LLM）
- [ ] 能解释 `extra="forbid"` 解决什么问题
- [ ] 编码练习 5 个任务跑通，三种错误用例都能正确拦截

**最终产出**：`models.py`，定义了 `ProjectQuery` / `Project` / `NotifyTask` / `TaskJudge` 模型，以及 `parse_llm_output` 函数——这是后面 Tool Schema 和 Structured Output 的契约层。

## 7. 自测题

> 1. 为什么不能直接 `json.loads` 用 LLM 返回的 JSON？Pydantic 解决了什么？
>
> 2. 类型注解 `x: str = 1` 运行时报错吗？那注解有什么用？
>
> 3. `Field(...)` 里的 `...` 表示什么？`default=False` 和 `...` 有什么区别？
>
> 4. `Field(description=...)` 这段描述最终给谁看？为什么 Tool 参数必须写？
>
> 5. `model_validate` 和 `model_validate_json` 区别？分别接收什么？
>
> 6. `model_dump()` 和 `model_dump_json()` 返回什么类型？
>
> 7. `ValidationError` 的 `errors()` 返回什么结构？`loc` 字段什么意思？
>
> 8. 嵌套模型 `tasks: list[NotifyTask]` 是怎么自动校验每一层的？
>
> 9. 枚举 `TaskType(3)` 和 `TaskType.model_validate(3)` 行为一样吗？非法值会怎样？
>
> 10. `Optional[str] = None` 表示「可空必填」吗？那它到底表示什么？
>
> 11. `extra="forbid"` 解决什么问题？为什么 Tool 参数建议开？
>
> 12. Pydantic v1 和 v2 的方法名差异？`parse_obj` 对应 v2 什么？

全部能答 → 进入 [[day4]]。
