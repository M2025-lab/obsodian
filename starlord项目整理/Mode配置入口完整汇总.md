[[starlord项目整理/Model=5、6、7 业务模式影响哪些]]
## 一、入口数量统计

项目中Mode配置共有 **7个主要入口**：

1. MaterialConstructionTaskController（主材任务创建）
2. MaterialTaskOpenController（开放任务创建）
3. TaskTemplateRoleServiceImpl.getTaskMode（模板角色服务）
4. TaskTemplateServiceImpl（模板服务）
5. ProjectVssEventHandler（VSS事件处理）
6. AtomProjectChangeEventHandler（原子项目变更事件）
7. MeasureFormFixServiceImpl（修复服务）

---

## 二、各入口详细介绍

### 入口1：MaterialConstructionTaskController（主材任务创建）

**文件位置**：[MaterialConstructionTaskController.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialConstructionTaskController.java:0:0-0:0)（742-771行）

**触发场景**：主材任务创建接口调用

**配置逻辑**：
```java
// 步骤1：默认设置 Mode=5
infoBO.setSupportMode(ModeEnum.HOME2_5);
processCreateV2Context.setSupportMode(ModeEnum.HOME2_5);
createParam.setSupportMode(ModeEnum.HOME2_5);

// 步骤2：判断全国2.5 → Mode=6
if (projectOrderUtil.isNewV2_5(orderDto.getProcessVersion(), 
        orderDto.getBusinessModel(), branchOfficeCode)) {
    processCreateV2Context.setSupportMode(ModeEnum.HOME2_5_MANPOWER);
    createParam.setSupportMode(ModeEnum.HOME2_5_MANPOWER);
}

// 步骤3：判断精装全案 → Mode=6
if (Objects.equals(orderDto.getDecorateType(), 
        CommissionDecorateTypeEnum.RETAIL.getCode().intValue())
        && Objects.equals(orderDto.getFulfillmentMode(), 
                FulfillmentModeEnum.HARDCOVER_HOUSE.getTag())) {
    processCreateV2Context.setSupportMode(ModeEnum.HOME2_5_MANPOWER);
    createParam.setSupportMode(ModeEnum.HOME2_5_MANPOWER);
}

// 步骤4：判断排程材料配置 → Mode=7
if (configQueryService.materialScheduleSwitch(
        MaterialScheduleSwitchParam.builder()
            .projectOrderId(String.valueOf(orderDto.getProjectOrderId()))
            .build(), false)) {
    processCreateV2Context.setSupportMode(ModeEnum.DELIVERY_FLOW);
    createParam.setSupportMode(ModeEnum.DELIVERY_FLOW);
}
```

**判断条件详解**：
- **isNewV2_5()**：processVersion=2.5 + businessModel=1 + 非北京分公司
- **精装全案**：decorateType=零售 + fulfillmentMode=精装全案
- **materialScheduleSwitch()**：配置开关启用 + 规则匹配（订单ID/分公司+销售类型+项目版本+签约时间）

**特点**：
- 最主要的配置入口
- 涵盖完整的判断逻辑
- 支持动态配置开关
- 优先级：Mode=7 > Mode=6 > Mode=5

---

### 入口2：MaterialTaskOpenController（开放任务创建）

**文件位置**：[MaterialTaskOpenController.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-web/src/main/java/com/ke/utopia/web/MaterialTaskOpenController.java:0:0-0:0)（259行）

**触发场景**：开放任务创建接口调用

**配置逻辑**：
```java
infoBO.setSupportMode(ModeEnum.HOME2_5);
```

**特点**：
- 固定使用Mode=5
- 无动态判断逻辑
- 简单直接，适用于开放任务场景

---

### 入口3：TaskTemplateRoleServiceImpl.getTaskMode（模板角色服务）

**文件位置**：[TaskTemplateRoleServiceImpl.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-service/src/main/java/com/ke/utopia/service/impl/TaskTemplateRoleServiceImpl.java:0:0-0:0)（459-484行）

**触发场景**：查询模板角色信息时

**配置逻辑**：
```java
private ModeEnum getTaskMode(String projectOrderId, String projectBusiness, String mdmCode) {
    MaterialScheduleSwitchParam param = MaterialScheduleSwitchParam.builder()
            .projectOrderId(projectOrderId)
            .build();

    // 1. 零售2.0 → Mode=5
    if (Objects.equals(projectBusiness, ProjectBusinessEnum.RETAIL_2_0.code)) {
        return ModeEnum.HOME2_5;
    }
    
    // 2. 整装2.0 → Mode=5
    if (Objects.equals(projectBusiness, ProjectBusinessEnum.WHOLE_2_0.code)) {
        return ModeEnum.HOME2_5;
    }
    
    // 3. 排程材料配置 → Mode=7
    if (configQueryService.materialScheduleSwitch(param, false)) {
        return ModeEnum.DELIVERY_FLOW;
    }
    
    // 4. 北京分公司 → Mode=5
    if (projectOrderUtil.isBeijing(mdmCode)) {
        return ModeEnum.HOME2_5;
    }
    
    // 5. 整装2.5 → Mode=6
    if (Objects.equals(projectBusiness, ProjectBusinessEnum.WHOLE_2_5.code)) {
        return ModeEnum.HOME2_5_MANPOWER;
    }
    
    // 6. 零售2.5 → Mode=6
    if (Objects.equals(projectBusiness, ProjectBusinessEnum.RETAIL_2_5.code)) {
        return ModeEnum.HOME2_5_MANPOWER;
    }
}
```

