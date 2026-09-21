package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Inspection;
import com.gec.seafood_traceability_system.pojo.NodeOwned;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.OwnedBatchRegistry;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各环节检测记录控制器
 * <p>
 * 四个环节（养殖/加工/批发/零售）共用同一套接口，用 stageType 区分，
 * 归属校验统一委托给 {@link OwnedBatchRegistry}（内部复用各批号 Service
 * 已实现的 requireOwned），因此本类不出现任何表名分支。
 * <p>
 * 鉴权：挂在 /inspection/** 下，已在 WebMvcConfig 追加到节点端拦截器，
 * 必须携带企业 token。
 */
@RestController
@RequestMapping("/inspection")
public class InspectionController {

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private OwnedBatchRegistry registry;

    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    /** 查询某批号的检测记录（只允许查本企业自己的批号） */
    @GetMapping("/list")
    public Result<List<Inspection>> list(@RequestParam Integer stageType, @RequestParam Integer batchId) {
        registry.requireOwned(stageType, batchId, currentNodeId());
        return Result.success(inspectionService.listByBatch(stageType, batchId));
    }

    /** 新增单条检测记录 */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result add(@RequestBody Inspection inspection) {
        NodeOwned owned = prepare(inspection, currentNodeId());
        inspectionService.save(inspection);
        inspectionService.refreshBatchQuality(inspection.getStageType(), inspection.getBatchId());
        return Result.success();
    }

    /**
     * 批量新增检测记录：一次录入一份报告的多个检测项。
     * 同批提交必须属于同一个批号。
     */
    @PostMapping("/batch")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> addBatch(@RequestBody List<Inspection> records) {
        if (records == null || records.isEmpty()) {
            throw new BizException("请至少录入一条检测记录");
        }
        Integer stageType = records.get(0).getStageType();
        Integer batchId = records.get(0).getBatchId();
        if (stageType == null || batchId == null) {
            throw new BizException("缺少环节类型或批号");
        }
        boolean sameBatch = records.stream()
                .allMatch(r -> stageType.equals(r.getStageType()) && batchId.equals(r.getBatchId()));
        if (!sameBatch) {
            throw new BizException("一次只能提交同一个批号的检测记录");
        }

        Integer current = currentNodeId();
        for (Inspection r : records) {
            prepare(r, current);
        }
        int inserted = inspectionService.saveBatchRecords(records);
        inspectionService.refreshBatchQuality(stageType, batchId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inserted", inserted);
        return Result.success(data);
    }

    /** 删除检测记录 */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result delete(@PathVariable Integer id) {
        Inspection exist = inspectionService.getById(id);
        if (exist == null) {
            throw new BizException("检测记录不存在或已被删除");
        }
        // 检测记录没有 status 字段，NodeOwned.getStatus() 返回 null，
        // 因此这里用不带状态白名单的归属校验重载
        if (!currentNodeId().equals(exist.getNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权删除其他企业的检测记录");
        }
        inspectionService.removeById(id);
        inspectionService.refreshBatchQuality(exist.getStageType(), exist.getBatchId());
        return Result.success();
    }

    /**
     * 写入前的统一处理：校验归属 + 回填冗余列 + 清除前端可伪造的字段。
     * <p>
     * nodeId 与 batchNo 必须由后端从批号实体回填，绝不接受前端传入 ——
     * 否则可以把检测记录挂到别人的批号上（伪造 batchNo 绕过列表校验）。
     */
    private NodeOwned prepare(Inspection inspection, Integer currentNodeId) {
        if (inspection.getStageType() == null || inspection.getBatchId() == null) {
            throw new BizException("缺少环节类型或批号");
        }
        if (inspection.getItemName() == null || inspection.getItemName().isBlank()) {
            throw new BizException("请填写检测项目");
        }
        NodeOwned owned = registry.requireOwnedForInspection(
                inspection.getStageType(), inspection.getBatchId(), currentNodeId);

        inspection.setInspectionId(null);
        inspection.setNodeId(currentNodeId);
        inspection.setBatchNo(owned.getBatchNo());
        if (inspection.getCreateTime() == null) {
            inspection.setCreateTime(LocalDateTime.now());
        }
        return owned;
    }
}
