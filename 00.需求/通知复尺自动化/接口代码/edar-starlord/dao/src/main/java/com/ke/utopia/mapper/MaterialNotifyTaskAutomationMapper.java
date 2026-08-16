package com.ke.utopia.mapper;

import com.ke.utopia.common.mybatis.BaseMapper;
import com.ke.utopia.model.MaterialNotifyTaskAutomation;
import com.ke.utopia.model.MaterialNotifyTaskAutomationExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MaterialNotifyTaskAutomationMapper extends BaseMapper<MaterialNotifyTaskAutomation, MaterialNotifyTaskAutomationExample> {
    /**
     * 批量插入 注意: 需要设置默认值
     *
     * @param records
     */
    int batchInsert(List<MaterialNotifyTaskAutomation> records);
}