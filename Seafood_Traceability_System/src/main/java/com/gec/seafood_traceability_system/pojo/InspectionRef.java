package com.gec.seafood_traceability_system.pojo;

import java.util.Objects;

/**
 * 一条检测记录的归属锚点。
 * <p>
 * 四个环节的批号分散在四张表里，检测记录共用一张 {@code inspection_record}，
 * 靠 {@code (stageType, batchId)} 定位"这条记录挂在哪一类批号的哪一条上"。
 * 顺带带上企业编号，供录入/删除时做归属校验。
 */
public class InspectionRef {

    /** 环节类型：1 养殖 2 冷冻加工 3 批发 4 零售（与 node_info.type 取值一致） */
    private final Integer stageType;

    /** 该环节批号表的主键 */
    private final Integer batchId;

    public InspectionRef(Integer stageType, Integer batchId) {
        this.stageType = stageType;
        this.batchId = batchId;
    }

    public Integer getStageType() {
        return stageType;
    }

    public Integer getBatchId() {
        return batchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InspectionRef)) {
            return false;
        }
        InspectionRef other = (InspectionRef) o;
        return Objects.equals(stageType, other.stageType) && Objects.equals(batchId, other.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageType, batchId);
    }

    @Override
    public String toString() {
        return "InspectionRef{stageType=" + stageType + ", batchId=" + batchId + '}';
    }
}
