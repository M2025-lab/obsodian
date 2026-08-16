# 调度派工模块

**接口数**: 31 | **类数**: 6

---

## CoordinatorPlatformController

`edar-starlord-web/src/main/java/com/ke/utopia/web/CoordinatorPlatformController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/coordinator/platform/task/list-es-content` | @author czc @date 2025年05月28日 11:54 |
| 2 | POST | `/coordinator/platform/retail/list-es-content` |  |
| 3 | POST | `/coordinator/platform/retail/trigger-es-resync` |  |

## DispatchCommonFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/DispatchCommonFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/dispatch/common/change-appoint` |  |
| 2 | POST | `/dispatch/common/batch-change-appoint` |  |
| 3 | GET | `/dispatch/common/change-appoint-list` |  |
| 4 | POST | `/dispatch/common/list-node-all-task` |  |
| 5 | POST | `/dispatch/common/list-node-task` |  |
| 6 | GET | `/dispatch/common/query-node-list` |  |
| 7 | POST | `/dispatch/common/visit-time-limit` |  |
| 8 | POST | `/dispatch/common/visit-time-limit/v2` |  |
| 9 | POST | `/dispatch/common/visit-time-limit/v3` |  |
| 10 | GET | `/dispatch/common/package-appoint-time-limit` |  |

## DispatchTaskTraceController

`edar-starlord-web/src/main/java/com/ke/utopia/web/trace/DispatchTaskTraceController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/dispatchTask/trace/queryProjectTaskNodeProgress` | DispatchTaskTraceController @author chenzifeng004 @date 2024-11-07 |
| 2 | POST | `/dispatchTask/trace/batchSubmit` | DispatchTaskTraceController @author chenzifeng004 @date 2024-11-07 |
| 3 | POST | `/dispatchTask/trace/download` |  |

## ReplenishCoordinatorTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/coordinator/ReplenishCoordinatorTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/dispatch/re-procurement/check/brand-sub-order` | @author czc @date 2025年03月24日 17:01 |
| 2 | POST | `/dispatch/re-procurement/place-order` | @author czc @date 2025年03月24日 17:01 |
| 3 | POST | `/dispatch/re-procurement/plan-pickup-time` |  |
| 4 | GET | `/dispatch/re-procurement/checkQueryCoordinatorTask` |  |
| 5 | POST | `/dispatch/re-procurement/fulfillment/control/drawingr` |  |
| 6 | POST | `/dispatch/trace/task/batchSubmit` | 跟单任务跟进提交接口 |
| 7 | POST | `/dispatch/trace/task/test` | 跟单任务跟进提交接口 |
| 8 | GET | `/dispatch/trace/follow-up-refresh` | 跟单任务跟进提交接口 |
| 9 | POST | `/dispatch/replenish-order/list-order-wrapper` |  |
| 10 | GET | `/dispatch/trace/modify-order-status` |  |
| 11 | POST | `/dispatch/trace/backdoor/modify` |  |

## ReplenishCoordinatorTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/ReplenishCoordinatorTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/dispatch/replenish-order/assignment-to-coordinator` | 跟单工作台-跟单任务相关接口 |
| 2 | POST | `/dispatch/replenish-order/list-task-by-material` | 跟单工作台-跟单任务相关接口 |

## StarlordController

`edar-starlord-web/src/main/java/com/ke/utopia/web/butler/StarlordController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/dispatch/list-task-node` | 获取主材任务列表 @param param @return |
| 2 | GET | `/dispatch/material-delivery/recent-tasks` | 获取主材任务列表 @param param @return |
