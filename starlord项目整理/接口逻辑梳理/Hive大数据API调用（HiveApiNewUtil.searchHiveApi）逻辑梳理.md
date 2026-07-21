---
source: code-flow
date: 2026-07-17
class: com.ke.utopia.manager.util.hive.HiveApiNewUtil
method: searchHiveApi
---

## 功能概述

通过 HTTP REST 方式调用外部大数据平台（Hive API），用于查询大数据分析结果（如订单数据、SKU 关系等）。核心逻辑：组装带 **MD5 签名**的 POST 请求，以 `application/x-www-form-urlencoded` 格式提交参数，解析返回的 JSON 数据，支持**自动重试**。

## 入口

- **入口方法**: `HiveApiNewUtil#searchHiveApi(Map<String, String> param, String url)`
- **返回类型**: `List<Map<String, Object>>` — 查询结果列表（每行为一个 Map）
- **触发方式**: 内部调用
  - `SupplierReplaceHiveManagerImpl` — 供应商替换业务，按 skuId+merchantId 查订单范围
  - `BackDoorController` — 后门调试接口
- **关键注解**: `@Component` — Spring Bean，从 Apollo 注入 key/secret

> 一句话：通过 x-www-form-urlencoded 格式 + MD5 签名向 Hive 大数据平台发 POST 请求并解析返回结果。

---

## searchHiveApi 处理逻辑

### 调用链全景

```
调用方
  │  SupplierReplaceHiveManagerImpl  ✓ 业务代码
  │  BackDoorController              ✓ 后门调试
  ▼
HiveApiNewUtil.searchHiveApi(param, url)
  │  ← 从 Apollo 注入 ${supplier.replace.app.key} / ${supplier.replace.app.secret}
  │
  ▼
HiveApiUtil.searchHiveApi(param, url, key, secret)   ← 静态工具类
  │
  ├── 1. 参数校验（url / key / secret 非空）
  ├── 2. 生成 MD5 签名 → 写入请求 header
  │     └── HiveApiSignUtil.getHeaderSignParamMap()
  ├── 3. 构建 FormBody POST 请求
  ├── 4. syncCall() → HTTP 调用 + 自动重试
  └── 5. 解析 JSON 响应 → 返回 List<Map<String, Object>>
```

> 一句话：HiveApiNewUtil 从 Apollo 获取凭证，透传给 HiveApiUtil 执行带签名的 HTTP 调用。

### HiveApiNewUtil — Spring 封装层

- 从 Apollo 注入 key/secret：
  - `${supplier.replace.app.key}` — API 密钥 key
  - `${supplier.replace.app.secret}` — API 密钥 secret
- 直接透传调用 `HiveApiUtil.searchHiveApi(param, url, key, secret)`

> 一句话：通过 Apollo 获取凭证，无业务逻辑。

### HiveApiUtil — 核心调用逻辑

**① 参数校验**：url / key / secret 均不能为空（`Assert.notNull`）

**② 构建请求 URL**：`HttpUrl.parse(url).newBuilder().build()`

**③ 构建请求体**：`FormBody.Builder`，param 的每个键值对通过 `builder::add` 追加 → content-type 为 `application/x-www-form-urlencoded`

**④ 构建请求头**：
- 调用 `HiveApiSignUtil.getHeaderSignParamMap(param, key, secret)` 生成三个 header 字段：
  - `key` — API 密钥 key
  - `ts` — 当前时间戳（yyyy-MM-dd HH:mm:ss）
  - `sign` — MD5 双重签名
- 额外加 header：`logId = "utopia_starlord" + UUID.randomUUID()`

**⑤ 执行 HTTP 调用**：`syncCall(request, param)`

**⑥ 解析返回 JSON**：
```java
JSON.parseObject(respStr, new TypeReference<HiveApiResult<Map<String, Object>>>() {})
→ hiveApiResult.getData()  // List<Map<String, Object>>
```

> 一句话：用 x-www-form-urlencoded 格式 + MD5 签名向 Hive API 发 POST 请求。

### HiveApiSignUtil — 双重 MD5 签名机制

**算法步骤**：

```
① 所有参数（含 key、ts）按 key 字典序排序 → URL 编码 → 拼装成 key1=val1&key2=val2
② 转小写 → 第一次 MD5
③ 第一次 MD5 结果 + secret 拼接 → 第二次 MD5
④ 最终结果作为 sign 放入 header
```

**示例**：

```
params = {sql: "...", key: "xxx", ts: "2026-07-17 10:00:00"}
   │ 排序 & URL 编码
   ▼
"key=xxx&sql=...&ts=2026-07-17+10%3A00%3A00"
   │ 转小写 → MD5
   ▼
md5_first = md5(lower(urlEncoded))
   │ + secret → MD5
   ▼
sign = md5(md5_first + secret)  → 放入 header["sign"]
```

