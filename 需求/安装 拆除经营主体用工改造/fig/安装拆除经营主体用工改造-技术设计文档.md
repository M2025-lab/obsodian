# 安装拆除经营主体用工改造-技术设计文档

## 1、需求背景

需求wiki：`/Users/mirror/wiki/需求/安装 拆除经营主体用工改造/【待评审0707】安装_拆除经营主体用工改造.md`

安装、拆除工从原有整装组织体系独立出列，新增拆除项目经理、安装项目经理以个体工商户为全新经营主体，专项承接公司安装业务，并全权负责统筹管理工人团队。组织架构与工班归属关系发生变化，原用工流程中涉及上级审批的环节需更新查询逻辑，派单流程需适配新增确认环节。

涉及业态：整装/搭售、翻新、零售。

---

## 2、需求理解

### 2.1 RD角度需求理解

本期需求可拆解为四个独立改造模块：

| 模块 | 需求点 | 改造说明 |
|------|--------|---------|
| 开关配置 | 组织城市 + 工种维度开关 | 安装和拆除节奏不一致，需按城市+工种控制功能生效范围 |
| 作业端改造 | 精工端登录&页面权限、查询上级逻辑 | 新增安装项目经理/拆除项目经理角色；问题反馈、约工驳回、整改费用审批、请假的审批人查询逻辑从"组织树直属上级"改为"工人长期合作关系项目经理" |
| 用工流程改造 | 派单新增确认流程、工程量确认 | 安装/拆除派单新增项目经理确认流程，派单任务「待接单」拆分为二级状态机；施工包新增字段「班组归属项目经理」；安装工程量确认查询&展示 |
| 刷数&文案 | 存量数据刷数、"直营"改"合营" | 开城前存量施工包和派单任务刷数；文案统一替换 |

### 2.2 引申需求

1. 派单状态机从一级状态拆分为二级状态，`DispatchStatusEnum` 和 `PackageLoanTask.dispatchStatus` 的改造会影响 minerva 派单回调链路，需同步确认 minerva 侧配合点。
2. 施工包新增"班组归属体外项目经理"字段，现状该信息通过 ceres 的 `IndustryGroupDTOV2.belongForemanUcId` 动态获取，不持久化。本期需新增 DB 字段并在派单/改派时覆写。
3. "班组请假"审批代码在 cube 仓库内未检索到实现，可能位于 workcenter 作业中心服务，需跨服务确认。

### 2.3 内容要求

- 新增角色：安装项目经理、拆除项目经理
- 新增状态：派单任务「待接单-待上级确认」「待接单-待工人确认」
- 新增字段：`package_construction` 表新增 "班组归属体外项目经理"
- 新增接口：经理确认派单、经理拒单、工程量确认查询、派单待确认列表
- 刷数范围：施工包类型 = 定制家具安装工/橱柜工/拆除工，且状态 <= 待派单
- 文案替换：全仓 14 处 "直营" 改为 "合营"

---

## 3、概要设计

### 3.0 设计思路与折衷

**关键设计决策：**

1. **状态机拆分方式**：`DispatchStatusEnum` 新增两个二级状态编码，复用现有 `PackageLoanTask.dispatchStatus` 字段存储，不新增字段。minerva 回调时通过状态编码区分处理节点。
2. **上级查询逻辑统一收口**：问题反馈、约工驳回、费用一级审批三处当前均调用 `PersonManager.querySuperiorByWorkType()`，本期统一改造为查询工人长期合作关系项目经理（通过 `KirinManager.longTermWorker()`）。
3. **"班组归属体外项目经理"持久化**：现状动态查询，本期新增 `package_construction` 字段，在派单/改派/返补完成时覆写，避免每次查询都调 ceres。

### 3.1 系统架构

