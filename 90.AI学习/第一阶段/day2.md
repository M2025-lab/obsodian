---
title: Day2：venv + pip + 模块
tags: [AI, Agent, Python, 第一阶段, Day2]
created: 2026-08-27
updated: 2026-08-27
---

# Day2：venv + pip + 模块（3 小时）

> 学习目标：能管理项目依赖，理解虚拟环境与包管理，能把代码拆成模块和包。

## 1. 知识点清单

今天要掌握的核心知识：

```text
venv                — 虚拟环境，隔离依赖
pip                 — 包管理工具
requirements.txt    — 依赖锁定文件
pyproject.toml      — 现代项目配置（了解）
模块 (module)        — 一个 .py 文件
包 (package)         — 一个目录，含 __init__.py
import 语法          — import / from...import / as
相对导入与绝对导入
PYTHONPATH          — 模块搜索路径
```

学完这些，你就能像 Maven 管理 Java 项目一样管理 Python 项目。

## 2. 怎么学

**时间分配**：

```text
0:00-0:30  venv 创建 / 激活 / 退出
0:30-1:00  pip 安装 / 查询 / 卸载
1:00-1:30  requirements.txt 锁定与还原
1:30-2:30  模块与包（拆分 Day1 的代码）
2:30-3:00  编码练习 + 验收
```

**学习方法**：

1. 每条命令自己敲一遍，看输出
2. 重点理解「为什么需要 venv」——这和 Java 的 Maven 依赖隔离思路不同
3. 把 Day1 的 `tool_framework.py` 拆成 `tools/` 包练手
4. 故意制造一次依赖冲突，体会 venv 的价值

## 3. 知识点详解

### 3.1 venv —— 虚拟环境

**为什么需要 venv**：

```text
项目 A 依赖 httpx 0.27
项目 B 依赖 httpx 0.24
→ 全局只装一个版本，另一个项目就跑不了
→ venv 给每个项目一套独立的依赖
```

**Java 对比**：Java 靠 Maven 把依赖装进 `~/.m2`，编译时打包进 jar；Python 靠 venv 在**运行环境**层面隔离——每个 venv 是一个独立的 `site-packages` 目录。

**核心命令**：

```bash
# 创建（在项目根目录）
python -m venv .venv

# 激活
source .venv/bin/activate          # macOS / Linux
# .venv\Scripts\activate           # Windows PowerShell

# 激活后提示符前会出现 (.venv)
which python                       # 应指向 .venv/bin/python

# 退出
deactivate
```

**激活做了什么**：把 `.venv/bin` 加到 PATH 最前面，让 `python` / `pip` 指向虚拟环境里的版本。退出就还原。

**关键理解**：venv 只是「一个目录 + 改了 PATH」，没有黑魔法。删掉 `.venv` 目录就等于卸载整个环境，重装即可恢复。

### 3.2 pip —— 包管理

```bash
pip install httpx                  # 装最新版
pip install httpx==0.27.2          # 指定版本
pip install "httpx>=0.27,<0.28"   # 版本范围
pip install -r requirements.txt    # 批量装
pip uninstall httpx                # 卸载
pip list                          # 已装列表
pip show httpx                    # 看某个包详情
pip freeze                        # 导出 已装=版本 格式
pip install --upgrade httpx       # 升级
```

**Java 对比**：

| 操作          | Maven                  | pip                        |
| ----------- | ---------------------- | -------------------------- |
| 声明依赖        | `pom.xml` `<dependency>` | `requirements.txt`         |
| 安装          | `mvn install`           | `pip install`              |
| 锁版本         | `pom.xml` 写死版本          | `requirements.txt` 写死版本    |
| 仓库          | Maven Central           | PyPI                       |
| 国内加速        | 镜像配置                    | `-i https://pypi.tuna.tsinghua.edu.cn/simple` |

**国内加速**（必装，否则慢到怀疑人生）：

```bash
pip install httpx -i https://pypi.tuna.tsinghua.edu.cn/simple
```

或一劳永逸，写进 `~/.pip/pip.conf`：

```ini
[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
```

### 3.3 requirements.txt —— 依赖锁定

```text
# requirements.txt
httpx==0.27.2
pydantic==2.9.2
fastapi==0.115.0
uvicorn==0.30.6
```

**生成**：

```bash
pip freeze > requirements.txt
```

**还原**（别人 clone 你项目后）：

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

> 这就是 Python 版的「`mvn clean install` 恢复全部依赖」。

**为什么用 `==` 锁死**：不锁版本，今天跑得好好的，明天某个包升级了 API 变了，就挂了。线上 Agent 必须锁死版本。

**进阶：pyproject.toml**（现代标准，了解即可）

```toml
[project]
name = "construction-agent"
version = "0.1.0"
dependencies = [
    "httpx>=0.27",
    "pydantic>=2.9",
]
```

