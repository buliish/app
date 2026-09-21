package com.gec.seafood_traceability_system.service.impl;

import com.gec.seafood_traceability_system.mapper.RetaBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.TraceChain;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.TraceChainLoader;
import com.gec.seafood_traceability_system.service.TraceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者溯源业务实现
 * <p>
 * 链路走查已抽到 {@link TraceChainLoader}（管理端复用同一实现），
 * 本类只负责：查入口批号 → 施加消费者侧的状态门槛 → 渲染成前端契约的 Map。
 */
@Service
public class TraceServiceImpl implements TraceService {

    @Autowired
    private TraceChainLoader traceChainLoader;

    @Autowired
    private RetaBatchService retaBatchService;

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private NodeInfoService nodeInfoService;

    /** 直接注入 Mapper：商品列表要走 XML 里的一条 JOIN 查询 */
    @Autowired
    private RetaBatchMapper retaBatchMapper;

    /** 单页最多返回的商品数，避免超大分页拖垮查询 */
    private static final long MAX_PAGE_SIZE = 50;

    @Override
    public Map<String, Object> listProducts(long current, long size, String keyword, String form, Integer provId) {
        long safeCurrent = Math.max(current, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        IPage<Map<String, Object>> page = new Page<>(safeCurrent, safeSize);
        // 一条 JOIN 取全四级链路信息，不做逐卡片的链路回走
        IPage<Map<String, Object>> result = retaBatchMapper.selectProductPage(page, keyword, form, provId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", result.getRecords());
        return data;
    }

    @Override
    public Map<String, Object> productDetailByTraceCode(String traceCode) {
        RetaBatch reta = findByCodeOrProductCode(traceCode);
        if (reta == null) {
            return null;
        }
        TraceChain chain = traceChainLoader.loadByRetaBatchId(reta.getRetaBatchId());
        if (chain == null) {
            return null;
        }
        // 产品身份字段已由 render() 统一填充，这里只补详情独有的部分
        Map<String, Object> data = render(chain);
        data.put("inspections", inspectionService.listByRefs(chain.refs()));
        // 已下架的链接仍可打开，但前端要给出明确提示
        data.put("offline", reta.getStatus() == null || reta.getStatus() != 3);
        return data;
    }

    @Override
    public boolean existsCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        // 只看存在性，不看 status —— 已下架批号的二维码仍应能生成
        return findByCodeOrProductCode(code.trim()) != null;
    }

    /** 先按溯源码查，再退回按对外产品编号查（消费者可能输的是包装上的产品编号） */
    private RetaBatch findByCodeOrProductCode(String code) {
        RetaBatch reta = retaBatchService.lambdaQuery()
                .eq(RetaBatch::getTraceCode, code)
                .one();
        if (reta == null) {
            reta = retaBatchService.lambdaQuery()
                    .eq(RetaBatch::getProductCode, code)
                    .orderByDesc(RetaBatch::getRetaBatchId)
                    .last("LIMIT 1")
                    .one();
        }
        return reta;
    }

    @Override
    public Map<String, Object> trace(String traceCode) {
        // 先按溯源码查，再退回按对外产品编号查
        RetaBatch reta = findByCodeOrProductCode(traceCode);
        if (reta == null) {
            return null;
        }
        // 已下架批号不再对外提供溯源（产品的流通凭证已失效）。
        // 注意：这个门槛属于消费者侧的展示策略，必须留在本门面层，
        // 不能下沉到 TraceChainLoader —— 否则管理端也看不到已下架批号了。
        if (reta.getStatus() == null || reta.getStatus() != 3) {
            throw new BizException("该产品批号已下架，暂不支持溯源查询");
        }

        TraceChain chain = traceChainLoader.loadByRetaBatchId(reta.getRetaBatchId());
        if (chain == null) {
            return null;
        }
        return render(chain);
    }

    /**
     * 渲染成前端契约。
     * <p>
     * 返回的 key 集合与改造前完全一致（TraceView.vue 的 nodeRows 直接读
     * farm/processor/wholesaler/retailer 四个 key），新增字段一律追加。
     */
    private Map<String, Object> render(TraceChain chain) {
        RetaBatch reta = chain.getReta();
        WholBatch whol = chain.getWhol();
        FrozBatch froz = chain.getFroz();
        FarmBatch farm = chain.getFarm();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traceCode", reta.getTraceCode());
        data.put("batchNo", reta.getBatchNo());
        data.put("breed", reta.getBreed());
        data.put("productType", reta.getProductType());
        data.put("traceTime", reta.getTraceTime());
        // 产品身份字段（消费者端商品卡用；老字段一律保留不动）
        data.put("productCode", reta.getProductCode());
        data.put("productForm", reta.getProductForm());
        data.put("specGrade", reta.getSpecGrade());
        data.put("price", reta.getPrice());
        data.put("imageUrl", reta.getImageUrl());
        data.put("qualityStatus", reta.getQualityStatus());
        data.put("overallQuality", chain.overallQuality());
        data.put("complete", chain.isComplete());
        if (farm != null) {
            data.put("sourceType", farm.getSourceType());
        }

        List<Map<String, Object>> stages = new ArrayList<>();
        if (farm != null) {
            stages.add(stage("养殖企业", chain.getFarmNode(), farm.getBatchNo(),
                    farm.getBreed(), farm.getBreedStage(), farm.getCreateTime()));
            data.put("farm", stageDetail(chain.getFarmNode()));
            data.put("farmBatch", farm);
        }
        if (froz != null) {
            stages.add(stage("冷冻加工企业", chain.getFrozNode(), froz.getBatchNo(),
                    froz.getBreed(), froz.getProductType(), froz.getCreateTime()));
            data.put("processor", stageDetail(chain.getFrozNode()));
            data.put("frozBatch", froz);
            //加工工序记录（清洗/分级/冷冻/包装）
            List<ProcessRecord> records = chain.getProcessRecords();
            data.put("processRecords", records);
        }
        if (whol != null) {
            stages.add(stage("批发商", chain.getWholNode(), whol.getBatchNo(),
                    whol.getBreed(), whol.getProductType(), whol.getCreateTime()));
            data.put("wholesaler", stageDetail(chain.getWholNode()));
            data.put("wholBatch", whol);
        }
        stages.add(stage("零售商", chain.getRetaNode(), reta.getBatchNo(),
                reta.getBreed(), reta.getProductType(), reta.getCreateTime()));
        data.put("retailer", stageDetail(chain.getRetaNode()));
        data.put("retaBatch", reta);
        data.put("chain", stages);
        data.put("relatedProducts", relatedProducts(chain));
        return data;
    }

    /**
     * 同源产品：与本商品出自<b>同一个养殖批号</b>的其他零售商品。
     * <p>
     * 一批虾可以同时被加工成虾滑、虾丸、冷冻整虾，消费者扫其中一件的码时，
     * 应该能知道"这批虾还做成了什么"。
     * <p>
     * 这里只回零售层的公开信息（溯源码、品名、品类、门店），
     * <b>刻意不返回中间环节</b> —— 消费者没有理由看到别人家的批发商、
     * 加工厂是谁，那属于管理端的视野。
     */
    private List<Map<String, Object>> relatedProducts(TraceChain chain) {
        List<Map<String, Object>> result = new ArrayList<>();
        FarmBatch farm = chain.getFarm();
        RetaBatch self = chain.getReta();
        if (farm == null || self == null) {
            return result;
        }
        // 从养殖批号向下找到全部零售端，剔除自己
        List<Integer> ids = new ArrayList<>(traceChainLoader.resolveCandidates(farm.getBatchNo()));
        ids.removeIf(id -> id.equals(self.getRetaBatchId()));
        if (ids.isEmpty()) {
            return result;
        }
        List<RetaBatch> others = retaBatchService.listByIds(ids);
        // 批量取门店名，避免逐条查企业
        Map<Integer, String> retailerNames = new LinkedHashMap<>();
        others.stream().map(RetaBatch::getNodeId).distinct().forEach(nodeId -> {
            NodeInfo node = nodeInfoService.getById(nodeId);
            retailerNames.put(nodeId, node == null ? null : node.getName());
        });
        for (RetaBatch other : others) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("retaBatchId", other.getRetaBatchId());
            item.put("traceCode", other.getTraceCode());
            item.put("batchNo", other.getBatchNo());
            item.put("breed", other.getBreed());
            item.put("productType", other.getProductType());
            item.put("productForm", other.getProductForm());
            item.put("qualityStatus", other.getQualityStatus());
            item.put("retailerName", retailerNames.get(other.getNodeId()));
            result.add(item);
        }
        return result;
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
