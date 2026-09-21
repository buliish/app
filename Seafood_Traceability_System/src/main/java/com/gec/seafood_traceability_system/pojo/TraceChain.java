package com.gec.seafood_traceability_system.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条完整的溯源链（零售 → 批发 → 冷冻加工 → 养殖）
 * <p>
 * 消费者门面与管理端门面共用同一个 {@code TraceChainLoader} 走查结果，
 * 各自渲染时按需取舍 —— 消费者端只用企业名与批号，
 * 管理端还要看检测明细、质量状态与是否链路完整。
 */
@Data
public class TraceChain {

    /** 零售批号（链路入口，必然非空） */
    private RetaBatch reta;

    /** 批发批号（链路断裂时为 null） */
    private WholBatch whol;

    /** 冷冻加工批号（链路断裂时为 null） */
    private FrozBatch froz;

    /** 养殖批号（链路断裂时为 null） */
    private FarmBatch farm;

    /** 四个环节对应的企业信息，与上面四个批号一一对应，可能为 null */
    private NodeInfo retaNode;
    private NodeInfo wholNode;
    private NodeInfo frozNode;
    private NodeInfo farmNode;

    /** 冷冻加工工序记录（仅加工环节有） */
    private List<ProcessRecord> processRecords = new ArrayList<>();

    /** 各环节检测记录（消费者端与管理端都展示） */
    private List<Inspection> inspections = new ArrayList<>();

    /** 链路是否完整（四级都能上溯到） */
    public boolean isComplete() {
        return reta != null && whol != null && froz != null && farm != null;
    }

    /**
     * 整链质量结论：任一环节不合格即不合格；全部环节合格才算合格；
     * 否则（存在待检环节）为待检。
     */
    public Integer overallQuality() {
        List<Integer> list = new ArrayList<>();
        if (farm != null) list.add(farm.getQualityStatus());
        if (froz != null) list.add(froz.getQualityStatus());
        if (whol != null) list.add(whol.getQualityStatus());
        if (reta != null) list.add(reta.getQualityStatus());
        if (list.isEmpty()) {
            return 0;
        }
        if (list.contains(2)) {
            return 2;
        }
        return list.contains(0) ? 0 : 1;
    }

    /** 链路上各环节的批号引用，用于一次性取回全部检测记录 */
    public List<BatchRef> refs() {
        List<BatchRef> refs = new ArrayList<>();
        if (farm != null) refs.add(new BatchRef(1, farm.getFarmBatchId(), farm.getBatchNo()));
        if (froz != null) refs.add(new BatchRef(2, froz.getFrozBatchId(), froz.getBatchNo()));
        if (whol != null) refs.add(new BatchRef(3, whol.getWholBatchId(), whol.getBatchNo()));
        if (reta != null) refs.add(new BatchRef(4, reta.getRetaBatchId(), reta.getBatchNo()));
        return refs;
    }
}
