package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.RetaBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /**
     * 下游待确认批号查询。
     * <p>
     * 零售是链路末端，本方法对零售自身不产生"下游确认"语义；
     * 它存在是因为批发商环节需要查 reta_batch，通过本 Service 委托过来，
     * 从而保证"每个环节查自己的下游表"这一对称结构。
     */
    @Override
    public List<ConfirmVO> listDownConfirm(List<String> upBatchNos, String downName) {
        if (upBatchNos == null || upBatchNos.isEmpty()) {
            return new ArrayList<>();
        }
        List<ConfirmVO> result = baseMapper.selectDownConfirmList(upBatchNos, downName);
        // 关联映射把企业塞在 downNode 里，这里派生出前端契约字段 downName
        result.forEach(vo -> vo.setDownName(vo.getDownNode() == null ? null : vo.getDownNode().getName()));
        return result;
    }

    @Override
    public boolean offline(Integer retaBatchId) {
        RetaBatch batch = new RetaBatch();
        batch.setRetaBatchId(retaBatchId);
        batch.setStatus(4);
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }

    /**
     * 确认零售商批号：状态置为已确认，并生成溯源标识码。
     * <p>
     * 加了事务，保证"改状态"和"写溯源码"要么都成功、要么都回滚，
     * 不会出现"状态已确认但溯源码为空"的中间态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirmBatch(Integer retaBatchId) {
        RetaBatch batch = getById(retaBatchId);
        if (batch == null) {
            throw new BizException("批号不存在或已被删除");
        }
        if (batch.getStatus() != null && batch.getStatus() == 4) {
            throw new BizException("已下架批号不能确认");
        }
        // 幂等：重复确认直接返回已有的溯源码，不再重新生成
        if (StringUtils.hasText(batch.getTraceCode())) {
            if (batch.getStatus() == null || batch.getStatus() != 3) {
                batch.setStatus(3);
                batch.setUpdateTime(LocalDateTime.now());
                updateById(batch);
            }
            return batch.getTraceCode();
        }

        batch.setTraceCode(genTraceCode());
        batch.setTraceTime(LocalDateTime.now());
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        updateById(batch);
        return batch.getTraceCode();
    }

    /**
     * 溯源标识码：SHZ + 日期 + 6 位随机数。
     * <p>
     * 先查重 + 最多重试 3 次；并发下若仍撞车，数据库唯一索引 uk_trace_code 会拦下来，
     * 由全局异常处理器转成友好提示。
     */
    private String genTraceCode() {
        for (int i = 0; i < 3; i++) {
            String code = "SHZ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
            if (lambdaQuery().eq(RetaBatch::getTraceCode, code).count() == 0) {
                return code;
            }
        }
        throw new BizException("溯源标识码生成冲突，请重试");
    }
}
