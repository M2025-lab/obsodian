---

---

---


**我们的目标不是做一个聊天机器人，而是把"做 AI 功能"这件事本身，从"老师傅的手艺"变成"标准化产线"——让 AI 真正成为业务的水电煤。**

这份分享讲一件事：**AI 落地真正的难，不难在"做出来"，难在"做第二个、第一百个"。** 下面我们跟着一个真实的需求——项目经理张工想要一个"材料风险预警"功能——看它在传统模式下有多痛，又是怎么被一条"产线"在 10 分钟里生产出来的。

---

## 一、为什么我们做 AI，越做越累？

最早做的是 1.0 版本的小师父——给**项目经理**用的 AI 助手，接了 LLM，能回答问题。第一个功能上线，项目经理觉得很好用。于是需求接踵而来。张工要"材料风险预警"，隔壁要"验收卡点提醒"，再隔壁要"材料进度查询"……看起来都是小功能，可每一个我们都得从头打一遍。

  

### **场景一：同一个 Agent，每个业务场景都要单独开发**

项目经理播报、验收卡点提醒、材料进度查询——每个需求都要走一遍：会话管理 → 场景路由规则设计 → 前端设计新卡片 → 接入 Bella 工作流 → 结果处理返回前端。**周期 15–20 人天/次。**

**我们不是在"做 AI"，是在做"重复的工程搭建"：新卡片、新接口、新数据源、新处理，每次从零开始，无休循环。**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=Y2U1M2M3MWI3MDNkZmJkNDA5NzA2YzZlYTNjNmUyNTNfbVFlUFJyemoxc0duaHVIZUMwc2RPQWlXMmVveHI2QkZfVG9rZW46VFBQVWJXV0lWb2RwRXl4UHFCNWNCZ3VZblpkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

同样的道理，张工在工地上手写一张辅材清单拍照发群——Agent 要识别图片、匹配系统 SKU、推荐给他。做法还是那套机制，只是多配一个 OCR Tool。

  

### **场景二：同一个功能，每个渠道都要单独开发**

好不容易把"材料预警"做好了，PC 端能用。结果发现工长不用 PC，得适配 APP；APP 做完，又要适配企业微信 Bot……同一套 AI 能力、同一套对话逻辑，**因为渠道不同，每接一个渠道就要专门开发一次。**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NDFlMmEyMjIyN2E2ZWRiNzEyNDg3MzkxYzdmOWQyNzhfck5mcVMzNGMzT0pPQmVJWllWMk9OZG1GSFJ6bTcyZmtfVG9rZW46VHZjb2JtU3h1b1NTMHJ4UVAyRmNzZ1V5bk9iXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

这是两个维度的重复开发：**业务场景维度 × 接入渠道维度**。没有底座，每一个组合都是一个独立项目。

**需求是乘法，人力是加法，跑不赢。**

  

**那么——如果换一种做法，能有多快？？？？？**

  

---

  

## 二、如果换种做法，能有多快？

**同一个"材料预警"需求，从 15–20 人天，压缩到 10 分钟。**

|   |   |   |
|---|---|---|
|**场景**|**平台化前**|**平台化后**|
|场景一：项目经理助手新增业务场景|15–20 人天|**10 分钟**|
|场景二：BSU 智慧解读新增接入渠道|每渠道 15–20 人天|**10 分钟**|

零代码、10 分钟上线，且无需修改业务流程。

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=Y2M2NGEyYjhlOWVmZTY4OTUyMzA0Mjg0YmY2NzRjZjdfQjJPbEphaHhoaEVDeWdEcGdiN3h6STNTSnRqTVVGc1lfVG9rZW46RmRkNWJwSjFGb2h2Y0d4QTlUdWNBdTNybkhnXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

> 系统演示地址：https://workcenter.home.ke.com/admin

  

**我们为什么能这么快：**

- ✅ **省去会话管理**：生命周期、上下文记忆、断点续传，平台统一承载，新 Agent 天然获得
    
- ✅ **省去前端联调**：统一 GUI 协议，新 Agent / 新场景上线，前端零改动
    
- ✅ **省去渠道适配**：一套 Agent 能力，配置即可对接任意渠道
    
- ✅ **省去接口重复开发**：Tool 一次注册，全平台永久复用，不用每个场景重写一遍
    
- ✅ **省去路由开发**：规则 + 轻量模型自动路由，新场景注册即生效
    
      
    

这五个"省去"，本质是同一件事：**把重复的部分沉到底座，让每个新 Agent 都站在别人肩膀上。** 凭什么能做到？答案在底层架构。

  

---

  

## 三、凭什么能这么快？——把"运行时"和"业务"彻底拆开

  

**别人做 AI 中台，是在 Web 分层上加 AI；我们是面向 Agent 从头做原生分层。**

  

"小师父"把整个服务核心拆成两个**完全解耦**的独立层——这是我们和其他通用 AI 中台最本质的区别：

- **Agent Runtime（运行时底座）**：统一 GUI 渲染协议、上下文记忆管理、全生命周期会话管理、多模态输入输出、统一协议转换。这些是所有 Agent 共用的"公共设施"，Agent 本身完全不感知。
    
- **Multi-Agent（业务执行层）**：具体业务逻辑，独立发布，改它不动底座。
    
    ![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZWI4ODFkNmVhY2RkM2VkODczMWM5NWI0OWVmNGZmNjFfUUlnM2dDcmlQdGZnZHQ3Y1BzZENOVTdyMGUyRDVZTXhfVG9rZW46RXJTWmJBc1gwbzdGekN4YU9pWWN5dVpxbnhoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)
    

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YWVjYmM3MjlkMTYwOTczNzFmMmQ3NjMzNjI2ZjAyYmZfSzB4a3hkTHQ5ekF0RFI4ZVVndk02RDdaNGNCbTAxSGJfVG9rZW46SmZBRGJiR21Nb3BwRVd4YTFiM2NsY0Fjbk5nXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

**分层架构，到底换来了什么——一张表说清：**

|   |   |   |   |
|---|---|---|---|
|**对比维度**|**传统 Web 分层 AI 中台**|**小师父 Agent 原生分层**|**带来的价值**|
|**会话管理**|每个 Agent 各自实现|Runtime 统一定义，Agent 不感知|会话管理写一次，所有 Agent 天然继承，无需重复实现|
|**前端适配**|每个 Agent 单独联调|一套协议，所有 Agent 通用|新 Agent 上线零前端工作量|
|**新增 Agent**|代码 + 接口 + 前端联调|配置化 + 零代码 Publish|上线周期从 20~30 人天缩至 1 天|
|**能力复用**|复制代码改参数|Runtime 能力自动继承|底座一次升级，全量受益|
|**稳定性**|业务改动可能影响会话|底座不变，业务层独立发布|风险隔离，业务层频繁迭代不会引发底层异常|
|**上下文记忆**|各 Agent 独立维护，无法共享|Runtime 统一管理会话生命周期|体验一致，成本可控，数据可统一沉淀|

  

  

  

底座搭好了，那么——一个新 Agent 到底是怎么"长出来"的？

---

## 四、一个 Agent 的一生：从出生到成长

