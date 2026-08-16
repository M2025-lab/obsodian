package com.ke.utopia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 通知任务识别结果入参（我方定义，直接交付上游调用方）
 *
 * <p>字段名与落库列一致（下划线命名），上游按我方字段直传。含义约定：
 * <ul>
 *   <li>{@code receive_time} 为 T 的时间基准，格式 yyyy-MM-dd；期望上门时间由我方按 T+3 计算。</li>
 *   <li>{@code judge_result} 取值参照 {@link com.ke.utopia.enumeration.NotifyJudgeResultEnum}：
 *   1-可通知，2-不可通知，3-无法判断，4-现场已完成，任务需关闭，上游按我方枚举值直传。</li>
 *   <li>{@code key_frame_urls} 由我方逗号拼接后入库。</li>
 * </ul>
 */
@ApiModel("通知任务识别结果入参")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTaskResultParam {

    @NotNull
    @JsonProperty("project_order_id")
    @ApiModelProperty(value = "项目订单id", example = "123456789012345678", required = true)
    private Long projectOrderId;

    @NotBlank
    @JsonProperty("receive_time")
    @ApiModelProperty(value = "识别日期，T的时间基准，格式 yyyy-MM-dd", example = "2026-08-10", required = true)
    private String receiveTime;

    @NotBlank
    @JsonProperty("material_code")
    @ApiModelProperty(value = "品类编码", example = "029001001", required = true)
    private String materialCode;

    @JsonProperty("material_name")
    @ApiModelProperty(value = "品类名字", example = "集成吊顶")
    private String materialName;

    @NotNull
    @JsonProperty("task_type")
    @ApiModelProperty(value = "任务类型", example = "9060", required = true)
    private Integer taskType;

    @JsonProperty("task_name")
    @ApiModelProperty(value = "任务类型名称", example = "通知复尺")
    private String taskName;

    @NotNull
    @JsonProperty("judge_result")
    @ApiModelProperty(value = "判断结果（我方枚举值）：1-可通知，2-不可通知，3-无法判断，4-现场已完成，任务需关闭", example = "1", required = true)
    private Integer judgeResult;

    @JsonProperty("judge_remark")
    @ApiModelProperty(value = "判断备注", example = "所有识别项均通过，满足通知条件")
    private String judgeRemark;

    @JsonProperty("key_frame_urls")
    @ApiModelProperty(value = "关键帧图片URL列表")
    private List<String> keyFrameUrls;
}