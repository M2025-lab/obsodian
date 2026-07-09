# Feign 内部接口

**接口数**: 154 | **类数**: 10

---

## AuditFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/AuditFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/audit/supplier-list` |  |
| 2 | POST | `/audit/material/batch-handle` |  |
| 3 | POST | `/audit/material/node-report` |  |
| 4 | POST | `/audit/material/judge-package-relation` |  |
| 5 | POST | `/audit/material/package-construction-handle` |  |

## CategoryProcessFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/deliveryflow/CategoryProcessFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/main/process/sync` |  |
| 2 | POST | `/api/config/materialScheduleSwitch` |  |
| 3 | POST | `/api/config/materialDispatchSwitch` |  |
| 4 | GET | `/api/config/delivery/flow/category/queryByCategoryId` |  |
| 5 | GET | `/api/config/delivery/flow/category/calculateDocumentType` |  |

## MaterialTaskConfigFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialTaskConfigFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/config/query` | @author: lixiangyang @Date: 2023/8/23 20:00 |
| 2 | GET | `/api/config/measure/query` | @author: lixiangyang @Date: 2023/8/23 20:00 |
| 3 | POST | `/api/config/v2/query` | 获取主材任务配置信息 @return |
| 4 | GET | `/api/config/final-payment/intercept` | 获取主材任务申请单配置 @param mdmCode @return |
| 5 | POST | `/api/config/task/query-task-activate-pre-condition` | 获取主材任务配置 v2 @param param @return |

## MaterialTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/task/cancel` | @author: lixiangyang @Date: 2023/8/28 16:33 |

## MaterialTemplateFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialTemplateFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/config/product-combo` |  |
| 2 | POST | `/material-task/config/material-list` |  |
| 3 | POST | `/material-task/config/material-list-v2` |  |
| 4 | POST | `/material-task/config/supplier-list` |  |
| 5 | POST | `/material-task/config/task-list` |  |
| 6 | GET | `/material-task/config/role-type-list` |  |
| 7 | POST | `/material-task/config/template-list` |  |
| 8 | POST | `/material-task/config/template-create` |  |
| 9 | POST | `/material-task/config/template-delete` |  |
| 10 | POST | `/material-task/config/template-to-edit` |  |
| 11 | POST | `/material-task/config/process-define-save` |  |
| 12 | POST | `/material-task/config/activate-option` |  |
| 13 | POST | `/material-task/config/examine-one-option` |  |
| 14 | POST | `/material-task/config/examine-two-option` |  |
| 15 | POST | `/material-task/config/select-option` |  |
| 16 | POST | `/material-task/config/process-define-list` |  |
| 17 | POST | `/material-task/config/branch-list` |  |
| 18 | POST | `/material-task/config/template-publish` |  |
| 19 | POST | `/material-task/config/node-delete-check` |  |
| 20 | POST | `/material-task/config/task-restart-check` |  |
| 21 | POST | `/material-task/config/logtab` |  |
| 22 | POST | `/material-task/config/log` |  |
| 23 | POST | `/material-task/config/audit/content` |  |
| 24 | POST | `/material-task/config/abort/audit` |  |
| 25 | GET | `/material-task/config/final-payment-option` |  |
| 26 | POST | `/material-task/config/final-payment/query` |  |
| 27 | POST | `/material-task/config/final-payment/save` |  |
| 28 | POST | `/material-task/config/final-payment/update` |  |
| 29 | GET | `/material-task/config/final-payment/copy` |  |
| 30 | GET | `/material-task/config/final-payment/delete` |  |
| 31 | GET | `/material-task/config/service-mode-list` |  |
| 32 | POST | `/material-task/config/copy-template-to-home2.5` |  |
| 33 | GET | `/material-task/config/copy-log-list` |  |
| 34 | POST | `/material-task/config/whole-house-material` |  |
| 35 | GET | `/material-task/config/whole-house-material-all-flat` |  |
| 36 | POST | `/material-task/order-strategy-list` |  |
| 37 | GET | `/material-task/relation-name/refresh` |  |

## SdmMessageSyncFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/SdmMessageSyncFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/starlord/sdm/status/sync` | SdmMessageSyncFeign @author huiming.suo @date 2024/11/19 20:33 |

## TaskDispatchFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/TaskDispatchFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/dispatch/change-status` |  |
| 2 | POST | `/material-task/dispatch/process-detail` |  |
| 3 | POST | `/material-task/dispatch/material-clerk/task-list` |  |
| 4 | POST | `/material-task/dispatch/select-type-option` |  |
| 5 | POST | `/material-task/dispatch/supplier-material-list` |  |
| 6 | POST | `/material-task/dispatch/progress-node` |  |
| 7 | GET | `/material-task/dispatch/show-qualified` |  |
| 8 | POST | `/material-task/dispatch/handle` |  |
| 9 | POST | `/material-task/batch/dispatch/handle` |  |
| 10 | POST | `/material-task/batch/dispatch/list/task` |  |
| 11 | POST | `/material-task/dispatch/update-notice-time` |  |
| 12 | POST | `/material-task/dispatch/supplier/task-list` |  |
| 13 | POST | `/material-task/dispatch/task-detail` |  |
| 14 | POST | `/material-task/dispatch/list/task` |  |
| 15 | POST | `/material-task/dispatch/task-node-detail` |  |
| 16 | POST | `/material-task/dispatch/material-task-detail` |  |
| 17 | POST | `/material-task/dispatch/measure-reject-detail` |  |
| 18 | POST | `/material-task/dispatch/material-task-restart-list` |  |
| 19 | POST | `/material-task/dispatch/material-remark-filter-option-list` |  |
| 20 | POST | `/material-task/dispatch/material-task-order-detail` |  |
| 21 | POST | `/material-task/dispatch/material-task-order-re-detail` |  |
| 22 | POST | `/material-task/dispatch/calendar-task-list` |  |
| 23 | POST | `/material-task/dispatch/list-owner-task` |  |
| 24 | POST | `/material-task/dispatch/list-foreman-task` |  |
| 25 | GET | `/material-task/dispatch/count-task-node` |  |
| 26 | GET | `/material-task/dispatch/list-task-node` |  |
| 27 | POST | `/material-task/dispatch/complete-task` |  |
| 28 | GET | `/material-task/dispatch/count-notify-install` |  |
| 29 | GET | `/material-task/dispatch/list-notify-install` |  |
| 30 | POST | `/material-task/dispatch/notify-install` |  |
| 31 | GET | `/material-task/dispatch/material-progress` |  |
| 32 | POST | `/material-task/dispatch/cal-time-for-cube` |  |
| 33 | POST | `/material-task/dispatch/detail` |  |
| 34 | POST | `/material-task/dispatch/count-by-conditon` |  |
| 35 | POST | `/material-task/dispatch/info` |  |
| 36 | POST | `/material-task/dispatch/batch-handle` |  |
| 37 | GET | `/material-task/dispatch/measure-module` |  |
| 38 | POST | `/material-task/dispatch/package-construction-handle` |  |
| 39 | POST | `/material-task/dispatch/packageStartNodeUnActive` |  |
| 40 | POST | `/material-task/dispatch/nodeUnReady` |  |
| 41 | POST | `/material-task/dispatch/get-task-detail` |  |
| 42 | POST | `/material-task/dispatch/list-tasks` |  |
| 43 | POST | `/material-task/dispatch/page-tasks` |  |
| 44 | POST | `/material-task/dispatch/count-tasks` |  |
| 45 | POST | `/material-task/dispatch/page-list-tasks` |  |
| 46 | POST | `/material-task/dispatch/query-task-detail` |  |
| 47 | GET | `/material-task/dispatch/delay-detail` |  |
| 48 | GET | `/material-task/dispatch/delay-filter` |  |
| 49 | POST | `/material-task/dispatch/reassignExecute` |  |
| 50 | POST | `/material-task/measure-form-change` |  |
| 51 | POST | `/material-task/design-review/submit` |  |
| 52 | POST | `/material-task/dispatch/completeOrderTask` |  |
| 53 | POST | `/material-task/dispatch/create-single-task` |  |
| 54 | POST | `/material-task/dispatch/superior/page-tasks` |  |
| 55 | POST | `/material-task/dispatch/superior/count-tasks` |  |
| 56 | POST | `/material-task/dispatch/cal-time-for-cube-batch` |  |
| 57 | POST | `/material-task/dispatch/select-node-info-by-task-dispatch-ids-and-node-type` |  |
| 58 | POST | `/material-task/dispatch/select-material-progress-by-task-dispatch-id` |  |

