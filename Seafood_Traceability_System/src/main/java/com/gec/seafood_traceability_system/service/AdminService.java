package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.Admin;

/** 系统管理员业务接口 */
public interface AdminService extends IService<Admin> {

    /** 按管理员账号查询 */
    Admin findByAdminName(String adminName);
}