下面我们跟着张工的"材料预警 Agent"，走完它从出生到成长的一生。这一生分五步：**出生（搭建）→ 喂养（知识）→ 上岗（触达）→ 管理（运营）→ 成长（记忆）。**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YmZiYWQxZDZiNjk4YmEzMzgwYzllZTE4OGE3NDgxMTlfdGFBTmFqZUZ5Sm42MDRoS3JUeHdSVmJLbms4VTZ3elNfVG9rZW46RFcySWI3Y0g4b2hiVGp4MVpCOWM2TUxzbndkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

### 4.1 出生：Agent 怎么搭出来——Skill / Tool / 知识库可插拔

Agent 本身只是一个"壳"，它的能力全部来自外挂：

- **加一个 Skill**（Markdown 指令文件）→ Agent 学会了怎么处理某类场景（比如"材料风险分析"）
    
- **绑一个 Tool**（本地方法或 MCP 远程接口）→ Agent 多了一项实操能力：搜订单、算报价、调外部服务
    
- **挂一个知识目录** → Agent 能回答这个领域的问题
    
      
    

**"材料预警 Agent"，就是选个模型、勾几个 Skill、绑几个 Tool、挂上知识库、配好渠道、点上线——不需要改一行 Java 代码。**

  

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YmZlZWYxYjUyZmY0MmRjNTBlMmExMzBkZjFlNmMzMjZfQ0FBQTJSbVl1NzFhTm1ZTWVhUUVmTmgzUTQ5eko4eHRfVG9rZW46WHVOSmI0R09CbzdrNk54cVEzQ2NJVzVObk9iXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

> **目前成果**：平台已沉淀 **31 个 Skill 领域、85 个 Tool 方法**（系统内嵌 62 个 / MCP 接口 23 个），全部可组合复用。

  

- **零代码极速对接**：工具对接从周级缩短至分钟级，无需写集成代码
    
- **积木式能力拼装**：资源独立解耦，自由组合生成不同场景 AI 能力
    
- **全域资产复用**：一次注册，全平台所有 Agent 共享，彻底消除重复开发
    

  

**搭好之后怎么上线？空间隔离，一键同步。** 平台支持三个粒度的配置同步，覆盖从整体上线到局部调优：

- **环境同步 — 批量晋升，整体上线**：开发调好、测试通过，点一下直接推生产。
    
- **空间同步 — 按需迁移，快速复用**：新业务线冷启动，直接克隆成熟空间改改就跑。
    
- **Agent 同步 — 精准同步，局部调优**：Prompt 改了、技能脚本更新了，只同步这一个，不影响其他配置。
    
    ![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=M2E4MjQ0MjlkNWQ2OGVjOGRjMGU2OGYzYzY3OTY3ZTZfaUNaaWxVdnFqMTg3RUtoUmFmazQ0UzZRVXFkR0pwbWpfVG9rZW46S2hBSmI0eDN3b0U5cWF4MmE1N2NBdE93bkpjXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)
    

  

### 4.2 喂养：让 Agent 肚里有货——知识检索更准确

材料Agent 要判断"材料有没有风险"，光靠 LLM 的训练知识不够——它得读得懂公司的施工规范、材料手册、FAQ，还要能结合订单数据一起回答。这就是知识检索要解决的问题。

- **以知识文档 + 知识 QA 为基座**
    

知识库分两类：一类是**知识文档**（施工规范、操作手册、标准流程），另一类是**知识 QA**（整理好的标准问答对、FAQ）。两类知识在存储结构和检索方式上完全不同，分开建库，各走各的最优路径。

  

- **Agent 自己决定用哪种方式检索**
    

这是和传统 RAG 最大的区别。传统方案是"问什么都走向量检索"，我们让 Agent 根据问题类型，自主选择检索工具：

**Agent 不是盲目调用检索，而是"看了问题再决定怎么找"，每类问题走最适合它的路径。**

  

- **数据 + 知识如何结合**
    

纯知识库解决不了"查实时数据"的问题，纯数据查询解决不了"解读和建议"的问题。张工问"这批定制柜材料有没有风险"——Agent 会先调 Tool 查订单状态，再检索知识库里的材料风险判断标准，把"这批货当前在哪个阶段"和"这个阶段的风险点是什么"合并成一个完整回答。数据提供事实，知识提供解读。

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MTJjYTEzYjU0YzQ4Y2FlYjE5M2I4MDljN2NjMWY0Y2FfQkQ2cGdMUVdDZlNKNFl1cHFvdUpVdGFnZ0UyVzBxbzNfVG9rZW46VXY2QmJHMUdQb2pGUjN4M0RWNWM0Q3ZRblRoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

- **效果上有什么不同**和通用 RAG 相比，差异主要在三点：
    

|   |   |   |
|---|---|---|
|**对比点**|**通用 RAG**|**小师父混合检索**|
|**检索策略**|问什么都走向量|按内容类型分流，各走最优路径|
|**QA 类问题**|召回文档片段，LLM 再理解|直接匹配标准答案，高置信度秒回|
|**数据 + 知识结合**|无，只能查静态文档|Tool 查实时数据 + 知识库解读，合并作答|
|**召回保障**|向量相似度低时直接漏掉|关键词兜底 + 安全网补全，不轻易漏|

