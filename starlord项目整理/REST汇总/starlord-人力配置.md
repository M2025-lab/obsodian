# 人力配置模块

**接口数**: 62 | **类数**: 7

---

## ManpowerCfgController

`edar-starlord-web/src/main/java/com/ke/utopia/web/manpower/ManpowerCfgController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/manpower/cfg/roleTypeListNew` |  |

## ManpowerCfgFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/manpower/ManpowerCfgFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/manpower/cfg/materialCategorySug` | 用工配置接口 @Author dongjunjie005 @Date 2024-03-29 10:52:36 |
| 2 | POST | `/manpower/cfg/companySug` | 用工配置接口 @Author dongjunjie005 @Date 2024-03-29 10:52:36 |
| 3 | POST | `/manpower/cfg/supplierSug` | 用工配置接口 @Author dongjunjie005 @Date 2024-03-29 10:52:36 |
| 4 | POST | `/manpower/cfg/taskTypeList` | 用工配置接口 @Author dongjunjie005 @Date 2024-03-29 10:52:36 |
| 5 | POST | `/manpower/cfg/queryRoleTypeList` |  |
| 6 | GET | `/manpower/cfg/roleTypeList` |  |
| 7 | GET | `/manpower/cfg/roleTypeListNew` |  |
| 8 | GET | `/manpower/cfg/saleTypeList` |  |
| 9 | POST | `/manpower/cfg/workTypeList` |  |
| 10 | POST | `/manpower/cfg/queryAllWorkTypes` |  |
| 11 | POST | `/manpower/cfg/templateList` |  |
| 12 | GET | `/manpower/cfg/queryTemplateById` |  |
| 13 | POST | `/manpower/cfg/queryTemplates` |  |
| 14 | POST | `/manpower/cfg/checkTemplateUsing` |  |
| 15 | POST | `/manpower/cfg/checkCfgUsing` |  |
| 16 | POST | `/manpower/cfg/templateToEdit` |  |
| 17 | POST | `/manpower/cfg/templateCreate` |  |
| 18 | POST | `/manpower/cfg/queryTaskRestart` |  |
| 19 | POST | `/manpower/cfg/processDefineSave` |  |
| 20 | POST | `/manpower/cfg/checkTemplateTime` |  |
| 21 | POST | `/manpower/cfg/processDefineList` |  |
| 22 | POST | `/manpower/cfg/processDefineListByCity` |  |
| 23 | POST | `/manpower/cfg/templatePublish` |  |
| 24 | POST | `/manpower/cfg/templateDelete` |  |
| 25 | GET | `/manpower/cfg/modeTypeConfig` |  |
| 26 | POST | `/manpower/cfg/queryModeType` |  |
| 27 | POST | `/manpower/cfg/exportTemplateList` |  |
| 28 | POST | `/manpower/cfg/companySugFromCfg` |  |
| 29 | POST | `/manpower/cfg/supplierSugFromCfg` |  |
| 30 | POST | `/manpower/cfg/materialCategorySugFromCfg` |  |

## ManpowerNodeCfgFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/manpower/ManpowerNodeCfgFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/manpower/node/cfg/timeActivateOption` | 用工节点配置接口 @Author dongjunjie005 @Date 2024-04-08 16:54 |
| 2 | GET | `/manpower/node/cfg/defaultActivateOption` | 用工节点配置接口 @Author dongjunjie005 @Date 2024-04-08 16:54 |
| 3 | GET | `/manpower/node/cfg/defaultActivateOptionV2` | 用工节点配置接口 @Author dongjunjie005 @Date 2024-04-08 16:54 |
| 4 | POST | `/manpower/node/cfg/examineOneLevelOption` | 用工节点配置接口 @Author dongjunjie005 @Date 2024-04-08 16:54 |
| 5 | POST | `/manpower/node/cfg/examineTwoLevelOption` |  |
| 6 | POST | `/manpower/node/cfg/examineOneLevelOptionDefaultList` |  |

## ManpowerTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/manpower/ManpowerTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/manpower/task/queryProjectDetail` | 用工任务管理接口 @author dongjunjie005 @date 2024-04-01 15:40 |
| 2 | GET | `/manpower/task/queryTaskDetails` | 用工任务管理接口 @author dongjunjie005 @date 2024-04-01 15:40 |
| 3 | GET | `/manpower/task/queryTaskDetailsSameRestart` | 用工任务管理接口 @author dongjunjie005 @date 2024-04-01 15:40 |
| 4 | GET | `/manpower/task/queryTaskDetail` | 用工任务管理接口 @author dongjunjie005 @date 2024-04-01 15:40 |
| 5 | GET | `/manpower/task/queryNodeCanRestart` |  |
| 6 | GET | `/manpower/task/queryTaskLogs` |  |
| 7 | GET | `/manpower/task/queryAllMaterialAndSupplier` |  |
| 8 | GET | `/manpower/task/queryAllMdm` |  |
| 9 | POST | `/manpower/task/visitTimeLimit` |  |

## ManpowerTaskOrderFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/manpower/ManpowerTaskOrderFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/manpower/order/queryByPurchaseOrderNo` |  |
| 2 | GET | `/manpower/order/queryMeasureDetailByRecheck` |  |

## MaterialMeasureTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/manpower/MaterialMeasureTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/manpower/task/measureApply` |  |
| 2 | POST | `/manpower/task/invokeRecheckAgain` |  |
| 3 | POST | `/manpower/task/createByDelivery` |  |

## MaterialMeasureTaskFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/manpower/MaterialMeasureTaskFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/manpower/measure/task/getMeasureTaskInfo` |  |
| 2 | POST | `/manpower/measure/task/saveMaterialMeasure` |  |
| 3 | GET | `/manpower/measure/task/queryTaskDetail` |  |
| 4 | GET | `/manpower/measure/task/queryTaskDetailByPurchaseOrderNo` |  |
| 5 | POST | `/manpower/task/queryMeasureUnitList` |  |
| 6 | POST | `/manpower/task/saveUsageConfirm` |  |
| 7 | POST | `/manpower/task/saveSkuInfo` |  |
| 8 | POST | `/manpower/task/queryUsageConfirm` |  |
| 9 | POST | `/manpower/task/batchQueryTaskDispatchExt` |  |
| 10 | GET | `/manpower/task/queryRecheckScaleOpenLable` |  |
| 11 | POST | `/manpower/task/getLastMeasureInfoV2` |  |
