# 通知指标统计 SQL（七项）

> 来源需求：[[数据需要]]
> 指标 6/7 为「自动通知品类自动完成通知复尺」需求新增：**驳回特殊标记**统计口径（区分手动通知的驳回、及常规自动通知复尺的驳回）。
> 涉及表（大数据平台表名）：
> - `ods.ods_plat_starlord_material_site_recognize_result_da`（识别结果表，别名 `sit`）
> - `dw.dw_plat_starlord_task_dispatch_node_da`（节点任务表，别名 `n`）
> - `dw.dw_plat_starlord_task_dispatch_da`（任务调度表，别名 `task_dispatch`）
>
> ⚠️ 三张表均为分区表，查询必须带 `pt = '20261027000000'`（按需替换分区值）

## 一、指标口径分析

| # | 指标 | 数据来源 | 判断逻辑 |
|---|------|---------|---------|
| 1 | 一键通知的数量 | `material_site_recognize_result` | `notify_execute_type = 1`（该表只存一键/自动通知，不含手动） |
| 2 | 手动通知经过系统识别的数量 | `task_dispatch_node` + 识别表 | `submit_name` 只有姓名（无后缀）→ 手动；`process_status = 3`；能反查到识别记录 → 经过系统识别 |
| 3 | 经过系统识别驳回的数量 | `task_dispatch_node` + 识别表 | 驳回 = 60 节点 `process_status = 3 AND qualified = 2`；且能反查到识别记录 → 经过系统识别 |
| 4 | 未经过系统识别驳回的数量 | `task_dispatch_node` | 驳回 = 60 节点 `process_status = 3 AND qualified = 2`；且反查不到识别记录 |
| 5 | 手动通知未经过系统的数量 | `task_dispatch_node` | `submit_name` 只有姓名、`process_status = 3`，且反查不到识别记录 |
| 6 | 驳回复尺任务总数 | `task_dispatch_node` + `task_dispatch` | 60 节点（`node_type = 60`）：`process_status = 3 AND qualified = 2`；且 `task_dispatch.task_type = 3`（复尺任务） |
| 7 | 自动通知驳回数（驳回特殊标记） | `task_dispatch_node` + `task_dispatch` + 识别表 | 同指标 6 口径；且反查识别记录存在、`notify_execute_type = 2`（自动通知） |

**全局约束**：7 个指标全部限定**复尺任务** `task_dispatch.task_type = 3`（指标 1 / UNION 第一分支加在反查 `task_dispatch` 的 EXISTS 子查询内，借 `task_type = sit.task_type` 同时约束识别表）。

**关联链路**：

```
task_dispatch_node.task_dispatch_id → task_dispatch.id
task_dispatch (project_order_id / material_code / task_type)
    → material_site_recognize_result (同三字段关联)
```

**submit_name 区分执行方式**：

- `姓名（一键通知）` → 一键通知
- `姓名（自动通知）` → 自动通知
- 只有 `姓名` → 手动通知

## 二、口径说明（均已与业务确认）

1. ✅ `task_dispatch` 的 DDL 已确认：关联字段 `project_order_id、material_code、task_type` 与实际列名一致；统计时补充 `td.state = 1` 排除无效任务；
2. ✅ 通知类节点（指标 2/5）只取 `node_type = 20`（通知可启动）；驳回类指标（3/4/6/7）取 **60 节点** `node_type = 60`（启动/完工自检，即执行节点）；
3. ✅ "驳回"统一以 Node 表 60 节点判断：`process_status = 3`（激活已完成）**且** `qualified = 2`（不合格，即驳回）；旧口径 `process_status = 2 AND restart > 0` 与 `judge_result = 1` 均已废弃。
4. ✅ 指标 6/7（驳回特殊标记）口径：驳回复尺任务以 **60 节点**（`node_type = 60`）+ `task_dispatch.task_type = 3` 圈定；其中"自动通知驳回"= 反查识别记录存在且 `notify_execute_type = 2`。与指标 3/4 的区别：同为 60 节点驳回，3/4 按能否反查识别记录拆分（3+4=6），6/7 中的 7 再按 `notify_execute_type = 2` 拆出自动通知驳回（即需求中"驳回了多少 / 驳回了自动通知的多少"）。
5. ✅ 全部指标限定复尺任务 `task_dispatch.task_type = 3`；口径对齐后恒等式成立：**3 + 4 = 6**（按识别记录拆）、**7 ⊆ 6**（按自动通知拆）。此前 3+4≠6 的原因即 3/4 未限 task_type，混入了非复尺任务的驳回。

