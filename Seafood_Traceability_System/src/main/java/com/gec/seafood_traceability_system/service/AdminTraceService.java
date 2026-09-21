package com.gec.seafood_traceability_system.service;

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
}
