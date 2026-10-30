---
source: code-flow
date: 2026-10-29
class: com.ke.utopia.scm.workbench.controller.ToolController
method: refreshEvalScheduleProjectOrder
related_methods:
  - IndicatorScheduleDAGService.checkBasicDiff
  - IndicatorScheduleDAGService.getBaseEventsBySchedule
  - IndicatorScheduleDAGService.rebuild
  - ConstructionScheduleService.getIndicatorRules
  - EvalScheduleOrderService.calculateScheduleIndicatorByEvents
---

# 排程考核（ScheduleIndicator）刷新与规则构建业务流梳理

本文根据 scm-workbench 当前工作区代码整理，覆盖手动刷新入口、基装包时间变化判断、计算范围选择、考核规则结构及依赖图构建。文中数据示例仅用于说明，不代表线上配置。

## 一、整体流程与方法分工

- `refreshEvalScheduleProjectOrder`：按项目批量触发手动刷新。
- `checkBasicDiff`：比较当前基装包节点时间与 Redis 记录，返回变化节点；强制模式下返回全部传入的有效基装包节点。
- `getBaseEventsBySchedule`：从项目依赖图选择计算起点和额外计算节点。
- `getIndicatorRules`：合并版本考核规则与返补考核规则。
- `rebuild`：将配置转换成候选事件节点和依赖规则，缓存基础结构。
- `getScheduleDAG`：连接节点的入边、出边，构建索引，过滤孤立节点。
- `calculateScheduleIndicatorByEvents`：查找受影响任务、读取时间轨迹、计算并更新考核结果。

主链路：项目准入检查 → 查询有效基装包任务 → 强制收集节点 → 获取依赖图并选择计算范围 → 根据实际时间和规则重算 → 更新考核及同步下游。

## 二、手动刷新接口

入口：`ToolController#refreshEvalScheduleProjectOrder`。

- 请求：`POST /api/refresh/eval/schedule/projectOrder`。
- 请求体：项目 ID 字符串数组，例如 `["项目ID1", "项目ID2"]`。
- 返回：`ResultDTO<Void>`，不包含逐项目处理明细。

### 1. 项目准入

- 依次处理每个项目。
- 查询 `schedule_project` 是否存在项目记录，得到 `scheduleInit`，代码将其作为排程初始化白名单判断。
- 调用主材服务 `materialScheduleSwitch` 查询 `inSchedule`。
- `scheduleInit` 或 `inSchedule` 为 true → 继续；否则跳过当前项目。
- 两次查询都会执行，即使命中白名单，也会查询排程开关。

### 2. 查询基装包任务并强制刷新

- 调用施工排程 `queryScheduleNode(projectOrderId, null, targetTaskTypeList)`。
- 类型范围：指派工人、施工排期、施工开始、施工、施工验收。
- 仅保留 `standardDeleteStatus=NO` 的任务。
- 调用 `checkBasicDiff(projectOrderId, tasks, true)`：所有这些节点都被视为变化，同时更新 Redis 时间缓存。

### 3. 组装计算范围并计算

- 调用 `getBaseEventsBySchedule(projectOrderId, true, true, basicNodeCodeSet)`。
- 含义：全部工期事件作为起点，匹配到的基装包事件作为起点，补充符合条件的签前节点。
- 返回 null → 当前项目不执行计算。
- 否则调用 `calculateScheduleIndicatorByEvents(projectOrderId, startNodes, extNodes, inSchedule)`。
- `inSchedule` 还参与控制是否同步主材任务；仅命中初始化白名单不等于同步开关开启。

### 4. 返回及异常

- 项目串行处理，主计算同步执行；部分下游同步在线程池中执行。
- 任意项目抛异常 → 包装为“排程考核后门计算失败”，立即终止循环，后续项目不再执行。
- 入口没有统一事务，前面已执行的更新不会由该方法整体回滚。
- 遍历结束返回成功；被准入条件过滤、没有规则而跳过的项目不会单独报告。
- 接口成功不代表所有异步同步已完成。

