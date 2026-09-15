package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.ConfirmVO;
import com.gec.seafood_traceability_system.pojo.FrozBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 冷冻加工企业产品批号 Mapper
 * <p>
 * 单表增删改查由 MyBatis-Plus BaseMapper 提供；
 * 涉及"批号 → 所属企业"的多对一关联查询写在 FrozBatchMapper.xml 里。
 */
@Mapper
public interface FrozBatchMapper extends BaseMapper<FrozBatch> {

    /**
     * 下游进场确认列表（多对一：下游批号 → 下游企业）
     *
     * @param upBatchNos 本企业已确认的产品批号集合，非空由 Service 保证
     * @param downName   下游企业名称模糊匹配条件，可为 null
     * @return 已补齐 downNode / downName 的确认项
     */
    List<ConfirmVO> selectDownConfirmList(@Param("upBatchNos") List<String> upBatchNos,
                                          @Param("downName") String downName);
}
