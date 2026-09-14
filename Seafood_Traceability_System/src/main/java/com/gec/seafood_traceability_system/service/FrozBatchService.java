package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FrozBatch;

import java.util.List;

/**
 * 冷冻加工企业产品批号业务接口
 * 状态：1 新建 2 待确认 3 已确认 4 已下架
 */
public interface FrozBatchService extends IService<FrozBatch> {

    List<FrozBatch> listByNodeAndStatus(Integer nodeId, Integer status);

    boolean existsBatchNo(String batchNo);

    boolean offline(Integer frozBatchId);

    /** 下游（批发商）进场确认列表 */
    List<ConfirmVO> listPendingConfirm(Integer frozNodeId, String downName);

    /** 确认下游企业进场（批发商批号状态 -> 已确认） */
    boolean confirmDownstream(Integer wholBatchId);
}