## 三、checkBasicDiff：基装包节点时间变化判断

签名：`Set<String> checkBasicDiff(String projectOrderId, List<DeliveryProcessTaskDTO> tasks, boolean forceAll)`。

### 1. 处理范围及比较时间

- 仅处理 `BASIC_PACKAGE_ACTION_TYPE_SET` 中的五类任务。
- 施工开始节点 `base_construction_work_start`：
  - `factStartTime` 不等于默认时间 → 取实际开始时间。
  - 等于默认时间 → 取 `predictStartTime`。
- 其余基装包节点：
  - `factEndTime` 不等于默认时间 → 取实际结束时间。
  - 等于默认时间 → 取 `predictEndTime`。
- 默认时间为字符串 `1970-01-01 00:00:00`。

### 2. 比较与写缓存

- Redis key：`scm-workbench:schedule-basic-package-diff:{projectOrderId}:{nodeCode}`。
- 满足任一条件 → 将 `nodeCode` 加入返回集合：
  - `forceAll=true`。
  - 缓存 key 不存在。
  - 选出的时间字符串与缓存值不同。
- 无论是否变化，都会写入当前时间并刷新过期时间。
- Apollo 配置 `schedule_interval` 默认 7200 秒，即 2 小时。
- 方法本身不执行考核重算，返回集合由调用方使用。

### 3. 边界行为

- 只比较一个选出的时间，不比较整个基装包对象。
- 实际时间有效时，预计时间变化不会触发 diff。
- 实际时间变为有效值，但与此前缓存的预计时间相同 → 不触发 diff。
- 首次处理或缓存过期，即使业务时间没变也会被视为变化。
- 仅遍历当前输入，无法检测已从列表中消失的节点；两个现有调用方均先过滤已删除任务。
- 代码只识别默认时间字符串，没有将 null 作为默认时间兜底；正常比较分支可能因选中时间为 null 而异常。
- MQ 调用传 false；手动刷新接口传 true。
- 缓存更新发生在后续重算之前，不代表计算已经成功。

## 四、getBaseEventsBySchedule：选择计算范围

签名：`getBaseEventsBySchedule(projectOrderId, constructionDiff, beforeContract, basicNodeCodeSet)`。

### 1. 获取依赖图

- 调用 `getScheduleDAG(projectOrderId)`，根据项目规则版本获取图。
- 无图或无有效事件节点 → 返回 null。
- 否则初始化 `FigureScheduleParam`，包含项目 ID、空 `startNodes` 和空 `extNodes`。

### 2. 工期起点

- `constructionDiff=true` → 图中所有 `CONSTRUCTION_SCHEDULE` 事件加入 `startNodes`。
- false → 不加入。
- 本方法不检测具体哪些工期时间变化；是否触发由调用方决定。

### 3. 补充签前节点

- `beforeContract=true` → 调用 `getNodesBeforeContract`。
- 通过主材服务查询设计、测量、复尺、下单任务。
- 遍历任务节点，保留 `restartCount=0` 且 `estimatedTime` 早于 `2000-01-01 00:00:00` 的节点。
- 通过“任务类型 + 节点类型 + 实际时间类型”匹配基础事件 code，再拼接材料编码，得到事件唯一编码。
- 在图中按唯一编码匹配，将匹配事件加入 `extNodes`。
- “签前”是方法的业务命名；这里没有直接检查合同签署状态，实际筛选条件以上述代码为准。

### 4. 基装包起点

- `basicNodeCodeSet` 为空 → 跳过。
- 否则筛选图中 `BASIC_PACKAGE_SCHEDULE` 事件。
- 以 `scheduleCode + "_" + categoryId` 匹配传入集合，匹配成功才加入 `startNodes`。
- 传入集合中存在但图中不存在的节点不会成为起点。

#### 基装包差异节点为什么加入 startNodes

这段逻辑把施工排程返回的基装包节点编码，映射成 DAG 中的完整事件对象，作为本次下游重算的起点；此处不执行时间计算。

