# 📚 Spring 事务失效排查手册

> **适用场景**：Spring/Spring Boot 项目中 `@Transactional` 标注的方法,异常时未回滚,数据被脏写。
> **一句话结论**：按"现象 → 注解 → 调用链 → 异常 → 环境"五层排查,90% 的问题集中在前 4 层;最常见的是 **自调用绕过代理**、**异常被吞**、**checked 异常不回滚** 三类。
> **沉淀日期**:2026-08-23

---

## 0. 排查总览(五层递进)

| 层次 | 排查方向 | 命中率 | 关键动作 |
|---|---|---|---|
| ① 现象 | 数据真的没回滚吗 | — | 开 SQL + 事务日志,看 `rollback` 是否执行 |
| ② 注解 | `@Transactional` 配得对不对 | ★★★★ | public 方法?rollbackFor?传播行为? |
| ③ 调用链 | 代理有没有被绕过 | ★★★★★ | 是否 `this` 自调用?是否跨线程? |
| ④ 异常 | 异常有没有被吞 | ★★★★ | try-catch 后有没有重新抛出? |
| ⑤ 环境 | Bean/数据源/引擎 | ★★ | 类被 Spring 管理?多数据源?InnoDB? |

> 经验:线上 9 成事务失效问题落在 ②③④ 三层。优先从这里查起。

---

## 1. 第一层:确认现象(别被假象骗了)

排查前先确认"事务失效"是真的,而不是别的现象误判:

- **数据真的没回滚?** 有时是主从库延迟、连接池读到旧数据、或客户端缓存导致的假象
- **看日志有没有 `Rolling back`**:Spring 事务回滚会打 `Initiating transaction rollback` / `Rolling back JDBC transaction`
- **开 SQL 日志**:确认数据库层面到底有没有执行 `rollback`

```yaml
# application.yml —— 快速定位事务行为的日志开关
logging:
  level:
    org.springframework.jdbc.datasource: DEBUG   # 数据源
    org.springframework.transaction: DEBUG        # 事务管理器(看 rollback / commit)
    org.springframework.orm.jpa: DEBUG            # JPA(若用)
    org.hibernate.SQL: DEBUG                      # SQL 语句
    org.hibernate.engine.transaction: TRACE       # 事务边界
```

```yaml
# MyBatis/MyBatis-Plus 打印执行的 SQL(含是否回滚)
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

> 看到 `rollback` 却没回滚 → 转第五层(数据源/引擎);完全没 `rollback` → 转第二~四层(注解/调用链/异常)。

---

## 2. 第二层:检查 @Transactional 注解本身

### 2.1 方法必须是 public(且非 final/static)

Spring AOP 代理**默认只拦截 public 方法**。`@Transactional` 加在 protected/private/final/static 方法上时**静默失效**,不会报错,也不会开事务。

```java
@Service
public class OrderService {
    @Transactional
    void pay() { ... }   // ❌ 包级私有,代理不拦截,事务失效(且静默)
}
```

> CGLIB 代理下 `final` 方法/类也会失效(JDK 动态代理则要求实现接口)。

### 2.2 注解加在哪里

| 加注解的位置 | 是否生效 | 说明 |
|---|---|---|
| 实现类的 public 方法 | ✅ | 最推荐 |
| 实现类类上 | ✅ | 该类所有 public 方法生效 |
| 接口方法上 | ⚠️ | 仅 JDK 动态代理生效;CGLIB 代理不生效 |
| 接口类上 | ⚠️ | 同上,Spring 官方不推荐 |

### 2.3 rollbackFor —— checked 异常默认不回滚

这是高频坑。`@Transactional` **默认只对 `RuntimeException` 和 `Error` 回滚**,checked exception(如 `IOException`、`SQLException` 视签名而定)**不回滚**。

```java
// ❌ checked exception 不回滚
@Transactional
public void doIt() throws Exception {
    throw new IOException();  // 不回滚!
}

