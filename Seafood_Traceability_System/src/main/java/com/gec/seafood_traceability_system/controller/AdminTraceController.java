package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.InspectionRecord;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.TraceChain;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.TraceChainLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端批次追溯查询控制器。
 * <p>
 * 与消费者端的区别在于<b>不做状态门槛</b>：消费者只能看到已确认在售的批号，
 * 而管理端的职责是监管与排障，必须能看到已下架批号、断链链路以及各环节检测记录。
 * 正因如此，这里直接复用 {@link TraceChainLoader}（该组件刻意不做状态过滤），
 * 而不是走消费者门面 {@code TraceService}。
 * <p>
 * 鉴权由 WebMvcConfig 的 AdminAuthInterceptor 统一负责（/admin/** 要求 role=admin）。
 */
@RestController
@RequestMapping("/admin/trace")
public class AdminTraceController {

    /** 追溯搜索一次最多返回的候选批号数，避免关键词过宽时拖垮查询 */
    private static final int SEARCH_LIMIT = 50;

    @Autowired
    private TraceChainLoader traceChainLoader;

    @Autowired
    private RetaBatchService retaBatchService;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private NodeInfoService nodeInfoService;

    /**
     * 追溯搜索：按溯源码 / 零售批号 / 对外产品编号模糊匹配零售批号。
     * <p>
     * 返回候选列表供管理端选中后查看完整链路。
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("请输入查询关键词");
        }
        String kw = keyword.trim();
        if (kw.length() > 50) {
            return Result.error("查询关键词过长");
        }
        List<RetaBatch> matches = retaBatchService.lambdaQuery()
                .and(w -> w.like(RetaBatch::getTraceCode, kw)
                        .or().like(RetaBatch::getBatchNo, kw)
                        .or().like(RetaBatch::getProductCode, kw))
                .orderByDesc(RetaBatch::getRetaBatchId)
                .last("LIMIT " + SEARCH_LIMIT)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (RetaBatch batch : matches) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("retaBatchId", batch.getRetaBatchId());
            item.put("batchNo", batch.getBatchNo());
            item.put("traceCode", batch.getTraceCode());
            item.put("productCode", batch.getProductCode());
            item.put("breed", batch.getBreed());
            item.put("productType", batch.getProductType());
            item.put("status", batch.getStatus());
            item.put("qualityStatus", batch.getQualityStatus());
            item.put("traceTime", batch.getTraceTime());
            NodeInfo retailer = batch.getNodeId() == null ? null
                    : findNode(batch.getNodeId());
            item.put("retailerName", retailer == null ? null : retailer.getName());
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 按关键词直接取完整链路（命中多条时取最新一条）。
     * <p>
     * 供管理端在搜索框里直接回车查询使用。
     */
    @GetMapping("/chain")
    public Result<Map<String, Object>> chain(@RequestParam String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("请输入查询关键词");
        }
        String kw = keyword.trim();
        if (kw.length() > 50) {
            return Result.error("查询关键词过长");
        }
        RetaBatch batch = findByCodeOrBatchNo(kw);
        if (batch == null) {
            return Result.error("未找到对应的批号或溯源标识码");
        }
        return Result.success(renderChain(batch.getRetaBatchId()));
    }

    /** 按零售批号主键取完整链路 */
    @GetMapping("/chain/{id}")
    public Result<Map<String, Object>> chainById(@PathVariable Integer id) {
        Map<String, Object> data = renderChain(id);
        if (data == null) {
            return Result.error("批号不存在或已被删除");
        }
        return Result.success(data);
    }

    /** 追溯概况统计：各状态批号数与链路完整情况 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        long total = retaBatchService.lambdaQuery().count();
        long confirmed = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getStatus, 3)
                .count();
        long withTraceCode = retaBatchService.lambdaQuery()
                .isNotNull(RetaBatch::getTraceCode)
                .count();
        long rejected = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getQualityStatus, TraceChain.QUALITY_REJECTED)
                .count();

        //链路是否完整要看上游能不能串起来，抽样统计最近若干条
        List<RetaBatch> sample = retaBatchService.lambdaQuery()
                .orderByDesc(RetaBatch::getRetaBatchId)
                .last("LIMIT 200")
                .list();
        long complete = sample.stream()
                .map(b -> traceChainLoader.loadByRetaBatchId(b.getRetaBatchId()))
                .filter(c -> c != null && c.isComplete())
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("confirmed", confirmed);
        data.put("withTraceCode", withTraceCode);
        data.put("rejected", rejected);
        data.put("sampledForChain", sample.size());
        data.put("completeChain", complete);
        return Result.success(data);
    }

    /** 先按溯源码 / 产品编号，再退回按零售批号查 */
    private RetaBatch findByCodeOrBatchNo(String keyword) {
        RetaBatch batch = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getTraceCode, keyword)
                .one();
        if (batch != null) {
            return batch;
        }
        batch = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getProductCode, keyword)
                .orderByDesc(RetaBatch::getRetaBatchId)
                .last("LIMIT 1")
                .one();
        if (batch != null) {
            return batch;
        }
        return retaBatchService.lambdaQuery()
                .eq(RetaBatch::getBatchNo, keyword)
                .one();
    }

    /**
     * 渲染管理端链路视图。
     * <p>
     * 字段比消费者端多出：链路是否完整、各级真实状态值（含已下架）、
     * 以及各环节的检测记录——这些正是管理端排障需要、
     * 而消费者端刻意隐藏的信息。
     */
    private Map<String, Object> renderChain(Integer retaBatchId) {
        TraceChain chain = traceChainLoader.loadByRetaBatchId(retaBatchId);
        if (chain == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("complete", chain.isComplete());
        data.put("overallQuality", chain.overallQuality());

        RetaBatch reta = chain.getReta();
        data.put("traceCode", reta.getTraceCode());
        data.put("batchNo", reta.getBatchNo());
        data.put("productCode", reta.getProductCode());
        data.put("breed", reta.getBreed());
        data.put("productType", reta.getProductType());
        data.put("productForm", reta.getProductForm());
        data.put("specGrade", reta.getSpecGrade());
        data.put("qualityStatus", reta.getQualityStatus());

        List<Map<String, Object>> stages = new ArrayList<>();
        if (chain.getFarm() != null) {
            // 养殖环节没有"产品类型"，用产品形态（鲜虾/冻虾）占该位置，前端按 stageType 区分展示
            stages.add(stageView(TraceChain.STAGE_FARM, "养殖企业",
                    chain.getFarmNode(), chain.getFarm().getBatchNo(),
                    chain.getFarm().getBreed(), chain.getFarm().getProductForm(),
                    chain.getFarm().getQualityStatus(), chain.getFarm().getCreateTime()));
        }
        if (chain.getFroz() != null) {
            stages.add(stageView(TraceChain.STAGE_FROZ, "冷冻加工企业",
                    chain.getFrozNode(), chain.getFroz().getBatchNo(),
                    chain.getFroz().getBreed(), chain.getFroz().getProductType(),
                    chain.getFroz().getQualityStatus(), chain.getFroz().getCreateTime()));
        }
        if (chain.getWhol() != null) {
            stages.add(stageView(TraceChain.STAGE_WHOL, "批发商",
                    chain.getWholNode(), chain.getWhol().getBatchNo(),
                    chain.getWhol().getBreed(), chain.getWhol().getProductType(),
                    chain.getWhol().getQualityStatus(), chain.getWhol().getCreateTime()));
        }
        stages.add(stageView(TraceChain.STAGE_RETA, "零售商",
                chain.getRetaNode(), reta.getBatchNo(),
                reta.getBreed(), reta.getProductType(),
                reta.getQualityStatus(), reta.getCreateTime()));
        data.put("chain", stages);

        //各环节检测记录：管理端要比消费者端看得更全
        List<InspectionRecord> inspections = inspectionService.listByRefs(chain.refs());
        data.put("inspections", inspections);
        data.put("processRecords", chain.getProcessRecords());
        return data;
    }

    private Map<String, Object> stageView(Integer stageType, String stageName, NodeInfo node,
                                          String batchNo, String breed, String productType,
                                          Integer qualityStatus, Object time) {
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("stageType", stageType);
        stage.put("stage", stageName);
        stage.put("nodeName", node == null ? "未知企业" : node.getName());
        stage.put("nodeCode", node == null ? null : node.getCode());
        stage.put("nodeAddress", node == null ? null : node.getAddress());
        stage.put("batchNo", batchNo);
        stage.put("breed", breed);
        stage.put("productType", productType);
        stage.put("qualityStatus", qualityStatus);
        stage.put("time", time);
        return stage;
    }

    /** 按主键取企业。管理端一次只渲染一条链路，无需引入缓存组件 */
    private NodeInfo findNode(Integer nodeId) {
        return nodeInfoService.getById(nodeId);
    }
}
