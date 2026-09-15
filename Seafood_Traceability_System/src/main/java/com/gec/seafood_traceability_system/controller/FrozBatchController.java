package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.ProcessRecord;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.ProcessRecordService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 冷冻加工企业功能控制器（3.2.4）
 * 新建批号（记录上游养殖信息）/ 批号管理 / 加工工序记录 / 下游企业进场确认
 */
@RestController
@RequestMapping("/froz")
public class FrozBatchController {

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private ProcessRecordService processRecordService;

    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    /** 批号列表：1 新建 / 2 待确认 / 3 已确认 */
    @GetMapping("/batch/list")
    public Result<List<FrozBatch>> list(@RequestParam(required = false) Integer status) {
        return Result.success(frozBatchService.listByNodeAndStatus(currentNodeId(), status));
    }

    @GetMapping("/batch/{id}")
    /** 批号详情（仅限本企业自己的批号） */
    public Result<FrozBatch> detail(@PathVariable Integer id) {
        return Result.success(frozBatchService.requireOwned(id, currentNodeId()));
    }

    @GetMapping("/batch/check")
    public Result<Boolean> check(@RequestParam String batchNo) {
        return Result.success(frozBatchService.existsBatchNo(batchNo));
    }

    /** 新建产品批号（含上游养殖企业进场信息） */
    @PostMapping("/batch")
    public Result save(@RequestBody FrozBatch batch) {
        if (!StringUtils.hasText(batch.getBatchNo())) {
            return Result.error("产品批号不能为空");
        }
        if (frozBatchService.existsBatchNo(batch.getBatchNo())) {
            return Result.error("产品批号已存在");
        }
        batch.setFrozBatchId(null);
        batch.setNodeId(currentNodeId());
        batch.setStatus(1);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        frozBatchService.save(batch);
        return Result.success();
    }

    /** 更新产品批号：sendConfirm=true 时向上游发送确认请求（状态 -> 待确认） */
    @PutMapping("/batch")
    public Result update(@RequestBody FrozBatch batch, @RequestParam(required = false) Boolean sendConfirm) {
        // 归属校验：只能改自己的批号，且已确认/已下架不可再改
        frozBatchService.requireOwned(batch.getFrozBatchId(), currentNodeId(), 1);
        batch.setNodeId(currentNodeId());
        batch.setStatus(Boolean.TRUE.equals(sendConfirm) ? 2 : 1);
        batch.setUpdateTime(LocalDateTime.now());
        frozBatchService.updateById(batch);
        return Result.success();
    }

    @DeleteMapping("/batch/{id}")
    public Result delete(@PathVariable Integer id) {
        frozBatchService.requireOwned(id, currentNodeId(), 1);
        frozBatchService.removeById(id);
        return Result.success();
    }

    /** 下架产品批号 */
    @PutMapping("/batch/offline/{id}")
    public Result offline(@PathVariable Integer id) {
        frozBatchService.requireOwned(id, currentNodeId(), 3);
        frozBatchService.offline(id);
        return Result.success();
    }

    // ---------------- 加工工序（清洗 / 分级 / 冷冻 / 包装） ----------------

    /** 查询某加工批号的工序记录 */
    @GetMapping("/process/{batchId}")
    public Result<List<ProcessRecord>> processList(@PathVariable Integer batchId) {
        // 工序属于加工批号，先确认该批号是本企业的
        frozBatchService.requireOwned(batchId, currentNodeId());
        return Result.success(processRecordService.listByBatchId(batchId));
    }

    /** 新增工序记录 */
    @PostMapping("/process")
    public Result addProcess(@RequestBody ProcessRecord record) {
        if (record.getFrozBatchId() == null) {
            throw new BizException("缺少所属加工批号");
        }
        // 只能给本企业自己的批号添加工序
        frozBatchService.requireOwned(record.getFrozBatchId(), currentNodeId(), 1, 2, 3);
        record.setRecordId(null);
        record.setNodeId(currentNodeId());
        if (record.getStepTime() == null) {
            record.setStepTime(LocalDateTime.now());
        }
        processRecordService.save(record);
        return Result.success();
    }

    /** 删除工序记录 */
    @DeleteMapping("/process/{id}")
    public Result deleteProcess(@PathVariable Integer id) {
        ProcessRecord record = processRecordService.getById(id);
        if (record == null) {
            throw new BizException("工序记录不存在或已被删除");
        }
        // 工序记录本身也带 node_id，直接校验归属即可
        if (!currentNodeId().equals(record.getNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权删除其他企业的工序记录");
        }
        processRecordService.removeById(id);
        return Result.success();
    }

    // ---------------- 下游企业进场确认 ----------------

    /** 下游（批发商）进场确认列表，支持按下游企业名称模糊查询 */
    @GetMapping("/confirm/list")
    public Result<List<ConfirmVO>> confirmList(@RequestParam(required = false) String downName) {
        return Result.success(frozBatchService.listPendingConfirm(currentNodeId(), downName));
    }

    /** 确认下游企业进场 */
    @PutMapping("/confirm/{id}")
    public Result confirm(@PathVariable Integer id) {
        return frozBatchService.confirmDownstream(id, currentNodeId()) ? Result.success() : Result.error("确认失败");
    }
}
