package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gec.seafood_traceability_system.mapper.FarmBatchMapper;
import com.gec.seafood_traceability_system.mapper.FrozBatchMapper;
import com.gec.seafood_traceability_system.mapper.RetaBatchMapper;
import com.gec.seafood_traceability_system.mapper.WholBatchMapper;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.FarmBatch;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import com.gec.seafood_traceability_system.service.BatchQuantityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * 批号领用量校验实现。
 * <p>
 * 只走「上游批号 → 各下游批号领用量之和」这一条口径：
 * 领用量记在下游行上，所以下游批号被<b>删除</b>时，那一行消失、
 * 汇总自然回落，占用的额度自动释放 —— 不需要额外写"回退"逻辑。
 * 而<b>下架</b>不算释放：货确实已经交付给下游了，下架只是不再对外展示。
 * <p>
 * <b>这里直接注入 Mapper 而不是各批号 Service</b>：各个 Service 之间本来
 * 就存在互相注入（FarmBatchService ↔ FrozBatchService 用于下游确认），
 * 再从这里注入 Service 会形成
 * {@code FarmBatchServiceImpl → FrozBatchServiceImpl → BatchQuantityServiceImpl
 * → FarmBatchServiceImpl} 的循环依赖，Spring Boot 2.6+ 默认拒绝启动。
 * Mapper 没有任何依赖，用它读数据可以直接把环断掉。
 */
@Service
public class BatchQuantityServiceImpl implements BatchQuantityService {

    @Autowired
    private FarmBatchMapper farmBatchMapper;

    @Autowired
    private FrozBatchMapper frozBatchMapper;

    @Autowired
    private WholBatchMapper wholBatchMapper;

    @Autowired
    private RetaBatchMapper retaBatchMapper;

    @Override
    public BigDecimal remaining(Integer downStageType, String upBatchNo, Integer upNodeId) {
        BigDecimal total = upstreamTotal(downStageType, upBatchNo, upNodeId, false);
        if (total == null) {
            return null;
        }
        return total.subtract(downstreamUsed(downStageType, upBatchNo, upNodeId, null));
    }

    @Override
    public void assertCanTake(Integer downStageType, String upBatchNo, Integer upNodeId, BigDecimal takeKg) {
        assertCanTake(downStageType, upBatchNo, upNodeId, takeKg, null);
    }

    @Override
    public void assertCanTake(Integer downStageType, String upBatchNo, Integer upNodeId,
                              BigDecimal takeKg, Integer selfBatchId) {
        // 未登记领用量：不校验（兼容历史数据）
        if (takeKg == null) {
            return;
        }
        if (takeKg.signum() <= 0) {
            throw new BizException("领用数量必须大于 0");
        }
        // 悲观锁：先锁住上游批号那一行再算剩余量。
        // 并发领用同一批货时，后到的事务会阻塞在这里，等前一个提交后再计算，
        // 从而被正确拒绝，而不是两个都通过校验后双双写入。
        BigDecimal total = upstreamTotal(downStageType, upBatchNo, upNodeId, true);
        if (total == null) {
            // 上游存在但未登记数量 → 不限制（批号不存在时 upstreamTotal 会抛异常）
            return;
        }
        BigDecimal used = downstreamUsed(downStageType, upBatchNo, upNodeId, selfBatchId);
        BigDecimal remainingKg = total.subtract(used);
        if (takeKg.compareTo(remainingKg) > 0) {
            throw new BizException(String.format(
                    "领用数量超出上游剩余量：本次 %s kg，剩余 %s kg",
                    takeKg.stripTrailingZeros().toPlainString(),
                    remainingKg.stripTrailingZeros().toPlainString()));
        }
    }