## 三、分项 SQL

```sql
-- ========== 1. 一键通知的数量 ==========
SELECT COUNT(*) AS cnt
FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
WHERE sit.notify_execute_type = 1
  AND sit.pt = '20261027000000'
  AND sit.material_code IN ('029006007','029006002','029006006','029006003')        -- 品类
  AND EXISTS (SELECT 1 FROM dw.dw_plat_starlord_task_dispatch_da task_dispatch
              WHERE task_dispatch.project_order_id = sit.project_order_id
                AND task_dispatch.material_code    = sit.material_code
                AND task_dispatch.task_type        = sit.task_type
                AND task_dispatch.task_type = 3   -- 复尺任务（同时约束 sit.task_type）
                AND task_dispatch.state = 1
                AND task_dispatch.pt = '20261027000000'
                AND task_dispatch.gb_code IN ('610100','610400','440100','440600'))  -- 城市
  -- AND sit.gmt_create >= '2026-10-01' AND sit.gmt_create < '2026-11-01'   -- 按需加时间范围
;

-- ========== 2. 手动通知 且 经过系统识别 ==========
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
JOIN ods.ods_plat_starlord_material_site_recognize_result_da sit
       ON sit.project_order_id = task_dispatch.project_order_id
      AND sit.material_code    = task_dispatch.material_code
      AND sit.task_type        = task_dispatch.task_type
WHERE n.state = 1
  AND n.node_type = 20                            -- 通知类节点（已确认只取 20-通知可启动）
  AND n.submit_name REGEXP '^[^（）()]+$'               -- 只有姓名、无（一键通知）/（自动通知）后缀 → 手动
  AND n.process_status = 3                          -- 处理状态 = 3
  AND n.pt = '20261027000000'
  AND sit.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
;

-- ========== 3. 经过系统识别被驳回的数量 ==========
-- 口径：驳回 = Node 表 60 节点 process_status = 3 且 qualified = 2（不合格）；经过系统识别 = 能反查到识别记录
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 60                              -- 60 节点：启动/完工自检（执行节点）
  AND n.process_status = 3                          -- 驳回判断①：激活已完成
  AND n.qualified = 2                               -- 驳回判断②：不合格（QualifiedEnum.UNQUALIFIED）即驳回
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND EXISTS (
      SELECT 1
      FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
      WHERE sit.project_order_id = task_dispatch.project_order_id
        AND sit.material_code    = task_dispatch.material_code
        AND sit.task_type        = task_dispatch.task_type
        AND sit.pt = '20261027000000'
  )
;

-- ========== 4. 未经过系统识别 被驳回的数量 ==========
-- 口径：驳回 = Node 表 60 节点 process_status = 3 且 qualified = 2（不合格），但反查不到识别记录
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 60                              -- 60 节点：启动/完工自检（执行节点）
  AND n.process_status = 3                          -- 驳回判断①：激活已完成
  AND n.qualified = 2                               -- 驳回判断②：不合格（QualifiedEnum.UNQUALIFIED）即驳回
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND NOT EXISTS (
      SELECT 1
      FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
      WHERE sit.project_order_id = task_dispatch.project_order_id
        AND sit.material_code    = task_dispatch.material_code
        AND sit.task_type        = task_dispatch.task_type
        AND sit.pt = '20261027000000'
  )
;

-- ========== 5. 手动通知 且 未经过系统识别 ==========
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'               -- 手动通知
  AND n.process_status = 3                          -- 处理状态 = 3
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND NOT EXISTS (
      SELECT 1
      FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
      WHERE sit.project_order_id = task_dispatch.project_order_id
        AND sit.material_code    = task_dispatch.material_code
        AND sit.task_type        = task_dispatch.task_type
        AND sit.pt = '20261027000000'
  )
;

-- ========== 6. 驳回复尺任务总数（60 节点驳回） ==========
-- 口径：60 节点（node_type = 60，复尺执行节点）process_status = 3 且 qualified = 2（不合格即驳回）；task_dispatch.task_type = 3（复尺任务）
-- 与指标 3/4 的区别：同为 60 节点驳回且同限复尺任务；3/4 按识别记录拆分（3+4=6），本指标即"所有驳回的复尺任务"全量
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 60                            -- 60 节点：启动/完工自检（复尺执行节点）
  AND n.process_status = 3                        -- 驳回判断①：激活已完成
  AND n.qualified = 2                             -- 驳回判断②：不合格即驳回
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
;

-- ========== 7. 自动通知驳回数（驳回特殊标记） ==========
-- 口径：同指标 6，且反查识别记录存在、notify_execute_type = 2（自动通知）
-- 即需求中"本场景下的自动通知，驳回特殊标记"：自动完成通知复尺后被驳回的量；非自动通知驳回 = 指标 6 - 指标 7
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 60                            -- 60 节点：启动/完工自检（复尺执行节点）
  AND n.process_status = 3                        -- 驳回判断①：激活已完成
  AND n.qualified = 2                             -- 驳回判断②：不合格即驳回
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND EXISTS (
      SELECT 1
      FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
      WHERE sit.project_order_id = task_dispatch.project_order_id
        AND sit.material_code    = task_dispatch.material_code
        AND sit.task_type        = task_dispatch.task_type
        AND sit.notify_execute_type = 2           -- 自动通知（NotifyExecuteTypeEnum.AUTO_NOTIFY）
        AND sit.pt = '20261027000000'
  )
;
```