[知识库多方案检索能力测评](https://beike.feishu.cn/wiki/ZSxxw9NLZiTYu1k1VFTce0axnDg)

  

**想深入了解检索细节，见下方架构图：**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=OGRlNTdkODY4MmVkNTkyZWVjOGM3OWNkMjE5MjU0MmJfM0RjbE5TNlpzZ0p1ckZzMkdFUVYyQjhFUk9nZjVPRFFfVG9rZW46SDB1ZWJFN0Ztb0pEYXR4MkY1U2M2R2kxbmRkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MjhmZmI5M2U0M2YyZjE2MDc2MjU5ZmVhNjk3OGQ1NjVfNVl4OVkwcktmZUxlTTlnV2ZqVENBUDFEWExOclpaR2ZfVG9rZW46UmdiSWIzbGplb2ROVk54b3AwTGNmTlVqbmpjXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

### 4.3 上岗：让 Agent 见到用户——触达场景更丰富

  

**材料Agent 造好、喂饱了，接下来要让它出现在张工面前。同一套 Agent 能力，需要暴露在 Web、H5、企业微信、小程序等多个渠道，每个渠道可能需要不同的编排和交互方式。**

  

**多渠道输出：**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MzNlZTJmYmJiMDBkYTZjYTI4ZTk5NjJkM2M5M2NmNjhfdXF1VWhFcXBQWk5QT2NOZXFadkVJaFM3aW4xbTJBc0tfVG9rZW46QVFzWmJESnNzb0Y5aW14bXBLeWMwY3pSbkdmXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YWMzZmFjNTNkZjE2NGMwMjFjYzQwZWJjZTg1ZmM1N2JfNGN4Z2wxdlpLZVdoeDZvU1k1cFRlVHhqdG82Z0ZiUmhfVG9rZW46SGxOS2JMY1BLb295OFh4dTJtb2NiTjFUbmgwXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YzE3NzcxY2ZjOGU4ODI1OTA0OTkwNmEyZTU4MDBhNThfT3piU2M0cXhoT25RdTYwWlFJbDdlS0NDTjFUTmRPSG9fVG9rZW46VURQSWJBMEk4b1ZEMm14SVYzMGNHVUF1bnJoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

**快捷场景 + 组件模板配置：** 不是所有问题都要浪费 LLM 算力。

**确定性场景走捷径（组件 / 工作流），又快又省；不确定场景走 Agent，灵活智能。**

  

快捷场景支持 Bella 工作流模式、前端组件模式（直接吊起组件）、Agent 模式；支持"前端常驻""指定用户展示"等。

- 前端组件配置化
    

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZTVjZTM1MDMxODVmY2E5YWRjNTI3ZGNiOWIxODRiYWRfbXJ4djltS2V1dFBDZjBJcDNTSno5bW9ZSTE0WGI5eGRfVG9rZW46SHBIOGJGNEF2b1ZWdE14VFRSNWNtcVNNbmZoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MWIxZDkxZGNlN2U4MTAyMjViZmY2ODQ1ZTQ2YWFmMWNfdHUwSDlMd3h6eW50ODFabkpZVEU4U3E3OHVNSWVDOFdfVG9rZW46RjZoa2JYdEFrbzUyODR4RUdqdmNnVEJmbm44XzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

- 卡片返回配置化
    

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YWMwYTdhYzBkYTViMTc4ZmZlZDNiNjJkYWMwMTliZTJfZG1DdkVYQ2xpODl5MkhnNjEzWW9DVGxVQVBkYXNyMHlfVG9rZW46QjhzM2J2bmFJb0VnZ2x4MHhJVmNsWVpZbnhpXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YjliY2FjMWE1YzcyNmY4ODI2NWY4NjZiYzkxOTQzOTJfM0FLUG56ejVtMU13OU81OEQyc0ZEUld0ZzR5YVVJMDZfVG9rZW46VnFENWJpYXlUb1V6eG94aGZDWmNnQ0VvbjFiXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

### 4.4 成长：Agent 越用越聪明

Agent 上线是起点，不是终点。跟着张工看三个阶段。

**Day1：张工**开始用材料预警 Agent。有些问题答得不够准——材料风险的 Skill 覆盖场景不全，有时路由选错了 Agent，有时知识库里根本没有这方面内容。这很正常，新 Agent 上线都这样。

  

**第一个月：回答越来越准**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=Zjg5NTBhZTdkOTA4NGNjMWQzZDE3ZjE5YmIwYzUyNzBfdkc5Z09NeDRua0ZuOENvS1pWbzh5SFprTkR0ZmYxa2VfVG9rZW46VzBYdWJoRFhibzZIYkJ4aVg2V2N4WGpsbjlSXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

- 每次对话结束后，Judge 机器人会异步对这条回答打分（0~5 分）。输入包含用户问题、工具调用链路和 Agent 回答，从相关性、准确性、完整性、逻辑性、流畅性五个维度综合评估，打分结果连同评分原因定时回写到数据库，同时标注是"好回答（4~5 分）/ 有效回答（3~5 分）/ 无效回答（0~2 分）"。
    
- 低分回答会被筛出来。运营打开**会话审阅台**，筛选 1~3 分的会话（支持按 messageId/traceId/sessionId 精确定位），右侧面板一站式呈现四类信息：对话内容（问 + 答）、评分 + 评分原因、工具调用链路（读了哪些 Skill、调了哪些工具、耗时多少）、AI 一键诊断。点击诊断，LLM 流式输出根因分析和具体改法。
    
- 找到根因后，进入**调优工作台**。系统自动判断问题属于哪种类型
    
- 运营确认 AI 建议后一键保存，每次修改自动留版本快照，改坏了可以一键回滚。改完用同一问题重跑，对比前后打分——从"改了感觉好像好了"变成有数据支撑的工程闭环。
    

  

_会话审阅台：_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NDg4ZTg0NDE2ZWYwNzcyMTZkNTU3NjBkODFiYmE0MzNfMWlMWW5iWUw0N0w0cG5FQVhFZ0ZyMGxZVUh2allqTElfVG9rZW46UE1vcGJnQ3dZb0d4OGd4WndmTmNyZ0JxbnNnXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

**第三个月：越来越懂张工**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MzYzMDU1MWMzYzNhYWMyNWE1YTRhYmExZWQxODA4ZjlfcXVxTUcwcGRZUjBqdHkzNjduenduSWFBcHlyRUtzdjNfVG9rZW46V2lpQmJSTFhabzhDTTF4WHE4SGMwWjFWbmliXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

- 每次对话结束，系统检测到满足条件（对话轮数 ≥3 且张工发言 >100 字），就会异步提取这次对话里的记忆，不影响主流程，不对每条消息都调大模型，成本可控。
    
- 记忆分三类：**偏好**（交互风格和习惯）、**经验**（讨论过的决策和解法）、**行为模式**（高频关注什么）。系统按张工的角色——项目经理——用专属提取规则处理，重点抓施工经验、工期排产方法、验收标准偏好，而不是用通用规则一把抓。
    
- 提取到的记忆存下来后，下次新会话开始时加载为快照，注入到系统提示词开头，500 字预算内按优先级排列：偏好全量 > 行为模式全量 > 经验取前 5 条。整次会话内容不变，避免对话中途记忆更新导致前后不一致。
    

  

**三个月后，张工不用每次解释"我是工长，管着 20 个工地"——Agent 已经知道了，知道他喜欢先看延期预警，偏好简洁回答，习惯周一早上看工地概览。**

  

**长期记忆，让agent更懂你**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZWE4NjA5ZTRmMTdkNmY5YTZmMjhlYzNkNDM4NTU5NjdfYjN1U3FYRm5WN3RPYjQzWVE0TlRtbmhMYU9xRmUxSFdfVG9rZW46TlBzTWJrVUFkb1Q2MnR4UmI1UmN0RUZrbnNkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

**下半年：Agent 开始主动发现自己不会什么**

当前的改进还依赖运营去审阅、发现问题。下半年规划让 Agent 更主动：

- **知识盲区感知**：ES 检索命中率持续偏低 + 同类问题高频出现，系统自动标记这是知识空白，生成待补充条目提示运营填
    
- **对话数据回流**：每日扫描对话，LLM 打分筛选优质问答，沉淀为知识库候选，人工审核后入库，对话本身成为知识生产的来源
    
- **记忆精细化**：写入新记忆前先检索已有记忆，判断是新增/更新/跳过，防止同一信息反复写入；接入部门角色画像系统，新用户第一次对话就有基础画像；记忆量大了之后按问题语义召回最相关记忆，而不是固定截取前 N 条
    
      
    

**用得越多，它越聪明——这是设计出来的，不是靠运气。**

  

至此，张工的"材料预警 Agent"走完了一生：搭出来、喂饱、上岗、越用越聪明。**而这一整套，别的框架能给你吗？**

  

---

## 五、这不就是又一个 Dify / LangGraph 吗？

  

**一句话：通用框架解决"能跑起来"的问题，我们解决"能持续运营"的问题。**

  

市面上的 AI Agent 框架大致四种路线：

|   |   |   |
|---|---|---|
|**路线**|**代表框架**|**核心理念**|
|企业级图编排|LangGraph|状态机驱动，精细控制执行流|
|多角色协作|CrewAI、AutoGen|多 Agent 像团队一样协作|
|低代码全栈平台|Dify|拖拽构建，业务人员可直接操作|
|自改进个人 Agent|Hermes|越用越聪明，自动积累技能|

Construction Agent 基于 Spring AI Alibaba 构建，面向家装垂直领域，走的是**垂直深耕 + 运营友好**路线：

|   |   |   |   |   |   |
|---|---|---|---|---|---|
|**维度**|**Construction Agent**|**LangGraph**|**CrewAI**|**Dify**|**Hermes**|
|**语言/生态**|Java / Spring|Python|Python|Python|Python|
|**定位**|垂直领域生产应用|通用图编排框架|角色驱动多 Agent|低代码全栈平台|个人自改进 Agent|
|**多 Agent 编排**|路由 + 并行子 Agent|高度灵活（图结构）|Crew 角色团队|有限支持|单 Agent 为主|
|**知识库**|文件系统 + LLM 元数据|需自行集成|需自行集成|内置向量 RAG|无内置|
|**Prompt 管理**|版本化 + 热重载|代码级|代码级|可视化编辑|无|
|**环境同步**|✓ 多环境一键同步|✗|✗|✗|✗|
|**空间隔离**|✓ 多空间完整隔离|✗|✗|工作区概念|✗|
|**运营后台**|✓ 完整运营 UI|✗|有限|核心优势|✗|
|**调优工作台**|✓ 发现→诊断→修复闭环|✗|✗|✗|✗|
|**长期记忆**|✓ 角色专属 + 自动提取|✗|✗|✗|✓（个人级）|
|**无需重启上线**|✓ Prompt/技能热更新|✗|✗|✓|✗|
|**面向用户**|工程师 + 运营人员|工程师|工程师|业务人员|个人开发者|
|**生产成熟度**|高（线上运行中）|最高|高|高|低（刚起步）|

> LangGraph、CrewAI 解决的是 Agent 怎么编排、怎么协作，但它们不关心这些**在家装业务里每周都会发生**的问题：
> 
> - Prompt 改了怎么不重启上线？
>     
> - 多个业务线怎么互不干扰？
>     
> - 测试环境调好的配置怎么快速推到线上？
>     
> - 低质量回答怎么自动发现、诊断、修复？
>     
> - Agent 怎么越用越了解用户？
>     

  

这些是纯工程框架留给开发者自己解决的问题。我们在 Spring AI Alibaba 基础上，围绕"可运营"建了一套完整配套：**空间隔离保证多线并行、版本管理支撑快速迭代、一键同步打通环境晋升、调优工作台闭环持续优化、长期记忆让 Agent 越用越聪明。**

  

---

  

## 六、说了这么多，真有效果吗？

  

平台已在生产环境运行。数据说话：

|   |   |   |
|---|---|---|
|**指标类别**|**具体指标**|**数据**|
|**覆盖规模**|已接入 Agent 数量|14 个|
|覆盖业务空间数|8 个|
|覆盖渠道数|APP（精工/home）、PC（鲁班/星海）、企微群、业主小程序、微信 Bot|
|覆盖业务场景|BSU 智慧解读、项目经理 AI 下单、项目经理小师父、供应链小师父等|
|**效率提升**|Agent 上线周期|20~30 人天 → **1天**|
|新增 Agent 前端联调|3~5 天 → **1 天**|
|工具接入周期|周级 → **分钟级**|
|**成本优化**|接口返回值压缩|token 消耗降低 **40%~60%**|
|路由阶段模型切换|完整模型 → QWEN3_5_FLASH，单次路由成本降约 **90%**|
|**用户反馈**|接入方整体满意度|**4.8 / 5**（10 位接入方参与）|
|推荐意愿（NPS）|**70**|
|效率提升感知|90% 认为更快，其中 70% 表示节省 50% 以上时间|

---

  

## 七、然后呢？——H2 规划：从"能用"到"好用、可信、会学习"

  

当前平台已具备多 Agent 路由、知识库检索、工具调用等核心能力。H2 目标是补齐**持续运营和智能进化**的底层能力。

  

### 7.1 让流程闭环 —— Agent DevOps

**核心问题**：当前 Agent 上线即"黑盒"，出问题靠用户反馈，能力改进靠人工，缺研发到运营的完整闭环。

|   |   |   |
|---|---|---|
|方向|具体内容|时间|
|安全守门|关键词黑名单拦截，高危输入不进推理链路|Q3|
|运维感知|错误率/延迟/消息量聚合指标，自动企微告警|Q3|
|能力评测|Langfuse 批量测试用例 + AI 打分，质量可量化|已有，持续完善|
|审阅台 + 调优工作台|低分会话自动发现 → AI 诊断根因 → 自动生成修复建议 → 版本保存|已有，持续完善|
|数据回流|每日扫消息表，LLM 打分，优质 QA 沉淀到知识库候选池|Q3|
|知识盲区发现|ES 命中率低 + 同类问题高频 → 自动标记知识空白|Q3|
|Prompt AB 测试|评测分下降自动生成改进建议，灰度验证后晋升主 Prompt|H2|

  

> **闭环路径**：研发配置 → 一键发布 → 线上运行 → 指标告警 → 审阅台发现问题 → AI 诊断 → 调优修复 → 评测验证 → 再发布

  

### 7.2 让数据流动起来

**核心问题**：Agent 每天产生大量高质量对话，但数据沉睡在消息表；数仓有海量业务数据，但 Agent 无法直接访问。

|   |   |   |
|---|---|---|
|方向|具体内容|时间|
|对话数据清洗回流|每日扫消息表，过滤低质，LLM 打分，优质 QA 沉淀候选池|Q3|
|知识盲区发现|ES 命中率低 + 同类问题高频 → 自动标记空白，生成待补条目|Q3|
|个人经验 → 团队知识|同一主题被 ≥3 个用户记忆覆盖时，触发知识沉淀建议|Q3|
|DataAgent 自然语言数仓|用自然语言查数仓（工期延误率/材料损耗等），完成调研+选型+MVP|H2|

> **数据流向**：线上对话 → 清洗+打分 → 知识库/Prompt 候选池；个人记忆 → 聚合分析 → 团队共享知识；业务数仓 → 自然语言查询 → Agent 直接回答数据类问题

  

### 7.3 提升用户体验

|   |   |   |
|---|---|---|
|方向|具体内容|时间|
|接入 a2ui|响应支持结构化 UI 输出（卡片/表格/图表），不再只是纯文本|Q3|
|减少工程约束|梳理硬编码路由、工具白名单等能力边界，逐步打开限制|Q3|
|提升回答覆盖率|高频"答不出"场景建兜底策略，保证每个问题都有有意义的回应|Q3|
|减少答非所问|优化意图识别和路由，分析错误路由典型 case 针对性改进|Q3|
|长期记忆质量提升|角色专属提取规则 + 语义去重 + 记忆检索，让 Agent 真正"认识"用户|Q3|

  

> **智能提升路径**：说得出（覆盖率）→ 说得准（路由+意图）→ 说得好看（富文本输出）→ 认识你（长期记忆）

  

---

  

> **回到开头那句话：我们的目标不是做一个聊天机器人，而是打造一套能自我进化、持续运营的 AI 工程体系——把"做 AI"从老师傅的手艺，变成一条越跑越快、越跑越聪明的产线，让 AI 真正成为业务的水电煤。**

  

---

  

## 附录：技术栈

  

**核心原则**：不追"最热"，选能和已有基础设施（Spring 生态、ES、Apollo）无缝集成的方案。

|   |   |   |
|---|---|---|
|层级|技术选型|选型理由|
|语言 & 框架|Java 17 + Spring Boot 3.2.5|团队技术栈统一，Spring 生态成熟|
|AI 框架|Spring AI Alibaba 1.1.2|与 Java 无缝集成，原生支持 ReAct Agent|
|Agent 运行时|Spring AI ReactAgent + Graph|支持多步推理、工具调用、图状态管理|
|数据库|MySQL|核心业务数据存储|
|缓存 & 分布式锁|Redis + Redisson|会话缓存、分布式锁、Graph Checkpoint|
|向量检索|Elasticsearch KNN + match_phrase + RRF|结合公司已有 ES 混合检索 + 本地检索|
|可观测性|Langfuse + OpenTelemetry|开源可自托管，三层 Span 覆盖请求→LLM→工具|
|配置中心|Apollo|动态配置，环境隔离，路由规则热更新|
|ORM|MyBatis-Plus 3.5.5|代码生成、分页、条件构造器|
|多模态处理|DJL + ONNX Runtime + JavaCV + FFmpeg|图片/视频/音频本地处理|
|工具协议|MCP（Model Context Protocol）|标准化外部工具接入|
|文件存储|AWS S3|多模态文件、知识库源文件|
|工作流引擎|Bella Workflow|公司内部工作流平台|
|通信协议|SSE（Server-Sent Events）|Agent 流式推理结果推流|

  

# 二、**平台化：10分钟-从需求到上线**

##   2.1 场景一：定制柜进度查询，平台接入全流程

  _**定制开发做了什么（15~20 人天）**_

|步骤|内容|耗时|
|---|---|---|
|写接口|取用户 → 查工地 → 按品类筛订单 → 调进度，可能涉及 3~4 个后端接口的串联调用代码|3~5 天|
|写意图识别|用户问"柜子到哪了""木门装好了吗"要能识别为"查进度"意图|2~3 天|
|调 prompt|把接口返回的 JSON 拼进 prompt，反复调试输出效果|2~3 天|
|写前端渲染|是输出纯文本还是卡片？进度信息怎么排版？|3~5 天|
|联调测试|前后端联调 + 多端适配（APP、PC 至少两轮）|3~5 天|

  _**平台做了什么（10 分钟，零代码）**_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZjQ0NDM1MzQ4YzMzYWI4YTJlNGQ5OGUyMDRjYTY2MjVfSTRiNnM3NnJ4enNxTFFOWFJxUERNeGxacHpKMXBqaEFfVG9rZW46SllvemJrTlRWb1hFYkZ4aFhERGNaUURXbmliXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

区别在哪儿呢？

**传统做法里最耗时的四个环节——写接口、写意图识别、前端联调、多端适配——平台全部省掉了。工具现成复用、路由自动注册、前端认协议不认 Agent。唯一需要人做的事是"描述这个 Agent 要干什么"，也就是写一句 prompt。**

  

##    2.2 场景二：净收入计算，平台接入全流程

  

   _**定制开发做了什么（15~20 人天）**_

|步骤|内容|耗时|
|---|---|---|
|搭向量库|搭 ES 集群/向量数据库，写 embedding 管线|3~5 天|
|文档处理|写切分逻辑、元数据生成、向量化入库脚本|3~5 天|
|写检索逻辑|混合检索策略、排序、去重、拼接上下文|3~5 天|
|写数据接口|调 BSU 接口取城市级规则和实际数据|2~3 天|
|调 prompt + 联调|prompt 工程 + 前后端联调 + 测试|3~5 天|

  

   _**平台做了什么（10 分钟，零代码）**_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZWE1MmYxZGY4Y2RiMDljNmIyZmRmMWU4NTYyYWQzM2VfQ0E4bHRNMkVMcllGdDlGYzlCZ1J0dWVFMDVYb0dQOTRfVG9rZW46QUxiVmJ3eXlob1FRZk14SXJUV2NYdjhQbjJiXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

区别在哪儿？

**传统做法里最重的三个环节——向量库搭建、文档处理管线、检索逻辑开发——平台全部收进系统。业务同学只需要做两件事：上传文档、写一句 prompt。检索怎么做的、切分怎么做的、向量怎么存的——完全不需要知道。**

##   2.3 系统演示：

  http://preview.workcenter.home.ke.com/agent-config.ejs

  

**我们为什么能这么快：**

✅ 省去接口开发：工具一次注册，全平台永久复用，不用每个场景重写一遍

✅ 省去意图开发：路由自动注册，规则 + 轻量模型搞定，不用手写意图识别

✅ 省去前端联调：统一 GUI 协议，新 Agent 上线前端零改动

✅ 省去检索开发：知识库能力开箱即用，不用搭向量库、写切分逻辑

  

# 三、**能力全景**

"小师父"采用面向 Agent 的原生分层设计，将整个服务核心拆分为两个完全解耦的独立层：**Agent Runtime（运行时底座）** 和 **Multi-Agent（业务执行层）**，这是我们与其他通用 AI 中台最本质的区别

  

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NDFmMzEzNzgzMzRjMzFkZDdjNDk0MTA4MTk4ZjE3NGJfZXJBZzB6Vzl0SVkwNU1tUUpMZ1ZEQmliYlpBMFExMGlfVG9rZW46TDBPNWJjbDJWb1hLNEx4Wmw4YmNZUFdQbkZlXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ODc4OTUzMTA5NmFmYmIyMGI5NmUyMGJjY2U2NjMzOThfRk16aDlkR0VwODkxNExJbmVsazhXN2JJZXRuREhqVllfVG9rZW46SlR3ZGJ5VkdCb25VbXl4MVZCRWNvc3ZPbkpiXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

**分层架构 vs 传统方案对比表：**

|   |   |   |   |
|---|---|---|---|
||**传统 Web 分层 AI 中台**|**小师父 Agent 原生分层**|**带来的价值**|
|**会话管理**|每个 Agent 各自实现|Runtime 统一定义，Agent 不感知|减少 80% 重复代码|
|**前端适配**|每个 Agent 单独联调|一套协议，所有 Agent 通用|新 Agent 上线零前端工作量|
|**新增 Agent**|代码编写 + 接口对接 + 前端联调|配置化 + 零代码 Publish|上线周期从 20~30 人天缩至 10 分钟，响应效率提升百倍|
|**能力复用**|复制代码改参数|Runtime 能力自动继承|消除重复开发，底座一次升级全量受益|
|**稳定性**|业务改动可能影响会话|Runtime 底座不变，业务层独立发布|风险隔离，业务迭代不干扰底层能力|
|**上下文记忆**|各自为政|Runtime 统一管理会话生命周期|体验一致，成本可控，会话数据可统一沉淀|

  

# 四、**核心能力矩阵：**

## 4.1 agent搭建更简单

### 4.1.1 skill/Tool/知识库可插拔

**Agent 本身只是一个"壳"，它的能力全部来自外挂：**

- **加一个 Skill（Markdown 指令文件）→ Agent 学会了怎么处理某类场景**
    
- **绑一个 Tool（本地方法或 MCP 远程接口）→ Agent 就多了一项实操能力，搜订单、算报价、调外部服务**
    
- **挂一个知识目录 → Agent 就能回答这个领域的问题**
    

**所有能力全部可插拔。新增一个 BSU 智能问答 Agent？选几个 Skill、勾几个 Tool、挂上供应链知识库，完成。不需要改一行 Java 代码。**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NWM3MDg1YTYyNWYxZjJhNTgxYzUyMzQxYzMxZjViNWRfM0hTaVNSWUNEZUczQ1JSY2dtck1CeG5NbTQxMUdaWVZfVG9rZW46QkxEdGJmOVlob0hhTzR4eGlCdWNScU1SbnNkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

**优势：**

- 零代码极速对接：工具对接周期从周级缩短至分钟级，无需编写任何集成代码
    
- 全栈资源可插拔：通用工具、业务工具、知识库全支持动态配置，线上零风险
    
- 积木式能力拼装：资源独立解耦，可自由组合生成不同场景 AI 能力
    
- 全域资产复用：一次注册，全平台所有 Agent 共享，彻底消除重复开发
    

  

暂时无法在飞书文档外展示此内容

  

### 4.1.2 空间隔离，一键上线

灵活的配置同步体系，平台支持三个粒度的配置同步，覆盖从整体上线到局部调优的全部场景。

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ODY2MDRiMGI4YjNjN2MzYmUwYzRkN2MyODc0Mzg2YzRfV2U3N2NqODJzcUFaNlhCQ2h1eEVLeVBUUXIzU201VjFfVG9rZW46V3hLN2JYQ3hKb0hBOTN4cVZYQWN1WWdwblNkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

- **环境同步 — 批量晋升，整体上线**
    

将当前环境的全部空间和配置一次性同步到目标环境，适合完整版本的上线流程。开发调好、测试通过，点一下直接推生产。

- **空间同步 — 按需迁移，快速复用**
    

将指定空间的配置（渠道、Agent、Prompt、技能、知识库）同步到另一个空间，可跨环境也可同环境内复制。适合新业务线冷启动——直接从成熟空间克隆一份，改改配置就能跑。

- **Agent 同步 — 精准同步，局部调优**
    

单个 Agent 粒度，不受空间限制。Prompt 改了、技能脚本更新了，只同步这一个 Agent，不影响其他配置。适合快速迭代期的小步调整。

  

## 4.2知识检索更准确

语义检索并不是新概念，但真正落地到业务中，门槛一直不低：需要搭向量库、写 embedding 流程、设计文档切分策略、调试检索召回……每个环节都要人来维护。

我们想解决的问题很简单：能不能让业务同学只管上传文档，剩下的事情系统自动搞定？

所以我们构建了这套本地知识库系统。上传一份 Word、Excel 或 PDF，系统自动完成切分、元数据生成、向量化入库，并对外提供统一的检索接口。整个过程不需要使用者了解向量检索的任何细节。

对接 Agent 也一样简单——给 Agent 关联几个知识目录，它就能自主检索、按需取用，不需要额外开发任何检索逻辑。

核心理念是：**把语义检索的复杂度收进系统，把简单留给使用者。**

#### **整体架构：**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MmEyYzUxZmFmMmEzMjk2MThjOWNjZjg0YzBlNDM1ZDNfWEJPUzJqWnVJckE0OTh1N2c1YndxWklleFpYVkVscnFfVG9rZW46RFU1ZmJCcGlpbzY2TmF4eUFobGNqVFdkbmdkXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

_**[后管系统-知识目录页面](http://preview.workcenter.home.ke.com/admin-kb.ejs)**__**：**_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MTBkMjkzYzQzOGE0ZWQ5MmJmMzE5YjY1YzRjZjViNzVfTER4MVBtcTJrNmVtaWlmQ0lHdmxqQnBFRVkzQzgyWHFfVG9rZW46Q0xvYWJMUTNjb3BuMlh4WHBYeWM5QWR2bjVjXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

_**[后管系统-index页面](http://preview.workcenter.home.ke.com/admin-kb.ejs)**__**：**_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=OGU0ZWIxOTNmMGVjZGMzODQxNjI1MzgyNzc1ZjZjZjlfZmFrQlV5cFpGbmRCNWlYZVFVTzgyQnhzU2k3bVR6Z0dfVG9rZW46UjcyVGJkNGxRbzVJRk94MzVib2NtOGhVbm5jXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

#### **设计亮点1：LLM 只生成元数据，不重写原文**

切分后每个 Section 的内容原样保留，LLM 仅负责生成 summary(摘要) 和 scenarios（检索短语）。规避了 LLM 重写原文带来的幻觉风险，保证 Agent 读到的是真实内容。

  

#### **设计亮点2：list_knowledge_index-两阶段混合检索**

- **第一阶段（关键词评分过滤）**：低延迟、零成本，解决"索引过大 Agent 看不完"的问题。对 query 分词后，按字段权重评分（适用×3、描述×2、摘要×1、列名×1），取 Top-20 返回。两个安全网保证召回率：条目数 ≤ 20 时全量返回不评分，命中数 < 10 时自动补充无分条目至 10 条。
    
- **第二阶段（ES 语义向量召回）**：KNN + match_phrase + RRF 融合，补充关键词无法命中的语义相关内容。ES 返回的是 filePath，系统反查 _index.md 获取标准格式条目，与第一阶段结果去重合并后统一输出。
    

暂时无法在飞书文档外展示此内容

  

_**index.md结构：**_

|字段|是否必填|说明|
|---|---|---|
|标题|必填|LLM 提取的切片主题|
|描述|必填|一句话摘要，检索权重×2|
|适用|必填|检索短语，权重最高×3|
|摘要|可选|原文250字片段，权重×1|
|类型|必填|文档 / 表格 / QA|
|源文件|可选|原始上传文件名（用于upsert时精确匹配）|
|Sheet|可选|仅表格类型有，Excel sheet名|
|文件|必填|切片文件相对路径，Agent 用这个来 fetch|

  

_**检索流程图：**_

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NDI5Yjk2MzQ2NGI0OTFlOWJjYTU5NDY3MTE0OGQzZmJfN3U2Q3o4RktIdUtuaWpEMEVOWUhYQmNOTFRmYTBSNXBfVG9rZW46UXdWNmJtNG5tb0NIUE94bGZXaWNUUzdibldoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

#### **设计亮点3：源文件版本管理**

1. 每次重新上传同名文件，旧版本自动存档，可随时回滚到历史版本重新触发转换，不丢数据。
    

  

#### **设计亮点4：和HOME客服的第二屏系统打通，做到真正的知识生产+知识应用：**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZTgzNmVjYzVjNGRiODEzODRhOWNkYTFjNjVhY2Y4NTRfZDV2VUZGc2RNWDZ4SlljVGZVN3VsY0xVTndsWm4yTUVfVG9rZW46VzNDQWI5cXlab1ZPanh4cHNrcWNPSW9JbmNlXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

_备注：__[第二屏系统](https://app-fly.ke.com/07be5f0f-dfc9-49d8-a98e-5a47ecba3b7e#/)_

## 4.3 触达场景更丰富

**多渠道输出**

同一套 Agent 能力需要对外暴露在 Web、H5、企业微信、小程序等多个渠道，每个渠道可能需要不同的 Agent 编排和交互方式。

  **web/h5：目前支持APP(精工端)、PC(鲁班、星海)、小程序（业主端小程序）**

  **企业微信：支持企微群机器人**

  **微信bot：支持扫码添加微信bot，使用自定义agent**

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ODBkODY5NWU3NWE1YmI0ZTQ1NWIxZmZkZWU3YjVjN2ZfVWJaZ3hScWFaR1dyUW9pcG5wMFpQWWRYWTZjRzlCSHdfVG9rZW46UW5rVGJZeGkxb3FqWUF4WlRmdWNVMnN2bm5oXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=OGI3MmEzZWM5ZjVlNDgzNWFmMTdiZDkxYWY0ZGYzNjJfR0pGaG93QWpWR0JmMTREVU12TXJXZW9VdklHeFZSRGJfVG9rZW46QWdKTGJPWmhZb1dRV1h4M0lwaWNEYkY3bnV1XzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=YjA2ZWRiZDIwOTZlMmYxZjUzMzQ5NjFiODVkMWQxOTNfSWJUaGRoQkFsN3VrNXJjNmc1MVJRWWtaZld4bnpRWjhfVG9rZW46SWNGVWI1UENQb09Cd1d4OFZnZWNaUVJmbkdlXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=MDMwZDkxMDY4NjIxMDlhYjA1MGNhMWY1ZGE3MDZlZjBfclc2M05OZkZnN3BrOWMxWjhOM3NuMEdod2dxQkgxMFBfVG9rZW46Qnp0dWJTTnlib0FFaXV4eVVZSmM0S3pQbkI0XzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

##### **快捷场景 + 组件模板配置**

  确定性场景走捷径（组件 / 工作流），又快又省；不确定场景走 Agent，灵活智能。不是所有问题都要浪费 LLM 算力。

- 快捷场景：支持公司bella-工作流模式、前端组件模式（直接吊起组件）、agent模式；
    
         支持是否“前端常驻”、支持“指定用户展示”等
    
- 组件模板：**skill指定组件key->根据key查询映射模板->直接返回模板前端渲染**
    

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NGUwNDU4NzNkNTI0YWUyMjhmNmViMTZiN2UwODkwMzBfUXhMUzhBM2trVUxhN05WOHNWUHRxVUNoTkxUVzhjQVRfVG9rZW46T0l1SGJ3MEExb2Vlb0h4THFxS2NkeXZIblRiXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

## 4.4 agent使用更可信：**全链路可观测性+运营支撑**

### **核心痛点**

- 线上出问题查得到：全链路 Trace，一步定位是模型错了还是工具错了
    
- 版本迭代测得准：自动打分对比，不用人工一条条验
    
- 用户投诉追得到：按用户 ID 查历史对话，快速复盘
    

### **Langfuse 全链路可观测性**

三层追踪覆盖

|层级|追踪对象|实现方式|在 Langfuse 中呈现|
|---|---|---|---|
|**请求层**|用户 ID、会话 ID、运行环境|通过 TracingInterceptor 注入 MDC，转化为 OpenTelemetry Span 属性|支持按用户、会话、环境多维度过滤查询|
|**LLM 调用层**|每次 ChatModel 调用的提示词、回复内容、Token 消耗、响应延迟|由 ChatModelFactory 注入 ObservationRegistry，Spring AI 自动生成 gen_ai span|每条 LLM 调用生成独立追踪链路，包含完整输入输出|
|**工具调用层**|每次 Agent 调用 MCP / 内部工具的工具名称、参数、返回值、耗时、调用成功 / 失败状态|通过 LangfuseToolInterceptor 拦截工具调用，使用 Observation API 手动创建 Span|嵌套在对应 LLM 调用的 Span 下，最终聚合到根 Span|

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=ZTIwNjhmZjgzZjlhN2Y2OTk2MzJjMGI0YjgyY2M1NWFfaDdYekltWkVROUh3UkN3S3hkQjllVENqbVZVQmRVY21fVG9rZW46VHJ5aGJSR3Zmb2xSOWp4dnFacGNCdE5Obm9mXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

### **自动化测评体系**

在线 Tracing 解决的是「看」，但不够。每次调整 Prompt、修改 Skill、更换模型后，Agent 的回答质量变好还是变坏，靠人工一条条测不现实。测评体系提供了量化、可对比、可回归的质量门禁。

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=M2U5M2JjN2EyZmVmMDQ1ODY4Mzk5MTZlZmE5ZjlkYjNfVjdoTmt3Z1liV3U2QkhGeHFkRnNxS0QwbVpPdjltRnhfVG9rZW46RDRJY2JlNlVmb1RpeUZ4bVY5a2NVV2U3blZoXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

![](https://beike.feishu.cn/space/api/box/stream/download/asynccode/?code=NDIwYjdiNDE1OGE1YjcyN2YzYmU2ODllYTA1NGI0NGVfcVFhYkoxOFoydFpOZkxidFc2aENXb1RkTDRtRUVQeEJfVG9rZW46SVl2WWJ0YkRWbzREa3R4SGlzSGNEV1VobmRjXzE3ODQxOTI1Njk6MTc4NDE5NjE2OV9WNA&add_watermark=true&scene_type=CCM)

  

# 五、与主流框架的对比

  

目前市面上的 AI Agent 框架大致分为四种路线：

|路线|代表框架|核心理念|
|---|---|---|
|企业级图编排|LangGraph|状态机驱动，精细控制执行流|
|多角色协作|CrewAI、AutoGen|多 Agent 像团队一样协作|
|低代码全栈平台|Dify|拖拽构建，业务人员可直接操作|
|自改进个人 Agent|Hermes|越用越聪明，自动积累技能|

Construction Agent 基于 Spring AI Alibaba 构建，面向家装垂直领域，走的是垂直深耕 + 运营友好路线，和上述框架有明显的差异点。

---

横向对比

|维度|Construction Agent|LangGraph|CrewAI|Dify|Hermes|
|---|---|---|---|---|---|
|语言/生态|Java / Spring|Python|Python|Python|Python|
|定位|垂直领域生产应用|通用图编排框架|角色驱动多 Agent|低代码全栈平台|个人自改进 Agent|
|多 Agent 编排|路由 + 并行子 Agent|高度灵活（图结构）|Crew 角色团队|有限支持|单 Agent 为主|
|知识库|文件系统 + LLM 元数据|需自行集成|需自行集成|内置向量 RAG|无内置|
|Prompt 管理|版本化管理 + 热重载|代码级|代码级|可视化编辑|无|
|环境同步|✓ 多环境一键同步|✗|✗|✗|✗|
|空间隔离|✓ 多空间完整隔离|✗|✗|工作区概念|✗|
|运营后台|✓ 完整运营 UI|✗|有限|核心优势|✗|
|无需重启上线|✓ Prompt/技能热更新|✗|✗|✓|✗|
|面向用户|工程师 + 运营人员|工程师|工程师|业务人员|个人开发者|
|生产成熟度|高（线上运行中）|最高|高|高|低（刚起步）|
|企业级运营能力||||||

---

我们的差异在哪里：**通用框架解决"能跑起来"的问题，我们解决"能持续运营"的问题。**

LangGraph、CrewAI 这类框架解决的是 Agent 如何编排、如何协作，但不关心：

- Prompt 改了怎么不重启上线
    
- 多个业务线怎么互不干扰
    
- 测试环境调好的配置怎么快速推到线上
    

这些是纯粹的工程框架留给开发者自己解决的问题，而在家装业务场景下，这些问题每周都会发生。

我们在 Spring AI Alibaba 的基础上，围绕"可运营"这个目标，建了一套完整的配套体系：空间隔离保证多线并行、版本管理支撑快速迭代、一键同步打通环境晋升。

# 六、落地数据与成果

|指标类别|具体指标|数据说明|
|---|---|---|
|覆盖规模|已接入 Agent 数量|14个|
|覆盖业务空间数|8个|
|覆盖的渠道数|APP(精工/home)、PC（鲁班/星海）、企微群、业主小程序、微信Bot|
|覆盖业务场景|BSU-智慧解读，项目经理-ai下单，项目经理-小师父，供应链-小师父等。|
|效率提升|Agent 上线周期|从 20~30 人天 → 最快 10 分钟|
|新增 Agent 前端联调|从 3~5 天 → 0 天|
|工具接入周期|从周级 → 分钟级|
|成本优化|接口返回值压缩|token 消耗降低 40%~60%|
|路由阶段模型切换|完整模型 → QWEN3_5_FLASH，单次路由成本降 ~90%|
|规则命中时 LLM 调用|0 次 LLM 调用（能统计命中率的话更有说服力）|

  

# 七、H2规划

## **一、让流程闭环 — Agent DevOps**

  

**核心问题：**当前 Agent 上线即"黑盒"，出问题靠用户反馈，能力改进靠人工，缺乏研发到运营的完整闭环。

**H2 目标：**构建 Agent 从研发、上线到持续迭代的完整工程体系。

**规划内容：**

|**方向**|**具体内容**|**时间**|
|---|---|---|
|**安全守门**|关键词黑名单拦截，高危输入不进推理链路|**Q3 7月**|
|**运维感知**|错误率 / 延迟 / 消息量聚合指标，自动企微告警|**Q3 7月**|
|**能力评测**|Langfuse 批量测试用例执行 + AI 打分，Agent 质量可量化|**已有，持续完善**|
|**Prompt AB 测试**|评测分下降时自动生成改进建议，灰度验证后晋升主 Prompt|**H2**|
|**Dev 可视化配置**|Agent 配置在线编辑 + 在线测试 + 一键发布，摆脱手动改 DB|**H2**|

**闭环路径：**

研发配置 → 一键发布 → 线上运行 → 指标告警 → 评测分析 → Prompt 优化 → 再发布

---

## **二、让数据流动起来**

**核心问题：**Agent 每天产生大量高质量对话，但数据沉睡在消息表里；数仓有海量业务数据，但 Agent 无法直接访问。

**H2 目标：**打通对话数据 → 能力建设、业务数据 → Agent 可查询两条数据流。

**规划内容：**

|**方向**|**具体内容**|**时间**|
|---|---|---|
|**对话数据清洗回流**|每日扫描消息表，过滤低质，LLM 打分，优质 QA 沉淀到知识库候选池|**Q3 8月**|
|**知识盲区发现**|ES 检索命中率低 + 同类问题高频出现 → 自动标记知识空白，生成待补充条目|**Q3 8月**|
|**个人经验 → 团队知识**|同一主题被 ≥3 个用户 memory 覆盖时，触发知识沉淀建议|**Q3 9月**|
|**DataAgent 自然语言数仓**|用自然语言查数仓（工期延误率 / 材料损耗等灵活问题），完成调研 + 选型 + MVP|**H2**|

**数据流向：**

- **线上对话** ──→ 清洗 + 打分 ──→ 知识库 / Prompt 候选池
    
- **个人记忆** ──→ 聚合分析 ──→ 团队共享知识
    
- **业务数仓** ──→ Text2SQL ──→ Agent 可直接回答数据类问题
    

---

## **三、让 Agent 更加智能**

**核心问题：**Agent 当前的"聪明"受两层约束——工程层（能力边界硬编码，遇到覆盖外的问题直接答不出）和表达层（回复是纯文本，信息密度低，用户体验差）。

**H2 目标：**拓宽回答覆盖边界，提升表达质量，让 Agent 对所有问题都能给出有价值的回应。

**规划内容：**

|**方向**|**具体内容**|**时间**|
|---|---|---|
|**接入 a2ui**|Agent 响应支持结构化 UI 输出（卡片 / 表格 / 图表等），不再只是纯文本|Q3|
|**减少工程约束**|梳理当前因硬编码路由、工具白名单等导致的能力边界，逐步打开限制|Q3|
|**提升回答覆盖率**|针对高频"答不出"场景建立兜底策略，保证每个问题都有有意义的回应|Q3|
|**减少答非所问**|优化意图识别和路由逻辑，分析当前错误路由的典型 case，针对性改进|Q3|
|**长期记忆质量提升**|角色专属提取规则 + 语义去重，让 Agent 真正"认识"用户|Q3 7-8月|

**智能提升路径：**

**说得出（覆盖率）** → **说得准（路由 + 意图）** → **说得好看（a2ui 富文本）** → **认识你（长期记忆）**

---

**「我们的目标不是做一个聊天机器人，而是打造一套能自我进化、持续运营的 AI 工程体系，让 AI 真正成为业务的水电煤。」**

  

# 附录：

## 1.技术栈List

_核心原则： 不追"最热"，选能和已有基础设施（Spring生态、ES、Apollo、Polaris）无缝集成的方案。_

|层级|技术选型|选型理由|
|---|---|---|
|语言 & 框架|Java 17 + Spring Boot 3.2.5|团队技术栈统一，Spring 生态成熟|
|AI 框架|Spring AI Alibaba 1.1.2|与 Java 技术栈无缝集成，原生支持 ReAct Agent|
|Agent 运行时|Spring AI ReactAgent + Graph|支持多步推理、工具调用、图状态管理|
|数据库|MySQL (utopia_titan)|核心业务数据存储|
|缓存 & 分布式锁|Redis + Redisson|会话缓存、分布式锁、Graph Checkpoint|
|向量检索|Elasticsearch KNN + match_phrase + RRF|结合公司已有ES混合检索+本地检索方式|
|可观测性|Langfuse + OpenTelemetry|开源可自托管，三层 Span 覆盖请求→LLM→工具|
|配置中心|Apollo|动态配置，环境隔离，Agent路由规则热更新|
|ORM|MyBatis-Plus 3.5.5|代码生成、分页、条件构造器|
|多模态处理|DJL + ONNX Runtime + JavaCV + FFmpeg|图片/视频/音频本地处理|
|工具协议|MCP (Model Context Protocol)|标准化外部工具接入|
|文件存储|AWS S3|多模态文件、知识库源文件|
|工作流引擎|Bella Workflow|公司内部工作流平台|
|通信协议|SSE (Server-Sent Events)|Agent 流式推理结果推流|

  

## 2.接入手册：

后管平台地址：

线上环境：https://workcenter.home.ke.com/agent-config.ejs

预发环境：http://preview.workcenter.home.ke.com/agent-config.ejs (预发环境目前内容最全面，可用来测试)

openApi：[Agent 接口文档](https://beike.feishu.cn/wiki/UItxwpOrxibwfRk3DBPcf2oOnIe)

后管接入手册：http://preview.workcenter.home.ke.com/quickstart-guide.ejs

---
## 相关文档

- [[会议与分享/分享/技术方案-小师傅]] — 「小师傅」AI助手1.0技术方案
- [[会议与分享/分享/知识库—— 四种技术方案对比实验报告]] — BSU知识库方案实验
- [[会议与分享/我的产出--新人工具/Obsidian + Git + 飞书 笔记协作工具链配置手册]] — Obsidian工具链配置
- [[会议与分享/分享/智能纪要：Obsidian工具使用经验分享 2026年7月2日]] — Obsidian使用经验分享