package com.ke.utopia.enumeration;

import lombok.Getter;

/**
 * 通知任务判断结果枚举（我方定义，同步给上游；上游按此值直接传入）
 */
@Getter
public enum NotifyJudgeResultEnum {

    CAN_NOTIFY(1, "可通知"),

    CANNOT_NOTIFY(2, "不可通知"),

    CANNOT_JUDGE(3, "无法判断"),

    SITE_COMPLETED(4, "现场已完成，任务需关闭");

    private final Integer value;
    private final String desc;

    NotifyJudgeResultEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}