```
                         精工端 App
   (工人/安装项目经理/拆除项目经理/安装主管)
              |
              v
        utopia-cube
  +-----------+-----------+-------------------+
  | Controller|  Service  |      Manager      |
  | (web/api) | (业务逻辑) | (ceres/minerva/   |
  |           |          |  kirin/acceptance) |
  +-----------+-----------+-------------------+
  |    DAO    |   定时任务  |
  | (MyBatis) | (30min自动确认)|
  +-----------+-----------+
              |
    +---------+---------+
    v         v         v
  ceres    minerva    kirin
(人员/组织) (派单平台) (长期合作)
```

**上下游依赖：**

| 系统 | 交互内容 | 备注 |
|------|---------|------|
| ceres（服务者中心） | 人员岗位查询、组织树查询、长期合作关系查询 | — |
| minerva（派单平台） | 派单需求发起、派单回调、状态同步 | 需对齐状态编码 |
| kirin（麒麟） | 长期合作工人/班组查询 | `KirinManager.longTermWorker()` |
| 精工端 | 角色登录、施工包列表、确认/拒单操作 | — |
| workcenter（作业中心） | 状态机流转、班组请假（待确认是否在workcenter） | 需跨服务确认 |

### 3.2 接口列表

以下接口来自 wiki 补充记录和 PRD 中明确涉及改造的部分。

#### 3.2.1 派单页面查询（变更）

**接口1：项目经理施工包列表查询**

- 接口描述：查询项目经理下所有施工包，精工端工地页数据源。记录中明确提到 `/utopia-cube/web/list-package-by-operator-manager`。**变更点**：新增对 `packageSecondStatus` 筛选的支持，新增「派单待确认」二级状态过滤（type=3 新增值 "31"、"32"）；按钮构建 `buildButtonInfo()` 新增确认/拒单按钮
- 接口路径：`POST /web/list-package-by-operator-manager`
- Controller：`PcController.listPackageConstructionByManager()`
- Service：`WorkerServiceImpl.listPackageConstructionByManager()`

入参 `PackageListParam`：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageStatus | Integer | 否 | 施工包状态（300-待进场, 400-施工中, 600-完成） |
| currentPage | Integer | 否 | 当前页码 |
| pageSize | Integer | 否 | 每页数量 |
| keyword | String | 否 | 搜索关键字 |
| extraFieldTypeInfoList | List\<ExtraFieldTypeInfo\> | 否 | 主管查看字段筛选条件 |

`ExtraFieldTypeInfo`：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| type | Integer | 字段类型（1-是否有问题单, 2-是否有在审核约工驳回申请单, **3-施工包二级状态（变更：新增31/32值）**, 4-额外项审核） |
| valueList | List\<String\> | 字段值列表 |

正常返回：`ResultVO<WorkerTaskResultVo>`

`WorkerTaskResultVo`：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| count | Integer | 施工包数量 |
| aggregationVOList | List\<AggregationDTO\> | 状态聚合统计 |
| workerTaskVOList | Page\<WorkerTaskVO\> | 任务列表（分页） |

`WorkerTaskVO` 核心字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| packageCode | String | 施工包单号 |
| packageId | Long | 施工包主键 |
| packageStatus | Integer | 施工包状态 |
| packageSecondStatus | Integer | 施工包二级状态（**变更：新增31/32值**） |
| packageType | Integer | 施工包类型 |
| workerType / workerTypeName | String | 工种code/名称 |
| homeownerName | String | 业主姓名 |
| address | String | 项目地址 |
| foremanName / foremanPhone | String | 项目经理姓名/电话 |
| tag | String | 标签（**变更：新增"待确认"橙色标签**） |
| buttonInfoList | List\<PackageButtonInfo\> | 操作按钮（**变更：新增confirm/reject按钮**） |
| packageProblemForm | Integer | 问题审核单数量 |
| packageReappointProblemForm | Integer | 约工驳回审核单数量 |
| supportReappoint | Integer | 是否支持约工驳回（1-支持, 2-不支持） |
| appointTime / appointEndTime | Date | 预约工期开始/结束时间 |
| projectPhase | Integer | 项目阶段（1-装中, 2-售后） |

