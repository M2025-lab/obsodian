# 📚 纯零售接入排程改造复盘（方案 vs 实现差异）

> 复盘对象：edar-starlord **纯零售（DocumentType=12）接入排程模式**改造，2026-08-31 ~ 2026-09-09。
> **结论先行**：所有差异中**没有一项源于"需求理解偏差"**——问题集中在 ①方案写得太粗（5 项）②临时决策未回写方案（2 项）③方案阶段信息不足/外部依赖不可见（1 项）④隐性取舍与无验收标准（2 项）。

---

## 0. 背景与分支现状

| 项 | 说明 |
|---|---|
| 需求 | 8 个改造点：分公司灰度开关+暴露接口、排程页面兼容、配置查询改造、中控/材料配置灰度、任务生成灰度 |
| 硬约束 | 仅迁移纯零售，不得改动其他业务逻辑，保持完全兼容 |
| **人工版** | `feat-20260831-material-process-pure-retail`（**当前分支**，18 commit，14 文件 +352/−66） |
| **AI 版** | `feat/20260908/ai-retail-move-process`（4f14bf2f8，10 文件） |
| 方案文档 | `doc/纯零售接入排程_改造计划.md` **已随分支切换丢失**（该分支仅剩 `panorama_ddl.sql`、`saleTypeConvert.md`） |
| 代价 | 用户指令级返工 4 次 + 编译失败 2 次，"改了又删"约 44 行 |

---

## 1. 方案 vs 实现差异表（D1–D10）

| # | 差异点（文件/位置） | 方案描述 vs 实际做法 | 根因归类 | 影响 | 处置建议 |
|---|---|---|---|---|---|
| **D1** | Apollo key 与 Bo 字段<br>`ApolloConfig:162` / `PureRetailScheduleSwitchBo:24` | 方案：key=`pure.retail.schedule.switch`，字段 `enable`<br>实现：key=`...switch.companyList`，字段 `nationEnable` | **未同步的临时决策** | 正确性**高**（配错即静默失效）、可维护性中 | 改代码统一（二选一后固化），方案同步 |
| **D2** | 开关判定签名/多值语义<br>`ConfigQueryServiceImpl:894-912` | 方案：`isPureRetailScheduleSwitchOn(String)` 单值 contains<br>实现：`(List<String>)` + `containsAll`（**AND**） | **方案粒度过粗**（未定义多分公司 AND/OR） | 正确性**高** | 先定语义→代码+方案同时对齐 |
| **D3** | 对外接口入参<br>`CategoryProcessFeign:85` | 方案：`PureRetailScheduleSwitchParam` 对象<br>实现：`List<String> mdmCodes` | **方案粒度过粗**（未定入参/返回结构） | 接口契约**高** | 改代码统一契约 |
| **D4** | 第4点 `configFlag=true` 行为<br>`ConfigQueryServiceImpl:338-340, 420-421` | 方案（后期追加）：纯零售未命中灰度→回退旧八合一<br>实现：一律走排程，灰度仅决定 `DELIVERY_ACTIVE`/`INIT_ACTIVE` | **未同步的临时决策**（实现早于决策） | 正确性中、**可回滚性高** | 改代码对齐（若业务要求可回滚） |
| **D5** | 第3点批量返回顺序<br>`ConfigQueryServiceImpl:289-300`+`:261` | 方案：未提及<br>实现：12 项先 add、其余循环后整批追加 | **方案粒度过粗**（未约束返回同序） | 正确性**中**（按 index 取会错乱） | 改代码保序 + 方案补约束 |
| **D6** | 第6点取值来源与短路<br>`TaskTemplateServiceImpl:255` | 方案：`listTaskParam.getSaleType()` + 遍历 mdmCode/mdmCodes（OR）<br>实现：`unitParam.getSaleType()` + `nonNull(mdmCodes)` 前置**短路** | **方案粒度过粗** + 实现缺陷 | 正确性**高**：单分公司（mdmCodes=null）时纯零售永不生效且不报错 | 改代码修短路 + 方案明确取值来源 |
| **D7** | 单据类型推导依赖<br>`DocumentTypeEnum:141-144` + `pom.xml:89-90` | 方案：判定第7点"天然兼容"，**未改任何枚举**（已验证：AI 版 `git diff` 无输出）<br>实现：新增 `RETAIL+SD_RETAIL_ORDER→12` | **方案阶段信息不足**：`BusinessType`/`SecondType` 在外部 jar（`com.ke.utopia.scm.oms.refactor.enums.core`），源码不可见，未追到 `calculateDocumentType→getByBusinessTypeAndSecondTypeSupportComplex` 链路 | 正确性**高**（推不出 12 则纯零售整体失效）、发布风险中（改 base→必须发 SNAPSHOT） | **必须改代码补枚举** + 方案补该依赖与发布影响 |
| **D8** | 第8点覆盖范围<br>`DeliveryMaterialBizServiceImpl:189-195 / :542-550` | 方案：`createByDelivery` + `repairCreateByDelivery` + 有订单纯零售判定<br>实现：仅 `createByDelivery`，且仅"无订单"场景 | **方案粒度过粗**（原文只点名 createByDelivery；repair 是对话中补充但**未写入方案文档**）+ 未同步 | 正确性**高**（补建路径、有订单纯零售单都不覆盖） | 改代码补齐 + 方案补全调用点 |
| **D9** | 第2点兼容性验收<br>`DeliveryFlowSelectQueryServiceImpl` 等 | 方案：判定"天然兼容、无需改动"，**无验收标准**<br>实现：同样未改（与方案一致） | **方案粒度过粗**（只给结论不给判据） | 正确性中（依赖 Apollo `delivery-flow.role` 是否有纯零售 key，**从未验证**） | 更新方案补验收标准（不必改码） |
| **D10** | 方案外改动<br>`BusinessServiceImpl`+39 / `MaterialCommonServiceImpl`+4 / `pom.xml` | 方案：仅纯零售相关<br>实现：混入排期查询缓存优化、pom 版本改 `2.6.11-ltd-SNAPSHOT` | **隐性取舍** + 未界定改动边界 | 可维护性中、发布风险中 | 拆分提交 + 方案明确边界 |

