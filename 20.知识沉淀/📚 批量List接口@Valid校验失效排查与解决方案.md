# 📚 批量 List 接口 @Valid 校验失效：排查与解决方案

> **踩坑对象**：`POST /api/recognize/task/result`（通知复尺自动化，`NotifyTaskAutomationController`）
> **一句话结论**：`@Valid` 写在 `List<X>` 方法参数上，Hibernate Validator **根本不会级联校验元素**——不管是 Feign 调用还是 HTTP 直连，注解都是"装饰性"的，非法数据会直接穿透写库。
> **沉淀日期**：2026-08-17

---

## 1. 问题现象

接口入参是 `List<NotifyTaskResultParam>`（JSON 数组，批量上报识别结果），DTO 每个字段都带校验注解：

```java
// NotifyTaskResultParam —— 8 个字段，全部带校验注解
@NotNull                    private Long projectOrderId;       // project_order_id
@NotBlank                   private String materialCode;       // material_code
@NotNull                    private Integer taskType;          // task_type
@NotBlank                   private String recognizeTime;      // recognize_time, yyyy-MM-dd
@NotBlank                   private String recommendVisitTime; // recommend_visit_time, yyyy-MM-dd
@NotNull                    private Integer judgeResult;       // judge_result: 1可通知/2不可通知/3无法判断/4现场已完成
@NotBlank                   private String judgeRemark;        // judge_remark
@NotEmpty @JsonProperty     private List<String> images;       // images 关键帧图片URL
```

实测发现以下请求**全部写库成功**（应该被拦截）：

- `images: []`（空数组，违反 `@NotEmpty`）
- `judge_remark: ""`（空字符串，违反 `@NotBlank`）
- `material_code: ""`（空字符串，违反 `@NotBlank`）

**注意**：真正失效的只有校验注解。`@JsonProperty` 反序列化照常工作（否则参数绑不进来），`@ApiModelProperty` 只是 Swagger 文档标注，与校验无关。所以现象是"数据能正常绑定、但没人检查合法性"。

---

## 2. 排查过程（时间线）

| 步骤 | 检查项 | 结论 |
|---|---|---|
| ① | Controller 参数上有没有 `@Valid` | 起初**没有**——`@Valid @RequestBody` 只写在 Feign 接口上（`NotifyTaskAutomationFeign`），Controller 实现方法是裸参数，注解不会可靠继承 |
| ② | 补上 `@Valid @RequestBody` 后重测 | **仍然失效**（这是最关键的转折点） |
| ③ | validation 依赖是否在 classpath | `./mvnw dependency:tree` 确认：hibernate-validator 6.0.18.Final、validation-api 2.0.1.Final、spring-boot-starter-validation 2.1.11 **全在 web 模块 compile classpath** ✅ |
| ④ | 是否有 `@EnableWebMvc` / 自定义 WebMvcConfigurer 覆盖 validator | 没有，默认 mvcValidator 应存在 ✅ |
| ⑤ | 自研 `spring-boot-starter-web-essential` 是否动过 MVC 配置 | 该 starter 实际不在 web 依赖链上，排除 ✅ |
| ⑥ | **最小实验验证 HV 行为（决定性证据）** | 见下节 |

### 2.1 决定性实验（不依赖 Spring，纯 HV 直出）

用 hibernate-validator 6.0.18 直接跑三类校验：

```java
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

// A. 直接 validate List 根对象  ← Spring 收到 @Valid List 实际就是这个调用
validator.validate(list);      // violations = 0   ← 元素约束完全不触发！

// B. validate 一个带 @Valid 字段的 Wrapper（对照：证明 HV 本身在工作）
validator.validate(wrapper);   // violations = 3   ← 字段上的 @Valid 正常级联

// C. validate 数组根对象
validator.validate(array);     // violations = 0   ← 数组根对象同样不触发
```

**结论：HV 只对"bean 字段上的 `@Valid`"做级联校验；当被校验的根对象本身就是 List/数组时，`validate()` 不会走进元素。**

