package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.ProcessRecordMapper;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import com.gec.seafood_traceability_system.service.ProcessRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 冷冻加工工序记录业务实现 */
@Service
public class ProcessRecordServiceImpl extends ServiceImpl<ProcessRecordMapper, ProcessRecord> implements ProcessRecordService {

    @Override
    public List<ProcessRecord> listByBatchId(Integer frozBatchId) {
        return lambdaQuery()
                .eq(ProcessRecord::getFrozBatchId, frozBatchId)
                .orderByAsc(ProcessRecord::getRecordId)
                .list();
    }
}