## TaskDispatchV2Feign

`edar-starlord-api/src/main/java/com/ke/utopia/api/TaskDispatchV2Feign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/v2/page-task-detail` |  |
| 2 | POST | `/material-task/v2/same-batch-node-detail` |  |
| 3 | POST | `/material-task/v2/get-finish-node-detail` |  |
| 4 | POST | `/material-task/v2/node-cfg-detail` |  |
| 5 | POST | `/material-task/v2/node-cfg-detail-list` |  |
| 6 | POST | `/material-task/v2/check-cancel` |  |
| 7 | POST | `/material-task/v2/save` |  |
| 8 | POST | `/material-task/v2/estimatedTime/save` |  |
| 9 | POST | `/material-task/task-process-batch/save` |  |
| 10 | POST | `/material-task/listProjectOrderIdByTaskId` |  |
| 11 | POST | `/material-task/listProjectOrderIdByTaskNodeId` |  |
| 12 | POST | `/material-task/aggregate/cascade` |  |
| 13 | POST | `/material-task/task-project-es` |  |
| 14 | POST | `/material-task/retail/task-project-es` |  |
| 15 | POST | `/material-task/process/node-link-url` |  |
| 16 | POST | `/material-task/process/node-time-relation` |  |
| 17 | POST | `/material-task/task-order-no-es` |  |
| 18 | GET | `/starlord/task/queryOperator` |  |
| 19 | POST | `/material-task/process/active-condition` |  |
| 20 | GET | `/material-task/deleteRestartTask` |  |
| 21 | GET | `/material-task/serviceOrderDownSwitch` |  |
| 22 | POST | `/api/config/delivery/flow/calculatePlanActiveTime` |  |

## TaskTemplateFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/TaskTemplateFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/template/edit` |  |
| 2 | POST | `/material-task/template/edit-check` |  |
| 3 | POST | `/material-task/template/delete` |  |
| 4 | POST | `/material-task/template/list` |  |
| 5 | POST | `/material-task/template/list-by-materials` |  |
| 6 | POST | `/material-task/template/material-list` |  |
| 7 | POST | `/material-task/template/exist-material-list` |  |
| 8 | POST | `/material-task/template/supplier-list` |  |
| 9 | POST | `/material-task/template/examine-one-option` |  |
| 10 | POST | `/material-task/template/examine-two-option` |  |
| 11 | POST | `/material-task/template/examine-two-option/edit` |  |
| 12 | POST | `/material-task/template/select-option` |  |
| 13 | POST | `/material-task/template/related-node` |  |
| 14 | POST | `/material-task/template/exist` |  |
| 15 | POST | `/material-task/template/queryActivateLevelDropDownList` |  |
| 16 | POST | `/material-task/template/list-all` |  |
| 17 | POST | `/material-task/template/list-all-compatibility` |  |
| 18 | POST | `/material-task/install-role/list` |  |

## WorkbenchTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/WorkbenchTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-task/workbench/task/cancel` |  |
| 2 | GET | `/material-task/workbench/project/whiteList` |  |
