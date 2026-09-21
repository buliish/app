package com.gec.seafood_traceability_system.service;

import java.math.BigDecimal;

/**
 * 批号领用量校验（超领防护）。
 * <p>
 * 现实里一批货是分批领用的：养殖场出场 3 吨冻虾，加工厂领 1 吨做虾滑、
 * 1 吨做虾丸、1 吨做虾球，分别流向三个下游。因此下游批号上的
 * {@code up_quantity_kg}（领用量）之和，不得超过上游批号的 {@code quantity_kg}（总量）。
 * <p>
 * <b>未登记数量即不限制</b>：上游 {@code quantity_kg} 为 null 表示"未登记"，
 * 此时直接放行。这是为兼容历史批号 —— 它们没有数量，
 * 若一刀切去校验会把既有的链路全判成超领。
 */
public interface BatchQuantityService {

    /** 环节类型，与 node_info.type 同码 */
    int STAGE_FARM = 1;
    int STAGE_FROZ = 2;
    int STAGE_WHOL = 3;
    int STAGE_RETA = 4;

    /**
     * 上游批号当前剩余可领用量（kg）。
     *
     * @param downStageType 下游环节类型（决定去查哪张下游表汇总领用量）
     * @param upBatchNo     上游批号
     * @param upNodeId      上游企业编号
     * @return 剩余量；上游未登记数量时返回 null，表示不限制
     */
    BigDecimal remaining(Integer downStageType, String upBatchNo, Integer upNodeId);

    /**
     * 校验本次领用不超量。
     * <p>
     * <b>会锁定上游批号行</b>（{@code SELECT ... FOR UPDATE}），
     * 因此必须在事务内调用，且该事务要覆盖到落库动作 ——
     * 否则锁提前释放，并发下两个下游可能同时领走最后一批货。
     *
     * @param downStageType 下游环节类型
     * @param upBatchNo     上游批号
     * @param upNodeId      上游企业编号
     * @param takeKg        本次领用量；为 null 表示未登记，直接放行
     * @throws com.gec.seafood_traceability_system.pojo.BizException 上游不存在，或领用量超出剩余
     */
    void assertCanTake(Integer downStageType, String upBatchNo, Integer upNodeId, BigDecimal takeKg);

    /**
     * 校验领用不超量，并排除自身批号已占用的量。
     * <p>
     * 更新批号时用：把领用量从 800kg 改成 1200kg，校验时不能把原来的
     * 800kg 也算进"已被领走"，否则会误判超领。
     *
     * @param selfBatchId 自身批号主键，新建时传 null
     */
    void assertCanTake(Integer downStageType, String upBatchNo, Integer upNodeId,
                       BigDecimal takeKg, Integer selfBatchId);
}
