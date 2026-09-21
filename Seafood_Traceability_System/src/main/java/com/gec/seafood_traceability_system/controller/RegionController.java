package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.City;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.Province;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.CityService;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.ProvinceService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.gec.seafood_traceability_system.service.BatchQuantityService;
import java.math.BigDecimal;

/**
 * 行政区域与上游企业联动接口
 * 供加工/批发/零售企业新建批号时逐级选择：省 -> 市 -> 上游企业 -> 上游批号 -> 品种
 */
@RestController
@RequestMapping("/region")
public class RegionController {

    @Autowired
    private ProvinceService provinceService;

    @Autowired
    private CityService cityService;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    /** 计算上游批号的剩余可领用量（上游批号下拉要显示它） */
    @Autowired
    private BatchQuantityService batchQuantityService;

    /** 查询全部省 */
    @GetMapping("/provinces")
    public Result<List<Province>> provinces() {
        return Result.success(provinceService.lambdaQuery().orderByAsc(Province::getProvId).list());
    }

    /** 查询指定省下的市 */
    @GetMapping("/cities")
    public Result<List<City>> cities(@RequestParam(required = false) Integer provId) {
        return Result.success(cityService.listByProvId(provId));
    }

    /** 查询指定类型、指定区域下的企业（上游企业下拉列表） */
    @GetMapping("/nodes")
    public Result<List<NodeInfo>> nodes(@RequestParam Integer type,
                                        @RequestParam(required = false) Integer provId,
                                        @RequestParam(required = false) Integer cityId) {
        return Result.success(nodeInfoService.listByTypeAndRegion(type, provId, cityId));
    }

    /**
     * 查询指定企业可流通的上游批号列表
     * 养殖企业取“已发布”批号，加工/批发企业取“已确认”批号
     */
    @GetMapping("/batches")
    public Result<List<Map<String, Object>>> batches(@RequestParam Integer nodeId) {
        NodeInfo node = nodeInfoService.getById(nodeId);
        if (node == null) {
            return Result.error("企业不存在");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        switch (node.getNodeType()) {
            case 1 -> farmBatchService.lambdaQuery()
                    .eq(FarmBatch::getNodeId, nodeId)
                    .eq(FarmBatch::getStatus, 2)
                    .orderByDesc(FarmBatch::getCreateTime)
                    .list()
                    .forEach(b -> {
                        Map<String, Object> m = batchMap(b.getBatchNo(), b.getBreed(), b.getBreedStage(), null);
                        m.put("productForm", b.getProductForm());
                        m.put("sourceType", b.getSourceType());
                        list.add(withQuantity(m, BatchQuantityService.STAGE_FARM,
                                b.getBatchNo(), b.getNodeId(), b.getQuantityKg()));
                    });
            case 2 -> frozBatchService.lambdaQuery()
                    .eq(FrozBatch::getNodeId, nodeId)
                    .eq(FrozBatch::getStatus, 3)
                    .orderByDesc(FrozBatch::getCreateTime)
                    .list()
                    .forEach(b -> {
                        Map<String, Object> m = batchMap(b.getBatchNo(), b.getBreed(), null, b.getProductType());
                        // 形态与规格由加工环节定型，下游新建批号时带出并锁定
                        m.put("productForm", b.getProductForm());
                        m.put("specGrade", b.getSpecGrade());
                        m.put("productCode", b.getProductCode());
                        list.add(withQuantity(m, BatchQuantityService.STAGE_FROZ,
                                b.getBatchNo(), b.getNodeId(), b.getQuantityKg()));
                    });
            case 3 -> wholBatchService.lambdaQuery()
                    .eq(WholBatch::getNodeId, nodeId)
                    .eq(WholBatch::getStatus, 3)
                    .orderByDesc(WholBatch::getCreateTime)
                    .list()
                    .forEach(b -> {
                        Map<String, Object> m = batchMap(b.getBatchNo(), b.getBreed(), null, b.getProductType());
                        m.put("productForm", b.getProductForm());
                        m.put("specGrade", b.getSpecGrade());
                        m.put("productCode", b.getProductCode());
                        list.add(withQuantity(m, BatchQuantityService.STAGE_WHOL,
                                b.getBatchNo(), b.getNodeId(), b.getQuantityKg()));
                    });
            default -> {
                return Result.error("该企业类型没有上游批号");
            }
        }
        return Result.success(list);
    }

    private Map<String, Object> batchMap(String batchNo, String breed, String breedStage, String productType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("batchNo", batchNo);
        map.put("breed", breed);
        map.put("breedStage", breedStage);
        map.put("productType", productType);
        return map;
    }

    /**
     * 给上游批号的返回项补上「总量 / 剩余可领量」。
     * <p>
     * 前端下拉据此显示"FARM20260101（南美白对虾 / 剩余 1200kg）"，
     * 并把已领完的批号（remainingKg = 0）置灰或不列出 ——
     * 否则用户要提交后才被超领校验拦下，白填一遍表。
     *
     * @param upStageType 上游环节类型（本批号属于哪一环）
     */
    private Map<String, Object> withQuantity(Map<String, Object> item, int upStageType,
                                             String batchNo, Integer upNodeId, BigDecimal quantityKg) {
        item.put("quantityKg", quantityKg);
        // 领用校验要看的是"下游表"里的领用量，故换算成下游环节类型
        BigDecimal remaining = batchQuantityService.remaining(upStageType + 1, batchNo, upNodeId);
        item.put("remainingKg", remaining);
        // remainingKg 为 null 表示上游未登记数量，不限制领用，不算领完
        item.put("soldOut", remaining != null && remaining.signum() <= 0);
        return item;
    }
}
