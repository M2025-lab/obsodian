# 后门 & 工具 & 测试接口

**接口数**: 81 | **类数**: 3

---

## BackDoorController

`edar-starlord-web/src/main/java/com/ke/utopia/web/BackDoorController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/backdoor/cache/claer/backdoor/scheduleProcessor` | 定时任务后门 |
| 2 | POST | `/backdoor/cache/claer/backdoor/mqmsg` | MQ消息后门 |
| 3 | POST | `/backdoor/cache/claer/backdoor/sinvoke` | service接口消息后门 |
| 4 | GET | `/backdoor/cache/claer/backdoor/fullCalculate` |  |
| 5 | GET | `/backdoor/cache/claer/backdoor/activateTaskDispatchDirect` |  |
| 6 | GET | `/backdoor/cache/claer/backdoor/activateTaskDispatchByTaskId` |  |
| 7 | GET | `/backdoor/cache/claer/backdoor/batchCancelMaterialOrderTask` |  |
| 8 | GET | `/backdoor/cache/claer/backdoor/completeNodeDirect` |  |
| 9 | GET | `/backdoor/cache/claer/backdoor/completeNode` |  |
| 10 | GET | `/backdoor/cache/claer/backdoor/refreshSupplier` |  |
| 11 | GET | `/backdoor/cache/claer/backdoor/refreshMeasureApplySupplier` |  |
| 12 | GET | `/backdoor/cache/claer/backdoor/instanceBind` |  |
| 13 | GET | `/backdoor/cache/claer/backdoor/opnode` |  |
| 14 | POST | `/backdoor/cache/claer/backdoor/autoCommitMeasureApply` |  |
| 15 | POST | `/backdoor/cache/claer/backdoor/project/search` |  |
| 16 | POST | `/backdoor/cache/claer/backdoor/config/search` |  |
| 17 | POST | `/backdoor/cache/claer/backdoor/athena/search` |  |
| 18 | GET | `/backdoor/cache/claer` |  |
| 19 | GET | `/backdoor/cache/claer/backdoor/redis/exist` |  |
| 20 | GET | `/backdoor/cache/claer/backdoor/redis/remove` |  |
| 21 | GET | `/backdoor/cache/claer/backdoor/order/executor/fill` |  |
| 22 | POST | `/backdoor/cache/claer/backdoor/order/taskDispatchNode/update` |  |
| 23 | POST | `/backdoor/cache/claer/backdoor/order/taskDispatch/update` |  |
| 24 | GET | `/backdoor/cache/claer/backdoor/order/taskDispatch/delete` |  |
| 25 | GET | `/backdoor/cache/claer/backdoor/order/taskDispatchNode/delete` |  |
| 26 | POST | `/backdoor/cache/claer/backdoor/order/taskDispatchNode/resend` |  |
| 27 | GET | `/backdoor/cache/claer/refreshDataProcessStatus` |  |
| 28 | POST | `/backdoor/cache/claer/backdoor/deleteTask` |  |
| 29 | POST | `/backdoor/cache/claer/backdoor/startTask` |  |
| 30 | POST | `/backdoor/cache/claer/backdoor/bindPackage` |  |
| 31 | GET | `/backdoor/cache/claer/refresh-vss-sync` |  |
| 32 | GET | `/backdoor/cache/claer/backdoor/diff` |  |
| 33 | GET | `/backdoor/cache/claer/backdoor/sendSdmByTaskId` |  |
| 34 | GET | `/backdoor/cache/claer/backdoor/copyConfigByProcessCode` | 复制配置的返补考核条件，并生效 |
| 35 | GET | `/backdoor/cache/claer/backdoor/copyConfigByProcessCodeList` | 复制配置的返补考核条件，并生效 |
| 36 | POST | `/backdoor/cache/claer/backdoor/updateAssessmentTime` | 复制配置的返补考核条件，并生效 |

## TestController

`edar-starlord-web/src/main/java/com/ke/utopia/web/TestController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/test/testVssMsg` |  |
| 2 | POST | `/test/testScmMsg` |  |
| 3 | POST | `/test/testScmMeasure` |  |
| 4 | POST | `/test/testRetry` |  |
| 5 | POST | `/test/userCreate` |  |
| 6 | GET | `/test/refreshTask2_0` |  |
| 7 | GET | `/test/refreshTask2_0/withMdmCode` |  |
| 8 | GET | `/test/refreshTask2_0/withFile` |  |
| 9 | GET | `/test/retryById` |  |
| 10 | GET | `/test/checkMaterialDeliveryBatch` |  |
| 11 | POST | `/test/refreshInstall` |  |
| 12 | GET | `/test/message` |  |
| 13 | GET | `/test/delayPush` |  |
| 14 | GET | `/test/clean` |  |
| 15 | POST | `/test/refreshProcessCode` |  |
| 16 | POST | `/test/refreshEstime` |  |
| 17 | GET | `/test/calculatePlanActiveTime` |  |
| 18 | GET | `/test/updatePlanActiveTime` |  |
| 19 | GET | `/test/activatePreCondition` |  |
| 20 | POST | `/test/insertRetryDelayQueue` |  |
| 21 | POST | `/test/insertMaterialFlowRuleTemplateMapping` |  |
| 22 | POST | `/test/deleteSpecifiedTable` |  |
| 23 | GET | `/tools/queryTaskCreateFailReasons` |  |
| 24 | GET | `/tools/queryMeasureReason` |  |
| 25 | GET | `/tools/delCategoryMapping` |  |
| 26 | GET | `/tools/reBuildCategoryMapping` |  |
| 27 | GET | `/tools/getAllCategoryCodeAndTemplateMappingError` |  |
| 28 | GET | `/tools/publishMaterialCustomer` |  |

## ToolsController

`edar-starlord-web/src/main/java/com/ke/utopia/web/ToolsController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/tools/updateTaskById` |  |
| 2 | POST | `/tools/updateTaskNodeById` |  |
| 3 | POST | `/tools/updateCfgTaskById` |  |
| 4 | POST | `/tools/updateCfgNodeById` |  |
| 5 | POST | `/tools/updateCfgProcessById` |  |
| 6 | POST | `/tools/updateCfgTemplateById` |  |
| 7 | POST | `/tools/updateCfgTemplateUnitById` |  |
| 8 | POST | `/tools/updateNMaterialRouteById` |  |
| 9 | GET | `/tools/delTemplateDatatById` |  |
| 10 | GET | `/tools/delTemplateDatatByTemplateId` |  |
| 11 | GET | `/tools/activityTemplateDatatById` |  |
| 12 | GET | `/tools/upodateMeasureRemarkFilter` |  |
| 13 | GET | `/tools/updatePurchaseOrder` |  |
| 14 | GET | `/tools/deleteCfgTemplateById` |  |
| 15 | GET | `/tools/calculateTimeProcess` |  |
| 16 | GET | `/tools/delProcessByProcessCode` |  |
| 17 | POST | `/tools/delProcessByProcessCodeList` | 批量删除，入参格式: processCodeList=code1_state1,code2_state2 每对用下划线连接processCode和state，多对用逗号分隔 |