**待办**：
- 佳熹哥提供触发 ES 更新的接口（传入施工包code）
- 皓雪姐提供反查派单平台接口，拿到二级状态 `packageSecondStatus`，完成数据库更新

---

#### 3.2.2 约工驳回（cube侧无改造）

**接口2：约工驳回申请提报**

- 接口描述：约工驳回申请提报（工人发起）。记录中明确提到 `/utopia-cube/web/package/reappointProblemSubmit`
- 接口路径：`POST /web/package/reappointProblemSubmit`
- Controller：`PcReappointController.reappointProblemSubmit()`
- Service：`PcReappointServiceImpl.reappointProblemSubmit()`

**cube侧改造说明**：
- 补充记录明确说明："不需要我修改吧？服务者中心修改，我拿到数据就好"
- 查询上级逻辑由 `PersonManager.querySuperiorByWorkType()` 改为查询工人长期合作关系项目经理，该改动在**服务者中心**完成，cube 消费返回结果
- 需确认服务者中心对接传回的值格式是否有变动

入参 `ProblemSubmitParam`：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageCode | String | 是 | 施工包单号 |
| needEnterAgain | Boolean | 否 | 是否需要二次进场 |
| problemDetailList | List\<ProblemDetail\> | 否 | 问题详情列表 |
| operatorUcid | String | 否 | 操作人ucId |
| operatorName | String | 否 | 操作人名称 |
| packageType | Integer | 否 | 施工包类型 |

正常返回：`ResultDTO<Void>`

---

#### 3.2.3 问题反馈费用提报（变更）

**接口3：费用申请提交**

- 接口描述：问题费用配额申请提交。记录中明确提到 `utopia-cube/api/pc/problem/order/quota/submit`。**变更点**：一级审批人查询逻辑，由 `PersonManager.querySuperiorByWorkType(targetWorkTypeList=[DELIVERY_EXPERT, DELIVERY_INSTALLATION_EXPERT, ERECTOR_MANAGER, ERECTOR_LEADER])` 改为查询工人长期合作关系的安装项目经理
- 接口路径：`POST /api/pc/problem/order/quota/submit`
- Controller：`PcProblemQuotaController.submitQuota()`（实现 `PcProblemQuotaFeign`）
- Service：`PcProblemQuotaServiceImpl.submitQuota()` -> `getFirstApproveUser()`

审批节点改造：

| 流程 | 一级审批 | 二级审批 | 超额审批 |
|------|---------|---------|---------|
| 问题单 | 安装项目经理 | COE | — |
| 费用单 | 安装项目经理 | COE | 上一级审批 |

**待办**：
- 与服务者中心确认查询上级传入和传出的岗位有无变动（工种在 `com.ke.ceres.api.dto.request.SuperiorWorkTypeQueryRequest`）
- `PcProblemQuotaServiceImpl.validateSecondApproval()` 验证超额并查二级审批，需确认改造后逻辑

入参 `PcQuotaApplyParam`：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageCode | String | 是 | 施工包Code |
| workOrderNo | String | 是 | 问题单费用号 |
| quotaType | Integer | 否 | 费用类型（1-返补人工费, 2-现场整改费） |
| quotaTypeName | String | 否 | 费用类型名称 |
| quotaCode | String | 否 | 额外项Code |
| quotaName | String | 否 | 额外项Name |
| quotaUnit | String | 否 | 额外项单位 |
| quotaPrice | BigDecimal | 否 | 额外项金额 |
| quotaDesc | String | 否 | 额外项说明 |
| quotaNum | BigDecimal | 否 | 额外项数量 |
| applyUcId | Long | 否 | 申请人ucId |
| applyName | String | 否 | 申请人姓名 |
| auditUcId | Long | 否 | 审核人ucId |
| auditName | String | 否 | 审核人姓名 |
| applyAuditStatus | Integer | 否 | 审核状态（1-审核通过, 2-审核驳回） |
| images | List\<String\> | 否 | 申请图片 |
| remark | String | 否 | 申请说明 |
| totalAmount | BigDecimal | 否 | 合计金额 |
| auditPass | Boolean | 否 | 是否一级审批通过 |

