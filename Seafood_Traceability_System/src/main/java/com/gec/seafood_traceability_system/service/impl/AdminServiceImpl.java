package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.AdminMapper;
import com.gec.seafood_traceability_system.pojo.Admin;
import com.gec.seafood_traceability_system.service.AdminService;
import org.springframework.stereotype.Service;

/** 系统管理员业务实现 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Override
    public Admin findByAdminName(String adminName) {
        return lambdaQuery().eq(Admin::getAdminName, adminName).one();
    }
}
