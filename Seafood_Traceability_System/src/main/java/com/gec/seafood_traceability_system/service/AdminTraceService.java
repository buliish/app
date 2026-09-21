package com.gec.seafood_traceability_system.service;

import com.gec.seafood_traceability_system.pojo.ChainTreeNode;

import java.util.List;
import java.util.Map;

/**
 * 管理端批次追溯业务接口
 * <p>
 * 与消费者端的区别：不施加"已下架不可溯源"的限制，并且返回检测明细、
 * 各环节质量状态、链路是否完整等运营视角的信息。
 */
public interface AdminTraceService {

    /** 按关键词搜索候选批次（产品编号/溯源码/批号/品种），返回可分页的候选列表 */
    List<Map<String, Object>> search(String keyword);

    /** 按关键词取完整链路详情，命中多条时返回 null（由前端先选候选） */
    Map<String, Object> chainByKeyword(String keyword);

    /** 按零售批号主键取完整链路详情 */
    Map<String, Object> chainByRetaBatchId(Integer retaBatchId);

    /** 各环节合格率与形态分布统计（管理端图表用） */
    Map<String, Object> stats();

    /**
     * 完整产业链树（从养殖源头向下展开全部分支）。
     * <p>
     * 与 {@link #chainByRetaBatchId(Integer)} 的区别：后者只给"从这件商品
     * 往上游的一条线"，本方法给的是<b>整棵产业树</b> —— 一批虾派生出的
     * 虾滑、虾丸等各条分支都能看到，用于监管视角的流向分析与排障。
     * <p>
     * 返回类型用 {@link ChainTreeNode} 而不是 Map：树的层次结构用 Map
     * 表达会退化成层层嵌套的字符串 key，前端拿不到可读的类型契约。
     *
     * @param retaBatchId 入口零售批号主键
     * @return 树根；批号不存在时返回 null
     */
    ChainTreeNode treeByRetaBatchId(Integer retaBatchId);
}