正常返回：`ResultDTO<Boolean>`

---

#### 3.2.4 派单平台变更通知（新增）

**接口4：派单变更通知**

- 接口描述：派单平台（minerva）通知 cube 某个施工包的派单状态发生了变更。cube 收到通知后，根据施工包code 触发后续处理（更新ES、反查派单平台获取最新状态）。对应待办中"佳熹哥提供触发 ES 更新的接口"
- 接口路径：`POST /web/package/dispatch/changeNotice`（待确认）
- 调用方：minerva 派单平台
- Controller：`PcController.dispatchChangeNotice()`（新增）

入参：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageCode | String | 是 | 施工包code |

正常返回：

```json
{
  "code": 0,
  "data": true,
  "message": "success"
}
```

处理流程：
1. 接收 packageCode，记录变更日志
2. 调用接口5（反查派单平台全量数据）获取最新派单状态
3. 更新 `package_loan_task.dispatchStatus` 及 `package_construction.packageSecondStatus`
4. 触发 ES 数据同步

**待确认**：
- 接口路径是否复用现有 `ProcessController` 还是新增 Controller
- minerva 侧调用时机（派单状态每次流转都通知，还是仅在二级状态变更时通知）

---

#### 3.2.5 反查派单平台全量数据（新增）

**接口5：派单全量数据反查**

- 接口描述：cube 主动反查派单平台（minerva），获取施工包在派单平台的全量数据，包括二级状态 `packageSecondStatus`，用于更新本地数据库。对应待办中"皓雪姐提供反查派单平台接口"
- 接口路径：`POST /web/package/dispatch/queryFullData`（待确认）
- 调用方：cube 内部调用（由接口4变更通知触发，或定时任务补偿调用）
- Service：`DispatchServiceImpl.queryDispatchFullData()`（新增）

入参：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| packageCode | String | 是 | 施工包code |
| 其他参数 | — | — | **待定**，需与 minerva 侧确认 |

正常返回：`ResultDTO<DispatchFullDataVO>`

`DispatchFullDataVO`：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| packageCode | String | 施工包code |
| packageSecondStatus | Integer | 施工包二级状态（31-待上级确认, 32-待工人确认, 其他值待确认） |
| 其他字段 | — | **待定**，需与 minerva 侧确认全量返回字段 |

**待确认**：
- minerva 侧反查接口的实际路径和入参
- 返回的全量字段列表
- 是否需要分页或批量反查能力

---

## 4、详细设计

### 4.1 流程图

#### 4.1.1 派单状态机流转

```
待派单(200)
    |
    | 派单员派单
    v
待接单-待上级确认(NEW)
    |
    |-- 项目经理确认 -----> 待接单-待工人确认(NEW)
    |                        |
    |                        |-- 工人接单 -----> 已接单(2)
    |                        |
    |                        |-- 工人拒单 -----> 待派单(200)
    |
    |-- 项目经理拒单 -----> 待派单(200)
    |
    |-- 30min超时未处理 ---> 待接单-待工人确认(NEW) [自动确认]
```

**改派/返补/批量派单**：
- 改派：无需确认，直接覆写"班组归属体外项目经理"字段
- 返补：同工人自动合包约派，无需确认，覆写字段
- 批量派单：同正向派单，需经理+工人确认

#### 4.1.2 上级查询逻辑改造流程

```
问题反馈/约工驳回/费用一级审批
    |
    | 旧逻辑
    v
PersonManager.querySuperiorByWorkType()
    targetWorkTypeList = [DELIVERY_EXPERT, DELIVERY_INSTALLATION_EXPERT,
                          ERECTOR_MANAGER, ERECTOR_LEADER]
    |
    | 新逻辑
    v
KirinManager.longTermWorker()
    输入：projectOrderId, packageCode, planStartDate, planEndDate,
         packagePlanCode, foremanUcId, workType
    输出：长期合作关系项目经理列表
    |
    v
取第一个项目经理作为审批人
```

