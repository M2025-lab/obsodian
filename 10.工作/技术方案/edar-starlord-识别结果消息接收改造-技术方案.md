# edar-starlord-识别结果消息接收改造-技术方案

> 飞书文档：<链接占位，需要同步跟我说"同步飞书">

## 1、背景[必选]

- 需求文档：<占位>
- keones：<占位>
- 范围：**仅 starlord 侧消息接收链路**。`receiveTaskResult` 落库逻辑不改；上游识别系统的发消息改造属上游依赖（第 6 节）；老 HTTP 接口过渡期保留、切流后二期下线。

上游识别系统当前通过 `POST /api/recognize/task/result` 回传识别结果，本次改为 Kafka 消息接收，按项目事件驱动惯例（`utopia-event-driven-starter-kafka`，Listener + `@EventType` Handler）落地，获得事件落库、可审计、可重放能力。同类先例：`XcsCollectionListener`/`XcsCollectionHandler`（2025/7 接 pfd-ailab 识别类消息）。

## 2、需求理解[必选]

| # | 需求点 | 口径 |
|-|-|-|
| 1 | 改造对象 | `POST /api/recognize/task/result`（`NotifyTaskAutomationController.java:36-41`），上游识别结果落库入口 |
| 2 | 目标形态 | Kafka 消息接收，方案A：标准事件驱动信封（已确认） |
| 3 | 消息粒度 | 一条识别结果一条消息，payloadData = 单个 `NotifyTaskResultParam`（已确认，不沿用批量 List） |
| 4 | 老接口 | 过渡期保留，上游切流验证完成后二期下线（已确认） |
| 5 | 落库逻辑 | `NotifyTaskAutomationServiceImpl.receiveTaskResult` 不改，Handler 直接复用 |
| 6 | 上游服务名/topic/集群 | 未定，本方案占位 + TODO（已确认） |

## 3、现状

### 3.1 现有 HTTP 接收链路

```
上游识别系统 ──HTTP POST /api/recognize/task/result──▶ NotifyTaskAutomationController(:36-41)
   body: @ValidList List<NotifyTaskResultParam>              │
                                                             ▼
                     NotifyTaskAutomationServiceImpl.receiveTaskResult(:34)
                       ├─ Apollo projectOrderId 映射(:36-38)
                       ├─ selectByEntity(projectOrderId+materialCode+taskType)(:40-42)
                       ├─ 无记录 → insertSelective(:45)
                       │  有记录 → updateById(:55)          ← insert-or-update，天然幂等
                       └─ images 逗号拼接，最多前 9 张(:26,:74-84)
```

- Feign 定义：`NotifyTaskAutomationFeign.java:24-25`
- 入参 DTO：`NotifyTaskResultParam.java:35-68`（projectOrderId/materialCode/taskType/recognizeTime/recommendVisitTime/judgeResult/judgeRemark/images，日期 `@JsonFormat yyyy-MM-dd`）

### 3.2 项目消息接收惯例（事件驱动框架）

```
上游 ──kafka(标准信封)──▶ XxxListener(@KafkaListener)
                              │ eventDrivenSubscriber.persistAndHandleMessage(record.value())
                              ▼            （框架：事件落库 + 按 eventType 分发）
                    @EventType(bizType, serverName) 的 Handler
                    implements DomainEventHandler<T>
                              │ handleBiz(payloadData, eventContextKey)
                              ▼
                          业务 service
```

- 信封格式（`ProgressTaskMaterialMsgListener.java:38` 注释样例）：
  `{"eventId":"...","eventType":"utopia:<serverName>:<bizType>","eventOccurTime":"...","eventContextKey":"","metadata":{},"payloadData":{...}}`
- 参照实现：`XcsCollectionListener.java:22-38`（Apollo 开关 + `@KafkaListener` + finally ack）、`XcsCollectionHandler.java:19-32`（`@EventType` + `KafkaEventLog.logReceive` 统一消费日志）
- 框架依赖已具备：`utopia-event-driven-starter-kafka`（根 `pom.xml:479`、`edar-starlord-service/pom.xml:101`）
- Kafka 集群配置惯例：Apollo `kafka.config.clusterNN.*`（brokers/group-id/listener-names），`@KafkaListener` 的 containerFactory 引用 `${kafka.config.clusterNN.consumer.properties.listener-names}`，项目现有 cluster01~36

