package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.NodeOwned;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 按"环节类型"路由到对应批号 Service 的注册表
 * <p>
 * 检测记录用 {@code stageType + batchId} 多态关联四个环节的批号，
 * 写入前必须校验该批号属于当前登录企业。四个批号的归属校验逻辑完全一致，
 * 已经由 {@link OwnedBatchService#requireOwned} 统一实现 ——
 * 本类只负责"按类型找对 Service"，不重复写任何越权判断。
 * <p>
 * 这样四个 Controller × 多个端点都复用同一套校验，避免 P0 阶段修过的
 * IDOR 问题在新功能里重新长出来。
 */
@Component
public class OwnedBatchRegistry {

    /** 环节类型常量，与 node_info.type 同码 */
    public static final int STAGE_FARM = 1;
    public static final int STAGE_FROZ = 2;
    public static final int STAGE_WHOL = 3;
    public static final int STAGE_RETA = 4;

    @Autowired
    private FarmBatchService farmBatchService;

    @Autowired
    private FrozBatchService frozBatchService;

    @Autowired
    private WholBatchService wholBatchService;

    @Autowired
    private RetaBatchService retaBatchService;

    /**
     * 取出批号并校验归属，越权直接抛 403。
     * <p>
     * 不传状态白名单 —— 各环节"已下架"在四张表里取值不同（养殖 3、其余 4），
     * 由 {@link #requireOwnedForInspection} 单独处理更清晰。
     */
    public NodeOwned requireOwned(Integer stageType, Integer batchId, Integer currentNodeId) {
        if (stageType == null) {
            throw new BizException("缺少环节类型");
        }
        return switch (stageType) {
            case STAGE_FARM -> farmBatchService.requireOwned(batchId, currentNodeId);
            case STAGE_FROZ -> frozBatchService.requireOwned(batchId, currentNodeId);
            case STAGE_WHOL -> wholBatchService.requireOwned(batchId, currentNodeId);
            case STAGE_RETA -> retaBatchService.requireOwned(batchId, currentNodeId);
            default -> throw new BizException("未知的环节类型：" + stageType);
        };
    }

    /**
     * 录入检测记录时的归属 + 状态校验：已下架的批号不允许再补录检测记录。
     * <p>
     * 下架状态值各环节不同（养殖是 3，其余三张表是 4），这里统一收口，
     * 避免调用方各自记忆。
     */
    public NodeOwned requireOwnedForInspection(Integer stageType, Integer batchId, Integer currentNodeId) {
        NodeOwned owned = requireOwned(stageType, batchId, currentNodeId);
        int offlineStatus = STAGE_FARM == stageType ? 3 : 4;
        if (owned.getStatus() != null && owned.getStatus() == offlineStatus) {
            throw new BizException("该批号已下架，不可再录入检测记录");
        }
        return owned;
    }

    /** 环节名称，用于报错与展示 */
    public String stageName(Integer stageType) {
        if (stageType == null) {
            return "未知环节";
        }
        return switch (stageType) {
            case STAGE_FARM -> "养殖";
            case STAGE_FROZ -> "冷冻加工";
            case STAGE_WHOL -> "批发";
            case STAGE_RETA -> "零售";
            default -> "未知环节";
        };
    }
}
