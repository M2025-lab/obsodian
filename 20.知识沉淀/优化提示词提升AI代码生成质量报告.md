# 优化提示词提升 AI 代码生成质量报告

> 模型 deepseek-v4-flash｜任务 零售切排程迁移（纯零售为重点）｜日期 2026-09-10
> 核心结论：结构化提示词模板使 AI 沟通轮数 8→2，返工率 ~34%→~16%。

## 一、背景
排程侧将零售查询逻辑迁移为排程查询，涉及多单据类型分流、多入口（列表/品类）、配置开关（全国/分公司白名单），条件分支多、上下游联动强。评估模板对轮数与返工率的影响。

## 二、实验设置
- **指标**：沟通轮数 = 首次指令到可接受代码的交互轮次；返工率 = 人工修改代码行数 ÷ AI 生成代码行数。
- **对照**：同一任务（零售切排程迁移）在两个分支上分别用优化前/后提示词实践，任务复杂度对等。优化前 8 轮/~34%，优化后 2 轮/~16%。
- 均为新开窗口（无历史对话/历史代码），单次对照。

## 三、问题分析
| # | 现象 | 根因 | 模板段 |
|---|------|------|--------|
| 1 | 开关「零售&&全国&&分公司」3 次均被理解为 AND（全国开启才用分公司开关） | `&&` 无法区分逻辑与/优先级短路 | 【3】【6】 |
| 2 | 「按现有方式/现有方法」3 次均被 AI 自行定方式 | 模糊指代无可观测锚点 | 【2】【6】 |
| 3 | 指定接口改了，统一入口下游漏改 | AI 不主动沿调用链外推 | 【5】 |
| 4 | 多路径只改一条/全改一条；「保证相等」无法识别 | 无显式分流派发表 + 含糊词 | 【4】【6】 |

## 四、模板
```
【1 改哪里】
- 改：`类#方法`、`类#方法`   ← 一行一个，到方法粒度
- 不要动：`类`、`类`、以及与本需求无关的代码

【2 认字】（必须一字不差）
- 术语：<名称> = <数值> = `<枚举常量名>`；易混邻域值：<…>
- 配置 key：`<…>`；配置字段：`<字段名>`(<类型>)；注入点：`<全类名>`
- 方法签名：`<返回类型 方法名(参数)>`；接口：`<METHOD /path>`，`<注解>`，返回 `<类型>`

【3 判断顺序】（按顺序判定，命中即返回并终止）
| 步 | 条件 | 结果 | 是否继续 |
| S1 | <条件> | <结果> | 终止 |
| S2 | <条件> | <结果> | 终止 —— 不得继续判 <X> |
禁止写成：<最容易写错的那种表述>

【4 按什么入参分流】
| 入参特征 | 走哪条逻辑 | 返回几条 / 顺序 |
| <特征> | 新逻辑 | N 条 |
| <特征> | 旧逻辑 | M 条 |
复杂动作拆子行；写清收尾（遍历完统一做什么、结果追加到哪里）。
示例：入参 <具体值> → 期望 <N> 条、顺序 <…>
常见错误：<最容易做错的>，必须 <正确做法>

【5 入口与下游】
以 `类#方法` 为入口，沿调用链逐点确认下列下游是否需要同步改：
`类#a`（要改 / 不改 + 依据）、`类#b`（要改 / 不改 + 依据）
未列出的不要改。若含"判断是否已兼容"类任务：输出结论表（逐项 已兼容 / 需改 + 依据）。

【6 不许含糊 + 断言验收】
禁用：「按现有方式」「保证与之前相等」「业务规则不变」「尽量」「合理」→ 一律换成
「输入 <A> → 返回 <N> 条 / 字段 <P>=<Q>」。
断言（逐条可观测）：
1) <输入> → <期望>    2) <输入> → <期望>    3) <字段/位置> = <期望>
验收：`<可执行命令>`，预期 <…>
```

## 五、效果
| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| AI 沟通轮数 | 8 | 2 |
| 返工率（人工改 ÷ AI 生成，按行数） | ~34% | ~16% |

| 问题 | 优化前 | 优化后 | 根除 |
|------|--------|--------|------|
| 1 `&&` 歧义 | 3/3 误判 AND | 未复现 | ✅ |
| 2 「按现有方式」被忽略 | 3/3 自行定方式 | 未复现 | ✅ |
| 3 入口下游不联动 | 下游漏改 | 列全后改善 | ⚠️ 靠人工列全 |
| 4 多路径分流改不到位 | 只改一条/全改一条 | 分流表后改善 | ⚠️ 靠入参写全 |

剩余 ~16% 返工 = 人工修改代码行数 ÷ AI 生成代码行数。

## 六、边界与结论
- **适用**：明确入口 + 多条件分流 + 上下游联动的迁移/改造。**不适用**：纯新功能、探索性重构、Bug 修复。
- **结论**：模板根除歧义类（1、2）；联动/分流类（3、4）把 AI 盲区转嫁为人的梳理责任，依赖列全。

## 附录：纯零售迁移示例
```
【2 认字】（必须一字不差）

