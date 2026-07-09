# 主材任务模块

**接口数**: 173 | **类数**: 18

---

## CommonTaskModuleController

`edar-starlord-web/src/main/java/com/ke/utopia/web/CommonTaskModuleController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/common-module/get-measure-module` | @author wangzhe089 |
| 2 | GET | `/api/common-module/get-material-delivery-module` | @author wangzhe089 |
| 3 | GET | `/api/common-module/get-order-module` | @author wangzhe089 |

## DelayFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/DelayFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/delay/delay-task` |  |
| 2 | GET | `/delay/about-to-delay-task` |  |
| 3 | POST | `/delay/push-delay-task-by-chat-id` |  |
| 4 | GET | `/delay/supplier-delay-task` |  |
| 5 | GET | `/delay/query-delay-reason-recovery` |  |

## HomePaymentApi

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/HomePaymentApi.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/fund/getProjectFund` |  |
| 2 | GET | `/api/fund/detail` |  |
| 3 | GET | `/api/fund/getProjectEscrowBasicInfo` |  |

## MaterialButlerFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialButlerFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-butler/recent-change-appoint-list` |  |
| 2 | GET | `/material-butler/appoint-worker-task-count` |  |
| 3 | GET | `/material-butler/project-appoint-worker` |  |
| 4 | GET | `/material-butler/project-appoint-task` |  |
| 5 | GET | `/material-butler/project-appoint-history` |  |
| 6 | GET | `/material-butler/central-project-change-appoint` |  |

## MaterialCustomerFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialCustomerFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-customer/dispatch/list-task-dispatch/detail` |  |
| 2 | GET | `/material-customer/dispatch/task-dispatch-detail` |  |
| 3 | GET | `/material-customer/dispatch/list-project-order-ids` |  |
| 4 | GET | `/material-customer/dispatch/list-task-dispatch-with-tasktype` |  |

## MaterialDelayApproveFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialDelayApproveFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-delay/approve/getUnApproveCountByUcId` |  |
| 2 | POST | `/material-delay/approve/approveDelayProcess` |  |

## MaterialDelayProcessFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialDelayProcessFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-delay-process/can-select-sku` | <p> </p> @author zhuyuehui001 @since 2023/09/01 2:46 PM |
| 2 | POST | `/material-delay-process/create-material-delay-process` | <p> </p> @author zhuyuehui001 @since 2023/09/01 2:46 PM |
| 3 | GET | `/material-delay-process/delay-reason-list` |  |
| 4 | POST | `/material-delay-process/preview` |  |
| 5 | GET | `/material-delay-process/reason-list-flat` |  |
| 6 | POST | `/material-delay-process/update` |  |
| 7 | GET | `/material-delay-process/detail` |  |
| 8 | GET | `/material-delay-process/delete` |  |
| 9 | GET | `/material-delay-process/detail-for-additional-record` |  |
| 10 | GET | `/material-delay-process/task/undo` |  |
| 11 | GET | `/material-delay-process/list` |  |
| 12 | POST | `/material-delay-process/confirm` |  |
| 13 | GET | `/material-delay-process/check-has-post-material-delay-process` |  |

## MaterialDeliveryFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialDeliveryFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-delivery/batch-task/list` |  |
| 2 | GET | `/material-delivery/batch-task/detail` |  |
| 3 | POST | `/material-delivery/batch-task/submit` |  |
| 4 | POST | `/material-delivery/notice-time/sync` |  |
| 5 | POST | `/material-delivery/query-delivery-count` |  |

## MaterialScheduleFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialScheduleFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-biz/get-material-dailyReport` | @author: lixiangyang @Date: 2024/1/10 20:21 |