// ✅ 显式指定回滚异常类型
@Transactional(rollbackFor = Exception.class)
public void doIt() throws Exception {
    throw new IOException();  // 回滚
}
```

> 经验:线上统一用 `@Transactional(rollbackFor = Exception.class)`,避免漏网。

### 2.4 propagation 传播行为

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // ❌ 以非事务方式执行
@Transactional(propagation = Propagation.NEVER)            // ❌ 存在事务则抛异常
@Transactional(propagation = Propagation.SUPPORTS)         // ⚠️ 有事务则加入,没有就非事务
```

`REQUIRED`(默认)才会在没有事务时新建。如果被设成了上面几种,本方法不会真正开启事务。

---

## 3. 第三层:检查调用链(代理是否被绕过)

`@Transactional` 靠 Spring AOP 代理生效。一旦调用没经过代理对象,注解就形同虚设。

### 3.1 自调用(this 调用)—— 最高频坑

类内部 A 方法直接调本类 B 方法,走的是 `this` 而非代理对象,**B 上的 `@Transactional` 失效**:

```java
@Service
public class OrderService {

    public void outer() {           // 没有事务
        this.inner();               // ❌ this 调用,绕过代理,inner 的 @Transactional 失效
    }

    @Transactional(rollbackFor = Exception.class)
    public void inner() {           // 期望开事务,实际没开
        saveA();
        saveB();                    // 这里抛异常不会回滚 saveA
    }
}
```

**三种解法**:

```java
// 解法 1:注入自己(推荐,最直观)
@Service
public class OrderService {
    @Autowired
    private OrderService self;     // Spring 注入的是代理对象

    public void outer() {
        self.inner();              // ✅ 走代理,事务生效
    }
}

// 解法 2:暴露并使用当前代理
@Configuration
public class Config {
    @Bean
    public Advisor advisor() { /* 需开启 @EnableAspectJAutoProxy(exposeProxy = true) */ return null; }
}
// 启动类加:@EnableAspectJAutoProxy(exposeProxy = true)
// 调用处:
((OrderService) AopContext.currentProxy()).inner();  // ✅

// 解法 3:把事务方法拆到另一个 Bean(结构最清晰)
```

### 3.2 多线程调用 —— 事务不跨线程

Spring 事务通过 `ThreadLocal` 绑定线程。**新线程里的数据库操作不在原事务中**,父线程异常不会回滚子线程已提交的数据。

```java
@Transactional(rollbackFor = Exception.class)
public void batch() {
    saveA();
    new Thread(() -> saveB()).start();  // ❌ 子线程独立连接、独立事务,不受 batch() 控制
    saveC();                            // 抛异常只会回滚 A、C,回滚不了 B
}
```

> 排查信号:方法里出现 `new Thread`、`@Async`、`CompletableFuture`、线程池提交,就要警惕事务边界被打破。

---

## 4. 第四层:检查异常处理

事务靠**异常抛出到代理边界**才触发回滚。如果异常在方法内被 `catch` 住、没有重新抛出,代理感知不到异常,自然不回滚。

```java
@Transactional(rollbackFor = Exception.class)
public void pay() {
    try {
        riskyOp();                  // 抛异常
    } catch (Exception e) {
        log.error("支付失败", e);
        // ❌ 异常被吞,代理以为正常结束 → 不回滚
    }
    // 后续逻辑继续走……
}
```

**正确做法**——处理完该处理的(打日志、转换异常),一定要往外抛:

```java
@Transactional(rollbackFor = Exception.class)
public void pay() {
    try {
        riskyOp();
    } catch (Exception e) {
        log.error("支付失败", e);
        throw new RuntimeException(e);  // ✅ 重新抛出,事务才会回滚
        // 或抛自定义业务异常(继承 RuntimeException)
    }
}
```

> 排查信号:方法体内出现 `try { ... } catch (Exception e) { log... }` 却没有 `throw`,基本可判定异常被吞。

---

## 5. 第五层:检查 Bean 与环境

### 5.1 类是否被 Spring 容器管理

`@Transactional` 生效前提是**对象由 Spring 创建、调用时经过代理**。自己 `new` 出来的对象不走代理,事务不生效:

```java
public class Foo {
    @Transactional
    public void bar() { ... }
}

// 某处:
Foo foo = new Foo();   // ❌ 非 Spring Bean,事务完全不生效
foo.bar();
```

