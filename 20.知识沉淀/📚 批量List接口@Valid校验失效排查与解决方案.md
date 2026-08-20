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

## 4. 解决方案（三选一，按侵入性排序）

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| ① Wrapper 包一层 | 参数改成 `Wrapper { @Valid @NotEmpty List<X> list }` | 零代码、HV 原生支持 | **改变 HTTP 契约**，前端/Feign 调用方全要改 |
| ② service 手动校验 | service 注入 `javax.validation.Validator`，循环 `validator.validate(x)` | 侵入小、可控 | 每个接口都要写一遍样板代码，易漏 |
| ③ **AOP 框架 @ValidList**（最终采用） | 自定义注解 + 切面，参数标 `@ValidList` 即自动逐条校验 | 声明式、一处实现全局生效、未标注接口零影响 | 有切点/代理的坑（见 §5） |

> 2026-08-20 起项目内已落地方案③：`@ValidList` + `ValidListAspect`（`edar-starlord-web/.../com/ke/utopia/config/`），编译通过、已 `git add`。**接口侧用法**：Feign 接口上的 `@Valid` 保留不动（只校验根对象），在 **Controller 实现类方法参数**上补 `@ValidList` 即生效。`allowEmpty()` 默认 false（空集合报 4004「第N个参数不能为空」），true 时空集合/null 放行。

---

## 5. AOP 框架落地踩坑（2026-08-20 实测，全部踩过）

### 5.1 坑一：切点表达式——项目没有 `controller` 包层级

`execution(* com.ke.utopia..controller..*(..))` **命中不到任何类**：全项目 Controller 都在 `com.ke.utopia.web` 及其 12 个子包（butler/manpower/materialflow/deliveryflow/panorama/selfbuy/coordinator/fix/foreman/mcp/trace/transfer），**根本不存在 `controller` 这个包层级**（仅 test 目录有）。

### 5.2 坑二：`within(@RestController *)` 依赖注解继承，CGLIB 代理后不可靠

`@RestController` **没有 `@Inherited`**，Controller 被 CGLIB 代理后，代理子类（`$$EnhancerBySpringCGLIB$$`）并不携带该注解，`within` 注解匹配存在命中不到的风险（这也是项目里原切面 `MethodLoggerAj` 坚持用 `execution` 的原因）。

**最终切点（双保险，推荐抄这个）**：

```java
@Around("execution(* com.ke.utopia.web..*.*(..)) || within(@org.springframework.web.bind.annotation.RestController *)")
```

- `execution(* com.ke.utopia.web..*.*(..))`：包路径匹配，100% 可靠、不依赖注解继承
- `within(@RestController *)`：兜底未来放包外的 controller
- 未标 `@ValidList` 参数的方法直接 `proceed()`，对现有接口零影响

### 5.3 坑三（最隐蔽）：CGLIB 代理类方法上的参数注解为空

`joinPoint.getSignature().getMethod()` 拿到的是 **CGLIB 代理类覆写的方法**——代理类方法**不会复制/继承方法参数注解**（注解不可继承）。就算切点命中了，用代理方法取 `Parameter[]` 也拿不到 `@ValidList`，切面会**静默放行**。

```java
// ❌ 拿不到注解：method 是代理类方法，参数注解为空
Method method = signature.getMethod();

// ✅ 必须剥掉代理后缀取原始 Controller 类
Method method = AopUtils.getMostSpecificMethod(signature.getMethod(),
        ClassUtils.getUserClass(joinPoint.getTarget()));
```

> 注意 `ClassUtils.getUserClass(target)` 而不是 `target.getClass()`——后者返回的仍是代理类。

### 5.4 坑四：进程没重启 = 切面静默失效（最容易被忽略）

切面是 Spring 启动扫描注册的 Bean，**启动之后新增的 class 不会自动进容器**。项目**没有 devtools 热部署**，改完代码不重启，`target/classes` 里文件再新也没用。

**铁证排查法（jcmd 看类加载）**：

```bash
jcmd <pid> GC.class_histogram | grep -iE "ValidList|MethodLogAspect"
```

- 有 `MethodLogAspect`（旧类）但**没有 `ValidListAspect`** → 进程跑的是旧代码，直接重启
- 若新类已加载但仍不生效，才需要往切点/代理链路挖

配套手段：
- 切面加 `@PostConstruct` 启动日志（如 `[ValidList] ValidListAspect 已加载`），bean 是否创建一眼可观测
- 标注接口加命中日志：`[ValidList] 命中校验: XxxController.method 参数数=N 错误数=M`
- **curl 要看 body 里的 `ResultDTO.code`，HTTP 状态码恒为 200**（`UtopiaExceptionHandler` 统一处理）；4004=ERROR_PARAM_ILLEGAL，5000 是兜底

### 5.5 使用约束

- `@ValidList` **只能标在 Controller 实现类方法参数上**：注解在 web 模块，api 模块 Feign 接口依赖方向 web→api **无法引用**（循环依赖）；Controller 实现方法需显式写注解，不能依赖 Feign 接口上的注解继承
- 若未来要让 Feign 接口参数也支持，需把注解下沉到 `edar-starlord-api` 模块（可选扩展，当前未做）
- AOP 只对 Spring 代理 bean 生效，Controller 内部 `this` 自调用拦不到（Controller 基本无此场景，可不处理）

### 5.6 切面校验逻辑要点

```java
// 校验顺序（固定，避免语义歧义）：
// null 视同空集合 → 走 allowEmpty 分支（true 放行 / false 报"第N个参数不能为空"）
// 非 null 且非 List → 报"参数类型必须为List"
// List 非空 → 逐条 validator.validate(item)，收集"第N个参数第J条数据[字段]消息"
// 有错误 → 抛 UtopiaBussinessException(ERROR_PARAM_ILLEGAL, String.join(";", errors))
```

错误出口复用现有 `UtopiaExceptionHandler` 统一转 4004，无需新增异常处理。

---