- 术语：纯定软电单（纯零售）= **12** = `RETAIL_REGULAR_SD_RETAIL_ORDER` 易混邻域值：配套单(定软电)=4、配套单(团装)=26、配套单(局装)=34；复合类型：主材单(整装)&配套单(定软电)=−99、主材单(团装)&配套单(团装)=−98、主材单(局装)&配套单(局装)=−97 排程引用状态 = `DELIVERY_ACTIVE`

- 配置 key：`pure.retail.schedule.switch.companyList`

- 配置字段：`nationEnable`(Boolean) + `companyList`(List<String>)，**新建一个类承载**

- 注入点：`com.ke.utopia.service.apollo.ApolloConfig`

- 方法签名：`Boolean isPureRetailScheduleSwitchOn(List<String> mdmCode)` —— 传列表时要求**全部**命中

- 接口：`POST /api/config/pureRetailScheduleSwitch`，`@Valid @RequestBody List<String>`，返回 `ResultDTO<Boolean>`（全部命中才 true）；挂 `CategoryProcessFeign` + `CategoryProcessController`

- 枚举：纯零售用已有枚举值 12，**不得新造**枚举或常量

【3 判断顺序】（按顺序判定，任一步命中即返回并终止）

|   |   |   |   |
|---|---|---|---|
|步|条件|结果|是否继续|
|S1|配置为 `null`|`false`|终止|
|S2|`nationEnable == true`|**`true`**|**终止 —— 不得继续判白名单**|
|S3|白名单非空 且 传入的**全部分公司**都在白名单内|`true`|终止|
|S4|其余情况|`false`|终止|

禁止写成：「全国开关开启 **且** 白名单命中」。 （S2 是本次重点：`nationEnable == true` 表示「全国全量开通、直接放行」。）

【4 按什么入参分流】 单据类型集合：`docTypes = {documentType} ∪ documentTypeList`（合并去重，忽略 null）。**两个来源都取，不得只认列表。** 不变量：只要 `documentType` 或 `documentTypeList` 任一来源能算出单据类型，`docTypes` 就不为空；只有两个来源都取不到时才为空。**不得**因为"列表来源为空"就判定 `docTypes` 为空。 返回顺序（两个入口相反，**不要统一成一种**）：列表入口 = 新逻辑结果在前（遍历中即时追加）、旧逻辑结果整批追加在末尾；品类入口 = 旧逻辑结果在前、新逻辑结果在后。 返回条数：分源命中时一个入参会展开成多条（12 一条 + 其余每个单据类型各一条）；**不得**为"与入参一一对应"做条数回填。

**4A · 列表入口 `queryConfigOFCList`（批量入参，逐参数判定）**

|   |   |   |
|---|---|---|
|行|条件|动作|
|A0|`configFlag == null` 或 `true`|该参数整参走新逻辑。不判断开关、不判断单据类型|
|A1–A3|`configFlag == false` 且「`docTypes` 为空 / 开关未命中 / `docTypes` 不含 12」任一成立|该参数整参走旧逻辑|
|A4|`configFlag == false` 且 `docTypes` 含 12 且 开关命中|逐项拆分（见子行）|

A4 逐项拆分：遍历 `docTypes` 每一项 `dt`

|   |   |   |
|---|---|---|
|子行|条件|动作|
|A4.1|`dt == 12`|新建查询参数对象（复制原参数），把单据类型字段设为 `12`、单据类型列表设为 `[12]` → 走新逻辑 → 结果直接追加到返回值。**禁止直接把原参数传入新逻辑**|
|A4.2|`dt != 12` 且 `secondTypeList` 中能筛出「映射后 == dt」的项|用筛出的子集构造旧逻辑入参，收集待批|
|A4.3|`dt != 12` 且筛不出任何项|跳过，不查询，不记日志|

