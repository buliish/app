package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.BatchRef;
import com.gec.seafood_traceability_system.pojo.Inspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 各环节检测记录 Mapper
 * <p>
 * 批量插入与多态 IN 查询写在 InspectionMapper.xml 里，
 * 单表增删改查仍走 BaseMapper。
 */
@Mapper
public interface InspectionMapper extends BaseMapper<Inspection> {

    /**
     * 批量新增检测记录（数据层批量操作，<foreach> 多值 INSERT）
     *
     * @param records 非空列表，调用方保证
     * @return 影响行数
     */
    int insertBatch(@Param("records") List<Inspection> records);

    /**
     * 一次取回一条溯源链上四个环节的全部检测记录。
     * <p>
     * 用 (stage_type, batch_id) 的 OR 元组条件匹配，避免逐环节各查一次。
     *
     * @param refs 链路上各环节的批号引用，非空且长度有限（一条链最多 4 个）
     */
    List<Inspection> selectByRefs(@Param("refs") List<BatchRef> refs);
}
