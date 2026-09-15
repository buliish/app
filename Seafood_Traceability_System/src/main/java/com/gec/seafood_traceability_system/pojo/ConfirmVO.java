package com.gec.seafood_traceability_system.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下游企业进场确认列表项
 * 养殖企业确认冷冻加工企业、冷冻加工企业确认批发商、批发商确认零售商，展示字段一致
 */
@Data
public class ConfirmVO {

    /** 下游批号主键（确认操作使用） */
    private Integer id;

    /** 下游企业节点ID */
    private Integer downNodeId;

    /**
     * 下游企业（多对一：多条下游批号可能归属同一家企业）。
     * <p>
     * 由 Mapper XML 的 &lt;association&gt; 一次 JOIN 查出（不是 N+1 的嵌套子查询），
     * 仅用于承载映射结果，不直接输出给前端——前端要的扁平字段见 downName。
     */
    @JsonIgnore
    private NodeInfo downNode;

    /** 下游企业名称（由 downNode.name 派生，维持前端原有契约不变） */
    private String downName;

    /** 下游企业产品批号 */
    private String downBatchNo;

    /** 进场批号（本企业产品批号） */
    private String upBatchNo;

    /** 品种 */
    private String breed;

    /** 产品类型 */
    private String productType;

    /** 待确认状态值 */
    private Integer status;

    /** 进场（建号）时间 */
    private LocalDateTime createTime;
}
