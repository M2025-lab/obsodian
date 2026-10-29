# 📚 starlord Kafka 消息接入配置手册（配置项 + 使用 + 作用）

> 沉淀对象：edar-starlord 消息接收全链路（Kafka 集群配置 → Listener → 事件驱动框架 → Handler）
> 一句话结论：消息接入的配置分三层——**集群层在 Apollo（brokers/group-id），入口层在 Listener 注解（topic/工厂名/开关），路由层在 Handler 注解（eventType）**，三层各管一段，任一层配错消息就断在那一段。
> 沉淀日期：2026-10-28
> 关联笔记：[[📚 Kafka开发级知识手册]]（Kafka 通用概念看这篇）、[[10.工作/技术方案/edar-starlord-识别结果消息接收改造-技术方案]]（实战案例出处）

## 1. 全景链路：一条消息从 Kafka 到落库经过的配置

```text
┌─ 配置层 ─────────────────────────────────────────────────────────────┐
│ Apollo namespace「kafka-config」(KafkaConfigTemplateConfig.java:12   │
│                                  @EnableApolloConfig 引入)           │
│   kafka.config.clusterNN.consumer.properties.bootstrap.servers ─┐    │
│   kafka.config.clusterNN.consumer.properties.group-id          ─┤    │
│   kafka.config.clusterNN.consumer.properties.listener-names    ─┘    │
│                     │                                                │
│                     ▼ keboot starter 按配置组装容器工厂 bean          │
│   com.lianjia.infrastructure:spring-boot-starter-kafka               │
│   (edar-starlord-web/pom.xml:119-120)                                │
│   KebootKafkaProperties.config = Map<clusterNN, KafkaProperties>     │
│   KebootKafkaContainerFactory → 注册 bean "listenerFactoryClusterNN" │
└──────────────────────┬───────────────────────────────────────────────┘
                       ▼
┌─ 代码层 ─────────────────────────────────────────────────────────────┐
│ @KafkaListener(topics="${...topic}",                ← 订阅哪个队列    │
│     containerFactory="${...listener-names}")        ← 用哪个工厂      │
│ @ConditionalOnProperty(...)                         ← 消费总开关     │
│   public void consume(ConsumerRecord, Acknowledgment)                │
│       └─▶ eventDrivenSubscriber.persistAndHandleMessage(value)       │
│             ├─ Jackson 解析信封 → Event                               │
│             ├─ checkContainEventType   ← eventType 没注册？忽略       │
│             ├─ checkSameEventEnv       ← serverGroup 环境隔离         │
│             ├─ saveNewEvent → event_sub 表落库（eventId 唯一键去重）  │
│             └─ dispatch: 按 eventType 查路由表                        │
│                   └─▶ @EventType(bizType, serverName) Handler         │
│                       payloadData 按 Handler 泛型 T 反序列化          │
│                       RetryTemplate 包裹调 handleBiz                  │
│                       → 业务 service 落库                             │
└──────────────────────────────────────────────────────────────────────┘
```

三层配置的职责划分：

| 层 | 配置放哪 | 管什么 | 配错的后果 |
|-|-|-|-|
| 集群层 | Apollo `kafka-config` namespace | 连哪套 Kafka（brokers）、消费身份（group-id）、工厂 bean 名（listener-names） | 连不上集群 / 消费进度错乱 / 工厂 bean 找不到启动失败 |
| 入口层 | Listener 类注解 | 订阅哪个 topic、用哪个工厂、开关 | topic 错收不到消息；开关没开整个链路静默 |
| 路由层 | Handler 类注解 `@EventType` | eventType 三段 `orgName:serverName:bizType` | 消息落库但不处理，只有 `no match handler` warn |

## 2. 三要素详解：brokers / topic / group-id

### 2.1 brokers —— 连哪套 Kafka 集群

