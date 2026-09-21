package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.TraceChain;

import java.util.List;

/**
 * 溯源链路走查器 —— 全系统<b>唯一</b>的链路构建实现
 * <p>
 * 消费者端与管理端的展示诉求不同（前者只要企业名与批号，后者要检测明细、
 * 质量状态、链路完整性），但"怎么把一条链走通"是同一件事。
 * 因此这里只负责走查，不负责渲染；两个门面各自取舍。
 * <p>
 * <b>注意</b>：本类不做任何状态门槛判断。消费者端"已下架不可溯源"的限制
 * 属于展示策略，必须留在消费者门面层 —— 否则管理端就没法查看
 * 已下架批号的完整链路与检测明细了。
 */
public interface TraceChainLoader {

    /**
     * 按零售批号主键装载完整链路（不含检测记录）。
     *
     * @param retaBatchId 零售批号主键
     * @return 链路对象，零售批号不存在时返回 null
     */
    TraceChain loadByRetaBatchId(Integer retaBatchId);

    /**
     * 按溯源码/产品编号/批号等任意关键词解析出链路。
     * <p>
     * 解析顺序：溯源码 → 产品编号 → 零售批号 → 批发批号 → 加工批号 → 养殖批号，
     * 即"反向解析 + 正向追踪"，因此养殖环节的批号也能一路查到最终流向的零售端。
     *
     * @param keyword 关键词
     * @return 唯一命中时返回链路；命中多条或无命中时返回 null
     */
    TraceChain loadByKeyword(String keyword);

    /**
     * 按关键词找出候选零售批号（命中多条时供用户选择，或精确命中一条）。
     *
     * @return 候选列表，无命中时为空列表
     */
    List<Integer> resolveCandidates(String keyword);
}