A 收尾：全部参数遍历完后，把收集到的旧逻辑入参**整批一次**调用旧查询接口，结果追加到返回值末尾。

**4B · 品类入口 `queryCategoryConfigOFCList`（单个入参）**

|   |   |   |
|---|---|---|
|行|条件|动作|
|B0|`configFlag == null` 或 `true`|整参走新逻辑，不判断开关|
|B1–B3|`configFlag == false` 且「`docTypes` 为空 / 开关未命中 / `docTypes` 不含 12」任一成立|整参走旧逻辑|
|B4|`configFlag == false` 且 `docTypes` 含 12 且 开关命中|独立查两次后拼接（见下）|

B4 两段执行：

|   |   |
|---|---|
|段|动作|
|B4.1|从 `secondTypeList` 剔除「映射后 == 12」的项，剩余入参走旧逻辑；剔完为空则跳过本段|
|B4.2|新建查询参数对象（复制原参数），单据类型收敛为只含 12 → 走新逻辑|
|B4 合并|两段结果用 `addAll` 拼成一个列表返回（旧在前、新在后）|

示例（务必对齐）：入参单据类型 `[1, 12]` 且开关命中 → B4.1 用 `1` 查旧逻辑，返回 1 条 → B4.2 用 `[12]` 查新逻辑，返回 1 条 → **最终返回 2 条**。 常见错误：命中时**只查一次新逻辑、返回 1 条**。这是错的，必须两段都查。
```

---
---

问题：收集的需求不多，这个模板仅适用明确入口 + 多条件分流 + 上下游联动的迁移/改造。**不适用**：纯新功能、探索性重构、Bug 修复。
下一步其余扩展需求事使用新的skill来开发，新功能时需要注意接口签名的规定以及每个方法的职责，数据库查询时的限制。

```
在 `edar-starlord-service` 中新建 `exp/guided/r2` 分支并在分支中实现「纯零售进排程」改造。本提示词为**六槽结构**，逐槽填写；槽内契约必须一字不差。

【1 改哪里】

- 改（2 个模块、6 个方法）：
    
    - 配置查询分源：`ConfigQueryServiceImpl#queryConfigOFCList`、`ConfigQueryServiceImpl#queryCategoryConfigOFCList`
    
    - 任务模式判定：`TaskTemplateRoleServiceImpl#getTaskMode`、`TaskTemplateServiceImpl#convert2NMaterialTemplateUnitParam`、`TaskTemplateServiceImpl#buildDeliveryFlowTaskTemplateDTO`、`DeliveryMaterialBizServiceImpl#createByDelivery`

- 新增：1 个承载灰度配置的类（见【2 认字】）

- 不要动：`MaterialCreateV2ServiceImpl`、`ComboInfoManager` / `ComboInfoManagerImpl`、`pom.xml`、以及任何与本需求无关的代码

【2 认字】（必须一字不差）

- 术语：纯定软电单（纯零售）= **12** = `RETAIL_REGULAR_SD_RETAIL_ORDER` 易混邻域值：配套单(定软电)=4、配套单(团装)=26、配套单(局装)=34；复合类型：主材单(整装)&配套单(定软电)=−99、主材单(团装)&配套单(团装)=−98、主材单(局装)&配套单(局装)=−97 排程引用状态 = `DELIVERY_ACTIVE`

- 配置 key：`pure.retail.schedule.switch.companyList`

- 配置字段：`nationEnable`(Boolean) + `companyList`(List<String>)，**新建一个类承载**

- 注入点：`com.ke.utopia.service.apollo.ApolloConfig`

- 方法签名：`Boolean isPureRetailScheduleSwitchOn(List<String> mdmCode)` —— 传列表时要求**全部**命中

- 接口：`POST /api/config/pureRetailScheduleSwitch`，`@Valid @RequestBody List<String>`，返回 `ResultDTO<Boolean>`（全部命中才 true）；挂 `CategoryProcessFeign` + `CategoryProcessController`

- 枚举：纯零售用已有枚举值 12，**不得新造**枚举或常量

