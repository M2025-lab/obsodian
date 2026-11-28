# starlord-queryByPlan单号条件扩展-技术方案

> 飞书文档：<链接占位，用户有则填>

## 1、背景

agenda 日报推送取数（[[eden-agenda-推送数据源queryProgress迁移-技术方案]]）拟从旧 `/task-supplier/progress/query` 切到新 `/task-supplier/progress/query-by-plan`，但新接口**没有任何单号条件**——agenda 批查C（按池B货单号集拉全量 plans 判「已排送」）无法用新接口表达，传 taskType=5 单条件等于查全库。本方案在 starlord 侧给 `query-by-plan` 补**货单号 + 项目单号**两个可选入参，作为 agenda 全量切新接口的前置改造（口径：两入参可不传，不传时行为与现状一致）。

## 2、需求理解

| # | 需求点 | 说明 |
|---|---|---|
| 1 | 加货单号查询 | 入参加 `orderNos`（IN，可选） |
| 2 | 加项目单号查询 | 入参加 `projectOrderIds`（IN，可选） |
| 3 | 两个入参均非必填 | 单号条件与既有 plan 条件任意组合；守卫放宽为「单号条件或 plan 条件至少传一个」，全不传仍报错 |

## 3、现状

`query-by-plan` 现链路（两表分工：`supplier_progress` 关系 / `supplier_plan` 时间，plan 表 2026-11-20 起已删 project_order_id 等四列，SupplierPlan.java:17-18）：

```
POST /task-supplier/progress/query-by-plan        SupplierProgressFeign.java:63
  └─ SupplierProgressController#queryProgressByPlan（SupplierProgressController.java:79）
       └─ SupplierProgressServiceImpl#queryByPlan（SupplierProgressServiceImpl.java:357）
            ├─ normalizeAndValidateProgressByPlanQuery（:418）
            │    · taskType/factTime/expectTime 至少传一个，否则 IllegalArgumentException（:427-430）
            │    · flowType 只支持 0/1（:432）
            ├─ ① taskSupplierPlanDao.queryForProgress(param)        先筛 supplier_plan（SupplierPlanDao.java:113）
            │      SupplierPlanMapper.xml:184 selectForProgressQuery：单表，仅 taskType/expected_time/fact_time 条件
            │      order by order_no asc, task_type asc, id asc（:211）
            ├─ ② supplierProgressDao.queryByOrderNos(命中货单号集)   反查 supplier_progress（SupplierProgressServiceImpl.java:377）
            │      DAO 层内部已按 500 分批（SupplierProgressDaoImpl.java:54 BATCH_SIZE=500）
            └─ ③ 内存组装：flowType 过滤（:387）+ plans 装配（:394-413）
```

现状事实表：

| 事实 | 代码依据 |
|---|---|
| 入参仅 6 个字段，无任何单号条件 | SupplierProgressByPlanQueryParam.java:31-50 |
| 全部条件落在**同一条 plan 行**上（AND） | SupplierProgressServiceImpl.java:360、SupplierPlanMapper.xml:188-210 |
| plan 表单号/主单号维度已删，只有 order_no | SupplierPlan.java:39（orderNo 是唯一单号列） |
| project_order_id 在 supplier_progress 表 | SupplierProgressMapper.xml:12 |
| 旧接口 queryProgress 的单号实现姿势：三单号 IN（上限 500，limitIn 去空去重） | SupplierProgressServiceImpl.java:303-305、1051-1059 |
| 老出参 plans 元素仅 taskType/expectTime/factTime 三字段 | SupplierProgressByPlanItemVO.java:20-30 |

## 4、详细设计

> ⚠️ 需求与代码现状不一致：用户要求「两个入参不能是必须的」——与现状守卫（plan 条件至少一个，SupplierProgressServiceImpl.java:427-430）不冲突：新增单号条件**不参与**该守卫的豁免（单号单查仍要求至少一个 plan 条件是不对的，agenda 批查C 只传 orderNos 不传时间）。**以需求为准：守卫改为「单号条件与 plan 条件至少传一个」**，单号单查合法。

### 4.1 入参扩展（SupplierProgressByPlanQueryParam）

新增两个字段（均为可选，都不传时与现状入参语义一致）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `orderNos` | List&lt;String&gt; | 货单号（IN，上限 500；组内 OR；与 plan 条件 AND 组合） |
| `projectOrderIds` | List&lt;Long&gt; | 项目单号/主单号（IN，上限 500；组内 OR；**与 orderNos 组间 OR**（并集）、与 plan 条件 AND；内部经 progress 表翻译为货单号） |

校验（normalizeAndValidateProgressByPlanQuery 内）：
- `limitIn(trimList(...))` 两个新集合，上限 500 超限报错（复用 SupplierProgressServiceImpl.java:1051 limitIn 与 trimList，与旧接口单号口径一致）
- 守卫放宽：`ObjectUtils.anyNotNull(taskType, factTime×2, expectTime×2)` **或** 两单号集非空，二者至少其一
- flowType 校验、时间区间校验不变

### 4.2 查询语义（单路径通用，用户定稿 2026-11-27）

