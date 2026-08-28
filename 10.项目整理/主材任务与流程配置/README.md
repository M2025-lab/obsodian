# 主材任务与流程配置（目录索引）

> 来源：2026-08-28 对 edar-starlord 主材任务链路的逐层代码调查。
> 所有结论均标注 文件路径:行号，代码仓库：`/Users/mirror/IdeaProjects/edar-starlord`（Spring Boot 多模块：web/api/service/dao/manager/base）。
> 不确定/无法从代码判定的事项集中在 [[99-待确认事项]]，未与结论混写。

## 阅读顺序（一条主线：业务问题 → 配置从哪来 → 任务怎么生成 → 怎么流转）

| 序号 | 文档 | 回答的问题 |
|---|---|---|
| 01 | [[01-接口速查与curl]] | 主材任务生成接口是哪个？各配置查询接口怎么调？ |
| 02 | [[02-配置体系与数据来源]] | 配置人员怎么配？模板（测量→复尺）数据来自哪里？node_type 来自哪里？ |
| 03 | [[03-任务生成与数据落表]] | 创建任务时查什么配置？node_task 数据来自哪？表间怎么关联？ |
| 04 | [[04-任务流转机制]] | 节点怎么流转？任务（测量→复尺）之间怎么流转？ |
| 05 | [[05-业务类型与整装零售]] | 业务类型是什么？整装/零售如何区分、如何命中不同配置？ |
| 99 | [[99-待确认事项]] | 代码无法判定、需线下确认的点 |

## 一图总览

![image.png](https://file.ljcdn.com/codelink-web/ob/1787917916723-bbd35860-719d-43d8-8f9b-0217ae6b4af2.png)

## 核心结论速览（详见各分篇）

1. **主材任务生成接口**：`POST /api/construction/task/create`（主用）；`POST /api/task/create` 已 @Deprecated。
2. **配置查询是生成的前置必要条件**：查不到模板配置直接 return，不生成任务。
3. **模板的"任务序列"来自 OFC**，本地 `n_material_process_template` 是只读镜像（本仓库零写入）。
4. **node_type 是本地枚举**（NodeTypeEnum，8 个值），配置人员页面逐节点选择，不是 OFC 下发。
5. **节点级流转**：`task_dispatch.node_task` 字符串顺序 + `activateNextNode`。
6. **任务级流转（测量→复尺）**：`n_material_route` type=2 连线 + `activateNextTaskDispatch` 级联激活。
7. **整装/零售区分只发生在规则入口层**（rule_unit），品类规则/模板层业务无关。