排查:类上有没有 `@Service` / `@Component` / `@Repository`?有没有被组件扫描到?

### 5.2 多数据源 / 事务管理器不匹配

多数据源场景下,`@Transactional` 默认用**主事务管理器**。如果操作的是另一个数据源,事务根本管不到:

```java
@Transactional  // 默认用 @Primary 的 DataSourceTransactionManager
public void pay() {
    orderMapper.insert(...);   // 主库
    logMapper.insert(...);     // 日志库(另一数据源) ← 不在事务内
}
```

排查:多数据源时需指定 `@Transactional(transactionManager = "logTransactionManager")`,或用 `@Primary` 标注主库事务管理器。

### 5.3 数据库引擎不支持事务

MySQL 的 `MyISAM` 引擎**不支持事务**,异常不会回滚。必须用 `InnoDB`:

```sql
-- 查看表的引擎
SHOW TABLE STATUS FROM your_db WHERE Name = 'your_table';

-- 改成 InnoDB
ALTER TABLE your_table ENGINE = InnoDB;
```

### 5.4 Bean 初始化阶段调用事务方法

在 `@PostConstruct` 或构造方法里调用事务方法时,**代理可能尚未完全就绪**,事务不生效:

```java
@Service
public class OrderService {
    @PostConstruct
    public void init() {
        this.pay();   // ⚠️ Bean 初始化阶段,代理未就绪,事务可能失效
    }
}
```

解法:延迟到容器就绪后执行,如用 `ApplicationRunner` / `@EventListener(ApplicationReadyEvent.class)`。

### 5.5 其他少见原因

- **`@Transactional` 与 `@Async` 组合**:异步方法在新线程执行,事务线程绑定丢失
- **ORM 的 flush/锁机制**:如 JPA 未 flush,异常前 SQL 没真正执行,看起来像"没回滚"实则是"没执行"
- **嵌套调用传播行为**:内层 `REQUIRES_NEW` 会挂起外层事务、独立提交,外层异常不影响内层

---

## 6. 排查 Checklist 速查

```
□ 方法是 public 且非 final/static?
□ 类被 Spring 容器管理(@Service/@Component)?
□ 注解加在实现类/方法上(而非仅接口)?
□ rollbackFor 是否覆盖到 checked 异常?
□ 传播行为是否真的需要/开启了事务?
□ 是否 this 自调用绕过了代理?
□ 是否跨线程(new Thread / @Async / 线程池)?
□ 异常有没有被 try-catch 吞掉、没重新抛?
□ 抛的是不是 RuntimeException(或配了 rollbackFor)?
□ 数据库引擎是 InnoDB(非 MyISAM)?
□ 单/多数据源的事务管理器是否匹配?
□ 是否在 Bean 初始化阶段(@PostConstruct)调用?
□ (开了 SQL + 事务日志)有没有真正执行 rollback?
```

---

## 7. 决定性实验:用日志验证代理是否生效

拿不准到底是"注解没生效"还是"异常没回滚"时,在事务方法里打断点或加日志,确认两件事:

```java
@Transactional(rollbackFor = Exception.class)
public void pay() {
    // ① 确认进了代理:看事务日志有没有 "Creating new transaction"
    log.info("pay 开始, 当前线程 = {}", Thread.currentThread().getName());

    saveA();

    // ② 故意抛异常,观察日志是否出现 "Initiating transaction rollback"
    if (true) {
        throw new RuntimeException("测试回滚");
    }
}
```

**判读**:

| 日志现象 | 结论 | 下一步 |
|---|---|---|
| 有 `Creating new transaction` + 有 `rollback` | 事务生效,排查方向在数据源/引擎(第五层) | 看是否多数据源、是否 InnoDB |
| 有 `Creating new transaction` + **无** `rollback` | 异常没抛到代理边界(第四层) | 检查 try-catch 是否吞异常 |
| **无** `Creating new transaction` | 注解压根没生效(第二/三层) | 检查 public/自调用/Bean 管理 |

> 比 `@Transactional` 看起来"对不对"更可靠的是日志里有没有那行 `Creating new transaction`——它就是代理生效的硬证据。