## 4、详细设计

### 4.1 新增 RecognizeTaskResultListener

位置：`edar-starlord-service/src/main/java/com/ke/utopia/service/listener/RecognizeTaskResultListener.java`（新增），完全参照 `XcsCollectionListener.java:22-38`：

```java
@Slf4j
@Service
@ConditionalOnProperty(name = "recognize.result.consume.enable", havingValue = "true")
public class RecognizeTaskResultListener {

    @Resource
    private EventDrivenSubscriber eventDrivenSubscriber;

    @KafkaListener(topics = "${kafka.config.<clusterNN>.producer.properties.topics:<topic占位>}",
                   containerFactory = "${kafka.config.<clusterNN>.consumer.properties.listener-names}")
    @EntryTrace
    public void consume(ConsumerRecord<String, String> consumerRecord, Acknowledgment ack) {
        try {
            eventDrivenSubscriber.persistAndHandleMessage(consumerRecord.value());
        } catch (Exception e) {
            log.error("RecognizeTaskResultListener consume error, value:{}", consumerRecord.value(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
```

> ⚠️ TODO（待与上游确认）：`<clusterNN>`/topic 占位，取决于上游 topic 建在哪个 Kafka 集群（见第 6 节）。

### 4.2 新增 RecognizeTaskResultHandler

位置：`edar-starlord-service/src/main/java/com/ke/utopia/service/listener/handler/RecognizeTaskResultHandler.java`（新增），参照 `XcsCollectionHandler.java:19-32`：

```java
@EventType(bizType = "recognize-task-result", serverName = "<上游服务名占位>")
@Component
@Slf4j
@ConditionalOnProperty(name = "recognize.result.consume.enable", havingValue = "true")
public class RecognizeTaskResultHandler implements DomainEventHandler<NotifyTaskResultParam> {

    @Resource
    private NotifyTaskAutomationService notifyTaskAutomationService;

    @Override
    @EntryTrace
    public void handleBiz(NotifyTaskResultParam payloadData, String eventContextKey) {
        KafkaEventLog.logReceive(this, payloadData, eventContextKey);
        if (payloadData == null || payloadData.getProjectOrderId() == null
                || StringUtils.isBlank(payloadData.getMaterialCode())
                || payloadData.getTaskType() == null || payloadData.getJudgeResult() == null) {
            log.warn("RecognizeTaskResultHandler invalid payload, skip");
            return;
        }
        notifyTaskAutomationService.receiveTaskResult(Collections.singletonList(payloadData));
    }
}
```

设计要点：

1. **必填字段兜底校验放 Handler**：消息链路没有 Controller 的 `@ValidList`（`NotifyTaskAutomationController.java:38`）校验；且项目已知坑——`@Valid` 加在 List 根对象不触发元素校验（见 `ai-memory/工作日志/2026-08-20.md` 踩坑点1），故不依赖注解校验，Handler 手工判空，非法消息 warn 后跳过（不重试）。
2. **幂等/重投安全**：`receiveTaskResult` 为 insert-or-update（`NotifyTaskAutomationServiceImpl.java:40-55`），消息重复消费无副作用，无需额外去重。
3. **payloadData 直接复用 `NotifyTaskResultParam`**，不新建 BO。

日期反序列化已核实（utopia-event-driven-core-1.3.8 字节码）：`DomainEventDispatcher.dispatch` 经 `Jacksons.readValue`（Jackson `ObjectMapper`）按 Handler 泛型反序列化 payloadData，`NotifyTaskResultParam` 的 `@JsonFormat(yyyy-MM-dd)`（`NotifyTaskResultParam.java:50-57`）生效，无需新建消息 BO；`handleBiz` 抛异常时框架 `RetryTemplate` 按 maxAttempts 重试，落库幂等重投安全。

### 4.3 Apollo 配置（新增）