| 维度 | 说明 |
|-|-|
| 是什么 | Kafka 集群的服务器地址列表，如 `kafka01:9092,kafka02:9092` |
| 作用 | consumer 启动时与它建 TCP 连接、拉元数据（topic 分区分布）、拉消息 |
| 配在哪 | Apollo `kafka-config` namespace：`kafka.config.clusterNN.consumer.properties.bootstrap.servers`（keboot starter 读取的键名即 `bootstrap.servers`，已从 `KebootKafkaUtils` 字节码核实） |
| 代码怎么用 | **不直接用**。方式A 下被容器工厂封装；方式B 手写 Config 时显式注入：`propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)`（`ProgressTaskMaterialMsgKafkaConfig.java:79`） |
| 一个 clusterNN = 一套集群 | 项目现有 cluster01~36 各对应一套集群；接新消息先问上游 topic 建在哪套集群，已有 clusterNN 就直接复用 |

### 2.2 topic —— 消费哪个消息队列

| 维度 | 说明 |
|-|-|
| 是什么 | Kafka 里的逻辑队列名，上游往这个 topic 发，你从这个 topic 收 |
| 作用 | 决定 Listener 订阅哪个队列；**是唯一出现在业务代码注解里的三要素** |
| 配在哪 | Apollo 配置键（如 `kafka.config.utopia-pfd-ailab.producer.properties.topics` 或业务自定义键），代码里用占位符引用 |
| 代码怎么用 | `@KafkaListener(topics = "${配置键:默认值}")`，例：`XcsCollectionListener.java:29` 带默认值 `utopia-pfd-ailab`；`ScmOrderEventListener.java:29` 同款。不带默认值时配置缺失会导致启动失败（可用开关兜底，见 4.2） |
| 谁定 | **上游定**（消息发到哪你就得订哪），接入前必须对齐 |

### 2.3 group-id —— 以什么身份消费

| 维度 | 说明 |
|-|-|
| 是什么 | 消费组标识。同组多实例**分摊**分区（线上多 pod 一条消息只被一个实例消费）；不同组各自独立消费**全量**；offset（消费进度）按 组+topic+分区 记录 |
| 作用 | 决定负载均衡边界、消费进度归属、重放能力（换 group 或重置 offset 可重新消费） |
| 配在哪 | Apollo：`kafka.config.clusterNN.consumer.properties.group-id`；keboot starter 还支持 `enable-auto-generate-group-id=true` 自动生成（`KebootKafkaUtils` 字节码核实，按应用名等拼接） |
| 代码怎么用 | 方式A不碰；方式B显式注入：`@Value("${xxx.group-id}")` → `propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)`（`ShuttleOrderStatusKafkaConfig.java:28,49`） |
| 环境隔离 | 测试/线上必须不同 group，否则测试实例抢线上消息。事件驱动框架还有第二道防线：`checkSameEventEnv` 按 serverGroup 拒收非本环境事件（warn `reject event... not same env`） |

## 3. 容器工厂层：两种接入方式的配置项逐项解释

容器工厂（`KafkaListenerContainerFactory`）= "怎么创建 consumer" 的模板：brokers、group-id、并发数、反序列化器、ack 模式都在这里定。项目有两种产生方式。

### 3.1 方式A：clusterNN 模板式（主流，21+ 个 listener 在用）

**零 Java 配置代码**。链路：

1. Apollo 独立 namespace `kafka-config`，由 `KafkaConfigTemplateConfig.java:12` 的 `@EnableApolloConfig("kafka-config")` 引入；
2. keboot starter（`com.lianjia.infrastructure:spring-boot-starter-kafka`，`edar-starlord-web/pom.xml:119-120`）的 `KebootKafkaProperties.config` 是 `Map<String, KafkaProperties>`，把 `kafka.config.*` 下每个 key（clusterNN 或业务名如 `utopia-pfd-ailab`）绑定成一份标准 Spring `KafkaProperties`；
3. `KebootKafkaContainerFactory` 为每份配置构建 `ConcurrentKafkaListenerContainerFactory`，按 `listener-names` 属性值注册 bean 名（如 `listenerFactoryCluster11`，见 `ScmOrderEventListener.java:29` 占位符默认值）。

Apollo 配置键速查：

