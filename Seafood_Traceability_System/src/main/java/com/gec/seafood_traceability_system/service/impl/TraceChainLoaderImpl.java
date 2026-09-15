package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.TraceChain;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.ProcessRecordService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.TraceChainLoader;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 溯源链路走查实现。
 * <p>
 * 上溯依据是各级批号的 {@code up_batch_no}：零售商录号时选定了上游批发商的
 * 批号，批发商录号时选定了上游加工厂的批号，如此逐级串成一条链。
 * 每一步都按批号唯一索引查单条（四张批号表的 batch_no 都是唯一键）。
 * <p>
 * 断链（某一级批号查不到）时不再往下走，直接把已装配的部分返回，
 * 由 {@link TraceChain#isComplete()} 暴露给调用方。
 */
@Service
public class TraceChainLoaderImpl implements TraceChainLoader {

    @Autowired
    private RetaBatchService retaBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private ProcessRecordService processRecordService;

    @Override
    public TraceChain loadByRetaBatchId(Integer retaBatchId) {
        if (retaBatchId == null) {
            return null;
        }
        RetaBatch reta = retaBatchService.getById(retaBatchId);
        if (reta == null) {
            return null;
        }
        return load(reta);
    }

    /** 按「零售 → 批发 → 冷冻加工 → 养殖」的顺序逐级上溯 */
    private TraceChain load(RetaBatch reta) {
        TraceChain chain = new TraceChain();
        chain.setReta(reta);
        chain.setRetaNode(nodeInfoService.getById(reta.getNodeId()));

        WholBatch whol = findWholByBatchNo(reta.getUpBatchNo());
        if (whol == null) {
            return chain;
        }
        chain.setWhol(whol);
        chain.setWholNode(nodeInfoService.getById(whol.getNodeId()));

        FrozBatch froz = findFrozByBatchNo(whol.getUpBatchNo());
        if (froz == null) {
            return chain;
        }
        chain.setFroz(froz);
        chain.setFrozNode(nodeInfoService.getById(froz.getNodeId()));
        //加工工序记录（清洗/分级/冷冻/包装）挂在加工批号上
        chain.setProcessRecords(processRecordService.listByBatchId(froz.getFrozBatchId()));

        FarmBatch farm = findFarmByBatchNo(froz.getUpBatchNo());
        if (farm == null) {
            return chain;
        }
        chain.setFarm(farm);
        chain.setFarmNode(nodeInfoService.getById(farm.getNodeId()));
        return chain;
    }

    private WholBatch findWholByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        return wholBatchService.lambdaQuery()
                .eq(WholBatch::getBatchNo, batchNo)
                .one();
    }

    private FrozBatch findFrozByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        return frozBatchService.lambdaQuery()
                .eq(FrozBatch::getBatchNo, batchNo)
                .one();
    }

    private FarmBatch findFarmByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        return farmBatchService.lambdaQuery()
                .eq(FarmBatch::getBatchNo, batchNo)
                .one();
    }
}
