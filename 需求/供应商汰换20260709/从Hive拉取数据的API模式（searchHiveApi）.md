#### 一句话总结

通过 **OkHttp3 + 签名鉴权 + 重试机制** 调用 Hive 大数据平台的 HTTP API，返回 `List<Map<String, Object>>` 格式数据。

#### 完整调用链路（7步）

```
调用方传入 url, param, key, secret
        │
        ▼
  ┌─ ① 组装 FormBody 请求体 ─────── OkHttp3 FormBody.Builder
  │
  ├─ ② 计算签名 Header (key+ts+sign) ─ JDK MessageDigest (MD5)
  │    └─ 参数排序 → URL编码 → MD5 → 拼secret → 再MD5
  │
  ├─ ③ 构建 HTTP Request ────────── OkHttp3 Request.Builder
  │
  ├─ ④ 发送请求（带重试） ───────── OkHttp3 newCall().execute()
  │    ├─ 失败？→ 等1秒 → 重试（最多3次）
  │    └─ 全部失败？→ 抛异常
  │
  ├─ ⑤ 解析 JSON 响应 ──────────── Hutool JSONUtil / Fastjson TypeReference
  │    └─ 注意泛型擦除，必须用 TypeReference
  │
  ├─ ⑥ 提取 data 字段 ──────────── 返回 List<Map<String, Object>>
  │
  └─ ⑦ 异常统一包装 ────────────── 自定义业务异常
```

#### 涉及的工具/库

| 工具 | 用途 | 关键代码 |
|---|---|---|
| **OkHttp3** | HTTP客户端：构建请求体、发送POST请求 | `FormBody.Builder`、`OkHttpClient.newCall(request).execute()` |
| **guava-retrying** | 重试框架：失败后重试N次，每次间隔固定时间 | `RetryerBuilder.retryIfExceptionOfType().withWaitStrategy().withStopStrategy().build()` |
| **Hutool / Fastjson** | JSON反序列化，注意泛型擦除 | `JSONUtil.toBean(respStr, new TypeReference<X>() {}, true)` |
| **JDK MessageDigest** | MD5签名 | `MessageDigest.getInstance("MD5")` |
| **JDK URLEncoder** | 签名前参数编码 | `URLEncoder.encode(str, "utf-8")` |
| **JDK UUID** | 请求追踪ID | `UUID.randomUUID()` |

#### 核心代码结构（3个类）

**1. HiveApiResult<T>** — 统一响应模型

```java
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class HiveApiResult<T> {
    private Integer errno;       // 0=成功
    private String errmsg;
    private List<T> data;

    public boolean isSuccess() { return Objects.nonNull(errno) && errno.equals(0); }
}
```

**2. HiveApiSignUtil** — 签名算法（Hive大数据平台鉴权）

```
参数排序 → URL编码 → 拼接key=val&key=val → 转小写 → MD5 → 拼接secret → 再MD5
```

**3. HiveApiUtil** — 核心调用器

- 声明 `static final OkHttpClient` 单例（复用连接池，避免重复创建）
- 构建 FormBody + 签名 Header → POST 请求
- guava-retrying 重试 3 次，间隔 1s
- 只判断 HTTP 200 + errno==0 视为成功

#### 典型调用方式

```java
// 直接调用
Map<String, String> param = new HashMap<>();
param.put("projectId", "12345");
List<Map<String, Object>> result = HiveApiUtil.searchHiveApi(
    param, "http://hive-api-url", "your-key", "your-secret");

// 或封装成 Spring Bean（通过 @Value 注入 key/secret）
@Component
public class HiveApiClient {
    @Value("${hive.api.key}") private String key;
    @Value("${hive.api.secret}") private String secret;
    public List<Map<String, Object>> search(Map<String, String> param, String url) {
        return HiveApiUtil.searchHiveApi(param, url, this.key, this.secret);
    }
}
```

#### 最佳实践

1. **OkHttpClient 必须声明为单例**（`private static final`），每个实例都带有独立的连接池和线程池，重复创建浪费资源
2. **泛型反序列化必须用 TypeReference**，Java 泛型擦除导致 `HiveApiResult.class` 拿不到 `<Map<String,Object>>` 的类型信息
3. **重试只重试特定异常**，不要 `retryIfException(true)`，避免把不可恢复的错误也重试
4. **签名参数必须包含所有请求参数**（包括 key 和 ts），否则 Hive 端验签失败

#### 不同方案的对比

| 方案 | 优点 | 缺点 |
|---|---|---|
| `OkHttpUtil` 统一封装 | 连接池复用、超时统一配置、拦截器可共用 | 多一个类，结构稍复杂 |
| 内联 `new OkHttpClient()` | 少一个依赖类 | 每次新建连接池，高并发浪费资源 |

推荐：**在 `HiveApiUtil` 内部声明一个 `static final OkHttpClient` 单例**，不需要单独抽 `OkHttpUtil`。

---
## 相关文档

- [[starlord项目整理/接口逻辑梳理/Hive大数据API调用（HiveApiNewUtil.searchHiveApi）逻辑梳理]] — Hive API代码级完整分析
- [[需求/供应商汰换20260709/技术方案]] — 供应商汰换方案（Hive API的使用场景）
- [[技能沉淀]] — Java技术要点（OkHttp、重试机制等）

