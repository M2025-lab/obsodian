# 安装拆除经营主体用工改造 — 开发总结（Cube 侧）

## 一、最终开发了什么（一句话）

在 **utopia-cube 施工包系统**中，把安装/拆除相关用工链路的「管理者」识别逻辑从**组织树上级**切换为**服务者中心（Ceres）的长期合作关系**；并新增派单「二级确认」状态（待上级确认 / 待工人确认），贯穿派单通知、列表查询、进展、审批与结算等多个环节。

## 二、涉及的内容 / 功能模块

1. **派单待确认列表**：工地页新增二级 tab「派单待确认」，支持「待上级确认 / 待工人确认 」状态。
2. **派单二级确认**：安装/拆除/橱柜派单由「待接单」拆为「待上级确认(2001)」「待工人确认(2002)」，拒单可重派。
3. **上级查询改造**：约工驳回、费用单提报改为查「长期合作项目经理」。
4. **问题费用审批**：一级审批=安装项目经理；二级审批=COE；费用超额查 COE 直属上级。
5. **安装/拆除项目经理存储→结算**：cube 需落库长期合作项目经理 id/name。
6. **施工包进展**：在作业中心展示「待上级确认/待工人确认」及操作人/原因 --「派单平台」。
7. **施工包卡片工程量确认标签**：需要展示是否确认工程量，点击可跳转工程量（依赖第三方接口，8/8 交付，未就绪）。
8. **班组归属体外项目经理字段**：派单/改派/回退时覆写 `new_package_member`。
9. **开城开关配置**：按 gb_code + 工种 code 走 Apollo 开关。
10. **存量刷数 / 定时任务**（方案待定，todo）。

## 三、涉及的 Cube 内部接口（已实现）

| 接口                                                                                                                 | 位置                                                                                      | 作用                                                       |
| ------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `POST /utopia-cube/web/list-package-by-operator-manager`                                                           | `PcController#listPackageConstructionByManager`                                         | 经理视角施工包列表，新增加 type=5 派单确认筛选、返回 `packageDispatchConfirm`  |
| `POST /package/changeNotice/updateDB`（dispatchChangeNotice）                                                        | `PackageConstructionController#dispatchChangeNotice` → `PcService#dispatchChangeNotice` | 接收派单平台状态变更通知，落库 `package_second_status` 与 `package_time` |
| `com.ke.utopia.cube.api.process.ProcessControlFeign#processControlAfter` + `ProcessController#processControlAfter` | `ProcessControlServiceImpl`                                                             | 接单/改派/回退时携带并覆写/清除长期合作项目经理                                |
| `POST /utopia-cube/web/list-package-by-operator/complete`、`/web/list-package-by-operator`（列表/详情）                   | —                                                                                       | 工程量确认标签展示范围                                              |
| `POST utopia-cube /construction/queryList`                                                                         | 结算侧拉取                                                                                   | 返回 `longTermPmId/longTermPmName`                         |
| Kafka Topic `utopia-cube-package`（bizType=package-construction-status-change / package-second-status-change）       | —                                                                                       | 状态变更消息推送结算                                               |

## 四、与上下游交互的外部接口

**下游 / 调用方 → Cube：**

- **派单平台 (minerva)**：`dispatchChangeNotice` 通知（依赖 `utopia-minerva-api 1.8.2-ljx-SNAPSHOT`），及 `utopia-minerva-dispatch` / `utopia-minerva-reassign` 消息。
- **结算系统**：消费 `utopia-cube-package` 消息 + 调 `/construction/queryList`。
- **作业中心**：接收派单确认状态（进展展示）--派单平台通知作业中心。
- **消息/Push、Apollo（开关）、工程量确认服务（陶思宇，8/8）**。

**Cube → 上游 服务者中心 (Ceres)：**

