package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.WholBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.RetaBatchService;
import com.gec.seafood_traceability_system.service.WholBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        //2.批发商的下游是零售商，委托给 RetaBatchService（它查 reta_batch 表）
        return retaBatchService.listDownConfirm(upBatchNos, downName);
    }

    /**
     * 查"以这些批号为进场批号"的下游批发批号，并 JOIN 出下游企业。
     * <p>
     * 本方法查的是自己这张 whol_batch 表 —— 对加工企业而言批发批号就是它的下游，
     * 所以加工环节的 listPendingConfirm 会委托到这里。
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
    public boolean confirmDownstream(Integer retaBatchId, Integer wholNodeId) {
        RetaBatch exist = retaBatchService.getById(retaBatchId);
        if (exist == null) {
            throw new BizException("下游批号不存在或已被删除");
        }
        // 只能确认"以本企业批号为进场批号"的下游批号，防止跨企业误确认
        if (!wholNodeId.equals(exist.getUpNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权确认其他企业的下游批号");
        }
        if (exist.getStatus() == null || exist.getStatus() != 2) {
            throw new BizException("该批号当前不是待确认状态");
        }
        //确认零售商进场：批号置为已确认，同时系统生成溯源标识码
        return retaBatchService.confirmBatch(retaBatchId) != null;
    }
}
