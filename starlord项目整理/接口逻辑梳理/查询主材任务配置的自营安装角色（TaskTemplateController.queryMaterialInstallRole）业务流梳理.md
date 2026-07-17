---
source: code-flow
date: 2026-07-17
class: com.ke.utopia.web.TaskTemplateController
method: queryMaterialInstallRole
---

## 功能概述

根据 SKU、店铺、项目业务类型等参数，查询主材任务配置表中对应物料的安装角色（roleCode），决定每个 SKU 应该由什么工人角色安装。查询时按"履约分公司 + 供应商"的匹配粒度逐级降级兜底（指定分公司指定供应商 → 指定分公司不限供应商 → 不限分公司指定供应商 → 不限分公司不限供应商）。

## 入口

- **入口方法**: `TaskTemplateController#queryMaterialInstallRole(List<MaterialInstallRoleQueryParam> paramList)`
- **HTTP 端点**: `POST /material-task/install-role/list`
- **请求体**: `List<MaterialInstallRoleQueryParam>` — 每个元素包含 key / skuId / merchantId / projectBusiness / projectOrderId / productItemCode 等
- **响应**: `ResultDTO<List<MaterialInstallRoleDTO>>` — 每个元素对应一个入参，包含 roleCode / processCode / materialCode / supplierCode 等
- **关键注解**: 无 `@Transactional` — 纯查询接口，无写入

> 一句话：根据 SKU 和店铺信息，从主材任务配置表中查出每个物料的安装角色（由什么工人安装），支持多维度逐级降级匹配。

---

## queryMaterialInstallRole 处理逻辑

### 前置准备（查 SKU 详情 + 查店铺信息）

- **queryMerchantSkuDetailFromParam(paramList)**
  - 按 merchantId 分组入参，收集每个店铺的所有 skuId
  - 批量调用 `scmProductManager.batchQuerySkuForAddMaterialSubentryV2()`
  - 返回每个 SKU 的履约类目、供应商等信息
- **queryMerchantMapFromParam(paramList)**
  - 收集入参中所有 merchantId（去重）
  - 批量调用 `scmManager.selectListByParam()`
  - 返回 merchantId → MerchantDto 的 Map

> 一句话：先批量查 SKU 详情和店铺信息作为后续匹配的基础数据。

### 分组 + 并行查安装角色（核心逻辑）

**queryInstallRoleFromTaskTemplate(merchantIdMap, skuList, params)**

- **assembleParamList()** — 给每个入参追加履约分公司 mdmCode
- **构建 merchantAndSkuMap**（merchantId_skuId → SkuInfo）
- **按 (projectBusiness, mdmCode) 分组** — 每组入参属于同一业务类型 + 同一履约分公司

**对每个分组（异步线程内）：**

- **getTaskMode()** — 根据业务类型确定任务模式（HOME2.5 / HOME2.5_MANPOWER / DELIVERY_FLOW）
- **getSaleTypeByProjectBusiness()** — 获取销售类型
- **getPerformanceCategoryIdListFromParam()** — 获取组内履约类目
- **并行两个查询：**
  - `queryInstallRole(mdmCode, ...)` — **限制履约分公司**的配置
  - `queryInstallRole(NO_LIMIT, ...)` — **不限履约分公司**的配置（仅 HOME2.5 模式需要，其他直接空 Map）

> 一句话：按照业务类型和履约分公司分组，并行查询限制分公司和不限制分公司的配置结果。

### 逐条匹配安装角色（四级降级策略）

**对分组内每个入参循环：**

- 组装 dto 基础信息（key, skuId, merchantId, mode, saleType）
- merchantAndSkuMap 中找不到 SKU？→ result.add(dto) + continue
- 从 SKU 信息提取 supplierCode + performanceCategoryId
- 组装两个 key：
  - `materialSupplier` = performanceCategoryId + supplierCode（限定供应商）
  - `materialNoLimitSupplier` = performanceCategoryId + "9999999"（不限供应商）

**四级降级匹配（按优先级）：**

```
L1: installRoleMapFuture.get(materialSupplier)
    → 指定履约分公司 + 指定供应商
    存在→ "匹配到指定履约分公司 + 指定供应商的配置"

L2: installRoleMapFuture.get(materialNoLimitSupplier)
    → 指定履约分公司 + 不限制供应商
    存在→ "匹配到指定履约分公司 + 不限制供应商的配置"

L3: mdmNoLimitInstallRoleMapFuture.get(materialSupplier)
    → 不限制履约分公司 + 指定供应商
    存在→ "匹配到不限制履约分公司 + 指定供应商的配置"

L4: mdmNoLimitInstallRoleMapFuture.get(materialNoLimitSupplier)
    → 不限制履约分公司 + 不限制供应商
    存在→ "匹配到不限制履约分公司 + 不限制供应商的配置"
```

