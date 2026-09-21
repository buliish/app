package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.WholBatch;

import java.util.List;

/**
 * 批发商产品批号业务接口
 * 状态：1 新建 2 待确认 3 已确认 4 已下架
 */
public interface WholBatchService extends OwnedBatchService<WholBatch> {

    List<WholBatch> listByNodeAndStatus(Integer nodeId, Integer status);

    boolean existsBatchNo(String batchNo);

    boolean offline(Integer wholBatchId);

    /**
     * 下游（零售商）进场确认列表。
     * <p>
     * 实现会委托给 RetaBatchService —— 批发商的下游是零售商，
     * 要查的是 reta_batch 表，而不是本类的 whol_batch。
     */
    List<ConfirmVO> listPendingConfirm(Integer wholNodeId, String downName);

    /**
     * 查"以这些批号为进场批号"的下游批发批号，并 JOIN 出下游企业。
     * <p>
     * 查的是 whol_batch 表本身。因为对加工企业来说，批发批号就是它的下游，
     * 所以加工环节的确认列表会委托到本方法（见 FrozBatchServiceImpl）。
     *
     * @param upBatchNos 上游（加工企业）已确认的批号
     * @param downName   下游企业名称模糊条件，可为 null
     */
    List<ConfirmVO> listDownConfirm(List<String> upBatchNos, String downName);

    /**
     * 确认下游企业进场（零售商批号 -> 已确认并生成溯源标识码）
     *
     * @param retaBatchId 下游零售商批号主键
     * @param wholNodeId  当前批发商编号，用于校验该批号确实是本企业的下游
     */
    boolean confirmDownstream(Integer retaBatchId, Integer wholNodeId);

    /**
     * 建号：先校验向上游领用的数量（含悲观锁），再落库。
     * <p>
     * 校验与落库必须在<b>同一事务</b>内 —— 行锁在事务提交时才释放，
     * 否则并发领用同一批货时，两个请求可能都通过校验再双双写入。
     */
    void saveWithQuantityCheck(WholBatch batch);

    /** 更新：同样校验领用量，并排除自身已占用的额度 */
    boolean updateWithQuantityCheck(WholBatch batch);
}
