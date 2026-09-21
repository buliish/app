package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.InspectionMapper;
import com.gec.seafood_traceability_system.pojo.BatchRef;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.Inspection;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 各环节检测记录业务实现 */
@Service
public class InspectionServiceImpl extends ServiceImpl<InspectionMapper, Inspection> implements InspectionService {

    /** 质量状态取值：0 待检 / 1 合格 / 2 不合格 */
    private static final int QUALITY_PENDING = 0;
    private static final int QUALITY_PASS = 1;
    private static final int QUALITY_FAIL = 2;

    /** 单项判定取值 */
    private static final int RESULT_FAIL = 2;

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    @Autowired
    private RetaBatchService retaBatchService;

    @Override
    public List<Inspection> listByBatch(Integer stageType, Integer batchId) {
        if (stageType == null || batchId == null) {
            return new ArrayList<>();
        }
        return lambdaQuery()
                .eq(Inspection::getStageType, stageType)
                .eq(Inspection::getBatchId, batchId)
                .orderByAsc(Inspection::getReportNo)
                .orderByAsc(Inspection::getInspectionId)
                .list();
    }

    @Override
    public List<Inspection> listByRefs(List<BatchRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return new ArrayList<>();
        }
        // 过滤掉环节或主键缺失的引用，避免拼出 (stage_type = null) 这种恒假条件
        List<BatchRef> valid = refs.stream()
                .filter(r -> r != null && r.getStageType() != null && r.getBatchId() != null)
                .toList();
        if (valid.isEmpty()) {
            return new ArrayList<>();
        }
        return baseMapper.selectByRefs(valid);
    }

    @Override
    public int saveBatchRecords(List<Inspection> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        // 走 XML 的 foreach 多值 INSERT，一次往返写完整套检测项
        return baseMapper.insertBatch(records);
    }

    @Override
    public void refreshBatchQuality(Integer stageType, Integer batchId) {
        if (stageType == null || batchId == null) {
            return;
        }
        // 有不合格项 → 整批不合格；否则有检测记录 → 合格；无记录 → 待检
        long total = lambdaQuery()
                .eq(Inspection::getStageType, stageType)
                .eq(Inspection::getBatchId, batchId)
                .count();
        long fails = lambdaQuery()
                .eq(Inspection::getStageType, stageType)
                .eq(Inspection::getBatchId, batchId)
                .eq(Inspection::getResult, RESULT_FAIL)
                .count();

        int quality = total == 0 ? QUALITY_PENDING : (fails > 0 ? QUALITY_FAIL : QUALITY_PASS);
        writeQuality(stageType, batchId, quality);
    }

    /**
     * 把质量状态写回对应环节的批号表。
     * <p>
     * 只 new 一个设了主键 + qualityStatus 的实体走 updateById ——
     * MyBatis-Plus 默认忽略 null 字段，天然形成"只改这一列"的窄更新，
     * 不会误伤批号的其他字段。
     */
    private void writeQuality(Integer stageType, Integer batchId, int quality) {
        switch (stageType) {
            case 1 -> {
                FarmBatch b = new FarmBatch();
                b.setFarmBatchId(batchId);
                b.setQualityStatus(quality);
                farmBatchService.updateById(b);
            }
            case 2 -> {
                FrozBatch b = new FrozBatch();
                b.setFrozBatchId(batchId);
                b.setQualityStatus(quality);
                frozBatchService.updateById(b);
            }
            case 3 -> {
                WholBatch b = new WholBatch();
                b.setWholBatchId(batchId);
                b.setQualityStatus(quality);
                wholBatchService.updateById(b);
            }
            case 4 -> {
                RetaBatch b = new RetaBatch();
                b.setRetaBatchId(batchId);
                b.setQualityStatus(quality);
                retaBatchService.updateById(b);
            }
            default -> {
                // 未知环节不写，由调用方的 requireOwned 提前拦下
            }
        }
    }
}