- 全部无匹配？→ matchNote 为"未匹配到任何安装类型的主材任务配置"
- 匹配成功 → 赋值 roleCode + processCode 并加入 result

> 一句话：对每个 SKU 按四级优先级依次尝试匹配安装角色，从最精确的分公司+供应商配置一直降级到不限分公司+不限供应商。

### 特殊处理（北京室内门/金属门历史数据兼容）

**specialHandlerResult(result)**

只针对 `029006006`（室内门）/ `029006007`（金属门）两个类目：

- projectOrderId 为空？→ 跳过
- **检查项目分公司**：V201601528（北京A）或 C200503113（北京B）？
  - 是 → 查该项目的施工包（`packageConstructionQueryManager`）
    - 有木门工施工包（CC018）？→ roleCode 强设为 [14]（木门工）
    - 无木门工包 → 不做特殊处理
  - 否 → 不做特殊处理
- **项目级缓存**：已查过的项目结果缓存到 `projectOrderResultMap`，避免重复查项目

> 一句话：北京的室内门和金属门历史数据特殊兼容——如果项目存在木门工施工包，强制返回木门工角色（roleCode=14）。

---

## 核心子逻辑

### queryInstallRole — 从主材任务配置中查询安装角色

```
根据条件查询任务模板 → 提取 START 节点的角色列表
```

- **组装查询参数 ListTemplateParam**：
  - mode = taskMode.getValue()
  - stateIn = [1]（只查生效的）
  - taskType = INSTALL（只查安装类型的模版）
  - mdmCode = 传入的履约分公司（或 "9999999" 不限）
  - materialCodes = 待查询的履约类目列表
  - saleType = 销售类型（非 HOME2.5 才传）
  - deliveryProcessCfgId = 排程模式下查配送流程配置
- **querySimpleTaskTemplate()** — 分页查询模板列表（一直翻页直到 currentPage >= totalPage）
- **遍历模板列表**，对每个模板的节点列表：
  - 找到 nodeType = START（启动类型）的节点
  - installRoleMap.put(materialCode_supplierCode, (template, roleTypeList))
- **返回** `Map<String, Pair<TaskTemplateDTO, List<Integer>>>`

> 一句话：按条件分页查询生效的安装类型任务模板，提取每个模板中启动节点的角色列表，组装成 materialCode+supplierCode 为 key 的 Map。

### getTaskMode — 根据业务类型确定任务模式

按优先级依次判断：

| 条件 | 结果 |
|------|------|
| RETAIL_2.0 / WHOLE_2.0 | HOME2.5 |
| 排程模式开启（materialScheduleSwitch） | DELIVERY_FLOW |
| 北京分公司 | HOME2.5 |
| WHOLE_2.5 / RETAIL_2.5 / QUAN_2.5 | HOME2.5_MANPOWER |
| GROUP_WHOLE_2.5 / GROUP_QUAN_2.5 | HOME2.5_MANPOWER |
| RENOVATE_2.5 | HOME2.5_MANPOWER |
| 其他 | 抛异常"不支持的项目版本类型" |

> 一句话：根据项目业务类型和区域判断属于哪种任务模式——2.0 全部走 HOME2.5，满足排程条件的走 DELIVERY_FLOW，2.5 系列走 HOME2.5_MANPOWER。

### getSaleTypeByProjectBusiness — 获取销售类型

| 条件 | 结果 |
|------|------|
| HOME2_5 模式 | null |
| WHOLE_2.0 / WHOLE_2.5 | PACKAGE_TIED（套餐捆绑） |
| RETAIL_2.0 / RETAIL_2.5 | RETAIL（零售） |
| QUAN_2.5 | FINE_TYPE（精装） |
| GROUP_WHOLE_2.5 | TUAN（团装） |
| GROUP_QUAN_2.5 | TUAN_RETAIL（团装零售） |
| RENOVATE_2.5 | PARTIAL_DECORATION（局装） |
| 未知 | null |

---

## 条件分支汇总

