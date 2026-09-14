package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节点企业 Mapper：继承 MyBatis-Plus BaseMapper，单表增删改查由 MP 提供
 */
@Mapper
public interface NodeInfoMapper extends BaseMapper<NodeInfo> {
}
