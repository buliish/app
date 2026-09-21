package com.gec.seafood_traceability_system.pojo;

import lombok.Data;

/**
 * 批号的轻量引用（环节类型 + 主键 + 批号 + 企业名）
 * <p>
 * 检测记录用 {@code stageType + batchId} 多态关联批号，查询某条溯源链上
 * 全部检测记录时，需要把四个环节的引用攒成一个列表、用一次 IN 查询取回
 * （而不是逐环节各查一次）。本类就是这个"攒起来"的载体。
 */
@Data
public class BatchRef {

    /** 环节类型：1 养殖 2 冷冻加工 3 批发 4 零售 */
    private Integer stageType;

    /** 批号主键（由 stageType 决定指向哪张批号表） */
    private Integer batchId;

    /** 批号（展示用） */
    private String batchNo;

    public BatchRef() {
    }

    public BatchRef(Integer stageType, Integer batchId, String batchNo) {
        this.stageType = stageType;
        this.batchId = batchId;
        this.batchNo = batchNo;
    }
}
