# 跟单任务搜索接口逻辑梳理（artisan-construction）

> 🔗 飞书：https://beike.feishu.cn/docx/AnN5dikscogF4gxA84zcefTknSg
>
> 梳理日期：2026-10-22
> 项目：/Users/mirror/IdeaProjects/artisan-construction
> 代码链路：`FollowUpTaskController` → `FollowUpTaskQueryServiceImpl` → `SalesProjectEsRepositoryImpl` / `FollowUpEsRepositoryImpl` / `FollowUpScopeServiceImpl` / `FollowUpConfig`

涉及接口：

| 接口 | 功能 |
|---|---|
| `POST api/construction/follow-up/task/search` | 跟单任务 ES 条件查询（page 的 ES 平替） |
| `POST api/construction/follow-up/task/order/search` | 模糊搜索订单 id 列表（search 的前置配套） |
| `POST api/construction/follow-up/task/materials` | 查询配置的跟单品类列表 |
| `POST api/construction/follow-up/task/companies` | 查询跟单可选公司 |
| `POST api/construction/follow-up/task/project-statuses` | 查询跟单可选项目状态 |

---

## 0. 共用的"服务端强制过滤漏斗"

`search` 和 `order/search` 共享同一套**不接受前端指定**的默认范围（`FollowUpTaskQueryServiceImpl` 中 `ORDER_SEARCH_STATUSES` + `resolveConfiguredMdmCodes()`）：

1. **只查配置分公司下的订单**：分公司取 Apollo `follow.up.task.companies`（`FollowUpConfig`），提取 mdmCode 去重去空。**未配置 → 直接返回空页/空列表，绝不退化成全量查询**（fail-closed，`SalesProjectEsRepositoryImpl#listProjectOrderIdsByMdmCodes` 里也有同样兜底）。
2. **只查施工中阶段的项目**：项目状态由服务端硬编码为 6 个 `StatusEnum`：
   - 302 未技术交底
   - 303 未隐蔽验收
   - 304 未中期验收（瓦木工程阶段）
   - 305 未基础竣工验收
   - 306 未竣工验收
   - 310 未软装验收
   入参无法覆盖。
3. **原因**：跟单任务 ES 索引里**没有分公司/项目状态字段**，范围收敛只能在订单 ES 侧先做，拿到订单 id 后再用 terms 收敛任务查询。

---

## 1. task/search — 跟单任务 ES 条件查询

**功能定位**：与老的 `task/page`（纯 MySQL）同参数、同返回结构，可无缝平替，但筛选能力更强（新增跟进人、大区、下次跟进时间、排序）。

### 执行流程