---

## 2. 人工版遗留缺陷（未被要求返工，但实际有问题）

| 缺陷 | 位置 | 为什么当时没暴露 |
|---|---|---|
| **注释与实现不一致**：Bo 注释仍写 key=`pure.retail.schedule.switch`、示例用 `enable`，实际字段 `nationEnable`、注入 key=`...companyList` | `PureRetailScheduleSwitchBo:10-14` vs `:24`、`ApolloConfig:162` | 改字段名/key 属局部小改，编译通过即可；注释错误**不影响运行**，无测试能发现；后人按注释配 Apollo 会**静默失效** |
| **第6点逻辑短路**：`Objects.nonNull(getMdmCodes())` 前置，单分公司场景 `singletonList(mdmCode)` 分支永不执行 | `TaskTemplateServiceImpl:255` | 多分公司场景正常；不报错只表现为"没走排程"，易被当作配置问题 |
| **第3点返回顺序被破坏** | `ConfigQueryServiceImpl:289-300` + `:261` 注释 | 调用方若按内容（品类 code）匹配则不显现，仅按 index 取才错乱 |
| **三套 Apollo key 并存**（方案 / 人工版 / AI 版） | — | 代码层无校验，只在实际配置环境才生效，缺一即静默失效 |

---

## 3. 两套实现速查对比

| 项 | 人工版 | AI 版 |
|---|---|---|
| Apollo key | `pure.retail.schedule.switch.companyList` | `pure.retail.schedule.switch` |
| Bo 字段 | `nationEnable` + companyList | `enable` + companyList |
| 注入位置 | `ApolloConfig:161-163` | `ConfigQueryServiceImpl` 内 |
| 方法签名 | `(List<String>)` 返回 Boolean，`containsAll`(AND) | `(String)` 返回 boolean，单值 contains |
| 对外接口入参 | `List<String> mdmCodes` | `PureRetailScheduleSwitchParam` |
| 第4点 configFlag=true | 一律走排程，灰度只影响激活状态 | 未命中灰度→回退旧八合一 |
| 第8点 | 仅 createByDelivery、仅无订单 | createByDelivery + repair、含 isPureRetailOrder |
| DocumentTypeEnum | 新增 `RETAIL+SD_RETAIL_ORDER→12`（:141-144） | **未改**（潜在功能失效） |
| pom / 缓存优化 | 改（SNAPSHOT） | 未改 |

---

## 4. 改进项（写方案时应补充）

1. **写死契约，不写概念**：Apollo key 全名、Bo 字段名与语义、接口入参/返回结构（D1/D3）。写"加个开关/暴露接口"必然分叉。
2. **追到跨模块依赖再定论，并标注验证边界**：涉及枚举推导、外部 jar 的，写明"依赖 X 链路，源码不可见需实测"，并列连带影响（改 base → 版本号/发布）（D7）。
3. **逐场景写"改造前 → 改造后"路径 + 覆盖全部同源调用点**：含 repair 类方法、有/无订单、单值/多值（D2/D6/D8），避免只点名一个入口。
4. **补验收标准与边界约束**：批量返回同序、幂等范围、未配置默认行为、多值判定规则 AND/OR（D5/D9）。
5. **方案定稿后冻结并回写**：对话中的临时决策（enable 语义、灰度定义、repair 是否改）必须同步回方案（D1/D4/D8）；方案文档要 commit 或放不受分支切换影响的位置（本次方案文档已丢失）。

---

## 5. 遗留待决策

- 以哪套实现为准（人工版 / AI 版 / 合并取长）？
- `configFlag=true` 的纯零售单现网是否真实存在？能否接受"灰度未命中即降级为旧逻辑"？
- ofc 外部批量接口返回是否严格与入参一一对应（影响 D5 保序方案）？
- 改 base 模块引发的 SNAPSHOT 版本发布流程如何走？