> 新项目推荐用 `pyproject.toml` + `uv`（更快的包管理器），但 `requirements.txt` 仍然最通用，今天先用它。

### 3.4 模块（module）

**一个 `.py` 文件就是一个模块**。

```text
project/
├── tool.py          # 模块名是 tool
└── main.py
```

```python
# tool.py
class Tool:
    ...

def helper():
    ...
```

```python
# main.py
import tool                        # 导入整个模块
tool.Tool()                        # 用时要带模块名

from tool import Tool, helper      # 导入具体名字，用时不带模块名
Tool()

import tool as t                   # 起别名（类似 Java import as）
t.Tool()
```

**Java 对比**：

| Python                  | Java                          |
| ----------------------- | ----------------------------- |
| `import tool`           | `import com.example.tool.*`   |
| `from tool import Tool` | `import com.example.tool.Tool` |
| 模块 = 文件              | 包 = 目录                      |
| 不用写 `package` 声明     | 文件首行 `package com.example;` |

### 3.5 包（package）

**一个目录 + `__init__.py` 就是一个包**（Python 3.3+ 普通目录也能 import，但约定仍建 `__init__.py`）。

```text
construction-agent/
├── main.py
├── tools/
│   ├── __init__.py          # 包标记 + 暴露 API
│   ├── base.py              # class Tool
│   ├── query_project.py    # class QueryProjectTool
│   └── query_task.py        # class QueryTaskTool
└── models.py
```

**导入方式**：

```python
# main.py
from tools.base import Tool
from tools.query_project import QueryProjectTool
from tools.query_task import QueryTaskTool

# 或从包的 __init__.py 暴露（更简洁）
from tools import Tool, QueryProjectTool, QueryTaskTool
```

**`__init__.py` 的作用**：

```python
# tools/__init__.py
from .base import Tool
from .query_project import QueryProjectTool
from .query_task import QueryTaskTool

__all__ = ["Tool", "QueryProjectTool", "QueryTaskTool"]
```

这样外面 `from tools import Tool` 就能直接拿到，不用关心内部文件结构——**类似 Java 包的 facade**。

**相对导入**（包内部用）：

```python
# tools/query_project.py
from .base import Tool       # . 表示当前包，相对导入
```

**绝对导入**：

```python
from tools.base import Tool  # 从项目根开始，绝对导入
```

> 项目内部推荐**绝对导入**，IDE 支持好、不易出错。相对导入主要用于包内部复用。

### 3.6 PYTHONPATH 与导入搜索

Python 查找模块的顺序：

```text
1. 当前目录
2. PYTHONPATH 环境变量里的目录
3. 解释器 site-packages（第三方库）
4. 标准库
```

**常见坑**：`main.py` 在 `src/` 下，从项目根跑 `python src/main.py`，`from tools import ...` 会找不到——因为「当前目录」是 `src/`，而 `tools` 可能在别的位置。

**解决**：

- 从 `main.py` 所在目录跑：`cd src && python main.py`
- 或设 `PYTHONPATH=.`
- 或用 `python -m src.main`（把项目根当包跑）

> 这是 Python 项目最常见的「ImportError: No module named xxx」，记住排查方向。

### 3.7 `__pycache__` 是什么

跑过之后会出现 `__pycache__/` 目录，里面是 `.pyc` 字节码缓存——类似 Java 的 `.class`，但 Python 自动生成。**可以删，不用提交 git**。`.gitignore` 里加 `__pycache__/` 和 `.venv/`。

## 4. 编码练习

### 任务 1：建项目 + venv

```bash
mkdir -p ~/construction-agent && cd ~/construction-agent
python -m venv .venv
source .venv/bin/activate
pip install httpx pydantic fastapi uvicorn \
  -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 任务 2：锁定依赖

```bash
pip freeze > requirements.txt
cat requirements.txt    # 应看到 httpx==0.x.x / pydantic==2.x.x ...
```

### 任务 3：拆分 Day1 代码成包

把 Day1 的 `tool_framework.py` 拆成：

```text
construction-agent/
├── .venv/
├── requirements.txt
├── .gitignore              # 加 .venv/ 和 __pycache__/
├── tools/
│   ├── __init__.py
│   ├── base.py            # Tool 基类
│   ├── query_project.py   # QueryProjectTool
│   └── query_task.py      # QueryTaskTool
└── main.py                 # 入口
```

`tools/base.py`：

```python
class Tool:
    def __init__(self, name: str, description: str = ""):
        self.name = name
        self.description = description

    def run(self, input: dict) -> str:
        raise NotImplementedError

    def __str__(self):
        return f"Tool(name={self.name})"
```

`tools/query_project.py`：

```python
from .base import Tool

class QueryProjectTool(Tool):
    def __init__(self):
        super().__init__(name="query_project", description="查询项目信息")

    def run(self, input: dict) -> str:
        project_id = input.get("projectId")
        if not project_id:
            raise ValueError("projectId 不能为空")
        return f"项目 {project_id} 的数据"