## MaterialTaskBizV2Feign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialTaskBizV2Feign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-biz/recent-appoint-delivery-uncompleted-list` | 主材任务业务接口 @author huiming.suo @date 2022/3/18 6:12 下午 |
| 2 | GET | `/material-biz/recent-appoint-measure-uncompleted-list` | 主材任务业务接口 @author huiming.suo @date 2022/3/18 6:12 下午 |
| 3 | GET | `/material-biz/recent-appoint-worker-uncompleted-list-new` | 主材任务业务接口 @author huiming.suo @date 2022/3/18 6:12 下午 |
| 4 | GET | `/material-biz/recent-appoint-delivery-uncompleted-list-new` | 主材任务业务接口 @author huiming.suo @date 2022/3/18 6:12 下午 |
| 5 | GET | `/material-biz/recent-appoint-measure-uncompleted-list-new` |  |
| 6 | GET | `/material-biz/recent-appoint-notice-measure-uncompleted-list-new` |  |
| 7 | GET | `/material-biz/recent-appoint-order-uncompleted-list-new` |  |
| 8 | POST | `/material-biz/dispatch-list` |  |
| 9 | GET | `/material-biz/recheck-scale-task-page` |  |
| 10 | GET | `/material-biz/retail-material-task-detail` |  |
| 11 | GET | `/material-biz/query-home-count` |  |
| 12 | GET | `/material-biz/query-home-count/v2` |  |
| 13 | GET | `/material-biz/to-home/query-count` |  |

## MaterialTaskConfigFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialTaskConfigFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/material/flow/query/config/taskinfo` | 获取尾款限制配置 @param mdmCode @return |
| 2 | POST | `/api/material/flow/query/apply/data` | 获取尾款限制配置 @param mdmCode @return |

## MaterialTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/butler/MaterialTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/material/self-check/acceptance-query` |  |
| 2 | POST | `/api/material/self-check/acceptance-fail` |  |
| 3 | POST | `/api/material/self-check/acceptance-pass` |  |

## MaterialTaskInstallerTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/foreman/MaterialTaskInstallerTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/material-task/installer-task/list-install-task` |  |
| 2 | POST | `/api/material-task/installer-task/complete-all` |  |
| 3 | POST | `/api/material-task/installer-task/pass-all` |  |
| 4 | GET | `/api/material-task/installer-task/list-task` |  |
| 5 | POST | `/api/material-task/installer-task/subordinate-task` |  |
| 6 | GET | `/api/material-task/installer-task/detail` |  |
| 7 | POST | `/api/material-task/dispatch/handle` |  |
| 8 | POST | `/api/material-task/dispatch/task-detail` |  |
| 9 | GET | `/api/material-task/dispatch/material-progress` |  |
| 10 | POST | `/api/material-task/dispatch/material-task-detail` |  |
| 11 | POST | `/api/material-task/dispatch/measure-reject-detail` |  |
| 12 | POST | `/api/material-task/dispatch/common/install-time-limit` |  |
| 13 | POST | `/api/material-task/dispatch/common/change-appoint` |  |
| 14 | GET | `/api/material-task/dispatch/common/change-appoint-list` |  |
| 15 | GET | `/api/material-task/installer-task/status-amount` |  |
| 16 | GET | `/api/material-task/installer-task/install-type-amount` |  |
| 17 | GET | `/api/material-task/installer-task/combine-install-task` |  |
| 18 | POST | `/api/material-task/dispatch/batch-handle` |  |
| 19 | POST | `/api/material-task/dispatch/batch-handle/package-construction-handle` | 没有模板时候兜底提交 |
| 20 | POST | `/api/material-task/material-delivery/batch-task/list` | 没有模板时候兜底提交 |
| 21 | POST | `/api/material-task/dispatch/material-task-order-detail` | 没有模板时候兜底提交 |

## MaterialTaskProcessTemplateController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/MaterialTaskProcessTemplateController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/task/process/template/list` |  |
| 2 | POST | `/task/process/template/list/add` |  |

## OfcDispatchConfigQueryService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/OfcDispatchConfigQueryService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/order/replenish/customized/thirdSelectList/brandOrderNo` | 获取供应链侧的分公司 |

## RefreshDataController

