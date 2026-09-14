package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;

import java.util.List;

/** 冷冻加工工序记录业务接口（清洗 / 分级 / 冷冻 / 包装） */
public interface ProcessRecordService extends IService<ProcessRecord> {

    /** 查询指定加工批号的工序记录 */
    List<ProcessRecord> listByBatchId(Integer frozBatchId);
}
