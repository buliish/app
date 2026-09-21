package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.BizException;
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
    /** 批号详情（仅限本企业自己的批号） */
    public Result<RetaBatch> detail(@PathVariable Integer id) {
        return Result.success(retaBatchService.requireOwned(id, currentNodeId()));
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
        retaBatchService.saveWithQuantityCheck(batch);
        return Result.success();
    }

    /** 更新产品批号：sendConfirm=true 时向上游发送确认请求 */
    @PutMapping("/batch")
    public Result update(@RequestBody RetaBatch batch, @RequestParam(required = false) Boolean sendConfirm) {
        // 归属校验：只能改自己的批号
        RetaBatch exist = retaBatchService.requireOwned(batch.getRetaBatchId(), currentNodeId());
        // 仅"新建"状态可改。若允许已确认(3)的批号改回待确认(2)，
        // 会出现"已生成溯源码却回到待确认"的脏状态，且上游会重复收到确认请求
        if (exist.getStatus() != null && exist.getStatus() == 3) {
            throw new BizException("批号已确认，溯源标识码 " + exist.getTraceCode() + " 已生效，不可再修改");
        }
        if (exist.getStatus() != null && exist.getStatus() == 4) {
            throw new BizException("批号已下架，不可再修改");
        }
        batch.setNodeId(currentNodeId());
        batch.setStatus(Boolean.TRUE.equals(sendConfirm) ? 2 : 1);
        batch.setUpdateTime(LocalDateTime.now());
        //批号更新不允许修改溯源标识码
        batch.setTraceCode(null);
        retaBatchService.updateWithQuantityCheck(batch);
        return Result.success();
    }

    @DeleteMapping("/batch/{id}")
    public Result delete(@PathVariable Integer id) {
        retaBatchService.requireOwned(id, currentNodeId(), 1);
        retaBatchService.removeById(id);
        return Result.success();
    }

    @PutMapping("/batch/offline/{id}")
    public Result offline(@PathVariable Integer id) {
        retaBatchService.requireOwned(id, currentNodeId(), 3);
        retaBatchService.offline(id);
        return Result.success();
    }
}
