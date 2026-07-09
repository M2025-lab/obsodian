# Edar-Starlord REST接口 · 数据表 · 字段映射分析

**生成时间**: 2026-07-08 21:09:00
**项目路径**: `/Users/mirror/IdeaProjects/edar-starlord`

---

**总计**: 39 个 Controller, 346 个 REST 接口, 52 张数据表

## 📋 目录

| # | Controller | 接口数 | 模块标签 |
|---|-----------|--------|---------|
| 1 | [AuditController](#auditcontroller) | 1 | Audit |
| 2 | [BackDoorController](#backdoorcontroller) | 35 | BackDoor |
| 3 | [CallRecordController](#callrecordcontroller) | 3 | CallRecord |
| 4 | [CommonTaskModuleController](#commontaskmodulecontroller) | 3 | 主材任务模块接口 |
| 5 | [MaterialDeliveryV2Controller](#materialdeliveryv2controller) | 7 | MaterialDeliveryV2 |
| 6 | [MaterialMeasureController](#materialmeasurecontroller) | 16 | 主材测量复尺controller |
| 7 | [MaterialMigrationV2Controller](#materialmigrationv2controller) | 4 | MaterialMigrationV2 |
| 8 | [MaterialTemplateV2Controller](#materialtemplatev2controller) | 1 | MaterialTemplateV2 |
| 9 | [MeasureConfigRuleExportController](#measureconfigruleexportcontroller) | 1 | MeasureConfigRuleExport |
| 10 | [RefreshDataController](#refreshdatacontroller) | 71 | RefreshData |
| 11 | [StarlordConfigController](#starlordconfigcontroller) | 1 | 主材任务切换2.5配置查询接口 |
| 12 | [StarlordInspectionController](#starlordinspectioncontroller) | 4 | StarlordInspection |
| 13 | [TaskDispatchV2Controller](#taskdispatchv2controller) | 4 | TaskDispatchV2 |
| 14 | [TaskMaterialRulerController](#taskmaterialrulercontroller) | 3 | 主材留尺任务相关接口 |
| 15 | [TestController](#testcontroller) | 30 | Test |
| 16 | [ToolsController](#toolscontroller) | 17 | Tools |
| 17 | [WelcomeController](#welcomecontroller) | 1 | Welcome |
| 18 | [XcsController](#xcscontroller) | 1 | Xcs |
| 19 | [StarlordController](#starlordcontroller) | 8 | 主材任务相关的接口 |
| 20 | [ReplenishCoordinatorTaskController](#replenishcoordinatortaskcontroller) | 15 | ReplenishCoordinatorTask |
| 21 | [CategoryProcessController](#categoryprocesscontroller) | 7 | CategoryProcess |
| 22 | [DeliveryFlowCategoryController](#deliveryflowcategorycontroller) | 15 | 材料流程品类规则配置 |
| 23 | [DeliveryFlowRuleInfoController](#deliveryflowruleinfocontroller) | 11 | 材料流程配置规则页面查询 |
| 24 | [DeliveryFlowSelectQueryController](#deliveryflowselectquerycontroller) | 6 | 材料流程配置页面筛选项条件 |
| 25 | [MaterialTaskProcessTemplateController](#materialtaskprocesstemplatecontroller) | 2 | MaterialTaskProcessTemplate |
| 26 | [EventSubExtFixController](#eventsubextfixcontroller) | 1 | 消息消费测试接口 |
| 27 | [MeasureConfigRuleMigrationController](#measureconfigrulemigrationcontroller) | 1 | 交界面规则迁移接口 |
| 28 | [MeasureFormFixController](#measureformfixcontroller) | 2 | 测量申请单补数据接口 |
| 29 | [MaterialTaskInstallerTaskController](#materialtaskinstallertaskcontroller) | 19 | 主材任务controller |
| 30 | [ManpowerCfgController](#manpowercfgcontroller) | 1 | ManpowerCfg |
| 31 | [MaterialMeasureTaskController](#materialmeasuretaskcontroller) | 3 | MaterialMeasureTask |
| 32 | [MaterialFlowCategoryController](#materialflowcategorycontroller) | 16 | 材料流程品类规则配置 |
| 33 | [MaterialFlowProcessController](#materialflowprocesscontroller) | 7 | 材料流程节点流程 |
| 34 | [MaterialFlowQueryController](#materialflowquerycontroller) | 1 | MaterialFlowQuery |
| 35 | [MaterialFlowRuleInfoController](#materialflowruleinfocontroller) | 14 | 材料流程配置规则页面查询 |
| 36 | [MaterialFlowRuleQueryController](#materialflowrulequerycontroller) | 1 | 材料流程配置规则页面查询 |
| 37 | [MaterialFlowSelectQueryController](#materialflowselectquerycontroller) | 8 | 材料流程配置页面筛选项条件 |
| 38 | [McpTestController](#mcptestcontroller) | 3 | mcp测试相关接口 |
| 39 | [DispatchTaskTraceController](#dispatchtasktracecontroller) | 2 | DispatchTaskTrace |

---

# AuditController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/AuditController.java`

---

## POST `/audit/supplier-list`

**返回类型**: `ResultDTO<Page<AuditSupplierDTO>>`

**调用链**:
  - `AuditService.getAuditSupplierList()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `processStatus` | Byte | `task_dispatch` | `process_status` |  |
| `processStatusStr` | String | `` | `` |  |
| `supplierCode` | String | `task_dispatch` | `supplier_code` |  |
| `supplierName` | String | `task_dispatch` | `supplier_name` |  |
| `supplierPhone` | String | `` | `` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `materialCode` | String | `task_dispatch` | `material_code` |  |
| `materialName` | String | `task_dispatch` | `material_name` |  |
| `estimatedTime` | Date | `task_dispatch` | `estimated_time` |  |
| `taskDispatchNodeId` | Long | `task_dispatch` | `id` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `taskNodeName` | String | `` | `` |  |
| `projectOrderInfo` | ProjectOrderInfo | `` | `` |  |
| `address` | String | `` | `` |  |
| `operatorId` | Long | `task_dispatch` | `id` |  |
| `orderNo` | String | `task_dispatch` | `order_no` |  |
| `sourceType` | Integer | `task_dispatch` | `source_type` |  |
| `serviceMode` | Integer | `task_dispatch` | `service_mode` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `ProjectOrderManager`
- `SupplierManager`


---

# BackDoorController
**基路径**: `/backdoor/cache/claer`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/BackDoorController.java`

---

## GET `/backdoor/cache/claer/backdoor/scheduleProcessor`

**返回类型**: `ResultDTO<List<String>>`

---

## POST `/backdoor/cache/claer/backdoor/mqmsg`

**返回类型**: `ResultDTO<List<String>>`

---

## POST `/backdoor/cache/claer/backdoor/sinvoke`

**返回类型**: `ResultDTO`

---

## GET `/backdoor/cache/claer/backdoor/fullCalculate`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `FullCalculateService.fullCalculate()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/activateTaskDispatchDirect`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.activateTaskDispatchDirect()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/activateTaskDispatchByTaskId`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.activateTaskDispatchByTaskId()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/batchCancelMaterialOrderTask`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `TaskDispatchDao.getById()`
  - `MaterialCancelV2Service.batchCancelMaterialOrderTask()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ScmManager`
- `ScmSupplierManager`

---

## GET `/backdoor/cache/claer/backdoor/completeNodeDirect`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.completeNodeDirect()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/completeNode`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.completeNode()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/refreshSupplier`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.refreshSupplier()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/refreshMeasureApplySupplier`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.refreshMeasureApplySupplier()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/instanceBind`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.instanceBind()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/opnode`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `BackdoorService.addNodeInstance()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**
  - `BackdoorService.deleteNodeInstance()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## POST `/backdoor/cache/claer/backdoor/autoCommitMeasureApply`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `MeasureApplyBusinessService.autoCommitMeasureApply()`

---

## POST `/backdoor/cache/claer/backdoor/project/search`
> **查询项目**

**返回类型**: `ResultDTO`

**调用链**:
  - `ProjectOrderManager.searchProjectOrderList()`

---

## POST `/backdoor/cache/claer/backdoor/config/search`
> **查询项目**

**返回类型**: `ResultDTO`

**调用链**:
  - `NMaterialTemplateUnitMapper.selectMaterialSupplierCode()`

---

## POST `/backdoor/cache/claer/backdoor/athena/search`
> **查询配置**

**返回类型**: `List`

**调用链**:
  - `AthenaManager.listConfirmedMaterial()`

---

## GET `/backdoor/cache/claer/backdoor/redis/exist`
> **中控test**

**返回类型**: `boolean`

**调用链**:
  - `RedisService.exists()`

---

## GET `/backdoor/cache/claer/backdoor/redis/remove`
> **中控test**

**返回类型**: `boolean`

**调用链**:
  - `RedisService.remove()`

---

## GET `/backdoor/cache/claer/backdoor/order/executor/fill`
> **中控test**

**返回类型**: `void`

**调用链**:
  - `BackdoorService.fillExecutorByProject()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## POST `/backdoor/cache/claer/backdoor/order/taskDispatchNode/update`
> **缓存清楚**

**返回类型**: `void`

**调用链**:
  - `TaskDispatchNodeMapper.updateByPrimaryKeySelective()`

---

## POST `/backdoor/cache/claer/backdoor/order/taskDispatch/update`
> **缓存清楚**

**返回类型**: `void`

**调用链**:
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`

---

## GET `/backdoor/cache/claer/backdoor/order/taskDispatch/delete`
> **redis缓存清楚**

**返回类型**: `void`

**调用链**:
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`

---

## GET `/backdoor/cache/claer/backdoor/order/taskDispatchNode/delete`
> **更新node节点信息**

**返回类型**: `void`

**调用链**:
  - `TaskDispatchNodeMapper.deleteByPrimaryKey()`

---

## POST `/backdoor/cache/claer/backdoor/order/taskDispatchNode/resend`
> **更新node节点信息**

**返回类型**: `void`

**调用链**:
  - `TaskDispatchNodeMapper.selectByExample()`
  - `TaskDispatchNodeMapper.updateByPrimaryKeySelective()`
  - `TaskDispatchMapper.selectByExample()`
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`
  - `EventDrivenPublisher.persistPublishMessage()`

---

## GET `/backdoor/cache/claer/refreshDataProcessStatus`

**返回类型**: `void`

**调用链**:
  - `ProjectOrderManager.getProjectOrder()`
  - `TaskDispatchDao.listByExample()`
  - `TaskDispatchNodeDao.listByExample()`
  - `TaskDispatchNodeDao.updateById()`

---

## POST `/backdoor/cache/claer/backdoor/deleteTask`

**返回类型**: `void`

**调用链**:
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`
  - `TaskDispatchNodeDao.listByTaskId()`
  - `TaskDispatchNodeDao.updateById()`

---

## POST `/backdoor/cache/claer/backdoor/startTask`

**返回类型**: `void`

**调用链**:
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`
  - `TaskDispatchNodeDao.listByExample()`
  - `TaskDispatchNodeDao.updateById()`

---

## POST `/backdoor/cache/claer/backdoor/bindPackage`

**返回类型**: `String`

**调用链**:
  - `PackageConstructionManager.queryMaterialDemandList()`
  - `PackageConstructionManager.queryList()`
  - `TaskDispatchDao.getById()`
  - `TaskDispatchNodeDao.listByExample()`
  - `TaskDispatchNodeDao.updateByPrimaryKeySelective()`

---

## GET `/backdoor/cache/claer/refresh-vss-sync`

**返回类型**: `void`

**调用链**:
  - `ScmSupplierManager.addOrUpdateByOrderNo()`
  - `TaskDispatchDao.getById()`
  - `TaskDispatchNodeDao.getById()`

---

## GET `/backdoor/cache/claer/backdoor/diff`

**返回类型**: `List<String>`

**调用链**:
  - `ScmManager.search()`
  - `TaskDispatchDao.getByPurchaseOrderNo()`

---

## GET `/backdoor/cache/claer/backdoor/sendSdmByTaskId`

**返回类型**: `void`

**调用链**:
  - `SdmMessageSyncService.sendSdmByTaskId()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `ProjectInfoDao` → `project_info`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `project_info` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmSupplierManager`

---

## GET `/backdoor/cache/claer/backdoor/copyConfigByProcessCode`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `BackdoorService.copyConfig()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/backdoor/cache/claer/backdoor/copyConfigByProcessCodeList`

**返回类型**: `ResultDTO<Object>`

**调用链**:
  - `BackdoorService.copyConfig()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MeasureApplyDao` → `measure_apply`
    - `NMaterialTemplateDao` → `n_material_template`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialTaskDao` → `material_task`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `measure_apply` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## POST `/backdoor/cache/claer/backdoor/updateAssessmentTime`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `AssessmentTimeChangeHandler.updateAssessmentTime()`


---

# CallRecordController
**基路径**: `/callRecord`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/CallRecordController.java`

---

## GET `/callRecord/queryCallRecordVoice`

**返回类型**: `ResultDTO<List<CallRecordVoiceDTO>>`

**调用链**:
  - `CallRecordMsgService.queryCallRecordVoice()`
    - `CallRecordDao` → `call_record`
    - `SinanCallManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `call_record` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `channel` | String | `` | `` |  |
| `content` | String | `` | `` |  |
| `time` | String | `call_record` | `call_out_time` |  |
| `contentNoSpace` | String | `` | `` |  |
| `startTime` | Date | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `SinanCallManager`

---

## POST `/callRecord/list`
> **根据callId查询通话记录语音**

**返回类型**: `ResultDTO<List<CallRecordDTO>>`

**调用链**:
  - `CallRecordMsgService.queryCallRecordMsgByBusinessIdAndType()`
    - `CallRecordDao` → `call_record`
    - `SinanCallManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `call_record` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `call_record` | `id` |  |
| `callId` | String | `call_record` | `call_id` |  |
| `callType` | String | `call_record` | `call_type` |  |
| `projectOrderId` | Long | `call_record` | `project_order_id` |  |
| `businessId` | String | `call_record` | `business_id` |  |
| `businessType` | Integer | `call_record` | `business_type` |  |
| `callOutTime` | Date | `call_record` | `call_out_time` |  |
| `isConnected` | String | `call_record` | `is_connected` |  |
| `callDuration` | String | `call_record` | `call_duration` |  |
| `caller` | String | `call_record` | `caller` |  |
| `callerUcid` | String | `call_record` | `caller_ucid` |  |
| `callOutNumber` | String | `call_record` | `call_out_number` |  |
| `customerName` | String | `call_record` | `customer_name` |  |
| `customerUcid` | String | `call_record` | `customer_ucid` |  |
| `cityCode` | String | `call_record` | `city_code` |  |
| `cityName` | String | `call_record` | `city_name` |  |
| `hangUpSide` | String | `call_record` | `hang_up_side` |  |
| `hangUpSideName` | String | `call_record` | `hang_up_side_name` |  |
| `transferType` | String | `call_record` | `transfer_type` |  |
| `transferTypeName` | String | `call_record` | `transfer_type_name` |  |
| `callChannelName` | String | `call_record` | `call_channel_name` |  |
| `extensionField7` | String | `call_record` | `extension_field7` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `SinanCallManager`

---

## GET `/callRecord/getCallRecord`
> **根据callId查询通话记录语音**

**返回类型**: `ResultDTO<CallRecordDTO>`

**调用链**:
  - `CallRecordMsgService.getCallRecordByCallId()`
    - `CallRecordDao` → `call_record`
    - `SinanCallManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `call_record` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `call_record` | `id` |  |
| `callId` | String | `call_record` | `call_id` |  |
| `callType` | String | `call_record` | `call_type` |  |
| `projectOrderId` | Long | `call_record` | `project_order_id` |  |
| `businessId` | String | `call_record` | `business_id` |  |
| `businessType` | Integer | `call_record` | `business_type` |  |
| `callOutTime` | Date | `call_record` | `call_out_time` |  |
| `isConnected` | String | `call_record` | `is_connected` |  |
| `callDuration` | String | `call_record` | `call_duration` |  |
| `caller` | String | `call_record` | `caller` |  |
| `callerUcid` | String | `call_record` | `caller_ucid` |  |
| `callOutNumber` | String | `call_record` | `call_out_number` |  |
| `customerName` | String | `call_record` | `customer_name` |  |
| `customerUcid` | String | `call_record` | `customer_ucid` |  |
| `cityCode` | String | `call_record` | `city_code` |  |
| `cityName` | String | `call_record` | `city_name` |  |
| `hangUpSide` | String | `call_record` | `hang_up_side` |  |
| `hangUpSideName` | String | `call_record` | `hang_up_side_name` |  |
| `transferType` | String | `call_record` | `transfer_type` |  |
| `transferTypeName` | String | `call_record` | `transfer_type_name` |  |
| `callChannelName` | String | `call_record` | `call_channel_name` |  |
| `extensionField7` | String | `call_record` | `extension_field7` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `SinanCallManager`


---

# CommonTaskModuleController
**Swagger 标签**: `主材任务模块接口`
**基路径**: `/api/common-module`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/CommonTaskModuleController.java`

---

## GET `/api/common-module/get-measure-module`

**返回类型**: `ResultVO<CommonTaskModuleV0>`

**调用链**:
  - `CommonTaskModuleService.getMeasureCommonTaskModule()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `ScmMerchantManager`
- `WorkCenterManager`

---

## GET `/api/common-module/get-material-delivery-module`

**返回类型**: `ResultVO<CommonTaskModuleV0>`

**调用链**:
  - `CommonTaskModuleService.getMaterialBookingCommonTaskModule()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `ScmMerchantManager`
- `WorkCenterManager`

---

## GET `/api/common-module/get-order-module`

**返回类型**: `ResultVO<CommonTaskModuleV0>`

**调用链**:
  - `CommonTaskModuleService.getOrderCommonTaskModule()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `ScmMerchantManager`
- `WorkCenterManager`


---

# MaterialDeliveryV2Controller
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialDeliveryV2Controller.java`

---

## POST `/batch/material/list`

**返回类型**: `ResultDTO<List<QueryBatchMaterialListDTO>>`

**调用链**:
  - `MaterialBatchV2Service.queryBatchMaterialList()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `frontCategoryId` | String | `task_dispatch` | `id` |  |
| `frontCategoryName` | String | `` | `` |  |
| `icon` | String | `` | `` |  |
| `needMaterialArrivalNotify` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## GET `/batch/material/list/need_arrival_notify`

**返回类型**: `ResultDTO<List<QueryBatchMaterialListDTO>>`

**调用链**:
  - `MaterialBatchV2Service.queryBatchMaterialListNeedMaterialArrivalNotify()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `frontCategoryId` | String | `task_dispatch` | `id` |  |
| `frontCategoryName` | String | `` | `` |  |
| `icon` | String | `` | `` |  |
| `needMaterialArrivalNotify` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## GET `/batch/material/info`

**返回类型**: `ResultDTO<QueryBatchMaterialInfoDTO>`

**调用链**:
  - `MaterialBatchV2Service.queryBatchMaterialInfo()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `taskBatchId` | Long | `task_batch_node_relation` | `task_batch_id` |  |
| `projectOrderId` | Long | `task_dispatch` | `project_order_id` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `batchType` | Integer | `task_process_batch` | `batch_type` |  |
| `taskDispatchNodeIds` | String | `task_process_batch` | `task_dispatch_node_ids` |  |
| `processStatus` | Byte | `task_dispatch` | `process_status` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `state` | Byte | `task_dispatch` | `state` |  |
| `noticeRetainTime` | Date | `task_dispatch` | `notice_retain_time` |  |
| `activeTime` | Date | `task_process_batch` | `active_time` |  |
| `estimatedTime` | Date | `task_dispatch` | `estimated_time` |  |
| `executorType` | Byte | `task_dispatch` | `executor_type` |  |
| `executorId` | String | `task_dispatch` | `executor_id` |  |
| `executorName` | String | `task_dispatch` | `executor_name` |  |
| `remarks` | String | `task_dispatch` | `remarks` |  |
| `orderNos` | List<String> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## POST `/batch/material/submit`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialBatchV2Service.batchMaterialSubmit()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## POST `/batch/material/update`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialBatchV2Service.batchMaterialUpdate()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## POST `/batch/material/expect_time/query`

**返回类型**: `ResultDTO<BatchDeliveryTimeInfoDTO>`

**调用链**:
  - `MaterialBatchV2Service.queryDeliveryExpectedTime()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `cutoffTime` | String | `` | `` |  |
| `earliestTimeInterval` | Integer | `` | `` |  |
| `earliestTime` | Date | `` | `` |  |
| `latestTime` | Date | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## GET `/batch/material/detail`

**返回类型**: `ResultDTO<TaskProcessBatchDTO>`

**调用链**:
  - `MaterialBatchV2Service.queryBatchMaterialDetail()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `task_dispatch` | `id` |  |
| `projectOrderId` | Long | `task_dispatch` | `project_order_id` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `batchType` | Integer | `task_process_batch` | `batch_type` |  |
| `taskDispatchNodeIds` | String | `task_process_batch` | `task_dispatch_node_ids` |  |
| `processStatus` | Byte | `task_dispatch` | `process_status` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `state` | Byte | `task_dispatch` | `state` |  |
| `noticeRetainTime` | Date | `task_dispatch` | `notice_retain_time` |  |
| `estimatedTime` | Date | `task_dispatch` | `estimated_time` |  |
| `executorType` | Byte | `task_dispatch` | `executor_type` |  |
| `executorId` | String | `task_dispatch` | `executor_id` |  |
| `executorName` | String | `task_dispatch` | `executor_name` |  |
| `remarks` | String | `task_dispatch` | `remarks` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`


---

# MaterialMeasureController
**Swagger 标签**: `主材测量复尺controller`
**基路径**: `/api/material-measure`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialMeasureController.java`

---

## POST `/api/material-measure/save-task`

**返回类型**: `ResultVO`

**调用链**:
  - `MaterialMeasureService.submitMeasureFormTemplate()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/get-task`

**返回类型**: `ResultVO<MaterialMeasureVO>`

**调用链**:
  - `MaterialMeasureService.getMaterialMeasure()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/v2/get-task`

**返回类型**: `ResultVO<MaterialMeasureVO>`

**调用链**:
  - `MaterialMeasureService.getMaterialMeasure()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/v3/get-task-resized`

**返回类型**: `ResultVO<MaterialMeasureVO>`

**调用链**:
  - `MaterialMeasureService.getMaterialMeasureByResized()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/get-measure`

**返回类型**: `ResultVO<MeasureVO>`

**调用链**:
  - `MaterialMeasureService.getMeasureInfo()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## POST `/api/material-measure/get-task-measure/last-measure`

**返回类型**: `ResultVO<MaterialMeasureDataVO>`

**调用链**:
  - `MaterialMeasureService.getLastMeasureInfo()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## POST `/api/material-measure/get-task-measure/last-measure-v2`

**返回类型**: `ResultVO<MaterialMeasureDataVO>`

**调用链**:
  - `MaterialMeasureService.getLastMeasureInfoV2()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## POST `/api/material-measure/get-task-measure`

**返回类型**: `ResultVO<MaterialMeasureDataVO>`

**调用链**:
  - `MaterialMeasureService.getMaterialMeasureData()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## POST `/api/material-measure/update-task-measure`

**返回类型**: `ResultVO<Boolean>`

**调用链**:
  - `MaterialMeasureService.updateMaterialMeasureData()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/has-measure-authority`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialMeasureService.hasMaterialMeasure()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/has-order-receive`
> **判断供应链是否已经单**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialMeasureService.hasOrderReceive()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/query-measure`
> **判断供应链是否已经单**

**返回类型**: `ResultDTO<MaterialMeasureListVO>`

**调用链**:
  - `MaterialMeasureService.queryMaterialMeasure()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/task/list-recent-material-ruler-measure-recheck`
> **判断供应链是否已经单**

**返回类型**: `ResultDTO<List<MeasureListVO>>`

**调用链**:
  - `MaterialMeasureService.listMeasureModuleTask()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/task/queryRecheckScaleTaskListByUcid`
> **判断供应链是否已经单**

**返回类型**: `ResultDTO<PageInfo<MeasureListVO>>`

**调用链**:
  - `MaterialMeasureService.queryRecheckScaleTaskListByUcid()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/task/queryMeasureListGrade`
> **判断供应链是否已经单**

**返回类型**: `ResultDTO<List<MeasuerGradeListVo>>`

**调用链**:
  - `MaterialMeasureService.queryMeasureListGrade()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/api/material-measure/task/queryTaskRemarkInfo`
> **测量、复尺列表**

**返回类型**: `ResultDTO<MeasureRemarkVo>`

**调用链**:
  - `MaterialMeasureService.queryTaskRemarkInfo()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`


---

# MaterialMigrationV2Controller
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialMigrationV2Controller.java`

---

## GET `/migrationPurchaseOrder`

**返回类型**: `void`

**调用链**:
  - `ScmManager.search()`

---

## GET `/migrationPurchaseOrderOne`

**返回类型**: `void`

**调用链**:
  - `ScmManager.search()`

---

## GET `/migrationFinishedPurchaseOrder`

**返回类型**: `String`

**调用链**:
  - `ScmManager.search()`
  - `TaskDispatchDao.list()`
  - `RefreshDataService.syncFinishedTaskStatus()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/migrationAutoAcceptPurchaseOrder`

**返回类型**: `String`

**调用链**:
  - `ScmManager.search()`
  - `ScmSupplierManager.operatePurchaseOrder()`


---

# MaterialTemplateV2Controller
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialTemplateV2Controller.java`

---

## POST `/material-task/config/export-template-list`

**返回类型**: `void`

**调用链**:
  - `MaterialTemplateV2Service.exportTemplateList()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `NMaterialTemplateDao` → `n_material_template`
    - `NMaterialTemplateCopyLogDao` → `n_material_template_copy_log`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ProductComboManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
  - `MaterialTemplateV2Service.exportTemplateListForHome25Manpower()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `NMaterialTemplateDao` → `n_material_template`
    - `NMaterialTemplateCopyLogDao` → `n_material_template_copy_log`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ProductComboManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `n_material_template_copy_log` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_payment_intercept_config` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `MerchantManager`
- `ProductComboManager`
- `ScmMerchantManager`
- `ScmProductManager`


---

# MeasureConfigRuleExportController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/MeasureConfigRuleExportController.java`

---

## POST `/material-measure/interface/config/export-file`

**返回类型**: `void`

**调用链**:
  - `MeasureConfigRuleService.exportList()`
    - `MeasureConfigRuleDao` → `material_measure_interface_config`
    - `MeasureConfigRuleDao` → `material_measure_interface_config_category_rel`
    - `MeasureConfigRuleDao` → `material_measure_interface_config_mdm_rel`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_measure_interface_config_mdm_rel` | SELECT/UPDATE |
| `material_measure_interface_config_category_rel` | SELECT/UPDATE |
| `material_measure_interface_config` | SELECT/UPDATE |


---

# RefreshDataController
**基路径**: `/api/task-dispatch/refresh-create`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/RefreshDataController.java`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch/compensation/update-schedule`

**返回类型**: `String`

**调用链**:
  - `DispatchCreateService.updateTaskSchedule()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `MaterialTaskRouteManager`
- `PackageConstructionManager`
- `ProjectOrderManager`
- `ScmManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/active-time`

**返回类型**: `Integer`

**调用链**:
  - `DispatchCreateService.updateActivateTime()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `MaterialTaskRouteManager`
- `PackageConstructionManager`
- `ProjectOrderManager`
- `ScmManager`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch/compensation/push-enter-message`

**返回类型**: `String`

**调用链**:
  - `MessagePushClient.pushMaterialEnterMessage()`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-config-supplier-name`

**返回类型**: `String`

**调用链**:
  - `TaskTemplateService.updateTaskSupplierName()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `TaskTemplateManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `MaterialTaskRouteManager`
- `ProjectOrderManager`
- `ScmProductManager`
- `SupplierManager`
- `TaskTemplateManager`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-dispatch-supplier-name`

**返回类型**: `String`

**调用链**:
  - `TaskTemplateService.updateTaskDispatchSupplierName()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `TaskTemplateManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `MaterialTaskRouteManager`
- `ProjectOrderManager`
- `ScmProductManager`
- `SupplierManager`
- `TaskTemplateManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-estimated-time`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchCreateService.batchUpdateEstimatedTime()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `MaterialTaskDao` → `material_task`
    - `ConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `ProjectOrderManager`
- `ScmManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/material-task-dispatch-change`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchService.sendMaterialTaskDispatchChange()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/change-appoint-handler`

**返回类型**: `String`

**调用链**:
  - `PackageChangeAppointEventHandler.handleBiz()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/push-visit-door-time-message`

**返回类型**: `String`

**调用链**:
  - `MessagePushClient.pushVisitDoorTimeMessage()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/notice-enter-task`

**返回类型**: `Integer`

**调用链**:
  - `DispatchCreateService.createNoticeEnterTask()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `MaterialTaskRouteManager`
- `PackageConstructionManager`
- `ProjectOrderManager`
- `ScmManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/update-process-batch`

**返回类型**: `String`

**调用链**:
  - `MaterialTaskService.updateTaskProcessBatch()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskNodeTimeDao` → `material_task_node_time`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MaterialTaskNodeTimeRelationDao` → `material_task_node_time_relation`
    - `TaskDispatchDao` → `task_dispatch`
    - `MaterialTaskRouteManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task_node_time` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `material_task_node_time_relation` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `MaterialTaskRouteManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/refresh-notice-batch`

**返回类型**: `String`

**调用链**:
  - `DispatchCreateService.refreshNoticeTaskBatch()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `MaterialTaskRouteManager`
- `PackageConstructionManager`
- `ProjectOrderManager`
- `ScmManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/project-package-relation`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeService.projectPackageRelation()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `ProjectOrderManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/system-complete-notice-task`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeService.systemCompleteNoticeTask()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `ProjectOrderManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/oms-sync`

**返回类型**: `String`

**调用链**:
  - `SupplierMessageSyncService.compensateOmsSync()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `OmsMessageSyncDao` → `oms_message_sync`
    - `TaskDispatchDao` → `task_dispatch`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `oms_message_sync` | SELECT/UPDATE |

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/handle-assigner-uncompleted`

**返回类型**: `AssignerUncompletedDTO`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `success` | List<Long> | `` | `` |  |
| `failed` | List<Long> | `` | `` |  |

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/compensation/complete-assigner-all`

**返回类型**: `String`

**调用链**:
  - `InstallerTaskService.completeAssignerAll()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `RubanManager`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch-node/send-notice-retain-msg`

**返回类型**: `String`

**调用链**:
  - `MaterialTaskProducer.publishTaskDispatchTimeChange()`
  - `TaskDispatchNodeDao.listByIds()`

---

## GET `/api/task-dispatch/refresh-create/task-dispatch-node/send-notice-es-msg`

**返回类型**: `String`

**调用链**:
  - `MaterialTaskProducer.publishTaskDispatchTimeChange()`
  - `TaskDispatchNodeDao.listByIds()`

---

## POST `/api/task-dispatch/refresh-create/refresh-data/task-dispatch/complet-order`

**返回类型**: `String`

**调用链**:
  - `OrderStatusChangeEventHandler.handleBiz()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/measure-apply-msg`

**返回类型**: `String`

**调用链**:
  - `DesignerMessageEventHandler.handleBiz()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/minerva-reassign-msg`

**返回类型**: `String`

**调用链**:
  - `MinervaDispatchEventHandler.handleBiz()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/project-vss-msg`

**返回类型**: `String`

**调用链**:
  - `ProjectVssEventHandler.handleBiz()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/activateTaskDispatch`

**返回类型**: `String`

**调用链**:
  - `DispatchActivateService.activateTaskDispatch()`
    - `MaterialTaskDao` → `material_task`
    - `TaskDispatchDao` → `task_dispatch`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/refresh`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchDao.updateById()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/refreshNode`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeDao.updateById()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/refreshNodeByTaskId`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeDao.updateByExampleSelective()`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/activateTaskDispatchV2`

**返回类型**: `String`

**调用链**:
  - `MaterialActivateV2Service.activateTaskDispatch()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `DeliveryProcessCfgManager`
- `PaymentManager`
- `ProjectOrderManager`
- `WorkBenchReadManager`
- `WorkCenterManager`
- `WorkbenchManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/updateTaskDispatchNodeV2`

**返回类型**: `String`

**调用链**:
  - `MaterialCommonService.updateTaskDispatchNode()`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MessageSendJobDao` → `message_send_job`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HomePaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `message_send_job` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `FulfillmentOrderQueryManager`
- `HomePaymentManager`
- `ProjectOrderManager`
- `ScmSupplierManager`
- `WorkCenterManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/updateTaskDispatchNodeV3`

**返回类型**: `String`

**调用链**:
  - `MaterialCommonService.updateTaskDispatchNodeV2()`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MessageSendJobDao` → `message_send_job`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HomePaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `message_send_job` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `FulfillmentOrderQueryManager`
- `HomePaymentManager`
- `ProjectOrderManager`
- `ScmSupplierManager`
- `WorkCenterManager`

---

## GET `/api/task-dispatch/refresh-create/cache/reload`

**返回类型**: `String`

**调用链**:
  - `StarlordCacheLoader.afterPropertiesSet()`

---

## GET `/api/task-dispatch/refresh-create/recalculateEstimatedTime`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `EstimatedTimeV2Service.calculateEstimatedTime()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_holiday_cfg_detail` | SELECT/UPDATE |
| `n_holiday_cfg` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `PackageConstructionQueryManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`

---

## GET `/api/task-dispatch/refresh-create/recalculateParentEstimatedTime`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `EstimatedTimeV2Service.updateParentNodeEstimatedTime()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_holiday_cfg_detail` | SELECT/UPDATE |
| `n_holiday_cfg` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `PackageConstructionQueryManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`

---

## GET `/api/task-dispatch/refresh-create/refreshEstimatedTime`

**返回类型**: `String`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `TaskDispatchNodeDao.updatePlatformCheckTime()`
  - `TaskDispatchNodeDao.updateEstimatedTime()`
  - `TaskDispatchNodeDao.updatePromiseTime()`
  - `EstimatedTimeV2Service.calculateEstimatedTime()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `EstimatedTimeV2Service.updateDelayDays()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_holiday_cfg_detail` | SELECT/UPDATE |
| `n_holiday_cfg` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `PackageConstructionQueryManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`

---

## POST `/api/task-dispatch/refresh-create/task-dispatch/groovy`

**返回类型**: `ResultDTO`

**调用链**:
  - `TaskDispatchService.measureFormChange()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## POST `/api/task-dispatch/refresh-create/compensate/orderTask`

**返回类型**: `ResultDTO`

**调用链**:
  - `TaskDispatchService.measureFormChange()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## GET `/api/task-dispatch/refresh-create/test`

**返回类型**: `void`

---

## GET `/api/task-dispatch/refresh-create/audit`

**返回类型**: `ResultDTO`

**调用链**:
  - `MaterialConfigV2ServiceImpl.auditProcessStart()`

---

## GET `/api/task-dispatch/refresh-create/reject`

**返回类型**: `ResultDTO`

**调用链**:
  - `MaterialConfigV2ServiceImpl.abortTemplateAudit()`

---

## GET `/api/task-dispatch/refresh-create/updateTemplateAllState`

**返回类型**: `ResultDTO`

**调用链**:
  - `MaterialConfigV2ServiceImpl.updateTemplateAllState()`

---

## GET `/api/task-dispatch/refresh-create/sendSupplierMsg`

**返回类型**: `ResultDTO`

**调用链**:
  - `MaterialTaskProducer.syncSupplierMaterial()`
  - `TaskDispatchNodeDao.listTaskByCondition()`
  - `ConversionService.convert()`

---

## GET `/api/task-dispatch/refresh-create/restartCopyNode`

**返回类型**: `ResultDTO`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `TaskDispatchDao.getById()`
  - `MaterialRestartV2Service.createRestartNodesAndCalculateEstimatedTime()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**
  - `MaterialConfigV2ServiceImpl.queryAllMaterialForm()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## POST `/api/task-dispatch/refresh-create/event/{type}`

**返回类型**: `ResponseEntity<String>`

**调用链**:
  - `EventSubMapper.selectByExample()`
  - `EventPubMapper.selectByExample()`

---

## POST `/api/task-dispatch/refresh-create/event/redo/{type}`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `EventDrivenController.publish()`
  - `EventDrivenController.subscribe()`

---

## GET `/api/task-dispatch/refresh-create/sync/package/task/status`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.listByTaskId()`
  - `TaskDispatchCompleteService.syncPackageTaskStatus()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`

---

## GET `/api/task-dispatch/refresh-create/sync/task/finished/status`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `RefreshDataService.syncFinishedTaskStatus()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/sync/task/city-code`

**返回类型**: `ResultDTO`

**调用链**:
  - `RefreshDataService.addCityCode()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/task/sdm/diff`

**返回类型**: `ResultDTO<List<SdmTaskStatusDiffBO>>`

**调用链**:
  - `RefreshDataService.sdmTaskStatusDiff()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/task/sdm/diff/new`

**返回类型**: `ResultDTO<List<SdmTaskStatusDiffBO>>`

**调用链**:
  - `RefreshDataService.sdmTaskStatusDiffNew()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/task/sdm/diff/new/single`

**返回类型**: `ResultDTO<List<SdmTaskStatusDiffBO>>`

**调用链**:
  - `RefreshDataService.sdmTaskStatusDiffNewSingle()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/task/sdm/auto-take-order`

**返回类型**: `ResultDTO`

**调用链**:
  - `RefreshDataService.autoTakeOrder()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## POST `/api/task-dispatch/refresh-create/task/sync/sdm`

**返回类型**: `ResultDTO<List<String>>`

**调用链**:
  - `RefreshDataService.syncSdmTaskStatus()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## POST `/api/task-dispatch/refresh-create/task/sdm/update-estimated-time`

**返回类型**: `ResultDTO`

**调用链**:
  - `RefreshDataService.updateEstimatedTime()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/task/sdm/update-estimated-time-by-node`

**返回类型**: `ResultDTO`

**调用链**:
  - `RefreshDataService.updateEstimatedTimeByNode()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## POST `/api/task-dispatch/refresh-create/task/sdm/project-order-diff`

**返回类型**: `ResultDTO<List<SdmTaskStatusDiffBO>>`

**调用链**:
  - `RefreshDataService.sdmProjectStatusDiff()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## POST `/api/task-dispatch/refresh-create/task/sdm/project-order-need-restart-diff`

**返回类型**: `ResultDTO<List<SdmTaskStatusDiffBO>>`

**调用链**:
  - `RefreshDataService.projectOrderNeedRestartDiff()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/api/task-dispatch/refresh-create/refreshNodeExecutor`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `TaskDispatchNodeDao.updateById()`
  - `PackageConstructionQueryManager.queryByPackageCode()`

---

## GET `/api/task-dispatch/refresh-create/updateEstimatedTimeByTemplateCode`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.listByExample()`
  - `TaskDispatchNodeDao.updateById()`
  - `EstimatedTimeV2Service.calculateEstimatedTime()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_holiday_cfg_detail` | SELECT/UPDATE |
| `n_holiday_cfg` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `PackageConstructionQueryManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`

---

## GET `/api/task-dispatch/refresh-create/refresh/completed_task_dispatch_node_estimated_time`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `MaterialCommonService.updateCompletedTaskDispatchNodeEstimatedTime()`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MessageSendJobDao` → `message_send_job`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HomePaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `message_send_job` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `FulfillmentOrderQueryManager`
- `HomePaymentManager`
- `ProjectOrderManager`
- `ScmSupplierManager`
- `WorkCenterManager`

---

## POST `/api/task-dispatch/refresh-create/tools/batchRefreshTaskEstimateTime`
> **根据货单号更新任务节点考核时间**

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `MaterialTaskProducer.publishTaskDispatchTimeChange()`
  - `TaskDispatchNodeDao.listByExample()`
  - `TaskDispatchNodeDao.updateEstimatedTime()`
  - `TaskDispatchDao.list()`
  - `EstimatedTimeV2Service.calculateEstimatedTime()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NHolidayCfgDao` → `n_holiday_cfg`
    - `NHolidayCfgDao` → `n_holiday_cfg_detail`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_holiday_cfg_detail` | SELECT/UPDATE |
| `n_holiday_cfg` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `PackageConstructionQueryManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`

---

## GET `/api/task-dispatch/refresh-create/refresh/executorId`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `TaskDispatchNodeDao.getById()`
  - `TaskDispatchNodeDao.updateById()`
  - `TaskDispatchDao.getById()`
  - `ProjectOrderManager.getProjectOrder()`
  - `ExecutorV2Service.assignExecutor()`
    - `CeresManager` → **[外部服务]**
    - `CustomerHomeManager` → **[外部服务]**
    - `HomeModelManager` → **[外部服务]**
    - `MinervaManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `RetailManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
  - `MaterialConfigV2ServiceImpl.queryAllMaterialForm()`

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `CustomerHomeManager`
- `HomeModelManager`
- `MinervaManager`
- `ProjectOrderManager`
- `RetailManager`
- `RubanManager`

---

## GET `/api/task-dispatch/refresh-create/refresh/fuchikaicheng`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.listTaskByIds()`
  - `TaskDispatchDao.pageByCondition()`
  - `TaskDispatchDao.list()`
  - `ScmManager.cancelOrder()`
  - `TaskDispatchExtendDao.queryOne()`
  - `TaskDispatchExtendDao.insertSelective()`

---

## GET `/api/task-dispatch/refresh-create/refresh/fuchikaicheng2`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.listTaskByIds()`
  - `TaskDispatchDao.getById()`
  - `TaskDispatchDao.list()`
  - `ScmManager.cancelOrder()`
  - `TaskDispatchExtendDao.queryOne()`
  - `TaskDispatchExtendDao.insertSelective()`

---

## GET `/api/task-dispatch/refresh-create/recreate/task`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.updateByExampleSelective()`
  - `TaskDispatchDao.pageByCondition()`
  - `TaskDispatchDao.updateById()`
  - `MaterialConstructionTaskController.create()`

---

## GET `/api/task-dispatch/refresh-create/recreate/task2`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchNodeDao.updateByExampleSelective()`
  - `TaskDispatchDao.getById()`
  - `TaskDispatchDao.updateById()`
  - `MaterialConstructionTaskController.create()`

---

## GET `/api/task-dispatch/refresh-create/test/send/vss`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `TaskDispatchDao.getById()`
  - `OmsMessageSyncService.sendVssNew()`
    - `OmsMessageSyncDao` → `oms_message_sync`
    - `TaskDispatchNodeDelayReasonDao` → `task_dispatch_node_delay_reason`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `oms_message_sync` | SELECT/UPDATE |
| `task_dispatch_node_delay_reason` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`

---

## GET `/api/task-dispatch/refresh-create/refresh-data/fix-duplicate-nodes`
> **修复nodeTask重复节点**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `TaskDispatchNodeDao.listByTaskId()`
  - `TaskDispatchNodeDao.deleteById()`
  - `TaskDispatchNodeDao.updateById()`
  - `TaskDispatchDao.getById()`
  - `TaskDispatchDao.updateById()`
  - `MaterialHandleV2Service.handleNode()`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**
    - `RetailManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_template_unit` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `RetailManager`
- `ScmManager`
- `WorkCenterManager`

---

## GET `/api/task-dispatch/refresh-create/refresh-data/fix-duplicate-config-nodes`
> **修复排程配置node/task重复数据**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `NMaterialProcessDefineDao.queryByTemplateId()`
  - `NMaterialNodeDao.queryList()`
  - `NMaterialNodeDao.deleteByExample()`
  - `NMaterialTaskCfgDao.deleteByExample()`

---

## POST `/api/task-dispatch/refresh-create/refresh/routeTime/workday`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `NMaterialRouteTimeMapper.updateByExampleSelective()`

---

## GET `/api/task-dispatch/refresh-create/refresh-data/get-purchase-no-test`

**返回类型**: `String`

**调用链**:
  - `SeqService.fetchPurchaseOrderNo()`

---

## GET `/api/task-dispatch/refresh-create/refresh/updateDispatchTemplateByProcessCode`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `TaskTemplateService.listAll()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `MaterialTaskDao` → `material_task`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `MaterialTaskRouteManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `TaskTemplateManager` → **[外部服务]**
  - `TaskDispatchDao.list()`
  - `TaskDispatchDao.updateById()`
  - `DeliveryProcessCfgManager.queryDeliveryProcess()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `MaterialTaskRouteManager`
- `ProjectOrderManager`
- `ScmProductManager`
- `SupplierManager`
- `TaskTemplateManager`


---

# StarlordConfigController
**Swagger 标签**: `主材任务切换2.5配置查询接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/StarlordConfigController.java`

---

## POST `/starlord/config/cityInstallWorkerRule`
> **是否配置测量复尺或者安装规则**

**返回类型**: `ConfigOutputListDTO`

**调用链**:
  - `NMaterialTemplateUnitDao.queryList()`


---

# StarlordInspectionController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/StarlordInspectionController.java`

---

## POST `/starlord/refreshAll`

**返回类型**: `ResultDTO<Boolean>`

---

## GET `/starlord/refreshByTaskType`

**返回类型**: `ResultDTO<Boolean>`

---

## POST `/starlord/refreshOrder`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `RefreshOrderService.refreshOrder()`

---

## POST `/starlord/inspection`

**返回类型**: `ResultDTO<List<StarlordInspectionResultDTO>>`

**调用链**:
  - `StarlordInspectionService.buildSelectCategory()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskSelectCategoryDao` → `task_select_category`
  - `StarlordInspectionService.measureInspection()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskSelectCategoryDao` → `task_select_category`
  - `StarlordInspectionService.orderInspection()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskSelectCategoryDao` → `task_select_category`
  - `ProjectOrderManager.getProjectOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_select_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `taskType` | Integer | `` | `` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `createState` | Boolean | `task_dispatch` | `state` |  |
| `selectExist` | Boolean | `` | `` |  |
| `configExist` | Boolean | `` | `` |  |
| `esTimeOK` | Boolean | `` | `` |  |
| `needComplete` | Boolean | `` | `` |  |
| `endTimeOK` | Boolean | `` | `` |  |
| `readyState` | Boolean | `task_dispatch` | `state` |  |
| `delayState` | Boolean | `task_dispatch` | `state` |  |


---

# TaskDispatchV2Controller
**基路径**: `/material-task/task-project/init`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/TaskDispatchV2Controller.java`

---

## POST `/material-task/task-project/init/material-task/task-related-es-info`

**返回类型**: `ResultDTO<List<TaskOrderEsSyncDTO>>`

**调用链**:
  - `TaskDispatchEsContentService.listTaskEsInfoDTO()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `CeresManager` → **[外部服务]**
    - `ConstructionManager` → **[外部服务]**
    - `CustomerSourceManager` → **[外部服务]**
    - `EdenRubanManager` → **[外部服务]**
    - `ElasticSearchManager` → **[外部服务]**
    - `OrganizationManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `RetailManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `WorkOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `coordinator_task_order` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `info` | TaskEsInfoDTO | `coordinator_task_order` | `sku_info` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ConstructionManager`
- `CustomerSourceManager`
- `EdenRubanManager`
- `ElasticSearchManager`
- `OrganizationManager`
- `PackageConstructionManager`
- `ProjectOrderManager`
- `RetailManager`
- `ScmManager`
- `WorkOrderManager`

---

## POST `/material-task/task-project/init/material-task/task-project/init`

**返回类型**: `ResultDTO<MaterialProcessNodeDTO>`

**调用链**:
  - `MaterialProcessV2Service.processNodeLinkUrl()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `linkUrl` | String | `` | `` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `taskDispatchNodeId` | Long | `task_dispatch` | `id` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `PackageConstructionManager`
- `WorkBenchReadManager`
- `WorkCenterManager`

---

## POST `/material-task/task-project/init/retail-task/task-project/init`

**返回类型**: `ResultDTO<MaterialProcessNodeDTO>`

**调用链**:
  - `MaterialProcessV2Service.processNodeLinkUrl()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `linkUrl` | String | `` | `` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `taskDispatchNodeId` | Long | `task_dispatch` | `id` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `PackageConstructionManager`
- `WorkBenchReadManager`
- `WorkCenterManager`

---

## POST `/material-task/task-project/init/material-task/task-order/init`

**返回类型**: `ResultDTO<MaterialProcessNodeDTO>`

**调用链**:
  - `MaterialProcessV2Service.processNodeLinkUrl()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `linkUrl` | String | `` | `` |  |
| `taskType` | Integer | `task_dispatch` | `task_type` |  |
| `nodeType` | Integer | `task_dispatch` | `node_type` |  |
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `taskDispatchNodeId` | Long | `task_dispatch` | `id` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `PackageConstructionManager`
- `WorkBenchReadManager`
- `WorkCenterManager`


---

# TaskMaterialRulerController
**Swagger 标签**: `主材留尺任务相关接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/TaskMaterialRulerController.java`

---

## GET `/list-completed-material-ruler`
> **获得已完成主材留尺任务列表**

**返回类型**: `ResultVO<List<CompletedMaterialRulerVO>>`

---

## GET `/get-tech-disclosure-material-ruler`
> **获得已完成主材留尺任务列表**

**返回类型**: `ResultVO<List<MaterialRulerVO>>`

---

## GET `/list-material-ruler-info`
> **获得已完成主材留尺任务列表**

**返回类型**: `ResultVO<List<MaterialRulerVO>>`


---

# TestController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/TestController.java`

---

## POST `/test/testVssMsg`

**返回类型**: `Boolean`

**调用链**:
  - `ProjectVssEventHandler.handleBiz()`

---

## POST `/test/testScmMsg`

**返回类型**: `Boolean`

**调用链**:
  - `ScmOrderEventHandler.handleBiz()`

---

## POST `/test/testScmMeasure`

**返回类型**: `Boolean`

**调用链**:
  - `ScmMeasureApplyEventHandler.handleBiz()`

---

## POST `/test/testRetry`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialTaskSchedule.nMaterialRetryDelayQueue()`

---

## POST `/test/userCreate`

**返回类型**: `Boolean`

**调用链**:
  - `CRMUserEventCreateHandler.handleBiz()`

---

## GET `/test/refreshTask2_0`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshDataService.refreshOrder()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**
  - `RefreshDataQuteService.refreshOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/test/refreshTask2_0/withMdmCode`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshDataService.queryAllOrder()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**
  - `RefreshDataService.refreshOrder()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**
  - `RefreshDataQuteService.refreshOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/test/refreshTask2_0/withFile`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshDataService.refreshOrder()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `AthenaManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `ProductReadManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `UploadManager` → **[外部服务]**
  - `RefreshDataQuteService.refreshOrder()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AthenaManager`
- `MerchantManager`
- `PackageConstructionManager`
- `ProductReadManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmSupplierManager`
- `UploadManager`

---

## GET `/test/retryById`

**返回类型**: `Boolean`

**调用链**:
  - `RetryDelayQueueService.queryRetryDelayById()`
    - `RetryDelayQueueDao` → `retry_delay_queue`
  - `RetryDelayQueueService.processRetry()`
    - `RetryDelayQueueDao` → `retry_delay_queue`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `retry_delay_queue` | SELECT/UPDATE |

---

## GET `/test/checkMaterialDeliveryBatch`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialBatchV2Service.checkMaterialDeliveryBatch()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `TaskDispatchDao` → `task_dispatch`
    - `OfcManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcManager`
- `PaymentManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`
- `WorkbenchManager`

---

## POST `/test/refreshInstall`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshInstallDataService.refreshInstall()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ScmManager`

---

## GET `/test/message`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialTaskSchedule.materialMessageDelayQueue()`

---

## GET `/test/delayPush`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialTaskSchedule.dailyPush()`

---

## GET `/test/clean`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialTaskSchedule.clean()`

---

## POST `/test/refreshProcessCode`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshProcessCodeService.refreshProcessCode()`

---

## POST `/test/refreshEstime`

**返回类型**: `Boolean`

**调用链**:
  - `RefreshEsTimeService.refreshEsTime()`

---

## GET `/test/calculatePlanActiveTime`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialCommonService.calculatePlanActiveTime()`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MessageSendJobDao` → `message_send_job`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HomePaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**
  - `TaskDispatchDao.getById()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `message_send_job` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `FulfillmentOrderQueryManager`
- `HomePaymentManager`
- `ProjectOrderManager`
- `ScmSupplierManager`
- `WorkCenterManager`

---

## GET `/test/updatePlanActiveTime`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialCommonService.calculatePlanActiveTime()`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MessageSendJobDao` → `message_send_job`
    - `TaskDispatchDao` → `task_dispatch`
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HomePaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**
  - `TaskDispatchDao.getById()`
  - `TaskDispatchDao.updateById()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `message_send_job` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`
- `FulfillmentOrderQueryManager`
- `HomePaymentManager`
- `ProjectOrderManager`
- `ScmSupplierManager`
- `WorkCenterManager`

---

## GET `/test/activatePreCondition`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialActivateV2Service.getScheduleTaskActivatePreCondition()`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialRouteDao` → `n_material_route`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AcceptanceReportManager` → **[外部服务]**
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `PaymentManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**
    - `WorkCenterManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**
  - `TaskDispatchDao.getById()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_payment_intercept_config` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AcceptanceReportManager`
- `DeliveryProcessCfgManager`
- `PaymentManager`
- `ProjectOrderManager`
- `WorkBenchReadManager`
- `WorkCenterManager`
- `WorkbenchManager`

---

## POST `/test/insertRetryDelayQueue`

**返回类型**: `Boolean`

**调用链**:
  - `RetryDelayQueueDao.insert()`

---

## POST `/test/insertMaterialFlowRuleTemplateMapping`

**返回类型**: `Boolean`

**调用链**:
  - `MaterialFlowRuleTemplateMappingDao.insertMaterialFlowRuleTemplateMapping()`

---

## POST `/test/deleteSpecifiedTable`

**返回类型**: `Boolean`

**调用链**:
  - `DynamicOperateService.deleteSpecifiedTable()`

---

## GET `/tools/queryTaskCreateFailReasons`
> **查询任务创建失败原因**

**返回类型**: `List<TaskCreateFailReason>`

**调用链**:
  - `MaterialCreateV2Service.queryTaskCreateFailReasons()`
    - `TaskCreateFailReasonDao` → `task_create_fail_reason`
    - `ProjectInfoDao` → `project_info`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `RetailManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_create_fail_reason` | SELECT/UPDATE |
| `project_info` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ConstructionManager`
- `DeliveryProcessCfgManager`
- `OfcManager`
- `ProjectOrderManager`
- `RetailManager`
- `ScmManager`

---

## GET `/tools/queryMeasureReason`
> **查询任务创建失败原因**

**返回类型**: `String`

**调用链**:
  - `MaterialMeasureService.queryTaskCreateFailReasons()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `MaterialSelectionManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `ScmManager`
- `ScmMerchantManager`

---

## GET `/tools/delCategoryMapping`
> **查询任务创建失败原因**

**返回类型**: `String`

**调用链**:
  - `MaterialFlowRuleTemplateMappingDao.queryAllMappingListByCategoryId()`
  - `MaterialFlowRuleTemplateMappingDao.deleteByIdList()`
  - `MaterialFlowRuleCategoryDao.queryRuleCategoryDataListByCondition()`

---

## GET `/tools/reBuildCategoryMapping`
> **根据规则id重新构建规则的安装映射关系**

**返回类型**: `String`

**调用链**:
  - `MaterialFlowRuleTemplateMappingDao.queryAllMappingListByCategoryId()`
  - `MaterialFlowProcessAndTemplateService.processTabInfo()`
  - `MaterialFlowRuleCategoryDao.queryRuleCategoryDataListByCondition()`

---

## GET `/tools/getAllCategoryCodeAndTemplateMappingError`
> **查询规则和安装配置维度错误的数据**

**返回类型**: `Map<String, String>`

**调用链**:
  - `MaterialFlowMixTemplateService.getAllCategoryCodeAndTemplateMappingError()`
    - `MaterialFlowRuleTemplateRecordDao` → `material_flow_rule_template_record`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `NMaterialTemplateDao` → `n_material_template`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_record` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/tools/publishMaterialCustomer`
> **查询规则和安装配置维度错误的数据**

**返回类型**: `String`

**调用链**:
  - `TaskDispatchDao.listByIds()`
  - `MaterialCustomerProducer.publishMaterialCustomer()`

---

## GET `/api/test/construction/withoutVirtual`
> **查询规则和安装配置维度错误的数据**

**返回类型**: `ResultDTO<List<ConstructionScheduleDTO>>`

**调用链**:
  - `ConstructionScheduleFeignService.queryConstructionSchedulesV2()`

---

## GET `/api/test/construction/withVirtual`
> **查询规则和安装配置维度错误的数据**

**返回类型**: `ResultDTO<List<ConstructionScheduleDTO>>`

**调用链**:
  - `ConstructionManager.queryConstructionSchedulesV2()`


---

# ToolsController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/ToolsController.java`

---

## POST `/tools/updateTaskById`
> **根据主键更新任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `TaskDispatchMapper.updateByPrimaryKeySelective()`

---

## POST `/tools/updateTaskNodeById`
> **根据主键更新任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `TaskDispatchNodeMapper.updateByPrimaryKeySelective()`

---

## POST `/tools/updateCfgTaskById`
> **根据主键更新任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTaskCfgDao.updateNMaterialTaskCfgById()`

---

## POST `/tools/updateCfgNodeById`
> **根据主键更新任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialNodeDao.updateNMaterialNodeCfgById()`

---

## POST `/tools/updateCfgProcessById`
> **根据主键更新任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialProcessDefineDao.updateById()`

---

## POST `/tools/updateCfgTemplateById`
> **根据主键更新任务节点**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTemplateDao.updateById()`

---

## POST `/tools/updateCfgTemplateUnitById`
> **根据主键更新配置任务**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTemplateUnitDao.updateSelectiveById()`

---

## POST `/tools/updateNMaterialRouteById`
> **根据主键更新配置节点**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialRouteMapper.updateByPrimaryKeySelective()`

---

## GET `/tools/delTemplateDatatById`
> **根据主键更新配置流程**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTemplateDao.queryListByTemplateCode()`
  - `NMaterialTemplateDao.deleteById()`
  - `NMaterialTemplateUnitDao.deleteByTemplateId()`
  - `NMaterialProcessDefineDao.queryByTemplateId()`
  - `NMaterialProcessDefineDao.deleteByPrimaryKey()`
  - `NMaterialNodeDao.deleteByExample()`
  - `NMaterialTaskCfgDao.deleteByExample()`
  - `NMaterialTimeDao.deleteByExample()`
  - `NMaterialTimeRelationDao.deleteByExample()`
  - `NMaterialRouteDao.deleteByExample()`
  - `NMaterialEdgeDao.deleteByExample()`
  - `NMaterialPushDao.deleteByExample()`

---

## GET `/tools/delTemplateDatatByTemplateId`
> **删除配置指定数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTemplateDao.queryById()`
  - `NMaterialTemplateDao.deleteById()`
  - `NMaterialTemplateUnitDao.deleteByTemplateId()`
  - `NMaterialProcessDefineDao.queryByTemplateId()`
  - `NMaterialProcessDefineDao.deleteByPrimaryKey()`
  - `NMaterialNodeDao.deleteByExample()`
  - `NMaterialTaskCfgDao.deleteByExample()`
  - `NMaterialTimeDao.deleteByExample()`
  - `NMaterialTimeRelationDao.deleteByExample()`
  - `NMaterialRouteDao.deleteByExample()`
  - `NMaterialEdgeDao.deleteByExample()`
  - `NMaterialPushDao.deleteByExample()`

---

## GET `/tools/activityTemplateDatatById`
> **生效指定配置数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialTemplateDao.queryListByTemplateCode()`
  - `NMaterialTemplateDao.updateById()`
  - `NMaterialTemplateUnitDao.updateByExample()`
  - `NMaterialProcessDefineDao.queryByTemplateId()`
  - `NMaterialProcessDefineDao.updateByExample()`
  - `NMaterialNodeDao.updateByExample()`
  - `NMaterialTaskCfgDao.updateByExampleSelective()`
  - `NMaterialTimeDao.updateByExampleSelective()`
  - `NMaterialTimeRelationDao.updateByExampleSelective()`
  - `NMaterialRouteDao.updateByExampleSelective()`
  - `NMaterialEdgeDao.updateByExampleSelective()`
  - `NMaterialPushDao.updateByExampleSelective()`

---

## GET `/tools/upodateMeasureRemarkFilter`
> **复尺驳回数据刷数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `TaskDispatchService.upodateMeasureRemarkFilter()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## GET `/tools/updatePurchaseOrder`
> **复尺驳回数据刷数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `TaskDispatchV2Service.updatePurchaseOrderNo()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `TaskBatchNodeRelationDao` → `task_batch_node_relation`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskProcessBatchDao` → `task_process_batch`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `TaskDispatchDao` → `task_dispatch`
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `task_batch_node_relation` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_process_batch` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ScmManager`

---

## GET `/tools/deleteCfgTemplateById`
> **复尺驳回数据刷数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `TaskDispatchService.upodateMeasureRemarkFilter()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## GET `/tools/calculateTimeProcess`
> **复尺驳回数据刷数据**

**返回类型**: `ResultDTO<FcnCalculateTimeDTO>`

**调用链**:
  - `ToolsService.calculateTimeProcess()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `projectOrderId` | Long | `task_dispatch` | `project_order_id` |  |
| `taskDispatchNodeId` | Long | `task_dispatch` | `id` |  |
| `taskDispatchId` | Long | `task_dispatch` | `task_dispatch_id` |  |
| `modeName` | String | `` | `` |  |
| `materialName` | String | `task_dispatch` | `material_name` |  |
| `supplierName` | String | `task_dispatch` | `supplier_name` |  |
| `processCode` | String | `task_dispatch` | `process_code` |  |
| `newProcessCode` | String | `task_dispatch` | `process_code` |  |
| `taskTypeName` | String | `` | `` |  |
| `nodeTypeName` | String | `` | `` |  |
| `oldEstimatedTime` | String | `task_dispatch` | `estimated_time` |  |
| `estimatedTime` | String | `task_dispatch` | `estimated_time` |  |
| `endTime` | String | `task_dispatch` | `end_time` |  |
| `state` | String | `task_dispatch` | `state` |  |
| `processStatus` | String | `task_dispatch` | `process_status` |  |
| `restartCount` | Integer | `` | `` |  |
| `timeRuleDesc` | String | `` | `` |  |
| `timeCfgList` | List<FcnCalculateTimeCfgDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`

---

## GET `/tools/delProcessByProcessCode`
> **复尺驳回数据刷数据**

**返回类型**: `ResultDTO<Integer>`

**调用链**:
  - `NMaterialProcessDefineDao.queryList()`
  - `NMaterialRouteMapper.deleteByExample()`
  - `NMaterialNodeCfgMapper.deleteByExample()`
  - `NMaterialTaskCfgMapper.deleteByExample()`
  - `NMaterialTimeCfgMapper.selectByExample()`
  - `NMaterialTimeCfgMapper.deleteByExample()`
  - `NMaterialTimeRelationMapper.deleteByExample()`
  - `NMaterialEdgeMapper.deleteByExample()`
  - `NMaterialProcessDefineMapper.deleteByExample()`

---

## POST `/tools/delProcessByProcessCodeList`
> **批量删除指定processCode+state的重复process数据**

**返回类型**: `ResultDTO<String>`


---

# WelcomeController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/WelcomeController.java`

---

## GET `/`

**返回类型**: `String`


---

# XcsController
**基路径**: `/api/v1/xcs`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/XcsController.java`

---

## POST `/api/v1/xcs/delay-reason-recovery`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `DelayReasonRecoveryRecordService.analyzeAndSaveDelayReasonRecoveries()`
    - `DelayReasonRecoveryRecordDao` → `delay_reason_recovery_record`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delay_reason_recovery_record` | SELECT/UPDATE |


---

# StarlordController
**Swagger 标签**: `主材任务相关的接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/butler/StarlordController.java`

---

## POST `/dispatch/list-task-node`

**返回类型**: `ResultVO<Page<MaterialTaskItemVO>>`

**调用链**:
  - `StarlordService.listMaterialTask()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `taskDispatchNodeId` | Long | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `processStatus` | Byte | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `designerId` | Long | `` | `` |  |
| `homeownerId` | Long | `` | `` |  |
| `id` | Long | `` | `` |  |
| `homeownerName` | String | `` | `` |  |
| `address` | String | `` | `` |  |
| `foremanId` | Long | `` | `` |  |
| `foremanName` | String | `` | `` |  |
| `foremanPhone` | String | `` | `` |  |
| `scheduleExpected` | LocalDateTime | `` | `` |  |
| `projectOrderId` | String | `` | `` |  |
| `homeownerSex` | Integer | `` | `` |  |
| `ScheduleExpectedType` | ScheduleExpectedTypeEnum | `` | `` |  |
| `modifyReason` | String | `` | `` |  |
| `addressCompleted` | Boolean | `` | `` |  |
| `remark` | String | `` | `` |  |
| `designerName` | String | `` | `` |  |
| `designerPhone` | String | `` | `` |  |
| `brandName` | String | `` | `` |  |
| `packageName` | String | `` | `` |  |
| `brand` | BrandEnum | `` | `` |  |
| `butlerName` | String | `` | `` |  |
| `homeownerPhone` | String | `` | `` |  |
| `consultantName` | String | `` | `` |  |
| `customerManagerName` | String | `` | `` |  |
| `customerManagerPhone` | String | `` | `` |  |
| `decoratePaymentOrderId` | String | `` | `` |  |
| `depositStatus` | DepositStatusEnum | `` | `` |  |
| `status` | Integer | `` | `` |  |
| `measurePicturesSwitch` | boolean | `` | `` |  |
| `preDisclosurePicturesSwitch` | boolean | `` | `` |  |
| `measurePicturesUpload` | Integer | `` | `` |  |
| `preDisclosurePicturesStatus` | BaseFileAuditState | `` | `` |  |
| `showSignBtn` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## GET `/dispatch/material-delivery/recent-tasks`
> **近3天的预约送货任务列表**

**返回类型**: `ResultVO<List<MaterialTaskCommonVO>>`

**调用链**:
  - `StarlordService.recentTasks()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `taskBatchId` | Long | `` | `` |  |
| `projectOrderId` | String | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialNameList` | List<String> | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `taskName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `nodeName` | String | `` | `` |  |
| `processBatch` | Integer | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `noticeStartTime` | Date | `` | `` |  |
| `address` | String | `` | `` |  |
| `homeownerId` | Long | `` | `` |  |
| `homeownerName` | String | `` | `` |  |
| `homeownerPhone` | String | `` | `` |  |
| `brandName` | String | `` | `` |  |
| `brandId` | Integer | `` | `` |  |
| `foremanId` | Long | `` | `` |  |
| `foremanName` | String | `` | `` |  |
| `foremanPhone` | String | `` | `` |  |
| `designerId` | Long | `` | `` |  |
| `designerName` | String | `` | `` |  |
| `designerPhone` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## POST `/page/list-material-task`
> **近3天的预约送货任务列表**

**返回类型**: `ResultVO<Page<ProjectMaterialTaskVO>>`

**调用链**:
  - `StarlordService.pageListTask()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `projectOrderId` | Long | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `taskTypeName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `alterRequired` | Integer | `` | `` |  |
| `projectInfo` | ProjectInfoVO | `` | `` |  |
| `restart` | Boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## GET `/measure/query-appointment-info`
> **近3天的预约送货任务列表**

**返回类型**: `ResultVO<MeasureTaskAppointmentVO>`

**调用链**:
  - `StarlordService.queryMeasureAppointmentInfo()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `projectOrderId` | String | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `remark` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## POST `/measure/batch-submit`
> **近3天的预约送货任务列表**

**返回类型**: `ResultVO<Integer>`

**调用链**:
  - `StarlordService.submitMeasureAppointment()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## POST `/measure/batch-change-appoint`
> **通用分页查询主材任务列表**

**返回类型**: `ResultVO<Integer>`

**调用链**:
  - `StarlordService.measureChangeAppoint()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## POST `/design-review/submit`
> **通用分页查询主材任务列表**

**返回类型**: `ResultVO<Integer>`

**调用链**:
  - `StarlordService.submitDesignReview()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`

---

## GET `/design-review/enable-ignore-alter`
> **查询测量任务通知节点预约信息**

**返回类型**: `ResultVO<Boolean>`

**调用链**:
  - `StarlordService.enableIgnoreAlter()`
    - `BimManager` → **[外部服务]**
    - `OrderManager` → **[外部服务]**
    - `TaskDispatchFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimManager`
- `OrderManager`
- `TaskDispatchFeign`


---

# ReplenishCoordinatorTaskController
**基路径**: `/api/trace/delivery-info/query`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/coordinator/ReplenishCoordinatorTaskController.java`

---

## GET `/api/trace/delivery-info/query/dispatch/re-procurement/check/brand-sub-order`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `CoordinatorTaskService.checkReplenishOrder()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/re-procurement/place-order`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `CoordinatorTaskService.placeOrder()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/re-procurement/plan-pickup-time`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `CoordinatorTaskService.setPlanPickupTime()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## GET `/api/trace/delivery-info/query/dispatch/re-procurement/checkQueryCoordinatorTask`

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `CoordinatorTaskService.checkQueryCoordinatorTask()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/re-procurement/fulfillment/control/drawingr`

**返回类型**: `ResultDTO<NeedDrawingConfigDto>`

**调用链**:
  - `CoordinatorTaskService.queryNeedDrawingConfig()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## GET `/api/trace/delivery-info/query/re-procurement/query-brand-order`

**返回类型**: `ResultDTO<List<ReplenishBrandOrderInfoDto>>`

**调用链**:
  - `SubOrderManager.queryBrandOrderInfoForOrder()`

---

## POST `/api/trace/delivery-info/query/api/order/pre-allocate/list`

**返回类型**: `ResultDTO<Page<PreDeliveryCubeAllocateOrderVo>>`

**调用链**:
  - `OfcDispatchConfigQueryService.deliveryPreAllocateOrderList()`

---

## GET `/api/trace/delivery-info/query/api/order/replenish/customized/thirdSelectList/brandOrderNo`

**返回类型**: `ResultDTO<List<CustomizedThirdReplenishOrderVo>>`

**调用链**:
  - `OfcDispatchConfigQueryService.queryCustomizedThirdReplenishListByBrandOrderNo()`

---

## POST `/api/trace/delivery-info/query/dispatch/trace/task/batchSubmit`

**返回类型**: `ResultDTO<Long>`

**调用链**:
  - `CoordinatorTaskService.replenishOrderAssignmentToCoordinator()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/trace/task/test`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `CoordinatorTaskService.coordinatorTaskFollowUpRefresh()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## GET `/api/trace/delivery-info/query/dispatch/trace/follow-up-refresh`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `CoordinatorTaskService.coordinatorTaskFollowUpRefresh()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/replenish-order/list-order-wrapper`

**返回类型**: `ResultDTO<ReplenishOrderWrapperDto>`

**调用链**:
  - `CoordinatorTaskService.listReplenishWrapper()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `replenishToBeOrderedTaskCount` | Long | `` | `` |  |
| `replenishOrderedTaskCount` | Long | `` | `` |  |
| `canBatchPlaceOrder` | Integer | `` | `` |  |
| `replenishOrderDetailDtoList` | List<ReplenishOrderDetailDto> | `` | `` |  |
| `workOrderQueryResultDtoList` | List<WorkOrderQueryResultDto> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## GET `/api/trace/delivery-info/query/dispatch/trace/modify-order-status`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `CoordinatorTaskService.modifyOrderStatus()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## POST `/api/trace/delivery-info/query/dispatch/trace/backdoor/modify`

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `CoordinatorTaskService.modifyData()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`

---

## GET `/api/trace/delivery-info/query/api/trace/delivery-info/query`

**返回类型**: `ResultDTO<Page<DeliveryInfoQueryResultDto>>`

**调用链**:
  - `CoordinatorTaskService.getRelateOrderNos()`
    - `CoordinatorTaskOrderDao` → `coordinator_task_order`
    - `CoordinatorTaskProgressDao` → `coordinator_task_progress`
    - `CustomerHomeManager` → **[外部服务]**
    - `CustomerTakeManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `IssueOrderManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmWorkbenchManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
  - `ScmMerchantManager.queryDeliveryInfoForRetail()`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `coordinator_task_order` | SELECT/UPDATE |
| `coordinator_task_progress` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CustomerHomeManager`
- `CustomerTakeManager`
- `FulfillmentOrderQueryManager`
- `IssueOrderManager`
- `OfcManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmWorkbenchManager`
- `SubOrderManager`


---

# CategoryProcessController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/CategoryProcessController.java`

---

## POST `/category/process/route/save`
> **保存品类流程完整连线信息**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `CategoryProcessRouteService.savaCategoryProcessRouteInfo()`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

---

## POST `/category/process/default/route/info`
> **保存品类流程完整连线信息**

**返回类型**: `ResultDTO<List<DefaultRouteInfoDTO>>`

**调用链**:
  - `CategoryProcessRouteService.getDefaultCategoryProcessRouteInfo()`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `materialRouteCfgDtoList` | List<MaterialRouteCfgDto> | `` | `` |  |
| `deliveryType` | Integer | `n_material_task_process_template` | `delivery_type` |  |
| `coordinateData` | String | `n_material_task_process_template` | `coordinate_data` |  |

---

## GET `/category/process/define/list`
> **保存品类流程完整连线信息**

**返回类型**: `ResultDTO<CategoryProcessInfoDto>`

**调用链**:
  - `CategoryProcessService.getProcessDefineInfoAndRouteInfoList()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialPeriodCalculateHitHouseCfgDao` → `n_material_period_calculate_hit_house_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialPeriodCalculateHitCraftCfgDao` → `n_material_period_calculate_hit_craft_cfg`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_period_calculate_hit_house_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_period_calculate_hit_craft_cfg` | SELECT/UPDATE |
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

---

## GET `/category/process/define/list/tasktype`
> **保存品类流程完整连线信息**

**返回类型**: `ResultDTO<CategoryProcessInfoDto>`

**调用链**:
  - `CategoryProcessService.processDefineInfoListByTaskType()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialPeriodCalculateHitHouseCfgDao` → `n_material_period_calculate_hit_house_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialPeriodCalculateHitCraftCfgDao` → `n_material_period_calculate_hit_craft_cfg`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_period_calculate_hit_house_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_period_calculate_hit_craft_cfg` | SELECT/UPDATE |
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

---

## GET `/category/process/edge/list`
> **保存品类流程完整连线信息**

**返回类型**: `ResultDTO<CategoryProcessInfoDto>`

**调用链**:
  - `CategoryProcessService.processEdgeInfoList()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialPeriodCalculateHitHouseCfgDao` → `n_material_period_calculate_hit_house_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialPeriodCalculateHitCraftCfgDao` → `n_material_period_calculate_hit_craft_cfg`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_period_calculate_hit_house_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_period_calculate_hit_craft_cfg` | SELECT/UPDATE |
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

---

## POST `/category/process/define/save`
> **查询默认完整连线信息**

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `CategoryProcessService.saveMaterialProcessDefineNew()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTimeRelationDao` → `n_material_time_relation`
    - `NMaterialPeriodCalculateHitHouseCfgDao` → `n_material_period_calculate_hit_house_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialPeriodCalculateHitCraftCfgDao` → `n_material_period_calculate_hit_craft_cfg`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_time_relation` | SELECT/UPDATE |
| `n_material_period_calculate_hit_house_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_period_calculate_hit_craft_cfg` | SELECT/UPDATE |
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

---

## POST `/category/process/edge/check`
> **任务信息列表**

**返回类型**: `ResultDTO<List<CategoryProcessRouteCheckResultDTO>>`

**调用链**:
  - `CategoryProcessRouteService.verify()`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_process_template` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `result` | Boolean | `` | `` |  |
| `errType` | Integer | `n_material_route` | `type` |  |
| `errMsg` | String | `` | `` |  |
| `errMaterialRouteCfgDtoList` | List<MaterialRouteCfgDto> | `` | `` |  |
| `errNode` | List<CategoryProcessInfoCheckParam.DeliveryFlowTaskNodeParam> | `` | `` |  |


---

# DeliveryFlowCategoryController
**Swagger 标签**: `材料流程品类规则配置`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowCategoryController.java`

---

## GET `/delivery/flow/category/query/list/bycondition`
> **品类规则查询列表**

**返回类型**: `ResultDTO<DeliveryFlowCategoryPageInfoDTO>`

**调用链**:
  - `DeliveryFlowCategoryService.queryCategoryListByCondition()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `total` | long | `` | `` |  |
| `categoryList` | List<DeliveryFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/query/info`
> **品类规则查询列表**

**返回类型**: `ResultDTO<DeliveryFlowCategoryInfoDTO>`

**调用链**:
  - `DeliveryFlowCategoryService.queryCategoryInfo()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `n_material_task_cfg` | `id` |  |
| `categoryId` | String | `n_material_task_cfg` | `category_id` |  |
| `categoryCode` | String | `delivery_flow_rule` | `category_code` |  |
| `ruleId` | Long | `delivery_flow_rule_category` | `rule_id` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `materialCode` | String | `delivery_flow_rule` | `material_code` |  |
| `materialName` | String | `delivery_flow_rule` | `material_name` |  |
| `supplierList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `delivery_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `delivery_flow_rule_category` | `node_process_name` |  |
| `taskTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `allowDeliveryProcessUse` | Boolean | `` | `` |  |
| `deliveryProcessTemplateId` | Long | `n_material_node_transfer_condition` | `delivery_process_template_id` |  |
| `isBeijing` | Boolean | `` | `` |  |
| `gbCode` | String | `` | `` |  |
| `createBy` | Long | `n_material_task_cfg` | `create_by` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `createTime` | Date | `n_material_task_cfg` | `create_time` |  |
| `modifyBy` | Long | `n_material_task_cfg` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `n_material_task_cfg` | `state` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/delete`
> **品类规则查询列表**

**返回类型**: `ResultDTO<DeliveryFlowCategoryInfoDTO>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryDelete()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `n_material_task_cfg` | `id` |  |
| `categoryId` | String | `n_material_task_cfg` | `category_id` |  |
| `categoryCode` | String | `delivery_flow_rule` | `category_code` |  |
| `ruleId` | Long | `delivery_flow_rule_category` | `rule_id` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `materialCode` | String | `delivery_flow_rule` | `material_code` |  |
| `materialName` | String | `delivery_flow_rule` | `material_name` |  |
| `supplierList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `delivery_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `delivery_flow_rule_category` | `node_process_name` |  |
| `taskTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `allowDeliveryProcessUse` | Boolean | `` | `` |  |
| `deliveryProcessTemplateId` | Long | `n_material_node_transfer_condition` | `delivery_process_template_id` |  |
| `isBeijing` | Boolean | `` | `` |  |
| `gbCode` | String | `` | `` |  |
| `createBy` | Long | `n_material_task_cfg` | `create_by` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `createTime` | Date | `n_material_task_cfg` | `create_time` |  |
| `modifyBy` | Long | `n_material_task_cfg` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `n_material_task_cfg` | `state` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/expire/check`
> **品类规则查询列表**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryExpireCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/expire`
> **品类规则查询详情**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryExpire()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/query/apply/material`
> **品类规则删除不可见**

**返回类型**: `ResultDTO<List<DeliveryFlowCategoryDTO>>`

**调用链**:
  - `DeliveryFlowCategoryService.queryApplyCategory()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `n_material_task_cfg` | `id` |  |
| `ruleId` | Long | `delivery_flow_rule_category` | `rule_id` |  |
| `categoryId` | String | `n_material_task_cfg` | `category_id` |  |
| `categoryCode` | String | `delivery_flow_rule` | `category_code` |  |
| `materialCode` | String | `delivery_flow_rule` | `material_code` |  |
| `materialName` | String | `delivery_flow_rule` | `material_name` |  |
| `supplierList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `delivery_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `delivery_flow_rule_category` | `node_process_name` |  |
| `state` | Integer | `n_material_task_cfg` | `state` |  |
| `createBy` | Long | `n_material_task_cfg` | `create_by` |  |
| `createTime` | Date | `n_material_task_cfg` | `create_time` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `n_material_task_cfg` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/query/apply/judge`
> **品类规则失效校验**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.applyCategoryCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/query/apply/copy`
> **品类规则失效按钮**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.applyCategoryCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**
  - `DeliveryFlowCategoryService.applyCategoryCopy()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/copy`
> **判断套用的品类规则是否满足**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.applyCategoryCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**
  - `DeliveryFlowCategoryService.categoryCopy()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/save`
> **复制品类规则**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categorySaveCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**
  - `DeliveryFlowCategoryService.categorySave()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/effective/check`
> **复制品类规则**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryEffectiveCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/category/effective`
> **品类规则保存**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryEffective()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/supplier/update`
> **品类规则生效校验**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categorySupplierUpdate()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/update`
> **品类规则生效按钮**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryUpdate()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/category/update/check`
> **品类规则生效按钮**

**返回类型**: `ResultBuildDTO<String>`

**调用链**:
  - `DeliveryFlowCategoryService.categoryUpdateCheck()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `NMaterialNodeTransferConditionDao` → `n_material_node_transfer_condition`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `NMaterialRouteDao` → `n_material_route`
    - `NMaterialEdgeDao` → `n_material_edge`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialTimeCalculateRuleCfgDao` → `n_material_time_calculate_rule_cfg`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `delivery_flow_rule` | SELECT/UPDATE |
| `n_material_node_transfer_condition` | SELECT/UPDATE |
| `n_material_process_template` | SELECT/UPDATE |
| `n_material_route` | SELECT/UPDATE |
| `n_material_edge` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_time_calculate_rule_cfg` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`


---

# DeliveryFlowRuleInfoController
**Swagger 标签**: `材料流程配置规则页面查询`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowRuleInfoController.java`

---

## GET `/delivery/flow/delivery/process/info`
> **根据排程流程配置id查询排程详情**

**返回类型**: `ResultDTO<DeliveryProcessCfgFlowDTO>`

**调用链**:
  - `DeliveryFlowRuleService.queryDeliveryProcessInfo()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `saleTypeList` | DeliveryFlowItemDTO | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/rule/query/list`
> **根据排程流程配置id查询排程详情**

**返回类型**: `ResultDTO<Page<DeliveryFlowRuleDTO>>`

**调用链**:
  - `DeliveryFlowRuleService.queryFlowRuleList()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `delivery_flow_rule` | `id` |  |
| `gbCode` | String | `delivery_flow_rule_unit` | `gb_code` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `delivery_flow_rule` | `create_by` |  |
| `createTime` | Date | `delivery_flow_rule` | `create_time` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `delivery_flow_rule` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `delivery_flow_rule` | `state` |  |
| `categoryList` | List<DeliveryFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/rule/info`
> **根据排程流程配置id查询排程详情**

**返回类型**: `ResultDTO<DeliveryFlowRuleDTO>`

**调用链**:
  - `DeliveryFlowRuleService.queryFlowRulePageInfo()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `delivery_flow_rule` | `id` |  |
| `gbCode` | String | `delivery_flow_rule_unit` | `gb_code` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `delivery_flow_rule` | `create_by` |  |
| `createTime` | Date | `delivery_flow_rule` | `create_time` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `delivery_flow_rule` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `delivery_flow_rule` | `state` |  |
| `categoryList` | List<DeliveryFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/rule/info/simple`
> **根据排程流程配置id查询排程详情**

**返回类型**: `ResultDTO<DeliveryFlowRuleDTO>`

**调用链**:
  - `DeliveryFlowRuleService.queryFlowRuleSimpleInfo()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `delivery_flow_rule` | `id` |  |
| `gbCode` | String | `delivery_flow_rule_unit` | `gb_code` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `delivery_flow_rule` | `create_by` |  |
| `createTime` | Date | `delivery_flow_rule` | `create_time` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `delivery_flow_rule` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `delivery_flow_rule` | `state` |  |
| `categoryList` | List<DeliveryFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/rule/expire/check`
> **根据排程流程配置id查询排程详情**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowRuleService.ruleExpireCheck()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/rule/expire`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `DeliveryFlowRuleService.ruleExpire()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/rule/save`
> **流程配置查看详情**

**返回类型**: `DeliveryResultDTO<Long>`

**调用链**:
  - `DeliveryFlowRuleService.ruleSave()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/rule/check/save`
> **流程配置失效检查功能**

**返回类型**: `ResultBuildDTO<DeliveryFlowRuleDTO>`

**调用链**:
  - `DeliveryFlowRuleService.ruleSaveCheck()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `delivery_flow_rule` | `id` |  |
| `gbCode` | String | `delivery_flow_rule_unit` | `gb_code` |  |
| `deliveryProcessCfgId` | Long | `delivery_flow_rule` | `delivery_process_cfg_id` |  |
| `saleTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `delivery_flow_rule` | `project_version` |  |
| `mdmCompanyList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `documentTypeList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `productComboList` | List<DeliveryFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `delivery_flow_rule` | `create_by` |  |
| `createTime` | Date | `delivery_flow_rule` | `create_time` |  |
| `createName` | String | `delivery_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `delivery_flow_rule` | `modify_by` |  |
| `modifyTime` | Date | `delivery_flow_rule` | `modify_time` |  |
| `modifyName` | String | `delivery_flow_rule` | `modify_name` |  |
| `state` | Integer | `delivery_flow_rule` | `state` |  |
| `categoryList` | List<DeliveryFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/rule/info/update`
> **流程配置新增**

**返回类型**: `ResultBuildDTO<Long>`

**调用链**:
  - `DeliveryFlowRuleService.ruleUpdate()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## POST `/delivery/flow/rule/query/updaterecord`
> **流程配置新增保存校验**

**返回类型**: `ResultDTO<Page<DeliveryFlowBusinessRecordDTO>>`

**调用链**:
  - `DeliveryFlowRuleService.queryFlowUpdateRecordList()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `ruleId` | Long | `delivery_flow_rule_unit` | `rule_id` |  |
| `categoryId` | String | `delivery_flow_rule` | `category_id` |  |
| `categoryCode` | String | `delivery_flow_rule` | `category_code` |  |
| `operatorType` | Integer | `` | `` |  |
| `operatorId` | String | `delivery_flow_rule` | `id` |  |
| `operatorName` | String | `` | `` |  |
| `operatorTime` | String | `` | `` |  |
| `content` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`

---

## GET `/delivery/flow/rule/send/push`
> **规则的修改记录**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `DeliveryFlowRuleService.sendPushMsg()`
    - `DeliveryFlowRuleDao` → `delivery_flow_rule`
    - `DeliveryFlowRuleUnitDao` → `delivery_flow_rule_unit`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `DeliveryProcessCfgManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `delivery_flow_rule` | SELECT/UPDATE |
| `delivery_flow_rule_unit` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `DeliveryProcessCfgManager`


---

# DeliveryFlowSelectQueryController
**Swagger 标签**: `材料流程配置页面筛选项条件`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/DeliveryFlowSelectQueryController.java`

---

## GET `/delivery/flow/query/selected/companySug`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<List<DeliveryFlowSelectItemDTO>>`

**调用链**:
  - `DeliveryFlowSelectQueryService.queryCompanySug()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`

---

## GET `/delivery/flow/query/selected/material`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<MaterialCategorySugDTOV2>`

**调用链**:
  - `DeliveryFlowSelectQueryService.querySelectedMaterial()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`

---

## POST `/delivery/flow/query/selected/supplier`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<List<DeliveryFlowSelectItemDTO>>`

**调用链**:
  - `DeliveryFlowSelectQueryService.querySelectedSupplier()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`

---

## GET `/delivery/flow/query/selected/documentType`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<List<DeliveryFlowSelectItemDTO>>`

**调用链**:
  - `DeliveryFlowSelectQueryService.queryDocumentType()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`

---

## GET `/delivery/flow/query/selected/nodeProcessTemplateList`
> **查询可以选择的品类**

**返回类型**: `ResultDTO<List<DeliveryFlowProcessTemplateDTO>>`

**调用链**:
  - `DeliveryFlowSelectQueryService.queryNodeProcessTemplateList()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `nodeProcess` | Integer | `delivery_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `delivery_flow_rule_category` | `node_process_name` |  |
| `taskList` | List<DeliveryFlowProcessTaskDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`

---

## GET `/delivery/flow/query/system/version`
> **查询可以选择的供应商**

**返回类型**: `ResultDTO<List<DeliveryFlowSelectItemDTO>>`

**调用链**:
  - `DeliveryFlowSelectQueryService.querySystemVersionList()`
    - `NMaterialProcessTemplateDao` → `n_material_process_template`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_process_template` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `ScmProductManager`


---

# MaterialTaskProcessTemplateController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/deliveryflow/MaterialTaskProcessTemplateController.java`

---

## POST `/task/process/template/list`
> **获取主材任务模版列表**

**返回类型**: `ResultDTO<List<MaterialTaskProcessTemplateDTO>>`

**调用链**:
  - `MaterialTaskProcessTemplateService.getTaskProcessTemplateMaterList()`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_process_template` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `n_material_task_process_template` | `id` |  |
| `processTemplateId` | Integer | `n_material_task_process_template` | `process_template_id` |  |
| `processTemplateName` | String | `n_material_task_process_template` | `process_template_name` |  |
| `unifiedNodeCodeList` | List<String> | `` | `` |  |
| `nodeList` | List<NodeDTO> | `` | `` |  |
| `processNodeDetail` | String | `n_material_task_process_template` | `process_node_detail` |  |
| `templateDesc` | String | `n_material_task_process_template` | `template_desc` |  |
| `taskType` | Integer | `n_material_task_process_template` | `task_type` |  |
| `deliveryType` | Integer | `n_material_task_process_template` | `delivery_type` |  |
| `workerType` | Integer | `n_material_task_process_template` | `worker_type` |  |
| `coordinateData` | String | `n_material_task_process_template` | `coordinate_data` |  |
| `nodeType` | Integer | `` | `` |  |
| `nodeCode` | Integer | `n_material_task_process_template` | `node_code` |  |
| `nodeName` | String | `` | `` |  |
| `virtualNode` | Integer | `` | `` |  |
| `virtualUnifiedCode` | String | `` | `` |  |
| `unifiedNodeCode` | String | `n_material_task_process_template` | `unified_node_code` |  |
| `nodeModeType` | Integer | `` | `` |  |
| `isFirstNode` | Integer | `` | `` |  |
| `isActivateNode` | Integer | `n_material_task_process_template` | `activate_node` |  |

---

## POST `/task/process/template/list/add`
> **获取主材任务模版列表**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialTaskProcessTemplateService.addTaskProcessTemplate()`
    - `NMaterialTaskProcessTemplateDao` → `n_material_task_process_template`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_process_template` | SELECT/UPDATE |


---

# EventSubExtFixController
**Swagger 标签**: `消息消费测试接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/fix/EventSubExtFixController.java`

---

## POST `/event_pub/ext/send/retry`
> **消息重发-主材消息bug修复**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `EventPubExtService.send()`


---

# MeasureConfigRuleMigrationController
**Swagger 标签**: `交界面规则迁移接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/fix/MeasureConfigRuleMigrationController.java`

---

## POST `/fix/measure-config-rule/migrate-from-apollo`
> **迁移旧 Apollo handOverRequest 到交界面规则表（默认写入 1_20/3_20，兼容 snake_case 与数组字段）**

**返回类型**: `ResultDTO<MeasureConfigRuleMigrationResult>`

**调用链**:
  - `MeasureConfigRuleMigrationService.migrateFromApollo()`
    - `MeasureConfigRuleDao` → `material_measure_interface_config`
    - `MeasureConfigRuleDao` → `material_measure_interface_config_category_rel`
    - `MeasureConfigRuleDao` → `material_measure_interface_config_mdm_rel`
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_measure_interface_config_mdm_rel` | SELECT/UPDATE |
| `material_measure_interface_config_category_rel` | SELECT/UPDATE |
| `material_measure_interface_config` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `MerchantManager`
- `OfcDispatchConfigQueryManager`


---

# MeasureFormFixController
**Swagger 标签**: `测量申请单补数据接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/fix/MeasureFormFixController.java`

---

## POST `/measure/form/sendCadPhoto`
> **补偿测量表单数据**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MeasureFormFixService.sendCadPhoto()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ScmManager`

---

## POST `/task_dispatch/sdm_diff`
> **补偿测量表单数据**

**返回类型**: `ResultDTO<List<String>>`

**调用链**:
  - `MeasureFormFixService.taskDispatchSdmDiff()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ScmManager`


---

# MaterialTaskInstallerTaskController
**Swagger 标签**: `主材任务controller`
**基路径**: `/api/material-task`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/foreman/MaterialTaskInstallerTaskController.java`

---

## GET `/api/material-task/installer-task/list-install-task`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<List<TaskInstallVO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.listTaskByType()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/installer-task/complete-all`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<Void>`

**调用链**:
  - `MaterialTaskInstallerTaskService.completeAll()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/installer-task/pass-all`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<Void>`

**调用链**:
  - `MaterialTaskInstallerTaskService.passAll()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/installer-task/list-task`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<Page<TaskDTO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.listTask()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `address` | String | `` | `` |  |
| `homeownerName` | String | `` | `` |  |
| `packageName` | String | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `projectOrderId` | Long | `` | `` |  |
| `orderNo` | String | `` | `` |  |
| `SupplierType` | Integer | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `executorType` | Integer | `` | `` |  |
| `workerType` | String | `` | `` |  |
| `sortField` | Integer | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `canChangeAppoint` | Boolean | `` | `` |  |
| `processStatus` | Integer | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `startTime` | Date | `` | `` |  |
| `endTime` | Date | `` | `` |  |
| `qualified` | Integer | `` | `` |  |
| `images` | List<String> | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/installer-task/subordinate-task`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<Page<TaskDTO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.subordinateTask()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `address` | String | `` | `` |  |
| `homeownerName` | String | `` | `` |  |
| `packageName` | String | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `projectOrderId` | Long | `` | `` |  |
| `orderNo` | String | `` | `` |  |
| `SupplierType` | Integer | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `executorType` | Integer | `` | `` |  |
| `workerType` | String | `` | `` |  |
| `sortField` | Integer | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `canChangeAppoint` | Boolean | `` | `` |  |
| `processStatus` | Integer | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `startTime` | Date | `` | `` |  |
| `endTime` | Date | `` | `` |  |
| `qualified` | Integer | `` | `` |  |
| `images` | List<String> | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/installer-task/detail`
> **获取安装/进场任务列表**

**返回类型**: `ResultVO<TaskDetailDTO>`

**调用链**:
  - `MaterialTaskInstallerTaskService.taskDetail()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `designerName` | String | `` | `` |  |
| `designerId` | Long | `` | `` |  |
| `designerPhone` | String | `` | `` |  |
| `assistantName` | String | `` | `` |  |
| `assistantId` | Long | `` | `` |  |
| `assistantPhone` | String | `` | `` |  |
| `roleTypeName` | String | `` | `` |  |
| `roleType` | Integer | `` | `` |  |
| `installerName` | String | `` | `` |  |
| `installerId` | Long | `` | `` |  |
| `installerPhone` | String | `` | `` |  |
| `taskDispatchNodeId` | Long | `` | `` |  |
| `nodeName` | String | `` | `` |  |
| `images` | List<String> | `` | `` |  |
| `remarks` | String | `` | `` |  |
| `executorName` | String | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `endTime` | Date | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `changeAppointCount` | Integer | `` | `` |  |
| `deliveryDate` | Date | `` | `` |  |
| `deliveryRemarks` | String | `` | `` |  |
| `submitBy` | String | `` | `` |  |
| `submitName` | String | `` | `` |  |
| `submitTime` | Date | `` | `` |  |
| `processStatus` | Integer | `` | `` |  |
| `qualified` | Integer | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `taskDispatchId` | Long | `` | `` |  |
| `projectOrderId` | Long | `` | `` |  |
| `orderNo` | String | `` | `` |  |
| `SupplierType` | Integer | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `executorType` | Integer | `` | `` |  |
| `workerType` | String | `` | `` |  |
| `packageName` | String | `` | `` |  |
| `sortField` | Integer | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `canChangeAppoint` | Boolean | `` | `` |  |
| `startTime` | Date | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/handle`
> **一键完成进场任务**

**返回类型**: `ResultVO<Boolean>`

**调用链**:
  - `MaterialTaskInstallerTaskService.handle()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/task-detail`
> **一键合格**

**返回类型**: `ResultVO<List<TaskDispatchDetailVO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.taskDispatchDetail()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/dispatch/material-progress`
> **获取任务详情**

**返回类型**: `ResultVO<List<MaterialProgressItemDTO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.materialProgress()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `projectOrderId` | String | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `rulerMaterialCodeList` | List<String> | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialRulerId` | Long | `` | `` |  |
| `materialRulerType` | Integer | `` | `` |  |
| `executorTypeName` | String | `` | `` |  |
| `taskNodeName` | String | `` | `` |  |
| `progressType` | Integer | `` | `` |  |
| `newProgressStatus` | Integer | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `eventState` | Integer | `` | `` |  |
| `daySpan` | Integer | `` | `` |  |
| `lightCopy` | String | `` | `` |  |
| `endTime` | Date | `` | `` |  |
| `processStatus` | Byte | `` | `` |  |
| `flowType` | Integer | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/material-task-detail`
> **获取任务详情**

**返回类型**: `ResultVO<List<TaskDispatchDetailDTO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.materialTaskDetail()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `supplierName` | String | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `state` | Integer | `` | `` |  |
| `deliverModelSwitch` | Boolean | `` | `` |  |
| `taskName` | String | `` | `` |  |
| `processBatch` | Integer | `` | `` |  |
| `orderNo` | String | `` | `` |  |
| `sourceType` | Integer | `` | `` |  |
| `category` | Integer | `` | `` |  |
| `processStatus` | Integer | `` | `` |  |
| `taskEstimatedTime` | Date | `` | `` |  |
| `taskDispatchNodeId` | String | `` | `` |  |
| `taskDispatchId` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `nodeName` | String | `` | `` |  |
| `nodeTask` | String | `` | `` |  |
| `roleName` | String | `` | `` |  |
| `projectOrderId` | Long | `` | `` |  |
| `taskProcessList` | List<TaskProcess> | `` | `` |  |
| `planActivateTime` | Date | `` | `` |  |
| `businessId` | String | `` | `` |  |
| `serviceMode` | Integer | `` | `` |  |
| `mode` | Integer | `` | `` |  |
| `mdmCode` | String | `` | `` |  |
| `processCode` | String | `` | `` |  |
| `syncToSdm` | Integer | `` | `` |  |
| `homeOrderNo` | String | `` | `` |  |
| `saleType` | Integer | `` | `` |  |
| `flowType` | Integer | `` | `` |  |
| `taskProcessBatchId` | Long | `` | `` |  |
| `images` | List<String> | `` | `` |  |
| `remarks` | String | `` | `` |  |
| `executorName` | String | `` | `` |  |
| `executorId` | String | `` | `` |  |
| `executorType` | Integer | `` | `` |  |
| `executorTypeList` | List<Integer> | `` | `` |  |
| `executorTypeName` | String | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |
| `startTime` | Date | `` | `` |  |
| `endTime` | Date | `` | `` |  |
| `noticeRetainTime` | Date | `` | `` |  |
| `changeAppointCount` | Integer | `` | `` |  |
| `canChangeAppoint` | Boolean | `` | `` |  |
| `packageCode` | String | `` | `` |  |
| `deliveryDate` | Date | `` | `` |  |
| `deliveryRemarks` | String | `` | `` |  |
| `qualified` | Integer | `` | `` |  |
| `submitBy` | String | `` | `` |  |
| `submitName` | String | `` | `` |  |
| `submitTime` | Date | `` | `` |  |
| `eventState` | Integer | `` | `` |  |
| `daySpan` | Integer | `` | `` |  |
| `lightCopy` | String | `` | `` |  |
| `restart` | Integer | `` | `` |  |
| `branchName` | String | `` | `` |  |
| `conditionName` | String | `` | `` |  |
| `remarksFilter` | String | `` | `` |  |
| `orderTaskExtDto` | OrderTaskExtDto | `` | `` |  |
| `operationalTimeDesc` | String | `` | `` |  |
| `showOperationalTime` | Boolean | `` | `` |  |
| `workOrderId` | Long | `` | `` |  |
| `processReason` | String | `` | `` |  |
| `driver` | String | `` | `` |  |
| `driverUcId` | Long | `` | `` |  |
| `driverPhone` | String | `` | `` |  |
| `warehouseInfoDTO` | WarehouseInfoDTO | `` | `` |  |
| `allTimes` | List<TimeNode> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/measure-reject-detail`
> **获取订单下所有的主材任务进展/主材进展**

**返回类型**: `ResultVO<MeasureRejectDetailDTO>`

**调用链**:
  - `MaterialTaskInstallerTaskService.measureRejectDetail()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `executorPhone` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/common/install-time-limit`
> **获取订单下所有的主材任务进展/主材进展**

**返回类型**: `ResultVO<InstallTimeLimitDTO>`

**调用链**:
  - `MaterialTaskInstallerTaskService.installTimeLimit()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `installStartTime` | Date | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/common/change-appoint`
> **获取订单下所有的主材任务进展/主材进展**

**返回类型**: `ResultVO<Void>`

**调用链**:
  - `MaterialTaskInstallerTaskService.changeAppoint()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/dispatch/common/change-appoint-list`
> **获取主材所有任务及详情**

**返回类型**: `ResultVO<List<ChangeAppointListDTO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.changeAppointList()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `gmtModified` | Date | `` | `` |  |
| `createName` | String | `` | `` |  |
| `originalNoticeTime` | Date | `` | `` |  |
| `currentNoticeTime` | Date | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/installer-task/status-amount`
> **测量/复尺驳回详情（后端聚合）**

**返回类型**: `ResultVO<StatusAmountDTO>`

**调用链**:
  - `MaterialTaskInstallerTaskService.statusAmount()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `allAmount` | Integer | `` | `` |  |
| `uncompletedAmount` | Integer | `` | `` |  |
| `completedAmount` | Integer | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/installer-task/install-type-amount`
> **测量/复尺驳回详情（后端聚合）**

**返回类型**: `ResultVO<InstallTypeAmountDTO>`

**调用链**:
  - `MaterialTaskInstallerTaskService.installTypeAmount()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `allAmount` | Integer | `` | `` |  |
| `consignmentAmount` | Integer | `` | `` |  |
| `stockAmount` | Integer | `` | `` |  |
| `appointInstallAmount` | Integer | `` | `` |  |
| `installCompletedAmount` | Integer | `` | `` |  |
| `enterCompletedAmount` | Integer | `` | `` |  |
| `installAmount` | Integer | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## GET `/api/material-task/installer-task/combine-install-task`
> **通知安装节点上门时间限制**

**返回类型**: `ResultVO<List<TaskInstallVO>>`

**调用链**:
  - `MaterialTaskInstallerTaskService.combineInstallTask()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/batch-handle`
> **主材任务改约**

**返回类型**: `ResultVO<Boolean>`

**调用链**:
  - `MaterialTaskInstallerTaskService.batchHandle()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `UserCenterFeign`

---

## POST `/api/material-task/dispatch/batch-handle/package-construction-handle`
> **主材任务改约记录**

**返回类型**: `ResultDTO<Boolean>`

**调用链**:
  - `MaterialTaskInstallerTaskService.packageConstructionHandle()`
    - `PackageConstructionManager` → **[外部服务]**
    - `RubanManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `UserCenterFeign` → **[外部服务]**
  - `PackageConstructionService.preCheckAcceptance()`
    - `WorkCenterManager` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `RubanManager`
- `ScmManager`
- `WorkCenterManager`
- `UserCenterFeign`


---

# ManpowerCfgController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/manpower/ManpowerCfgController.java`

---

## GET `/manpower/cfg/roleTypeListNew`

**返回类型**: `ResultDTO<List<RoleTypeDTO>>`

**调用链**:
  - `MaterialTemplateV2Service.roleTypeListNew()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `NMaterialTemplateDao` → `n_material_template`
    - `NMaterialTemplateCopyLogDao` → `n_material_template_copy_log`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ProductComboManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `n_material_template_copy_log` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_payment_intercept_config` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `roleTypeCode` | Integer | `` | `` |  |
| `roleType` | String | `` | `` |  |
| `roleTypeName` | String | `n_material_template` | `name` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `MerchantManager`
- `ProductComboManager`
- `ScmMerchantManager`
- `ScmProductManager`


---

# MaterialMeasureTaskController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/manpower/MaterialMeasureTaskController.java`

---

## POST `/manpower/task/measureApply`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `ScmMerchantManager.initConfigurationId()`
  - `ScmMeasureApplyService.createTask()`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**
  - `ScmMeasureApplyService.createOrderTask()`
    - `TaskDispatchDao` → `task_dispatch`
    - `ProjectOrderManager` → **[外部服务]**
    - `WorkbenchManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `ProjectOrderManager`
- `WorkbenchManager`

---

## POST `/manpower/task/invokeRecheckAgain`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `MaterialMeasureService.judgeNewFuchiProcess()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `AquamanManager` → **[外部服务]**
    - `AthenaManager` → **[外部服务]**
    - `AtomManager` → **[外部服务]**
    - `BudgetPreviewManager` → **[外部服务]**
    - `MaterialSelectionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ProjectServerManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
  - `TaskDispatchDao.getById()`
  - `TaskDispatchDao.list()`
  - `TaskDispatchNodeDao.listByTaskId()`
  - `ProjectOrderManager.getProjectOrder()`
  - `MaterialCreateV2Service.newFuchiMaterialTaskCreate()`
    - `TaskCreateFailReasonDao` → `task_create_fail_reason`
    - `ProjectInfoDao` → `project_info`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `ConstructionManager` → **[外部服务]**
    - `DeliveryProcessCfgManager` → **[外部服务]**
    - `OfcManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `RetailManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_create_fail_reason` | SELECT/UPDATE |
| `project_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `AquamanManager`
- `AthenaManager`
- `AtomManager`
- `BudgetPreviewManager`
- `ConstructionManager`
- `DeliveryProcessCfgManager`
- `MaterialSelectionManager`
- `OfcManager`
- `ProjectOrderManager`
- `ProjectServerManager`
- `RetailManager`
- `ScmManager`
- `ScmMerchantManager`

---

## POST `/manpower/task/createByDelivery`

**返回类型**: `ResultDTO<Void>`

**调用链**:
  - `ScmManager.querySubOrderBaseInfo()`
  - `DeliveryMaterialBizService.createByDelivery()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskDispatchDao` → `task_dispatch`
    - `PackageConstructionManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `PackageConstructionManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmProductManager`


---

# MaterialFlowCategoryController
**Swagger 标签**: `材料流程品类规则配置`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowCategoryController.java`

---

## GET `/material/flow/category/query/list/bycondition`
> **品类规则查询列表**

**返回类型**: `ResultDTO<MaterialFlowCategoryPageInfoDTO>`

**调用链**:
  - `MaterialFlowCategoryService.queryCategoryListByCondition()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `total` | long | `` | `` |  |
| `categoryList` | List<MaterialFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/material/flow/category/query/list`
> **品类规则查询列表**

**返回类型**: `ResultDTO<MaterialFlowCategoryPageInfoDTO>`

**调用链**:
  - `MaterialFlowCategoryService.queryCategoryList()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `total` | long | `` | `` |  |
| `categoryList` | List<MaterialFlowCategoryDTO> | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/material/flow/category/query/info`
> **品类规则查询列表**

**返回类型**: `ResultDTO<MaterialFlowCategoryInfoDTO>`

**调用链**:
  - `MaterialFlowCategoryService.queryCategoryInfo()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_template_mapping` | `id` |  |
| `categoryId` | String | `material_flow_rule_category` | `category_id` |  |
| `categoryCode` | String | `material_flow_rule_category` | `category_code` |  |
| `state` | Integer | `material_flow_rule_template_mapping` | `state` |  |
| `ruleId` | Long | `material_flow_rule_template_mapping` | `rule_id` |  |
| `materialCode` | String | `material_flow_rule_category` | `material_code` |  |
| `materialName` | String | `material_flow_rule_category` | `material_name` |  |
| `supplierList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `material_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `material_flow_rule_category` | `node_process_name` |  |
| `workType` | String | `material_flow_rule_category` | `work_type` |  |
| `isShow` | Integer | `material_flow_rule_category` | `is_show` |  |
| `taskTypeList` | List<MaterialFlowTaskTypeItemDTO> | `` | `` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `` | `` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `gbCode` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/material/flow/category/delete`
> **品类规则查询列表**

**返回类型**: `ResultDTO<MaterialFlowCategoryInfoDTO>`

**调用链**:
  - `MaterialFlowCategoryService.categoryDelete()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_template_mapping` | `id` |  |
| `categoryId` | String | `material_flow_rule_category` | `category_id` |  |
| `categoryCode` | String | `material_flow_rule_category` | `category_code` |  |
| `state` | Integer | `material_flow_rule_template_mapping` | `state` |  |
| `ruleId` | Long | `material_flow_rule_template_mapping` | `rule_id` |  |
| `materialCode` | String | `material_flow_rule_category` | `material_code` |  |
| `materialName` | String | `material_flow_rule_category` | `material_name` |  |
| `supplierList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `material_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `material_flow_rule_category` | `node_process_name` |  |
| `workType` | String | `material_flow_rule_category` | `work_type` |  |
| `isShow` | Integer | `material_flow_rule_category` | `is_show` |  |
| `taskTypeList` | List<MaterialFlowTaskTypeItemDTO> | `` | `` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `` | `` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `gbCode` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/material/flow/category/expire`
> **品类规则查询列表**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categoryExpire()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## GET `/material/flow/category/effective`
> **品类规则查询详情**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categoryEffective()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/query/apply/material`
> **品类规则删除不可见**

**返回类型**: `ResultDTO<List<MaterialFlowCategoryDTO>>`

**调用链**:
  - `MaterialFlowCategoryService.queryApplyCategory()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_template_mapping` | `id` |  |
| `ruleId` | Long | `material_flow_rule_template_mapping` | `rule_id` |  |
| `categoryId` | String | `material_flow_rule_category` | `category_id` |  |
| `categoryCode` | String | `material_flow_rule_category` | `category_code` |  |
| `materialCode` | String | `material_flow_rule_category` | `material_code` |  |
| `materialName` | String | `material_flow_rule_category` | `material_name` |  |
| `supplierList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `material_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `material_flow_rule_category` | `node_process_name` |  |
| `nodeProcessDetailList` | List<MaterialFlowNodeProcessDTO> | `` | `` |  |
| `workType` | String | `material_flow_rule_category` | `work_type` |  |
| `isShow` | Integer | `material_flow_rule_category` | `is_show` |  |
| `state` | Integer | `material_flow_rule_template_mapping` | `state` |  |
| `createBy` | String | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `modifyBy` | String | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/query/apply/judge`
> **品类规则失效按钮**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.applyCategoryCheck()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/query/apply/copy`
> **品类规则生效按钮**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.applyCategoryCopy()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/copy`
> **查询品类规则套用的品类规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categoryCopy()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/save`
> **套用品类规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categorySave()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/saveWorkType`
> **套用品类规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categorySaveWorkType()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/effctive/check`
> **复制品类规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categoryEffectiveCheck()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/update`
> **品类规则保存**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowCategoryService.categoryUpdate()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/category/update/info`
> **品类规则保存作业角色**

**返回类型**: `ResultDTO<MaterialFlowCategoryInfoDTO>`

**调用链**:
  - `MaterialFlowCategoryService.categoryInfoUpdate()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_template_mapping` | `id` |  |
| `categoryId` | String | `material_flow_rule_category` | `category_id` |  |
| `categoryCode` | String | `material_flow_rule_category` | `category_code` |  |
| `state` | Integer | `material_flow_rule_template_mapping` | `state` |  |
| `ruleId` | Long | `material_flow_rule_template_mapping` | `rule_id` |  |
| `materialCode` | String | `material_flow_rule_category` | `material_code` |  |
| `materialName` | String | `material_flow_rule_category` | `material_name` |  |
| `supplierList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `nodeProcess` | Integer | `material_flow_rule_category` | `node_process` |  |
| `nodeProcessName` | String | `material_flow_rule_category` | `node_process_name` |  |
| `workType` | String | `material_flow_rule_category` | `work_type` |  |
| `isShow` | Integer | `material_flow_rule_category` | `is_show` |  |
| `taskTypeList` | List<MaterialFlowTaskTypeItemDTO> | `` | `` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `` | `` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `gbCode` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/process/business/check`
> **品类规则编辑**

**返回类型**: `ResultDTO<MaterialFlowItemDTO>`

**调用链**:
  - `MaterialFlowCategoryService.judgeBusinessHandle()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`


---

# MaterialFlowProcessController
**Swagger 标签**: `材料流程节点流程`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowProcessController.java`

---

## POST `/material/flow/rule/query/merge/list`
> **查询可以合并的配置规则列表**

**返回类型**: `ResultDTO<List<MaterialFlowRuleDTO>>`

**调用链**:
  - `MaterialFlowProcessAndTemplateServiceImpl.queryMergeFlowRuleList()`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `` | `` |  |
| `gbCode` | String | `` | `` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `` | `` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `` | `` |  |
| `createTime` | Date | `` | `` |  |
| `createName` | String | `` | `` |  |
| `modifyBy` | Long | `` | `` |  |
| `modifyTime` | Date | `` | `` |  |
| `modifyName` | String | `` | `` |  |
| `state` | Integer | `` | `` |  |
| `isDefault` | Integer | `` | `` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## POST `/material/flow/category/query/installBaseLine`
> **查询可以合并的配置规则列表**

**返回类型**: `ResultDTO<Page<TemplateListDTO>>`

**调用链**:
  - `MaterialFlowProcessAndTemplateServiceImpl.queryInstallBaseLine()`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `` | `` |  |
| `name` | String | `` | `` |  |
| `templateCode` | String | `` | `` |  |
| `description` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialCategoryList` | List<MaterialCategory> | `` | `` |  |
| `mode` | Integer | `` | `` |  |
| `companyInfoList` | List<CompanyInfo> | `` | `` |  |
| `productComboList` | List<ProductCombo> | `` | `` |  |
| `supplierList` | List<Supplier> | `` | `` |  |
| `processDefineList` | List<ProcessDefine> | `` | `` |  |
| `taskType` | TaskType | `` | `` |  |
| `operationAudit` | OperationAudit | `` | `` |  |
| `state` | Integer | `` | `` |  |
| `stateText` | String | `` | `` |  |
| `updateTime` | Date | `` | `` |  |
| `workType` | List<WorkTypeVO> | `` | `` |  |
| `saleTypes` | List<SaleTypeVO> | `` | `` |  |
| `materialSuppliers` | List<MaterialSupplierVO> | `` | `` |  |
| `version` | Integer | `` | `` |  |
| `mdmCode` | String | `` | `` |  |
| `mdmCompanyName` | String | `` | `` |  |
| `productComboId` | Long | `` | `` |  |
| `productComboName` | String | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `templateId` | Long | `` | `` |  |
| `copyId` | Long | `` | `` |  |
| `copyTemplateId` | Long | `` | `` |  |
| `copyTemplateName` | String | `` | `` |  |
| `baseId` | Long | `` | `` |  |
| `baseTemplateId` | Long | `` | `` |  |
| `baseTemplateName` | String | `` | `` |  |
| `sourceId` | Long | `` | `` |  |
| `sourceName` | String | `` | `` |  |
| `sourceDesc` | String | `` | `` |  |
| `rangeType` | Byte | `` | `` |  |
| `taskName` | String | `` | `` |  |
| `processCode` | String | `` | `` |  |
| `restarted` | Integer | `` | `` |  |
| `branchName` | String | `` | `` |  |
| `coordinateData` | String | `` | `` |  |
| `processBatch` | Integer | `` | `` |  |
| `taskNodeList` | List<TaskNodeDTO> | `` | `` |  |
| `changeNodeList` | List<Integer> | `` | `` |  |
| `baseTemplateCode` | String | `` | `` |  |
| `nodeName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `nodeCode` | Integer | `` | `` |  |
| `conditionList` | List<Condition> | `` | `` |  |
| `sourceType` | Integer | `` | `` |  |
| `sourceTypeText` | String | `` | `` |  |
| `serviceMode` | Integer | `` | `` |  |
| `serviceModeText` | String | `` | `` |  |
| `activateMode` | Integer | `` | `` |  |
| `activateModeText` | String | `` | `` |  |
| `activateRelationType` | Byte | `` | `` |  |
| `activateRelationCode` | String | `` | `` |  |
| `activateRelationName` | String | `` | `` |  |
| `activateScheduleRelation` | Byte | `` | `` |  |
| `activateDuration` | Integer | `` | `` |  |
| `processBatchText` | String | `` | `` |  |
| `remindType` | Integer | `` | `` |  |
| `roleName` | String | `` | `` |  |
| `roleType` | Integer | `` | `` |  |
| `roleNameList` | List<String> | `` | `` |  |
| `roleTypeList` | List<Integer> | `` | `` |  |
| `taskNodeTimeList` | List<TaskNodeTimeDTO> | `` | `` |  |
| `restartRemindType` | Integer | `` | `` |  |
| `restartRoleType` | Integer | `` | `` |  |
| `restartRoleName` | String | `` | `` |  |
| `restartRoleTypeList` | List<Integer> | `` | `` |  |
| `restartRoleNameList` | List<String> | `` | `` |  |
| `dispatchTypeList` | List<Integer> | `` | `` |  |
| `restartNodeType` | Integer | `` | `` |  |
| `restartTaskType` | Integer | `` | `` |  |
| `unionRule` | Integer | `` | `` |  |
| `ext` | String | `` | `` |  |
| `noticeType` | Integer | `` | `` |  |
| `virtualNode` | String | `` | `` |  |
| `nodeModeType` | String | `` | `` |  |
| `isSendInstall` | Boolean | `` | `` |  |
| `noticeList` | List<TaskNodeNoticeList> | `` | `` |  |
| `baseLineModify` | Integer | `` | `` |  |
| `branchConditionCode` | String | `` | `` |  |
| `branchConditionName` | String | `` | `` |  |
| `userCode` | String | `` | `` |  |
| `userName` | String | `` | `` |  |
| `passStatus` | Byte | `` | `` |  |
| `rejectStatus` | Byte | `` | `` |  |
| `processDefId` | String | `` | `` |  |
| `processInstId` | String | `` | `` |  |
| `processStatus` | Byte | `` | `` |  |
| `createBy` | String | `` | `` |  |
| `modifyBy` | String | `` | `` |  |
| `createTime` | Date | `` | `` |  |
| `ruleId` | Long | `` | `` |  |
| `ruleCategoryId` | Long | `` | `` |  |
| `nTemplateId` | Long | `` | `` |  |

---

## POST `/material/flow/category/getSaleType`
> **查询可以合并的配置规则列表**

**返回类型**: `ResultDTO<List<SaleTypeVO>>`

**调用链**:
  - `MaterialFlowMixTemplateService.transTemplateSaleType()`
    - `MaterialFlowRuleTemplateRecordDao` → `material_flow_rule_template_record`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `NMaterialTemplateDao` → `n_material_template`
    - `OfcDispatchConfigMergeManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_record` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigMergeManager`

---

## POST `/material/flow/process/task/info`
> **查询可以合并的配置规则列表**

**返回类型**: `ResultDTO<List<InstallTemplateTabDto>>`

**调用链**:
  - `MaterialFlowProcessAndTemplateServiceImpl.processTabInfo()`

---

## POST `/material/flow/process/task/save`
> **品类规则查询安装底线接口**

**返回类型**: `ResultDTO< List<MaterialFlowProcessMappingDTO>>`

**调用链**:
  - `MaterialFlowProcessAndTemplateServiceImpl.checkSaveParam()`
  - `MaterialFlowProcessAndTemplateServiceImpl.processSave()`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `` | `` |  |
| `ruleId` | Long | `` | `` |  |
| `ruleCategoryId` | String | `` | `` |  |
| `templateId` | Long | `` | `` |  |
| `taskType` | Integer | `` | `` |  |
| `configType` | Integer | `` | `` |  |
| `categoryList` | List<MaterialFlowCategorySingleDto> | `` | `` |  |
| `templateInfoList` | TemplateListDTO | `` | `` |  |

---

## POST `/material/flow/process/merge/task/list`
> **作业流程节点详情**

**返回类型**: `ResultDTO<TemplateListDTO>`

**调用链**:
  - `MaterialFlowProcessAndTemplateServiceImpl.queryMergeCityTempList()`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `` | `` |  |
| `name` | String | `` | `` |  |
| `templateCode` | String | `` | `` |  |
| `description` | String | `` | `` |  |
| `materialCode` | String | `` | `` |  |
| `materialName` | String | `` | `` |  |
| `materialCategoryList` | List<MaterialCategory> | `` | `` |  |
| `mode` | Integer | `` | `` |  |
| `companyInfoList` | List<CompanyInfo> | `` | `` |  |
| `productComboList` | List<ProductCombo> | `` | `` |  |
| `supplierList` | List<Supplier> | `` | `` |  |
| `processDefineList` | List<ProcessDefine> | `` | `` |  |
| `taskType` | TaskType | `` | `` |  |
| `operationAudit` | OperationAudit | `` | `` |  |
| `state` | Integer | `` | `` |  |
| `stateText` | String | `` | `` |  |
| `updateTime` | Date | `` | `` |  |
| `workType` | List<WorkTypeVO> | `` | `` |  |
| `saleTypes` | List<SaleTypeVO> | `` | `` |  |
| `materialSuppliers` | List<MaterialSupplierVO> | `` | `` |  |
| `version` | Integer | `` | `` |  |
| `mdmCode` | String | `` | `` |  |
| `mdmCompanyName` | String | `` | `` |  |
| `productComboId` | Long | `` | `` |  |
| `productComboName` | String | `` | `` |  |
| `supplierCode` | String | `` | `` |  |
| `supplierName` | String | `` | `` |  |
| `templateId` | Long | `` | `` |  |
| `copyId` | Long | `` | `` |  |
| `copyTemplateId` | Long | `` | `` |  |
| `copyTemplateName` | String | `` | `` |  |
| `baseId` | Long | `` | `` |  |
| `baseTemplateId` | Long | `` | `` |  |
| `baseTemplateName` | String | `` | `` |  |
| `sourceId` | Long | `` | `` |  |
| `sourceName` | String | `` | `` |  |
| `sourceDesc` | String | `` | `` |  |
| `rangeType` | Byte | `` | `` |  |
| `taskName` | String | `` | `` |  |
| `processCode` | String | `` | `` |  |
| `restarted` | Integer | `` | `` |  |
| `branchName` | String | `` | `` |  |
| `coordinateData` | String | `` | `` |  |
| `processBatch` | Integer | `` | `` |  |
| `taskNodeList` | List<TaskNodeDTO> | `` | `` |  |
| `changeNodeList` | List<Integer> | `` | `` |  |
| `baseTemplateCode` | String | `` | `` |  |
| `nodeName` | String | `` | `` |  |
| `nodeType` | Integer | `` | `` |  |
| `nodeCode` | Integer | `` | `` |  |
| `conditionList` | List<Condition> | `` | `` |  |
| `sourceType` | Integer | `` | `` |  |
| `sourceTypeText` | String | `` | `` |  |
| `serviceMode` | Integer | `` | `` |  |
| `serviceModeText` | String | `` | `` |  |
| `activateMode` | Integer | `` | `` |  |
| `activateModeText` | String | `` | `` |  |
| `activateRelationType` | Byte | `` | `` |  |
| `activateRelationCode` | String | `` | `` |  |
| `activateRelationName` | String | `` | `` |  |
| `activateScheduleRelation` | Byte | `` | `` |  |
| `activateDuration` | Integer | `` | `` |  |
| `processBatchText` | String | `` | `` |  |
| `remindType` | Integer | `` | `` |  |
| `roleName` | String | `` | `` |  |
| `roleType` | Integer | `` | `` |  |
| `roleNameList` | List<String> | `` | `` |  |
| `roleTypeList` | List<Integer> | `` | `` |  |
| `taskNodeTimeList` | List<TaskNodeTimeDTO> | `` | `` |  |
| `restartRemindType` | Integer | `` | `` |  |
| `restartRoleType` | Integer | `` | `` |  |
| `restartRoleName` | String | `` | `` |  |
| `restartRoleTypeList` | List<Integer> | `` | `` |  |
| `restartRoleNameList` | List<String> | `` | `` |  |
| `dispatchTypeList` | List<Integer> | `` | `` |  |
| `restartNodeType` | Integer | `` | `` |  |
| `restartTaskType` | Integer | `` | `` |  |
| `unionRule` | Integer | `` | `` |  |
| `ext` | String | `` | `` |  |
| `noticeType` | Integer | `` | `` |  |
| `virtualNode` | String | `` | `` |  |
| `nodeModeType` | String | `` | `` |  |
| `isSendInstall` | Boolean | `` | `` |  |
| `noticeList` | List<TaskNodeNoticeList> | `` | `` |  |
| `baseLineModify` | Integer | `` | `` |  |
| `branchConditionCode` | String | `` | `` |  |
| `branchConditionName` | String | `` | `` |  |
| `userCode` | String | `` | `` |  |
| `userName` | String | `` | `` |  |
| `passStatus` | Byte | `` | `` |  |
| `rejectStatus` | Byte | `` | `` |  |
| `processDefId` | String | `` | `` |  |
| `processInstId` | String | `` | `` |  |
| `processStatus` | Byte | `` | `` |  |
| `createBy` | String | `` | `` |  |
| `modifyBy` | String | `` | `` |  |
| `createTime` | Date | `` | `` |  |
| `ruleId` | Long | `` | `` |  |
| `ruleCategoryId` | Long | `` | `` |  |
| `nTemplateId` | Long | `` | `` |  |

---

## POST `/material/flow/manpower/cfg/templateList`
> **作业流程节点详情**

**返回类型**: `ResultDTO<List<TemplateListDTO>>`

**调用链**:
  - `MaterialTemplateV2Service.templateList()`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `NMaterialTemplateUnitDao` → `n_material_template_unit`
    - `NMaterialTemplateDao` → `n_material_template`
    - `NMaterialTemplateCopyLogDao` → `n_material_template_copy_log`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `MaterialPaymentInterceptConfigDao` → `material_payment_intercept_config`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `ProductComboManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `n_material_task_cfg` | SELECT/UPDATE |
| `n_material_template_unit` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |
| `n_material_template_copy_log` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `material_payment_intercept_config` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `n_material_task_cfg` | `id` |  |
| `name` | String | `n_material_template` | `name` |  |
| `templateCode` | String | `n_material_template` | `template_code` |  |
| `description` | String | `n_material_template` | `description` |  |
| `materialCode` | String | `n_material_template_unit` | `material_code` |  |
| `materialName` | String | `n_material_template_unit` | `material_name` |  |
| `materialCategoryList` | List<MaterialCategory> | `` | `` |  |
| `mode` | Integer | `n_material_template_unit` | `mode` |  |
| `companyInfoList` | List<CompanyInfo> | `` | `` |  |
| `productComboList` | List<ProductCombo> | `` | `` |  |
| `supplierList` | List<Supplier> | `` | `` |  |
| `processDefineList` | List<ProcessDefine> | `` | `` |  |
| `taskType` | TaskType | `n_material_process_define` | `task_type` |  |
| `operationAudit` | OperationAudit | `` | `` |  |
| `state` | Integer | `n_material_task_cfg` | `state` |  |
| `stateText` | String | `` | `` |  |
| `updateTime` | Date | `n_material_task_cfg` | `update_time` |  |
| `workType` | List<WorkTypeVO> | `n_material_template_unit` | `work_type` |  |
| `saleTypes` | List<SaleTypeVO> | `` | `` |  |
| `materialSuppliers` | List<MaterialSupplierVO> | `` | `` |  |
| `version` | Integer | `n_material_task_cfg` | `version` |  |
| `mdmCode` | String | `n_material_template_unit` | `mdm_code` |  |
| `mdmCompanyName` | String | `n_material_template_unit` | `mdm_company_name` |  |
| `productComboId` | Long | `n_material_template_unit` | `product_combo_id` |  |
| `productComboName` | String | `n_material_template_unit` | `product_combo_name` |  |
| `supplierCode` | String | `n_material_template_unit` | `supplier_code` |  |
| `supplierName` | String | `n_material_template_unit` | `supplier_name` |  |
| `templateId` | Long | `n_material_template_unit` | `template_id` |  |
| `copyId` | Long | `n_material_task_cfg` | `id` |  |
| `copyTemplateId` | Long | `n_material_process_define` | `copy_template_id` |  |
| `copyTemplateName` | String | `n_material_process_define` | `copy_template_name` |  |
| `baseId` | Long | `n_material_process_define` | `base_id` |  |
| `baseTemplateId` | Long | `n_material_process_define` | `base_template_id` |  |
| `baseTemplateName` | String | `n_material_process_define` | `base_template_name` |  |
| `sourceId` | Long | `n_material_process_define` | `source_id` |  |
| `sourceName` | String | `n_material_process_define` | `source_name` |  |
| `sourceDesc` | String | `n_material_process_define` | `source_desc` |  |
| `rangeType` | Byte | `n_material_template_unit` | `range_type` |  |
| `taskName` | String | `n_material_template` | `name` |  |
| `processCode` | String | `n_material_task_cfg` | `process_code` |  |
| `restarted` | Integer | `` | `` |  |
| `branchName` | String | `n_material_template` | `name` |  |
| `coordinateData` | String | `` | `` |  |
| `processBatch` | Integer | `n_material_process_define` | `process_batch` |  |
| `taskNodeList` | List<TaskNodeDTO> | `` | `` |  |
| `changeNodeList` | List<Integer> | `` | `` |  |
| `baseTemplateCode` | String | `n_material_process_define` | `base_template_code` |  |
| `nodeName` | String | `n_material_template` | `name` |  |
| `nodeType` | Integer | `n_material_task_cfg` | `restart_node_type` |  |
| `nodeCode` | Integer | `n_material_task_cfg` | `node_code` |  |
| `conditionList` | List<Condition> | `` | `` |  |
| `sourceType` | Integer | `n_material_process_define` | `source_type` |  |
| `sourceTypeText` | String | `` | `` |  |
| `serviceMode` | Integer | `n_material_task_cfg` | `service_mode` |  |
| `serviceModeText` | String | `` | `` |  |
| `activateMode` | Integer | `n_material_template_unit` | `mode` |  |
| `activateModeText` | String | `` | `` |  |
| `activateRelationType` | Byte | `` | `` |  |
| `activateRelationCode` | String | `` | `` |  |
| `activateRelationName` | String | `n_material_template` | `name` |  |
| `activateScheduleRelation` | Byte | `` | `` |  |
| `activateDuration` | Integer | `` | `` |  |
| `processBatchText` | String | `` | `` |  |
| `remindType` | Integer | `n_material_task_cfg` | `remind_type` |  |
| `roleName` | String | `n_material_template` | `name` |  |
| `roleType` | Integer | `` | `` |  |
| `roleNameList` | List<String> | `` | `` |  |
| `roleTypeList` | List<Integer> | `` | `` |  |
| `taskNodeTimeList` | List<TaskNodeTimeDTO> | `` | `` |  |
| `restartRemindType` | Integer | `n_material_task_cfg` | `restart_remind_type` |  |
| `restartRoleType` | Integer | `` | `` |  |
| `restartRoleName` | String | `n_material_template` | `name` |  |
| `restartRoleTypeList` | List<Integer> | `` | `` |  |
| `restartRoleNameList` | List<String> | `` | `` |  |
| `dispatchTypeList` | List<Integer> | `` | `` |  |
| `restartNodeType` | Integer | `n_material_task_cfg` | `restart_node_type` |  |
| `restartTaskType` | Integer | `n_material_task_cfg` | `restart_task_type` |  |
| `unionRule` | Integer | `n_material_task_cfg` | `union_rule` |  |
| `ext` | String | `` | `` |  |
| `noticeType` | Integer | `n_material_task_cfg` | `notice_type` |  |
| `virtualNode` | String | `` | `` |  |
| `nodeModeType` | String | `` | `` |  |
| `isSendInstall` | Boolean | `n_material_task_cfg` | `is_send_install` |  |
| `noticeList` | List<TaskNodeNoticeList> | `` | `` |  |
| `baseLineModify` | Integer | `` | `` |  |
| `branchConditionCode` | String | `` | `` |  |
| `branchConditionName` | String | `n_material_template` | `name` |  |
| `userCode` | String | `` | `` |  |
| `userName` | String | `n_material_template` | `name` |  |
| `passStatus` | Byte | `` | `` |  |
| `rejectStatus` | Byte | `` | `` |  |
| `processDefId` | String | `n_material_task_cfg` | `id` |  |
| `processInstId` | String | `n_material_task_cfg` | `id` |  |
| `processStatus` | Byte | `` | `` |  |
| `createBy` | String | `n_material_task_cfg` | `create_by` |  |
| `modifyBy` | String | `n_material_task_cfg` | `modify_by` |  |
| `createTime` | Date | `n_material_task_cfg` | `create_time` |  |
| `ruleId` | Long | `delivery_flow_rule_category` | `rule_id` |  |
| `ruleCategoryId` | Long | `n_material_task_cfg` | `id` |  |
| `nTemplateId` | Long | `n_material_task_cfg` | `id` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `MerchantManager`
- `ProductComboManager`
- `ScmMerchantManager`
- `ScmProductManager`


---

# MaterialFlowQueryController
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowQueryController.java`

---

## POST `/material/flow/rule/send/applydata/msg`
> **手动发送消息**

**返回类型**: `String`

**调用链**:
  - `MaterialFlowQueryService.sendApplyMsgHandler()`
    - `TaskDispatchDao` → `task_dispatch`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `NMaterialTemplateDao` → `n_material_template`
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `n_material_template` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigQueryManager`
- `ScmManager`


---

# MaterialFlowRuleInfoController
**Swagger 标签**: `材料流程配置规则页面查询`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowRuleInfoController.java`

---

## POST `/material/flow/rule/query/list`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<Page<MaterialFlowRuleDTO>>`

**调用链**:
  - `MaterialFlowRuleService.queryFlowRuleList()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_category` | `id` |  |
| `gbCode` | String | `material_flow_rule_unit` | `gb_code` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `material_flow_rule_unit` | `project_version` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `createName` | String | `material_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |
| `modifyName` | String | `material_flow_rule` | `modify_name` |  |
| `state` | Integer | `material_flow_rule_category` | `state` |  |
| `isDefault` | Integer | `material_flow_rule` | `is_default` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## GET `/material/flow/rule/info`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<MaterialFlowRuleDTO>`

**调用链**:
  - `MaterialFlowRuleService.queryFlowRulePageInfo()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_category` | `id` |  |
| `gbCode` | String | `material_flow_rule_unit` | `gb_code` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `material_flow_rule_unit` | `project_version` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `createName` | String | `material_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |
| `modifyName` | String | `material_flow_rule` | `modify_name` |  |
| `state` | Integer | `material_flow_rule_category` | `state` |  |
| `isDefault` | Integer | `material_flow_rule` | `is_default` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## GET `/material/flow/rule/info/simple`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<MaterialFlowRuleDTO>`

**调用链**:
  - `MaterialFlowRuleService.queryFlowRuleInfo()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_category` | `id` |  |
| `gbCode` | String | `material_flow_rule_unit` | `gb_code` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `material_flow_rule_unit` | `project_version` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `createName` | String | `material_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |
| `modifyName` | String | `material_flow_rule` | `modify_name` |  |
| `state` | Integer | `material_flow_rule_category` | `state` |  |
| `isDefault` | Integer | `material_flow_rule` | `is_default` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## POST `/material/flow/rule/info/update`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<Long>`

**调用链**:
  - `MaterialFlowRuleService.ruleUpdate()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## GET `/material/flow/rule/expire`
> **查询流程配置规则列表**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleExpire()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## GET `/material/flow/rule/effective`
> **流程配置查看**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleEffective()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## POST `/material/flow/rule/save`
> **流程配置编辑**

**返回类型**: `ResultDTO<MaterialFlowRuleDTO>`

**调用链**:
  - `MaterialFlowRuleService.ruleSave()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_category` | `id` |  |
| `gbCode` | String | `material_flow_rule_unit` | `gb_code` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `material_flow_rule_unit` | `project_version` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `createName` | String | `material_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |
| `modifyName` | String | `material_flow_rule` | `modify_name` |  |
| `state` | Integer | `material_flow_rule_category` | `state` |  |
| `isDefault` | Integer | `material_flow_rule` | `is_default` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## GET `/material/flow/rule/set/default`
> **流程配置失效功能**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleSetDefault()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## POST `/material/flow/rule/check/save`
> **流程配置列表页生效功能**

**返回类型**: `ResultBuildDTO<MaterialFlowRuleDTO>`

**调用链**:
  - `MaterialFlowRuleService.ruleSaveCheck()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `id` | Long | `material_flow_rule_category` | `id` |  |
| `gbCode` | String | `material_flow_rule_unit` | `gb_code` |  |
| `saleTypeList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `projectVersion` | String | `material_flow_rule_unit` | `project_version` |  |
| `mdmCompanyList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `merchantList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `productComboList` | List<MaterialFlowItemDTO> | `` | `` |  |
| `createBy` | Long | `material_flow_rule_category` | `create_by` |  |
| `createTime` | Date | `material_flow_rule_category` | `create_time` |  |
| `createName` | String | `material_flow_rule` | `create_name` |  |
| `modifyBy` | Long | `material_flow_rule_category` | `modify_by` |  |
| `modifyTime` | Date | `material_flow_rule_category` | `modify_time` |  |
| `modifyName` | String | `material_flow_rule` | `modify_name` |  |
| `state` | Integer | `material_flow_rule_category` | `state` |  |
| `isDefault` | Integer | `material_flow_rule` | `is_default` |  |
| `categoryPageInfoDTO` | MaterialFlowCategoryPageInfoDTO | `` | `` |  |

---

## POST `/material/flow/rule/add/company`
> **流程配置设置默认规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleAddCompany()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## POST `/material/flow/rule/all/copy`
> **流程配置设置默认规则**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleAllCopy()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## POST `/material/flow/rule/all/effective`
> **流程配置保存校验**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowRuleService.ruleAllEffective()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

---

## POST `/material/flow/rule/query/updaterecord`
> **流程配置保存校验**

**返回类型**: `ResultDTO<Page<MaterialFlowBusinessRecordDTO>>`

**调用链**:
  - `MaterialFlowRuleService.queryFlowUpdateRecordList()`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `MaterialFlowRuleUnitDao` → `material_flow_rule_unit`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_category` | SELECT/UPDATE |
| `material_flow_rule` | SELECT/UPDATE |
| `material_flow_rule_unit` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `ruleId` | Long | `material_flow_rule_category` | `rule_id` |  |
| `categoryId` | String | `material_flow_rule_category` | `category_id` |  |
| `categoryCode` | String | `material_flow_rule_category` | `category_code` |  |
| `operatorType` | Integer | `` | `` |  |
| `operatorId` | String | `material_flow_rule_category` | `id` |  |
| `operatorName` | String | `` | `` |  |
| `operatorTime` | String | `` | `` |  |
| `content` | String | `` | `` |  |

---

## POST `/material/flow/rule/excel/export`
> **刷数：根据规则id新增分公司**

**返回类型**: `ResultDTO<String>`

**调用链**:
  - `MaterialFlowHandleService.excelExport()`
    - `MaterialFlowRuleDao` → `material_flow_rule`
    - `OfcDispatchConfigQueryManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `OfcDispatchConfigQueryManager`


---

# MaterialFlowRuleQueryController
**Swagger 标签**: `材料流程配置规则页面查询`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowRuleQueryController.java`

---

## GET `/material/flow/query/category/code`
> **根据用工code查询规则id**

**返回类型**: `ResultDTO<List<String>>`

**调用链**:
  - `MaterialFlowRuleQueryService.queryCategoryCodeByTemplateCode()`
    - `MaterialFlowRuleTemplateMappingDao` → `material_flow_rule_template_mapping`
    - `MaterialFlowRuleCategoryDao` → `material_flow_rule_category`
    - `NMaterialProcessDefineDao` → `n_material_process_define`
    - `DeliveryFlowRuleCategoryDao` → `delivery_flow_rule_category`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_flow_rule_template_mapping` | SELECT/UPDATE |
| `material_flow_rule_category` | SELECT/UPDATE |
| `n_material_process_define` | SELECT/UPDATE |
| `delivery_flow_rule_category` | SELECT/UPDATE |


---

# MaterialFlowSelectQueryController
**Swagger 标签**: `材料流程配置页面筛选项条件`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/materialflow/MaterialFlowSelectQueryController.java`

---

## GET `/material/flow/query/selected/companySug`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<MaterialFlowSelectItemDTO>`

**调用链**:
  - `MaterialFlowSelectQueryService.queryCompanySug()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |
| `isSelected` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## GET `/material/flow/query/selected/material`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<MaterialCategorySugDTOV2>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySelectedMaterial()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## POST `/material/flow/query/selected/supplier`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<MaterialFlowSelectItemDTO>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySelectedSupplier()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |
| `isSelected` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## POST `/material/flow/query/selected/product`
> **查询登录人的分公司**

**返回类型**: `ResultDTO<MaterialFlowSelectItemDTO>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySelectedProduct()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |
| `isSelected` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## POST `/material/flow/query/selected/merchant`
> **查询可以选择的品类**

**返回类型**: `ResultDTO<MaterialFlowSelectItemDTO>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySelectedMerchant()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |
| `isSelected` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## GET `/material/flow/query/selected/saleType`
> **查询可以选择的供应商**

**返回类型**: `ResultDTO<MaterialFlowSelectItemDTO>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySaleType()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `key` | String | `` | `` |  |
| `value` | String | `` | `` |  |
| `isSelected` | boolean | `` | `` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## GET `/material/flow/query/selected/ofcNodeProcessTemplateList`
> **查询可以选择的套餐**

**返回类型**: `ResultDTO<List<DispatchTemplateDto>>`

**调用链**:
  - `MaterialFlowSelectQueryService.queryOfcNodeProcessTemplateList()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`

---

## GET `/material/flow/query/system/version`
> **查询可以选择的店铺**

**返回类型**: `ResultDTO<List<EnumDto>>`

**调用链**:
  - `MaterialFlowSelectQueryService.querySystemVersionList()`
    - `CeresManager` → **[外部服务]**
    - `ComboInfoManager` → **[外部服务]**
    - `MerchantManager` → **[外部服务]**
    - `OfcDispatchConfigMergeManager` → **[外部服务]**
    - `OfcDispatchConfigQueryManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `CeresManager`
- `ComboInfoManager`
- `MerchantManager`
- `OfcDispatchConfigMergeManager`
- `OfcDispatchConfigQueryManager`
- `ScmProductManager`


---

# McpTestController
**Swagger 标签**: `mcp测试相关接口`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/mcp/McpTestController.java`

---

## GET `/mcp/query/match/task`
> **查询匹配的任务**

**返回类型**: `ResultDTO<McpQueryTaskResultDTO>`

**调用链**:
  - `TaskDispatchDao.list()`
  - `TaskDispatchService.getBatchTaskList()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `currNodeId` | Long | `material_task_node` | `id` |  |
| `currNodeName` | String | `` | `` |  |
| `currTaskName` | String | `` | `` |  |
| `nextNodeId` | Long | `material_task_node` | `id` |  |
| `nextNodeName` | String | `` | `` |  |
| `nextTaskName` | String | `` | `` |  |
| `status` | Integer | `task_dispatch` | `process_status` |  |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`

---

## GET `/mcp/query/match/task/detail`
> **查询匹配的任务详情**

**返回类型**: `ResultDTO<TaskDetailMCPDTO>`

**调用链**:
  - `TaskDispatchDao.getById()`
  - `TaskDispatchNodeDao.getById()`

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `executorName` | String | `` | `` |  |
| `executorId` | String | `` | `` |  |
| `executorTypeDesc` | String | `` | `` |  |
| `estimatedTime` | Date | `` | `` |  |

---

## GET `/mcp/query/match/task/process`
> **查询匹配的任务详情**

**返回类型**: `ResultDTO<List<TaskDispatchDetailDTO.TaskProcess>>`

**调用链**:
  - `TaskDispatchDao.list()`
  - `TaskDispatchService.getBatchTaskList()`
    - `MaterialTaskNodeDao` → `material_task_node`
    - `NMaterialTaskCfgDao` → `n_material_task_cfg`
    - `MaterialTaskDao` → `material_task`
    - `DesignReviewAlterationDao` → `design_review_alteration`
    - `NoticeTimeHistoryDao` → `notice_time_history`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `SupplierSyncInfoDao` → `supplier_sync_info`
    - `TaskDispatchDao` → `task_dispatch`
    - `BimKnowledgeManager` → **[外部服务]**
    - `CeresManager` → **[外部服务]**
    - `FulfillmentLinkRelationManager` → **[外部服务]**
    - `FulfillmentOrderQueryManager` → **[外部服务]**
    - `HemeraDataManager` → **[外部服务]**
    - `HomeUnifyQueryManager` → **[外部服务]**
    - `PackageConstructionManager` → **[外部服务]**
    - `PackageConstructionQueryManager` → **[外部服务]**
    - `ProjectOrderManager` → **[外部服务]**
    - `ScmManager` → **[外部服务]**
    - `ScmMerchantManager` → **[外部服务]**
    - `ScmProductManager` → **[外部服务]**
    - `ScmSupplierManager` → **[外部服务]**
    - `ScmTmsManager` → **[外部服务]**
    - `SubOrderManager` → **[外部服务]**
    - `SupplierManager` → **[外部服务]**
    - `WorkBenchReadManager` → **[外部服务]**

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `material_task_node` | SELECT/UPDATE |
| `n_material_task_cfg` | SELECT/UPDATE |
| `material_task` | SELECT/UPDATE |
| `design_review_alteration` | SELECT/UPDATE |
| `notice_time_history` | SELECT/UPDATE |
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `supplier_sync_info` | SELECT/UPDATE |

### 外部服务调用

以下调用通过 Feign 访问其他微服务，数据库表不可见：
- `BimKnowledgeManager`
- `CeresManager`
- `FulfillmentLinkRelationManager`
- `FulfillmentOrderQueryManager`
- `HemeraDataManager`
- `HomeUnifyQueryManager`
- `PackageConstructionManager`
- `PackageConstructionQueryManager`
- `ProjectOrderManager`
- `ScmManager`
- `ScmMerchantManager`
- `ScmProductManager`
- `ScmSupplierManager`
- `ScmTmsManager`
- `SubOrderManager`
- `SupplierManager`
- `WorkBenchReadManager`


---

# DispatchTaskTraceController
**基路径**: `/dispatchTask/trace`
**文件**: `/edar-starlord-web/src/main/java/com/ke/utopia/web/trace/DispatchTaskTraceController.java`

---

## POST `/dispatchTask/trace/queryProjectTaskNodeProgress`

**返回类型**: `ResultDTO<MaterialProgressQueryResultDTO>`

**调用链**:
  - `TaskNodeProgressService.queryProjectTaskNodeProgress()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskNodeProgressDao` → `task_node_progress`
    - `TaskDispatchDao` → `task_dispatch`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_node_progress` | SELECT/UPDATE |

### 返回字段 ↔ 数据库列映射

| 返回字段 | 类型 | 来源表 | 数据库列 | 字段说明 |
|----------|------|--------|----------|----------|
| `projectOrderId` | Long | `task_dispatch` | `project_order_id` |  |
| `supplierCode` | String | `task_dispatch` | `supplier_code` |  |
| `supplierName` | String | `task_dispatch` | `supplier_name` |  |
| `materialCode` | String | `task_dispatch` | `material_code` |  |
| `materialName` | String | `task_dispatch` | `material_name` |  |
| `orderNo` | String | `task_dispatch` | `order_no` |  |
| `materialProgressList` | List<ProjectTaskNodeProgressDTO> | `` | `` |  |

---

## POST `/dispatchTask/trace/batchSubmit`

**返回类型**: `ResultDTO<List<Long>>`

**调用链**:
  - `TaskNodeProgressService.batchSaveTaskNodeProgress()`
    - `TaskDispatchNodeDao` → `task_dispatch`
    - `TaskDispatchNodeDao` → `task_dispatch_node`
    - `TaskNodeProgressDao` → `task_node_progress`
    - `TaskDispatchDao` → `task_dispatch`

### 涉及数据表

| 表名 | 操作 |
|------|------|
| `task_dispatch` | SELECT/UPDATE |
| `task_dispatch_node` | SELECT/UPDATE |
| `task_node_progress` | SELECT/UPDATE |


---

# 📚 数据字典
| # | 表名 | DAO 接口 | Mapper 命名空间 | 映射列数 |
|---|------|----------|----------------|----------|
| 1 | `call_record` | `CallRecordDao` | `CallRecordMapper` | 25 |
| 2 | `cfg_material_task_route` | `CfgMaterialTaskRouteDao` | `CfgMaterialTaskRouteMapper` | 7 |
| 3 | `cfg_measure_attr_used` | `CfgMeasureAttrUsedDao` | `CfgMeasureAttrUsedMapper` | 5 |
| 4 | `cfg_measure_template` | `CfgMeasureTemplateDao` | `CfgMeasureTemplateMapper` | 12 |
| 5 | `coordinator_task_order` | `CoordinatorTaskOrderDao` | `CoordinatorTaskOrderMapper` | 33 |
| 6 | `coordinator_task_progress` | `CoordinatorTaskProgressDao` | `CoordinatorTaskProgressMapper` | 16 |
| 7 | `delay_reason_recovery_record` | `DelayReasonRecoveryRecordDao` | `DelayReasonRecoveryRecordMapper` | 17 |
| 8 | `delivery_flow_rule_category` | `DeliveryFlowRuleCategoryDao` | `DeliveryFlowRuleCategoryMapper` | 18 |
| 9 | `delivery_flow_rule` | `DeliveryFlowRuleDao` | `DeliveryFlowRuleMapper` | 26 |
| 10 | `delivery_flow_rule_unit` | `DeliveryFlowRuleUnitDao` | `DeliveryFlowRuleUnitMapper` | 15 |
| 11 | `design_review_alteration` | `DesignReviewAlterationDao` | `DesignReviewAlterationMapper` | 8 |
| 12 | `event_pub` | `EventPubDao` | `EventPubExtMapper` | 13 |
| 13 | `material_delay_process_log` | `MaterialDelayProcessLogDao` | `MaterialDelayProcessLogMapper` | 7 |
| 14 | `material_delay_process` | `MaterialDelayProcessDao` | `MaterialDelayProcessMapper` | 25 |
| 15 | `material_delay_process_reason` | `MaterialDelayProcessReasonDao` | `MaterialDelayProcessReasonMapper` | 12 |
| 16 | `material_flow_rule_category` | `MaterialFlowRuleCategoryDao` | `MaterialFlowRuleCategoryMapper` | 17 |
| 17 | `material_flow_rule` | `MaterialFlowRuleDao` | `MaterialFlowRuleMapper` | 9 |
| 18 | `material_flow_rule_template_mapping` | `MaterialFlowRuleTemplateMappingDao` | `MaterialFlowRuleTemplateMappingMapper` | 7 |
| 19 | `material_flow_rule_template_record` | `MaterialFlowRuleTemplateRecordDao` | `MaterialFlowRuleTemplateRecordMapper` | 9 |
| 20 | `material_flow_rule_unit` | `MaterialFlowRuleUnitDao` | `MaterialFlowRuleUnitMapper` | 15 |
| 21 | `material_payment_intercept_config` | `MaterialPaymentInterceptConfigDao` | `MaterialPaymentInterceptConfigMapper` | 11 |
| 22 | `material_task` | `MaterialTaskDao` | `MaterialTaskMapper` | 21 |
| 23 | `material_task_node` | `MaterialTaskNodeDao` | `MaterialTaskNodeMapper` | 19 |
| 24 | `material_task_node_time` | `MaterialTaskNodeTimeDao` | `MaterialTaskNodeTimeMapper` | 11 |
| 25 | `material_task_node_time_relation` | `MaterialTaskNodeTimeRelationDao` | `MaterialTaskNodeTimeRelationMapper` | 12 |
| 26 | `measure_apply` | `MeasureApplyDao` | `MeasureApplyMapper` | 12 |
| 27 | `measure_apply_operate_log` | `MeasureApplyOperateLogDao` | `MeasureApplyOperateLogMapper` | 14 |
| 28 | `measure_apply_range_type_config` | `MeasureApplyRangeTypeConfigDao` | `MeasureApplyRangeTypeConfigMapper` | 11 |
| 29 | `material_measure_interface_config` | `MeasureConfigRuleDao` | `MeasureConfigRuleMapper` | 8 |
| 30 | `material_measure_interface_config_category_rel` | `MeasureConfigRuleDao` | `MeasureConfigRuleMapper` | 8 |
| 31 | `material_measure_interface_config_mdm_rel` | `MeasureConfigRuleDao` | `MeasureConfigRuleMapper` | 8 |
| 32 | `measure_material_detail` | `MeasureMaterialDetailDao` | `MeasureMaterialDetailMapper` | 8 |
| 33 | `measure_material_unit` | `MeasureMaterialUnitDao` | `MeasureMaterialUnitMapper` | 11 |
| 34 | `message_send_job` | `MessageSendJobDao` | `MessageSendJobMapper` | 12 |
| 35 | `n_holiday_cfg_detail` | `NHolidayCfgDetailDao` | `NHolidayCfgDetailMapper` | 17 |
| 36 | `n_holiday_cfg` | `NHolidayCfgDao` | `NHolidayCfgMapper` | 8 |
| 37 | `n_material_branch_condition` | `NMaterialBranchConditionDao` | `NMaterialBranchConditionMapper` | 13 |
| 38 | `n_material_branch` | `NMaterialBranchDao` | `NMaterialBranchMapper` | 11 |
| 39 | `n_material_cfg_log` | `NMaterialCfgLogDao` | `NMaterialCfgLogMapper` | 12 |
| 40 | `n_material_define` | `NMaterialDefineDao` | `NMaterialDefineMapper` | 18 |
| 41 | `n_material_edge` | `NMaterialEdgeDao` | `NMaterialEdgeMapper` | 10 |
| 42 | `n_material_node_cfg` | `NMaterialNodeCfgDao` | `NMaterialNodeCfgMapper` | 23 |
| 43 | `n_material_node_transfer_condition` | `NMaterialNodeTransferConditionDao` | `NMaterialNodeTransferConditionMapper` | 19 |
| 44 | `n_material_period_calculate_hit_craft_cfg` | `NMaterialPeriodCalculateHitCraftCfgDao` | `NMaterialPeriodCalculateHitCraftCfgMapper` | 8 |
| 45 | `n_material_period_calculate_hit_house_cfg` | `NMaterialPeriodCalculateHitHouseCfgDao` | `NMaterialPeriodCalculateHitHouseCfgMapper` | 7 |
| 46 | `n_material_process_define` | `NMaterialProcessDefineDao` | `NMaterialProcessDefineMapper` | 28 |
| 47 | `n_material_process_template` | `NMaterialProcessTemplateDao` | `NMaterialProcessTemplateMapper` | 8 |
| 48 | `n_material_push_cfg` | `NMaterialPushCfgDao` | `NMaterialPushCfgMapper` | 17 |
| 49 | `n_material_route` | `NMaterialRouteDao` | `NMaterialRouteMapper` | 15 |
| 50 | `n_material_route_time` | `NMaterialRouteTimeDao` | `NMaterialRouteTimeMapper` | 11 |
| 51 | `n_material_task_cfg` | `NMaterialTaskCfgDao` | `NMaterialTaskCfgMapper` | 26 |
| 52 | `n_material_task_process_template` | `NMaterialTaskProcessTemplateDao` | `NMaterialTaskProcessTemplateMapper` | 16 |
| 53 | `n_material_template_copy_log` | `NMaterialTemplateCopyLogDao` | `NMaterialTemplateCopyLogMapper` | 7 |
| 54 | `n_material_template` | `NMaterialTemplateDao` | `NMaterialTemplateMapper` | 13 |
| 55 | `n_material_template_unit` | `NMaterialTemplateUnitDao` | `NMaterialTemplateUnitMapper` | 22 |
| 56 | `n_material_time_calculate_rule_cfg` | `NMaterialTimeCalculateRuleCfgDao` | `NMaterialTimeCalculateRuleCfgMapper` | 12 |
| 57 | `n_material_time_cfg` | `NMaterialTimeCfgDao` | `NMaterialTimeCfgMapper` | 21 |
| 58 | `n_material_time_relation` | `NMaterialTimeRelationDao` | `NMaterialTimeRelationMapper` | 16 |
| 59 | `notice_task_info` | `NoticeTaskInfoDao` | `NoticeTaskInfoMapper` | 11 |
| 60 | `notice_time_history` | `NoticeTimeHistoryDao` | `NoticeTimeHistoryMapper` | 14 |
| 61 | `oms_message_sync` | `OmsMessageSyncDao` | `OmsMessageSyncMapper` | 18 |
| 62 | `operation_audit` | `OperationAuditDao` | `OperationAuditMapper` | 16 |
| 63 | `operation_log` | `OperationLogDao` | `OperationLogMapper` | 12 |
| 64 | `project_info` | `ProjectInfoDao` | `ProjectInfoMapper` | 19 |
| 65 | `refresh_data_log` | `RefreshDataLogDao` | `RefreshDataLogMapper` | 14 |
| 66 | `retry_delay_queue` | `RetryDelayQueueDao` | `RetryDelayQueueMapper` | 11 |
| 67 | `self_buy_remind_record` | `SelfBuyRemindRecordDao` | `SelfBuyRemindRecordMapper` | 5 |
| 68 | `stock_up_condition_rule` | `StockUpConditionRuleDao` | `StockUpConditionRuleMapper` | 14 |
| 69 | `stock_up` | `StockUpDao` | `StockUpExtMapper` | 0 |
| 70 | `supplier_sync_info` | `SupplierSyncInfoDao` | `SupplierSyncInfoMapper` | 15 |
| 71 | `task_batch_node_relation` | `TaskBatchNodeRelationDao` | `TaskBatchNodeRelationMapper` | 6 |
| 72 | `task_create_fail_reason` | `TaskCreateFailReasonDao` | `TaskCreateFailReasonMapper` | 14 |
| 73 | `task_dispatch_extend` | `TaskDispatchendDao` | `TaskDispatchExtendMapper` | 12 |
| 74 | `task_dispatch` | `TaskDispatchDao` | `TaskDispatchMapper` | 39 |
| 75 | `task_dispatch_node_delay_reason` | `TaskDispatchNodeDelayReasonDao` | `TaskDispatchNodeDelayReasonMapper` | 18 |
| 76 | `task_dispatch_node` | `TaskDispatchNodeDao` | `TaskDispatchNodeMapper` | 45 |
| 77 | `task_dispatch_node_report_relation` | `TaskDispatchNodeReportRelationDao` | `TaskDispatchNodeReportRelationMapper` | 12 |
| 78 | `task_handle_extension` | `TaskHandleensionDao` | `TaskHandleExtensionMapper` | 13 |
| 79 | `task_node_progress` | `TaskNodeProgressDao` | `TaskNodeProgressMapper` | 20 |
| 80 | `task_process_batch` | `TaskProcessBatchDao` | `TaskProcessBatchMapper` | 19 |
| 81 | `task_select_category` | `TaskSelectCategoryDao` | `TaskSelectCategoryMapper` | 8 |
| 82 | `thirdparty_data` | `ThirdpartyDataDao` | `ThirdpartyDataMapper` | 7 |
**总计**: 82 张表