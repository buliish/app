package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.ProcessRecordService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.TraceService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 消费者溯源业务实现：沿批号逐级上溯 零售 -> 批发 -> 冷冻加工 -> 养殖 */
@Service
public class TraceServiceImpl implements TraceService {

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
    public Map<String, Object> trace(String traceCode) {
        RetaBatch reta = retaBatchService.lambdaQuery().eq(RetaBatch::getTraceCode, traceCode).one();
        if (reta == null) {
            return null;
        }
        // 已下架批号不再对外提供溯源（产品的流通凭证已失效）
        if (reta.getStatus() == null || reta.getStatus() != 3) {
            throw new BizException("该产品批号已下架，暂不支持溯源查询");
        }

        // 逐级上溯：除了批号，还限定"上游企业编号"，
        // 避免不同企业出现同号批号时串链（与 up_node_id 字段的设计语义保持一致）
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

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traceCode", reta.getTraceCode());
        data.put("batchNo", reta.getBatchNo());
        data.put("breed", reta.getBreed());
        data.put("productType", reta.getProductType());
        data.put("traceTime", reta.getTraceTime());

        List<Map<String, Object>> chain = new ArrayList<>();
        if (farm != null) {
            chain.add(stage("养殖企业", nodeInfoService.getById(farm.getNodeId()), farm.getBatchNo(),
                    farm.getBreed(), farm.getBreedStage(), farm.getCreateTime()));
            data.put("farm", stageDetail(nodeInfoService.getById(farm.getNodeId())));
            data.put("farmBatch", farm);
        }
        if (froz != null) {
            chain.add(stage("冷冻加工企业", nodeInfoService.getById(froz.getNodeId()), froz.getBatchNo(),
                    froz.getBreed(), froz.getProductType(), froz.getCreateTime()));
            data.put("processor", stageDetail(nodeInfoService.getById(froz.getNodeId())));
            data.put("frozBatch", froz);
            //加工工序记录（清洗/分级/冷冻/包装）
            List<ProcessRecord> records = processRecordService.listByBatchId(froz.getFrozBatchId());
            data.put("processRecords", records);
        }
        if (whol != null) {
            chain.add(stage("批发商", nodeInfoService.getById(whol.getNodeId()), whol.getBatchNo(),
                    whol.getBreed(), whol.getProductType(), whol.getCreateTime()));
            data.put("wholesaler", stageDetail(nodeInfoService.getById(whol.getNodeId())));
            data.put("wholBatch", whol);
        }
        chain.add(stage("零售商", nodeInfoService.getById(reta.getNodeId()), reta.getBatchNo(),
                reta.getBreed(), reta.getProductType(), reta.getCreateTime()));
        data.put("retailer", stageDetail(nodeInfoService.getById(reta.getNodeId())));
        data.put("retaBatch", reta);
        data.put("chain", chain);
        return data;
    }

    /** 链路节点简要信息 */
    private Map<String, Object> stage(String stageName, NodeInfo node, String batchNo,
                                      String breed, String productType, Object time) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stage", stageName);
        map.put("nodeName", node == null ? "未知企业" : node.getName());
        map.put("batchNo", batchNo);
        map.put("breed", breed);
        map.put("productType", productType);
        map.put("time", time);
        return map;
    }

    /** 企业联系方式明细 */
    private Map<String, Object> stageDetail(NodeInfo node) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (node == null) {
            return map;
        }
        map.put("name", node.getName());
        map.put("code", node.getCode());
        map.put("address", node.getAddress());
        map.put("corporation", node.getCorporation());
        map.put("telephone", node.getTelephone());
        map.put("businessId", node.getBusinessId());
        return map;
    }
}
