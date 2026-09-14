package com.gec.seafood_traceability_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.seafood_traceability_system.pojo.Admin;
import org.apache.ibatis.annotations.Mapper;

/** 系统管理员 Mapper */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
