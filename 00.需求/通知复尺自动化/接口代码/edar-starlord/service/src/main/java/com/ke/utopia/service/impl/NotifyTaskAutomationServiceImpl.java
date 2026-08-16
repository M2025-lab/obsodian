package com.ke.utopia.service.impl;

import com.ke.utopia.common.enumeration.ResultCodeEnum;
import com.ke.utopia.common.exception.UtopiaBussinessException;
import com.ke.utopia.dao.MaterialNotifyTaskAutomationDao;
import com.ke.utopia.dto.NotifyTaskResultParam;
import com.ke.utopia.model.MaterialNotifyTaskAutomation;
import com.ke.utopia.service.NotifyTaskAutomationService;
import com.ke.utopia.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

@Service
@Slf4j
public class NotifyTaskAutomationServiceImpl implements NotifyTaskAutomationService {

    @Resource
    private MaterialNotifyTaskAutomationDao materialNotifyTaskAutomationDao;

    @Override
    public void receiveTaskResult(NotifyTaskResultParam param) {
        if (param == null) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_PARAM_ILLEGAL, "参数不能为null");
        }

        Date receiveTime = DateUtil.parse(param.getReceiveTime(), DateUtil.DEFAULT_DAY_FORMAT);
        if (receiveTime == null) {
            throw new UtopiaBussinessException(ResultCodeEnum.ERROR_PARAM_ILLEGAL, "receive_time格式不合法,需为yyyy-MM-dd");
        }

        MaterialNotifyTaskAutomation record = MaterialNotifyTaskAutomation.builder()
                .receiveTime(receiveTime)
                .projectOrderId(param.getProjectOrderId())
                .materialCode(param.getMaterialCode())
                .materialName(param.getMaterialName())
                .taskType(param.getTaskType())
                .taskName(param.getTaskName())
                .notifyStatus(1)
                .judgeResult(param.getJudgeResult())
                .judgeRemark(param.getJudgeRemark())
                .keyFrameUrls(buildKeyFrameUrls(param))
                .recommendVisitTime(addDays(receiveTime, 3))
                .build();

        materialNotifyTaskAutomationDao.insertSelective(record);
        log.info("receiveTaskResult save success, record={}", record);
    }

    /**
     * keyFrameUrls 逗号分隔拼接
     */
    private String buildKeyFrameUrls(NotifyTaskResultParam param) {
        if (CollectionUtils.isEmpty(param.getKeyFrameUrls())) {
            return "";
        }
        return String.join(",", param.getKeyFrameUrls());
    }

    /**
     * T + N 天
     */
    private Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}