- 输入来源：`basicNodeCodeSet` 来自 `checkBasicDiff`。正常 MQ 场景为检测到差异的节点集合；手动刷新传 `forceAll=true`，因此包含全部传入的有效基装包节点，不要求时间真的变化。
- 匹配要求：事件类型必须是 `BASIC_PACKAGE_SCHEDULE`，且 `scheduleCode_categoryId` 必须在集合中，两项同时满足才选中。
- 这里匹配的是施工排程节点编码格式 `scheduleCode_categoryId`，不是直接匹配 DAG 内部的 `uniqCode`（品类事件通常为 `categoryId_ofcCode`）。

示例：

```text
变化集合包含：base_construction_work_start_包A

DAG 事件：
  scheduleCode = base_construction_work_start
  categoryId   = 包A

拼接编码匹配 → 将该事件对象加入 startNodes
```

假设规则配置存在以下依赖：

```text
包A施工开始 → 材料送达 → 安装
```

- 包A施工开始时间变化后，以该事件为起点，后续沿出边寻找符合载体及规则条件的送达、安装节点并计算。
- 不是要求对基装包起点自身执行一次考核计算，也不是无条件刷新项目所有材料。
- 与签前补算的区别：基装包事件进入 `startNodes`，触发下游重算；签前事件进入 `extNodes`，直接补入自身的待计算范围。

### 5. 两个列表的区别

- `startNodes` 是变化源头，后续沿出边寻找受影响的任务；起点本身不一定参与计算。
- `extNodes` 直接补入待计算集合，不再以它们为起点额外向下遍历。
- 有图但未匹配任何节点时，可返回列表为空的参数对象，不一定返回 null。

### 6. getNodesBeforeContract 的编码转换和使用方式

- 方法返回 `Set<String>`，不计算时间、不更新任务。
- 先读取未删除的基础事件，建立映射：
  - key：`taskInfo + "_" + timeType`，对应“任务类型 + 节点类型 + 时间类型”。
  - value：基础事件 `code`。
  - key 重复时保留第一条。
- 通过主材服务查询项目实际的设计、测量、复尺、下单任务及节点详情。
- 对 `restartCount=0` 且 `estimatedTime` 早于 2000 年的节点，使用 `taskType_nodeType_FACT_TIME` 查找基础事件 code。
- 将 `materialCode + "_" + 基础事件code` 加入返回集合，与 DAG 的 `uniqCode` 对应。
- 固定使用 `FACT_TIME` 是为了定位对应的事件定义，不表示节点已经完成，也不是在此读取实际完成时间。
- 返回的是品类级事件编码，不是具体任务节点 ID；同品类同事件的多条任务节点会在 Set 中合并，实际计算再匹配任务载体。
- 如果基础事件映射缺失，当前代码会拼出 `材料编码_null`，没有在本方法内报错或过滤；调用方随后按 DAG 唯一编码匹配。
- “签前”是代码命名；这里没有判断合同状态。早于 2000 年的时间可从用途理解为需要补算的占位状态，其上游业务含义仍需结合 estimatedTime 的赋值逻辑确认。

### 7. 为什么拿到 DAG 后还需要补充 extNodes

构建 DAG 只是建立“谁依赖谁、按什么规则计算”的关系，并没有代入项目时间计算考核结果。图中存在某个节点，也不代表该节点本次一定进入计算范围。

- `getBaseEventsBySchedule` 只选定本次计算范围的入口，不在这里遍历下游或计算时间。
- 后续从 `startNodes` 沿出边查找符合载体规则的下游节点，得到待计算集合。
- 再把 `extNodes` 中尚未包含的节点补入集合，按 `uniqCode` 避免重复加入。
- 因此 extNodes 是额外补算入口，不是第二次重算整张图，也不是把这些节点作为新的起点再次向下展开。

示意关系如下，具体依赖以实际配置为准：

```text
测量 → 下单 → 送达
              ↑
           工期节点
```

