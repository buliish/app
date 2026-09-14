package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.RetaBatchMapper;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 零售商产品批号业务实现
 * 状态：1 新建 2 待确认 3 已确认 4 已下架
 */
@Service
public class RetaBatchServiceImpl extends ServiceImpl<RetaBatchMapper, RetaBatch> implements RetaBatchService {

    @Override
    public List<RetaBatch> listByNodeAndStatus(Integer nodeId, Integer status) {
        return lambdaQuery()
                .eq(RetaBatch::getNodeId, nodeId)
                .eq(status != null, RetaBatch::getStatus, status)
                .ne(status == null, RetaBatch::getStatus, 4)
                .orderByDesc(RetaBatch::getCreateTime)
                .list();
    }

    @Override
    public boolean existsBatchNo(String batchNo) {
        return lambdaQuery().eq(RetaBatch::getBatchNo, batchNo).count() > 0;
    }

    @Override
    public boolean offline(Integer retaBatchId) {
        RetaBatch batch = new RetaBatch();
        batch.setRetaBatchId(retaBatchId);
        batch.setStatus(4);
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }

    @Override
    public String confirmBatch(Integer retaBatchId) {
        RetaBatch batch = getById(retaBatchId);
        if (batch == null) {
            return null;
        }
        if (!StringUtils.hasText(batch.getTraceCode())) {
            batch.setTraceCode(genTraceCode());
            batch.setTraceTime(LocalDateTime.now());
        }
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        updateById(batch);
        return batch.getTraceCode();
    }

    /** 溯源标识码：SHZ + 日期 + 6 位随机数，保证唯一 */
    private String genTraceCode() {
        String code;
        do {
            code = "SHZ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        } while (lambdaQuery().eq(RetaBatch::getTraceCode, code).count() > 0);
        return code;
    }
}