- `PersonHighServiceApi#querySuperiorByWorkType` —— 约工驳回查长期合作项目经理（逻辑复用，无需改）。
- `PersonHighServiceApi#queryBusinessSuperior` —— 费用超额审批查 COE 及上级（`CeresManager#queryBusinessSuperior`）。
- `AttachInfoApi#listAttachInfo` —— 安装/拆除工人查上级安装拆除项目经理（`AttachInfoManager#listAttachInfo`，约工驳回提交处调用）。

## 五、开发进展（基于 git 提交 + 代码落地）

**核心功能已基本落地，处于联调/待评审阶段：**

- ✅ 派单确认状态枚举：`DispatchConfirmStatusEnum`(2000/2001/2002)、`DispatchConfirmStatusMapping`(-1/1/0→统一编码)。
- ✅ 派单变更通知：`DispatchChangeNoticeParam` + `PackageConstructionController#dispatchChangeNotice` + `PcServiceImpl#dispatchChangeNotice`（状态映射、校验、落库）。
- ✅ 列表查询：`PackageListParam` 新增 `type=5 dispatchConfirmStatusList`；`WorkerServiceImpl` 返回 `packageDispatchConfirm` 及确认时间。
- ✅ 长期合作项目经理：`NewPackageMember` 表新增 `long_term_pm_id/long_term_pm_name`；`DispatchBO`/`MinervalDispatchParam`/`ProcessControlParam`/`PackageConstructionDto`/`PackageConstructionInfoMessageDTO` 均已携带；接单/改派写入、回退清除。
- ✅ 结算消息：`/construction/queryList` 新增长期合作项目经理字段（`7f4861ca1`/`f8f645c4b`）。
- ✅ 约工驳回：逻辑改造（`aa966aec7`/`3bfcd91f9`）。
- ✅ 费用单二级审批逻辑修改（`25b07a7b6`/`4c84893c9`）；开城判断（`06289db39`）；派单时间字段（`bd155ef48`）。

**未完成 / 阻塞（文档标 todo）：**

- ⏳ 3.2.7 工程量确认标签（依赖陶思宇接口 8/8 交付）。
- ⏳ 3.2.9 生成施工包流程改造（产品方案未确认 @姜淇）。
- ⏳ 3.2.10 定时刷「拆建模式」任务（逻辑未梳理清楚待定）。

## 六、功能做了什么修改（相对原逻辑）

1. **管理者识别逻辑重构**：从「组织树上级」→「服务者中心长期合作关系」，约工驳回/审批查询点语义调整。
2. **派单状态细化**：复用 `PackageSecondStatusEnum`(施工中二级状态)，在 `packageStatus=200 待派单` 时承载派单确认状态，**不新增 ES 字段**，仅复用 `packageSecondStatus` / 新增 VO 字段 `packageDispatchConfirm`。
3. **列表入参扩展**：`extraFieldTypeInfoList` 在原有 type 1/2/3/4 基础上**新增 type=5**（派单确认状态），筛选项组装到 ES 查询与聚合。
4. **落库字段新增**：`new_package_member` 增加 `long_term_pm_id/long_term_pm_name`，接单/改派覆写、回退清除。
5. **消息/接口双向携带**：派单确认状态来自 `DispatchChangeNotice`，长期合作项目经理来自 `processControlAfter` Feign 与 minerva 消息，来源不同但均收敛到同一存储。
6. **审批链路调整**：问题单一级审批人=安装项目经理（结算侧）；超额改走 `queryBusinessSuperior` 查 COE 上级。

## 七、风险点（文档列示）

- 服务者中心长期合作关系数据质量「注意接口是否按预期返回，由于角色新增和组织树调整可能不按预期返回」（缺失/过期/错误）→ 开城前存数据核对 + 关系准确性监控。
- 开关切换瞬间数据一致性 → 低峰期切换、存量刷数完成、保留回滚与脚本。
- 上下游接口变更同步 → 正式邮件通知 + 联调对齐版本。
- 第三方工程量接口(8/8)未就绪 → Mock 联调 + 标签降级方案。
- 产品方案未确认（生成施工包流程）→ 作为上线前置依赖跟踪。
