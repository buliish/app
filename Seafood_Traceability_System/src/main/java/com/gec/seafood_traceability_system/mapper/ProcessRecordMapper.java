package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 冷冻加工工序记录 Mapper
 * <p>
 * 批量插入见 ProcessRecordMapper.xml —— 一条批号常要一次录入
 * 清洗/分级/冷冻/包装整套工序，foreach 拼多值 INSERT 比循环单条省往返。
 */
@Mapper
public interface ProcessRecordMapper extends BaseMapper<ProcessRecord> {

    /**
     * 批量新增工序记录（数据层批量操作）
     *
     * @param records 非空列表，由调用方保证
     * @return 影响行数
     */
    int insertBatch(@Param("records") List<ProcessRecord> records);
}