## 四、一次跑出 7 个数（UNION ALL 汇总）

```sql
SELECT '1-一键通知数量' AS metric, COUNT(*) AS cnt
FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
WHERE sit.notify_execute_type = 1
  AND sit.pt = '20261027000000'
  AND sit.material_code IN ('029006007','029006002','029006006','029006003')        -- 品类
  AND EXISTS (SELECT 1 FROM dw.dw_plat_starlord_task_dispatch_da task_dispatch
              WHERE task_dispatch.project_order_id = sit.project_order_id
                AND task_dispatch.material_code    = sit.material_code
                AND task_dispatch.task_type        = sit.task_type
                AND task_dispatch.task_type = 3   -- 复尺任务（同时约束 sit.task_type）
                AND task_dispatch.state = 1
                AND task_dispatch.pt = '20261027000000'
                AND task_dispatch.gb_code IN ('610100','610400','440100','440600'))  -- 城市
UNION ALL
SELECT '2-手动通知经过系统识别', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'
  AND n.process_status = 3
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
              WHERE sit.project_order_id = task_dispatch.project_order_id
                AND sit.material_code = task_dispatch.material_code
                AND sit.task_type = task_dispatch.task_type
                AND sit.pt = '20261027000000')
UNION ALL
SELECT '3-经过系统识别驳回', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 60
  AND n.process_status = 3 AND n.qualified = 2
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
              WHERE sit.project_order_id = task_dispatch.project_order_id
                AND sit.material_code = task_dispatch.material_code
                AND sit.task_type = task_dispatch.task_type
                AND sit.pt = '20261027000000')
UNION ALL
SELECT '4-未经过系统识别驳回', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 60
  AND n.process_status = 3 AND n.qualified = 2
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND NOT EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
                  WHERE sit.project_order_id = task_dispatch.project_order_id
                    AND sit.material_code = task_dispatch.material_code
                    AND sit.task_type = task_dispatch.task_type
                    AND sit.pt = '20261027000000')
UNION ALL
SELECT '5-手动通知未经过系统', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'
  AND n.process_status = 3
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3                 -- 复尺任务
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND NOT EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
                  WHERE sit.project_order_id = task_dispatch.project_order_id
                    AND sit.material_code = task_dispatch.material_code
                    AND sit.task_type = task_dispatch.task_type
                    AND sit.pt = '20261027000000')
UNION ALL
SELECT '6-驳回复尺任务总数', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 60
  AND n.process_status = 3 AND n.qualified = 2
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
UNION ALL
SELECT '7-自动通知驳回(特殊标记)', COUNT(*)
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1 AND n.node_type = 60
  AND n.process_status = 3 AND n.qualified = 2
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
  AND task_dispatch.task_type = 3
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
              WHERE sit.project_order_id = task_dispatch.project_order_id
                AND sit.material_code = task_dispatch.material_code
                AND sit.task_type = task_dispatch.task_type
                AND sit.notify_execute_type = 2
                AND sit.pt = '20261027000000');
```

