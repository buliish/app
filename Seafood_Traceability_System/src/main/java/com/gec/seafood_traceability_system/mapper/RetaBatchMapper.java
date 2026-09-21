package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.RetaBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 零售商产品批号 Mapper
 * <p>
 * 消费者端"在售产品"列表需要跨四级链路取产地、形态、规格等信息，
 * 用一条 JOIN 一次取全（见 RetaBatchMapper.xml），避免逐条回走链路造成 N+1。
 */
@Mapper
public interface RetaBatchMapper extends BaseMapper<RetaBatch> {

    /**
     * 在售商品分页列表。
     * <p>
     * "在售" = 已确认(status=3) 且已生成溯源码、且零售环节质量不是不合格(quality_status<>2)。
     * 零售是最终放行环节，其质量状态即在售门槛；上游各环节的状态在详情页逐个展示，
     * 不会因为这里过滤而丢失。
     *
     * @param page    分页对象
     * @param keyword 名称/品种/编号模糊匹配，可为 null
     * @param form    产品形态筛选（鲜虾/冻虾），可为 null
     * @param provId  产地省份筛选，可为 null
     */
    IPage<Map<String, Object>> selectProductPage(IPage<Map<String, Object>> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("form") String form,
                                                 @Param("provId") Integer provId);

    /** 商品详情所需的链路概要（按零售批号主键） */
    Map<String, Object> selectProductDetail(@Param("retaBatchId") Integer retaBatchId);

    /** 在售商品的形态分布（管理端统计图用） */
    List<Map<String, Object>> selectFormDist();

    /**
     * 下游（零售商）进场确认列表，多对一 JOIN 出下游企业信息。
     * <p>
     * 供批发商环节的"下游企业进场确认"使用——批发商查的是零售批号表，
     * 因此这条查询必须定义在 RetaBatchMapper 上。
     */
    List<ConfirmVO> selectDownConfirmList(@Param("upBatchNos") List<String> upBatchNos,
                                          @Param("downName") String downName);
}