```

`tools/__init__.py`：

```python
from .base import Tool
from .query_project import QueryProjectTool
from .query_task import QueryTaskTool

__all__ = ["Tool", "QueryProjectTool", "QueryTaskTool"]
```

`main.py`：

```python
from tools import QueryProjectTool, QueryTaskTool

def main():
    tool = QueryProjectTool()
    print(tool.run({"projectId": "826051217000001469"}))

if __name__ == "__main__":
    main()
```

### 任务 4：跑通 + 验证依赖还原

```bash
python main.py                    # 应打印项目数据
deactivate                        # 退出虚拟环境
rm -rf .venv                      # 删掉环境
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt   # 从 requirements 还原
python main.py                    # 应再次跑通
```

### 任务 5：写 `.gitignore`

```text
.venv/
__pycache__/
*.pyc
.env
```

### 验证

- `from tools import Tool` 能导入（`__init__.py` 起作用）
- 删 `.venv` 后 `pip install -r requirements.txt` 能完整还原
- `python main.py` 跑通

## 5. 常见坑

### 坑 1：没激活 venv 就装包

```bash
pip install httpx      # 装到全局了！
```

装之前一定看提示符有没有 `(.venv)`。`which pip` 应指向 `.venv/bin/pip`，否则装错地方。

### 坑 2：把 `.venv` 提交进 git

`.venv` 几百 MB 且跟机器相关，**绝不提交**。`.gitignore` 必加 `.venv/`。别人 clone 后自己 `python -m venv .venv && pip install -r requirements.txt` 还原。

### 坑 3：`requirements.txt` 不锁版本

```text
httpx             # ❌ 没版本，哪天升级就挂
httpx==0.27.2     # ✅ 锁死
```

线上必须用 `==` 锁。`pip freeze` 自动带版本，别手写。

### 坑 4：循环导入

```python
# a.py
from b import B

# b.py
from a import A    # ❌ a 导 b，b 又导 a，ImportError
```

**解决**：把公共依赖抽到第三个模块，或用函数内导入（延迟导入）。

### 坑 5：`__init__.py` 忘建

目录里没 `__init__.py`（Python 3.3+ 名为 namespace package 也能用，但行为微妙）。**项目内包都建 `__init__.py`**，省得踩坑。

### 坑 6：从错误位置跑导致 ImportError

```bash
cd tools && python ../main.py    # ❌ 当前目录是 tools，找不到 tools 包
```

**解决**：从项目根跑 `python main.py`，或 `python -m main`。

### 坑 7：装了包却 `ModuleNotFoundError`

```bash
pip install httpx       # 装到全局
python main.py          # 在 .venv 里跑，找不到 httpx
```

**原因**：装的环境和跑的环境不是同一个。确认 `which pip` 和 `which python` 指向同一个 `.venv`。

## 6. 验收标准

- [ ] 能创建、激活、退出 venv，能解释激活做了什么
- [ ] 能用 pip 安装指定版本包，并配置国内镜像加速
- [ ] 能用 `pip freeze > requirements.txt` 锁定，`pip install -r` 还原
- [ ] 能解释 venv 和 Maven 依赖隔离的区别
- [ ] 能把 Day1 单文件代码拆成 `tools/` 包，`from tools import ...` 导入
- [ ] 能解释 `__init__.py` 的作用
- [ ] `.gitignore` 包含 `.venv/` 和 `__pycache__/`
- [ ] 删掉 `.venv` 后能用 `requirements.txt` 完整还原并跑通 `main.py`

**最终产出**：`construction-agent/` 项目骨架，含 venv、`requirements.txt`、`tools/` 包、`main.py`，能一键还原依赖并运行。

## 7. 自测题

> 1. 为什么要用 venv？不用会怎样？
>
> 2. venv 和 Maven 的依赖隔离机制有什么本质区别？
>
> 3. 激活 venv 时 shell 做了什么？`deactivate` 又做了什么？
>
> 4. `requirements.txt` 为什么要用 `==` 锁版本？不锁会怎样？
>
> 5. `pip freeze` 和 `pip list` 有什么区别？
>
> 6. Python 的「模块」和「包」分别是什么？Java 里对应什么？
>
> 7. `__init__.py` 有什么作用？不建会怎样？
>
> 8. `from tools.base import Tool` 和 `from tools import Tool` 有什么区别？后者依赖什么？
>
> 9. 相对导入 `from .base import Tool` 的 `.` 表示什么？
>
> 10. 跑 `python main.py` 报 `ModuleNotFoundError: No module named 'tools'`，可能的原因有哪些？怎么排查？
>
> 11. `.venv` 要不要提交 git？为什么？别人怎么还原你的环境？
>
> 12. `__pycache__` 是什么？要不要提交？

全部能答 → 进入 [[day3]]。