- 本次起点如果只有工期节点，沿出边能找到送达，但不会反向去计算上游测量、下单。
- 即使测量和下单都在 DAG 中，它们也可能没有进入本次计算范围。
- 若这些事件对应的实际任务满足首次节点及 estimatedTime 条件，就通过 extNodes 额外补入。
- 如果某事件已经通过正常遍历选中，后续不会因为同时出现在 extNodes 中就重复加入。

补充签前节点的代码还做了一次“编码 → 图节点”的转换：

```text
项目任务详情
  → 筛选符合条件的首次任务节点
  → 转成事件唯一编码 extCodeSet
  → 在 DAG 中按 uniqCode 匹配完整事件对象
  → 加入 extNodes
  → 后续补入待计算集合
```

- extCodeSet 只有字符串编码，计算需要节点对象上连接的入边与考核规则，因此必须回到 DAG 中匹配。
- 不在当前 DAG 中的编码不会加入 extNodes，也不会临时创建新节点。
- 能进入待计算集合，不代表一定会更新：最终仍需要有适用规则、任务载体和有效的依赖时间。
- 三者分工：DAG 表达完整计算关系；startNodes 决定本次从哪里查找受影响节点；extNodes 补充本次遍历可能覆盖不到但需要计算的节点。

## 五、getIndicatorRules：返回什么数据

返回类型：`List<ProcessNodeCfgDTO>`。

### 1. 数据来源

- `queryAssessmentRuleCfgList(version)`：指定版本的考核规则。
- `queryRebateRuleList()`：额外追加返补规则，该调用不传 version。
- 方法只合并，不去重、不计算。`rebuild` 再过滤材料编码为空的配置。

### 2. 外层：规则适用于谁

`ProcessNodeCfgDTO` 在本链路使用的主要字段：

- `materialCode / materialName`：被考核任务所属材料。
- `supplierCode`：适用供应商；构图时 `9999999` 转为内部不限制值 `-1`。
- `deliveryType`：适用配送类型；内部匹配时 `-1` 表示不限制。
- `taskType`：被考核任务类型。
- `valueRetrievalLogic`：多组候选计算结果取最早或最晚。
- `nodeAssessmentRuleCfgList`：具体规则列表。

### 3. 内层：依据什么时间、怎么计算

`NodeAssessmentRuleCfgDTO` 在本链路使用的主要字段：

- `materialCode / nodeCode`：目标材料及目标节点。
- `relationMaterialCode / relationMaterialName / relationNodeCode`：前置材料或基装包及依赖节点。
- `scheduleRelation`：之前、之后、当天等相对关系。
- `duration / durationType`：偏移时长及日历类型。
- `divide`：组内编号，构图时成为 `indexId`。
- `restartType`：用于区分重启、返补等规则场景。

示意：“材料 B 的送达，依据基装包 A 的施工完成，提前 2 天”。

- 目标是 B 的送达节点。
- 前置是 A 的施工完成节点。
- 相对关系是“之前”，偏移时长为 2，日历类型由配置确定。
- 示例不指定枚举数值；真实枚举及线上配置需以接口返回为准。

## 六、rebuild：将配置转换成图的基础结构

### 1. 基础数据

- 查询 `indicator_base_event`，仅取 `isDelete=0` 的基础事件。
- 获取版本规则并追加返补规则，过滤材料编码为空的配置。

### 2. 收集材料及基装包：categoryMap

- 先收集外层配置自身的 `materialCode → materialName`。
- 再遍历每条内层规则，将非空 `relationMaterialCode → relationMaterialName` 补入。
- 后一步就是讨论过的 301–307 行：补齐仅出现在上游依赖端的材料或基装包。
- 示例 A → B：外层只收集到 B，内层补充 A，后面才能创建 A 的节点并连上依赖。
- 关联材料编码为空时不加入；工期事件单独初始化。
- 相同编码使用 Map 的 put 更新名称。

### 3. 创建候选节点

