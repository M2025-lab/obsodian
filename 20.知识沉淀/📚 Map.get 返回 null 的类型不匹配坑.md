# 📚 Map.get(key) 明明有值却返回 null：key 类型不匹配的坑

> **踩坑对象**：`Map<Long, String>` 用 `String` 类型的 key 去 `get`，明明日志/ debug 看到 key 存在，取出来却是 `null`。
> **一句话结论**：`Map.get` 用的是 `equals` + `hashCode` 精确匹配,**`Long` 的 `1L` 和 `String` 的 `"1"` 不是同一个对象、`equals` 返回 `false`**,所以 map 里确实有那条记录,但用错类型的 key 去取,得到的就是 `null`——不是 map 没值,是 key 对不上。
> **沉淀日期**:2026-08-25
> **关联**:[[📚 String.join 传 null 抛 NPE 的坑与判空方案]](同属「Map / 集合取值的坑」系列)

## 1. 问题现象

业务里把项目信息组装成 map,后续根据项目地址取值透传:

```java
// 组装:项目信息里 id 是 Long,地址是 String
Map<Long, String> projectAddrMap = new HashMap<>();
for (Project p : projects) {
    projectAddrMap.put(p.getId(), p.getAddress());   // p.getId() 是 Long
}

// 取值:某处拿到的 projectId 是 String(比如从 URL / JSON / 前端入参来的)
String projectId = "10086";                           // ← String,不是 Long
String addr = projectAddrMap.get(projectId);          // ← 返回 null!
```

**诡异点**:

- debug 看 `projectAddrMap` 里**确实有** key=`10086`(Long 类型)对应的地址。
- 直接 `projectAddrMap.get(10086L)` 能取到,换成 `projectAddrMap.get("10086")` 就是 `null`。
- 日志里打 `projectId` 看着也是 `10086`,和 map 里的 key"长得一模一样",肉眼根本看不出区别。

这就是典型被"值相等但类型不同"骗了的坑——**`Map` 不关心值在语义上等不等,只关心 `equals` 和 `hashCode` 是不是同一个 key 对象**。

---
## 2. 根因分析(HashMap 源码)