1. **分公司收敛**：用配置分公司 + 固定 6 状态去订单 ES 捞出**全量**订单 id（每页 1000、按 projectOrderId 升序翻页保证多页稳定；任何一页失败返回空，不给出不完整范围）。
2. **与入参订单条件取交集**：请求带 `projectOrderId`（单值）或 `projectOrderIds`（多值，即模糊搜索的回传结果）时 retainAll；**交集为空或未配置分公司 → 直接返回空页，不再查任务 ES**。
3. **任务 ES 查询**：交集订单 id + 其余筛选（任务类型/状态/风险等级/供应商/品类/订单号/bizId/来源/**跟进人 ucid**/**作业大区列表**/**下次跟进时间区间**）走搜索平台 SEARCH API。**ES 只出任务 id 和 total，详情回 MySQL 组装**（避免 ES 与 DB 不一致时返回脏详情）。
4. **保持 ES 顺序回填**：按 ES 返回的 id 顺序 selectByIds 后重排，再挂跟进记录和订单信息。

### 新增的"下次跟进时间"筛选

- 闭区间 `nextFollowUpTimeBegin/End`；ES 文档该字段来自**跟进记录最新一条约跟进的 next_follow_up_time**（推送时聚合写入），**从未约过下次跟进的任务不会命中**该筛选。
- 边界自动补全：只传 `yyyy-MM-dd` 时 begin 补 `00:00:00`、end 补 `23:59:59`（结束边界兜到当天末尾）；到分钟的补 `:00`。
- 起止颠倒不报错，只打日志，ES 闭区间自然返回 0 条。

### 排序逻辑

- `sortField` 支持 `ctime` / `risk_level` / `assessment_end_time` / `next_follow_up_time`，`sortOrder` 缺省 **desc**。
- **自动追加次级排序 `follow_up_task_id asc` 兜底**：主排序字段并列（含空值/缺失）时保证翻页稳定、不重复不丢数据。
- 空串字段推送 ES 时归一为 null（字段缺失），升/降序都排末尾，行为一致。
- 分页：入参页码从 1 起转 ES 的 0 起；pageSize 默认 20、**上限 300**。

### 返回值组装（每条任务）

- 任务本体
- 全部跟进记录流水；`nextFollowUpTime` = 按记录 id 倒序第一条非空的约跟进时间
- 订单侧信息：业主姓名、服务者列表、物业地址、订单阶段及描述、按 handlerRole 匹配订单服务者得到的处理人姓名
- **瓦木实际完成时间**：仅订单处于 304 瓦木阶段时返回实际竣工日期，其它阶段留空（前端不展示）

---

## 2. task/order/search — 模糊搜索订单 id

**功能定位**：页面顶部模糊搜索框专用，"**先查订单、再查任务**"两段式的第一步；返回的 id 列表由前端回传给 `task/search` 的 `projectOrderIds`。

- 关键字 `search` 必填，在订单 ES 上 6 字段模糊匹配：项目订单 id、家装订单号、业主姓名、项目地址、服务者姓名列表、履约单号列表（与 hestia `/api/order/project/page/luban/v2` 同款字段集）。
- 分公司**只取服务端配置**、项目状态**只取服务端固定枚举**，均不接受入参 —— 与任务查询范围口径完全一致。
- 模糊搜索**只取第一页**（1000 条），再与**跟单范围表** `follow_up_project_scope`（由 `task/scope/sync` 系列接口维护）按订单 id 取交集 —— 双重收敛。
- 结果**截断 100 条**，防前端把超长 id 列表回传任务查询（ES terms 过大）。

---

## 3. task/materials — 查询配置的跟单品类列表

- 品类**范围**来自 Apollo `follow.up.task.generate.rule` 中 `materialTaskTypes` 的 key 集合（材料 code → 需跟进任务类型列表），即"**只有会生成跟单任务的品类才出现在下拉里**"，与任务生成规则天然一致。
- 品类**名称**调 SCM 按 `category.configuration`（默认 **029 履约类目**）查询后匹配；匹配不到时用 code 兜底。
- 规则未配置或 SCM 返回空 → 返回空列表。

---

## 4. task/companies + task/project-statuses — 筛选项元数据接口

- **companies**：原样返回 Apollo `follow.up.task.companies` 配置（公司名称 + 分公司编码 mdmCode），未配置返回空列表。查询接口不接受分公司入参，此接口是**只读透出**（展示当前跟单覆盖哪些分公司）。
- **project-statuses**：遍历订单 `StatusEnum`，过滤出固定 6 状态（302/303/304/305/306/310），**按编码升序**返回 code + name。让前端"项目状态"下拉与 `search`/`order/search` 实际使用的状态**同源同口径**，避免前端硬编码枚举后与服务端漂移。

---

## 设计取向总结

| 维度 | 取向 |
|---|---|
| 范围控制 | 分公司、项目状态全部服务端收口（Apollo 配置 + 硬编码枚举），前端只能在此范围内二次筛选 |
| 异常/空配置 | 一律 fail-closed：返回空页/空列表，绝不放开全量 |
| 数据一致性 | ES 只负责"筛选 + 排序 + 出 id"，详情一律回 MySQL/订单服务实时组装 |
| 分页稳定性 | 订单侧按 projectOrderId 升序翻页；任务侧排序自动加 id 兜底键 |
| 配套关系 | `order/search`（模糊搜 id，截断100）→ `search`（id 交集 + 全条件 ES 查询）；`materials`/`companies`/`project-statuses` 提供筛选项元数据 |

## 与飞书文档的差异

飞书《任务总览-跟进任务 前端对接文档》目前只登记了 `page` / `handle` / `scope/sync` / `materials` 四个接口；`search`、`order/search`、`companies`、`project-statuses` 尚未写入，需要时可补充进该文档。
