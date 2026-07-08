package com.ke.utopia.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author huiming.suo
 * @date 2022/02/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDispatchDetailSimpleDTO {
    @ApiModelProperty("taskDispatchId")
    private Long taskDispatchId;

    @ApiModelProperty("供应商名称")
    private String supplierName;

    @ApiModelProperty("供应商Code")
    private String supplierCode;

    @ApiModelProperty("主材品类")
    private String materialName;

    @ApiModelProperty("主材品类Code")
    private String materialCode;

    @ApiModelProperty("任务类型，5-送货任务")
    private Integer taskType;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("主材订单号")
    private String orderNo;

    @ApiModelProperty("服务单号")
    private String serviceOrderNo;

	@ApiModelProperty("采购服务单号")
	private String purchaseOrderNo;

    private Integer syncToSdm;

    @ApiModelProperty("主材任务创建来源，默认0-供应链订单创建，1-测量申请单创建")
    private Integer sourceType;

    @ApiModelProperty(value = "服务模式", example = "1")
    private Integer serviceMode;

    @ApiModelProperty("任务状态：1-未激活 2-激活未完成 3-已完成")
    private Integer processStatus;

    @ApiModelProperty(value = "订单id", example = "41762866697412608")
    private Long projectOrderId;

    @ApiModelProperty(value = "订单id", example = "41762866697412608")
    private String projectOrderIdStr;

    @ApiModelProperty(value = "地址", example = "projectAddress")
    private String projectAddress;

    @ApiModelProperty("任务节点")
    private List<TaskNodeDetail> taskNodeDetailList;

    private Long templateId;
    private String templateIdStr;
    private String gbCode;
    private Integer processBatch;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date planActivateTime;
    private String nodeTask;
    private Integer currentNodeType;
    private Byte state;
    private String modifyBy;
    private String modifyName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gmtCreate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gmtModified;
    private String processCode;
    private String conditionCode;
    private String businessId;

    private Integer mode;

    @ApiModelProperty("项目经理id")
    private Long foremanId;
    @ApiModelProperty("项目经理名称")
    private String foremanName;
    @ApiModelProperty("项目经理手机号")
    private String foremanPhone;
    private String mdmCode;
    private String mdmCompanyName;
    @ApiModelProperty(value = "分公司code")
    private String branchOfficeCode;
    @ApiModelProperty(value = "分公司name")
    private String branchOfficeName;

    @ApiModelProperty(value = "业务类型,0:正单,1:返补")
    private Integer flowType;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaskNodeDetail {
        @ApiModelProperty("任务ID")
        private Long taskDispatchId;

        @ApiModelProperty("节点id")
        private Long taskDispatchNodeId;

        @ApiModelProperty("任务结点名称")
        private String nodeName;

        @ApiModelProperty("任务结点类型")
        private Integer nodeType;

        @ApiModelProperty("责任人名称")
        private String executorName;

        @ApiModelProperty("责任人ID")
        private String executorId;

        @ApiModelProperty("执行角色类型")
        private Integer executorType;

        @ApiModelProperty("执行角色类型名称")
        private String executorTypeName;

        @ApiModelProperty("执行角色类型列表")
        private List<Integer> executorTypeList;

        @ApiModelProperty("预估完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date estimatedTime;

        @ApiModelProperty("实际完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date endTime;

        @ApiModelProperty("对客承诺时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date promiseTime;

        @ApiModelProperty("可上门时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date noticeRetainTime;

        @ApiModelProperty("审核结果：1-合格 2-不合格")
        private Integer qualified;

        @ApiModelProperty("提交人ID")
        private String submitBy;

        @ApiModelProperty("提交人姓名")
        private String submitName;

        @ApiModelProperty("提交时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date submitTime;

        @ApiModelProperty("进展状态: 1-未激活 2-激活未完成 3-激活已完成")
        private Integer processStatus;

        @ApiModelProperty("进展状态")
        private String processStatusName;

        @ApiModelProperty("重启次数")
        private Integer restartCount;

        @ApiModelProperty("执行角色,人员系统的枚举，WorkTypeEnum的code")
        private List<String> workTypeList;

        private String packageCode;
        private String remarks;
        private List<String> images;
        @ApiModelProperty("附件list")
        private List<Map<String, String>> attachmentList;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date platformCheckTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date startTime;
        private Byte state;
        private String modifyBy;
        private String modifyName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date gmtCreate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date gmtModified;
        private String processCode;
        private Integer nodeCode;
        @ApiModelProperty("执行人手机号 需根据业务自行取")
        private String executorPhone;
        @ApiModelProperty("无法复尺的原因的筛选项")
        private String remarksFilter;

        @ApiModelProperty("零售业务 定制设计师id")
        private Long woodDesignerUcid;
        @ApiModelProperty("零售业务 定制设计师名称")
        private String woodDesignerName;
        @ApiModelProperty("零售业务 定制设计师手机号")
        private String woodDesignerPhone;

    }
}