- 工期事件只创建一份：`uniqCode=基础事件code`，品类字段为空。
- 对每个 categoryMap 编码，创建所有非工期基础事件的节点，包括基装包事件：
  - `uniqCode=categoryId + "_" + 基础事件code`。
  - 保存品类编码、名称及事件属性。
- 这一步是品类和非工期基础事件的组合，尚未过滤未被规则使用的节点。
- 所有节点的入边、出边均初始化为空。
- 建立事件唯一编码到名称、排程编码到内部事件编码的映射。

### 4. 编码转换与规则合并

- 前置端取 `relationNodeCode / relationMaterialCode`，目标端取 `nodeCode / materialCode`。
- `convertNodeCode` 转换：
  - 无品类编码 → 映射为工期事件编码。
  - 普通品类节点 → 品类编码加内部事件编码。
  - `task_generate` → 按目标 taskType 映射，再追加 `_generate`。
  - `task_activate...` → 按目标 taskType 映射，再追加 `_active`。
- `ruleId` 由材料、供应商、配送类型、任务类型、restartType 拼接。
- 再按 `divide` 分组，同一 `ruleId + divide` 合并为一条 `ScheduleEdgeRule`。
- 同组首次创建时设置目标节点、时长、相对关系、供应商等属性，后续仅追加 `codeFromList`。
- 因此同组属性取首次遇到的配置，代码不校验后续配置的目标和时长是否一致。
- `valueRetrievalLogic=0` 或重启规则 → `funcName=最晚`；其余 → 最早。

### 5. 缓存及与 getScheduleDAG 的分工

- 打平规则列表，返回包含 `scheduleEventNodes` 和 `scheduleEdgeRules` 的 `ScheduleDAG`。
- JSON 序列化后按 version 写入本地 `SCHEDULE_DAG_CACHE`，最大 100 项，最后访问后 10 分钟过期。
- `rebuild` 尚未真正连接节点引用。
- `getScheduleDAG` 负责按规则建立入边、出边、节点索引及边索引；前置或目标节点不存在时跳过该连接；最终过滤入边和出边都为空的节点。
- `getScheduleDAG` 没有项目规则版本时返回 null。

### 6. 为什么 getScheduleDAG 在 237–241 行拿到 DAG 后还要继续处理

237–241 行拿到了 `ScheduleDAG` 对象，但其中主要是“节点列表 + 以编码描述关系的规则列表”。后续逻辑负责把这些定义连接成可以遍历的对象图。行号对应本次梳理时的工作区。

#### 第一步：237–241 行，加载定义并建立节点索引

- 从缓存反序列化，或调用 `rebuild(version)`，取得节点和规则。
- 初始化空的 `scheduleEdgeMap`。
- 按 `uniqCode` 建立 `scheduleNodeMap`，方便通过字符串编码查找节点对象。
- 此时规则可以表达 A → B → C，但节点自身的入边和出边仍为空，因为 `rebuild` 初始化的是空列表，缓存也在连接之前写入。

```text
节点列表：[A, B, C]
规则1：codeFromList=[A]，codeTo=B
规则2：codeFromList=[B]，codeTo=C
节点索引：A编码 → A对象，B编码 → B对象，C编码 → C对象

A.edgeOutList = []
B.edgeInList  = []
B.edgeOutList = []
C.edgeInList  = []
```

#### 第二步：245–269 行，将编码关系转换成对象引用

- 遍历每条规则的 `codeFromList`，与 `codeTo` 逐一配对。
- 从节点索引取得前置节点和目标节点；任意一端不存在 → 记录日志并跳过该连接。
- 以 `codeFrom + "," + codeTo` 作为边索引键。
- 同一对节点尚无边 → 创建 `ScheduleNodeEdge`，引用前置和目标节点，并挂到前置节点的 `edgeOutList`、目标节点的 `edgeInList`。
- 将当前规则加入边的 `ruleList`。
- 同一对起点和终点只创建一条边，多条规则挂在同一条边上。