## 五、补充说明

- **手动通知的正则**：`'^[^（）()]+$'` 表示整串不含全角/半角括号，即"只有姓名"。若后缀写法固定，更直白的写法：
  `submit_name NOT LIKE '%一键通知%' AND submit_name NOT LIKE '%自动通知%' AND submit_name <> ''`
- **`process_status` / `qualified`（节点状态）**：指标 2/5（手动通知）取 20 节点 `process_status = 3`；驳回类指标 3/4/6/7 统一取 60 节点 `process_status = 3 AND qualified = 2`（`ProcessStatusEnum`：3-激活已完成；`QualifiedEnum`：1-合格、2-不合格，`ProcessCombineStatusEnum.REJECT` 即由 qualified=2 派生）；指标 1 不涉及 Node 表。
- **时间范围**：所有查询可按需加时间过滤，建议统一用 `submit_time`（通知任务实际提交时间）作为统计时间轴。
- `task_dispatch` DDL 已核对：`project_order_id、material_code、task_type` 列名一致，且已在所有关联中补充 `task_dispatch.state = 1` 排除无效任务。
- 口径已全部确认：通知类节点（2/5）只取 `node_type = 20`；"驳回"统一以 Node 表 60 节点判断 —— `process_status = 3`（激活已完成）且 `qualified = 2`（不合格即驳回）；旧口径 `process_status = 2 AND restart > 0`、`judge_result = 1` 均已废弃。
- **分区字段 `pt`**：三张大数据表均需带分区过滤，当前统一为 `pt = '20261027000000'`（2026-10-27 分区）；换统计日期时，注意同步替换所有出现位置（含 `EXISTS` / `NOT EXISTS` 子查询内部的 `sit.pt`）。
- 若引擎不支持 `REGEXP` 关键字（如 Hive），可改写为 `n.submit_name RLIKE '^[^（）()]+$'`，语义相同。
- **品类 / 城市过滤**（全部 5 个指标已统一加上）：
  - 品类：`material_code IN ('029006007','029006002','029006006','029006003')`
  - 城市：`gb_code IN ('610100','610400','440100','440600')`（西安、咸阳、广州、佛山）
  - 指标 2/3/4/5 直接在 `task_dispatch` 上过滤；指标 1 主表是 `sit`（无 `gb_code`），品类用 `sit.material_code` 过滤，城市通过 `EXISTS` 反查 `task_dispatch.gb_code` 实现（用 EXISTS 而非 JOIN，避免一条识别记录匹配多条调度任务时重复计数）。
  - ⚠️ 隐含口径：指标 1 要求识别记录**必须能关联到有效的 `task_dispatch`（state=1）**才计数；若识别表本身有城市字段（如 `gb_code`/`city_code`），可改为直接过滤，去掉 EXISTS 子查询。
- **指标 6/7（驳回特殊标记）口径要点**：
  - 主体是**60 节点**：`node_type = 60`（`NodeTypeEnum.START` 启动/完工自检，即复尺执行节点），不是指标 2/5 用的通知节点 `node_type = 20`；`task_dispatch.task_type = 3`（`TaskTypeEnum.RECHECK_SCALE`）圈定复尺任务。
  - 驳回判断沿用统一口径：`process_status = 3 AND qualified = 2`（60 节点已完成且不合格）。
  - "自动通知驳回"= 按 `(project_order_id, material_code, task_type)` 反查识别记录存在，且 `notify_execute_type = 2`（`NotifyExecuteTypeEnum`：0-未执行 / 1-一键通知 / 2-自动通知 / 3-手动通知）。
  - 两个数的关系：指标 6 = 驳回复尺任务全量；指标 7 = 其中驳回自动通知的（即需求红框"驳回特殊标记"）；**非自动通知驳回 = 6 − 7**，无需单独跑。
  - ⚠️ 品类 / 城市过滤与前 5 项保持一致；若要看全量驳回（不限品类城市），去掉对应 IN 条件即可。
- **复尺任务限制**：全部 7 个指标已加 `task_dispatch.task_type = 3`（指标 1 / UNION 第一分支加在 EXISTS 子查询内）。此前"3+4≠6"即因 3/4 未限 task_type；对齐后 3+4=6、7⊆6 恒成立，可用作跑数自检。
