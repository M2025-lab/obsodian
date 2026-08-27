# 📚 Kafka 开发级知识手册(概念 + 源码链路)

> **沉淀对象**:Kafka 作为 Java/Spring Boot 后端开发必须掌握的分布式消息系统。
> **一句话结论**:Kafka 不是"会配 `@KafkaListener`、会 `kafkaTemplate.send()`"就算会用,开发级理解 = **概念模型 + Producer/Consumer 调用链源码 + 可靠性三阶段 + 线上排障思维**。记忆主线:`Producer → Topic → Partition → Replica → Consumer → Group → Offset → 幂等`。
> **沉淀日期**:2026-08-27
> **关联笔记**:[[📚 Spring事务失效排查手册]]、[[📚 异常排查 & 调用链分析 完整手册]]、[[技能沉淀]]

---

## 目录

- [0. 学习路线总览](#0-学习路线总览)
- [1. 整体认知:Kafka 是什么](#1-整体认知kafka-是什么)
- [2. Topic 与 Partition](#2-topic-与-partition)
- [3. 为什么需要 Partition](#3-为什么需要-partition)
- [4. Producer 篇](#4-producer-篇)
- [5. Consumer 篇](#5-consumer-篇)
- [6. 可靠性篇](#6-可靠性篇)
- [7. 源码链路篇(开发级核心)](#7-源码链路篇开发级核心)
- [8. 线上排障 / 配置 / 生活比喻 / 口诀](#8-线上排障--配置--生活比喻--口诀)

---

## 0. 学习路线总览

> 目标是**达到开发级别**,而不是背诵"Kafka 是什么"。按后端开发真正需要的路径学:

```
会用 API → 理解 Producer → 理解 Broker → 理解 Consumer → 理解分区
→ 理解 Offset → 理解消费组 → 理解可靠性 → 理解重复消费 → 理解顺序
→ 理解高吞吐 → 能排查线上问题
```

### Kafka 开发者脑图(先记住这张骨架)

```
                         Kafka
                           │
             ┌─────────────┴─────────────┐
             │                           │
          Producer                    Consumer
             │                           │
          Key/Value                    Group
             │                           │
        Partition 选择               Partition 分配
             │                           │
             └───────────┬───────────────┘
                         ↓
                       Topic
                         │
               ┌─────────┼─────────┐
               ↓         ↓         ↓
              P0        P1        P2
               │         │         │
          Offset 0    Offset 0   Offset 0
          Offset 1    Offset 1   Offset 1
               │
               ↓
          Leader / Replica
               │
               ↓
             ISR
               │
               ↓
           持久化 Log
               │
               ↓
          Retention
```

### 业务可靠性主线(套在脑图上)

```
Producer
   ├── Key / Partition / Batch / acks / Retry / Idempotence
        ↓
     Kafka(Partition / Replica / Leader / ISR / Retention)
        ↓
    Consumer(Group / Poll / Offset / Commit / Rebalance / Lag)
        ↓
     业务处理(DB / Redis / RPC / MQ)
        ↓
       幂等
        ↓
      最终一致性
```

> **这才是 Kafka 开发真正需要掌握的完整链路。**

---

## 1. 整体认知:Kafka 是什么

Kafka 本质上是一个:

> **分布式、高吞吐、可持久化的消息系统 / 事件流平台。**

先把它理解成这样一个结构:

```text
生产者 Producer
      │
      │ 发送消息
      ↓
┌───────────────────────┐
│       Kafka 集群       │
│       Topic            │
│  ├── Partition 0       │
│  ├── Partition 1       │
│  └── Partition 2       │
└───────────────────────┘
      │
      │ 拉取消息
      ↓
消费者 Consumer
```

### 核心概念速查表

| 概念             | 作用                  |
| -------------- | ------------------- |
| Producer       | 生产消息                |
| Consumer       | 消费消息                |
| Broker         | Kafka 服务节点          |
| Topic          | 消息分类                |
| Partition      | Topic 的物理分片         |
| Offset         | 消息在 Partition 中的位置  |
| Consumer Group | 消费者协作消费             |
| Replica        | Partition 的副本       |
| Leader         | Partition 当前负责读写的副本 |
| Follower       | Leader 的副本          |

> **开发 Kafka,最重要的就是搞懂这几个概念之间的关系。**

---

## 2. Topic 与 Partition

假设创建:

```text
topic = order-created
partition = 3
```

实际上:

```text
order-created
  ├── Partition 0
  ├── Partition 1
  └── Partition 2
```

每个 Partition 都是一个**有序的追加日志(Append Only Log)**:

```text
Partition 0

offset   0       1       2       3       4
         ↓       ↓       ↓       ↓       ↓
      [消息A] [消息B] [消息C] [消息D] [消息E]
```

> **非常重要**:Kafka 的消息不是简单存在一个 Topic 里面,而是存在 Topic 的 **Partition** 里面。层级关系:

```text
Topic → Partition → Message → Offset
```

---

## 3. 为什么需要 Partition

因为单机处理能力有限。假设单个 Kafka Broker 吞吐 `100 MB/s`,如果 Topic 只有一个 Partition,吞吐量受单个 Partition/Broker 限制。拆成多个 Partition 分布到不同 Broker 就可以并行处理。

### Partition 的核心作用

1. **提高吞吐** —— 多 Partition 多 Broker 并行写入
2. **实现并行消费** —— 多 Consumer 各消费不同 Partition
3. **实现水平扩展** —— 加 Broker 即可承载更多 Partition
4. **提供局部有序性** —— 见下文

### 关键:Kafka 只保证单个 Partition 内有序

```text
Partition 0:A → B → C → D        ✅ 顺序一定保持
```

但如果消息分散到不同 Partition:

```text
Partition 0:A → C
Partition 1:B → D
```

Kafka **不保证** `A → B → C → D` 的全局顺序。

> 所以业务里要求"全局有序"只能用 1 个 Partition,但并发能力有限。实际业务通常只要求**同一个业务实体有序**(如同一个 orderId),用 `key = orderId` 让它落进同一个 Partition。详见 [§4 Key](#4-producer-篇) 和 [§6 顺序与吞吐矛盾](#6-可靠性篇)。

---

## 4. Producer 篇

### 4.1 发送消息的基本写法

```java
ProducerRecord<String, String> record =
        new ProducerRecord<>("order-created", "10001", "order created");
producer.send(record);
```

一条消息包含:`topic` / `key` / `value`(完整构造还带 `partition` / `timestamp` / `headers`)。

### 4.2 最关键的东西:Key

很多开发者 Kafka 用得不深入,就是因为没真正理解 Key。Kafka 根据 Key 计算 Partition,简化理解:

```text
partition = hash(key) % partitionCount

orderId 10001  →  hash  →  Partition 1
```

如果同一个订单后续消息(创建 / 支付 / 发货 / 完成)都用 `key = orderId`,就会全部进入同一个 Partition,从而保证**同一个订单的消息顺序**:

```text
Partition 1: 订单创建 → 订单支付 → 订单发货 → 订单完成
```

### 4.3 Producer 发送不是简单的 HTTP 请求

Producer 会维护 `Topic → Partition → Leader Broker` 的元数据:

```text
order-created
  P0 → Broker1
  P1 → Broker2
  P2 → Broker3
```

Producer 知道 `P1 的 Leader 是 Broker2`,消息就直接发给 `Broker2`,而不是发给任意 Broker 再转发。

> `bootstrap.servers` 只是 Producer 启动时找到集群、获取元数据的**入口**,之后 Producer 会和实际 Leader Broker 通信。

### 4.4 Producer 高吞吐的设计

| 设计         | 作用                                   |
| ---------- | ------------------------------------ |
| 顺序写磁盘       | Append Only Log,顺序写非常适合磁盘            |
| Batch 批量发送 | 凑成一批一次发送,减少网络 IO                     |
| Page Cache | 大量利用操作系统页缓存                           |
| Zero Copy  | 发送数据时减少用户态/内核态之间的数据拷贝                 |
| Compression | 支持 gzip / snappy / lz4 / zstd        |
| Partition 并行 | 多 Partition 并行处理                     |

#### Batch:`batch.size` 与 `linger.ms`

```properties
batch.size=16384   # 一个 Partition 的 Batch 尽量凑到 16KB
linger.ms=5        # 即使没满,也最多等 5ms 让更多消息进来
```

- `batch.size` 控制 **尽量攒多大**
- `linger.ms` 控制 **最多等多久**

#### `send()` 返回的是 Future,不是"发送结果"

```java
CompletableFuture<SendResult<K, V>> future = kafkaTemplate.send(...);
```

`send()` 调用时消息可能还在 `RecordAccumulator` 缓冲区,没有 Broker ACK。未来某个时刻 Broker ACK 后 Future 才 complete:

```java
kafkaTemplate.send(...)
    .whenComplete((result, ex) -> {
        if (ex != null) { /* 失败 */ }
        else { /* 成功 */ }
    });
```

### 4.5 可靠性核心参数:`acks`

| acks  | 行为                     | 优点 | 缺点          |
| ----- | ---------------------- | -- | ----------- |
| `0`   | 发送后不等 Broker 确认,直接返回   | 快  | 消息可能丢       |
| `1`   | Leader 写成功就返回          | 折中 | Leader 挂了 + Follower 没同步 → 丢消息 |
| `all` | 等所有 ISR 副本同步后才返回(满足 ISR 条件) | 可靠性最高 | 慢 |

> 生产环境通常搭配 `acks=all`。

### 4.6 ISR(In-Sync Replicas)

假设 `Partition 0` 有 Leader + 2 个 Follower:

```text
正常同步:  ISR = Broker1, Broker2, Broker3
Broker3 长时间跟不上:  ISR = Broker1, Broker2
```

`acks=all` 实际等待的是 **ISR 中的副本**同步完成,和 `min.insync.replicas` 强相关。

> Broker/Topic 关键配置:`replication.factor`(副本总数)、`min.insync.replicas`(最小同步副本数,如设为 2,则至少 2 个副本写成功才 ACK)。

### 4.7 消息存储:Log Segment

Kafka 不把消息存成 MySQL 一行一行,而是 Log Segment:

```text
topic/order-created-0/
  00000000000000000000.log
  00000000000000123456.log
  00000000000000234567.log
```

一个 Partition 对应一个 Log,Log 又切成多个 Segment,方便查找、删除旧数据、控制磁盘空间。

> **Kafka 消息不是消费完就删除。** 是否删除由 `retention.ms` / `retention.bytes` 决定,而不是由 Consumer 是否消费决定。这让 Kafka 更像"一个可以回放的日志系统"。

### 4.8 消息回放能力

假设今天凌晨 Consumer 出 Bug 导致 10 万条消息处理错误,但 Kafka 消息还在,就可以:

```text
修改 Consumer Group Offset  →  从 offset=10000 重新消费
```

这是 Kafka 很强的地方。

---

## 5. Consumer 篇

### 5.1 Consumer 是主动 Pull,不是 Broker Push

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    for (ConsumerRecord<String, String> record : records) {
        System.out.println(record.value());
    }
}
```

> **为什么 Pull?** Consumer 自己控制消费速度。Kafka 生产 `10000 msg/s`、Consumer 只能处理 `1000 msg/s` 时,如果 Kafka 主动 Push 很容易把 Consumer 打爆;Pull 模式让 Consumer 按自己的节奏拉取。

### 5.2 Consumer Group 是 Kafka 的核心

假设 Topic 有 P0/P1/P2,Consumer Group `group-A` 有 3 个 Consumer:

```text
P0 → Consumer1
P1 → Consumer2
P2 → Consumer3
```

> **同一个 Consumer Group 内,一个 Partition 同一时刻只能分配给一个 Consumer。**

#### 为什么需要 Consumer Group

实现**消费者水平扩展**:一个 Consumer 处理不过来,加 Consumer 并行消费。

#### Consumer 数量超过 Partition 会怎样

```text
Partition = 3, Consumer = 5
→ P0 → C1, P1 → C2, P2 → C3
→ C4、C5 空闲
```

> **同一个 Consumer Group 的有效并发度最多等于 Partition 数量。** 这就是为什么 Topic 的 Partition 数量设计非常重要。

#### 两个 Consumer Group 呢

```text
             Topic
          /         \
     Group A       Group B
     / | \         / | \
   C1 C2 C3      C4 C5 C6
```

**两个 Group 都会完整消费 Topic。** 这就是 Kafka 实现"一条消息同时被多个业务系统消费"的方式:

```text
order-created
   ├── order-service     (Group A)
   ├── inventory-service (Group B)
   ├── marketing-service (Group C)
   └── data-service      (Group D)
```

> 每个业务一个 Consumer Group。Producer 只关心 `Topic / Key / Partition`,Consumer 只关心 `Group / Partition / Offset`,两边通过 Partition Log 解耦。

### 5.3 Offset:消息在 Partition 中的逻辑位置

```text
offset: 0 1 2 3 4 5 6 7
```

Consumer 消费到 `100/101/102`,需要记录"我消费到哪里了",这就是 Offset。Kafka 把 Consumer Group 的 Offset 存在内部 Topic:

```text
__consumer_offsets
```

### 5.4 自动提交 vs 手动提交

#### 自动提交

```properties
enable.auto.commit=true
```

开发简单,但存在问题:poll 拿到消息 → Offset 自动提交 → 业务代码执行失败 → Kafka 认为已消费成功 → **消息丢失**。

#### 手动提交(更可靠)

```properties
enable.auto.commit=false
```

```java
consumer.poll();
处理业务();
consumer.commitSync();
```

流程:`poll → 业务处理 → 成功 → commit offset`。

### 5.5 `auto.offset.reset`

新的 Consumer Group 从来没消费过,Kafka 问"从哪里开始?":

| 配置        | 行为            |
| --------- | ------------- |
| `earliest` | 从最早的可用消息开始    |
| `latest`   | 从最新位置开始(默认)   |

> **只有当这个 Group 没有有效 Offset 时,这个配置才决定从哪开始。** 如果已有 `offset=10000`,改成 `earliest` 也不会从 0 重新开始。

### 5.6 Rebalance

假设 `group-A` 中 C2 挂了,Kafka 需要重新分配它负责的 Partition,这个过程就是 **Rebalance**。

#### 为什么必须关注

Rebalance 期间可能**暂停消费**。如果 Consumer 处理很慢(`poll()` 后处理 10 分钟),Kafka 会判断 Consumer 挂了从而触发 Rebalance。相关参数:

```properties
max.poll.interval.ms    # 两次 poll 最大允许间隔(最关键)
max.poll.records        # 单次 poll 最大记录数
session.timeout.ms     # 心跳超时
heartbeat.interval.ms  # 心跳间隔
```

> 尤其 `max.poll.interval.ms`,它决定 Consumer 两次 poll 之间最大允许间隔,超过就会被踢出触发 Rebalance。

### 5.7 Consumer Lag(积压)排查

```text
Kafka offset = 100000
Consumer offset = 80000
→ Lag = 20000  (有 20000 条消息还没被处理)
```

#### Lag 增长的常见原因

1. **Consumer 处理速度太慢** —— 生产 10000/s、消费 5000/s
2. **Consumer 数量不足** —— Partition=10、Consumer=2,并发不足
3. **下游慢** —— Consumer → MySQL 慢,拖累整个消费速度
4. **Rebalance 频繁** —— 不断 消费 → Rebalance → 消费
5. **某个 Partition 成为热点(Partition Hotspot)** —— Key 都一样(如固定 `key=10001`)→ 所有消息进 P0 → P0 很忙、其他很闲

> 这是非常典型的 Partition 热点问题,根因常在 **Key 设计错误**。

---

## 6. 可靠性篇

### 6.1 消息可靠性拆成三阶段

```text
Producer → Kafka → Consumer
```

每一阶段都有可能丢:

| 阶段             | 关注配置                                   |
| -------------- | -------------------------------------- |
| Producer → Kafka | `acks` / `retries` / `enable.idempotence` |
| Kafka 内部        | `replication.factor` / `min.insync.replicas` / ISR |
| Kafka → Consumer | `offset` / `enable.auto.commit` / `commit` / 幂等 |

> 正确说法:**Kafka 提供了完善的持久化和复制机制,但最终是否丢消息取决于 Producer、Broker、Consumer 三阶段的配置和业务处理方式。**

### 6.2 副本机制为什么不怕消息丢

`replication.factor=3`:

```text
Partition 0
  Broker1  Leader
  Broker2  Follower
  Broker3  Follower
```

Broker1 挂掉 → Broker2 成为 Leader → 业务继续运行。这就是副本机制提供的高可用。

### 6.3 为什么 Kafka 消费经常重复(必须掌握幂等)

```text
offset=100
poll → 处理业务 → DB 成功 → Consumer 突然挂掉 → Offset 还没提交
重启 → 继续从 offset=100 → 再次处理 100
```

所以 Kafka 很典型的是 **At Least Once(至少一次)**:可能重复,但尽量不丢。**Kafka 消费端必须考虑幂等。**

#### 常见幂等方案

**方案一:数据库唯一索引**

```text
message_id UNIQUE
第一次 INSERT 成功
第二次 INSERT 唯一键冲突 → 判定已处理过
```

**方案二:Redis Set**

```text
SETNX kafka:message:123
成功 → 第一次处理
失败 → 已处理过
```

**方案三:业务状态机**

```text
订单: 待支付 → 已支付 → 已发货
重复收到"支付成功" → 若当前已"已支付",直接忽略
```

### 6.4 三种语义

| 语义             | 实现方式                | 代价        |
| -------------- | ------------------- | --------- |
| **At Most Once**  | 先提交 Offset 再处理消息     | 可能丢消息     |
| **At Least Once** | 先处理成功再提交 Offset(默认) | 可能重复,需幂等  |
| **Exactly Once**  | Kafka 事务 + 幂等 Producer | 复杂,且仅限 Kafka 链路内 |

> **注意**:Kafka 的 Exactly Once **不是说整个业务系统天然 exactly once**。例如 `Consumer → HTTP 调第三方`,第三方接口执行两次,Kafka 事务不能自动帮你解决。真正业务中的"一次且仅一次"通常仍需 **幂等 + 事务 + 状态控制**。

### 6.5 Kafka 事务解决什么

```properties
enable.idempotence=true
transactional.id=xxx
```

事务用于"消费 Kafka → 处理 → 生产另一条 Kafka 消息 → 一起提交"的原子过程:

```text
Topic A → Consumer → 处理 → Topic B
希望: A 消费成功 + B 生产成功 作为一个原子过程
```

### 6.6 Kafka 与数据库的一致性(本地消息表 / Outbox Pattern)

实际开发最常见的问题:数据库成功但 Kafka 发送失败怎么办?

```java
updateOrder();        // DB 成功
kafkaTemplate.send(); // Kafka 失败 → 不一致
```

解法——**本地消息表(Outbox Pattern)**:

```text
事务
 ├── 更新订单
 └── 写消息表(本地 DB,与业务同事务)

事务提交
   ↓
消息投递程序(定时扫描消息表)
   ↓
Kafka
   ↓
投递成功后标记消息表
```

这样数据库和消息可以更可靠地协调,保证最终一致性。

### 6.7 顺序与吞吐量的矛盾

| 需求        | Partition 策略        | 结果             |
| --------- | ------------------- | -------------- |
| 全局有序      | 只能 `1 Partition`     | 并发能力有限          |
| 高吞吐       | `100 Partition`     | 只保证 Partition 内有序 |
| 同一业务实体有序  | `key = orderId`      | 同 orderId 进同一 Partition,兼顾顺序与吞吐 ✅ |

> 实际业务通常不要求全局有序,而是要求**同一个业务实体有序**(同一个 orderId)。

---

## 7. 源码链路篇(开发级核心)

限定典型场景:

```java
// 生产
kafkaTemplate.send("order-topic", "10001", "hello");

// 消费
@KafkaListener(topics = "order-topic", groupId = "order-group")
public void consume(String message) { /* 业务代码 */ }
```

### 7.1 Producer 调用链

```text
业务代码
   ↓
KafkaTemplate.send()
   ↓
KafkaProducer.send()
   ↓
doSend()
   ↓
   ├── 检查参数
   ├── 获取 Topic 元数据
   ├── 序列化 Key
   ├── 序列化 Value
   ├── 计算 Partition(Partitioner)
   ├── 写入 RecordAccumulator
   └── 唤醒 Sender 线程
```

#### 关键认知

- **`KafkaTemplate` 本身并没有真正把消息发送到 Kafka**,它只是 Spring 对 Kafka Producer 的封装,最终核心还是 `KafkaProducer.send()`。
- **`send()` 返回的 Future 不是"发送结果"**,此时消息可能还在 `RecordAccumulator` 缓冲区,没有 Broker ACK。

#### 序列化

Kafka 网络上传输的不是 Java 对象,必须序列化:

```text
Java Object → Serializer → byte[]
"10001" → byte[]
Order → JSON → byte[]
```

配置 `key.serializer` / `value.serializer`(`StringSerializer` / `JsonSerializer` / `ByteArraySerializer`)。

#### Partition 选择

```text
hash(key) % partitionCount
10001 → hash → Partition 1
```

#### `RecordAccumulator`(消息缓冲区)

```text
send(message1/2/3/4)
   ↓
RecordAccumulator(按 Topic + Partition 组织 Batch)
   ↓
Batch: [msg1, msg2, msg3, msg4]
   ↓
一次网络发送
```

> **消息最终按 Topic + Partition 组织 Batch**。Partition 不仅决定顺序,还直接影响 Producer 的批处理和并行发送。`batch.size` 控制攒多大,`linger.ms` 控制最多等多久。

#### Sender 线程(真正的发送者)

```text
业务线程:send() → 把消息放进 RecordAccumulator → 立即返回 Future
Sender 线程:检查 RecordAccumulator → 发现 Batch ready → 通过 NetworkClient 发给 Broker
```

> **为什么要这么设计?** 如果业务线程(Tomcat 线程)每次 send 都做网络 IO 等待 Broker,会被大量网络 IO 阻塞。现在业务线程只负责"快速放入内存返回 Future",Sender 线程负责"批量网络 IO",两者解耦。

#### 进入 Broker

```text
Sender → NetworkClient → ProduceRequest → TCP → Broker Leader
```

Broker 在 Partition 中**追加**消息时分配 Offset(Offset 由 Broker 决定,不是 Producer):

```text
已有 0/1/2/3,新消息来了 → offset=4
Broker 返回: topic / partition / offset
```

#### `acks=all` 时

```text
Producer → Leader(Broker1) → 写入 → 复制到 Follower(Broker2/Broker3)
满足 min.insync.replicas → ProduceResponse → Sender → Future complete
```

### 7.2 Producer 完整链路汇总

```text
业务代码
   ↓
KafkaTemplate.send()
   ↓
ProducerRecord
   ↓
KafkaProducer.send()
   ↓
Serializer
   ↓
Partitioner
   ↓
Metadata
   ↓
RecordAccumulator
   ↓
Batch
   ↓
Sender 线程
   ↓
NetworkClient
   ↓
ProduceRequest
   ↓
Broker Leader
   ↓
Partition Log
   ↓
Replica
   ↓
ProduceResponse
   ↓
Future
```

### 7.3 Consumer 调用链

```text
Spring Boot 启动
   ↓
扫描 @KafkaListener
   ↓
KafkaListenerAnnotationBeanPostProcessor
   ↓
解析 @KafkaListener → 创建 ListenerContainer
   ↓
启动 Container → new KafkaConsumer<>(properties)
   ↓
加入 Consumer Group(Group Coordinator)
   ↓
Partition Assignment
   ↓
poll()
   ↓
Fetcher → FetchRequest → Broker
   ↓
Partition Log(从指定 Offset 开始返回一批消息)
   ↓
ConsumerRecords
   ↓
ListenerContainer
   ↓
MessageConverter
   ↓
MessagingMessageListenerAdapter
   ↓
@KafkaListener 方法
   ↓
业务代码
```

#### 关键认知

- **`@KafkaListener` 本身不是一个 Consumer**。Spring 会创建 `KafkaMessageListenerContainer`(或 `ConcurrentMessageListenerContainer`),它内部 `new KafkaConsumer<>()`,负责"创建 Consumer → 订阅 → poll → 转换 → 调用你的 Listener → 处理 Offset/异常"整个生命周期。
- **最终调用你方法的是 Spring Kafka 的 `MessagingMessageListenerAdapter`**,不是 Kafka 直接调用。

#### poll() 内部

```text
KafkaConsumer.poll()
   ↓
Coordinator(确认 Consumer Group)
   ↓
Fetcher(发送 FetchRequest)
   ↓
Broker(从指定 Offset 返回消息)
   ↓
ConsumerRecords
```

例如 Consumer 当前 `P1 offset=100`,FetchRequest 告诉 Broker"从 P1 的 offset 100 开始给我数据",Broker 返回 `100/101/102/103...`。

#### 消息参数转换

Kafka 实际收到的是 `byte[]`,经过:

```text
Kafka bytes → Deserializer → String/byte[] → MessageConverter → JSON → Order → consume(Order)
```

> 你写 `public void consume(Order order)` 不要以为 Kafka 直接传了 Order,实际经历了 `Deserializer → Converter → Order`。

#### Spring Kafka 的 AckMode

| AckMode            | 行为                          |
| ------------------ | --------------------------- |
| `RECORD`           | 一条处理成功就提交对应 Offset          |
| `BATCH`            | 一次 poll 全部成功后提交(默认)         |
| `MANUAL`           | 业务代码 `ack.acknowledge()` 决定 |
| `MANUAL_IMMEDIATE` | 手动且立即提交                     |

### 7.4 为什么要理解 Offset 在哪处理

```text
Partition 0: offset 100/101/102/103
Consumer poll 拿到 100/101/102
业务 consume() 执行成功 → 提交 Offset
```

如果 `consume()` 抛异常 → Spring Kafka 的 `ErrorHandler` 参与处理。所以线上排查一定要关注:

```text
ListenerContainer / ErrorHandler / AckMode / Offset
```

### 7.5 Producer + Consumer 全链路合并图

```text
                 【Producer 线程】
                       │
                       ↓
              KafkaTemplate
                       │
                       ↓
                KafkaProducer
                       │
                       ↓
              RecordAccumulator
                       │
                       ↓
                   Sender
                       │
                       ↓
                    Broker
                       │
                       ↓
               Partition Log
                       │
                       ↓
                    Fetcher
                       │
                       ↓
               KafkaConsumer
                       │
                       ↓
              ListenerContainer
                       │
                       ↓
        MessagingMessageListenerAdapter
                       │
                       ↓
                @KafkaListener
                       │
                       ↓
                  业务代码
```

> **把这条链真正搞懂,Kafka 就不再是"会配 `@KafkaListener`、会 `kafkaTemplate.send()`",而是已经进入开发级理解。**

---

## 8. 线上排障 / 配置 / 生活比喻 / 口诀

### 8.1 线上排障链路("消息没消费"别只看消费者代码)

有人告诉你"Kafka 消息没消费",不要马上看消费者代码,沿这条链路逐层查:

```text
① Producer 有没有 send?
② KafkaTemplate 有没有异常?
③ Future 最终成功了吗?
④ Topic 对不对?
⑤ Partition 对不对?
⑥ Broker 有没有收到?
⑦ 消息是否真的进入 Log?
⑧ Consumer Group 对不对?
⑨ Partition 有没有分配给这个 Consumer?
⑩ Consumer 有没有 poll?
⑪ Offset 在哪里?
⑫ Listener 有没有执行?
⑬ 业务有没有抛异常?
⑭ ErrorHandler 怎么处理?
⑮ Offset 最终有没有提交?
```

### 8.2 看到 Consumer Lag 持续上涨的排查路径

```text
生产速度 > 消费速度
   ↓
检查 Consumer 处理耗时
   ↓
检查 Partition 数
   ↓
检查 Consumer 数
   ↓
检查热点 Partition(Key 是否都一样)
   ↓
检查下游 DB / RPC
   ↓
检查 Rebalance
```

### 8.3 Kafka 开发中最常见的坑

| 坑                | 后果                  |
| ---------------- | ------------------- |
| Key 设计错误         | Partition 热点        |
| Partition 太少     | Consumer 并发上不去     |
| Consumer 处理太慢   | Lag 持续增长           |
| 自动提交 Offset      | 业务失败但 Offset 已提交 → 消息丢失 |
| 没有幂等             | 重复消费 → 重复扣款/发券/更新  |
| 误以为 Kafka 全局有序  | 实际只保证 Partition 内有序 |
| Consumer 数量超过 Partition | 多出来的 Consumer 闲置 |
| Consumer 长时间不 poll | 触发 Rebalance       |

### 8.4 Java 开发至少要掌握的配置清单

**Producer**

```properties
bootstrap.servers
key.serializer
value.serializer
acks
retries
enable.idempotence
batch.size
linger.ms
buffer.memory
compression.type
```

**Consumer**

```properties
bootstrap.servers
group.id
key.deserializer
value.deserializer
enable.auto.commit
auto.offset.reset
max.poll.records
max.poll.interval.ms
session.timeout.ms
heartbeat.interval.ms
```

**Broker / Topic**

```text
partitions
replication.factor
min.insync.replicas
retention.ms
retention.bytes
```

### 8.5 Spring Kafka 在项目里的典型用法

生产:

```java
kafkaTemplate.send("order-created", orderId.toString(), message);
```

消费:

```java
@KafkaListener(topics = "order-created", groupId = "order-service")
public void consume(String message) {
    // 业务处理
}
```

### 8.6 源码阅读顺序(想深入源码时)

不要一上来乱看几十个类,沿**一条真实消息**追:

**Producer 方向**

```text
KafkaTemplate.send()
   → KafkaProducer.send() → doSend()
   → RecordAccumulator.append()
   → Sender.run() → sendProducerData()
   → NetworkClient → Broker
```

**Consumer 方向**

```text
KafkaConsumer.poll()
   → Fetcher → ConsumerRecords
   → KafkaMessageListenerContainer → invokeRecordListener()
   → MessagingMessageListenerAdapter → invoke()
   → 你的 @KafkaListener
```

**重点 8 个类(按顺序)**

```text
1. KafkaTemplate          2. KafkaProducer
3. RecordAccumulator      4. Sender
5. NetworkClient          6. KafkaConsumer
7. KafkaMessageListenerContainer
8. MessagingMessageListenerAdapter
```

进一步可深入:`ConsumerCoordinator` / `Fetcher` / `AbstractCoordinator` / `ConsumerNetworkClient` / `GroupCoordinator` / `ReplicaManager` / `Log` / `LogSegment`。

### 8.7 达到"开发级别"的自检清单

- **基础**:Topic / Partition / Offset / Broker / Consumer Group 是什么?
- **Producer**:怎么选 Partition?Key 作用?acks=0/1/all 区别?retries 干什么?为什么高吞吐?batch.size/linger.ms 干什么?
- **Consumer**:为什么 Pull?Group 怎么分配 Partition?Consumer 数量超过 Partition 会怎样?Offset 存在哪?auto commit 有什么问题?Rebalance 为什么发生?max.poll.interval.ms 干什么?
- **可靠性**:为什么可能重复消费?怎么保证幂等?At Least Once 是什么?Exactly Once 是什么?Kafka 事务解决什么?与 MySQL 如何保证最终一致性?
- **性能**:Kafka 为什么快?Page Cache / Zero Copy / Batch / Compression / Partition 并行?
- **线上排障**:看到 Consumer Lag 上涨,能否按上面的排查路径走一遍?

### 8.8 生活比喻

把 Kafka 想象成**一栋大型快递分拣中心**:

- **Topic** = 快递分类(如"订单类"、"支付类"),不同类别走不同流水线
- **Partition** = 同一条流水线拆成多条并行传送带,提高吞吐
- **Offset** = 每个包裹在传送带上的编号,顺序递增
- **Producer** = 寄快递的人,把包裹放到传送带
- **Consumer Group** = 一组快递员,每个传送带同一时刻只派一个快递员负责(不能两个人抢一条带)
- **Consumer 超过 Partition 数** = 多出来的快递员没活干,闲着
- **Key** = 收件人身份证号,同一个收件人的所有包裹都进同一条传送带,保证顺序
- **Replica** = 每条传送带旁边有备用带,主带坏了立刻切换,包裹不丢
- **ISR** = 跟得上主带的备用带集合,只有它们都确认收到才算"妥投"
- **Retention** = 传送带上的包裹不会因为快递员取走就消失,而是按时间/容量定期清理
- **消息回放** = 快递员发现昨天送错了,可以回到传送带某个编号重新取件重送
- **Rebalance** = 某个快递员突然请假,中心重新分配他负责的传送带给其他人
- **Lag** = 传送带上堆积的未取包裹越来越多,说明快递员处理不过来

### 8.9 一句话记忆口诀

> **消息存 Partition,顺序只在 Partition 内;Key 定 Partition,同实体才有序;Producer 攒批异步入缓冲,Sender 网络发;acks=all 配 ISR,副本兜底不轻丢;Consumer 主动 Pull 按 Group 分 Partition,Offset 手动提交防丢;处理必幂等防重复,本地消息表保最终一致。**

**四个要点**:

- **现象**:消息丢了 / 重复了 / Lag 涨了 / 顺序乱了
- **根因**:三阶段配置(Producer acks / Broker ISR / Consumer Offset+幂等)没对齐,或 Key/Partition 设计错误
- **陷阱**:自动提交丢消息、没幂等致重复、误以为全局有序、Consumer 数 > Partition 数
- **定位**:沿 Producer → Broker → Consumer 链路逐层查,看到 Lag 先查生产/消费速度差 + 热点 Partition

---
---

---