`HashMap.get(Object key)` 的核心逻辑(JDK 8 `HashMap.java:562`):

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    // ...
    // ① 先比 hash(定位桶)
    // ② 再比 == 或 equals(确认是同一个 key)
    if (first.hash == hash &&                  // hash 相等
        ((k = first.key) == key ||               // 同一对象,或
         (key != null && key.equals(k))))      // equals 相等
        return first;
    // ... 遍历链表 / 红黑树
    return null;                               // ← 没匹配上就返回 null
}
```

**关键在两道关**:

1. **hash 关**:`hash(key)` 由 `key.hashCode()` 算出。`Long(10086L).hashCode()` 和 `String("10086").hashCode()` 是两套完全不同的算法,算出的 int 几乎不可能相等 → **大概率直接落在不同的桶里**,连 `equals` 这一步都走不到,直接 `return null`。
2. **equals 关**(即使 hash 撞巧相等,这关也过不去):`Long.equals(Object)` 的实现是先 `instanceof Long` 再比值:

   ```java
   public boolean equals(Object obj) {
       if (obj instanceof Long) {              // ← String 进来直接 false
           return value == ((Long)obj).longValue();
       }
       return false;
   }
   ```

   传进来的 `key` 是 `String`,`instanceof Long` 为 `false`,直接返回 `false` → 不匹配 → 返回 `null`。

> **所以"map 里有值但取出 null"的真相**:不是 map 没存进去,是**取值的 key 和存值的 key 不是同一类型**,在 `hash` 或 `equals` 阶段就被判为"不是同一个 key"。Java 的 `Map` 是**类型敏感**的——`Map<Long, V>` 用 `String` 取,编译器不报错(`get` 的参数是 `Object`,这是历史设计,见 §5),但运行时永远取不到。

---
## 3. 类型匹配矩阵(实测,JDK 8)

```java
Map<Long, String> map = new HashMap<>();
map.put(1L, "地址A");
```

| # | 调用 | 结果 | 说明 |
|---|---|---|---|
| A | `map.get(1L)` | ✅ `"地址A"` | 同类型 Long,正常命中 |
| B | `map.get(1)` | ❌ `null` | **`int` 自动装箱成 `Integer`,不是 `Long`** → 类型不匹配 |
| C | `map.get("1")` | ❌ `null` | String ≠ Long,hash 和 equals 都过不去 |
| D | `map.get(Integer.valueOf(1))` | ❌ `null` | Integer ≠ Long,同 B |
| E | `map.get(Long.valueOf(1))` | ✅ `"地址A"` | 显式 Long,正常 |
| F | `((Object)1L).equals("1")` | `false` | Long.equals(String) 永远 false |
| G | `1L == 1` | ✅ `true`(编译期) | **陷阱!** `==` 会做基本类型拓宽比较,看着相等,别拿来类比 `equals` |
| H | `map.containsKey("1")` | `false` | `containsKey` 同理,也走 hash + equals |

**两条铁律**:

1. **存什么类型,取就得什么类型**:`Map<Long, V>` 只能用 `Long` 取,`int` / `Integer` / `String` 都取不到。最隐蔽的是 B——`get(1)` 传字面量,自动装箱成 `Integer`,编译器不吭声,运行时返回 `null`。
2. **`==` 和 `equals` 对数字的行为不同,别混用**:`1L == 1` 是 `true`(基本类型拓宽),但 `Long.valueOf(1).equals(Integer.valueOf(1))` 是 `false`(不同类型)。看代码时不要因为 `==` 成立就以为 `equals` / `Map.get` 也能命中。

---
## 4. 解决方案

### 4.1 方案选型

| 方案 | 做法 | 适用 | 缺点 |
|---|---|---|---|
| ① 统一 key 类型 | 入口处把 `String` 转 `Long` 再存 / 取 | **首选**,根治 | 需确认 id 一定是数字,非数字要兜底 |
| ② 统一用 String 做 key | map 声明成 `Map<String, V>`,存时 `String.valueOf(id)` | id 来源杂、混有非数字时 | 需统一全链路,别一处 Long 一处 String |
| ③ 取值时转类型 | `map.get(Long.parseLong(projectId))` | 临时修复、改动面小 | 每处取值都要转,易遗漏;parse 失败会抛异常 |
| ④ 用 String key + Number 统一 | 全用 `String` key,`get` 前 `String.valueOf()` | 跨服务、JSON 来回转的场景 | 同②,注意一致性 |

> **推荐**:能在入口收敛就收敛——**项目内 id 统一一种类型**(业务项目通常 `Long` 更自然),所有 `Map` / 接口入参 / 出参都用它。临时救火用③,但要在代码里留注释说明为什么这里要转。

### 4.2 代码示例

```java
// ① 根治:入口统一转 Long(推荐)
Map<Long, String> projectAddrMap = new HashMap<>();
// ... 组装时 put(p.getId())   ← p.getId() 是 Long,OK
Long projectIdLong = Long.parseLong(projectId);   // 入口转好,parse 失败要兜底
String addr = projectAddrMap.get(projectIdLong);   // ✅ 命中

// ② 全链路用 String key(适合 id 来源杂的场景)
Map<String, String> projectAddrMap = new HashMap<>();
for (Project p : projects) {
    projectAddrMap.put(String.valueOf(p.getId()), p.getAddress());
}
String addr = projectAddrMap.get(projectId);       // ✅ projectId 本来就是 String