**判断条件详解**：
- **零售2.0/整装2.0**：直接返回Mode=5
- **排程材料配置**：调用配置开关判断，返回Mode=7
- **北京分公司**：判断分公司编码，返回Mode=5
- **整装2.5/零售2.5**：返回Mode=6

**特点**：
- 基于业务模式的判断逻辑
- 与入口1类似但判断条件不同
- 用于模板角色查询场景

---

### 入口4：TaskTemplateServiceImpl（模板服务）

**文件位置**：[TaskTemplateServiceImpl.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-service/src/main/java/com/ke/utopia/service/impl/TaskTemplateServiceImpl.java:0:0-0:0)（1240-1248行）

**触发场景**：查询任务模板列表时

**配置逻辑**：
```java
// 1. 使用传入的Mode参数
if (Objects.nonNull(listTaskParam.getMode())) {
    unitParam.setMode(listTaskParam.getMode());
}

// 2. 通过processCode判断排程模式
if (StringUtils.isNotEmpty(listTaskParam.getProcessCode())) {
    String templateId = listTaskParam.getProcessCode().split("_")[0];
    if (templateId.length() > 12) {
        unitParam.setMode(ModeEnum.DELIVERY_FLOW.getValue());
    }
}
```

**判断条件详解**：
- **参数传入**：优先使用外部传入的Mode参数
- **模板ID识别**：processCode拆分后的模板ID长度>12，自动识别为Mode=7

**特点**：
- 支持外部参数传入
- 通过模板ID长度自动识别排程模式
- 灵活性高，支持多种查询方式

---

### 入口5：ProjectVssEventHandler（VSS事件处理）

**文件位置**：[ProjectVssEventHandler.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-service/src/main/java/com/ke/utopia/service/listener/handler/scm/ProjectVssEventHandler.java:0:0-0:0)（223行）

**触发场景**：VSS供应链消息触发任务创建

**配置逻辑**：
```java
singleOrderInfo.setSupportMode(modeByBusinessLabel);
```

**前置判断**（220-222行）：
```java
if (业务标签判断) {
    modeByBusinessLabel = ModeEnum.HOME2_5;
}
```

**特点**：
- 根据业务标签选择Mode
- 用于供应链事件驱动场景
- 相对简单的配置逻辑

---

### 入口6：AtomProjectChangeEventHandler（原子项目变更事件）

**文件位置**：[AtomProjectChangeEventHandler.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-service/src/main/java/com/ke/utopia/service/listener/handler/utopia/AtomProjectChangeEventHandler.java:0:0-0:0)（211行）

**触发场景**：原子项目变更触发复尺任务创建

**配置逻辑**：
```java
processCreateV2Context.setSupportMode(
    ModeEnum.getModeEnumByValue(taskDispatchList.get(0).getMode())
);
```

**特点**：
- 继承原任务的Mode
- 用于项目变更场景
- 保持Mode的一致性

---

### 入口7：MeasureFormFixServiceImpl（修复服务）