全部条件（单号 + 类型 + 时间窗）**AND 落在同一条 plan 行上**，缺哪个条件即不参与过滤——通用语义：

```
queryByPlan(param)
  ├─ normalize（4.1）
  ├─ resolveOrderNoSet：货单号集 = orderNos ∪ projectOrderIds 翻译出的货单号（同传取并集，
  │    单传取该组；projectOrderIds 非空才查 progress 翻页翻译；无任何单号条件返回 null）
  ├─ ① taskSupplierPlanDao.queryForProgress(param)     筛 supplier_plan 单表
  │        WHERE order_no IN (货单号集)                  ← 单号 OR（组内/组间都是）
  │          AND task_type = ?                          ← 全部条件 AND 同一 plan 行
  │          AND expected_time BETWEEN ...
  ├─ ② supplierProgressDao.queryByOrderNos(命中货单号集)  小范围反查 progress
  └─ ③ 组装：flowType 过滤（货单级）+ plans 挂载 → VO
```

projectOrderIds 翻译（`resolveOrderNoSet`，复用 `queryByCondition` 分页翻全量）要点：
- plan 表无 project_order_id 列（SupplierPlan.java:17-18），必须经 supplier_progress 翻译
- 翻页传**空 `SupplierProgressQueryParam`**（全字段 null）：只留 project_order_id 单条件，不被旧 SQL 组间 AND 污染
- 翻完边界：`offset + 500 ≥ count` 或返回行数 < 500 即止；`listForQuery` 固定 `order by id asc`（SupplierProgressMapper.xml:135），offset 翻页安全

入参组合语义表（通用化核心价值）：

| 入参组合 | 语义 | plans 内容 |
|---|---|---|
| 只传时间窗/类型 | 条件命中的 plan 行所在货单 | 命中条件的 plan 行 |
| 只传 orderNos | 这些货单 | **全部** plan 行 |
| 只传 projectOrderIds | 该项目下全部货单 | 全部 plan 行 |
| **projectOrderIds + 时间窗** | **该项目下、时间窗命中的 plan 行**（新能力） | 命中条件的 plan 行 |
| orderNos + 时间窗 | 这些货单中条件命中者 | 命中条件的 plan 行 |
| projectOrderIds + orderNos | **并集**：两组货单 OR——这些货单的进展都返回（交集版试用后改回，用户定稿 2026-11-27） | 全部 plan 行 |

架构演进记录：v1（单号塞回 param 走单路径，progress 查两次，已废弃）→ v2（双路径，projectOrderIds+plan 条件同传时 projectOrderIds 被忽略，已废弃）→ **v3 定稿（单路径通用：归一集 + AND 组合，projectOrderIds 与 plan 条件同传生效，progress 查询 projectOrderIds 场景 2 次（翻译+组装）/ 其余场景 1 次）**。

### 4.3 SQL 改动（SupplierPlanMapper.xml selectForProgressQuery）

```xml
<if test="query.orderNos != null and query.orderNos.size() > 0">
  and order_no in
  <foreach collection="query.orderNos" item="item" open="(" close=")" separator=",">
    #{item,jdbcType=VARCHAR}
  </foreach>
</if>
```

加在 `task_type` 条件之后（SupplierPlanMapper.xml:189-191 同款），order by 不变（order_no asc, task_type asc, id asc）。

### 4.4 出参改动（用户定稿 2026-11-27：全量补齐）

`SupplierProgressByPlanVO.plans` 元素（SupplierProgressByPlanItemVO.java:20-30）补齐到与旧接口 `SupplierProgressPlanItemVO` 完全同构的 8 字段（新增 5 个加粗）：

| 字段 | 类型 | 用途 |
|---|---|---|
| **`planId`** | Long | plan 行主键 |
| `taskType` | Integer | 已有 |
| **`flowType`** | Integer | plan 行级正单/返补判据（agenda 板块①③判据从货单级降回 plan 行级，口径与旧接口对齐） |
| **`expectedTime`** | Date | 已有 expectTime 字段重命名（见下） |
| `factTime` | Date | 已有 |
| **`status`** | Byte | 0 进行中 / 1 已下发 |
| **`remark`** | String | 板块 remark 展示列 + 排查上游口径原文 |
| **`modifyTime`** | Date | 多次返补判断哪条最新 |

**字段命名对齐旧接口**：现状 `expectTime` 改名 `expectedTime`（旧接口 SupplierProgressPlanItemVO.java:43 同名）。改名是**契约破坏性变更**——现接口调用方只有 agenda（尚未接入）和 skill 脚本（无，引用面已核实），无存量消费者，改名成本为零；不改名则 agenda/Skill 需记忆两套字段名。**按改名设计**。

装配：③ 步 builder 加 5 字段（SupplierProgressServiceImpl.java:394-400 处，plan 模型列已全备：SupplierPlan.java:32 id / :51 flowType / :73 modifyTime / :80 remark / :85 status）。

> ⚠️ 注意：**plan 行级 flowType 与货单级 flowType 可能不同**（同一货单正单/返补共存），agenda 判据用 plan 行级——这正是旧接口的行为（旧接口 plans 元素带 flowType，SupplierProgressPlanItemVO.java:39）。