    /**
     * 取上游批号的总量。
     * <p>
     * 上游批号不存在时抛业务异常 —— 不能放行，否则下游可以随便填一个
     * 不存在的批号把链路指到空处。
     *
     * @param lock 是否加行锁（校验路径加，只读展示路径不加）
     * @return 总量；上游存在但未登记数量时返回 null
     */
    private BigDecimal upstreamTotal(Integer downStageType, String upBatchNo, Integer upNodeId, boolean lock) {
        if (upBatchNo == null || upBatchNo.isBlank()) {
            throw new BizException("请先选择上游产品批号");
        }
        return switch (downStageType) {
            case STAGE_FROZ -> {
                FarmBatch up = farmBatchMapper.selectOne(new LambdaQueryWrapper<FarmBatch>()
                        .eq(FarmBatch::getBatchNo, upBatchNo)
                        .eq(upNodeId != null, FarmBatch::getNodeId, upNodeId)
                        .last(lock, "FOR UPDATE"));
                if (up == null) {
                    throw new BizException("上游养殖批号不存在：" + upBatchNo);
                }
                yield up.getQuantityKg();
            }
            case STAGE_WHOL -> {
                FrozBatch up = frozBatchMapper.selectOne(new LambdaQueryWrapper<FrozBatch>()
                        .eq(FrozBatch::getBatchNo, upBatchNo)
                        .eq(upNodeId != null, FrozBatch::getNodeId, upNodeId)
                        .last(lock, "FOR UPDATE"));
                if (up == null) {
                    throw new BizException("上游加工批号不存在：" + upBatchNo);
                }
                yield up.getQuantityKg();
            }
            case STAGE_RETA -> {
                WholBatch up = wholBatchMapper.selectOne(new LambdaQueryWrapper<WholBatch>()
                        .eq(WholBatch::getBatchNo, upBatchNo)
                        .eq(upNodeId != null, WholBatch::getNodeId, upNodeId)
                        .last(lock, "FOR UPDATE"));
                if (up == null) {
                    throw new BizException("上游批发批号不存在：" + upBatchNo);
                }
                yield up.getQuantityKg();
            }
            default -> throw new BizException("未知的下游环节类型：" + downStageType);
        };
    }

    /**
     * 汇总该上游批号已被领走的量。
     * <p>
     * 按环节去对应的下游表统计：加工(2)的领用记在 froz_batch、
     * 批发(3)记在 whol_batch、零售(4)记在 reta_batch。
     * 不加状态过滤 —— 已下架的批号同样占用额度（货已经给出去了）。
     */
    private BigDecimal downstreamUsed(Integer downStageType, String upBatchNo,
                                      Integer upNodeId, Integer selfBatchId) {
        return switch (downStageType) {
            case STAGE_FROZ -> {
                LambdaQueryWrapper<FrozBatch> w = new LambdaQueryWrapper<FrozBatch>()
                        .eq(FrozBatch::getUpBatchNo, upBatchNo)
                        .eq(upNodeId != null, FrozBatch::getUpNodeId, upNodeId)
                        .ne(selfBatchId != null, FrozBatch::getFrozBatchId, selfBatchId);
                yield sumUp(frozBatchMapper.selectList(w), FrozBatch::getUpQuantityKg);
            }
            case STAGE_WHOL -> {
                LambdaQueryWrapper<WholBatch> w = new LambdaQueryWrapper<WholBatch>()
                        .eq(WholBatch::getUpBatchNo, upBatchNo)
                        .eq(upNodeId != null, WholBatch::getUpNodeId, upNodeId)
                        .ne(selfBatchId != null, WholBatch::getWholBatchId, selfBatchId);
                yield sumUp(wholBatchMapper.selectList(w), WholBatch::getUpQuantityKg);
            }
            case STAGE_RETA -> {
                LambdaQueryWrapper<RetaBatch> w = new LambdaQueryWrapper<RetaBatch>()
                        .eq(RetaBatch::getUpBatchNo, upBatchNo)
                        .eq(upNodeId != null, RetaBatch::getUpNodeId, upNodeId)
                        .ne(selfBatchId != null, RetaBatch::getRetaBatchId, selfBatchId);
                yield sumUp(retaBatchMapper.selectList(w), RetaBatch::getUpQuantityKg);
            }
            default -> throw new BizException("未知的下游环节类型：" + downStageType);
        };
    }

    /** 汇总：null 视为 0（未登记领用量的行不占额度） */
    private <T> BigDecimal sumUp(List<T> rows, Function<T, BigDecimal> pick) {
        BigDecimal sum = BigDecimal.ZERO;
        for (T row : rows) {
            BigDecimal v = pick.apply(row);
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }
}