`edar-starlord-web/src/main/java/com/ke/utopia/web/RefreshDataController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/task-dispatch/refresh-create` |  |
| 2 | GET | `/api/task-dispatch/refresh-create/task-dispatch/compensation/update-schedule` |  |
| 3 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/active-time` |  |
| 4 | GET | `/api/task-dispatch/refresh-create/task-dispatch/compensation/push-enter-message` |  |
| 5 | GET | `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-config-supplier-name` |  |
| 6 | GET | `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-dispatch-supplier-name` |  |
| 7 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-estimated-time` |  |
| 8 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/material-task-dispatch-change` |  |
| 9 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/change-appoint-handler` |  |
| 10 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/push-visit-door-time-message` |  |
| 11 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/notice-enter-task` |  |
| 12 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/update-process-batch` |  |
| 13 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-notice-batch` |  |
| 14 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/project-package-relation` |  |
| 15 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/system-complete-notice-task` |  |
| 16 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/oms-sync` |  |
| 17 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/handle-assigner-uncompleted` |  |
| 18 | POST | `/api/task-dispatch/refresh-create/task-dispatch/compensation/complete-assigner-all` |  |
| 19 | GET | `/api/task-dispatch/refresh-create/task-dispatch-node/send-notice-retain-msg` |  |
| 20 | GET | `/api/task-dispatch/refresh-create/task-dispatch-node/send-notice-es-msg` |  |
| 21 | POST | `/api/task-dispatch/refresh-create/refresh-data/task-dispatch/complet-order` |  |
| 22 | POST | `/api/task-dispatch/refresh-create/task-dispatch/measure-apply-msg` |  |
| 23 | POST | `/api/task-dispatch/refresh-create/task-dispatch/minerva-reassign-msg` |  |
| 24 | POST | `/api/task-dispatch/refresh-create/task-dispatch/project-vss-msg` |  |
| 25 | POST | `/api/task-dispatch/refresh-create/task-dispatch/activateTaskDispatch` |  |
| 26 | POST | `/api/task-dispatch/refresh-create/task-dispatch/refresh` |  |
| 27 | POST | `/api/task-dispatch/refresh-create/task-dispatch/refreshNode` |  |
| 28 | POST | `/api/task-dispatch/refresh-create/task-dispatch/refreshNodeByTaskId` |  |
| 29 | POST | `/api/task-dispatch/refresh-create/task-dispatch/activateTaskDispatchV2` |  |
| 30 | POST | `/api/task-dispatch/refresh-create/task-dispatch/updateTaskDispatchNodeV2` |  |
| 31 | POST | `/api/task-dispatch/refresh-create/task-dispatch/updateTaskDispatchNodeV3` |  |
| 32 | GET | `/api/task-dispatch/refresh-create/cache/reload` |  |
| 33 | GET | `/api/task-dispatch/refresh-create/recalculateEstimatedTime` |  |
| 34 | GET | `/api/task-dispatch/refresh-create/recalculateParentEstimatedTime` |  |
| 35 | GET | `/api/task-dispatch/refresh-create/refreshEstimatedTime` |  |
| 36 | POST | `/api/task-dispatch/refresh-create/task-dispatch/groovy` |  |
| 37 | POST | `/api/task-dispatch/refresh-create/compensate/orderTask` |  |
| 38 | GET | `/api/task-dispatch/refresh-create/test` |  |
| 39 | GET | `/api/task-dispatch/refresh-create/audit` |  |
| 40 | GET | `/api/task-dispatch/refresh-create/reject` |  |
| 41 | GET | `/api/task-dispatch/refresh-create/updateTemplateAllState` |  |
| 42 | GET | `/api/task-dispatch/refresh-create/template/audit/{type}` |  |
| 43 | GET | `/api/task-dispatch/refresh-create/sendSupplierMsg` |  |
| 44 | GET | `/api/task-dispatch/refresh-create/restartCopyNode` |  |
| 45 | POST | `/api/task-dispatch/refresh-create/event/{type}` |  |
| 46 | POST | `/api/task-dispatch/refresh-create/event/redo/{type}` |  |
| 47 | GET | `/api/task-dispatch/refresh-create/supplier/holiday/calTime` |  |
| 48 | GET | `/api/task-dispatch/refresh-create/refresh/central/before/order` |  |
| 49 | GET | `/api/task-dispatch/refresh-create/refresh/central/after/order` |  |
| 50 | GET | `/api/task-dispatch/refresh-create/refreshErrorData` |  |
| 51 | GET | `/api/task-dispatch/refresh-create/sync/package/task/status` |  |
| 52 | GET | `/api/task-dispatch/refresh-create/sync/task/finished/status` |  |
| 53 | GET | `/api/task-dispatch/refresh-create/sync/task/city-code` |  |
| 54 | GET | `/api/task-dispatch/refresh-create/task/sdm/diff` |  |
| 55 | GET | `/api/task-dispatch/refresh-create/task/sdm/diff/new` |  |
| 56 | GET | `/api/task-dispatch/refresh-create/task/sdm/diff/new/single` |  |
| 57 | GET | `/api/task-dispatch/refresh-create/task/sdm/auto-take-order` |  |
| 58 | POST | `/api/task-dispatch/refresh-create/task/sync/sdm` |  |
| 59 | POST | `/api/task-dispatch/refresh-create/task/sdm/update-estimated-time` |  |
| 60 | GET | `/api/task-dispatch/refresh-create/task/sdm/update-estimated-time-by-node` |  |
| 61 | POST | `/api/task-dispatch/refresh-create/task/sdm/project-order-diff` |  |
| 62 | POST | `/api/task-dispatch/refresh-create/task/sdm/project-order-need-restart-diff` |  |
| 63 | GET | `/api/task-dispatch/refresh-create/refreshNodeExecutor` |  |
| 64 | GET | `/api/task-dispatch/refresh-create/updateEstimatedTimeByTemplateCode` |  |
| 65 | GET | `/api/task-dispatch/refresh-create/refresh/completed_task_dispatch_node_estimated_time` |  |
| 66 | POST | `/api/task-dispatch/refresh-create/tools/batchRefreshTaskEstimateTime` |  |
| 67 | GET | `/api/task-dispatch/refresh-create/refresh/executorId` |  |
| 68 | GET | `/api/task-dispatch/refresh-create/refresh/fuchikaicheng` |  |
| 69 | GET | `/api/task-dispatch/refresh-create/refresh/fuchikaicheng2` |  |
| 70 | GET | `/api/task-dispatch/refresh-create/recreate/task` |  |
| 71 | GET | `/api/task-dispatch/refresh-create/recreate/task2` |  |
| 72 | GET | `/api/task-dispatch/refresh-create/test/send/vss` |  |
| 73 | GET | `/api/task-dispatch/refresh-create/refresh/huifudata_mode8` |  |
| 74 | GET | `/api/task-dispatch/refresh-create/refresh/refreshSendVssFinish` |  |
| 75 | GET | `/api/task-dispatch/refresh-create/refresh/updateNewRecheck20NodeFinishi` |  |
| 76 | GET | `/api/task-dispatch/refresh-create/refresh/refreshSyncToSdmByPurchaseOrderNo` |  |
| 77 | GET | `/api/task-dispatch/refresh-create/refresh/refreshSyncToTaskId` |  |
| 78 | GET | `/api/task-dispatch/refresh-create/refresh/updateDesignFinishedTaskId` |  |
| 79 | GET | `/api/task-dispatch/refresh-create/refresh-data/fix-duplicate-nodes` | 修复nodeTask重复导致的node表重复节点问题 场景：nodeTask中存在如"60,60"的重复值，task_dispatch_node表也产生了重复节点 规则： 1. 校验nodeTask和node表是否真有重复，无重复/已失效则跳过 2. 更新task的nodeTask去重 3. 全是NOT_ACTIVE → 删除id更大的 4. NOT_ACTIVE + UNFINISHED ... |
| 80 | GET | `/api/task-dispatch/refresh-create/refresh-data/fix-duplicate-config-nodes` | 修复排程配置中node/task数据重复问题 场景：同一categoryId(即processDefine的templateId)下，n_material_node_cfg和n_material_task_cfg存在重复记录 查询方式参考 list-all-compatibility mode=7(排程模式) 规则： 1. categoryId等价processDefine.template... |
| 81 | POST | `/api/task-dispatch/refresh-create/refresh/routeTime/workday` |  |
| 82 | GET | `/api/task-dispatch/refresh-create/refresh-data/get-purchase-no-test` |  |
| 83 | GET | `/api/task-dispatch/refresh-create/refresh/updateDispatchTemplateByProcessCode` | 根据旧 processCode 刷 task_dispatch 的 templateId 和 processCode 逻辑：取旧模板的分公司/品类/供应商 + 各项目订单的 deliveryProcessCfgId → 查新模板 → 更新 |

## ReplenishCoordinatorTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/coordinator/ReplenishCoordinatorTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/order/pre-allocate/list` |  |
| 2 | GET | `/api/order/replenish/customized/thirdSelectList/brandOrderNo` |  |

## TaskProgressTipFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/TaskProgressTipFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-task-tip/project/hastip` | 主材任务进展服务 |
| 2 | GET | `/material-task-tip/ruler/hastip` | 主材任务进展服务 |
| 3 | GET | `/material-task-tip/material/hastip` | 判断订单上是否有主材任务新进展 @param projectOrderId 订单ID @return boolean |
| 4 | GET | `/material-task-tip/clear` | 判断留尺上是否有主材任务新进展 @param projectOrderId 订单ID @param rulerType 任务类型 @return boolean |