### 4.5 无需改动部分

- Controller/Feign：方法签名不变（`@RequestBody SupplierProgressByPlanQueryParam`），字段增删对 HTTP 层透明
- `queryByOrderNos` 反查链路：不变
- mock-data / sync / saveOnly / cleanup 等无关接口：零改动
- `queryByCondition`/`countByCondition` DAO 与 SQL：**零改动**（中转只是新的调用方，签名与条件不动）

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|---|---|---|---|
| `POST /task-supplier/progress/query-by-plan` | SupplierProgressFeign.java:63 / SupplierProgressController.java:79 | 入参加 `orderNos`/`projectOrderIds`（可选，IN 上限 500）；守卫放宽为「单号或 plan 条件至少一个」；单路径通用编排（单号归一 + 全条件 AND 落同一 plan 行，4.2）；SQL 加 order_no IN（SupplierPlanMapper.xml:184）；projectOrderIds 复用 `queryByCondition` 翻译（4.2）；出参 plans 元素补齐 8 字段并与旧接口命名对齐（expectTime→expectedTime，4.4） |  |

引用面核实（写方案前 grep 全量）：`selectForProgressQuery`/`queryForProgress` 调用链仅 4 处（ServiceImpl:361 → SupplierPlanDao.java:113 → SupplierPlanDaoImpl.java:173 → SupplierPlanMapper.xml:184），无测试类引用（仅 SupplierPlanDaoImplTest.java 存在于 DAO 层，不涉及该 SQL）；`queryByPlan` 无 skill/脚本引用。DTO 改动不影响序列化兼容（新增字段对老调用方 JSON 反序列化无感）。

## 6、上游依赖

无（本工程是服务提供方；agenda 侧的依赖见 [[eden-agenda-推送数据源queryProgress迁移-技术方案]] 的 jar 升级条目，属 agenda 方案范围）。

## 7、表结构改动

无（两表现有列足够：supplier_plan.order_no / supplier_progress.project_order_id）。

## 8、风险评估

| # | 风险点 | 评估 |
|---|---|---|
| 1 | projectOrderIds 中转查询量级：一个项目单号命中的货单数无上界（大项目数百货单） | 中转复用 `queryByCondition`：走 project_order_id in（SupplierProgressMapper.xml:82）+ 500 步长翻页；需核实线上该列有无索引；无索引时全表扫——上线前 `EXPLAIN` 一次，无索引则加 `KEY idx_project (project_order_id)`（supplier_order_relation.sql:15 同款已有先例） |
| 2 | orderNos 单查绕过 plan 条件时的全表量级：order_no in (500) 命中行数 = 每货单全部 plan 行（×3 类型×多版本） | 500 货单 × ~10 plan 行 ≈ 5000 行，可控；且 order by order_no 有序，全量返回无内存压力 |
| 3 | 两单号集 OR 合并后 ① 步 IN 集膨胀（入参 orderNos 500 + projectOrderIds 反查出的货单数） | 反查集去重后并入，量级 = 单项目货单数（数百级），IN 内仍为常数级；MySQL IN 上千项性能可接受 |

## 9、Check List

- [ ] Apollo 配置：无新增/变更
- [ ] SQL 变更：无表结构变更；**核实** supplier_progress.project_order_id 索引（EXPLAIN，无则补 idx_project）——见风险评估 #1
- [ ] 上线顺序：starlord 先行（本方案）→ api jar 发正式版 → agenda 升 jar 切接口（另期方案）
- [ ] 回滚方案：新增字段/条件均为可选分支，不传单号时 SQL 与现状逐字节等价（仅 4.4 字段名 expectTime→expectedTime 是破坏性变更，见该节说明）；回滚 = revert 单 commit
- [ ] 测试要点：①单传 orderNos（无 plan 条件）不再 4004，plans 为全部 plan 行；②单传 projectOrderIds 翻译正确（count+翻页边界），plans 为全部 plan 行；③**projectOrderIds + 时间窗同传**：该项目下时间窗命中的 plan 行（新能力）；④两单号都传 OR 并集语义；⑤plan 条件单传回归现状；⑥出参 plans 元素 8 字段与旧接口 SupplierProgressPlanItemVO 同构；⑦expectTime→expectedTime 改名后出参 key 正确
- [x] ~~v1 编排（中转塞回 param.orderNos 单路径，progress 查两次）~~（**已废弃 2026-11-27**）
- [x] ~~v2 编排（双路径分派：projectOrderIds 仅路径二生效，与 plan 条件同传被忽略）~~（**已废弃 2026-11-27**：通用化定稿 v3 单路径——单号归一 + 全条件 AND，见 4.2 架构演进记录）
- [x] ~~TODO①中转查询实现~~（**已定稿 2026-11-27**：复用 `queryByCondition` count+500 翻页，见 4.2）
- [x] ~~TODO②出参补齐范围~~（**已定稿 2026-11-27**：planId/status/modifyTime 一并补齐，见 4.4）

## 10、评审会议纪要

（待评审）
