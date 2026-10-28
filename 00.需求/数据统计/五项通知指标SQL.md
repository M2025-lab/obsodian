# 五项通知指标统计 SQL

> 来源需求：[[数据需要]]
> 涉及表：`material_site_recognize_result`（识别结果表）、`task_dispatch_node`（节点任务表）、`task_dispatch`（任务调度表）

## 一、指标口径分析

| # | 指标 | 数据来源 | 判断逻辑 |
|---|------|---------|---------|
| 1 | 一键通知的数量 | `material_site_recognize_result` | `notify_execute_type = 1`（该表只存一键/自动通知，不含手动） |
| 2 | 手动通知经过系统识别的数量 | `task_dispatch_node` + 识别表 | `submit_name` 只有姓名（无后缀）→ 手动；能反查到识别记录 → 经过系统识别 |
| 3 | 经过系统识别驳回的数量 | `material_site_recognize_result` | 有识别记录且被驳回，`judge_result = 1`（业务确认口径） |
| 4 | 未经过系统识别驳回的数量 | `task_dispatch_node` | `restart > 0`（被驳回重启过）且反查不到识别记录 |
| 5 | 手动通知未经过系统的数量 | `task_dispatch_node` | `submit_name` 只有姓名且反查不到识别记录 |

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
3. ✅ 指标 3 的"驳回"只算 `judge_result = 1`（注意：DDL 注释中 1 为"任务可通知"，此口径为业务方明确指定）。

## 三、分项 SQL

```sql
-- ========== 1. 一键通知的数量 ==========
SELECT COUNT(*) AS cnt
FROM material_site_recognize_result
WHERE notify_execute_type = 1
  -- AND gmt_create >= '2026-10-01' AND gmt_create < '2026-11-01'   -- 按需加时间范围
;

-- ========== 2. 手动通知 且 经过系统识别 ==========
SELECT COUNT(*) AS cnt
FROM task_dispatch_node n
JOIN task_dispatch td          ON td.id = n.task_dispatch_id AND td.state = 1
JOIN material_site_recognize_result r
       ON r.project_order_id = td.project_order_id
      AND r.material_code    = td.material_code
      AND r.task_type        = td.task_type
WHERE n.state = 1
  AND n.node_type = 20                            -- 通知类节点（已确认只取 20-通知可启动）
  AND n.submit_name REGEXP '^[^（）()]+$'               -- 只有姓名、无（一键通知）/（自动通知）后缀 → 手动
;

-- ========== 3. 经过系统识别被驳回的数量 ==========
-- 口径（业务已确认）：judge_result = 1
SELECT COUNT(*) AS cnt
FROM material_site_recognize_result
WHERE judge_result = 1
;

-- ========== 4. 未经过系统识别 被驳回的数量 ==========
-- 口径：通知节点被驳回重启过（restart > 0），但反查不到识别记录
SELECT COUNT(*) AS cnt
FROM task_dispatch_node n
JOIN task_dispatch td ON td.id = n.task_dispatch_id AND td.state = 1
WHERE n.state = 1
  AND n.node_type = 20
  AND n.restart > 0
  AND NOT EXISTS (
      SELECT 1
      FROM material_site_recognize_result r
      WHERE r.project_order_id = td.project_order_id
        AND r.material_code    = td.material_code
        AND r.task_type        = td.task_type
  )
;

-- ========== 5. 手动通知 且 未经过系统识别 ==========
SELECT COUNT(*) AS cnt
FROM task_dispatch_node n
JOIN task_dispatch td ON td.id = n.task_dispatch_id AND td.state = 1
WHERE n.state = 1
  AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'               -- 手动通知
  AND NOT EXISTS (
      SELECT 1
      FROM material_site_recognize_result r
      WHERE r.project_order_id = td.project_order_id
        AND r.material_code    = td.material_code
        AND r.task_type        = td.task_type
  )
;
```

## 四、一次跑出 5 个数（UNION ALL 汇总）

```sql
SELECT '1-一键通知数量' AS metric, COUNT(*) AS cnt
FROM material_site_recognize_result
WHERE notify_execute_type = 1
UNION ALL
SELECT '2-手动通知经过系统识别', COUNT(*)
FROM task_dispatch_node n
JOIN task_dispatch td ON td.id = n.task_dispatch_id AND td.state = 1
WHERE n.state = 1 AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'
  AND EXISTS (SELECT 1 FROM material_site_recognize_result r
              WHERE r.project_order_id = td.project_order_id
                AND r.material_code = td.material_code
                AND r.task_type = td.task_type)
UNION ALL
SELECT '3-经过系统识别驳回', COUNT(*)
FROM material_site_recognize_result
WHERE judge_result = 1
UNION ALL
SELECT '4-未经过系统识别驳回', COUNT(*)
FROM task_dispatch_node n
JOIN task_dispatch td ON td.id = n.task_dispatch_id AND td.state = 1
WHERE n.state = 1 AND n.node_type = 20 AND n.restart > 0
  AND NOT EXISTS (SELECT 1 FROM material_site_recognize_result r
                  WHERE r.project_order_id = td.project_order_id
                    AND r.material_code = td.material_code
                    AND r.task_type = td.task_type)
UNION ALL
SELECT '5-手动通知未经过系统', COUNT(*)
FROM task_dispatch_node n
JOIN task_dispatch td ON td.id = n.task_dispatch_id AND td.state = 1
WHERE n.state = 1 AND n.node_type = 20
  AND n.submit_name REGEXP '^[^（）()]+$'
  AND NOT EXISTS (SELECT 1 FROM material_site_recognize_result r
                  WHERE r.project_order_id = td.project_order_id
                    AND r.material_code = td.material_code
                    AND r.task_type = td.task_type);
```

## 五、补充说明

- **手动通知的正则**：`'^[^（）()]+$'` 表示整串不含全角/半角括号，即"只有姓名"。若后缀写法固定，更直白的写法：
  `submit_name NOT LIKE '%一键通知%' AND submit_name NOT LIKE '%自动通知%' AND submit_name <> ''`
- **`process_status = 2`（待完成）**：5 个指标统计的都是"已发生"的量，故未加此过滤；若统计"当前待完成且手动"口径，再补 `n.process_status = 2`。
- **时间范围**：所有查询可按需加时间过滤，建议统一用 `submit_time`（通知任务实际提交时间）作为统计时间轴。
- `task_dispatch` DDL 已核对：`project_order_id、material_code、task_type` 列名一致，且已在所有关联中补充 `td.state = 1` 排除无效任务。
- 口径已全部确认：通知类节点只取 `node_type = 20`；指标 3 的"驳回"只算 `judge_result = 1`（业务方明确指定，注意与 DDL 注释"1-任务可通知"的字面含义不同）。
