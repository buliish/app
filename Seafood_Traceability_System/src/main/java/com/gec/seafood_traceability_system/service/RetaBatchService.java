package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.RetaBatch;

import java.util.List;

/**
 * 零售商产品批号业务接口
 * 状态：1 新建 2 待确认 3 已确认（自动生成溯源标识码） 4 已下架
 */
public interface RetaBatchService extends OwnedBatchService<RetaBatch> {

    List<RetaBatch> listByNodeAndStatus(Integer nodeId, Integer status);

    boolean existsBatchNo(String batchNo);

    boolean offline(Integer retaBatchId);

    /** 进场确认：批号状态置为已确认，并自动生成溯源标识码 */
    String confirmBatch(Integer retaBatchId);

    /**
     * 查"以这些批号为进场批号"的下游待确认批号，并 JOIN 出下游企业。
     * <p>
     * 零售是链路末端，自己没有下游，因此本方法在零售环节不用于"下游确认"，
     * 而是作为批发商环节的委托目标 —— 批发商查的正是 reta_batch 表。
     *
     * @param upBatchNos 上游（批发商）已确认的批号
     * @param downName   下游企业名称模糊条件，可为 null
     */
    List<ConfirmVO> listDownConfirm(List<String> upBatchNos, String downName);

    /**
     * 建号：先校验向上游领用的数量（含悲观锁），再落库。
     * <p>
     * 校验与落库必须在<b>同一事务</b>内 —— 行锁在事务提交时才释放，
     * 否则并发领用同一批货时，两个请求可能都通过校验再双双写入。
     */
    void saveWithQuantityCheck(RetaBatch batch);

    /** 更新：同样校验领用量，并排除自身已占用的额度 */
    boolean updateWithQuantityCheck(RetaBatch batch);
}
