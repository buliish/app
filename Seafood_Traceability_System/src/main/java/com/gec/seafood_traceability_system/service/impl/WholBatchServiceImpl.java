package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.WholBatchMapper;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批发商产品批号业务实现
 * 状态：1 新建 2 待确认 3 已确认 4 已下架
 */
@Service
public class WholBatchServiceImpl extends ServiceImpl<WholBatchMapper, WholBatch> implements WholBatchService {

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private RetaBatchService retaBatchService;

    @Override
    public List<WholBatch> listByNodeAndStatus(Integer nodeId, Integer status) {
        return lambdaQuery()
                .eq(WholBatch::getNodeId, nodeId)
                .eq(status != null, WholBatch::getStatus, status)
                .ne(status == null, WholBatch::getStatus, 4)
                .orderByDesc(WholBatch::getCreateTime)
                .list();
    }

    @Override
    public boolean existsBatchNo(String batchNo) {
        return lambdaQuery().eq(WholBatch::getBatchNo, batchNo).count() > 0;
    }

    @Override
    public boolean offline(Integer wholBatchId) {
        WholBatch batch = new WholBatch();
        batch.setWholBatchId(wholBatchId);
        batch.setStatus(4);
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }

    @Override
    public List<ConfirmVO> listPendingConfirm(Integer wholNodeId, String downName) {
        //1.本批发商已确认的产品批号
        List<String> upBatchNos = lambdaQuery()
                .eq(WholBatch::getNodeId, wholNodeId)
                .eq(WholBatch::getStatus, 3)
                .list().stream().map(WholBatch::getBatchNo).collect(Collectors.toList());
        if (upBatchNos.isEmpty()) {
            return new ArrayList<>();
        }
        //2.以这些批号作为进场批号、且状态为待确认的下游（零售商）批号
        List<RetaBatch> downs = retaBatchService.list(Wrappers.lambdaQuery(RetaBatch.class)
                .in(RetaBatch::getUpBatchNo, upBatchNos)
                .eq(RetaBatch::getStatus, 2)
                .orderByDesc(RetaBatch::getCreateTime));
        if (downs.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, String> nameMap = nodeInfoService.listByIds(
                        downs.stream().map(RetaBatch::getNodeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(NodeInfo::getNodeId, NodeInfo::getName));

        List<ConfirmVO> result = new ArrayList<>();
        for (RetaBatch d : downs) {
            String name = nameMap.get(d.getNodeId());
            if (StringUtils.hasText(downName) && (name == null || !name.contains(downName))) {
                continue;
            }
            ConfirmVO vo = new ConfirmVO();
            vo.setId(d.getRetaBatchId());
            vo.setDownNodeId(d.getNodeId());
            vo.setDownName(name);
            vo.setDownBatchNo(d.getBatchNo());
            vo.setUpBatchNo(d.getUpBatchNo());
            vo.setBreed(d.getBreed());
            vo.setProductType(d.getProductType());
            vo.setStatus(d.getStatus());
            vo.setCreateTime(d.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public boolean confirmDownstream(Integer retaBatchId) {
        //确认零售商进场：批号置为已确认，同时系统生成溯源标识码
        return retaBatchService.confirmBatch(retaBatchId) != null;
    }
}