**费用二级审批（新增）**：
```
一级审批通过
    |
    v
validateSecondApproval() 判断是否需要二级审批
    |
    |-- 需要 --> 查询安装项目经理所在组织负责人（工程coe）
    |              CeresManager.queryPersonAndOrgTree() -> 取orgCode
    |              -> queryByOrgCodeAndWorkType(orgCode, "工程coe工种编码")
    |
    |-- 不需要 -> 审批完成
```

**费用超出审批（原二级->三级）**：
```
二级审批通过且费用超出
    |
    v
查询上一环节安装coe的直属上级
    旧：queryByOrgCodeAndWorkType(parentOrgCode, BUTLER_MANAGER)
    新：通过组织树查询上一环节coe的直属上级
```

### 4.2 数据表设计

#### 4.2.1 新增字段

**表：`package_construction`**

| 字段名 | 字段含义 | 字段类型 | 默认值 | 说明 |
|--------|---------|---------|--------|------|
| `group_belong_foreman_id` | 班组归属体外项目经理ID | bigint(20) | NULL | 新增，派单/改派时覆写 |
| `group_belong_foreman_name` | 班组归属体外项目经理姓名 | varchar(64) | '' | 新增，派单/改派时覆写 |

```sql
ALTER TABLE `package_construction`
ADD COLUMN `group_belong_foreman_id` bigint(20) DEFAULT NULL COMMENT '班组归属体外项目经理ID',
ADD COLUMN `group_belong_foreman_name` varchar(64) DEFAULT '' COMMENT '班组归属体外项目经理姓名';
```

**注**：`package_loan_task` 表当前通过 `dispatchStatus` 存储派单状态（0待处理/2已接单/3待接单/4已拒单）。本期新增两个二级状态，需确认是：
- 方案A：在 `dispatchStatus` 上新增编码（如 31=待上级确认，32=待工人确认）
- 方案B：新增独立字段存储二级状态

**待确认**：minerva 侧回调时传入的状态值如何映射，需与 minerva 侧对齐。

#### 4.2.2 枚举变更

**`DispatchStatusEnum.java`**

```java
// 现状
NO(0, "待处理"),
ACCEPTED(2, "已接单"),
TO_BE_ACCEPTED(3, "待接单"),
REJECTED(4, "已拒单");

// 新增
TO_BE_MANAGER_CONFIRM(31, "待接单-待上级确认"),
TO_BE_WORKER_CONFIRM(32, "待接单-待工人确认");
```

**`TagEnum.java`**

新增标签：
- `D_P_D_Q_R`（待确认，橙色）—— 派单待确认
- `W_Q_R_G_L`（未确认工程量，红色）—— 工程量未确认
- `Y_Q_R_G_L`（已确认工程量，绿色）—— 工程量已确认

### 4.3 数据规模与性能评估

| 场景 | 数据规模预估 | 性能评估 |
|------|------------|---------|
| 派单待确认列表查询 | 单项目经理下活跃包通常 < 50 条 | 现有 `listPackageConstructionByUcId` 已按 `workerIdIn` 查询，新增过滤条件不影响性能 |
| 30min自动确认定时任务 | 开城城市待确认任务量级待评估 | 建议按城市分片扫描，单次处理量控制在 500 条以内 |
| 长期合作工人查询 | `KirinManager.longTermWorker()` 单次查询 | 外部 Feign 调用，需评估超时和降级策略 |
| 刷数 | 存量待派单施工包量级待业务确认 | 建议分批刷数，每批 1000 条，记录刷数日志 |

### 4.4 特殊功能设计

#### 4.4.1 开关配置

Apollo 配置项（建议新增）：

```properties
# 安装拆除经营主体用工改造开关
# key: 城市code_工种code，value: 1开启 0关闭
install.demolition.worker.reform.switch={"C101_001":"1","C101_002":"1"}

# 派单自动确认超时时间（分钟）
dispatch.manager.auto.confirm.minutes=30
```