| 键（`kafka.config.<clusterKey>.` 前缀） | 作用 |
|-|-|
| `consumer.properties.bootstrap.servers` | 集群地址（2.1） |
| `consumer.properties.group-id` | 消费组（2.3）；或 `enable-auto-generate-group-id=true` 自动生成 |
| `consumer.properties.listener-names` | 容器工厂 bean 名，Listener 的 containerFactory 占位符解析后的值 |
| `producer.properties.topics` | topic 名（约定放在同组配置里，供 Listener topics 占位符引用） |
| `consumer.properties.concurrency` | 每个 @KafkaListener 起几个消费线程（`KebootKafkaUtils.getListenerConcurrency`） |

### 3.2 方式B：手写 KafkaConfig 式（专属集群/需要定制参数时）

位置惯例：`edar-starlord-manager/.../manager/config/kafka/` 或 `servicev2/listener/config/`。一个 `@Configuration` 类产出一个或多个命名工厂 bean。配置项逐项（以 `BaseKafkaConfig.java:18-29` + `CallRecordKafkaConfig.java:19-47` 为准）：

| 配置项 | 项目取值 | 作用 |
|-|-|-|
| `BOOTSTRAP_SERVERS_CONFIG` | `@Value` 注入 | 集群地址（2.1） |
| `GROUP_ID_CONFIG` | `@Value` 注入 | 消费组（2.3） |
| `ENABLE_AUTO_COMMIT_CONFIG` | Base 默认 `true`；CallRecord 改 `false` | offset 自动提交与否；false 时必须配合手动 ack（见 4.5） |
| `AUTO_COMMIT_INTERVAL_MS_CONFIG` | Base `100`；ProgressTask `10000` | 自动提交间隔；间隔越大宕机时重复消费窗口越大 |
| `SESSION_TIMEOUT_MS_CONFIG` | `15000` | 心跳超时判定 consumer 死亡、触发 rebalance 的阈值 |
| `KEY_DESERIALIZER_CLASS_CONFIG` | `ByteArrayDeserializer` | key 反序列化（业务一般不用 key） |
| `VALUE_DESERIALIZER_CLASS_CONFIG` | `StringDeserializer` | value 按原始字符串进 Listener，JSON 解析在代码里做 |
| `AUTO_OFFSET_RESET_CONFIG` | `earliest` | 无历史 offset 时（新 group 首次上线）从头消费还是 latest；earliest=不漏消息但新 group 上线可能瞬间涌入存量 |
| `INTERCEPTOR_CLASSES_CONFIG` | `HawkKafkaConsumerInterceptor` + `com.ke.iprd.log.aop.KafkaConsumerInterceptor` | 公司监控埋点 + 消费日志拦截器 |
| `factory.setConcurrency(N)` | `5` | 消费线程数（≤ topic 分区数才有效，多了空闲） |
| `containerProperties.setPollTimeout(ms)` | `5000` | 单次 poll 阻塞上限 |
| `containerProperties.setAckMode(MANUAL_IMMEDIATE)` | CallRecord 用 | 手动 ack 模式：代码里 `acknowledgment.acknowledge()` 立即提交 offset |

参照类：`ProgressTaskMaterialMsgKafkaConfig.java:19-84`（一个 Config 类管 3 套集群 3 个工厂）、`CallRecordKafkaConfig.java:19-48`（手动 ack 完整示例）。

### 3.3 两种方式怎么选

| 维度 | 方式A clusterNN 模板 | 方式B 手写 Config |
|-|-|-|
| Java 代码量 | 0（纯 Apollo 配置） | 一个 Config 类 |
| 适用 | 上游集群已被公司 keboot 体系覆盖（绝大多数） | 集群不在模板体系 / 需要定制 ack 模式、拦截器 |
| 项目先例 | `XcsCollectionListener`、`ScmOrderEventListener` 等 21+ | `CallRecordListener`、manager 下 10 个 Config |
| 本次识别结果改造 | ✅ 采用（TODO：上游定集群后把占位键换成 clusterNN 惯例键） | 备选 |

## 4. Listener 层：注解与方法签名逐项解释

以本次新增的 `RecognizeTaskResultListener.java` 为解剖对象（与 `XcsCollectionListener.java:22-38` 同款）：

