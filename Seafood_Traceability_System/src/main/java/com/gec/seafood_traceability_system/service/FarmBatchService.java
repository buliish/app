package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FarmBatch;

import java.util.List;

/**
 * 养殖企业产品批号业务接口
 * 状态：1 待发布 2 已发布 3 已下架
 */
public interface FarmBatchService extends OwnedBatchService<FarmBatch> {

    /** 按状态查询本企业批号列表（已下架不展示） */
    List<FarmBatch> listByNodeAndStatus(Integer nodeId, Integer status);

    /** 批号是否已存在（新建时 blur 校验） */
    boolean existsBatchNo(String batchNo);

    /** 下架批号 */
    boolean offline(Integer farmBatchId);

    /** 下游（冷冻加工企业）进场确认列表 */
    List<ConfirmVO> listPendingConfirm(Integer farmNodeId, String downName);

    /**
     * 确认下游企业进场（冷冻加工批号状态 -> 已确认）
     *
     * @param frozBatchId 下游冷冻加工批号主键
     * @param farmNodeId  当前养殖企业编号，用于校验该批号确实是本企业的下游
     */
    boolean confirmDownstream(Integer frozBatchId, Integer farmNodeId);
}