| 位置 | 条件 | 走向 | 结果 |
|------|------|------|------|
| 分组循环 | HOME2.5 模式 | 额外创建 mdmNoLimit 异步查询 | 查不限分公司的配置用于降级 |
| 分组循环 | 非 HOME2.5 模式 | mdmNoLimitFuture = 空 Map | 不查不限分公司的配置 |
| SKU 匹配 | merchantAndSkuMap 无此 Key | 跳过匹配，直接加 dto | roleCode 为空 |
| 降级匹配 L1 | installRoleMap 命中指定分公司+指定供应商 | 取该角色 | matchNote=指定分公司+指定供应商 |
| 降级匹配 L2 | installRoleMap 命中指定分公司+不限供应商 | 取该角色 | matchNote=指定分公司+不限供应商 |
| 降级匹配 L3 | mdmNoLimit 命中不限分公司+指定供应商 | 取该角色 | matchNote=不限分公司+指定供应商 |
| 降级匹配 L4 | mdmNoLimit 命中不限分公司+不限供应商 | 取该角色 | matchNote=不限分公司+不限供应商 |
| 全未匹配 | 四级都未命中 | 直接加 dto（roleCode 空） | 附"未匹配到任何安装类型" |
| 特殊处理 | 室内门/金属门 + 北京项目 + 有木门工施工包 | roleCode 强设为 [14] | 兼容北京历史数据 |
| 特殊处理 | 项目已查过且命中 | 直接从缓存取结果 | 减少外部调用 |

---

## 数据查询汇总

| 查询来源 | 查询条件 | 预期返回 | 空结果处理 |
|----------|---------|---------|-----------|
| scmProductManager.batchQuerySkuForAddMaterialSubentryV2 | merchantId + skuIds | List<SkuInfoForAddMaterialSubentryDto> | → SKU 找不到则匹配降级跳过 |
| scmManager.selectListByParam | merchantId 列表 | List<MerchantDto> | → merchantIdMap 缺失，assembleParamList 抛空指针 |
| taskTemplateService.pageListTemplate4Role | mode / mdmCode / materialCodes / taskType=INSTALL / state=生效 / saleType / deliveryProcessCfgId | Page<TaskTemplateDTO> | → installRoleMap 为空，所有 SKU 匹配不到 |
| deliveryProcessCfgManager.queryDeliveryProcess | projectOrderId | DeliveryProcessDTO | → 不设置 deliveryProcessCfgId |
| projectOrderManager.getByProjectOrderId | projectOrderId | ProjectOrderDto | → 特殊处理跳过该 SKU |
| packageConstructionQueryManager.queryPackageConstruction | projectOrderId | List<PackageConstructionDTO> | → 木门工施工包不存在则不改 roleCode |

---

## 状态变更

无数据库写入操作 — 纯查询接口，不涉及状态变更。

---

## 外部调用和事件

| 类型 | 目标 | 时机 | 用途 |
|------|------|------|------|
| Feign | scmManager.selectListByParam | 前置准备 | 批量查店铺信息 |
| Feign | scmProductManager.batchQuerySkuForAddMaterialSubentryV2 | 前置准备 | 批量查 SKU 履约类目和供应商 |
| Feign | taskTemplateService.pageListTemplate4Role | 分组内 | 分页查主材任务模板（含节点） |
| Feign | deliveryProcessCfgManager.queryDeliveryProcess | 排程模式 | 查配送流程配置 ID |
| Feign | projectOrderManager.getByProjectOrderId | 特殊处理 | 查项目订单详情（判断分公司） |
| Feign | packageConstructionQueryManager.queryPackageConstruction | 特殊处理 | 查项目施工包（看是否有木门工包） |

---

## 待澄清点

- [ ] `specialHandlerResult` 中北京室内门/金属门的兼容逻辑硬编码了两个分公司 code，后续新增北京分公司需要同步修改
- [ ] `NO_LIMIT_VALUE = "9999999"` 作为"不限"的魔数，入参的 supplierCode 能否与之冲突？
- [ ] 非 HOME2.5 模式下 `mdmNoLimitInstallRoleMapFuture` 直接 `completedFuture(new HashMap())`，如果 DELIVERY_FLOW 模式也需要不限分公司的降级则四级降级策略实际退化为两级
- [ ] `querySimpleTaskTemplate` 不分页上限，如果配置模板数量巨大，while(true) 循环可能 OOM 或耗时过长

---

## 总结

**主干路径**: 批量查 SKU 详情 + 批量查店铺信息 → 按 projectBusiness+mdmCode 分组 → 每组并行查"限制分公司"和"不限分公司"的安装角色配置 → 逐 SKU 按四级降级匹配安装角色 → 北京门类特殊兼容处理 → 返回结果

**架构特点**:
1. **纯查询** — 无写入、无事务、无状态变更，Feign 查询全部是读取
2. **异步并行** — 使用 asyncQueryExecutor 线程池并行查询限制分公司和不限制分公司两个维度的配置
3. **四级降级匹配** — 从最精确的履约分公司+供应商，逐步退到最宽松的不限分公司+不限供应商
4. **历史兼容** — specialHandlerResult 专门处理北京的室内门和金属门旧数据，缓存项目级查询结果减少重复调用
5. **分页保障** — querySimpleTaskTemplate 用 while 循环确保超过一页也能全部查完
