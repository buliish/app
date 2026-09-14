package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.NodeInfoMapper;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 节点企业业务实现（MyBatis-Plus）
 */
@Service
public class NodeInfoServiceImpl extends ServiceImpl<NodeInfoMapper, NodeInfo> implements NodeInfoService {

    @Override
    public NodeInfo findByCode(String code) {
        return lambdaQuery().eq(NodeInfo::getCode, code).one();
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer nodeId = (Integer) map.get("id");
        NodeInfo node = new NodeInfo();
        node.setNodeId(nodeId);
        node.setPassword(newPwd);
        updateById(node);
    }

    @Override
    public void update(NodeInfo nodeInfo) {
        //只允许企业自己维护法人、联系电话与地址（MP 默认忽略 null 字段）
        NodeInfo update = new NodeInfo();
        update.setNodeId(nodeInfo.getNodeId());
        update.setCorporation(nodeInfo.getCorporation());
        update.setTelephone(nodeInfo.getTelephone());
        update.setAddress(nodeInfo.getAddress());
        updateById(update);
    }

    @Override
    public List<NodeInfo> listByTypeAndRegion(Integer nodeType, Integer provId, Integer cityId) {
        return lambdaQuery()
                .eq(nodeType != null, NodeInfo::getNodeType, nodeType)
                .eq(provId != null, NodeInfo::getProvId, provId)
                .eq(cityId != null, NodeInfo::getCityId, cityId)
                .orderByAsc(NodeInfo::getNodeId)
                .list();
    }

    @Override
    public Page<NodeInfo> pageQuery(long current, long size, String code, String name,
                                    Integer nodeType, Integer provId, Integer cityId) {
        return lambdaQuery()
                .like(StringUtils.hasText(code), NodeInfo::getCode, code)
                .like(StringUtils.hasText(name), NodeInfo::getName, name)
                .eq(nodeType != null, NodeInfo::getNodeType, nodeType)
                .eq(provId != null, NodeInfo::getProvId, provId)
                .eq(cityId != null, NodeInfo::getCityId, cityId)
                .orderByAsc(NodeInfo::getNodeId)
                .page(new Page<>(current, size));
    }
}
