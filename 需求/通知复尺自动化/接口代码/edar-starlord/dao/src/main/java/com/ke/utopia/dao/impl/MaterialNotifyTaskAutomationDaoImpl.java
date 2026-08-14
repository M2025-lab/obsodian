package com.ke.utopia.dao.impl;

import com.ke.utopia.dao.MaterialNotifyTaskAutomationDao;
import com.ke.utopia.mapper.MaterialNotifyTaskAutomationMapper;
import com.ke.utopia.model.MaterialNotifyTaskAutomation;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class MaterialNotifyTaskAutomationDaoImpl implements MaterialNotifyTaskAutomationDao {

    @Resource
    private MaterialNotifyTaskAutomationMapper materialNotifyTaskAutomationMapper;

    @Override
    public Long insertSelective(MaterialNotifyTaskAutomation record) {
        materialNotifyTaskAutomationMapper.insertSelective(record);
        return record.getId();
    }

    @Override
    public MaterialNotifyTaskAutomation selectById(Long id) {
        return materialNotifyTaskAutomationMapper.selectByPrimaryKey(id);
    }

    @Override
    public void updateById(MaterialNotifyTaskAutomation record) {
        materialNotifyTaskAutomationMapper.updateByPrimaryKeySelective(record);
    }
}