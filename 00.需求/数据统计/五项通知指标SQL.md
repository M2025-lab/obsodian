# 五项通知指标统计 SQL

> 来源需求：[[数据需要]]
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
| 3 | 经过系统识别驳回的数量 | `task_dispatch_node` + 识别表 | 驳回 = `process_status = 2 AND restart > 0`；且能反查到识别记录 → 经过系统识别 |
| 4 | 未经过系统识别驳回的数量 | `task_dispatch_node` | 驳回 = `process_status = 2 AND restart > 0`；且反查不到识别记录 |
| 5 | 手动通知未经过系统的数量 | `task_dispatch_node` | `submit_name` 只有姓名、`process_status = 3`，且反查不到识别记录 |

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
2. ✅ 通知类节点只取 `node_type = 20`（通知可启动）；
3. ✅ "驳回"统一以 Node 表判断：`process_status = 2`（待完成）**且** `restart > 0`（被驳回重启过）；指标 3 旧的 `judge_result = 1` 口径已废弃。

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
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
;

-- ========== 3. 经过系统识别被驳回的数量 ==========
-- 口径：驳回 = Node 表 process_status = 2 且 restart > 0；经过系统识别 = 能反查到识别记录
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 20
  AND n.process_status = 2                          -- 驳回判断①：待完成
  AND n.restart > 0                                 -- 驳回判断②：被驳回重启过
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
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
-- 口径：驳回 = Node 表 process_status = 2 且 restart > 0，但反查不到识别记录
SELECT COUNT(*) AS cnt
FROM dw.dw_plat_starlord_task_dispatch_node_da n
JOIN dw.dw_plat_starlord_task_dispatch_da task_dispatch
       ON task_dispatch.id = n.task_dispatch_id AND task_dispatch.state = 1
WHERE n.state = 1
  AND n.node_type = 20
  AND n.process_status = 2                          -- 驳回判断①：待完成
  AND n.restart > 0                                 -- 驳回判断②：被驳回重启过
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
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
```

## 四、一次跑出 5 个数（UNION ALL 汇总）

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
WHERE n.state = 1 AND n.node_type = 20
  AND n.process_status = 2 AND n.restart > 0
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
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
WHERE n.state = 1 AND n.node_type = 20
  AND n.process_status = 2 AND n.restart > 0
  AND n.pt = '20261027000000'
  AND task_dispatch.pt = '20261027000000'
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
  AND task_dispatch.material_code IN ('029006007','029006002','029006006','029006003')   -- 品类
  AND task_dispatch.gb_code IN ('610100','610400','440100','440600')                     -- 城市
  AND NOT EXISTS (SELECT 1 FROM ods.ods_plat_starlord_material_site_recognize_result_da sit
                  WHERE sit.project_order_id = task_dispatch.project_order_id
                    AND sit.material_code = task_dispatch.material_code
                    AND sit.task_type = task_dispatch.task_type
                    AND sit.pt = '20261027000000');
```

## 五、补充说明

- **手动通知的正则**：`'^[^（）()]+$'` 表示整串不含全角/半角括号，即"只有姓名"。若后缀写法固定，更直白的写法：
  `submit_name NOT LIKE '%一键通知%' AND submit_name NOT LIKE '%自动通知%' AND submit_name <> ''`
- **`process_status`（节点处理状态）**：指标 2/5（手动通知）取 `process_status = 3`；指标 3/4（驳回）取 `process_status = 2 AND restart > 0`；指标 1 不涉及 Node 表。
- **时间范围**：所有查询可按需加时间过滤，建议统一用 `submit_time`（通知任务实际提交时间）作为统计时间轴。
- `task_dispatch` DDL 已核对：`project_order_id、material_code、task_type` 列名一致，且已在所有关联中补充 `task_dispatch.state = 1` 排除无效任务。
- 口径已全部确认：通知类节点只取 `node_type = 20`；"驳回"统一以 Node 表判断 —— `process_status = 2`（待完成）且 `restart > 0`（被驳回重启过），指标 3 旧的 `judge_result = 1` 口径已废弃。
- **分区字段 `pt`**：三张大数据表均需带分区过滤，当前统一为 `pt = '20261027000000'`（2026-10-27 分区）；换统计日期时，注意同步替换所有出现位置（含 `EXISTS` / `NOT EXISTS` 子查询内部的 `sit.pt`）。
- 若引擎不支持 `REGEXP` 关键字（如 Hive），可改写为 `n.submit_name RLIKE '^[^（）()]+$'`，语义相同。
- **品类 / 城市过滤**（全部 5 个指标已统一加上）：
  - 品类：`material_code IN ('029006007','029006002','029006006','029006003')`
  - 城市：`gb_code IN ('610100','610400','440100','440600')`（西安、咸阳、广州、佛山）
  - 指标 2/3/4/5 直接在 `task_dispatch` 上过滤；指标 1 主表是 `sit`（无 `gb_code`），品类用 `sit.material_code` 过滤，城市通过 `EXISTS` 反查 `task_dispatch.gb_code` 实现（用 EXISTS 而非 JOIN，避免一条识别记录匹配多条调度任务时重复计数）。
  - ⚠️ 隐含口径：指标 1 要求识别记录**必须能关联到有效的 `task_dispatch`（state=1）**才计数；若识别表本身有城市字段（如 `gb_code`/`city_code`），可改为直接过滤，去掉 EXISTS 子查询。
