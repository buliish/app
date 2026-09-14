package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.FarmBatchMapper;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 养殖企业产品批号业务实现
 * 状态：1 待发布 2 已发布 3 已下架
 */
@Service
public class FarmBatchServiceImpl extends ServiceImpl<FarmBatchMapper, FarmBatch> implements FarmBatchService {

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Override
    public List<FarmBatch> listByNodeAndStatus(Integer nodeId, Integer status) {
        return lambdaQuery()
                .eq(FarmBatch::getNodeId, nodeId)
                .eq(status != null, FarmBatch::getStatus, status)
                //不传状态时排除已下架批号（已下架不能浏览）
                .ne(status == null, FarmBatch::getStatus, 3)
                .orderByDesc(FarmBatch::getCreateTime)
                .list();
    }

    @Override
    public boolean existsBatchNo(String batchNo) {
        return lambdaQuery().eq(FarmBatch::getBatchNo, batchNo).count() > 0;
    }

    @Override
    public boolean offline(Integer farmBatchId) {
        FarmBatch batch = new FarmBatch();
        batch.setFarmBatchId(farmBatchId);
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }

    @Override
    public List<ConfirmVO> listPendingConfirm(Integer farmNodeId, String downName) {
        //1.本养殖企业已发布的产品批号
        List<String> upBatchNos = lambdaQuery()
                .eq(FarmBatch::getNodeId, farmNodeId)
                .eq(FarmBatch::getStatus, 2)
                .list().stream().map(FarmBatch::getBatchNo).collect(Collectors.toList());
        if (upBatchNos.isEmpty()) {
            return new ArrayList<>();
        }
        //2.以这些批号作为进场批号、且状态为待确认的下游（冷冻加工企业）批号
        List<FrozBatch> downs = frozBatchService.list(Wrappers.lambdaQuery(FrozBatch.class)
                .in(FrozBatch::getUpBatchNo, upBatchNos)
                .eq(FrozBatch::getStatus, 2)
                .orderByDesc(FrozBatch::getCreateTime));
        if (downs.isEmpty()) {
            return new ArrayList<>();
        }
        //3.补齐下游企业名称，并支持按下游企业名称模糊查询
        Map<Integer, String> nameMap = nodeInfoService.listByIds(
                        downs.stream().map(FrozBatch::getNodeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(NodeInfo::getNodeId, NodeInfo::getName));

        List<ConfirmVO> result = new ArrayList<>();
        for (FrozBatch d : downs) {
            String name = nameMap.get(d.getNodeId());
            if (StringUtils.hasText(downName) && (name == null || !name.contains(downName))) {
                continue;
            }
            ConfirmVO vo = new ConfirmVO();
            vo.setId(d.getFrozBatchId());
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
    public boolean confirmDownstream(Integer frozBatchId) {
        FrozBatch batch = new FrozBatch();
        batch.setFrozBatchId(frozBatchId);
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        return frozBatchService.updateById(batch);
    }
}
