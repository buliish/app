package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.InspectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检测记录 Mapper
 * <p>
 * 批量插入见 InspectionMapper.xml —— 一份检测报告的多个检测项一次提交，
 * foreach 拼多值 INSERT 比循环单条省往返。
 */
@Mapper
public interface InspectionMapper extends BaseMapper<InspectionRecord> {

    /**
     * 批量新增检测记录
     *
     * @param records 非空列表，由调用方保证
     * @return 影响行数
     */
    int insertBatch(@Param("records") List<InspectionRecord> records);
}