> 一句话：参数排序 → URL 编码 → MD5 → 拼接 secret → 再 MD5，双重摘要确保请求不被篡改。

### syncCall — HTTP 调用 + 自动重试

**重试器**（guava-retrying 库）：

| 参数 | 值 |
|------|-----|
| 重试条件 | `UtopiaBussinessException` |
| 等待策略 | 固定 1 秒 |
| 停止策略 | 最多 3 次（含第一次） |

**每次调用流程**：

```
① OkHttpUtil.syncCall(request) → Response
② response.code() != 200（HTTP 非正常响应）？
   → 第 3 次 → log.error
   → 不是第 3 次 → log.warn
   → 抛 UtopiaBussinessException（触发重试）
③ 解析 body 为 JSON → HiveApiResult
④ targetResult.isSuccess() == false 或 data == null（业务失败）？
   → 第 3 次 → log.error
   → 不是第 3 次 → log.warn
   → 抛 UtopiaBussinessException（触发重试）
⑤ 返回 response body 字符串
```

> 一句话：最多 3 次 HTTP 调用，遇到 HTTP 错误码或业务失败状态，间隔 1 秒重试，3 次全部失败后抛异常。

---

## 条件分支汇总

| 位置 | 条件 | 走向 | 结果 |
|------|------|------|------|
| HiveApiNewUtil | Apollo 未配 key/secret | 空值传递 | HiveApiUtil 抛 `IllegalArgumentException` |
| syncCall | HTTP status != 200 | 抛 `UtopiaBussinessException` | 触发重试（下次继续） |
| syncCall | 第 3 次仍 HTTP 失败 | log.error + 抛异常 | 不再重试，异常冒泡给上游 |
| syncCall | `!isSuccess() \|\| data == null` | 抛 `UtopiaBussinessException` | 触发重试 |
| syncCall | 第 3 次仍业务失败 | log.error + 抛异常 | 不再重试 |
| 最外层 catch | 任意异常（含重试耗尽） | log.warn | 包装为 `UtopiaBussinessException(ERROR_BUSINESS)` |

---

## 数据查询

无数据库查询。数据来源：

| 来源 | 方式 | URL 示例 |
|------|------|---------|
| Hive 大数据平台 | HTTP POST | `http://i.data.api.lianjia.com/v2/meta/bi/xxx` |

---

## 状态变更

无状态变更 — 纯查询工具类。

---

## 外部调用和事件

| 类型 | 目标 | 时机 | 说明 |
|------|------|------|------|
| HTTP POST | `i.data.api.lianjia.com`（外部分部数据平台） | searchHiveApi 调用时 | 执行 Hive SQL 查询，返回 JSON 结果 |

---

## 待澄清点

- [ ] 重试器是 `static` 字段，多个请求共享同一 Retryer 实例；`syncCall` 内部匿名类中的 `times` 变量作为实例字段，在每个 `Callable` 对象内是独立的，但 `Retryer` 本身是否为线程安全需要确认
- [ ] 重试策略为固定 1s 间隔最多 3 次，对于大数据查询（可能本身就很慢）不够合理，应该根据 API 响应时间动态调整
- [ ] `HiveApiSignUtil.getApiSign` 中局部变量 `key` 遮蔽了方法参数 `key`，命名冲突但不影响结果

---

## 总结

**主干路径**: HiveApiNewUtil（注入 Apollo 凭证）→ HiveApiUtil.searchHiveApi（拼装请求+签名+HTTP 调用+重试）→ 解析 HiveApiResult → 返回数据列表

**架构特点**:
1. **两层封装** — HiveApiNewUtil 是 Spring Bean（注入配置），HiveApiUtil 是无状态静态工具，各司其职
2. **MD5 双重签名** — 参数排序 → URL 编码 → MD5 → 拼接 secret → 再 MD5，避免 Secret 明文传输
3. **自动重试** — 基于 guava-retrying 库，最大 3 次，固定间隔 1s，针对 `UtopiaBussinessException` 重试
4. **异常转译** — 所有异常最终转为 `UtopiaBussinessException(ERROR_BUSINESS)`，不暴露底层 HTTP 异常细节
5. **调用方** — 当前用于供应商替换业务查订单范围和后门调试接口

---
## 相关文档

- [[需求/供应商汰换20260709/从Hive拉取数据的API模式（searchHiveApi）]] — Hive API调用的需求场景说明
- [[技能沉淀]] — Java技术要点（OkHttp、重试机制等）
- [[需求/供应商汰换20260709/技术方案]] — 供应商汰换技术方案