// ③ 临时修复:取值处转类型
String addr = projectAddrMap.get(Long.parseLong(projectId));
// ⚠ 注意:projectId 不是合法数字时 Long.parseLong 抛 NumberFormatException
//   更稳的写法:先校验 / 用 try-catch / 用 NumberUtils.toLong(projectId, -1L) 兜底
```

### 4.3 防御性写法:取出来永远别直接用

不管 key 类型对不对,`Map.get` 本身就可能返回 `null`(key 不存在)。取出来用之前**务必判空**:

```java
String addr = projectAddrMap.get(projectIdLong);
if (StringUtils.isBlank(addr)) {
    // 兜底:给默认值 / 抛业务异常 / 记日志告警
    throw new BizException("项目地址不存在, projectId=" + projectIdLong);
}
```

> 这一步和 key 类型问题是两码事,但经常一起出现——key 类型错了返回 `null`,你以为是"没数据",其实是"取错 key"。所以排查时两个方向都要看。

---
## 5. 易混淆点

### 5.1 为什么 `Map.get` 不直接报类型错?

`Map.get` 的签名是 `V get(Object key)`,参数是 `Object` 而不是泛型 `K`。这是 JDK 的历史设计(泛型是 JDK 5 才加的,`get(Object)` 为了向后兼容一直没改)。**后果**:`map.get("任意类型")` 编译期不会报错,运行时静默返回 `null`。编译器帮不了你,只能靠类型自觉。

> 这也是为什么 IDE / 代码扫描有时会提示 "suspicious get with wrong type"——它发现你 `get` 的参数类型和 `Map` 声明的 key 类型对不上,但 Java 语言层面不强制。

### 5.2 `equals` 和 `==` 在数字上的差异

最容易记混的一张表:

| 比较 | `1L == 1` | `Long.valueOf(1L).equals(Integer.valueOf(1))` | `new Long(1L).equals(new Long(1L))` |
|---|---|---|---|
| 结果 | `true` | `false` | `true` |
| 原因 | 基本类型拓宽,直接比数值 | 不同类型,`equals` 先 `instanceof` 判类型 | 同类型,比 `longValue()` |

**口诀**:`==` 对基本类型只看数值(会拓宽),`equals` 对包装类型先看类型再看数值。Map 走的是 `equals`,所以类型不对一律不命中。

### 5.3 String 类型的 key 会不会有同样问题?

会,而且更隐蔽。`Map<String, V>` 用 `"1"`(String)取,看着应该没问题,但如果存的 key 是 `"1"` 而你取的是 `1`(int 装箱成 Integer),照样返回 `null`。**只要存取类型不一致就中招**,和具体是 Long 还是 String 无关——本案例只是恰好落在 Long/String 上。

---
## 6. 生活比喻

把 `Map` 想成一个**带编号的储物柜**:

- **hash** = 柜号。每个 key 算出一个柜号,东西放进几号柜。
- **equals** = 核对柜号后,再核对"这把钥匙和柜子标签是不是同一个东西"。

你存的时候用的是**门禁卡**(Long 类型的 id,卡片上印着 `10086`),柜子系统记下"10086 号柜、卡片类型=门禁卡"。

你取的时候递进去一张**便签纸**(String 类型的 `"10086"`),纸上也写着 `10086`。柜子系统:

1. 先看便签该进几号柜 → 算出来柜号和门禁卡的柜号**对不上**(hash 不同) → 直接告诉你"没这柜子"(返回 null)。连开柜这步都没到。
2. 就算撞大运柜号对上了,它再核对"便签纸 ≠ 门禁卡"(类型不同,equals false) → 还是"这不是你的柜子" → 返回 null。

**柜子不在乎"10086"这个数字长一样,它在乎"钥匙是什么东西做的"。** 门禁卡和便签纸虽然都写着 10086,但在柜子眼里是两种完全不同的钥匙。

修复办法很简单:**取的时候也递门禁卡**(把 String 转回 Long),或者**从一开始柜子就只认便签纸**(全链路统一用 String key)。

---
## 7. 一句话记忆

> **存取类型要一致,Long 别用 String 取;get 返回 null 先查 key 类型,再看有没有值。**

- **现象**:map 里明明有值,`get` 出来是 `null`,debug 看着 key 还一模一样。
- **根因**:`Map.get` 走 `hash` + `equals`,`Long` 和 `String` 的 hash 算法不同、`equals` 跨类型返回 `false`,key 对不上。
- **陷阱**:`get(int)` 会装箱成 `Integer` 不是 `Long`;`1L == 1` 成立但 `Long.equals(Integer)` 不成立——别拿 `==` 类比。
- **根治**:入口统一 key 类型(项目内 id 统一 `Long`),取值前转好类型,取出来再判空。
- **定位口诀**:看到 `map.get(x)` 返回 null,先 `map.containsKey(x)` 验一下;若 `containsKey` 也 false 但日志里有值,**九成是 key 类型不匹配**。

---
---
