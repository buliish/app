package com.gec.seafood_traceability_system.service;

import java.util.Map;

/** 消费者溯源业务接口（3.2.7） */
public interface TraceService {

    /**
     * 根据溯源标识码查询全链路溯源信息
     * 链路：养殖企业 -> 冷冻加工企业（含加工工序） -> 批发商 -> 零售商
     * <p>
     * 也接受对外产品编号（消费者可能输的是包装上的产品编号）。
     *
     * @return 溯源数据，标识码不存在时返回 null
     */
    Map<String, Object> trace(String traceCode);

    /**
     * 在售商品分页列表（消费者端首页"售卖的产品"）
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数（上限 50）
     * @param keyword 品种/品类/编号模糊匹配，可为 null
     * @param form    形态筛选（鲜虾/冻虾），可为 null
     * @param provId  产地省份筛选，可为 null
     */
    Map<String, Object> listProducts(long current, long size, String keyword, String form, Integer provId);

    /**
     * 商品详情：产品信息 + 四级企业链 + 各环节检测记录 + 加工工序。
     * <p>
     * 与 {@link #trace(String)} 的区别是入口用零售批号主键，且不限制状态
     * （消费者可能通过已购买的链接进入，但已下架商品会返回状态标记供前端提示）。
     *
     * @return 详情数据，批号不存在时返回 null
     */
    Map<String, Object> productDetailByTraceCode(String traceCode);

    /**
     * 判断某个编码是否存在对应的零售批号（溯源码或对外产品编号皆可）。
     * <p>
     * 专供二维码生成使用，<b>刻意不区分上下架状态</b> —— 二维码的语义是
     * "把产品编号印在包装上"，批号下架后包装上已印的码不该跟着失效；
     * 能否查到内容由扫码后的查询接口决定。
     *
     * @return true 表示存在
     */
    boolean existsCode(String code);
}
