package com.ke.utopia.model;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description material_notify_task_automation 主材通知任务自动化记录表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialNotifyTaskAutomation implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 主材接收上游识别结果日期，T的时间基准
     */
    private Date receiveTime;

    /**
     * 项目订单id
     */
    private Long projectOrderId;

    /**
     * 品类编码
     */
    private String materialCode;

    /**
     * 品类名字
     */
    private String materialName;

    /**
     * 任务类型
     */
    private Integer taskType;

    /**
     * 任务类型名称
     */
    private String taskName;

    /**
     * 通知任务状态：1-未完成通知任务，2-已完成通知任务
     */
    private Integer notifyStatus;

    /**
     * 最新系统判定结果：1-任务可通知，2-任务不可通知，3-无法识别是否可通知，4-现场已完成任务需关闭
     */
    private Integer judgeResult;

    /**
     * 判断备注
     */
    private String judgeRemark;

    /**
     * 关键帧图片URL，逗号分隔
     */
    private String keyFrameUrls;

    /**
     * 最新判定期望上门时间，T+3
     */
    private Date recommendVisitTime;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    private static final long serialVersionUID = 1L;
}