**文件位置**：[MeasureFormFixServiceImpl.java](cci:7://file:///Users/mirror/IdeaProjects/edar-starlord/edar-starlord-service/src/main/java/com/ke/utopia/service/fix/impl/MeasureFormFixServiceImpl.java:0:0-0:0)（91行）

**触发场景**：数据修复脚本执行

**配置逻辑**：
```java
condition.setMode(5);
```

**特点**：
- 硬编码Mode=5
- 仅用于数据修复
- 不影响正常业务流程

---

## 三、配置入口对比表

| 入口编号 | 入口名称 | 文件位置 | 触发场景 | 配置方式 | 动态性 | 优先级 |
|---------|---------|---------|---------|---------|--------|--------|
| 1 | MaterialConstructionTaskController | MaterialConstructionTaskController.java:742-771 | 主材任务创建 | 订单属性+配置开关 | ✅ 高度动态 | ⭐⭐⭐⭐⭐ |
| 2 | MaterialTaskOpenController | MaterialTaskOpenController.java:259 | 开放任务创建 | 固定Mode=5 | ❌ 固定 | ⭐⭐ |
| 3 | TaskTemplateRoleServiceImpl.getTaskMode | TaskTemplateRoleServiceImpl.java:459-484 | 模板角色查询 | 业务模式+配置开关 | ✅ 高度动态 | ⭐⭐⭐⭐ |
| 4 | TaskTemplateServiceImpl | TaskTemplateServiceImpl.java:1240-1248 | 模板查询 | 参数传入+模板ID识别 | ✅ 支持参数 | ⭐⭐⭐⭐ |
| 5 | ProjectVssEventHandler | ProjectVssEventHandler.java:223 | VSS事件处理 | 业务标签 | ✅ 根据标签 | ⭐⭐⭐ |
| 6 | AtomProjectChangeEventHandler | AtomProjectChangeEventHandler.java:211 | 项目变更事件 | 继承原任务Mode | ✅ 继承模式 | ⭐⭐⭐ |
| 7 | MeasureFormFixServiceImpl | MeasureFormFixServiceImpl.java:91 | 数据修复 | 硬编码Mode=5 | ❌ 硬编码 | ⭐ |

---

## 四、配置入口分类

### 主要配置入口（3个）

**入口1：MaterialConstructionTaskController**
- **地位**：最重要的配置入口
- **影响范围**：主材任务创建
- **配置能力**：完整的判断逻辑 + 动态配置开关
- **推荐使用**：生产环境首选

**入口3：TaskTemplateRoleServiceImpl.getTaskMode**
- **地位**：模板查询的核心入口
- **影响范围**：模板角色查询
- **配置能力**：基于业务模式的判断 + 配置开关
- **推荐使用**：模板相关场景

**入口4：TaskTemplateServiceImpl**
- **地位**：模板查询的灵活入口
- **影响范围**：任务模板列表查询
- **配置能力**：支持参数传入 + 自动识别
- **推荐使用**：需要灵活配置的场景

### 次要配置入口（4个）

**入口2：MaterialTaskOpenController**
- 固定Mode=5，用于开放任务场景
- 配置简单，无需动态判断

**入口5：ProjectVssEventHandler**
- VSS事件处理，根据业务标签配置
- 用于供应链集成场景

**入口6：AtomProjectChangeEventHandler**
- 项目变更事件，继承原任务Mode
- 保持Mode一致性

**入口7：MeasureFormFixServiceImpl**
- 数据修复专用，硬编码Mode=5
- 不影响正常业务流程

---

## 五、配置逻辑统一性分析

### 判断方法对比

| 判断方法 | 入口1 | 入口3 | 入口4 | 入口5 | 入口6 |
|---------|------|------|------|------|------|
| isNewV2_5() | ✅ | ❌ | ❌ | ❌ | ❌ |
| 精装全案判断 | ✅ | ❌ | ❌ | ❌ | ❌ |
| materialScheduleSwitch() | ✅ | ✅ | ❌ | ❌ | ❌ |
| 业务模式判断 | ❌ | ✅ | ❌ | ✅ | ❌ |
| 北京分公司判断 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 参数传入 | ❌ | ❌ | ✅ | ❌ | ❌ |
| 模板ID识别 | ❌ | ❌ | ✅ | ❌ | ❌ |
| 业务标签 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 继承Mode | ❌ | ❌ | ❌ | ❌ | ✅ |

### 配置开关使用情况

| 配置开关 | 入口1 | 入口3 | 入口4 | 入口5 | 入口6 |
|---------|------|------|------|------|------|
| materialScheduleSwitch() | ✅ | ✅ | ❌ | ❌ | ❌ |
| isNewV2_5() | ✅ | ❌ | ❌ | ❌ | ❌ |
| 北京分公司配置 | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 六、总结

### 核心发现

1. **配置入口分散**：7个入口分布在不同的服务和控制器中
2. **判断逻辑不统一**：不同入口使用不同的判断方法
3. **配置开关复用**：materialScheduleSwitch()在多个入口中使用
4. **优先级不明确**：缺乏统一的配置优先级管理

### 推荐配置策略

**生产环境配置**：
- **主材任务创建**：使用入口1，通过配置开关动态控制
- **模板查询**：使用入口3或入口4，根据场景选择
- **事件驱动**：使用入口5和入口6，保持业务逻辑一致性

**配置管理建议**：
1. **统一配置入口**：将Mode配置逻辑集中到入口1和入口3
2. **统一判断方法**：标准化判断逻辑，确保一致性
3. **统一配置开关**：通过materialScheduleSwitch()统一管理Mode=7
4. **建立配置优先级**：明确不同配置入口的优先级关系

**维护建议**：
- 新增Mode配置时优先使用入口1
- 模板相关场景使用入口3或入口4
- 定期审查各入口的配置逻辑一致性
- 建立配置文档，记录各入口的使用场景和配置规则

---
## 相关文档

- [[starlord项目整理/Model=5、6、7 业务模式影响哪些]] — Mode配置对各入口的影响详解
- [[starlord项目整理/主材全链路业务与系统配置总结]] — 三套配置模式的业务含义
- [[starlord项目整理/材料配置问题]] — 节点配置与数据库表映射