```text
A.edgeOutList → [边AB]
                 ├─ nodeFrom → A
                 ├─ nodeTo   → B
                 └─ ruleList → [对应考核规则]

B.edgeInList  → [同一条边AB]
B.edgeOutList → [边BC]
C.edgeInList  → [同一条边BC]
```

- 后续查找受影响的下游节点，需要遍历 `edgeOutList`。
- 后续查询目标节点依赖的数据与计算规则，需要遍历 `edgeInList`。
- 如果省略连接过程，即使规则列表描述了 A → B，从 A 的空出边列表出发，DFS 也找不到 B。

#### 第三步：271–276 行，过滤孤立节点并重建索引

- `rebuild` 按“品类 × 非工期基础事件”生成候选节点，其中可能有未被规则使用的节点。
- 入边和出边都为空 → 从 `scheduleEventNodes` 移除。
- 根据保留节点重新生成 `scheduleNodeMap`。

因此，前半段加载图的定义，后半段建立可遍历的对象引用并清理无用节点。即使缓存命中也要连接：缓存保存的是连接前的基础结构，而不是挂好入边、出边的运行时对象图。代码中的“深拷贝后引用需要重新赋值”注释，应结合这一实际缓存写入时机理解。

### 7. ScheduleDAG 对象结构及使用方式

`ScheduleDAG` 是节点和规则结构的容器，本身没有计算方法，也不保存项目实际时间轨迹。它包含四个字段：

- `scheduleEventNodes`：所有事件节点。
- `scheduleEdgeRules`：所有依赖规则。
- `scheduleNodeMap`：`uniqCode → ScheduleEventNode`，用于按编码查找节点。
- `scheduleEdgeMap`：`前置节点uniqCode,目标节点uniqCode → ScheduleNodeEdge`，用于查找和复用一对节点之间的边。

#### ScheduleEventNode：事件节点

- 身份信息：
  - `uniqCode`：图内唯一编码，品类事件通常为 categoryId_ofcCode，工期事件直接使用基础事件 code。
  - `name`：事件名称。
  - `ofcCode`：内部基础事件编码。
  - `scheduleCode`：施工排程对应任务类型编码。
  - `categoryId / categoryName`：所属品类或基装包。
- 时间读取信息：
  - `eventType`：事件来源类型，供后续选择时间提取方式。
  - `timeType`：对应生成、完成、激活等哪种时间。
  - `taskInfo`：定位任务使用的任务类型、节点类型条件。
- 连接信息：
  - `edgeInList`：指向自己的边，表示自己依赖谁。
  - `edgeOutList`：从自己出发的边，表示自己影响谁。

#### ScheduleNodeEdge：节点间的边

- `nodeFrom`：前置节点对象。
- `nodeTo`：目标节点对象。
- `ruleList`：这对节点之间的考核规则列表。
- 同一对起点和终点只有一条边，但可以挂不同供应商、配送类型等多条规则。
- 边引用的是节点列表和索引中的节点对象；前置节点的出边列表与目标节点的入边列表引用同一条边。

#### ScheduleEdgeRule：计算条件和算法

- 适用范围：`categoryId / categoryName`、`deliveryId / deliveryType`、`restart / replenish`。
- 依赖来源：`codeFromList` 是一组前置事件编码，`codeTo` 是目标编码；同一规则可能挂在多条来源不同的边上。
- 时间算法：`timeRef` 表示相对关系，`interval` 表示偏移时长，`timeType` 表示自然日或工作日。
- 规则分组：`ruleId` 区分规则方案，`indexId` 对应 divide，`funcName` 决定不同组结果取早或取晚，`indicatorName` 描述考核事件。
- 打平扩展字段：`codeFrom` 是单个前置来源，`cross` 记录是否跨品类。
- 注意两个对象的同名字段：事件节点的 timeType 表示读取哪个时间字段；规则的 timeType 表示时长采用何种日历单位。

#### 业务代码如何使用

