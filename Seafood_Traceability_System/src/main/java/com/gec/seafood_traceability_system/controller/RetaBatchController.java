package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 零售商功能控制器（3.2.6）
 * 已确认的零售批号由系统自动生成溯源标识码
 */
@RestController
@RequestMapping("/reta")
public class RetaBatchController {

    @Autowired
    private RetaBatchService retaBatchService;

    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    @GetMapping("/batch/list")
    public Result<List<RetaBatch>> list(@RequestParam(required = false) Integer status) {
        return Result.success(retaBatchService.listByNodeAndStatus(currentNodeId(), status));
    }

    @GetMapping("/batch/{id}")
    public Result<RetaBatch> detail(@PathVariable Integer id) {
        return Result.success(retaBatchService.getById(id));
    }

    @GetMapping("/batch/check")
    public Result<Boolean> check(@RequestParam String batchNo) {
        return Result.success(retaBatchService.existsBatchNo(batchNo));
    }

    /** 新建产品批号（含上游批发商进场信息） */
    @PostMapping("/batch")
    public Result save(@RequestBody RetaBatch batch) {
        if (!StringUtils.hasText(batch.getBatchNo())) {
            return Result.error("产品批号不能为空");
        }
        if (retaBatchService.existsBatchNo(batch.getBatchNo())) {
            return Result.error("产品批号已存在");
        }
        batch.setRetaBatchId(null);
        batch.setNodeId(currentNodeId());
        batch.setStatus(1);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        retaBatchService.save(batch);
        return Result.success();
    }

    /** 更新产品批号：sendConfirm=true 时向上游发送确认请求 */
    @PutMapping("/batch")
    public Result update(@RequestBody RetaBatch batch, @RequestParam(required = false) Boolean sendConfirm) {
        batch.setNodeId(currentNodeId());
        batch.setStatus(Boolean.TRUE.equals(sendConfirm) ? 2 : 1);
        batch.setUpdateTime(LocalDateTime.now());
        //批号更新不允许修改溯源标识码
        batch.setTraceCode(null);
        retaBatchService.updateById(batch);
        return Result.success();
    }

    @DeleteMapping("/batch/{id}")
    public Result delete(@PathVariable Integer id) {
        retaBatchService.removeById(id);
        return Result.success();
    }

    @PutMapping("/batch/offline/{id}")
    public Result offline(@PathVariable Integer id) {
        retaBatchService.offline(id);
        return Result.success();
    }
}
