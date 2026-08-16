# 其他接口

**接口数**: 51 | **类数**: 25

---

## AuditController

`edar-starlord-web/src/main/java/com/ke/utopia/web/AuditController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/audit/supplier-list` |  |

## BigCFeignService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/BigCFeignService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/ershou-decor-app-api/api/v1/config/xcxbrand` | 大c接口 |

## CallRecordController

`edar-starlord-web/src/main/java/com/ke/utopia/web/CallRecordController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/callRecord/queryCallRecordVoice` | CallRecordController @author chenzifeng004 @date 2024-11-13 |
| 2 | POST | `/callRecord/list` | CallRecordController @author chenzifeng004 @date 2024-11-13 |
| 3 | GET | `/callRecord/getCallRecord` | CallRecordController @author chenzifeng004 @date 2024-11-13 |

## CipherFeign

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/CipherFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/encrypt` | CRM 服务调用 |

## CustomerHomeFeign

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/CustomerHomeFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/open/finance/config/strong-catena-city` | @Author: chengzhijun @Description: @Date: Created in 14:51 2021/12/23 @Modified By: |
| 2 | GET | `/api/customer/maintain-user/search-all` | @Author: chengzhijun @Description: @Date: Created in 14:51 2021/12/23 @Modified By: |
| 3 | GET | `/open/furniture/maintain-user/task-user` | 获取维护人详情 @param customerCommissionCode @param emergency @return |
| 4 | GET | `/open/furniture/decorate/get-decorate-info` | 获取任务成员 @param customerCommissionCode @param maintainType @return |
| 5 | GET | `/open/furniture/decorate/get-furniture-info` | 获取 |
| 6 | GET | `/open/user/operation/config` | 通过DFcode 获取整装订单号 @param customerCommissionCodes @return |

## ESTraceTaskFeignService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/ESTraceTaskFeignService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/estracetaskfeignservice` | @author ：LM @date ：Created in 2019-10-16 15:30 @description：搜索服务接口 @modified By： @version: 1.0.0 |

## EventSubExtFixController

`edar-starlord-web/src/main/java/com/ke/utopia/web/fix/EventSubExtFixController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/event_pub/ext/send/retry` |  |

## HolidayCfgFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/HolidayCfgFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/holiday-cfg/save` |  |
| 2 | GET | `/holiday-cfg/queryById` |  |

## MaterialMigrationV2Controller

`edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialMigrationV2Controller.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/migrationPurchaseOrder` |  |
| 2 | GET | `/migrationPurchaseOrderOne` |  |
| 3 | GET | `/migrationFinishedPurchaseOrder` |  |
| 4 | GET | `/migrationAutoAcceptPurchaseOrder` |  |

## MaterialSelfBuyFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/selfbuy/MaterialSelfBuyFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/selfBuy/tob/list` | @author: lixiangyang @Date: 2023/6/8 19:49 |
| 2 | POST | `/selfBuy/add` | @author: lixiangyang @Date: 2023/6/8 19:49 |
| 3 | POST | `/selfBuy/del` | @author: lixiangyang @Date: 2023/6/8 19:49 |

## MaterialTemplateV2Controller

`edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialTemplateV2Controller.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/config/export-template-list` |  |

## McpTestController

`edar-starlord-web/src/main/java/com/ke/utopia/web/mcp/McpTestController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/mcp/query/match/task` |  |
| 2 | GET | `/mcp/query/match/task/detail` | 检查预算预览中是否存在匹配的sku @param projectOrderId 订单ID @param materialName 材料名称 @param supplierName 供应商名称 @return 0：报价无这个品 1：报价有这个品  无任务 2：报价有这个品 任务未激活 3: 报价有这个品 任务进行中 4: 报价有这个品 任务已完成。 |
| 3 | GET | `/mcp/query/match/task/process` |  |

## MeasureConfigRuleMigrationController

`edar-starlord-web/src/main/java/com/ke/utopia/web/fix/MeasureConfigRuleMigrationController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/fix/measure-config-rule/migrate-from-apollo` | 迁移接口对应库表：material_measure_interface_config + _mdm_rel + _category_rel。 实现依赖 {@link com.ke.utopia.dao.MeasureConfigRuleDao#replaceRelations}，与拆表后的正式写入路径一致。 |

## MeasureFormFixController

`edar-starlord-web/src/main/java/com/ke/utopia/web/fix/MeasureFormFixController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/task_dispatch/sdm_diff` |  |

## OfcDispatchConfigQueryService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/OfcDispatchConfigQueryService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/api/ousuo/order/branchCompanyList` | 获取供应链侧的分公司 |

## PermissionFeignService

`edar-starlord-manager/src/main/java/com/ke/utopia/manager/feign/PermissionFeignService.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/{app}/users/{ucid}/roles` | 获取用户权限 @param app app @param ucid uc id @return 权限信息 |

## ProductScheduleFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/ProductScheduleFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/product-schedule/complete` |  |

## ReplenishCoordinatorTaskController

`edar-starlord-web/src/main/java/com/ke/utopia/web/coordinator/ReplenishCoordinatorTaskController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/re-procurement/query-brand-order` |  |

## StarlordConfigController

`edar-starlord-web/src/main/java/com/ke/utopia/web/StarlordConfigController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/starlord/config/cityInstallWorkerRule` |  |

## StarlordController

`edar-starlord-web/src/main/java/com/ke/utopia/web/butler/StarlordController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/page/list-material-task` | 获取主材任务列表 @param param @return |

## StarlordInspectionController

`edar-starlord-web/src/main/java/com/ke/utopia/web/StarlordInspectionController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/starlord/refreshAll` | 全量刷新订单 @param refreshDTO @return |
| 2 | GET | `/starlord/refreshByTaskType` | 刷新单个的订单的任务 @param projectOrderId @param taskType @param orderNo @return |
| 3 | POST | `/starlord/refreshOrder` | 补下单任务 @param taskDTO @return |
| 4 | POST | `/starlord/inspection` | @param projectOrderId @param taskType @throws Exception |

## StockUpCycleFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/stockup/StockUpCycleFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/stock-up/add` |  |
| 2 | POST | `/stock-up/status/update` |  |
| 3 | POST | `/stock-up/update` |  |
| 4 | POST | `/stock-up/query` |  |

## TaskDispatchV2Controller

`edar-starlord-web/src/main/java/com/ke/utopia/web/TaskDispatchV2Controller.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-task/task-related-es-info` |  |
| 2 | POST | `/material-task/task-project/init` |  |
| 3 | POST | `/retail-task/task-project/init` |  |
| 4 | POST | `/material-task/task-order/init` |  |

## TaskMaterialRulerController

`edar-starlord-web/src/main/java/com/ke/utopia/web/TaskMaterialRulerController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/list-completed-material-ruler` | 主材留尺子任务相关接口 copy from eden-butler @author taokun @date 2020/9/14 |
| 2 | GET | `/get-tech-disclosure-material-ruler` | 主材留尺子任务相关接口 copy from eden-butler @author taokun @date 2020/9/14 |
| 3 | GET | `/list-material-ruler-info` |  |

## XcsController

`edar-starlord-web/src/main/java/com/ke/utopia/web/XcsController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/v1/xcs/delay-reason-recovery` | 类名：XcsController 描述：小材神测试 作者：zifengchen 日期：2025/7/18 |
