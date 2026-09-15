package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.InspectionRecord;
import com.gec.seafood_traceability_system.pojo.InspectionRef;

import java.util.List;

/**
 * 检测记录业务接口（四环节共用）。
 * <p>
 * 继承 {@link OwnedBatchService} 以获得统一的归属校验：
 * 检测记录的删除必须限制在"本企业自己的"记录上。
 */
public interface InspectionService extends OwnedBatchService<InspectionRecord> {

    /** 查询某个环节某条批号下的全部检测记录 */
    List<InspectionRecord> listByStage(Integer stageType, Integer batchId);

    /**
     * 一次取全多条锚点的检测记录。
     * <p>
     * 消费者端溯源时一条链路最多四级，逐级查会造成 4 次往返，
     * 故按环节分组后用 IN 批量取回。
     *
     * @param refs 链路各级的检测锚点，可为空
     */
    List<InspectionRecord> listByRefs(List<InspectionRef> refs);

    /**
     * 新增一条检测记录。
     * <p>
     * 写入后重算所属批号的质量状态（见 {@code refreshBatchQuality}）。
     *
     * @param record 待写入记录，batchId / stageType / nodeId 由调用方补齐
     */
    void addRecord(InspectionRecord record);

    /**
     * 批量新增检测记录（同一份报告多个检测项）。
     * <p>
     * 走数据层批量插入，并在最后统一重算一次批号质量状态。
     *
     * @param records 非空列表
     * @return 实际写入行数
     */
    int addRecords(List<InspectionRecord> records);

    /**
     * 删除检测记录，并重算所属批号的质量状态。
     *
     * @param inspectionId 记录主键
     */
    void deleteRecord(Integer inspectionId);
}
