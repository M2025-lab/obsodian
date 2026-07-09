---
# 📋 Starlord REST 接口字典

**总计**: 748 个接口 | 89 个 Controller/Feign 类

> 自动生成自代码扫描 — 覆盖 `edar-starlord-web` (Controller) + `edar-starlord-api` (FeignClient)
> 含 JavaDoc 注释描述

## 📑 模块目录

| # | 模块 | 接口数 | 链接 |
|---|------|------|------|
| 1 | 后门 & 工具 & 测试接口 | 81 | [[后门-工具-测试|详情]] |
| 2 | 人力配置模块 | 62 | [[人力配置|详情]] |
| 3 | 安装任务 & 验收模块 | 49 | [[安装任务|详情]] |
| 4 | 调度派工模块 | 31 | [[调度派工|详情]] |
| 5 | 测量复尺模块 | 32 | [[测量复尺|详情]] |
| 6 | 材料流程配置模块 | 115 | [[材料流程|详情]] |
| 7 | 主材任务模块 | 173 | [[主材任务|详情]] |
| 8 | Feign 内部接口 | 154 | [[Feign接口|详情]] |
| 9 | 其他接口 | 51 | [[其他|详情]] |

---

## 接口分布统计

| 模块 | 类名 | 接口数 |
|---|------|------|
| 后门 & 工具 & 测试接口 | BackDoorController | 36 |
|  | TestController | 28 |
|  | ToolsController | 17 |
| 人力配置模块 | ManpowerCfgFeign | 30 |
|  | MaterialMeasureTaskFeign | 11 |
|  | ManpowerTaskFeign | 9 |
|  | ManpowerNodeCfgFeign | 6 |
|  | MaterialMeasureTaskController | 3 |
|  | ManpowerTaskOrderFeign | 2 |
|  | ManpowerCfgController | 1 |
| 安装任务 & 验收模块 | MaterialMeasureController | 16 |
|  | InstallerTaskFeign | 13 |
|  | AcceptanceReportFeign | 7 |
|  | MaterialConstructionTaskFeign | 5 |
|  | OmsMessageSyncFeign | 4 |
|  | TestController | 2 |
|  | ReplenishCoordinatorTaskController | 1 |
|  | TaskDispatchFeign | 1 |
| 调度派工模块 | ReplenishCoordinatorTaskController | 11 |
|  | DispatchCommonFeign | 10 |
|  | CoordinatorPlatformController | 3 |
|  | DispatchTaskTraceController | 3 |
|  | ReplenishCoordinatorTaskFeign | 2 |
|  | StarlordController | 2 |
| 测量复尺模块 | MeasureConfigRuleFeign | 8 |
|  | MeasureFormTemplateConfigurationFeign | 8 |
|  | StarlordController | 5 |
|  | MeasureApplyRangeConfigFeign | 4 |
|  | MeasureFormTemplateFeign | 3 |
|  | MeasureApplyFeign | 2 |
|  | MeasureConfigRuleExportController | 1 |
|  | MeasureFormFixController | 1 |
| 材料流程配置模块 | MaterialFlowCategoryController | 16 |
|  | DeliveryFlowCategoryController | 15 |
|  | MaterialFlowRuleInfoController | 14 |
|  | DeliveryFlowRuleInfoController | 11 |
|  | CategoryProcessFeign | 8 |
|  | MaterialFlowSelectQueryController | 8 |
|  | CategoryProcessController | 7 |
|  | DeliveryFlowSelectQueryController | 7 |
|  | MaterialDeliveryV2Controller | 7 |
|  | MaterialFlowProcessController | 7 |
|  | MaterialFlowRuleQueryFeign | 5 |
|  | DeliveryFlowTaskController | 4 |
|  | DeliveryFlowRuleFeign | 3 |
|  | MaterialFlowQueryController | 1 |
|  | MaterialFlowRuleQueryController | 1 |
|  | OfcDispatchConfigQueryService | 1 |
| 主材任务模块 | RefreshDataController | 83 |
|  | MaterialTaskInstallerTaskController | 21 |
|  | MaterialDelayProcessFeign | 13 |
|  | MaterialTaskBizV2Feign | 13 |
|  | MaterialButlerFeign | 6 |
|  | DelayFeign | 5 |
|  | MaterialDeliveryFeign | 5 |
|  | MaterialCustomerFeign | 4 |
|  | TaskProgressTipFeign | 4 |
|  | CommonTaskModuleController | 3 |
|  | HomePaymentApi | 3 |
|  | MaterialTaskController | 3 |
|  | MaterialDelayApproveFeign | 2 |
|  | MaterialTaskConfigFeign | 2 |
|  | MaterialTaskProcessTemplateController | 2 |
|  | ReplenishCoordinatorTaskController | 2 |
|  | MaterialScheduleFeign | 1 |
|  | OfcDispatchConfigQueryService | 1 |
| Feign 内部接口 | TaskDispatchFeign | 58 |
|  | MaterialTemplateFeign | 37 |
|  | TaskDispatchV2Feign | 22 |
|  | TaskTemplateFeign | 18 |
|  | AuditFeign | 5 |
|  | CategoryProcessFeign | 5 |
|  | MaterialTaskConfigFeign | 5 |
|  | WorkbenchTaskFeign | 2 |
|  | MaterialTaskFeign | 1 |
|  | SdmMessageSyncFeign | 1 |
| 其他接口 | CustomerHomeFeign | 6 |
|  | MaterialMigrationV2Controller | 4 |
|  | StarlordInspectionController | 4 |
|  | StockUpCycleFeign | 4 |
|  | TaskDispatchV2Controller | 4 |
|  | CallRecordController | 3 |
|  | MaterialSelfBuyFeign | 3 |
|  | McpTestController | 3 |
|  | TaskMaterialRulerController | 3 |
|  | HolidayCfgFeign | 2 |
|  | AuditController | 1 |
|  | BigCFeignService | 1 |
|  | CipherFeign | 1 |
|  | ESTraceTaskFeignService | 1 |
|  | EventSubExtFixController | 1 |
|  | MaterialTemplateV2Controller | 1 |
|  | MeasureConfigRuleMigrationController | 1 |
|  | MeasureFormFixController | 1 |
|  | OfcDispatchConfigQueryService | 1 |
|  | PermissionFeignService | 1 |
|  | ProductScheduleFeign | 1 |
|  | ReplenishCoordinatorTaskController | 1 |
|  | StarlordConfigController | 1 |
|  | StarlordController | 1 |
|  | XcsController | 1 |

---
**生成时间**: 2026-07-09 10:42
