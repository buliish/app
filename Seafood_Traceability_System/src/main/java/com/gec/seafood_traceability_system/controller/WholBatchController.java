package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.WholBatchService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批发商功能控制器（3.2.5）
 */
@RestController
@RequestMapping("/whol")
public class WholBatchController {

    @Autowired
    private WholBatchService wholBatchService;

    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    @GetMapping("/batch/list")
    public Result<List<WholBatch>> list(@RequestParam(required = false) Integer status) {
        return Result.success(wholBatchService.listByNodeAndStatus(currentNodeId(), status));
    }

    @GetMapping("/batch/{id}")
    /** 批号详情（仅限本企业自己的批号） */
    public Result<WholBatch> detail(@PathVariable Integer id) {
        return Result.success(wholBatchService.requireOwned(id, currentNodeId()));
    }

    @GetMapping("/batch/check")
    public Result<Boolean> check(@RequestParam String batchNo) {
        return Result.success(wholBatchService.existsBatchNo(batchNo));
    }

    /** 新建产品批号（含上游冷冻加工企业进场信息） */
    @PostMapping("/batch")
    public Result save(@RequestBody WholBatch batch) {
        if (!StringUtils.hasText(batch.getBatchNo())) {
            return Result.error("产品批号不能为空");
        }
        if (wholBatchService.existsBatchNo(batch.getBatchNo())) {
            return Result.error("产品批号已存在");
        }
        batch.setWholBatchId(null);
        batch.setNodeId(currentNodeId());
        batch.setStatus(1);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        wholBatchService.saveWithQuantityCheck(batch);
        return Result.success();
    }

    /** 更新产品批号：sendConfirm=true 时向上游发送确认请求 */
    @PutMapping("/batch")
    public Result update(@RequestBody WholBatch batch, @RequestParam(required = false) Boolean sendConfirm) {
        // 归属校验：只能改自己的批号，且已确认/已下架不可再改
        wholBatchService.requireOwned(batch.getWholBatchId(), currentNodeId(), 1);
        batch.setNodeId(currentNodeId());
        batch.setStatus(Boolean.TRUE.equals(sendConfirm) ? 2 : 1);
        batch.setUpdateTime(LocalDateTime.now());
        wholBatchService.updateWithQuantityCheck(batch);
        return Result.success();
    }

    @DeleteMapping("/batch/{id}")
    public Result delete(@PathVariable Integer id) {
        wholBatchService.requireOwned(id, currentNodeId(), 1);
        wholBatchService.removeById(id);
        return Result.success();
    }

    @PutMapping("/batch/offline/{id}")
    public Result offline(@PathVariable Integer id) {
        wholBatchService.requireOwned(id, currentNodeId(), 3);
        wholBatchService.offline(id);
        return Result.success();
    }

    /** 下游（零售商）进场确认列表 */
    @GetMapping("/confirm/list")
    public Result<List<ConfirmVO>> confirmList(@RequestParam(required = false) String downName) {
        return Result.success(wholBatchService.listPendingConfirm(currentNodeId(), downName));
    }

    /** 确认下游企业进场（零售批号置为已确认并生成溯源标识码） */
    @PutMapping("/confirm/{id}")
    public Result confirm(@PathVariable Integer id) {
        return wholBatchService.confirmDownstream(id, currentNodeId()) ? Result.success() : Result.error("确认失败");
    }
}