```java
@Slf4j
@Service
@ConditionalOnProperty(name = "recognize.result.consume.enable", havingValue = "true")  // ①
public class RecognizeTaskResultListener {

    @Resource
    private EventDrivenSubscriber eventDrivenSubscriber;                                 // ②

    @KafkaListener(topics = "${recognize.result.kafka.topic}",                           // ③
            containerFactory = "${recognize.result.kafka.listener-names}")               // ④
    @EntryTrace                                                                          // ⑤
    public void consume(ConsumerRecord<String, String> consumerRecord,                   // ⑥
                        Acknowledgment ack) {                                            // ⑦
        try {
            eventDrivenSubscriber.persistAndHandleMessage(consumerRecord.value());       // ⑧
        } catch (Exception e) {
            log.error("... consume error, value:{}", consumerRecord.value(), e);
        } finally {
            ack.acknowledge();                                                           // ⑨
        }
    }
}
```

| # | 配置/元素 | 作用与注意点 |
|-|-|-|
| ① | `@ConditionalOnProperty` | 消费总开关（Apollo）。**启动时判定一次**，改配置需重启实例才生效；缺省不配=不装配（安全默认）。承担灰度上线、秒级止血、保护未就绪的 topic 占位配置三个职责 |
| ② | `EventDrivenSubscriber` | 事件驱动框架入口 bean（`utopia-event-driven-core`），负责信封解析→落库→分发，见第 5 节 |
| ③ | `topics` 占位符 | 订阅的 topic（2.2）。`${key:默认值}` 语法，无默认值且配置缺失时启动报错——但被①保护：开关关=bean 不创建=不解析 |
| ④ | `containerFactory` 占位符 | 引用容器工厂 bean 名（3.1），值来自 Apollo `listener-names` |
| ⑤ | `@EntryTrace` | SkyWalking 链路追踪：把消费标记为入口 span，消息链路在 APM 上独立成 trace |
| ⑥ | `ConsumerRecord<String, String>` | value 是 `StringDeserializer` 出的原始 JSON 字符串（3.2），**不在容器层做对象反序列化**，JSON 解析交给框架/代码 |
| ⑦ | `Acknowledgment` | 手动 ack 句柄，仅当工厂 AckMode 为 MANUAL/MANUAL_IMMEDIATE 时可用 |
| ⑧ | `persistAndHandleMessage` | 一行完成"落库+分发"，Listener 保持哑管道，业务全部在 Handler |
| ⑨ | `finally ack` | 无论处理成败都提交 offset：失败消息**不靠 Kafka 重投**，靠 event_sub 表状态（ERROR）做事后补偿/重放——这是事件驱动框架的可靠性设计，见 5.2 |

ack 语义对比（项目里两种流派）：

| 流派 | 代表 | 异常时行为 | 适用 |
|-|-|---|-|
| 吞异常 + finally ack | `XcsCollectionListener.java:32-38`、本 Listener | offset 照常提交，不重复消费；排障靠日志 + event_sub | 走事件驱动框架（已有落库补偿），主流 |
| 异常上抛 + finally ack | `AbstractReceiver.java:28-41`（`Throwables.propagate`） | 同样提交 offset，但异常进容器错误处理/日志更醒目 | 同上，历史写法 |
| 手动 ack + 自动提交关 | `CallRecordListener.java:56-61`（方式B工厂） | 不 ack 则不提交，重启后重复消费 | 未接事件驱动框架、需 Kafka 层重投兜底 |

## 5. 事件驱动框架层（utopia-event-driven-core 1.3.8，字节码已核实）

### 5.1 消息信封格式（上游必须按此发）

```json
{
  "eventId": "全局唯一，去重键",
  "eventType": "utopia:<serverName>:<bizType>",
  "eventOccurTime": "2026-10-28T10:00:00.000Z",
  "eventContextKey": "透传给 Handler 的业务上下文键，可空",
  "metadata": {},
  "payloadData": { "业务负载，结构由 Handler 泛型决定": "..." }
}
```

| 字段 | 作用 |
|-|-|
| `eventId` | event_sub 表唯一键，重复投递时 `saveNewEvent` 唯一键冲突 → warn `insert event sub key warn` → 去重不重复处理 |
| `eventType` | **路由键**，三段 `orgName:serverName:bizType`（`EventTypeUtils.generateEventType` 拼接，`orgName` 缺省 `"utopia"`）；须与 Handler `@EventType` 注册值字符串完全相等 |
| `eventContextKey` | 原样传入 `handleBiz` 第二参数 |
| `payloadData` | 业务负载 JSON，框架按 Handler 泛型 T 反序列化成强类型对象 |

