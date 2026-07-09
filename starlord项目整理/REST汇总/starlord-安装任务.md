# 安装任务 & 验收模块

**接口数**: 49 | **类数**: 8

---

## AcceptanceReportFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/AcceptanceReportFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/acceptance-report/installer/acceptance-submit` | （安装工）安装自检提交和暂存都是这个接口 如果没有模板，则调用的是以前“api/material-task/dispatch/handle”这个接口提交 有模板的时候才会调用这个接口 业务方： 1.foreman-api安装自检调用 |
| 2 | GET | `/acceptance-report/installer/acceptance-template-query` | （安装工）安装自检提交和暂存都是这个接口 如果没有模板，则调用的是以前“api/material-task/dispatch/handle”这个接口提交 有模板的时候才会调用这个接口 业务方： 1.foreman-api安装自检调用 |
| 3 | GET | `/acceptance-report/installer/acceptance-info` | （安装工）安装自检提交和暂存都是这个接口 如果没有模板，则调用的是以前“api/material-task/dispatch/handle”这个接口提交 有模板的时候才会调用这个接口 业务方： 1.foreman-api安装自检调用 |
| 4 | GET | `/acceptance-report/report-relation-get` | 点击去处理按钮，有暂存返回暂存数据，无暂存返回模板数据 业务方： 1.foreman-api安装自检调用 |
| 5 | POST | `/acceptance-report/combine-acceptance-template` | 有验收报告的情况下，查询"已提交"的数据，底层查询Acceptance服务 业务方： 1.foreman-api安装自检调用 |
| 6 | POST | `/acceptance-report/batch-acceptance-submit` | 根据nodeId查询主材任务节点验收报告关联关系表 |
| 7 | POST | `/acceptance-report/package-acceptance-submit` | （安装工批量）安装自检提交和暂存都是这个接口 如果没有模板，则调用的是批量处理“api/material-task/dispatch/batch-handle”这个接口提交 有模板的时候才会调用这个接口 业务方： 1.foreman-api安装自检调用 |

## InstallerTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/InstallerTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/installer-task/complete-all` |  |
| 2 | POST | `/installer-task/pass-all` |  |
| 3 | GET | `/installer-task/list-task` |  |
| 4 | POST | `/installer-task/subordinate-task` |  |
| 5 | GET | `/installer-task/detail` |  |
| 6 | GET | `/installer-task/acceptance-detail` |  |
| 7 | GET | `/installer-task/project-complete-status` |  |
| 8 | GET | `/installer-task/project-material-list` |  |
| 9 | GET | `/installer-task/project-material-list-package-construction` |  |
| 10 | POST | `/installer-task/install-stage-project` |  |
| 11 | GET | `/installer-task/status-amount` |  |
| 12 | GET | `/installer-task/install-type-amount` |  |
| 13 | GET | `/installer-task/combine-install-task` |  |

## MaterialConstructionTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialConstructionTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/construction/task/cancel` |  |
| 2 | GET | `/api/construction/task/sdm/create` |  |
| 3 | GET | `/api/construction/task/sdm/cancel` |  |
| 4 | POST | `/api/construction/task/activity` |  |
| 5 | GET | `/api/construction/task/merge/switch` |  |

## MaterialMeasureController

`edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialMeasureController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/material-measure/save-task` | copy from eden foreman @Author yanghaowei005 @Date 2021/8/25 |
| 2 | GET | `/api/material-measure/get-task` | copy from eden foreman @Author yanghaowei005 @Date 2021/8/25 |
| 3 | GET | `/api/material-measure/v2/get-task` |  |
| 4 | GET | `/api/material-measure/v3/get-task-resized` |  |
| 5 | GET | `/api/material-measure/get-measure` |  |
| 6 | POST | `/api/material-measure/get-task-measure/last-measure` |  |
| 7 | POST | `/api/material-measure/get-task-measure/last-measure-v2` |  |
| 8 | POST | `/api/material-measure/get-task-measure` |  |
| 9 | POST | `/api/material-measure/update-task-measure` |  |
| 10 | GET | `/api/material-measure/has-measure-authority` |  |
| 11 | GET | `/api/material-measure/has-order-receive` |  |
| 12 | GET | `/api/material-measure/query-measure` |  |
| 13 | GET | `/api/material-measure/task/list-recent-material-ruler-measure-recheck` |  |
| 14 | GET | `/api/material-measure/task/queryRecheckScaleTaskListByUcid` |  |
| 15 | GET | `/api/material-measure/task/queryMeasureListGrade` |  |
| 16 | GET | `/api/material-measure/task/queryTaskRemarkInfo` |  |

## OmsMessageSyncFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/OmsMessageSyncFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/installer-task/message-sync-ceshi` |  |
| 2 | POST | `/installer-task/image-sync` |  |
| 3 | POST | `/installer-task/delay-reason-sync` |  |
| 4 | POST | `/installer-task/acceptance-template-result-sync` |  |

## ReplenishCoordinatorTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/coordinator/ReplenishCoordinatorTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/trace/delivery-info/query` |  |

## TaskDispatchFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/TaskDispatchFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/trace/delay-reason-type` |  |

## TestController

`edar-starlord-web/src/main/java/com/ke/utopia/web/TestController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/test/construction/withoutVirtual` |  |
| 2 | GET | `/api/test/construction/withVirtual` |  |
