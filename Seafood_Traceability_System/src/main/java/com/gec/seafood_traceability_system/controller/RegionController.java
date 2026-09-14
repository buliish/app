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
                    .forEach(b -> list.add(batchMap(b.getBatchNo(), b.getBreed(), b.getBreedStage(), null)));
            case 2 -> frozBatchService.lambdaQuery()
                    .eq(FrozBatch::getNodeId, nodeId)
                    .eq(FrozBatch::getStatus, 3)
                    .orderByDesc(FrozBatch::getCreateTime)
                    .list()
                    .forEach(b -> list.add(batchMap(b.getBatchNo(), b.getBreed(), null, b.getProductType())));
            case 3 -> wholBatchService.lambdaQuery()
                    .eq(WholBatch::getNodeId, nodeId)
                    .eq(WholBatch::getStatus, 3)
                    .orderByDesc(WholBatch::getCreateTime)
                    .list()
                    .forEach(b -> list.add(batchMap(b.getBatchNo(), b.getBreed(), null, b.getProductType())));
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
}