【3 判断顺序】（按顺序判定，任一步命中即返回并终止）

|   |   |   |   |
|---|---|---|---|
|步|条件|结果|是否继续|
|S1|配置为 `null`|`false`|终止|
|S2|`nationEnable == true`|**`true`**|**终止 —— 不得继续判白名单**|
|S3|白名单非空 且 传入的**全部分公司**都在白名单内|`true`|终止|
|S4|其余情况|`false`|终止|

禁止写成：「全国开关开启 **且** 白名单命中」。 （S2 是本次重点：`nationEnable == true` 表示「全国全量开通、直接放行」。）

【4 按什么入参分流】 单据类型集合：`docTypes = {documentType} ∪ documentTypeList`（合并去重，忽略 null）。**两个来源都取，不得只认列表。** 不变量：只要 `documentType` 或 `documentTypeList` 任一来源能算出单据类型，`docTypes` 就不为空；只有两个来源都取不到时才为空。**不得**因为"列表来源为空"就判定 `docTypes` 为空。 返回顺序（两个入口相反，**不要统一成一种**）：列表入口 = 新逻辑结果在前（遍历中即时追加）、旧逻辑结果整批追加在末尾；品类入口 = 旧逻辑结果在前、新逻辑结果在后。 返回条数：分源命中时一个入参会展开成多条（12 一条 + 其余每个单据类型各一条）；**不得**为"与入参一一对应"做条数回填。

**4A · 列表入口 `queryConfigOFCList`（批量入参，逐参数判定）**

|   |   |   |
|---|---|---|
|行|条件|动作|
|A0|`configFlag == null` 或 `true`|该参数整参走新逻辑。不判断开关、不判断单据类型|
|A1–A3|`configFlag == false` 且「`docTypes` 为空 / 开关未命中 / `docTypes` 不含 12」任一成立|该参数整参走旧逻辑|
|A4|`configFlag == false` 且 `docTypes` 含 12 且 开关命中|逐项拆分（见子行）|

A4 逐项拆分：遍历 `docTypes` 每一项 `dt`

|   |   |   |
|---|---|---|
|子行|条件|动作|
|A4.1|`dt == 12`|新建查询参数对象（复制原参数），把单据类型字段设为 `12`、单据类型列表设为 `[12]` → 走新逻辑 → 结果直接追加到返回值。**禁止直接把原参数传入新逻辑**|
|A4.2|`dt != 12` 且 `secondTypeList` 中能筛出「映射后 == dt」的项|用筛出的子集构造旧逻辑入参，收集待批|
|A4.3|`dt != 12` 且筛不出任何项|跳过，不查询，不记日志|

A 收尾：全部参数遍历完后，把收集到的旧逻辑入参**整批一次**调用旧查询接口，结果追加到返回值末尾。

**4B · 品类入口 `queryCategoryConfigOFCList`（单个入参）**

|   |   |   |
|---|---|---|
|行|条件|动作|
|B0|`configFlag == null` 或 `true`|整参走新逻辑，不判断开关|
|B1–B3|`configFlag == false` 且「`docTypes` 为空 / 开关未命中 / `docTypes` 不含 12」任一成立|整参走旧逻辑|
|B4|`configFlag == false` 且 `docTypes` 含 12 且 开关命中|独立查两次后拼接（见下）|

B4 两段执行：

|   |   |
|---|---|
|段|动作|
|B4.1|从 `secondTypeList` 剔除「映射后 == 12」的项，剩余入参走旧逻辑；剔完为空则跳过本段|
|B4.2|新建查询参数对象（复制原参数），单据类型收敛为只含 12 → 走新逻辑|
|B4 合并|两段结果用 `addAll` 拼成一个列表返回（旧在前、新在后）|

示例（务必对齐）：入参单据类型 `[1, 12]` 且开关命中 → B4.1 用 `1` 查旧逻辑，返回 1 条 → B4.2 用 `[12]` 查新逻辑，返回 1 条 → **最终返回 2 条**。 常见错误：命中时**只查一次新逻辑、返回 1 条**。这是错的，必须两段都查。

**4C · 新逻辑输出规格（两入口通用）** 复合类型的结果**拆成两条**，内容相同、仅单据类型不同：

|   |   |   |
|---|---|---|
|复合类型|主材单侧|配套单侧|
|−99|保持 −99|4|
|−98|保持 −98|26|
|−97|保持 −97|34|

