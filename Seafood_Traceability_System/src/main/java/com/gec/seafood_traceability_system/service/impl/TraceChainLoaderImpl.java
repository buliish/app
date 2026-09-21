package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.pojo.ChainTreeNode;
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
import java.util.Comparator;
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

    // ------------------------------------------------------------------
    // 产业链树
    // ------------------------------------------------------------------

    @Override
    public ChainTreeNode loadTreeByRetaBatchId(Integer retaBatchId) {
        if (retaBatchId == null) {
            return null;
        }
        RetaBatch reta = retaBatchService.getById(retaBatchId);
        if (reta == null) {
            return null;
        }
        // 先走一遍当前链，得到"哪几个批号是用户正在看的那条"，用于打 onCurrentChain 标记
        TraceChain current = walkUp(reta);
        Set<String> currentPath = currentPathKeys(current);

        // 链路上游是线、下游是树：从能上溯到的最靠上游那一级开始向下展开。
        // 养殖断链时就退化为以加工（或批发/零售）为根，而不是直接返回 null。
        if (current.getFarm() != null) {
            return buildFarmNode(current.getFarm(), currentPath);
        }
        if (current.getFroz() != null) {
            return buildFrozNode(current.getFroz(), currentPath);
        }
        if (current.getWhol() != null) {
            return buildWholNode(current.getWhol(), currentPath);
        }
        return buildRetaNode(reta, currentPath);
    }

    /** 当前链上各级批号的唯一标识（环节码 + 批号），供打标记时比对 */
    private Set<String> currentPathKeys(TraceChain chain) {
        Set<String> keys = new LinkedHashSet<>();
        if (chain.getFarm() != null) {
            keys.add(key(STAGE_FARM, chain.getFarm().getBatchNo()));
        }
        if (chain.getFroz() != null) {
            keys.add(key(STAGE_FROZ, chain.getFroz().getBatchNo()));
        }
        if (chain.getWhol() != null) {
            keys.add(key(STAGE_WHOL, chain.getWhol().getBatchNo()));
        }
        if (chain.getReta() != null) {
            keys.add(key(STAGE_RETA, chain.getReta().getBatchNo()));
        }
        return keys;
    }

    private String key(int stageType, String batchNo) {
        return stageType + ":" + batchNo;
    }

    /**
     * 向下找某一级的所有下游批号。
     * <p>
     * 同时限定 {@code up_batch_no} 与 {@code up_node_id}：不同企业可能出现同号批号，
     * 只按批号匹配会串链（与 walkUp 的匹配口径保持一致）。
     * 结果按批号排序，保证同一份数据每次渲染顺序一致。
     */
    private List<FrozBatch> findFrozDownstream(String upBatchNo, Integer upNodeId) {
        if (upBatchNo == null) {
            return new ArrayList<>();
        }
        List<FrozBatch> list = frozBatchService.lambdaQuery()
                .eq(FrozBatch::getUpBatchNo, upBatchNo)
                .eq(upNodeId != null, FrozBatch::getUpNodeId, upNodeId)
                .list();
        list.sort(Comparator.comparing(FrozBatch::getBatchNo));
        return list;
    }

    private List<WholBatch> findWholDownstream(String upBatchNo, Integer upNodeId) {
        if (upBatchNo == null) {
            return new ArrayList<>();
        }
        List<WholBatch> list = wholBatchService.lambdaQuery()
                .eq(WholBatch::getUpBatchNo, upBatchNo)
                .eq(upNodeId != null, WholBatch::getUpNodeId, upNodeId)
                .list();
        list.sort(Comparator.comparing(WholBatch::getBatchNo));
        return list;
    }

    private List<RetaBatch> findRetaDownstream(String upBatchNo, Integer upNodeId) {
        if (upBatchNo == null) {
            return new ArrayList<>();
        }
        List<RetaBatch> list = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getUpBatchNo, upBatchNo)
                .eq(upNodeId != null, RetaBatch::getUpNodeId, upNodeId)
                .list();
        list.sort(Comparator.comparing(RetaBatch::getBatchNo));
        return list;
    }

    private ChainTreeNode buildFarmNode(FarmBatch farm, Set<String> currentPath) {
        ChainTreeNode node = newNode(STAGE_FARM, "养殖企业", farm.getFarmBatchId(),
                farm.getBatchNo(), farm.getNodeId(), farm.getBreed(), farm.getProductForm(),
                farm.getStatus(), farm.getQualityStatus(), farm.getCreateTime(), currentPath);
        for (FrozBatch child : findFrozDownstream(farm.getBatchNo(), farm.getNodeId())) {
            node.getChildren().add(buildFrozNode(child, currentPath));
        }
        return node;
    }

    private ChainTreeNode buildFrozNode(FrozBatch froz, Set<String> currentPath) {
        ChainTreeNode node = newNode(STAGE_FROZ, "冷冻加工企业", froz.getFrozBatchId(),
                froz.getBatchNo(), froz.getNodeId(), froz.getBreed(), froz.getProductType(),
                froz.getStatus(), froz.getQualityStatus(), froz.getCreateTime(), currentPath);
        for (WholBatch child : findWholDownstream(froz.getBatchNo(), froz.getNodeId())) {
            node.getChildren().add(buildWholNode(child, currentPath));
        }
        return node;
    }

    private ChainTreeNode buildWholNode(WholBatch whol, Set<String> currentPath) {
        ChainTreeNode node = newNode(STAGE_WHOL, "批发商", whol.getWholBatchId(),
                whol.getBatchNo(), whol.getNodeId(), whol.getBreed(), whol.getProductType(),
                whol.getStatus(), whol.getQualityStatus(), whol.getCreateTime(), currentPath);
        for (RetaBatch child : findRetaDownstream(whol.getBatchNo(), whol.getNodeId())) {
            node.getChildren().add(buildRetaNode(child, currentPath));
        }
        return node;
    }

    /** 零售是链路末端，不再向下展开 */
    private ChainTreeNode buildRetaNode(RetaBatch reta, Set<String> currentPath) {
        return newNode(STAGE_RETA, "零售商", reta.getRetaBatchId(),
                reta.getBatchNo(), reta.getNodeId(), reta.getBreed(), reta.getProductType(),
                reta.getStatus(), reta.getQualityStatus(), reta.getCreateTime(), currentPath);
    }

    private ChainTreeNode newNode(int stageType, String stageName, Integer batchId, String batchNo,
                                 Integer nodeId, String breed, String productType,
                                 Integer status, Integer qualityStatus, Object createTime,
                                 Set<String> currentPath) {
        ChainTreeNode node = new ChainTreeNode();
        node.setStageType(stageType);
        node.setStage(stageName);
        node.setBatchId(batchId);
        node.setBatchNo(batchNo);
        node.setNode(nodeId == null ? null : nodeInfoService.getById(nodeId));
        node.setBreed(breed);
        node.setProductType(productType);
        node.setStatus(status);
        node.setQualityStatus(qualityStatus);
        node.setOnCurrentChain(currentPath.contains(key(stageType, batchNo)));
        return node;
    }
}
