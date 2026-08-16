---
title: 外部系统清单
type: concept
tags: [external-systems, dependency, feign, integration]
---

# 外部系统清单

> edar-starlord（家装交付中台）依赖的外部系统一览。

## 外部系统依赖关系

```
edar-starlord
├── 消息通知
│   ├── ke-diting（谛听）          — 企微群消息推送
│   ├── wechat-message             — 微信个人消息
│   └── call-service（司南）        — 语音转文字
├── 供应链
│   └── scm-beiwo（北沃）          — 供应链配置查询、预分配送货单
├── 搜索服务
│   └── search-service (ES)        — 跟单任务/任务单 ES 索引查询
├── 客户域
│   ├── customer-home（客户主页）    — CRM 客户信息、整装订单映射
│   ├── big-c（大C）                — 城市小程序开城配置
│   └── cipher-feign（隐私加密）    — 手机号加密
├── 权限
│   └── permission-service         — 用户角色权限
├── 资金
│   └── utopia-nrs-sales-project（HOME资金） — 项目款项、存管信息
└── （预留）DeliveryPreAllocateFeign — 空接口，待扩展
```

## 系统详情

### 1. ke-diting（谛听）
- **FeignClient**: `DiTingFeignService` → `url=${ke-diting.url}`，`name=ke-diting`
- **用途**: 向企业微信群推送消息通知（延期告警、任务提醒等）
- **方法**: `sendWeChatGroupMessage(DiTingSendWeChatMessageParam)`  → `POST /tripartite/wxwork/group/send-message`

### 2. wechat-message（微信消息服务）
- **FeignClient**: `WechatMessageFeignService` → `url=${wechat.message.url}`，`name=wechat-message`
- **用途**: 发送个人微信消息通知
- **方法**: `sendWechatMessage(WechatMessageSendParam)` → `POST api/notice/send-message`

### 3. call-service（司南语音服务）
- **FeignClient**: `CallToTextNewFeignService` → `url=${sinan.lianjia.url}`，`name=call-service`
- **用途**: 通话录音语音转文字
- **条件**: `sinan.lianjia.enable=true` 时启用
- **方法**: `callToTextNew(callId, type)` → `GET /api/callRecord/callToTextServieImpl/callToTextNew`

### 4. scm-beiwo（供应链-北沃）
- **FeignClient 1**: `DeliveryPreAllocateFeign` → `url=${scm.beiwo.web.host}`，`name=scm-beiwo`（空接口）
- **FeignClient 2**: `OfcDispatchConfigQueryService` → `url=${scm.beiwo.web.host}`，`name=permission-service`
- **用途**: 获取供应链侧的分公司列表、预分配送货单查询、定制化返补单查询
- **方法**:
  - `getBranchcompanyList()` → `GET /api/ousuo/order/branchCompanyList`
  - `deliveryPreAllocateOrderList(param)` → `POST /api/delivery/pre-allocate/list`
  - `queryCustomizedThirdReplenishListByBrandOrderNo(brandOrderNo)` → `GET /api/order/replenish/customized/thirdSelectList/brandOrderNo`

### 5. search-service（ES搜索服务）
- **FeignClient 1**: `ESTraceTaskFeignService` → `url=${trace.task.search.api.url}`，`name=search-service`
- **用途**: 跟单任务 ES 全文搜索
- **方法**:
  - `searchTraceTaskFullMessage(SearchMessage, token)` → 全量信息搜索
  - `searchTraceTaskMessage(SearchMessage, token)` → 精简信息搜索
- **FeignClient 2**: `ESTaskOrderFeignService` → `url=${task.order.search.api.url}`，`name=search-service`
- **用途**: 任务单 ES 搜索
- **方法**: `searchTaskOrderFullMessage(SearchMessage, token)`

### 6. customer-home（客户主页服务 / CRM）
- **FeignClient**: `CustomerHomeFeign` → `url=${customer-home-host}`，`name=customer-home`
- **用途**: CRM 集成——获取城市强弱耦合配置、维护人信息、DFcode→整装订单号映射、服务者运营配置
- **方法**:
  - `getStrongCatenaCity(gbCode, emergency)` → 获取强弱耦合城市
  - `getMaintainUser(customerCommissionCode, emergency)` → 获取维护人详情
  - `getMaintainTaskUser(customerCommissionCode, maintainType)` → 获取任务成员
  - `getProjectOrderInfo(customerCommissionCodes)` → DFcode→整装订单号
  - `getFurnitureInfo(projectOrderIds)` → 整装单号→零售单号
  - `getOperationConfig(ucIds, roleType)` → 服务者运营配置（品类/接单上限）

### 7. big-c（大C服务）
- **FeignClient**: `BigCFeignService` → `url=${big-c.url}`，`name=big-c`
- **用途**: B端获取指定城市小程序开城信息
- **方法**: `getConfigXcxbrand(cityId)` → `GET ershou-decor-app-api/api/v1/config/xcxbrand`

### 8. cipher-feign（隐私加密服务）
- **FeignClient**: `CipherFeign` → `url=${privacy-data-cipher.url}`，`name=cipher-feign`
- **用途**: CRM 侧手机号加密
- **方法**: `cipher(phoneList)` → `POST /api/encrypt`

### 9. permission-service（权限服务）
- **FeignClient**: `PermissionFeignService` → `url=${platform.permission.host}`，`name=permission-service`
- **用途**: 获取用户角色权限
- **方法**: `getRoles(app, ucid)` → `GET /{app}/users/{ucid}/roles`

### 10. utopia-nrs-sales-project（HOME 资金服务）
- **FeignClient**: `HomePaymentApi` → `value=utopia-nrs-sales-project`（通过注册中心发现）
- **用途**: 查询项目款项信息、存管信息、通用节点状态
- **方法**:
  - `queryGeneralNode(projectOrderIds)` → 通用节点查询
  - `getProjectFund(projectOrderId)` → 项目款项信息
  - `getFundDetailInfo(fundId, fundType, projectOrderId)` → 款项详情
  - `getProjectEscrowBasicInfo(projectOrderId)` → 项目存管信息

### 11. OMS（订单管理系统）— 被回调方
- **方向**: OMS → starlord（外部调我们）
- **starlord 接口**: `OmsMessageSyncFeign` → POST `/installer-task/message-sync`
- **用途**: OMS 将订单状态变更同步给 starlord（任务进展、图片、延期原因、验收结果）

### 12. SDM（供应链配送管理）— 被回调方
- **方向**: SDM → starlord（外部调我们）
- **starlord 接口**: `SdmMessageSyncFeign` → POST `/starlord/sdm/status/sync`
- **用途**: SDM 将采购单/服务单状态变更同步给 starlord

---

## 注册中心发现的服务

| 服务名 | 说明 |
|---|---|
| `utopia-nrs-sales-project` | HOME 资金服务，通过注册中心（Eureka/Nacos）发现 |

## URL 直连的服务

| 配置项 | 对应系统 |
|---|---|
| `${big-c.url}` | big-c（大C服务） |
| `${ke-diting.url}` | ke-diting（谛听） |
| `${wechat.message.url}` | wechat-message |
| `${sinan.lianjia.url}` | call-service（司南） |
| `${scm.beiwo.web.host}` | scm-beiwo（供应链） |
| `${trace.task.search.api.url}` | ES 搜索服务（跟单） |
| `${task.order.search.api.url}` | ES 搜索服务（任务单） |
| `${privacy-data-cipher.url}` | cipher 加密服务 |
| `${customer-home-host}` | customer-home（CRM） |
| `${platform.permission.host}` | permission-service |
