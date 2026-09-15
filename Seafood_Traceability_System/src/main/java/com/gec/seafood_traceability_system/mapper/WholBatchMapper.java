package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.WholBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 批发商产品批号 Mapper
 * <p>
 * 关联查询见 WholBatchMapper.xml（下游确认列表的多对一映射）。
 */
@Mapper
public interface WholBatchMapper extends BaseMapper<WholBatch> {

    /** 下游（零售商）进场确认列表，多对一 JOIN 出下游企业信息 */
    List<ConfirmVO> selectDownConfirmList(@Param("upBatchNos") List<String> upBatchNos,
                                          @Param("downName") String downName);
}
