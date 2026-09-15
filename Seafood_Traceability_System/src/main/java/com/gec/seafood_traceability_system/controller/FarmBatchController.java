package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 养殖企业功能控制器（3.2.3）
 * 新建批号 / 批号管理（浏览、更新、删除、下架、详情）/ 下游企业进场确认
 */
@RestController
@RequestMapping("/farm")
public class FarmBatchController {

    @Autowired
    private FarmBatchService farmBatchService;

    /** 当前登录企业ID（取自 token） */
    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    /** 批号列表：status 传 1 待发布 / 2 已发布，不传则返回全部可浏览批号 */
    @GetMapping("/batch/list")
    public Result<List<FarmBatch>> list(@RequestParam(required = false) Integer status) {
        return Result.success(farmBatchService.listByNodeAndStatus(currentNodeId(), status));
    }

    /** 批号详情（仅限本企业自己的批号） */
    @GetMapping("/batch/{id}")
    public Result<FarmBatch> detail(@PathVariable Integer id) {
        return Result.success(farmBatchService.requireOwned(id, currentNodeId()));
    }

    /** 批号blur校验：返回 true 表示批号已存在 */
    @GetMapping("/batch/check")
    public Result<Boolean> check(@RequestParam String batchNo) {
        return Result.success(farmBatchService.existsBatchNo(batchNo));
    }

    /** 新建产品批号（初始状态：待发布） */
    @PostMapping("/batch")
    public Result save(@RequestBody FarmBatch batch) {
        if (!StringUtils.hasText(batch.getBatchNo())) {
            return Result.error("产品批号不能为空");
        }
        if (farmBatchService.existsBatchNo(batch.getBatchNo())) {
            return Result.error("产品批号已存在");
        }
        batch.setFarmBatchId(null);
        batch.setNodeId(currentNodeId());
        batch.setStatus(1);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        farmBatchService.save(batch);
        return Result.success();
    }

    /** 更新产品批号：publish=true 时同时将状态更新为已发布 */
    @PutMapping("/batch")
    public Result update(@RequestBody FarmBatch batch, @RequestParam(required = false) Boolean publish) {
        // 归属校验 + 已下架不可修改
        farmBatchService.requireOwned(batch.getFarmBatchId(), currentNodeId(), 1, 2);
        batch.setNodeId(currentNodeId());
        batch.setStatus(Boolean.TRUE.equals(publish) ? 2 : 1);
        batch.setUpdateTime(LocalDateTime.now());
        farmBatchService.updateById(batch);
        return Result.success();
    }

    /** 删除产品批号（仅限待发布状态） */
    @DeleteMapping("/batch/{id}")
    public Result delete(@PathVariable Integer id) {
        farmBatchService.requireOwned(id, currentNodeId(), 1);
        farmBatchService.removeById(id);
        return Result.success();
    }

    /** 下架产品批号（仅限已发布状态） */
    @PutMapping("/batch/offline/{id}")
    public Result offline(@PathVariable Integer id) {
        farmBatchService.requireOwned(id, currentNodeId(), 2);
        farmBatchService.offline(id);
        return Result.success();
    }

    /** 下游（冷冻加工企业）进场确认列表，支持按下游企业名称模糊查询 */
    @GetMapping("/confirm/list")
    public Result<List<ConfirmVO>> confirmList(@RequestParam(required = false) String downName) {
        return Result.success(farmBatchService.listPendingConfirm(currentNodeId(), downName));
    }

    /** 确认下游企业进场 */
    @PutMapping("/confirm/{id}")
    public Result confirm(@PathVariable Integer id) {
        return farmBatchService.confirmDownstream(id, currentNodeId())
                ? Result.success() : Result.error("确认失败");
    }
}
