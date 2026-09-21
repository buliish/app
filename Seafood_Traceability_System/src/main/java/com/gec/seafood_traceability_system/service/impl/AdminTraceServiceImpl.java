package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.Inspection;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.TraceChain;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.AdminTraceService;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.TraceChainLoader;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端批次追溯实现
 * <p>
 * 复用 {@link TraceChainLoader} 的走查结果（与消费者端同一套逻辑），
 * 但按运营视角渲染：包含检测明细、各环节质量状态、链路完整性。
 * 这里<b>不做</b> status 门槛 —— 管理员要能看到已下架批号的完整信息。
 */
@Service
public class AdminTraceServiceImpl implements AdminTraceService {

    @Autowired
    private TraceChainLoader traceChainLoader;

    @Autowired
    private InspectionService inspectionService;

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

    @Override
    public List<Map<String, Object>> search(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StringUtils.hasText(keyword)) {
            return result;
        }
        for (Integer id : traceChainLoader.resolveCandidates(keyword)) {
            RetaBatch r = retaBatchService.getById(id);
            if (r == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("retaBatchId", r.getRetaBatchId());
            m.put("traceCode", r.getTraceCode());
            m.put("productCode", r.getProductCode());
            m.put("batchNo", r.getBatchNo());
            m.put("breed", r.getBreed());
            m.put("productType", r.getProductType());
            m.put("productForm", r.getProductForm());
            m.put("specGrade", r.getSpecGrade());
            m.put("qualityStatus", r.getQualityStatus());
            m.put("status", r.getStatus());
            m.put("traceTime", r.getTraceTime());
            NodeInfo node = nodeInfoService.getById(r.getNodeId());
            m.put("retailerName", node == null ? null : node.getName());
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> chainByKeyword(String keyword) {
        TraceChain chain = traceChainLoader.loadByKeyword(keyword);
        return chain == null ? null : renderChain(chain);
    }

    @Override
    public Map<String, Object> chainByRetaBatchId(Integer retaBatchId) {
        TraceChain chain = traceChainLoader.loadByRetaBatchId(retaBatchId);
        return chain == null ? null : renderChain(chain);
    }

    /** 把链路渲染成管理端要的形态：环节明细 + 检测记录 + 质量汇总 */
    private Map<String, Object> renderChain(TraceChain chain) {
        RetaBatch reta = chain.getReta();
        WholBatch whol = chain.getWhol();
        FrozBatch froz = chain.getFroz();
        FarmBatch farm = chain.getFarm();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traceCode", reta.getTraceCode());
        data.put("productCode", reta.getProductCode());
        data.put("batchNo", reta.getBatchNo());
        data.put("breed", reta.getBreed());
        data.put("productType", reta.getProductType());
        data.put("productForm", reta.getProductForm());
        data.put("specGrade", reta.getSpecGrade());
        data.put("price", reta.getPrice());
        data.put("traceTime", reta.getTraceTime());
        data.put("complete", chain.isComplete());
        data.put("overallQuality", chain.overallQuality());

        // 四个环节的明细（自上而下：养殖 → 加工 → 批发 → 零售）
        List<Map<String, Object>> stages = new ArrayList<>();
        if (farm != null) {
            stages.add(stageRow(1, "养殖", chain.getFarmNode(), farm.getBatchNo(), null,
                    farm.getBreed(), farm.getProductForm(), null, farm.getQualityStatus(),
                    farm.getSourceType(), farm.getStatus(), farm.getCreateTime()));
        }
        if (froz != null) {
            stages.add(stageRow(2, "冷冻加工", chain.getFrozNode(), froz.getBatchNo(), froz.getUpBatchNo(),
                    froz.getBreed(), froz.getProductForm(), froz.getSpecGrade(), froz.getQualityStatus(),
                    null, froz.getStatus(), froz.getCreateTime()));
        }
        if (whol != null) {
            stages.add(stageRow(3, "批发", chain.getWholNode(), whol.getBatchNo(), whol.getUpBatchNo(),
                    whol.getBreed(), whol.getProductForm(), whol.getSpecGrade(), whol.getQualityStatus(),
                    null, whol.getStatus(), whol.getCreateTime()));
        }
        stages.add(stageRow(4, "零售", chain.getRetaNode(), reta.getBatchNo(), reta.getUpBatchNo(),
                reta.getBreed(), reta.getProductForm(), reta.getSpecGrade(), reta.getQualityStatus(),
                null, reta.getStatus(), reta.getCreateTime()));
        data.put("stages", stages);

        // 检测记录：一次取回整条链上的全部记录
        List<Inspection> inspections = inspectionService.listByRefs(chain.refs());
        data.put("inspections", inspections);
        // 各环节检测项计数，便于前端在环节行上显示"3 项检测 / 1 项不合格"
        data.put("inspectionSummary", summarize(inspections));

        data.put("processRecords", chain.getProcessRecords());
        return data;
    }

    /** 环节明细行 */
    private Map<String, Object> stageRow(int stageType, String stageName, NodeInfo node,
                                         String batchNo, String upBatchNo, String breed,
                                         String productForm, String specGrade, Integer qualityStatus,
                                         String sourceType, Integer status, Object time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stageType", stageType);
        m.put("stageName", stageName);
        m.put("nodeName", node == null ? "未知企业" : node.getName());
        m.put("nodeAddress", node == null ? null : node.getAddress());
        m.put("nodeTelephone", node == null ? null : node.getTelephone());
        m.put("batchNo", batchNo);
        m.put("upBatchNo", upBatchNo);
        m.put("breed", breed);
        m.put("productForm", productForm);
        m.put("specGrade", specGrade);
        m.put("qualityStatus", qualityStatus);
        m.put("sourceType", sourceType);
        m.put("status", status);
        m.put("time", time);
        return m;
    }

    /** 按环节汇总检测项数与不合格数 */
    private Map<String, Object> summarize(List<Inspection> inspections) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Inspection i : inspections) {
            @SuppressWarnings("unchecked")
            Map<String, Object> one = (Map<String, Object>) summary.computeIfAbsent(
                    String.valueOf(i.getStageType()),
                    k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("stageType", i.getStageType());
                        m.put("total", 0);
                        m.put("fails", 0);
                        return m;
                    });
            one.put("total", (Integer) one.get("total") + 1);
            if (i.getResult() != null && i.getResult() == 2) {
                one.put("fails", (Integer) one.get("fails") + 1);
            }
        }
        return summary;
    }

    @Override
    public Map<String, Object> stats() {
        // 各环节合格率：合格批号数 / 该环节批号总数
        List<Map<String, Object>> qualityRate = new ArrayList<>();
        qualityRate.add(rate("养殖", farmBatchService.count(), farmBatchService.lambdaQuery()
                .eq(FarmBatch::getQualityStatus, 1).count()));
        qualityRate.add(rate("冷冻加工", frozBatchService.count(), frozBatchService.lambdaQuery()
                .eq(FrozBatch::getQualityStatus, 1).count()));
        qualityRate.add(rate("批发", wholBatchService.count(), wholBatchService.lambdaQuery()
                .eq(WholBatch::getQualityStatus, 1).count()));
        qualityRate.add(rate("零售", retaBatchService.count(), retaBatchService.lambdaQuery()
                .eq(RetaBatch::getQualityStatus, 1).count()));

        // 在售产品的形态分布
        Map<String, Integer> formCount = new LinkedHashMap<>();
        formCount.put("鲜虾", 0);
        formCount.put("冻虾", 0);
        retaBatchService.lambdaQuery()
                .eq(RetaBatch::getStatus, 3)
                .list()
                .forEach(r -> {
                    String form = StringUtils.hasText(r.getProductForm()) ? r.getProductForm() : "冻虾";
                    formCount.merge(form, 1, Integer::sum);
                });
        List<Map<String, Object>> formDist = new ArrayList<>();
        formCount.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", k);
            m.put("value", v);
            formDist.add(m);
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("qualityRate", qualityRate);
        data.put("formDist", formDist);
        return data;
    }

    private Map<String, Object> rate(String name, long total, long pass) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("total", total);
        m.put("pass", pass);
        m.put("rate", total == 0 ? 0 : Math.round(pass * 1000.0 / total) / 10.0);
        return m;
    }
}
