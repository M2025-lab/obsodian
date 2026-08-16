package com.ke.utopia.dao;

import com.ke.utopia.model.MaterialNotifyTaskAutomation;

public interface MaterialNotifyTaskAutomationDao {

    Long insertSelective(MaterialNotifyTaskAutomation record);

    MaterialNotifyTaskAutomation selectById(Long id);

    void updateById(MaterialNotifyTaskAutomation record);

}