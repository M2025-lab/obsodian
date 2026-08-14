package com.ke.utopia.web;

import com.ke.utopia.api.NotifyTaskAutomationFeign;
import com.ke.utopia.common.dto.ResultDTO;
import com.ke.utopia.dto.NotifyTaskResultParam;
import com.ke.utopia.service.NotifyTaskAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class NotifyTaskAutomationController implements NotifyTaskAutomationFeign {

    private final Logger logger = LoggerFactory.getLogger(NotifyTaskAutomationController.class);

    @Resource
    private NotifyTaskAutomationService notifyTaskAutomationService;

    @Override
    public ResultDTO<Void> receiveTaskResult(NotifyTaskResultParam param) {
        notifyTaskAutomationService.receiveTaskResult(param);
        return new ResultDTO<>().success();
    }

}