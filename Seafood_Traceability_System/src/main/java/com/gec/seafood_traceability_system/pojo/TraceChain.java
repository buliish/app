package com.gec.seafood_traceability_system.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 一条完整的溯源链路：零售 → 批发 → 冷冻加工 → 养殖。
 * <p>
 * 由 {@code TraceChainLoader} 沿 {@code up_batch_no} 逐级上溯装配，
 * 消费者端与管理端复用同一份链路对象，只是各自的门槛与渲染不同。
 * <p>
 * 上溯过程中任一级缺失（历史数据没接上链路）时，对应字段为 {@code null}，
 * 由 {@link #isComplete()} 反映出来——不在这里抛异常，因为管理端需要
 * 看到"链路断在哪一段"。
 */
@Data
public class TraceChain {

    /** 环节类型，与 node_info.type 取值保持一致 */
    public static final int STAGE_FARM = 1;
    public static final int STAGE_FROZ = 2;
    public static final int STAGE_WHOL = 3;
    public static final int STAGE_RETA = 4;

    /** 质量状态取值：与前端 QualityTag 的 QUALITY_STATUS 映射一致 */
    public static final int QUALITY_PENDING = 0;
    public static final int QUALITY_PASSED = 1;
    public static final int QUALITY_REJECTED = 2;

    /** 四级批号，链路缺失的那一级为 null */
    private RetaBatch reta;
    private WholBatch whol;
    private FrozBatch froz;
    private FarmBatch farm;

    /** 各级批号所属企业 */
    private NodeInfo retaNode;
    private NodeInfo wholNode;
    private NodeInfo frozNode;
    private NodeInfo farmNode;

    /** 冷冻加工环节的工序记录（清洗/分级/冷冻/包装） */
    private List<ProcessRecord> processRecords = new ArrayList<>();

    /**
     * 链路是否完整。
     * <p>
     * 四级批号全部上溯到位才算完整；断链说明历史数据没接上，
     * 消费者端要给出提示，管理端要能定位断点。
     */
    public boolean isComplete() {
        return farm != null && froz != null && whol != null && reta != null;
    }

    /**
     * 全链路整体质量结论：取各级里最差的一档。
     * <p>
     * 规则：任一级"不合格"则整体不合格；未判定（null）的环节不参与拉低，
     * 只有全部有判定且都合格才算合格；若四级都没有质量数据则视为待检。
     * <p>
     * 之所以取最差而非取最后一级，是因为溯源的意义在于"任何一环出过问题
     * 都要能被消费者看见"。
     */
    public Integer overallQuality() {
        List<Integer> qualities = new ArrayList<>();
        if (farm != null) {
            qualities.add(farm.getQualityStatus());
        }
        if (froz != null) {
            qualities.add(froz.getQualityStatus());
        }
        if (whol != null) {
            qualities.add(whol.getQualityStatus());
        }
        if (reta != null) {
            qualities.add(reta.getQualityStatus());
        }
        qualities.removeIf(Objects::isNull);

        if (qualities.isEmpty()) {
            return QUALITY_PENDING;
        }
        if (qualities.contains(QUALITY_REJECTED)) {
            return QUALITY_REJECTED;
        }
        boolean allPassed = qualities.stream().allMatch(q -> q == QUALITY_PASSED);
        return allPassed ? QUALITY_PASSED : QUALITY_PENDING;
    }

    /**
     * 本链路涉及的检测记录锚点，供 {@code InspectionService.listByRefs} 一次取全。
     * <p>
     * 只为链路中真实存在的环节生成锚点，断链时不会去查不存在批号的检测记录。
     */
    public List<InspectionRef> refs() {
        List<InspectionRef> refs = new ArrayList<>();
        if (farm != null) {
            refs.add(new InspectionRef(STAGE_FARM, farm.getFarmBatchId()));
        }
        if (froz != null) {
            refs.add(new InspectionRef(STAGE_FROZ, froz.getFrozBatchId()));
        }
        if (whol != null) {
            refs.add(new InspectionRef(STAGE_WHOL, whol.getWholBatchId()));
        }
        if (reta != null) {
            refs.add(new InspectionRef(STAGE_RETA, reta.getRetaBatchId()));
        }
        return refs;
    }
}
