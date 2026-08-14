package com.ke.utopia.api;

import com.ke.utopia.common.dto.ResultDTO;
import com.ke.utopia.dto.NotifyTaskResultParam;
import io.swagger.annotations.Api;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@Api(tags = "通知任务识别结果接收")
@FeignClient(value = "edar-starlord", contextId = "notify-task-automation-feign")
public interface NotifyTaskAutomationFeign {

    /**
     * 接收调度层识别结果并暂存
     */
    @PostMapping("/api/deduct/task/result")
    ResultDTO<Void> receiveTaskResult(@Valid @RequestBody NotifyTaskResultParam param);

}