### 5.2 `persistAndHandleMessage(String)` 六步流程

```text
1. Jacksons.readValue(json, Event.class)        ← Jackson(ObjectMapper) 解析信封
2. checkContainEventType(eventType)             ← eventType 未在本应用注册 → 直接忽略（不报错）
3. checkSameEventEnv(event, properties)         ← serverGroup 环境隔离，不匹配 warn "reject event... not same env"
4. saveNewEvent → EventSubMapper.insertSelective ← 事件落 event_sub 表；eventId 重复 → 唯一键冲突去重
5. doSubscribe → DomainEventDispatcher.dispatch  ← 按 eventType 查路由表，命中则调 handleBiz（5.3）
6. CAS 更新 event_sub 状态 → DONE / 异常 → ERROR  ← ERROR 事件可事后补偿重放
```

可靠性设计的分工：**Kafka 层不重投**（Listener finally ack），**可靠性由 event_sub 表承担**（每条消息必落库，失败标 ERROR 可重放）——所以 Listener 敢吞异常。

### 5.3 @EventType 注册与路由机制

| 阶段 | 机制 |
|-|-|
| 启动注册 | `DomainEventDispatcher`（`InitializingBean`）`afterPropertiesSet()` 扫描容器内所有 `DomainEventHandler` bean → 读实现类 `@EventType(orgName/serverName/bizType)` → 拼 eventType key 存入 `ConcurrentHashMap<String, List<DomainEventHandler>>`；同时把每个 Handler 的**泛型参数 T** 缓存为 `ResolvableType` |
| 消费路由 | `dispatch`：`map.get(eventSub.getEventType())` → 未命中 warn `no match handler`（消息已落库但静默不处理）→ 命中则遍历 Handler 列表 |
| 反序列化 | `Jacksons.readValue(payloadData字符串, 泛型T的Type)`——**`DomainEventHandler<T>` 的泛型声明就是反序列化契约**，Jackson 体系，DTO 上的 `@JsonFormat` 等注解生效 |
| 重试 | `RetryTemplate.execute(() -> handleBiz(...))`，次数由 `EventDrivenProperties.maxAttempts` 配置 → **handleBiz 必须幂等** |

推论：Handler 加 `@ConditionalOnProperty` 关闭时 bean 不存在 → eventType 不注册 → 消息在第 2 步就被忽略，开关对 Listener/Handler 双端自洽。

### 5.4 框架自身配置项（`EventDrivenProperties`）

| 配置 | 作用 |
|-|-|
| `serverGroup` | 环境隔离标识（checkSameEventEnv 用） |
| `maxAttempts` | handleBiz 重试次数 |
| `transactionEnable` / `bizTypeListOfIsolationTransaction` | 落库+分发是否包事务 / 指定 bizType 走独立事务 |

## 6. 实战案例：识别结果消息接入的完整配置清单

本次 `/api/recognize/task/result` 改消息接收（详见 [[10.工作/技术方案/edar-starlord-识别结果消息接收改造-技术方案]]）涉及的全部配置：

| 配置 | 位置 | 当前状态 |
|-|-|-|
| `recognize.result.consume.enable` | Apollo | 待新增，默认不配=关 |
| `recognize.result.kafka.topic` | Apollo | 占位键，待上游定 topic 后换成 `kafka.config.clusterNN.producer.properties.topics` 惯例键 |
| `recognize.result.kafka.listener-names` | Apollo | 占位键，待换成 `kafka.config.clusterNN.consumer.properties.listener-names` |
| clusterNN 的 brokers/group-id | Apollo `kafka-config` namespace | 上游定集群后：已有 clusterNN 直接复用；新集群新增一组 |
| `@EventType(bizType="recognize-task-result", serverName=...)` | `RecognizeTaskResultHandler.java` | bizType 已定；serverName 占位 `TODO-upstream-server-name` 待替换 |
| payloadData 结构 | 上游按 `NotifyTaskResultParam.java:39-68` 字段发 | 日期 `yyyy-MM-dd` 字符串（Jackson `@JsonFormat` 已核实兼容） |

