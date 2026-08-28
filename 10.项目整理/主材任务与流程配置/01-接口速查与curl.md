# 01 接口速查与 curl

> 本地服务地址：`127.0.0.1:8080`。以下 curl 均为可直接执行的示例；带 ⚠️ 的注意点来自代码核验。

## 1. 主材任务生成接口

| 接口 | 位置 | 状态 |
|---|---|---|
| `POST /api/construction/task/create` | `MaterialConstructionTaskController.java:102` | **主用** |
| `POST /api/task/create` | — | 已 `@Deprecated` |
| `GET /api/construction/task/sdm/create` | — | SDM 入口 |

创建链路：
`MaterialConstructionTaskController.create` → `materialTaskCreate` → `MaterialCreateV2ServiceImpl.createMaterialTask` → `execCreateMaterialProcess`（:760-822）。

```bash
curl -X POST 'http://127.0.0.1:8080/api/construction/task/create' \
  -H 'Content-Type: application/json' \
  -d '{ /* 按接口参数文档传项目/订单维度数据 */ }'
```

> ⚠️ 具体业务入参结构未在本次调查中逐字段展开，如需逐字段核对请另行确认。

## 2. material/flow 系列配置查询接口

| 接口 | 方法 | 关键入参 | 行为要点 |
|---|---|---|---|
| `/material/flow/rule/query/list` | POST | **必须传 `mdmCompanyCodeList`**，否则走登录人权限返回空 | 分页上限 key=NO_LIMIT 时取 9999999 |
| `/material/flow/query/selected/material` | GET | 忽略入参 | 固定 mode=HOME2_5_MANPOWER |
| `/material/flow/query/selected/ofcNodeProcessTemplateList` | GET | 无参 | **Feign 查 OFC 模板列表**（非本地表） |
| `/material/flow/rule/info` | GET | `ruleId` 必传 | 不传静默返回 null |
| `/material/flow/category/query/list/bycondition` | GET | `ruleId` 必传 | 不传返回 null；内存分页默认 1/20 |

```bash
# 规则列表（必须带 mdmCompanyCodeList）
curl -X POST 'http://127.0.0.1:8080/material/flow/rule/query/list' \
  -H 'Content-Type: application/json' \
  -d '{"mdmCompanyCodeList":["<分公司code>"],"currentPage":1,"pageSize":10}'

# 模板下拉（OFC 远程）
curl 'http://127.0.0.1:8080/material/flow/query/selected/ofcNodeProcessTemplateList'

# 规则详情 / 品类列表
curl 'http://127.0.0.1:8080/material/flow/rule/info?ruleId=<ruleId>'
curl 'http://127.0.0.1:8080/material/flow/category/query/list/bycondition?ruleId=<ruleId>'

# 品类维度下拉
curl 'http://127.0.0.1:8080/material/flow/query/selected/material'
```

## 3. 模板查询接口

`POST /material-task/config/template-list`，入参 `TemplateListParam`：

```bash
curl -X POST 'http://127.0.0.1:8080/material-task/config/template-list' \
  -H 'Content-Type: application/json' \
  -d '{
    "stateList": ["1","4"],
    "mode": 1,
    "overrideByState": true,
    "pageSize": 1000000,
    "currentPage": 1,
    "dataTypeList": ["template_unit","process_define","node","task","audit_info"],
    "gbCode": "110000"
  }'
```

字段说明：

| 字段 | 类型/示例 | 说明 |
|---|---|---|
| `productComboId` | Long | ⚠️ 是 Long 类型，传 `""` 会 400；不传或传 null 均可 |
| `stateList` | `["1","4"]` | 模板状态过滤 |
| `mode` | 1 | 1=BW 北京被窝；mode 决定走哪条配置路线（见 03 篇） |
| `dataTypeList` | 数组 | 返回数据类型：template_unit/process_define/node/task/audit_info |
| `gbCode` | "110000" | 地区码 |
| `pageSize` | 默认 100000 | — |

无登录依赖。

## 4. 配置保存接口（写入侧，供对照）

| 接口 | 落表 | 说明 |
|---|---|---|
| `POST /material/flow/rule/save` | `material_flow_rule` + `material_flow_rule_unit` + Feign 同步 OFC `saveMainRule` | 主规则：勾选单据类型/订单版本/分公司/店铺/套餐 |
| `POST /material/flow/category/save` | `material_flow_rule_category`（每供应商一条，category_id=雪花id）+ OFC `saveSecondRule` | 品类规则：勾选品类+供应商+**下拉选模板**（node_process=模板id） |
