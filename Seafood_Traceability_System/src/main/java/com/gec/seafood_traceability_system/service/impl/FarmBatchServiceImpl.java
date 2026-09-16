package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.FarmBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.FarmBatchService;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        //2.养殖企业的下游是加工企业，而 FrozBatchService.listDownConfirm 查的正是
        //  froz_batch 表（见 FrozBatchMapper.xml），因此直接委托给它。
        //  统一约定：**每个环节的 listDownConfirm 只查它自己那张批号表**，
        //  listPendingConfirm 负责"委托给下游环节的 Service"。
        return frozBatchService.listDownConfirm(upBatchNos, downName);
    }

    @Override
    public boolean confirmDownstream(Integer frozBatchId, Integer farmNodeId) {
        FrozBatch exist = frozBatchService.getById(frozBatchId);
        if (exist == null) {
            throw new BizException("下游批号不存在或已被删除");
        }
        // 只能确认"以本企业批号为进场批号"的下游批号，防止跨企业误确认
        if (!farmNodeId.equals(exist.getUpNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权确认其他企业的下游批号");
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException("该批号当前不是待确认状态");
        }
        FrozBatch batch = new FrozBatch();
        batch.setFrozBatchId(frozBatchId);
        batch.setStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        return frozBatchService.updateById(batch);
    }
}
