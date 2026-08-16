# 材料流程配置模块

**接口数**: 115 | **类数**: 16

---

## CategoryProcessController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/CategoryProcessController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/category/process/route/save` |  |
| 2 | POST | `/category/process/default/route/info` |  |
| 3 | GET | `/category/process/define/list` |  |
| 4 | GET | `/category/process/define/list/tasktype` |  |
| 5 | GET | `/category/process/edge/list` |  |
| 6 | POST | `/category/process/define/save` |  |
| 7 | POST | `/category/process/edge/check` |  |

## CategoryProcessFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/deliveryflow/CategoryProcessFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/category/process/list` |  |
| 2 | POST | `/delivery/process/sync` |  |
| 3 | POST | `/delivery/process/query` |  |
| 4 | GET | `/delivery/process/query/by-category-id` |  |
| 5 | POST | `/delivery/process/query-list` |  |
| 6 | POST | `/delivery/process/query-category-config-list` |  |
| 7 | POST | `/delivery/process/query-config-list` |  |
| 8 | POST | `/delivery/process/query-by-flow-config-id-list` |  |

## DeliveryFlowCategoryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowCategoryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/delivery/flow/category/query/list/bycondition` |  |
| 2 | GET | `/delivery/flow/category/query/info` |  |
| 3 | GET | `/delivery/flow/category/delete` |  |
| 4 | GET | `/delivery/flow/category/expire/check` |  |
| 5 | GET | `/delivery/flow/category/expire` |  |
| 6 | POST | `/delivery/flow/category/query/apply/material` |  |
| 7 | POST | `/delivery/flow/category/query/apply/judge` |  |
| 8 | POST | `/delivery/flow/category/query/apply/copy` |  |
| 9 | POST | `/delivery/flow/category/copy` |  |
| 10 | POST | `/delivery/flow/category/save` |  |
| 11 | GET | `/delivery/flow/category/effective/check` |  |
| 12 | GET | `/delivery/flow/category/effective` |  |
| 13 | POST | `/delivery/flow/category/supplier/update` |  |
| 14 | POST | `/delivery/flow/category/update` |  |
| 15 | POST | `/delivery/flow/category/update/check` |  |

## DeliveryFlowRuleFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/deliveryflow/DeliveryFlowRuleFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/delivery/rule/update/info` |  |
| 2 | POST | `/delivery/rule/delivery/process/state/effect` |  |
| 3 | GET | `/delivery/rule/delivery/category/query/code` |  |

## DeliveryFlowRuleInfoController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowRuleInfoController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/delivery/flow/delivery/process/info` |  |
| 2 | POST | `/delivery/flow/rule/query/list` |  |
| 3 | GET | `/delivery/flow/rule/info` |  |
| 4 | GET | `/delivery/flow/rule/info/simple` |  |
| 5 | GET | `/delivery/flow/rule/expire/check` |  |
| 6 | GET | `/delivery/flow/rule/expire` |  |
| 7 | POST | `/delivery/flow/rule/save` |  |
| 8 | POST | `/delivery/flow/rule/check/save` |  |
| 9 | POST | `/delivery/flow/rule/info/update` |  |
| 10 | POST | `/delivery/flow/rule/query/updaterecord` |  |
| 11 | GET | `/delivery/flow/rule/send/push` | 更新排程对应的分公司和套餐 |

## DeliveryFlowSelectQueryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowSelectQueryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/delivery/flow/query/selected/companySug` |  |
| 2 | GET | `/delivery/flow/query/selected/material` |  |
| 3 | POST | `/delivery/flow/query/selected/supplier` |  |
| 4 | GET | `/delivery/flow/query/selected/documentType` |  |
| 5 | GET | `/delivery/flow/query/selected/nodeProcessTemplateList` |  |
| 6 | GET | `/delivery/flow/query/system/version` |  |
| 7 | POST | `/delivery/flow/query/selected/activate/condition` |  |

## DeliveryFlowTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/delivery/flow/task/node/mode/query` | Description <p> DATE 2025/10/15. @author mafuxin. |
| 2 | GET | `/delivery/flow/task/role/query` | Description <p> DATE 2025/10/15. @author mafuxin. |
| 3 | GET | `/delivery/flow/task/work-type/query` | Description <p> DATE 2025/10/15. @author mafuxin. |
| 4 | POST | `/delivery/flow/task/time/condition/level1` |  |

## MaterialDeliveryV2Controller

`edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialDeliveryV2Controller.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/batch/material/list` |  |
| 2 | GET | `/batch/material/list/need_arrival_notify` |  |
| 3 | GET | `/batch/material/info` |  |
| 4 | POST | `/batch/material/submit` |  |
| 5 | POST | `/batch/material/update` |  |
| 6 | POST | `/batch/material/expect_time/query` |  |
| 7 | GET | `/batch/material/detail` |  |

## MaterialFlowCategoryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowCategoryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material/flow/category/query/list/bycondition` |  |
| 2 | GET | `/material/flow/category/query/list` |  |
| 3 | GET | `/material/flow/category/query/info` |  |
| 4 | GET | `/material/flow/category/delete` |  |
| 5 | GET | `/material/flow/category/expire` |  |
| 6 | GET | `/material/flow/category/effective` |  |
| 7 | POST | `/material/flow/category/query/apply/material` |  |
| 8 | POST | `/material/flow/category/query/apply/judge` |  |
| 9 | POST | `/material/flow/category/query/apply/copy` |  |
| 10 | POST | `/material/flow/category/copy` |  |
| 11 | POST | `/material/flow/category/save` |  |
| 12 | POST | `/material/flow/category/saveWorkType` |  |
| 13 | POST | `/material/flow/category/effctive/check` |  |
| 14 | POST | `/material/flow/category/update` |  |
| 15 | POST | `/material/flow/category/update/info` |  |
| 16 | POST | `/material/flow/process/business/check` |  |

## MaterialFlowProcessController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowProcessController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material/flow/rule/query/merge/list` |  |
| 2 | POST | `/material/flow/category/query/installBaseLine` |  |
| 3 | POST | `/material/flow/category/getSaleType` |  |
| 4 | POST | `/material/flow/process/task/info` |  |
| 5 | POST | `/material/flow/process/task/save` |  |
| 6 | POST | `/material/flow/process/merge/task/list` |  |
| 7 | POST | `/material/flow/manpower/cfg/templateList` |  |

## MaterialFlowQueryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowQueryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material/flow/rule/send/applydata/msg` |  |

## MaterialFlowRuleInfoController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowRuleInfoController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material/flow/rule/query/list` |  |
| 2 | GET | `/material/flow/rule/info` |  |
| 3 | GET | `/material/flow/rule/info/simple` |  |
| 4 | POST | `/material/flow/rule/info/update` |  |
| 5 | GET | `/material/flow/rule/expire` |  |
| 6 | GET | `/material/flow/rule/effective` |  |
| 7 | POST | `/material/flow/rule/save` |  |
| 8 | GET | `/material/flow/rule/set/default` |  |
| 9 | POST | `/material/flow/rule/check/save` |  |
| 10 | POST | `/material/flow/rule/add/company` |  |
| 11 | POST | `/material/flow/rule/all/copy` |  |
| 12 | POST | `/material/flow/rule/all/effective` |  |
| 13 | POST | `/material/flow/rule/query/updaterecord` |  |
| 14 | POST | `/material/flow/rule/excel/export` |  |

## MaterialFlowRuleQueryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowRuleQueryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material/flow/query/category/code` | 根据规则id查询品类规则 |

## MaterialFlowRuleQueryFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MaterialFlowRuleQueryFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material/flow/query/template/task` |  |
| 2 | GET | `/material/flow/query/categoryid` |  |
| 3 | GET | `/material/flow/query/historyId/byCode` |  |
| 4 | GET | `/material/flow/isSupplierExecutorByCategoryId` |  |
| 5 | GET | `/material/flow/batchIsSupplierExecutorByCategoryId` |  |

## MaterialFlowSelectQueryController

`edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowSelectQueryController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material/flow/query/selected/companySug` | @author sunbin050 |
| 2 | GET | `/material/flow/query/selected/material` | @author sunbin050 |
| 3 | POST | `/material/flow/query/selected/supplier` | @author sunbin050 |
| 4 | POST | `/material/flow/query/selected/product` |  |
| 5 | POST | `/material/flow/query/selected/merchant` |  |
| 6 | GET | `/material/flow/query/selected/saleType` |  |
| 7 | GET | `/material/flow/query/selected/ofcNodeProcessTemplateList` |  |
| 8 | GET | `/material/flow/query/system/version` |  |

## OfcDispatchConfigQueryService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/OfcDispatchConfigQueryService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/delivery/pre-allocate/list` | 获取供应链侧的分公司 |
