package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.InspectionMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.InspectionRecord;
import com.gec.seafood_traceability_system.pojo.InspectionRef;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.TraceChain;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.InspectionService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检测记录业务实现。
 * <p>
 * 核心职责有两个：按环节/批号读写记录，以及在记录变动后
 * <b>重算所属批号的质量状态</b>——批号的 qualityStatus 是检测结果的派生值，
 * 消费者端的"在售商品"过滤和商品卡上的质量标签都读它，
 * 所以不能只写检测记录而不回写批号。
 */
@Service
public class InspectionServiceImpl extends ServiceImpl<InspectionMapper, InspectionRecord>
        implements InspectionService {

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    @Autowired
    private RetaBatchService retaBatchService;

    @Override
    public List<InspectionRecord> listByStage(Integer stageType, Integer batchId) {
        if (stageType == null || batchId == null) {
            return new ArrayList<>();
        }
        return lambdaQuery()
                .eq(InspectionRecord::getStageType, stageType)
                .eq(InspectionRecord::getBatchId, batchId)
                .orderByAsc(InspectionRecord::getInspectionId)
                .list();
    }

    @Override
    public List<InspectionRecord> listByRefs(List<InspectionRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return new ArrayList<>();
        }
        //按环节分组，一条链路最多四级 → 最多 4 次查询，而不是每个锚点查一次
        Map<Integer, List<Integer>> idsByStage = new LinkedHashMap<>();
        for (InspectionRef ref : refs) {
            if (ref == null || ref.getStageType() == null || ref.getBatchId() == null) {
                continue;
            }
            idsByStage.computeIfAbsent(ref.getStageType(), k -> new ArrayList<>()).add(ref.getBatchId());
        }
        List<InspectionRecord> result = new ArrayList<>();
        idsByStage.forEach((stageType, batchIds) -> result.addAll(
                lambdaQuery()
                        .eq(InspectionRecord::getStageType, stageType)
                        .in(InspectionRecord::getBatchId, batchIds)
                        .orderByAsc(InspectionRecord::getInspectionId)
                        .list()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRecord(InspectionRecord record) {
        validate(record);
        record.setInspectionId(null);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        save(record);
        refreshBatchQuality(record.getStageType(), record.getBatchId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addRecords(List<InspectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (InspectionRecord record : records) {
            validate(record);
            record.setInspectionId(null);
            record.setCreateTime(now);
            record.setUpdateTime(now);
        }
        //数据层批量插入：foreach 拼多值 INSERT，一次往返写完一份报告的全部检测项
        int rows = baseMapper.insertBatch(records);
        //同一次提交里可能有多条记录挂同一个批号，去重后只重算一次
        records.stream()
                .map(r -> new InspectionRef(r.getStageType(), r.getBatchId()))
                .distinct()
                .forEach(ref -> refreshBatchQuality(ref.getStageType(), ref.getBatchId()));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Integer inspectionId) {
        InspectionRecord exist = getById(inspectionId);
        if (exist == null) {
            throw new BizException("检测记录不存在或已被删除");
        }
        removeById(inspectionId);
        //删掉记录可能让"不合格"的结论消失，质量状态要跟着回退
        refreshBatchQuality(exist.getStageType(), exist.getBatchId());
    }

    /** 必填项校验：环节、批号、检测项目缺一不可，且批号必须属于本企业 */
    private void validate(InspectionRecord record) {
        if (record.getStageType() == null || record.getBatchId() == null) {
            throw new BizException("缺少环节类型或批号编号");
        }
        if (!StringUtils.hasText(record.getItemName())) {
            throw new BizException("检测项目名称不能为空");
        }
        assertBatchOwnedByCurrentNode(record);
    }

    /**
     * 校验检测记录挂靠的批号确实属于本企业。
     * <p>
     * 复用各批号 Service 的 {@code requireOwned}：记录只有挂在自家批号上才有意义，
     * 否则会出现"给别家批号补录检测结果"的越权写入。
     */
    private void assertBatchOwnedByCurrentNode(InspectionRecord record) {
        if (record.getNodeId() == null) {
            throw new BizException(Result.CODE_FORBIDDEN, "缺少企业信息，请重新登录");
        }
        switch (record.getStageType()) {
            case TraceChain.STAGE_FARM ->
                    farmBatchService.requireOwned(record.getBatchId(), record.getNodeId());
            case TraceChain.STAGE_FROZ ->
                    frozBatchService.requireOwned(record.getBatchId(), record.getNodeId());
            case TraceChain.STAGE_WHOL ->
                    wholBatchService.requireOwned(record.getBatchId(), record.getNodeId());
            case TraceChain.STAGE_RETA ->
                    retaBatchService.requireOwned(record.getBatchId(), record.getNodeId());
            default -> throw new BizException("未知的环节类型：" + record.getStageType());
        }
    }

    /** 重算并回写某条批号的质量状态 */
    private void refreshBatchQuality(Integer stageType, Integer batchId) {
        if (stageType == null || batchId == null) {
            return;
        }
        Integer quality = computeQuality(listByStage(stageType, batchId));
        writeBackQuality(stageType, batchId, quality);
    }

    /**
     * 由检测记录推导批号质量状态。
     * <p>
     * 只要有一条"不合格"结论，整批即不合格；否则有任意一条"合格"结论即为合格；
     * 一条有结论的都没有则保持待检。取"最差档"而不是"最后一条"，
     * 是为了避免后录入的合格项把先前的不合格项盖掉。
     */
    private Integer computeQuality(List<InspectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return TraceChain.QUALITY_PENDING;
        }
        boolean anyRejected = records.stream().anyMatch(
                r -> r.getConclusion() != null && r.getConclusion() == InspectionRecord.RESULT_REJECTED);
        if (anyRejected) {
            return TraceChain.QUALITY_REJECTED;
        }
        boolean anyPassed = records.stream().anyMatch(
                r -> r.getConclusion() != null && r.getConclusion() == InspectionRecord.RESULT_PASSED);
        return anyPassed ? TraceChain.QUALITY_PASSED : TraceChain.QUALITY_PENDING;
    }

    /**
     * 按环节把质量状态写回对应的批号表。
     * <p>
     * 四类批号的主键名与实体类型都不同，MyBatis-Plus 的 lambda 更新无法泛型化，
     * 这里按环节分派。新增环节时在这里补一个分支即可。
     */
    private void writeBackQuality(Integer stageType, Integer batchId, Integer quality) {
        switch (stageType) {
            case TraceChain.STAGE_FARM -> farmBatchService.lambdaUpdate()
                    .eq(FarmBatch::getFarmBatchId, batchId)
                    .set(FarmBatch::getQualityStatus, quality)
                    .update();
            case TraceChain.STAGE_FROZ -> frozBatchService.lambdaUpdate()
                    .eq(FrozBatch::getFrozBatchId, batchId)
                    .set(FrozBatch::getQualityStatus, quality)
                    .update();
            case TraceChain.STAGE_WHOL -> wholBatchService.lambdaUpdate()
                    .eq(WholBatch::getWholBatchId, batchId)
                    .set(WholBatch::getQualityStatus, quality)
                    .update();
            case TraceChain.STAGE_RETA -> retaBatchService.lambdaUpdate()
                    .eq(RetaBatch::getRetaBatchId, batchId)
                    .set(RetaBatch::getQualityStatus, quality)
                    .update();
            default -> throw new BizException("未知的环节类型：" + stageType);
        }
    }
}
