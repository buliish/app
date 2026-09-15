package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.InspectionRecord;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 各环节检测记录控制器（四个角色共用）。
 * <p>
 * 四个环节的检测记录结构一致，只是挂在不同的批号表上，因此共用一套接口，
 * 由 {@code stageType} 区分（取值与 nodeType 一致：1养殖 2冷冻加工 3批发 4零售）。
 * <p>
 * 归属约束在 Service 层完成（记录挂靠的批号必须属于当前登录企业，删除也只限本企业记录），
 * 本类只负责取当前登录企业编号并转交。
 */
@RestController
@RequestMapping("/inspection")
public class InspectionController {

    @Autowired
    private InspectionService inspectionService;

    private Integer currentNodeId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }

    /** 查询某条批号下的全部检测记录 */
    @GetMapping("/list")
    public Result<List<InspectionRecord>> list(@RequestParam Integer stageType,
                                               @RequestParam Integer batchId) {
        return Result.success(inspectionService.listByStage(stageType, batchId));
    }

    /** 新增一条检测记录（一份报告里的单个检测项） */
    @PostMapping
    public Result save(@RequestBody InspectionRecord record) {
        record.setInspectionId(null);
        record.setNodeId(currentNodeId());
        inspectionService.addRecord(record);
        return Result.success();
    }

    /** 批量新增检测记录（一份检测报告的多个检测项一次提交） */
    @PostMapping("/batch")
    public Result saveBatch(@RequestBody List<InspectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return Result.error("检测记录不能为空");
        }
        //逐条补上录入企业，Service 会逐条校验批号是否属于本企业
        Integer nodeId = currentNodeId();
        records.forEach(r -> {
            r.setInspectionId(null);
            r.setNodeId(nodeId);
        });
        return Result.success(inspectionService.addRecords(records));
    }

    /** 删除检测记录（仅限本企业录入的记录） */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        inspectionService.requireOwned(id, currentNodeId());
        inspectionService.deleteRecord(id);
        return Result.success();
    }
}
