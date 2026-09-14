package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import org.apache.ibatis.annotations.Mapper;

/** 冷冻加工工序记录 Mapper */
@Mapper
public interface ProcessRecordMapper extends BaseMapper<ProcessRecord> {
}