- 通过 `getScheduleDAG(projectOrderId)` 获取连接完成的图。
- 筛选起点：遍历 `scheduleEventNodes`，根据事件类型和编码筛选，`getBaseEventsBySchedule` 就在做这一步。
- 查下游：沿起点的 `edgeOutList` 获取 `nodeTo`，结合规则及任务载体条件继续向下查找。
- 算目标：读取目标的 `edgeInList`，获取前置节点及 ruleList，筛选适用规则，另行读取前置时间轨迹后计算。
- 例如 A → B → C：A 变化时可沿出边查 B、C；计算 B 时通过入边找到 A 及 A→B 的规则。

## 七、规则如何参与实际计算

- 根据 startNodes 沿出边寻找符合规则载体条件的下游任务，去重后加入 extNodes。
- 查找计算所需上游节点及目标节点自身的时间轨迹。
- 依据品类、供应商、配送类型、重启或返补等条件筛选适用规则。
- 按 ruleId 分组并排序，限定配送类型、供应商的配置排在不限制配置之前；只选择排序后的第一组规则方案。
- 在所选规则方案中，按 divide/indexId 计算候选时间：
  - 对一个前置节点，过滤有效轨迹，优先使用完成时间；存在多个完成时间时取最晚。
  - 没有完成时间时，使用考核时间，多个考核时间取最晚。
  - 同一个 indexId 内多个前置节点的候选基准时间，再取最早。
  - 按相对关系、偏移时长及日历计算预计开始时间。
  - 结合任务内部耗时计算预计结束时间；耗时大于零且已有实际开始时间时，用实际开始时间计算结束时间。
- 不同 indexId 的候选结果，最终按 funcName 取最早或最晚；重启规则使用最晚。
- 日历处理以 HolidayService 和 TimeProcessor 为准，不能简单理解为对时间戳直接加减 N×24 小时。
- 未命中适用规则、缺少有效时间或未算出结果时，可跳过对应节点更新。

### 结果更新与同步

- 货单任务：更新项目单或货单的考核结果，并更新履约任务时间。
- 用工任务：新增或更新 `indicator_task_dispatch_node` 的考核开始、结束时间等。
- 已计算结果调用考核时间变更通知，同步排程。
- `inSchedule=true` 且更新成功时，异步同步主材任务；送达任务满足条件时发布供应商同步事件。
- 用工任务存在例外：`mode=7` 且更新成功时，即使开关关闭也会同步主材任务。
- 这里的“强制刷新”表示强制选择触发范围，最终仍受图关系、载体、规则和时间数据约束，并非所有项目节点无条件更新。

## 八、代码定位

以下路径相对于 scm-workbench 仓库，行号对应本次梳理时的工作区，后续可能随代码调整。

- `scm-workbench-start/src/main/java/com/ke/utopia/scm/workbench/controller/ToolController.java`：230 行附近，刷新入口。
- `scm-workbench-service/src/main/java/com/ke/utopia/scm/workbench/service/service/ConstructionScheduleService.java`：103 行附近，规则合并。
- `scm-workbench-service/src/main/java/com/ke/utopia/scm/workbench/service/rpc/ConstructionScheduleRpcService.java`：89 行附近，任务查询；102 行附近，规则查询。
- `scm-workbench-service/src/main/java/com/ke/utopia/scm/workbench/service/service/IndicatorScheduleDAGService.java`：230 行附近获取图；281 行附近 rebuild；301–307 行补充关联品类；437 行附近查找计算节点；822 行附近选择计算范围；930 行附近基装包 diff。
- `scm-workbench-service/src/main/java/com/ke/utopia/scm/workbench/service/service/EvalScheduleOrderService.java`：129 行附近计算入口；374 行附近规则方案及时间计算；467 行附近前置时间选择。
- `scm-workbench-service/src/main/java/com/ke/utopia/scm/workbench/service/mq/listener/handler/ConstructionScheduleTaskChangeEventHandler.java`：正常 MQ 触发，与手动刷新对照。
