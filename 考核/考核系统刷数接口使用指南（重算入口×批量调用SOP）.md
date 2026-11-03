---
source: refresh-interface-guide
date: 2026-11-03
system: scm-workbench
type: 刷数接口使用指南 / 批量重算 SOP
---

# 考核系统（scm-workbench）刷数接口使用指南 — 重算入口 × 批量调用 SOP

> 用途：回答"刷数接口怎么用、有没有现成实现、如何 for 循环调用"。核心结论：**刷数 = 调 `ToolController` 里的重算后门；接口内部已经是 for 循环逐单串行处理，调用方传 List 即可；大范围刷数自己再套一层分批循环，串行慢跑、不要并发。**
> 依据：`ToolController.java` 代码走读 + 交接文字记录（20261027，00:49 附近）+ `JavaJobDemo` Spark 批处理范例。
> 关联笔记：[[考核系统开发维护注意事项（上下游变更适配手册）]]（§六 修数五步 SOP）、[[考核系统scm-workbench-功能与代码学习指南]]、[[考核时间计算逻辑梳理（工作日自然日与节假日日历）]]

---

## 一、什么是"刷数"

考核的前提是配置不变。一旦业务改了配置（考核规则、阿波罗配置如"周末能不能备货"、日历打标口径），存量项目的考核结果就停在旧输入上——系统**不存在**"输入变了 → 自动找受影响单据 → 级联重算"的机制，必须人工把受影响的项目/货单重新算一遍，这就是刷数。

交接记录原话（说话人 1）：

> "调那个重算接口。接口有一个入口，入参就是项目 ID，调那入口就行了。只不过项目范围比较大，比如把最近这 6 个月开工的项目全重算一遍，你就 for 循环慢慢跑，**别并发跑**，单个任务一个一个跑就行。"
> "刷数一般就是找到受影响的项目刷就行了。"（受影响项目根据配置反查；业务有时给范围，如"北京在途没竣工的全刷"）

---

## 二、四个重算入口（`scm-workbench-start/src/main/java/com/ke/utopia/scm/workbench/controller/ToolController.java`）

| 接口 | 入参 | 用途 | 循环中异常行为 |
|------|------|------|------|
| `POST /api/refresh/eval/order`（:149 `refreshEvalOrder`） | `List<String>` 货单号 | 旧全国货单考核重算；查不到考核载体会自动补建 `indicator_order` 再算 | **不 catch，直接抛出中断整批** |
| `POST /api/refresh/eval/projectOrder`（:178 `refreshEvalProjectOrder`） | `List<String>` 项目ID | 旧考核按项目重算；在排程开关内的项目自动跳过 | catch 后记日志**继续下一单** |
| `POST /api/refresh/eval/schedule/projectOrder`（:230 `refreshEvalScheduleProjectOrder`） | `List<String>` 项目ID | **新排程考核**重算；白名单/排程开关内才处理，先做基装包节点 diff（`checkBasicDiff`）再算 | 抛 `ServiceException` **整批中断** |
| `POST /api/refresh/eval/order/latestVersion`（:363 `refreshEvalOrderWithLatestVersion`） | `List<RefreshOrderVersionParam>`（orderNo + newVersion） | 用**最新版规则**重算旧货单考核；`newVersion=true` 先清空 `base/cusEvalResultNew` 强制重算 | catch 后继续下一单 |

**选型速查**（来自学习指南 §排查路由）：

- 旧考核结果不对，按货单刷 → `/api/refresh/eval/order`
- 旧考核要强制用最新规则 → `/api/refresh/eval/order/latestVersion`
- 新排程考核（项目粒度）→ `/api/refresh/eval/schedule/projectOrder`
- 刷完抽查现状 → `GET /api/tool/projectAssessment?projectOrderId=xxx`

### 配套工具接口（刷数前后常配合使用）

| 接口 | 何时用 |
|------|------|
| `POST /api/holidayCache/delete` | 日历数据补录/修正后**必须先删缓存**再刷数（滑动 TTL 下缓存永不自动回源，不删等于白刷）；`POST /api/holidayCache/get` 可查缓存覆盖了哪些日期 |
| `POST /api/remove/schedule/dag/cache` | 改了指标规则配置后清 DAG 本地缓存（JVM ConcurrentHashMap + Redisson 版本号），否则刷数仍按旧 DAG 算 |

