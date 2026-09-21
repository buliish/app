package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.FrozBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.FrozBatchService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        //2.加工企业的下游是批发商，委托给 WholBatchService（它查 whol_batch 表）
        return wholBatchService.listDownConfirm(upBatchNos, downName);
    }

    /**
     * 查"以这些批号为进场批号"的下游冷冻加工批号，并 JOIN 出下游企业。
     * <p>
     * 本方法查的是自己这张 froz_batch 表 —— 对养殖企业而言加工批号就是它的下游，
     * 所以养殖环节的 listPendingConfirm 会委托到这里。
     */
    @Override
    public List<ConfirmVO> listDownConfirm(List<String> upBatchNos, String downName) {
        if (upBatchNos == null || upBatchNos.isEmpty()) {
            return new ArrayList<>();
        }
        List<ConfirmVO> result = baseMapper.selectDownConfirmList(upBatchNos, downName);
        // 关联映射把企业塞在 downNode 里，这里派生出前端契约字段 downName
        result.forEach(vo -> vo.setDownName(vo.getDownNode() == null ? null : vo.getDownNode().getName()));
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
