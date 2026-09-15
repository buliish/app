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

    /** 下游（零售商）进场确认列表 */
    List<ConfirmVO> listPendingConfirm(Integer wholNodeId, String downName);

    /**
     * 按进场批号查下游待确认批号，并 JOIN 出下游企业（多对一，见 WholBatchMapper.xml）
     *
     * @param upBatchNos 本企业已确认的产品批号，调用方保证非空
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
}