---

## 3. 根因分析（三层）

### 3.1 第一层：级联校验只认"字段上的 @Valid"

JSR-380 中 `@Valid` 的语义是"校验对象图时，遇到这个点要继续往下一层翻"。它只有出现在**对象图的边上**（即某个 bean 的字段/属性上）才有意义：

```java
class Wrapper {
    @Valid @NotEmpty private List<NotifyTaskResultParam> list;  // ← 这里是"对象图的边"
}
```

`validate(wrapper)` 遍历属性时看到 `list` 字段标了 `@Valid` → 深入元素逐条校验。而 `@Valid` 标在**方法参数**上时，它不在任何对象的字段上下文里，只是孤零零挂在参数声明上。

### 3.2 第二层：Spring MVC 把参数注解当成"开关"，而不是"指令"

Spring MVC 处理 `@RequestBody` 校验的路径（`RequestResponseBodyMethodProcessor` → `WebDataBinder`）：

```java
// 1. 检测到参数上有 @Valid —— 只决定一件事：要不要调 validator
validateIfApplicable(binder, parameter);   // 命中 @Valid → 调 binder.validate()

// 2. 执行校验时，参数注解彻底消失，只把对象交出去
validator.validate(getTarget());           // getTarget() == 那个 List 实例
```

`validateIfApplicable` 只提取出一个布尔信号（"有校验注解，去校验"），注解本身（包括"我要级联"的意图）不会随对象传给 validator。

### 3.3 第三层：HV 拿到裸 List 后，什么约束都查不到

`validator.validate(list)` 只做两步：

1. 查 **List 类自身**的约束 —— `ArrayList`/`List` 上没有任何 `@NotNull` 之类注解 → 0 条；
2. 遍历 List 的属性找带 `@Valid` 的字段 —— 没有 → 0 条。

容器元素的约束只有在"字段上的 @Valid 触发级联"时才会被递归到。List 作为根对象时不在任何字段边上，级联入口不存在 → **violations = 0，静默放行**。

### 3.4 为什么 method validation 能生效？（对比）

| 写法 | 校验器看到的 | 结果 |
|---|---|---|
| `@Valid @RequestBody List<X>`（DataBinder 路径） | 裸 List（参数注解丢失） | ❌ 不级联 |
| `@Valid @RequestBody Wrapper`（字段 `@Valid List<X> list`） | Wrapper 对象，字段注解可见 | ✅ 级联到元素 |
| 类标 `@Validated` + 参数 `@Valid`（method validation 路径） | `MethodValidationInterceptor` 拿着 method 元数据调 `validateParameters` | ✅ 能读参数注解并级联 |

方法级校验走的是 **AOP 拦截器**，`ExecutableValidator.validateParameters(bean, method, args)` 手里有完整的**方法签名**，参数注解不会丢失；而 `@RequestBody` 走的是 DataBinder，两条路不互通。

> **所以这不是 Spring 的 bug，也不是 HV 的 bug**，而是：`@Valid` 在参数上是给 method validation 用的；MVC 只借用它当"要不要校验"的开关，级联意图在 DataBinder 路径上被丢掉了。

### 3.5 生活比喻

把一批请求比作**一箱苹果**，每个苹果上贴着"烂的不要"（= 元素字段上的校验注解）：

- **现在的写法** = 快递单上写"帮我检查这箱苹果"。快递员（Spring）看到要求，把箱子端给质检员。质检员只看箱子本身——箱子是普通塑料筐，筐上没标签；苹果上的标签？**他根本没翻开看**（质检员只看物品上的标签，看不到快递单）。→ 一箱烂苹果放行了 ❌
- **正确写法** = 箱子隔层上贴着"检查每一层里的每个苹果"（= `Wrapper { @Valid List<X> list }` 字段上的 `@Valid`）。质检员一层层打开，看到隔层上的标签才逐个检查 → 烂苹果被拦下 ✅

---

