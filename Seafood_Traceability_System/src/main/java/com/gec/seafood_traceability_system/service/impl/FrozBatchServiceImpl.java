package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.FrozBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 冷冻加工企业产品批号业务实现
 * 状态：1 新建 2 待确认 3 已确认 4 已下架
 */
@Service
public class FrozBatchServiceImpl extends ServiceImpl<FrozBatchMapper, FrozBatch> implements FrozBatchService {

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private WholBatchService wholBatchService;

    @Override
    public List<FrozBatch> listByNodeAndStatus(Integer nodeId, Integer status) {
        return lambdaQuery()
                .eq(FrozBatch::getNodeId, nodeId)
                .eq(status != null, FrozBatch::getStatus, status)
                .ne(status == null, FrozBatch::getStatus, 4)
                .orderByDesc(FrozBatch::getCreateTime)
                .list();
    }

    @Override
    public boolean existsBatchNo(String batchNo) {
        return lambdaQuery().eq(FrozBatch::getBatchNo, batchNo).count() > 0;
    }

    @Override
    public boolean offline(Integer frozBatchId) {
        FrozBatch batch = new FrozBatch();
        batch.setFrozBatchId(frozBatchId);
        batch.setStatus(4);
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }

    @Override
    public List<ConfirmVO> listPendingConfirm(Integer frozNodeId, String downName) {
        //1.本加工企业已确认的产品批号（已确认后才进入下游流通）
        List<String> upBatchNos = lambdaQuery()
                .eq(FrozBatch::getNodeId, frozNodeId)
                .eq(FrozBatch::getStatus, 3)
                .list().stream().map(FrozBatch::getBatchNo).collect(Collectors.toList());
        if (upBatchNos.isEmpty()) {
            return new ArrayList<>();
        }
        //2.以这些批号作为进场批号、且状态为待确认的下游（批发商）批号
        List<WholBatch> downs = wholBatchService.list(Wrappers.lambdaQuery(WholBatch.class)
                .in(WholBatch::getUpBatchNo, upBatchNos)
                .eq(WholBatch::getStatus, 2)
                .orderByDesc(WholBatch::getCreateTime));
        if (downs.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, String> nameMap = nodeInfoService.listByIds(
                        downs.stream().map(WholBatch::getNodeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(NodeInfo::getNodeId, NodeInfo::getName));

        List<ConfirmVO> result = new ArrayList<>();
        for (WholBatch d : downs) {
            String name = nameMap.get(d.getNodeId());
            if (StringUtils.hasText(downName) && (name == null || !name.contains(downName))) {
                continue;
            }
            ConfirmVO vo = new ConfirmVO();
            vo.setId(d.getWholBatchId());
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
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmDownstream(Integer wholBatchId, Integer frozNodeId) {
        WholBatch exist = wholBatchService.getById(wholBatchId);
        if (exist == null) {
            throw new BizException("下游批号不存在或已被删除");
        }
        // 只能确认"以本企业批号为进场批号"的下游批号，防止跨企业误确认
        if (!frozNodeId.equals(exist.getUpNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权确认其他企业的下游批号");
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException("该批号当前不是待确认状态");
        }
        WholBatch batch = new WholBatch();
        batch.setWholBatchId(wholBatchId);
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        return wholBatchService.updateById(batch);
    }
}
