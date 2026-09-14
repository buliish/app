package com.gec.seafood_traceability_system.service;

import java.util.Map;

/** 消费者溯源业务接口（3.2.7） */
public interface TraceService {

    /**
     * 根据溯源标识码查询全链路溯源信息
     * 链路：养殖企业 -> 冷冻加工企业（含加工工序） -> 批发商 -> 零售商
     *
     * @return 溯源数据，标识码不存在时返回 null
     */
    Map<String, Object> trace(String traceCode);
}