【5 入口与下游（含落点锚点）】 本节 4 处是**互相独立**的判定点（不是一条链上的 4 站）；**除下表 4 处外，其余调用点一律不改**（未列出的不要改）。

|   |   |   |   |   |
|---|---|---|---|---|
|#|文件#方法|触发条件|动作|落点锚点|
|T1|`TaskTemplateRoleServiceImpl#getTaskMode`|`projectBusiness == RETAIL_2_5` 且 开关命中 `[mdmCode]`|`return ModeEnum.DELIVERY_FLOW`|在该 `RETAIL_2_5` 分支内、原 `return HOME2_5_MANPOWER` **之前**|
|T2|`TaskTemplateServiceImpl#convert2NMaterialTemplateUnitParam`|`unitParam.getMdmCodes()` 非空 且（开关命中 `mdmCodes` 或 开关命中 `[mdmCode]`）且 `saleType == RETAIL`|`unitParam.setMode(ModeEnum.DELIVERY_FLOW)`|**必须在「全国配置不使用套餐 → `setProductComboIds(null)`」块之后**|
|T3|`TaskTemplateServiceImpl#buildDeliveryFlowTaskTemplateDTO`|`saleType == RETAIL`|`param.setDocumentTypeList([12])`（**覆盖默认的 `[-99]`**）|在 `param.setMdmCompanyCodeList(...)` **之前**|
|T4|`DeliveryMaterialBizServiceImpl#createByDelivery`|`projectOrderDetailBO == null` 且 `supportMode == HOME2_5_MANPOWER` 且 开关命中 `[mdmCode]`|`setSupportMode(ModeEnum.DELIVERY_FLOW)`|在原有 `materialScheduleSwitch` 判定块**之后**|

硬要求：T2 落点放早了 → 纯零售的 `productComboId` 不会被清空；T3 不做 → 排程查询仍按复合类型 `[-99]` 过滤、拿不到零售排程配置；T1/T4 的开关入参都传 `Collections.singletonList(mdmCode)`。

【6 不许含糊 + 断言验收】 禁用表述：「按现有方式」「保证与之前相等」「业务规则不变」「尽量」「合理」—— 一律换成「输入 A → 返回 N 条 / 字段 P=Q」。

硬约束（逐条遵守）：

1. 两个方法的单据类型来源必须一致：都取「单值 ∪ 列表」合并去重，不允许一个只读列表。

2. 筛不出对应 `secondType` 的类型直接跳过，**禁止为此新增任何 `log.warn` / `log.error` / 异常**。

3. 走新逻辑前必须构造新参数对象并收敛单据类型，不得把含多类型的原参数直接传入。

4. 注释必须与实际行为一致：不得留下描述"未实现行为"的注释；改动后原注释若与代码不符，一并修正。

5. 返回值契约显式：条数与顺序见【4】。

6. 不得删除已有判空守卫；不得为省事引入新的 NPE 风险（例如对可能为 `null` 的集合直接构造包装，或对可能为 `null` 的 `secondTypeList` 直接 `.stream()`）。

7. 不得越界：只改【1】【5】点名的文件与方法，不顺手重构。

8. 对外契约不变：两个查询方法的入参、返回类型不变；`configFlag` 为空 / true 分支的行为不变。

【必须新增】（这是必须实现的守卫，**不是**"既有行为"）：新逻辑的生效单据类型列表为空时 → 记告警日志并返回空结果，不抛异常、不降级。 注意：**不要**把它扩大成"单值入参也返回空"的捷径 —— 单值来源能算出单据类型时不算空。

断言（逐条可观测，交付前逐条自证）：

1. 开关：配置 `{"nationEnable":true}` 时 `isPureRetailScheduleSwitchOn([任意分公司])` → `true`（不看白名单）；

2. 分源条数：`configFlag == false`、入参单据类型 `[1, 12]`、开关命中 → 返回 **2 条**；

3. 顺序：`queryConfigOFCList` 新逻辑结果在前；`queryCategoryConfigOFCList` 旧逻辑结果在前；

4. 落点：T2 的灰度块在 `setProductComboIds(null)` 之后；T3 的 `documentTypeList` 为 `[12]`；T1/T4 的开关入参为 `Collections.singletonList(mdmCode)`；
```