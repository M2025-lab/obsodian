package com.ke.utopia.service;

import com.ke.utopia.dto.NotifyTaskResultParam;

public interface NotifyTaskAutomationService {

    /**
     * 接收上游识别结果并暂存
     *
     * @param param 上游识别结果
     */
    void receiveTaskResult(NotifyTaskResultParam param);

}