| Key | 值 | 说明 |
|-|-|-|
| `recognize.result.consume.enable` | `true`/`false` | 消费总开关，参照 `xcs.message.consume.enable`（`XcsCollectionListener.java:22`）；默认 false，灰度打开 |
| `kafka.config.<clusterNN>.consumer.properties.*` | brokers/group-id/listener-names | 复用上游 topic 所在集群的既有配置组；group-id 需为本消费者新申请（TODO 待上游定集群后补充） |
| `kafka.config.<clusterNN>.producer.properties.topics` | topic 名 | TODO 待上游定 |

### 4.4 老接口保留与下线

- 本次 `NotifyTaskAutomationController.java:36-41`、`NotifyTaskAutomationFeign.java:24-25` **不动**；
- 二期下线条件：上游停发 HTTP + 消息链路事件落库/日志确认承接全量流量；下线时同步删除 Controller 方法、Feign 方法及 `@ValidList` 相关引用。

## 5、接口改动

| 接口路径 | 接口文档 | 改动点 | 估时 |
|-|-|-|-|
| 消息（新增）topic=`<TODO>`，eventType=`utopia:<上游服务名>:recognize-task-result` | payloadData = 单个 `NotifyTaskResultParam`（字段见 `NotifyTaskResultParam.java:39-68`） | 新增 `RecognizeTaskResultListener`（4.1）+ `RecognizeTaskResultHandler`（4.2），复用 `NotifyTaskAutomationServiceImpl.receiveTaskResult(:34)` 不改 |  |
| HTTP `POST /api/recognize/task/result`（`NotifyTaskAutomationController.java:36`） | 现有接口文档不变 | 本次无代码改动；上游切流后二期下线（4.4） |  |

## 6、上游依赖

| # | 依赖方 | 依赖内容 | 是否卡上线 |
|-|-|-|-|
| 1 | 上游识别系统 | 按事件驱动标准信封发消息：`{"eventId","eventType":"utopia:<serverName>:recognize-task-result","eventOccurTime","eventContextKey","metadata","payloadData":{单个识别结果}}`；payloadData 字段与现 HTTP body 元素一致（`NotifyTaskResultParam.java:39-68`，日期 `yyyy-MM-dd` 字符串）；一条结果一条消息 | 是（topic 创建 + 上游发消息发版） |
| 2 | 上游识别系统 | 提供：服务名（serverName）、topic 名、所属 Kafka 集群（clusterNN 编号/brokers）、消费 group-id 申请 | 是（4.1/4.3 占位符落地） |
| 3 | jar `utopia-event-driven-starter-kafka` | 既有依赖（根 `pom.xml:479`、`edar-starlord-service/pom.xml:101`），无新增；反序列化已核实为 Jackson（4.2），`@JsonFormat` 日期字段兼容 | 否 |

## 7、表结构改动

无。事件落库表随 `utopia-event-driven-starter-kafka` 框架自带，项目既有 21 个 listener 已在用，无新增表。

## 8、风险评估

无。单条消息处理链路为一次 select + 一次 insert/update（`NotifyTaskAutomationServiceImpl.java:40-55`），与现有 HTTP 逐条循环落库相比无放大；无新增循环调用/批量查询/锁。

## 9、Check List

- [ ] Apollo 配置：**有**——新增 `recognize.result.consume.enable` 开关 + `kafka.config.<clusterNN>.*` 集群/topic/group-id 配置（4.3，TODO 待上游确认后补值）
- [ ] SQL 变更：**无**
- [ ] 上线顺序：上游建 topic 并按信封发消息发版 → starlord 配置 Apollo（开关默认 false）→ starlord 发布（Listener+Handler）→ 打开开关灰度消费 → 观察事件落库/`KafkaEventLog` 日志与 `material_notify_task_automation` 数据 → 上游停发 HTTP → 二期下线老接口
- [ ] 回滚：关闭 `recognize.result.consume.enable` 即停消费；消息仍在 topic 可重放；过渡期老 HTTP 接口保留，上游可回退 HTTP 调用，无需回退发版

## 10、评审会议纪要

### 参会人员

QA:
开发:

### Todo List

> 记录评审改进意见。