**开关判断逻辑**：
```java
private boolean isReformSwitchOn(String branchOfficeCode, String workerType) {
    String key = branchOfficeCode + "_" + workerType;
    // 定制家具安装工、橱柜工、拆除工 且 开城城市
    return reformSwitchMap.getOrDefault(key, "0").equals("1");
}
```

#### 4.4.2 30min自动确认

定时任务配置：
- 任务名称：`DispatchManagerAutoConfirmJob`
- 执行频率：每 5 分钟
- 处理逻辑：扫描 `package_loan_task` 表中 `dispatchStatus = TO_BE_MANAGER_CONFIRM` 且创建时间超过 30min 的记录，自动更新为 `TO_BE_WORKER_CONFIRM`。

#### 4.4.3 Push文案改造

拒单时 push 文案需根据拒单角色动态替换：
- 工人拒单：`工人拒单`
- 项目经理拒单：`项目经理拒单`

Push 发送位置：`DispatchServiceImpl.afterDispatch()` 拒单分支、`PcAppointServiceImpl` 相关通知逻辑。

### 4.5 上线步骤

| 步骤 | 操作内容 | 预期结果 | 回滚方案 |
|------|---------|---------|---------|
| 1 | DDL上线：`package_construction` 新增 `group_belong_foreman_id`、`group_belong_foreman_name` 字段 | 字段创建成功 | `ALTER TABLE DROP COLUMN` |
| 2 | Apollo配置上线：新增开关配置、自动确认超时配置 | 配置生效 | 删除配置项 |
| 3 | 代码上线：utopia-cube 打包发布 | 服务正常启动 | 回滚上一版本 |
| 4 | 前期准备：安装/拆除全切产业模式，开城城市全切平台模式 | 配置完成 | 恢复原有配置 |
| 5 | 存量刷数：执行施工包和派单任务刷数脚本 | 数据更新完成 | 备份后回滚 |
| 6 | 功能验证：派单确认流程、上级查询逻辑、工程量确认 | 流程正常 | — |

### 4.6 数据迁移（刷数）

#### 4.6.1 施工包刷数

**刷数范围**：
1. 施工包类型 = 定制家具安装工、橱柜工、拆除工
2. 施工包类型（shareMode）= 平台模式
3. 施工包状态 <= 待派单（200）
4. 派单任务状态 <= 待派单

**刷数规则**：
1. 施工包：`gradeCategory` 总工制（1）-> 产业制（2）；`shareMode` 普通（1）/拆借（2）-> 平台模式（3）
2. 派单任务：开城时存量「待确认」->「待工人确认」；关闭开城时「待上级确认」和「待工人确认」统一回退为「待确认」

**刷数接口**：在 `RefreshDataController` 新增刷数方法，或复用现有 `POST /update/package` 接口。

### 4.7 风险点

| 风险 | 影响 | 缓解策略 |
|------|------|---------|
| minerva 状态机对接 | 派单状态拆分后，minerva 回调传入的状态值需对齐 | 提前与 minerva 侧对齐状态编码和回调逻辑 |
| "工程coe"角色编码 | `WorkTypeEnum` 中未引用"工程coe"编码，二级审批人查询可能失败 | 与 ceres 侧确认工程coe对应的工种编码 |
| 班组请假代码位置 | cube 内未找到请假审批代码，可能位于 workcenter | 与 workcenter 侧确认改造范围 |
| 长期合作工人查询失败 | `KirinManager.longTermWorker()` 查询不到时阻断派单 | 派单工作台增加校验，查询不到则提示"派单失败，无法获取该班组归属项目经理" |
| 30min自动确认定时任务 | 大规模开城时待确认任务量可能较大 | 定时任务按城市分片，控制单次扫描量 |
| "直营"改"合营"文案 | PRD正文写"合营"，变更记录写"联营"，最终文案待确认 | 与PM确认最终文案后统一替换 |
| 刷数影响范围 | 刷数涉及存量待派单施工包，可能影响在途派单流程 | 刷数前备份数据，选择低峰期执行，刷数后抽样验证 |

