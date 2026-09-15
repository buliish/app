package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.TraceChain;

/**
 * 溯源链路走查器。
 * <p>
 * 从零售批号出发，沿 {@code up_batch_no} 逐级上溯，装配出
 * 「零售 → 批发 → 冷冻加工 → 养殖」的完整链路对象。
 * <p>
 * 抽成独立组件是因为消费者端与管理端要用同一条链路，只是对外的
 * 门槛不同（消费者端拒绝已下架批号，管理端要能看到断点与下架批号）。
 * <b>因此本组件不做任何状态过滤</b>——过滤是调用方的事，
 * 放在这里会让管理端也看不到那些批号。
 */
public interface TraceChainLoader {

    /**
     * 按零售批号主键装配链路。
     *
     * @param retaBatchId 零售批号主键
     * @return 链路对象；批号不存在时返回 {@code null}。
     *         批号存在但上游断链时返回的对象里对应字段为 {@code null}，
     *         用 {@link TraceChain#isComplete()} 判断。
     */
    TraceChain loadByRetaBatchId(Integer retaBatchId);
}
