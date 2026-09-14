package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.NodeInfo;

import java.util.List;

/**
 * 节点企业业务接口（MyBatis-Plus IService）
 */
public interface NodeInfoService extends IService<NodeInfo> {

    /** 根据登录编码查询企业信息 */
    NodeInfo findByCode(String code);

    /** 更新当前登录企业密码（企业ID取自 token） */
    void updatePwd(String newPwd);

    /** 更新企业联系方式（只更新企业法人、联系电话、地址） */
    void update(NodeInfo nodeInfo);

    /** 按类型与行政区域查询企业列表（加工企业选择上游养殖企业等联动使用） */
    List<NodeInfo> listByTypeAndRegion(Integer nodeType, Integer provId, Integer cityId);

    /** 管理端：节点企业注册信息分页 + 模糊查询 */
    Page<NodeInfo> pageQuery(long current, long size, String code, String name,
                            Integer nodeType, Integer provId, Integer cityId);
}