消息样例（上游照此发）：

```json
{
  "eventId": "recognize-20261028-0001",
  "eventType": "utopia:<上游服务名>:recognize-task-result",
  "eventOccurTime": "2026-10-28T10:00:00.000Z",
  "eventContextKey": "",
  "metadata": {},
  "payloadData": {
    "projectOrderId": 59506381124583424,
    "materialCode": "029001001",
    "taskType": 3,
    "recognizeTime": "2026-10-27",
    "recommendVisitTime": "2026-10-30",
    "judgeResult": 1,
    "judgeRemark": "可通知",
    "images": ["https://...jpg"]
  }
}
```

## 7. 新消息接入 SOP

1. **与上游对齐五要素**：topic、所在 Kafka 集群（clusterNN）、serverName、bizType、payloadData 字段结构；group-id 复用已有集群则不用申请；
2. **选方式**：集群被 keboot 模板覆盖 → 方式A（零 Config 代码）；需定制 ack/拦截器 → 方式B 手写 Config；
3. **写 Handler**（`service/listener/handler/`）：`@EventType` + `DomainEventHandler<负载DTO>`，`handleBiz` 开头 `KafkaEventLog.logReceive`，手工必填校验（消息链路没有 `@ValidList`），保证幂等（框架会重试）；
4. **写 Listener**（`service/listener/`）：`@ConditionalOnProperty` 开关 + `@KafkaListener` + `persistAndHandleMessage` + finally ack，抄 `XcsCollectionListener`；
5. **Apollo 配置**：开关（默认关）、topic 键、listener-names/clusterNN 引用；新集群才加 brokers/group-id；
6. **联调**：发一条样例消息 → 查 event_sub 表有记录且状态 DONE → 查业务表落库 → 若状态 DONE 但业务没动，搜日志 `no match handler`（eventType 不匹配）；
7. **灰度**：开开关单实例观察 → 全量；
8. **止血预案**：关开关（需重启生效）；消息继续积压在 topic 与 event_sub，恢复后可重放。

## 8. 常见坑速查

| 坑 | 现象 | 根因 | 对策 |
|-|-|-|-|
| serverName/bizType 不匹配 | 消息落了 event_sub 但业务没处理，日志只有 `no match handler` warn | eventType 字符串完全相等匹配，占位符没替换 | 上线前核对 `@EventType` 与上游实际 eventType 逐字符一致 |
| 开关改了不生效 | Apollo 改 true 后还是不消费 | `@ConditionalOnProperty` 启动时判定一次 | 重启实例 |
| 校验注解不生效 | 非法字段直接落库/NPE | 消息链路无 Controller `@ValidList`；且 `@Valid` 加 List 根对象本就不校验元素（[[📚 批量List接口@Valid校验失效排查与解决方案]]） | Handler 手工判空 |
| 消息重复处理 | 同 eventId 业务被执行两次 | handleBiz 抛异常触发 RetryTemplate 重试，业务不幂等 | 落库用 insert-or-update/CAS，消费前判状态 |
| 测试抢线上消息 | 线上消息"丢了" | 两环境共用 group-id | group 按环境隔离；框架 serverGroup 校验是第二道防线不是万能 |
| 启动失败 placeholder 解析报错 | `Could not resolve placeholder 'xxx.topic'` | topic 占位符无默认值且 Apollo 没配，开关又是开的 | 配置先行再开开关；或占位符给默认值 |

## 9. 一句话记忆

> **brokers 找集群、topic 找队列、group-id 定身份；工厂管消费参数、Listener 管收、eventType 管分发；开关一关全链路静默，落库补偿代替 Kafka 重投。**

- 配置分三层：Apollo 集群层 / Listener 入口层 / Handler 路由层，哪层断在哪层查；
- 排障顺序：event_sub 表有没有 → 状态 DONE 还是 ERROR → `no match handler` warn → Handler 内业务日志（`KafkaEventLog`）；
- 代码零改动的接入 = 方式A：只在 Apollo 加配置 + 两个注解类。

---
---