---

## 附录：代码改动清单

### utopia-cube-dao

| 文件 | 改动类型 | 改动点 |
|------|---------|--------|
| `model/PackageConstruction.java` | 修改 | + `groupBelongForemanId`、`groupBelongForemanName` |
| `model/PackageLoanTask.java` | 不修改/待确认 | `dispatchStatus` 字段复用或新增字段 |
| `resources/soMapper/PackageConstructionMapper.xml` | 修改 | Base_Column_List 新增字段，新增查询SQL |
| `resources/soMapper/PackageLoanTaskMapper.xml` | 修改 | 新增状态条件查询 |

### utopia-cube-base

| 文件 | 改动类型 | 改动点 |
|------|---------|--------|
| `v2/enums/DispatchStatusEnum.java` | 修改 | + `TO_BE_MANAGER_CONFIRM`、`TO_BE_WORKER_CONFIRM` |
| `v2/enums/TagEnum.java` | 修改 | + 派单待确认、工程量确认标签 |
| `v2/enums/BizModelEnum.java` | 修改 | "直营" -> "合营" |
| `v2/enums/GradeCategoryEnum.java` | 修改 | "直营产业模式" -> "合营产业模式" |

### utopia-cube-service

| 文件 | 改动类型 | 改动点 |
|------|---------|--------|
| `v2/impl/WorkerServiceImpl.java` | 修改 | 新增角色判断、派单待确认tab查询、按钮/标签构建 |
| `v2/impl/PcAppointServiceImpl.java` | 修改 | 派单后状态流转逻辑 |
| `process/impl/DispatchServiceImpl.java` | 修改 | 派单回调状态机处理、经理确认/拒单 |
| `v2/impl/PcReconstructionServiceImpl.java` | 修改 | problemSubmit 上级查询逻辑 |
| `v2/impl/PcProblemQuotaServiceImpl.java` | 修改 | 一/二/三级审批人查询逻辑 |
| `v2/impl/PcReappointServiceImpl.java` | 修改 | reappointProblemSubmit 上级查询逻辑 |
| `v2/impl/PcBatchOperateServiceImpl.java` | 修改 | 批量派单确认流程 |
| `helper/PackageShareModeService.java` | 修改 | 拆除/安装包强制平台模式 |

### utopia-cube-manager

| 文件 | 改动类型 | 改动点 |
|------|---------|--------|
| `KirinManager.java` | 不修改（复用） | `longTermWorker()` 复用现有方法 |
| `PersonManager.java` | 不修改（复用） | 原 `querySuperiorByWorkType` 在非改造场景继续使用 |

### utopia-cube-start

| 文件 | 改动类型 | 改动点 |
|------|---------|--------|
| `v2/controller/PcController.java` | 修改 | + 派单待确认列表查询 |
| `v2/controller/PackageAcceptanceController.java` | 待确认 | + 工程量确认查询接口 |
| `v2/controller/fix/RefreshDataController.java` | 修改 | + 刷数接口 |
| `configuration/ScheduleConfiguration.java` | 修改 | + 30min自动确认定时任务 |

---

## 待确认事项汇总

1. `package_loan_task.dispatchStatus` 新增状态编码（31/32）还是新增独立字段存储二级状态？需与 minerva 对齐。
2. "工程coe"在 ceres `WorkTypeEnum` 中的确切编码是什么？
3. 班组请假审批代码是否在 workcenter 服务？改造范围如何划分？
4. 工程量确认查询接口是否由 cube 实现？PRD标注"@陶思宇 提供查询接口"，需确认实现方。
5. "直营"改"合营"还是"联营"？最终文案待PM确认。
6. 开城城市列表和工种开关配置的具体值，需业务提供。
7. 三级审批人名单配置方式（Apollo配置还是DB配置）。
8. 刷数执行的具体时间和批次策略，需DBA评估。
