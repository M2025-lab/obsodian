# 测量复尺模块

**接口数**: 32 | **类数**: 8

---

## MeasureApplyFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MeasureApplyFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/designer/measure-apply/detail` | description:测量申请单相关接口 @author xiaozhenzhen001 @date 2021/9/2 |
| 2 | POST | `/api/designer/measure-apply/autoCommitMeasureApply` | description:测量申请单相关接口 @author xiaozhenzhen001 @date 2021/9/2 |

## MeasureApplyRangeConfigFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MeasureApplyRangeConfigFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/api/designer/measure-apply-range-config/modify` | description:测量范围配置信息相关接口 @author xiaozhenzhen001 @date 2021/10/18 |
| 2 | POST | `/api/designer/measure-apply-range-config/delete` | description:测量范围配置信息相关接口 @author xiaozhenzhen001 @date 2021/10/18 |
| 3 | POST | `/api/designer/measure-apply-range-config/list` | description:测量范围配置信息相关接口 @author xiaozhenzhen001 @date 2021/10/18 |
| 4 | POST | `/api/designer/measure-apply-range-config/listAll` | description:测量范围配置信息相关接口 @author xiaozhenzhen001 @date 2021/10/18 |

## MeasureConfigRuleExportController

`edar-starlord-web/src/main/java/com/ke/utopia/web/MeasureConfigRuleExportController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/material-measure/interface/config/export-file` | 交界面规则文件导出单独使用 |

## MeasureConfigRuleFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MeasureConfigRuleFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/material-measure/interface/config/info` |  |
| 2 | POST | `/material-measure/interface/config/check-save` |  |
| 3 | POST | `/material-measure/interface/config/save` |  |
| 4 | POST | `/material-measure/interface/config/update` |  |
| 5 | POST | `/material-measure/interface/config/delete` |  |
| 6 | POST | `/material-measure/interface/config/export` |  |
| 7 | POST | `/material-measure/interface/config/query/requirement` |  |
| 8 | GET | `/material-measure/interface/config/node-options` |  |

## MeasureFormFixController

`edar-starlord-web/src/main/java/com/ke/utopia/web/fix/MeasureFormFixController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/measure/form/sendCadPhoto` |  |

## MeasureFormTemplateConfigurationFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MeasureFormTemplateConfigurationFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/measure/form-template/configuration/list/usedAttrItem` |  |
| 2 | POST | `/measure/form-template/configuration/information` |  |
| 3 | POST | `/measure/form-template/configuration/queryPage` |  |
| 4 | GET | `/measure/form-template/configuration/queryById` |  |
| 5 | POST | `/measure/form-template/configuration/submit` |  |
| 6 | POST | `/measure/form-template/configuration/update` |  |
| 7 | GET | `/measure/form-template/configuration/category/enum` |  |
| 8 | POST | `/measure/form-template/import` |  |

## MeasureFormTemplateFeign

`edar-starlord-api/src/main/java/com/ke/utopia/api/MeasureFormTemplateFeign.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | POST | `/measure/form/get` |  |
| 2 | POST | `/measure/form/get-v2` |  |
| 3 | POST | `/measure/form/update` |  |

## StarlordController

`edar-starlord-web/src/main/java/com/ke/utopia/web/butler/StarlordController.java`

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/measure/query-appointment-info` | 获取主材任务列表 @param param @return |
| 2 | POST | `/measure/batch-submit` |  |
| 3 | POST | `/measure/batch-change-appoint` |  |
| 4 | POST | `/design-review/submit` |  |
| 5 | GET | `/design-review/enable-ignore-alter` |  |
