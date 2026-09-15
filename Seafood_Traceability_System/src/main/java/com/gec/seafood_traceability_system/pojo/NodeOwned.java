package com.gec.seafood_traceability_system.pojo;

/**
 * "归属于某家节点企业"的数据
 * <p>
 * 四类产品批号以及加工工序都带 node_id 表示归属企业，
 * 实现本接口后即可复用 {@code OwnedBatchService.requireOwned()} 做统一的归属校验，
 * 避免在 4 个 Controller × 多个端点上重复写"这条数据是不是我的"。
 */
public interface NodeOwned {

    /** 归属的企业编号（node_info.node_id） */
    Integer getNodeId();

    /**
     * 业务状态值。
     * <p>
     * 四类批号实体都有 status 字段，Lombok 生成的 getStatus() 会自动覆盖本默认方法；
     * 没有状态概念的实体（如 ProcessRecord）沿用这里返回 null，表示"不参与状态校验"。
     */
    default Integer getStatus() {
        return null;
    }
}
