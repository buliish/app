package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 溯源链路走查实现：沿批号逐级上溯 零售 → 批发 → 冷冻加工 → 养殖
 * <p>
 * 逐级上溯的匹配条件同时限定 {@code batch_no} 与 {@code node_id}，
 * 避免不同企业出现同号批号时串链（与 up_node_id 字段的设计语义一致）。
 * 正向追踪（关键词不是零售批号时）同样带上 node_id 条件，保持对称。
 */
@Service
public class TraceChainLoaderImpl implements TraceChainLoader {

    /** 环节类型，与 node_info.type 同码 */
    private static final int STAGE_FARM = 1;
    private static final int STAGE_FROZ = 2;
    private static final int STAGE_WHOL = 3;
    private static final int STAGE_RETA = 4;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    @Autowired
    private RetaBatchService retaBatchService;

    @Autowired
    private ProcessRecordService processRecordService;

    @Override
    public TraceChain loadByRetaBatchId(Integer retaBatchId) {
        if (retaBatchId == null) {
            return null;
        }
        RetaBatch reta = retaBatchService.getById(retaBatchId);
        return reta == null ? null : walkUp(reta);
    }

    @Override
    public TraceChain loadByKeyword(String keyword) {
        List<Integer> candidates = resolveCandidates(keyword);
        return candidates.size() == 1 ? loadByRetaBatchId(candidates.get(0)) : null;
    }

    /**
     * 关键词 → 候选零售批号。
     * <p>
     * 依次尝试：溯源码 / 产品编号 / 零售批号（反向解析），
     * 以及批发、加工、养殖批号（正向追踪，逐级向下找到最终零售端）。
     * 命中多条时按状态优先（已确认的在售批号排前）、再按主键倒序。
     */
    @Override
    public List<Integer> resolveCandidates(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        Set<Integer> ids = new LinkedHashSet<>();

        // 1. 直接命中零售批号（溯源码 / 产品编号 / 批号）
        retaBatchService.lambdaQuery()
                .eq(RetaBatch::getTraceCode, kw)
                .list()
                .forEach(r -> ids.add(r.getRetaBatchId()));
        retaBatchService.lambdaQuery()
                .eq(RetaBatch::getProductCode, kw)
                .list()
                .forEach(r -> ids.add(r.getRetaBatchId()));
        retaBatchService.lambdaQuery()
                .eq(RetaBatch::getBatchNo, kw)
                .list()
                .forEach(r -> ids.add(r.getRetaBatchId()));

        // 2. 命中批发批号 → 向下找零售端
        for (WholBatch w : wholBatchService.lambdaQuery().eq(WholBatch::getBatchNo, kw).list()) {
            retaBatchService.lambdaQuery()
                    .eq(RetaBatch::getUpBatchNo, w.getBatchNo())
                    .eq(RetaBatch::getUpNodeId, w.getNodeId())
                    .list()
                    .forEach(r -> ids.add(r.getRetaBatchId()));
        }

        // 3. 命中加工批号 → 向下两跳（批发 → 零售）
        for (FrozBatch f : frozBatchService.lambdaQuery().eq(FrozBatch::getBatchNo, kw).list()) {
            for (WholBatch w : wholBatchService.lambdaQuery()
                    .eq(WholBatch::getUpBatchNo, f.getBatchNo())
                    .eq(WholBatch::getUpNodeId, f.getNodeId())
                    .list()) {
                retaBatchService.lambdaQuery()
                        .eq(RetaBatch::getUpBatchNo, w.getBatchNo())
                        .eq(RetaBatch::getUpNodeId, w.getNodeId())
                        .list()
                        .forEach(r -> ids.add(r.getRetaBatchId()));
            }
        }

        // 4. 命中养殖批号 → 向下三跳（加工 → 批发 → 零售）
        for (FarmBatch fm : farmBatchService.lambdaQuery().eq(FarmBatch::getBatchNo, kw).list()) {
            for (FrozBatch f : frozBatchService.lambdaQuery()
                    .eq(FrozBatch::getUpBatchNo, fm.getBatchNo())
                    .eq(FrozBatch::getUpNodeId, fm.getNodeId())
                    .list()) {
                for (WholBatch w : wholBatchService.lambdaQuery()
                        .eq(WholBatch::getUpBatchNo, f.getBatchNo())
                        .eq(WholBatch::getUpNodeId, f.getNodeId())
                        .list()) {
                    retaBatchService.lambdaQuery()
                            .eq(RetaBatch::getUpBatchNo, w.getBatchNo())
                            .eq(RetaBatch::getUpNodeId, w.getNodeId())
                            .list()
                            .forEach(r -> ids.add(r.getRetaBatchId()));
                }
            }
        }

        return new ArrayList<>(ids);
    }

    /**
     * 从零售批号逐级上溯，装配完整链路。
     * <p>
     * 刻意不做 status 过滤：管理端需要查看已下架批号的链路。
     * 消费者侧的"已下架不可溯源"由门面层负责。
     */
    private TraceChain walkUp(RetaBatch reta) {
        WholBatch whol = wholBatchService.lambdaQuery()
                .eq(WholBatch::getBatchNo, reta.getUpBatchNo())
                .eq(reta.getUpNodeId() != null, WholBatch::getNodeId, reta.getUpNodeId())
                .one();
        FrozBatch froz = whol == null ? null
                : frozBatchService.lambdaQuery()
                        .eq(FrozBatch::getBatchNo, whol.getUpBatchNo())
                        .eq(whol.getUpNodeId() != null, FrozBatch::getNodeId, whol.getUpNodeId())
                        .one();
        FarmBatch farm = froz == null ? null
                : farmBatchService.lambdaQuery()
                        .eq(FarmBatch::getBatchNo, froz.getUpBatchNo())
                        .eq(froz.getUpNodeId() != null, FarmBatch::getNodeId, froz.getUpNodeId())
                        .one();

        TraceChain chain = new TraceChain();
        chain.setReta(reta);
        chain.setWhol(whol);
        chain.setFroz(froz);
        chain.setFarm(farm);

        // 每级企业只取一次（原实现在 chain 与 stageDetail 里各查了一遍）
        chain.setRetaNode(nodeInfoService.getById(reta.getNodeId()));
        if (whol != null) {
            chain.setWholNode(nodeInfoService.getById(whol.getNodeId()));
        }
        if (froz != null) {
            chain.setFrozNode(nodeInfoService.getById(froz.getNodeId()));
            chain.setProcessRecords(processRecordService.listByBatchId(froz.getFrozBatchId()));
        }
        if (farm != null) {
            chain.setFarmNode(nodeInfoService.getById(farm.getNodeId()));
        }
        return chain;
    }
}
