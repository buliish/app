package com.gec.seafood_traceability_system.service;

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
}