---

## 三、怎么"for 循环调用"

### 3.1 通常不用自己写循环

四个接口都是**收 List、内部串行逐单处理**，一次 POST 一批即可：

```bash
curl -X POST 'http://<scm-workbench>/api/refresh/eval/schedule/projectOrder' \
  -H 'Content-Type: application/json' \
  -d '["10001234","10001235","10001236"]'
```

### 3.2 大批量：外层再套分批循环

因为**无事务保护、串行处理、中途异常即终止且不回滚**（schedule / order 两个入口尤其如此），SOP 要求拆小批 + 事后核对：

```java
List<List<String>> batches = Lists.partition(projectOrderIds, 50); // 拆小批
for (List<String> batch : batches) {
    HttpResponse<String> resp = Unirest.post(url)
            .header("Content-Type", "application/json")
            .body(JSONObject.toJSONString(batch))
            .asString();
    // 留痕：时间、范围、原因、操作人（45 个后门接口无审计，这是唯一追溯手段）
    log.info("刷数批次 {} 结果 code:{} body:{}", batch, resp.getStatus(), resp.getBody());
}
```

### 3.3 现成的完整范例：`JavaJobDemo`（Spark T+1 批处理）

`scm-workbench-job/src/main/java/com/ke/utopia/scm/workbench/job/JavaJobDemo.java` —— 线上实际在跑的"循环调用刷数"实现：

1. 从 Hive 查 2.0 在途销售单（排除集采）；
2. 按 `categoryId_branchCode` 分组（`groupByKey`）；
3. `foreachPartition` 里用 Unirest **逐条串行 POST** 给考核服务重算（`pushToCompute` 方法，:57-72），每条记录耗时与返回；
4. spark-submit 提交。

这就是"找到受影响单据 → 串行慢跑 → 逐条留日志"的标准模板，手动刷大批量时照抄这个节奏即可。

---

## 四、刷数标准流程（五步，对齐维护手册 §六）

```text
① 定范围    配置变更点 → 反查受影响项目/货单（业务给范围或按配置维度圈定）
② 刷缓存    涉及日历 → holidayCache/delete；涉及规则/DAG → remove/schedule/dag/cache
③ 分批重算  按 §二 选型调对应 refresh 接口，小批串行，不并发
④ 验证      /api/tool/projectAssessment 抽查；注意接口返回成功 ≠ 异步下游（ES 指标等）同步完成
⑤ 留痕      记录时间、范围、原因、操作人（后门接口无审计）
```

## 五、注意事项汇总

- ⚠️ **不要并发跑**：交接明确要求串行慢跑；并发会打乱事件顺序且下游（DAG 缓存、ES 推送）扛不住。
- ⚠️ **异常中断不回滚**：`/api/refresh/eval/order` 与 `/api/refresh/eval/schedule/projectOrder` 遇异常整批终止，已刷的不回滚、未刷的不再刷——所以必须拆小批，中断后从断点续刷。
- ⚠️ **排程开关跳过逻辑**：`refreshEvalProjectOrder` 会跳过在排程开关内的项目；`refreshEvalScheduleProjectOrder` 只处理白名单/排程开关内的项目——新旧考核别刷错入口。
- ⚠️ **latestVersion 有破坏性**：`newVersion=true` 会先把 `indicator_order` 的 `baseEvalResultNew/cusEvalResultNew` 清成 `[]` 再重算，刷失败会留下空结果，小批验证后再放量。
- ⚠️ **返回成功 ≠ 全部生效**：下游 ES 指标同步是异步的，刷完延迟抽查；缺数可走 `/api/workbench/es/indicator/rePush/byIndicatorId` 重推。

---

## 六、一句话记忆

> **刷数四问：刷哪个（新排程 vs 旧货单）？先删缓存没？拆小批串行了没？留痕了没？**
> 入口收 List 内部自带 for，大批量外层再分批；串行慢跑不并发，异常中断不回头。

---
---
