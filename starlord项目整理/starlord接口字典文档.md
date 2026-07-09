### 全量接口整理（按业务模块分类）

---

| 方法   | 接口路径                                                           | 功能说明                | 对应方法                                            |
| :--- | :------------------------------------------------------------- | :------------------ | :---------------------------------------------- |
| GET  | `/`                                                            | 欢迎接口                | WelcomeController#welcome                       |
| GET  | `/工厂子单校验`                                                      | 工厂子单校验              | checkReplenishOrder                             |
| POST | `/返补跟单任务下单`                                                    | 返补跟单任务下单            | placeOrder                                      |
| POST | `/返补跟单任务计划提货时间`                                                | 返补跟单任务计划提货时间        | setPlanPickupTime                               |
| GET  | `/查询是否存在返补的跟单任务`                                               | 查询是否存在返补的跟单任务       | checkQueryCoordinatorTask                       |
| POST | `/`                                                            | 查询是否需要图纸配置          | queryNeedDrawingConfig                          |
| GET  | `/返补订单-查询工厂主单`                                                 | 返补订单-查询工厂主单         | queryBrandOrderInfoForOrder                     |
| GET  | `/api/order/pre-allocate/list`                                 | 订单预分配列表查询           | deliveryPreAllocateOrderList                    |
| GET  | `/api/order/replenish/customized/thirdSelectList/brandOrderNo` | 根据品牌单号查询可关联的三方补货单列表 | queryCustomizedThirdReplenishListByBrandOrderNo |
| ANY  | `/api/trace/delivery-info/query`                               | 发货情况查询              | queryDeliveryInfo                               |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| GET | `/api/common-module/get-material-delivery-module` | 预约材料模块数据 | getMaterialDeliveryInfo |
| GET | `/api/common-module/get-measure-module` | 测量任务列表模块数据 | getMaterialMeasureInfo |
| GET | `/api/common-module/get-order-module` | 下单任务模块数据 | getOrderInfo |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| GET | `/api/material-measure/get-measure` | 获取类目下所有测量复尺空间属性模板 | getMeasure |
| GET | `/api/material-measure/get-task` | 根据订单号+类目查询测量复尺空间属性模板 | getMaterialMeasure |
| POST | `/api/material-measure/get-task-measure` | 获取测量复尺数据 | getMaterialMeasureInfo |
| POST | `/api/material-measure/get-task-measure/last-measure` | 复尺任务查看历史测量数据 | getLastMeasureInfo |
| POST | `/api/material-measure/get-task-measure/last-measure-v2` | 复用上次的测量数据 | getLastMeasureInfoV2 |
| GET | `/api/material-measure/has-measure-authority` | 判断当前用户是否有测量修改权限 | hasMaterialMeasure |
| GET | `/api/material-measure/has-order-receive` | 判断供应链是否已接单 | hasOrderReceive |
| GET | `/api/material-measure/query-measure` | 测量、复尺列表查询 | queryMaterialMeasure |
| POST | `/api/material-measure/save-task` | 保存测量复尺数据 | saveMaterialMeasure |
| GET | `/api/material-measure/task/list-recent-material-ruler-measure-recheck` | 查询留尺、测量、复尺任务列表 | listRecentMeasureMaterialRecheck |
| GET | `/api/material-measure/task/queryMeasureListGrade` | 项目详情页复尺数据分级展示 | queryMeasureListGrade |
| GET | `/api/material-measure/task/queryRecheckScaleTaskListByUcid` | 查询某人的所有复尺任务列表 | queryRecheckScaleTaskListByUcid |
| GET | `/api/material-measure/task/queryTaskRemarkInfo` | 通知复尺任务详情+驳回原因 | queryTaskRemarkInfo |
| POST | `/api/material-measure/update-task-measure` | 修改测量复尺数据 | updateMaterialMeasureInfo |
| GET | `/api/material-measure/v2/get-task` | V2版本：查询测量复尺空间属性模板 | getMaterialMeasureV2 |
| GET | `/api/material-measure/v3/get-task-resized` | V3版本：查询测量复尺完成后属性模板 | getMaterialMeasureV3FromResized |

---


| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| POST | `/api/material-task/` | 获取任务详情 | taskDispatchDetail |
| GET | `/api/material-task/` | 获取订单下所有主材任务进展 | materialProgress |
| POST | `/api/material-task/` | 获取主材所有任务及详情 | materialTaskDetail |
| POST | `/api/material-task/` | 测量/复尺驳回详情（后端聚合） | measureRejectDetail |
| POST | `/api/material-task/` | 通知安装节点上门时间限制 | installTimeLimit |
| POST | `/api/material-task/dispatch/batch-handle` | 合并主材任务批量处理 | batchHandle |
| POST | `/api/material-task/dispatch/batch-handle/package-construction-handle` | 施工包维度主材兜底提交 | packageConstructionHandle |
| POST | `/api/material-task/dispatch/common/change-appoint` | 主材任务改约 | changeAppoint |
| GET | `/api/material-task/dispatch/common/change-appoint-list` | 主材任务改约记录 | changeAppointList |
| POST | `/api/material-task/dispatch/handle` | 主材任务处理 | handle |
| POST | `/api/material-task/dispatch/material-task-order-detail` | 主材订单维度详情 | materialTaskOrderDetail |
| GET | `/api/material-task/installer-task/combine-install-task` | 合并任务列表 | combineInstallTask |
| POST | `/api/material-task/installer-task/complete-all` | 一键完成进场任务 | completeAll |
| GET | `/api/material-task/installer-task/detail` | 获取任务详情 | taskDetail |
| GET | `/api/material-task/installer-task/install-type-amount` | 获取任务类型分类数量 | installTypeAmount |
| GET | `/api/material-task/installer-task/list-install-task` | 获取安装/进场任务列表 | listTaskByType |
| GET | `/api/material-task/installer-task/list-task` | 获取任务列表 | listTask |
| POST | `/api/material-task/installer-task/pass-all` | 一键合格 | passAll |
| GET | `/api/material-task/installer-task/status-amount` | 获取任务完成状态统计 | statusAmount |
| POST | `/api/material-task/installer-task/subordinate-task` | 获取安装组长下属任务 | subordinateTask |
| POST | `/api/material-task/material-delivery/batch-task/list` | 批次任务列表查询 | listBatchTask |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| POST | `/api/material/self-check/acceptance-fail` | 主材任务自检报告驳回 | selfCheckAcceptanceFail |
| POST | `/api/material/self-check/acceptance-pass` | 主材任务自检报告通过 | selfCheckAcceptancePass |
| GET | `/api/material/self-check/acceptance-query` | 主材任务自检报告查询 | selfCheckAcceptanceQuery |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| ANY | `/api/task-dispatch/refresh-create` | 任务调度数据刷新重建 | RefreshDataController#refreshTaskDispatch |
| POST | `/api/v1/xcs/delay-reason-recovery` | 延期原因回收恢复 | XcsController#delayReasonRecovery |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| GET | `/api/test/construction/withoutVirtual` | 无虚拟节点施工测试 | TestController#getConstructionWithoutVirtual |
| GET | `/api/test/construction/withVirtual` | 带虚拟节点施工测试 | TestController#getConstructionWithVirtual |
| GET | `/audit` | 启动审计 | RefreshDataController#startAudit |
| POST | `/audit/supplier-list` | 查询审计供应商列表 | AuditController#getAuditSupplierList |

---

| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| GET | `/backdoor/activateTaskDispatchByTaskId` | 按任务ID激活任务调度 | activateTaskDispatchByTaskId |
| GET | `/backdoor/activateTaskDispatchDirect` | 直接激活任务调度 | activateTaskDispatchDirect |
| POST | `/backdoor/athena/search` | 中控测试 | searchAthena |
| POST | `/backdoor/autoCommitMeasureApply` | 自动提交测量申请单 | autoCommitMeasureApply |
| GET | `/backdoor/batchCancelMaterialOrderTask` | 批量取消主材订单任务 | batchCancelMaterialOrderTask |
| POST | `/backdoor/bindPackage` | 绑定施工包 | bindPackage |
| ANY | `/backdoor/cache/claer` | 清除缓存 | clearCache |
| GET | `/backdoor/completeNode` | 完成节点（间接） | completeNodeDirect |
| GET | `/backdoor/completeNodeDirect` | 直接完成节点 | completeNodeDirect |
| POST | `/backdoor/config/search` | 查询项目配置 | searchProject |
| POST | `/backdoor/deleteTask` | 删除任务 | deleteTasks |
| GET | `/backdoor/diff` | 配置对比 | diff |
| GET | `/backdoor/fullCalculate` | 全量时间计算 | fullCalculate |
| GET | `/backdoor/instanceBind` | 实例绑定 | instanceBind |
| POST | `/backdoor/mqmsg` | 发送MQ消息 | mqmsg |
| GET | `/backdoor/opnode` | 新增节点实例 | addNodeInstance |
| GET | `/backdoor/order/executor/fill` | 按订单补充执行人员 | fillExecutorByProject |
| GET | `/backdoor/order/taskDispatch/delete` | 删除任务调度 | refreshTask |
| POST | `/backdoor/order/taskDispatch/update` | 更新任务调度 | refreshTask |
| GET | `/backdoor/order/taskDispatchNode/delete` | 删除任务节点 | refreshTaskNode |
| POST | `/backdoor/order/taskDispatchNode/resend` | 重发并更新任务节点 | resendAndUpdate |
| POST | `/backdoor/order/taskDispatchNode/update` | 更新任务节点 | refreshTaskNode |
| POST | `/backdoor/project/search` | 查询项目 | searchProject |
| GET | `/backdoor/redis/exist` | 检查Redis缓存是否存在 | exists |
| GET | `/backdoor/redis/remove` | 清除Redis缓存 | remove |
| GET | `/backdoor/refreshMeasureApplySupplier` | 刷新测量申请供应商 | refreshSupplier |
| GET | `/backdoor/refreshSupplier` | 刷新供应商数据 | refreshSupplier |
| GET | `/backdoor/scheduleProcessor` | 调度处理器执行 | scheduleProcessor |
| GET | `/backdoor/sendSdmByTaskId` | 按任务ID发送SDM消息 | sendSdmByTaskId |
| POST | `/backdoor/sinvoke` | 服务内部调用 | sinvoke |
| POST | `/backdoor/startTask` | 启动任务 | startTasks |
| POST | `/backdoor/updateAssessmentTime` | 更新考核时间 | updateAssessmentTime |

---



| 方法 | 接口路径 | 功能说明 | 对应方法 |
| :--- | :--- | :--- | :--- |
| GET | `/batch/material/detail` | 批量材料详情查询 | queryBatchMaterialDetail |
| POST | `/batch/material/expect_time/query` | 查询配送预计时间 | queryDeliveryExpectedTime |
| GET | `/batch/material/info` | 批量材料基础信息 | queryBatchMaterialInfo |
| POST | `/batch/material/list` | 批量材料列表查询 | queryBatchMaterialList |
| GET | `/batch/material/list/need_arrival_notify` | 查询需到货通知的批量材料 | queryBatchMaterialListNeedMaterialArrivalNotify |
| POST | `/batch/material/submit` | 批量材料提交 | batchMaterialSubmit |
| POST | `/batch/material/update` | 批量材料更新 | batchMaterialUpdate |