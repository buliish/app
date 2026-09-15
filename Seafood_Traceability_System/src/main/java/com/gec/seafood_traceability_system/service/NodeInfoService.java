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

    /**
     * 按企业编号更新密码。
     * <p>
     * 登录时 ThreadLocal 还没有值（登录接口在拦截器白名单里），
     * 所以"登录成功顺带把明文密码升级为 BCrypt 哈希"这个动作必须走本方法。
     */
    void updatePwdById(Integer nodeId, String newPwd);

    /** 更新企业联系方式（只更新企业法人、联系电话、地址） */
    void update(NodeInfo nodeInfo);

    /** 按类型与行政区域查询企业列表（加工企业选择上游养殖企业等联动使用） */
    List<NodeInfo> listByTypeAndRegion(Integer nodeType, Integer provId, Integer cityId);

    /** 管理端：节点企业注册信息分页 + 模糊查询 */
    Page<NodeInfo> pageQuery(long current, long size, String code, String name,
                            Integer nodeType, Integer provId, Integer cityId);
}
