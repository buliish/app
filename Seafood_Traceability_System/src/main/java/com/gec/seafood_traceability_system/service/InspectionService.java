package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.BatchRef;
import com.gec.seafood_traceability_system.pojo.Inspection;

import java.util.List;

/** 各环节检测记录业务接口 */
public interface InspectionService extends IService<Inspection> {

    /** 查询某个批号的全部检测记录（按报告号、主键排序） */
    List<Inspection> listByBatch(Integer stageType, Integer batchId);

    /** 一次取回整条溯源链（最多 4 个环节）的全部检测记录 */
    List<Inspection> listByRefs(List<BatchRef> refs);

    /**
     * 批量新增检测记录（数据层批量操作，见 InspectionMapper.xml 的 foreach）
     *
     * @param records 检测项列表，调用方需保证已校验归属并回填 nodeId/batchNo
     * @return 实际插入条数
     */
    int saveBatchRecords(List<Inspection> records);

    /**
     * 重算并写回某批号的 quality_status（0 待检 / 1 合格 / 2 不合格）。
     * <p>
     * 这是 inspection 明细表与批号冗余字段之间<b>唯一</b>的一致性维护点：
     * 取该批号下所有检测项的最差结论 —— 只要有一条不合格，整批即为不合格；
     * 否则只要有记录就算合格；一条都没有则回到待检。
     *
     * @param stageType 环节类型
     * @param batchId   批号主键
     */
    void refreshBatchQuality(Integer stageType, Integer batchId);
}
